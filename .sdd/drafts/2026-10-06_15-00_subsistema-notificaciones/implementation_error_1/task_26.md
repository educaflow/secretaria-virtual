---
type: implementation-task
template: system
---

# Tarea 26 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

## Vista Eleccion-Notificacion.xml

Las decisiones difíciles, con sus alternativas, están en [`decisiones.md`](decisiones.md) (D1–D6); este documento las cita por su número.

## Ficheros a crear o modificar (extracto del diseño)

Rutas relativas a `src/main/java/com/educaflow/` salvo que empiecen por `src/`, `agent_docs/` o `.claude/`.

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/notificaciones/views/Eleccion-Notificacion.xml` | Crear | k-vistas (forms.md, actions.md) | Elección de canal (popup) |

> **Nota para `/sdd-implementer`:** los XML de `domains/`, `views/` y `menus.xml` ya están materializados en la carpeta `design/`. **MUST NOT** modificarlos, reescribirlos ni regenerarlos: se **copian verbatim** a su ubicación final (`menus.xml` se fusiona en el `menus.xml` único del proyecto). El código Java es lo único que se implementa a partir de las firmas y comentarios del diseño.

**Materialización:** el fichero ya está materializado y validado en `design/views/Eleccion-Notificacion.xml`. Se **copia literalmente** (`cp`) a `src/main/java/com/educaflow/subsystem/notificaciones/views/Eleccion-Notificacion.xml` (creando la carpeta con `mkdir -p` si no existe), **sin regenerarlo, reescribirlo ni reformatearlo** (`implementation.md` §1). Acción: `Crear`.

## Paso del diseño

### Paso 10 — Vistas

Copiar `design/views/*.xml` a `subsystem/notificaciones/views/` (las vistas antiguas se borraron en el Paso 1).

**`Eleccion-Notificacion.xml`** — `action-view` de form en popup (`popup`, `popup-save=false`, `show-toolbar=false`, `show-toolbar-form=false`, `forceEdit`). Form `Eleccion@Notificacion-form` (model `Notificacion`), panel «Canal» y botones Cancelar (`close`) y Continuar (`readonlyIf="tipoNotificacion == null"`, grupo `Remote-continuarAlta` → `close`).

```
Canal:    tttt········   ← tipoNotificacion(4, RadioSelect horizontal) — único campo, sin relacionados
Botones:  ········ccxx   ← Cancelar(2, offset 8) + Continuar(2)
```

Verificación: `./gradlew -q test --tests 'com.educaflow.views.*'` tras el Paso 11 (con `VAR-7.2` ya ampliada).

## Trazabilidad Origen spec → V/R/U → ubicación

### U

| ID | Origen spec | Ubicación |
|---|---|---|
| U-notificaciones-todas-001 | RUI-notificaciones-todas-eleccion-canal-001 | `Eleccion@Notificacion-form` `tipoNotificacion` sin valor por defecto (el dominio no declara `default`) |
| U-notificaciones-todas-002 | RUI-notificaciones-todas-eleccion-canal-002 | `Eleccion@Notificacion-form` `btnContinuar` `readonlyIf="tipoNotificacion == null"` |

## Notas y supuestos

- **Riesgos del front a confirmar en `/sdd-debug-with-test-e2e-desc`** (D3): que `popup="reload"` refresque la pestaña de «Todas» tras guardar un alta abierta desde la elección de canal (popup sobre popup). Si `continuarAlta` + `close` cerrara también el alta, la alternativa es que el controlador responda `setCanClose(true)` después del `setView`.
- **`destino` en la base** (D2): `Notificacion.computeDestino()` lanza `IllegalStateException`. El único sitio donde existe una `Notificacion` «genérica» en memoria es el form de elección de canal, y `NotificacionController.continuarAlta` lee `tipoNotificacion` del contexto sin construir el bean ni pedir su destino; si en la depuración apareciera algún camino del framework que serializa ese registro nuevo completo, se resuelve en ese camino, no relajando la base a `return null`.
