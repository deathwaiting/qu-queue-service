curl --insecure -X POST http://localhost:8180/auth/realms/qu/protocol/openid-connect/token \
    --user backend-service:secret \
    -H 'content-type: application/x-www-form-urlencoded' \
    -d 'username=a@fake.com&password=1234&grant_type=password'>> access.token;

curl --insecure -X POST http://localhost:8080/auth/realms/qu/protocol/openid-connect/token \
    --user backend-service:secret \
    -H 'content-type: application/x-www-form-urlencoded' \
    -d 'username=m7r_owner@fake.com&password=1234&grant_type=password' > admin_access.json;