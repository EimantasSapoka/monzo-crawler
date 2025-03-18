package com.monzo.web_crawler.crawler.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class WebService {

    private static final Logger logger = LoggerFactory.getLogger(WebService.class);

    public Document getDocument(String path) throws IOException {
        logger.debug("Fetching document from {}", path);
        return Jsoup.connect(path).get();
    }
}
