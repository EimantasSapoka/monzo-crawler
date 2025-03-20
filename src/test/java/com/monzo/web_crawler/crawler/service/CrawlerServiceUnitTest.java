package com.monzo.web_crawler.crawler.service;

import com.monzo.web_crawler.crawler.model.UrlNode;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@ExtendWith(SpringExtension.class)
@ExtendWith(MockitoExtension.class)
public class CrawlerServiceUnitTest {

    @Mock
    private WebService webService;

    private CrawlerService crawler;

    private final URI rootUrl = URI.create("https://www.monzo.com");

    @BeforeEach
    void setUp() {
        crawler = new CrawlerService(webService, 2, 1);
    }

    @Captor
    private ArgumentCaptor<String> urlCaptor;

    @Test
    public void crawl_monzoPage_makesRequestToUrlsWithinPage_returnsAllUrlsFromPage_noDuplicates() throws IOException {
        // ARRANGE
        Mockito.when(webService.getDocument(rootUrl.toString())).thenReturn(List.of("https://www.monzo.com", "https://www.monzo.com/help", "https://www.monzo.com/faq", "https://www.monzo.com/faq", "https://www.monzo.com/faq"));

        Mockito.when(webService.getDocument("https://www.monzo.com/faq")).thenReturn(List.of("https://www.monzo.com", "https://www.google.com", "https://www.test.com", "https://www.monzo.com/fraud"));

        // any other page should return empty page with no links to simplify
        Mockito.when(webService.getDocument(Mockito.argThat(url -> !url.equals(rootUrl.toString()) && !url.equals("https://www.monzo.com/faq")))).thenReturn(List.of());

        // ACT
        UrlNode result = crawler.crawl(rootUrl);

        // ASSERT
        Assertions.assertEquals(rootUrl, result.getUrl());
        Assertions.assertEquals(2, result.getChildren().size());
        Set<String> uniqueChildUrls = result.getChildren().stream().map(child -> child.getUrl().toString()).collect(Collectors.toSet());
        Assertions.assertEquals(uniqueChildUrls.size(), result.getChildren().size(), "There should be no duplicate child URLs");

        UrlNode monzoFaqNode = result.getChildren()
                .stream()
                .filter(node -> StringUtils.equals(node.getUrl().toString(), "https://www.monzo.com/faq"))
                .findFirst().get();

        Assertions.assertEquals(4, monzoFaqNode.getChildren().size());

        // verify all calls to web service to retrieve documents are for monzo.com domain as per requirement
        Mockito.verify(webService, Mockito.atLeast(1)).getDocument(urlCaptor.capture());
        String expectedDomain = rootUrl.getHost().replace("www.", "");
        urlCaptor.getAllValues().forEach(url -> Assertions.assertEquals(URI.create(url).getHost().replace("www.", ""), expectedDomain));
    }


    @Test
    public void crawl_cyclicalLink_doesNotLoopForever() throws IOException {
        // ARRANGE
        Mockito.when(webService.getDocument(rootUrl.toString())).thenReturn(List.of("https://www.monzo.com", "https://www.monzo.com/cycle"));
        Mockito.when(webService.getDocument("https://www.monzo.com/cycle")).thenReturn(List.of("https://www.monzo.com", "https://www.monzo.com/cycle", "https://www.monzo.com/cycle2"));
        Mockito.when(webService.getDocument("https://www.monzo.com/cycle2")).thenReturn(List.of("https://www.monzo.com", "https://www.monzo.com/cycle", "https://www.monzo.com/cycle2"));

        // ACT
        UrlNode monzoPage = crawler.crawl(rootUrl);

        // ASSERT
        Assertions.assertEquals(rootUrl, monzoPage.getUrl());
        Assertions.assertEquals(1, monzoPage.getChildren().size(), "There should be one child URL");
        Assertions.assertEquals("https://www.monzo.com/cycle", monzoPage.getChildren().stream().findFirst().get().getUrl().toString(), "Child URL should be monzo.com/cycle");

        UrlNode cycleNode = monzoPage.getChildren().stream().findFirst().get();
        Assertions.assertEquals(2, cycleNode.getChildren().size(), "There should be two child URLs, monzo.com and monzo.com/cycle2");

        UrlNode cycleNode2 = cycleNode.getChildren().get(1);
        Assertions.assertEquals(2, cycleNode2.getChildren().size(), "There should be two child URLs, monzo.com and monzo.com/cycle");
        Assertions.assertEquals(0, cycleNode2.getChildren().get(0).getChildren().size(), "Cycle node's children should not have any children to prevent infinite loop");
        Assertions.assertEquals(0, cycleNode2.getChildren().get(1).getChildren().size(), "Cycle node's children should not have any children to prevent infinite loop");

    }

    @Test
    public void crawl_multiplePagesHaveSameLink_processesThatLinkOnlyOnce() throws IOException {
        // ARRANGE
        // it will add /help and /repeated to work queue. Then it will crawl /help page and add /repeated to work queue again. Need to make sure /repeated is only crawled once.
        Mockito.when(webService.getDocument(rootUrl.toString())).thenReturn(List.of("https://www.monzo.com/help", "https://www.monzo.com/repeated"));
        Mockito.when(webService.getDocument("https://www.monzo.com/help")).thenReturn(List.of("https://www.monzo.com/repeated"));

        // ACT
        crawler.crawl(rootUrl);

        // ASSERT
        Mockito.verify(webService, Mockito.times(1)).getDocument("https://www.monzo.com/repeated");
    }


}
