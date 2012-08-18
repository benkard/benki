--*- mode: sql; coding: utf-8 -*--

BEGIN TRANSACTION;

CREATE TABLE roles(
  id SERIAL NOT NULL,
  PRIMARY KEY(id)
);

CREATE TABLE users(
  id           SERIAL    NOT NULL,
  first_name   VARCHAR,
  middle_names VARCHAR,
  last_name    VARCHAR,
  email        VARCHAR,
  website      VARCHAR,
  status       VARCHAR,
  "role"       INTEGER   NOT NULL,
  PRIMARY KEY(id),
  CHECK (status IN ('admin', 'approved', 'visitor', 'disabled')),
  FOREIGN KEY("role") REFERENCES roles
);

CREATE TABLE openids(
  "user"       INTEGER   NOT NULL,
  openid       VARCHAR   NOT NULL,
  PRIMARY KEY(openid),
  FOREIGN KEY("user") REFERENCES users
);

CREATE TABLE webids(
  "user"       INTEGER   NOT NULL,
  webid        VARCHAR   NOT NULL,
  PRIMARY KEY(webid),
  FOREIGN KEY("user") REFERENCES users
);

CREATE TABLE rsa_keys(
  modulus  NUMERIC   NOT NULL,
  exponent NUMERIC   NOT NULL,
  PRIMARY KEY(modulus, exponent)
);

CREATE TABLE user_rsa_keys(
  "user"   INTEGER   NOT NULL,
  modulus  NUMERIC   NOT NULL,
  exponent NUMERIC   NOT NULL,
  PRIMARY KEY("user", modulus, exponent),
  FOREIGN KEY("user") REFERENCES users,
  FOREIGN KEY(modulus, exponent) REFERENCES rsa_keys
);

CREATE TABLE user_email_addresses(
  "user"       INTEGER   NOT NULL,
  email        VARCHAR   NOT NULL,
  PRIMARY KEY(email),
  FOREIGN KEY("user") REFERENCES users
);

CREATE TABLE user_nicknames(
  "user"   INTEGER NOT NULL,
  nickname VARCHAR NOT NULL,
  PRIMARY KEY(nickname),
  FOREIGN KEY("user") REFERENCES users
);
CREATE INDEX user_nicknames_user ON user_nicknames ("user");

CREATE TABLE user_jids(
  "user" INTEGER NOT NULL,
  jid    VARCHAR NOT NULL,
  PRIMARY KEY("user", jid),
  FOREIGN KEY("user") REFERENCES users
);
CREATE INDEX user_jids_user ON user_jids ("user");

CREATE TABLE role_subroles(
  "superrole" INTEGER NOT NULL,
  "subrole"   INTEGER NOT NULL,
  PRIMARY KEY("superrole", "subrole"),
  FOREIGN KEY("superrole") REFERENCES roles,
  FOREIGN KEY("subrole") REFERENCES roles
);

CREATE TABLE user_roles(
  "user" INTEGER NOT NULL,
  "role" INTEGER NOT NULL,
  PRIMARY KEY("user", "role"),
  FOREIGN KEY("user") REFERENCES users,
  FOREIGN KEY("role") REFERENCES roles
);

CREATE TABLE page_keys(
  "user" INTEGER NOT NULL,
  page   VARCHAR NOT NULL,
  "key"  DECIMAL NOT NULL,   -- (~ NUMERIC DECIMAL)
  PRIMARY KEY(page, "key"),
  FOREIGN KEY("user") REFERENCES users
);

CREATE TABLE wiki_pages(
  id           SERIAL    NOT NULL,
  PRIMARY KEY(id)
);

CREATE TABLE wiki_page_revisions(
  id           SERIAL    NOT NULL,
  page         INTEGER   NOT NULL,
  date         TIMESTAMP WITH TIME ZONE DEFAULT now(),
  title        VARCHAR,
  content      VARCHAR,
  author       INTEGER,
  format       VARCHAR,
  PRIMARY KEY(id),
  FOREIGN KEY(page) REFERENCES wiki_pages,
  FOREIGN KEY(author) REFERENCES users,
  CHECK (format IN ('mulkwiki', 'html5', 'xhtml5', 'markdown', 'textile', 'muse', 'bbcode'))
);


