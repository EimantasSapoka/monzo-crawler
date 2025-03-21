package com.monzo.web_crawler.crawler.service;

import com.monzo.web_crawler.crawler.model.UrlNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.net.URI;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ExtendWith(SpringExtension.class)
public class CrawlerUnitTest {

    @Mock
    private WebService webService;


    private Map<String, UrlNode> visitedUrls;
    private Set<String> seenUrls;

    private final URI domain = URI.create("https://example.com");
    private UrlNode parentNode;

    private Crawler crawler;

    @BeforeEach
    void setUp() {
        parentNode = new UrlNode(domain, null);
        visitedUrls = new HashMap<>();
        seenUrls = new HashSet<>();
        crawler = new Crawler(webService, visitedUrls, seenUrls, domain);

    }

    @Test
    void shouldAddUrlsToWorkQueue_whenUrlsAreFromSameHostAndNotVisited() throws Exception {
        // Arrange
        when(webService.getDocument("https://example.com")).thenReturn(List.of("/page1", "/page2"));

        // Act
        List<UrlNode> resultNodes = crawler.crawl(parentNode);

        // Assert
        assertEquals(2, resultNodes.size());
        assertTrue(resultNodes.stream().anyMatch(node -> node.getUrl().toString().equals("https://example.com/page1")));
        assertTrue(resultNodes.stream().anyMatch(node -> node.getUrl().toString().equals("https://example.com/page2")));
    }

    @Test
    void shouldNotAddUrlToWorkQueue_whenUrlIsNotFromSameHost() throws Exception {
        // Arrange
        when(webService.getDocument("https://example.com")).thenReturn(List.of("https://other.com/page1"));

        // Act
        List<UrlNode> resultNodes = crawler.crawl(parentNode);

        // Assert
        assertEquals(0, resultNodes.size());
    }

    @Test
    void shouldMarkUrlAsVisited_whenFetchedSuccessfully() throws Exception {
        // Arrange
        when(webService.getDocument("https://example.com")).thenReturn(List.of());

        // Act
        crawler.crawl(parentNode);

        // Assert
        assertTrue(visitedUrls.containsKey("https://example.com"));
    }

    @Test
    void shouldNotAddUrlToWorkQueue_whenUrlIsAlreadySeen() throws Exception {
        // Arrange
        seenUrls.add("https://example.com/page1");

        when(webService.getDocument("https://example.com")).thenReturn(List.of("/page1"));

        // Act
        List<UrlNode> resultNodes = crawler.crawl(parentNode);

        // Assert
        assertEquals(0, resultNodes.size());
    }

    @Test
    void shouldHandleExceptionGracefully_whenWebServiceThrowsException() throws Exception {
        // Arrange
        when(webService.getDocument("https://example.com")).thenThrow(new RuntimeException("WebService failure"));

        // Act
        List<UrlNode> resultNodes = crawler.crawl(parentNode);

        // Assert
        assertTrue(visitedUrls.isEmpty());
        assertTrue(resultNodes.isEmpty());
    }

    @Test
    void shouldSkipAddingUrlAsChild_whenUrlIsAlreadyChild() throws Exception {
        // Arrange
        parentNode.addChild(new UrlNode(URI.create("https://example.com/page1"), parentNode));
        when(webService.getDocument("https://example.com")).thenReturn(List.of("/page1", "/page1", "/page1"));

        // Act
        crawler.crawl(parentNode);

        // Assert
        Assertions.assertEquals(1, parentNode.getChildren().size());
    }

    @Test
    void shouldAddChildNode_whenUrlNodeIsValidAndNotPresentAsChild() throws Exception {
        // Arrange
        when(webService.getDocument("https://example.com")).thenReturn(List.of("/page1"));

        // Act
        crawler.crawl(parentNode);

        // Assert
        Assertions.assertEquals(1, parentNode.getChildren().size());
    }
}