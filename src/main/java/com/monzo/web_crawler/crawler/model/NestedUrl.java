package com.monzo.web_crawler.crawler.model;


import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@RequiredArgsConstructor
public class NestedUrl {

    private final String url;
    private final List<NestedUrl> children;

    public NestedUrl(String url) {
        this(url, new ArrayList<>());
    }

}
