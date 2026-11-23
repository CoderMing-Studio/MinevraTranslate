package com.minevra.translate

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Locale

enum class DictMode(val title: String, val searchPrefix: String) {
    EN_TR("English ⇄ Turkish", "Search"),
    TR_EN("Turkish ⇄ English", "Ara"),
    PHRASAL("Phrasal Verbs", "Phrasal"),
    IRREGULAR("Irregular Verbs", "Verb")
}

enum class MainTab { SEARCH, DICTIONARY, BULK }
enum class DictSubTab { ALL, FAVORITES, TAGS }

data class BinarySearchResult(
    val key: String,
    val dataOffset: Long,
    val flags: Int
)

data class DictRecord(
    val word: String,
    val pos: String = "",
    val turkish: String = "",
    val english: String = "",
    val examples: List<String> = emptyList(),
    val synonyms: List<String> = emptyList(),
    val phonetic: String = "",
    val pastSimple: String = "",
    val pastParticiple: String = "",
    val thirdPerson: String = "",
    val gerund: String = "",
    val rootWord: String = "",
    val source: String = "Offline Binary"
)

data class DefinitionItem(
    val definition: String,
    val example: String = ""
)

data class MeaningGroup(
    val partOfSpeech: String,
    val definitions: List<DefinitionItem>,
    val synonyms: List<String> = emptyList()
)

data class SavedWordEntry(
    val word: String,
    val phonetic: String = "",
    val turkish: String = "",
    val source: String = "Offline Binary",
    val rootWord: String = "",
    val meanings: List<MeaningGroup> = emptyList(),
    val needsFetch: Boolean = false,
    val enriched: Boolean = false,
    val favorite: Boolean = false,
    val tags: List<String> = emptyList(),
    val addedAt: Long = System.currentTimeMillis(),
    val lastViewedAt: Long = System.currentTimeMillis()
)

data class PersonalStore(
    val words: Map<String, SavedWordEntry> = emptyMap(),
    val customTags: List<String> = emptyList()
)

data class DownloadProgress(
    val running: Boolean = false,
    val stage: String = "",
    val percent: Int = 0,
    val totalIndexedWords: Int = 0,
    val error: String? = null,
    val completedBanner: Boolean = false
)

data class BulkCommitSummary(
    val addedNew: Int,
    val offlinePopulated: Int,
    val pendingCount: Int,
    val taggedExisting: Int,
    val skippedExisting: Int
)

object PersonalStoreRepo {
    private const val FILE_NAME = "minevra_personal_store_v1.json"

    fun load(context: Context): PersonalStore {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return PersonalStore()
        return runCatching {
            fromJson(file.readText(Charsets.UTF_8))
        }.getOrDefault(PersonalStore())
    }

    fun save(context: Context, store: PersonalStore) {
        runCatching {
            val tmp = File(context.filesDir, "$FILE_NAME.tmp")
            val dst = File(context.filesDir, FILE_NAME)
            tmp.writeText(toJson(store), Charsets.UTF_8)
            tmp.renameTo(dst)
        }
    }

    fun toJson(store: PersonalStore): String {
        val root = JSONObject()
        val tagsArr = JSONArray()
        store.customTags.sorted().forEach { tagsArr.put(it) }
        root.put("customTags", tagsArr)

        val wordsObj = JSONObject()
        store.words.forEach { (k, e) ->
            val w = JSONObject()
            w.put("word", e.word)
            w.put("phonetic", e.phonetic)
            w.put("turkish", e.turkish)
            w.put("source", e.source)
            w.put("rootWord", e.rootWord)
            w.put("needsFetch", e.needsFetch)
            w.put("enriched", e.enriched)
            w.put("favorite", e.favorite)
            w.put("addedAt", e.addedAt)
            w.put("lastViewedAt", e.lastViewedAt)

            val wTags = JSONArray()
            e.tags.forEach { wTags.put(it) }
            w.put("tags", wTags)

            val mArr = JSONArray()
            e.meanings.forEach { m ->
                val mObj = JSONObject()
                mObj.put("partOfSpeech", m.partOfSpeech)
                val dArr = JSONArray()
                m.definitions.forEach { d ->
                    val dObj = JSONObject()
                    dObj.put("definition", d.definition)
                    dObj.put("example", d.example)
                    dArr.put(dObj)
                }
                mObj.put("definitions", dArr)
                val sArr = JSONArray()
                m.synonyms.forEach { sArr.put(it) }
                mObj.put("synonyms", sArr)
                mArr.put(mObj)
            }
            w.put("meanings", mArr)
            wordsObj.put(k, w)
        }
        root.put("words", wordsObj)
        return root.toString(2)
    }

