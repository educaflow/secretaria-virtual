# Contrato de plantilla — documentación de cierre de un sistema/subsistema

Esta carpeta es el **contrato** que lee el subagente **documentador** de `/sdd-close`. El `SKILL.md` es un motor agnóstico: **todo** lo específico de qué documentación se produce por sistema, su formato, cómo se deriva el modelo de datos y cómo se renderiza la imagen está **aquí**. Si cambias esta carpeta (o apuntas `--template-dir` a otra), cambias qué y cómo se documenta **sin tocar el skill**.

---

## 1. Ficheros de la plantilla

| Fichero | Lo lee | Para qué |
|---------|--------|----------|
| `README.md` (este) | el documentador | índice del contrato, contexto del proyecto, herramienta de render |
| `claude-md.md` | el documentador | formato exacto del `CLAUDE.md` del sistema + criterio de inclusión + checklist |
| `data-model.md` | el documentador | cómo derivar `modelo.puml` de los `domains/*.xml`, mapeo XML→PlantUML, comando de render a `modelo.png` + checklist |

El documentador **MUST** leer los tres antes de empezar.

---

## 2. Rol: documentador (uno por sistema/subsistema afectado)

| Entrada propia | Lee de esta plantilla | Resultado |
|---|---|---|
| la ruta de **un** sistema/subsistema afectado (`src/main/java/com/educaflow/{...}/`) | `README.md` + `claude-md.md` + `data-model.md` | en la **raíz** de ese sistema: `CLAUDE.md`, `modelo.puml`, `modelo.png` + token `DOCUMENTADO:` |

El documentador:

- **MUST** regenerar la documentación **desde el código real** del sistema (Java/Kotlin/XML y subcarpetas). El código es la **única** fuente de verdad.
- **MUST NOT** leer ni usar nada bajo `.sdd/` como fuente de contenido (specification/analysis/design **no** son input). La documentación describe lo que el código **es**, no lo que se planeó.
- **MUST NOT** hacer merge textual con el `CLAUDE.md` previo: se **regenera desde cero**; el previo es solo referencia de estructura.
- **MUST NOT** modificar código de la aplicación: solo escribe los ficheros de documentación en la raíz del sistema (`CLAUDE.md`, `modelo.puml`, `modelo.png`).
- **MUST NOT** usar `AskUserQuestion`: ante un bloqueo devuelve `BLOQUEADO: {ruta-sistema} — {motivo}`.
- **MUST NOT** pegar el contenido de los ficheros en la respuesta (ya está en disco): solo el token.
- **MUST** cargar el skill `/k-sistemas` (estructura de sistemas, modelos de dominio, servicios y controladores) para interpretar el código. Si el modelo de datos tiene dudas de dominio, también `/k-validaciones`.

### 2.1 Orden de trabajo del documentador

1. Lee este `README.md`, `claude-md.md` y `data-model.md`.
2. Carga `/k-sistemas`. Lee el código del sistema asignado (en especial `domains/*.xml`, `services/`, `controllers/`, `views/`).
3. Escribe `CLAUDE.md` siguiendo `claude-md.md`.
4. Escribe `modelo.puml` siguiendo `data-model.md` y **renderiza** `modelo.png` con el comando de §3.
5. Aplica los checklists de `claude-md.md` y `data-model.md` (**LIMIT** 3 iteraciones de autocorrección).
6. Responde con el token único.

---

## 3. Render de la imagen (lo hace el documentador)

El modelo de datos se entrega como `modelo.puml` (texto PlantUML) **y** su render `modelo.png`, ambos en la raíz del sistema. **El jar de PlantUML NO se vendoriza**: ya está en el repositorio Maven local (`~/.m2`) porque el proyecto `EducaFlowBuildTools` lo usa. **MUST** resolver la **última** versión disponible dinámicamente (no fijar la versión en el comando):

```bash
PLANTUML_JAR=$(find ~/.m2/repository/net/sourceforge/plantuml/plantuml \
  -name 'plantuml-*.jar' ! -name '*-sources.jar' ! -name '*-javadoc.jar' | sort -V | tail -1)
```

Si `PLANTUML_JAR` queda vacío (no está en `.m2`) → `BLOQUEADO: {ruta-sistema} — no se encuentra plantuml.jar en ~/.m2 (¿falta build de EducaFlowBuildTools?)`.

**Comando de render** (ejecútalo con `Bash` desde la raíz del repo, sustituyendo `{ruta-sistema}`):

```bash
java -Djava.awt.headless=true -Djava.io.tmpdir="${TMPDIR:-/tmp}" \
  -jar "$PLANTUML_JAR" -tpng {ruta-sistema}/modelo.puml
```

Genera `{ruta-sistema}/modelo.png` junto al `.puml`. Reglas:

- **CRITICAL — `-Djava.awt.headless=true`** es obligatorio: sin él PlantUML aborta buscando un servidor X11 (`Can't connect to X11 window server`).
- **CRITICAL — `-Djava.io.tmpdir="${TMPDIR:-/tmp}"`** es obligatorio: ImageIO escribe ficheros de caché temporales y `/tmp` puede estar en solo lectura (sandbox); apuntarlo a `$TMPDIR` lo evita (`Can't create cache file!`).
- El render usa el `graphviz/dot` del sistema (ya instalado) para los diagramas de entidades.
- **Verifica** que `modelo.png` existe y **no está vacío** (`> 0 bytes`) tras el render. Si está a 0 bytes o el comando falla, el `.puml` tiene un error de sintaxis: corrígelo y vuelve a renderizar (**LIMIT** 3). Si tras la 3ª no renderiza → `BLOQUEADO: {ruta-sistema} — el modelo.puml no renderiza: {último error}`.

- ✅ CORRECTO: `modelo.png` existe, `file modelo.png` dice `PNG image data`.
- ❌ INCORRECTO: dejar `modelo.png` a 0 bytes (render fallido silencioso) o dar `DOCUMENTADO` sin haber renderizado.

---

## 4. Contexto del proyecto

- La app es una secretaría virtual sobre **Axelor**: entidades JPA definidas en `domains/*.xml` (dominios), servicios `*Service`/`*ServiceImpl`, controladores con `@CallMethod`, vistas en XML. Lo describe `/k-sistemas` (cárgalo).
- Estructura: `subsystem/{nombre}/` y `system/{nombre}/` para los sistemas de negocio; `base/infrastructure/{nombre}/` y `base/util/` para la base. Cada uno **dueño** de sus `domains/*.xml`.
- El modelo de datos de un sistema es el conjunto de entidades de **sus** `domains/*.xml`. Las relaciones a entidades de **otros** sistemas se dibujan como referencia externa (ver `data-model.md`).
- Un sistema puede **no tener** `domains/*.xml` (p.ej. utilidades): en ese caso **no** se genera `modelo.puml` ni `modelo.png` (lo detalla `data-model.md`); el `CLAUDE.md` sí se genera siempre.

---

## 5. Convención de nombres de salida

| Fichero | Ubicación | Obligatorio |
|---------|-----------|-------------|
| `CLAUDE.md` | raíz del sistema | siempre |
| `modelo.puml` | raíz del sistema | solo si el sistema tiene `domains/*.xml` |
| `modelo.png` | raíz del sistema | solo si existe `modelo.puml` |

- ✅ CORRECTO: `src/main/java/com/educaflow/subsystem/firmas/{CLAUDE.md,modelo.puml,modelo.png}`
- ❌ INCORRECTO: poner el `.puml`/`.png` en una subcarpeta (`domains/modelo.puml`) o con otro nombre (`firmas.puml`).
