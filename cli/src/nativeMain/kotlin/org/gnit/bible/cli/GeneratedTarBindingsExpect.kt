package org.gnit.bible.cli

/**
 * Expect declaration for nativeMain used by platform-specific generated actuals.
 *
 * The actual implementation is generated at build time by the `generateTarBindingsKt` task into:
 * - build/generated/cli/org/gnit/bible/cli/GeneratedTarBindings.kt
 *
 * This function is called from CliBibleResourcestReader.readBytes(). Do not remove this expect unless
 * you also refactor the call site or replace the generation/wiring mechanism.
 */
internal expect fun generatedReaderFor(translation: String): TarPtrReader?
