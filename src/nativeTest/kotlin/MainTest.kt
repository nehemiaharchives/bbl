import kotlin.test.Test
import kotlin.test.assertEquals

class MainTest {

    @Test
    fun gen1Test(){
        assertEquals(parseBook("genesis"), 1)
    }
}