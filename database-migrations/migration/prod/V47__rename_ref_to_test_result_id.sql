ALTER TABLE test_trials DROP CONSTRAINT test_trials_pkey;
ALTER TABLE test_trials DROP CONSTRAINT test_trials_test_id_fkey;
ALTER TABLE test_trials RENAME COLUMN test_id TO test_result_id;
ALTER TABLE test_trials ADD PRIMARY KEY (test_result_id, begin_time);
ALTER TABLE test_trials ADD FOREIGN KEY (test_result_id) REFERENCES test_results(id) ON UPDATE CASCADE ON DELETE CASCADE;

ALTER TABLE test_surveys DROP CONSTRAINT test_surveys_pkey;
ALTER TABLE test_surveys DROP CONSTRAINT test_surveys_test_id_fkey;
ALTER TABLE test_surveys RENAME COLUMN test_id TO test_result_id;
ALTER TABLE test_surveys ADD PRIMARY KEY (test_result_id, type);
ALTER TABLE test_surveys ADD FOREIGN KEY (test_result_id) REFERENCES test_results(id) ON UPDATE CASCADE ON DELETE CASCADE;
