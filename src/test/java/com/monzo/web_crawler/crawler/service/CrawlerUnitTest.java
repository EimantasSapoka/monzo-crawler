package com.monzo.web_crawler.crawler.service;

import com.monzo.web_crawler.crawler.model.UrlNode;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class CrawlerUnitTest {

    @Test
    void shouldAddUrlsToWorkQueue_whenUrlsAreFromSameHostAndNotVisited() throws Exception {
        // Arrange
        WebService webService = mock(WebService.class);
        Queue<UrlNode> workQueue = new LinkedList<>();
        Map<String, UrlNode> visitedUrls = new HashMap<>();
        URI domain = URI.create("https://example.com");
        UrlNode parentNode = new UrlNode(domain, null);

        when(webService.getDocument("https://example.com")).thenReturn(List.of("/page1", "/page2"));
        Crawler crawler = new Crawler(webService, workQueue, visitedUrls, domain);

        // Act
        crawler.crawl(parentNode);

        // Assert
        assertEquals(2, workQueue.size());
        assertTrue(workQueue.stream().anyMatch(node -> node.getUrl().toString().equals("https://example.com/page1")));
        assertTrue(workQueue.stream().anyMatch(node -> node.getUrl().toString().equals("https://example.com/page2")));
    }

    @Test
    void shouldNotAddUrlToWorkQueue_whenUrlIsNotFromSameHost() throws Exception {
        // Arrange
        WebService webService = mock(WebService.class);
        Queue<UrlNode> workQueue = new LinkedList<>();
        Map<String, UrlNode> visitedUrls = new HashMap<>();
        URI domain = URI.create("https://example.com");
        UrlNode parentNode = new UrlNode(domain, null);

        when(webService.getDocument("https://example.com")).thenReturn(List.of("https://other.com/page1"));
        Crawler crawler = new Crawler(webService, workQueue, visitedUrls, domain);

        // Act
        crawler.crawl(parentNode);

        // Assert
        assertEquals(0, workQueue.size());
    }

    @Test
    void shouldMarkUrlAsVisited_whenFetchedSuccessfully() throws Exception {
        // Arrange
        WebService webService = mock(WebService.class);
        Queue<UrlNode> workQueue = new LinkedList<>();
        Map<String, UrlNode> visitedUrls = new HashMap<>();
        URI domain = URI.create("https://example.com");
        UrlNode parentNode = new UrlNode(domain, null);

        when(webService.getDocument("https://example.com")).thenReturn(List.of());
        Crawler crawler = new Crawler(webService, workQueue, visitedUrls, domain);

        // Act
        crawler.crawl(parentNode);

        // Assert
        assertTrue(visitedUrls.containsKey("https://example.com"));
    }

    @Test
    void shouldNotAddUrlToWorkQueue_whenUrlIsAlreadyQueued() throws Exception {
        // Arrange
        WebService webService = mock(WebService.class);
        Queue<UrlNode> workQueue = new LinkedList<>();
        Map<String, UrlNode> visitedUrls = new HashMap<>();
        URI domain = URI.create("https://example.com");
        UrlNode parentNode = new UrlNode(domain, null);

        UrlNode existingNode = new UrlNode(URI.create("https://example.com/page1"), parentNode);
        workQueue.add(existingNode);

        when(webService.getDocument("https://example.com")).thenReturn(List.of("/page1"));
        Crawler crawler = new Crawler(webService, workQueue, visitedUrls, domain);

        // Act
        crawler.crawl(parentNode);

        // Assert
        assertEquals(1, workQueue.size());
    }

    @Test
    void shouldHandleExceptionGracefully_whenWebServiceThrowsException() throws Exception {
        // Arrange
        WebService webService = mock(WebService.class);
        Queue<UrlNode> workQueue = new LinkedList<>();
        Map<String, UrlNode> visitedUrls = new HashMap<>();
        URI domain = URI.create("https://example.com");
        UrlNode parentNode = new UrlNode(domain, null);

        when(webService.getDocument("https://example.com")).thenThrow(new RuntimeException("WebService failure"));
        Crawler crawler = new Crawler(webService, workQueue, visitedUrls, domain);

        // Act
        crawler.crawl(parentNode);

        // Assert
        assertTrue(visitedUrls.isEmpty());
        assertTrue(workQueue.isEmpty());
    }

    @Test
    void shouldSkipAddingUrlAsChild_whenUrlIsAlreadyChild() throws Exception {
        // Arrange
        WebService webService = mock(WebService.class);
        Queue<UrlNode> workQueue = new LinkedList<>();
        Map<String, UrlNode> visitedUrls = new HashMap<>();
        URI domain = URI.create("https://example.com");
        UrlNode parentNode = mock(UrlNode.class);

        when(parentNode.getUrl()).thenReturn(domain);
        when(parentNode.containsChild(URI.create("https://example.com/page1"))).thenReturn(true);
        when(webService.getDocument("https://example.com")).thenReturn(List.of("/page1"));

        Crawler crawler = new Crawler(webService, workQueue, visitedUrls, domain);

        // Act
        crawler.crawl(parentNode);

        // Assert
        verify(parentNode, never()).addChild(any());
    }

    @Test
    void shouldAddChildNode_whenUrlNodeIsValidAndNotPresentAsChild() throws Exception {
        // Arrange
        WebService webService = mock(WebService.class);
        Queue<UrlNode> workQueue = new LinkedList<>();
        Map<String, UrlNode> visitedUrls = new HashMap<>();
        URI domain = URI.create("https://example.com");
        UrlNode parentNode = mock(UrlNode.class);

        when(parentNode.getUrl()).thenReturn(domain);
        when(parentNode.containsChild(any(URI.class))).thenReturn(false);
        when(webService.getDocument("https://example.com")).thenReturn(List.of("/page1"));

        Crawler crawler = new Crawler(webService, workQueue, visitedUrls, domain);

        // Act
        crawler.crawl(parentNode);

        // Assert
        verify(parentNode).addChild(any(UrlNode.class));
    }
}