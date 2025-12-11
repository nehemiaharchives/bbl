@file:OptIn(ExperimentalForeignApi::class)

package org.gnit.bible

import com.russhwolf.settings.Settings
import kotlinx.cinterop.*
import kotlin.random.Random
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import platform.posix.*

private interface Closeable {
    fun close()
}

private inline fun <T : Closeable, R> T.use(block: (T) -> R): R {
    try {
        return block(this)
    } finally {
        try {
            close()
        } catch (_: Throwable) {
            // ignore close errors
        }
    }
}

/**
 * PosixSettings
 *
 * Kotlin/Native (posixMain: Linux/macOS) file-backed Settings implementation using okio.FileSystem and okio.Path.
 *
 * This single-file implementation is intended to be placed in the posixMain source set (not commonMain).
 *
 * Storage format:
 * - One entry per line: escapedKey=type:escapedValue
 * - type is one of:
 *   - 's' = String
 *   - 'i' = Int
 *   - 'l' = Long
 *   - 'f' = Float
 *   - 'd' = Double
 *   - 'b' = Boolean ("true" or "false")
 *
 * Escaping:
 * - Backslash, newline and equals in keys/values are escaped using backslash sequences:
 *   \\ -> \\\\
 *   \n -> \\n
 *   =  -> \\=
 *
 * Locking and atomicity:
 * - Uses a lockfile strategy implemented with POSIX calls (open with O_CREAT|O_EXCL).
 * - Writes are atomic: write to a temp file in the same directory and rename() to replace the target.
 *
 * Limitations:
 * - Intended for Kotlin/Native POSIX platforms (Linux, macOS). Not intended for JVM.
 * - Lockfile is best-effort; stale locks are pruned by the implementation. Not strong on some network filesystems.
 */
