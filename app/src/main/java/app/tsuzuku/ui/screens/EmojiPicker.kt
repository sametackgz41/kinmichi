package app.tsuzuku.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.tsuzuku.R
import java.text.Normalizer

/** One emoji with search words (Turkish and English; Unicode's own name is searched too). */
internal data class EmojiEntry(val emoji: String, val words: String)

internal data class EmojiGroup(@StringRes val title: Int, val entries: List<EmojiEntry>)

private fun group(@StringRes title: Int, vararg pairs: Pair<String, String>) =
    EmojiGroup(title, pairs.map { EmojiEntry(it.first, it.second) })

internal val emojiCatalog = listOf(
    group(R.string.emoji_health,
        "💧" to "su damla içmek water drink", "🥤" to "bardak içecek cup drink", "💊" to "ilaç hap vitamin pill medicine",
        "🦷" to "diş fırçalamak tooth teeth", "🪥" to "diş fırçası toothbrush", "🧴" to "krem cilt bakım lotion skin",
        "🧼" to "sabun yıkamak soap wash", "🛁" to "banyo küvet bath", "🚿" to "duş shower", "😴" to "uyku uyumak sleep",
        "🛌" to "yatak uyku bed sleep", "🌙" to "gece ay night moon", "⏰" to "alarm saat uyanmak wake clock", "🌅" to "sabah gündoğumu morning sunrise",
        "❤️" to "kalp sağlık heart health", "🫁" to "nefes akciğer breath lungs", "🩺" to "doktor kontrol doctor checkup", "⚖️" to "kilo tartı weight scale",
        "🚭" to "sigara bırakmak no smoking quit", "🍷" to "alkol şarap alcohol wine", "☕" to "kahve kafein coffee caffeine", "🍵" to "çay tea",
    ),
    group(R.string.emoji_sport,
        "🏃" to "koşu koşmak run running", "🚶" to "yürüyüş yürümek walk", "🚴" to "bisiklet cycling bike", "🏊" to "yüzme swim",
        "🏋️" to "ağırlık spor salonu weights gym", "💪" to "kas güç şınav strength push-ups", "🤸" to "esneme jimnastik stretch", "🧘" to "yoga meditasyon meditate",
        "⚽" to "futbol football soccer", "🏀" to "basketbol basketball", "🎾" to "tenis tennis", "🏓" to "masa tenisi ping pong",
        "🥊" to "boks boxing", "🧗" to "tırmanış climbing", "⛰️" to "dağ yürüyüş hiking mountain", "🎿" to "kayak ski",
        "🛹" to "kaykay skateboard", "🤾" to "hentbol handball", "🏐" to "voleybol volleyball", "🦵" to "bacak leg",
    ),
    group(R.string.emoji_food,
        "🍎" to "elma meyve apple fruit", "🍌" to "muz banana", "🥦" to "brokoli sebze vegetable broccoli", "🥗" to "salata salad",
        "🥕" to "havuç carrot", "🍳" to "kahvaltı yumurta breakfast egg", "🥣" to "kase yulaf bowl oats", "🍞" to "ekmek bread",
        "🍽️" to "yemek tabak meal dinner", "🥜" to "fıstık kuruyemiş nuts", "🍫" to "çikolata tatlı chocolate sweet", "🍬" to "şeker candy sugar",
        "🍔" to "hamburger fast food", "🍕" to "pizza", "🥛" to "süt milk", "🍯" to "bal honey", "🥑" to "avokado avocado", "🍓" to "çilek strawberry",
        "🫖" to "demlik çaydanlık teapot", "🧃" to "meyve suyu juice",
    ),
    group(R.string.emoji_mind,
        "🧠" to "beyin zihin brain mind", "🙏" to "şükran dua gratitude pray", "✍️" to "yazmak günlük write journal", "📓" to "defter günlük notebook journal",
        "😊" to "mutlu gülümseme happy smile", "😌" to "sakin huzur calm relaxed", "🌿" to "doğa sakinlik nature calm", "🍃" to "yaprak rüzgar leaf",
        "🕯️" to "mum candle", "🎵" to "müzik music", "🎨" to "resim sanat art paint", "📵" to "telefon yok ekran no phone screen",
        "📴" to "kapalı telefon off phone", "🌸" to "çiçek sakura flower", "☀️" to "güneş sun", "🌈" to "gökkuşağı rainbow",
        "🫶" to "sevgi kalp eller love", "🤗" to "sarılmak hug", "🧩" to "bulmaca puzzle", "♟️" to "satranç chess",
    ),
    group(R.string.emoji_learning,
        "📖" to "kitap okumak book read", "📚" to "kitaplar books", "🗂️" to "kelime kart flashcards cards", "🎧" to "dinlemek podcast listen",
        "🗣️" to "konuşmak dil speak language", "💬" to "sohbet chat", "🌍" to "dünya dil world language", "✏️" to "kalem çalışmak pencil study",
        "🧮" to "matematik math abacus", "🔬" to "bilim science", "💻" to "bilgisayar kod computer code", "⌨️" to "klavye yazılım keyboard",
        "🎓" to "okul mezuniyet school graduation", "🎹" to "piyano piano", "🎸" to "gitar guitar", "🎻" to "keman violin",
        "📝" to "not plan notes plan", "🧑‍💻" to "programlama coding", "📰" to "gazete haber news", "🖋️" to "dolma kalem pen",
    ),
    group(R.string.emoji_home,
        "🏠" to "ev home house", "🧹" to "temizlik süpürmek clean sweep", "🧺" to "çamaşır laundry", "🍳" to "yemek pişirmek cook",
        "🪴" to "bitki sulamak plant water", "🛏️" to "yatak toplamak make bed", "🗑️" to "çöp trash", "🧽" to "bulaşık sünger dishes sponge",
        "🐕" to "köpek gezdirmek dog walk", "🐈" to "kedi cat", "🐟" to "balık fish", "🌱" to "fide büyümek seedling grow",
        "🛒" to "alışveriş market shopping", "🧾" to "fatura bills receipt", "🔧" to "tamir fix tool", "👶" to "bebek çocuk baby child",
    ),
    group(R.string.emoji_work,
        "💼" to "iş çanta work briefcase", "📅" to "takvim plan calendar", "✅" to "yapıldı görev done task", "📌" to "önemli pin important",
        "📈" to "büyüme grafik growth chart", "💰" to "para birikim money save", "💳" to "kart harcama card spend", "🏦" to "banka bank",
        "📧" to "e-posta email", "📞" to "telefon arama call", "⏳" to "zaman time hourglass", "🎯" to "hedef odak goal focus",
        "🚀" to "proje başlamak project launch", "🧭" to "yön pusula compass", "🗓️" to "haftalık plan weekly planner", "📊" to "rapor report",
    ),
    group(R.string.emoji_social,
        "👨‍👩‍👧" to "aile family", "👫" to "arkadaş friends", "📱" to "telefon mesaj phone text", "💌" to "mektup love letter",
        "🎁" to "hediye gift", "🤝" to "yardım gönüllü help volunteer", "🎉" to "kutlama celebrate party", "☎️" to "anneyi ara call mom",
        "😂" to "gülmek laugh", "🥰" to "sevgi love", "👋" to "selam hello", "🌻" to "ayçiçeği sunflower",
    ),
    group(R.string.emoji_symbols,
        "✓" to "tik onay check", "⭐" to "yıldız star", "🔥" to "ateş seri fire streak", "✨" to "parıltı sparkle",
        "💎" to "elmas diamond", "🏆" to "kupa trophy", "🥇" to "madalya birinci medal", "🔔" to "zil bell",
        "⚡" to "enerji enerji lightning energy", "🌀" to "girdap spiral", "🔵" to "mavi blue", "🟢" to "yeşil green",
        "🟡" to "sarı yellow", "🔴" to "kırmızı red", "🟣" to "mor purple", "⚪" to "beyaz white",
    ),
)

