-- From the issue description of #310 (Gitlab):
-- We can have users that do not authenticate against our system/have accounts (with the purpose of obtaining analysis data from the API).
-- These users are just wearing 1-2 wearable devices while not actually authenticating against the API (instead, other users are viewing the data of these users).
-- The schema should support this and hence we should make auth_id in the users table a nullable field.
ALTER TABLE users
    ALTER COLUMN auth_id DROP NOT NULL;