package org.gnit.bible

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import io.github.oshai.kotlinlogging.KotlinLogging

class QuoteVerseAppWidgetProvider : AppWidgetProvider() {

    private val logger = KotlinLogging.logger {}

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        logger.debug { "QuoteVerseAppWidgetProvider onReceive called with Intent: $intent" }

        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE ||
            intent.action == "android.appwidget.action.APPWIDGET_UPDATE"
        ) {
            extractParams(intent)?.let { params ->
                updateWidgets(context, intent, params)
            }
        }
    }

    private fun updateWidgets(context: Context, intent: Intent, params: VerseParams) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val widgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
            ?: appWidgetManager.getAppWidgetIds(
                ComponentName(context, QuoteVerseAppWidgetProvider::class.java)
            )

        val platform = getPlatform(context)
        val bible = Bible(assetManager = AssetManagerImpl(platform = platform)).apply {
            bibleTextReader = ComposeBibleTextReader()
        }

        val translation = resolveTranslation(params.translationCode, bible.availableTranslations())
        val bookIndex = resolveBookIndex(params.bookValue, translation)
        val chapter = params.chapter.coerceIn(1, Books.maxChapter(bookIndex))
        val verseRange = params.verses

        val verseText = runCatching {
            val chapterText = bible.verses(translation.code, bookIndex, chapter)
            val verses = Bible.splitChapterToVerses(chapterText)
            val startIdx = (verseRange.first - 1).coerceAtLeast(0)
            val endIdx = (verseRange.last - 1).coerceAtMost(verses.lastIndex)
            if (startIdx > endIdx) "" else verses.slice(startIdx..endIdx).joinToString(" ") { it.trim() }
        }.getOrElse { "" }

        val referenceName = translation.books()[bookIndex]
            ?: Translation.webus.books()[bookIndex]
            ?: "Book $bookIndex"

        val views = RemoteViews(context.packageName, R.layout.quote_verse_widget).apply {
            setTextViewText(R.id.verse_text, verseText.ifBlank { "Verse unavailable" })
            val verseLabel = if (verseRange.first == verseRange.last) {
                "${verseRange.first}"
            } else {
                "${verseRange.first}-${verseRange.last}"
            }
            setTextViewText(R.id.verse_reference, "$referenceName $chapter:$verseLabel (${translation.code.uppercase()})")
        }

        val verseParam = if (verseRange.first == verseRange.last) {
            "${verseRange.first}"
        } else {
            "${verseRange.first}-${verseRange.last}"
        }
        val uri = Uri.parse("bbl://read?translation=${translation.code}&book=$bookIndex&chapter=$chapter&verse=$verseParam")
        val openIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            `package` = context.packageName
        }
        val pending = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, pending)

        appWidgetManager.updateAppWidget(widgetIds, views)
    }

    private fun extractParams(intent: Intent): VerseParams? {
        val bookParam = intent.stringParam("book") ?: return null
        val chapterParam = intent.stringParam("chapter")?.toIntOrNull() ?: return null
        val verseRange = intent.stringParam("verse")?.let { parseVerseRange(it) } ?: return null

        val translationCode = intent.stringParam("translation")

        return VerseParams(
            bookValue = bookParam,
            chapter = chapterParam,
            verses = verseRange,
            translationCode = translationCode
        )
    }

    private fun parseVerseRange(raw: String): IntRange? {
        val cleaned = raw.normalizeDigits().trim()
        if (cleaned.isEmpty()) return null
        return if (cleaned.contains('-')) {
            val parts = cleaned.split('-', limit = 2)
            val start = parts.getOrNull(0)?.filter { it.isDigit() }?.toIntOrNull() ?: return null
            val end = parts.getOrNull(1)?.filter { it.isDigit() }?.toIntOrNull() ?: return null
            if (start <= 0 || end <= 0 || end < start) return null
            IntRange(start, end)
        } else {
            val single = cleaned.filter { it.isDigit() }.toIntOrNull() ?: return null
            if (single <= 0) return null
            IntRange(single, single)
        }
    }

    private fun resolveTranslation(code: String?, available: List<Translation>): Translation {
        if (code != null) {
            available.firstOrNull { it.code.equals(code, ignoreCase = true) }?.let { return it }
            available.firstOrNull {
                it.englishName.equals(code, ignoreCase = true) || it.nativeName.equals(code, ignoreCase = true)
            }?.let { return it }
        }
        return Translation.kjv
    }

    private fun resolveBookIndex(bookValue: String, translation: Translation): Int {
        val cleaned = sanitizeBookName(bookValue)

        val numeric = cleaned?.toIntOrNull()
        if (numeric != null && numeric in 1..66) return numeric

        translation.books().entries.firstOrNull { it.value.equals(cleaned, ignoreCase = true) }?.let {
            return it.key
        }

        Language.embeddedLanguages.forEach { lang ->
            lang.bookNames().forEachIndexed { idx, name ->
                if (name.equals(cleaned, ignoreCase = true)) return idx + 1
            }
        }

        Translation.webus.books().entries.firstOrNull { it.value.equals(cleaned, ignoreCase = true) }?.let {
            return it.key
        }

        return 1
    }

    private fun Intent.stringParam(key: String): String? =
        getStringExtra(key) ?: extras?.getString(key)

    private data class VerseParams(
        val bookValue: String,
        val chapter: Int,
        val verses: IntRange,
        val translationCode: String?
    )

    private fun String.normalizeDigits(): String =
        map { ch ->
            when (ch) {
                in '０'..'９' -> '0' + (ch - '０')
                else -> ch
            }
        }.joinToString("")

    private fun sanitizeBookName(raw: String?): String? {
        if (raw == null) return null
        return raw
            .normalizeDigits()
            .replace("’", "'")
            .replace(CHAPTER_WORD_REGEX, " ")
            .replace(BOOK_PREFIX_REGEX, " ")
            .trim()
            .ifBlank { null }
    }

    private val CHAPTER_WORD_REGEX =
        "(?i)\\b(chapter|chapitre|capitulo|capítulo|capitolo|kapitel|kapitola|hoofdstuk|cap|chap)\\b".toRegex()
    private val BOOK_PREFIX_REGEX =
        "(?i)\\b(book of|the gospel of|gospel of|book)\\b".toRegex()
}
