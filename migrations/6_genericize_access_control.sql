BEGIN;

ALTER TABLE lazychat_targets RENAME TO post_targets;

CREATE VIEW user_visible_posts AS
   SELECT eur.user, t.message
     FROM effective_user_roles eur, post_targets t
    WHERE t.target = eur.role
  UNION
    SELECT m.owner, m.id
    FROM posts m;

CREATE OR REPLACE VIEW user_visible_lazychat_messages AS
  SELECT uvp.user, uvp.message
    FROM user_visible_posts uvp
    INNER JOIN lazychat_messages lm ON lm.id = uvp.message;

COMMIT;