    fun fromJson(jsonStr: String): PersonalStore {
        val root = JSONObject(jsonStr)
        val tagSet = linkedSetOf<String>()
        root.optJSONArray("customTags")?.let { arr ->
            for (i in 0 until arr.length()) {
                val t = arr.optString(i).trim()
                if (t.isNotEmpty()) tagSet.add(t)
            }
        }
        val wordsMap = linkedMapOf<String, SavedWordEntry>()
        root.optJSONObject("words")?.let { wObj ->
            val keys = wObj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val raw = wObj.optJSONObject(k) ?: continue
                val word = raw.optString("word", k).trim().lowercase(Locale.ROOT)
                if (word.isEmpty()) continue
                val tags = mutableListOf<String>()
                raw.optJSONArray("tags")?.let { ta ->
                    for (i in 0 until ta.length()) {
                        val t = ta.optString(i).trim()
                        if (t.isNotEmpty() && t !in tags) {
                            tags.add(t)
                            tagSet.add(t)
                        }
                    }
                }
                val meanings = mutableListOf<MeaningGroup>()
                raw.optJSONArray("meanings")?.let { ma ->
                    for (i in 0 until ma.length()) {
                        val mo = ma.optJSONObject(i) ?: continue
                        val pos = mo.optString("partOfSpeech", "other")
                        val defs = mutableListOf<DefinitionItem>()
                        mo.optJSONArray("definitions")?.let { da ->
                            for (j in 0 until da.length()) {
                                val dobj = da.optJSONObject(j) ?: continue
                                val defText = dobj.optString("definition").trim()
                                if (defText.isNotEmpty()) {
                                    defs.add(DefinitionItem(defText, dobj.optString("example").trim()))
                                }
                            }
                        }
                        val syns = mutableListOf<String>()
                        mo.optJSONArray("synonyms")?.let { sa ->
                            for (j in 0 until sa.length()) {
                                val s = sa.optString(j).trim().lowercase(Locale.ROOT)
                                if (s.isNotEmpty() && s !in syns) syns.add(s)
                            }
                        }
                        if (defs.isNotEmpty() || syns.isNotEmpty()) {
                            meanings.add(MeaningGroup(pos, defs, syns))
                        }
                    }
                }
                val turkish = raw.optString("turkish").trim()
                val needsFetch = raw.optBoolean("needsFetch", false) && meanings.isEmpty() && turkish.isEmpty()
                wordsMap[word] = SavedWordEntry(
                    word = word,
                    phonetic = raw.optString("phonetic").trim(),
                    turkish = turkish,
                    source = raw.optString("source", if (needsFetch) "Pending Online" else "Cached Offline"),
                    rootWord = raw.optString("rootWord").trim(),
                    meanings = meanings,
                    needsFetch = needsFetch,
                    enriched = raw.optBoolean("enriched", false),
                    favorite = raw.optBoolean("favorite", false),
                    tags = tags,
                    addedAt = raw.optLong("addedAt", System.currentTimeMillis()),
                    lastViewedAt = raw.optLong("lastViewedAt", System.currentTimeMillis())
                )
            }
        }
        return PersonalStore(wordsMap, tagSet.sortedBy { it.lowercase(Locale.ROOT) })
    }
}

