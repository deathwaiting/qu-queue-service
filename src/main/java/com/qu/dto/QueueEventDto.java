package com.qu.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.qu.services.QueueEventPhase;
import lombok.Data;

import java.util.Map;

public class QueueEventDto {
    public QueueEventPhase type;
    public String eventHandlerName;
    public Map<String, ?> commonParams;
}
