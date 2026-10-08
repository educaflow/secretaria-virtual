---
type: implementation-task
template: system
---

# Tarea 26 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

El fichero XML **ya está materializado** en `design/views/Centro-Correo.xml` (validado con `xmllint` por el diseñador). **MUST** copiarse **literalmente** a su ruta destino `src/main/java/com/educaflow/subsystem/notificaciones/views/Centro-Correo.xml`, **sin regenerarlo, reescribirlo ni reformatearlo** (ver `implementation.md` §1 y §3). Acción `Crear`: si el destino ya existe, se reporta `CONFLICT` según `implementation.md` §3.

### Ficheros a crear o modificar (fila de esta tarea)

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/notificaciones/views/Centro-Correo.xml` | Crear | k-vistas (forms.md) | Form del correo en «Del centro» |

> **Nota para `/sdd-implementer`:** los XML de `domains/`, `views/` y `menus.xml` ya están materializados en la carpeta `design/`. **MUST NOT** modificarlos, reescribirlos ni regenerarlos: se **copian verbatim** a su ubicación final (`menus.xml` se fusiona en el `menus.xml` único del proyecto; `views/DefaultModelController.xml` va a `base/infrastructure/controller/DefaultModelController.xml`, Paso 8). El código Java es lo único que se implementa a partir de las firmas y comentarios del diseño.

### Paso 10 — Vistas

Copiar `design/views/*.xml` a `subsystem/notificaciones/views/` (las vistas antiguas se borraron en el Paso 1), **salvo** `design/views/DefaultModelController.xml`, que no es del subsistema y se copia a `base/infrastructure/controller/` (Paso 8).

**`Centro-Correo.xml`** — form `Centro@Correo-form` de solo lectura; `Centro@Correo-btnReenviar-action` contiene una sola acción, el grupo `Main@Correo-btnReenviar-action` (la secuencia del reenvío, con su `remote-refreshTab-action`, vive solo en `Main@Correo`); Salir `close`.

```
Datos del correo: igual que Main, con historialEstado(8) al borde derecho y showIf="historialEstado != null"
  cccchhhhhhhh   [ligada a un estado]     cccc········   [sin estado: hueco al borde derecho, nada se desplaza]
Datos del envío:
  eeeeeerrrccc   ← estado(6) + numeroReintentos(3) + fechaCreacion(3)
  panel «Intentos» (showIf estado != PENDIENTE; grupo condicional → panel propio):
    pppuuu...fff [ENVIADO]    pppuuu...... [FALLIDO]    (panel oculto) [PENDIENTE]
  dddddddddddd   ← descripcionUltimoFallo(12) [solo FALLIDO]
Botones:  rr........ss   ← Reenviar(2, showIf FALLIDO, primero y sin offset) + Salir(2, offset 8)  [FALLIDO]
          ..........ss   ← (Reenviar oculto al borde izquierdo) + Salir                          [resto]
```

Verificación: `./gradlew -q test --tests 'com.educaflow.views.*'` tras el Paso 11 (con el glosario, `VAR-7.2` y `VAR-7.3` ya ampliados).

## Trazabilidad Origen spec → V/R/U → ubicación (filas de esta tarea)

### U

| ID | Origen spec | Ubicación |
|---|---|---|
| U-notificaciones-centro-001 | RUI-notificaciones-centro-listado-001 | `Centro@Notificacion-grid` `<hilite color="danger" if="estado == 'FALLIDO'">` |
| U-notificaciones-centro-002 | RUI-notificaciones-centro-formulario-correo-001 | `Centro@Correo-form` `fechaEnvio` `showIf="estado == 'ENVIADO'"` |
| U-notificaciones-centro-003 | RUI-notificaciones-centro-formulario-correo-002 | `Centro@Correo-form` `descripcionUltimoFallo` `showIf="estado == 'FALLIDO'"` |
| U-notificaciones-centro-004 | RUI-notificaciones-centro-formulario-correo-003 | `Centro@Correo-form` `btnReenviar` `showIf="estado == 'FALLIDO'"` |
| U-notificaciones-centro-005 | RUI-notificaciones-centro-formulario-correo-004 | `CorreoController.reenviar` → `setNotify` |
| U-notificaciones-centro-006 | RUI-notificaciones-centro-formulario-correo-005 | `Centro@Correo-form` panel `Intentos` `showIf="estado != 'PENDIENTE'"` |
| U-notificaciones-centro-007 | RUI-notificaciones-centro-formulario-correo-006 | `Centro@Correo-form` `historialEstado` `showIf="historialEstado != null"` |
