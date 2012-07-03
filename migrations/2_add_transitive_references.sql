BEGIN TRANSACTION;

CREATE FUNCTION compute_lazychat_transitive_references(message INTEGER)
RETURNS TABLE (referrer INTEGER, referee INTEGER) AS $$
  WITH RECURSIVE t(referrer, referee) AS (
      SELECT * FROM lazychat_references WHERE referee = $1
    UNION
      SELECT lr.*
        FROM lazychat_references lr
       INNER JOIN t ON (t.referee = lr.referrer)
  )
  SELECT * FROM t
$$ LANGUAGE SQL;

CREATE FUNCTION compute_lazychat_transitive_referrals(message INTEGER)
RETURNS TABLE (referrer INTEGER, referee INTEGER) AS $$
  WITH RECURSIVE t(referrer, referee) AS (
      SELECT * FROM lazychat_references WHERE referee = $1
    UNION
      SELECT lr.*
        FROM lazychat_references lr
       INNER JOIN t ON (lr.referee = t.referrer)
  )
  SELECT * FROM t
$$ LANGUAGE SQL;

COMMIT;
