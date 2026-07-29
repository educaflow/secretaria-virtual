# Plantilla de diseño de un sistema — guía e índice

Esta carpeta de plantillas define **todo lo específico de convertir una especificación funcional en un DISEÑO de un sistema/subsistema** del proyecto EducaFlow. El skill `sdd-designer` aporta solo el **flujo** (localizar la spec, decidir modo, lanzar diseñadores en paralelo, elegir el mejor con un juez, verificar/corregir, cerrar) y es **agnóstico**: no sabe nada de cómo es el diseño. Lo lee todo de aquí.

Este `README.md` es **el único fichero que el skill conoce por nombre**. **Lo leen los ocho subagentes** y, según su rol, cada uno tiene una tarea distinta sobre el mismo diseño:

- **diseñador** — **crea** un diseño desde cero (§2.1).
- **juez** — **elige** entre dos diseños cuál es mejor, detallando las ventajas **y los defectos/errores** de cada uno (§2.2).
- **enriquecedor** — **detecta** qué ventajas de los diseños descartados conviene incorporar al ganador y qué **defectos que el juez detectó en el propio ganador** siguen presentes para sanearlos (§2.5).
- **verificador** — **busca problemas** en un diseño y los reporta (§2.3).
- **corrector** — **corrige** los problemas que el verificador reportó e **incorpora** las mejoras que el enriquecedor propuso (§2.4).
- **test-unitarios** — **describe** los tests unitarios (JUnit + Mockito) de las clases Java del diseño en `test-unit-desc.md` (§2.6).
- **verificador-test-unitarios** — **comprueba** que `test-unit-desc.md` es coherente con el diseño y reporta las incoherencias (§2.7).
- **corrector-test-unitarios** — **corrige** en `test-unit-desc.md` las incoherencias reportadas (§2.8).

A través de este README cada subagente descubre y lee **solo los ficheros de esta carpeta que su tarea necesita** (§2). **MUST NOT** copiarse ningún bloque explicativo de esta plantilla al `design.md` de salida.

> **Contrato fijo (lo garantiza el skill, no lo cambia esta plantilla):** la entrada es `specification.md` (`type: specification`) y la salida es una **carpeta `design/`** dentro de la carpeta de la iniciativa, cuyo índice es `design.md` con frontmatter `type: design`. Todo lo demás (el resto de la estructura de `design/`, la conversión técnica, las pasadas, la validación) lo define esta plantilla.

---

## 1. Ficheros de esta carpeta de plantillas

El skill abre `README.md` y, a través de él, usa los demás. Cada uno se transmite al subagente o fase que lo necesita.

