package com.hellodoc.config;
import com.hellodoc.service.SpecialtyService; import org.springframework.boot.ApplicationRunner; import org.springframework.context.annotation.*; import org.springframework.core.annotation.Order; import java.util.List;
@Configuration public class SpecialtySeed {
 @Bean @Order(0) ApplicationRunner seedSpecialties(SpecialtyService specialties){return args->{specialties.renameLegacy("ORL","Oto-rhino-laryngologie");for(String name:List.of("Médecine générale","Cardiologie","Dermatologie","Gynécologie","Pédiatrie","Ophtalmologie","Oto-rhino-laryngologie","Neurologie","Psychiatrie","Orthopédie","Urologie","Odontologie"))specialties.findOrCreate(name);};}
}
