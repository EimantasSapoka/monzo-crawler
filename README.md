# monzo-scraper

## Requirements

* Given a starting URL, the crawler should visit each URL it finds on the same domain. 
* It should print each URL visited, and a list of links found on that page. 
* The crawler should be limited to one subdomain - so when you start with *https://monzo.com/*, it would crawl all pages on the monzo.com website, but not follow external links, for example to facebook.com or community.monzo.com.
* No use of crawler libraries / frameworks 
* Can use libraries to handle things like HTML parsing.
* Productionize code