CREATE TABLE bookmarks(
  id          SERIAL    NOT NULL,
  owner       INTEGER,
  date        TIMESTAMP WITH TIME ZONE DEFAULT now(),
  uri         VARCHAR   NOT NULL,
  title       VARCHAR,
  description VARCHAR,
  visibility  VARCHAR,
  PRIMARY KEY(id),
  FOREIGN KEY(owner) REFERENCES users,
  CHECK (visibility IN ('private', 'protected', 'public'))
);

CREATE TABLE bookmark_tags(
  bookmark INTEGER NOT NULL,
  tag      VARCHAR NOT NULL,
  PRIMARY KEY(bookmark, tag),
  FOREIGN KEY(bookmark) REFERENCES bookmarks
);


CREATE TABLE lazychat_messages(
  id          SERIAL    NOT NULL,
  author      INTEGER,
  date        TIMESTAMP WITH TIME ZONE DEFAULT now(),
  content     VARCHAR,
  format      VARCHAR NOT NULL,
  PRIMARY KEY(id),
  FOREIGN KEY(author) REFERENCES users,
  CHECK (format IN ('markdown'))
);

CREATE TABLE lazychat_targets(
  message INTEGER NOT NULL,
  target  INTEGER NOT NULL,
  PRIMARY KEY(message, target),
  FOREIGN KEY(message) REFERENCES lazychat_messages,
  FOREIGN KEY(target)  REFERENCES roles
);

CREATE TABLE lazychat_references(
  referrer INTEGER NOT NULL,
  referee  INTEGER NOT NULL,
  PRIMARY KEY(referrer, referee),
  FOREIGN KEY(referrer) REFERENCES lazychat_messages,
  FOREIGN KEY(referee)  REFERENCES lazychat_messages
);

CREATE TABLE user_default_target(
  "user" INTEGER NOT NULL,
  target INTEGER NOT NULL,
  PRIMARY KEY("user", target),
  FOREIGN KEY("user") REFERENCES users,
  FOREIGN KEY(target) REFERENCES roles
);

CREATE VIEW effective_role_subroles AS
  WITH RECURSIVE t(superrole, subrole) AS (
      SELECT id, id
        FROM roles
    UNION
      SELECT t.superrole, rs.subrole
        FROM t t
       INNER JOIN role_subroles rs
          ON (rs.superrole = t.subrole)
  )
  SELECT * FROM t;

CREATE VIEW effective_user_roles AS
    (SELECT ur."user", er.subrole AS role
       FROM user_roles ur
      INNER JOIN effective_role_subroles er
         ON (er.superrole = ur.role))
  UNION
    (SELECT u.id AS "user", rt.role FROM users u, role_tags rt WHERE rt.tag IN ('everyone', 'world'))
  UNION
    (SELECT NULL, rt.role FROM role_tags rt WHERE rt.tag = 'world');

CREATE FUNCTION new_user_put_in_universal_role() RETURNS TRIGGER AS $$
DECLARE
  universal_role INTEGER;
BEGIN
  SELECT "role" FROM role_tags WHERE tag = 'everyone' INTO universal_role;
  INSERT INTO user_roles VALUES (NEW.id, universal_role);
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER new_user_put_in_universal_role
  AFTER INSERT ON users
  FOR EACH ROW
  EXECUTE PROCEDURE put_new_user_in_universal_role();

CREATE FUNCTION new_user_put_user_in_user_role() RETURNS TRIGGER AS $$
BEGIN
  INSERT INTO user_roles VALUES (NEW."role", user_role);
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER new_user_put_user_in_user_role
  AFTER INSERT ON users
  FOR EACH ROW
  EXECUTE PROCEDURE new_user_put_user_in_user_role();

CREATE FUNCTION new_user_create_user_role() RETURNS TRIGGER AS $$
DECLARE
  new_role INTEGER;
BEGIN
  INSERT INTO roles("name") VALUES (concat(NEW.first_name, ' ', NEW.last_name))
    RETURNING id INTO STRICT new_role;
  UPDATE users SET "role" = new_role WHERE id = NEW.id;
  NEW.role := new_role;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER new_user_create_user_role
  BEFORE INSERT ON users
  FOR EACH ROW
  EXECUTE PROCEDURE new_user_create_user_role();

CREATE VIEW user_visible_lazychat_messages AS
   SELECT eur.user, t.message
     FROM effective_user_roles eur, lazychat_targets t
    WHERE t.target = eur.role
  UNION
    SELECT m.author, m.message
    FROM lazychat_messages m;

ROLLBACK;
--COMMIT;
