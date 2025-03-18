package com.monzo.web_crawler.crawler.service;

import com.monzo.web_crawler.crawler.model.UrlNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;


public class Crawler {

    private static final Logger logger = LoggerFactory.getLogger(Crawler.class);

    private final WebService webService;

    private final Queue<UrlNode> workQueue = new ConcurrentLinkedQueue<>();
    private final URI domain;

    public Crawler(WebService webService, URI domain) {
        this.webService = webService;
        this.domain = domain;
    }

    public UrlNode crawl() {
        UrlNode root = new UrlNode(domain, null);
        workQueue.add(root);

        while (!workQueue.isEmpty()) {
            processNestedUrl(workQueue.poll());
        }

        return root;
    }

    private void processNestedUrl(UrlNode currentUrl) {
        logger.debug("Processing url {}", currentUrl.getUrl());
        Document doc;
        String path = currentUrl.getUrl().toString();
        try {
            doc = webService.getDocument(path);
        } catch (Exception e) {
            logger.error("Failed to fetch document from url {}", path, e);
            return;
        }

        Elements links = doc.select("a[href]");

        for (Element link : links) {
            URI uri = createUri(currentUrl.getUrl(), link);

            if (uri != null) {
                UrlNode urlNode = new UrlNode(uri, currentUrl);

                if (currentUrl.hasAncestor(uri)) {
                    logger.debug("Skipping url {} as it is already in node's ancestry", uri);
                } else if (currentUrl.containsChild(uri)) {
                    logger.debug("Skipping url {} as it is already in node's children", uri);
                } else {
                    logger.debug("Adding url {} to work queue", uri);
                    workQueue.add(urlNode);
                }

                if (currentUrl.containsChild(urlNode.getUrl())) {
                    logger.debug("Skipping url node {} as it is already in node's children", urlNode.getUrl());
                } else {
                    logger.debug("Adding url node {} as child to node {}", uri, currentUrl.getUrl());
                    currentUrl.addChild(urlNode);
                }
            }
        }

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
