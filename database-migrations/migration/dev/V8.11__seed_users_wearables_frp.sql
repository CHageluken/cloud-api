-- Seed users with wearables, user-wearable links, group memberships and FRPs

INSERT INTO public.wearables (id, tenant_id) VALUES ('heelable_37', 1);
INSERT INTO public.wearables (id, tenant_id) VALUES ('heelable_48', 1);
INSERT INTO public.wearables (id, tenant_id) VALUES ('heelable_50', 1);

INSERT INTO public.users (auth_id, tenant_id) VALUES ('heelable_37', 1);
INSERT INTO public.users (auth_id, tenant_id) VALUES ('heelable_50', 1);

INSERT INTO public.user_wearable_links (user_id, wearable_id, begin_time, end_time, side) VALUES (5, 'heelable_37', '2020-07-10 20:19:48.000000', '2020-07-10 20:24:48.000000', 1);
INSERT INTO public.user_wearable_links (user_id, wearable_id, begin_time, end_time, side) VALUES (6, 'heelable_48', '2020-07-12 19:19:46.000000', '2020-07-12 19:29:46.000000', 0);
INSERT INTO public.user_wearable_links (user_id, wearable_id, begin_time, end_time, side) VALUES (6, 'heelable_48', '2020-07-10 20:19:46.000000', '2020-07-10 20:19:46.000000', 1);
INSERT INTO public.user_wearable_links (user_id, wearable_id, begin_time, end_time, side) VALUES (6, 'heelable_50', '2021-01-10 20:19:46.000000', '2022-01-10 20:19:46.000000', 1);

INSERT INTO public.group_users (group_id, user_id) VALUES (1, 5);
INSERT INTO public.group_users (group_id, user_id) VALUES (1, 6);

INSERT INTO public.fall_risk_profiles (wearable_id, floor_id, created_at, begin_time, end_time, walking_speed, step_length, step_frequency, rms_vertical_accel, hidden) VALUES ('heelable_37', 3, '2020-07-10 20:19:53.000000', '2020-07-10 20:19:48.000000', '2020-07-10 20:24:42.000000', 1.04, 0.7, 1.5, 10.22, false);
INSERT INTO public.fall_risk_profiles (wearable_id, floor_id, created_at, begin_time, end_time, walking_speed, step_length, step_frequency, rms_vertical_accel, hidden) VALUES ('heelable_48', 3, '2020-07-12 19:27:55.000000', '2020-07-12 19:19:46.000000', '2020-07-12 19:27:42.000000', 1.55, 0.9, 1.7, 11, false);
INSERT INTO public.fall_risk_profiles (wearable_id, floor_id, created_at, begin_time, end_time, walking_speed, step_length, step_frequency, rms_vertical_accel, hidden) VALUES ('heelable_48', 3, '2020-07-10 20:27:55.000000', '2020-07-10 20:19:46.000000', '2020-07-10 20:22:42.000000', 3, 1, 2.5, 9.32, false);
INSERT INTO public.fall_risk_profiles (wearable_id, floor_id, created_at, begin_time, end_time, walking_speed, step_length, step_frequency, rms_vertical_accel, hidden) VALUES ('heelable_50', 3, '2021-01-10 20:27:55.000000', '2021-01-10 20:19:46.000000', '2021-01-10 20:22:42.000000', 3, 1, 2.5, 9.32, false);
