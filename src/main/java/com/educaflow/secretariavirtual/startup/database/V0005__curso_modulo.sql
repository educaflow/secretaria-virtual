-- Relacionar Cursos con Módulos en la tabla sistemaeducativo_curso_modulo
INSERT INTO sistemaeducativo_curso_modulo (id, version, created_on, created_by, curso, modulo)
VALUES (nextval('sistemaeducativo_curso_modulo_seq'), 0, CURRENT_TIMESTAMP, 1,
        (SELECT id FROM sistemaeducativo_curso WHERE code = 'DAW1'),
        (SELECT id FROM sistemaeducativo_modulo WHERE code = 'IPE1'));

INSERT INTO sistemaeducativo_curso_modulo (id, version, created_on, created_by, curso, modulo)
VALUES (nextval('sistemaeducativo_curso_modulo_seq'), 0, CURRENT_TIMESTAMP, 1,
        (SELECT id FROM sistemaeducativo_curso WHERE code = 'DAW2'),
        (SELECT id FROM sistemaeducativo_modulo WHERE code = 'DWS'));

INSERT INTO sistemaeducativo_curso_modulo (id, version, created_on, created_by, curso, modulo)
VALUES (nextval('sistemaeducativo_curso_modulo_seq'), 0, CURRENT_TIMESTAMP, 1,
        (SELECT id FROM sistemaeducativo_curso WHERE code = 'DAW2'),
        (SELECT id FROM sistemaeducativo_modulo WHERE code = 'DWC'));

INSERT INTO sistemaeducativo_curso_modulo (id, version, created_on, created_by, curso, modulo)
VALUES (nextval('sistemaeducativo_curso_modulo_seq'), 0, CURRENT_TIMESTAMP, 1,
        (SELECT id FROM sistemaeducativo_curso WHERE code = 'DAW2'),
        (SELECT id FROM sistemaeducativo_modulo WHERE code = 'DAW'));

INSERT INTO sistemaeducativo_curso_modulo (id, version, created_on, created_by, curso, modulo)
VALUES (nextval('sistemaeducativo_curso_modulo_seq'), 0, CURRENT_TIMESTAMP, 1,
        (SELECT id FROM sistemaeducativo_curso WHERE code = 'DAW2'),
        (SELECT id FROM sistemaeducativo_modulo WHERE code = 'DIW'));

INSERT INTO sistemaeducativo_curso_modulo (id, version, created_on, created_by, curso, modulo)
VALUES (nextval('sistemaeducativo_curso_modulo_seq'), 0, CURRENT_TIMESTAMP, 1,
        (SELECT id FROM sistemaeducativo_curso WHERE code = 'DAW2'),
        (SELECT id FROM sistemaeducativo_modulo WHERE code = 'OPTATIVA2DAW'));

INSERT INTO sistemaeducativo_curso_modulo (id, version, created_on, created_by, curso, modulo)
VALUES (nextval('sistemaeducativo_curso_modulo_seq'), 0, CURRENT_TIMESTAMP, 1,
        (SELECT id FROM sistemaeducativo_curso WHERE code = 'DAM1'),
        (SELECT id FROM sistemaeducativo_modulo WHERE code = 'IPE1'));