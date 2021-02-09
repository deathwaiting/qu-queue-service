package com.qu.services.queue.event;

import com.qu.services.queue.event.model.QueueEventHandlerInfo;
import io.smallrye.mutiny.Multi;

public interface QueueManagementService {
    Multi<QueueEventHandlerInfo<?>> getAllEventHandlers();
}
