package com.hellodoc.dto;
import java.time.Instant; import java.util.Map;
public record ApiError(int status,String code,String message,Map<String,String> fieldErrors,String timestamp) { public static ApiError of(int status,String code,String message,Map<String,String> fields){return new ApiError(status,code,message,fields,Instant.now().toString());} }
