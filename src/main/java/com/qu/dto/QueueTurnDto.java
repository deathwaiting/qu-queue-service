package com.qu.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class QueueTurnDto {
    public String clientId;
    public Map<String,?> clientDetails;
    public ZonedDateTime enqueueTime;
    public String acceptorId;
    public String number;
    public ZonedDateTime leaveTime;
    public ZonedDateTime pickTime;
    public ZonedDateTime skipTime;
    public String skipReason;
    public String picker;
    public Map<String,?> pickerDetails;
    @JsonIgnore
    public Optional<Long> turnAfter;
}
