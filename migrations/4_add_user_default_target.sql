BEGIN TRANSACTION;

CREATE TABLE user_default_target(
  "user" INTEGER NOT NULL,
  target INTEGER NOT NULL,
  PRIMARY KEY("user", target),
  FOREIGN KEY("user") REFERENCES users,
  FOREIGN KEY(target) REFERENCES roles
);

INSERT INTO user_default_target
     SELECT users.id, roles.id
       FROM users
      INNER JOIN roles ON (roles."name" = 'Inner Circle');

COMMIT;

