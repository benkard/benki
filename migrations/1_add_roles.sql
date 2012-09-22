BEGIN TRANSACTION;

CREATE TABLE roles(
  id SERIAL NOT NULL,
  name VARCHAR,
  PRIMARY KEY(id)
);

ALTER TABLE lazychat_targets DROP CONSTRAINT lazychat_targets_target_fkey;
ALTER TABLE lazychat_targets ADD CONSTRAINT lazychat_targets_target_fkey FOREIGN KEY(target) REFERENCES roles;

CREATE TABLE role_tags(
  "role" INTEGER NOT NULL,
  tag    VARCHAR NOT NULL,
  PRIMARY KEY("role", tag),
  CHECK (tag IN ('admin', 'everyone', 'world'))
);

ALTER TABLE users ADD COLUMN "role" INTEGER REFERENCES roles;

CREATE TABLE user_roles(
  "user" INTEGER NOT NULL,
  "role" INTEGER NOT NULL,
  PRIMARY KEY("user", "role"),
  FOREIGN KEY("user") REFERENCES users,
  FOREIGN KEY("role") REFERENCES roles
);

-- Create singleton roles for existing users.
CREATE FUNCTION create_singleton_roles() RETURNS VOID AS $$
DECLARE
  "user"    INTEGER;
  user_name VARCHAR;
BEGIN
  FOR "user", user_name IN SELECT id, concat(first_name, ' ', last_name) FROM users LOOP
    DECLARE
      new_role INTEGER;
    BEGIN
      INSERT INTO roles("name") VALUES (user_name)
        RETURNING id INTO STRICT new_role;
      UPDATE users SET "role" = new_role WHERE id = "user";
      INSERT INTO user_roles VALUES ("user", new_role);
    END;
  END LOOP;
END;
$$ LANGUAGE plpgsql;
SELECT create_singleton_roles();
DROP FUNCTION create_singleton_roles();

ALTER TABLE users ALTER COLUMN "role" SET NOT NULL;

CREATE TABLE role_subroles(
  "superrole" INTEGER NOT NULL,
  "subrole"   INTEGER NOT NULL,
  PRIMARY KEY("superrole", "subrole"),
  FOREIGN KEY("superrole") REFERENCES roles,
  FOREIGN KEY("subrole") REFERENCES roles
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

WITH r(id) AS (
  INSERT INTO roles(name) VALUES ('Admininistrators') RETURNING id
)
INSERT INTO role_tags SELECT r.id, 'admin' FROM r;

WITH world(id) AS (
  INSERT INTO roles(name) VALUES ('World') RETURNING id
),   t AS (
  INSERT INTO lazychat_targets
       SELECT m.id, world.id
         FROM lazychat_messages m, world
        WHERE m.visibility = 'public'
)
INSERT INTO role_tags SELECT id, 'world' FROM world;

WITH r(id) AS (
  INSERT INTO roles(name) VALUES ('Logged-In Users') RETURNING id
),   t AS (
  INSERT INTO user_roles SELECT users.id, r.id FROM users, r
)
INSERT INTO role_tags SELECT r.id, 'everyone' FROM r;

WITH inner_circle(id) AS (
  INSERT INTO roles(name) VALUES ('Inner Circle') RETURNING id
),   t AS (
  INSERT INTO lazychat_targets
       SELECT m.id, inner_circle.id
         FROM lazychat_messages m, inner_circle
        WHERE m.visibility = 'protected'
)
INSERT INTO user_roles SELECT users.id, inner_circle.id FROM users, inner_circle;

ALTER TABLE lazychat_messages DROP COLUMN visibility;

CREATE VIEW user_visible_lazychat_messages AS
  SELECT eur.user, t.message
    FROM effective_user_roles eur, lazychat_targets t
   WHERE t.target = eur.role;

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
  EXECUTE PROCEDURE new_user_put_in_universal_role();

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

COMMIT;
