# Parte del diseño: tests unitarios

Como parte del diseño, **el subagente `test-unitarios`** escribe `design/test-unit-desc.md`: la **descripción** (no el código) de los tests unitarios JUnit 5 + Mockito necesarios para las clases Java que el diseño planifica. `/sdd-implementer` genera el código de los tests a partir de esta descripción.

**CRITICAL — todavía no hay código.** Cuando se ejecuta esta fase, las clases Java del sistema **aún no existen** (las crea `/sdd-implementer`). El subagente `test-unitarios` enumera las clases y sus métodos **desde el diseño** (`design.md`: la sección de ficheros a crear/modificar, los servicios, controladores y helpers, y las reglas `V`/`R`/`CC` que cada método aplica), **no** del árbol de fuentes. La descripción de los tests se escribe **antes** del código, a partir del diseño.

**Quién lo usa** (`README.md` §2): lo produce el subagente `test-unitarios` (§2.6); ningún otro rol lo modifica.

---

## 1. Qué clases describir (y cuáles no)

- **MUST** describir tests para **cada clase Java con lógica** que el diseño crea o modifica: servicios (`*ServiceImpl` y sus `validate*`, reglas de negocio `R`, campos calculados `CC`), controladores (`@CallMethod`) y helpers/calculadores/clases de apoyo.
- **MUST NOT** describir tests para:
  - Entidades de dominio generadas por Axelor (POJOs) sin lógica propia → decláralas «sin lógica testable» y omítelas (sus getters calculados se testan en el servicio al que delegan, ver §3).
  - Reglas que viven **solo en la capa cliente/XML** (`U-` de vistas: `showIf`/`readonlyIf`/`<action-validate>`/`<action-condition>` de cliente) → no son testables con JUnit; **MUST** listarlas como **excluidas** (van como E2E en `test-e2e-desc.md`).
  - Interfaces, enums y DTOs sin comportamiento.
- Para una clase que el diseño **modifica** (ya existe en `src/main/java/...`), describe **solo** los tests del comportamiento nuevo/cambiado; puedes leer el fichero real para conocer su forma.

## 2. Qué describir en cada test (y qué NO)

- **MUST NOT** escribir código Java: ni `@Test`, ni imports, ni cuerpos de método, ni aserciones en código. **Solo descripción.**
- Cada test se describe con estos campos:
  - **Nombre** — descriptivo, estilo `metodo_condicion_resultadoEsperado` (p.ej. `validateInsert_cursoNulo_lanzaValidationException`).
  - **Tipo** — `happy` | `error` | `borde`.
  - **Verifica** — la(s) regla(s) `V-`/`R-`/`CC-` del diseño que ejerce, o `—`.
  - **Arrange** — los objetos de entrada y **qué colaboradores se mockean y qué devuelve cada stub**.
  - **Act** — el método que se invoca.
  - **Assert** — el resultado esperado: valor de retorno, **excepción esperada + mensaje exacto** (tal cual lo define la regla del spec) y/o **interacciones a verificar** (`verify(...)`).
- **MUST** cubrir, por método con lógica: el **camino feliz**, **una rama por cada validación/regla** que el método aplica (caso de fallo) y los **casos borde** (nulos, vacíos, límites numéricos —p.ej. «máx. 3 MH»—, colecciones vacías).

- ✅ CORRECTO (un test descrito): `validateUpdate_grupoCerrado_lanzaExcepcion` — error — Verifica `V-Nota-015`; Arrange: nota de un grupo en estado `CERRADO`, mock `grupoRepository.find(id)` → ese grupo; Act: `validateUpdate(nota)`; Assert: lanza `ValidationException` con mensaje «No se pueden modificar notas de un grupo cerrado».
- ❌ INCORRECTO: pegar `@Test void x(){ ... }` (eso es código; aquí solo se describe), o un test sin `Arrange`/`Assert` (no es accionable por el implementador).

## 3. Estrategia de mocking (stack Axelor / Guice / JPA)

