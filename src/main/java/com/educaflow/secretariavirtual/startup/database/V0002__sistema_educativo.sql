INSERT INTO sistemaeducativo_grado (id, version, created_on, created_by, code, name) SELECT nextval('sistemaeducativo_grado_seq'), 0, CURRENT_TIMESTAMP, 1, 'A', 'Acreditación parcial de competencia';
INSERT INTO sistemaeducativo_grado (id, version, created_on, created_by, code, name) SELECT nextval('sistemaeducativo_grado_seq'), 0, CURRENT_TIMESTAMP, 1, 'B', 'Certificado de competencia';
INSERT INTO sistemaeducativo_grado (id, version, created_on, created_by, code, name) SELECT nextval('sistemaeducativo_grado_seq'), 0, CURRENT_TIMESTAMP, 1, 'C', 'Certificado profesional';
INSERT INTO sistemaeducativo_grado (id, version, created_on, created_by, code, name) SELECT nextval('sistemaeducativo_grado_seq'), 0, CURRENT_TIMESTAMP, 1, 'D', 'Ciclo formativo';
INSERT INTO sistemaeducativo_grado (id, version, created_on, created_by, code, name) SELECT nextval('sistemaeducativo_grado_seq'), 0, CURRENT_TIMESTAMP, 1, 'E', 'Curso de especialización';


INSERT INTO sistemaeducativo_nivel (id, version, created_on, created_by, code, name) SELECT nextval('sistemaeducativo_nivel_seq'), 0, CURRENT_TIMESTAMP, 1, '1', 'Ciclos formativos de grado Básico';
INSERT INTO sistemaeducativo_nivel (id, version, created_on, created_by, code, name) SELECT nextval('sistemaeducativo_nivel_seq'), 0, CURRENT_TIMESTAMP, 1, '2', 'Ciclos Formativos de Grado Medio';
INSERT INTO sistemaeducativo_nivel (id, version, created_on, created_by, code, name) SELECT nextval('sistemaeducativo_nivel_seq'), 0, CURRENT_TIMESTAMP, 1, '3', 'Ciclos Formativos de Grado Superior';


INSERT INTO sistemaeducativo_ley_educativa (id, version, created_on, created_by, code, name) SELECT nextval('sistemaeducativo_ley_educativa_seq'), 0, CURRENT_TIMESTAMP, 1, 'LOGSE','Ley Orgánica 1/1990, de 3 de octubre, de Ordenación General del Sistema Educativo';
INSERT INTO sistemaeducativo_ley_educativa (id, version, created_on, created_by, code, name) SELECT nextval('sistemaeducativo_ley_educativa_seq'), 0, CURRENT_TIMESTAMP, 1, 'LOE', 'Ley Orgánica 2/2006, de 3 de mayo, de Educación';
INSERT INTO sistemaeducativo_ley_educativa (id, version, created_on, created_by, code, name) SELECT nextval('sistemaeducativo_ley_educativa_seq'), 0, CURRENT_TIMESTAMP, 1, 'LOMCE','Ley Orgánica 8/2013, de 9 de diciembre, para la Mejora de la Calidad Educativa';
INSERT INTO sistemaeducativo_ley_educativa (id, version, created_on, created_by, code, name) SELECT nextval('sistemaeducativo_ley_educativa_seq'), 0, CURRENT_TIMESTAMP, 1, 'LOMLOE','Ley Orgánica 3/2020, de 29 de diciembre, de Modificación de la LOE';
INSERT INTO sistemaeducativo_ley_educativa (id, version, created_on, created_by, code, name) SELECT nextval('sistemaeducativo_ley_educativa_seq'), 0, CURRENT_TIMESTAMP, 1, 'LOFP','Ley Orgánica 3/2022, de 31 de marzo, de Ordenación e Integración de la Formación Profesional';


