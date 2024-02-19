-- When a user measurement is deleted, we do not actually want to (hard) delete it.
-- Instead, we want to mark the value for deletion such that it does not show up again for the client
-- but is still kept for archival purposes.
ALTER TABLE user_measurements ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE;