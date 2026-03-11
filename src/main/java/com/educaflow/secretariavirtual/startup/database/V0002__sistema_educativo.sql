INSERT INTO sistemaeducativo_grado (id, version, created_on, created_by, code, name) SELECT nextval('sistemaeducativo_grado_seq'), 0, CURRENT_TIMESTAMP, 1, 'A', 'Acreditación parcial de competencia';
INSERT INTO sistemaeducativo_grado (id, version, created_on, created_by, code, name) SELECT nextval('sistemaeducativo_grado_seq'), 0, CURRENT_TIMESTAMP, 1, 'B', 'Certificado de competencia';
INSERT INTO sistemaeducativo_grado (id, version, created_on, created_by, code, name) SELECT nextval('sistemaeducativo_grado_seq'), 0, CURRENT_TIMESTAMP, 1, 'C', 'Certificado profesional';
INSERT INTO sistemaeducativo_grado (id, version, created_on, created_by, code, name) SELECT nextval('sistemaeducativo_grado_seq'), 0, CURRENT_TIMESTAMP, 1, 'D', 'Ciclo formativo';
INSERT INTO sistemaeducativo_grado (id, version, created_on, created_by, code, name) SELECT nextval('sistemaeducativo_grado_seq'), 0, CURRENT_TIMESTAMP, 1, 'E', 'Curso de especialización';


INSERT INTO sistemaeducativo_nivel (id, version, created_on, created_by, code, name) SELECT nextval('sistemaeducativo_nivel_seq'), 0, CURRENT_TIMESTAMP, 1, '1', 'Ciclos formativos de grado Básico';
INSERT INTO sistemaeducativo_nivel (id, version, created_on, created_by, code, name) SELECT nextval('sistemaeducativo_nivel_seq'), 0, CURRENT_TIMESTAMP, 1, 'GM', 'Ciclos Formativos de Grado Medio';
INSERT INTO sistemaeducativo_nivel (id, version, created_on, created_by, code, name) SELECT nextval('sistemaeducativo_nivel_seq'), 0, CURRENT_TIMESTAMP, 1, 'GS', 'Ciclos Formativos de Grado Superior';
INSERT INTO sistemaeducativo_nivel (id, version, created_on, created_by, code, name) SELECT nextval('sistemaeducativo_nivel_seq'), 0, CURRENT_TIMESTAMP, 1, 'CES', 'Cursos de Especialización';


INSERT INTO sistemaeducativo_ley_educativa (id, version, created_on, created_by, code, name) SELECT nextval('sistemaeducativo_ley_educativa_seq'), 0, CURRENT_TIMESTAMP, 1, 'LOGSE','Ley Orgánica 1/1990, de 3 de octubre, de Ordenación General del Sistema Educativo';
INSERT INTO sistemaeducativo_ley_educativa (id, version, created_on, created_by, code, name) SELECT nextval('sistemaeducativo_ley_educativa_seq'), 0, CURRENT_TIMESTAMP, 1, 'LOE', 'Ley Orgánica 2/2006, de 3 de mayo, de Educación';
INSERT INTO sistemaeducativo_ley_educativa (id, version, created_on, created_by, code, name) SELECT nextval('sistemaeducativo_ley_educativa_seq'), 0, CURRENT_TIMESTAMP, 1, 'LOMCE','Ley Orgánica 8/2013, de 9 de diciembre, para la Mejora de la Calidad Educativa';
INSERT INTO sistemaeducativo_ley_educativa (id, version, created_on, created_by, code, name) SELECT nextval('sistemaeducativo_ley_educativa_seq'), 0, CURRENT_TIMESTAMP, 1, 'LOMLOE','Ley Orgánica 3/2020, de 29 de diciembre, de Modificación de la LOE';
INSERT INTO sistemaeducativo_ley_educativa (id, version, created_on, created_by, code, name) SELECT nextval('sistemaeducativo_ley_educativa_seq'), 0, CURRENT_TIMESTAMP, 1, 'LOFP','Ley Orgánica 3/2022, de 31 de marzo, de Ordenación e Integración de la Formación Profesional';


