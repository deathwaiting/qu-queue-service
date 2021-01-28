cp ../../../main/resources/db/sql_change_logs/00000_initial.sql  00000_initial.sql
sudo podman build --tag=qu/db --file=Dockerfile.postgres.test