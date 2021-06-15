package com.qu.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;

@Data
public class QueueTurnDto {
    public Long id;
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
    public Long turnAfter;
}
