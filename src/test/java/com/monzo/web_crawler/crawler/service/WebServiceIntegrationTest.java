package com.monzo.web_crawler.crawler.service;


import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = WebService.class)
public class WebServiceIntegrationTest {

    @Autowired
    private WebService webService;

    @Test
    public void crawl_testUrlPage_parsesHyperlinks_returnsListOfUrls() throws IOException, TimeoutException {
        // ARRANGE

        // ACT
        List<String> urls = webService.getDocumentLinks("https://www.monzo.com");

        // ASSERT
        Assertions.assertTrue(urls.size() > 1);
        // let's make the assumption the help page will always be in the main page
        Assertions.assertTrue(urls.stream().anyMatch(url -> StringUtils.equals(url, "https://monzo.com/help")));
    }

}
