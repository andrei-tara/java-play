-- Capitalises the first letter of every word.
DROP FUNCTION IF EXISTS CAPITALIZE;

DELIMITER $$

CREATE FUNCTION CAPITALIZE(input VARCHAR(250))
  RETURNS VARCHAR(250) deterministic

BEGIN
	DECLARE len INT;
	DECLARE i INT;

	SET len   = CHAR_LENGTH(input);
	SET input = LOWER(input);
	SET i = 0;
	WHILE (i < len) DO
		IF ((MID(input,i,1) = ' ' OR i = 0) AND (i < len)) THEN			
			SET input = CONCAT(LEFT(input,i),UPPER(MID(input,i + 1,1)), RIGHT(input,len - i - 1));			
		END IF;
		SET i = i + 1;
	END WHILE;
	RETURN input;
END;
$$
DELIMITER ;

SELECT CAPITALIZE('UNITED states Of AmERIca')