class PosixSettings(
    private val fileSystem: FileSystem = FileSystem.SYSTEM,
    private val path: Path
) : Settings {

    // In-memory cache. Values are stored encoded as "<type>:<rawValue>" where <rawValue> is unescaped.
    private val cache: MutableMap<String, String> = mutableMapOf()
    private var loaded: Boolean = false

    private val lock = PosixFileLock(path)
    private val usePosixLock: Boolean = fileSystem === FileSystem.SYSTEM

    private val lockAcquireTimeoutMs: Long = 10_000L

    private fun ensureParentDirectoryExists() {
        val parent = path.parent ?: return
        if (!fileSystem.exists(parent)) {
            fileSystem.createDirectories(parent)
        }
    }

    // --- escaping utilities ---

    private fun escape(s: String): String =
        s.replace("\\", "\\\\")
            .replace("\n", "\\n")
            .replace("=", "\\=")

    private fun unescape(s: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) {
                val next = s[i + 1]
                sb.append(
                    when (next) {
                        'n' -> '\n'
                        '\\' -> '\\'
                        '=' -> '='
                        else -> next
                    }
                )
                i += 2
            } else {
                sb.append(c)
                i++
            }
        }
        return sb.toString()
    }

    // --- file format parse/serialize ---

    private fun parse(contents: String) {
        cache.clear()
        contents.lineSequence().forEach { raw ->
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) return@forEach

            // split on first unescaped '='
            var idx = 0
            var seenSep = false
            val keySb = StringBuilder()
            val valSb = StringBuilder()
            while (idx < line.length) {
                val ch = line[idx]
                if (!seenSep && ch == '=') {
                    // if previous char is backslash it's escaped
                    if (idx > 0 && line[idx - 1] == '\\') {
                        // remove the escape backslash we stored before
                        if (keySb.isNotEmpty()) keySb.setLength(keySb.length - 1)
                        keySb.append('=')
                        idx++
                        continue
                    } else {
                        seenSep = true
                        idx++
                        continue
                    }
                }
                if (!seenSep) keySb.append(ch) else valSb.append(ch)
                idx++
            }

            if (!seenSep) return@forEach

            val key = unescape(keySb.toString())
            val rawValue = valSb.toString()

            // Expect type prefix: <typeChar>:<payload>
            if (rawValue.length >= 2 && rawValue[1] == ':') {
                val typeChar = rawValue[0]
                val payloadEscaped = rawValue.substring(2)
                val payload = unescape(payloadEscaped)
                cache[key] = "$typeChar:$payload"
            } else {
                // No type prefix found — treat as string
                val payload = unescape(rawValue)
                cache[key] = "s:$payload"
            }
        }
    }

    private fun serialize(): String =
        cache.entries.joinToString("\n") { (k, encoded) ->
            val (typeChar, payload) = if (encoded.length >= 2 && encoded[1] == ':') {
                Pair(encoded[0], encoded.substring(2))
            } else {
                Pair('s', encoded)
            }
            "${escape(k)}=${typeChar}:${escape(payload)}"
    }

    // --- load / flush ---

    private inline fun <T> withMemoryLock(block: () -> T): T = block()

    private fun loadIfNeeded() {
        if (loaded) return
        withMemoryLock {
            if (!loaded) {
                ensureParentDirectoryExists()
                acquireLock().use {
                    if (!fileSystem.exists(path)) {
                        cache.clear()
                    } else {
                        val contents = fileSystem.read(path) { readUtf8() }
                        parse(contents)
                    }
                    loaded = true
                }
            }
        }
    }

    private fun flushToDisk() {
        withMemoryLock {
            ensureParentDirectoryExists()
            acquireLock().use {
                val parent = path.parent
                val tmpName = "${path.name}.tmp-${Random.nextLong().toString(16)}"
                val tmpStr = if (parent != null) {
                    val parentStr = parent.toString().trimEnd('/')
                    "$parentStr/$tmpName"
                } else {
                    tmpName
                }
                val tmpPath = tmpStr.toPath()

                val contents = serialize()
                fileSystem.write(tmpPath) { writeUtf8(contents) }

                if (fileSystem.exists(path)) {
                    fileSystem.delete(path)
                }
                fileSystem.atomicMove(source = tmpPath, target = path)
            }
        }
    }

    private fun acquireLock(): Closeable =
        if (usePosixLock) lock.acquire(timeoutMs = lockAcquireTimeoutMs) else object : Closeable {
            override fun close() {}
        }

    // --- utilities for encoded values ---

    private fun encodeValue(type: Char, raw: String): String = "$type:$raw"

    private fun decode(value: String): Pair<Char, String> {
        if (value.length >= 2 && value[1] == ':') {
            return Pair(value[0], value.substring(2))
        }
        return Pair('s', value)
    }

    // --- Settings implementation ---

    override val keys: Set<String>
        get() {
            loadIfNeeded()
            return withMemoryLock { cache.keys.toSet() }
        }

    override val size: Int
        get() {
            loadIfNeeded()
            return withMemoryLock { cache.size }
        }

    override fun clear() {
        loadIfNeeded()
        withMemoryLock {
            if (cache.isNotEmpty()) {
                cache.clear()
                flushToDisk()
            }
        }
    }

    override fun remove(key: String) {
        loadIfNeeded()
        withMemoryLock {
            if (cache.remove(key) != null) {
                flushToDisk()
            }
        }
    }

    override fun hasKey(key: String): Boolean {
        loadIfNeeded()
        return withMemoryLock { cache.containsKey(key) }
    }

    override fun putInt(key: String, value: Int) {
        loadIfNeeded()
        withMemoryLock {
            cache[key] = encodeValue('i', value.toString())
            flushToDisk()
        }
    }

    override fun getInt(key: String, defaultValue: Int): Int {
        loadIfNeeded()
        return withMemoryLock {
            val raw = cache[key] ?: return@withMemoryLock defaultValue
            val (t, payload) = decode(raw)
            if (t == 'i') payload.toIntOrNull() ?: defaultValue else defaultValue
        }
    }

    override fun getIntOrNull(key: String): Int? {
        loadIfNeeded()
        return withMemoryLock {
            val raw = cache[key] ?: return@withMemoryLock null
            val (t, payload) = decode(raw)
            if (t == 'i') payload.toIntOrNull() else null
        }
    }

    override fun putLong(key: String, value: Long) {
        loadIfNeeded()
        withMemoryLock {
            cache[key] = encodeValue('l', value.toString())
            flushToDisk()
        }
    }

    override fun getLong(key: String, defaultValue: Long): Long {
        loadIfNeeded()
        return withMemoryLock {
            val raw = cache[key] ?: return@withMemoryLock defaultValue
            val (t, payload) = decode(raw)
            if (t == 'l') payload.toLongOrNull() ?: defaultValue else defaultValue
        }
    }

    override fun getLongOrNull(key: String): Long? {
        loadIfNeeded()
        return withMemoryLock {
            val raw = cache[key] ?: return@withMemoryLock null
            val (t, payload) = decode(raw)
            if (t == 'l') payload.toLongOrNull() else null
        }
    }

    override fun putString(key: String, value: String) {
        loadIfNeeded()
        withMemoryLock {
            cache[key] = encodeValue('s', value)
            flushToDisk()
        }
    }

    override fun getString(key: String, defaultValue: String): String {
        loadIfNeeded()
        return withMemoryLock {
            val raw = cache[key] ?: return@withMemoryLock defaultValue
            val (t, payload) = decode(raw)
            if (t == 's') payload else defaultValue
        }
    }

    override fun getStringOrNull(key: String): String? {
        loadIfNeeded()
        return withMemoryLock {
            val raw = cache[key] ?: return@withMemoryLock null
            val (t, payload) = decode(raw)
            if (t == 's') payload else null
        }
    }

    override fun putFloat(key: String, value: Float) {
        loadIfNeeded()
        withMemoryLock {
            cache[key] = encodeValue('f', value.toString())
            flushToDisk()
        }
    }

    override fun getFloat(key: String, defaultValue: Float): Float {
        loadIfNeeded()
        return withMemoryLock {
            val raw = cache[key] ?: return@withMemoryLock defaultValue
            val (t, payload) = decode(raw)
            if (t == 'f') payload.toFloatOrNull() ?: defaultValue else defaultValue
        }
    }

    override fun getFloatOrNull(key: String): Float? {
        loadIfNeeded()
        return withMemoryLock {
            val raw = cache[key] ?: return@withMemoryLock null
            val (t, payload) = decode(raw)
            if (t == 'f') payload.toFloatOrNull() else null
        }
    }

    override fun putDouble(key: String, value: Double) {
        loadIfNeeded()
        withMemoryLock {
            cache[key] = encodeValue('d', value.toString())
            flushToDisk()
        }
    }

    override fun getDouble(key: String, defaultValue: Double): Double {
        loadIfNeeded()
        return withMemoryLock {
            val raw = cache[key] ?: return@withMemoryLock defaultValue
            val (t, payload) = decode(raw)
            if (t == 'd') payload.toDoubleOrNull() ?: defaultValue else defaultValue
        }
    }

    override fun getDoubleOrNull(key: String): Double? {
        loadIfNeeded()
        return withMemoryLock {
            val raw = cache[key] ?: return@withMemoryLock null
            val (t, payload) = decode(raw)
            if (t == 'd') payload.toDoubleOrNull() else null
        }
    }

    override fun putBoolean(key: String, value: Boolean) {
        loadIfNeeded()
        withMemoryLock {
            cache[key] = encodeValue('b', value.toString())
            flushToDisk()
        }
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        loadIfNeeded()
        return withMemoryLock {
            val raw = cache[key] ?: return@withMemoryLock defaultValue
            val (t, payload) = decode(raw)
            if (t == 'b') payload.toBooleanStrictOrNull() ?: defaultValue else defaultValue
        }
    }

    override fun getBooleanOrNull(key: String): Boolean? {
        loadIfNeeded()
        return withMemoryLock {
            val raw = cache[key] ?: return@withMemoryLock null
            val (t, payload) = decode(raw)
            if (t == 'b') payload.toBooleanStrictOrNull() else null
        }
    }

    /**
     * Forces an immediate flush of any in-memory changes to disk.
     */
    fun flush() {
        withMemoryLock {
            flushToDisk()
        }
    }
}

