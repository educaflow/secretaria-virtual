---
name: sdd-close-spec
description: Cierra una iniciativa SDD — lee el último draft (user-story + analysis + design), usa `git diff` para identificar qué ficheros realmente cambiaron, actualiza los CLAUDE.md de cada carpeta afectada regenerándolos desde el código real, y archiva los artefactos en `.sdd/specs/NNNN_desc/` con versiones as-built: el `design.md` y el `analysis.md` se corrigen para reflejar lo que se implementó (validaciones reales, firmas reales, vistas reales) y el `user-story.md` se ajusta solo si la implementación reveló contradicciones evidentes con la intención original. Es el último paso del pipeline SDD: la entrada es el draft producido por `/sdd-implementer-system` ya aplicado al código, la salida son los artefactos as-built archivados y los `CLAUDE.md` regenerados.
---

# sdd-close-spec

Eres el paso de cierre del pipeline SDD. Transformas **un draft (user-story + analysis + design) + el código real ya implementado** en **una spec archivada as-built en `.sdd/specs/NNNN_desc/` + un `CLAUDE.md` regenerado por cada carpeta de código afectada**. La fuente de verdad es **el código**, no el diseño.

---

## User Input

```text
$ARGUMENTS
```

You **MUST** consider the user input before proceeding (if not empty). Los argumentos esperables son:

- `ruta-design` (opcional, posicional 1): ruta absoluta o relativa al `design_NN.md` del draft a cerrar. Si se omite, auto-detecta el último (paso 0.1).
- `hash-commit-base` (opcional, posicional 2): hash del commit anterior al inicio de la iniciativa. Si se pasa, el `git diff` de la Fase 1 abarca desde ese commit hasta el workspace incluido (commits posteriores + staged + unstaged + untracked); si no, el diff cubre solo lo que hay en el workspace contra `HEAD`.

Si los argumentos están vacíos, asume cierre del último draft sin hash base.

---

## Outline

1. **Localizar y confirmar** el draft a cerrar (Fase 0).
2. **Identificar** qué cambió realmente con `git diff` (Fase 1).
3. **Regenerar** los `CLAUDE.md` de cada carpeta afectada en paralelo (Fase 2).
4. **Archivar** los tres artefactos como versiones as-built en `.sdd/specs/NNNN_desc/` (Fase 3).
5. **Reportar** al usuario qué se cerró y dónde quedó (Fase 4).

**STOP conditions**:

- El draft no contiene los tres artefactos mínimos (`user-story.md`, `analysis.md`, `design_NN.md`) → **ERROR** y detente.
- El frontmatter `type:` de cualquier artefacto no coincide con el esperado (§4.3) → **ERROR** y detente indicando qué fichero falla.
- Las listas del diff en §5.2 salen vacías y **no** se pasó hash base → **STOP** y avisa al usuario: probablemente la iniciativa ya está comiteada y necesita pasar el hash.
- El usuario rechaza el draft auto-detectado en §4.2 y no proporciona ruta alternativa → **STOP**.
- Una carpeta afectada no tiene código legible (acceso denegado, vacía) → **STOP** y avisa al usuario.

---

## 1. Entrada y salida

### 1.1 Entrada

- `.sdd/drafts/{YYYY-MM-DD_HH-MM_nombre}/user-story.md` — historia de usuario original.
- `.sdd/drafts/{...}/design-guidelines.md` — directrices opcionales (no es error si falta).
- `.sdd/drafts/{...}/analysis_NN/analysis.md` — análisis del que cuelga el diseño.
- `.sdd/drafts/{...}/analysis_NN/design_NN.md` — diseño implementado.
- **El código real** del workspace (es la fuente de verdad).

### 1.2 Salida

- `{carpeta-afectada}/CLAUDE.md` regenerado por cada carpeta tocada por el diff.
- `.sdd/specs/{NNNN}_{descripcion}/user-story.md` — versión as-built.
- `.sdd/specs/{NNNN}_{descripcion}/analysis.md` — versión as-built.
- `.sdd/specs/{NNNN}_{descripcion}/design.md` — versión as-built.
- `.sdd/specs/{NNNN}_{descripcion}/design-guidelines.md` — copia literal si existía.

### 1.3 Estructura de carpetas

