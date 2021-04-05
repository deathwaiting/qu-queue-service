package com.qu.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.util.List;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@Data
public class QueueDetailsDto extends QueueDto{
    public List<QueueActionDto> actions;
    public List<QueueRequestDto> requests;
    public List<QueueTurnDto> turns;
}
