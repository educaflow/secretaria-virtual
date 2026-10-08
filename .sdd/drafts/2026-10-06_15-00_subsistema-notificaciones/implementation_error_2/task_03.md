---
type: implementation-task
template: system
---

# Tarea 03 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas

El fichero XML **ya está materializado** en `design/domains/Correo.xml` (validado con `xmllint` por el diseñador). **MUST** copiarse **literalmente** a su ruta destino `src/main/java/com/educaflow/subsystem/notificaciones/domains/Correo.xml`, **sin regenerarlo, reescribirlo ni reformatearlo** (ver `implementation.md` §1 y §3). Acción `Crear`: si el destino ya existe, se reporta `CONFLICT` según `implementation.md` §3.
El borrado de `subsystem/correos/` y `subsystem/sms/` que menciona el Paso 1 lo hace la Tarea 01, no esta.

### Ficheros a crear o modificar (fila de esta tarea)

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/notificaciones/domains/Correo.xml` | Crear | k-sistemas (modelos.md) | `Correo extends Notificacion` |

### Paso 1 — Dominios (y retirada de los dominios antiguos)

Copiar `design/domains/{Notificacion,Correo,Sms,Adjunto}.xml` a `subsystem/notificaciones/domains/` y **borrar** en el mismo paso los árboles completos `subsystem/correos/` y `subsystem/sms/` (si conviven, Axelor ve dos entidades `Correo` y dos `Adjunto`; todo lo que contienen lo sustituyen los Pasos 3–13).
Desde aquí el proyecto no compila hasta terminar el Paso 9 (las referencias externas se arreglan en los Pasos 7 y 12): los Pasos 1–9 se verifican juntos con `./gradlew -q compileJava` al final del 9.

Resumen estructural:

- **`Correo.xml`** — `Correo extends Notificacion`: `para`, `enCopia`, `enCopiaOculta`, `asunto`, `cuerpo` (large, multiline), `adjuntos` (o2m `Adjunto` mappedBy `correo`); finder `findByEstado` (con el tipo explícito en `using`, porque `estado` es heredado, `all="true"`); `extra-code-model` con `computeDestino()` → `getPara()` (CC-Correo-001).

Verificación: la generación de código de Axelor (parte de `./gradlew -q compileJava`) produce `build/src-gen/…/notificaciones/db/{Notificacion,Correo,Sms,Adjunto,TipoNotificacion,EstadoNotificacion}.java` y `db/repo/{Notificacion,Correo,Sms,Adjunto}Repository.java`, con `computeDestino()` sobrescrito en `Correo` y `Sms`; `grep -rl "subsystem\.correos\|subsystem\.sms" src/main/java` solo devuelve `tramites/util/verificacion/VerificacionHelper.java` (Paso 7).

## Trazabilidad Origen spec → V/R/U → ubicación (filas de esta tarea)

### R

| ID | Origen spec | Ubicación | Momento |
|---|---|---|---|
| CC destino | CC-Notificacion-002, CC-Correo-001, CC-Sms-001 | Campo función `destino` de `Notificacion` + `computeDestino()` en `Correo.xml`/`Sms.xml` | Lectura (se guarda al hacer flush) |
