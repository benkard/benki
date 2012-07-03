--*- mode: sql; coding: utf-8 -*--

BEGIN TRANSACTION;

CREATE TABLE users(
  id           SERIAL    NOT NULL,
  first_name   VARCHAR,
  middle_names VARCHAR,
  last_name    VARCHAR,
  email        VARCHAR,
  website      VARCHAR,
  status       VARCHAR,
  PRIMARY KEY(id),
  CHECK (status IN ('admin', 'approved', 'visitor', 'disabled'))
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
  visibility  VARCHAR NOT NULL,
  format      VARCHAR NOT NULL,
  PRIMARY KEY(id),
  FOREIGN KEY(author) REFERENCES users,
  CHECK (format     IN ('markdown')),
  CHECK (visibility IN ('personal', 'protected', 'public'))
);

CREATE TABLE lazychat_targets(
  message INTEGER NOT NULL,
  target  INTEGER NOT NULL,
  PRIMARY KEY(message, target),
  FOREIGN KEY(message) REFERENCES lazychat_messages,
  FOREIGN KEY(target)  REFERENCES users
);

CREATE TABLE lazychat_references(
  referrer INTEGER NOT NULL,
  referee  INTEGER NOT NULL,
  PRIMARY KEY(referrer, referee),
  FOREIGN KEY(referrer) REFERENCES lazychat_messages,
  FOREIGN KEY(referee)  REFERENCES lazychat_messages
);

COMMIT;

