module org.gnit.bible {
    requires com.github.ajalt.clikt;
    requires kotlin.stdlib;
    requires kotlin.test;
    requires kotlinx.serialization.core;
    requires kotlinx.serialization.json;
    requires org.slf4j;
    requires org.tinylog.api.slf4j;
    requires org.tinylog.api;
    requires org.tinylog.impl;
    requires org.apache.lucene.core;
    requires org.apache.lucene.queryparser;
    requires org.apache.lucene.analysis.smartcn;
    requires org.apache.lucene.analysis.nori;
    requires org.apache.lucene.analysis.kuromoji;
    requires jimfs;
    requires kotlin.stdlib.jdk7;
    requires jdk.management;
    requires com.sun.jna;
    exports org.gnit.bible;
}