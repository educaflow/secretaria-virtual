---
type: implementation-task
template: system
---

# Tarea 10 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-code-quality

## MetaFileUtil.fileSizeOnDisk

### Fila de la tabla «Ficheros a crear o modificar»

Rutas relativas a `src/main/java/com/educaflow/` salvo que empiecen por `src/`, `agent_docs/` o `.claude/`.

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `base/util/MetaFileUtil.java` | Modificar | k-code-quality | + `fileSizeOnDisk(MetaFile)` (lo usan adjunto y correo) |

Para una clase Java con `Acción: Modificar` se declaran solo las firmas nuevas o cambiadas; **el resto de la clase se conserva**.

### Paso 4 (extracto verbatim del design.md: `MetaFileUtil`)

### Paso 4 — Canal correo y adjunto

**`MetaFileUtil`** (Modificar, el resto se conserva): `public static long fileSizeOnDisk(MetaFile metaFile)` — `Files.size(MetaFiles.getPath(metaFile))`, `IOException` → `UncheckedIOException`.

Verificación (al cerrar el bloque 1–9): compila.
