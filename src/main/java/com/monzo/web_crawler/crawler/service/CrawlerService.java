package com.monzo.web_crawler.crawler.service;

import com.monzo.web_crawler.crawler.model.UrlNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;


@Service
public class CrawlerService {
    private static final Logger logger = LoggerFactory.getLogger(CrawlerService.class);

    private final WebService webService;

    private final int threadPoolSize;


    public CrawlerService(WebService webService,
                          @Value("${crawler.thread-pool-size:5}") int threadPoolSize) {
        this.webService = webService;
        this.threadPoolSize = threadPoolSize;
    }

    public UrlNode crawl(URI domain) {
        long startTime = System.currentTimeMillis(); // Start time for performance monitoring
        UrlNode root = new UrlNode(domain, null);

        // setup thread safe collections to allow for parallel crawling
        LinkedBlockingQueue<UrlNode> workQueue = new LinkedBlockingQueue<>(); // to keep adding new pages to crawl

        // to keep track of what pages have been crawled already to prevent double work
        Map<String, UrlNode> visitedUrls = new ConcurrentHashMap<>();

        // to keep track of urls that have been seen. Due to multithreaded nature a url could have been queued for work and no longer in work queue
        // but still not processed and not appear in visitedUrls map
        Set<String> seenUrls = new ConcurrentSkipListSet<>();

        AtomicInteger jobCount = new AtomicInteger(0); // to keep track of workers actively crawling

        // add the root page to work queue
        workQueue.add(root);

        try (ExecutorService pool = Executors.newFixedThreadPool(threadPoolSize)) {

            // the main thread will keep polling the queue while there are urls to crawl OR there are workers still crawling
            // it creates a crawler for each page that needs crawling and submits to the thread pool
            while (!workQueue.isEmpty() || jobCount.get() > 0) {
                UrlNode urlToCrawl;
                logger.info("Active workers: {}. Work queue size: {}", jobCount.get(), workQueue.size());
                try {
                    urlToCrawl = workQueue.poll(1, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    logger.warn("Thread interrupted while waiting for work queue to poll");
                    continue;
                }
                if (Objects.isNull(urlToCrawl)) {
                    logger.warn("Thread received null url to crawl");
                    continue;
                }

                jobCount.incrementAndGet();
                pool.submit(() -> CompletableFuture.supplyAsync(() -> {
                    try {
                        Crawler crawler = new Crawler(webService, visitedUrls, seenUrls, domain);
                        List<UrlNode> newNodes = crawler.crawl(urlToCrawl);
                        workQueue.addAll(newNodes);
                    } finally {
                        jobCount.decrementAndGet();
                    }
                    return null;
                }).get(3, TimeUnit.SECONDS));
            }

            // Await termination to ensure all tasks complete before returning root
            try {
                logger.info("All threads finished, shutting down thread pool");
                pool.shutdown();
                boolean finishedSuccessfully = pool.awaitTermination(1, TimeUnit.SECONDS);
                if (!finishedSuccessfully) {
                    logger.error("Thread pool did not terminate within expected time of {} seconds", 1);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Thread pool interrupted during execution", e);
            }
        }

        long endTime = System.currentTimeMillis(); // End time for performance monitoring
        logger.info("Crawling completed in {} ms", (endTime - startTime));

        return root;
    }
}
