package com.monzo.web_crawler.crawler.service;

import com.monzo.web_crawler.crawler.model.UrlNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class CrawlerService {
    private static final Logger logger = LoggerFactory.getLogger(CrawlerService.class);

    private final WebService webService;

    private final int threadPoolSize;
    private final int crawlTimeoutMinutes;


    public CrawlerService(WebService webService,
                          @Value("${crawler.thread-pool-size:5}") int threadPoolSize,
                          @Value("${crawler.crawl-timeout-minutes:10}") int crawlTimeoutMinutes) {
        this.webService = webService;
        this.threadPoolSize = threadPoolSize;
        this.crawlTimeoutMinutes = crawlTimeoutMinutes;
    }

    public UrlNode crawl(URI domain) {
        UrlNode root = new UrlNode(domain, null);

        // setup thread safe collections to allow for parallel crawling
        LinkedBlockingQueue<UrlNode> workQueue = new LinkedBlockingQueue<>(); // to keep adding new pages to crawl
        Map<String, UrlNode> visitedUrls = new ConcurrentHashMap<>(); // to keep track of what pages have been crawled already to prevent double work
        AtomicInteger activeWorkerCount = new AtomicInteger(0);

        // add the root page to work queue
        workQueue.add(root);

        // a latch that will force main thread to wait until all threads are finished
        CountDownLatch latch = new CountDownLatch(threadPoolSize);

        try (ExecutorService pool = Executors.newFixedThreadPool(threadPoolSize)) {

            // start thread workers that will crawl pages
            for (int i = 0; i < threadPoolSize; i++) {

                // each worker will stay alive until there are items in the queue OR there are other workers still crawling
                // if neither is true they will finish
                // some extra error handling in case it can't get item within certain time or in case it gets null from queue for whatever reason
                pool.execute(() -> {
                    try {
                        while (!workQueue.isEmpty() || activeWorkerCount.get() > 0) {
                            UrlNode urlToCrawl;
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
                            Crawler crawler = new Crawler(webService, workQueue, visitedUrls, domain);
                            activeWorkerCount.incrementAndGet();
                            crawler.crawl(urlToCrawl);
                            activeWorkerCount.decrementAndGet();
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }

            // Await termination to ensure all tasks complete before returning root
            try {
                latch.await(); // main thread waits until all threads are finished
                pool.shutdown();
                boolean finishedSuccessfully = pool.awaitTermination(crawlTimeoutMinutes, TimeUnit.MINUTES);
                if (!finishedSuccessfully) {
                    logger.error("Thread pool did not terminate within expected time of {} minutes", crawlTimeoutMinutes);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Thread pool interrupted during execution", e);
            }
        }

        return root;
    }
}
