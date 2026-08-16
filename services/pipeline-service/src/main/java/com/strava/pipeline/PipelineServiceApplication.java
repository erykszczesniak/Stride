package com.strava.pipeline;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class PipelineServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PipelineServiceApplication.class, args);
    }
}
