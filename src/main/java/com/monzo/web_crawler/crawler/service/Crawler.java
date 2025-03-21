package com.monzo.web_crawler.crawler.service;

import com.monzo.web_crawler.crawler.model.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Crawler is responsible for crawling a given web page and extracting links
 * from it to build up a set of linked URIs. It uses a WebService instance to
 * fetch the document and parse its content to find links.
 * <p>
 * This class fetches a web page from a given URI and processes the document
 * to extract valid URLs, resolving relative paths and ensuring proper formatting.
 */
public class Crawler {

    private static final Logger logger = LoggerFactory.getLogger(Crawler.class);

    private final WebService webService;


    public Crawler(WebService webService) {
        this.webService = webService;
    }

    public Page crawl(URI currentPageUri) {
        logger.debug("Processing url {}", currentPageUri);

        List<String> pageUrlStrings;
        try {
            pageUrlStrings = webService.getDocumentLinks(currentPageUri.toString());
        } catch (Exception e) {
            throw new RuntimeException(String.format("Failed to get URIs from url %s", currentPageUri.toString()), e);
        }

        Set<URI> pageLinks = pageUrlStrings.stream()
                .map(url -> URIUtils.createUri(currentPageUri, url))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        return new Page(currentPageUri, pageLinks);
    }


}