- **Convenciones del proyecto** (ya en uso): JUnit 5 (Jupiter) con `org.junit.jupiter.api.Assertions` (`assertThrows`, `assertEquals`…) —**no** AssertJ— y Mockito con `@ExtendWith(MockitoExtension.class)` y `MockedStatic` para los estáticos. JUnit 5 + Mockito ya están en el proyecto (Axelor + `mockito-junit-jupiter`); **no** hace falta añadir dependencias. **MUST**: describe los tests siguiendo el estilo de los tests existentes en `src/test/java/com/educaflow/...` (incluidos helpers como `JUnitHelper`), para que sean coherentes.
- **Clase bajo test**: instánciala (o `@InjectMocks`) e inyéctale **mocks** de sus colaboradores. **MUST NOT** tocar la base de datos real.
- **Repositorios** (`*Repository`, finders JPA): **mock**; programa cada finder para devolver el objeto/lista preparado del `Arrange`.
- **Otros servicios** de los que dependa la clase: **mock**.
- **Métodos estáticos** del stack — `SecurityUtil` (`isAdmin`, usuario/centro activo), `Beans.get(...)`, `ModelServiceFactory`, `I18n.get(...)`: describe mockearlos con **`Mockito.mockStatic(...)`** (try-with-resources o setup/teardown). Indica, por test, qué devuelve cada estático (p.ej. `SecurityUtil.isAdmin()` → `false`; usuario actual → supervisor del centro X).
- **Entidades de dominio**: instáncialas directamente (`new <Entidad>()`) y rellena campos con setters; **no** se mockean.
- **Controladores**: mockea `ActionRequest`/`ActionResponse`; describe qué devuelve `request.getContext()`/`getBean(...)` y **verifica** las interacciones sobre `response` (`setValue`, `setError`, `setFlash`, …).
- **Campos calculados `CC`** (getters transient que delegan en `Beans.get(ModelServiceFactory).resolve(...).metodo(this)`): describe el test **sobre el método del servicio** que hace el cálculo; añade, si aporta, **un** test de delegación del getter con `mockStatic(Beans)`.
- **Autorización / multi-centro**: programa el usuario actual (rol/centro) vía el estático correspondiente y describe **ambas ramas** (autorizado → OK; no autorizado → excepción).

## 4. Trazabilidad y cobertura

- **MUST**: cada clase Java con lógica del diseño aparece en `test-unit-desc.md` (las omitidas, declaradas «sin lógica testable» con su razón).
- **MUST**: cada regla **server-side** (`V-`/`R-`/`CC-`) del diseño está cubierta por **≥1 test** (con su rama de fallo y, donde aplique, la rama OK).
- **MUST**: las reglas **solo-cliente** (`U-`) se listan como **excluidas** (E2E en `test-e2e-desc.md`), no se testean aquí.
- Cada test declara en `Verifica` la regla que ejerce (o `—` si es un método sin regla asociada, p.ej. un helper de cálculo puro).

## 5. Plantilla de `test-unit-desc.md`

El subagente escribe un fichero con esta estructura exacta:

```markdown
# Tests unitarios

Descripción de los tests unitarios (JUnit 5 + Mockito) por clase y método para el diseño. **Solo descripción, sin código**: `/sdd-implementer` genera el código a partir de aquí. Las reglas que viven solo en la capa cliente/XML (`U-`) no se testean aquí (van como E2E en `test-e2e-desc.md`).

## Convenciones
- JUnit 5 (Jupiter) + Mockito (`MockitoExtension`). Estáticos del stack con `Mockito.mockStatic`.
- Nombres de test: `metodo_condicion_resultadoEsperado`.

---

## Clase: `<FQN de la clase>`  —  <servicio | controlador | helper>

**Responsabilidad:** <qué hace, según el diseño>
**Colaboradores a mockear:** <repositorios, otros servicios, SecurityUtil (estático), Beans/ModelServiceFactory, I18n, ActionRequest/ActionResponse…>
**Origen diseño:** <métodos y reglas V/R/CC de design.md que cubren estos tests>

### Método: `<firma del método>`

- **`<nombre_test>`** — Tipo: happy|error|borde. Verifica: `V-…`/`R-…`/`CC-…` (o `—`).
  - **Arrange:** <entrada; mocks programados y qué devuelven>.
  - **Act:** <invocación>.
  - **Assert:** <retorno esperado / excepción + mensaje exacto / verify(...)>.
- **`<otro_test>`** — …

(repite por cada método público con lógica)

---

## Clase: `<FQN>` — sin lógica testable
**Motivo:** <p.ej. POJO de dominio generado por Axelor; sin métodos con lógica>.

---

## Cobertura
- Clases con lógica descritas: <N>.
- Clases omitidas (sin lógica): <lista>.
- Reglas server-side cubiertas (`V`/`R`/`CC`): <lista>.
- Reglas solo-cliente excluidas (E2E en test-e2e-desc.md): <lista de `U-`>.
```

