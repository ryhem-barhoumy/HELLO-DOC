package com.hellodoc.service;
import com.hellodoc.dto.SpecialtyDto; import com.hellodoc.entity.Specialty; import com.hellodoc.repository.SpecialtyRepository; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional; import java.util.*;
@Service public class SpecialtyService {
 private final SpecialtyRepository specialties; public SpecialtyService(SpecialtyRepository specialties){this.specialties=specialties;}
 @Transactional(readOnly=true) public List<SpecialtyDto> list(){return specialties.findAllByOrderByNameAsc().stream().map(SpecialtyDto::from).toList();}
 @Transactional public void renameLegacy(String oldName,String fullName){String oldKey=Specialty.normalize(oldName),newKey=Specialty.normalize(fullName);var legacy=specialties.findByNormalizedName(oldKey);if(legacy.isPresent()&&specialties.findByNormalizedName(newKey).isEmpty()){legacy.get().rename(fullName);specialties.save(legacy.get());}}
 @Transactional public Specialty findOrCreate(String name){String clean=name==null?"":name.trim().replaceAll("\\s+"," ");if(clean.isBlank())throw new IllegalArgumentException("La spécialité est obligatoire.");String key=Specialty.normalize(clean);return specialties.findByNormalizedName(key).orElseGet(()->specialties.save(new Specialty(clean)));}
}
