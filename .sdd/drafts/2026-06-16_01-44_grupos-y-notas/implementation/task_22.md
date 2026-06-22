---
type: implementation-task
---

# Tarea 22 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-code-quality
- k-sistemas

Genera el código de los tests unitarios descritos en `design/test-unit-desc.md` para la clase `com.educaflow.system.gruposnotas.service.impl.NotaServiceImpl`.

- La descripción es el contrato: implementa EXACTAMENTE los tests que describe (nombre, propósito, mocks, acción, aserción/mensaje esperado, y la regla V/R/CC que verifica) en la sección «Clase: `com.educaflow.system.gruposnotas.service.impl.NotaServiceImpl`» de `design/test-unit-desc.md`. **MUST NOT** inventar tests que la descripción no liste ni omitir ninguno.
- Ubicación de salida: `src/test/java/com/educaflow/system/gruposnotas/service/impl/NotaServiceImplTest.java`.
- Stack: JUnit 5/Jupiter + Mockito.
- Las clases de producción y los XML ya están en el árbol (las tareas previas las materializaron): los tests se escriben CONTRA ellas. Si una clase o método que la descripción cita no existe en el código, **detente y reporta** (BLOCKED).

Cubre, según la descripción: `validateInsert` (V-Nota-005), `validateGuardarNota` (V-Nota-001/002/003, incluidos los bordes: 3ª MH permitida, ya-era-MH no cuenta, valor=null), `guardarNota` (R-Nota-001/002: primera calificación fija `fechaCalificacion`, modificación posterior fija `fechaUltimaModificacion`, no-cambio no toca fechas, cliente no puede dictar fechas, grupo cerrado y 4ª MH lanzan y no guardan) y `allowProperties*` (whitelist `valor` en guardarNota; deny-all en insert/update/remove).

Supuestos de mocking específicos (de `design/test-unit-desc.md`): `NotaRepository` (`countMatriculasHonorByModuloGrupo`, `save`, consulta de unicidad) → mock; entidades `Nota`, `ModuloGrupo`, `Grupo`, `AlumnoGrupo` reales con `new` + setters. **V-Nota-001:** al ser `valor` un enum, el caso testeable a nivel de servicio es `valor=null`; un literal fuera de dominio no es construible como enum.
