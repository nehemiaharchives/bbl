package org.gnit.bible

import java.io.File
import java.io.IOException

interface ResourceReader {
    fun readText(path: String): String
    fun readBytes(path: String): ByteArray
}

class RunInIDEResourceReader : ResourceReader {
    override fun readText(path: String): String {
        //path = "texts/webus/webus.9.9.txt"
        return File(ClassLoader.getSystemResource(path).file).readText().replace("\r\n", "\n")
    }

    override fun readBytes(path: String): ByteArray {
        return File(ClassLoader.getSystemResource(path).file).readBytes()
    }
}

class ProdResourceReader : ResourceReader {
    override fun readText(path: String): String {
        val inputStream = object {}.javaClass.getResourceAsStream("/$path")

        if (inputStream != null) {
            return inputStream.reader(Charsets.UTF_8).readText().replace("\r\n", "\n")
        } else {
            throw IOException("$path was null")
        }
    }

    override fun readBytes(path: String): ByteArray {
        val bytes = object {}.javaClass.getResourceAsStream("/$path").use { it?.readBytes() }

        if (bytes != null) {
            return bytes
        } else {
            throw IOException("$path was null")
        }
    }
}

fun getResourceReader() = if (System.getenv("RUN_IN_IDE") == "TRUE") RunInIDEResourceReader() else ProdResourceReader()
