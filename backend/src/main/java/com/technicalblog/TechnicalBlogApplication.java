package com.technicalblog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TechnicalBlogApplication {

    public static void main(String[] args) {
        SpringApplication.run(TechnicalBlogApplication.class, args);
    }
}
