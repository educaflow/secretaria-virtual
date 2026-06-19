# Contrato de generación de código de tests

Lo leen el **descomponedor** (README §2.1, para crear las tareas de test) y el **implementador** (README §2.2, para materializarlas). Define cómo convertir las **descripciones de tests** que el diseñador dejó en `design/` en **código real** de tests en `src/test/...`.

El diseñador (`/sdd-designer`) **solo describe** los tests; aquí se **genera el código**. Dos descripciones, dos tipos de test:

- `design/test-unit-desc.md` → tests unitarios (JUnit 5 + Mockito).
- `design/test-arch-desc.md` → tests de arquitectura (ArchUnit).

Ambos los compila y ejecuta `./gradlew clean build` (ver `build.md`): si fallan, los arregla el bucle de build. Los tests **E2E** (`tests.md`) **NO** se generan aquí — los ejecuta `/sdd-debug-app`.

> Si el diseño **no** trae `test-unit-desc.md` ni `test-arch-desc.md` (no hay clases Java), **no se crea ninguna tarea de test** (sin error).

---

## 1. Ubicación y skills

| Tipo | Descripción de entrada | Código de salida | Skills de la tarea |
|---|---|---|---|
| Tests unitarios | `design/test-unit-desc.md` | `src/test/java/com/educaflow/<paquete-de-la-clase>/<Clase>Test.java` | `k-code-quality` (y los skills de dominio de la clase bajo test, p.ej. `k-sistemas`) |
| Tests de arquitectura | `design/test-arch-desc.md` | `src/test/java/com/educaflow/.../architecture/<...>Test.java` según las convenciones de `k-archunit` | `k-archunit` |

El paquete de cada test **MUST** reflejar el de la clase que prueba (mismo paquete bajo `src/test/...`). Para los tests de arquitectura, la ubicación, el `@AnalyzeClasses` y las convenciones de clase las define el skill `k-archunit` (`secretaria-virtual-rules.md` es el catálogo de reglas `C1`–`C22`).

---

## 2. Tareas de test que crea el descomponedor

En el orden de §2 de `decomposition.md` (tests al final):

1. **Tests unitarios** — **una tarea por clase de producción** que `test-unit-desc.md` describe (o agrupando la clase con su test, criterio de acoplamiento de `decomposition.md` §2). Cada tarea referencia la **sección concreta** de `test-unit-desc.md` que describe esa clase.
2. **Tests de arquitectura** — **una sola tarea** que materializa todo `test-arch-desc.md` (las reglas seleccionadas del catálogo `k-archunit`).

Plantilla del `<texto del prompt>` de una tarea de test (rellena la de `decomposition.md` §4):

```
Genera el código de los tests <unitarios|de arquitectura> descritos en `design/<test-unit-desc.md|test-arch-desc.md>`
<para la clase <FQN> | en su totalidad>.

- La descripción es el contrato: implementa EXACTAMENTE los tests que describe (nombre, propósito, mocks,
  acción, aserción/mensaje esperado, y la regla V/R/CC o C…/A-NNN que verifica). **MUST NOT** inventar tests
  que la descripción no liste ni omitir ninguno.
- Ubicación de salida: `src/test/java/com/educaflow/<paquete>/<...>Test.java`.
- Stack: <JUnit 5/Jupiter + Mockito | ArchUnit 1.4.2 + JUnit 5>.
- Las clases de producción y los XML ya están en el árbol (las tareas previas las materializaron): los tests
  se escriben CONTRA ellas. Si una clase o método que la descripción cita no existe en el código, **detente y
  reporta** (BLOCKED): la descripción y el código deben cuadrar.
```

**MUST** añadir a la tarea los skills de §1.

---

## 3. Materialización (implementador)

Una tarea de test es **código Java** → aplica `implementation.md` §2: carga primero los skills de la tarea (`Skill`) y delega en `code-implementer` con el texto de la tarea **verbatim**. La descripción (`test-unit-desc.md` / `test-arch-desc.md`) es el contrato del **qué** testear; `code-implementer` escribe el **cómo** (el código JUnit/ArchUnit real).

**MUST NOT**:

- **MUST NOT** modificar el código de producción para que un test pase: si un test no cuadra con el código, es señal de un fallo previo → reporta `BLOCKED`. (Cuadrar tests con código que ya está mal es trabajo del bucle de build, no de la generación.)
- **MUST NOT** convertir en tests las reglas `U-` (UI/cliente): esas se verifican como E2E en `tests.md`, no aquí.
- **MUST NOT** redefinir con otro criterio una regla de arquitectura que el catálogo `k-archunit` ya define; usa la del catálogo.
