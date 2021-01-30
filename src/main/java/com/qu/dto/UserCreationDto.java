package com.qu.dto;


import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class UserCreationDto extends UserDto{
    private String password;
}
