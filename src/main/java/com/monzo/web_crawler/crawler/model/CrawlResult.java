package com.monzo.web_crawler.crawler.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@RequiredArgsConstructor
public class CrawlResult {

    private final String url;
    private final List<CrawlResult> children;

    public CrawlResult(String url) {
        this(url, new ArrayList<>());
    }
}
