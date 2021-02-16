package com.qu.services.queue.event;

import com.qu.dto.*;
import com.qu.services.queue.event.model.QueueEventHandlerInfo;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public interface QueueManagementService {
    Multi<QueueEventHandlerInfo<?>> getAllEventHandlers();

    Uni<Long> createQueueType(QueueTypeDto queueTemplate);

    Multi<QueueTypeDto> getQueueTypes();

    Uni<Long> createQueue(QueueDto queue);

    Uni<QueueListResponse>  getQueueList(QueueListParams params);

    Uni<QueueDetailsDto> getQueue(Long id);
}
