package com.qu.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.qu.commons.enums.QueueStatus;

import java.time.ZonedDateTime;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class QueueDto {
    public Long id;
    public Boolean autoAcceptEnabled;
    public Integer maxSize;
    public Boolean holdEnabled;
    public ZonedDateTime startTime;
    public ZonedDateTime endTime;
    public Long queueTypeId;
    public String name;
    public QueueStatus status;
}
