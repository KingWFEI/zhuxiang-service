ALTER TABLE region MODIFY code VARCHAR(50) NULL;

UPDATE region
SET code = NULL
WHERE TRIM(code) = '';
