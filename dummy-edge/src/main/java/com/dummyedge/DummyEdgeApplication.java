package com.dummyedge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class DummyEdgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(DummyEdgeApplication.class, args);
    }
}
