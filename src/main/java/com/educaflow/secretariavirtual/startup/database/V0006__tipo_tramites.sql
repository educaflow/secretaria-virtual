-- Insertar tipos de trámites en expedientes_tipo_tramite
INSERT INTO expedientes_tipo_tramite (id, version, created_on, created_by, code, name)
VALUES (nextval('expedientes_tipo_tramite_seq'), 0, CURRENT_TIMESTAMP, 1, 'PROFESOR', 'Trámites si eres un profesores');

INSERT INTO expedientes_tipo_tramite (id, version, created_on, created_by, code, name)
VALUES (nextval('expedientes_tipo_tramite_seq'), 0, CURRENT_TIMESTAMP, 1, 'ALUMNO', 'Trámites si eres un alumno');

INSERT INTO expedientes_tipo_tramite (id, version, created_on, created_by, code, name)
VALUES (nextval('expedientes_tipo_tramite_seq'), 0, CURRENT_TIMESTAMP, 1, 'TUTOR', 'Trámites si eres una madre/padre/tutor');

INSERT INTO expedientes_tipo_tramite (id, version, created_on, created_by, code, name)
VALUES (nextval('expedientes_tipo_tramite_seq'), 0, CURRENT_TIMESTAMP, 1, 'DIRECCION', 'Trámites si eres de dirección');

INSERT INTO expedientes_tipo_tramite (id, version, created_on, created_by, code, name)
VALUES (nextval('expedientes_tipo_tramite_seq'), 0, CURRENT_TIMESTAMP, 1, 'ADMINISTRATIVO', 'Trámites si eres administrativo');

INSERT INTO expedientes_tipo_tramite (id, version, created_on, created_by, code, name)
VALUES (nextval('expedientes_tipo_tramite_seq'), 0, CURRENT_TIMESTAMP, 1, 'CONSERJE', 'Trámites si eres conserje');