# Parte del diseño: tests de arquitectura (ArchUnit)

Como parte del diseño, **el subagente `test-arquitectura`** escribe `design/arch-test-desc.md`: la **descripción** (no el código) de los tests de arquitectura **ArchUnit** que verifican que las clases Java que el diseño planifica respetan la arquitectura documentada del proyecto (capas, Controller→Service→Repository, nomenclatura/ubicación, inyección de dependencias, higiene). `/sdd-implementer` genera el código de los tests a partir de esta descripción.

**CRITICAL — todavía no hay código.** Cuando se ejecuta esta fase, las clases Java del sistema **aún no existen** (las crea `/sdd-implementer`). El subagente `test-arquitectura` enumera los **paquetes y artefactos** que el diseño va a crear (controladores, servicios, impl., repositorios, módulos Guice, DTOs, entidades) **desde el diseño** (`design.md`: la sección de ficheros a crear/modificar y los FQN de cada clase), **no** del árbol de fuentes. Es un planteamiento *fitness-function*: la descripción de los tests de arquitectura se escribe **antes** del código, a partir del diseño.

**Diferencia con `unit-test-desc.md`.** Los tests unitarios (`tests-unitarios.md`) verifican el **comportamiento** de cada método (validaciones, reglas de negocio, campos calculados) con JUnit + Mockito. Los tests de arquitectura verifican la **estructura** del código (de qué depende cada capa, cómo se llaman y dónde viven las clases) con ArchUnit, sin ejecutar lógica. Son complementarios: una clase puede pasar todos sus tests unitarios y aun así violar la arquitectura (p.ej. un controlador que accede al repositorio).

**Quién lo usa** (`README.md` §2): lo produce el subagente `test-arquitectura` (§2.9); ningún otro rol lo modifica.

**REQUIRED — fuente de las reglas.** El catálogo de reglas de arquitectura del proyecto NO se inventa aquí: vive en el skill **`k-archunit`**, fichero **`secretaria-virtual-rules.md`** (reglas `C1`–`C22`, ancladas en los paquetes reales de `com.educaflow`, cada una con su ejemplo ArchUnit listo, su origen y su estado). El subagente `test-arquitectura` **MUST** cargar el skill `k-archunit` y leer `secretaria-virtual-rules.md`, y **selecciona/especializa** de ese catálogo las reglas que apliquen al diseño. **MUST NOT** redefinir una regla del catálogo con otro criterio: si una regla del catálogo aplica, se referencia por su id (`C1`…`C22`).

---

## 1. Qué reglas describir (y cuáles no)

El criterio es: **toda regla del catálogo `C1`–`C22` que afecte a algún artefacto que el diseño crea o modifica MUST aparecer descrita**, mapeada a los paquetes/clases concretos del diseño. Para decidir cuáles aplican, cruza los artefactos del diseño con el catálogo:

| Si el diseño crea/modifica… | Aplican (catálogo) |
|---|---|
| Cualquier clase en una capa (`subsystem`/`system`/…) | `C1`–`C8` (dependencias entre capas, ciclos, independencia de sistemas) — la(s) que correspondan a la capa del diseño |
| Un controlador (`..controller..`) | `C9`, `C10`, `C14`, `C15` |
| Un servicio / impl. (`..service..`, `..service.impl..`) | `C12`, `C14`, `C16`, `C17` |
| Un repositorio (`..db.repo..`) | `C11`, `C18` |
| Una entidad de dominio (`..db..`) | `C13` |
| Un módulo Guice (`..module..`) | `C19` |
| Un DTO (`*DTO`) | `C20` |
| Un campo/uso de `ModelService` | `C21` |
| Cualquier clase Java (higiene) | `C22` |

- **MUST** describir **solo** las reglas cuyo sujeto **existe** en el diseño. Si el diseño **no** tiene DTOs, **no** se describe `C20`; si no tiene módulo Guice, **no** se describe `C19`; etc. Indicar las no aplicables en la sección «Reglas del catálogo no aplicables» con su motivo (p.ej. «C20 — el diseño no define DTOs»).
- **MUST** describir las reglas de **capa** (`C1`–`C8`) que correspondan a la capa donde vive el diseño. Ejemplo: un diseño en `com.educaflow.subsystem.X` describe `C3` (no depende de `system`/`secretariavirtual`) y `C7` (sin ciclos entre subsistemas); un diseño en `com.educaflow.system.X` describe `C4` (no depende de `secretariavirtual`) y `C8` (sistemas independientes).
- **PUEDE** describir **reglas nuevas, específicas de este diseño**, cuando el spec o las guías imponen una restricción estructural que el catálogo genérico no cubre (p.ej. «el sistema `grupos` no debe depender del subsistema `firmas`»). Numéralas `A-NNN` (`A-001`, `A-002`…) para distinguirlas de las del catálogo (`C…`), y dales el mismo tratamiento (qué verifica, paquete, origen).
- **MUST NOT** describir reglas que el catálogo lista en «Reglas genéricas deliberadamente NO incluidas» ni cosas que «Fuera del alcance de ArchUnit» (mass-assignment, autorización multicentro/IDOR, validación de adjuntos…): esas se cubren con `k-secure-coding` y con los tests E2E de `tests.md`, no con ArchUnit.

