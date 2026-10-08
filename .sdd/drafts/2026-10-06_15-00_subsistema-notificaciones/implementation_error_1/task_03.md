---
type: implementation-task
template: system
---

# Tarea 03 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas

## Dominio Sms

Las decisiones difíciles, con sus alternativas, están en [`decisiones.md`](decisiones.md) (D1–D6); este documento las cita por su número.

## Ficheros a crear o modificar (extracto del diseño)

Rutas relativas a `src/main/java/com/educaflow/` salvo que empiecen por `src/`, `agent_docs/` o `.claude/`.

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/notificaciones/domains/Sms.xml` | Crear | k-sistemas (modelos.md) | `Sms extends Notificacion` |

> **Nota para `/sdd-implementer`:** los XML de `domains/`, `views/` y `menus.xml` ya están materializados en la carpeta `design/`. **MUST NOT** modificarlos, reescribirlos ni regenerarlos: se **copian verbatim** a su ubicación final (`menus.xml` se fusiona en el `menus.xml` único del proyecto). El código Java es lo único que se implementa a partir de las firmas y comentarios del diseño.

**Materialización:** el fichero ya está materializado y validado en `design/domains/Sms.xml`. Se **copia literalmente** (`cp`) a `src/main/java/com/educaflow/subsystem/notificaciones/domains/Sms.xml` (creando la carpeta con `mkdir -p` si no existe), **sin regenerarlo, reescribirlo ni reformatearlo** (`implementation.md` §1). Acción: `Crear`.

## Paso del diseño

### Paso 1 — Dominios (y retirada de los dominios antiguos)

Copiar `design/domains/{Notificacion,Correo,Sms,Adjunto}.xml` a `subsystem/notificaciones/domains/` y **borrar** en el mismo paso los árboles completos `subsystem/correos/` y `subsystem/sms/` (si conviven, Axelor ve dos entidades `Correo` y dos `Adjunto`; todo lo que contienen lo sustituyen los Pasos 3–13).
En el mismo paso se borran sus tests unitarios (`src/test/java/com/educaflow/subsystem/correos/` y `…/sms/`; los nuevos los describe `test-unit-desc.md`) y sus E2E supersedidos (`src/test/e2e/subsystem/correos/` y `…/sms/`, ver `## Tests E2E supersedidos`).
Desde aquí el proyecto no compila hasta terminar el Paso 9 (las referencias externas se arreglan en los Pasos 7 y 12): los Pasos 1–9 se verifican juntos con `./gradlew -q compileJava` al final del 9.

Resumen estructural:

- **`Notificacion.xml`** — `module notificaciones` (`com.educaflow.subsystem.notificaciones.db`). Entidad `Notificacion` con `strategy="JOINED"` (D2).
  Campos: `tipoNotificacion` (enum, servidor), `name` («Motivo», cliente), `dniDestinatario`, `nombre`, `apellidos` (cliente), `destino` (campo función: `computeDestino()` que la base rechaza con `IllegalStateException` y cada subclase sobrescribe), `centro` (m2o `Centro`, cliente), `historialEstado` (m2o `HistorialEstado`, «Estado del expediente», cliente), `nombreExpediente` (`formula`, «Expediente»), `estado`, `fechaCreacion`, `fechaPrimerIntentoEnvio`, `fechaUltimoIntentoEnvio`, `fechaEnvio`, `numeroReintentos`, `descripcionUltimoFallo` (servidor).
  Enum `TipoNotificacion` (`CORREO` «Correo», `SMS` «SMS__!!») con `extra-code-model`: `getClaseNotificacion()` (`switch` exhaustivo) y `static deClase(Class<?>)` (lanza `IllegalStateException` si la clase no es de ningún canal) — el **único** sitio que sabe qué clase es cada canal (D2).
  Enum `EstadoNotificacion` (`PENDIENTE` «Pendiente», `ENVIADO` «Enviado», `FALLIDO` «Fallido»).
  `required="true"` solo en los campos de las RES (k-validaciones/restricciones.md §1): `name` (RES-Notificacion-001), `dniDestinatario` (RES-Notificacion-014) y `centro` (RES-Notificacion-013); `validateInsert` conserva sus comprobaciones solo para dar el mensaje personalizado. Los VAL- (nombre, apellidos, para, asunto, cuerpo, teléfono, mensaje) no llevan `required` en el dominio, y ningún campo lleva `max` declarativo.
- **`Correo.xml`** — `Correo extends Notificacion`: `para`, `enCopia`, `enCopiaOculta`, `asunto`, `cuerpo` (large, multiline), `adjuntos` (o2m `Adjunto` mappedBy `correo`); finder `findByEstado` (con el tipo explícito en `using`, porque `estado` es heredado, `all="true"`); `extra-code-model` con `computeDestino()` → `getPara()` (CC-Correo-001).
- **`Sms.xml`** — `Sms extends Notificacion`: `telefono`, `mensaje` (multiline); `extra-code-model` con `computeDestino()` → `getTelefono()` (CC-Sms-001).
- **`Adjunto.xml`** — `nombreFichero`, `contenido` (m2o `MetaFile`), `correo` (m2o `Correo`), los tres `required="true"` (RES-Adjunto-002/003/001), y `unique-constraint correo,nombreFichero`.

Verificación: la generación de código de Axelor (parte de `./gradlew -q compileJava`) produce `build/src-gen/…/notificaciones/db/{Notificacion,Correo,Sms,Adjunto,TipoNotificacion,EstadoNotificacion}.java` y `db/repo/{Notificacion,Correo,Sms,Adjunto}Repository.java`, con `computeDestino()` sobrescrito en `Correo` y `Sms`; `grep -rl "subsystem\.correos\|subsystem\.sms" src/main/java` solo devuelve `tramites/util/verificacion/VerificacionHelper.java` (Paso 7).

> **Nota del descomponedor:** el borrado de `subsystem/correos/**`, `subsystem/sms/**`, sus tests y sus E2E que el Paso 1 pide «en el mismo paso» lo hacen las tareas 05–08, inmediatamente después de los cuatro dominios. Esta tarea solo copia el XML.

## Trazabilidad Origen spec → V/R/U → ubicación

### R

| ID | Origen spec | Ubicación | Momento |
|---|---|---|---|
| CC destino | CC-Notificacion-002, CC-Correo-001, CC-Sms-001 | Campo función `destino` de `Notificacion` + `computeDestino()` en `Correo.xml`/`Sms.xml` | Lectura (se guarda al hacer flush) |
