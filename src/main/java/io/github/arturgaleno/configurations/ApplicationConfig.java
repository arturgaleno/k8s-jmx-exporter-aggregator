package io.github.arturgaleno.configurations;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class ApplicationConfig {

    @Bean
    public ExecutorService executorService(@Value("${numThreads:10}") Integer numThreads) {
        return Executors.newFixedThreadPool(numThreads);
    }
}
