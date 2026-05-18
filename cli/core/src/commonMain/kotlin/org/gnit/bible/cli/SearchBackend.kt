package org.gnit.bible.cli

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import org.gnit.bible.Bible
import org.gnit.bible.Language
import org.gnit.bible.SearchModuleId
import org.gnit.bible.Translation
import org.gnit.bible.VersePointerJson

data class SearchRequest(
    val term: String,
    val translation: Translation,
    val bookNumber: Int?,
    val startChapter: Int?,
    val endChapter: Int?,
    val verses: Int
)

data class SearchOutput(val text: String)

class SearchBackendException(message: String) : RuntimeException(message)

interface SearchBackend {
    fun search(request: SearchRequest): SearchOutput
}

class InternalSearchBackend(
    private val bible: Bible
) : SearchBackend {
    override fun search(request: SearchRequest): SearchOutput {
        val results = bible.search(
            term = request.term,
            bookNumber = request.bookNumber,
            startChapter = request.startChapter,
            endChapter = request.endChapter,
            verses = request.verses,
            translation = request.translation
        )
        return SearchOutput(VersePointerJson.encodeList(results))
    }
}

class ExternalSearchBackend(
    private val processRunner: ProcessRunner,
    private val fileSystem: FileSystem,
    private val binaryPath: Path,
    private val moduleId: SearchModuleId
) : SearchBackend {
    private var helperVersionValidated = false

    override fun search(request: SearchRequest): SearchOutput {
        if (!fileSystem.exists(binaryPath)) {
            throw SearchBackendException(
                "Search helper '$binaryPath' is missing for module ${moduleId.name.lowercase()}. " +
                    "Run `bbl install ${request.translation.code}` to install it."
            )
        }

        ensureCompatibleHelperVersion()

        val command = buildCommand(request)
        val result = processRunner.run(command)
        if (result.exitCode != 0) {
            val detail = result.stderr.ifBlank { result.stdout }.ifBlank { "unknown error" }
            throw SearchBackendException(
                "Search helper ${moduleId.name.lowercase()} failed (exit ${result.exitCode}): $detail"
            )
        }

        return SearchOutput(result.stdout.trimEnd())
    }

    private fun ensureCompatibleHelperVersion() {
        if (helperVersionValidated) return

        val versionCommand = listOf(binaryPath.toString(), "--artifact-compat-version")
        val result = processRunner.run(versionCommand)
        if (result.exitCode != 0) {
            val detail = result.stderr.ifBlank { result.stdout }.ifBlank { "unknown error" }
            throw SearchBackendException(
                "Search helper ${moduleId.name.lowercase()} failed artifact compatibility version check (exit ${result.exitCode}): $detail"
            )
        }

        val expected = bblSearchHelperArtifactCompatibilityVersionLine()
        val actual = result.stdout.trim()
        if (actual != expected) {
            val actualDisplay = actual.ifBlank { "<blank>" }
            throw SearchBackendException(
                "Search helper ${moduleId.name.lowercase()} artifact compatibility version mismatch: expected '$expected' but got '$actualDisplay'"
            )
        }

        helperVersionValidated = true
    }

    private fun buildCommand(request: SearchRequest): List<String> {
        val args = mutableListOf<String>()
        args.add(binaryPath.toString())
        args.add("-t")
        args.add(request.translation.code)
        request.bookNumber?.let {
            args.add("--book")
            args.add(it.toString())
        }
        request.startChapter?.let {
            args.add("--chapter")
            args.add(it.toString())
        }
        request.endChapter?.let {
            args.add("--end-chapter")
            args.add(it.toString())
        }
        if (request.verses > 0) {
            args.add("--verses")
            args.add(request.verses.toString())
        }
        args.add(request.term)
        return args
    }
}

class SearchBackendSelector(
    private val bible: Bible,
    private val processRunner: ProcessRunner = PlatformProcessRunner(),
    private val fileSystem: FileSystem = bible.assetManager.fileSystem,
    binDirProvider: (() -> Path)? = null
) {
    private val binDirProvider: () -> Path = binDirProvider ?: { defaultBinDir() }

    fun backendFor(language: Language): SearchBackend {
        return if (language.searchModuleId == SearchModuleId.COMMON) {
            InternalSearchBackend(bible)
        } else {
            val executableSuffix = if (bible.assetManager.platform.name == "Windows") ".exe" else ""
            val binaryName = "bbl-search-${language.searchModuleId.name.lowercase()}$executableSuffix"
            val binaryPath = binDirProvider() / binaryName
            ExternalSearchBackend(processRunner, fileSystem, binaryPath, language.searchModuleId)
        }
    }

    fun defaultBinDir(): Path {
        val platform = bible.assetManager.platform
        val packDir = platform.packDir.toPath()
        val bblDir = packDir.parent
            ?: throw SearchBackendException("Unable to resolve bbl dir from pack dir: ${platform.packDir}")
        return bblDir / "bin"
    }
}
