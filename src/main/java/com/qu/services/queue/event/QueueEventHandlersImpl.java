package com.qu.services.queue.event;

import com.qu.services.queue.event.model.QueueEventHandlerInfo;
import io.quarkus.runtime.StartupEvent;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Singleton;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

@ApplicationScoped
public class QueueEventHandlersImpl implements QueueEventHandlers {
    private static final Set<QueueEventHandlerInfo<?>> HANDLERS =
            Set.of();

    private Map<String, QueueEventHandlerInfo<?>> handlersMap;


    //we want this a singleton because it should be eagerly created at startup
    void startup(@Observes StartupEvent event) {
        createHandlersMap();
    }


    private void createHandlersMap(){
        handlersMap =
                HANDLERS
                .stream()
                .collect(Collectors.toMap(info -> info.name, info -> info));
    }


    @Override
    public Set<QueueEventHandlerInfo<?>> getActiveHandlers(){
        return HANDLERS;
    }



    public Map<String, QueueEventHandlerInfo<?>> getHandlersMap(){
        if(isNull(handlersMap)){
            createHandlersMap();
        }
        return handlersMap;
    }
}
