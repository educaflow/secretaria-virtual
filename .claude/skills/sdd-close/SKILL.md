---
name: sdd-close
description: Último paso del pipeline SDD. Cierra una iniciativa ya implementada y aplicada al código. Identifica a partir de su `design.md` qué sistemas/subsistemas tocó (no de `git diff`) y lanza un subagente documentador por sistema en paralelo que regenera desde el código su `CLAUDE.md`, su `modelo.puml` (esquema PlantUML del modelo de datos a partir de los `domains/*.xml`) y su `modelo.png` renderizado —todos en la raíz del sistema/subsistema—; luego mueve la carpeta del draft **verbatim** a `.sdd/archive/<nombre>`. El alcance sale del `design.md`; el contenido de cada documento sale del código real. Lo específico de la documentación (formato del `CLAUDE.md`, derivación del modelo, render) lo define `template-system/README.md` (configurable con `--template-dir`). No reescribe los artefactos del draft; la documentación viva pasa a ser el `CLAUDE.md` por sistema.
---

# sdd-close

Eres el **paso de cierre** del pipeline SDD: tomas una iniciativa ya implementada y aplicada al código y produces dos cosas independientes — (1) la **documentación viva** de cada sistema/subsistema que la iniciativa tocó, **regenerada desde el código real**; (2) el **archivado verbatim** del draft. La fuente de verdad del **contenido** de la documentación es **el código**, nunca el draft.

Lo específico de la documentación (qué ficheros se generan, el formato del `CLAUDE.md`, cómo se deriva el modelo de datos, el comando de render) lo define `template-system/README.md` (configurable con `--template-dir`), que el documentador lee como contrato (§2.1). Este `SKILL.md` aporta el flujo; **MUST NOT** hardcodear aquí ese detalle.

El skill lanza **un único rol** de subagente, el **documentador**, en **paralelo** (uno por sistema/subsistema afectado), cada uno en contexto aislado leyendo el mismo `README.md`.

---

## User Input

```text
$ARGUMENTS
```

You **MUST** consider the user input before proceeding (if not empty). Argumentos esperables:

- `ruta-iniciativa` (opcional, posicional 1): ruta a la carpeta del draft a cerrar (o a su `design/design.md`). Si se omite, auto-detecta la última (§4.1).
- Flags de override `--template-dir=`, `--in=`, `--out=`, `--root=` (Apéndice A).

Si los argumentos están vacíos, asume cierre del último draft.

---

## Outline

1. **Fase 0 — Localizar** el draft a cerrar y **confirmar** con el usuario (§4).
2. **Fase 1 — Cargar** el contrato (`template-system/README.md`) e **identificar** a partir del `design.md` del draft qué sistemas/subsistemas tocó la iniciativa (§5).
3. **Fase 2 — Documentar**: lanzar **N documentadores en paralelo** (uno por sistema afectado), cada uno regenera la documentación de su sistema desde el código según el contrato (§6).
4. **Fase 3 — Archivar**: mover la carpeta del draft **verbatim** a `.sdd/archive/<nombre>` (§7).
5. **Fase 4 — Reportar** qué se documentó y dónde quedó archivado el draft (§8).

**STOP conditions**:

- `--template-dir=` apunta a una carpeta que **no contiene `README.md`** → **ERROR** y detente.
- No se encuentra ninguna carpeta de iniciativa con `design/design.md` y el usuario no da ruta → **STOP** y avisa: probablemente no hay nada implementado que cerrar.
- El usuario rechaza el draft auto-detectado y no da ruta alternativa (§4.2) → **STOP**.
- El `design.md` del draft no existe o no permite deducir ningún sistema/subsistema afectado (§5.2) → **STOP** y avisa: sin alcance no se sabe qué documentar.
- El destino `.sdd/archive/<nombre>` **ya existe** → **STOP** y avisa: la iniciativa parece ya cerrada; no sobrescribas (§7).

---

## 1. Entrada y salida

### 1.1 Entrada

- La carpeta del draft de la iniciativa en `.sdd/drafts/{YYYY-MM-DD_HH-MM_nombre}/`. De ella el motor lee **solo** `design/design.md`, y **solo** para deducir el **alcance** (qué sistemas/subsistemas tocó la iniciativa, §5). El resto de artefactos (`specification.md`, `analysis/`, `implementation/`, `test-e2e-desc/`) no se leen: se archivan tal cual.
- **El código real** del workspace: **única** fuente de verdad del **contenido** de la documentación que se genera.
- `template-system/README.md`: el contrato que define qué documenta el documentador y cómo.

### 1.2 Salida

