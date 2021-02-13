package com.qu.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.ArrayList;
import java.util.List;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class QueueTypeDto {
    public Long id;
    public Boolean defaultAutoAcceptEnabled;
    public Integer defaultMaxSize;
    public Boolean defaultHoldEnabled;
    public String name;
    public List<QueueEventDto> eventHandlers;

    public QueueTypeDto(){
        eventHandlers = new ArrayList<>();
    }
}
