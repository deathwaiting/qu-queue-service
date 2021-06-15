package com.qu.dto;


import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class QueueRequestAnswerDto {
    public QueueRequestDto request;
    public QueueTurnDto turn;

    public QueueRequestAnswerDto(QueueRequestDto request, QueueTurnDto turn) {
        this.turn = turn;
        this.request = request;
    }
}
