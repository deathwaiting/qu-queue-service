package com.qu.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.time.ZonedDateTime;
import java.util.Map;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class QueueRequestDto {
    public ZonedDateTime requestTime;
    public String clientId;
    public Map<String,?> clientDetails;
    public Boolean refused;
    public String refuser;
    public ZonedDateTime responseTime;
}
