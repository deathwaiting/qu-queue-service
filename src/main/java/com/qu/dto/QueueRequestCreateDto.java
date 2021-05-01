package com.qu.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.Map;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class QueueRequestCreateDto {
    public String clientId;
    public Map<String,?> clientDetails;
    public Long queueId;
}
