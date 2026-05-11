---
name: sdd-close-spec
description: Cierra una iniciativa SDD — lee el último draft (user-story + analysis + design), usa `git diff` para identificar qué ficheros realmente cambiaron, actualiza los CLAUDE.md de cada carpeta afectada regenerándolos desde el código real, y archiva los artefactos en .sdd/specs/NNNN_desc/ con versiones as-built: el design.md y el analysis.md se corrigen para reflejar lo que se implementó (validaciones reales, firmas reales, vistas reales) y el user-story.md se ajusta solo si la implementación reveló contradicciones evidentes con la intención original.
---

# sdd-close-spec

Eres el paso de cierre del pipeline SDD. Tu trabajo tiene tres entregables concretos:

1. **CLAUDE.md actualizados** — uno por cada carpeta de código afectada, generados desde el código real.
2. **Artefactos archivados** en `.sdd/specs/NNNN_desc/` — los tres artefactos (`user-story.md`, `analysis.md`, `design.md`) en versión **as-built**: el `analysis.md` y el `design.md` se corrigen para reflejar la implementación real, y el `user-story.md` se ajusta solo en cambios excepcionales (ver Fase 3.2.a).
3. **Confirmación al usuario** de qué se cerró y dónde quedó todo, indicando para cada artefacto archivado si hubo cambios respecto al draft.

---

## Fase 0 — Localizar y confirmar el draft

### Argumentos aceptados

```
/sdd-close-spec [ruta-design] [hash-commit-base]
```

- `ruta-design` (opcional): ruta al `design_NN.md` del draft. Si se omite, se auto-detecta (paso 0.1).
- `hash-commit-base` (opcional): hash del commit anterior al inicio de la iniciativa. Si se pasa, el `git diff` de la Fase 1 abarcará desde ese commit hasta el workspace incluido (commits posteriores + staged + unstaged + untracked); si no, el diff cubre solo lo que hay en el workspace contra HEAD (sin comitear + untracked).

### 0.1 Localizar el último draft

Si el usuario no proporciona ruta:

1. Lista las subcarpetas de `.sdd/drafts/` cuyo nombre empieza por `^[0-9]{4}-[0-9]{2}-[0-9]{2}_[0-9]{2}-[0-9]{2}_`. Ordénalas alfabéticamente (el prefijo timestamp hace que orden alfabético = cronológico). Toma la última.
2. Dentro de esa iniciativa, toma la subcarpeta `analysis_NN/` con el número más alto.
3. Dentro de esa subcarpeta, toma el fichero `design_NN.md` con el número más alto.
4. Si no hay iniciativas, indica al usuario que no hay drafts y detente.

### 0.2 Confirmar con el usuario

Muestra al usuario con `AskUserQuestion`:

> Voy a cerrar la iniciativa: `{nombre-iniciativa}`
> Draft: `{ruta/design_NN.md}`
> ¿Continuamos?

Opciones: "Sí, cerrar esta iniciativa" / "No, quiero indicar otra ruta". Si "No", pide la ruta y vuelve al paso 0.1 con esa ruta.

### 0.3 Leer artefactos del draft

Lee en paralelo:
- `user-story.md` (dos niveles arriba del design)
- `analysis.md` (mismo nivel que el design)
- `design_NN.md` (el localizado)
- `design-guidelines.md` (opcional, junto al user-story — no es error si no existe)

Valida los frontmatter de todos los artefactos. Si alguno no coincide, detente con error indicando qué fichero falla y qué `type:` se esperaba:
- `user-story.md` debe contener `type: user-story`.
- `analysis.md` debe contener `type: analysis`.
- `design_NN.md` debe contener `type: design`.
- `design-guidelines.md`, si existe, debe contener `type: design-guidelines`.

---

## Fase 1 — Identificar qué cambió realmente

Esta es la fuente de verdad: **el código real, no el diseño**.

### 1.1 Git diff para ficheros modificados

**Antes de ejecutar el diff, anuncia al usuario qué modo vas a usar.** Una sola línea, con el formato:

- Sin hash: `Buscando diferencias solo en el workspace (cambios sin comitear contra HEAD).`
- Con hash: `Buscando diferencias desde el commit {hash-corto} hasta el workspace (commits posteriores + cambios sin comitear).`

Esto le da al usuario la oportunidad de corregirte si el modo no es el correcto antes de procesar el diff completo.

