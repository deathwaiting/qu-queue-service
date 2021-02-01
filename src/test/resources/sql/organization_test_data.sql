insert into organization (
    "id",
    "name",
    owner_data,
    owner_id,
    subscription_type)
values (8888,
 'test_org',
  '{"name": "fakeman", "id": "owner@fake.com", "organizationId": 8888, "email": "owner@fake.com"}',
  'owner@fake.com',
  'free')
;

