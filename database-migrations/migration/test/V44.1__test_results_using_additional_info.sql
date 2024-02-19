-- This migration is added to test if the prod migration V45 successfully extracts trials, surveys and wearables
-- from existing test results.

-- Old timestamp fields: startTime, manualStartTime/manualEndTime, automaticStartTime/automaticEndTime
-- Old wearable fields: defaultDevice, optionalDevice, leftDevice, rightDevice,
-- sitStandWearableId (this one will not be stored in the new field)

INSERT INTO wearables (id) VALUES ('EFS33rdFF20');
INSERT INTO wearables (id) VALUES ('heelable_37');
INSERT INTO wearables (id) VALUES ('heelable_48');
INSERT INTO wearables (id) VALUES ('heelable_50');

--- WALK ---
-- It takes no surveys, has no trials. V45 should create a single trial out of the TR timestamps.
-- defaultDevice and optionalDevice
INSERT INTO test_results (user_id, type, begin_time, end_time, additional_info, deleted, remarks)
    VALUES (1, 6, '2022-08-31 13:50:00.299000', '2022-08-31 13:50:55.299000',
    '{"defaultDevice": {"id": "heelable_37", "side": "LEFT"}, "optionalDevice": {"id": "heelable_50", "side": "RIGHT"}}',
    false, '');

--- 6MWT ---
-- Trials using the 'startTime' and 'endTime'.
-- Surveys are filled.
-- rightDevice
INSERT INTO test_results (user_id, type, begin_time, end_time, additional_info, deleted, remarks)
    VALUES (1, 2, '2023-01-11 22:10:07.299000', '2023-01-11 22:12:07.299000',
    '{"rightDevice": "heelable_48", "trials": [{"endTime": 1673475067299, "startTime": 1673475007299}, {"endTime": 1673475127299, "startTime": 1673475067299}], "surveys": [{"type":"FES_I","content":{"totalScore":0,"questionResults":[{"question":"Cleaning in the house (e.g. sweeping, vacuuming or dusting)"},{"question":"Dressing or undressing"},{"question":"Preparing simple meals"},{"question":"Taking a bath or shower"},{"question":"Shopping"},{"question":"Getting in or out of a chair"},{"question":"Going up or down stairs"},{"question":"Taking a walk in the neighborhood"},{"question":"Reaching for something above your head or for something on the ground"},{"question":"Answering the phone before it stops ringing"},{"question":"Walking on a slippery surface (e.g. wet or frozen)"},{"question":"Visiting a friend, acquaintance or family member"},{"question":"Walking in a place where there are many people"},{"question":"Walking on uneven surfaces (e.g. cobblestones or poorly maintained sidewalks)"},{"question":"Going up or down a slope"},{"question":"Visiting a social occasion (e.g. a visit to a church, family or association)"}]}},{"type":"NPRS","content":{"score":-1}},{"type":"FAC","content":{"score":4}}]}',
    false, '');

--- TUG ---
-- Trials using the 'manual' and 'automatic' timestamps.
-- No wearable
-- Typically has surveys
INSERT INTO test_results (user_id, type, begin_time, end_time, additional_info, deleted, remarks)
    VALUES (1, 4, '2023-01-11 22:10:07.299000', '2023-01-11 22:12:07.299000',
    '{"trials": [{"manualEndTime": 1673475067299, "manualStartTime": 1673475007299}, {"automaticEndTime": 1673475127299, "automaticStartTime": 1673475067299}]}',
    false, '');

--- 10MWT ---
-- The only test type utilizing all surveys.
-- leftDevice
INSERT INTO test_results (user_id, type, begin_time, end_time, additional_info, deleted, remarks)
    VALUES (1, 3, '2023-01-11 22:10:07.299000', '2023-01-11 22:12:07.299000',
    '{"leftDevice": "EFS33rdFF20", "trials": [{"endTime": 1673475067299, "startTime": 1673475007299}, {"endTime": 1673475127299, "startTime": 1673475067299}], "surveys": [{"type":"FES_I","content":{"questionResults":[{"question":"Cleaning in the house (e.g. sweeping, vacuuming or dusting)","result":1},{"question":"Dressing or undressing","result":1},{"question":"Preparing simple meals","result":1},{"question":"Taking a bath or shower","result":1},{"question":"Shopping","result":1},{"question":"Getting in or out of a chair","result":1},{"question":"Going up or down stairs","result":2},{"question":"Taking a walk in the neighborhood","result":2},{"question":"Reaching for something above your head or for something on the ground","result":2},{"question":"Answering the phone before it stops ringing","result":2},{"question":"Walking on a slippery surface (e.g. wet or frozen)","result":2},{"question":"Visiting a friend, acquaintance or family member","result":2},{"question":"Walking in a place where there are many people","result":3},{"question":"Walking on uneven surfaces (e.g. cobblestones or poorly maintained sidewalks)","result":3},{"question":"Going up or down a slope","result":3},{"question":"Visiting a social occasion (e.g. a visit to a church, family or association)","result":3}],"totalScore":30}},{"type":"NPRS","content":{"score":10}},{"type":"NRS","content":{"score":7}},{"type":"BORG_RPE","content":{"score":6}},{"type":"FAC","content":{"score":5}}]}',
    false, '');
