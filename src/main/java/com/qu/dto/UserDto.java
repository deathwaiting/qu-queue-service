package com.qu.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class UserDto {
    public String id;
    public String email;
    public String phone;
    public String name;
    public List<String> roles;
    public Map<String,String> extraDetails;
}
