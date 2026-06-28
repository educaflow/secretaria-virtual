# Contrato de generación de código de tests

Lo leen el **descomponedor** (README §2.1, para crear las tareas de test) y el **implementador** (README §2.2, para materializarlas). Define cómo convertir las **descripciones de tests** que el diseñador dejó en `design/` en **código real** de tests en `src/test/...`.

El diseñador (`/sdd-designer`) **solo describe** los tests; aquí se **genera el código**:

- `design/test-unit-desc.md` → tests unitarios (JUnit 5 + Mockito).

Los compila y ejecuta `./gradlew clean build` (ver `build.md`): si fallan, los arregla el bucle de build. Los tests **E2E** (`test-e2e-desc.md`) **NO** se generan aquí — los ejecuta `/sdd-debug-with-test-e2e-desc`.

> Si el diseño **no** trae `test-unit-desc.md` (no hay clases Java), **no se crea ninguna tarea de test** (sin error).

---

## 1. Ubicación y skills

| Tipo | Descripción de entrada | Código de salida | Skills de la tarea |
|---|---|---|---|
| Tests unitarios | `design/test-unit-desc.md` | `src/test/java/com/educaflow/<paquete-de-la-clase>/<Clase>Test.java` | `k-code-quality` (y los skills de dominio de la clase bajo test, p.ej. `k-sistemas`) |

El paquete de cada test **MUST** reflejar el de la clase que prueba (mismo paquete bajo `src/test/...`).

---

## 2. Tareas de test que crea el descomponedor

En el orden de §2 de `decomposition.md` (tests al final):

1. **Tests unitarios** — **una tarea por clase de producción** que `test-unit-desc.md` describe (o agrupando la clase con su test, criterio de acoplamiento de `decomposition.md` §2). Cada tarea referencia la **sección concreta** de `test-unit-desc.md` que describe esa clase.

Plantilla del `<texto del prompt>` de una tarea de test (rellena la de `decomposition.md` §4):

```
Genera el código de los tests unitarios descritos en `design/test-unit-desc.md`
para la clase <FQN>.

- La descripción es el contrato: implementa EXACTAMENTE los tests que describe (nombre, propósito, mocks,
  acción, aserción/mensaje esperado, y la regla V/R/CC que verifica). **MUST NOT** inventar tests
  que la descripción no liste ni omitir ninguno.
- Ubicación de salida: `src/test/java/com/educaflow/<paquete>/<...>Test.java`.
- Stack: JUnit 5/Jupiter + Mockito.
- Las clases de producción y los XML ya están en el árbol (las tareas previas las materializaron): los tests
  se escriben CONTRA ellas. La descripción y el código **MUST** cuadrar en AMBOS sentidos; si NO cuadran,
  **detente y reporta** (BLOCKED) en vez de adaptar el test. Reporta BLOCKED si:
    - una clase/método que la descripción cita **no existe** en el código, o
    - el código expone una **firma o nombre distinto** del que la descripción cita (p.ej. la descripción dice
      `insert(X)` y el código tiene `guardarX(X, Long)`), o
    - el código expone **clases/métodos públicos que la descripción no lista** (superficie de más).
  **MUST NOT** "adaptar" los tests al código divergente (ni reinterpretar a qué método apuntan): esa divergencia
  es un fallo previo del implementador que decide el motor/usuario, no algo que el generador de tests deba tapar.
```

**MUST** añadir a la tarea los skills de §1.

---

## 3. Materialización (implementador)

Una tarea de test es **código Java** → aplica `implementation.md` §2: carga primero los skills de la tarea (`Skill`) y delega en `code-implementer` con el texto de la tarea **verbatim**. La descripción (`test-unit-desc.md`) es el contrato del **qué** testear; `code-implementer` escribe el **cómo** (el código JUnit real).

**MUST NOT**:

- **MUST NOT** modificar el código de producción para que un test pase: si un test no cuadra con el código, es señal de un fallo previo → reporta `BLOCKED`. (Cuadrar tests con código que ya está mal es trabajo del bucle de build, no de la generación.)
- **MUST NOT** adaptar un test a una superficie de producción que **diverge** de la descripción (nombre/firma distintos, o clases/métodos públicos que la descripción no lista): repórtalo `BLOCKED`. Adaptar el test —aunque sea "consistente" con el código real— **enmascara** una desviación del implementador y la hace invisible al usuario.
- **MUST NOT** convertir en tests las reglas `U-` (UI/cliente): esas se verifican como E2E en `test-e2e-desc.md`, no aquí.
