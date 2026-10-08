---
type: implementation-task
template: system
---

# Tarea 24 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

El fichero XML **ya está materializado** en `design/views/Eleccion-Notificacion.xml` (validado con `xmllint` por el diseñador). **MUST** copiarse **literalmente** a su ruta destino `src/main/java/com/educaflow/subsystem/notificaciones/views/Eleccion-Notificacion.xml`, **sin regenerarlo, reescribirlo ni reformatearlo** (ver `implementation.md` §1 y §3). Acción `Crear`: si el destino ya existe, se reporta `CONFLICT` según `implementation.md` §3.

### Ficheros a crear o modificar (fila de esta tarea)

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/notificaciones/views/Eleccion-Notificacion.xml` | Crear | k-vistas (forms.md, actions.md) | Elección de canal (popup) |

> **Nota para `/sdd-implementer`:** los XML de `domains/`, `views/` y `menus.xml` ya están materializados en la carpeta `design/`. **MUST NOT** modificarlos, reescribirlos ni regenerarlos: se **copian verbatim** a su ubicación final (`menus.xml` se fusiona en el `menus.xml` único del proyecto; `views/DefaultModelController.xml` va a `base/infrastructure/controller/DefaultModelController.xml`, Paso 8). El código Java es lo único que se implementa a partir de las firmas y comentarios del diseño.

### Paso 10 — Vistas

Copiar `design/views/*.xml` a `subsystem/notificaciones/views/` (las vistas antiguas se borraron en el Paso 1), **salvo** `design/views/DefaultModelController.xml`, que no es del subsistema y se copia a `base/infrastructure/controller/` (Paso 8).

**`Eleccion-Notificacion.xml`** — `action-view` de form en popup (`popup`, `popup-save=false`, `show-toolbar=false`, `show-toolbar-form=false`, `forceEdit`). Form `Eleccion@Notificacion-form` (model `Notificacion`), panel «Canal» y botones Cancelar (`close`) y Continuar (`readonlyIf="tipoNotificacion == null"`, grupo `Remote-continuarAlta` → `close`).

```
Canal:    tttt········   ← tipoNotificacion(4, RadioSelect horizontal) — único campo, sin relacionados
Botones:  ········ccxx   ← Cancelar(2, offset 8) + Continuar(2)
```

Verificación: `./gradlew -q test --tests 'com.educaflow.views.*'` tras el Paso 11 (con el glosario, `VAR-7.2` y `VAR-7.3` ya ampliados).

## Trazabilidad Origen spec → V/R/U → ubicación (filas de esta tarea)

### U

| ID | Origen spec | Ubicación |
|---|---|---|
| U-notificaciones-todas-001 | RUI-notificaciones-todas-eleccion-canal-001 | `Eleccion@Notificacion-form` `tipoNotificacion` sin valor por defecto (el dominio no declara `default`) |
| U-notificaciones-todas-002 | RUI-notificaciones-todas-eleccion-canal-002 | `Eleccion@Notificacion-form` `btnContinuar` `readonlyIf="tipoNotificacion == null"` |
