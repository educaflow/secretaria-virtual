# Plantilla de diseño de una feature libre («free») — guía e índice

Esta carpeta de plantillas define **todo lo específico de convertir una especificación de feature libre en un DISEÑO**.
Una feature libre es la que no encaja en ninguna otra plantilla: toca varios sistemas a la vez, es transversal (seguridad, i18n, infraestructura, configuración…), vive en la carpeta `base` o no es ni un sistema/subsistema ni un tipo de expediente.
El skill `sdd-designer` aporta solo el **flujo** (localizar la spec, lanzar diseñadores en paralelo, elegir el mejor con un juez, criticar/corregir, verificar/corregir, cerrar) y es **agnóstico**: lo lee todo de aquí.

**La libertad de esta plantilla es de forma, no de rigor.**
Aquí el diseñador decide la arquitectura, la estructura y las piezas como mejor convenga a la feature —no hay un esquema de entidad/servicio/controlador ni de fases que seguir—, pero a cambio **MUST** explicitar y justificar cada decisión, y **MUST** cumplir las mismas salvaguardas que el resto del pipeline: trazabilidad completa con la spec y la regla de los tests (TDD y E2E siempre que se pueda; si no, justificado) que fija `design-contract.md` §3 `## 6`.

Este `README.md` es **el único fichero que el skill conoce por nombre**. **Lo leen los nueve roles de subagente** (tabla de §2), cada uno con una tarea distinta sobre el mismo diseño.

A través de este README cada subagente descubre y lee **solo los ficheros de esta carpeta que su tarea necesita** (§2).

> **Contrato fijo (lo garantiza el skill, no lo cambia esta plantilla):** la entrada es `specification.md` (`type: specification`) y la salida es una **carpeta `design/`** dentro de la carpeta de la iniciativa, cuyo índice es `design.md` con frontmatter `type: design` más la clave `template:` copiada del `specification.md`. Todo lo demás lo define esta plantilla.

---

## 1. Ficheros de esta carpeta de plantillas

| Fichero | Qué define | Quién lo lee |
|---|---|---|
| `README.md` | **Esta guía/índice**: contrato fijo, roles, estructura de salida, contexto a cargar y la tabla de lentes. | Todos los subagentes. |
| `design-contract.md` | **Qué produce el diseño y cómo**: la estructura del `design.md` (cabecera con `**Ubicación del código:**` y `**Carpeta de tests E2E:**`, decisiones de ubicación, ficheros, piezas, trazabilidad, **estrategia de verificación**, pasos), las reglas de arquitectura del proyecto que **sí** son fijas aunque la forma sea libre, cuándo se materializan XML, y el **checklist** del diseño. | El **diseñador** (produce); el **juez** y el **enriquecedor** (criterios); el **verificador** y el **corrector** (qué debía cumplirse). |
| `tests-e2e.md` | **Parte del diseño — tests E2E**: materialización de los `ESC-NNN` en `test-e2e-desc.md`, con el campo `Manual` para los escenarios no automatizables, la numeración desde la carpeta destino, la plantilla y el checklist. | El **diseñador** (produce); el **verificador** (formato y cobertura); el **corrector** (si un fallo lo afecta). |
| `tests-unitarios.md` | **Parte del diseño — tests unitarios (TDD)**: qué piezas testear, qué cuenta como justificación válida para no testear una, mocking del stack, plantilla de `test-unit-desc.md`, checklist y comprobaciones de coherencia. | El **test-unitarios** (produce); el **verificador-test-unitarios** (§7); el **corrector-test-unitarios**. |
| `validacion.md` | **Reglas de verificación**: validación mecánica de los XML que el diseño materialice (si los hay) y las comprobaciones semánticas: estructura, cobertura, arquitectura, **estrategia de verificación justificada**, seguridad, coherencia con el árbol real y con las guías. | El **verificador** (aplica); el **corrector** (por qué cada cosa es un fallo). |

No hay `validate.sh`: una feature libre puede no materializar ningún XML. Si lo hace, `validacion.md` §1 dice cómo validarlo con `xmllint` directamente.

---

## 2. Tareas de los nueve roles

Los nueve reciben las **mismas rutas de entrada** (este `README.md`, el `specification.md` y, si existe, `design-guidelines.md`; el **enriquecedor** recibe además `log_best.txt`), pero **cada rol hace una tarea distinta y lee un subconjunto distinto de los ficheros de esta carpeta**. El contrato de tokens y la orquestación los fija el skill.

