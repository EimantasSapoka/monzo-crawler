package com.monzo.web_crawler.crawler.service;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;

public class URIUtils {
    private static final Logger logger = LoggerFactory.getLogger(URIUtils.class);

    public static URI createUri(URI basePath, String url) {
        String href = url.strip().replaceAll("\\s+", "%20");

        URI uri;
        try {
            uri = URI.create(href);
        } catch (Exception e) {
            logger.debug("Skipping invalid url {}", href);
            return null;
        }

        // to handle relative paths like ./document.html
        if (!uri.isAbsolute()) {
            logger.trace("Adding relative path {} to base path {}", href, basePath);
            uri = org.apache.http.client.utils.URIUtils.resolve(basePath, uri);
        }

        if (!StringUtils.equals(uri.getScheme(), "http") && !StringUtils.equals(uri.getScheme(), "https")) {
            logger.debug("Skipping non-http/https url {}", uri);
            return null;
        }

        String path = uri.getPath();
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        try {

            return new URIBuilder()
                    .setScheme(uri.getScheme())
                    .setHost(uri.getHost())
                    .setPath(path)
                    .build();
        } catch (URISyntaxException e) {
            logger.debug("Failed to create clean URI from {}", uri, e);
            return null;
        }
    }
}
