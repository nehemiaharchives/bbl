package org.gnit.bible.test

import org.gnit.bible.Platform

expect abstract class ResourcesTestBase(){
    fun createTestPlatform(): Platform
}
