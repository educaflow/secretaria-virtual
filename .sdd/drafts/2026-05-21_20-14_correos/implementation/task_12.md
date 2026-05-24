---
type: implementation-task
---

# Tarea 12 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

El XML de esta vista YA está materializado y validado con `xmllint` por el diseñador en `.sdd/drafts/2026-05-21_20-14_correos/design/views/Correo-GraficaDia.xml`. **Cópialo literalmente** (sin regenerarlo ni reformatearlo) a su ruta destino `src/main/java/com/educaflow/subsystem/correos/views/Correo-GraficaDia.xml`. Si detectas que el XML está mal, DETENTE y notifica; no lo edites aquí.

Fila de la tabla "Ficheros a crear o modificar":

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/correos/views/Correo-GraficaDia.xml` | Crear | k-vistas (charts.md) | `@GraficaDia-action` + chart apilado por estado, granularidad día. |

Descripción de diseño (Paso 5 — Vistas):

`design/views/Correo-GraficaDia.xml`: `action-view @GraficaDia-action` (sin `model`) → `chart @GraficaDia-chart` `stacked="true"`, 2 search-fields (fechaInicial, fechaFinal), dataset SQL `DATE_TRUNC('day', fecha_creacion)` agrupando por intervalo y estado, `series groupBy="_estado"`.

Trazabilidad:
- U-grafica-001 (granularidad día): chart con `DATE_TRUNC('day', ...)`, expuesto como subentrada de menú "Diaria".
- U-grafica-002 (fecha final >= inicial): Filtro `BETWEEN` (`fecha_creacion >= :fechaInicial AND <= :fechaFinal`) en el dataset: un rango invertido devuelve la gráfica vacía. **Divergencia con T-017** (contrato fijo; lo reinterpreta `sdd-implementer` al ejecutar tests).

Seguridad (Paso 8): SQL del chart con parámetros nombrados; cero concatenación.
