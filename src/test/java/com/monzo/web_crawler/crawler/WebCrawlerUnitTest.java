package com.monzo.web_crawler.crawler;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class WebCrawlerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void postCrawlerRequest_callsCrawlerService() throws Exception {
        // ARRANGE
        String requestBody = """
                              {
                                "domain": "www.monzo.com"
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

        this.mockMvc.perform(post("/crawl").contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isOk())
                .andExpect(content().json(expectedResponse));

    }

}
