---
type: implementation-task
template: expediente
---

# Tarea 01 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-tramite

## Qué hay que hacer

Copia **literalmente** el fichero ya materializado por el diseño

- origen: `.sdd/drafts/2026-09-10_18-48_anulacion-matricula-ciclo-formativo/design/TramiteInstance.xml`
- destino: `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/TramiteInstance.xml`

**sobrescribiendo** el fichero que ya existe en el árbol (y sobrescribiendo también el esqueleto que hubiera dejado `CreateFilesTask`, si lo hubiera). El XML del diseño es **contrato fijo**: **MUST NOT** modificarse, reescribirse, reformatearse ni regenerarse. La acción de esta fila de §6 es `Modificar`: el fichero ya existe, no se crea de cero.

## Filas de la tabla «## 6. Ficheros a crear o modificar» del diseño (verbatim)

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/TramiteInstance.xml` | Modificar | `k-tramite` | Copia de `design/TramiteInstance.xml` (corrige la errata del `<help>` con el texto de ayuda de la especificación) |

## `### Paso 1 — TramiteInstance.xml` del diseño (verbatim)

### Paso 1 — `TramiteInstance.xml`

El fichero está materializado en `design/TramiteInstance.xml`. **Cópialo literalmente** a `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/TramiteInstance.xml`, **sobrescribiendo** el actual. **MUST NOT** modificarlo, reescribirlo ni regenerarlo.

**Verificación:** el fichero existe y `diff` contra `design/TramiteInstance.xml` es vacío; el `<help>` ya no contiene «permite a ja un alumno».


## `## 2. Identidad del trámite y del tipo` del diseño (verbatim)

## 2. Identidad del trámite y del tipo

| Dato | Valor |
|---|---|
| `code` del trámite | `AnulacionMatriculaCicloFormativo` |
| Nombre visible (`<name>`) | Anulación de matrícula en ciclo formativo |
| `tipoTramite` | `ALUMNO` (existe en `subsystem/expedientes/data-init/input/TipoTramites.xml`) |
| Carpeta del trámite | `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/` |
| Carpeta de la versión | `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/` |
| `<defaultTipoExpediente>` | `v1` |
| `code` / entidad del tipo | `AnulacionMatriculaCicloFormativoV1` |
| FQN de la entidad | `com.educaflow.subsystem.expedientes.db.AnulacionMatriculaCicloFormativoV1` |
| `basePackageName` | `com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1` |
| Clase `States` (generada) | `com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.States` |
| Form plantilla | `exp-AnulacionMatriculaCicloFormativoV1-Templates` |

La carpeta del trámite y su `TramiteInstance.xml` **ya existen** en el árbol (con el `<help>` erróneo y el `TipoExpedienteInstance.xml` vacío de fases), así que esas dos filas de la tabla de ficheros van con acción `Modificar`. No es una iniciativa de modificación de una versión existente: la versión `v1` no tiene todavía ni modelo, ni clases, ni vistas, ni expedientes vivos.
