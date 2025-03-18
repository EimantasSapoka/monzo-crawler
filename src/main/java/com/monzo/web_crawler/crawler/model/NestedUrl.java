package com.monzo.web_crawler.crawler.model;


import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Getter
@RequiredArgsConstructor
public class NestedUrl {

    private final URI url;
    private final List<NestedUrl> children;

    public NestedUrl(URI url) {
        this(url, new ArrayList<>());
    }

    public void addChild(NestedUrl nestedUrl) {
        this.children.add(nestedUrl);
    }

}
