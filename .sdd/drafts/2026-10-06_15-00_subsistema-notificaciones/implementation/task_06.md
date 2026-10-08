---
type: implementation-task
template: system
---

# Tarea 06 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-code-quality

## MetaFileUtil.fileSizeOnDisk

## Ficheros a crear o modificar

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `base/util/MetaFileUtil.java` | Modificar | k-code-quality | + `fileSizeOnDisk(MetaFile)` (lo usan adjunto y correo) |

Para una clase Java con `Acción: Modificar` se declaran solo las firmas nuevas o cambiadas; **el resto de la clase se conserva**.

### Paso 4 — Canal correo y adjunto (extracto)

**`MetaFileUtil`** (Modificar, el resto se conserva): `public static long fileSizeOnDisk(MetaFile metaFile)` — `Files.size(MetaFiles.getPath(metaFile))`, `IOException` → `UncheckedIOException`.