| Fichero | Qué define | Quién lo lee |
|---|---|---|
| `README.md` | **Esta guía/índice**: contrato fijo, estructura de salida, contexto del proyecto a cargar, y las partes que el diseño incluye. | Todos los subagentes (es el contrato que el skill nombra). **MUST NOT** copiarse al output. |
| `design-contract.md` | **Qué produce el diseño y cómo**: estructura del diseño, conversión spec→V/R/U, clasificación `cliente`/`servidor`, cobertura, mapeo de capas, reglas arquitectónicas, reglas para los pasos y su orden, cómo documentar servicios y campos `servidor`, frontera de confianza `AllowProperties`, el checklist del diseño y la estructura del `design.md`. | El **diseñador** (para producir el diseño); el **juez** (para comparar); el **enriquecedor** (como criterios para decidir si una ventaja procede); el **verificador** (para verificar) y el **corrector** (para saber a qué regla ajustar la corrección). |
| `vistas.md` | **Parte del diseño — vistas**: el contrato de `views/*.xml` + `menus.xml` — reglas de creación (un `<action-view>` por fichero, menús en fichero único, PI `sv-*`, `buttons-panel`, validación remota global, forms modales, maquetación **ASCII Layout**), su checklist y las comprobaciones/detectores de verificación, incluida la **auditoría de layout**. La técnica vive en el skill `k-vistas`. | El **diseñador** (produce las vistas cumpliéndolo); el **juez** y el **enriquecedor** (criterios sobre vistas); el **verificador** (aplica su §3, invocado desde `validacion.md` §2.f); el **corrector** (solo si un fallo/mejora afecta a una vista). |
| `reglas-complejas.md` | **Parte del diseño — reglas complejas**: criterios de "regla compleja" y formato del fichero `rules/R-*.md`. | El **diseñador** (lo produce como parte del diseño); el **verificador** (comprueba el formato); el **corrector** (solo si un fallo reportado afecta a una `rules/R-*.md`). |
| `tests-e2e.md` | **Parte del diseño — tests E2E**: reglas de materialización, plantilla de `design/test-e2e-desc.md` y checklist. | El **diseñador** (lo produce); el **verificador** (comprueba el formato); el **corrector** (solo si un fallo reportado afecta a `test-e2e-desc.md`). |
| `tests-unitarios.md` | **Parte del diseño — tests unitarios**: qué clases Java testear, estrategia de mocking del stack (Axelor/Guice/JPA), plantilla de `design/test-unit-desc.md`, trazabilidad, checklist y las **comprobaciones de coherencia con el diseño**. | El **test-unitarios** (lo produce); el **verificador-test-unitarios** (aplica las comprobaciones de coherencia §7); el **corrector-test-unitarios** (para ajustar la corrección al contrato). |
| `validacion.md` | **Reglas de verificación**: la validación XML mecánica (delegada en `validate.sh`) y las comprobaciones semánticas de coherencia/cobertura/seguridad. | El **verificador** (aplica las comprobaciones semánticas §2); el **corrector** (para entender por qué cada cosa es un fallo y cuál es la corrección esperada). |
| `validate.sh` | **Script de validación mecánica**: valida cada XML del diseño contra su XSD de AOP con `xmllint`; imprime `FAIL: <fichero>` y termina con código `≠0` si algo no valida. | **Lo ejecuta el verificador** con `Bash` (lo prescribe `validacion.md` §1). **MUST NOT** copiarse al output. |

---

## 2. Tareas de los ocho subagentes

El skill `sdd-designer` lanza estos ocho roles (diseñador, juez, enriquecedor, verificador, corrector, test-unitarios, verificador-test-unitarios, corrector-test-unitarios). Los ocho reciben las **mismas rutas de entrada** (este `README.md`, el `specification.md` y, si existe, `design-guidelines.md`; el **enriquecedor** recibe además el log `log_best.txt`), pero **cada rol hace una tarea distinta y lee un subconjunto distinto de los ficheros de esta carpeta**. Lo que sigue acota, por rol, **qué hace** y **qué ficheros de esta plantilla le aplican**. El contrato de tokens y la orquestación los fija el skill; aquí solo se delimita el trabajo de cada uno.

> **Común a los ocho:** **MUST** leer este `README.md` y seguir desde él a los ficheros que su tarea necesite. **MUST NOT** copiar ningún bloque explicativo de la plantilla al `design.md`. **MUST NOT** usar `AskUserQuestion`.

**Resumen por rol** (el detalle de cada uno en §2.1–§2.8):

| Rol | Escenario — qué hace | Entrada propia | Lee de esta plantilla | Resultado |
|---|---|---|---|---|
| **Diseñador** (§2.1) | **Crea** un diseño desde cero | spec (+ guías) + contexto del proyecto (§4) | `design-contract.md` + `vistas.md`; `reglas-complejas.md` / `tests-e2e.md` solo si aplican (§5) | la carpeta `design_<n>/` completa |
| **Juez** (§2.2) | **Elige** entre dos diseños, detallando ventajas **y defectos** de cada uno | las dos carpetas `design_<n>/` a comparar | `design-contract.md` + `vistas.md` (como criterios) | el diseño ganador + ventajas/defectos de cada uno (no lo modifica) |
| **Enriquecedor** (§2.5) | **Detecta** qué ventajas de los descartados incorporar al ganador **y qué defectos del propio ganador sanear** | la carpeta `design/` ganadora + `log_best.txt` | `design-contract.md` (+ `vistas.md` si la ventaja/defecto afecta a vistas) | la lista de mejoras a aplicar (no las aplica) |
| **Verificador** (§2.3) | **Busca problemas** en un diseño | la carpeta `design/` ganadora | `validacion.md` + `vistas.md` §3; consulta `design-contract.md` / `reglas-complejas.md` / `tests-e2e.md` para saber qué *debería* existir | la lista de fallos, o conforme (no corrige) |
| **Corrector** (§2.4) | **Corrige** los problemas reportados e **incorpora** las mejoras del enriquecedor | la carpeta `design/` + la lista de fallos/mejoras | `validacion.md` + `design-contract.md`; `vistas.md` / `reglas-complejas.md` / `tests-e2e.md` solo si un fallo/mejora afecta a esa parte | la carpeta `design/` corregida/enriquecida en sitio |
| **test-unitarios** (§2.6) | **Describe** los tests unitarios de las clases Java del diseño | la carpeta `design/` (sobre todo `design.md`) | `tests-unitarios.md` (qué testear, mocking, plantilla, cobertura) | `design/test-unit-desc.md` (solo descripción, sin código) |
| **verificador-test-unitarios** (§2.7) | **Comprueba** que `test-unit-desc.md` es coherente con el diseño | `design/test-unit-desc.md` + `design.md` (fuente de verdad) | `tests-unitarios.md` §7 (comprobaciones de coherencia) | la lista de incoherencias, o conforme (no corrige) |
| **corrector-test-unitarios** (§2.8) | **Corrige** las incoherencias de `test-unit-desc.md` | `design/test-unit-desc.md` + la lista de incoherencias | `tests-unitarios.md` (plantilla/contrato) | `design/test-unit-desc.md` corregido en sitio |

