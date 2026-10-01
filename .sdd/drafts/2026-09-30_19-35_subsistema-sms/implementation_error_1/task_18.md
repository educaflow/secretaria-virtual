---
type: implementation-task
template: system
---

# Tarea 18 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas

### Fichero(s) de esta tarea (de «Ficheros a crear o modificar» de `design/design.md`)

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/subsystem/sms/domains/Sms.xml` | Crear | k-sistemas (modelos.md) | Entidad `Sms` + enum `EstadoSms` |

**XML ya materializado:** el fichero está en `design/domains/Sms.xml` y se debe **copiar literalmente** a `src/main/java/com/educaflow/subsystem/sms/domains/Sms.xml`, **sin regenerarlo** ni reformatearlo (`implementation.md` §1). La fila es `Acción: Crear`.

> **Nota para `/sdd-implementer`:** los XML de `domains/`, `views/` y `menus.xml` ya están materializados en la carpeta `design/`. **MUST NOT** modificarlos, reescribirlos ni regenerarlos: se **copian verbatim** a su ubicación final (`menus.xml` se fusiona en el `menus.xml` único del proyecto). El código Java es lo único que se implementa a partir de las firmas y comentarios de este diseño. Los `i18n_*.csv` **MUST NOT** crearse a mano.

### Paso 4 — Dominio `Sms` (de `design/design.md`)

**Fichero:** `subsystem/sms/domains/Sms.xml` (Crear) — XML completo en `design/domains/Sms.xml`, válido contra `domain-models.xsd`.

**Resumen estructural:** módulo `sms`, paquete `com.educaflow.subsystem.sms.db`.

- Datos del destinatario (`cliente`): `dniDestinatario`, `nombre`, `apellidos` (string, texto libre: el DNI no enlaza con ninguna ficha).
- `telefono` (string, `cliente`): el móvil, guardado en E.164 (RES-Sms-004).
- `mensaje` (string `multiline`, `cliente`): el texto. **Sin `max`**: el límite es de VAL-Sms-008, que depende de la codificación (160 unidades GSM-7 o 70 caracteres UCS-2); un `max` sería un segundo dueño de la misma decisión con un mensaje genérico de JPA.
- `centro` (many-to-one a `subsystem.common.db.Centro`, `cliente`) y `historialEstado` (many-to-one a `subsystem.expedientes.db.HistorialEstado`, `cliente`, opcional).
- Datos del envío, todos `servidor`: `estado` (enum `EstadoSms`), `fechaCreacion`, `fechaPrimerIntentoEnvio`, `fechaUltimoIntentoEnvio`, `fechaEnvio`, `numeroReintentos`, `descripcionUltimoFallo`.
- `nombreExpediente`: campo derivado `formula="true"` (CC-Sms-008, `momento: lectura`) con un subselect correlacionado sobre `expedientes_historial_estado` + `expedientes_expediente`; `null` si el SMS no tiene estado de expediente. No se persiste.
- Enum `EstadoSms`: `PENDIENTE` («Pendiente»), `ENVIADO` («Enviado»), `FALLIDO` («Fallido») — los nombres que fija `entity-Sms.md`.
- **Sin** `finder-method` y **sin** `repository="abstract"`: no hay ninguna consulta propia (el reenvío en bloque está fuera de alcance, así que no hay `findByEstado`).
- La dependencia va `subsystem/sms → subsystem/expedientes` (solo lectura de `HistorialEstado`), nunca al revés, igual que en correos.

**Verificar:** `./gradlew -q build` genera `build/src-gen/.../subsystem/sms/db/Sms.java` y `EstadoSms.java`; al arrancar, la tabla `sms_sms` existe con la columna `historial_estado`.

### Campos derivados de solo lectura (de «Trazabilidad», `design/design.md`)

| Campo | Origen spec | Ubicación |
|-------|-------------|-----------|
| `nombreExpediente` | CC-Sms-008 (`momento: lectura`) | `design/domains/Sms.xml`, `<string name="nombreExpediente" formula="true">` (subselect SQL; no se persiste) |
