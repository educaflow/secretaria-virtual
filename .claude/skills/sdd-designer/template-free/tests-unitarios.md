# Parte del diseño: tests unitarios (TDD)

Como parte del diseño, **el subagente `test-unitarios`** escribe `design/test-unit-desc.md`: la **descripción** (no el código) de los tests unitarios JUnit 5 + Mockito de las piezas con lógica del diseño.
`/sdd-implementer` genera el código de cada test **antes** que el de su pieza de producción (fase «red» del TDD) y la pieza se implementa hasta que el test pasa.

**CRITICAL — todavía no hay código.** Las piezas se enumeran **desde el diseño** (`design.md`: tabla de ficheros, sección «Piezas» y, sobre todo, la tabla 6.1 de la «Estrategia de verificación»), no del árbol de fuentes. Para piezas que el diseño **modifica** se puede leer el fichero real.

**Quién lo usa** (`README.md` §2): lo produce `test-unitarios` (§2.6); lo verifica `verificador-test-unitarios` (§7) y lo corrige `corrector-test-unitarios`.

---

## 1. Qué piezas describir (y cuáles no)

- La fuente es la **tabla 6.1 «Tests unitarios (TDD)»** del `design.md`: **MUST** describir tests para **cada pieza** con `Test unitario` distinto de `—`, y **MUST NOT** describirlos para las que el diseño excluyó con justificación (se listan en «Cobertura» como excluidas, con la justificación copiada).
- Si el `test-unitarios` cree que una exclusión **no está justificada** (la pieza tiene lógica y sí se puede mockear), **no inventa el test**: lo anota en «Cobertura» bajo `Observaciones` para que quede constancia. El verificador del diseño ya debió reportarlo (`validacion.md` §2.g); si no lo hizo, el usuario lo verá en el informe.
- Para una pieza que el diseño **modifica**, describe **solo** los tests del comportamiento nuevo o cambiado.
- Nunca se describen tests para POJOs generados, XML, ficheros de propiedades ni interfaces sin comportamiento.

## 2. Qué describir en cada test (y qué NO)

- **MUST NOT** escribir código Java: ni `@Test`, ni imports, ni cuerpos. **Solo descripción.**
- Cada test: **Nombre** (`metodo_condicion_resultadoEsperado`), **Tipo** (`happy` | `error` | `borde`), **Verifica** (los `REQ-NNN` que ejerce, o `—`), **Arrange** (entrada y qué colaboradores se mockean y qué devuelve cada stub), **Act**, **Assert** (retorno, **excepción + mensaje exacto** de la spec, o `verify(...)`).
- **MUST** cubrir por método con lógica: camino feliz, **una rama por cada requisito/condición** que aplica, y casos borde (nulos, vacíos, límites como «500 caracteres»).
- **CRITICAL — un test comprueba comportamiento, no repite el valor del código** (`k-code-quality/tests.md`): el valor esperado sale de la **spec** (el mensaje, el límite, el orden), no de «lo que devolverá el método».

- ✅ CORRECTO: `insert_textoVacio_lanzaExcepcion` — error — Verifica `REQ-003`; Arrange: aviso con texto `""`; mock `SecurityUtil.getUser()` → admin; Act: `insert(aviso)`; Assert: lanza `ValidationException` con «El texto del aviso es obligatorio».
- ❌ INCORRECTO: pegar `@Test void x(){…}`, o un test sin `Assert` con el mensaje exacto.

## 3. Estrategia de mocking (stack Axelor / Guice / JPA)

- Convenciones del proyecto: JUnit 5 (Jupiter) con `org.junit.jupiter.api.Assertions`, Mockito con `@ExtendWith(MockitoExtension.class)` y `MockedStatic` para los estáticos. Ya están en el proyecto. **MUST** seguir el estilo de los tests existentes en `src/test/java/com/educaflow/...` (incluido `JUnitHelper`).
- Clase bajo test: instanciada (o `@InjectMocks`) con **mocks** de sus colaboradores. **MUST NOT** tocar la BD real.
- Repositorios y otros servicios: **mock**; programa cada finder.
- Estáticos del stack — `SecurityUtil` (`getUser`, `isAdmin`), `Beans.get(...)`, `ModelServiceFactory`, `I18n.get(...)`, `AppSettings`: `Mockito.mockStatic(...)`. Indica por test qué devuelve cada uno.
- Entidades: `new <Entidad>()` + setters; no se mockean.
- Controladores: mockea `ActionRequest`/`ActionResponse`; verifica las interacciones sobre `response`.
- Configuración (`axelor-config.properties`): mockea `AppSettings.get()` o el `Provider` que la encapsule; un test **nunca** lee el fichero real.
- Autorización / multicentro: programa el usuario actual y describe **ambas ramas** (autorizado / no autorizado).

## 4. Trazabilidad y cobertura