> **Común a los nueve:** **MUST** leer este `README.md` y seguir desde él a los ficheros que su tarea necesite. **MUST NOT** copiar ningún bloque explicativo de la plantilla al `design.md`. **MUST NOT** usar `AskUserQuestion`.

**Resumen por rol:**

| Rol | Escenario — qué hace | Entrada propia | Lee de esta plantilla | Resultado |
|---|---|---|---|---|
| **Diseñador** (§2.1) | **Crea** un diseño desde cero | spec (+ guías) + contexto del proyecto (§4) | `design-contract.md` + `tests-e2e.md` | la carpeta `design_<n>/` completa |
| **Juez** (§2.2) | **Elige** entre dos diseños, detallando ventajas **y defectos** de cada uno | las dos carpetas `design_<n>/` | `design-contract.md` (criterios) | el ganador + ventajas/defectos de cada uno |
| **Enriquecedor** (§2.5) | **Detecta** ventajas de los descartados que faltan y defectos del ganador que persisten | `design/` ganadora + `log_best.txt` | `design-contract.md` | la lista de mejoras (no las aplica) |
| **Crítico** (§2.9), uno por lente | **Critica** la calidad con una sola lente | `design/` ganadora + el rasero de su lente | `design-contract.md` | la lista de críticas (no las aplica) |
| **Verificador** (§2.3) | **Busca problemas** de cumplimiento | `design/` ganadora | `validacion.md`; consulta `design-contract.md` / `tests-e2e.md` | la lista de fallos, o conforme |
| **Corrector** (§2.4) | **Corrige** fallos, **incorpora** mejoras y **aplica** críticas | `design/` + la lista | `validacion.md` + `design-contract.md`; `tests-e2e.md` si afecta a `test-e2e-desc.md` | `design/` corregida en sitio |
| **test-unitarios** (§2.6) | **Describe** los tests unitarios de las piezas con lógica | `design/` (sobre todo `design.md`) | `tests-unitarios.md` | `design/test-unit-desc.md` |
| **verificador-test-unitarios** (§2.7) | **Comprueba** la coherencia de `test-unit-desc.md` con el diseño | `test-unit-desc.md` + `design.md` | `tests-unitarios.md` §7 | la lista de incoherencias, o conforme |
| **corrector-test-unitarios** (§2.8) | **Corrige** `test-unit-desc.md` | `test-unit-desc.md` + la lista | `tests-unitarios.md` | `test-unit-desc.md` corregido |

### 2.1 Diseñador — crea un diseño

**Tarea:** producir un **diseño completo y autosuficiente** en `design_<n>/` a partir del `specification.md` (y las guías), siguiendo `design-contract.md` al pie de la letra.

- **Lee de esta plantilla:** `design-contract.md` (estructura, reglas fijas, estrategia de verificación, pasos y **checklist**) y `tests-e2e.md` (siempre que la spec tenga escenarios, que es lo normal).
- **Carga además** el contexto de §4.
- **Produce:** `design.md`, `test-e2e-desc.md` y, **solo si la solución los necesita**, los ficheros XML materializados que `design-contract.md` §4 permite (dominios, vistas, menús, `data-init`).
- Las decisiones de ubicación son obligatorias: `design-contract.md` §3 `## 2` y §8.
- **MUST NOT** dar el diseño por terminado sin pasar el checklist de `design-contract.md` §9. **MUST NOT** inventar funcionalidad que la spec no pide.

### 2.2 Juez — elige entre dos diseños

**Tarea:** dadas **dos** carpetas de diseño completas, decidir **cuál cumple mejor** la spec, las guías y `design-contract.md`, detallando ventajas y defectos de **cada uno**.

- **Lee de esta plantilla:** `design-contract.md` — los criterios (cobertura, ubicación justificada, reglas fijas de arquitectura, estrategia de verificación completa y justificada, superficie mínima).
- **En una feature libre pesa especialmente**: (1) que la ubicación del código respete `agent_docs/architecture.md` y no engorde `subsystem/tramitador` ni acople `base` a los sistemas; (2) que la estrategia de verificación no se escaquee (una justificación de «no se puede testear» que en realidad sí se puede es un defecto grave); (3) que no reinvente lo que `base/infrastructure` o un sistema ya tienen.
- **MUST NOT** ejecutar la validación ni modificar ningún diseño. Si ambos son deficientes, elige el menos malo.

### 2.3 Verificador — busca problemas en un diseño

**Tarea:** revisar `design/` y **reportar todos los fallos de cumplimiento**; si no hay nada, declararlo conforme.

