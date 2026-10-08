---
type: implementation-task
template: system
---

# Tarea 06 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- ninguno (la fila de la tabla no indica skill: tarea sin código Java ni XML de Axelor)

## Eliminar subsystem/sms

### Fila de la tabla «Ficheros a crear o modificar»

Rutas relativas a `src/main/java/com/educaflow/` salvo que empiecen por `src/`, `agent_docs/` o `.claude/`.

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/sms/**` | Eliminar | — | Subsistema retirado (todo su árbol) |

### Paso 1 (verbatim del design.md)

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

### Eliminaciones declaradas (filas que aplican)

## Eliminaciones declaradas

| Fichero | Elemento eliminado | Justificación (ID de spec) |
|---|---|---|
| `subsystem/sms/**` | Subsistema completo (dominio `Sms`, enum `EstadoSms`, servicio, controlador, módulo, vistas, data-init) | § Objetivo |
