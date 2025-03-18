package com.monzo.web_crawler.crawler.service;


import com.monzo.web_crawler.crawler.model.NestedUrl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.net.URI;

@ExtendWith(MockitoExtension.class)
@ExtendWith(SpringExtension.class)
public class CrawlerServiceUnitTest {

    @InjectMocks
    private CrawlerService crawlerService;


    @Test
    public void crawlDomain_returnsNestedUrls() {
        // ARRANGE
        URI domain = URI.create("www.monzo.com");

        // ACT
        NestedUrl result = crawlerService.crawl(domain);

        // ASSERT
        Assertions.assertEquals(result.getUrl(), domain);
    }
}
