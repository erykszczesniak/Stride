package com.strava.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.elasticsearch.ReactiveElasticsearchRepositoriesAutoConfiguration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

// Reactive Elasticsearch is unused (this is an MVC app with imperative ES repositories);
// excluding it stops the reactive module from scanning every JPA repository at startup.
@SpringBootApplication(exclude = ReactiveElasticsearchRepositoriesAutoConfiguration.class)
@EnableScheduling
@EnableAsync
public class StravaApplication {

    public static void main(String[] args) {
        SpringApplication.run(StravaApplication.class, args);
    }
}
