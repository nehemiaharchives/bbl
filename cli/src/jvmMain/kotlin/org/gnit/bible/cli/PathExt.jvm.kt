package org.gnit.bible.cli

import okio.Path
import okio.Path.Companion.toPath

actual fun currentDir(): Path = System.getProperty("user.dir").toPath()
