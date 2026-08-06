package com.example.staffagent;

import com.example.staffagent.config.KnowledgeBaseProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableFeignClients
@EnableConfigurationProperties(KnowledgeBaseProperties.class)
public class StaffAgentApplication {
    public static void main(String[] args) {
        SpringApplication.run(StaffAgentApplication.class, args);
    }
}