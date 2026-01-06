package org.gnit.bible.cli

import okio.Path

/**
 * :cli defines this helper, but :cli:packer is a separate module so we re-declare it here.
 */
expect fun currentDir(): Path