- **Lee de esta plantilla:** `validacion.md` — la lista de qué cuenta como fallo. Si el diseño materializa XML, **ejecuta tú mismo** la validación `xmllint` de `validacion.md` §1. Para saber qué debería existir, consulta `design-contract.md` y `tests-e2e.md`.
- **Solo cumplimiento:** **MUST NOT** reportar olores ni preferencias; la calidad es de los críticos.
- **MUST NOT** corregir nada.

### 2.4 Corrector — corrige un diseño

**Tarea:** aplicar en sitio sobre `design/` la lista de fallos del verificador, de mejoras del enriquecedor o de críticas de los críticos, sin regenerar el diseño.

- **Lee de esta plantilla:** `validacion.md` y `design-contract.md`; `tests-e2e.md` solo si un fallo afecta a `test-e2e-desc.md`.
- **Corrige sin complicar:** **MUST NOT** resolver un problema añadiendo piezas si se resuelve modificando lo que hay. Si dos críticas se contradicen, aplica la que deja menos cosas que recordar.
- **MUST NOT** renombrar ni mover `design/`, ni regenerarlo desde la spec.

### 2.5 Enriquecedor — incorpora ventajas de los descartados y sanea los defectos del ganador

**Tarea:** con `log_best.txt`, decidir (a) qué ventajas de los descartados faltan en el ganador y procede incorporar, y (b) qué defectos que el juez atribuyó al propio ganador siguen presentes; **reportarlos**, no aplicarlos.

- **Lee de esta plantilla:** `design-contract.md` (criterios).
- **MUST NOT** modificar el diseño ni cargar el contexto de §4.

### 2.6 test-unitarios — describe los tests unitarios (TDD)

**Tarea:** describir en `design/test-unit-desc.md` los tests unitarios de **cada pieza con lógica** del diseño, **antes de que exista el código** (es la fase «red» del TDD: `/sdd-implementer` genera estos tests **antes** que el código de producción de cada pieza).

- **Lee de esta plantilla:** `tests-unitarios.md`.
- **Entrada propia:** `design/` (el inventario de piezas y su «Estrategia de verificación») y el `specification.md` (mensajes exactos).
- Qué piezas testear y cuáles no: `tests-unitarios.md` §1.
- **MUST NOT** escribir código Java: solo descripción.

### 2.7 verificador-test-unitarios — comprueba la coherencia de los tests unitarios

**Tarea:** revisar `test-unit-desc.md` y **reportar incoherencias** con el diseño (`tests-unitarios.md` §7). **MUST NOT** modificar nada.

### 2.8 corrector-test-unitarios — corrige los tests unitarios

**Tarea:** aplicar en sitio las incoherencias reportadas sobre `test-unit-desc.md`, ajustándose a `tests-unitarios.md`. La fuente de verdad es `design.md`; **MUST NOT** tocarlo.

### 2.9 Crítico — critica el diseño con una sola lente

**Tarea:** el skill lanza **un crítico por cada lente** de la tabla, en paralelo, antes del verificador. Cada crítico mira `design/` con **su lente y solo la suya**, carga **entero** el rasero de su fila y reporta lo que un revisor senior especializado no dejaría pasar. El verificador comprueba que el diseño **cumple**; el crítico, que sea **bueno**.

**Tabla de lentes** (contrato con el skill, que extrae de aquí los nombres; **MUST** conservar la primera columna `Lente`):

