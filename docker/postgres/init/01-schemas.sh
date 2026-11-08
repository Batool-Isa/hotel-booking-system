#!/bin/bash
# Runs once, the first time the Postgres volume is created.
# One database (vibestay), one schema and one user per service.
# Each user owns only its own schema, so a service cannot read another service's tables.
set -euo pipefail

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE USER user_svc    WITH PASSWORD '${USER_SVC_DB_PASSWORD}';
    CREATE USER hotel_svc   WITH PASSWORD '${HOTEL_SVC_DB_PASSWORD}';
    CREATE USER booking_svc WITH PASSWORD '${BOOKING_SVC_DB_PASSWORD}';
    CREATE USER billing_svc WITH PASSWORD '${BILLING_SVC_DB_PASSWORD}';

    CREATE SCHEMA users    AUTHORIZATION user_svc;
    CREATE SCHEMA hotels   AUTHORIZATION hotel_svc;
    CREATE SCHEMA bookings AUTHORIZATION booking_svc;
    CREATE SCHEMA billing  AUTHORIZATION billing_svc;

    ALTER ROLE user_svc    SET search_path = users;
    ALTER ROLE hotel_svc   SET search_path = hotels;
    ALTER ROLE booking_svc SET search_path = bookings;
    ALTER ROLE billing_svc SET search_path = billing;

    REVOKE ALL ON SCHEMA public FROM PUBLIC;
    REVOKE ALL ON DATABASE ${POSTGRES_DB} FROM PUBLIC;
    GRANT CONNECT ON DATABASE ${POSTGRES_DB} TO user_svc, hotel_svc, booking_svc, billing_svc;
EOSQL
