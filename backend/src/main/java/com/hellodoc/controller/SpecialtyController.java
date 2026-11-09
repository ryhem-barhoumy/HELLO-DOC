package com.hellodoc.controller;
import com.hellodoc.dto.SpecialtyDto; import com.hellodoc.service.SpecialtyService; import org.springframework.web.bind.annotation.*; import java.util.List;
@RestController @RequestMapping("/api/specialties") public class SpecialtyController { private final SpecialtyService specialties; public SpecialtyController(SpecialtyService specialties){this.specialties=specialties;} @GetMapping public List<SpecialtyDto> list(){return specialties.list();} }
