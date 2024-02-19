ALTER TABLE users
DROP CONSTRAINT avatarfk,
DROP COLUMN avatar_id;

DROP TABLE avatars;