---
type: implementation-task
---

# Tarea 02 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas

El XML de este dominio YA está materializado y validado con `xmllint` por el diseñador en `.sdd/drafts/2026-05-21_20-14_correos/design/domains/AdjuntoCorreo.xml`. **Cópialo literalmente** (sin regenerarlo ni reformatearlo) a su ruta destino `src/main/java/com/educaflow/subsystem/correos/domains/AdjuntoCorreo.xml`. Si detectas que el XML está mal, DETENTE y notifica; no lo edites aquí.

Fila de la tabla "Ficheros a crear o modificar":

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/correos/domains/AdjuntoCorreo.xml` | Crear | k-sistemas (modelos.md) | Entidad `AdjuntoCorreo` (hija de `Correo`). |

Descripción de diseño (Paso 1 — Dominios):

`design/domains/AdjuntoCorreo.xml` — `nombreFichero` (cliente, required), `contenido` (`MetaFile`, cliente, required), `correo` (→ `Correo`, servidor, required; lo fija el `onNew __parent__` del modal hijo). Borrado en cascada **desde el padre** (R-Correo-008, materializado en `CorreoServiceImpl.remove`).

> **Decisión `contenido` = `MetaFile`** (no `<binary>`): descarga nativa con widget `binary`/`binary-link`, `fileName`/`fileType` ya gestionados, y construcción del `Attach` con `MetaFileUtil.downloadContent`. Es el patrón del proyecto (DocumentoFirma / RegistroSalida). La "copia inmutable" se garantiza por la inmutabilidad del `AdjuntoCorreo` (update prohibido).
