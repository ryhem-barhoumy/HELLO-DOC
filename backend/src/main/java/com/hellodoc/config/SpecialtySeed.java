package com.hellodoc.config;

import com.hellodoc.entity.ApplicationSeedMarker;
import com.hellodoc.repository.ApplicationSeedMarkerRepository;
import com.hellodoc.service.SpecialtyService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.util.List;

@Configuration
public class SpecialtySeed {
    private static final String SEED_MARKER = "specialty-catalog-v1";
    private static final List<String> DEFAULT_SPECIALTIES = List.of(
            "Médecine générale", "Cardiologie", "Dermatologie", "Gynécologie", "Pédiatrie",
            "Ophtalmologie", "Oto-rhino-laryngologie", "Neurologie", "Psychiatrie",
            "Orthopédie", "Urologie", "Odontologie");

    @Bean @Order(0)
    ApplicationRunner seedSpecialties(SpecialtyService specialties, ApplicationSeedMarkerRepository markers) {
        return args -> {
            specialties.renameLegacy("ORL", "Oto-rhino-laryngologie");
            if (markers.existsById(SEED_MARKER)) return;
            DEFAULT_SPECIALTIES.forEach(specialties::findOrCreate);
            markers.save(new ApplicationSeedMarker(SEED_MARKER));
        };
    }
}
