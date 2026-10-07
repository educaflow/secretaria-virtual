# Contrato de generación de código de tests (TDD, fase red)

Lo leen el **descomponedor** (README §2.1, para crear las tareas de test) y el **implementador** (README §2.2, para materializarlas). Define cómo convertir `design/test-unit-desc.md` en **código real** de tests JUnit 5 + Mockito en `src/test/java/...`, **antes** de que exista la pieza de producción.

**CRITICAL — esto es TDD de verdad:** el test se escribe **contra las firmas que el diseño declara** (`design.md` → «Piezas»), no contra código existente. Cuando se materializa, la clase bajo test normalmente **aún no existe**: el test no compila todavía o falla. Es lo esperado (fase **red**). La tarea **siguiente** implementa la pieza hasta que el test pase (`implementation.md` §3).

> Si el diseño no trae `test-unit-desc.md`, no se crea ninguna tarea de test (sin error): el diseño lo justificó en su «Estrategia de verificación».

---

## 1. Ubicación y skills

| Tipo | Descripción de entrada | Código de salida | Skills de la tarea |
|---|---|---|---|
| Tests unitarios | `design/test-unit-desc.md`, sección `## Clase: <FQN>` | `src/test/java/com/educaflow/<paquete-de-la-clase>/<Clase>Test.java` (el `Fichero de test` que declara la sección) | `k-code-quality` (sobre todo su `tests.md`) y los skills de dominio de la pieza |

El paquete del test **MUST** reflejar el de la clase que prueba.

---

## 2. Tareas de test que crea el descomponedor

**Una tarea por `## Clase:`** de `test-unit-desc.md`, colocada **inmediatamente antes** de la tarea de su pieza de producción (`decomposition.md` §2).

Plantilla del `<texto del prompt>` de una tarea de test:

```
Genera el código de los tests unitarios descritos en `design/test-unit-desc.md`
para la clase <FQN> (sección «## Clase: <FQN>»).

- Es la fase RED de TDD: la clase <FQN> AÚN NO EXISTE (la crea la tarea siguiente). Escribe el test
  contra las FIRMAS que declara `design.md` en su sección «Piezas» para esa clase (pegadas abajo),
  no contra código existente. Es normal que el test no compile o falle ahora.
- La descripción es el contrato: implementa EXACTAMENTE los tests que describe (nombre, tipo, mocks,
  acción, aserción con el mensaje exacto, y el REQ- que verifica). **MUST NOT** inventar tests
  ni omitir ninguno. **MUST NOT** ajustar un test a lo que «probablemente hará» la implementación:
  los valores esperados salen de la spec (los trae la descripción).
- Ubicación de salida: `<Fichero de test de la sección>`.
- Stack: JUnit 5/Jupiter + Mockito (`MockitoExtension`, `mockStatic` para SecurityUtil/Beans/I18n/AppSettings),
  siguiendo el estilo de los tests existentes en `src/test/java/com/educaflow/...` (incluido `JUnitHelper`).
- Un test comprueba COMPORTAMIENTO (k-code-quality/tests.md): nada de copiar el valor del código ni
  aserciones triviales.
- Si la descripción cita un método o firma que NO está en las firmas del diseño pegadas abajo,
  **detente y reporta** (`DESIGN-ERRATA` si es una errata pequeña; si no, `BLOCKED`), no lo inventes.

Firmas de la pieza según design.md:
<bloque de firmas de la subsección «Piezas» de esa clase, verbatim>
```

**MUST** añadir a la tarea los skills de §1 y, en su fila de `=== TAREAS ===`, la marca `(TDD red)`.

---

## 3. Materialización (implementador)

1. Una tarea de test es **código Java** → aplica `implementation.md` §2: carga los skills y delega en `developer-code-implementer` con el texto verbatim.
2. Al terminar, **MUST** comprobar que el test **compila sintácticamente** en lo que depende de sí mismo (imports de JUnit/Mockito, estructura), aceptando que falle o no compile por la **ausencia de la clase bajo test**.
3. No ejecutes `./gradlew test` (la clase no existe).
4. Devuelve `DONE` con el resumen «fase red: N tests escritos contra las firmas del diseño; la clase <FQN> la crea `task_MM.md`».

**MUST NOT**:

- **MUST NOT** crear la clase de producción «para que compile»: eso es la tarea siguiente.
- **MUST NOT** adaptar el test a una superficie distinta de la del diseño.
- **MUST NOT** convertir en tests unitarios lo que la «Estrategia de verificación» del diseño asignó solo a E2E.
