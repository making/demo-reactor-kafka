package com.example.demoreactorkafka;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AccessLog {

    private final String date;

    private final String method;

    private final String path;

    private final String host;

    private final boolean crawler;

    private final String userAgent;

    @JsonCreator
    public AccessLog(@JsonProperty("date") String date, @JsonProperty("method") String method, @JsonProperty("path") String path, @JsonProperty("host") String host, @JsonProperty("crawler") boolean crawler,
                     @JsonProperty("userAgent") String userAgent) {
        this.date = date;
        this.method = method;
        this.path = path;
        this.host = host;
        this.crawler = crawler;
        this.userAgent = userAgent;
    }

    public String getDate() {
        return date;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public boolean isCrawler() {
        return crawler;
    }

    public String getUserAgent() {
        return userAgent;
    }

    @Override
    public String toString() {
        return "AccessLog{" +
            "date='" + date + '\'' +
            ", method='" + method + '\'' +
            ", path='" + path + '\'' +
            ", crawler=" + crawler +
            ", userAgent='" + userAgent + '\'' +
            '}';
    }
}
