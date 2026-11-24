package com.minevra.translate

import android.content.Context
import android.util.JsonReader
import android.util.JsonToken
import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.Locale
import kotlin.coroutines.coroutineContext

/**
 * Özel İkili Sözlük Motoru (.mvidx + .mvbin) — v1.2
 * Tüm veri setleri (EN-TR, Webster EN, Phrasal Verbs, Irregular Verbs)
 * ilk açılışta internetten indirilir ve ikili binary dosyalarına derlenir.
 */
class BinaryIndexStore(private val idxFile: File, private val binFile: File) {
    private var idxRaf: RandomAccessFile? = null
    private var binRaf: RandomAccessFile? = null
    private var idxBuf: MappedByteBuffer? = null
    private var entryCount: Int = 0
    private var keyBlobStart: Long = 0L

    val count: Int get() = entryCount
    val isOpen: Boolean get() = idxBuf != null && entryCount > 0

    @Synchronized
    fun open(): Boolean {
        close()
        if (!idxFile.exists() || !binFile.exists() || idxFile.length() < 16L) return false
        return runCatching {
            val rIdx = RandomAccessFile(idxFile, "r")
            val rBin = RandomAccessFile(binFile, "r")
            val buf = rIdx.channel.map(FileChannel.MapMode.READ_ONLY, 0L, idxFile.length())
            val m0 = buf.get(0).toInt().toChar()
            val m1 = buf.get(1).toInt().toChar()
            val m2 = buf.get(2).toInt().toChar()
            val m3 = buf.get(3).toInt().toChar()
            if (m0 != 'M' || m1 != 'V' || m2 != 'B' || m3 != '1') {
                rIdx.close(); rBin.close(); return false
            }
            entryCount = buf.getInt(4)
            keyBlobStart = buf.getLong(8)
            idxRaf = rIdx
            binRaf = rBin
            idxBuf = buf
            entryCount > 0
        }.getOrElse {
            close()
            false
        }
    }

    @Synchronized
    fun close() {
        idxBuf = null
        entryCount = 0
        runCatching { idxRaf?.close() }
        runCatching { binRaf?.close() }
        idxRaf = null
        binRaf = null
    }

    private fun compareAt(buf: MappedByteBuffer, index: Int, query: ByteArray): Int {
        val recPos = 16 + index * 16
        val keyPos = keyBlobStart.toInt() + buf.getInt(recPos)
        val keyLen = buf.getShort(recPos + 4).toInt() and 0xFFFF
        val minLen = minOf(keyLen, query.size)
        for (i in 0 until minLen) {
            val a = buf.get(keyPos + i).toInt() and 0xFF
            val b = query[i].toInt() and 0xFF
            if (a != b) return a - b
        }
        return keyLen - query.size
    }

    private fun readKeyAt(buf: MappedByteBuffer, index: Int): BinarySearchResult {
        val recPos = 16 + index * 16
        val keyPos = keyBlobStart.toInt() + buf.getInt(recPos)
        val keyLen = buf.getShort(recPos + 4).toInt() and 0xFFFF
        val flags = buf.getShort(recPos + 6).toInt() and 0xFFFF
        val dataOffset = buf.getLong(recPos + 8)
        val bytes = ByteArray(keyLen)
        for (i in 0 until keyLen) {
            bytes[i] = buf.get(keyPos + i)
        }
        return BinarySearchResult(String(bytes, Charsets.UTF_8), dataOffset, flags)
    }

