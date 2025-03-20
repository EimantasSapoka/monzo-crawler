package com.monzo.web_crawler.crawler.service;

import com.monzo.web_crawler.crawler.model.UrlNode;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class CrawlerService {

    private final WebService webService;


    public CrawlerService(WebService webService) {
        this.webService = webService;
    }

    public UrlNode crawl(URI domain) {
        UrlNode root = new UrlNode(domain, null);

        // setup thread safe collections to allow for parallel crawling
        Queue<UrlNode> workQueue = new ConcurrentLinkedQueue<>(); // to keep adding new pages to crawl
        Map<String, UrlNode> visitedUrls = new ConcurrentHashMap<>(); // to keep track of what pages have been crawled already to prevent double work

        // add the root page to work queue
        workQueue.add(root);

        while (!workQueue.isEmpty()) {
            Crawler crawler = new Crawler(webService, workQueue, visitedUrls, domain);
            crawler.crawl(workQueue.poll());
        }

        return root;
    }
}
