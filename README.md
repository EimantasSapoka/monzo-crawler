# monzo-scraper

## Requirements

* Given a starting URL, the crawler should visit each URL it finds on the same domain. 
* It should print each URL visited, and a list of links found on that page. 
* The crawler should be limited to one subdomain - so when you start with *https://monzo.com/*, it would crawl all pages on the monzo.com website, but not follow external links, for example to facebook.com or community.monzo.com.
* No use of crawler libraries / frameworks 
* Can use libraries to handle things like HTML parsing.
* Productionize code


## Running the application

Requires java 23. Within the project directory where web-crawler.jar is run

```java -jar web-crawler.jar```

navigate to http://localhost:8080/swagger-ui/index.html#/web-crawler-controller/crawl

enter a url e.g. https://monzo.com in there and click execute.

to change number of threads used pass -Dcrawler.thread-pool-size=5

```java -jar web-crawler.jar -Dcrawler.thread-pool-size=5```

