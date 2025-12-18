package org.gnit.bible.cli

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.UByteVar
import org.gnit.bible.BibleResourcesReader
import kotlinx.cinterop.*

actual class CliBibleResourcesReader : BibleResourcesReader {

    actual override fun chapterFile(translation: String, book: Int, chapter: Int): String =
        "$base/$translation/$translation.$book.$chapter.txt"

    actual override fun readByPath(path: String): String {
        // Expected: "$base/<translation>/.<book>.<chapter>.txt"
        val parts = path.split('/', limit = 3)
        val translation = parts[1]
        val reader = generatedReaderFor(translation)

        val bytes = reader!!.read(path)
        return bytes.decodeToString()
    }

}

internal class TarPtrReader(private val tar: CPointer<UByteVar>?, private val size: Int) {
    private data class Entry(val dataOff: Int, val dataLen: Int)
    private val cache = HashMap<String, Entry>(64)

    private fun u8(ptr: CPointer<UByteVar>, index: Int): Int = ((ptr + index)!!).pointed.value.toInt() and 0xFF

    fun read(path: String): ByteArray {
        val base = tar ?: error("Embedded TAR pointer is null")
        cache[path]?.let { e -> return slice(base, e.dataOff, e.dataLen)}

        var p = 0
        while (p + 512 <= size) {
            if(isZeroBlock(base, p)) break
            val name = readCString(base, p + 0, 100)
            val fileSize = parseOctal(base, p + 124, 12)
            val typeFlag = u8(base, p + 156)
            val dataOff = p + 512

            if(name == path && (typeFlag == 0 || typeFlag == '0'.code)){
                val e = Entry(dataOff, fileSize)
                cache[path] = e
                return slice(base, e.dataOff, e.dataLen)
            }
            p += 512 + roundUp512(fileSize)
        }
        error("Embedded resource not found in TAR: $path")
    }

    private fun slice(ptr: CPointer<UByteVar>, off: Int, len: Int): ByteArray {
        val out = ByteArray(len)
        for(i in 0 until len) out[i] = ((ptr + off + i)!!).pointed.value.toByte()
        return out
    }

    private fun isZeroBlock(ptr: CPointer<UByteVar>, off: Int): Boolean {
        for (i in 0 until 512) if(u8(ptr, off + i) != 0) return false
        return true
    }

    private fun readCString(ptr: CPointer<UByteVar>, off: Int, len: Int): String {
        val sb = StringBuilder(len)
        var i = 0
        while (i < len) {
            val b = u8(ptr, off + i)
            if (b == 0) break
            sb.append(b.toChar()); i++
        }
        return sb.toString()
    }

    private fun parseOctal(ptr: CPointer<UByteVar>, off: Int, len: Int): Int {
        var i = off
        val end = off + len
        while(i < end) {
            val b = u8(ptr, i)
            if(b != 0 && b != ' '.code) break
            i++
        }
        var v = 0
        while(i < end) {
            val b = u8(ptr, i)
            if(b < '0'.code || b > '7'.code) break
            v = (v shl 3) + (b - '0'.code)
            i++
        }
        return v
    }

    private fun roundUp512(n: Int): Int {
        val rem = n and 511
        return if(rem == 0) n else n + (512 - rem)
    }

}