- Por cada sistema/subsistema afectado, en su **raíz** (`src/main/java/com/educaflow/{...}/`): la documentación que defina el contrato, regenerada desde el código. Con la plantilla por defecto: `CLAUDE.md` + `modelo.puml` + `modelo.png`.
- La carpeta del draft movida **verbatim** a `.sdd/archive/<nombre>` (mismo nombre, con su timestamp).
- En la conversación: el reporte final (§8).

**MUST NOT** reescribir ni corregir ningún artefacto del draft (no hay versión "as-built"): el draft es histórico inmutable y se mueve tal cual. **MUST NOT** tocar `.sdd/drafts/` salvo para mover la carpeta a `.sdd/archive/` en la Fase 3.

### 1.3 Estructura de carpetas

```
.sdd/
├── drafts/
│   └── 2026-06-28_10-00_firmas-bulk/     ← se MUEVE entera en la Fase 3
│       ├── specification.md
│       ├── analysis/ · design/ · implementation/ · test-e2e-desc/   (lo que haya)
└── archive/
    └── 2026-06-28_10-00_firmas-bulk/     ← destino del draft (verbatim, mismo nombre)

src/main/java/com/educaflow/subsystem/firmas/   ← un sistema/subsistema afectado
├── CLAUDE.md        ← regenerado desde el código        ┐
├── modelo.puml      ← esquema PlantUML del modelo datos  │ los produce el documentador
├── modelo.png       ← render del .puml                   ┘ según el contrato
├── domains/ · services/ · controllers/ · views/ …
```

---

## 2. Principios (aplican a todas las fases)

### 2.1 El README es el contrato único

Todo lo específico (qué ficheros documenta el documentador, el formato del `CLAUDE.md`, cómo se deriva el modelo de datos de los `domains/*.xml`, con qué comando se renderiza la imagen y qué **MUST NOT** tocarse) lo define `template-system/README.md` y los ficheros que él referencie. El documentador los **lee de disco**; el motor **MUST NOT** asumirlos ni hardcodearlos. El motor solo pasa a cada subagente **las rutas** (la del contrato y la del sistema asignado) y su rol.

**CRITICAL — `README.md` es el ÚNICO fichero de la plantilla que el motor conoce por nombre.** Único acoplamiento por nombre: `README.md` (contrato), la entrada (la carpeta del sistema afectado) y la salida (la documentación en la raíz de ese sistema).

### 2.2 Alcance del draft, contenido del código

- **CRITICAL — dos fuentes distintas**: el **alcance** (qué sistemas/subsistemas re-documentar) sale del **`design.md`** del draft (§5); el **contenido** de cada `CLAUDE.md`/modelo sale **del código real** de ese sistema. El motor lee `design.md`; el documentador lee código.
- El documentador **MUST NOT** leer nada del draft ni de `.sdd/` para generar la documentación: su único insumo de contenido es el código del sistema asignado. **MUST NOT** hacer merge textual con el `CLAUDE.md` previo: se **regenera desde cero** (el previo es solo referencia de estructura, lo gestiona el contrato).

### 2.3 El `design.md` es la fuente de verdad de qué se tocó

- **MUST NOT** usar `git diff` (una iniciativa puede tener muchos commits y un diff por hash base es frágil e incompleto). **MUST** deducir los sistemas/subsistemas afectados del `design.md` del draft (§5), que enumera los ficheros y clases que la iniciativa crea/modifica.
- El alcance incluye **cualquier** capa que la iniciativa tocara: `subsystem/*`, `system/*`, `base/infrastructure/*`, `base/util` (§5.2). Los sistemas que el design solo menciona como **dependencia** (lectura, no creación/modificación) **MUST NOT** entrar en el alcance.

### 2.4 Orquestación de subagentes

- Los documentadores corren **en paralelo**: cada uno trabaja una carpeta de sistema distinta, no se pisan. **CRITICAL**: lánzalos **en una única respuesta** con N invocaciones a `Agent`. **REQUIRED**: exactamente N (N = número de sistemas afectados). **MUST NOT** lanzarlos secuencialmente. **MUST NOT** usar `run_in_background` (necesitas sus resultados para el reporte).
- Cada documentador responde con un **token literal** que el motor parsea (§6.2). El motor compara por literal exacto.
- Los documentadores **MUST NOT** usar `AskUserQuestion`: ante un bloqueo lo reportan con su token y el motor lo gestiona (§6.3).

### 2.5 El cierre es semi-autónomo