object TextExtractor {
    private val POS_REGEX = Regex("\\(\\s*(?:n|v|vt|vi|adj|adv|prep|conj|pron|interj|det|aux|abbr|pl|sing)\\.?\\s*\\)", RegexOption.IGNORE_CASE)
    private val SPLIT_REGEX = Regex("[\\s,;:|/\\\\()\\[\\]{}\"“”„«»<>!?•·—–=]+")
    private val EDGE_PUNCT = Regex("^['’`.\\-*_~]+|['’`.\\-*_~]+$")
    private val TURKISH_OR_DIGIT = Regex("[çğıİöşüÇĞÖŞÜ0-9]")
    private val PURE_ENGLISH = Regex("^[a-zA-Z]+(?:-[a-zA-Z]+)?$")

    private val STOP_WORDS = setOf(
        "a","an","the","and","or","but","if","then","else","when","at","by","for","with",
        "about","against","between","into","through","during","before","after","above",
        "below","to","from","up","down","in","out","on","off","over","under","again",
        "further","once","here","there","where","why","how","all","any","both","each",
        "few","more","most","other","some","such","no","nor","not","only","own","same",
        "so","than","too","very","can","will","just","should","now","is","am","are","was",
        "were","be","been","being","have","has","had","having","do","does","did","doing",
        "would","could","might","must","shall","may","this","that","these","those","it",
        "its","itself","i","me","my","myself","we","our","ours","ourselves","you","your",
        "yours","yourself","he","him","his","himself","she","her","hers","herself","they",
        "them","their","theirs","themselves","what","which","who","whom","whose","as","of",
        "because","while","until","also","even","still","well","back","much","many","one",
        "two","get","got","like","make","made","said","say","says","see","use","used"
    )

    fun extractEnglishWords(rawText: String, ignoreStopWords: Boolean, ignoreShort: Boolean): List<String> {
        if (rawText.isBlank()) return emptyList()
        val cleaned = POS_REGEX.replace(rawText, " ")
        val tokens = cleaned.split(SPLIT_REGEX)
        val out = ArrayList<String>()
        val seen = HashSet<String>()

        for (raw in tokens) {
            if (raw.isEmpty()) continue
            var token = EDGE_PUNCT.replace(raw, "")
            if (token.isEmpty()) continue
            if (token.endsWith("'s", true) || token.endsWith("’s", true)) {
                token = token.dropLast(2)
            }
            if (token.isEmpty()) continue
            if (TURKISH_OR_DIGIT.containsMatchIn(token)) continue
            if (token.any { it.code > 127 }) continue
            if (!PURE_ENGLISH.matches(token)) continue

            val lower = token.lowercase(Locale.ROOT)
            if (lower.length < 2) continue
            if (ignoreShort && lower.length < 3) continue
            if (ignoreStopWords && lower in STOP_WORDS) continue
            if (seen.add(lower)) out.add(lower)
        }
        return out
    }

