package org.gnit.bible

// commenting out so that nativeMain Platform.native.kt to work
import platform.UIKit.UIDevice
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSUserDomainMask
import platform.Foundation.NSHomeDirectory

class IOSPlatform: Platform() {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
    override val packDir: String by lazy {
        val paths = NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true)
        val base = (paths.firstOrNull() as? String) ?: NSHomeDirectory()
        "$base/$bblDir/$packBaseDir"
    }

}

actual fun getPlatform(platformContext: Any?): Platform = IOSPlatform()
