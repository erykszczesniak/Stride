package com.strava.clubevent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class ClubEventServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClubEventServiceApplication.class, args);
    }
}