- **MUST**: cada pieza marcada testeable en la tabla 6.1 del `design.md` aparece en `test-unit-desc.md`; cada pieza excluida aparece en «Cobertura → Excluidas» con su justificación copiada.
- **MUST**: cada `REQ-NNN` que la trazabilidad del `design.md` asigna a una pieza testeable está cubierto por ≥1 test (rama de fallo y, donde aplique, rama OK).
- Los `REQ-` que solo se verifican por E2E (reglas de UI, textos en pantalla) se listan como «solo E2E».

## 5. Plantilla de `test-unit-desc.md`

```markdown
# Tests unitarios

Descripción de los tests unitarios (JUnit 5 + Mockito) por pieza y método. **Solo descripción, sin código**: `/sdd-implementer` genera el código de cada test **antes** que su pieza de producción (TDD) y la implementa hasta que pasa.

## Convenciones
- JUnit 5 (Jupiter) + Mockito (`MockitoExtension`). Estáticos del stack con `Mockito.mockStatic`.
- Nombres de test: `metodo_condicion_resultadoEsperado`.
- Los valores esperados salen de la spec (`REQ-`), nunca del código.

---

## Clase: `<FQN de la clase>`  —  <servicio | controlador | helper | componente>

**Responsabilidad:** <según el diseño>
**Colaboradores a mockear:** <…>
**Origen diseño:** <sección «Piezas» y REQ- que cubre>
**Fichero de test:** `src/test/java/com/educaflow/<paquete>/<Clase>Test.java`

### Método: `<firma del método>`

- **`<nombre_test>`** — Tipo: happy|error|borde. Verifica: `REQ-…` (o `—`).
  - **Arrange:** <…>
  - **Act:** <…>
  - **Assert:** <…>

---

## Cobertura
- Piezas testeadas: <lista>.
- Excluidas (con la justificación de la tabla 6.1 del diseño): <pieza — justificación>.
- `REQ-` cubiertos por tests unitarios: <lista>.
- `REQ-` solo E2E: <lista>.
- Observaciones: <exclusiones que parecen injustificadas, o *(ninguna)*>.
```

## 6. Checklist del subagente `test-unitarios`

- [ ] ¿Cada pieza testeable de la tabla 6.1 del `design.md` tiene su sección, con su `Fichero de test` en el paquete espejo?
- [ ] ¿Cada pieza excluida está en «Cobertura → Excluidas» con la justificación copiada, y ninguna excluida tiene tests inventados?
- [ ] ¿Cada método con lógica tiene camino feliz + una rama por requisito/condición + bordes?
- [ ] ¿Cada test indica `Arrange` (mocks + qué devuelven), `Act` y `Assert` con el mensaje exacto de la spec?
- [ ] ¿La estrategia de mocking respeta §3?
- [ ] ¿Ningún valor esperado se deduce «del código» en vez de la spec?
- [ ] ¿No hay código Java?
- [ ] ¿La estructura sigue la plantilla §5?

**MUST NOT** devolver `ESCRITO: test-unit-desc.md` si queda algo. **LIMIT**: 3 iteraciones.

## 7. Verificación de coherencia con el diseño (post-generación)

La **fuente de verdad** es `design.md`: si un test no cuadra, se corrige el **test**.

### 7.1 Comprobaciones del `verificador-test-unitarios`

- **Pieza existe y es testeable:** cada `## Clase:` corresponde a una pieza del diseño marcada testeable en la tabla 6.1. Una clase que el diseño no define → `BLOCKING`; una que el diseño excluyó con justificación → `IMPORTANT` (se mueve a Excluidas).
- **Método existe:** cada `### Método:` existe en la pieza según «Piezas» del `design.md`. Si no → `BLOCKING`.
- **Requisito existe:** cada `Verifica: REQ-…` existe en la spec y el `design.md` lo asigna a esa pieza. Si no → `IMPORTANT`.
- **Cobertura cuadra:** las piezas testeadas + excluidas = todas las piezas Java/Kotlin de producción de la tabla de ficheros; las exclusiones llevan la justificación del diseño. Discrepancias → `IMPORTANT`.
- **Sin invención:** nada que no esté en el diseño → `BLOCKING`.
- **Fichero de test** en el paquete espejo de la clase → si no, `MINOR`.
- **Estructura y forma** (§5, sin código) → `MINOR`/`IMPORTANT`.

### 7.2 Tarea del `corrector-test-unitarios`

- Aplica en sitio cada incoherencia sobre `design/test-unit-desc.md`, ajustándose a §5.
- **MUST NOT** modificar `design.md` ni ningún otro fichero del diseño. **MUST NOT** añadir clases/métodos/requisitos que no estén en el diseño.

El contrato de salida (`OK-CORRECTO` o JSONL `P-NNN`) y el bucle (LIMIT 10) los fija el skill.
