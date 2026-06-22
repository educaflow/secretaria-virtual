---
type: implementation-task
---

# Tarea 19 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-code-quality
- k-sistemas

Genera el código de los tests unitarios descritos en `design/test-unit-desc.md` para la clase `com.educaflow.system.gruposnotas.service.impl.GrupoServiceImpl`.

- La descripción es el contrato: implementa EXACTAMENTE los tests que describe (nombre, propósito, mocks, acción, aserción/mensaje esperado, y la regla V/R/CC que verifica) en la sección «Clase: `com.educaflow.system.gruposnotas.service.impl.GrupoServiceImpl`» de `design/test-unit-desc.md`. **MUST NOT** inventar tests que la descripción no liste ni omitir ninguno.
- Ubicación de salida: `src/test/java/com/educaflow/system/gruposnotas/service/impl/GrupoServiceImplTest.java`.
- Stack: JUnit 5/Jupiter + Mockito.
- Las clases de producción y los XML ya están en el árbol (las tareas previas las materializaron): los tests se escriben CONTRA ellas. Si una clase o método que la descripción cita no existe en el código, **detente y reporta** (BLOCKED): la descripción y el código deben cuadrar.

Convenciones y supuestos de mocking (de `design/test-unit-desc.md`):
- JUnit 5 (Jupiter) + Mockito (`MockitoExtension`). Estáticos del stack con `Mockito.mockStatic`. Aserciones con `org.junit.jupiter.api.Assertions`; para excepciones envueltas en cadena de causas se puede usar `JUnitHelper.assertThrowsCause`. Nombres de test: `metodo_condicion_resultadoEsperado`.
- El `*ServiceImpl` se construye con `(Class<T> model, Repository<T> repository)`; se instancia con un mock de `Repository<Grupo>` (o el repo personalizado) y los demás colaboradores (`@Inject`) se inyectan por reflexión / constructor de test, sin tocar BD real.
- `GrupoRepository` (`findByNombreCentroCursoAcademico`, `save`), `Repository<ModuloGrupo>`/`ModuloGrupoRepository` (`save`) → mocks. Usuario/rol con `Mockito.mockStatic(SecurityUtil.class)`/`AuthUtils.class` (supervisor de centro X / administrador). Entidades reales con `new` + setters.
- Los `validate*` devuelven `Optional<BusinessMessages>` (vacío = OK; presente = error). Las acciones invocan `validateXxx(...).ifPresent(BusinessMessages::throwIfInvalid)`; se comprueba el mensaje exacto del spec sobre el `BusinessMessage`/excepción.
