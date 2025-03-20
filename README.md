# monzo-scraper

## Requirements

* Given a starting URL, the crawler should visit each URL it finds on the same domain. 
* It should print each URL visited, and a list of links found on that page. 
* The crawler should be limited to one subdomain - so when you start with *https://monzo.com/*, it would crawl all pages on the monzo.com website, but not follow external links, for example to facebook.com or community.monzo.com.
* No use of crawler libraries / frameworks 
* Can use libraries to handle things like HTML parsing.
* Productionize code

## Assumptions

* A page will not be listed as having itself as a child. I.e. if a page contains a link to itself it will not be shown
* A cycle will be handled by showing the URL as a child, but not it's children.

e.g. in page A has children B and C and page B has child A, the page A will show up as child of B but no children of A
will be shown