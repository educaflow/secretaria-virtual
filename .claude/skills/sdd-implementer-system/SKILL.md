---
name: sdd-implementer-system
description: Dado un `design.md` ya producido por `/sdd-designer-system`, lo descompone en una lista de tareas atómicas (`implementation/task_NN.md` más el índice `task.md`) con una tarea por fichero, agrupando los ficheros fuertemente acoplados, donde cada tarea lleva sus skills de dominio y el texto verbatim del diseño que la describe. Tras la aprobación del usuario, implementa cada tarea delegando en `code-implementer` (o copiando literalmente los XML ya materializados), y por último compila el proyecto en un bucle de auto-corrección hasta que `./gradlew clean build` pase (LIMIT 3 iteraciones). Es el quinto paso del pipeline SDD; la entrada la produce `/sdd-designer-system` y la salida es código real en `src/main/...` listo para `/sdd-close-spec`.
handoffs:
  - label: Cerrar la iniciativa
    agent: sdd-close-spec
    prompt: Cerrar la iniciativa recién implementada — archivar en .sdd/specs/ y actualizar los CLAUDE.md afectados.
allowed-tools: Bash(playwright-cli:*) Bash(ls:*) Bash(grep:*) Bash(cp:*) Bash(mkdir:*) Bash(find:*) Bash(xmllint:*) Read Edit(src/**) Edit(.sdd/**) Write(src/**) Write(.sdd/**) Bash(./gradlew:*) Skill AskUserQuestion Agent
---

# sdd-implementer-system

Eres un delegador. Conviertes un `design.md` ya producido por `/sdd-designer-system` en código real dentro del proyecto en tres movimientos: 
1. **descompones** el diseño en una lista de tareas atómicas escritas en `implementation/`, una por fichero (agrupando ficheros fuertemente acoplados), cada una con sus skills de dominio y el texto del diseño que la describe
2. Tras la aprobación del usuario, **implementas** cada tarea delegando en `code-implementer` o copiando literalmente los XML ya materializados por el diseñador
3. **compilas** el proyecto en un bucle de auto-corrección hasta que `./gradlew clean build` pase. Es el quinto paso del pipeline SDD: la entrada la produce `/sdd-designer-system` y la salida es código real en `src/main/...` listo para ser cerrado con `/sdd-close-spec`.

---

## User Input

```text
$ARGUMENTS
```

You **MUST** consider the user input before proceeding (if not empty). Los argumentos esperables son:

- Una **ruta explícita** a un `design.md` (p.ej. `.sdd/drafts/2026-05-21_20-14_correos/design/design.md`). Se valida que el fichero existe dentro de `.sdd/drafts/{iniciativa}/design/` y se entra directamente en la Fase 1.
- **Sin argumentos** → se activa la auto-detección de la Fase 0 (última carpeta `.sdd/drafts/YYYY-MM-DD_HH-MM_*` por orden alfabético del prefijo timestamp que contenga `design/design.md`).
- Overrides de testing del Apéndice A (`--in=`, `--root=`).

---

## Outline

1. **Localizar** el `design.md` (Fase 0 — ruta explícita o auto-detección).
2. **Validar** el frontmatter del diseño (Fase 1 — `type: design` REQUIRED).
3. **Generar las tareas** en `implementation/` (Fase 2 — una `task_NN.md` por fichero, agrupando acoplados).
4. **Generar el índice** `implementation/task.md` (Fase 3).
5. **Pedir revisión** al usuario y **STOP** hasta que apruebe (Fase 4).
6. **Implementar** cada tarea: delegar Java en `code-implementer`, copiar literal los XML materializados (Fase 5).
7. **Compilar** en bucle hasta que `./gradlew clean build` pase (Fase 6 — `**LIMIT**: 3 iteraciones`).
8. **Cerrar** con mensaje final apuntando a `/sdd-close-spec` (Fase 7).

**STOP conditions**:

- El fichero de entrada no tiene `type: design` en el frontmatter → **ERROR** y detente sin escribir nada.
- El `design.md` no contiene la tabla "Ficheros a crear o modificar" → **ERROR** y detente.
- Un XML ya materializado en `design/` está mal y `code-implementer` lo detecta → **STOP**, no editarlo aquí; volver a `/sdd-designer-system`.
- Conflicto al sobrescribir un fichero destino o un `<menuitem>` ya existente → **STOP** y `AskUserQuestion` (sobrescribir / mantener / abortar).
- La validación con `xmllint` del `menus.xml` fusionado falla → **STOP** sin continuar con esa tarea.
- El usuario no aprueba la lista de tareas en la Fase 4 → **MUST NOT** implementar nada.
- `code-implementer` reporta un bloqueo (dependencia inexistente, instrucción ambigua, recurso no disponible) → **STOP** y pregunta al usuario.
- Tras `**LIMIT**: 3 iteraciones` del bucle de compilación el proyecto sigue sin compilar → **STOP** y `AskUserQuestion`.

---

## 1. Entrada y salida

### 1.1 Entrada

Un único `design.md` cuyo frontmatter debe contener (al menos) `type: design`. Acompañando al `design.md`, en su misma carpeta `design/`, el diseñador ya dejó materializados los XML del diseño y, opcionalmente, las reglas complejas:

```
.sdd/drafts/{iniciativa}/design/
├── design.md
├── domains/<Entidad>.xml          ← uno por entidad (XML materializado, ya validado con xmllint)
├── views/<Fichero>.xml            ← uno por <action-view> (XML materializado)
├── menus.xml                      ← porción de <menuitem> a fusionar
└── rules/R-<Entidad>-NNN.md       ← opcional, solo documentación referenciada por el design.md
```

Los XML **ya están validados con `xmllint`** por el diseñador. Son la fuente de verdad: este skill los copia tal cual, no los regenera.

### 1.2 Salida

Este skill escribe en dos sitios:

- En `.sdd/drafts/{iniciativa}/implementation/`: la lista de tareas (`task_NN.md`) y su índice (`task.md`).
- En el árbol del proyecto (`src/main/java/com/educaflow/...`): los XML del diseño copiados/fusionados a su ubicación real y todo el código Java escrito por `code-implementer`.
- En la conversación: un mensaje final indicando que la implementación está completa y que el siguiente paso es `/sdd-close-spec`.

### 1.3 Estructura de carpetas

```
.sdd/
└── drafts/
    └── YYYY-MM-DD_HH-MM_{resumen}/          ← carpeta de la iniciativa
        ├── analysis/                        ← input del designer
        ├── design/                          ← input de este skill
        │   ├── design.md
        │   ├── domains/<Entidad>.xml
        │   ├── views/<Fichero>.xml
        │   ├── menus.xml
        │   └── rules/R-<Entidad>-NNN.md  (opcional)
        └── implementation/                  ← salida de la Fase 2 y 3 de este skill
            ├── task.md                       ← índice (type: implementation-tasks)
            ├── task_01.md                    ← una tarea (type: implementation-task)
            ├── task_02.md
            └── …

src/main/java/com/educaflow/
├── <capa>/<x>/domains/<Entidad>.xml          ← destino de los dominios
├── <capa>/<x>/views/<Fichero>.xml            ← destino de las vistas
├── secretariavirtual/menus/menus.xml         ← destino único de menús (fusión)
└── <capa>/<x>/...                            ← código Java (escrito por code-implementer)
```

---

## 2. Principios (aplican a todas las fases)

### 2.1 No regenerar los XML materializados — copiarlos literalmente

Los XML de `design/domains/`, `design/views/` y `design/menus.xml` son la fuente de verdad: el diseñador ya los validó con `xmllint` contra sus XSD. **MUST** copiarlos tal cual al destino.

**MUST NOT**:

- **MUST NOT** reescribir los XML desde el `design.md`.
- **MUST NOT** reformatearlos al vuelo (cambios de indentación, reordenar atributos, etc.).

Re-generarlos pierde correcciones manuales aplicadas al diseño, rompe la validación del designer e introduce divergencias silenciosas. Si al copiar detectas que un XML del diseño está mal, **STOP** y pide al usuario reabrir `/sdd-designer-system`. **MUST NOT** arreglarlo aquí.

### 2.2 No implementar Java directamente — delegar en `code-implementer`

Este skill **MUST NOT** escribir código Java. Toda la implementación de Java (servicios, controladores, repositorios, datos iniciales, seguridad, jobs) se delega en `code-implementer`, pasándole **el texto de la tarea tal cual** (Fase 2) y sus skills de dominio. **CRITICAL**: `k-secure-coding` se incluye **siempre** en cualquier tarea Java que toque entidades, servicios o controladores — define defensas (mass-assignment, AllowProperties, asignación incondicional de campos `servidor`, multi-centro/IDOR, JPQL, adjuntos) que protegen al resto del sistema.

### 2.3 Los XML ya copiados son contrato fijo para el Java

Cuando `code-implementer` escriba Java, los XML de dominios y vistas ya están en su ubicación real. Esto significa:

- Las firmas de los métodos Java deben coincidir con las acciones declaradas en las vistas (`<action-method method="action-..." class="..."/>` ↔ controlador.método).
- Las entidades JPA generadas deben coincidir con los dominios XML (nombres de campos, tipos, relaciones).
- Si `code-implementer` detecta que un XML ya copiado tiene un error, debe **detenerse y notificar** — no editarlo. Corregirlo requiere volver a `/sdd-designer-system`.

### 2.4 Detenerse y preguntar ante un bloqueo

**STOP** y preguntar es la respuesta correcta ante:

- Una dependencia declarada en el diseño que no existe o tiene una API diferente.
- Una instrucción del diseño ambigua o contradictoria con el código existente.
- Una verificación que falla repetidamente y cuyo motivo no está cubierto en el diseño.
- Un recurso requerido (fichero, certificado, credencial, clase generada) que no está disponible.
- Un fichero XML ya copiado que contiene un error.

**CRITICAL**: **MUST NOT** adivinar ni inventar soluciones. Continuar a ciegas ante un bloqueo genera deuda técnica silenciosa.

`AskUserQuestion` solo se usa para lo imprescindible: confirmación de la ruta del diseño detectado (Fase 0), aprobación de la lista de tareas (Fase 4), conflictos al sobrescribir ficheros o `<menuitem>`, y decisión tras agotar el bucle de compilación (Fase 6). **MUST NOT** pedir aprobaciones cosméticas.

### 2.5 Una tarea por fichero, agrupando ficheros acoplados

Cada fila de la tabla "Ficheros a crear o modificar" genera **una tarea**, salvo los ficheros **fuertemente acoplados**, que van juntos en **una sola tarea**. Están fuertemente acoplados ficheros que no tienen sentido implementar por separado:

- ✅ AGRUPAR: una interfaz `XService`, su `XServiceImpl` y su `XInsertDTO` → una tarea de "servicio X".
- ✅ AGRUPAR: clases auxiliares privadas de un servicio (factories, records de resultado) con ese servicio.
- ❌ NO AGRUPAR: `Correo.xml` (dominio) con `CorreoController.java` (capas distintas, contratos distintos).
- ❌ NO AGRUPAR: dos vistas distintas (`Correo-Todos.xml` y `Correo-MiCentro.xml`) que el diseño describe por separado.

**LIMIT**: una tarea agrupa como mucho los ficheros de **un único componente lógico** (un servicio con su impl/DTO/auxiliares, o un dominio con su enum embebido). Si dudas, **NO agrupes**.

---

## 3. Flujo general

```
┌─────────────────────────────────────────────────────────────────────┐
│  Fase 0   Localizar el design.md                                    │
│  Fase 1   Validar el frontmatter del diseño                         │
│  Fase 2   Generar las tareas en implementation/task_NN.md           │
│             ├── 6.1  Leer design.md íntegro + tabla de ficheros     │
│             ├── 6.2  Agrupar ficheros en tareas                     │
│             ├── 6.3  Determinar skills de cada tarea                 │
│             └── 6.4  Escribir cada task_NN.md                       │
│  Fase 3   Generar el índice implementation/task.md                  │
│  Fase 4   Pedir revisión al usuario y STOP hasta aprobación         │
│  Fase 5   Implementar cada tarea (code-implementer / copia XML)     │
│  Fase 6   Compilar en bucle hasta que pase (LIMIT 3 iteraciones)    │
│  Fase 7   Mensaje final al usuario                                  │
└─────────────────────────────────────────────────────────────────────┘
```

Las fases se ejecutan **estrictamente en orden**. No se implementa ninguna tarea (Fase 5) hasta que el usuario apruebe la lista (Fase 4).

---

## 4. Fase 0 — Localizar el diseño

### 4.1 Caso 1 — Ruta explícita

Si el usuario invoca el skill con una ruta (p.ej. `.sdd/drafts/2026-05-21_20-14_correos/design/design.md`):

1. Comprueba que el fichero existe y está dentro de `.sdd/drafts/{iniciativa}/design/`.
2. La **carpeta de la iniciativa** es la que contiene la subcarpeta `design/`.
3. Pasa a la Fase 1 con esa ruta.

### 4.2 Caso 2 — Sin ruta (auto-detección)

Si el skill se invoca sin argumentos:

1. Listar las subcarpetas de `.sdd/drafts/` cuyo nombre cumple `^[0-9]{4}-[0-9]{2}-[0-9]{2}_[0-9]{2}-[0-9]{2}_`:
   ```bash
   ls -d .sdd/drafts/[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]_[0-9][0-9]-[0-9][0-9]_*/ 2>/dev/null
   ```
2. Ordenar alfabéticamente (el prefijo timestamp hace que el orden alfabético coincida con el cronológico) y tomar la **última**.
3. Comprobar que esa iniciativa contiene `design/design.md`:
   ```bash
   ls .sdd/drafts/{iniciativa}/design/design.md 2>/dev/null
   ```
4. Si no hay ninguna carpeta con ese formato o la última no contiene `design/design.md`, indica al usuario que no hay diseños disponibles y pide una ruta. Detente.
5. Muestra al usuario la ruta detectada y pregunta con `AskUserQuestion` si quiere usar ese diseño:
   - Sí → continuar con la Fase 1.
   - No → pedir la ruta del diseño a implementar. Detente.

**MUST NOT**:

- **MUST NOT** elegir una iniciativa que no sea la última por orden alfabético del prefijo timestamp.
- **MUST NOT** usar `mtime` o cualquier criterio distinto del orden alfabético del timestamp.
- **MUST NOT** continuar sin confirmación del usuario tras mostrar la ruta detectada.

---

## 5. Fase 1 — Validar el diseño

1. Lee el contenido del `design.md` antes de continuar.
2. **Valida el frontmatter.** El fichero debe comenzar con un bloque `---` … `---` que contenga la línea `type: design`. Puede haber más campos; solo `type` es obligatorio.
3. Si el frontmatter no contiene `type: design`, **STOP** y muestra este **ERROR** al usuario, sin continuar:

   > Error: el fichero `{ruta}` no es un diseño válido. Debe contener en el frontmatter:
   > ```
   > ---
   > type: design
   > ---
   > ```
   > Si tienes una historia de usuario, usa `/sdd-analyst-system`. Si tienes un análisis, usa `/sdd-designer-system`.

---

## 6. Fase 2 — Generar las tareas

Tu trabajo en esta fase es **descomponer el `design.md` en tareas atómicas** escritas en `.sdd/drafts/{iniciativa}/implementation/`. **MUST NOT** implementar nada todavía: solo escribir los ficheros de tarea.

### 6.1 Leer el `design.md` íntegro y su tabla de ficheros

1. Lee **todo** el `design.md`, no solo la tabla. La tabla dice **qué** ficheros hay; las secciones "Paso N", "Frontera de confianza / AllowProperties" y "Trazabilidad V/R/U" dicen **cómo** se implementa cada uno.
2. Localiza la tabla "Ficheros a crear o modificar". Cada fila tiene la forma:

   | Fichero | Acción | Skill | Descripción |
   |---------|--------|-------|-------------|
   | `subsystem/foo/domains/Bar.xml` | Crear | k-sistemas (modelos.md) | Entidad Bar |
   | `subsystem/foo/service/BarService.java` | Crear | k-sistemas, k-secure-coding | Interfaz del servicio |
   | `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` | Modificar | k-vistas (menus.md) | Añadir menú |

   Si no existe esa tabla → **ERROR** y detente (STOP condition).
3. Las rutas relativas tipo `subsystem/foo/domains/Bar.xml` se resuelven contra el prefijo estándar `src/main/java/com/educaflow/`. Las rutas que ya empiezan por `src/main/...` se usan tal cual.

### 6.2 Agrupar los ficheros en tareas

Aplica el principio 2.5: una tarea por fila, agrupando los ficheros fuertemente acoplados de un mismo componente lógico. Numera las tareas `01`, `02`, … en el orden lógico de implementación del diseño (normalmente: dominios → servicios → repositorios → controladores → vistas → menús → jobs/seguridad).

### 6.3 Determinar los skills de cada tarea

Los skills de una tarea salen de la columna `Skill` de la tabla para sus ficheros, normalizados al nombre real del skill (`k-sistemas`, `k-vistas`, `k-secure-coding`, `k-scheduler`, `k-code-quality`, `k-i18n`, …; ignora las anotaciones entre paréntesis tipo `(modelos.md)`).

**CRITICAL**: añade `k-secure-coding` y `k-code-quality` a **toda** tarea cuyo código Java toque entidades, servicios o controladores, aunque la tabla no lo liste (principio 2.2). Para tareas de solo XML (dominios, vistas, menús) no lo añadas si no aporta.

### 6.4 Escribir cada `task_NN.md`

Por cada tarea, escribe `.sdd/drafts/{iniciativa}/implementation/task_NN.md` con **exactamente** esta plantilla:

```
---
type: implementation-task
---

# Tarea NN a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- <skill A>
- <skill B>

<texto del prompt>
```

Reglas de relleno:

- `NN` es el número de dos dígitos de la tarea (`01`, `02`, …).
- La lista de skills es la determinada en 6.3.
- **`<texto del prompt>`**: todo lo relevante del `design.md` para los ficheros de esta tarea, copiado **verbatim**. **MUST** incluir, cuando apliquen:
  - La(s) fila(s) de la tabla "Ficheros a crear o modificar" de esos ficheros (con su ruta destino).
  - La(s) sección(es) "Paso N" que describen esos ficheros (firmas, comentarios, estructura).
  - Las secciones transversales que apliquen a esos ficheros: "Frontera de confianza / AllowProperties" y las filas de "Trazabilidad V/R/U → ubicación" que les correspondan.
  - Las referencias a `rules/R-*.md` citadas para esos ficheros (cita la ruta; **MUST NOT** copiar su contenido entero si es extenso).
- Para una tarea de **XML ya materializado** (dominio, vista, `menus.xml`), el `<texto del prompt>` **MUST** indicar explícitamente que el fichero está en `design/...` y que se debe **copiar literalmente** (o fusionar, para `menus.xml`) a su ruta destino, **sin regenerarlo** (principio 2.1).

**MUST NOT**:

- **MUST NOT** resumir, reescribir o parafrasear el texto del diseño. Se copia verbatim — el diseño es el contrato (principio 2.2).
- **MUST NOT** inventar pasos, validaciones o ficheros que no estén en el `design.md`.

Ejemplos ✅/❌ de cabecera de tarea:

- ✅ CORRECTO: `# Tarea 03 a implementar` con `type: implementation-task` a ras de margen.
- ❌ INCORRECTO: `# Tarea 3` (sin dos dígitos) o frontmatter `type: design` (tipo equivocado).

---

## 7. Fase 3 — Generar el índice `task.md`

Escribe `.sdd/drafts/{iniciativa}/implementation/task.md` con **exactamente** esta plantilla, una línea por tarea generada:

```
---
type: implementation-tasks
---

# Lista de tareas a implementar
- [Tarea 01](task_01.md)
- [Tarea 02](task_02.md)
```

Reglas:

- Un enlace por cada `task_NN.md` creado, en orden.
- El texto del enlace es `Tarea NN`; el destino es `task_NN.md`.
- ✅ CORRECTO: `- [Tarea 01](task_01.md)`.
- ❌ INCORRECTO: `- [Tarea 1](tarea_01.md)` (número sin dos dígitos y nombre de fichero que no coincide con el real).

---

## 8. Fase 4 — Pedir revisión al usuario

1. Muestra al usuario un resumen: número de tareas generadas y, por cada una, su título y los ficheros que cubre.
2. Indícale la ruta de la carpeta `implementation/` para que revise los `task_NN.md`.
3. **STOP** con `AskUserQuestion`: pregunta si las tareas están conformes para empezar a implementarlas (opciones: empezar / revisar más / abortar).
4. **MUST NOT** pasar a la Fase 5 sin aprobación explícita. Si el usuario edita tareas a mano, vuelve a leer la carpeta `implementation/` antes de implementar.

---

## 9. Fase 5 — Implementar las tareas

1. Lee `implementation/task.md` y, en su orden, cada `task_NN.md`.
2. Para cada tarea, según su naturaleza:
   - **Tarea de XML ya materializado** (dominio, vista): localiza el XML en `design/...`, crea la carpeta destino con `mkdir -p` si no existe y **copia el fichero literalmente** (`cp`) a su ruta destino (principio 2.1). Si el destino ya existe, **STOP** y `AskUserQuestion` (sobrescribir / abortar).
   - **Tarea de `menus.xml`**: lee el `design/menus.xml`, extrae sus `<menuitem>` e **insértalos** en `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` justo antes de `</object-views>`. Si ya existe un `<menuitem name="...">` con el mismo `name`, **STOP** y `AskUserQuestion` (sobrescribir / mantener / abortar). Tras fusionar, **MUST** validar con `xmllint`:
     ```bash
     xmllint --noout --schema ../axelor-open-platform/axelor-core/src/main/resources/object-views.xsd \
       src/main/java/com/educaflow/secretariavirtual/menus/menus.xml
     ```
     Si falla, **STOP** y muestra el **ERROR** sin continuar.
   - **Tarea de Java** (servicios, controladores, repositorios, DTOs, jobs, datos iniciales): **lanza un subagente** con la herramienta `Agent` (`subagent_type: claude`) para implementar la tarea. **MUST NOT** invocar `code-implementer` tú mismo: lo hará el subagente. El prompt del subagente **MUST** instruirle a, en este orden:
     1. **OBLIGATORIAMENTE** leer la sección `## Skills a usar` de la tarea y **cargar cada skill listado con la herramienta `Skill`** **antes** de implementar nada. **MUST NOT** empezar a implementar sin haber cargado esos skills.
     2. Una vez cargados los skills, **invocar `code-implementer`** pasándole el `<texto del prompt>` de la tarea **verbatim** (principio 2.2).

     El prompt del subagente **MUST** incluir además, para que se los traslade a `code-implementer`:
     - El `<texto del prompt>` de la tarea **tal cual** y la lista de skills de `## Skills a usar`.
     - Una **nota explícita** de que los XML de dominios/vistas/menús de los que dependa **ya están copiados** en `src/main/...` y son **contrato fijo**: **NO** debe regenerarlos ni editarlos; las firmas Java deben coincidir con las acciones de las vistas y las entidades con los dominios (principio 2.3). Si detecta un XML mal, **detenerse y notificar**, no editarlo.
     - La instrucción de **STOP** y preguntar ante cualquier bloqueo (principio 2.4). **MUST NOT** adivinar.

     **MUST NOT** usar `run_in_background` en este `Agent`: necesitas el resultado del subagente antes de pasar a la siguiente tarea.
3. Implementa las tareas en orden. **MUST NOT** pasar a la Fase 6 hasta que todas las tareas estén implementadas (o se haya detenido por un bloqueo).

**MUST NOT**:

- **MUST NOT** que `code-implementer` lea otros `design.md` o `analysis.md` de otras iniciativas como referencia. Implementa únicamente la tarea recibida.

---

## 10. Fase 6 — Compilar en bucle hasta que pase

Tras implementar todas las tareas, verifica que el proyecto compila y entra en un bucle de auto-corrección si falla.

**Variables del bucle:**

- **LIMIT**: `max_iter = 3`. **MUST NOT** superar este número de iteraciones bajo ningún concepto.
- `iter = 1`.
- `errores_previos = []` (para detectar fallos persistentes que `code-implementer` no resuelve).

**Iteración:**

1. Compila el proyecto:
   ```bash
   ./gradlew clean build --info
   ```
2. **Si compila sin errores** → sal del bucle con éxito y pasa a la Fase 7.
3. **Si falla la compilación**:
   - **Detectar fallos persistentes**: si los errores de compilación son **idénticos** a los de la iteración anterior, `code-implementer` no está progresando. Trata el caso como `iter == max_iter` y **STOP**.
   - **Si `iter < max_iter`**:
     - Construye un **plan de corrección** pequeño en markdown — un paso por error de compilación, con el mensaje literal del compilador, el fichero/línea y la tarea de origen. **MUST NOT** volver a pasar todas las tareas completas; solo los errores a corregir.
     - **Reinvoca `code-implementer`** con ese plan de corrección y los skills de dominio de las tareas afectadas. Instrúyele **explícitamente** que: (a) **solo corrija código Java**, (b) **NO edite los XML ya copiados** (principio 2.1).
     - Incrementa `iter` y vuelve al paso 1.
   - **Si `iter == max_iter`**:
     - **STOP** y `AskUserQuestion` ofreciendo: (1) dejar el reporte de errores para investigación manual; (2) revisar el diseño relanzando `/sdd-designer-system`; (3) continuar sin compilación limpia (no recomendado).

---

## 11. Fase 7 — Mensaje final al usuario

Tras completar la implementación y la compilación, indica:

```
Implementación completada.

Tareas generadas e implementadas: N (ver .sdd/drafts/{iniciativa}/implementation/).
Compilación: {limpia en M iteraciones | NO limpia tras 3 iteraciones}.

Los artefactos del draft se mantienen en .sdd/drafts/{iniciativa}/ — no se ha archivado nada en .sdd/specs/.
Cuando estés conforme con la implementación, lanza `/sdd-close-spec` para cerrar la iniciativa: actualizará los CLAUDE.md afectados y archivará la spec en .sdd/specs/.
```

Sustituye `{iniciativa}` por el nombre real de la carpeta del draft.

**CRITICAL**: si la Fase 6 acabó sin compilación limpia (bug irresoluble o elección del usuario), **MUST** decirlo explícitamente:

> Atención: el proyecto no compila limpio tras 3 iteraciones. Revisa los errores antes de lanzar `/sdd-close-spec`, o relanza este skill tras corregir el diseño.

**MUST NOT** lanzar `/sdd-close-spec` tú mismo. El usuario decide cuándo ejecutarlo.

---

## Quick Guidelines

- Eres un **delegador**: descompones el diseño en tareas, copias XML del diseñador y delegas el Java en `code-implementer`. **MUST NOT** reescribir XML ni generar Java tú mismo.
- Una tarea por fichero, **agrupando ficheros fuertemente acoplados** de un mismo componente lógico (servicio + impl + DTO). Si dudas, NO agrupes.
- El `<texto del prompt>` de cada tarea se copia **verbatim** del `design.md` (fila de la tabla + "Paso N" + transversales aplicables). **MUST NOT** resumir ni inventar.
- `k-secure-coding` va **siempre** en tareas Java de entidades/servicios/controladores.
- Los XML de `design/` son **contrato fijo**: se copian literalmente. Si están mal, **STOP** y vuelve a `/sdd-designer-system`.
- **MUST NOT** implementar nada hasta que el usuario apruebe la lista de tareas (Fase 4).
- El bucle de compilación tiene **LIMIT**: 3 iteraciones. Tras agotarlo, **STOP** y `AskUserQuestion`.
- `AskUserQuestion` solo para lo imprescindible: ruta auto-detectada, aprobación de tareas, sobrescritura de ficheros/`<menuitem>` y decisión tras agotar el bucle de compilación.

---

## 12. Checklist final del implementer

Antes de emitir el mensaje final de la Fase 7, **MUST** recorrer este checklist. Si alguno falla, vuelve a la fase indicada. **LIMIT**: máximo 3 iteraciones de corrección.

- [ ] ¿El `design.md` tenía `type: design` en el frontmatter? (Fase 1)
- [ ] ¿Se leyó el `design.md` íntegro y se localizó la tabla de ficheros? (Fase 2.1)
- [ ] ¿Cada fichero de la tabla está cubierto por exactamente una tarea (agrupando solo acoplados)? (Fase 2.2)
- [ ] ¿Cada `task_NN.md` tiene `type: implementation-task`, su lista de skills y el texto del diseño verbatim? (Fase 2.4)
- [ ] ¿Las tareas Java de entidades/servicios/controladores incluyen `k-secure-coding`? (Fase 2.3)
- [ ] ¿Existe `implementation/task.md` con `type: implementation-tasks` y un enlace correcto por tarea? (Fase 3)
- [ ] ¿Se pidió aprobación al usuario y se esperó antes de implementar? (Fase 4)
- [ ] ¿Los XML del diseño se copiaron/fusionaron literalmente, sin regenerarlos? (Fase 5, principio 2.1)
- [ ] ¿El Java se implementó lanzando un subagente (`Agent`) por tarea, y ese subagente cargó **obligatoriamente** los skills de `## Skills a usar` antes de invocar `code-implementer`? (Fase 5, principio 2.2)
- [ ] ¿El bucle de compilación no superó `**LIMIT**: 3 iteraciones`? (Fase 6)
- [ ] ¿El mensaje final dice si la compilación quedó limpia o no? (Fase 7)

---

## Apéndice A — Override de rutas (para testing)

Para probar este skill en un sandbox alternativo sin tocar el árbol real:

- `--in=<ruta>` — fichero `design.md` de entrada explícito. **Desactiva la auto-detección** de la Fase 0 caso 2. La "carpeta de la iniciativa" es la que contiene la subcarpeta `design/` de ese fichero.
- `--root=<ruta>` — raíz alternativa a `.sdd/drafts/`. Todas las rutas relativas se resuelven contra esta raíz.

En uso normal no se especifican.
