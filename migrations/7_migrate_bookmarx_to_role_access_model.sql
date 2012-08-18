BEGIN TRANSACTION;

ALTER TABLE post_targets DROP CONSTRAINT lazychat_targets_message_fkey;
ALTER TABLE lazychat_references DROP CONSTRAINT lazychat_references_referee_fkey; 

INSERT INTO post_targets
     SELECT bm.id, rt.role
       FROM bookmarks bm
      INNER JOIN role_tags rt ON tag = 'world'
      WHERE visibility = 'public';
INSERT INTO post_targets
     SELECT bm.id, udt.target
       FROM bookmarks bm
      INNER JOIN user_default_target udt ON bm.owner = udt."user"
      WHERE visibility = 'protected';

ALTER TABLE bookmarks DROP COLUMN visibility;

CREATE VIEW user_visible_bookmarks AS
  SELECT uvp.user, uvp.message
    FROM user_visible_posts uvp
    INNER JOIN bookmarks bm ON bm.id = uvp.message;

COMMIT;
