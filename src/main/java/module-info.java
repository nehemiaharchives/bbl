module org.gnit.bible {
    requires com.github.ajalt.clikt;
    requires jdk.crypto.cryptoki;
    requires ktor.http.jvm;
    requires ktor.client.okhttp.jvm;
    requires ktor.client.core.jvm;
    requires kotlin.stdlib;
    requires kotlinx.coroutines.core.jvm;
    requires kotlinx.serialization.json;
    requires kotlinx.serialization.core;
    requires kotlin.test;
    requires org.slf4j;
    requires org.tinylog.api.slf4j;
    requires org.tinylog.api;
    requires org.tinylog.impl;
    exports org.gnit.bible;
}