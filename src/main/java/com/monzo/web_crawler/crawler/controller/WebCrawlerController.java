package com.monzo.web_crawler.crawler.controller;

import com.monzo.web_crawler.crawler.model.UrlNode;
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

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

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
            UrlNode crawlResponse = crawlerService.crawl(domain);
            printResult(crawlResponse, Files.createTempFile("monzo-crawler", ".txt"));

            return ResponseEntity.ok("Done, check file for results");
        } catch (Exception e) {
            logger.error("Failed to crawl url {}", crawlRequest.getDomain(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    public static void printResult(UrlNode root, Path outputPath) throws IOException {
        logger.info("Printing crawl result to {}", outputPath);
        Deque<UrlNode> stack = new ArrayDeque<>();
        Map<UrlNode, Integer> depthMap = new HashMap<>();

        stack.push(root);
        depthMap.put(root, 0);

        while (!stack.isEmpty()) {
            UrlNode currentNode = stack.pop();
            int currentLevel = depthMap.get(currentNode);
            String padding = " ".repeat(currentLevel);
            String line = String.format("%s-%s\n", padding, currentNode.getUrl().toString());
            Files.writeString(outputPath, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);

            for (UrlNode child : currentNode.getChildren()) {
                stack.push(child);
                depthMap.put(child, currentLevel + 1);
            }
        }
        logger.info("Printing crawl result to {} done", outputPath);
    }


}
