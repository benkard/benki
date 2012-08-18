-- Add table.
CREATE TABLE posts(
  id          SERIAL    NOT NULL,
  owner       INTEGER,
  date        TIMESTAMP WITH TIME ZONE DEFAULT now(),
  PRIMARY KEY(id),
  FOREIGN KEY(owner) REFERENCES users
);

-- Uniquify bookmarks.id wrt. lazychat_messages.id.
ALTER TABLE bookmark_tags
  DROP CONSTRAINT bookmark_tags_bookmark_fkey;
ALTER TABLE bookmark_tags
  ADD CONSTRAINT bookmark_tags_bookmark_fkey
    FOREIGN KEY (bookmark) REFERENCES bookmarks(id) ON UPDATE CASCADE;

CREATE or replace FUNCTION tmp_update_bookmarx_ids() RETURNS VOID AS $$
DECLARE
  offst INTEGER;
  bookmark RECORD;
BEGIN
  SELECT MAX(id)+1 FROM lazychat_messages INTO offst;
  UPDATE bookmarks SET id = id + offst cascade;
END;
$$ LANGUAGE plpgsql;
SELECT tmp_update_bookmarx_ids();
DROP FUNCTION tmp_update_bookmarx_ids();

-- Enable inheritance.
ALTER TABLE bookmarks INHERIT posts;

ALTER TABLE lazychat_messages RENAME COLUMN author TO owner;
ALTER TABLE lazychat_messages INHERIT posts;

-- Merge id seqs.
ALTER TABLE bookmarks ALTER COLUMN id SET DEFAULT nextval('posts_id_seq'::regclass);
ALTER TABLE lazychat_messages ALTER COLUMN id SET DEFAULT nextval('posts_id_seq'::regclass);
DROP SEQUENCE bookmarks_id_seq;
DROP SEQUENCE lazychat_messages_id_seq;

SELECT setval('posts_id_seq', MAX(id)+1) FROM posts;

-- FIXME: We ought to add some kind of constraint on bookmarks.id and
-- lazychat_messages.id such that uniqueness across both tables is
-- guaranteed.
