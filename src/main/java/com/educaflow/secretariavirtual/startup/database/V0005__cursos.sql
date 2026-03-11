INSERT INTO sistemaeducativo_curso (id, version, created_on, created_by, code, name, ciclo, ley_educativa)
VALUES (nextval('sistemaeducativo_curso_seq'), 0, CURRENT_TIMESTAMP, 1, 'DAW1', '1º DAW',
        (SELECT id FROM sistemaeducativo_ciclo WHERE code = 'DAW'),
        (SELECT id FROM sistemaeducativo_ley_educativa WHERE code = 'LOFP'));

INSERT INTO sistemaeducativo_curso (id, version, created_on, created_by, code, name, ciclo, ley_educativa)
VALUES (nextval('sistemaeducativo_curso_seq'), 0, CURRENT_TIMESTAMP, 1, 'DAW2', '2º DAW',
        (SELECT id FROM sistemaeducativo_ciclo WHERE code = 'DAW'),
        (SELECT id FROM sistemaeducativo_ley_educativa WHERE code = 'LOFP'));

INSERT INTO sistemaeducativo_curso (id, version, created_on, created_by, code, name, ciclo, ley_educativa)
VALUES (nextval('sistemaeducativo_curso_seq'), 0, CURRENT_TIMESTAMP, 1, 'DAM1', '1º DAM',
        (SELECT id FROM sistemaeducativo_ciclo WHERE code = 'DAM'),
        (SELECT id FROM sistemaeducativo_ley_educativa WHERE code = 'LOFP'));

INSERT INTO sistemaeducativo_curso (id, version, created_on, created_by, code, name, ciclo, ley_educativa)
VALUES (nextval('sistemaeducativo_curso_seq'), 0, CURRENT_TIMESTAMP, 1, 'DAM2', '2º DAM',
        (SELECT id FROM sistemaeducativo_ciclo WHERE code = 'DAM'),
        (SELECT id FROM sistemaeducativo_ley_educativa WHERE code = 'LOFP'));

INSERT INTO sistemaeducativo_curso (id, version, created_on, created_by, code, name, ciclo, ley_educativa)
VALUES (nextval('sistemaeducativo_curso_seq'), 0, CURRENT_TIMESTAMP, 1, 'ASIR1', '1º ASIR',
        (SELECT id FROM sistemaeducativo_ciclo WHERE code = 'ASIR'),
        (SELECT id FROM sistemaeducativo_ley_educativa WHERE code = 'LOFP'));

INSERT INTO sistemaeducativo_curso (id, version, created_on, created_by, code, name, ciclo, ley_educativa)
VALUES (nextval('sistemaeducativo_curso_seq'), 0, CURRENT_TIMESTAMP, 1, 'ASIR2', '2º ASIR',
        (SELECT id FROM sistemaeducativo_ciclo WHERE code = 'ASIR'),
        (SELECT id FROM sistemaeducativo_ley_educativa WHERE code = 'LOFP'));

INSERT INTO sistemaeducativo_curso (id, version, created_on, created_by, code, name, ciclo, ley_educativa)
VALUES (nextval('sistemaeducativo_curso_seq'), 0, CURRENT_TIMESTAMP, 1, 'SMR1', '1º 1SMR',
        (SELECT id FROM sistemaeducativo_ciclo WHERE code = 'SMR'),
        (SELECT id FROM sistemaeducativo_ley_educativa WHERE code = 'LOFP'));

INSERT INTO sistemaeducativo_curso (id, version, created_on, created_by, code, name, ciclo, ley_educativa)
VALUES (nextval('sistemaeducativo_curso_seq'), 0, CURRENT_TIMESTAMP, 1, 'SMR2', '2º SMR',
        (SELECT id FROM sistemaeducativo_ciclo WHERE code = 'SMR'),
        (SELECT id FROM sistemaeducativo_ley_educativa WHERE code = 'LOFP'));

INSERT INTO sistemaeducativo_curso (id, version, created_on, created_by, code, name, ciclo, ley_educativa)
VALUES (nextval('sistemaeducativo_curso_seq'), 0, CURRENT_TIMESTAMP, 1, 'GAT1', '1º GAT',
        (SELECT id FROM sistemaeducativo_ciclo WHERE code = 'GAT'),
        (SELECT id FROM sistemaeducativo_ley_educativa WHERE code = 'LOFP'));

INSERT INTO sistemaeducativo_curso (id, version, created_on, created_by, code, name, ciclo, ley_educativa)
VALUES (nextval('sistemaeducativo_curso_seq'), 0, CURRENT_TIMESTAMP, 1, 'GAT2', '2º GAT',
        (SELECT id FROM sistemaeducativo_ciclo WHERE code = 'GAT'),
        (SELECT id FROM sistemaeducativo_ley_educativa WHERE code = 'LOFP'));

INSERT INTO sistemaeducativo_curso (id, version, created_on, created_by, code, name, ciclo, ley_educativa)
VALUES (nextval('sistemaeducativo_curso_seq'), 0, CURRENT_TIMESTAMP, 1, 'GIAT1', '1º GIAT',
        (SELECT id FROM sistemaeducativo_ciclo WHERE code = 'GIAT'),
        (SELECT id FROM sistemaeducativo_ley_educativa WHERE code = 'LOFP'));

INSERT INTO sistemaeducativo_curso (id, version, created_on, created_by, code, name, ciclo, ley_educativa)
VALUES (nextval('sistemaeducativo_curso_seq'), 0, CURRENT_TIMESTAMP, 1, 'GIAT2', '2º GIAT',
        (SELECT id FROM sistemaeducativo_ciclo WHERE code = 'GIAT'),
        (SELECT id FROM sistemaeducativo_ley_educativa WHERE code = 'LOFP'));

INSERT INTO sistemaeducativo_curso (id, version, created_on, created_by, code, name, ciclo, ley_educativa)
VALUES (nextval('sistemaeducativo_curso_seq'), 0, CURRENT_TIMESTAMP, 1, 'AVGA1', '1º AVGA',
        (SELECT id FROM sistemaeducativo_ciclo WHERE code = 'AVGA'),
        (SELECT id FROM sistemaeducativo_ley_educativa WHERE code = 'LOFP'));

INSERT INTO sistemaeducativo_curso (id, version, created_on, created_by, code, name, ciclo, ley_educativa)
VALUES (nextval('sistemaeducativo_curso_seq'), 0, CURRENT_TIMESTAMP, 1, 'AVGA2', '2º AVGA',
        (SELECT id FROM sistemaeducativo_ciclo WHERE code = 'AVGA'),
        (SELECT id FROM sistemaeducativo_ley_educativa WHERE code = 'LOFP'));