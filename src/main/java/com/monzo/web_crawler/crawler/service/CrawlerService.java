package com.monzo.web_crawler.crawler.service;

import com.monzo.web_crawler.crawler.model.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A service responsible for managing the crawling of web pages starting from a root URI.
 * The service uses a configurable thread pool size, worker timeout, and manager timeout
 * to manage the crawling process in an asynchronous manner.
 * <p>
 * This class interacts with the {@link WebService} to fetch and crawl web pages.
 */
@Service
public class CrawlerService {
    private static final Logger logger = LoggerFactory.getLogger(CrawlerService.class);

    private final WebService webService;

    private final int threadPoolSize;
    private final int crawlerWorkerTimeout;
    private final int crawlerManagerTimeout;

    public CrawlerService(WebService webService,
                          @Value("${crawler.thread-pool-size:5}") int threadPoolSize,
                          @Value("${crawler.worker-timeout-seconds:2}") int crawlerWorkerTimeout,
                          @Value("${crawler.manager-timeout-seconds:120}") int crawlerManagerTimeout) {
        this.webService = webService;
        this.threadPoolSize = threadPoolSize;
        this.crawlerWorkerTimeout = crawlerWorkerTimeout;
        this.crawlerManagerTimeout = crawlerManagerTimeout;
    }

    public List<Page> crawl(URI rootPage) {
        long startTime = System.currentTimeMillis();
        List<Page> result = new ArrayList<>();

        CrawlerManager crawlerManager = new CrawlerManager(webService, threadPoolSize, crawlerWorkerTimeout);
        CompletableFuture<List<Page>> future = CompletableFuture.supplyAsync(() -> crawlerManager.crawl(rootPage));

        try {
            result = future.get(crawlerManagerTimeout, TimeUnit.SECONDS);
            long endTime = System.currentTimeMillis();
            logger.debug("Crawling completed in {} ms", (endTime - startTime));
        } catch (TimeoutException e) {
            logger.error("Task did not complete within {} seconds", crawlerManagerTimeout);
            future.cancel(true);
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Task failed", e);
        }

        return result;
    }


}
