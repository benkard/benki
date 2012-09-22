CREATE OR REPLACE VIEW user_visible_lazychat_messages AS
   SELECT eur.user, t.message
     FROM effective_user_roles eur, lazychat_targets t
    WHERE t.target = eur.role
  UNION
    SELECT m.author, m.id
    FROM lazychat_messages m;
