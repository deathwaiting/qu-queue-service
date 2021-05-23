package com.qu.services;

import com.qu.commons.enums.QueueActionType;
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

    Uni<Void> setQueueStatus(Long id, QueueActionType action);

    Uni<QueueTurnDto> enqueue(QueueTurnCreateDto turn);

    Uni<QueueRequestAnswerDto> makeRequest(QueueRequestCreateDto request);
}
