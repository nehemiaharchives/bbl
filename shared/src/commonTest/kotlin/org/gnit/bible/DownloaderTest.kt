package org.gnit.bible

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DownloaderTest {

    @Test
    fun testDownload() {
        // val httpClient = createPlatformHttpClient()
        // this will create real HttpClient which makes real network access and cert processing which fails in iOS simulator
        // replacing with MockHttpClient

        val mockEngine = MockEngine { request ->
            val url = request.url
            val isGithubRaw = url.host == "raw.githubusercontent.com"
            val isZip = url.encodedPath.endsWith("/kttv.zip")
            if (isGithubRaw && isZip) {
                // Load bytes from test resources and respond as if from raw.githubusercontent.com
                val bytes = buildMinimalZip(listOf("kttv.1.1.txt" to genesisOneKttv))
                respond(
                    content = bytes,
                    headers = headersOf(
                        "Content-Type" to listOf("application/zip"),
                        "Content-Length" to listOf(bytes.size.toString())
                    )
                )
            } else {
                respond(
                    content = ByteArray(0),
                    headers = headersOf(
                        "Content-Type" to listOf("text/plain")
                    )
                )
            }
        }

        val httpClient = HttpClient(mockEngine)
        val platform = getPlatform()
        val am = AssetManager(httpClient, platform)
        val fileName = "kttv.zip"
        val baseUrl =
            "https://raw.githubusercontent.com/nehemiaharchives/bbl-kmp/refs/heads/master/shared/src/commonTest/resources/data/"
        am.download(baseUrl, fileName)
        assertNotNull(am.listDownloadedPacks().map { it.name }.find { it.endsWith("kttv.zip") })
    }

    private fun buildMinimalZip(entries: List<Pair<String, String>>): ByteArray {
        val out = Buffer()
        val central = mutableListOf<CentralRecord>()
        var offset = 0

        for ((name, content) in entries) {
            val data = content.encodeToByteArray()
            val crc = crc32(data)

            val localHeader = ByteArrayWriter()
            localHeader.leInt(0x04034b50) // local header sig
            localHeader.leShort(20)       // version needed
            localHeader.leShort(0)        // flags
            localHeader.leShort(0)        // method=store
            localHeader.leShort(0)        // time
            localHeader.leShort(0)        // date
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
            ce.leShort(0)        // time
            ce.leShort(0)        // date
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

private val genesisOneKttv = """
    1 Ban đầu Đức Chúa Trời dựng nên trời đất.
    2 Vả, đất là vô-hình và trống không, sự mờ-tối ở trên mặt vực; Thần Đức Chúa Trời vận-hành trên mặt nước.
    3 Đức Chúa Trời phán rằng: Phải có sự sáng; thì có sự sáng.
    4 Đức Chúa Trời thấy sáng là tốt-lành, bèn phân sáng ra cùng tối.
    5 Đức Chúa Trời đặt tên sự sáng là ngày; sự tối là đêm. Vậy, có buổi chiều và buổi mai; ấy là ngày thứ nhứt.
    6 Đức Chúa Trời lại phán rằng: Phải có một khoảng không ở giữa nước đặng phân-rẽ nước cách với nước.
    7 Ngài làm nên khoảng không, phân-rẽ nước ở dưới khoảng không cách với nước ở trên khoảng không; thì có như vậy.
    8 Đức Chúa Trời đặt tên khoảng không là trời. Vậy, có buổi chiều và buổi mai; ấy là ngày thứ nhì.
    9 Đức Chúa Trời lại phán rằng: Những nước ở dưới trời phải tụ lại một nơi, và phải có chỗ khô-cạn bày ra; thì có như vậy.
    10 Đức Chúa Trời đặt tên chỗ khô-cạn là đất, còn nơi nước tụ lại là biển. Đức Chúa Trời thấy điều đó là tốt-lành.
    11 Đức Chúa Trời lại phán rằng: Đất phải sanh cây-cỏ; cỏ kết hột giống, cây-trái kết quả, tùy theo loại mà có hột giống trong mình trên đất; thì có như vậy.
    12 Đất sanh cây-cỏ: Cỏ kết hột tùy theo loại, cây kết quả có hột trong mình, tùy theo loại. Đức Chúa Trời thấy điều đó là tốt-lành.
    13 Vậy, có buổi chiều và buổi mai; ấy là ngày thứ ba.
    14 Đức Chúa Trời lại phán rằng: Phải có các vì sáng trong khoảng-không trên trời, đặng phân ra ngày với đêm, và dùng làm dấu để định thì-tiết, ngày và năm;
    15 lại dùng làm vì sáng trong khoảng không trên trời để soi xuống đất; thì có như vậy.
    16 Đức Chúa Trời làm nên hai vì sáng lớn; vì lớn hơn để cai-trị ban ngày, vì nhỏ hơn để cai-trị ban đêm; Ngài cũng làm các ngôi sao.
    17 Đức Chúa Trời đặt các vì đó trong khoảng không trên trời, đặng soi sáng đất,
    18 đặng cai-trị ban ngày và ban đêm, đặng phân ra sự sáng với sự tối. Đức Chúa Trời thấy điều đó là tốt-lành.
    19 Vậy, có buổi chiều và buổi mai; ấy là ngày thứ tư.
    20 Đức Chúa Trời lại phán rằng: Nước phải sanh các vật sống cho nhiều, và các loài chim phải bay trên mặt đất trong khoảng không trên trời.
    21 Đức Chúa Trời dựng nên các loài cá lớn, các vật sống hay động nhờ nước mà sanh nhiều ra, tùy theo loại, và các loài chim hay bay, tùy theo loại. Đức Chúa Trời thấy điều đó là tốt-lành.
    22 Đức Chúa Trời ban phước cho các loài đó mà phán rằng: Hãy sanh-sản, thêm nhiều, làm cho đầy-dẫy dưới biển; còn các loài chim hãy sanh-sản trên đất cho nhiều.
    23 Vậy, có buổi chiều và buổi mai; ấy là ngày thứ năm.
    24 Đức Chúa Trời lại phán rằng: Đất phải sanh các vật sống tùy theo loại, tức súc-vật, côn-trùng, và thú rừng, đều tùy theo loại; thì có như vậy.
    25 Đức Chúa Trời làm nên các loài thú rừng tùy theo loại, súc-vật tùy theo loại, và các côn-trùng trên đất tùy theo loại. Đức Chúa Trời thấy điều đó là tốt-lành.
    26 Đức Chúa Trời phán rằng: Chúng ta hãy làm nên loài người như hình ta và theo tượng ta, đặng quản-trị loài cá biển, loài chim trời, loài súc-vật, loài côn-trùng bò trên mặt đất, và khắp cả đất.
    27 Đức Chúa Trời dựng nên loài người như hình Ngài; Ngài dựng nên loài người giống như hình Đức Chúa Trời; Ngài dựng nên người nam cùng người nữ.
    28 Đức Chúa Trời ban phước cho loài người và phán rằng: Hãy sanh-sản, thêm nhiều, làm cho đầy-dẫy đất; hãy làm cho đất phục-tùng, hãy quản-trị loài cá dưới biển, loài chim trên trời cùng các vật sống hành-động trên mặt đất.
    29 Đức Chúa Trời lại phán rằng: Nầy, ta sẽ ban cho các ngươi mọi thứ cỏ kết hột mọc khắp mặt đất, và các loài cây sanh quả có hột giống; ấy sẽ là đồ-ăn cho các ngươi.
    30 Còn các loài thú ngoài đồng, các loài chim trên trời, và các động-vật khác trên mặt đất, phàm giống nào có sự sống thì ta ban cho mọi thứ cỏ xanh đặng dùng làm đồ-ăn; thì có như vậy.
    31 Đức Chúa Trời thấy các việc Ngài đã làm thật rất tốt-lành. Vậy, có buổi chiều và buổi mai; ấy là ngày thứ sáu.
""".trimIndent()
