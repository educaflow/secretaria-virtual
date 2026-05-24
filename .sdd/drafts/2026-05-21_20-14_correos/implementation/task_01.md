---
type: implementation-task
---

# Tarea 01 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas

El XML de este dominio YA está materializado y validado con `xmllint` por el diseñador en `.sdd/drafts/2026-05-21_20-14_correos/design/domains/Correo.xml`. **Cópialo literalmente** (sin regenerarlo ni reformatearlo) a su ruta destino `src/main/java/com/educaflow/subsystem/correos/domains/Correo.xml`. Si detectas que el XML está mal, DETENTE y notifica; no lo edites aquí.

Fila de la tabla "Ficheros a crear o modificar":

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/correos/domains/Correo.xml` | Crear | k-sistemas (modelos.md) | Entidad `Correo` + enum `EstadoCorreo` + finders. |

Descripción de diseño (Paso 1 — Dominios):

`design/domains/Correo.xml` — entidad `Correo` + enum `EstadoCorreo`.
- **Campos cliente** (`required="true"`): `asunto`, `cuerpo` (HTML, `large`), `dniDestinatario`, `emailDestinatario`.
- **Campos servidor** (SIN `required`, los rellena el servicio tras el persist — k-sistemas/modelos.md): `fechaCreacion`, `fechaEnvio`, `estado` (enum), `numeroIntentos`, `fechaUltimoIntento`, `motivoUltimoFallo`, `centro` (→ `Centro`), `referenciaHistorialEstadoExpediente` (→ `com.educaflow.subsystem.expedientes.db.HistorialEstado`, opcional).
- `one-to-many adjuntos` (`mappedBy="correo"`).
- Finders: `findByEstado`, `findByCentro`, `findByDniDestinatario`.
- Enum `EstadoCorreo`: PENDIENTE / ENVIADO / FALLIDO.

> `db/repo/` queda vacío (con `.gitkeep`): los `<finder-method>` del dominio bastan; **NO** se crea `CorreoRepository` personalizado y por tanto **NO** se pone `repository="abstract"` en `Correo` (regla k-sistemas).

Nota de unificación 1: **`referenciaHistorialEstadoExpediente` → `com.educaflow.subsystem.expedientes.db.HistorialEstado`.** El campo es opcional y solo asignable programáticamente (V-Correo-006 / E-UN-009).