```
.sdd/
├── drafts/
│   └── 2026-05-21_14-30_firmas-bulk/      ← intacta tras el cierre
│       ├── user-story.md
│       ├── design-guidelines.md
│       └── analysis_02/
│           ├── analysis.md
│           └── design_01.md
└── specs/
    └── 0007_firmas-bulk/                  ← creada por este skill
        ├── user-story.md   (as-built)
        ├── analysis.md     (as-built)
        ├── design.md       (as-built)
        └── design-guidelines.md (si existía)
```

---

## 2. Principios

### 2.1 El código manda

Los artefactos archivados reflejan **lo que se implementó**, no lo que se planeó. Si el código difiere del diseño, **MUST** corregir `analysis.md` y `design.md` al estado real. `user-story.md` se conserva salvo contradicción dura (ver §7.2.a).

### 2.2 git diff es la fuente de verdad de qué cambió

**MUST NOT** leer carpetas enteras buscando cambios. **MUST** partir del diff y a partir de él identificar carpetas afectadas.

### 2.3 Regenerar, no fusionar

Los `CLAUDE.md` se **regeneran desde cero** desde el código. El CLAUDE.md anterior es referencia de estructura, nunca fuente de verdad. **MUST NOT** hacer merge textual.

### 2.4 El draft no se toca

**MUST NOT** modificar nada bajo `.sdd/drafts/`. Es histórico.

### 2.5 Specs anteriores no son referencia de contenido

**MUST NOT** leer `.sdd/specs/*` como fuente de contenido. El contenido viene del código.

---

## 3. Flujo general

```
Fase 0 ── localizar draft ── confirmar con usuario ── leer + validar frontmatter
   │
Fase 1 ── anunciar modo diff ── git diff vs base ── identificar carpetas afectadas
   │
Fase 2 ── N subagentes en paralelo (uno por carpeta) ── regenerar CLAUDE.md
   │
Fase 3 ── numerar spec ── generar as-built (user-story, analysis, design) ── escribir
   │
Fase 4 ── reportar al usuario
```

---

## 4. Fase 0 — Localizar y confirmar el draft

### 4.1 Localizar el último draft

Si el usuario no proporciona `ruta-design`:

1. Lista las subcarpetas de `.sdd/drafts/` cuyo nombre cumpla el regex `^[0-9]{4}-[0-9]{2}-[0-9]{2}_[0-9]{2}-[0-9]{2}_`. Ordena alfabéticamente (el prefijo timestamp ya da orden cronológico). Toma la última.
2. Dentro de esa iniciativa, toma la subcarpeta `analysis_NN/` con `NN` más alto.
3. Dentro, toma el `design_NN.md` con `NN` más alto.
4. Si no hay iniciativas → **STOP** y avisa al usuario.

Ejemplos de nombres de iniciativa:

- ✅ CORRECTO: `2026-05-21_14-30_firmas-bulk`
- ❌ INCORRECTO: `firmas-bulk_2026-05-21` (timestamp al final, no ordenable)
- ❌ INCORRECTO: `2026-5-21_14-30_firmas-bulk` (mes sin pad de cero, regex no cumple)

### 4.2 Confirmar con el usuario

Pregunta con `AskUserQuestion`:

> Voy a cerrar la iniciativa: `{nombre-iniciativa}`
> Draft: `{ruta/design_NN.md}`
> ¿Continuamos?

Opciones: "Sí, cerrar esta iniciativa" / "No, quiero indicar otra ruta". Si "No", pide la ruta y vuelve a §4.1 con esa ruta.

### 4.3 Leer y validar artefactos del draft

Lee en paralelo:

- `user-story.md` (dos niveles arriba del design)
- `analysis.md` (mismo nivel que el design)
- `design_NN.md` (el localizado)
- `design-guidelines.md` (opcional)

**REQUIRED**: validar frontmatter `type:` de cada artefacto. Si alguno no coincide → **ERROR** y detente indicando el fichero y el `type:` esperado:

| Fichero | `type:` esperado |
|---------|------------------|
| `user-story.md` | `user-story` |
| `analysis.md` | `analysis` |
| `design_NN.md` | `design` |
| `design-guidelines.md` (si existe) | `design-guidelines` |

---

## 5. Fase 1 — Identificar qué cambió realmente

