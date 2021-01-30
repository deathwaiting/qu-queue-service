package com.qu.mappers;

import com.qu.dto.UserCreationDto;
import com.qu.dto.UserDto;
import org.mapstruct.Mapper;

@Mapper(config = QuarkusMappingConfig.class)
public interface UserDtoMapper {
    UserDto clone(UserDto source);
    UserDto toUserDto(UserCreationDto creationDto);
}
