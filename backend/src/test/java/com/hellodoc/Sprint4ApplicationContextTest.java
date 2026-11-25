package com.hellodoc;

import com.hellodoc.config.TeleconsultationWebSocketConfiguration;
import com.hellodoc.controller.AppointmentMeetLinkController;
import com.hellodoc.controller.PrescriptionController;
import com.hellodoc.repository.TeleconsultationMessageRepository;
import com.hellodoc.repository.TeleconsultationSessionRepository;
import com.hellodoc.repository.PrescriptionRepository;
import com.hellodoc.web.TeleconsultationSignalHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/** Full application wiring check using only a disposable H2 database and test-only bootstrap values. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties={
        "spring.datasource.url=jdbc:h2:mem:hellodoc_sprint4_context",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "hellodoc.admin.email=sprint4-context@example.invalid",
        "hellodoc.admin.initial-password=isolated-context-test-password",
        "hellodoc.email.enabled=false",
        "hellodoc.frontend-origin=http://localhost:4200"
})
class Sprint4ApplicationContextTest {
    @Autowired ApplicationContext context;

    @Test void fullApplicationStartsWithJpaRepositoriesAndTeleconsultationWiring(){
        assertNotNull(context.getBean(PrescriptionRepository.class));
        assertNotNull(context.getBean(TeleconsultationMessageRepository.class));
        assertNotNull(context.getBean(TeleconsultationSessionRepository.class));
        assertNotNull(context.getBean(TeleconsultationSignalHandler.class));
        assertNotNull(context.getBean(TeleconsultationWebSocketConfiguration.class));
        assertNotNull(context.getBean(AppointmentMeetLinkController.class));
        assertNotNull(context.getBean(PrescriptionController.class));
    }
}
