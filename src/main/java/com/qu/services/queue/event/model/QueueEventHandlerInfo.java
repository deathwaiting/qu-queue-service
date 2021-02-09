package com.qu.services.queue.event.model;

import lombok.Data;

public class QueueEventHandlerInfo<T> {
    public String name;
    public String description;
    public Class<? extends T> parameterClass;
    public T parameterDefaults;
}
