./build_test_db_img.sh
sudo podman run --env POSTGRES_USER=postgres --env POSTGRES_PASSWORD=postgres -p 5432:5432 qu/db