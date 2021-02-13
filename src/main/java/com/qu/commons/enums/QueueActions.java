package com.qu.commons.enums;

import lombok.Getter;

import static com.qu.commons.enums.QueueStatus.*;

@Getter
public enum QueueActions {
    SUSPEND(SUSPENDED), START(ACTIVE), END(ENDED);

    private QueueStatus status;
    QueueActions(QueueStatus status){
        this.status = status;
    }
}