    fun generateRootCandidates(word: String): List<String> {
        val set = LinkedHashSet<String>()
        fun add(w: String) { if (w.length >= 3 && w != word) set.add(w) }

        if (word.endsWith("ally") && word.length > 5) {
            add(word.dropLast(2)); add(word.dropLast(4)); add(word.dropLast(4) + "e")
        }
        if (word.endsWith("ily") && word.length > 4) add(word.dropLast(3) + "y")
        if (word.endsWith("bly") && word.length > 4) add(word.dropLast(1) + "e")
        if (word.endsWith("ly") && word.length > 4) {
            add(word.dropLast(2)); add(word.dropLast(2) + "e"); add(word.dropLast(1) + "e")
        }
        if (word.endsWith("ation") && word.length > 6) {
            add(word.dropLast(5) + "e"); add(word.dropLast(5))
        } else if (word.endsWith("tion") && word.length > 5) {
            add(word.dropLast(4) + "te"); add(word.dropLast(3))
        }
        if ((word.endsWith("able") || word.endsWith("ible")) && word.length > 5) {
            add(word.dropLast(4)); add(word.dropLast(4) + "e")
        }
        if (word.endsWith("iness") && word.length > 6) add(word.dropLast(5) + "y")
        else if (word.endsWith("ness") && word.length > 5) add(word.dropLast(4))
        if (word.endsWith("ment") && word.length > 5) {
            add(word.dropLast(4)); add(word.dropLast(4) + "e")
        }
        if (word.endsWith("ing") && word.length > 5) {
            val stem = word.dropLast(3)
            add(stem); add(stem + "e")
            if (stem.length >= 3 && stem[stem.length - 1] == stem[stem.length - 2]) add(stem.dropLast(1))
        }
        if (word.endsWith("ied") && word.length > 4) add(word.dropLast(3) + "y")
        else if (word.endsWith("ed") && word.length > 4) {
            val stem = word.dropLast(2)
            add(stem); add(stem + "e")
            if (stem.length >= 3 && stem[stem.length - 1] == stem[stem.length - 2]) add(stem.dropLast(1))
        }
        if (word.endsWith("ies") && word.length > 4) add(word.dropLast(3) + "y")
        else if (word.endsWith("es") && word.length > 4) {
            add(word.dropLast(2)); add(word.dropLast(1))
        } else if (word.endsWith("s") && !word.endsWith("ss") && word.length > 3) {
            add(word.dropLast(1))
        }
        if (word.startsWith("un") && word.length > 6) {
            val noUn = word.drop(2)
            add(noUn)
            if (noUn.endsWith("ly")) add(noUn.dropLast(2))
        }
        return set.take(7)
    }

    fun makeThirdPerson(base: String): String {
        val w = base.lowercase(Locale.ROOT)
        return when {
            w == "be" -> "is"
            w == "have" -> "has"
            w.endsWith("s") || w.endsWith("x") || w.endsWith("z") || w.endsWith("ch") || w.endsWith("sh") || w.endsWith("o") -> "${w}es"
            w.endsWith("y") && w.length > 1 && w[w.length - 2] !in "aeiou" -> w.dropLast(1) + "ies"
            else -> "${w}s"
        }
    }

    fun makeGerund(base: String): String {
        val w = base.lowercase(Locale.ROOT)
        if (w.length < 2) return "${w}ing"
        if (w == "be") return "being"
        if (w.endsWith("ie")) return w.dropLast(2) + "ying"
        if (w.endsWith("e") && !w.endsWith("ee") && !w.endsWith("ye") && !w.endsWith("oe")) {
            return w.dropLast(1) + "ing"
        }
        val last = w.last()
        val prev = w[w.length - 2]
        if (last !in "aeiouwxy" && prev in "aeiou") {
            val isQuCluster = w.length >= 4 && w[w.length - 3] == 'u' && w[w.length - 4] == 'q'
            val beforePrevIsVowel = w.length >= 3 && w[w.length - 3] in "aeiou" && !isQuCluster
            if (!beforePrevIsVowel) {
                val syllables = countVowelGroups(w)
                val prefixedCvc = listOf("be", "for", "up", "out", "over", "under", "mis", "un", "re", "sub", "ad", "in")
                    .any { p -> w.startsWith(p) && w.length - p.length in 3..4 && countVowelGroups(w.drop(p.length)) == 1 }
                if (syllables == 1 || prefixedCvc) {
                    return "$w${last}ing"
                }
            }
        }
        return "${w}ing"
    }

    private fun countVowelGroups(word: String): Int {
        var count = 0
        var inVowel = false
        for (c in word) {
            val v = c in "aeiou"
            if (v && !inVowel) count++
            inVowel = v
        }
        return count
    }
}
