---
type: implementation-task
template: system
---

# Tarea 31 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

## Vista Centro-Sms.xml

Las decisiones difíciles, con sus alternativas, están en [`decisiones.md`](decisiones.md) (D1–D6); este documento las cita por su número.

## Ficheros a crear o modificar (extracto del diseño)

Rutas relativas a `src/main/java/com/educaflow/` salvo que empiecen por `src/`, `agent_docs/` o `.claude/`.

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/notificaciones/views/Centro-Sms.xml` | Crear | k-vistas (forms.md) | Form del SMS en «Del centro» |

> **Nota para `/sdd-implementer`:** los XML de `domains/`, `views/` y `menus.xml` ya están materializados en la carpeta `design/`. **MUST NOT** modificarlos, reescribirlos ni regenerarlos: se **copian verbatim** a su ubicación final (`menus.xml` se fusiona en el `menus.xml` único del proyecto). El código Java es lo único que se implementa a partir de las firmas y comentarios del diseño.

**Materialización:** el fichero ya está materializado y validado en `design/views/Centro-Sms.xml`. Se **copia literalmente** (`cp`) a `src/main/java/com/educaflow/subsystem/notificaciones/views/Centro-Sms.xml` (creando la carpeta con `mkdir -p` si no existe), **sin regenerarlo, reescribirlo ni reformatearlo** (`implementation.md` §1). Acción: `Crear`.

## Paso del diseño

### Paso 10 — Vistas

Copiar `design/views/*.xml` a `subsystem/notificaciones/views/` (las vistas antiguas se borraron en el Paso 1).

> Contexto: el bloque de la vista de correo al que el diseño remite («análogo a» / «idénticos a»):

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

**`Centro-Sms.xml`** — form `Centro@Sms-form` de solo lectura; Reenviar reutiliza `Main@Sms-Local-confirmarReenvio` y las remotas de `Main@Sms`; Salir `close`.

```
Datos del SMS (readonly; historialEstado(8) al borde derecho con showIf="historialEstado != null"):
  tttnnnnnnnnn   ← tipoNotificacion(3) + name/motivo(9)
  cccchhhhhhhh   ← centro(4) + historialEstado(8)                          [ligada a un estado]
  cccc········   ← historialEstado oculto: hueco al borde derecho, nada se desplaza  [sin estado]
  dddooooaaaaa   ← dniDestinatario(3) + nombre(4) + apellidos(5)
  tttt········   ← telefono(4, phone ES); el colOffset(8) de mensaje completa la fila
  mmmmmmmmmmmm   ← mensaje(12, Text)
Datos del envío y botones: idénticos a Centro-Correo.xml
```

Verificación: `./gradlew -q test --tests 'com.educaflow.views.*'` tras el Paso 11 (con `VAR-7.2` ya ampliada).

## Trazabilidad Origen spec → V/R/U → ubicación

### U

| ID | Origen spec | Ubicación |
|---|---|---|
| U-notificaciones-centro-008 | RUI-notificaciones-centro-formulario-sms-001 | `Centro@Sms-form` `fechaEnvio` `showIf` ENVIADO |
| U-notificaciones-centro-009 | RUI-notificaciones-centro-formulario-sms-002 | `Centro@Sms-form` `descripcionUltimoFallo` `showIf` FALLIDO |
| U-notificaciones-centro-010 | RUI-notificaciones-centro-formulario-sms-003 | `Centro@Sms-form` `btnReenviar` `showIf` FALLIDO |
| U-notificaciones-centro-012 | RUI-notificaciones-centro-formulario-sms-005 | `Main@Sms-Local-confirmarReenvio-action` (alert) en `Centro@Sms-btnReenviar-action` |
| U-notificaciones-centro-013 | RUI-notificaciones-centro-formulario-sms-006 | `Centro@Sms-form` panel `Intentos` `showIf="estado != 'PENDIENTE'"` |
| U-notificaciones-centro-014 | RUI-notificaciones-centro-formulario-sms-007 | `Centro@Sms-form` `historialEstado` `showIf="historialEstado != null"` |
