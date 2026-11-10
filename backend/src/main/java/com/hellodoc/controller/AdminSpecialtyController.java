package com.hellodoc.controller;

import com.hellodoc.dto.SpecialtyDto;
import com.hellodoc.dto.SpecialtyRequest;
import com.hellodoc.service.SpecialtyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/specialties")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSpecialtyController {
    private final SpecialtyService specialties;

    public AdminSpecialtyController(SpecialtyService specialties) {
        this.specialties = specialties;
    }

    @GetMapping
    public List<SpecialtyDto> list() {
        return specialties.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SpecialtyDto create(@Valid @RequestBody SpecialtyRequest request) {
        return SpecialtyDto.from(specialties.create(request.name()));
    }

    @PutMapping("/{id}")
    public SpecialtyDto update(@PathVariable UUID id, @Valid @RequestBody SpecialtyRequest request) {
        return SpecialtyDto.from(specialties.update(id, request.name()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        specialties.delete(id);
    }
}
