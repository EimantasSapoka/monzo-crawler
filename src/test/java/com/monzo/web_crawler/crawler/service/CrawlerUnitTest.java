package com.monzo.web_crawler.crawler.service;

import com.monzo.web_crawler.crawler.model.UrlNode;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@ExtendWith(SpringExtension.class)
@ExtendWith(MockitoExtension.class)
public class CrawlerUnitTest {

    @Mock
    private WebService webService;



    private Crawler crawler;

    private final URI rootUrl = URI.create("https://www.monzo.com");

    @BeforeEach
    void setUp() {
        crawler = new Crawler(webService, rootUrl);
    }

    @Value("classpath:/service/monzo_page.html")
    private Resource monzoPage;

    @Value("classpath:/service/monzo_faq_page.html")
    private Resource monzoFaqPage;

    @Value("classpath:/service/empty_page.html")
    private Resource emptyPage;

    @Captor
    ArgumentCaptor<String> urlCaptor;

    @Test
    public void crawl_monzoPage_makesRequestToUrlsWithinPage_returnsAllUrlsFromPage_noDuplicates() throws IOException {
        // ARRANGE
        Document monzoDocument = Jsoup.parse(monzoPage.getContentAsString(StandardCharsets.UTF_8));
        Mockito.when(webService.getDocument(rootUrl.toString())).thenReturn(monzoDocument);

        Document monzoFaqDocument = Jsoup.parse(monzoFaqPage.getContentAsString(StandardCharsets.UTF_8));
        Mockito.when(webService.getDocument("https://monzo.com/faq")).thenReturn(monzoFaqDocument);

        // any other page should return empty page with no links to simplify
        Document emptyDocument = Jsoup.parse(emptyPage.getContentAsString(StandardCharsets.UTF_8));
        Mockito.when(webService.getDocument(Mockito.argThat(url -> !url.equals(rootUrl.toString()) && !url.equals("https://monzo.com/faq")))).thenReturn(emptyDocument);

        // ACT
        UrlNode result = crawler.crawl();

        // ASSERT
        Assertions.assertEquals(rootUrl, result.getUrl());
        Assertions.assertEquals(67, result.getChildren().size());
        Set<String> uniqueChildUrls = result.getChildren().stream().map(child -> child.getUrl().toString()).collect(Collectors.toSet());
        Assertions.assertEquals(uniqueChildUrls.size(), result.getChildren().size(), "There should be no duplicate child URLs");

        UrlNode monzoFaqNode = result.getChildren()
                .stream()
                .filter(node -> StringUtils.equals(node.getUrl().toString(), "https://monzo.com/faq"))
                .findFirst().get();

        Assertions.assertEquals(71, monzoFaqNode.getChildren().size());

        // verify all calls to web service to retrieve documents are for monzo.com domain as per requirement
        Mockito.verify(webService, Mockito.atLeast(1)).getDocument(urlCaptor.capture());
        String expectedDomain = rootUrl.getHost().replace("www.", "");
        urlCaptor.getAllValues().forEach(url -> Assertions.assertEquals(URI.create(url).getHost().replace("www.", ""), expectedDomain));
    }


    @Value("classpath:/service/test_url_links.html")
    private Resource stubPage;

    @Test
    public void crawl_testUrlPage_parsesAllTypesOfUrl_returnsCorrectListOfUrls() throws IOException {
        // ARRANGE
        Document stubDocument = Jsoup.parse(stubPage.getContentAsString(StandardCharsets.UTF_8));
        Mockito.when(webService.getDocument(rootUrl.toString())).thenReturn(stubDocument);

        // ACT
        UrlNode result = crawler.crawl();

        // ASSERT
        Assertions.assertEquals(result.getUrl(), rootUrl);
        List<UrlNode> children = result.getChildren();
        Assertions.assertTrue(children.stream().anyMatch(url -> StringUtils.equals(url.getUrl().toString(), "https://example.com")));
        Assertions.assertTrue(children.stream().anyMatch(url -> StringUtils.equals(url.getUrl().toString(), "http://example.org")));
        Assertions.assertTrue(children.stream().anyMatch(url -> StringUtils.equals(url.getUrl().getPath(), rootUrl.getPath() + "/relative/path")));
        Assertions.assertTrue(children.stream().anyMatch(url -> StringUtils.equals(url.getUrl().getPath(), rootUrl.getPath() + "/relative2.html")));
        Assertions.assertTrue(children.stream().anyMatch(url -> StringUtils.equals(url.getUrl().toString(), "https://example.com/page")));
        Assertions.assertTrue(children.stream().anyMatch(url -> StringUtils.equals(url.getUrl().toString(), "https://example.com/nested")));
        Assertions.assertTrue(children.stream().anyMatch(url -> StringUtils.equals(url.getUrl().toString(), "https://example.com/hidden")));
        Assertions.assertTrue(children.stream().anyMatch(url -> StringUtils.equals(url.getUrl().toString(), "https://example.com/image-link")));

        // more interesting cases

        // there should only be one /page link as there's one with query string and one with anchor link. They're the same page.
        Assertions.assertEquals(1, children.stream().filter(url -> StringUtils.equals(url.getUrl().toString(), "https://example.com/page")).count());
        Assertions.assertEquals(1, children.stream().filter(url -> StringUtils.equals(url.getUrl().toString(), "https://repeated.com")).count());

        // below two should not be captured even though they're anchor links
        Assertions.assertFalse(children.stream().anyMatch(url -> StringUtils.contains(url.getUrl().toString(), "mailto:test@example.com")));
        Assertions.assertFalse(children.stream().anyMatch(url -> StringUtils.equals(url.getUrl().toString(), "tel:+123456789")));

        // crawler does not support links within handlers / js
        Assertions.assertFalse(children.stream().anyMatch(url -> StringUtils.equals(url.getUrl().toString(), "https://example.com/button-link")));

        // crawler does not support dynamic links
        Assertions.assertFalse(children.stream().anyMatch(url -> StringUtils.equals(url.getUrl().toString(), "https://example.com/dynamic")));

        // crawler does not support iframes
        Assertions.assertFalse(children.stream().anyMatch(url -> StringUtils.equals(url.getUrl().toString(), "https://example.com/iframe-content")));
    }

    @Value("classpath:/service/cyclical_link.html")
    private Resource cyclicalLinkPage;

    @Test
    public void crawl_cyclicalLink_doesNotLoopForever() throws IOException {
        // ARRANGE
        Document cyclicalDocument = Jsoup.parse(cyclicalLinkPage.getContentAsString(StandardCharsets.UTF_8));
        Mockito.when(webService.getDocument(rootUrl.toString())).thenReturn(cyclicalDocument);
        Mockito.when(webService.getDocument("https://monzo.com/cycle")).thenReturn(cyclicalDocument);

        // ACT
        UrlNode result = crawler.crawl();

        // ASSERT
        Assertions.assertEquals(rootUrl, result.getUrl());
        Assertions.assertEquals(1, result.getChildren().size(), "There should be one child URL");
        Assertions.assertEquals("https://monzo.com/cycle", result.getChildren().stream().findFirst().get().getUrl().toString(), "Child URL should be monzo.com/cycle");
    }


}
