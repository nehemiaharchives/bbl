package org.gnit.bible

import kotlin.test.Test
import com.goncalossilva.resources.Resource
import kotlin.test.assertTrue

class ZipTest {

    @Test
    fun testZipExistInResources(){
        assertTrue(Resource("kttv.zip").exists())
    }
}