INSERT INTO sistemaeducativo_familia_profesional (id, version, created_on, created_by, code, NAME) SELECT nextval('sistemaeducativo_familia_profesional_seq'), 0, CURRENT_TIMESTAMP, 1, 'FISICAS_DEPORTIVAS', 'Actividades Físicas y Deportivas' ;
INSERT INTO sistemaeducativo_familia_profesional (id, version, created_on, created_by, code, NAME) SELECT nextval('sistemaeducativo_familia_profesional_seq'), 0, CURRENT_TIMESTAMP, 1, 'ADMINISTRACION', 'Administración y Gestión' ;
INSERT INTO sistemaeducativo_familia_profesional (id, version, created_on, created_by, code, NAME) SELECT nextval('sistemaeducativo_familia_profesional_seq'), 0, CURRENT_TIMESTAMP, 1, 'AGRARIA', 'Agraria' ;
INSERT INTO sistemaeducativo_familia_profesional (id, version, created_on, created_by, code, NAME) SELECT nextval('sistemaeducativo_familia_profesional_seq'), 0, CURRENT_TIMESTAMP, 1, 'GRAFICAS', 'Artes Gráficas' ;
INSERT INTO sistemaeducativo_familia_profesional (id, version, created_on, created_by, code, NAME) SELECT nextval('sistemaeducativo_familia_profesional_seq'), 0, CURRENT_TIMESTAMP, 1, 'COMERCIO', 'Comercio y Marketing' ;
INSERT INTO sistemaeducativo_familia_profesional (id, version, created_on, created_by, code, NAME) SELECT nextval('sistemaeducativo_familia_profesional_seq'), 0, CURRENT_TIMESTAMP, 1, 'EDIFICACION', 'Edificación y Obra Civil' ;
INSERT INTO sistemaeducativo_familia_profesional (id, version, created_on, created_by, code, NAME) SELECT nextval('sistemaeducativo_familia_profesional_seq'), 0, CURRENT_TIMESTAMP, 1, 'ELECTRICIDAD', 'Electricidad y Electrónica' ;
INSERT INTO sistemaeducativo_familia_profesional (id, version, created_on, created_by, code, NAME) SELECT nextval('sistemaeducativo_familia_profesional_seq'), 0, CURRENT_TIMESTAMP, 1, 'FABRIC_MECANICA', 'Fabricación Mecánica' ;
INSERT INTO sistemaeducativo_familia_profesional (id, version, created_on, created_by, code, NAME) SELECT nextval('sistemaeducativo_familia_profesional_seq'), 0, CURRENT_TIMESTAMP, 1, 'HOSTELERIA_TURISMO', 'Hostelería y Turismo' ;
INSERT INTO sistemaeducativo_familia_profesional (id, version, created_on, created_by, code, NAME) SELECT nextval('sistemaeducativo_familia_profesional_seq'), 0, CURRENT_TIMESTAMP, 1, 'IMAGENPERSONAL', 'Imagen Personal' ;
INSERT INTO sistemaeducativo_familia_profesional (id, version, created_on, created_by, code, NAME) SELECT nextval('sistemaeducativo_familia_profesional_seq'), 0, CURRENT_TIMESTAMP, 1, 'IMAGENSONIDO', 'Imagen y Sonido' ;
INSERT INTO sistemaeducativo_familia_profesional (id, version, created_on, created_by, code, NAME) SELECT nextval('sistemaeducativo_familia_profesional_seq'), 0, CURRENT_TIMESTAMP, 1, 'INDUSTRIA_ALIMENTARIA', 'Industrias Alimentarias' ;
INSERT INTO sistemaeducativo_familia_profesional (id, version, created_on, created_by, code, NAME) SELECT nextval('sistemaeducativo_familia_profesional_seq'), 0, CURRENT_TIMESTAMP, 1, 'INDUSTRIA_EXTRACTIVA', 'Industrias Extractivas' ;
INSERT INTO sistemaeducativo_familia_profesional (id, version, created_on, created_by, code, NAME) SELECT nextval('sistemaeducativo_familia_profesional_seq'), 0, CURRENT_TIMESTAMP, 1, 'INFORMATICA', 'Informática y Comunicaciones' ;
INSERT INTO sistemaeducativo_familia_profesional (id, version, created_on, created_by, code, NAME) SELECT nextval('sistemaeducativo_familia_profesional_seq'), 0, CURRENT_TIMESTAMP, 1, 'INSTALACION', 'Instalación y Mantenimiento' ;
INSERT INTO sistemaeducativo_familia_profesional (id, version, created_on, created_by, code, NAME) SELECT nextval('sistemaeducativo_familia_profesional_seq'), 0, CURRENT_TIMESTAMP, 1, 'MADERA', 'Madera, Mueble y Corcho' ;
INSERT INTO sistemaeducativo_familia_profesional (id, version, created_on, created_by, code, NAME) SELECT nextval('sistemaeducativo_familia_profesional_seq'), 0, CURRENT_TIMESTAMP, 1, 'MARITIMO', 'Marítimo - Pesquera' ;
INSERT INTO sistemaeducativo_familia_profesional (id, version, created_on, created_by, code, NAME) SELECT nextval('sistemaeducativo_familia_profesional_seq'), 0, CURRENT_TIMESTAMP, 1, 'QUIMICA', 'Química' ;
INSERT INTO sistemaeducativo_familia_profesional (id, version, created_on, created_by, code, NAME) SELECT nextval('sistemaeducativo_familia_profesional_seq'), 0, CURRENT_TIMESTAMP, 1, 'SANIDAD', 'Sanidad' ;
INSERT INTO sistemaeducativo_familia_profesional (id, version, created_on, created_by, code, NAME) SELECT nextval('sistemaeducativo_familia_profesional_seq'), 0, CURRENT_TIMESTAMP, 1, 'MEDIO_AMBIENTE', 'Seguridad y Medio Ambiente' ;
INSERT INTO sistemaeducativo_familia_profesional (id, version, created_on, created_by, code, NAME) SELECT nextval('sistemaeducativo_familia_profesional_seq'), 0, CURRENT_TIMESTAMP, 1, 'SOCIO_CULTURALES', 'Servicios Socioculturales y a la Comunidad' ;
INSERT INTO sistemaeducativo_familia_profesional (id, version, created_on, created_by, code, NAME) SELECT nextval('sistemaeducativo_familia_profesional_seq'), 0, CURRENT_TIMESTAMP, 1, 'TEXTIL', 'Textil, Confección y Piel' ;
INSERT INTO sistemaeducativo_familia_profesional (id, version, created_on, created_by, code, NAME) SELECT nextval('sistemaeducativo_familia_profesional_seq'), 0, CURRENT_TIMESTAMP, 1, 'TRANSPORTE', 'Transporte y Mantenimiento de Vehículos' ;
INSERT INTO sistemaeducativo_familia_profesional (id, version, created_on, created_by, code, NAME) SELECT nextval('sistemaeducativo_familia_profesional_seq'), 0, CURRENT_TIMESTAMP, 1, 'VIDRIO', 'Vidrio y Cerámica' ;
INSERT INTO sistemaeducativo_familia_profesional (id, version, created_on, created_by, code, NAME) SELECT nextval('sistemaeducativo_familia_profesional_seq'), 0, CURRENT_TIMESTAMP, 1, 'ARTES_GRAFICAS', 'Artes y Artesanías' ;
INSERT INTO sistemaeducativo_familia_profesional (id, version, created_on, created_by, code, NAME) SELECT nextval('sistemaeducativo_familia_profesional_seq'), 0, CURRENT_TIMESTAMP, 1, 'ENERGIA', 'Energía y Agua' ;


