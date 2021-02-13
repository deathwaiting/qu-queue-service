package com.qu.services.queue.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qu.commons.constants.Roles;
import com.qu.dto.QueueEventDto;
import com.qu.dto.QueueTypeDto;
import com.qu.exceptions.RuntimeBusinessException;
import com.qu.persistence.entities.QueueEventDefinition;
import com.qu.persistence.entities.QueueType;
import com.qu.services.QueueEventPhase;
import com.qu.services.SecurityService;
import com.qu.services.queue.event.model.QueueEventHandlerInfo;
import com.qu.utils.Utils;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.jboss.logging.Logger;
import org.jboss.logmanager.LogManager;

import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import java.time.Duration;
import java.util.Set;

import static com.qu.commons.constants.Roles.QUEUE_ADMIN;
import static com.qu.exceptions.Errors.E$GEN$00001;
import static com.qu.exceptions.Errors.E$QUE$00001;
import static com.qu.utils.Utils.anyIsNull;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_ACCEPTABLE;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toSet;


@ApplicationScoped
public class QueueManagementServiceImpl implements QueueManagementService{

    private static final Logger LOG = Logger.getLogger(QueueManagementServiceImpl.class);

    @Inject
    QueueEventHandlers handlers;

    @Inject
    SecurityService securityService;

    @Inject
    ObjectMapper objectMapper;

    @Override
    @RolesAllowed(QUEUE_ADMIN)
    public Multi<QueueEventHandlerInfo<?>> getAllEventHandlers() {
        var eventHandlers = handlers.getActiveHandlers();
        return Multi
                .createFrom()
                .<QueueEventHandlerInfo<?>>iterable(eventHandlers);
    }




    @Override
    @RolesAllowed(QUEUE_ADMIN)
    @Transactional
    public Uni<Long> createQueueType(QueueTypeDto dto) {
        validateQueueType(dto);
//        Long id = dto.id;
        return  ofNullable(dto.id)
                .map(id -> QueueType.<QueueType>findById(id))
                .orElse(Uni.createFrom().item(new QueueType()))
                .flatMap(entity -> prepareQueueTypeEntity(entity, dto))
                .flatMap(entity ->
                            entity
                            .persistAndFlush()
                            .chain(() -> Uni.createFrom().item(entity)))
                .map(QueueType::getId);
    }



    private void validateQueueType(QueueTypeDto dto) {
        if(anyIsNull(dto, dto.name)){
            throw new RuntimeBusinessException(NOT_ACCEPTABLE, E$GEN$00001);
        }
        dto.eventHandlers.forEach(this::validateEventDto);
    }



    private void validateEventDto(QueueEventDto dto) {
        if(anyIsNull(dto, dto.eventHandlerName, dto.type)){
            throw new RuntimeBusinessException(NOT_ACCEPTABLE, E$GEN$00001);
        }
    }


    private Uni<QueueType> prepareQueueTypeEntity(QueueType entity, QueueTypeDto dto) {
        return clearQueueTypeEvents(entity)
                .chain(() -> updateQueueTypeData(entity, dto));
    }



    private Uni<QueueType> updateQueueTypeData(QueueType entity, QueueTypeDto dto) {
        var holdEnabled = ofNullable(dto.defaultHoldEnabled).orElse(false);
        var autoAccept = ofNullable(dto.defaultAutoAcceptEnabled).orElse(true);
        var maxSize = ofNullable(dto.defaultMaxSize).orElse(Integer.MAX_VALUE);
        entity.setDefaultHoldEnabled(holdEnabled);
        entity.setDefaultAutoAcceptEnabled(autoAccept);
        entity.setDefaultMaxSize(maxSize);
        entity.setName(dto.name);
        entity.setOrganizationId(securityService.getUserOrganization());
        entity.setEventHandlers(createEventHandlers(entity, dto));
        return Uni.createFrom().item(entity);
    }



    private Set<QueueEventDefinition> createEventHandlers(QueueType entity, QueueTypeDto dto) {
        return dto
                .eventHandlers
                .stream()
                .map(handler -> createEventHandlerDto(entity, handler))
                .collect(toSet());
    }



    private QueueEventDefinition createEventHandlerDto(QueueType queueType, QueueEventDto dto) {
        try {
            var commonParams = objectMapper.writeValueAsString(dto.commonParams);
            var type =
                    ofNullable(dto.type)
                    .map(QueueEventPhase::name)
                    .orElseThrow(() -> new RuntimeBusinessException(NOT_ACCEPTABLE, E$GEN$00001));
            var entity = new QueueEventDefinition();
            entity.setQueueType(queueType);
            entity.setEventType(type);
            entity.setEventData(commonParams);
            entity.setName(dto.eventHandlerName);
            return entity;
        } catch (JsonProcessingException e) {
            LOG.error(e,e);
            throw new RuntimeBusinessException(NOT_ACCEPTABLE, E$QUE$00001, e.getCause());
        }
    }


    private Uni<Void> clearQueueTypeEvents(QueueType type) {
        return   Multi
                .createFrom()
                .iterable(type.getEventHandlers())
                .map(QueueEventDefinition.class::cast)
                .call(handler -> handler.delete())
                .collectItems()
                .asList()
                .onItem()
                    .invoke(item -> type.getEventHandlers().clear())
                .chain(() -> Uni.createFrom().voidItem());
    }
}
