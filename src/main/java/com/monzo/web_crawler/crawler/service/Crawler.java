package com.monzo.web_crawler.crawler.service;

import com.monzo.web_crawler.crawler.model.NestedUrl;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;


public class Crawler {

    private static final Logger logger = LoggerFactory.getLogger(Crawler.class);

    private final WebService webService;

    private final Queue<NestedUrl> workQueue = new ConcurrentLinkedQueue<>();

    public Crawler(WebService webService) {
        this.webService = webService;
    }

    public NestedUrl crawl(URI domain) {
        NestedUrl root = new NestedUrl(domain);
        workQueue.add(root);

        NestedUrl currentUrl;
        Document doc;
        while (!workQueue.isEmpty()) {
            currentUrl = workQueue.poll();
            String path = currentUrl.getUrl().getPath();
            try {
                doc = webService.getDocument(path);
                Elements links = doc.select("a[href]");

                for (Element link : links) {
                    String href = link.attr("href");
                    NestedUrl nestedUrl = new NestedUrl(URI.create(href));
                    currentUrl.addChild(nestedUrl);

                    // add it to the work queue be crawled
                    workQueue.add(nestedUrl);
                }
            } catch (Exception e) {
                logger.error("Failed to fetch document from url {}", path, e);
            }
        }

        return root;
    }
}
