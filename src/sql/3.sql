-- CREATE TABLE sometbl ( ID INT, NAME VARCHAR(50) );
-- INSERT INTO sometbl VALUES (1, 'Smith'), (2, 'Julio|Jones|Falcons'), (3, 'White|Snow'), (4, 'Paint|It|Red'), (5, 'Green|Lantern'), (6, 'Brown|bag');

DROP PROCEDURE IF EXISTS SPLITTED_TABLE;
DELIMITER $$
CREATE PROCEDURE SPLITTED_TABLE(delimeter VARCHAR(255))

BEGIN

	DECLARE _id INT DEFAULT 0;
    DECLARE _name VARCHAR(250);
	DECLARE i INT DEFAULT 0;
    DECLARE splitted VARCHAR(50);
    DECLARE done INT DEFAULT 0;
	DECLARE source CURSOR FOR SELECT sometbl.id, sometbl.name FROM sometbl WHERE sometbl.name != '';
	DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;
	
    -- Table that will handle the records for the splitted columns.
    DROP TABLE IF EXISTS temp_table;
    CREATE TABLE temp_table(id INT, name VARCHAR(250));

    OPEN source;      
      read_loop: LOOP FETCH source INTO _id, _name;        
        IF done THEN 
        	LEAVE read_loop; 
        END IF;
		
        IF LOCATE(delimeter, _name) <= 0 THEN
	        -- The name has no piple so add it directly to the new table.
        	INSERT INTO temp_table VALUES (_id, _name);
        	ITERATE read_loop;
		END IF;
        
        -- If the name contains pipe '|' then split and add as a new record.
        SET i = 1;        
		WHILE i <= (SELECT LENGTH(_name) - LENGTH(REPLACE(_name, delimeter, '')) + 1 ) DO				
			SET splitted = (SELECT REPLACE(SUBSTRING(SUBSTRING_INDEX(_name, delimeter, i), LENGTH(SUBSTRING_INDEX(_name, delimeter, i - 1)) + 1), delimeter, ''));
			INSERT INTO temp_table(id, name) VALUES (_id, splitted);
            SET i = i + 1;                
		END WHILE;
	          
      END LOOP;

      SELECT * FROM temp_table;      
	CLOSE source;
	
	DROP TABLE IF EXISTS temp_table;
END;
$$

DELIMITER ;

CALL SPLITTED_TABLE('|');