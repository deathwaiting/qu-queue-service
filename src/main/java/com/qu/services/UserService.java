package com.qu.services;

import com.qu.dto.UserCreationDto;
import com.qu.dto.UserDto;
import io.smallrye.mutiny.Uni;

public interface UserService {
    Uni<UserDto> createOrganizationOwner(UserCreationDto owner);
}
