package com.emmanuelfinance.transaction;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class SchedulerConfig {

    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(10); // quantidades de threads simultâneas para agendamento
        scheduler.setThreadNamePrefix("transaction-scheduler");
        scheduler.initialize();
        return scheduler;
    }
}