## 2. Qué describir en cada test de arquitectura (y qué NO)

- **MUST NOT** escribir código Java: ni `@ArchTest`, ni `@AnalyzeClasses`, ni el cuerpo fluido de la regla (`noClasses().that()…`), ni imports. **Solo descripción.** (El catálogo `secretaria-virtual-rules.md` ya trae el código de referencia de cada `C…`; aquí solo se **describe** cuál aplica y cómo se concreta para este diseño.)
- Cada test de arquitectura se describe con estos campos:
  - **Id** — la regla del catálogo (`C1`…`C22`) o, si es específica del diseño, `A-NNN`.
  - **Nombre** — descriptivo, estilo del catálogo (p.ej. `controladorNoAccedeARepositorio`, `implServicioNombreYUbicacion`).
  - **Qué verifica** — la restricción estructural en una frase (qué está prohibido o qué se exige).
  - **Ámbito** — el/los paquete(s) concretos del diseño sobre los que aplica (FQN real, p.ej. `com.educaflow.system.grupos..`, `..grupos.controller..`).
  - **Sujetos del diseño** — las clases concretas del diseño a las que afecta (p.ej. `GrupoController`, `NotaServiceImpl`).
  - **Resultado esperado** — para código **nuevo**, **MUST** pasar (`PASS`): el diseño está obligado a cumplir la arquitectura. Si la regla aplica a una clase **ya existente** que el diseño modifica y esa clase **hoy** viola la regla (ver «Estado actual» de la regla en el catálogo), indícalo como `FREEZE` (se introduce con `FreezingArchRule.freeze(...)` para no romper el build) y anótalo como deuda.
  - **Origen** — la regla del catálogo (`C…` de `secretaria-virtual-rules.md`) y su origen documental (el skill `k-…` que la respalda), o, para `A-NNN`, el punto del spec/guías que la impone.
- **MUST** mapear cada `C…` aplicable **al menos a un** ámbito/sujeto concreto del diseño: no basta con citar la regla genérica, hay que decir sobre qué paquete/clases del diseño se comprueba.

- ✅ CORRECTO (un test descrito): **C9** `controladorNoAccedeARepositorio` — Verifica: ningún controlador depende de `..db.repo..`. Ámbito: `com.educaflow.system.grupos.controller..`. Sujetos: `GrupoController`, `NotaController`. Resultado: `PASS` (código nuevo). Origen: `C9` (catálogo) ← `k-sistemas/controladores.md`.
- ❌ INCORRECTO: pegar `noClasses().that()…` (eso es código; aquí solo se describe), citar `C9` «en general» sin decir sobre qué paquete/clases del diseño aplica, o describir una regla cuyo sujeto no existe en el diseño.

## 3. Estrategia de anclaje y ámbito (ArchUnit)

- **Una clase de test por iniciativa/sistema.** Describe una única clase de test de arquitectura para el sistema/subsistema del diseño, nombre `Arquitectura<Sistema>Test` (p.ej. `ArquitecturaGruposTest`), anclada con `@AnalyzeClasses(packages = "<paquete-raíz-del-sistema>", importOptions = ImportOption.DoNotIncludeTests.class)`. **CRITICAL**: el ámbito se acota al **paquete del propio sistema** que el diseño crea (p.ej. `com.educaflow.system.grupos`), **no** a todo `com.educaflow`: así el test falla específicamente por el código nuevo y no por violaciones preexistentes de otros subsistemas (de eso se ocupa el catálogo global).
- **Matchers relativos.** Las reglas de estructura interna del catálogo usan matchers relativos (`..controller..`, `..service.impl..`, `..db.repo..`) que, con el `@AnalyzeClasses` acotado al paquete del sistema, ya seleccionan solo las clases del diseño. Descríbelo así; no hace falta reescribir los matchers a FQN salvo en `A-NNN` específicas.
- **Reglas de capa (`C1`–`C8`) y dependencias entrantes/salientes.** Para verificar que el diseño **no** depende de capas superiores (p.ej. un `system` que no debe tocar `secretariavirtual`), la regla mira el **destino** de la dependencia, que puede estar fuera del paquete anclado; descríbelo indicando que el ámbito de origen es el paquete del sistema y el destino es la capa prohibida (FQN completo, p.ej. `com.educaflow.secretariavirtual..`). Para ciclos (`C7`) e independencia (`C8`), indica el `slices().matching(...)` del catálogo acotado al paquete del diseño.
- **Paquetes exentos.** Si el diseño **no** es de `expedientes`/`tiposexpedientes`/`tramites` (arquitectura propia, exentos en el catálogo), normalmente **no** hace falta el array `PAQUETES_EXENTOS`. Si por algún motivo el ámbito incluye uno de esos paquetes, descríbelo siguiendo el catálogo. **MUST NOT** crear exenciones nuevas para silenciar una violación del propio diseño: el código nuevo **cumple** la regla, no se exime.
- **`freeze` solo para deuda preexistente.** `FreezingArchRule.freeze(...)` se reserva para reglas que aplican a una clase **ya existente** que el diseño modifica y que **hoy** incumple (estado `❌`/`⚠️` en el catálogo). El código **nuevo** del diseño **MUST** cumplir sin `freeze`.
- **`allowEmptyShould(true)`** cuando una regla de tipo `should()` podría no tener sujetos en el ámbito acotado (p.ej. `C21` si el diseño no declara campos `ModelService`): descríbelo para que el test no falle por «no matching classes» (igual que hace el catálogo en `C21`).

