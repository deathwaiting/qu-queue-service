package com.qu.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qu.commons.enums.QueueActions;
import com.qu.commons.enums.QueueActionType;
import com.qu.dto.*;
import com.qu.exceptions.Errors;
import com.qu.exceptions.RuntimeBusinessException;
import com.qu.mappers.QueueDtoMapper;
import com.qu.persistence.entities.*;
import com.qu.persistence.entities.Queue;
import com.qu.services.queue.event.QueueEventHandlers;
import com.qu.services.queue.event.model.QueueEventHandlerInfo;
import com.qu.vertx.events.number_generators.IntegerGenerator;
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.eventbus.EventBus;
import io.vertx.mutiny.core.eventbus.Message;
import lombok.Data;
import org.jboss.logging.Logger;

import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

import static com.qu.commons.constants.Roles.QUEUE_ADMIN;
import static com.qu.commons.constants.Roles.QUEUE_MANAGER;
import static com.qu.commons.enums.QueueActions.*;
import static com.qu.commons.enums.QueueActionType.CREATE;
import static com.qu.exceptions.Errors.*;
import static com.qu.utils.Utils.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static java.lang.String.format;
import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;
import static java.math.RoundingMode.FLOOR;
import static java.time.LocalDateTime.now;
import static java.util.Collections.emptySet;
import static java.util.Comparator.comparing;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.*;


@ApplicationScoped
public class QueueManagementServiceImpl implements QueueManagementService{

    private static final Logger LOG = Logger.getLogger(QueueManagementServiceImpl.class);
    private static final ZoneId UTC = ZoneId.of("UTC");
    private static final String DEFAULT_QU_NUM_GENERATOR = IntegerGenerator.NAME;

    @Inject
    QueueEventHandlers handlers;

    @Inject
    SecurityService securityService;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    QueueDtoMapper queueDtoMapper;

    @Inject
    EventBus eventBus;

    @Override
    @RolesAllowed(QUEUE_ADMIN)
    public Multi<QueueEventHandlerInfo<?>> getAllEventHandlers() {
        var eventHandlers = handlers.getActiveHandlers();
        return Multi
                .createFrom()
                .iterable(eventHandlers);
    }




