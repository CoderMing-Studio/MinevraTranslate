package com.minevra.translate

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private lateinit var engine: BinaryDictEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        engine = BinaryDictEngine(applicationContext)
        engine.initAndEnsureStarterBinary()
        tts = TextToSpeech(this, this)

        setContent {
            MinevraTranslateApp(
                engine = engine,
                onSpeak = { word -> speakWord(word) }
            )
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            ttsReady = true
        }
    }

    private fun speakWord(word: String) {
        if (ttsReady && word.isNotBlank()) {
            tts?.speak(word, TextToSpeech.QUEUE_FLUSH, null, "minevra_tts")
        } else {
            Toast.makeText(this, "Ses motoru hazırlanıyor...", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}

private val LightColors = lightColorScheme(
    primary = Color(0xFF0F766E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCCFBF1),
    onPrimaryContainer = Color(0xFF134E4A),
    secondary = Color(0xFF4F46E5),
    background = Color(0xFFF8FAFC),
    surface = Color.White,
    surfaceVariant = Color(0xFFF1F5F9)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF2DD4BF),
    onPrimary = Color(0xFF042F2E),
    primaryContainer = Color(0xFF115E59),
    onPrimaryContainer = Color(0xFFCCFBF1),
    secondary = Color(0xFF818CF8),
    background = Color(0xFF090E1A),
    surface = Color(0xFF111827),
    surfaceVariant = Color(0xFF1E293B)
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MinevraTranslateApp(
    engine: BinaryDictEngine,
    onSpeak: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val prefs = remember { context.getSharedPreferences("minevra_prefs", Context.MODE_PRIVATE) }

    var isDark by remember { mutableStateOf(prefs.getBoolean("dark_mode", false)) }
    var activeTab by remember { mutableStateOf(MainTab.SEARCH) }
    var dictMode by remember { mutableStateOf(DictMode.EN_TR) }
    var personalStore by remember { mutableStateOf(PersonalStoreRepo.load(context)) }
    var downloadProgress by remember {
        mutableStateOf(DownloadProgress(totalIndexedWords = engine.totalOfflineWords()))
    }
    var downloadJob by remember { mutableStateOf<Job?>(null) }

    // Split-pane arama durumu
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<BinarySearchResult>>(emptyList()) }
    var lastSearchMicros by remember { mutableLongStateOf(0L) }
    var selectedRecord by remember { mutableStateOf<DictRecord?>(null) }
    var selectedKey by remember { mutableStateOf("") }
    val navStack = remember { mutableStateListOf<String>() }
    var isEnrichingWord by remember { mutableStateOf(false) }
    var showTagDialogForWord by remember { mutableStateOf<String?>(null) }
    var showSettingsModal by remember { mutableStateOf(false) }

    // Pending toplu online zenginleştirme
    var isFetchingPending by remember { mutableStateOf(false) }
    var pendingProgressText by remember { mutableStateOf("") }
    var pendingJob by remember { mutableStateOf<Job?>(null) }

    fun updateStore(newStore: PersonalStore) {
        personalStore = newStore
        scope.launch(Dispatchers.IO) { PersonalStoreRepo.save(context, newStore) }
    }

    fun startBinaryDownload() {
        if (downloadProgress.running) return
        downloadJob = scope.launch {
            engine.downloadAndBuildBinaryDatabase { p ->
                downloadProgress = p
            }
            // İndirme bitince mevcut aramayı hemen yenile
            val t0 = System.nanoTime()
            val res = engine.search(dictMode, searchQuery, 65)
            lastSearchMicros = (System.nanoTime() - t0) / 1000L
            searchResults = res
            if (res.isNotEmpty()) {
                selectedKey = res.first().key
                selectedRecord = engine.readRecord(dictMode, res.first().dataOffset)
            }
            if (downloadProgress.completedBanner) {
                delay(3000)
                downloadProgress = downloadProgress.copy(completedBanner = false)
            }
        }
    }

    // İlk açılışta tam ikili paket yoksa arka planda otomatik indir
    LaunchedEffect(Unit) {
        if (!engine.isFullPackReady()) {
            startBinaryDownload()
        }
    }

    // Anlık (<1 ms) Binary Search tetikleyicisi
    LaunchedEffect(dictMode, searchQuery, downloadProgress.totalIndexedWords) {
        val t0 = System.nanoTime()
        val list = engine.search(dictMode, searchQuery, 65)
        lastSearchMicros = (System.nanoTime() - t0) / 1000L
        searchResults = list
        if (list.isNotEmpty()) {
            val first = list.first()
            selectedKey = first.key
            selectedRecord = engine.readRecord(dictMode, first.dataOffset)
        } else if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim().lowercase(Locale.ROOT)
            selectedKey = q
            selectedRecord = personalStore.words[q]?.let { saved ->
                DictRecord(
                    word = saved.word,
                    turkish = saved.turkish,
                    english = saved.meanings.joinToString("\n") { m ->
                        "[${m.partOfSpeech}] " + m.definitions.joinToString("; ") { it.definition }
                    },
                    synonyms = saved.meanings.flatMap { it.synonyms }.distinct(),
                    phonetic = saved.phonetic,
                    rootWord = saved.rootWord,
                    source = saved.source
                )
            }
        } else {
            selectedRecord = null
            selectedKey = ""
        }
    }

    fun selectWordDirectly(word: String, pushToStack: Boolean = true) {
        val clean = word.trim().lowercase(Locale.ROOT)
        if (clean.isEmpty()) return
        if (pushToStack && selectedKey.isNotEmpty() && selectedKey != clean) {
            navStack.add(selectedKey)
        }
        activeTab = MainTab.SEARCH
        searchQuery = clean
        selectedKey = clean
        val offline = engine.findExactAny(clean)
        if (offline != null) {
            selectedRecord = offline
        } else {
            // Online 5-katmanlı arama başlat
            isEnrichingWord = true
            scope.launch {
                val online = engine.enrichOnline(clean)
                isEnrichingWord = false
                if (online != null) {
                    val prev = personalStore.words[clean]
                    val merged = online.copy(
                        favorite = prev?.favorite ?: false,
                        tags = prev?.tags ?: emptyList(),
                        addedAt = prev?.addedAt ?: System.currentTimeMillis(),
                        lastViewedAt = System.currentTimeMillis()
                    )
                    updateStore(personalStore.copy(words = personalStore.words + (clean to merged)))
                    selectedRecord = DictRecord(
                        word = merged.word,
                        turkish = merged.turkish,
                        english = merged.meanings.joinToString("\n\n") { m ->
                            "[${m.partOfSpeech}]\n" + m.definitions.mapIndexed { idx, d ->
                                "${idx + 1}. ${d.definition}" + if (d.example.isNotEmpty()) "\n   “${d.example}”" else ""
                            }.joinToString("\n")
                        },
                        synonyms = merged.meanings.flatMap { it.synonyms }.distinct(),
                        phonetic = merged.phonetic,
                        rootWord = merged.rootWord,
                        source = merged.source
                    )
                }
            }
        }
    }

    fun ensureSavedAndModify(word: String, modifier: (SavedWordEntry) -> SavedWordEntry) {
        val clean = word.trim().lowercase(Locale.ROOT)
        if (clean.isEmpty()) return
        val existing = personalStore.words[clean]
        val base = existing ?: run {
            val rec = selectedRecord?.takeIf { it.word.equals(clean, true) } ?: engine.findExactAny(clean)
            val meanings = if (!rec?.english.isNullOrBlank()) {
                listOf(MeaningGroup(rec!!.pos.ifEmpty { "other" }, listOf(DefinitionItem(rec.english)), rec.synonyms))
            } else emptyList()
            SavedWordEntry(
                word = clean,
                phonetic = rec?.phonetic.orEmpty(),
                turkish = rec?.turkish.orEmpty(),
                source = rec?.source ?: "Offline Binary",
                meanings = meanings,
                needsFetch = meanings.isEmpty() && rec?.turkish.isNullOrBlank()
            )
        }
        val updated = modifier(base.copy(lastViewedAt = System.currentTimeMillis()))
        val newTags = (personalStore.customTags + updated.tags).distinct().sortedBy { it.lowercase(Locale.ROOT) }
        updateStore(personalStore.copy(words = personalStore.words + (clean to updated), customTags = newTags))
    }

    fun enrichSingleWordOnline(word: String) {
        val clean = word.trim().lowercase(Locale.ROOT)
        if (clean.isEmpty()) return
        isEnrichingWord = true
        scope.launch {
            val online = engine.enrichOnline(clean)
            isEnrichingWord = false
            if (online != null) {
                ensureSavedAndModify(clean) { cur ->
                    cur.copy(
                        phonetic = online.phonetic.ifEmpty { cur.phonetic },
                        turkish = online.turkish.ifEmpty { cur.turkish },
                        source = online.source,
                        rootWord = online.rootWord,
                        meanings = online.meanings.ifEmpty { cur.meanings },
                        needsFetch = false,
                        enriched = true
                    )
                }
                Toast.makeText(context, "Online detaylar & eş anlamlılar güncellendi", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Online kaynakta bulunamadı veya internet yok", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun runPendingOnlineBatch() {
        if (isFetchingPending) {
            pendingJob?.cancel()
            isFetchingPending = false
            return
        }
        val pending = personalStore.words.values.filter { it.needsFetch }
        if (pending.isEmpty()) {
            Toast.makeText(context, "Bekleyen kelime yok", Toast.LENGTH_SHORT).show()
            return
        }
        isFetchingPending = true
        pendingJob = scope.launch {
            var done = 0
            for (item in pending) {
                pendingProgressText = "Online çekiliyor: ${done + 1}/${pending.size} (${item.word})"
                val fetched = engine.enrichOnline(item.word)
                if (fetched != null) {
                    val cur = personalStore.words[item.word]
                    if (cur != null) {
                        val updated = cur.copy(
                            phonetic = fetched.phonetic,
                            turkish = fetched.turkish.ifEmpty { cur.turkish },
                            source = fetched.source,
                            rootWord = fetched.rootWord,
                            meanings = fetched.meanings,
                            needsFetch = false,
                            enriched = true
                        )
                        updateStore(personalStore.copy(words = personalStore.words + (item.word to updated)))
                    }
                }
                done++
            }
            isFetchingPending = false
            pendingProgressText = ""
            Toast.makeText(context, "$done bekleyen kelime işlendi", Toast.LENGTH_SHORT).show()
        }
    }

    // Yedekleme Dosya Seçicileri (SAF)
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use {
                    it.write(PersonalStoreRepo.toJson(personalStore).toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(context, "Yedek dışa aktarıldı", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, "Dışa aktarma hatası", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val importBackupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
                val imported = PersonalStoreRepo.fromJson(text)
                val mergedWords = personalStore.words + imported.words
                val mergedTags = (personalStore.customTags + imported.customTags).distinct().sortedBy { it.lowercase(Locale.ROOT) }
                updateStore(PersonalStore(mergedWords, mergedTags))
                Toast.makeText(context, "${imported.words.size} kelime içe aktarıldı", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, "Geçersiz JSON yedek dosyası", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val drawerState = rememberDrawerState(DrawerValue.Closed)

    MaterialTheme(colorScheme = if (isDark) DarkColors else LightColors) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(modifier = Modifier.width(280.dp)) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Minevra Translate", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                        Text(
                            "${engine.totalOfflineWords()} ikili offline kelime",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))
                    DictMode.entries.forEach { mode ->
                        NavigationDrawerItem(
                            label = { Text(mode.title, fontWeight = FontWeight.SemiBold) },
                            selected = dictMode == mode && activeTab == MainTab.SEARCH,
                            onClick = {
                                dictMode = mode
                                activeTab = MainTab.SEARCH
                                searchQuery = ""
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider()
                    NavigationDrawerItem(
                        label = { Text("Ayarlar ve Yedekleme") },
                        icon = { Icon(Icons.Default.Settings, null) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            showSettingsModal = true
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        ) {
            Scaffold(
                topBar = {
                    Column {
                        TopAppBar(
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                                navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                                actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            navigationIcon = {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(Icons.Default.Menu, contentDescription = "Menü")
                                }
                            },
                            title = {
                                Column {
                                    Text(
                                        text = when (activeTab) {
                                            MainTab.SEARCH -> dictMode.title
                                            MainTab.DICTIONARY -> "Sözlüğüm (${personalStore.words.size})"
                                            MainTab.BULK -> "Toplu Kelime Ekle"
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Minevra Translate",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                                    )
                                }
                            },
                            actions = {
                                if (activeTab == MainTab.SEARCH && (dictMode == DictMode.EN_TR || dictMode == DictMode.TR_EN)) {
                                    IconButton(onClick = {
                                        dictMode = if (dictMode == DictMode.EN_TR) DictMode.TR_EN else DictMode.EN_TR
                                        searchQuery = ""
                                    }) {
                                        Icon(Icons.Default.SwapHoriz, contentDescription = "Yön Değiştir")
                                    }
                                }
                                IconButton(onClick = { showSettingsModal = true }) {
                                    Icon(Icons.Default.Settings, contentDescription = "Ayarlar")
                                }
                                IconButton(onClick = {
                                    isDark = !isDark
                                    prefs.edit().putBoolean("dark_mode", isDark).apply()
                                }) {
                                    Icon(if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode, contentDescription = "Tema")
                                }
                            }
                        )

                        // İlk Kurulum / İkili Veritabanı İndirme İlerleme Çubuğu
                        if (downloadProgress.running || downloadProgress.error != null || downloadProgress.completedBanner) {
                            Surface(
                                color = when {
                                    downloadProgress.completedBanner -> Color(0xFF065F46)
                                    downloadProgress.error != null -> Color(0xFF92400E)
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = downloadProgress.error ?: "${downloadProgress.stage} (${downloadProgress.percent}%)",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (downloadProgress.completedBanner || downloadProgress.error != null) Color.White else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (downloadProgress.error != null) {
                                            TextButton(onClick = { startBinaryDownload() }) {
                                                Text("Tekrar", color = Color.White, fontSize = 12.sp)
                                            }
                                        } else if (downloadProgress.running) {
                                            IconButton(
                                                onClick = {
                                                    downloadJob?.cancel()
                                                    downloadProgress = DownloadProgress(totalIndexedWords = engine.totalOfflineWords())
                                                },
                                                modifier = Modifier.size(26.dp)
                                            ) {
                                                Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                    if (downloadProgress.running) {
                                        LinearProgressIndicator(
                                            progress = { downloadProgress.percent / 100f },
                                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                bottomBar = {
                    NavigationBar {
                        NavigationBarItem(
                            selected = activeTab == MainTab.SEARCH,
                            onClick = { activeTab = MainTab.SEARCH },
                            icon = { Icon(Icons.Default.Search, null) },
                            label = { Text("Sözlük") }
                        )
                        NavigationBarItem(
                            selected = activeTab == MainTab.DICTIONARY,
                            onClick = { activeTab = MainTab.DICTIONARY },
                            icon = {
                                BadgedBox(badge = {
                                    if (personalStore.words.isNotEmpty()) {
                                        Badge { Text(if (personalStore.words.size > 999) "999+" else "${personalStore.words.size}") }
                                    }
                                }) {
                                    Icon(Icons.Default.LibraryBooks, null)
                                }
                            },
                            label = { Text("Sözlüğüm") }
                        )
                        NavigationBarItem(
                            selected = activeTab == MainTab.BULK,
                            onClick = { activeTab = MainTab.BULK },
                            icon = { Icon(Icons.Default.PostAdd, null) },
                            label = { Text("Toplu Ekle") }
                        )
                    }
                }
            ) { innerPadding ->
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                    when (activeTab) {
                        MainTab.SEARCH -> SplitSearchScreen(
                            dictMode = dictMode,
                            onModeChange = { dictMode = it; searchQuery = "" },
                            searchQuery = searchQuery,
                            onQueryChange = { searchQuery = it },
                            searchResults = searchResults,
                            lastSearchMicros = lastSearchMicros,
                            selectedKey = selectedKey,
                            selectedRecord = selectedRecord,
                            savedEntry = personalStore.words[selectedKey],
                            hasBackStack = navStack.isNotEmpty(),
                            isEnriching = isEnrichingWord,
                            onBackStackPop = {
                                if (navStack.isNotEmpty()) {
                                    val prev = navStack.removeAt(navStack.lastIndex)
                                    selectWordDirectly(prev, pushToStack = false)
                                }
                            },
                            onSelectResult = { res ->
                                selectedKey = res.key
                                selectedRecord = engine.readRecord(dictMode, res.dataOffset)
                            },
                            onSpeak = onSpeak,
                            onToggleFavorite = { word ->
                                ensureSavedAndModify(word) { it.copy(favorite = !it.favorite) }
                            },
                            onOpenTags = { word ->
                                ensureSavedAndModify(word) { it }
                                showTagDialogForWord = word.lowercase(Locale.ROOT)
                            },
                            onOnlineEnrich = { word -> enrichSingleWordOnline(word) },
                            onTapSynonymOrRoot = { syn -> selectWordDirectly(syn, pushToStack = true) }
                        )

                        MainTab.DICTIONARY -> MyDictionaryScreen(
                            store = personalStore,
                            isFetchingPending = isFetchingPending,
                            pendingProgressText = pendingProgressText,
                            onFetchPending = { runPendingOnlineBatch() },
                            onOpenWord = { w -> selectWordDirectly(w, pushToStack = false) },
                            onToggleFav = { w -> ensureSavedAndModify(w) { it.copy(favorite = !it.favorite) } },
                            onDeleteWord = { w ->
                                updateStore(personalStore.copy(words = personalStore.words - w))
                            },
                            onDeleteGlobalTag = { tag ->
                                val cleanedWords = personalStore.words.mapValues { (_, e) ->
                                    e.copy(tags = e.tags.filterNot { it == tag })
                                }
                                updateStore(PersonalStore(cleanedWords, personalStore.customTags.filterNot { it == tag }))
                            }
                        )

                        MainTab.BULK -> BulkAddScreen(
                            store = personalStore,
                            engine = engine,
                            isFetchingPending = isFetchingPending,
                            onFetchPending = { runPendingOnlineBatch() },
                            onCommitBulk = { words, bulkTags ->
                                val now = System.currentTimeMillis()
                                val hasTags = bulkTags.isNotEmpty()
                                val mutableWords = personalStore.words.toMutableMap()
                                var addedNew = 0
                                var offlinePop = 0
                                var pendingCount = 0
                                var taggedExisting = 0
                                var skippedExisting = 0

                                words.forEachIndexed { idx, w ->
                                    val existing = mutableWords[w]
                                    if (existing != null) {
                                        if (hasTags) {
                                            val mergedTags = (existing.tags + bulkTags).distinct()
                                            if (mergedTags.size > existing.tags.size) {
                                                mutableWords[w] = existing.copy(tags = mergedTags)
                                                taggedExisting++
                                            } else skippedExisting++
                                        } else {
                                            skippedExisting++
                                        }
                                    } else {
                                        val off = engine.findExactAny(w)
                                        val hasOff = off != null && (off.turkish.isNotBlank() || off.english.isNotBlank())
                                        val meanings = if (off != null && off.english.isNotBlank()) {
                                            listOf(MeaningGroup(off.pos.ifEmpty { "other" }, listOf(DefinitionItem(off.english))))
                                        } else emptyList()
                                        mutableWords[w] = SavedWordEntry(
                                            word = w,
                                            turkish = off?.turkish.orEmpty(),
                                            source = if (hasOff) "Offline Binary" else "Pending Online",
                                            meanings = meanings,
                                            needsFetch = !hasOff,
                                            enriched = false,
                                            favorite = false,
                                            tags = bulkTags,
                                            addedAt = now - idx,
                                            lastViewedAt = now - idx
                                        )
                                        addedNew++
                                        if (hasOff) offlinePop++ else pendingCount++
                                    }
                                }
                                val mergedGlobalTags = (personalStore.customTags + bulkTags).distinct().sortedBy { it.lowercase(Locale.ROOT) }
                                updateStore(PersonalStore(mutableWords, mergedGlobalTags))
                                BulkCommitSummary(addedNew, offlinePop, pendingCount, taggedExisting, skippedExisting)
                            },
                            onGoToDictionary = { activeTab = MainTab.DICTIONARY }
                        )
                    }
                }
            }
        }

        // Etiket Ekle / Yönet Dialogu
        showTagDialogForWord?.let { targetWord ->
            val entry = personalStore.words[targetWord]
            var newTagText by remember(targetWord) { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showTagDialogForWord = null },
                title = { Text("#$targetWord Etiketleri", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = newTagText,
                                onValueChange = { newTagText = it },
                                placeholder = { Text("Yeni etiket (ör. YDS, B1)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Button(onClick = {
                                val clean = newTagText.trim().removePrefix("#").take(28)
                                if (clean.isNotEmpty()) {
                                    ensureSavedAndModify(targetWord) { e ->
                                        e.copy(tags = (e.tags + clean).distinct())
                                    }
                                    newTagText = ""
                                }
                            }) { Text("Ekle") }
                        }
                        if (personalStore.customTags.isNotEmpty()) {
                            Text("Mevcut etiketlere dokun:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                personalStore.customTags.forEach { tag ->
                                    val assigned = entry?.tags?.contains(tag) == true
                                    FilterChip(
                                        selected = assigned,
                                        onClick = {
                                            ensureSavedAndModify(targetWord) { e ->
                                                val next = if (tag in e.tags) e.tags - tag else e.tags + tag
                                                e.copy(tags = next)
                                            }
                                        },
                                        label = { Text("#$tag") },
                                        leadingIcon = if (assigned) { { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) } } else null
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showTagDialogForWord = null }) { Text("Tamam") }
                }
            )
        }

        // Ayarlar & Yedekleme Modalı
        if (showSettingsModal) {
            AlertDialog(
                onDismissRequest = { showSettingsModal = false },
                title = { Text("Minevra Translate — Ayarlar", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            StatChip("Kayıtlı", "${personalStore.words.size}")
                            StatChip("Favori", "${personalStore.words.values.count { it.favorite }}")
                            StatChip("Offline Bin", "${engine.totalOfflineWords()}")
                        }
                        HorizontalDivider()
                        Button(
                            onClick = {
                                showSettingsModal = false
                                startBinaryDownload()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Download, null)
                            Spacer(Modifier.width(8.dp))
                            Text("İkili Offline Sözlüğü İndir / Güncelle")
                        }
                        OutlinedButton(
                            onClick = { exportLauncher.launch("minevra-backup.json") },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Yedeği Dışa Aktar (.json)") }
                        OutlinedButton(
                            onClick = { importBackupLauncher.launch(arrayOf("application/json", "*/*")) },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Yedeği İçe Aktar (.json)") }
                        OutlinedButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(PersonalStoreRepo.toJson(personalStore)))
                                Toast.makeText(context, "JSON yedek panoya kopyalandı", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Yedeği Panoya Kopyala") }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSettingsModal = false }) { Text("Kapat") }
                }
            )
        }
    }
}

@Composable
private fun StatChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * EKRAN 1: Fotoğraflardaki gibi Anlık Bölünmüş Sözlük (Sol: Benzer/Ön-ek Liste, Sağ: Anlık Detay)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SplitSearchScreen(
    dictMode: DictMode,
    onModeChange: (DictMode) -> Unit,
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    searchResults: List<BinarySearchResult>,
    lastSearchMicros: Long,
    selectedKey: String,
    selectedRecord: DictRecord?,
    savedEntry: SavedWordEntry?,
    hasBackStack: Boolean,
    isEnriching: Boolean,
    onBackStackPop: () -> Unit,
    onSelectResult: (BinarySearchResult) -> Unit,
    onSpeak: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onOpenTags: (String) -> Unit,
    onOnlineEnrich: (String) -> Unit,
    onTapSynonymOrRoot: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Hızlı Mod Şeridi (EN⇄TR | TR→EN | Phrasal | Irregular)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                DictMode.EN_TR to "EN⇄TR",
                DictMode.TR_EN to "TR→EN",
                DictMode.PHRASAL to "Phrasal",
                DictMode.IRREGULAR to "Irregular"
            ).forEach { (mode, label) ->
                val selected = dictMode == mode
                Surface(
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).clickable { onModeChange(mode) }
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 7.dp)) {
                        Text(
                            label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Üst Arama Çubuğu
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            if (hasBackStack) {
                IconButton(onClick = onBackStackPop, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Önceki Kelime")
                }
            }
            Text(
                text = "${dictMode.searchPrefix} : ",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Box(modifier = Modifier.weight(1f)) {
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                )
                if (searchQuery.isEmpty()) {
                    Text(
                        "Kelime yazın (${String.format(Locale.US, "%.2f", lastSearchMicros / 1000.0)} ms)",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            }
            if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Temizle")
                }
            }
        }
        HorizontalDivider()

        // BÖLÜNMÜŞ EKRAN: Sol (%38) Benzer Kelimeler | Sağ (%62) Anlık Anlam & Detaylar
        Row(modifier = Modifier.fillMaxSize()) {
            // SOL PANEL: Alfabetik İkili Arama Sonuçları
            LazyColumn(
                modifier = Modifier
                    .weight(0.38f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            ) {
                items(searchResults, key = { "${it.key}_${it.dataOffset}" }) { item ->
                    val isSelected = item.key.equals(selectedKey, ignoreCase = true)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                            .clickable { onSelectResult(item) }
                    ) {
                        Text(
                            text = if (dictMode == DictMode.IRREGULAR) item.key.replaceFirstChar { it.uppercaseChar() } else item.key,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)
                        )
                        HorizontalDivider(thickness = 0.5.dp)
                    }
                }
            }

            VerticalDivider(thickness = 1.dp)

            // SAĞ PANEL: Seçili Kelime Detayı
            Column(
                modifier = Modifier
                    .weight(0.62f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surface)
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val activeWord = selectedRecord?.word?.ifEmpty { selectedKey } ?: selectedKey
                if (activeWord.isBlank()) {
                    Text(
                        "Sol listeden bir kelime seçin veya yukarıdan anlık arama yapın.\n\nİkili (Binary) arama motoru 100.000+ kelimede milisaniyeler içinde sonuç getirir.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                } else {
                    // Başlık + Hoparlör
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = activeWord,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val phonetic = savedEntry?.phonetic?.ifEmpty { selectedRecord?.phonetic.orEmpty() }.orEmpty()
                            if (phonetic.isNotEmpty()) {
                                Text(phonetic, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        IconButton(onClick = { onSpeak(activeWord) }) {
                            Icon(Icons.Default.VolumeUp, contentDescription = "Seslendir", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    // Aksiyon Çubuğu (Favori, Etiket, Online Zenginleştir)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isFav = savedEntry?.favorite == true
                        Surface(
                            color = if (isFav) Color(0xFFFEF3C7) else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.clickable { onToggleFavorite(activeWord) }
                        ) {
                            Row(Modifier.padding(horizontal = 8.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (isFav) Icons.Default.Star else Icons.Default.StarBorder,
                                    null,
                                    tint = if (isFav) Color(0xFFD97706) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("Favori", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        val tagCount = savedEntry?.tags?.size ?: 0
                        Surface(
                            color = if (tagCount > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.clickable { onOpenTags(activeWord) }
                        ) {
                            Row(Modifier.padding(horizontal = 8.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Label, null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(if (tagCount > 0) "Etiket ($tagCount)" else "Etiket", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.clickable { onOnlineEnrich(activeWord) }
                        ) {
                            Row(Modifier.padding(horizontal = 8.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (isEnriching) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(15.dp))
                                }
                                Spacer(Modifier.width(4.dp))
                                Text("Online", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Atanmış Etiketler
                    if (!savedEntry?.tags.isNullOrEmpty()) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            savedEntry!!.tags.forEach { t ->
                                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(6.dp)) {
                                    Text("#$t", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp))
                                }
                            }
                        }
                    }

                    HorizontalDivider()

                    // Düzensiz Fiil 4 Çekim Tablosu
                    if (selectedRecord != null && (selectedRecord.pastSimple.isNotEmpty() || selectedRecord.pastParticiple.isNotEmpty())) {
                        IrregularFormRow("Past Simple", selectedRecord.pastSimple)
                        IrregularFormRow("Past Participle", selectedRecord.pastParticiple)
                        if (selectedRecord.thirdPerson.isNotEmpty()) IrregularFormRow("3rd Person Singular", selectedRecord.thirdPerson)
                        if (selectedRecord.gerund.isNotEmpty()) IrregularFormRow("Present Participle Gerund", selectedRecord.gerund)
                        HorizontalDivider()
                    }

                    // Kelime Türü ([Fiil], [İsim] vb.)
                    val posText = selectedRecord?.pos.orEmpty()
                    if (posText.isNotEmpty()) {
                        Text(
                            text = "[$posText]",
                            color = Color(0xFF059669),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    // Türkçe Karşılıklar
                    val trText = savedEntry?.turkish?.ifEmpty { selectedRecord?.turkish.orEmpty() }
                        ?: selectedRecord?.turkish.orEmpty()
                    if (trText.isNotEmpty()) {
                        Text(
                            text = trText,
                            fontSize = 16.sp,
                            lineHeight = 23.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Kök Kelime Uyarısı (Stemming)
                    val rootW = savedEntry?.rootWord?.ifEmpty { selectedRecord?.rootWord.orEmpty() }.orEmpty()
                    if (rootW.isNotEmpty()) {
                        Surface(
                            color = Color(0xFFFEF3C7),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().clickable { onTapSynonymOrRoot(rootW) }
                        ) {
                            Text(
                                "Kök kelime: $rootW → açmak için dokun",
                                color = Color(0xFF92400E),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    // Online Zenginleştirilmiş İngilizce Detaylar
                    if (savedEntry != null && savedEntry.enriched && savedEntry.meanings.isNotEmpty()) {
                        savedEntry.meanings.forEach { m ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(m.partOfSpeech, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                                    m.definitions.forEachIndexed { i, d ->
                                        Text("${i + 1}. ${d.definition}", fontSize = 13.5.sp)
                                        if (d.example.isNotEmpty()) {
                                            Text("“${d.example}”", fontSize = 12.5.sp, fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    if (m.synonyms.isNotEmpty()) {
                                        Text("SYNONYMS", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            m.synonyms.forEach { syn ->
                                                Surface(
                                                    color = MaterialTheme.colorScheme.primaryContainer,
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.clickable { onTapSynonymOrRoot(syn) }
                                                ) {
                                                    Text(syn, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else if (!selectedRecord?.english.isNullOrBlank()) {
                        // Offline Binary İngilizce Tanım
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("English Definition (Offline)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text(selectedRecord!!.english, fontSize = 13.5.sp, lineHeight = 19.sp)
                                selectedRecord.examples.forEach { ex ->
                                    Text("“$ex”", fontSize = 12.5.sp, fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    } else if (trText.isEmpty()) {
                        Text(
                            "Offline pakette bulunamadı. Yukarıdaki 'Online' butonuna basarak 5 katmanlı sözlükten çekebilirsiniz.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IrregularFormRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(label, fontStyle = FontStyle.Italic, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value.replaceFirstChar { it.uppercaseChar() }, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

/**
 * EKRAN 2: Sözlüğüm (Tümü, Favoriler, Etiket Grupları, Sıralama, Filtreleme, Pending Fetcher)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MyDictionaryScreen(
    store: PersonalStore,
    isFetchingPending: Boolean,
    pendingProgressText: String,
    onFetchPending: () -> Unit,
    onOpenWord: (String) -> Unit,
    onToggleFav: (String) -> Unit,
    onDeleteWord: (String) -> Unit,
    onDeleteGlobalTag: (String) -> Unit
) {
    var subTab by remember { mutableStateOf(DictSubTab.ALL) }
    var filterText by remember { mutableStateOf("") }
    var sortAlpha by remember { mutableStateOf(false) }
    var selectedTag by remember { mutableStateOf("__ALL__") }
    var managingTags by remember { mutableStateOf(false) }

    val allWords = store.words.values.toList()
    val pendingCount = allWords.count { it.needsFetch }
    val q = filterText.trim().lowercase(Locale.ROOT)

    val filtered = allWords.filter { e ->
        val tabOk = when (subTab) {
            DictSubTab.ALL -> true
            DictSubTab.FAVORITES -> e.favorite
            DictSubTab.TAGS -> if (selectedTag == "__ALL__") e.tags.isNotEmpty() else selectedTag in e.tags
        }
        val queryOk = q.isEmpty() || e.word.contains(q) || e.turkish.lowercase(Locale.ROOT).contains(q) || e.tags.any { it.lowercase(Locale.ROOT).contains(q) }
        tabOk && queryOk
    }.let { list ->
        if (sortAlpha) list.sortedBy { it.word } else list.sortedByDescending { it.lastViewedAt }
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Alt Sekmeler
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            listOf(
                DictSubTab.ALL to "Tümü (${allWords.size})",
                DictSubTab.FAVORITES to "Favori (${allWords.count { it.favorite }})",
                DictSubTab.TAGS to "Etiket (${store.customTags.size})"
            ).forEach { (tab, label) ->
                val active = subTab == tab
                Surface(
                    color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).clickable { subTab = tab; managingTags = false }
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 9.dp)) {
                        Text(
                            label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Bekleyen Kelimeler Online Çekim Bandı
        if (pendingCount > 0 || isFetchingPending) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        if (isFetchingPending) pendingProgressText else "$pendingCount kelime online detay bekliyor",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Button(onClick = onFetchPending, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                        Text(if (isFetchingPending) "Durdur" else "Online Çek", fontSize = 12.sp)
                    }
                }
            }
        }

        // Filtre ve Sıralama Çubuğu
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = filterText,
                onValueChange = { filterText = it },
                placeholder = { Text("Kelime veya etiket filtrele...") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedButton(onClick = { sortAlpha = !sortAlpha }, modifier = Modifier.height(54.dp)) {
                Icon(Icons.Default.SortByAlpha, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(if (sortAlpha) "A-Z" else "Son", fontSize = 12.sp)
            }
        }

        // Etiket Gezgini ve Yönetimi
        if (subTab == DictSubTab.TAGS) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Etiket Grupları", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        TextButton(onClick = { managingTags = !managingTags }) {
                            Text(if (managingTags) "Bitti" else "Etiketleri Yönet", fontSize = 12.sp)
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (!managingTags) {
                            FilterChip(selected = selectedTag == "__ALL__", onClick = { selectedTag = "__ALL__" }, label = { Text("Tüm Etiketli") })
                        }
                        store.customTags.forEach { tag ->
                            if (managingTags) {
                                Surface(color = Color(0xFFFEE2E2), shape = RoundedCornerShape(10.dp)) {
                                    Row(
                                        Modifier.clickable { onDeleteGlobalTag(tag); if (selectedTag == tag) selectedTag = "__ALL__" }.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("#$tag", color = Color(0xFF991B1B), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Spacer(Modifier.width(6.dp))
                                        Text("✕", color = Color(0xFFDC2626), fontWeight = FontWeight.ExtraBold)
                                    }
                                }
                            } else {
                                FilterChip(selected = selectedTag == tag, onClick = { selectedTag = tag }, label = { Text("#$tag") })
                            }
                        }
                    }
                }
            }
        }

        // Kayıtlı Kelime Kartları
        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Kayıtlı kelime bulunamadı.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                items(filtered, key = { it.word }) { entry ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onOpenWord(entry.word) },
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(entry.word, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                                    if (entry.needsFetch) {
                                        Surface(color = Color(0xFFFEF3C7), shape = RoundedCornerShape(6.dp)) {
                                            Text("Pending", fontSize = 10.sp, color = Color(0xFF92400E), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                        }
                                    }
                                }
                                val preview = entry.turkish.ifEmpty { entry.meanings.firstOrNull()?.definitions?.firstOrNull()?.definition.orEmpty() }
                                if (preview.isNotEmpty()) {
                                    Text(preview, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (entry.tags.isNotEmpty()) {
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        entry.tags.forEach { t ->
                                            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(6.dp)) {
                                                Text("#$t", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                            }
                                        }
                                    }
                                }
                            }
                            IconButton(onClick = { onToggleFav(entry.word) }) {
                                Icon(
                                    if (entry.favorite) Icons.Default.Star else Icons.Default.StarBorder,
                                    null,
                                    tint = if (entry.favorite) Color(0xFFD97706) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { onDeleteWord(entry.word) }) {
                                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * EKRAN 3: Toplu Ekle (Word List & Text/Article, Dosya Yükleme, Toplu Etiketleme, Birebir Kopya Kuralı)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BulkAddScreen(
    store: PersonalStore,
    engine: BinaryDictEngine,
    isFetchingPending: Boolean,
    onFetchPending: () -> Unit,
    onCommitBulk: (List<String>, List<String>) -> BulkCommitSummary,
    onGoToDictionary: () -> Unit
) {
    val context = LocalContext.current
    var isTextMode by remember { mutableStateOf(false) }
    var rawText by remember { mutableStateOf("") }
    var ignoreStopWords by remember { mutableStateOf(true) }
    var ignoreShort by remember { mutableStateOf(false) }
    var tagInput by remember { mutableStateOf("") }
    var bulkTags by remember { mutableStateOf<List<String>>(emptyList()) }
    var previewWords by remember { mutableStateOf<List<String>>(emptyList()) }
    var lastSummary by remember { mutableStateOf<BulkCommitSummary?>(null) }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                rawText = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
                Toast.makeText(context, "Dosya yüklendi", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, "Dosya okunamadı", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Mod Değiştirici
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            listOf(false to "Kelime Listesi", true to "Metin / Makale").forEach { (tm, label) ->
                val active = isTextMode == tm
                Surface(
                    color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).clickable {
                        isTextMode = tm
                        ignoreShort = tm
                    }
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 10.dp)) {
                        Text(label, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }

        // Metin Girişi & Dosya Yükle
        Card {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(if (isTextMode) "İngilizce Metin / Makale Yapıştır" else "Kelime Listesi Yapıştır", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = { filePicker.launch(arrayOf("text/*", "*/*")) }, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                            Icon(Icons.Default.UploadFile, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Dosya", fontSize = 12.sp)
                        }
                        TextButton(onClick = { rawText = ""; previewWords = emptyList(); lastSummary = null }) { Text("Temizle", fontSize = 12.sp) }
                    }
                }
                OutlinedTextField(
                    value = rawText,
                    onValueChange = { rawText = it },
                    placeholder = { Text(if (isTextMode) "İngilizce paragraf yapıştırın..." else "resilient (adj) - dayanıklı\nephemeral, ubiquitous...") },
                    minLines = 5,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )
                if (isTextMode) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = ignoreStopWords, onCheckedChange = { ignoreStopWords = it })
                        Text("Yaygın bağlaç/edatları atla (the, and, is...)", fontSize = 13.sp)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = ignoreShort, onCheckedChange = { ignoreShort = it })
                    Text("Kısa kelimeleri atla (< 3 harf)", fontSize = 13.sp)
                }
            }
        }

        // Toplu Etiketleme
        Card {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Toplu Etiketler (Opsiyonel)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = tagInput,
                        onValueChange = { tagInput = it },
                        placeholder = { Text("Ör: IELTS, Reading...") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(onClick = {
                        val added = tagInput.split(",", ";", "\n").map { it.trim().removePrefix("#").take(28) }.filter { it.isNotEmpty() }
                        bulkTags = (bulkTags + added).distinct()
                        tagInput = ""
                    }) { Text("Ekle") }
                }
                if (bulkTags.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        bulkTags.forEach { t ->
                            FilterChip(
                                selected = true,
                                onClick = { bulkTags = bulkTags - t },
                                label = { Text("#$t ✕") }
                            )
                        }
                    }
                }
                if (store.customTags.isNotEmpty()) {
                    Text("Mevcut etiketlerden seç:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        store.customTags.forEach { t ->
                            val on = t in bulkTags
                            FilterChip(
                                selected = on,
                                onClick = { bulkTags = if (on) bulkTags - t else bulkTags + t },
                                label = { Text("#$t") }
                            )
                        }
                    }
                }
            }
        }

        Button(
            onClick = {
                if (tagInput.isNotBlank()) {
                    val added = tagInput.split(",", ";", "\n").map { it.trim().removePrefix("#").take(28) }.filter { it.isNotEmpty() }
                    bulkTags = (bulkTags + added).distinct()
                    tagInput = ""
                }
                val extracted = TextExtractor.extractEnglishWords(rawText, isTextMode && ignoreStopWords, ignoreShort)
                if (extracted.isEmpty()) {
                    Toast.makeText(context, "Geçerli İngilizce kelime bulunamadı", Toast.LENGTH_SHORT).show()
                }
                previewWords = extracted
                lastSummary = null
            },
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("Kelimeleri Ayıkla", fontWeight = FontWeight.Bold)
        }

        // Önizleme Paneli
        if (previewWords.isNotEmpty()) {
            val newCount = previewWords.count { it !in store.words }
            val existingCount = previewWords.size - newCount
            val hasTags = bulkTags.isNotEmpty()
            val actionable = if (hasTags) previewWords.size else newCount

            Card {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        StatChip("Bulunan", "${previewWords.size}")
                        StatChip("Yeni", "$newCount")
                        StatChip("Mevcut", "$existingCount")
                    }
                    Text(
                        if (existingCount > 0 && hasTags) "$existingCount mevcut kelimeye sadece seçili etiket(ler) eklenecek."
                        else if (existingCount > 0) "$existingCount mevcut kelime atlanacak (mevcutları etiketlemek için üstten etiket seçin)."
                        else "Tüm $newCount kelime yeni olarak eklenecek.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        previewWords.take(150).forEach { w ->
                            val exists = w in store.words
                            val badge = if (!exists) "Yeni" else if (hasTags) "+Tag" else "Atla"
                            val bg = when {
                                !exists -> Color(0xFFD1FAE5)
                                hasTags -> Color(0xFFE0E7FF)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                            Surface(
                                color = bg,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.clickable { previewWords = previewWords - w }
                            ) {
                                Text(
                                    "$w ($badge) ✕",
                                    fontSize = 12.sp,
                                    color = Color(0xFF0F172A),
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                    Button(
                        enabled = actionable > 0,
                        onClick = {
                            lastSummary = onCommitBulk(previewWords, bulkTags)
                            previewWords = emptyList()
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Sözlüğe Ekle ($newCount yeni${if (existingCount > 0 && hasTags) ", +$existingCount etiket" else ""})")
                    }
                }
            }
        }

        // Özet Sonuç Bandı
        lastSummary?.let { s ->
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFD1FAE5))) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Eklendi: ${s.addedNew} yeni • Etiketlendi: ${s.taggedExisting} • Atlandı: ${s.skippedExisting}",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF065F46)
                    )
                    Text(
                        "${s.offlinePopulated} kelime ikili offline sözlükten anında doldu • ${s.pendingCount} kelime online bekliyor.",
                        fontSize = 12.sp,
                        color = Color(0xFF065F46)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onGoToDictionary) { Text("Sözlüğümü Aç") }
                        if (s.pendingCount > 0) {
                            OutlinedButton(onClick = onFetchPending) {
                                Text(if (isFetchingPending) "Durdur" else "Bekleyenleri Online Çek")
                            }
                        }
                    }
                }
            }
        }
    }
}
