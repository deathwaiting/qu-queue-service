insert into organization ("id", "name", owner_data, owner_id, subscription_type)
values (8888, 'test_org',
  '{"name": "fakeman", "id": "owner@fake.com", "organizationId": 8888, "email": "owner@fake.com"}',
  'owner@fake.com',
  'free')
;

insert into queue_type ( "id", default_auto_accept_enabled, default_hold_enabled, default_max_size, "name",
    organization_id)
values ( 88888, true, true, 80, 'dummy_queue_type', 8888);

insert into queue_event_handler ("id",  common_data, event_type, "name",  queue_type_id)
values ( 77771, '{"pokemon": "polbasor"}', 'ENQUEUE_ACTION', 'CATCH_POKEMON', 88888);

insert into queue_event_handler ("id",  common_data, event_type, "name",  queue_type_id)
values ( 77772, '{"pokemon": "sharizard"}', 'ENQUEUE_ACTION', 'RELEASE_POKEMON', 88888);



insert into queue ( "id", auto_accept_enabled, end_time, hold_enabled, max_size,"name", queue_type_id, start_time)
values ( 99933, false, '2020-12-25 13:04:05.123', true, 10, 'Empty queue', 88888,'2020-12-24 13:04:05.123');

insert into queue ( "id", auto_accept_enabled, end_time, hold_enabled, max_size,"name", queue_type_id, start_time)
values ( 99934, false, '2020-12-26 13:04:05.123', true, 10, 'Handled queue', 88888,'2020-12-25 13:04:05.123');

insert into queue ( "id", auto_accept_enabled, end_time, hold_enabled, max_size,"name", queue_type_id, start_time)
values ( 99935, false, '2020-12-25 13:04:05.123', true, 10, 'Stopped queue', 88888,'2020-12-24 13:04:05.123');


insert into queue_actions ("id",action_time, action_type, queue_id)values ( 888777, now(), 'START', 99934);

insert into queue_actions ("id",action_time, action_type, queue_id)values ( 888778, now(), 'START', 99933);

insert into queue_actions ("id",action_time, action_type, queue_id)values ( 888779, now(), 'START', 99935);
insert into queue_actions ("id",action_time, action_type, queue_id)values ( 888780, now(), 'SUSPEND', 99935);

insert into queue_request ("id", acceptor_id, client_details, client_id, queue_id, refused, request_time,  response_time)
values (98001, 'antekhios', '{"email": "user@fake.com"}', 'user@fake.com', 99934, false, now(), now());

insert into queue_request ("id", acceptor_id, client_details, client_id, queue_id, refused, request_time,  response_time)
values (98002, 'antekhios', '{"email": "faker@fake.com"}', 'fake@fake.com', 99934, false, now(), now());

insert into queue_request ("id", acceptor_id, client_details, client_id, queue_id, refused, request_time,  response_time)
values (98003, 'antekhios', '{"email": "wasta@fake.com"}', 'wasta@fake.com', 99934, false, now(), now());

insert into queue_request ("id", acceptor_id, client_details, client_id, queue_id, refused, request_time,  response_time)
values (98004, 'antekhios', '{"email": "wasta@fake.com"}', 'wasta@fake.com', 99935, false, now(), now());

insert into queue_turn ("id", enqueue_time, queue_number, request_id)
values (97001, now(), 'ABC123', 98001);

insert into queue_turn ("id", enqueue_time, queue_number, request_id)
values (97002, now(), 'ABC124', 98002);

insert into queue_turn ("id", enqueue_time, queue_number, request_id)
values (97003, now(), 'ABC125', 98003);

insert into queue_turn ("id", enqueue_time, queue_number, request_id)
values (97004, now(), 'ABC222', 98004);

insert into queue_turn_insertion ("id", insert_before_turn, inserted_by, inserted_turn)
values ( 96001, 97002, 'antekhios', 97003);

insert into queue_turn_pick ( "id", pick_time, queue_turn_id,  server_details, server_id, skip_reason, skip_time)
values (95001, now(), 97001, '{
  "iss": "DinoChiesa.github.io",
  "sub": "ming",
  "aud": "anna",
  "iat": 1612110856,
  "exp": 2612211456,
  "alpha": [
    "6kpheyn74hvc8swiyndcs",
    false
  ],
  "name": "fakeserver",
  "email": "server@fake.com",
  "groups": [
    "ROLE_QUEUE_MANAGER"
  ],
  "organizationId": 8888
}'
    , 'ming', null, null);

insert into queue_turn_pick ( "id", pick_time, queue_turn_id,  server_details, server_id, skip_reason, skip_time)
values (95002, now(), 97002, '{
  "iss": "DinoChiesa.github.io",
  "sub": "ming",
  "aud": "anna",
  "iat": 1612110856,
  "exp": 2612211456,
  "alpha": [
    "6kpheyn74hvc8swiyndcs",
    false
  ],
  "name": "fakeserver",
  "email": "server@fake.com",
  "groups": [
    "ROLE_QUEUE_MANAGER"
  ],
  "organizationId": 8888
}'
    , 'ming', null, null);

insert into queue_turn_pick ( "id", pick_time, queue_turn_id,  server_details, server_id, skip_reason, skip_time)
values (95003, now(), 97003, '{
  "iss": "DinoChiesa.github.io",
  "sub": "ming",
  "aud": "anna",
  "iat": 1612110856,
  "exp": 2612211456,
  "alpha": [
    "6kpheyn74hvc8swiyndcs",
    false
  ],
  "name": "fakeserver",
  "email": "server@fake.com",
  "groups": [
    "ROLE_QUEUE_MANAGER"
  ],
  "organizationId": 8888
}'
    , 'ming', null, null);








