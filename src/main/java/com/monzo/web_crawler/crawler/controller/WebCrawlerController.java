package com.monzo.web_crawler.crawler.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monzo.web_crawler.crawler.model.Page;
import com.monzo.web_crawler.crawler.service.CrawlerService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api")
public class WebCrawlerController {

    private static final Logger logger = LoggerFactory.getLogger(WebCrawlerController.class);


    private final CrawlerService crawlerService;

    public WebCrawlerController(CrawlerService crawlerService) {
        this.crawlerService = crawlerService;
    }

    @PostMapping(value = "/v1/crawl", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> crawl(@RequestBody @Valid CrawlRequest crawlRequest) {
        try {
            URI domain = URI.create(crawlRequest.getDomain());
            if (!"https".equalsIgnoreCase(domain.getScheme()) && !"http".equalsIgnoreCase(domain.getScheme())) {
                logger.error("Invalid scheme for URL: {}", crawlRequest.getDomain());
                return ResponseEntity.badRequest().body(null);
            }
            List<Page> crawledPages = crawlerService.crawl(domain);
            CrawlResponse crawlResponse = new CrawlResponse(crawledPages);

            // just to use the logger to print it into a file
            LoggerFactory.getLogger("OutputLog").info(new ObjectMapper().writeValueAsString(crawlResponse));
            logger.info("Crawl output logged to /logs/output.log");

            return ResponseEntity.ok(crawlResponse);
        } catch (Exception e) {
            logger.error("Failed to crawl url {}", crawlRequest.getDomain(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

}