### 5.1 Anunciar el modo de diff

**MUST**, antes de ejecutar el diff, anunciar al usuario en una sola línea qué modo vas a usar (le da oportunidad de corregirte):

- Sin hash: `Buscando diferencias solo en el workspace (cambios sin comitear contra HEAD).`
- Con hash: `Buscando diferencias desde el commit {hash-corto} hasta el workspace (commits posteriores + cambios sin comitear).`

### 5.2 Calcular el diff

El diff **MUST** abarcar desde un punto base hasta el workspace incluido (commits posteriores al base + staged + unstaged + untracked). El punto base depende del argumento:

**Sin hash** — punto base `HEAD`:

```bash
git diff --name-only HEAD
git ls-files --others --exclude-standard
```

**Con hash** — punto base `{hash-base}`:

```bash
git diff --name-only {hash-base}
git ls-files --others --exclude-standard
```

`git diff --name-only` cubre staged + unstaged contra el base; `git ls-files --others --exclude-standard` añade los untracked (que el diff no ve). Unifica ambas listas en un único conjunto de ficheros, eliminando duplicados.

### 5.3 Identificar carpetas afectadas

A partir de la lista unificada, identifica carpetas dentro de `src/main/java/com/educaflow/` candidatas a tener `CLAUDE.md`:

- `subsystem/{nombre}/`
- `system/{nombre}/`
- `base/infrastructure/{nombre}/`
- `base/util/`

**REQUIRED**: la granularidad es el **subsistema/sistema completo**, no subcarpetas internas.

- ✅ CORRECTO: diff toca `subsystem/firmas/service/Foo.java` y `subsystem/firmas/controller/Bar.java` → carpeta afectada = `subsystem/firmas/`
- ❌ INCORRECTO: dos entradas separadas `subsystem/firmas/service/` y `subsystem/firmas/controller/` (granularidad incorrecta, se regeneraría el mismo CLAUDE.md dos veces)
- ❌ INCORRECTO: `base/util/StringUtil.java` → carpeta afectada = `base/util/StringUtil/` (subcarpeta inexistente; lo correcto es `base/util/`)

---

## 6. Fase 2 — Regenerar los CLAUDE.md afectados

**CRITICAL**: lanza **un subagente por cada carpeta afectada identificada en §5.3**, todos en **una única respuesta** con N invocaciones a `Agent`. **REQUIRED**: exactamente N subagentes (N = número de carpetas afectadas). **MUST NOT** lanzarlos secuencialmente. **MUST NOT** usar `run_in_background` (necesitas los resultados para Fase 3).

**REQUIRED — restricciones de los subagentes**:

- **MUST NOT** invocar `AskUserQuestion`. Cualquier pregunta la hace el agente principal antes o después.
- **MUST NOT** leer otras carpetas distintas de la asignada.
- **MUST** escribir el `CLAUDE.md` sobrescribiendo si existe.

### 6.1 Plantilla literal del prompt del subagente

El prompt **MUST** ser literal (copiar tal cual, sustituyendo `{ruta-carpeta}`):

````text
Eres un generador de CLAUDE.md para una carpeta de código.

Tu tarea:
1. Lee TODOS los ficheros Java/Kotlin/XML de la carpeta `{ruta-carpeta}` y sus subcarpetas.
2. Si existe `{ruta-carpeta}/CLAUDE.md`, léelo. Lo usarás solo como referencia de estructura,
   nunca como fuente de verdad — el código manda.
3. Genera un nuevo CLAUDE.md que responda a este criterio:
   "¿Qué necesita saber un agente que nunca ha visto esta carpeta para trabajar en ella
   sin leer todo el código?"

CRITERIO DE INCLUSIÓN: Si un agente experimentado con el framework pero que nunca ha visto
esta carpeta podría inferirlo en 30 segundos leyendo el código → NO incluyas eso.
Si tardaría 10 minutos o requiere contexto externo → SÍ incluye.

FORMATO OBLIGATORIO del CLAUDE.md:

## ¿Para qué sirve esto?
[1-2 frases. Lo que no se infiere del nombre de la carpeta.]

