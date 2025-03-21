package com.monzo.web_crawler.crawler.service;

import com.monzo.web_crawler.crawler.model.UrlNode;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Crawler {

    private static final Logger logger = LoggerFactory.getLogger(Crawler.class);

    private final WebService webService;

    private final Map<String, UrlNode> visitedUrls;
    private final Set<String> seenUrls;

    private final String host;


    public Crawler(WebService webService, Map<String, UrlNode> visitedUrls, Set<String> seenUrls, URI domain) {
        this.webService = webService;
        this.host = getUrlDomain(domain.getHost());
        this.visitedUrls = visitedUrls;
        this.seenUrls = seenUrls;
    }

    public List<UrlNode> crawl(UrlNode currentPageNode) {
        logger.debug("Processing url {}", currentPageNode.getUrl());

        String path = currentPageNode.getUrl().toString();
        List<String> pageUrls;
        try {
            pageUrls = webService.getDocument(path);
            visitedUrls.put(currentPageNode.getUrl().toString(), currentPageNode);
        } catch (Exception e) {
            logger.error("Failed to fetch document from url {}", path, e);
            return List.of();
        }
        ArrayList<UrlNode> urlNodes = new ArrayList<>();

        for (String url : pageUrls) {
            URI uri = URIUtils.createUri(currentPageNode.getUrl(), url);

            if (uri != null) {
                UrlNode urlNode = getOrCreateUrlNode(currentPageNode, uri);
                String urlDomain = getUrlDomain(uri.getHost());

                if (currentPageNode.hasAncestor(uri)) {
                    logger.debug("Skipping url from being added to work queue {} as it is already in node's {} ancestry", uri, currentPageNode.getUrl());
                } else if (currentPageNode.containsChild(uri)) {
                    logger.debug("Skipping url from being added to work queue {} as it is already in node's {} children", uri, currentPageNode.getUrl());
                } else if (!StringUtils.equals(urlDomain, host)) {
                    logger.debug("Skipping url from being added to work queue {} as it is not from the same host {}", uri, host);
                } else if (visitedUrls.containsKey(uri.toString())) {
                    logger.debug("Skipping url from being added to work queue {} as it has already been visited", uri);
                } else if (seenUrls.contains(uri.toString())) {
                    logger.debug("Skipping url from being added to work queue {} as it is already in work queue", uri);
                } else {
                    logger.info("Adding url {} to work queue", uri);
                    urlNodes.add(urlNode);
                    seenUrls.add(urlNode.getUrl().toString());
                }

                if (currentPageNode.containsChild(urlNode.getUrl())) {
                    logger.debug("Skipping url node {} from being added as child as it is already in node's {} children", urlNode.getUrl(), currentPageNode.getUrl());
                } else if (currentPageNode.getUrl().equals(urlNode.getUrl())) {
                    logger.debug("Skipping url node {} from being added as child as it is the same as node's {}", urlNode.getUrl(), currentPageNode.getUrl());
                } else if (currentPageNode.hasAncestor(urlNode.getUrl())) {
                    logger.debug("Skipping url node {} is already present in the ancestry of current node {}", urlNode.getUrl(), currentPageNode.getUrl());
                } else {
                    logger.debug("Adding url node {} as child to node {}", uri, currentPageNode.getUrl());
                    currentPageNode.addChild(urlNode);
                }
            }
        }
        return urlNodes;
    }

    private UrlNode getOrCreateUrlNode(UrlNode currentUrlNode, URI uri) {
        UrlNode urlNode;
        if (visitedUrls.containsKey(uri.toString())) {
            logger.debug("Url {} has already been visited, reusing previous value", uri);
            urlNode = visitedUrls.get(uri.toString()).clone(currentUrlNode);
        } else {
            logger.debug("Adding url {} to visited urls", uri);
            urlNode = new UrlNode(uri, currentUrlNode);
        }
        return urlNode;
    }

    private static String getUrlDomain(String uri) {
        return uri.startsWith("www.") ? uri.substring(4) : uri;
    }


}
