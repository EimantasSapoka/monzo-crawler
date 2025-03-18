package com.monzo.web_crawler.crawler.service;

import com.monzo.web_crawler.crawler.model.UrlNode;
import org.springframework.stereotype.Service;

import java.net.URI;

@Service
public class CrawlerService {

    private final WebService webService;

    public CrawlerService(WebService webService) {
        this.webService = webService;
    }

    public UrlNode crawl(URI domain) {
        Crawler crawler = new Crawler(webService);
        return crawler.crawl(domain);
    }
}
