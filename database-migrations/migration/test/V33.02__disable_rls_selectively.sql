-- We disable RLS for the footsteps tables during tests as normally this application will not create any footsteps
-- except during integration testing. This will hence allow us to create footsteps within test cases.
ALTER TABLE footsteps DISABLE ROW LEVEL SECURITY;
ALTER TABLE footstep_positions DISABLE ROW LEVEL SECURITY;

-- We disable RLS for the fall risk profiles table during tests as normally this application will not create any
-- fall risk profiles except during integration testing. This will hence allow us to create fall risk profiles within
-- test cases.
ALTER TABLE fall_risk_profiles DISABLE ROW LEVEL SECURITY;