    @Synchronized
    fun searchPrefix(rawQuery: String, limit: Int = 60): List<BinarySearchResult> {
        val buf = idxBuf ?: return emptyList()
        if (entryCount == 0) return emptyList()
        val q = rawQuery.trim().lowercase(Locale.ROOT)
        if (q.isEmpty()) {
            val end = minOf(entryCount, limit)
            val list = ArrayList<BinarySearchResult>(end)
            for (i in 0 until end) list.add(readKeyAt(buf, i))
            return list
        }
        val qBytes = q.toByteArray(Charsets.UTF_8)
        var lo = 0
        var hi = entryCount
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if (compareAt(buf, mid, qBytes) < 0) lo = mid + 1 else hi = mid
        }
        val start = if (lo >= entryCount) maxOf(0, entryCount - limit) else lo
        val end = minOf(entryCount, start + limit)
        val results = ArrayList<BinarySearchResult>(end - start)
        for (i in start until end) {
            results.add(readKeyAt(buf, i))
        }
        return results
    }

    @Synchronized
    fun findExact(rawWord: String): DictRecord? {
        val buf = idxBuf ?: return null
        if (entryCount == 0) return null
        val qBytes = rawWord.trim().lowercase(Locale.ROOT).toByteArray(Charsets.UTF_8)
        var lo = 0
        var hi = entryCount - 1
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            val cmp = compareAt(buf, mid, qBytes)
            when {
                cmp < 0 -> lo = mid + 1
                cmp > 0 -> hi = mid - 1
                else -> {
                    val dataOffset = buf.getLong(16 + mid * 16 + 8)
                    return readRecordAtOffset(dataOffset)
                }
            }
        }
        return null
    }

    @Synchronized
    fun readRecordAtOffset(offset: Long): DictRecord? {
        val raf = binRaf ?: return null
        return runCatching {
            raf.seek(offset)
            fun readStr(): String {
                val len = raf.readInt()
                if (len <= 0) return ""
                val b = ByteArray(len)
                raf.readFully(b)
                return String(b, Charsets.UTF_8)
            }
            val word = readStr()
            val pos = readStr()
            val turkish = readStr()
            val english = readStr()
            val exRaw = readStr()
            val synRaw = readStr()
            val phonetic = readStr()
            val pastSimple = readStr()
            val pastParticiple = readStr()
            val formsExtra = readStr()
            val parts = formsExtra.split("|")
            DictRecord(
                word = word,
                pos = pos,
                turkish = turkish,
                english = english,
                examples = if (exRaw.isEmpty()) emptyList() else exRaw.split("\n").filter { it.isNotBlank() },
                synonyms = if (synRaw.isEmpty()) emptyList() else synRaw.split("|").filter { it.isNotBlank() },
                phonetic = phonetic,
                pastSimple = pastSimple,
                pastParticiple = pastParticiple,
                thirdPerson = parts.getOrNull(0).orEmpty(),
                gerund = parts.getOrNull(1).orEmpty(),
                source = "Offline Binary"
            )
        }.getOrNull()
    }

    companion object {
        private val UNSIGNED_BYTE_COMP = Comparator<Pair<ByteArray, DictRecord>> { a, b ->
            val ka = a.first
            val kb = b.first
            val minLen = minOf(ka.size, kb.size)
            for (i in 0 until minLen) {
                val va = ka[i].toInt() and 0xFF
                val vb = kb[i].toInt() and 0xFF
                if (va != vb) return@Comparator va - vb
            }
            ka.size - kb.size
        }

        fun writeBinaryFiles(idxFile: File, binFile: File, entries: Map<String, DictRecord>) {
            val idxTmp = File(idxFile.parentFile, idxFile.name + ".tmp")
            val binTmp = File(binFile.parentFile, binFile.name + ".tmp")

            val sorted = ArrayList<Pair<ByteArray, DictRecord>>(entries.size)
            for ((k, v) in entries) {
                val keyClean = k.trim().lowercase(Locale.ROOT)
                if (keyClean.isNotEmpty() && keyClean.length <= 240) {
                    sorted.add(keyClean.toByteArray(Charsets.UTF_8) to v)
                }
            }
            sorted.sortWith(UNSIGNED_BYTE_COMP)

            val offsets = LongArray(sorted.size)
            DataOutputStream(BufferedOutputStream(FileOutputStream(binTmp), 65536)).use { binOut ->
                var curOffset = 0L
                fun writeStr(s: String) {
                    val b = s.toByteArray(Charsets.UTF_8)
                    binOut.writeInt(b.size)
                    binOut.write(b)
                    curOffset += 4 + b.size
                }
                for (i in sorted.indices) {
                    offsets[i] = curOffset
                    val r = sorted[i].second
                    writeStr(r.word)
                    writeStr(r.pos)
                    writeStr(r.turkish)
                    writeStr(r.english)
                    writeStr(r.examples.joinToString("\n"))
                    writeStr(r.synonyms.joinToString("|"))
                    writeStr(r.phonetic)
                    writeStr(r.pastSimple)
                    writeStr(r.pastParticiple)
                    writeStr("${r.thirdPerson}|${r.gerund}")
                }
            }

            DataOutputStream(BufferedOutputStream(FileOutputStream(idxTmp), 65536)).use { idxOut ->
                val count = sorted.size
                val keyBlobStart = 16L + count.toLong() * 16L
                idxOut.writeByte('M'.code)
                idxOut.writeByte('V'.code)
                idxOut.writeByte('B'.code)
                idxOut.writeByte('1'.code)
                idxOut.writeInt(count)
                idxOut.writeLong(keyBlobStart)

                var relOffset = 0
                for (i in sorted.indices) {
                    val kb = sorted[i].first
                    idxOut.writeInt(relOffset)
                    idxOut.writeShort(kb.size)
                    idxOut.writeShort(0)
                    idxOut.writeLong(offsets[i])
                    relOffset += kb.size
                }
                for (i in sorted.indices) {
                    idxOut.write(sorted[i].first)
                }
            }

            if (binFile.exists()) binFile.delete()
            if (idxFile.exists()) idxFile.delete()
            binTmp.renameTo(binFile)
            idxTmp.renameTo(idxFile)
        }
    }
}