La **única** pregunta al usuario es en la **Fase 0** (qué iniciativa cerrar, como todos los `/sdd-*`). A partir de ahí **MUST NOT** usar `AskUserQuestion`. Un sistema que no se puede documentar se reporta como fallo y no aborta el cierre; solo las **STOP conditions** del Outline detienen la pasada.

---

## 3. Flujo general

```
Fase 0  Localizar el draft (ruta explícita | última por timestamp) ── confirmar con el usuario
   │
Fase 1  Cargar el contrato (template-system/README.md) ── leer design/design.md del draft
   │     ── deducir los sistemas/subsistemas afectados (granularidad §5.2)
   │
Fase 2  N documentadores EN PARALELO (uno por sistema) ── cada uno regenera
   │     su documentación desde el código según el contrato ── recoger tokens ── re-lanzar fallidos (LIMIT 3)
   │
Fase 3  Mover .sdd/drafts/<nombre> ──► .sdd/archive/<nombre> (verbatim)
   │
Fase 4  Reportar al usuario
```

---

## 4. Fase 0 — Localizar y confirmar el draft

### 4.1 Localizar la iniciativa

Si el usuario no da `ruta-iniciativa`:

1. Lista las carpetas con formato de iniciativa:
   ```bash
   ls -d .sdd/drafts/[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]_[0-9][0-9]-[0-9][0-9]_*/ 2>/dev/null
   ```
2. Ordena alfabéticamente (el prefijo timestamp = orden cronológico) y toma la **última**.
3. Si ninguna existe → **STOP** y avisa: no hay iniciativas que cerrar.

Ejemplos de nombre de iniciativa:

- ✅ CORRECTO: `2026-05-21_14-30_firmas-bulk`
- ❌ INCORRECTO: `firmas-bulk_2026-05-21` (timestamp al final, no ordenable)
- ❌ INCORRECTO: `2026-5-21_14-30_firmas-bulk` (mes sin pad de cero; no cumple el patrón)

### 4.2 Confirmar con el usuario

Pregunta con `AskUserQuestion`:

> Voy a cerrar la iniciativa: `{nombre-iniciativa}`
> Se documentarán los sistemas que tocó (desde el código) y se archivará el draft en `.sdd/archive/{nombre-iniciativa}`.
> ¿Continuamos?

Opciones: "Sí, cerrar esta iniciativa" / "No, quiero indicar otra ruta". Si "No", pide la ruta y vuelve a §4.1 con ella.

**MUST NOT** usar `mtime` ni elegir una carpeta que no sea la última por orden alfabético del timestamp.

---

## 5. Fase 1 — Cargar contrato e identificar el alcance

### 5.0 Cargar el contrato y resolver rutas

1. **REQUIRED — lee con `Read` la guía `template-system/README.md`** (resuelta contra `--template-dir`). Si no existe → **ERROR** y detente. Entiende a alto nivel qué pide al documentador; el resto del contrato lo leen los subagentes de disco.
2. Resuelve la ruta de `template-system/README.md` (las reglas) y el nombre de la iniciativa.

### 5.1 Leer el `design.md` del draft

**REQUIRED — lee con `Read` el `design/design.md`** del draft localizado en la Fase 0 (es la fuente de verdad del alcance, **no** `git diff`; ver §2.3). Si no existe → **STOP** y avisa: sin diseño no se sabe qué sistemas tocó la iniciativa.

El `design.md` enumera los ficheros y clases que la iniciativa **crea/modifica**. De ahí se deduce el alcance. Señales típicas a aprovechar:

- El campo **`**Capa:**`** (p.ej. `**Capa:** subsystem/importacion`): el sistema/subsistema principal.
- La **tabla de ficheros** con rutas `src/main/java/com/educaflow/...` marcadas `Creado`/`Modificado`.
- Los **FQN** `com.educaflow.{...}` de clases/entidades que el diseño declara crear o modificar.

### 5.2 Deducir los sistemas/subsistemas afectados

1. Recolecta del `design.md` toda ruta `src/main/java/com/educaflow/...` y todo FQN `com.educaflow....` que corresponda a algo **creado o modificado** por la iniciativa.
2. Mapea cada ruta/FQN a su **raíz de sistema** dentro de `src/main/java/com/educaflow/`:
   - `subsystem/{nombre}/`
   - `system/{nombre}/`
   - `base/infrastructure/{nombre}/`
   - `base/util/`
3. **Dedupe**: varias entradas del mismo sistema cuentan como uno.
4. **MUST NOT** incluir sistemas que el diseño solo menciona como **dependencia** (una entidad de otro subsistema usada como `ref`, un servicio que solo se invoca): el alcance son los que la iniciativa **cambió**, no los que solo usa (§2.3).

