package com.monzo.web_crawler.crawler.model;


import lombok.Getter;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Getter

public class UrlNode {

    private final URI url;
    private final UrlNode parent;
    private final List<UrlNode> children = new ArrayList<>();

    public UrlNode(URI url, UrlNode parent) {
        this.url = url;
        this.parent = parent;
    }

    public void addChild(UrlNode urlNode) {
        this.children.add(urlNode);
    }

    /**
     * Checks if given uri is equal to this node or any parent in ancestry.
     * Used to prevent cycles in crawling
     *
     * @param uri to check if in ancestry
     * @return true if uri is in ancestry or false if not
     */
    public boolean hasAncestor(URI uri) {
        if (url.equals(uri)) {
            return true;
        }
        if (parent == null) {
            return false;
        } else {
            return parent.hasAncestor(uri);
        }
    }

    public boolean containsChild(URI uri) {
        return children.stream().anyMatch(child -> child.getUrl().equals(uri));
    }

}