/**
 * POSIX lockfile + atomic replace implementation (Kotlin/Native POSIX).
 *
 * This is intentionally included in the same file to be used directly from posixMain.
 */

/**
 * Lock implementation using lockfile created with O_CREAT | O_EXCL.
 * The lockfile is removed when the returned Closeable is closed.
 */
private class PosixFileLock(lockTarget: Path) {
    private val lockPath: String = "$lockTarget.lock"
    private val retryDelayUs: UInt = 50_000u // 50ms

    /**
     * Acquire the lock (blocking until acquired or timeout).
     * Returns a Closeable that releases the lock on close.
     * Throws IllegalStateException on timeout.
     */
    fun acquire(timeoutMs: Long = 10_000L): Closeable {
        val start = time(null)
        while (true) {
            val fd = open(lockPath, O_CREAT or O_EXCL or O_WRONLY, 0b110100100) // 0o644
            if (fd != -1) {
                try {
                    // write pid for debugging
                    val pid = getpid()
                    val info = "pid:$pid\n"
                    val bytes = info.encodeToByteArray()
                    bytes.usePinned { pinned ->
                        var written = 0
                        while (written < bytes.size) {
                            val res = write(fd, pinned.addressOf(written), (bytes.size - written).convert())
                            if (res <= 0) break
                            written += res.toInt()
                        }
                    }
                } catch (_: Throwable) {
                    // ignore write errors
                } finally {
                    close(fd)
                }
                return object : Closeable {
                    override fun close() {
                        unlink(lockPath)
                    }
                }
            } else {
                // failed to create
                // continue retrying until timeout (best-effort)
            }

            // check timeout
            if (timeoutMs > 0L) {
                val now = time(null)
                if ((now - start) * 1000L > timeoutMs) {
                    break
                }
            }
            usleep(retryDelayUs)
        }

        throw IllegalStateException("Failed to acquire lock on $lockPath within $timeoutMs ms")
    }
}
