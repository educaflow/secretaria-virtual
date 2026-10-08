---
type: implementation-task
template: system
---

# Tarea 10 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-code-quality

## Ficheros

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `base/util/MetaFileUtil.java` | Modificar | k-code-quality | + `fileSizeOnDisk(MetaFile)` (lo usan adjunto y correo) |

Para una clase Java con `Acción: Modificar` se declaran solo las firmas nuevas o cambiadas; **el resto de la clase se conserva**.

## Diseño — Paso 4 (extracto)

### Paso 4 — Canal correo y adjunto

**`MetaFileUtil`** (Modificar, el resto se conserva): `public static long fileSizeOnDisk(MetaFile metaFile)` — `Files.size(MetaFiles.getPath(metaFile))`, `IOException` → `UncheckedIOException`.

Verificación (al cerrar el bloque 1–9): compila.
