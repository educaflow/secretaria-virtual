---
type: implementation-task
template: system
---

# Tarea 03 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- Ninguno (la tabla del diseño no asigna skill a este fichero)

## Ficheros

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/test/java/com/educaflow/subsystem/correos/**`, `src/test/java/com/educaflow/subsystem/sms/**` | Eliminar | — | Sustituidos por los tests de `notificaciones` (`test-unit-desc.md`) |

**Materialización (eliminación):** la fila es `Acción: Eliminar`. El contrato de materialización no define este caso; decisión del descomponedor: borrar el árbol indicado con `git rm -r` (o `rm -r` si no está versionado). Si el árbol ya no existe, la tarea se da por hecha. **MUST NOT** borrar nada fuera de las rutas de la fila.

## Diseño — Paso 1

### Paso 1 — Dominios (y retirada de los dominios antiguos)

Copiar `design/domains/{Notificacion,Correo,Sms,Adjunto}.xml` a `subsystem/notificaciones/domains/` y **borrar** en el mismo paso los árboles completos `subsystem/correos/` y `subsystem/sms/` (si conviven, Axelor ve dos entidades `Correo` y dos `Adjunto`; todo lo que contienen lo sustituyen los Pasos 3–13).
En el mismo paso se borran sus tests unitarios (`src/test/java/com/educaflow/subsystem/correos/` y `…/sms/`; los nuevos los describe `test-unit-desc.md`) y sus E2E supersedidos (`src/test/e2e/subsystem/correos/` y `…/sms/`, ver `## Tests E2E supersedidos`).
Desde aquí el proyecto no compila hasta terminar el Paso 9 (las referencias externas se arreglan en los Pasos 7 y 12): los Pasos 1–9 se verifican juntos con `./gradlew -q compileJava` al final del 9.
