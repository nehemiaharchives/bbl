package org.gnit.bible

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import io.github.oshai.kotlinlogging.KotlinLogging

class MainActivity : ComponentActivity() {

    private val logger = KotlinLogging.logger {}

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        logger.debug { "DEBUG MainActivity onCreate called with savedInstanceState=$savedInstanceState intent=$intent" }

        val initialState = parseBibleState(intent)
        logger.debug { "DEBUG MainActivity onCreate resolved initialBibleState=$initialState" }

        setContent {
            App(
                platformContext = this.applicationContext,
                initialBibleState = initialState
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        logger.debug { "DEBUG MainActivity onNewIntent received: intent=$intent data=${intent.data}" }
    }

    private fun parseBibleState(intent: Intent?): BibleState? {
        val safeIntent = intent ?: run {
            logger.debug { "DEBUG MainActivity parseBibleState received null intent" }
            return null
        }
        logger.debug { "DEBUG MainActivity parseBibleState inspecting intent: $safeIntent" }

        // Preferred: deep link URI.
        safeIntent.data?.let { data ->
            if (data.scheme == "bbl" && data.host == "read") {
                return buildState(
                    translationCode = data.getQueryParameter("translation"),
                    bookValue = data.getQueryParameter("book"),
                    chapterValue = data.getQueryParameter("chapter")
                ).also { logger.debug { "DEBUG MainActivity Deep link state from data URI: $it" } }
            } else {
                logger.debug { "DEBUG MainActivity Intent data did not match expected deep link: $data" }
            }
        } ?: logger.debug { "DEBUG MainActivity Intent data URI was null" }

        // Fallback: some Assistant flows send parameters via extras.
        val translationExtra = safeIntent.getStringExtra("translation")
            ?: safeIntent.getStringExtra("org.gnit.bible.translation")
        val bookExtra = safeIntent.getStringExtra("book")
            ?: safeIntent.getStringExtra("org.gnit.bible.book")
        val chapterExtra = safeIntent.getStringExtra("chapter")
            ?: safeIntent.getStringExtra("org.gnit.bible.chapter")

        if (translationExtra != null || bookExtra != null || chapterExtra != null) {
            logger.debug { "DEBUG MainActivity Extras detected translation=$translationExtra book=$bookExtra chapter=$chapterExtra" }
            return buildState(
                translationCode = translationExtra,
                bookValue = bookExtra,
                chapterValue = chapterExtra
            ).also { logger.debug { "DEBUG MainActivity Deep link state from extras: $it" } }
        }

        // Fallback: parse a free-form query string if Assistant provided one (sometimes delivered when capability match fails).
        val queryText = safeIntent.getStringExtra(android.app.SearchManager.QUERY)
            ?: safeIntent.getStringExtra(Intent.EXTRA_TEXT)
            ?: safeIntent.extras?.getString("query")
            ?: safeIntent.extras?.getString(Intent.EXTRA_SUBJECT)
        if (!queryText.isNullOrBlank()) {
            parseFromQuery(queryText)?.let {
                logger.debug { "DEBUG MainActivity Deep link state from free-form query: $it (query='$queryText')" }
                return it
            }
        }

        logger.debug { "DEBUG MainActivity Deep link params NOT found; falling back to saved state" }
        return null
    }

    private fun buildState(
        translationCode: String?,
        bookValue: String?,
        chapterValue: String?
    ): BibleState {
        logger.debug {
            "buildState with translationCode=$translationCode bookValue=$bookValue chapterValue=$chapterValue"
        }
        val (cleanBook, chapterOverride) = splitBookAndChapter(bookValue, chapterValue)
        val translation = resolveTranslation(translationCode)
        val book = resolveBookIndex(cleanBook, translation)
        val chapter = parseChapterNumber(chapterOverride)?.coerceIn(1, Books.maxChapter(book)) ?: 1

        val state = BibleState(
            mainTranslation = translation,
            book = book,
            chapter = chapter
        )
        logger.debug { "DEBUG MainActivity buildState produced: $state" }
        return state
    }

    private fun splitBookAndChapter(bookValue: String?, chapterValue: String?): Pair<String?, String?> {
        logger.debug { "DEBUG MainActivity splitBookAndChapter called with bookValue=$bookValue chapterValue=$chapterValue" }
        val result = when {
            !chapterValue.isNullOrBlank() -> sanitizeBookName(bookValue) to chapterValue
            bookValue.isNullOrBlank() -> null to null
            else -> {
                val normalized = bookValue.normalizeDigits()
                val match = "\\d+".toRegex().find(normalized)
                if (match == null) {
                    sanitizeBookName(bookValue) to null
                } else {
                    val chapterPart = match.value
                    val bookPart = normalized.substring(0, match.range.first)
                        .replace(CHAPTER_WORD_REGEX, " ")
                        .trim()
                    sanitizeBookName(bookPart) to chapterPart
                }
            }
        }
        logger.debug { "DEBUG MainActivity splitBookAndChapter returning $result" }
        return result
    }

    private fun parseChapterNumber(raw: String?): Int? {
        logger.debug { "DEBUG MainActivity parseChapterNumber called with raw=$raw" }
        if (raw.isNullOrBlank()) {
            logger.debug { "DEBUG MainActivity parseChapterNumber returning null because raw was blank" }
            return null
        }
        val normalized = raw.normalizeDigits()
        val match = "\\d+".toRegex().find(normalized)
        val result = match?.value?.toIntOrNull()
        logger.debug { "DEBUG MainActivity parseChapterNumber normalized=$normalized result=$result" }
        return result
    }

    private fun parseFromQuery(query: String): BibleState? {
        logger.debug { "DEBUG MainActivity parseFromQuery called with query='$query'" }
        val normalized = query.normalizeDigits()

        val tokens = normalized.split(Regex("[\\s,]+")).filter { it.isNotBlank() }

        val translationToken = tokens.firstOrNull { token ->
            Translation.embeddedTranslations.any {
                it.code.equals(token, ignoreCase = true) ||
                        it.englishName.equals(token, ignoreCase = true) ||
                        it.nativeName.equals(token, ignoreCase = true)
            }
        }
        val translation = resolveTranslation(translationToken)

        val joinedTokens = tokens.joinToString(" ")
        val bookMatch = BOOK_REGEX.find(joinedTokens)
        val bookCandidate = bookMatch?.groupValues?.getOrNull(1)?.trim()
        val chapterCandidate = bookMatch?.groupValues?.getOrNull(2)

        val bookIndex = resolveBookIndex(bookCandidate, translation)
        val chapter = parseChapterNumber(chapterCandidate)?.coerceIn(1, Books.maxChapter(bookIndex)) ?: 1

        val state = BibleState(mainTranslation = translation, book = bookIndex, chapter = chapter)
        logger.debug { "DEBUG MainActivity parseFromQuery produced $state" }
        return state
    }

    private fun String.normalizeDigits(): String {
        val normalized = map { ch ->
            when (ch) {
                in '０'..'９' -> '0' + (ch - '０')
                else -> ch
            }
        }.joinToString("")
        logger.debug { "DEBUG MainActivity normalizeDigits input='$this' output='$normalized'" }
        return normalized
    }

    private fun resolveTranslation(codeOrName: String?): Translation {
        logger.debug { "DEBUG MainActivity resolveTranslation called with codeOrName=$codeOrName" }
        val translation = when {
            codeOrName == null -> Translation.webus
            else -> {
                Translation.embeddedTranslations.firstOrNull { it.code.equals(codeOrName, ignoreCase = true) }
                    ?: Translation.embeddedTranslations.firstOrNull {
                        it.englishName.equals(codeOrName, ignoreCase = true) ||
                            it.nativeName.equals(codeOrName, ignoreCase = true)
                    }
                    ?: Translation.webus
            }
        }
        logger.debug { "DEBUG MainActivity resolveTranslation returning ${translation.code}" }
        return translation
    }

    private fun resolveBookIndex(bookValue: String?, translation: Translation): Int {
        logger.debug { "DEBUG MainActivity resolveBookIndex called with bookValue=$bookValue translation=${translation.code}" }
        val cleaned = sanitizeBookName(bookValue)
        val numeric = cleaned?.toIntOrNull()?.takeIf { it in 1..66 }
        val translationSpecific = translation.books().entries.firstOrNull { it.value.equals(cleaned, ignoreCase = true) }?.key
        val embeddedMatch = if (translationSpecific == null) {
            Language.embeddedLanguages.firstNotNullOfOrNull { lang ->
                lang.bookNames().indexOfFirst { it.equals(cleaned, ignoreCase = true) }.takeIf { it >= 0 }?.plus(1)
            }
        } else null
        val result = numeric ?: translationSpecific ?: embeddedMatch ?: 1
        logger.debug { "DEBUG MainActivity resolveBookIndex cleaned=$cleaned result=$result" }
        return result
    }

    private fun sanitizeBookName(raw: String?): String? {
        logger.debug { "DEBUG MainActivity sanitizeBookName called with raw=$raw" }
        val sanitized = raw
            ?.normalizeDigits()
            ?.replace("’", "'")
            ?.replace(CHAPTER_WORD_REGEX, " ")
            ?.replace(BOOK_PREFIX_REGEX, " ")
            ?.trim()
            ?.ifBlank { null }
        logger.debug { "DEBUG MainActivity sanitizeBookName returning $sanitized" }
        return sanitized
    }

    private fun String.ifBlankOrNull(): String? {
        val result = if (isBlank()) null else this
        logger.debug { "DEBUG MainActivity ifBlankOrNull evaluated '$this' to $result" }
        return result
    }

    private val CHAPTER_WORD_REGEX =
        "(?i)\\b(chapter|chapitre|capitulo|capítulo|capitolo|kapitel|kapitola|hoofdstuk|cap|chap)\\b".toRegex()
    private val BOOK_PREFIX_REGEX =
        "(?i)\\b(book of|the gospel of|gospel of|book)\\b".toRegex()
    private val BOOK_REGEX =
        "(?i)([\\p{L}\\d\\s']+?)\\s+(?:chapter\\s+)?(\\d+)".toRegex()
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
