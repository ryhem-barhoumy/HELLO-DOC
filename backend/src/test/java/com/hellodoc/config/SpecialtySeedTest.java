package com.hellodoc.config;

import com.hellodoc.entity.ApplicationSeedMarker;
import com.hellodoc.repository.ApplicationSeedMarkerRepository;
import com.hellodoc.service.SpecialtyService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import static org.mockito.Mockito.*;

class SpecialtySeedTest {
 @Test void initializedCatalogIsNotRepopulatedOnRestart() throws Exception {SpecialtyService service=mock(SpecialtyService.class);ApplicationSeedMarkerRepository markers=mock(ApplicationSeedMarkerRepository.class);when(markers.existsById("specialty-catalog-v1")).thenReturn(true);new SpecialtySeed().seedSpecialties(service,markers).run(mock(ApplicationArguments.class));verify(service).renameLegacy("ORL","Oto-rhino-laryngologie");verify(service,never()).findOrCreate(anyString());verify(markers,never()).save(any(ApplicationSeedMarker.class));}
 @Test void initialCatalogIsSeededAndMarkedOnce() throws Exception {SpecialtyService service=mock(SpecialtyService.class);ApplicationSeedMarkerRepository markers=mock(ApplicationSeedMarkerRepository.class);when(markers.existsById("specialty-catalog-v1")).thenReturn(false);new SpecialtySeed().seedSpecialties(service,markers).run(mock(ApplicationArguments.class));verify(service,times(12)).findOrCreate(anyString());verify(markers).save(any(ApplicationSeedMarker.class));}
}
