package com.monzo.web_crawler.crawler.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URI;

public class URIUtilsUnitTest {

    @Test
    public void createUri_variousUrls_createsCorrectFormatterURIs() {
        // ARRANGE
        URI basePath = URI.create("https://monzo.com");


        // ACT / ASSERT
        Assertions.assertEquals("https://example.com", URIUtils.createUri(basePath, "https://example.com").toString());
        Assertions.assertEquals("https://example.com", URIUtils.createUri(basePath, "https://example.com/").toString());
        Assertions.assertEquals("http://example.org", URIUtils.createUri(basePath, "http://example.org").toString());
        Assertions.assertEquals(basePath + "/example.org", URIUtils.createUri(basePath, "example.org").toString());
        Assertions.assertEquals(basePath + "/www.example.org", URIUtils.createUri(basePath, "www.example.org").toString());
        Assertions.assertEquals(basePath + "/relative/path", URIUtils.createUri(basePath, "/relative/path").toString());
        Assertions.assertEquals(basePath + "/relative2.html", URIUtils.createUri(basePath, "./relative2.html").toString());
        Assertions.assertEquals(basePath + "/Link%20With%20Spaces", URIUtils.createUri(basePath, "/Link With Spaces").toString());
        Assertions.assertEquals("https://example.com/image-link.jpg", URIUtils.createUri(basePath, "https://example.com/image-link.jpg").toString());
        Assertions.assertEquals("https://example.com/page", URIUtils.createUri(basePath, "https://example.com/page?query=123").toString());
        Assertions.assertEquals("https://example.com/page", URIUtils.createUri(basePath, "https://example.com/page#fragment").toString());
        Assertions.assertEquals(basePath + "/", URIUtils.createUri(basePath, "#section").toString());

        // These href links are not supported
        Assertions.assertNull(URIUtils.createUri(basePath, "mailto:test@example.com"));
        Assertions.assertNull(URIUtils.createUri(basePath, "tel:+123456789"));
    }
}
