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






