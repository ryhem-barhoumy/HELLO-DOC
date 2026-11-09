package com.hellodoc.dto;
import com.hellodoc.entity.Specialty; import java.util.UUID;
public record SpecialtyDto(UUID id,String name) { public static SpecialtyDto from(Specialty s){return new SpecialtyDto(s.getId(),s.getName());} }