## Lo no obvio
[Solo si hay algo que se desvía de la arquitectura estándar (DefaultModelService,
@CallMethod, action-views en XML, etc.), restricciones ocultas, workarounds, otras clases
además de los controladores y servicios, decisiones contraintuitivas. Si todo sigue el
patrón estándar, OMITE esta sección.]

## Controladores y métodos (Una tabla por controlador)
| Método | Qué hace en una línea |
|---|---|
| `NombreControlador.metodo(params)` | descripción |
[Solo métodos públicos relevantes — no getters/setters]

## Servicios y métodos públicos (Una tabla por servicio)
| Método | Qué hace en una línea |
|---|---|
| `NombreService.metodo(params)` | descripción |
[Solo métodos públicos relevantes — no getters/setters, no métodos heredados de DefaultModelService]

## Repositorios y métodos públicos (Una tabla por repositorio)
| Método | Qué hace en una línea |
|---|---|
| `NombreRepository.metodo(params)` | descripción |
[Solo métodos públicos relevantes — no getters/setters, no métodos heredados de JpaRepository]

## Vistas   (Una tabla por vista)
| Vista | Para qué |
|---|---|
| `nombre-vista` | descripción |

## Dependencias

Tabla con dependencias con otros subsistemas
| Subsistema | Para qué |
|---|---|
| `nombre-subsistema` | descripción del motivo |

Tabla con dependencias con infraestructura
| Subsistema | Para qué |
|---|---|
| `nombre-subsistema` | descripción del motivo |

QUÉ NO INCLUIR (aunque lo veas en el código):
- Campos de entidades (ya están en los XML de dominio)
- Que existe un servicio o repositorio (es la arquitectura estándar)
- Javadoc de métodos privados
- Nada que sea consecuencia directa del nombre de la carpeta

Escribe el CLAUDE.md en `{ruta-carpeta}/CLAUDE.md`. Sobreescríbelo si ya existe.
No escribas nada más. No expliques lo que has hecho.
No hagas preguntas al usuario. No invoques AskUserQuestion bajo ninguna circunstancia.
````

### 6.2 Checklist del subagente

Antes de devolver, el subagente **MUST** auto-verificar:

- [ ] ¿Existe el fichero `{ruta-carpeta}/CLAUDE.md` recién escrito?
- [ ] ¿Contiene al menos la sección `## ¿Para qué sirve esto?`?
- [ ] ¿No quedó vacío (>200 bytes razonable)?
- [ ] ¿Toda tabla con encabezado tiene al menos una fila, o se ha omitido la sección entera?

El subagente **MUST NOT** devolver si queda algún punto sin cumplir.

### 6.3 Relanzamiento del agente principal

Tras recoger los resultados de los N subagentes, el agente principal vuelve a aplicar el checklist §6.2 sobre cada fichero generado. Si alguno falla, **relánzalo** sobre la misma carpeta con el mismo prompt.

**LIMIT**: máximo 3 relanzamientos por carpeta. Si tras la 3ª sigue fallando, anota la incidencia y continúa con Fase 3 reportándolo en el mensaje final.

---

## 7. Fase 3 — Archivar los artefactos as-built

### 7.1 Determinar el número de spec

Lista `.sdd/specs/`. Considera solo carpetas cuyo nombre cumpla `^[0-9]{4}_`. Toma el máximo numérico y suma 1, con pad a 4 dígitos. Si no hay ninguna, el número es `0001`.

La descripción es el nombre de la iniciativa sin el prefijo de timestamp (todo lo que va tras `YYYY-MM-DD_HH-MM_`).

Destino: `.sdd/specs/{NNNN}_{descripcion}/`

- ✅ CORRECTO: iniciativa `2026-05-21_14-30_firmas-bulk` con último spec `0006_…` → destino `.sdd/specs/0007_firmas-bulk/`
- ❌ INCORRECTO: `.sdd/specs/7_firmas-bulk/` (falta pad a 4 dígitos)
- ❌ INCORRECTO: `.sdd/specs/0007_2026-05-21_14-30_firmas-bulk/` (lleva el timestamp original; **MUST** quitarlo)

### 7.2 Generar las versiones as-built

Cada artefacto del draft refleja una **intención** en un momento concreto. La spec archivada **MUST** reflejar la **realidad**. Aplica "el código manda" con diferente intensidad según el tipo de artefacto.

