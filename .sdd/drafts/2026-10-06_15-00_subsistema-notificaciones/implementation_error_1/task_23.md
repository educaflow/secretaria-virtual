---
type: implementation-task
template: system
---

# Tarea 23 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

## Vista Main-Notificacion.xml

Las decisiones difíciles, con sus alternativas, están en [`decisiones.md`](decisiones.md) (D1–D6); este documento las cita por su número.

## Ficheros a crear o modificar (extracto del diseño)

Rutas relativas a `src/main/java/com/educaflow/` salvo que empiecen por `src/`, `agent_docs/` o `.claude/`.

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/notificaciones/views/Main-Notificacion.xml` | Crear | k-vistas (grids.md, actions.md) | «Todas» |

> **Nota para `/sdd-implementer`:** los XML de `domains/`, `views/` y `menus.xml` ya están materializados en la carpeta `design/`. **MUST NOT** modificarlos, reescribirlos ni regenerarlos: se **copian verbatim** a su ubicación final (`menus.xml` se fusiona en el `menus.xml` único del proyecto). El código Java es lo único que se implementa a partir de las firmas y comentarios del diseño.

**Materialización:** el fichero ya está materializado y validado en `design/views/Main-Notificacion.xml`. Se **copia literalmente** (`cp`) a `src/main/java/com/educaflow/subsystem/notificaciones/views/Main-Notificacion.xml` (creando la carpeta con `mkdir -p` si no existe), **sin regenerarlo, reescribirlo ni reformatearlo** (`implementation.md` §1). Acción: `Crear`.

## Paso del diseño

### Paso 10 — Vistas

Copiar `design/views/*.xml` a `subsystem/notificaciones/views/` (las vistas antiguas se borraron en el Paso 1).

**`Main-Notificacion.xml`** — `action-view` «Todas las notificaciones» (solo grid). Grid `Main@Notificacion-grid`: columnas estado, tipo, motivo, DNI, nombre, apellidos, destino, centro, expediente, fecha de creación, fecha de envío; `orderBy="-fechaCreacion"`; `action` = `Remote-abrirTodas` (sin `canEditOnClick`/`canViewOnClick`, VAR-8.1); barra con `btnNuevaNotificacion` → grupo que abre `Eleccion@Notificacion-action`.

Verificación: `./gradlew -q test --tests 'com.educaflow.views.*'` tras el Paso 11 (con `VAR-7.2` ya ampliada).