Solo el **diseñador** carga el contexto del proyecto de §4; los otros siete trabajan sobre lo que ya está en disco (el **test-unitarios** y su verificador pueden consultar el código real: el **test-unitarios** para clases que el diseño *modifica* o utilidades a mockear).

### 2.1 Diseñador — crea un diseño

**Tarea:** producir un **diseño completo y autosuficiente** en su carpeta `design_<n>/`, a partir del `specification.md` (y las guías, si existen), siguiendo el contrato al pie de la letra.

- **Lee de esta plantilla:** `design-contract.md` (qué producir, cómo convertir el spec, cómo estructurar, el orden de los pasos y el **checklist**, que aplica antes de terminar); `vistas.md` (el contrato de las vistas — **siempre**, todo diseño tiene vistas — incluido su checklist §2); `reglas-complejas.md` y `tests-e2e.md` **solo cuando aplican** (§5).
- **Carga además** el contexto del proyecto de §4 (skills técnicos + código existente): es el único rol que lo necesita para producir.
- **Produce:** todos los ficheros de §3 — `design.md`, `domains/*.xml`, `views/*.xml`, `menus.xml`, y las partes condicionales (`rules/R-*.md`, `test-e2e-desc.md`) cuando apliquen.
- **MUST NOT** dar el diseño por terminado sin pasar los checklists de `design-contract.md` §9 y `vistas.md` §2. **MUST NOT** inventar elementos que no estén en el spec.

### 2.2 Juez — elige entre dos diseños

**Tarea:** dadas **dos** carpetas de diseño completas, decidir **cuál cumple mejor** la especificación, las guías y las reglas de la plantilla, y devolver el ganador, **detallando las ventajas y los defectos/errores concretos de CADA uno** (el formato exacto de la respuesta lo fija el skill).

- **Lee de esta plantilla:** `design-contract.md` y `vistas.md` — son los **criterios** que un buen diseño debe cumplir (cobertura, taxonomía, clasificación de campos, reglas arquitectónicas, frontera de confianza, contrato de vistas y calidad del layout **ASCII Layout**). Son el rasero tanto para las ventajas como para los defectos de cada diseño.
- **Detalla los defectos de los DOS diseños, no solo del perdedor:** los defectos que reportes del diseño que acabe ganando se auditarán después (el enriquecedor, §2.5, los corrige sobre el ganador). Sé concreto: qué punto del spec/guías/reglas incumple o cubre peor, qué regla/escenario falta.
- **NO** ejecuta la validación de `validacion.md` (eso es del verificador) ni carga el contexto del proyecto de §4: compara los dos diseños tal y como están en disco.
- **MUST NOT** modificar, completar ni corregir ninguno de los dos diseños: su única salida es **elegir uno** y exponer ventajas/defectos de ambos. Si ambos son deficientes, elige el menos malo (el enriquecedor y el bucle de verificación posteriores corrigen el ganador).

### 2.3 Verificador — busca problemas en un diseño

