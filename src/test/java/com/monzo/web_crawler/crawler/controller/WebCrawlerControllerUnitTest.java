package com.monzo.web_crawler.crawler.controller;

import com.monzo.web_crawler.crawler.model.UrlNode;
import com.monzo.web_crawler.crawler.service.CrawlerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URI;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(MockitoExtension.class)
public class WebCrawlerControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CrawlerService crawlerService;

    @Test
    public void postCrawlerRequest_callsCrawlerService() throws Exception {
        // ARRANGE
        UrlNode rootNode = new UrlNode(URI.create("www.monzo.com"), null);
        UrlNode childNode = new UrlNode(URI.create("www.monzo.com/help"), rootNode);
        rootNode.addChild(childNode);
        Mockito.when(crawlerService.crawl(Mockito.eq(URI.create("https://www.monzo.com")))).thenReturn(rootNode);

        String requestBody = """
                {
                  "domain": "https://www.monzo.com"
                }
                """;

        String expectedResponse = """
                {
                  "url": "www.monzo.com",
                  "children": [
                       {
                        "url": "www.monzo.com/help",
                        "children": []
                       }
                  ]
                }
                """;


        // ACT / ASSERT

        this.mockMvc.perform(post("/api/v1/crawl").contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isOk())
                .andExpect(content().json(expectedResponse));

    }

    @Test
    public void postCrawlerRequest_invalidUriProvided_returnsBadRequestResponse() throws Exception {
        // ARRANGE

        // ACT / ASSERT

        this.mockMvc.perform(
                        post("/api/v1/crawl")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "domain": "test_not_uri"
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest());

        this.mockMvc.perform(
                        post("/api/v1/crawl")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "domain": "www.no-scheme.com"
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest());

        this.mockMvc.perform(
                        post("/api/v1/crawl")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "domain": "www.test"
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest());

        this.mockMvc.perform(
                        post("/api/v1/crawl")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "no_domain_field": ""
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest());

    }

}
