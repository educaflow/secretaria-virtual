---
type: implementation-task
---

# Tarea 13 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

El XML de esta vista YA está materializado y validado con `xmllint` por el diseñador en `.sdd/drafts/2026-05-21_20-14_correos/design/views/Correo-GraficaSemana.xml`. **Cópialo literalmente** (sin regenerarlo ni reformatearlo) a su ruta destino `src/main/java/com/educaflow/subsystem/correos/views/Correo-GraficaSemana.xml`. Si detectas que el XML está mal, DETENTE y notifica; no lo edites aquí.

Fila de la tabla "Ficheros a crear o modificar":

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/correos/views/Correo-GraficaSemana.xml` | Crear | k-vistas (charts.md) | `@GraficaSemana-action` + chart, granularidad semana. |

Descripción de diseño (Paso 5 — Vistas):

`design/views/Correo-GraficaSemana.xml`: igual que `Correo-GraficaDia.xml` pero con `DATE_TRUNC('week', ...)`. `action-view @GraficaSemana-action` (sin `model`) → `chart @GraficaSemana-chart` `stacked="true"`, 2 search-fields (fechaInicial, fechaFinal), `series groupBy="_estado"`.

Trazabilidad: U-grafica-001 (granularidad semana): chart con `DATE_TRUNC('week', ...)`, expuesto como subentrada de menú "Semanal". U-grafica-002: filtro `BETWEEN` en el dataset.

Seguridad (Paso 8): SQL del chart con parámetros nombrados; cero concatenación.
