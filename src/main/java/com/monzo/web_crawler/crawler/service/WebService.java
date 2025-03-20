package com.monzo.web_crawler.crawler.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class WebService {

    private static final Logger logger = LoggerFactory.getLogger(WebService.class);

    public List<String> getDocument(String path) throws IOException {
        logger.debug("Fetching document from {}", path);
        Document doc = Jsoup.connect(path).get();
        Elements links = doc.select("a[href]");
        return links.eachAttr("abs:href");
    }
}
