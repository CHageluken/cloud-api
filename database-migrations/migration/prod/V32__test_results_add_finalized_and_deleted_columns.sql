-- After a test result is finalized, we do not want to be able to hard delete it anymore.
-- Instead, should the test result be deleted by the user, we mark the value for deletion such that it does
-- not show up again for the client but is still kept for archival purposes.
ALTER TABLE test_results ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE;