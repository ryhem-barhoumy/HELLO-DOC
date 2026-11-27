package com.hellodoc.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class MailDeliveryConfiguration {
    @Bean
    ThreadPoolTaskExecutor passwordRecoveryExecutor() {
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(32);
        executor.setThreadNamePrefix("password-recovery-mail-");
        return executor;
    }

    @Bean
    ApplicationRunner reportMailConfiguration(@Value("${hellodoc.email.enabled:false}") boolean enabled,
            @Value("${spring.mail.host:}") String host) {
        return args -> {
            if(!enabled) System.getLogger(getClass().getName()).log(System.Logger.Level.WARNING,
                    "Courriels d’approbation désactivés. Configurez HELLODOC_EMAIL_ENABLED=true et les variables SMTP pour notifier les médecins.");
            else if(host==null||host.isBlank()) System.getLogger(getClass().getName()).log(System.Logger.Level.ERROR,
                    "Courriels d’approbation activés mais HELLODOC_SMTP_HOST est vide; aucun courriel ne sera envoyé.");
        };
    }
}