**Tarea:** revisar la carpeta `design/` (el ganador ya seleccionado) y **reportar todos los fallos** de la forma más clara posible (qué falla, en qué fichero, por qué); si no hay nada que corregir, declararlo conforme.

- **Lee de esta plantilla:** `validacion.md` — es **la lista de qué cuenta como fallo**. **Ejecuta tú mismo** (con `Bash`) el script `validate.sh` que prescribe `validacion.md` §1 para la validación XML mecánica contra los XSD, y aplica además las **comprobaciones semánticas** de cobertura, coherencia y seguridad (`validacion.md` §2) — las de vistas están delegadas en `vistas.md` §3, incluida la **auditoría de layout (ASCII Layout)** de cada `<form>`. Para saber qué *debería* existir, consulta `design-contract.md` y `vistas.md`, y `reglas-complejas.md` / `tests-e2e.md` para las partes condicionales que el spec exija.
- **Comprueba** que las partes que **deben** existir (§5) existen, y que cada artefacto valida.
- **MUST NOT** corregir nada: solo **detecta y reporta**. Corregir es tarea del corrector (§2.4).

### 2.4 Corrector — corrige un diseño

**Tarea:** dada la lista de fallos del verificador (§2.3) o la lista de mejoras del enriquecedor (§2.5), **aplicarla en sitio** sobre la carpeta `design/` (`Edit`/`Write` sobre sus ficheros), sin regenerar el diseño.

- **Lee de esta plantilla:** `validacion.md` (para entender por qué cada cosa es un fallo y cuál es la corrección esperada) y `design-contract.md` (las reglas a las que debe ajustar la corrección o la mejora); además `vistas.md`, `reglas-complejas.md` o `tests-e2e.md` **solo si** un fallo/mejora afecta a una vista, a una `rules/R-*.md` o a `test-e2e-desc.md` respectivamente (para conocer la regla o el formato correcto al que ajustar el cambio).
- **Aplica** exactamente los fallos/mejoras reportados, manteniendo la estructura y las decisiones del diseño que no estén en falta.
- **MUST NOT** renombrar ni mover la carpeta `design/`. **MUST NOT** regenerar el diseño entero ni reconstruirlo desde el spec: solo aplica los cambios puntuales.

### 2.5 Enriquecedor — incorpora ventajas de los descartados y sanea los defectos del ganador

**Tarea:** dado el ganador ya seleccionado y el log `log_best.txt` (las ventajas **y los defectos** que el juez atribuyó a cada diseño comparado, incluidos los **descartados** y el **propio ganador**), decidir **(a)** qué ventajas de los descartados conviene incorporar al ganador y **(b)** qué **defectos que el juez atribuyó al propio ganador siguen presentes** en él, y **reportar** unas y otros (no los aplica; los aplica el corrector, §2.4).

- **Lee de esta plantilla:** `design-contract.md` — son los **criterios** para juzgar si una ventaja ya está cubierta y si tiene sentido aplicarla, y para juzgar si un defecto del ganador es real y persiste (cobertura, taxonomía, clasificación de campos, reglas arquitectónicas, frontera de confianza), igual que el juez — y `vistas.md` cuando la ventaja/defecto afecta a una vista.
- **Entrada propia:** la carpeta `{iniciativa}/design` (el ganador) y `{iniciativa}/design/log_best.txt`. Los diseños descartados ya no están en disco: la fuente de sus ventajas es `log_best.txt`.
- **(a) Ventajas de los descartados:** para **cada ventaja** de un descartado en el log, comprueba si **ya existe** en el ganador y si **tiene sentido aplicarla** (coherente con spec/guías/reglas, sin contradecir las decisiones del ganador). Reporta **solo** las que faltan **y** procede incorporar.
- **(b) Defectos del propio ganador:** para **cada defecto** que el juez atribuyó al **diseño que ganó** (bloques `=== DEFECTOS <ganador> ===` del log), comprueba **en la carpeta `design/` actual** si **sigue presente**. Reporta **solo** los que **persisten** y procede corregir. **Ignora** los defectos de los diseños **descartados** (murieron con su diseño) y los marcados `Ninguno detectado`.
- **MUST NOT** modificar el diseño ni cargar el contexto del proyecto de §4: solo **detecta y reporta** sobre lo que ya está en disco.

### 2.6 test-unitarios — describe los tests unitarios