**REQUIRED**: la granularidad es el **sistema/subsistema completo**, no subcarpetas internas.

- ✅ CORRECTO: el design crea `subsystem/firmas/services/Foo.java` y `subsystem/firmas/controllers/Bar.java` → sistema afectado = `subsystem/firmas/`
- ✅ CORRECTO: el design también modifica `base/infrastructure/correo/CorreoSender.java` → se añade `base/infrastructure/correo/` al alcance
- ❌ INCORRECTO: dos entradas `subsystem/firmas/services/` y `subsystem/firmas/controllers/` (granularidad incorrecta; se documentaría el mismo sistema dos veces)
- ❌ INCORRECTO: incluir `subsystem/common/` porque el design referencia `com.educaflow.subsystem.common.db.Centro` como `ref` (es una dependencia, no un cambio)

Si tras deducir **no queda ningún** sistema → **STOP** y avisa: el `design.md` no permite determinar el alcance; ¿seguro que es esta la iniciativa?

---

## 6. Fase 2 — Documentar los sistemas afectados (en paralelo)

**CRITICAL**: lanza **un documentador por cada sistema afectado de §5.2**, todos en **una única respuesta** con N invocaciones a `Agent` (`subagent_type: claude`, `run_in_background: false`). **REQUIRED**: exactamente N. **MUST NOT** secuencial, **MUST NOT** `run_in_background`.

### 6.1 Plantilla literal del prompt del documentador

El prompt **MUST** ser literal (sustituye `{ruta de template-system/README.md}` y `{ruta-sistema}`):

````text
Eres un documentador de un sistema/subsistema de la secretaría virtual (Axelor). Tu tarea es
regenerar la documentación de UN sistema a partir de su CÓDIGO REAL.

- Reglas (contrato): lee `{ruta de template-system/README.md}` y TODOS los ficheros que referencie.
  Define exactamente qué ficheros generar, su formato, cómo derivar el modelo de datos del código y
  con qué comando renderizar la imagen. Síguelo al pie de la letra.
- Sistema a documentar: `{ruta-sistema}`. Lee su código (Java/Kotlin/XML y sus subcarpetas), en
  especial sus `domains/*.xml` para el modelo de datos. El código es la ÚNICA fuente de verdad.
- MUST NOT leer ni usar nada bajo `.sdd/` como fuente de contenido: la documentación sale del código.
- MUST NOT modificar código de la aplicación (`src/main/...` salvo los ficheros de documentación que
  el contrato te manda escribir en la raíz del sistema).
- MUST NOT usar AskUserQuestion. Aplica el checklist del contrato antes de terminar (LIMIT 3 iteraciones).
- Al terminar responde EXACTAMENTE una línea:
  - `DOCUMENTADO: {ruta-sistema}` — escribiste la documentación que pide el contrato.
  - `BLOQUEADO: {ruta-sistema} — {motivo}` — no se pudo (carpeta ilegible, render imposible, etc.).
No escribas nada más. No expliques lo que hiciste. No pegues el contenido de los ficheros.
````

### 6.2 Contrato de respuesta

El motor parsea la primera línea de cada documentador:

- `DOCUMENTADO: {ruta-sistema}` → sistema documentado correctamente.
- `BLOQUEADO: {ruta-sistema} — {motivo}` → no se pudo; se registra en el reporte (§8) y **no** aborta el cierre.

- ✅ CORRECTO: `DOCUMENTADO: src/main/java/com/educaflow/subsystem/firmas`
- ✅ CORRECTO: `BLOQUEADO: src/main/java/com/educaflow/base/util — la carpeta no tiene código legible`
- ❌ INCORRECTO: que el documentador devuelva el contenido del `CLAUDE.md` o explicaciones en prosa.

### 6.3 Re-lanzamiento de fallidos

Tras recoger los N tokens, por cada `BLOQUEADO` el motor decide:

1. Si el motivo es transitorio (timeout, render fallido recuperable), **re-lanza** ese documentador con el mismo prompt. **LIMIT**: máximo 3 re-lanzamientos por sistema.
2. Si tras la 3ª sigue `BLOQUEADO`, anota la incidencia y continúa: el cierre **no** se aborta por un sistema sin documentar; se reporta en §8.

---

## 7. Fase 3 — Archivar el draft (verbatim)

