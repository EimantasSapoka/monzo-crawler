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
* A cycle will be handled by not showing the url if it is already in hierarchy

## Running the application

Disclaimer: I've run it against some pages and there's so much output that i couldn't get it printing as JSON.
Even printing in text is a ton of output, so I'm writing it to file instead. It takes longer to write the file than to
crawl. 