**Tarea:** describir en `design/test-unit-desc.md` los **tests unitarios** (JUnit 5 + Mockito) necesarios para las clases Java del diseño. **Solo descripción, sin código** (el código lo genera `/sdd-implementer` a partir de `test-unit-desc.md`).

- **Lee de esta plantilla:** `tests-unitarios.md` — define qué clases testear, la **estrategia de mocking** del stack (Axelor/Guice/JPA), la **plantilla** de `test-unit-desc.md`, la trazabilidad y el checklist.
- **Entrada propia:** la carpeta `{iniciativa}/design` (sobre todo `design.md`, de donde sale el **inventario de clases Java**, sus métodos y las reglas `V`/`R`/`CC` que aplican) y el `specification.md` (para los mensajes/semántica exactos).
- **CRITICAL:** en esta fase **aún no existe el código Java** del sistema; enumera las clases **desde el diseño**, no del árbol de fuentes. Para clases que el diseño **modifica** (ya existentes) o utilidades/bases a mockear, puede consultar el código real.
- **MUST NOT** escribir código Java (ni `@Test`, ni imports, ni cuerpos): solo la descripción de cada test (nombre, propósito, mocks, acción, aserción/mensaje, regla `V`/`R`/`CC` que verifica).

### 2.7 verificador-test-unitarios — comprueba la coherencia de los tests unitarios

**Tarea:** revisar `design/test-unit-desc.md` (ya escrito por el `test-unitarios`) y **reportar todas las incoherencias** respecto al diseño; si todo cuadra, declararlo conforme. **MUST NOT** regenerar ni completar los tests: solo **detecta y reporta**.

- **Lee de esta plantilla:** `tests-unitarios.md` — en particular su sección de **comprobaciones de coherencia con el diseño** (§7): clases descritas que existen en el diseño, métodos que existen, reglas `V`/`R`/`CC` referenciadas que existen, cobertura declarada que cuadra, sin clases ni métodos inventados, estructura de plantilla respetada y sin código Java.
- **Entrada propia:** `{iniciativa}/design/test-unit-desc.md` (lo que se verifica) y `{iniciativa}/design` —sobre todo `design.md`— como **fuente de verdad** del inventario de clases/métodos/reglas.
- **MUST NOT** modificar nada: corregir es tarea del corrector-test-unitarios (§2.8). **MUST NOT** modificar el diseño para que cuadre con los tests.

### 2.8 corrector-test-unitarios — corrige los tests unitarios

**Tarea:** dada la lista de incoherencias del verificador-test-unitarios (§2.7), **aplicarla en sitio** sobre `design/test-unit-desc.md` (`Edit`/`Write`), sin regenerar el fichero.

- **Lee de esta plantilla:** `tests-unitarios.md` (la plantilla/contrato al que ajustar la corrección).
- **Fuente de verdad:** `design.md`. **MUST NOT** modificar el diseño para que cuadre con los tests: corrige los tests para que cuadren con el diseño.
- **Aplica** exactamente las incoherencias reportadas, manteniendo lo correcto. **MUST NOT** tocar otros ficheros del diseño.

---

## 3. Estructura de salida `design/`

La salida es una **carpeta** `design/` dentro de la carpeta de la iniciativa. El detalle de cada fichero está en `design-contract.md` §1.

```
.sdd/drafts/YYYY-MM-DD_HH-MM_{resumen}/   ← carpeta de la iniciativa
├── specification.md                      ← índice (type: specification) — input
├── <ficheros que enlace specification.md> ← entidades, pantallas, diagramas… — input
├── design-guidelines.md                  ← opcional (input)
└── design/                               ← salida del skill
    ├── design.md                         ← índice (type: design)
    ├── domains/<Entidad>.xml             ← un fichero por entidad
    ├── views/<Fichero>.xml               ← un fichero por <action-view>
    ├── menus.xml                         ← <menuitem> del subsistema
    ├── test-e2e-desc.md                          ← materializado desde los escenarios del spec (si los hay)
    ├── test-unit-desc.md                            ← descripción de los tests unitarios (si hay clases Java) — lo produce el rol `test-unitarios`
    └── rules/R-<Entidad>-NNN.md          ← solo si hay reglas R complejas
```

Esta estructura es la que consumen `/sdd-implementer`, `/sdd-debug-with-test-e2e-desc` y `/sdd-close`: **MUST** producirse tal cual.

