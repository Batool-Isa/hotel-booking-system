package com.ga.hotel_booking_app.config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncEmailConfig {

    @Bean(name = "emailThreadExecutor")
    public Executor emailExecutor(){
        ThreadPoolTaskExecutor emailExecutor = new ThreadPoolTaskExecutor();
        emailExecutor.setCorePoolSize(5); // minimum size
        emailExecutor.setMaxPoolSize(7); // maximum size
        emailExecutor.setQueueCapacity(20);
        emailExecutor.setThreadNamePrefix("email-worker-thread-");
        emailExecutor.initialize();
        return emailExecutor;
    }



}
