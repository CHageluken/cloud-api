-- Aims to extract a wearable from the `additional_info` column into a separate column in the `test_results` table
-- and add all trials and surveys to their respective tables.

-- There are a few possible fields we've used for storing a wearable used during a test:
-- rightDevice and leftDevice, which are both a wearable id,
-- defaultDevice and optionalDevice, which are objects with two properties - `id` and `side` (string,
-- corresponding to the Wearable.Side enum).

-- Add the new columns.
ALTER TABLE test_results ADD COLUMN wearable_side integer;
ALTER TABLE test_results ADD COLUMN wearable_id character varying(255),
    ADD CONSTRAINT wearablefk FOREIGN KEY (wearable_id) REFERENCES wearables(id) ON UPDATE CASCADE ON DELETE RESTRICT;

-- Copy wearable to the new columns.
UPDATE test_results
SET wearable_id = CASE
  WHEN additional_info->>'defaultDevice' IS NOT NULL THEN additional_info->'defaultDevice'->>'id'
  WHEN additional_info->>'optionalDevice' IS NOT NULL THEN additional_info->'optionalDevice'->>'id'

  WHEN additional_info->>'rightDevice' IS NOT NULL THEN additional_info->>'rightDevice'
  WHEN additional_info->>'leftDevice' IS NOT NULL THEN additional_info->>'leftDevice'
  ELSE NULL
END;

UPDATE test_results
SET wearable_side = CASE
  WHEN additional_info->>'defaultDevice' IS NOT NULL THEN
    CASE
        WHEN additional_info->'defaultDevice'->>'side' = 'LEFT' THEN 0
        ELSE 1
    END
  WHEN additional_info->>'optionalDevice' IS NOT NULL THEN
    CASE
        WHEN additional_info->'optionalDevice'->>'side' = 'LEFT' THEN 0
        ELSE 1
    END
  WHEN additional_info->>'rightDevice' IS NOT NULL THEN 1
  WHEN additional_info->>'leftDevice' IS NOT NULL THEN 0
  ELSE NULL
END;

-- We change the PK for the `test_trials` table, since we have never stored the `nr` in the `additional_info`.
-- Moreover, the number of the trial isn't currently used for anything.
ALTER TABLE test_trials DROP CONSTRAINT test_trials_pkey;
ALTER TABLE test_trials ADD PRIMARY KEY (test_id, begin_time);
ALTER TABLE test_trials DROP COLUMN trial_nr;

-- Copy trials to the `test_trials` table.
-- For tests without trials (WALK), we create a single trial using the begin and end time of the test result.
-- This makes all test types trial based.
INSERT INTO test_trials (test_id, begin_time, end_time)
SELECT tr.id,
       COALESCE(
           to_timestamp((COALESCE(t->>'startTime', t->>'manualStartTime', t->>'automaticStartTime'))::float/1000) AT TIME ZONE 'UTC',
           tr.begin_time
       ),
       COALESCE(
           to_timestamp((COALESCE(t->>'endTime', t->>'manualEndTime', t->>'automaticEndTime'))::float/1000) AT TIME ZONE 'UTC',
           tr.end_time
       )
FROM test_results tr
LEFT JOIN LATERAL jsonb_array_elements(COALESCE(tr.additional_info->'trials', '[]'::jsonb)) t ON true;

-- Maps the survey types in `additional_info` to their corresponding Id in the `test_survey_types` table.
CREATE FUNCTION get_survey_type_id(type text) returns bigint as
$$
BEGIN
    RETURN (
        SELECT id FROM test_survey_types WHERE name = CASE
            WHEN type = 'BORG_RPE' THEN 'Borg-RPE'
            WHEN type = 'FAC' THEN 'FAC'
            WHEN type = 'FES_I' THEN 'FES-I'
            WHEN type = 'NPRS' THEN 'NPRS'
            WHEN type = 'NRS' THEN 'NRS'
        END
    );
END;
$$ LANGUAGE plpgsql;

-- Copy surveys to the `test_surveys` table.
INSERT INTO test_surveys (test_id, type, content)
SELECT tr.id, get_survey_type_id(s->>'type'), s->'content'
FROM test_results tr
LEFT JOIN LATERAL jsonb_array_elements(tr.additional_info->'surveys') s ON true
WHERE tr.additional_info->'surveys' IS NOT NULL AND jsonb_array_length(tr.additional_info->'surveys') > 0;

-- Drop function
DROP FUNCTION get_survey_type_id(text);