1. Determina el destino: `.sdd/archive/{nombre-iniciativa}` (mismo nombre que la carpeta del draft, con su timestamp). Crea `.sdd/archive/` si no existe.
2. Si el destino **ya existe** → **STOP** y avisa: la iniciativa parece ya cerrada; **MUST NOT** sobrescribir.
3. **Mueve** la carpeta entera del draft al destino (verbatim, sin alterar su contenido):
   ```bash
   git mv .sdd/drafts/{nombre-iniciativa} .sdd/archive/{nombre-iniciativa} 2>/dev/null \
     || mv .sdd/drafts/{nombre-iniciativa} .sdd/archive/{nombre-iniciativa}
   ```
   (`git mv` si está versionado; si no, `mv` normal.)

- ✅ CORRECTO: `.sdd/archive/2026-05-21_14-30_firmas-bulk/` (mismo nombre, timestamp incluido)
- ❌ INCORRECTO: renumerar o renombrar la carpeta al archivar (p.ej. `.sdd/archive/0007_firmas-bulk/`); el archivado es un **movimiento verbatim**, no una transformación.

**MUST NOT** archivar antes de la Fase 2: documenta primero (lee el código en su sitio) y mueve el draft después.

---

## 8. Fase 4 — Reportar al usuario

Plantilla literal del mensaje final:

```text
Iniciativa cerrada: {nombre-iniciativa}

Documentación regenerada (desde el código):
  - {ruta-sistema-1}/  → CLAUDE.md, modelo.puml, modelo.png
  - {ruta-sistema-2}/  → CLAUDE.md, modelo.puml, modelo.png
  ...
  {- {ruta-sistema-k}/  → SIN DOCUMENTAR (BLOQUEADO: {motivo})   ← solo si hubo fallos}

Draft archivado (verbatim) en: .sdd/archive/{nombre-iniciativa}/
```

**MUST NOT** declarar éxito si algún sistema quedó `BLOQUEADO`: indícalo explícitamente y di que se puede re-documentar relanzando `/sdd-close` sobre la misma iniciativa (la carpeta ya estará en `.sdd/archive/`, pásala con `--in=`).

---

## Quick Guidelines

- **Contrato en `template-system/README.md`**: lo específico (qué ficheros, formato del `CLAUDE.md`, derivación del modelo, comando de render) lo define `template-system/README.md` (configurable con `--template-dir`), que **lee el documentador**. **MUST NOT** hardcodear ese detalle en este skill. Contrato fijo: entrada = carpeta del sistema afectado; salida = documentación en su raíz; el draft se archiva verbatim.
- **Dos mitades independientes**: (1) documentar cada sistema afectado desde el **código** (el draft NO es input); (2) mover el draft a `.sdd/archive/`. **MUST NOT** generar documentación a partir del draft.
- **Localizar** (§4): ruta explícita o la **última** iniciativa por timestamp, y **confirmar**. **MUST NOT** usar `mtime`.
- **El `design.md` manda el alcance** (§5): los sistemas afectados se deducen del `design.md` del draft (campo `**Capa:**`, tabla de ficheros `Creado`/`Modificado`, FQN), granularidad = sistema/subsistema completo, incluida base/infraestructura si cambió. **MUST NOT** usar `git diff`. **MUST NOT** incluir dependencias de solo lectura.
- **Documentadores en paralelo** (§6): una única respuesta con N invocaciones a `Agent`, sin `run_in_background`, sin `AskUserQuestion`. Token literal `DOCUMENTADO:` / `BLOQUEADO:`. Re-lanzar fallidos (**LIMIT** 3); un sistema sin documentar no aborta el cierre.
- **Archivar verbatim** (§7): mover `.sdd/drafts/<nombre>` → `.sdd/archive/<nombre>` con el **mismo nombre**; si el destino existe → **STOP**. **MUST NOT** reescribir el draft (no hay versión as-built).
- **Semi-autónomo** (§2.5): la única pregunta es la Fase 0 (qué iniciativa). Después, **MUST NOT** `AskUserQuestion`.

---

## Apéndice A — Override de rutas (para testing y versatilidad)

- `--template-dir=<ruta>` — carpeta de plantillas alternativa a `template-system/`. **MUST** contener un `README.md` redactado para el rol documentador; si falta → **ERROR**.
- `--in=<ruta>` — carpeta de la iniciativa (draft) de entrada explícita. Desactiva la auto-detección de §4.1.
- `--out=<ruta>` — carpeta de archivo alternativa a `.sdd/archive/` para el movimiento de la Fase 3.
- `--root=<ruta>` — raíz alternativa a `.sdd/` para resolver `drafts/` y `archive/`.

En uso normal no se especifican: se usa `template-system/`, la última iniciativa, `.sdd/drafts/` y `.sdd/archive/`.
