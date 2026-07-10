# lucene-kmp has runtime codec and index-reader relationships that R8 cannot
# currently infer. Optimizing or removing these classes corrupts stored-field
# reads in minified builds, while the same index works in unminified builds.
-keep class org.gnit.lucenekmp.** { *; }
