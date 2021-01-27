--liquibase fromatted sql
--changeset ahmed:20210127-1 splitStatement:true endDelimiter:;
create table queue_type(
    id bigserial primary key,
    default_max_size integer,
    default_hold_enabled boolean,
    default_auto_accept_enabled boolean,
    organization_id bigint not null,
);



create table queue_event_definition(
    id bigserial primary key,
    event_type varchar(20),
    queue_type_id bigint references queue_type(id),
    event_data text
);


create table queue(
    id bigserial primary key,
    start_time timestamp with time zone non null,
    end_time timestamp with time zone,
    max_size integer,
    hold_enable boolean,
    auto_accept_enabled boolean,
    queue_type_id bigint references queue_type(id)
)


create table queue_actions(
    id bigserial primary key,
    action_time timestamp with time zone non null,
    action_type varchar(20)
);


create table queue_request(
    id bigserial primary key,
    client_id text,
    request_time timestamp with time zone non null,
    response_time timestamp with time zone,
    client_details text,
    queue_id bigint references queue(id),
    refused boolean default false
);



create table queue_turn(
    id bigserial primary key,
    enqueue_time timestamp with time zone non null,
    request_id bigint references queue_request(id),
    queue_number text not null
);


create table queue_leave(
    id bigserial primary key,
    queue_turn_id bigint references queue_turn(id),
    leave_time timestamp with time zone non null
);


create table queue_turn_insertion(
    id bigserial primary key,
    insert_before_turn bigint references queue_turn(id),
    inserted_turn bigint references queue_turn(id),
    inserted_by text non null
);



create table queue_turn_pick(
    id bigserial primary key,
    pick_time timestamp with time zone non null,
    skip_time timestamp with time zone non null,
    skip_reason varchar(1000),
    server_id text,
    server_details text
);