package com.proxyapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ProxyAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProxyAppApplication.class, args);
    }
}
