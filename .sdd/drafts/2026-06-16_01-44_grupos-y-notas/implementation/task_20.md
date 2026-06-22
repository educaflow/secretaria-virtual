---
type: implementation-task
---

# Tarea 20 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-code-quality
- k-sistemas

Genera el código de los tests unitarios descritos en `design/test-unit-desc.md` para la clase `com.educaflow.system.gruposnotas.service.impl.ModuloGrupoServiceImpl`.

- La descripción es el contrato: implementa EXACTAMENTE los tests que describe (nombre, propósito, mocks, acción, aserción/mensaje esperado, y la regla V/R que verifica) en la sección «Clase: `com.educaflow.system.gruposnotas.service.impl.ModuloGrupoServiceImpl`» de `design/test-unit-desc.md`. **MUST NOT** inventar tests que la descripción no liste ni omitir ninguno.
- Ubicación de salida: `src/test/java/com/educaflow/system/gruposnotas/service/impl/ModuloGrupoServiceImplTest.java`.
- Stack: JUnit 5/Jupiter + Mockito.
- Las clases de producción y los XML ya están en el árbol (las tareas previas las materializaron): los tests se escriben CONTRA ellas. Si una clase o método que la descripción cita no existe en el código, **detente y reporta** (BLOCKED).

Cubre, según la descripción: `validateInsert` (V-ModuloGrupo-001: happy y duplicado), `update` (lanza `UnsupportedOperationException` incondicionalmente) y `allowPropertiesInsert/Update` (deny-all). Convenciones y supuestos de mocking generales: ver las «Convenciones» y «Notas y supuestos de mocking (comunes)» de `design/test-unit-desc.md` (JUnit 5 + Mockito; construcción del `*ServiceImpl` con `(Class<T>, Repository<T>)`; entidades reales con `new` + setters).
