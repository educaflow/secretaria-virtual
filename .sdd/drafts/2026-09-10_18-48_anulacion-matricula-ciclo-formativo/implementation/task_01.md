---
type: implementation-task
template: expediente
---

# Tarea 01 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-tramite

Materializa el `TramiteInstance.xml` del trámite.

El fichero **ya está materializado** en `design/TramiteInstance.xml`. **Cópialo literalmente** (contenido byte a byte) a la ruta destino

```
src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/TramiteInstance.xml
```

**sobrescribiendo** el fichero que hoy existe ahí. **MUST NOT** modificarlo, reescribirlo, regenerarlo ni reformatearlo: el XML del diseño es **contrato fijo**.

## Fila de la tabla `## 6. Ficheros a crear o modificar` del diseño

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/TramiteInstance.xml` | Modificar | `k-tramite` | Copia de `design/TramiteInstance.xml` (corrige la errata del `<help>` con el texto de ayuda de la especificación) |

## Paso del diseño (verbatim)

### Paso 1 — `TramiteInstance.xml`

El fichero está materializado en `design/TramiteInstance.xml`. **Cópialo literalmente** a `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/TramiteInstance.xml`, **sobrescribiendo** el actual. **MUST NOT** modificarlo, reescribirlo ni regenerarlo.

**Verificación:** el fichero existe y `diff` contra `design/TramiteInstance.xml` es vacío; el `<help>` ya no contiene «permite a ja un alumno».


## `## 2. Identidad del trámite y del tipo` (verbatim)

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

