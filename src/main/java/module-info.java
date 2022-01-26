module org.gnit.bible {
    requires com.github.ajalt.clikt;
    requires jdk.crypto.cryptoki;
    requires ktor.client.okhttp.jvm;
    requires ktor.client.core.jvm;
    requires kotlin.stdlib;
    exports org.gnit.bible;
}