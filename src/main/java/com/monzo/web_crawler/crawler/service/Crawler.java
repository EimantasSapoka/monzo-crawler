package com.monzo.web_crawler.crawler.service;

import com.monzo.web_crawler.crawler.model.NestedUrl;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;


public class Crawler {

    private static final Logger logger = LoggerFactory.getLogger(Crawler.class);

    private final WebService webService;

    private final Queue<NestedUrl> workQueue = new ConcurrentLinkedQueue<>();
    private final Map<String, String> visitedPaths = new HashMap<>();

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
            String path = currentUrl.getUrl().toString();
            try {
                doc = webService.getDocument(path);
                Elements links = doc.select("a[href]");

                for (Element link : links) {
                    URI uri = createUri(currentUrl.getUrl(), link);

                    if (uri != null) {
                        NestedUrl nestedUrl = new NestedUrl(uri);

                        // add it to the work queue be crawled
                        if (!visitedPaths.containsKey(uri.toString())) {
                            visitedPaths.put(uri.toString(), uri.toString());
                            currentUrl.addChild(nestedUrl);
                            workQueue.add(nestedUrl);
                        }
                    }


                }
            } catch (Exception e) {
                logger.error("Failed to fetch document from url {}", path, e);
            }
        }

        return root;
    }

    private static URI createUri(URI basePath, Element link) {
        String href = link.attr("href");

        // to handle relative paths like ./document.html
        if (href.startsWith(".")) {
            href = href.substring(1);
        }
        URI uri = URI.create(href);

        if (!uri.isAbsolute()) {
            logger.trace("Adding relative path {} to base path {}", href, basePath);
            uri = URI.create(basePath.toString() + href);
        }

        if (!StringUtils.equals(uri.getScheme(), "http") && !StringUtils.equals(uri.getScheme(), "https")) {
            logger.debug("Skipping non-http/https url {}", uri);
            return null;
        }

        try {
            return new URIBuilder()
                    .setScheme(uri.getScheme())
                    .setHost(uri.getHost())
                    .setPath(uri.getPath())
                    .build();
        } catch (URISyntaxException e) {
            logger.error("Failed to create clean URI from {}", uri, e);
            return null;
        }
    }
}