## 4. Trazabilidad y cobertura

- **MUST**: cada artefacto del diseño (controlador, servicio, impl., repositorio, módulo, DTO, entidad) está cubierto por **al menos una** regla de arquitectura aplicable (las de su tipo según §1).
- **MUST**: cada regla descrita declara su `Origen` (la `C…` del catálogo + el skill que la respalda, o el punto del spec/guías para `A-NNN`).
- **MUST**: las reglas del catálogo cuyo sujeto **no** existe en el diseño se listan como **no aplicables** con su motivo (no se describen como test, pero se justifican para que conste que se evaluaron).
- **MUST**: toda regla sobre código **nuevo** tiene resultado esperado `PASS`; cualquier `FREEZE` se justifica con la clase preexistente y su estado en el catálogo.

## 5. Plantilla de `arch-test-desc.md`

El subagente escribe un fichero con esta estructura exacta:

```markdown
# Tests de arquitectura (ArchUnit)

Descripción de los tests de arquitectura (ArchUnit 1.4.2, JUnit 5) que verifican que las clases del diseño respetan la arquitectura documentada del proyecto. **Solo descripción, sin código**: `/sdd-implementer` genera el código a partir de aquí. El catálogo de reglas (`C1`–`C22`) vive en el skill `k-archunit` (`secretaria-virtual-rules.md`); aquí se selecciona y concreta el subconjunto que aplica a este diseño.

## Clase de test
- **Nombre:** `Arquitectura<Sistema>Test`
- **Ámbito (`@AnalyzeClasses`):** `<paquete-raíz-del-sistema>` (`importOptions = DoNotIncludeTests`).
- **Catálogo de referencia:** `k-archunit/secretaria-virtual-rules.md`.

---

## Reglas aplicables

### <CN o A-NNN> — `<nombre_test>`
- **Qué verifica:** <restricción estructural en una frase>.
- **Ámbito:** `<paquete(s) concretos del diseño>`.
- **Sujetos del diseño:** `<Clase1>`, `<Clase2>` (o «todas las clases de <capa>»).
- **Resultado esperado:** PASS | FREEZE (<motivo si FREEZE>).
- **Origen:** `<CN>` (catálogo) ← `<k-skill que la respalda>`  /  o `A-NNN` ← `<punto del spec/guías>`.

(repite por cada regla aplicable)

---

## Reglas del catálogo no aplicables
- **<CN>** — <motivo: p.ej. «el diseño no define DTOs», «el diseño no crea módulo Guice»>.
- …

## Cobertura
- Artefactos del diseño cubiertos: <controladores, servicios, repos, módulos, DTOs, entidades cubiertos>.
- Reglas del catálogo aplicadas: <lista de C…>.
- Reglas específicas del diseño: <lista de A-NNN, o «ninguna»>.
- Reglas no aplicables (justificadas): <lista de C…>.
- Reglas en FREEZE (deuda preexistente): <lista, o «ninguna»>.
```

---

## 6. Checklist del subagente `test-arquitectura`

