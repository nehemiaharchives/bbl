package org.gnit.bible.cli

import okio.Path.Companion.toPath
import okio.Path

actual fun currentDir(): Path = System.getProperty("user.dir").toPath()
