package com.qu.mappers;

import com.qu.dto.QueueDetailsDto;
import com.qu.dto.QueueDto;
import com.qu.dto.UserCreationDto;
import com.qu.dto.UserDto;
import org.mapstruct.Mapper;

@Mapper(config = QuarkusMappingConfig.class)
public interface QueueDtoMapper {
    QueueDto clone(QueueDto source);
    QueueDetailsDto toQueueDetailedDto(QueueDto source);
}
