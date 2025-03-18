package com.monzo.web_crawler.crawler.controller;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import org.hibernate.validator.constraints.URL;


@Getter
public class CrawlRequest {

    @Parameter(name="domain", required = true, description = "Domain to crawl", example = "www.monzo.com")
    @URL(message = "Invalid URI format")
    @NotNull
    private String domain;
}