class BinaryDictEngine(private val context: Context) {
    private val enTrStore = BinaryIndexStore(File(context.filesDir, "en_tr.mvidx"), File(context.filesDir, "en_tr.mvbin"))
    private val trEnStore = BinaryIndexStore(File(context.filesDir, "tr_en.mvidx"), File(context.filesDir, "tr_en.mvbin"))
    private val phrasalStore = BinaryIndexStore(File(context.filesDir, "phrasal.mvidx"), File(context.filesDir, "phrasal.mvbin"))
    private val irregularStore = BinaryIndexStore(File(context.filesDir, "irregular.mvidx"), File(context.filesDir, "irregular.mvbin"))

    private val prefs by lazy { context.getSharedPreferences("minevra_prefs", Context.MODE_PRIVATE) }

    companion object {
        // v1.2: Temiz Phrasal Verbs paketi için sürüm anahtarı güncellendi
        private const val PREF_PACK_READY = "binary_pack_v3_ready"
        private const val URL_FREEDICT_EN_TR = "https://raw.githubusercontent.com/freedict/fd-dictionaries/master/eng-tur/eng-tur.tei"
        private const val URL_WEBSTER_EN = "https://raw.githubusercontent.com/matthewreagan/WebstersEnglishDictionary/master/dictionary_compact.json"
        private const val URL_IRREGULAR_JSON = "https://raw.githubusercontent.com/WithEnglishWeCan/generated-english-irregular-verbs/master/irregular.verbs.build.json"

        // Phrasal verb parçacıkları (kısa ve temiz deyimsel fiil tespiti için)
        private val PHRASAL_PARTICLES = setOf(
            "up", "down", "in", "out", "on", "off", "over", "away", "back", "by",
            "for", "into", "onto", "through", "with", "after", "about", "across",
            "along", "around", "ahead", "apart", "aside", "at", "from", "of", "to",
            "under", "aboard", "ashore", "astray", "forward", "together", "against",
            "behind", "beyond", "without", "upon", "round", "past"
        )
        private val CLEAN_PHRASE_REGEX = Regex("^[a-z]+(?: [a-z]+){1,3}$")
    }

    fun isFullPackReady(): Boolean =
        prefs.getBoolean(PREF_PACK_READY, false) &&
            enTrStore.count > 10000 &&
            irregularStore.count > 100 &&
            phrasalStore.count > 100

    fun totalOfflineWords(): Int = enTrStore.count

    fun getStore(mode: DictMode): BinaryIndexStore = when (mode) {
        DictMode.EN_TR -> enTrStore
        DictMode.TR_EN -> trEnStore
        DictMode.PHRASAL -> phrasalStore
        DictMode.IRREGULAR -> irregularStore
    }

    fun initAndEnsureStarterBinary() {
        enTrStore.open()
        trEnStore.open()
        phrasalStore.open()
        irregularStore.open()
    }

    fun search(mode: DictMode, query: String, limit: Int = 60): List<BinarySearchResult> {
        return getStore(mode).searchPrefix(query, limit)
    }