- [ ] ¿Se ha cargado el skill `k-archunit` y leído `secretaria-virtual-rules.md` como fuente de las reglas?
- [ ] ¿Cada artefacto del diseño (controlador/servicio/impl./repo/módulo/DTO/entidad) tiene al menos una regla de arquitectura aplicable descrita?
- [ ] ¿Se describen las reglas de capa (`C1`–`C8`) que corresponden a la capa del diseño, con el destino prohibido por FQN?
- [ ] ¿Cada regla descrita indica `Qué verifica`, `Ámbito`, `Sujetos del diseño`, `Resultado esperado` y `Origen`?
- [ ] ¿Cada regla se mapea a paquetes/clases concretos del diseño (no se cita la regla «en general»)?
- [ ] ¿Las reglas del catálogo cuyo sujeto no existe en el diseño están listadas como no aplicables con su motivo?
- [ ] ¿El código nuevo tiene resultado `PASS` y los `FREEZE` se justifican con una clase preexistente y su estado en el catálogo?
- [ ] ¿NO se crean exenciones nuevas (`PAQUETES_EXENTOS`/`freeze`) para silenciar violaciones del propio diseño?
- [ ] ¿NO hay nada de código Java (ni `@ArchTest`, ni `@AnalyzeClasses`, ni reglas fluidas, ni imports)? Solo descripción.
- [ ] ¿La estructura sigue la plantilla §5?

El subagente **MUST NOT** devolver `ESCRITO: arch-test-desc.md` si queda algún punto sin cumplir. **LIMIT**: máximo 3 iteraciones de autocorrección.

---

## 7. Verificación de coherencia con el diseño (post-generación)

Tras escribir `arch-test-desc.md`, el skill lanza un bucle aparte con dos subagentes: **`verificador-test-arquitectura`** (busca incoherencias entre `arch-test-desc.md`, el diseño y el catálogo) y **`corrector-test-arquitectura`** (las corrige). Esta sección define **qué cuenta como incoherencia** — es la referencia del verificador. La **fuente de verdad** es el diseño (`design.md`) y el catálogo `k-archunit` (`secretaria-virtual-rules.md`): si una regla descrita no cuadra, se corrige el **fichero de tests**, nunca el diseño ni el catálogo.

### 7.1 Comprobaciones del `verificador-test-arquitectura`

- **Paquete/clase existe:** cada `Ámbito`/`Sujetos del diseño` de una regla descrita **MUST** corresponder a paquetes/clases (FQN) que el diseño **crea o modifica** (en el inventario de `design.md`). Un paquete o clase que el diseño no define → incoherencia `BLOCKING`.
- **Regla del catálogo existe:** cada id `C…` **MUST** existir en `secretaria-virtual-rules.md` y usarse con **su** criterio (no redefinido). Un `C…` inexistente o tergiversado → `BLOCKING`.
- **`A-NNN` trazada:** cada regla específica `A-NNN` **MUST** declarar el punto del spec/guías que la impone. Una `A-NNN` sin origen trazable → `IMPORTANT`.
- **Cobertura cuadra:** cada artefacto del diseño (controlador/servicio/impl./repo/módulo/DTO/entidad) **MUST** estar cubierto por al menos una regla aplicable, y la sección «Reglas del catálogo no aplicables» **MUST** justificar las omitidas (su sujeto realmente no existe en el diseño). Discrepancias → `IMPORTANT`.
- **Resultado coherente:** el `Resultado esperado` de una regla sobre código **nuevo** **MUST** ser `PASS`; un `FREEZE` **MUST** justificarse con una clase **preexistente** que el diseño modifica y su «Estado actual» en el catálogo. Un `FREEZE` sobre código nuevo → `BLOCKING`.
- **Sin invención:** **MUST NOT** haber paquetes, clases ni reglas en `arch-test-desc.md` que no estén en el diseño o en el catálogo. Cualquier elemento inventado → `BLOCKING`.
- **Estructura y forma:** la estructura sigue la plantilla §5 y **no** hay código Java (ni `@ArchTest`, ni `@AnalyzeClasses`, ni reglas fluidas, ni imports). Desvíos → `MINOR`/`IMPORTANT` según gravedad.

### 7.2 Tarea del `corrector-test-arquitectura`

- Aplica **en sitio** sobre `design/arch-test-desc.md` cada incoherencia reportada (eliminar la regla sin sujeto y moverla a «no aplicables», corregir el FQN del ámbito, añadir la traza de la `A-NNN`, ajustar `PASS`/`FREEZE`…), ajustándose a la plantilla §5.
- **MUST NOT** modificar `design.md`, el catálogo ni ningún otro fichero del diseño: la fuente de verdad es el diseño y el catálogo.
- **MUST NOT** introducir paquetes/clases/reglas nuevos que no estén en el diseño o el catálogo.

El contrato de salida (token `OK-CORRECTO` o líneas JSONL `id`/`severidad`/`fichero`/`ubicacion`/`origen`/`problema`/`correccion`) y el bucle (LIMIT 10) los fija el skill `sdd-designer`.
