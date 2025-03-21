package com.monzo.web_crawler.crawler.service;

import com.monzo.web_crawler.crawler.model.Page;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The CrawlerManager class is responsible for managing the web crawling process.
 * It uses multithreading to crawl web pages in parallel, collect data about the pages,
 * and efficiently manage the queue of URLs to be processed.
 * Crawling is restricted to pages within the same host domain as the root page.
 * <p>
 * It maintains a thread-safe work queue for URLs to be processed, a list of processed pages,
 * and a set of seen URLs for tracking already-queued or processed URLs.
 * The class utilizes a thread pool for concurrent processing of pages.
 */
public class CrawlerManager {

    private static final Logger logger = LoggerFactory.getLogger(CrawlerManager.class);

    private final WebService webService;
    private final int threadPoolSize;


    private final List<Page> processedPages = new CopyOnWriteArrayList<>();

    // setup thread safe work queue to allow for parallel crawling
    private final LinkedBlockingQueue<URI> workQueue = new LinkedBlockingQueue<>();

    // to keep track of urls that have been seen. Due to multithreaded nature url could have been queued for work and no longer in work queue
    // this keeps track of urls that have been seen in general
    private final Map<URI, URI> seenUrls = new ConcurrentHashMap<>();

    // to keep track of amount of jobs remaining / total / failed / etc for statistics
    private final AtomicInteger remainingJobCount = new AtomicInteger(0);
    private final AtomicInteger totalJobCount = new AtomicInteger(0);
    private final AtomicInteger jobCompletions = new AtomicInteger(0);
    private final AtomicInteger failedJobCount = new AtomicInteger(0);

    private final int crawlerWorkerTimeout;

    public CrawlerManager(WebService webService, int threadPoolSize, int crawlerWorkerTimeout) {
        this.webService = webService;
        this.threadPoolSize = threadPoolSize;
        this.crawlerWorkerTimeout = crawlerWorkerTimeout;
    }

    /**
     * Crawls the given root page and returns a list of all discovered pages
     * belonging to the same host domain as the root page. The method processes
     * the URLs iteratively using a thread pool and ensures only pages within
     * the main host domain are processed.
     *
     * @param rootPage the URI of the root page to start the crawling process from
     * @return a list of pages discovered during the crawling process, each containing its URL and child links
     */
    public List<Page> crawl(URI rootPage) {
        long startTime = System.currentTimeMillis();
        String mainHost = getUrlDomain(rootPage.getHost());
        workQueue.add(rootPage);

        try (ExecutorService pool = Executors.newFixedThreadPool(threadPoolSize)) {

            while (!workQueue.isEmpty() || remainingJobCount.get() > 0) {
                URI urlToCrawl;
                try {
                    urlToCrawl = workQueue.poll(1, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    logger.warn("Thread interrupted while waiting for work queue to poll");
                    continue;
                }
                if (Objects.isNull(urlToCrawl)) {
                    logger.debug("Thread received null url to crawl");
                    continue;
                }

                if (seenUrls.containsKey(urlToCrawl)) {
                    logger.trace("Skipping already seen url {}", urlToCrawl);
                } else if (!StringUtils.equals(getUrlDomain(urlToCrawl.getHost()), mainHost)) {
                    logger.trace("Skipping url {} as it is not within the main host domain {}", urlToCrawl, mainHost);
                } else {
                    int remaining = remainingJobCount.incrementAndGet();
                    int total = totalJobCount.incrementAndGet();
                    printStatus(total, jobCompletions.get(), remaining);
                    seenUrls.put(urlToCrawl, urlToCrawl);
                    pool.submit(() -> createCrawler(urlToCrawl));
                }
            }

            // Await termination to ensure all crawlers complete before returning root
            try {
                logger.info("All threads finished, shutting down thread pool");
                pool.shutdown();
                boolean finishedSuccessfully = pool.awaitTermination(1, TimeUnit.SECONDS);
                if (!finishedSuccessfully) {
                    logger.error("Thread pool did not terminate within expected time of {} seconds", 1);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException("Thread pool interrupted during execution", e);
            }
        }
        long endTime = System.currentTimeMillis();
        logger.info("Crawling completed in {} ms. Processed {} pages. Failed to process {} pages. Unique urls seen {}", (endTime - startTime), processedPages.size(), failedJobCount, seenUrls.size());
        return processedPages;
    }

    /**
     * Creates a crawler task to process the given URL and handle its linked pages.
     *
     * @param urlToCrawl the URL to be crawled
     */
    private void createCrawler(URI urlToCrawl) {
        CompletableFuture<Object> future = CompletableFuture.supplyAsync(() -> {
            Crawler crawler = new Crawler(webService);
            try {
                Page page = crawler.crawl(urlToCrawl);
                processedPages.add(page);
                logger.debug("Processed url {}. Adding URls to work queue: {}", urlToCrawl, page.getChildren());
                workQueue.addAll(page.getChildren());
            } catch (Exception e) {
                logger.debug("Failed to crawl url {}", urlToCrawl, e);
                failedJobCount.incrementAndGet();
            }
            return null;
        });
        try {
            future.get(crawlerWorkerTimeout, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            logger.debug("Crawler for url {} did not complete within {} seconds", urlToCrawl, crawlerWorkerTimeout);
            future.cancel(true);
            failedJobCount.incrementAndGet();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Crawler failed", e);
            failedJobCount.incrementAndGet();
        } finally {
            int completions = jobCompletions.incrementAndGet();
            int remaining = remainingJobCount.decrementAndGet();
            printStatus(totalJobCount.get(), completions, remaining);

        }
    }

    private void printStatus(int totalJobCount, int completions, int remaining) {
        logger.info("Total count: {},\tTotal crawls: {}.\tFailures: {}.\tSuccesses: {}.\tRemaining: {}.", totalJobCount, completions, failedJobCount.get(), processedPages.size(), remaining);
    }

    private static String getUrlDomain(String uri) {
        return uri.startsWith("www.") ? uri.substring(4) : uri;
    }
}
