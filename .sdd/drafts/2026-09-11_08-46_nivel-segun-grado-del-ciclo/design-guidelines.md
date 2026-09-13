---
type: design-guidelines
---

- El indicador «admite nivel» del grado (`CC-Grado-001`) debe resolverse como un campo del modelo de dominio **no persistido en base de datos**, calculado al leer el grado a partir de sus niveles. No se añade ninguna columna al catálogo de grados.
- Actualizar el diagrama del subsistema `src/main/java/com/educaflow/subsystem/sistemaeducativo/domains/tablas.plantuml`, que hoy no dibuja ni `Grado` ni `Nivel`: añadir las dos tablas y la relación nueva `Grado 1 — * Nivel`, además de las relaciones de `Ciclo` con ambas. Regenerar después su PNG (`./gradlew -q GenerateDocs`), que es incremental por fecha y no se actualiza solo si el `.puml` queda más antiguo.
- El formulario de ciclo lleva hoy, en el campo `nivel`, un filtro de valores elegibles copiado por error del de `grado` (pide códigos `D` o `E`, que ningún nivel tiene), por lo que el selector de nivel no ofrece nada. Ese filtro se sustituye por el de `RUI-ciclos-formulario-004` (los niveles del grado elegido), y el filtro `D`/`E` del campo `grado` se elimina (`RUI-ciclos-formulario-005`).
- Las restricciones de `Ciclo` y `Nivel` son la única defensa real: el `showIf`/`requiredIf` de la vista no protege nada (la entidad se puede escribir por otras vías). Hoy no existe ni `CicloService` ni `NivelService`, así que hay que crearlos.
- Al introducir una lista cerrada de propiedades editables en `Ciclo`, vigilar que el panel maestro-detalle de cursos del formulario de ciclo siga guardando sus cursos (lo cubre `ESC-011`).
