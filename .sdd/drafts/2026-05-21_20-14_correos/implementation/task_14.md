---
type: implementation-task
---

# Tarea 14 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

El XML de esta vista YA está materializado y validado con `xmllint` por el diseñador en `.sdd/drafts/2026-05-21_20-14_correos/design/views/Correo-GraficaMes.xml`. **Cópialo literalmente** (sin regenerarlo ni reformatearlo) a su ruta destino `src/main/java/com/educaflow/subsystem/correos/views/Correo-GraficaMes.xml`. Si detectas que el XML está mal, DETENTE y notifica; no lo edites aquí.

Fila de la tabla "Ficheros a crear o modificar":

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/correos/views/Correo-GraficaMes.xml` | Crear | k-vistas (charts.md) | `@GraficaMes-action` + chart, granularidad mes. |

Descripción de diseño (Paso 5 — Vistas):

`design/views/Correo-GraficaMes.xml`: igual que `Correo-GraficaDia.xml` pero con `DATE_TRUNC('month', ...)`, category `type="month"`. `action-view @GraficaMes-action` (sin `model`) → `chart @GraficaMes-chart` `stacked="true"`, 2 search-fields (fechaInicial, fechaFinal), `series groupBy="_estado"`.

Trazabilidad: U-grafica-001 (granularidad mes): chart con `DATE_TRUNC('month', ...)`, expuesto como subentrada de menú "Mensual". U-grafica-002: filtro `BETWEEN` en el dataset.

Seguridad (Paso 8): SQL del chart con parámetros nombrados; cero concatenación.
