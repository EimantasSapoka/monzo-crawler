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

@Service
public class WebService {

    private static final Logger logger = LoggerFactory.getLogger(WebService.class);

    public List<String> getDocument(String path) throws IOException {
        long startTime = System.currentTimeMillis();
        logger.info("Fetching document from {}", path);
        URL url = URI.create(path).toURL();
        URLConnection connection = url.openConnection();
        connection.connect();
        String mimeType = connection.getContentType();
        if (mimeType == null || (!mimeType.contains("text/") && !mimeType.contains("/xml") && !mimeType.endsWith("+xml"))) {
            logger.debug("Skipping non-html document {}", path);
            return List.of();
        }
        try {
            Document doc = Jsoup.connect(path).timeout(3000).get();
            Elements links = doc.select("a[href]");
            long endTime = System.currentTimeMillis();
            logger.info("Fetched document from {} in {} ms", path, (endTime - startTime));
            return links.eachAttr("abs:href");
        } catch (Exception e) {
            logger.error("Failed to fetch document from {}", path, e);
            return List.of();
        }
    }
}
