---
type: implementation-task
template: system
---

# Tarea 27 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

El fichero XML **ya está materializado** en `design/views/Mis-Correo.xml` (validado con `xmllint` por el diseñador). **MUST** copiarse **literalmente** a su ruta destino `src/main/java/com/educaflow/subsystem/notificaciones/views/Mis-Correo.xml`, **sin regenerarlo, reescribirlo ni reformatearlo** (ver `implementation.md` §1 y §3). Acción `Crear`: si el destino ya existe, se reporta `CONFLICT` según `implementation.md` §3.

### Ficheros a crear o modificar (fila de esta tarea)

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/notificaciones/views/Mis-Correo.xml` | Crear | k-vistas (forms.md) | Form del correo en «Recibidas» |

> **Nota para `/sdd-implementer`:** los XML de `domains/`, `views/` y `menus.xml` ya están materializados en la carpeta `design/`. **MUST NOT** modificarlos, reescribirlos ni regenerarlos: se **copian verbatim** a su ubicación final (`menus.xml` se fusiona en el `menus.xml` único del proyecto; `views/DefaultModelController.xml` va a `base/infrastructure/controller/DefaultModelController.xml`, Paso 8). El código Java es lo único que se implementa a partir de las firmas y comentarios del diseño.

### Paso 10 — Vistas

Copiar `design/views/*.xml` a `subsystem/notificaciones/views/` (las vistas antiguas se borraron en el Paso 1), **salvo** `design/views/DefaultModelController.xml`, que no es del subsistema y se copia a `base/infrastructure/controller/` (Paso 8).

**`Mis-Correo.xml`** — form `Mis@Correo-form` de solo lectura; adjuntos (Ref) solo si hay alguno; Salir `close`.

```
  sssssssssfff   ← asunto(9) + fechaEnvio(3)
  bbbbbbbbbbbb   ← cuerpo(12, Text)
  ppppppeeeeee   ← para(6) + enCopia(6, showIf enCopia)   [con copia]
  pppppp······   ← enCopia oculto al borde derecho         [sin copia]
  Botones: ..........ss   ← Salir(2, offset 10)
```

Verificación: `./gradlew -q test --tests 'com.educaflow.views.*'` tras el Paso 11 (con el glosario, `VAR-7.2` y `VAR-7.3` ya ampliados).

## Trazabilidad Origen spec → V/R/U → ubicación (filas de esta tarea)

### U

| ID | Origen spec | Ubicación |
|---|---|---|
| U-notificaciones-recibidas-001 | RUI-notificaciones-recibidas-formulario-correo-001 | `Mis@Correo-form` `enCopia` `showIf="enCopia"` |
| U-notificaciones-recibidas-002 | RUI-notificaciones-recibidas-formulario-correo-002 | `Mis@Correo-form` panel-related `adjuntos` `showIf="adjuntos && adjuntos.length > 0"` (equivale al «al cargar»: el form es de solo lectura y la colección no cambia) |
