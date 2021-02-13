package com.qu.services.queue.event;

import com.qu.services.queue.event.model.QueueEventHandlerInfo;

import java.util.Set;

public interface QueueEventHandlers {
    Set<QueueEventHandlerInfo<?>> getActiveHandlers();
}
