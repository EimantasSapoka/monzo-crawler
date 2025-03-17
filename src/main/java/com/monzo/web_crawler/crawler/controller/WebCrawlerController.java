package com.monzo.web_crawler.crawler.controller;

import com.monzo.web_crawler.crawler.model.CrawlResult;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class WebCrawlerController {



    @PostMapping(value= "/crawl", consumes=MediaType.APPLICATION_JSON_VALUE)
    public CrawlResult crawl(@RequestBody CrawlRequest crawlRequest) {

        return new CrawlResult("www.monzo.com", List.of(new CrawlResult("www.monzo.com/help")));
    }

}