El diff debe abarcar **desde un punto base hasta el workspace incluido** (commits posteriores al base + staged + unstaged + untracked). El punto base depende de si el usuario pasó un hash:

**Por defecto (sin hash) — el punto base es HEAD.** Asume que las modificaciones de la iniciativa están sin comitear:

```bash
git diff --name-only HEAD
git status --porcelain
```

`git diff --name-only HEAD` cubre staged + unstaged respecto a HEAD; `git status --porcelain` añade los untracked (nuevos sin añadir al index). Unifica ambas listas.

**Si el usuario pasó un hash — el punto base es ese hash.** En este caso el diff debe cubrir todo el rango desde el hash hasta el workspace, incluyendo commits intermedios, staged, unstaged y untracked:

```bash
git diff --name-only {hash-base}
git status --porcelain
```

`git diff --name-only {hash-base}` (sin segundo argumento) compara el workspace contra ese commit, por lo que ya incluye en una sola pasada los commits posteriores y los cambios staged/unstaged. `git status --porcelain` añade los untracked. Unifica ambas listas.

**Si no hay nada que mostrar** (las listas vacías y no se ha pasado hash), detente y avisa al usuario: la iniciativa no parece haber tocado código, o las modificaciones están en commits anteriores y necesitas un hash base.

### 1.2 Identificar carpetas afectadas

A partir de la lista de ficheros modificados, identifica qué carpetas dentro de `src/main/java/com/educaflow/` están afectadas. Las carpetas candidatas a tener `CLAUDE.md` son:

- `subsystem/{nombre}/` — si hay ficheros en ese subsistema
- `system/{nombre}/` — si hay ficheros en ese sistema
- `base/infrastructure/{nombre}/` — si hay ficheros en esa carpeta de infraestructura
- `base/util/` — si hay ficheros en util

**Regla**: si el diff toca ficheros de `subsystem/firmas/service/` y `subsystem/firmas/controller/`, la carpeta afectada es `subsystem/firmas/` (el nivel del subsistema, no sus subcarpetas).

---

## Fase 2 — Actualizar CLAUDE.md de cada carpeta afectada

Para cada carpeta afectada, lanza un subagente en paralelo. Todos los subagentes se lanzan en una única respuesta (una invocación a `Agent` por carpeta).

### Prompt del subagente (autocontenido)

El prompt que recibirá cada subagente debe incluir literalmente:

```
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
@CallMethod, action-views en XML, etc.), restricciones ocultas, workarounds, otras clases además de los controladores y servicios
decisiones contraintuitivas. Si todo sigue el patrón estándar, OMITE esta sección.]

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
| `NombreService.metodo(params)` | descripción |
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
```

Sustituye `{ruta-carpeta}` por la ruta real de cada carpeta.

### Esperar a todos los subagentes

Recoge los resultados de todos los subagentes antes de continuar a la Fase 3.

---

## Fase 3 — Archivar los artefactos

### 3.1 Determinar el número de spec

Lista las entradas de `.sdd/specs/`. Considera solo las carpetas cuyo nombre empiece por 4 dígitos seguidos de `_` (regex `^[0-9]{4}_`). Toma el máximo de esos números y suma 1. Formato 4 dígitos: `0001`, `0002`…

Si no hay carpetas que cumplan el patrón, el número es `0001`.

La descripción es el nombre de la iniciativa sin el prefijo de timestamp (todo lo que va después de `YYYY-MM-DD_HH-MM_`).

Destino: `.sdd/specs/{NNNN}_{descripcion}/`

### 3.2 Generar las versiones as-built de los tres artefactos

Cada artefacto del draft refleja una **intención** en un momento concreto. La spec archivada debe reflejar la **realidad**. Aplica el principio "el código manda" a los tres niveles, pero con diferente intensidad según el tipo de artefacto.

En cada uno de los tres, **añade al final una sección "Notas de cierre (as-built)"** listando los cambios aplicados (o "Sin cambios respecto al draft" si no hubo). Esa sección es obligatoria aunque la lista esté vacía: hace explícita la verificación.

#### 3.2.a `user-story.md` — cambios excepcionales

