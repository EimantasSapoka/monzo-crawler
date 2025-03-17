package com.monzo.web_crawler.crawler.controller;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CrawlRequest {

    @Parameter(name="domain", required = true, description = "Domain to crawl", example = "www.monzo.com")
    private String domain;
}