| Lente | Rasero que carga | Qué busca |
|---|---|---|
| `solid` | El skill `k-code-quality` **entero**; el peso está en sus reglas de **clases (SOLID)**, de **métodos**, en las **convenciones del proyecto** y en los **olores de diseño**. | En las piezas Java/Kotlin que el `design.md` describe:<br>- una clase con más de una razón para cambiar;<br>- un método con varios pasos sin descomponer o que mezcla cálculo puro y efectos;<br>- una rama por tipo que habrá que reabrir;<br>- una decisión con varios dueños;<br>- una regla que decide por dentro si aplica;<br>- un retorno defensivo que delega;<br>- dos piezas que se conocen entre sí;<br>- lógica de negocio en un controlador o en una vista;<br>- una dependencia de un concreto donde cabía una abstracción;<br>- una colaboradora que falta o que sobra. |
| `simplicidad` | **El resto del proyecto**, no el encargo: el código real de §4.2 —`base/`, los sistemas y subsistemas que la feature toca y sus hermanos— más el `CLAUDE.md` del proyecto, `agent_docs/architecture.md` y los `CLAUDE.md` de `subsystem/tramitador`, `subsystem/expedientes` y `tramites/util`. | - Piezas que la spec **no pide**;<br>- **reinvención** de algo que ya existe en `base/infrastructure`, en `tramites/util` o en un sistema;<br>- **divergencia** — resolver de otra forma lo que el proyecto ya resuelve, sin declararlo patrón nuevo en `decisiones.md`;<br>- **indirección gratuita** (interfaz con una sola implementación que no es un `ModelService`, colaboradora de un solo uso, capas que solo delegan, `Provider` o módulo Guice innecesarios);<br>- **ubicación** — una pieza genérica enterrada en un sistema, una pieza de un sistema metida en `base`, o cualquier cosa en `subsystem/tramitador` que no sea del motor;<br>- una alternativa más simple descartada en `decisiones.md` por un motivo que no se sostiene.<br>Pregunta final obligatoria: **¿se consigue lo mismo con la mitad de piezas?** |
| `skills` | Los skills de §4.1 que apliquen al contenido del diseño, **enteros**: siempre `k-secure-coding` y `k-code-quality`; `k-sistemas`, `k-vistas`, `k-validaciones`, `k-guice`, `k-scheduler`, `k-datainit`, `k-i18n`, `k-tramite`/`k-tipo-expediente` **solo** si el diseño toca esa parte. Más el `CLAUDE.md` del proyecto y `agent_docs/architecture-rules.md`. | Cada regla `MUST`/`MUST NOT`/`CRITICAL`/`REQUIRED` de esos skills y de `architecture-rules.md` contrastada contra el diseño:<br>- un patrón seguido a medias;<br>- un nombre, ubicación o firma que el skill fija y el diseño altera;<br>- un campo `servidor` que el cliente puede dictar o un `AllowProperties` que falta;<br>- `AuthUtils.getUser()` en vez de `SecurityUtil.getUser()`;<br>- el multicentro o la i18n resueltos de una forma que `CLAUDE.md` prohíbe;<br>- una dependencia entre capas que `architecture-rules.md` veta (`base` → sistemas, `expedientes` → `tramitador`).<br>El `origen` **MUST** citar `<skill>/<fichero> § <sección>` o la regla `C-N`. |
| `verificacion` | Esta plantilla: `design-contract.md` §3 `## 6` («Estrategia de verificación»), `tests-unitarios.md` §1 y `tests-e2e.md`; más `k-code-quality/tests.md` (cómo se escriben los tests) y el `playwright.config.ts` del proyecto. | Que la estrategia de verificación sea **real y honesta**, no un trámite:<br>- una pieza con lógica marcada «sin test» con una justificación que no se sostiene (p. ej. «es trivial», «es difícil de mockear» cuando el stack ya se mockea en el proyecto);<br>- un escenario marcado manual que sí se podría automatizar;<br>- un `ESC-` sin `T-NNN`;<br>- un `T-NNN` cuyo resultado esperado no comprueba lo que el escenario afirma;<br>- tests unitarios previstos que repetirían el valor del código en vez de comprobar el comportamiento;<br>- lógica colocada donde **no** se puede testear unitariamente (en una vista o en un controlador) cuando podría vivir en un servicio testeable — la crítica entonces es **mover** la lógica, no renunciar al test. |

- **Lee de esta plantilla:** `design-contract.md` (para entender qué es cada pieza). **MUST NOT** ejecutar `validacion.md`: es del verificador.
- **Carga el rasero de su fila aunque sea contexto de §4**. El código real es de **solo lectura**.
- **MUST NOT** reportar fallos de cobertura de la spec ni de formato (son del verificador), ni el trabajo de otra lente.
- **MUST NOT** proponer ampliar `subsystem/tramitador` ni `subsystem/expedientes`, ni añadir piezas que la spec no pide: una crítica quita, funde, mueve o sustituye.
- **MUST NOT** modificar nada.

---

## 3. Estructura de salida `design/`

