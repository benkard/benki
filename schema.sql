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

CREATE TABLE wiki_pages(
  id           SERIAL    NOT NULL,
  PRIMARY KEY(id)
);

CREATE TABLE wiki_page_revisions(
  id           SERIAL    NOT NULL,
  page         INTEGER   NOT NULL,
  date         TIMESTAMP DEFAULT now(),
  title        VARCHAR,
  content      VARCHAR,
  author       INTEGER,
  format       VARCHAR,
  PRIMARY KEY(id),
  FOREIGN KEY(page) REFERENCES wiki_pages,
  FOREIGN KEY(author) REFERENCES users,
  CHECK (format IN ('mulkwiki', 'html5', 'xhtml5', 'markdown', 'textile', 'muse', 'bbcode'))
);

ROLLBACK;
--COMMIT;
