package com.qu.services.queue.event;

import com.qu.dto.QueueTypeDto;
import com.qu.services.queue.event.model.QueueEventHandlerInfo;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public interface QueueManagementService {
    Multi<QueueEventHandlerInfo<?>> getAllEventHandlers();

    Uni<Long> createQueueType(QueueTypeDto queueTemplate);
}
