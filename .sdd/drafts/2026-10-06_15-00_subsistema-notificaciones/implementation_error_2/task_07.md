---
type: implementation-task
template: system
---

# Tarea 07 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-code-quality

Acción `Modificar`: la clase **ya existe**; se añade **solo** el método declarado y se conserva todo lo demás (ver `implementation.md` §2).

### Ficheros a crear o modificar (fila de esta tarea)

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `base/util/MetaFileUtil.java` | Modificar | k-code-quality | + `fileSizeOnDisk(MetaFile)` (lo usan adjunto y correo) |

Para una clase Java con `Acción: Modificar` se declaran solo las firmas nuevas o cambiadas; **el resto de la clase se conserva**.

### Paso 4 — Canal correo y adjunto

**`MetaFileUtil`** (Modificar, el resto se conserva): `public static long fileSizeOnDisk(MetaFile metaFile)` — `Files.size(MetaFiles.getPath(metaFile))`, `IOException` → `UncheckedIOException`.

Verificación (al cerrar el bloque 1–9): compila.

## Trazabilidad Origen spec → V/R/U → ubicación (filas de esta tarea)

### V

| ID | Origen spec | Ubicación |
|---|---|---|
| V-Correo-011 | VAL-Correo-019 | `CorreoServiceImpl.validateDatosDelCanal` |
| V-Adjunto-008 | VAL-Adjunto-003 | `AdjuntoServiceImpl.validateInsert` (+ límite de subida de la plataforma, D5) |
| V-Adjunto-012 | VAL-Adjunto-007 | `AdjuntoServiceImpl.validateInsert`; cliente: `Local-validateSave` del modal |