private fun fold(text: String): String =
    Normalizer.normalize(text.lowercase(java.util.Locale.ROOT).replace('ı', 'i'), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")

private fun unicodeName(emoji: String): String =
    runCatching { Character.getName(emoji.codePointAt(0)) }.getOrNull().orEmpty()

/** Emoji whose words or Unicode name contain every part of [query]. */
internal fun searchEmoji(query: String): List<EmojiEntry> {
    val parts = fold(query).split(' ').filter(String::isNotBlank)
    if (parts.isEmpty()) return emptyList()
    return emojiCatalog.flatMap { it.entries }.distinctBy { it.emoji }.filter { entry ->
        val hay = fold(entry.words + " " + unicodeName(entry.emoji))
        parts.all(hay::contains)
    }
}

/** Full-screen picker: search on top, categories below. */
@Composable
internal fun EmojiPickerDialog(selected: String, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        EmojiPickerContent(selected, onPick, onDismiss)
    }
}

@Composable
internal fun EmojiPickerContent(selected: String, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    run {
        Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().statusBarsPadding()) {
                Row(Modifier.padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back)) }
                    Text(stringResource(R.string.icon_label), style = MaterialTheme.typography.titleLarge)
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it.take(40) },
                    leadingIcon = { Icon(Icons.Rounded.Search, null) },
                    placeholder = { Text(stringResource(R.string.emoji_search_hint)) },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                )
                val results = if (query.isBlank()) null else searchEmoji(query)
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(56.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (results != null) {
                        if (results.isEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Text(stringResource(R.string.emoji_no_results), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(8.dp))
                            }
                        }
                        items(results, key = { "r" + it.emoji }) { EmojiCell(it.emoji, it.emoji == selected) { onPick(it.emoji) } }
                    } else {
                        emojiCatalog.forEach { g ->
                            item(span = { GridItemSpan(maxLineSpan) }, key = "h${g.title}") {
                                Text(
                                    stringResource(g.title),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 10.dp, bottom = 2.dp, start = 4.dp),
                                )
                            }
                            items(g.entries, key = { "${g.title}-" + it.emoji }) { EmojiCell(it.emoji, it.emoji == selected) { onPick(it.emoji) } }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmojiCell(emoji: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.aspectRatio(1f).clip(CircleShape)
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(emoji, fontSize = 26.sp) }
}
