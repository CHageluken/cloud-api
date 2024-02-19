-- Table 'avatars' will define all avatars available to be used by users.
-- The field "name" contains the name of the avatar (i.e. describing what the avatar contains, like a dog) in English.
CREATE TABLE avatars
(
    id   bigserial,
    name character varying(100) NOT NULL,
    PRIMARY KEY (id)
);

-- Adds a column 'avatar_id' that is used to in the foreign key relationship with the Avatar entity.
ALTER TABLE users
    ADD COLUMN avatar_id bigint,
    ADD CONSTRAINT avatarfk FOREIGN KEY (avatar_id) REFERENCES avatars (id) ON UPDATE CASCADE ON DELETE RESTRICT;

-- Seed table 'avatars' with initial set of avatars available at the time of creating this migration.
INSERT INTO avatars (id, name)
VALUES (1, 'scorpion'),
       (2, 'rubber duck'),
       (3, 'kangaroo'),
       (4, 'rat'),
       (5, 'skunk'),
       (6, 'shell'),
       (7, 'playing dog'),
       (8, 'koala'),
       (9, 'camel'),
       (10, 'bats'),
       (11, 'kitten'),
       (12, 'rhino'),
       (13, 'seal'),
       (14, 'dolphin'),
       (15, 'bear'),
       (16, 'duck'),
       (17, 'chameleon'),
       (18, 'bull'),
       (19, 'eagle'),
       (20, 'cat'),
       (21, 'elephant'),
       (22, 'penguin'),
       (23, 'gorilla'),
       (24, 'snail'),
       (25, 'rabbit'),
       (26, 'shark'),
       (27, 'bug'),
       (28, 'crab'),
       (29, 'chicken'),
       (30, 'fox'),
       (31, 'clownfish'),
       (32, 'frog'),
       (33, 'giraffe'),
       (34, 'monkey'),
       (35, 'cow'),
       (36, 'chick'),
       (37, 'crocodile'),
       (38, 'flamingo'),
       (39, 'hummingbird'),
       (40, 'koi'),
       (41, 'goat'),
       (42, 'pig'),
       (43, 'puppy'),
       (44, 'tyrannosaurus'),
       (45, 'rooster'),
       (46, 'dove'),
       (47, 'orca'),
       (48, 'dog'),
       (49, 'deer'),
       (50, 'fish'),
       (51, 'gecko'),
       (52, 'peacock'),
       (53, 'lion'),
       (54, 'horse'),
       (55, 'swan'),
       (56, 'sloth'),
       (57, 'squirrel'),
       (58, 'sheep'),
       (59, 'sparrow'),
       (60, 'vulture'),
       (61, 'snake'),
       (62, 'turtle'),
       (63, 'narwhale'),
       (64, 'hippo'),
       (65, 'whale'),
       (66, 'wolf'),
       (67, 'tiger'),
       (68, 'unicorn'),
       (69, 'octopus');