    @Override
    @RolesAllowed(QUEUE_ADMIN)
    @Transactional
    public Uni<Long> createQueueType(QueueTypeDto dto) {
        validateQueueType(dto);
        return  ofNullable(dto.id)
                .map(QueueType::<QueueType>findById)
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




    @Override
    @RolesAllowed(QUEUE_ADMIN)
    public Uni<Long> createQueue(QueueDto queue) {
        validateQueue(queue);
        return QueueType
                .<QueueType>findById(queue.queueTypeId)
                    .onItem()
                    .ifNull()
                    .failWith(() -> new RuntimeBusinessException(NOT_ACCEPTABLE, E$QUE$00002))
                .map(type -> createQueueEntity(queue, type))
                .flatMap(entity ->
                        entity
                        .persistAndFlush()
                        .chain(() -> Uni.createFrom().item(entity)))
                .map(Queue::getId);
    }




    @Override
    @RolesAllowed(QUEUE_MANAGER)
    @Transactional
    public Uni<QueueListResponse> getQueueList(QueueListParams params) {
        Long orgId = securityService.getUserOrganization();
        return Queue
                .getQueuesByOrganization(orgId, params)
                .map(this::toQueueListResponse);
    }




    @Override
    @RolesAllowed(QUEUE_MANAGER)
    @Transactional
    public Uni<QueueDetailsDto> getQueue(Long id) {
        Long orgId = securityService.getUserOrganization();
        return Queue
                .getQueueFullDetailsById(id, orgId)
                .onItem()
                    .ifNotNull()
                    .transform(this::toQueueDetailsDto)
                .onItem()
                    .ifNull()
                    .failWith(new RuntimeBusinessException(NOT_FOUND, E$QUE$00003, id));
    }



    @Override
    @Transactional
    public Uni<Void> setQueueStatus(Long id, QueueActionType action) {
        Long orgId = securityService.getUserOrganization();
        return Queue
                .findByIdAndOrganizationId(id, orgId)
                    .onItem().ifNull().failWith(() -> new RuntimeBusinessException(NOT_FOUND, E$QUE$00003, id))
                .onItem().ifNotNull()
                    .invoke(qu -> validateNewAction(qu, action))
                    .map(qu -> createQueueAction(qu, action))
                    .flatMap(QueueAction::persistAndFlush);
    }



    @Override
    @RolesAllowed(QUEUE_MANAGER)
    @Transactional
    public Uni<QueueTurnDto> enqueue(QueueTurnCreateDto turn) {
        return createTurnRequestForAdmin(turn)
                .map(QueueRequestDto::getId)
                .flatMap(this::createTurn);
    }



    @Override
    @Transactional
    public Uni<QueueRequestAnswerDto> makeRequest(QueueRequestCreateDto request) {
        return createTurnRequestForClient(request)
                .flatMap(req -> {
                    if(req.autoAccept){
                        return createTurn(req.id)
                                .map(turn -> new QueueRequestAnswerDto(req, turn));
                    }else{
                        return Uni.createFrom().item( new QueueRequestAnswerDto(req, null));
                    }});
    }



    @Override
    @Transactional
    @RolesAllowed(QUEUE_MANAGER)
    public Uni<QueueTurnDto> acceptRequest(Long queueId, Long requestId) {
        var orgId = securityService.getUserOrganization();
        return QueueRequest
                .<QueueRequest>findByIdAndQueueAndOrganization(requestId, queueId, orgId)
                .onItem()
                    .ifNull()
                    .failWith(new RuntimeBusinessException(NOT_ACCEPTABLE, E$QUE$00008))
                .map(this::validateRequestCanBeHandled)
                .flatMap(this::doAcceptRequest);
    }



    @Override
    @Transactional
    @RolesAllowed(QUEUE_MANAGER)
    public Uni<Void> denyRequest(Long queueId, Long requestId) {
        var orgId = securityService.getUserOrganization();
        return QueueRequest
                .<QueueRequest>findByIdAndQueueAndOrganization(requestId, queueId, orgId)
                .onItem()
                .ifNull()
                .failWith(new RuntimeBusinessException(NOT_ACCEPTABLE, E$QUE$00008))
                .map(this::validateRequestCanBeHandled)
                .flatMap(this::doDenyRequest);
    }



    @Override
    @Transactional
    @RolesAllowed(QUEUE_MANAGER)
    public Uni<QueueTurnDto> dequeue(Long id) {
        return getQueue(id)
                .map(this::validateQueue)
                .map(QueueDetailsDto::getTurns)
                .toMulti()
                .flatMap(Multi.createFrom()::iterable)
                .filter(this::notPicked)
                .collectItems()
                .first()
                    .onItem().ifNull().failWith(new RuntimeBusinessException(NOT_ACCEPTABLE, E$QUE$00011))
                .flatMap(this::doDequeue);
    }



    @Override
    @Transactional
    public Uni<QueueTurnDto> skipTurn(Long id, String skipReason) {
        return  getQueue(id)
                .map(this::validateQueue)
                .map(QueueDetailsDto::getTurns)
                .toMulti()
                .flatMap(Multi.createFrom()::iterable)
                .filter(this::notPicked)
                .collectItems()
                .first()
                    .onItem().ifNull().failWith(new RuntimeBusinessException(NOT_ACCEPTABLE, E$QUE$00011))
                .flatMap(turn -> doTurnSkip(turn, skipReason));
    }



    @Override
    @Transactional
    @RolesAllowed(QUEUE_MANAGER)
    public Uni<Void> cancelTurn(Long queueId, Long turnId) {
        var orgId = securityService.getUserOrganization();
        return QueueTurn
                .findFullDataByIdAndOrgIdAndQuId(turnId, orgId, queueId)
                    .onItem().ifNull()
                        .failWith(new RuntimeBusinessException(NOT_ACCEPTABLE, E$QUE$00013))
                .onItem().ifNotNull()
                        .transform(this::validateTurnToCancel)
                        .flatMap(this::doCancelTurn)
                        .chain(Uni.createFrom()::voidItem);
    }



    @Override
    @Transactional
    public Uni<Void> cancelTurnByCustomer(Long queueId, Long turnId, String clientId) {
        return QueueTurn
                .findFullDataByIdAndClientIdAndQuId(turnId, clientId, queueId)
                    .onItem().ifNull()
                                .failWith(new RuntimeBusinessException(NOT_ACCEPTABLE, E$QUE$00013))
                .onItem().ifNotNull()
                .transform(this::validateTurnToCancel)
                .flatMap(this::doCancelTurn)
                .chain(Uni.createFrom()::voidItem);
    }


    private QueueTurn validateTurnToCancel(QueueTurn turn) {
        ofNullable(turn)
            .map(QueueTurn::getRequest)
            .map(QueueRequest::getQueue)
            .ifPresent(this::validateQueueIsOperational);
        if(anyNonNull(turn.getLeave(), turn.getPick())){
            throw new RuntimeBusinessException(NOT_ACCEPTABLE, E$QUE$00013);
        }
        return turn;
    }


    private Uni<QueueLeave> doCancelTurn(QueueTurn queueTurn) {
        var leave = new QueueLeave();
        leave.setTurn(queueTurn);
        leave.setLeaveTime(now());
        return leave.persistAndFlush().map(e -> leave);
    }


    private QueueDetailsDto validateQueue(QueueDetailsDto qu){
        if(!QueueActionType.START.equals(qu.status)){
            throw new RuntimeBusinessException(NOT_ACCEPTABLE, E$QUE$00012);
        }
        return qu;
    }


    private Uni<QueueTurnDto> doDequeue(QueueTurnDto dto) {
        return QueueTurn
                .<QueueTurn>findById(dto.id)
                .flatMap(this::dequeueTurn)
                .map(pick -> addPickInfo(dto, pick));
    }


    private Uni<QueueTurnDto> doTurnSkip(QueueTurnDto dto, String skipReason) {
        return QueueTurn
                .<QueueTurn>findById(dto.id)
                .flatMap(turn -> skipTurn(turn, skipReason))
                .map(pick -> addPickInfo(dto, pick));
    }



    private QueueTurnDto addPickInfo(QueueTurnDto dto, QueueTurnPick pick) {
        try {
            var pickerDetails = objectMapper.readValue(pick.getServerDetails(), new TypeReference<Map<String,Object>>(){});
            dto.setPicker(pick.getServerId());
            dto.setPickerDetails(pickerDetails);
            dto.setPickTime(pick.getPickTime());
            dto.setSkipTime(pick.getSkipTime());
            dto.setSkipReason(pick.getSkipReason());
            return dto;
        } catch (JsonProcessingException e) {
            LOG.error(e,e);
            throw new RuntimeBusinessException(INTERNAL_SERVER_ERROR, E$QUE$00010);
        }
    }


    private Uni<QueueTurnPick> dequeueTurn(QueueTurn turn) {
        var userDetails = securityService.getUserJwtDecoded();
        var userId = securityService.getUserId();
        var pick = new QueueTurnPick();
        pick.setPickTime(ZonedDateTime.now());
        pick.setTurn(turn);
        pick.setServerId(userId);
        pick.setServerDetails(userDetails);
        return  pick
                .persistAndFlush()
                .map( v -> pick);
    }



    private Uni<QueueTurnPick> skipTurn(QueueTurn turn, String skipReason) {
        var userDetails = securityService.getUserJwtDecoded();
        var userId = securityService.getUserId();
        var skip = new QueueTurnPick();
        skip.setSkipTime(ZonedDateTime.now());
        skip.setTurn(turn);
        skip.setServerId(userId);
        skip.setServerDetails(userDetails);
        skip.setSkipReason(skipReason);
        return  skip
                .persistAndFlush()
                .map( v -> skip);
    }



    private boolean notPicked(QueueTurnDto turn) {
        return allIsNull(turn.picker, turn.pickTime, turn.skipTime);
    }



    private Uni<QueueTurnDto> doAcceptRequest(QueueRequest queueRequest) {
        var user = securityService.getUserId();
        queueRequest.setAcceptorId(user);
        queueRequest.setResponseTime(now());
        queueRequest.setRefused(false);
        return queueRequest
                .persistAndFlush()
                .flatMap((e) -> createTurn(queueRequest.getId()));
    }



    private Uni<Void> doDenyRequest(QueueRequest queueRequest) {
        var user = securityService.getUserId();
        queueRequest.setAcceptorId(user);
        queueRequest.setResponseTime(now());
        queueRequest.setRefused(true);
        return queueRequest
                .persistAndFlush();
    }


    private QueueRequest validateRequestCanBeHandled(QueueRequest request) {
        var isHandled =
                ofNullable(request.getRefused()).orElse(false) ||
                        nonNull(request.getAcceptorId()) ||
                        nonNull(request.getResponseTime()) ||
                        nonNull(request.getTurn());
        if(isHandled){
            throw new RuntimeBusinessException(NOT_ACCEPTABLE, E$QUE$00009);
        }
        return request;
    }


    private Uni<QueueTurnDto> createTurn(Long requestId) {
        return QueueRequest
                .findFullDataById(requestId)
                .flatMap(this::getNewQueueNum)
                .flatMap( this::createTurnEntity)
                .map(this::toQueueTurnDto);
    }



    private Uni<QueueNumber> getNewQueueNum(QueueRequest request) {
        var quNumGenerator =
                ofNullable(request)
                        .map(QueueRequest::getQueue)
                        .map(Queue::getNumberGenerator)
                        .orElse(DEFAULT_QU_NUM_GENERATOR);
        return eventBus
                .<String>request(quNumGenerator, request)
                .map(Message::body)
                .map(num -> new QueueNumber(request,num));
    }


    private Uni<QueueTurn>  createTurnEntity(QueueNumber quNum) {
        var turn = new QueueTurn(quNum.request, quNum.number);
        return  turn.persistAndFlush()
                .onFailure().invoke(LOG::error)
                .onItem().transformToUni(v -> updateResponseTime(quNum.request, turn))
                .onItem().transform(v -> turn);
    }



    private Uni<Void> updateResponseTime(QueueRequest request, QueueTurn turn) {
        request.setResponseTime(turn.getEnqueueTime());
        return request.persistAndFlush();
    }


    private Uni<QueueRequestDto> createTurnRequestForAdmin(QueueRequestCreateDto turn) {
        var orgId = securityService.getUserOrganization();
        var qu = Queue.getQueueFullDetailsById(turn.queueId, orgId);
        return createTurnRequest(turn, qu);
    }



    private Uni<QueueRequestDto> createTurnRequestForClient(QueueRequestCreateDto turn) {
        var qu = Queue.<Queue>getQueueFullDetailsById(turn.queueId);
        return createTurnRequest(turn, qu);
    }



    private Uni<QueueRequestDto> createTurnRequest(QueueRequestCreateDto turn, Uni<Queue> queue) {
        return queue
                .onItem().ifNull()
                .failWith(() -> new RuntimeBusinessException(NOT_FOUND, E$QUE$00003, turn.queueId))
                .onItem().ifNotNull()
                    .transform(this::validateQueueForNewRequest)
                    .flatMap(qu -> createTurnRequestEntity(turn, qu))
                    .map(this::toQueueRequestDto);
    }



    private Queue validateQueueForNewRequest(Queue qu) {
        validateQueueIsOperational(qu);
        validateQueueIsNotFull(qu);
        return qu;
    }


    private void validateQueueIsNotFull(Queue qu) {
        var maxQuSize = ofNullable(qu.getMaxSize()).orElse(0);
        if(getQueueSize(qu) >= maxQuSize){
            throw new RuntimeBusinessException(NOT_ACCEPTABLE, E$QUE$00007);
        }
    }


    private void validateQueueIsOperational(Queue qu) {
        var status = getQueueStatus(qu);
        if(!QueueActionType.START.equals(status)){
            throw new RuntimeBusinessException(NOT_ACCEPTABLE, E$QUE$00012);
        }
    }


    private Uni<QueueRequest> createTurnRequestEntity(QueueRequestCreateDto request, Queue qu) {
        var clientDetails = writeMapAsJson(request.clientDetails);
        var clientId = ofNullable(request.clientId).orElseGet(() -> createRandomClientId(qu));
        var entity = new QueueRequest();
        entity.setQueue(qu);
        entity.setClientId(clientId);
        entity.setClientDetails(clientDetails);
        return entity
                .persistAndFlush()
                .chain(() -> Uni.createFrom().item(entity));
    }



    private String createRandomClientId(Queue qu) {
        var uuid = UUID.randomUUID().toString();
        return format("client-qu[%d]-%s", qu.getId(), uuid);
    }


    private QueueAction createQueueAction(Queue qu, QueueActionType actionType) {
        var action = new QueueAction();
        action.setActionType(actionType.name());
        action.setQueue(qu);
        return action;
    }



    private void validateNewAction(Queue qu, QueueActionType action) {
        var lastAction = getLastAction(qu);
        if(!lastAction.canHaveNextStatusOf(action)){
            throw new RuntimeBusinessException(NOT_ACCEPTABLE, E$QUE$00004, lastAction.name(), action.name());
        }
    }



    private QueueActionType getLastAction(Queue qu) {
        return ofNullable(qu.getActions())
                .orElse(emptySet())
                .stream()
                .max(comparing(QueueAction::getActionTime))
                .map(QueueAction::getActionType)
                .map(QueueActionType::valueOf)
                .orElse(CREATE);
    }


    private QueueDetailsDto toQueueDetailsDto(Queue queue) {
        var actions = getQueueActions(queue);
        var requests = getQueueRequests(queue);
        var baseInfo = toQueueDto(queue);
        var turns = getQueueTurns(queue);
        var detailedDto = queueDtoMapper.toQueueDetailedDto(baseInfo);
        detailedDto.actions = actions;
        detailedDto.requests = requests;
        detailedDto.turns = turns;
        return detailedDto;
    }




    private List<QueueTurnDto> getQueueTurns(Queue queue) {
        return queue
                .getRequests()
                .stream()
                .map(QueueRequest::getTurn)
                .filter(Objects::nonNull)
                .map(this::toQueueTurnDto)
                .sorted(comparing(this::getTurnOrderingScore))
                .collect(toList());
    }



    private Long getQueueSize(Queue queue) {
        return queue
                .getRequests()
                .stream()
                .map(QueueRequest::getTurn)
                .filter(Objects::nonNull)
                .count();
    }



    private BigDecimal getTurnOrderingScore(QueueTurnDto turn) {
        return ofNullable(turn)
                    .map(QueueTurnDto::getTurnAfter)
                    .map(turnAfter -> calcOrderScore(turn))
                    .or( () -> ofNullable(turn)
                                .map(QueueTurnDto::getId)
                                .map(BigDecimal::new))
                    .orElse(ZERO);
    }



    private BigDecimal calcOrderScore(QueueTurnDto turn) {
        var inverseTime = ONE.divide(new BigDecimal(turn.enqueueTime.toEpochSecond()), 10, FLOOR);
        return new BigDecimal(turn.turnAfter).subtract(inverseTime);
    }



    private QueueTurnDto toQueueTurnDto(QueueTurn turn) {
        var dto = new QueueTurnDto();
        dto.id = turn.getId();
        dto.acceptorId = turn.getRequest().getAcceptorId();
        dto.clientDetails = parseJsonAsMap(turn.getRequest().getClientDetails());
        dto.number = turn.getQueueNumber();
        dto.clientId = turn.getRequest().getClientId();
        dto.turnAfter = ofNullable(turn.getTurnMove()).map(QueueTurnMove::getInsertedBeforeTurnId).orElse(null);
        ofNullable(turn.getPick()).map(QueueTurnPick::getSkipTime).ifPresent(time -> dto.skipTime = time);
        ofNullable(turn.getPick()).map(QueueTurnPick::getSkipReason).ifPresent(reason -> dto.skipReason = reason);
        ofNullable(turn.getPick()).map(QueueTurnPick::getPickTime).ifPresent(time -> dto.pickTime = time);
        ofNullable(turn.getPick()).map(QueueTurnPick::getServerId).ifPresent(picker -> dto.picker = picker);
        ofNullable(turn.getLeave()).map(QueueLeave::getLeaveTime).ifPresent(time -> dto.leaveTime = time.atZone(UTC));
        ofNullable(turn.getEnqueueTime()).ifPresent( time -> dto.enqueueTime = time.atZone(UTC));
        ofNullable(turn.getPick())
                .map(QueueTurnPick::getServerDetails)
                .map(this::parseJsonAsMap)
                .ifPresent(details -> dto.pickerDetails = details);
        return dto;
    }



    private List<QueueRequestDto> getQueueRequests(Queue queue) {
        return queue
                .getRequests()
                .stream()
                .sorted(comparing(QueueRequest::getRequestTime))
                .map(this::toQueueRequestDto)
                .collect(toList());
    }



    private QueueRequestDto toQueueRequestDto(QueueRequest request) {
        var dto = new QueueRequestDto();
        dto.clientId = request.getClientId();
        dto.clientDetails = parseJsonAsMap(request.getClientDetails());
        dto.requestTime = request.getRequestTime().atZone(UTC);
        dto.refused = ofNullable(request.getRefused()).orElse(false);
        dto.refuser = request.getAcceptorId();
        dto.id = request.getId();
        dto.queueId = request.getQueue().getId();
        dto.autoAccept = request.getQueue().getAutoAcceptEnabled();
        ofNullable(request.getResponseTime())
                .map(time -> time.atZone(UTC))
                .ifPresent(dto::setResponseTime);
        return dto;
    }



    private List<QueueActionDto> getQueueActions(Queue queue) {
        return queue
                .getActions()
                .stream()
                .map(this::toQueueActionDto)
                .collect(toList());
    }




    private QueueActionDto toQueueActionDto(QueueAction queueAction) {
        var dto = new QueueActionDto();
        ofNullable(queueAction.getActionType()).map(QueueActions::valueOf).ifPresent(action -> dto.action = action);
        dto.actionTime = queueAction.getActionTime().atZone(UTC);
        return dto;
    }



    private QueueListResponse toQueueListResponse(Queue.QueueListPage page) {
        var data = page.page.stream().map(this::toQueueDto).collect(toList());
        return new QueueListResponse(page.totalPagesCount, data);
    }




    private QueueDto toQueueDto(Queue entity) {
        var dto = new QueueDto();
        dto.autoAcceptEnabled = entity.getAutoAcceptEnabled();
        dto.endTime = entity.getEndTime().atZone(UTC);
        dto.id = entity.getId();
        dto.name = entity.getName();
        dto.holdEnabled = entity.getHoldEnabled();
        dto.maxSize = entity.getMaxSize();
        dto.queueTypeId = entity.getType().getId();
        dto.startTime = entity.getStartTime().atZone(UTC);
        dto.status = getQueueStatus(entity);
        return dto;
    }



    private QueueActionType getQueueStatus(Queue entity) {
        return entity
                .getActions()
                .stream()
                .sorted(comparing(QueueAction::getActionTime).reversed()
                        .thenComparing(QueueAction::getId).reversed())
                .map(QueueAction::getActionType)
                .map(QueueActions::valueOf)
                .filter(action -> Set.of(SUSPEND, END, START).contains(action))
                .map(QueueActions::getStatus)
                .findFirst()
                .orElse(CREATE);
    }


    private Queue createQueueEntity(QueueDto queue, QueueType type) {
        var endTime = queue.endTime.withZoneSameInstant(UTC).toLocalDateTime();
        var startTime = queue.startTime.withZoneSameInstant(UTC).toLocalDateTime();
        var autoAccept = ofNullable(queue.autoAcceptEnabled).orElse(type.getDefaultAutoAcceptEnabled());
        var holdAccept = ofNullable(queue.holdEnabled).orElse(type.getDefaultHoldEnabled());
        var maxSize =  ofNullable(queue.maxSize).orElse(type.getDefaultMaxSize());
        var name = ofNullable(queue.name).orElse(type.getName());

        var entity = new Queue();
        entity.setType(type);
        entity.setEndTime(endTime);
        entity.setStartTime(startTime);
        entity.setAutoAcceptEnabled(autoAccept);
        entity.setHoldEnabled(holdAccept);
        entity.setMaxSize(maxSize);
        entity.setName(name);
        return entity;
    }



    private void validateQueue(QueueDto queue) {
        if(anyIsNull(queue, queue.endTime, queue.startTime, queue.queueTypeId)){
            throw new RuntimeBusinessException(NOT_ACCEPTABLE, E$GEN$00001);
        }
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
        var commonData = eventDef.getCommonData();
        dto.commonParams = parseJsonAsMap(commonData);
        return dto;
    }



    private Map<String, ?> parseJsonAsMap(String commonData) {
        try {
             return objectMapper.readValue(commonData, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            LOG.error(e,e);
            throw new RuntimeBusinessException(INTERNAL_SERVER_ERROR, E$GEN$00004, commonData);
        }
    }


    private String writeMapAsJson(Map<String,?> map){
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            LOG.error(e,e);
            throw new RuntimeBusinessException(INTERNAL_SERVER_ERROR, E$GEN$00003, map);
        }
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
                .call(PanacheEntityBase::delete)
                .collectItems()
                .asList()
                .onItem()
                    .invoke(item -> type.getEventHandlers().clear())
                .chain(() -> Uni.createFrom().voidItem());
    }
}


@Data
class QueueNumber{
    QueueRequest request;
    String number;

    public QueueNumber(QueueRequest request, String number) {
        this.request = request;
        this.number = number;
    }
}
