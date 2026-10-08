---
type: implementation-task
template: system
---

# Tarea 05 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas

## Dominio Adjunto

## Ficheros a crear o modificar

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/notificaciones/domains/Adjunto.xml` | Crear | k-sistemas (modelos.md) | `Adjunto` (de un `Correo`) |

**Instrucción de materialización (XML ya materializado):** el fichero ya está materializado en `design/domains/Adjunto.xml` (carpeta de la iniciativa). **MUST** copiarse literalmente a `src/main/java/com/educaflow/subsystem/notificaciones/domains/Adjunto.xml`, sin regenerarlo, reescribirlo ni reformatearlo (`implementation.md` §1). La fila es `Acción: Crear`: si el destino ya existe, es `CONFLICT` (`implementation.md` §3).

> **Nota para `/sdd-implementer`:** los XML de `domains/`, `views/` y `menus.xml` ya están materializados en la carpeta `design/`. **MUST NOT** modificarlos, reescribirlos ni regenerarlos: se **copian verbatim** a su ubicación final (`menus.xml` se fusiona en el `menus.xml` único del proyecto; `views/DefaultModelController.xml` va a `base/infrastructure/controller/DefaultModelController.xml`, Paso 8). El código Java es lo único que se implementa a partir de las firmas y comentarios del diseño.

### Paso 1 — Dominios (y retirada de los dominios antiguos)

Copiar `design/domains/{Notificacion,Correo,Sms,Adjunto}.xml` a `subsystem/notificaciones/domains/` y **borrar** en el mismo paso los árboles completos `subsystem/correos/` y `subsystem/sms/` (si conviven, Axelor ve dos entidades `Correo` y dos `Adjunto`; todo lo que contienen lo sustituyen los Pasos 3–13).
En el mismo paso se borran sus tests unitarios (`src/test/java/com/educaflow/subsystem/correos/` y `…/sms/`; los nuevos los describe `test-unit-desc.md`) y sus E2E supersedidos (`src/test/e2e/subsystem/correos/` y `…/sms/`, ver `## Tests E2E supersedidos`).
Desde aquí el proyecto no compila hasta terminar el Paso 9 (las referencias externas se arreglan en los Pasos 7 y 12): los Pasos 1–9 se verifican juntos con `./gradlew -q compileJava` al final del 9.

Resumen estructural:

- **`Adjunto.xml`** — `nombreFichero`, `contenido` (m2o `MetaFile`), `correo` (m2o `Correo`), los tres `required="true"` (RES-Adjunto-002/003/001), y `unique-constraint correo,nombreFichero`.

Verificación: la generación de código de Axelor (parte de `./gradlew -q compileJava`) produce `build/src-gen/…/notificaciones/db/{Notificacion,Correo,Sms,Adjunto,TipoNotificacion,EstadoNotificacion}.java` y `db/repo/{Notificacion,Correo,Sms,Adjunto}Repository.java`, con `computeDestino()` sobrescrito en `Correo` y `Sms`; `grep -rl "subsystem\.correos\|subsystem\.sms" src/main/java` solo devuelve `tramites/util/verificacion/VerificacionHelper.java` (Paso 7).

## Trazabilidad Origen spec → V/R/U → ubicación (filas de esta tarea)

### V

| ID | Origen spec | Ubicación |
|---|---|---|
| V-Adjunto-013 | RES-Correo-002 | `AdjuntoServiceImpl.validateInsert` + `unique-constraint correo,nombreFichero` |

### R

| ID | Origen spec | Ubicación | Momento |
|---|---|---|---|
| R-Adjunto-001 | RN-Adjunto-002 | Estructural: los adjuntos se guardan en cascada con el único `repository.save(correo)` de `insert`, en la misma transacción | Antes de commit |
