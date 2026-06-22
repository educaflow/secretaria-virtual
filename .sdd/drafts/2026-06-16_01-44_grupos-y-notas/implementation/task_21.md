---
type: implementation-task
---

# Tarea 21 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-code-quality
- k-sistemas

Genera el código de los tests unitarios descritos en `design/test-unit-desc.md` para la clase `com.educaflow.system.gruposnotas.service.impl.AlumnoGrupoServiceImpl`.

- La descripción es el contrato: implementa EXACTAMENTE los tests que describe (nombre, propósito, mocks, acción, aserción/mensaje esperado, y la regla V/R/CC que verifica) en la sección «Clase: `com.educaflow.system.gruposnotas.service.impl.AlumnoGrupoServiceImpl`» de `design/test-unit-desc.md`. **MUST NOT** inventar tests que la descripción no liste ni omitir ninguno.
- Ubicación de salida: `src/test/java/com/educaflow/system/gruposnotas/service/impl/AlumnoGrupoServiceImplTest.java`.
- Stack: JUnit 5/Jupiter + Mockito.
- Las clases de producción y los XML ya están en el árbol (las tareas previas las materializaron): los tests se escriben CONTRA ellas. Si una clase o método que la descripción cita no existe en el código, **detente y reporta** (BLOCKED).

Cubre, según la descripción: `validateInsert` (V-AlumnoGrupo-001..005), `validateRemove` (V-AlumnoGrupo-006), `insert` (R-AlumnoGrupo-002 restaurar grupo desde contexto e ignorar grupo del cliente — defensa IDOR; R-AlumnoGrupo-001 crear notas NO_EVALUADO), `update` (lanza `UnsupportedOperationException`), `calcularNotaMedia` (CC-001: «Sin nota», una nota numérica, MH=10, media 8.5→9, 8 y 6→7, redondeo 5.5→6, lista nula→«Sin nota») y `allowProperties*`.

Supuestos de mocking específicos (de `design/test-unit-desc.md`):
- Colaboradores mock: `AlumnoGrupoRepository` (`existsOtroGrupoMismoCursoAcademico`, `findByGrupo`, `save`), `NotaRepository`, `ModelServiceFactory` (`resolve(Nota.class)` → `NotaService` mock), `GrupoRepository` (si la restauración del grupo resuelve por id), repo/relación para V-AlumnoGrupo-003 (centro + tipo Alumno), `SecurityUtil`/`AuthUtils` estático. Entidades `AlumnoGrupo`, `Grupo`, `User`, `Centro`, `ModuloGrupo`, `Nota` reales.
- **Cálculo de la media (CC-001):** el algoritmo vive en el cuerpo CDATA de la propiedad `notaMedia` del dominio; el getter generado `AlumnoGrupo.getNotaMedia()` lo ejecuta y `calcularNotaMedia` delega en él. Se prueba a través de `calcularNotaMedia(alumnoGrupo)` con `AlumnoGrupo`/`Nota` reales (con `new` + setters), **sin mocks**.
- En el alta de notas se restaura el `grupo` desde el contexto del padre antes de validar; los tests programan que `getGrupo()` quede en el grupo legítimo y verifican que un `grupo` dictado por el cliente se ignora (asignación incondicional).