---

## 4. Contexto del proyecto a cargar (Fase 1)

Lo carga el **diseñador** (§2.1) — es el único rol que lo necesita para producir. Antes de generar, carga el contexto que esta plantilla indica:

### 4.1 Skills técnicos

- **Siempre** `k-sistemas` — arquitectura de dominios, servicios, controladores; convenciones de FQN y nombres de clase.
- **Siempre** `k-validaciones` — categorías V/R/U, en qué capa va cada tipo, cómo se redactan los mensajes. **Es la referencia de la conversión** spec → V/R/U (`design-contract.md` §2).
- **Siempre** `k-code-quality` — reglas de calidad de Java/Kotlin (descomposición de métodos, responsabilidad única, nombrado, idiomas modernos, convenciones Axelor/Guice/JPA).
- **Siempre** `k-secure-coding` — frontera de confianza Axelor, mass-assignment, asignación incondicional de campos `servidor`, `AllowProperties` por acción, multi-centro/IDOR. **Determina** la clasificación `cliente`/`servidor` (`design-contract.md` §3).
- **Siempre** `k-vistas` — estructura de ficheros XML, nombres de vistas y acciones, y el patrón de **botones de formulario** (`forms.md`): el trío "botones estándar" (Guardar/Cancelar/Borrar) del spec se traduce **siempre** como el `buttons-panel` explícito de `forms.md`, **nunca** como los atributos nativos `canSave`/`canDelete`/`canBack` del `<form>` de Axelor (ver `vistas.md` §1.4).
- **Condicional** `k-guice` — cargar **solo si** el diseño necesita cablear DI no trivial: un objeto que NO es `ModelService` y cuya construcción requiere un `Provider`, binding explícito, o dependencias que vienen de configuración/runtime. Para `ModelService` **NO** hace falta (los descubre `ModelServiceFactory`; ver `design-contract.md` §6).
- **Condicional** `k-scheduler` — cargar **solo si** el spec implica una tarea recurrente (job Quartz / `MetaSchedule`).
- **MUST NOT** cargar `k-seguridad` (marcado OBSOLETO). Para roles/permisos, leer el código real de `src/main/java/com/educaflow/subsystem/security/` y apoyarse en `k-secure-coding`.

Los skills son la fuente de verdad sobre **qué piezas existen y cómo se llaman**, no sobre el código exacto que las implementa.

### 4.2 Código existente a explorar

- `CLAUDE.md` del proyecto — capas, convenciones, tipos de usuario, árbol de subsistemas existentes.
- `src/main/java/com/educaflow/subsystem/` y `src/main/java/com/educaflow/system/` — qué reutilizar (FQN, dependencias) y dependencias potenciales.
- `src/main/java/com/educaflow/base/infrastructure/` — utilidades reutilizables (PDF, mail, evaluator, etc.).
- **MUST NOT** usar como referencia el código de `expedientes`/`tramites` (siguen otra arquitectura) ni `design.md`/XML de diseños previos como plantilla.

Cada subagente que necesite este contexto (sobre todo el diseñador) lo **carga él mismo** leyendo estos skills y explorando el código real; no se le copia código, se le indica dónde mirar.

---

## 5. Partes condicionales del diseño

Además de los dominios, vistas y menús, el diseño que escribe el diseñador incluye **estas partes cuando aplican**. El contrato de cada una está en su fichero; el diseñador las produce **dentro de su carpeta `design_<n>/`** como parte del diseño completo.

| Parte | Cuándo se incluye | Contrato |
|---|---|---|
| `rules/R-<Entidad>-NNN.md` (una por regla compleja) | si alguna regla `R-` cumple los criterios de complejidad | `reglas-complejas.md` |
| `test-e2e-desc.md` (tests E2E) | si el spec tiene escenarios | `tests-e2e.md` |

Si una parte no aplica (su columna "Cuándo se incluye" no se cumple), simplemente **no se crea** (sin error). El verificador comprueba que las partes que **deben** existir, existen.

`test-unit-desc.md` (tests unitarios) **NO** lo produce el diseñador: lo genera el rol **`test-unitarios`** en una fase posterior del skill (sobre el ganador `design/`), siempre que el diseño defina clases Java — ver §2.6 y `tests-unitarios.md`.
