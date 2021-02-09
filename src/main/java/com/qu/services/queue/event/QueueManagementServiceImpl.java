package com.qu.services.queue.event;

import com.qu.commons.constants.Roles;
import com.qu.services.queue.event.model.QueueEventHandlerInfo;
import io.smallrye.mutiny.Multi;

import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import static com.qu.commons.constants.Roles.QUEUE_ADMIN;


@ApplicationScoped
public class QueueManagementServiceImpl implements QueueManagementService{
    @Inject
    QueueEventHandlers handlers;

    @Override
    @RolesAllowed(QUEUE_ADMIN)
    public Multi<QueueEventHandlerInfo<?>> getAllEventHandlers() {
        return null;
    }
}
