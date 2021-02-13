package com.qu.services.queue.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qu.dto.QueueEventDto;
import com.qu.dto.QueueTypeDto;
import com.qu.exceptions.RuntimeBusinessException;
import com.qu.persistence.entities.QueueEventHandler;
import com.qu.persistence.entities.QueueType;
import com.qu.services.QueueEventPhase;
import com.qu.services.SecurityService;
import com.qu.services.queue.event.model.QueueEventHandlerInfo;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.jboss.logging.Logger;

import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.qu.commons.constants.Roles.QUEUE_ADMIN;
import static com.qu.exceptions.Errors.*;
import static com.qu.utils.Utils.anyIsNull;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_ACCEPTABLE;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.*;


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



    @Override
    @RolesAllowed(QUEUE_ADMIN)
    public Multi<QueueTypeDto> getQueueTypes() {
        var orgId = securityService.getUserOrganization();
        return QueueType
                .getByOrganization(orgId)
                .map(this::toQueueTypeDto);
    }




    private QueueTypeDto toQueueTypeDto(QueueType type) {
        var dto = new QueueTypeDto();
        dto.id = type.getId();
        dto.defaultAutoAcceptEnabled = type.getDefaultAutoAcceptEnabled();
        dto.defaultMaxSize = type.getDefaultMaxSize();
        dto.defaultHoldEnabled = type.getDefaultHoldEnabled();
        dto.name = type.getName();
        dto.eventHandlers = createEventHandlersDtoList(type);
        return dto;
    }



    private List<QueueEventDto> createEventHandlersDtoList(QueueType type) {
        return type
                .getEventHandlers()
                .stream()
                .map(this::toEventHandlerDto)
                .collect(toList());
    }




    private QueueEventDto toEventHandlerDto(QueueEventHandler eventDef) {
         var dto = new QueueEventDto();
         dto.eventHandlerName = eventDef.getName();
         dto.type = QueueEventPhase.valueOf(eventDef.getEventType());
        try {
            dto.commonParams = objectMapper.readValue(eventDef.getCommonData(), new TypeReference<Map<String, ?>>() {});
        } catch (JsonProcessingException e) {
            LOG.error(e,e);
            throw new RuntimeBusinessException(INTERNAL_SERVER_ERROR, E$GEN$00004, eventDef.getCommonData());
        }
        return dto;
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



    private Set<QueueEventHandler> createEventHandlers(QueueType entity, QueueTypeDto dto) {
        return dto
                .eventHandlers
                .stream()
                .map(handler -> createEventHandlerDto(entity, handler))
                .collect(toSet());
    }



    private QueueEventHandler createEventHandlerDto(QueueType queueType, QueueEventDto dto) {
        try {
            var commonParams = objectMapper.writeValueAsString(dto.commonParams);
            var type =
                    ofNullable(dto.type)
                    .map(QueueEventPhase::name)
                    .orElseThrow(() -> new RuntimeBusinessException(NOT_ACCEPTABLE, E$GEN$00001));
            var entity = new QueueEventHandler();
            entity.setQueueType(queueType);
            entity.setEventType(type);
            entity.setCommonData(commonParams);
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
                .map(QueueEventHandler.class::cast)
                .call(handler -> handler.delete())
                .collectItems()
                .asList()
                .onItem()
                    .invoke(item -> type.getEventHandlers().clear())
                .chain(() -> Uni.createFrom().voidItem());
    }
}
