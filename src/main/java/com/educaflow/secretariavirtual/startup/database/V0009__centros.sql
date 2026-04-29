INSERT INTO common_centro (id, version, created_on, created_by, code, name, direccion, codigo_postal,municipio,conselleria, curso) VALUES
(nextval('common_centro_seq'), 0, CURRENT_TIMESTAMP, 1, '46019660', 'CIPFP Mislata', 'C/ Dolores Ibarruri, 32','46920',(SELECT id FROM common_municipio WHERE code = '46169'),(SELECT id FROM common_conselleria WHERE code = '01'), 2024),
(nextval('common_centro_seq'), 0, CURRENT_TIMESTAMP, 1, '03012165', 'CIPFP Batoi', 'C/ Societat Unió Musical, 8','46920',(SELECT id FROM common_municipio WHERE code = '03009'),(SELECT id FROM common_conselleria WHERE code = '01'), 2024)
ON CONFLICT (code) DO UPDATE SET
    municipio    = EXCLUDED.municipio,
    conselleria  = EXCLUDED.conselleria;