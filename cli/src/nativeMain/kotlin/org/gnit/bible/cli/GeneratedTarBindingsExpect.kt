package org.gnit.bible.cli

/**
 * Expect declaration for nativeMain used by platform-specific generated actuals.
 *
 * The actual implementation is generated at build time by the `generateTarBindingsKt` task into a
 * target-specific generated source dir (e.g. build/generated/cli-linuxX64Main).
 */
internal expect fun generatedReaderFor(translation: String): TarPtrReader?