- **Por defecto, copia literal del draft.** La intención del usuario no debe falsearse: si la implementación se desvió del objetivo, eso es información histórica valiosa que la nota de cierre captura, no un error a corregir reescribiendo la historia.
- **Solo se modifica si** la implementación reveló que un actor, una restricción dura o un caso de uso del flujo principal estaba **mal expresado** (no "incompleto" o "matizado", sino contradictorio con el código que se implementó y aprobó). Ejemplos:
  - La user-story decía "solo el administrador ve todas las solicitudes" pero se implementó (con razón) que también las ven los supervisores → corregir.
  - La user-story decía "una solicitud rechazada no se puede revertir" pero se implementó la posibilidad de reabrir → corregir.
- **No se modifica** para: añadir matices que se aclararon durante el análisis, ajustar redacción, completar secciones que estaban vacías porque el usuario las dejó así adrede.

#### 3.2.b `analysis.md` — correcciones de requisitos

- Lee el `analysis.md` del draft.
- Usando los ficheros del `git diff` de la Fase 1, identifica divergencias entre lo analizado y lo implementado:
  - **Validaciones V-XXX** que no se implementaron, o cuya condición/mensaje cambió.
  - **Campos de entidad** añadidos, eliminados o renombrados.
  - **Operaciones** (endpoints, métodos públicos) con firma o nombre distinto.
  - **Vistas** con nombre, granularidad o filtro distinto.
  - **Reglas de seguridad** ajustadas durante la implementación.
- Aplica las correcciones al `analysis.md`. Si se ajustaron filas de la tabla `V-XXX`, **renumera consecutivamente sin huecos**. Si el código no validó algo que el análisis decía que sí, NO inventes la validación: bórrala del analysis y déjalo reflejado en la nota de cierre.

#### 3.2.c `design.md` — as-built completo

- Lee el `design_NN.md` del draft.
- Usando los ficheros del `git diff`, identifica divergencias entre lo diseñado y lo implementado:
  - Métodos añadidos, eliminados o con firma cambiada.
  - Entidades con campos distintos.
  - Vistas con nombres o estructura distinta.
  - Validaciones que cambiaron de capa (cliente↔servidor).
  - Matriz de trazabilidad: cada V-XXX/RN debe seguir apuntando a una ubicación real del código.
- Aplica las correcciones al diseño.

Formato común de la nota de cierre (al final de cada uno de los tres ficheros):

```markdown
## Notas de cierre (as-built)

Cambios aplicados respecto al draft original:
- {descripción breve de cada cambio, o "Sin cambios respecto al draft original."}
```

### 3.3 Escribir ficheros al destino

Escribe al destino las versiones **as-built** producidas en el paso 3.2 (no copias literales del draft, salvo si la versión as-built coincide tal cual con el original):

- `user-story.md` — versión as-built del paso 3.2.a (casi siempre idéntica al draft + nota de cierre vacía)
- `analysis.md` — versión as-built del paso 3.2.b
- `design.md` — versión as-built del paso 3.2.c
- `design-guidelines.md` — solo si existía en el draft; copia literal (este fichero no se reescribe, es la decisión local del subsistema y no cambia con la implementación)

---

## Fase 4 — Mensaje final al usuario

```
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

## Reglas críticas

- **El código manda, no la intención.** Si el código difiere de lo planeado, los artefactos archivados (`analysis.md`, `design.md`) reflejan el código. El `user-story.md` solo se ajusta en cambios excepcionales (ver 3.2.a); en lo demás, conserva la intención original.
- **git diff es la fuente de verdad** para saber qué cambió. No leas carpetas enteras si el diff te dice que no se tocaron.
- **Regenera el CLAUDE.md desde cero** usando el código como fuente. El CLAUDE.md anterior es solo referencia de estructura. Nunca hagas merge textual.
- **No modifiques el draft original.** Los ficheros en `.sdd/drafts/` quedan intactos.
- **No leas specs anteriores** de `.sdd/specs/` como referencia para el contenido. El contenido viene del código.
- **Los subagentes de Fase 2 se lanzan en paralelo** — una sola respuesta con múltiples invocaciones a `Agent`.

## Cuándo parar y pedir ayuda

Detente y avisa al usuario si:
- Las tres listas de Fase 1.1 (`git diff HEAD`, status untracked y, en su caso, `git diff {hash-base} HEAD`) salen vacías. Probablemente la iniciativa ya está comiteada y el usuario debe pasar el hash base.
- Una carpeta afectada no tiene código legible (error de acceso, vacía, etc.).
- El draft no tiene los tres artefactos mínimos (user-story, analysis, design).