    fun readRecord(mode: DictMode, offset: Long): DictRecord? {
        return getStore(mode).readRecordAtOffset(offset)
    }

    fun findExactAny(word: String): DictRecord? {
        return enTrStore.findExact(word)
            ?: phrasalStore.findExact(word)
            ?: irregularStore.findExact(word)
    }

    private fun isCleanPhrasalCandidate(key: String, pos: String): Boolean {
        if (key.length !in 4..28) return false
        if (!CLEAN_PHRASE_REGEX.matches(key)) return false
        val parts = key.split(' ')
        if (parts.size !in 2..4) return false
        // Fiil olarak işaretlenmiş çok kelimeli kayıtlar veya 2./son kelimesi phrasal parçacık olanlar
        if (pos.equals("Fiil", ignoreCase = true) || pos.equals("v", ignoreCase = true)) return true
        return parts[1] in PHRASAL_PARTICLES || parts.last() in PHRASAL_PARTICLES
    }

    /**
     * Tüm açık kaynak veri setlerini indirip ikili (.mvidx + .mvbin) formatına dönüştürür.
     */
    suspend fun downloadAndBuildBinaryDatabase(
        onProgress: (DownloadProgress) -> Unit
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val mainMap = HashMap<String, MutableEntry>(135000)
            val trReverse = HashMap<String, LinkedHashSet<String>>(45000)
            val phrasalMap = HashMap<String, DictRecord>(6000)
            val irregularMap = HashMap<String, DictRecord>(600)

            // 1) FreeDict İngilizce-Türkçe XML (~36.000+ kayıt + Türkçe Phrasal Verbs)
            onProgress(DownloadProgress(true, "1/4 İngilizce-Türkçe sözlük indiriliyor...", 10))
            runCatching {
                openHttpStream(URL_FREEDICT_EN_TR, 25000).use { rawStream ->
                    val parser = Xml.newPullParser()
                    parser.setInput(BufferedInputStream(rawStream, 65536), "UTF-8")
                    var inEntry = false
                    var currentWord = ""
                    var currentPos = ""
                    val trQuotes = LinkedHashSet<String>()
                    var tag = ""
                    var eventType = parser.eventType
                    while (eventType != XmlPullParser.END_DOCUMENT && coroutineContext.isActive) {
                        when (eventType) {
                            XmlPullParser.START_TAG -> {
                                tag = parser.name
                                if (tag == "entry") {
                                    inEntry = true
                                    currentWord = ""
                                    currentPos = ""
                                    trQuotes.clear()
                                }
                            }
                            XmlPullParser.TEXT -> {
                                if (inEntry) {
                                    val txt = parser.text?.trim().orEmpty()
                                    if (txt.isNotEmpty()) {
                                        when (tag) {
                                            "orth" -> if (currentWord.isEmpty()) currentWord = txt
                                            "pos" -> if (currentPos.isEmpty()) currentPos = mapPosToTurkish(txt)
                                            "quote", "def" -> trQuotes.add(txt)
                                        }
                                    }
                                }
                            }
                            XmlPullParser.END_TAG -> {
                                if (parser.name == "entry" && inEntry) {
                                    inEntry = false
                                    val key = currentWord.trim().lowercase(Locale.ROOT)
                                    if (key.isNotEmpty() && trQuotes.isNotEmpty()) {
                                        val trJoined = trQuotes.joinToString(" ● ")
                                        val m = mainMap.getOrPut(key) { MutableEntry(key) }
                                        if (m.pos.isEmpty()) m.pos = currentPos
                                        m.appendTurkish(trJoined)

                                        // Ters Türkçe -> İngilizce indekslemesi
                                        for (q in trQuotes) {
                                            val trKey = q.split(",", ";").firstOrNull()?.trim()?.lowercase(Locale("tr", "TR")).orEmpty()
                                            if (trKey.length in 2..60) {
                                                trReverse.getOrPut(trKey) { LinkedHashSet() }.add(key)
                                            }
                                        }

                                        // Temiz, kısa Phrasal Verbs (ör. "go aboard" -> "gemiye binmek")
                                        if (isCleanPhrasalCandidate(key, currentPos)) {
                                            val existing = phrasalMap[key]
                                            if (existing == null) {
                                                phrasalMap[key] = DictRecord(
                                                    word = key,
                                                    pos = "Phrasal Verb",
                                                    turkish = trJoined
                                                )
                                            } else if (!existing.turkish.contains(trJoined) && existing.turkish.length < 500) {
                                                phrasalMap[key] = existing.copy(turkish = "${existing.turkish} ● $trJoined")
                                            }
                                        }
                                    }
                                }
                                tag = ""
                            }
                        }
                        eventType = parser.next()
                    }
                }
            }

            if (!coroutineContext.isActive) return@withContext Result.failure(Exception("İptal edildi"))

            // 2) Webster's Unabridged (~102.000+ İngilizce kelime) - Streaming JsonReader
            onProgress(DownloadProgress(true, "2/4 100.000+ İngilizce kelime indiriliyor...", 35, mainMap.size))
            runCatching {
                openHttpStream(URL_WEBSTER_EN, 30000).use { rawStream ->
                    JsonReader(InputStreamReader(BufferedInputStream(rawStream, 65536), Charsets.UTF_8)).use { reader ->
                        reader.beginObject()
                        var parsed = 0
                        while (reader.hasNext() && coroutineContext.isActive) {
                            val name = reader.nextName().trim().lowercase(Locale.ROOT)
                            val defVal = if (reader.peek() == JsonToken.STRING) reader.nextString().trim() else {
                                reader.skipValue(); ""
                            }
                            if (name.isNotEmpty() && defVal.isNotEmpty()) {
                                val shortDef = if (defVal.length > 900) defVal.take(900) + "…" else defVal
                                val m = mainMap.getOrPut(name) { MutableEntry(name) }
                                if (m.english.isEmpty()) m.english = shortDef

                                // Eğer phrasalMap'te varsa İngilizce tanımını da ekle
                                val existingP = phrasalMap[name]
                                if (existingP != null && existingP.english.isEmpty()) {
                                    phrasalMap[name] = existingP.copy(english = shortDef)
                                }

                                parsed++
                                if (parsed % 20000 == 0) {
                                    val pct = 35 + ((parsed * 32) / 105000).coerceAtMost(32)
                                    onProgress(DownloadProgress(true, "2/4 İşleniyor (${mainMap.size} kelime)...", pct, mainMap.size))
                                }
                            }
                        }
                        reader.endObject()
                    }
                }
            }

            if (!coroutineContext.isActive) return@withContext Result.failure(Exception("İptal edildi"))

            // 3) Irregular Verbs (~470+ tam liste — açık kaynak JSON)
            onProgress(DownloadProgress(true, "3/4 Irregular Verbs indiriliyor...", 74, mainMap.size))
            runCatching {
                val jsonText = openHttpStream(URL_IRREGULAR_JSON, 20000).bufferedReader(Charsets.UTF_8).use { it.readText() }
                val root = JSONObject(jsonText)
                val keys = root.keys()
                while (keys.hasNext() && coroutineContext.isActive) {
                    val verb = keys.next().trim().lowercase(Locale.ROOT)
                    if (verb.isEmpty()) continue
                    val arr = root.optJSONArray(verb) ?: continue

                    val v2Set = LinkedHashSet<String>()
                    val v3Set = LinkedHashSet<String>()
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        extractJsonStringList(obj.opt("2")).forEach { v2Set.add(it) }
                        extractJsonStringList(obj.opt("3")).forEach { v3Set.add(it) }
                    }

                    val v2 = v2Set.joinToString("/")
                    val v3 = v3Set.joinToString("/")
                    if (v2.isNotEmpty() || v3.isNotEmpty()) {
                        val third = TextExtractor.makeThirdPerson(verb)
                        val gerund = TextExtractor.makeGerund(verb)
                        val mainEntry = mainMap[verb]
                        val tr = mainEntry?.turkish.orEmpty()
                        val en = mainEntry?.english.orEmpty()

                        irregularMap[verb] = DictRecord(
                            word = verb.replaceFirstChar { it.uppercaseChar() },
                            pos = "Fiil (Irregular)",
                            turkish = tr,
                            english = en,
                            pastSimple = v2,
                            pastParticiple = v3,
                            thirdPerson = third,
                            gerund = gerund
                        )

                        val m = mainMap.getOrPut(verb) { MutableEntry(verb) }
                        m.pastSimple = v2
                        m.pastParticiple = v3
                        m.thirdPerson = third
                        m.gerund = gerund
                    }
                }
            }

