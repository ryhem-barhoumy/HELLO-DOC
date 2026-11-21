package com.hellodoc.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class ApprovalEmailExecutorConfiguration {
    @Bean(name="doctorApprovalEmailExecutor")
    ThreadPoolTaskExecutor doctorApprovalEmailExecutor() {
        ThreadPoolTaskExecutor executor=new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);executor.setMaxPoolSize(2);executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("doctor-approval-mail-");
        executor.setWaitForTasksToCompleteOnShutdown(true);executor.setAwaitTerminationSeconds(20);
        return executor;
    }
}
