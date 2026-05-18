---
type: design-guidelines
---

- El proceso de importación, ya existe en el subsistema `importacion`
- Este proceso, deberá modificar la implementación `ImportadorUsuarioCsv` de `ImportadorFichero`.
- Esta iniciativa es continuación directa de la historia de usuario `importacion-vistas`, archivada en `.sdd/specs/0003_importacion-vistas/`. El diseño debe respetar todas las decisiones de diseño tomadas allí (especialmente las del fichero `design-guidelines.md` de esa spec: entidad `TareaImportacion`, enum `TipoFicheroImportacion`, `ImportadorException`, contrato de `ImportadorFichero` y su factoría, forma del resultado, alcance estricto, etc.).
- Si durante el análisis o el diseño se detecta cualquier discrepancia entre lo que esta iniciativa pide y lo decidido en `0003_importacion-vistas`, detener el trabajo y preguntar al usuario antes de proceder. No resolver discrepancias por iniciativa propia.
- **Calidad del código:** aplicar SIEMPRE las reglas del skill `k-code-quality` (ficheros `metodos.md`, `clases.md`, `java-idioms.md` y `proyecto.md`) en todas las decisiones de diseño de servicios y controladores: descomposición de métodos, responsabilidad única, idiomas Java modernos (Optional, streams, records, var), convenciones Axelor (controladores, capa de servicio, repositorios, DI/Guice).