```
.sdd/drafts/YYYY-MM-DD_HH-MM_{resumen}/   ← carpeta de la iniciativa
├── specification.md                      ← input (type: specification)
├── anexo-*.md                            ← input, si la spec los tiene
├── design-guidelines.md                  ← opcional (input)
└── design/                               ← salida del skill
    ├── design.md                         ← índice (type: design) — SIEMPRE
    ├── decisiones.md                     ← decisiones del diseñador — lo declara el motor, no esta plantilla
    ├── test-e2e-desc.md                  ← SIEMPRE que la spec tenga escenarios (tests-e2e.md)
    ├── test-unit-desc.md                 ← lo produce el rol test-unitarios (tests-unitarios.md)
    ├── domains/<Entidad>.xml             ← SOLO si la solución crea o modifica entidades (design-contract.md §4)
    ├── views/<Fichero>.xml               ← SOLO si la solución crea o modifica vistas
    ├── menus.xml                         ← SOLO si la solución añade menús
    └── data-init/…                       ← SOLO si la solución necesita datos iniciales
```

Esta estructura es la que consumen `/sdd-implementer`, `/sdd-debug-with-test-e2e-desc`, `/sdd-create-tests-e2e` y `/sdd-close`: **MUST** producirse tal cual. `decisiones.md` y los `log_*.txt` los pone el motor: el verificador **MUST NOT** reportarlos como sobrantes.

**Los dos datos de los que viven los skills de aguas abajo** van en la cabecera del `design.md` (`design-contract.md` §2): `**Ubicación del código:**` (de ahí deriva el descomponedor de `/sdd-implementer` las rutas) y `**Carpeta de tests E2E:**` (de ahí resuelve `/sdd-create-tests-e2e` dónde persistir los tests). Sin ellos ambos abortan.

---

## 4. Contexto del proyecto a cargar (Fase 1)

Lo carga el **diseñador**; cada **crítico** solo lo que declare su fila (§2.9).

### 4.1 Skills técnicos

- **Siempre** `k-code-quality` — calidad de Java/Kotlin y, en su `tests.md`, cómo se escriben los tests sin copiar los valores del código.
- **Siempre** `k-secure-coding` — frontera de confianza, mass-assignment, `AllowProperties`, multi-centro/IDOR, secretos. Una feature transversal suele tocar justo lo que este skill protege.
- **Según lo que la feature toque** (léelo de «Contexto y alcance» de la spec y confírmalo explorando §4.2): `k-sistemas` (servicios, controladores, modelos), `k-vistas` (vistas, menús, acciones), `k-validaciones` (reglas), `k-guice` (DI no trivial), `k-scheduler` (jobs), `k-datainit` (datos iniciales), `k-i18n` (textos), `k-tramite`/`k-tipo-expediente` (si roza un trámite — con la prohibición de §4.2 sobre el motor).
- **MUST NOT** cargar `k-seguridad` (OBSOLETO). Para roles/permisos, leer `src/main/java/com/educaflow/subsystem/security/`.
- **MUST NOT** cargar todos «por si acaso»: carga los que la feature necesita y **anota en `decisiones.md`** cuáles y por qué, para que el crítico `skills` pueda contrastarlo.

### 4.2 Código existente a explorar

- `CLAUDE.md` del proyecto y `agent_docs/architecture.md` + `agent_docs/architecture-rules.md` — las capas, qué puede depender de qué, y las invariantes que los tests ArchUnit ya verifican. **Una feature libre es la que más fácilmente las rompe.**
- Los `CLAUDE.md` de `src/main/java/com/educaflow/subsystem/tramitador/`, `subsystem/expedientes/` y `tramites/util/` — dónde va cada cosa si la feature roza los expedientes.
- `src/main/java/com/educaflow/base/` entero — es donde suele acabar lo transversal; **MUST** conocer qué hay ya (`infrastructure/`, `util/`, …) para no reinventarlo y para decidir si la feature va ahí.
- Las carpetas de **cada parte afectada** que declara «Contexto y alcance» de la spec: léelas a fondo, son la base de toda fila `Modificar`.
- `src/test/java/com/educaflow/` — cómo se escriben los tests unitarios del proyecto (JUnit 5 + Mockito, `JUnitHelper`, `mockStatic` de `SecurityUtil`) y qué tests de arquitectura/vistas/orden de métodos existen y la feature debe seguir cumpliendo.
- `src/test/e2e/` — los tests E2E persistidos: para elegir la carpeta destino (`design-contract.md` §2) y numerar los `T-NNN` desde el primer libre (`tests-e2e.md` §1).
- **MUST NOT** proponer crear ni modificar nada dentro de `subsystem/tramitador` salvo que la feature sea **del motor mismo** y así lo justifique `decisiones.md`; lo que un expediente necesita va a `tramites/util/` o a su carpeta de versión (ver sus `CLAUDE.md`).
- **MUST NOT** usar `design.md` de otras iniciativas como plantilla, **salvo lectura** de las archivadas que `design-guidelines.md` cite.
