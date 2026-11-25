package com.hellodoc.dto;
import java.util.List;
public record IceServerDto(List<Server> iceServers){public record Server(List<String> urls,String username,String credential){}}
