package com.monzo.web_crawler.crawler.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class WebService {

    private static final Logger logger = LoggerFactory.getLogger(WebService.class);

    public List<String> getDocumentLinks(String path) throws IOException, TimeoutException {
        logger.debug("Fetching document from {}", path);
        URL url = URI.create(path).toURL();
        URLConnection connection = url.openConnection();
        connection.connect();
        String mimeType = connection.getContentType();
        if (mimeType == null || (!mimeType.contains("text/") && !mimeType.contains("/xml") && !mimeType.endsWith("+xml"))) {
            logger.debug("Skipping non-html document {}", path);
            return List.of();
        }
        try {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    long startTime = System.currentTimeMillis();
                    Document doc = Jsoup.connect(path).timeout(3000).get();
                    long endTime = System.currentTimeMillis();
                    logger.debug("Fetching document from {} took {} ms", path, (endTime - startTime));
                    Elements links = doc.select("a[href]");
                    return links.eachAttr("abs:href");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).get(3, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            throw new TimeoutException(String.format("Failed to fetch document from %s within 3 seconds", path));
        } catch (Exception e) {
            throw new RuntimeException(String.format("Failed to fetch document from %s", path), e);
        }
    }
}
