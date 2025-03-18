package com.monzo.web_crawler.crawler.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class WebService {

    public Document getDocument(String path) throws IOException {
        return Jsoup.connect(path).get();
    }
}
