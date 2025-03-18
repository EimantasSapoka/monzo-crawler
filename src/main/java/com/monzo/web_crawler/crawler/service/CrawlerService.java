package com.monzo.web_crawler.crawler.service;

import com.monzo.web_crawler.crawler.model.NestedUrl;
import org.springframework.stereotype.Service;

import java.net.URI;

@Service
public class CrawlerService {

    private final WebService webService;

    public CrawlerService(WebService webService) {
        this.webService = webService;
    }

    public NestedUrl crawl(URI domain) {
        Crawler crawler = new Crawler(webService);
        return crawler.crawl(domain);
    }
}
