package com.monzo.web_crawler.crawler.service;

import com.monzo.web_crawler.crawler.model.Page;
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
import java.util.concurrent.TimeoutException;

@ExtendWith(SpringExtension.class)
@ExtendWith(MockitoExtension.class)
public class CrawlerServiceUnitTest {

    @Mock
    private WebService webService;

    private CrawlerService crawler;

    private final URI rootUrl = URI.create("https://www.monzo.com");

    @BeforeEach
    void setUp() {
        crawler = new CrawlerService(webService, 2, 10, 10);
    }

    @Captor
    private ArgumentCaptor<String> urlCaptor;

    @Test
    public void crawl_monzoPage_makesRequestToUrlsWithinPage_returnsAllUrlsFromRequiredDomain_noDuplicates() throws IOException, TimeoutException {
        // ARRANGE
        Mockito.when(webService.getDocumentLinks(rootUrl.toString())).thenReturn(List.of("https://www.monzo.com", "https://www.monzo.com/help", "https://www.monzo.com/faq", "https://www.monzo.com/faq", "https://www.monzo.com/faq"));

        Mockito.when(webService.getDocumentLinks("https://www.monzo.com/faq")).thenReturn(List.of("https://www.monzo.com", "https://www.google.com", "https://www.test.com", "https://www.monzo.com/fraud"));

        // any other page should return empty page with no links to simplify
        Mockito.when(webService.getDocumentLinks(Mockito.argThat(url -> !url.equals(rootUrl.toString()) && !url.equals("https://www.monzo.com/faq")))).thenReturn(List.of());

        // ACT
        List<Page> result = crawler.crawl(rootUrl);

        // ASSERT
        Page firstPage = result.get(0);
        Assertions.assertEquals(rootUrl, firstPage.getUrl());
        Assertions.assertEquals(3, firstPage.getChildren().size());
        assertContainsChildPage(firstPage, "https://www.monzo.com");
        assertContainsChildPage(firstPage, "https://www.monzo.com/help");
        assertContainsChildPage(firstPage, "https://www.monzo.com/faq");

        Page monzoFaqPage = result
                .stream()
                .filter(page -> StringUtils.equals(page.getUrl().toString(), "https://www.monzo.com/faq"))
                .findFirst().get();

        Assertions.assertEquals(4, monzoFaqPage.getChildren().size());
        assertContainsChildPage(monzoFaqPage, "https://www.monzo.com");
        assertContainsChildPage(monzoFaqPage, "https://www.google.com");
        assertContainsChildPage(monzoFaqPage, "https://www.test.com");
        assertContainsChildPage(monzoFaqPage, "https://www.monzo.com/fraud");

        // verify all calls to web service to retrieve documents are for monzo.com domain as per requirement
        Mockito.verify(webService, Mockito.atLeast(1)).getDocumentLinks(urlCaptor.capture());
        String expectedDomain = rootUrl.getHost().replace("www.", "");
        urlCaptor.getAllValues().forEach(url -> Assertions.assertEquals(URI.create(url).getHost().replace("www.", ""), expectedDomain));
    }

    private static void assertContainsChildPage(Page monzoFaqPage, String url) {
        Assertions.assertTrue(monzoFaqPage.getChildren().stream().anyMatch(uri -> uri.toString().equals(url)),
                "Expected page " + monzoFaqPage.getUrl() + " to have child page with URL: " + url);
    }


    @Test
    public void crawl_cyclicalLink_doesNotLoopForever() throws IOException, TimeoutException {
        // ARRANGE
        Mockito.when(webService.getDocumentLinks(rootUrl.toString())).thenReturn(List.of("https://www.monzo.com", "https://www.monzo.com/cycle"));
        Mockito.when(webService.getDocumentLinks("https://www.monzo.com/cycle")).thenReturn(List.of("https://www.monzo.com", "https://www.monzo.com/cycle", "https://www.monzo.com/cycle2"));
        Mockito.when(webService.getDocumentLinks("https://www.monzo.com/cycle2")).thenReturn(List.of("https://www.monzo.com", "https://www.monzo.com/cycle", "https://www.monzo.com/cycle2"));

        // ACT
        List<Page> pages = crawler.crawl(rootUrl);

        // ASSERT
        Page monzoPage = pages.get(0);
        Assertions.assertEquals(rootUrl, monzoPage.getUrl());
        Assertions.assertEquals(2, monzoPage.getChildren().size());

        Page cyclePage = pages.get(1);
        Assertions.assertEquals(3, cyclePage.getChildren().size());

        Page cycleNode2 = pages.get(2);
        Assertions.assertEquals(3, cycleNode2.getChildren().size());

    }

    @Test
    public void crawl_multiplePagesHaveSameLink_processesThatLinkOnlyOnce() throws IOException, TimeoutException {
        // ARRANGE
        // it will add /help and /repeated to work queue. Then it will crawl /help page and add /repeated to work queue again. Need to make sure /repeated is only crawled once.
        Mockito.when(webService.getDocumentLinks(rootUrl.toString())).thenReturn(List.of("https://www.monzo.com/help", "https://www.monzo.com/repeated"));
        Mockito.when(webService.getDocumentLinks("https://www.monzo.com/help")).thenReturn(List.of("https://www.monzo.com/repeated"));

        // ACT
        crawler.crawl(rootUrl);

        // ASSERT
        Mockito.verify(webService, Mockito.times(1)).getDocumentLinks("https://www.monzo.com/repeated");
    }

}
