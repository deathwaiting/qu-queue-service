package com.qu.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class UserDto {
    public String id;
    public String email;
    public String phone;
    public String name;
    public Long organizationId;
    public List<String> roles;
    public Map<String,String> extraDetails;
}
