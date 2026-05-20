package org.gnit.bible

fun resolveCategoryFilterOrThrow(
    rawKey: String,
    errorFactory: (String) -> Throwable
): BibleFilter {
    return categoryFilterOrNull(rawKey)
        ?: throw errorFactory(rawKey)
}

fun resolveCategoryFiltersOrThrow(
    rawKeys: Iterable<String>,
    errorFactory: (String) -> Throwable
): List<BibleFilter> {
    return rawKeys.map { rawKey -> resolveCategoryFilterOrThrow(rawKey, errorFactory) }
}
