package org.gnit.bible.test

object ZipUtil {

    fun buildMinimalZip(entries: List<Pair<String, String>>): ByteArray {
        val out = Buffer()
        val central = mutableListOf<CentralRecord>()
        var offset = 0

        // Set a valid MS-DOS date (2020-01-01) and time (00:00:00)
        val msDosTime = 0 // 00:00:00
        val msDosDate = (2020 - 1980 shl 9) or (1 shl 5) or 1 // year=2020, month=1, day=1

        for ((name, content) in entries) {
            val data = content.encodeToByteArray()
            val crc = crc32(data)

            val localHeader = ByteArrayWriter()
            localHeader.leInt(0x04034b50) // local header sig
            localHeader.leShort(20)       // version needed
            localHeader.leShort(0)        // flags
            localHeader.leShort(0)        // method=store
            localHeader.leShort(msDosTime) // time
            localHeader.leShort(msDosDate) // date
            localHeader.leInt(crc)
            localHeader.leInt(data.size)  // comp size
            localHeader.leInt(data.size)  // uncomp size
            localHeader.leShort(name.length)
            localHeader.leShort(0)        // extra len
            localHeader.ascii(name)
            out.write(localHeader.bytes())
            out.write(data)

            central += CentralRecord(
                name = name,
                crc = crc,
                size = data.size,
                offset = offset,
            )
            offset += localHeader.size() + data.size
        }

        val centralStart = offset
        var centralLen = 0
        for (c in central) {
            val ce = ByteArrayWriter()
            ce.leInt(0x02014b50) // central header sig
            ce.leShort(20)       // version made by
            ce.leShort(20)       // version needed
            ce.leShort(0)        // flags
            ce.leShort(0)        // method=store
            ce.leShort(msDosTime) // time
            ce.leShort(msDosDate) // date
            ce.leInt(c.crc)
            ce.leInt(c.size)
            ce.leInt(c.size)
            ce.leShort(c.name.length)
            ce.leShort(0) // extra
            ce.leShort(0) // comment
            ce.leShort(0) // disk number
            ce.leShort(0) // internal attrs
            ce.leInt(0)   // external attrs
            ce.leInt(c.offset)
            ce.ascii(c.name)
            out.write(ce.bytes())
            centralLen += ce.size()
        }

        val eocd = ByteArrayWriter()
        eocd.leInt(0x06054b50) // EOCD sig
        eocd.leShort(0)        // disk
        eocd.leShort(0)        // start disk
        eocd.leShort(central.size)
        eocd.leShort(central.size)
        eocd.leInt(centralLen)
        eocd.leInt(centralStart)
        eocd.leShort(0)        // comment
        out.write(eocd.bytes())

        return out.toByteArray()
    }

    private data class CentralRecord(val name: String, val crc: Int, val size: Int, val offset: Int)

    private class ByteArrayWriter {
        private val buf = mutableListOf<Byte>()
        fun leInt(v: Int) = write(v, 4)
        fun leShort(v: Int) = write(v, 2)
        private fun write(v: Int, n: Int) {
            var x = v
            repeat(n) {
                buf += (x and 0xff).toByte()
                x = x ushr 8
            }
        }
        fun ascii(s: String) { s.encodeToByteArray().forEach { buf += it } }
        fun bytes(): ByteArray = buf.toByteArray()
        fun size(): Int = buf.size
    }

    private class Buffer {
        private val out = ArrayList<Byte>(4096)
        fun write(b: ByteArray) { for (x in b) out += x }
        fun toByteArray(): ByteArray = out.toByteArray()
    }


    // Simple CRC32 (IEEE 802.3)
    private fun crc32(data: ByteArray): Int {
        var crc = -1
        for (b in data) {
            var c = (crc xor (b.toInt() and 0xff))
            repeat(8) {
                c = if ((c and 1) != 0) (c ushr 1) xor 0xEDB88320.toInt() else (c ushr 1)
            }
            crc = c
        }
        return crc xor -1
    }
}