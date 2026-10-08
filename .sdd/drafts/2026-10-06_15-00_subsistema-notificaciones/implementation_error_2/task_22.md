---
type: implementation-task
template: system
---

# Tarea 22 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

El fichero XML **ya está materializado** en `design/views/Centro-Notificacion.xml` (validado con `xmllint` por el diseñador). **MUST** copiarse **literalmente** a su ruta destino `src/main/java/com/educaflow/subsystem/notificaciones/views/Centro-Notificacion.xml`, **sin regenerarlo, reescribirlo ni reformatearlo** (ver `implementation.md` §1 y §3). Acción `Crear`: si el destino ya existe, se reporta `CONFLICT` según `implementation.md` §3.

### Ficheros a crear o modificar (fila de esta tarea)

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/notificaciones/views/Centro-Notificacion.xml` | Crear | k-vistas (grids.md, actions.md) | «Del centro» |

> **Nota para `/sdd-implementer`:** los XML de `domains/`, `views/` y `menus.xml` ya están materializados en la carpeta `design/`. **MUST NOT** modificarlos, reescribirlos ni regenerarlos: se **copian verbatim** a su ubicación final (`menus.xml` se fusiona en el `menus.xml` único del proyecto; `views/DefaultModelController.xml` va a `base/infrastructure/controller/DefaultModelController.xml`, Paso 8). El código Java es lo único que se implementa a partir de las firmas y comentarios del diseño.

### Paso 10 — Vistas

Copiar `design/views/*.xml` a `subsystem/notificaciones/views/` (las vistas antiguas se borraron en el Paso 1), **salvo** `design/views/DefaultModelController.xml`, que no es del subsistema y se copia a `base/infrastructure/controller/` (Paso 8).

**`Centro-Notificacion.xml`** — `action-view` con `<domain>self.centro.id IN (:idsCentrosGestionados)</domain>` y `<context … expr="eval: com.educaflow.subsystem.notificaciones.util.GestorNotificacionesUtil.idsCentrosGestionados(__user__)"/>` (la misma expresión que los `conditionParams` de los permisos; la clase es `@ScriptAllowed`). Grid con columnas centro, tipo, estado, DNI, nombre, apellidos, motivo, destino, expediente, fechas; `hilite` `danger` si FALLIDO; `action` = `Remote-abrirDelCentro`.

Verificación: `./gradlew -q test --tests 'com.educaflow.views.*'` tras el Paso 11 (con el glosario, `VAR-7.2` y `VAR-7.3` ya ampliados).

## Trazabilidad Origen spec → V/R/U → ubicación (filas de esta tarea)

### U

| ID | Origen spec | Ubicación |
|---|---|---|
| U-notificaciones-centro-001 | RUI-notificaciones-centro-listado-001 | `Centro@Notificacion-grid` `<hilite color="danger" if="estado == 'FALLIDO'">` |
