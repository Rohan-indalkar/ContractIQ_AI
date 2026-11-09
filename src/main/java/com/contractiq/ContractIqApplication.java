package com.contractiq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableKafka
@EnableScheduling
public class ContractIqApplication {
    public static void main(String[] args) {
        SpringApplication.run(ContractIqApplication.class, args);
    }
}
