package com.qu.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.qu.commons.enums.QueueActions;

import java.time.ZonedDateTime;

public class QueueActionDto {
    public QueueActions action;
    public ZonedDateTime actionTime;
}