            if (!coroutineContext.isActive) return@withContext Result.failure(Exception("İptal edildi"))

            // 4) İkili (.mvidx + .mvbin) sistemine derle
            onProgress(DownloadProgress(true, "4/4 İkili Binary sistemine derleniyor (${mainMap.size} kelime)...", 86, mainMap.size))

            val finalMain = HashMap<String, DictRecord>(mainMap.size)
            for ((k, m) in mainMap) {
                finalMain[k] = DictRecord(
                    word = m.word,
                    pos = m.pos,
                    turkish = m.turkish,
                    english = m.english,
                    pastSimple = m.pastSimple,
                    pastParticiple = m.pastParticiple,
                    thirdPerson = m.thirdPerson,
                    gerund = m.gerund
                )
            }
            mainMap.clear()

            enTrStore.close()
            BinaryIndexStore.writeBinaryFiles(
                File(context.filesDir, "en_tr.mvidx"),
                File(context.filesDir, "en_tr.mvbin"),
                finalMain
            )
            enTrStore.open()

            val finalTr = HashMap<String, DictRecord>(trReverse.size)
            for ((trKey, enSet) in trReverse) {
                finalTr[trKey] = DictRecord(
                    word = trKey,
                    pos = "TR → EN",
                    turkish = enSet.take(16).joinToString(" ● ")
                )
            }
            trReverse.clear()

