package com.ctailee.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SpringBootApplication
@RestController
public class Main {
    public static void main(String[] args){
        SpringApplication.run(Main.class, args);
    }

    @GetMapping("/health")
    public Map<String, String> healthCheck(){
        Map<String, String> response = Map.of(
                "message", "ok"
        );

        return response;
    }
}