-- Seed groups and group_users tables

INSERT INTO public.groups (name, tenant_id) VALUES ('test', 1);
INSERT INTO public.groups (name, tenant_id) VALUES ('demo', 1);

INSERT INTO public.group_users (group_id, user_id) VALUES (1, 1);
INSERT INTO public.group_users (group_id, user_id) VALUES (1, 2);
INSERT INTO public.group_users (group_id, user_id) VALUES (2, 3);
