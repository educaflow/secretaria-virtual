---
type: implementation-task
template: system
---

# Tarea 28 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

## Vista Centro-Correo.xml

Las decisiones difíciles, con sus alternativas, están en [`decisiones.md`](decisiones.md) (D1–D6); este documento las cita por su número.

## Ficheros a crear o modificar (extracto del diseño)

Rutas relativas a `src/main/java/com/educaflow/` salvo que empiecen por `src/`, `agent_docs/` o `.claude/`.

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/notificaciones/views/Centro-Correo.xml` | Crear | k-vistas (forms.md) | Form del correo en «Del centro» |

> **Nota para `/sdd-implementer`:** los XML de `domains/`, `views/` y `menus.xml` ya están materializados en la carpeta `design/`. **MUST NOT** modificarlos, reescribirlos ni regenerarlos: se **copian verbatim** a su ubicación final (`menus.xml` se fusiona en el `menus.xml` único del proyecto). El código Java es lo único que se implementa a partir de las firmas y comentarios del diseño.

**Materialización:** el fichero ya está materializado y validado en `design/views/Centro-Correo.xml`. Se **copia literalmente** (`cp`) a `src/main/java/com/educaflow/subsystem/notificaciones/views/Centro-Correo.xml` (creando la carpeta con `mkdir -p` si no existe), **sin regenerarlo, reescribirlo ni reformatearlo** (`implementation.md` §1). Acción: `Crear`.

## Paso del diseño

### Paso 10 — Vistas

Copiar `design/views/*.xml` a `subsystem/notificaciones/views/` (las vistas antiguas se borraron en el Paso 1).

**`Centro-Correo.xml`** — form `Centro@Correo-form` de solo lectura; Reenviar reutiliza `Main@Correo-Remote-validateReenviar`/`-reenviar`; Salir `close`.

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

Verificación: `./gradlew -q test --tests 'com.educaflow.views.*'` tras el Paso 11 (con `VAR-7.2` ya ampliada).

## Trazabilidad Origen spec → V/R/U → ubicación

### U

| ID | Origen spec | Ubicación |
|---|---|---|
| U-notificaciones-centro-002 | RUI-notificaciones-centro-formulario-correo-001 | `Centro@Correo-form` `fechaEnvio` `showIf="estado == 'ENVIADO'"` |
| U-notificaciones-centro-003 | RUI-notificaciones-centro-formulario-correo-002 | `Centro@Correo-form` `descripcionUltimoFallo` `showIf="estado == 'FALLIDO'"` |
| U-notificaciones-centro-004 | RUI-notificaciones-centro-formulario-correo-003 | `Centro@Correo-form` `btnReenviar` `showIf="estado == 'FALLIDO'"` |
| U-notificaciones-centro-006 | RUI-notificaciones-centro-formulario-correo-005 | `Centro@Correo-form` panel `Intentos` `showIf="estado != 'PENDIENTE'"` |
| U-notificaciones-centro-007 | RUI-notificaciones-centro-formulario-correo-006 | `Centro@Correo-form` `historialEstado` `showIf="historialEstado != null"` |
