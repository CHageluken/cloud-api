-- The most common data access path on user-wearable links is a time range query for a given user's user wearable links.
-- The range is a time range that defines when the user was wearing the wearable (or linked to it).
-- Therefore, we create a concatenated index on user_id, begin_time, and end_time.
-- This will allow us to efficiently query for a range of user wearable links for a given user.
-- In addition, this should also prevent duplicate (identical) user wearable links from occurring for a given user.
CREATE UNIQUE INDEX uwl_range_idx ON user_wearable_links (user_id, begin_time, end_time);

-- The most common data access path on fall risk profiles is a time range query for a given wearable's fall risk profiles.
-- The range is a time range that is usually based on the time range of a user wearable link that spans zero or more FRPs.
-- Therefore, we create a concatenated index on wearable_id, begin_time, and end_time.
-- This will allow us to efficiently query for a range of fall risk profiles for a given wearable.
-- In addition, this should also prevent duplicate (identical) fall risk profiles from occurring for a given wearable.
CREATE UNIQUE INDEX frp_range_idx ON fall_risk_profiles (wearable_id, begin_time, end_time);