package com.qu.commons.enums;

import lombok.Getter;

import static com.qu.commons.enums.QueueActionType.*;

@Getter
public enum QueueActions {
    SUSPEND(HOLD), START(QueueActionType.START), END(QueueActionType.END);

    private QueueActionType status;
    QueueActions(QueueActionType status){
        this.status = status;
    }
}
