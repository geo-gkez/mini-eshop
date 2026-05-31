-- Demo user seed script
-- Run once on the VM after docker compose up:
--   docker compose exec -T postgres psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" < scripts/seed-users.sql
--
-- Hashes: Argon2id via DelegatingPasswordEncoder (prefix: argon2@SpringSecurity_v5_8)
-- alice / alicepass
-- bob   / bobpass

INSERT INTO users (username, password_hash, first_name, last_name, email)
VALUES
    ('alice',
     '{argon2@SpringSecurity_v5_8}$argon2id$v=19$m=16384,t=2,p=1$hp9jQvZs8lvWNzB/+zrdnQ$rW8hKbhfb3Ll1UgpvZqcDkb2fAUPWuvZkDo4F+XvNBU',
     'Alice', 'Smith', 'alice@example.com'),
    ('bob',
     '{argon2@SpringSecurity_v5_8}$argon2id$v=19$m=16384,t=2,p=1$nZaxx6E7lJhJgwy6mdOlXw$T1nC3DmqNBZuoq4GJD3+Ir+5e5FsFatnmBULlV+soPA',
     'Bob',   'Jones', 'bob@example.com')
ON CONFLICT (username) DO NOTHING;
