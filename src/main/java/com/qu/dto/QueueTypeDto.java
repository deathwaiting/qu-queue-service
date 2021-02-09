package com.qu.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class QueueTypeDto {
    public Boolean defaultAutoAcceptEnabled;
    public Integer defaultMaxSize;
    public String name;
    public List<QueueEventDto> eventHandlers;
}
