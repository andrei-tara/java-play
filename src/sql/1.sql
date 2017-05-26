-- CREATE TABLE votes ( name CHAR(10), votes INT ); INSERT INTO votes VALUES
-- ('Smith',10), ('Jones',15), ('White',20), ('Black',40), ('Green',50), ('Brown',20);

SELECT @rank := @rank + 1 as rank, name, votes FROM votes v, (SELECT @rank := 0 ) vr order by votes desc;