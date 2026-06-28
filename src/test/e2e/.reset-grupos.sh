#\!/usr/bin/env bash
# Limpia SOLO las tablas del subsistema grupos-y-notas (no toca datos maestros).
PGPASSWORD=educaflow psql -h localhost -p 5432 -U educaflow -d educaflow -v ON_ERROR_STOP=1 -q <<'SQL'
TRUNCATE TABLE gruposnotas_nota, gruposnotas_alumno_grupo, gruposnotas_modulo_grupo, gruposnotas_grupo RESTART IDENTITY CASCADE;
SQL