En cada uno de los tres, **MUST** añadir al final la sección `## Notas de cierre (as-built)` con la lista de cambios aplicados o explícitamente "Sin cambios respecto al draft original." (la sección es obligatoria aunque la lista esté vacía: hace explícita la verificación).

#### 7.2.a `user-story.md` — cambios excepcionales

- **Por defecto, copia literal del draft.** La intención del usuario no se falsea: si la implementación se desvió del objetivo, eso es información histórica valiosa que captura la nota de cierre, no un error a corregir reescribiendo la historia.
- **Solo se modifica si** la implementación reveló que un actor, una restricción dura o un caso de uso del flujo principal estaba **mal expresado** (contradictorio con el código aprobado, no meramente "incompleto" o "matizado").
  - ✅ Ejemplo: user-story decía "solo el administrador ve todas las solicitudes" pero se implementó (con razón) que también las ven los supervisores → corregir.
  - ✅ Ejemplo: user-story decía "una solicitud rechazada no se puede revertir" pero se implementó la posibilidad de reabrir → corregir.
- **MUST NOT** modificar para: añadir matices que se aclararon durante el análisis, ajustar redacción, completar secciones que estaban vacías porque el usuario las dejó así adrede.

#### 7.2.b `analysis.md` — correcciones de requisitos

Usando los ficheros del `git diff` de Fase 1, identifica divergencias entre lo analizado y lo implementado:

- **Validaciones `V-XXX`** no implementadas, o con condición/mensaje cambiado.
- **Reglas de negocio `R-XXX`** no implementadas, o con operación/momento/efecto cambiado.
- **Reglas de UI `U-XXX`** no implementadas, o con disparador/efecto/condición cambiado.
- **Campos de entidad** añadidos, eliminados o renombrados.
- **Operaciones** (endpoints, métodos públicos) con firma o nombre distinto.
- **Vistas** con nombre, granularidad o filtro distinto.
- **Reglas de seguridad** ajustadas.
- **Columna "Origen del valor"** de cada `entity-*.md`: para cada campo clasificado como `servidor`, verificar que el `*ServiceImpl.insert`/`update` real lo asigna o recalcula **incondicionalmente** (sin `if (campo == null)`); para cada campo `cliente`, verificar que el servicio NO lo asigna en una R-Antes-de-Crear. Si la realidad del código discrepa, corregir la clasificación en el `analysis.md` as-built y dejarlo en la nota de cierre. Ver `[[k-secure-coding]]` §2.

**REQUIRED**: si se ajustaron filas de alguna tabla `V-XXX`/`R-XXX`/`U-XXX`, **MUST** renumerar cada tabla por separado de forma **consecutiva sin huecos**, manteniendo el orden de aparición tras el ajuste.

- ✅ CORRECTO: tabla original `V-001, V-002, V-003, V-004`; se elimina la 2 → resultado `V-001, V-002, V-003` (renumeradas).
- ❌ INCORRECTO: tras eliminar `V-002`, dejar `V-001, V-003, V-004` (hueco en la numeración).
- ❌ INCORRECTO: renumerar conjuntamente V y R (mezclar categorías). Cada tabla se renumera **por separado**.

**MUST NOT** inventar una regla porque "el código la tiene": si la regla está en el código pero no estaba en el análisis original, añádela documentándola en la nota de cierre. Si el código **no** la implementó pero el análisis decía que sí, **MUST** borrarla del analysis y dejarlo en la nota de cierre.

#### 7.2.c `design.md` — as-built completo

Usando el `git diff`, identifica divergencias entre lo diseñado y lo implementado:

