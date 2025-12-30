-- Insertar ciclos formativos de INFORMATICA
INSERT INTO sistemaeducativo_ciclo (id, version, created_on, created_by, code, name, familia_profesional, grado, nivel)
VALUES (nextval('sistemaeducativo_ciclo_seq'), 0, CURRENT_TIMESTAMP, 1, 'DAW', 'Desarrollo de Aplicaciones Web',
        (SELECT id FROM sistemaeducativo_familia_profesional WHERE code = 'INFORMATICA'), (SELECT id FROM sistemaeducativo_grado WHERE code = 'D'), (SELECT id FROM sistemaeducativo_nivel WHERE code = '3'));

INSERT INTO sistemaeducativo_ciclo (id, version, created_on, created_by, code, name, familia_profesional, grado, nivel)
VALUES (nextval('sistemaeducativo_ciclo_seq'), 0, CURRENT_TIMESTAMP, 1, 'DAM', 'Desarrollo de Aplicaciones Multiplataforma',
        (SELECT id FROM sistemaeducativo_familia_profesional WHERE code = 'INFORMATICA'), (SELECT id FROM sistemaeducativo_grado WHERE code = 'D'), (SELECT id FROM sistemaeducativo_nivel WHERE code = '3'));

INSERT INTO sistemaeducativo_ciclo (id, version, created_on, created_by, code, name, familia_profesional, grado, nivel)
VALUES (nextval('sistemaeducativo_ciclo_seq'), 0, CURRENT_TIMESTAMP, 1, 'ASIR', 'Administración de Sistemas Informáticos en Red',
        (SELECT id FROM sistemaeducativo_familia_profesional WHERE code = 'INFORMATICA'), (SELECT id FROM sistemaeducativo_grado WHERE code = 'D'), (SELECT id FROM sistemaeducativo_nivel WHERE code = '3'));

INSERT INTO sistemaeducativo_ciclo (id, version, created_on, created_by, code, name, familia_profesional, grado, nivel)
VALUES (nextval('sistemaeducativo_ciclo_seq'), 0, CURRENT_TIMESTAMP, 1, 'SMR', 'Sistemas Microinformáticos y Redes',
        (SELECT id FROM sistemaeducativo_familia_profesional WHERE code = 'INFORMATICA'), (SELECT id FROM sistemaeducativo_grado WHERE code = 'D'), (SELECT id FROM sistemaeducativo_nivel WHERE code = '2'));

-- Insertar ciclos formativos de HOSTELERIA_TURISMO
INSERT INTO sistemaeducativo_ciclo (id, version, created_on, created_by, code, name, familia_profesional, grado, nivel)
VALUES (nextval('sistemaeducativo_ciclo_seq'), 0, CURRENT_TIMESTAMP, 1, 'GAT', 'Gestión de Alojamientos Turísticos',
        (SELECT id FROM sistemaeducativo_familia_profesional WHERE code = 'HOSTELERIA_TURISMO'), (SELECT id FROM sistemaeducativo_grado WHERE code = 'D'), (SELECT id FROM sistemaeducativo_nivel WHERE code = '3'));

INSERT INTO sistemaeducativo_ciclo (id, version, created_on, created_by, code, name, familia_profesional, grado, nivel)
VALUES (nextval('sistemaeducativo_ciclo_seq'), 0, CURRENT_TIMESTAMP, 1, 'GIAT', 'Guía, Información y Asistencia Turística',
        (SELECT id FROM sistemaeducativo_familia_profesional WHERE code = 'HOSTELERIA_TURISMO'), (SELECT id FROM sistemaeducativo_grado WHERE code = 'D'), (SELECT id FROM sistemaeducativo_nivel WHERE code = '3'));

INSERT INTO sistemaeducativo_ciclo (id, version, created_on, created_by, code, name, familia_profesional, grado, nivel)
VALUES (nextval('sistemaeducativo_ciclo_seq'), 0, CURRENT_TIMESTAMP, 1, 'AVGA', 'Agencia de Viajes y Gestión de Eventos',
        (SELECT id FROM sistemaeducativo_familia_profesional WHERE code = 'HOSTELERIA_TURISMO'), (SELECT id FROM sistemaeducativo_grado WHERE code = 'D'), (SELECT id FROM sistemaeducativo_nivel WHERE code = '3'));