package com.monzo.web_crawler.crawler.model;


import lombok.Getter;

import java.net.URI;
import java.util.Set;

@Getter
public class Page {

    private final URI url;

    private final Set<URI> children;

    public Page(URI url, Set<URI> children) {
        this.url = url;
        this.children = children;
    }

}