---

## 6. Checklist del subagente `test-unitarios`

- [ ] ¿Cada clase Java con lógica del diseño tiene su sección (o está declarada «sin lógica testable» con motivo)?
- [ ] ¿Cada método público con lógica tiene camino feliz + una rama por validación/regla + casos borde?
- [ ] ¿Cada regla server-side `V`/`R`/`CC` del diseño está en algún `Verifica`?
- [ ] ¿Las reglas solo-cliente `U-` están listadas como excluidas (no testeadas aquí)?
- [ ] ¿Cada test indica `Arrange` (mocks + qué devuelven), `Act` y `Assert` (con mensaje exacto si es excepción)?
- [ ] ¿La estrategia de mocking respeta §3 (estáticos con `mockStatic`, repos/servicios mockeados, sin BD real, entidades instanciadas)?
- [ ] ¿NO hay nada de código Java (ni `@Test`, ni imports, ni cuerpos)? Solo descripción.
- [ ] ¿La estructura sigue la plantilla §5?

El subagente **MUST NOT** devolver `ESCRITO: test-unit-desc.md` si queda algún punto sin cumplir. **LIMIT**: máximo 3 iteraciones de autocorrección.

---

## 7. Verificación de coherencia con el diseño (post-generación)

Tras escribir `test-unit-desc.md`, el skill lanza un bucle aparte con dos subagentes: **`verificador-test-unitarios`** (busca incoherencias entre `test-unit-desc.md` y el diseño) y **`corrector-test-unitarios`** (las corrige). Esta sección define **qué cuenta como incoherencia** — es la referencia del verificador. La **fuente de verdad** es el diseño (`design.md`): si un test no cuadra con el diseño, se corrige el **test**, nunca el diseño.

### 7.1 Comprobaciones del `verificador-test-unitarios`

- **Clase existe:** cada `## Clase: <FQN>` de `test-unit-desc.md` **MUST** corresponder a una clase Java que el diseño **crea o modifica** (en el inventario de `design.md`). Una clase descrita que el diseño no define → incoherencia `BLOCKING`.
- **Método existe:** cada `### Método: <firma>` **MUST** existir en esa clase según el diseño (nombre y, si el diseño la fija, firma compatible). Un método inexistente o que no pertenece a la clase → `BLOCKING`.
- **Regla existe:** cada `Verifica: V-…/R-…/CC-…` **MUST** referenciar una regla que el diseño define para ese método/clase. Una regla inventada o mal asignada → `IMPORTANT`.
- **Cobertura cuadra:** la sección «Cobertura» **MUST** ser veraz respecto al diseño: las clases con lógica declaradas coinciden con las del diseño, las omitidas «sin lógica testable» realmente no la tienen, y las reglas server-side listadas como cubiertas tienen ≥1 test. Discrepancias → `IMPORTANT`.
- **Reglas solo-cliente:** las `U-` **MUST** aparecer como excluidas, no como tests JUnit. Una `U-` testeada como unitaria → `IMPORTANT`.
- **Sin invención:** **MUST NOT** haber clases, métodos ni reglas en `test-unit-desc.md` que no estén en el diseño. Cualquier elemento inventado → `BLOCKING`.
- **Estructura y forma:** la estructura sigue la plantilla §5 y **no** hay código Java (ni `@Test`, ni imports, ni cuerpos). Desvíos → `MINOR`/`IMPORTANT` según gravedad.

### 7.2 Tarea del `corrector-test-unitarios`

- Aplica **en sitio** sobre `design/test-unit-desc.md` cada incoherencia reportada (eliminar/reasignar la clase/método/regla inexistente, corregir la cobertura, mover la `U-` a excluidas…), ajustándose a la plantilla §5.
- **MUST NOT** modificar `design.md` ni ningún otro fichero del diseño: la fuente de verdad es el diseño.
- **MUST NOT** introducir clases/métodos/reglas nuevos que no estén en el diseño.

El contrato de salida (token `OK-CORRECTO` o líneas JSONL `id`/`severidad`/`fichero`/`ubicacion`/`origen`/`problema`/`correccion`) y el bucle (LIMIT 10) los fija el skill `sdd-designer`.