INSERT INTO sistemaeducativo_modulo (id, version, created_on, created_by, code, NAME) SELECT nextval('sistemaeducativo_modulo_seq'), 0, CURRENT_TIMESTAMP, 1, 'DWS', 'Desarrollo Web Servidor' ;
INSERT INTO sistemaeducativo_modulo (id, version, created_on, created_by, code, NAME) SELECT nextval('sistemaeducativo_modulo_seq'), 0, CURRENT_TIMESTAMP, 1, 'DWC', 'Desarrollo Web Cliente' ;
INSERT INTO sistemaeducativo_modulo (id, version, created_on, created_by, code, NAME) SELECT nextval('sistemaeducativo_modulo_seq'), 0, CURRENT_TIMESTAMP, 1, 'DAW', 'Despliegue de Aplicaciones Web' ;
INSERT INTO sistemaeducativo_modulo (id, version, created_on, created_by, code, NAME) SELECT nextval('sistemaeducativo_modulo_seq'), 0, CURRENT_TIMESTAMP, 1, 'DIW', 'Diseño de Interfaces Web' ;
INSERT INTO sistemaeducativo_modulo (id, version, created_on, created_by, code, NAME) SELECT nextval('sistemaeducativo_modulo_seq'), 0, CURRENT_TIMESTAMP, 1, 'OPTATIVA2DAW', 'Testing' ;
INSERT INTO sistemaeducativo_modulo (id, version, created_on, created_by, code, NAME) SELECT nextval('sistemaeducativo_modulo_seq'), 0, CURRENT_TIMESTAMP, 1, 'IPE1', 'Itinerario Personal para la Empleabilidad I' ;
