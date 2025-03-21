package com.monzo.web_crawler.crawler.controller;

import com.monzo.web_crawler.crawler.model.Page;
import lombok.Getter;

import java.util.List;

@Getter
public class CrawlResponse {

    private final int pageCount;
    private final List<Page> pages;

    public CrawlResponse(List<Page> crawledPages) {
        this.pages = crawledPages;
        this.pageCount = crawledPages.size();
    }
}
