module org.gnit.bible {
    requires com.github.ajalt.clikt;
    requires jdk.crypto.cryptoki;
    requires ktor.http.jvm;
    requires ktor.client.okhttp.jvm;
    requires ktor.client.core.jvm;
    requires kotlin.stdlib;
    requires kotlinx.coroutines.core.jvm;
    exports org.gnit.bible;
}