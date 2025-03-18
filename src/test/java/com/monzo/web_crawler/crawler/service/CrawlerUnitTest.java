package com.monzo.web_crawler.crawler.service;

import com.monzo.web_crawler.crawler.model.UrlNode;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Set;

@ExtendWith(SpringExtension.class)
@ExtendWith(MockitoExtension.class)
public class CrawlerUnitTest {

    @Mock
    private WebService webService;

    @Value("classpath:/service/monzo_page.html")
    private Resource monzoPage;

    @Value("classpath:/service/empty_page.html")
    private Resource emptyPage;

    private Crawler crawler;

    @BeforeEach
    void setUp() {
        crawler = new Crawler(webService);
    }

    @Test
    public void crawl_monzoPage_makesRequestToUrlsWithinPage_returnsNestedUrls() throws IOException {
        // ARRANGE
        URI domain = URI.create("www.monzo.com");
        Document monzoDocument = Jsoup.parse(monzoPage.getContentAsString(StandardCharsets.UTF_8));
        Mockito.when(webService.getDocument(domain.getPath())).thenReturn(monzoDocument);

        Document emptyDocument = Jsoup.parse(emptyPage.getContentAsString(StandardCharsets.UTF_8));
        Mockito.when(webService.getDocument(Mockito.matches("^(?!www\\.monzo\\.com$).*"))).thenReturn(emptyDocument);

        // ACT
        UrlNode result = crawler.crawl(domain);

        // ASSERT
        Assertions.assertEquals(result.getUrl(), domain);
        Assertions.assertEquals(result.getChildren().size(), 31);
    }

    @Value("classpath:/service/test_url_links.html")
    private Resource stubPage;

    @Test
    public void crawl_stubPage_parsesAllTypesOfUrl_returnsCorrectListOfUrls() throws IOException {
        // ARRANGE
        URI domain = URI.create("https://www.monzo.com");
        Document stubDocument = Jsoup.parse(stubPage.getContentAsString(StandardCharsets.UTF_8));
        Mockito.when(webService.getDocument(domain.toString())).thenReturn(stubDocument);

        Document emptyDocument = Jsoup.parse(emptyPage.getContentAsString(StandardCharsets.UTF_8));
        Mockito.when(webService.getDocument(Mockito.matches("^(?!https://www\\.monzo\\.com$).*"))).thenReturn(emptyDocument);

        // ACT
        UrlNode result = crawler.crawl(domain);

        // ASSERT
        Assertions.assertEquals(result.getUrl(), domain);
        Set<UrlNode> children = result.getChildren();
        Assertions.assertTrue(children.stream().anyMatch(url -> StringUtils.equals(url.getUrl().toString(), "https://example.com")));
        Assertions.assertTrue(children.stream().anyMatch(url -> StringUtils.equals(url.getUrl().toString(), "http://example.org")));
        Assertions.assertTrue(children.stream().anyMatch(url -> StringUtils.equals(url.getUrl().getPath(), domain.getPath() + "/relative/path")));
        Assertions.assertTrue(children.stream().anyMatch(url -> StringUtils.equals(url.getUrl().getPath(), domain.getPath() + "/relative2.html")));
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

        // crawler does not support iframes
        Assertions.assertFalse(children.stream().anyMatch(url -> StringUtils.equals(url.getUrl().toString(), "https://example.com/iframe-content")));
    }

    @Value("classpath:/service/cyclical_link.html")
    private Resource cyclicalLinkPage;

    @Test
    public void crawl_cyclicalLink_doesNotLoopForever() throws IOException {
        // ARRANGE
        URI domain = URI.create("https://www.monzo.com");
        Document cyclicalDocument = Jsoup.parse(cyclicalLinkPage.getContentAsString(StandardCharsets.UTF_8));
        Mockito.when(webService.getDocument(domain.toString())).thenReturn(cyclicalDocument);
        Mockito.when(webService.getDocument("https://test.com")).thenReturn(cyclicalDocument);

        // ACT
        UrlNode result = crawler.crawl(domain);

        // ASSERT
        Assertions.assertEquals(result.getUrl(), domain);
        Assertions.assertEquals(result.getChildren().size(), 1);
        Assertions.assertEquals(result.getChildren().stream().findFirst().get().getUrl().toString(), "https://test.com");
    }


}
