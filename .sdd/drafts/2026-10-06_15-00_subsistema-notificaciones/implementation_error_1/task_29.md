---
type: implementation-task
template: system
---

# Tarea 29 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

## Vista Mis-Correo.xml

Las decisiones difíciles, con sus alternativas, están en [`decisiones.md`](decisiones.md) (D1–D6); este documento las cita por su número.

## Ficheros a crear o modificar (extracto del diseño)

Rutas relativas a `src/main/java/com/educaflow/` salvo que empiecen por `src/`, `agent_docs/` o `.claude/`.

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/notificaciones/views/Mis-Correo.xml` | Crear | k-vistas (forms.md) | Form del correo en «Recibidas» |

> **Nota para `/sdd-implementer`:** los XML de `domains/`, `views/` y `menus.xml` ya están materializados en la carpeta `design/`. **MUST NOT** modificarlos, reescribirlos ni regenerarlos: se **copian verbatim** a su ubicación final (`menus.xml` se fusiona en el `menus.xml` único del proyecto). El código Java es lo único que se implementa a partir de las firmas y comentarios del diseño.

**Materialización:** el fichero ya está materializado y validado en `design/views/Mis-Correo.xml`. Se **copia literalmente** (`cp`) a `src/main/java/com/educaflow/subsystem/notificaciones/views/Mis-Correo.xml` (creando la carpeta con `mkdir -p` si no existe), **sin regenerarlo, reescribirlo ni reformatearlo** (`implementation.md` §1). Acción: `Crear`.

## Paso del diseño

### Paso 10 — Vistas

Copiar `design/views/*.xml` a `subsystem/notificaciones/views/` (las vistas antiguas se borraron en el Paso 1).

**`Mis-Correo.xml`** — form `Mis@Correo-form` de solo lectura; adjuntos (Ref) solo si hay alguno; Salir `close`.

```
  sssssssssfff   ← asunto(9) + fechaEnvio(3)
  bbbbbbbbbbbb   ← cuerpo(12, Text)
  ppppppeeeeee   ← para(6) + enCopia(6, showIf enCopia)   [con copia]
  pppppp······   ← enCopia oculto al borde derecho         [sin copia]
  Botones: ..........ss   ← Salir(2, offset 10)
```

Verificación: `./gradlew -q test --tests 'com.educaflow.views.*'` tras el Paso 11 (con `VAR-7.2` ya ampliada).

## Trazabilidad Origen spec → V/R/U → ubicación

### U

| ID | Origen spec | Ubicación |
|---|---|---|
| U-notificaciones-recibidas-001 | RUI-notificaciones-recibidas-formulario-correo-001 | `Mis@Correo-form` `enCopia` `showIf="enCopia"` |
| U-notificaciones-recibidas-002 | RUI-notificaciones-recibidas-formulario-correo-002 | `Mis@Correo-form` panel-related `adjuntos` `showIf="adjuntos && adjuntos.length > 0"` (equivale al «al cargar»: el form es de solo lectura y la colección no cambia) |