            trEnStore.close()
            BinaryIndexStore.writeBinaryFiles(
                File(context.filesDir, "tr_en.mvidx"),
                File(context.filesDir, "tr_en.mvbin"),
                finalTr
            )
            trEnStore.open()

            phrasalStore.close()
            BinaryIndexStore.writeBinaryFiles(
                File(context.filesDir, "phrasal.mvidx"),
                File(context.filesDir, "phrasal.mvbin"),
                phrasalMap
            )
            phrasalStore.open()

            irregularStore.close()
            BinaryIndexStore.writeBinaryFiles(
                File(context.filesDir, "irregular.mvidx"),
                File(context.filesDir, "irregular.mvbin"),
                irregularMap
            )
            irregularStore.open()

            val total = enTrStore.count
            if (total > 1000 && irregularStore.count > 100) {
                prefs.edit().putBoolean(PREF_PACK_READY, true).apply()
            } else {
                throw Exception("Veri setlerinden biri indirilemedi (İnternet bağlantınızı kontrol edin)")
            }

            onProgress(
                DownloadProgress(
                    running = false,
                    stage = "Hazır ($total kelime • ${irregularStore.count} irregular • ${phrasalStore.count} phrasal)",
                    percent = 100,
                    totalIndexedWords = total,
                    completedBanner = true
                )
            )
            Result.success(total)
        } catch (e: Exception) {
            onProgress(DownloadProgress(false, "İndirme kesildi", 0, enTrStore.count, error = e.message ?: "Bağlantı hatası"))
            Result.failure(e)
        }
    }

    /**
     * Online 5 Katmanlı Zenginleştirme (Free Dictionary API -> Wiktionary -> Datamuse + Stemming)
     */
    suspend fun enrichOnline(rawWord: String): SavedWordEntry? = withContext(Dispatchers.IO) {
        val word = rawWord.trim().lowercase(Locale.ROOT)
        if (word.isEmpty()) return@withContext null

        var res = fetchFreeDictionary(word)
            ?: fetchWiktionary(word)
            ?: fetchDatamuseDefs(word)

        if (res != null && res.meanings.sumOf { it.synonyms.size } == 0) {
            val extraSyns = fetchDatamuseSynonyms(word)
            if (extraSyns.isNotEmpty() && res.meanings.isNotEmpty()) {
                val first = res.meanings.first().copy(synonyms = extraSyns)
                res = res.copy(meanings = listOf(first) + res.meanings.drop(1))
            }
        }

        if (res == null) {
            for (root in TextExtractor.generateRootCandidates(word)) {
                val rootRes = fetchFreeDictionary(root) ?: fetchWiktionary(root) ?: fetchDatamuseDefs(root)
                if (rootRes != null) {
                    res = rootRes.copy(word = word, rootWord = root, source = "Root Match")
                    break
                }
            }
        }

        val offlineTr = enTrStore.findExact(word)?.turkish.orEmpty()
        res?.copy(turkish = offlineTr.ifEmpty { res.turkish })
    }

    private fun fetchFreeDictionary(word: String): SavedWordEntry? = runCatching {
        val enc = URLEncoder.encode(word, "UTF-8")
        val body = httpGetString("https://api.dictionaryapi.dev/api/v2/entries/en/$enc", 6000) ?: return null
        val arr = JSONArray(body)
        if (arr.length() == 0) return null

        var phonetic = ""
        val meaningsMap = LinkedHashMap<String, Pair<MutableList<DefinitionItem>, LinkedHashSet<String>>>()

        for (i in 0 until arr.length()) {
            val item = arr.optJSONObject(i) ?: continue
            if (phonetic.isEmpty()) phonetic = item.optString("phonetic").trim()
            item.optJSONArray("phonetics")?.let { phs ->
                for (p in 0 until phs.length()) {
                    val txt = phs.optJSONObject(p)?.optString("text")?.trim().orEmpty()
                    if (phonetic.isEmpty() && txt.isNotEmpty()) phonetic = txt
                }
            }
            item.optJSONArray("meanings")?.let { ms ->
                for (m in 0 until ms.length()) {
                    val mo = ms.optJSONObject(m) ?: continue
                    val pos = mo.optString("partOfSpeech", "other").lowercase(Locale.ROOT)
                    val pair = meaningsMap.getOrPut(pos) { mutableListOf<DefinitionItem>() to linkedSetOf() }
                    mo.optJSONArray("synonyms")?.let { sa ->
                        for (s in 0 until sa.length()) {
                            val syn = sa.optString(s).trim().lowercase(Locale.ROOT)
                            if (syn.isNotEmpty() && syn != word) pair.second.add(syn)
                        }
                    }
                    mo.optJSONArray("definitions")?.let { da ->
                        for (d in 0 until da.length()) {
                            val dobj = da.optJSONObject(d) ?: continue
                            val dt = dobj.optString("definition").trim()
                            if (dt.isNotEmpty() && pair.first.none { it.definition == dt }) {
                                pair.first.add(DefinitionItem(dt, dobj.optString("example").trim()))
                            }
                            dobj.optJSONArray("synonyms")?.let { sa ->
                                for (s in 0 until sa.length()) {
                                    val syn = sa.optString(s).trim().lowercase(Locale.ROOT)
                                    if (syn.isNotEmpty() && syn != word) pair.second.add(syn)
                                }
                            }
                        }
                    }
                }
            }
        }
        val meanings = meaningsMap.entries
            .filter { it.value.first.isNotEmpty() }
            .map { MeaningGroup(it.key, it.value.first, it.value.second.take(16)) }
        if (meanings.isEmpty()) return null
        SavedWordEntry(word = word, phonetic = phonetic, source = "Free Dictionary", meanings = meanings, enriched = true)
    }.getOrNull()

    private fun fetchWiktionary(word: String): SavedWordEntry? = runCatching {
        val enc = URLEncoder.encode(word, "UTF-8")
        val body = httpGetString("https://en.wiktionary.org/api/rest_v1/page/definition/$enc", 6000) ?: return null
        val enArr = JSONObject(body).optJSONArray("en") ?: return null
        val meanings = mutableListOf<MeaningGroup>()
        for (i in 0 until enArr.length()) {
            val sec = enArr.optJSONObject(i) ?: continue
            val pos = sec.optString("partOfSpeech", "other").lowercase(Locale.ROOT)
            val defs = mutableListOf<DefinitionItem>()
            sec.optJSONArray("definitions")?.let { da ->
                for (j in 0 until da.length()) {
                    val dobj = da.optJSONObject(j) ?: continue
                    val clean = dobj.optString("definition").replace(Regex("<[^>]+>"), " ").replace(Regex("\\s+"), " ").trim()
                    if (clean.isNotEmpty()) defs.add(DefinitionItem(clean))
                }
            }
            if (defs.isNotEmpty()) meanings.add(MeaningGroup(pos, defs))
        }
        if (meanings.isEmpty()) return null
        SavedWordEntry(word = word, source = "Wiktionary", meanings = meanings, enriched = true)
    }.getOrNull()

    private fun fetchDatamuseDefs(word: String): SavedWordEntry? = runCatching {
        val enc = URLEncoder.encode(word, "UTF-8")
        val body = httpGetString("https://api.datamuse.com/words?sp=$enc&md=d&max=5", 5000) ?: return null
        val arr = JSONArray(body)
        val posMap = mapOf("n" to "noun", "v" to "verb", "adj" to "adjective", "adv" to "adverb")
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            if (obj.optString("word").equals(word, ignoreCase = true)) {
                val defsArr = obj.optJSONArray("defs") ?: continue
                val grouped = LinkedHashMap<String, MutableList<DefinitionItem>>()
                for (j in 0 until defsArr.length()) {
                    val raw = defsArr.optString(j)
                    val tab = raw.indexOf('\t')
                    if (tab > 0) {
                        val p = posMap[raw.substring(0, tab)] ?: "other"
                        val d = raw.substring(tab + 1).trim()
                        if (d.isNotEmpty()) grouped.getOrPut(p) { mutableListOf() }.add(DefinitionItem(d))
                    }
                }
                val meanings = grouped.map { MeaningGroup(it.key, it.value) }
                if (meanings.isNotEmpty()) {
                    return SavedWordEntry(word = word, source = "Datamuse", meanings = meanings, enriched = true)
                }
            }
        }
        null
    }.getOrNull()

    private fun fetchDatamuseSynonyms(word: String): List<String> = runCatching {
        val enc = URLEncoder.encode(word, "UTF-8")
        val body = httpGetString("https://api.datamuse.com/words?rel_syn=$enc&max=14", 4500) ?: return emptyList()
        val arr = JSONArray(body)
        val out = mutableListOf<String>()
        for (i in 0 until arr.length()) {
            val s = arr.optJSONObject(i)?.optString("word")?.trim()?.lowercase(Locale.ROOT).orEmpty()
            if (s.isNotEmpty() && s != word && s !in out) out.add(s)
        }
        out
    }.getOrDefault(emptyList())

    private fun openHttpStream(urlStr: String, timeoutMs: Int): java.io.InputStream {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            connectTimeout = timeoutMs
            readTimeout = timeoutMs
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "MinevraTranslate/1.2 (Android)")
        }
        if (conn.responseCode !in 200..299) {
            throw Exception("HTTP ${conn.responseCode}")
        }
        return conn.inputStream
    }

    private fun httpGetString(urlStr: String, timeoutMs: Int): String? = runCatching {
        openHttpStream(urlStr, timeoutMs).bufferedReader(Charsets.UTF_8).use { it.readText() }
    }.getOrNull()

    private fun mapPosToTurkish(pos: String): String = when (pos.lowercase(Locale.ROOT)) {
        "n", "noun" -> "İsim"
        "v", "verb", "vt", "vi" -> "Fiil"
        "adj", "adjective" -> "Sıfat"
        "adv", "adverb" -> "Zarf"
        "prep" -> "Edat"
        "conj" -> "Bağlaç"
        "pron" -> "Zamir"
        else -> pos
    }

    private fun extractJsonStringList(value: Any?): List<String> {
        val out = mutableListOf<String>()
        when (value) {
            is JSONArray -> for (i in 0 until value.length()) {
                val s = value.optString(i).trim()
                if (s.isNotEmpty()) out.add(s)
            }
            is String -> if (value.isNotBlank()) out.add(value.trim())
        }
        return out
    }

    private class MutableEntry(
        val word: String,
        var pos: String = "",
        var turkish: String = "",
        var english: String = "",
        var pastSimple: String = "",
        var pastParticiple: String = "",
        var thirdPerson: String = "",
        var gerund: String = ""
    ) {
        fun appendTurkish(extra: String) {
            if (extra.isEmpty()) return
            turkish = if (turkish.isEmpty()) extra
            else if (!turkish.contains(extra) && turkish.length < 700) "$turkish ● $extra"
            else turkish
        }
    }
}
