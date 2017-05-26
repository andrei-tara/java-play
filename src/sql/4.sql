DROP PROCEDURE IF EXISTS SEARCH_BUGS;
DELIMITER $$
CREATE PROCEDURE SEARCH_BUGS(startDate DATE, endDate DATE)
BEGIN
  DECLARE output VARCHAR(500);
  DECLARE indexDate DATE;  
  SET output = '';
  SET indexDate = startDate;
  
  iterLoop: LOOP  
     IF indexDate = startDate THEN     
       SET output = CONCAT(output, ' SELECT * FROM bugs WHERE open_date <= DATE(\'',indexDate,'\') AND close_date = DATE(\'',indexDate,'\') UNION ALL');     
     ELSEIF indexDate = endDate THEN     
       SET output = CONCAT(output, ' SELECT * FROM bugs WHERE open_date <= DATE(\'',indexDate,'\') AND close_date >= DATE(\'',indexDate,'\') UNION ALL');     
     ELSE     
       SET output = CONCAT(output, ' SELECT * FROM bugs WHERE open_date <= DATE(\'',indexDate,'\') AND close_date = DATE(\'',indexDate,'\') UNION ALL');     
     END IF;

     SET indexDate = DATE_ADD(indexDate, INTERVAL 1 DAY);     
     IF indexDate <= endDate THEN     
        ITERATE iterLoop;     
     END IF;
     
     LEAVE iterLoop;   
   END LOOP iterLoop;

   SET output = LEFT(output, LENGTH(output)-LENGTH('UNION ALL'));
   PREPARE  statement FROM @output;
   EXECUTE statement;   
END;
$$
DELIMITER ;

CALL SEARCH_BUGS(STR_TO_DATE('2016-11-10', '%Y-%m-%d'), STR_TO_DATE('2017-11-03', '%Y-%m-%d'));