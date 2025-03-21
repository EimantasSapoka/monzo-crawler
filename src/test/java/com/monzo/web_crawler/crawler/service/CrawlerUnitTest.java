package com.monzo.web_crawler.crawler.service;

import com.monzo.web_crawler.crawler.model.Page;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class CrawlerUnitTest {

    private WebService webServiceMock;
    private Crawler crawler;

    private final URI currentPageUri = URI.create("http://example.com");

    @BeforeEach
    void setUp() {
        webServiceMock = mock(WebService.class);
        crawler = new Crawler(webServiceMock);
    }

    @Test
    void testCrawlSuccessful() throws IOException, TimeoutException {
        // Arrange
        List<String> documentLinks = List.of("http://example.com/page1", "/page2", "http://example.org");
        when(webServiceMock.getDocumentLinks(currentPageUri.toString())).thenReturn(documentLinks);

        // Act
        Page resultPage = crawler.crawl(currentPageUri);

        // Assert
        assertNotNull(resultPage);
        assertEquals(currentPageUri, resultPage.getUrl());
        Set<URI> expectedLinks = Set.of(
                URI.create("http://example.com/page1"),
                URI.create("http://example.com/page2"),
                URI.create("http://example.org")
        );
        assertEquals(expectedLinks, resultPage.getChildren());
        verify(webServiceMock, times(1)).getDocumentLinks(currentPageUri.toString());
    }

    @Test
    void testCrawlHandlesEmptyLinks() throws IOException, TimeoutException {
        // Arrange
        when(webServiceMock.getDocumentLinks(currentPageUri.toString())).thenReturn(List.of());

        // Act
        Page resultPage = crawler.crawl(currentPageUri);

        // Assert
        assertNotNull(resultPage);
        assertEquals(currentPageUri, resultPage.getUrl());
        assertTrue(resultPage.getChildren().isEmpty());
        verify(webServiceMock, times(1)).getDocumentLinks(currentPageUri.toString());
    }


    @Test
    void testCrawlHandlesWebServiceException() throws IOException, TimeoutException {
        // Arrange
        when(webServiceMock.getDocumentLinks(currentPageUri.toString())).thenThrow(new RuntimeException("WebService error"));

        // Act
        Assertions.assertThrows(RuntimeException.class, () -> crawler.crawl(currentPageUri));

        // Assert
        verify(webServiceMock, times(1)).getDocumentLinks(currentPageUri.toString());
    }
}