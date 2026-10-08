---
type: implementation-task
template: system
---

# Tarea 05 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas

## Retirada del subsistema correos

Las decisiones difíciles, con sus alternativas, están en [`decisiones.md`](decisiones.md) (D1–D6); este documento las cita por su número.

## Ficheros a crear o modificar (extracto del diseño)

Rutas relativas a `src/main/java/com/educaflow/` salvo que empiecen por `src/`, `agent_docs/` o `.claude/`.

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/correos/**` | Eliminar | — | Subsistema retirado (todo su árbol) |

**Materialización:** borrar el árbol completo `src/main/java/com/educaflow/subsystem/correos/` (dominios, servicios, controlador, módulo y providers, vistas y data-init). No se crea nada.

## Paso del diseño

### Paso 1 — Dominios (y retirada de los dominios antiguos)

Copiar `design/domains/{Notificacion,Correo,Sms,Adjunto}.xml` a `subsystem/notificaciones/domains/` y **borrar** en el mismo paso los árboles completos `subsystem/correos/` y `subsystem/sms/` (si conviven, Axelor ve dos entidades `Correo` y dos `Adjunto`; todo lo que contienen lo sustituyen los Pasos 3–13).
En el mismo paso se borran sus tests unitarios (`src/test/java/com/educaflow/subsystem/correos/` y `…/sms/`; los nuevos los describe `test-unit-desc.md`) y sus E2E supersedidos (`src/test/e2e/subsystem/correos/` y `…/sms/`, ver `## Tests E2E supersedidos`).
Desde aquí el proyecto no compila hasta terminar el Paso 9 (las referencias externas se arreglan en los Pasos 7 y 12): los Pasos 1–9 se verifican juntos con `./gradlew -q compileJava` al final del 9.

## Eliminaciones declaradas

| Fichero | Elemento eliminado | Justificación (ID de spec) |
|---|---|---|
| `subsystem/correos/**` | Subsistema completo (dominios `Correo`, `Adjunto`, enum `EstadoCorreo`, servicios, controlador, módulo, vistas, data-init) | `specification.md` § Objetivo («sustituye a los dos subsistemas actuales… que desaparecen») |

## Notas y supuestos

- **Frozen store de ArchUnit**: al desaparecer `correos.service.CorreoService.enviarCorreo`, la violación congelada de C23 desaparece sola.

> **Nota del descomponedor:** la columna Skill del diseño indica «—»; se asigna `k-sistemas` porque describe la estructura de carpetas de un subsistema. Las tareas posteriores que «conservan» o «mueven» código de este árbol lo leen con `git show HEAD:<ruta>`.
