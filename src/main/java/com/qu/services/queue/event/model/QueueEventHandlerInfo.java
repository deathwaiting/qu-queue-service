package com.qu.services.queue.event.model;

import com.qu.services.QueueEventPhase;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class QueueEventHandlerInfo<T> {
    public String name;
    public String description;
    public Class<? extends T> parameterClass;
    public T parameterDefaults;
    public QueueEventPhase eventPhase;
}