- Métodos añadidos, eliminados o con firma cambiada.
- Entidades con campos distintos.
- Vistas con nombres o estructura distinta.
- `V-XXX` que cambiaron de **capa de validación** (navegador `<action-validate>` ↔ servicio `validateInsert`/`validateUpdate` ↔ modelo JPA). **Nota terminológica**: aquí "capa" se refiere al sitio donde se ejecuta la validación, no al "Origen del valor" de un campo (ese eje vive en el `analysis.md` y se trata en §7.2.b).
- `R-XXX` que cambiaron de momento (Antes↔Después) u operación.
- `U-XXX` que cambiaron de mecanismo (atributo inline ↔ `<action-attrs>`/`<action-record>`).
- Matriz de trazabilidad: cada `V-XXX`, `R-XXX`, `U-XXX` **MUST** seguir apuntando a una ubicación real del código.
- **Sección "Frontera de confianza — AllowProperties por acción"**: para cada acción del servicio invocada desde `@CallMethod` (que tenga su `allowPropertiesXxx` declarado), verificar contra el código real (a) la **forma** declarada en `allowPropertiesXxx` (`createAllowProperties(Map.of(...))` whitelist / `createAllowAllProperties()` abierto) coincide con la implementación; (b) en whitelist, la lista real no contiene ningún campo `servidor` y enumera todos los `cliente` necesarios; (c) en abierto, **todos** los campos `servidor` se asignan **incondicionalmente** en la acción del `*ServiceImpl` real (sin `if (campo == null)`). Si discrepa, corregir el `design.md` as-built y registrar en la nota de cierre. Ver `[[k-secure-coding]]` §3.

#### 7.2.d Plantilla literal de la nota de cierre

Al final de cada uno de los tres ficheros archivados:

```markdown
## Notas de cierre (as-built)

Cambios aplicados respecto al draft original:
- {descripción breve de cada cambio}
```

Si no hubo cambios:

```markdown
## Notas de cierre (as-built)

Sin cambios respecto al draft original.
```

### 7.3 Escribir ficheros al destino

Escribe al destino las versiones as-built del paso 7.2:

- `user-story.md` — as-built de §7.2.a.
- `analysis.md` — as-built de §7.2.b.
- `design.md` — as-built de §7.2.c.
- `design-guidelines.md` — copia literal del draft **solo si existía**. Este fichero **MUST NOT** reescribirse: es la decisión local del subsistema y no cambia con la implementación.

---

## 8. Fase 4 — Reportar al usuario

Plantilla literal del mensaje final:

```text
Iniciativa cerrada: {nombre-iniciativa}

CLAUDE.md actualizados:
  - {ruta-carpeta-1}/CLAUDE.md
  - {ruta-carpeta-2}/CLAUDE.md
  ...

Spec archivada en: .sdd/specs/{NNNN}_{desc}/
  - user-story.md   ← {sin cambios | N ajustes — ver "Notas de cierre"}
  - analysis.md     ← {sin cambios | N correcciones — ver "Notas de cierre"}
  - design.md       ← {sin cambios | N divergencias — ver "Notas de cierre"}
  {- design-guidelines.md (si existía, copia literal)}
```

---

## Quick Guidelines

- **El código manda**, no la intención: `analysis.md` y `design.md` archivados reflejan el código; `user-story.md` solo se ajusta en contradicciones duras (§7.2.a).
- **`git diff` es la fuente de verdad** de qué cambió. **MUST NOT** explorar carpetas que el diff no señala.
- **Regenera** los `CLAUDE.md` desde cero usando el código; el CLAUDE.md previo es solo referencia de estructura. **MUST NOT** hacer merge textual.
- **Subagentes Fase 2 en paralelo**: una única respuesta con N invocaciones a `Agent`, sin `run_in_background`, sin `AskUserQuestion`.
- **Granularidad de carpeta afectada** = subsistema/sistema completo, no subcarpetas internas (`subsystem/firmas/`, no `subsystem/firmas/service/`).
- **No tocar el draft** (`.sdd/drafts/` queda intacto) ni leer otras specs (`.sdd/specs/*`) como referencia de contenido.
- **Numeración de spec**: 4 dígitos con pad cero (`0007_firmas-bulk`), sin timestamp en el nombre archivado.
- Nota de cierre **obligatoria** en los tres artefactos archivados, aunque sea para decir "Sin cambios".

---

## Apéndice A — Override de rutas (para testing)

Para ejecutar el skill sobre un sandbox alternativo sin tocar el árbol real:

- `--in=<ruta>` — ruta explícita al `design_NN.md` del draft. Desactiva la auto-detección de §4.1.
- `--out=<ruta>` — carpeta destino explícita para los artefactos as-built (sustituye `.sdd/specs/{NNNN}_{desc}/`).
- `--root=<ruta>` — raíz alternativa a `.sdd/` para resolver `drafts/` y `specs/`.

En uso normal no se especifican.