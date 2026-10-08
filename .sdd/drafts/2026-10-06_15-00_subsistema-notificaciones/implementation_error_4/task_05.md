---
type: implementation-task
template: system
---

# Tarea 05 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas

## Ficheros

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/notificaciones/domains/Notificacion.xml` | Crear | k-sistemas (modelos.md) | Entidad base `Notificacion` (JOINED) + enums `TipoNotificacion` y `EstadoNotificacion` |

**Materialización (XML ya materializado):** el fichero está en `design/domains/Notificacion.xml` (dentro de la carpeta de la iniciativa) y se debe **copiar literalmente** a `src/main/java/com/educaflow/subsystem/notificaciones/domains/Notificacion.xml`, **sin regenerarlo** ni reformatearlo (contrato de materialización, `implementation.md` §1 y §3).

> **Nota para `/sdd-implementer`:** los XML de `domains/`, `views/` y `menus.xml` ya están materializados en la carpeta `design/`. **MUST NOT** modificarlos, reescribirlos ni regenerarlos: se **copian verbatim** a su ubicación final (`menus.xml` se fusiona en el `menus.xml` único del proyecto; `views/DefaultModelController.xml` va a `base/infrastructure/controller/DefaultModelController.xml`, Paso 8). El código Java es lo único que se implementa a partir de las firmas y comentarios del diseño.

## Diseño — Paso 1

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

Verificación: la generación de código de Axelor (parte de `./gradlew -q compileJava`) produce `build/src-gen/…/notificaciones/db/{Notificacion,Correo,Sms,Adjunto,TipoNotificacion,EstadoNotificacion}.java` y `db/repo/{Notificacion,Correo,Sms,Adjunto}Repository.java`, con `computeDestino()` sobrescrito en `Correo` y `Sms`; `grep -rl "subsystem\.correos\|subsystem\.sms" src/main/java` solo devuelve `tramites/util/verificacion/VerificacionHelper.java` (Paso 7).

## Trazabilidad Origen spec → V/R/U → ubicación (filas que aplican a esta tarea)

### R

| ID | Origen spec | Ubicación | Momento |
|---|---|---|---|
| CC destino | CC-Notificacion-002, CC-Correo-001, CC-Sms-001 | Campo función `destino` de `Notificacion` + `computeDestino()` en `Correo.xml`/`Sms.xml` | Lectura (se guarda al hacer flush) |
| CC expediente | CC-Notificacion-003 | `Notificacion.nombreExpediente` (`formula`) | Lectura |
