---
type: design-guidelines
---

- Usar el widget de fecha estándar del proyecto para `fechaInicio`/`fechaFin` (lo pidió explícitamente el usuario); consultar la convención de campos de fecha en `k-vistas`/`k-validaciones`.
- El enum `TipoJornadaFaltaJustificacionFaltaProfesoradoV1` ya existe en `domains.xml` con 2 valores (`TODA_LA_JORNADA`, `JORNADA_PARCIAL`): redefinir sus items en el mismo sitio (no crear un enum nuevo), para no romper las referencias que ya usan su FQN en `documentospdf/_template.xml` y en `recepcion/StateEventValidatorImpl.kt`.
- Los campos `dias`/`mes`/`anyo` y el enum `MesJustificacionFaltaProfesoradoV1` se eliminan por completo de `domains.xml`; no hay que dejar ningún residuo ni columna de compatibilidad, porque no hace falta migrar datos (la base de datos se vacía antes de desplegar este cambio).
