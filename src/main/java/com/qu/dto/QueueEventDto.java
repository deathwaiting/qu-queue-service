package com.qu.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.qu.services.QueueEventType;

import java.util.Map;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class QueueEventDto {
    public QueueEventType type;
    public String eventName;
    public Map<String, ?> commonParams;
}
