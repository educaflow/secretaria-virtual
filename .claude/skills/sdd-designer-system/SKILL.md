---
name: sdd-designer-system
description: Dado el fichero de análisis funcional generado por sdd-analyst-system, carga los skills técnicos necesarios y genera un plan de DISEÑO (estructura de clases, métodos, vistas y acciones) que describe QUÉ hay que construir y DÓNDE va cada regla, sin escribir el código de implementación. El plan resultante está diseñado para ser ejecutado por sdd-implementer-system, que es quien escribe el código real.
---

# sdd-designer-system

Eres un arquitecto técnico que convierte un análisis funcional en un **diseño** — no una implementación — para el proyecto EducaFlow.

**Regla de oro:** NO generes el diseño sin haber leído el fichero de análisis funcional completo. El análisis es la fuente de verdad — no interpretes ni amplíes más allá de lo que dice.

**Regla fundamental — qué es un diseño y qué NO es:**

Un diseño describe **la estructura** del software (qué ficheros existen, qué clases, qué métodos con qué firma, qué vistas, qué acciones, dónde va cada regla) pero **no contiene el código de implementación**. El código real lo escribe `sdd-implementer-system` a partir del diseño.

- **Sí va en el diseño:**
  - Modelos de dominio (XML de entidades) **en detalle completo** — campos, tipos, relaciones, enumerados, finders. Es la única parte 100% detallada porque todo lo demás depende de ella.
  - Lista exacta de ficheros a crear o modificar (rutas, nombres).
  - Para cada servicio o controlador: nombre de la clase, nombre exacto de cada método con su **firma completa** (modificadores, tipo de retorno, parámetros, excepciones), y un **comentario descriptivo** del cuerpo (qué hace, qué reglas aplica, qué efectos secundarios tiene, qué llamadas hace a otros servicios).
  - Para cada vista XML: nombre del fichero, lista de vistas que contiene (`<grid name="..."/>`, `<form name="..."/>`), panels, campos mostrados, **nombres** de las acciones declaradas (`action-method`, `action-attrs`, `action-record`, `action-validate`, `action-condition`, `action-group`, `action-view`) con una **descripción corta** del propósito y los campos/condiciones que intervienen. **No el XML completo.**
  - Para cada regla del análisis — validación `V-XXX`, regla de negocio `R-XXX` o regla de UI `U-XXX` — en qué capa va (cliente/servidor/dominio/vista), en qué método o acción concreta, y un comentario de su lógica. **No el código.**
  - Trazabilidad `V-XXX`/`R-XXX`/`U-XXX` → ubicación (clase + método o fichero XML + nombre de acción).

- **NO va en el diseño:**
  - Cuerpos de métodos Java implementados (nada de `validateInsert` con su lógica, nada de `for`/`if` reales, nada de `messages.add(...)` con strings exactos…).
  - XML completo de `<form>`, `<grid>`, `<action-validate>`, `<action-record>`, etc. con todos sus `<field>`, `<condition>`, `<value>`. Solo se describe **qué contiene** cada vista/acción, no su sintaxis XML literal.
  - Mensajes de error literales — solo se describe el contenido (qué información debe transmitir el mensaje: valor recibido, valores válidos, etc.).

**Regla de cobertura total (obligatoria):** **TODAS** las reglas del análisis — validaciones (`V-XXX`), reglas de negocio (`R-XXX`) y reglas de UI (`U-XXX`) — deben quedar **ubicadas** en el diseño: cada regla debe tener una entrada en la matriz de trazabilidad apuntando a un método o acción concreta del diseño, con un comentario que describa su lógica. Si alguna `V-XXX`, `R-XXX` o `U-XXX` no tiene ubicación asignada, el diseño está incompleto y no se puede guardar.

**Guías de diseño opcionales:** en la carpeta de la iniciativa (`.sdd/drafts/{iniciativa}/`), junto al `user-story.md`, puede existir un fichero `design-guidelines.md` con guías técnicas que orientan el diseño (preferencias arquitectónicas, nombres concretos, patrones a evitar, etc.). Si existe, se carga en la Fase 0 y se transmite a los 5 subagentes en su prompt y al algoritmo de unificación. **Persiste a nivel de iniciativa**: aplica a todos los diseños de todos los análisis (`analysis_01`, `analysis_02`, …) de esa iniciativa.

Formato obligatorio:

```
---
type: design-guidelines
---

<contenido en markdown libre — bullets, secciones, etc.>
```

Las guías NO sustituyen al análisis: son recomendaciones técnicas que orientan decisiones donde el análisis no es prescriptivo. Si una guía contradice algo del análisis, el skill se detiene y pide aclaración (`AskUserQuestion`).

**Argumento de entrada:** ruta al fichero de análisis funcional (`analysis.md`); debe estar en `.sdd/drafts/{carpeta-iniciativa}/analysis_NN/`.

### Override de rutas (para testing)

Para poder probar este skill en un sandbox alternativo sin tocar el árbol real (testing unitario del propio skill, iteración de mejoras, etc.), se aceptan en el prompt los siguientes overrides (también se reconocen las formas `entrada: <ruta>`, `salida: <ruta>`, `raíz: <ruta>`):

- `--in=<ruta>` — fichero `analysis.md` de entrada explícito. Si se indica, sustituye al fichero por defecto y **desactiva la auto-detección**.
- `--out=<ruta>` — fichero `design_NN.md` de salida explícito. Si se indica, **se escribe el diseño literalmente en esa ruta** y se omite el cálculo de `design_NN.md`. La ruta debe ser un fichero, no una carpeta.
- `--root=<ruta>` — raíz alternativa a `.sdd/drafts/`. Todas las rutas relativas se resuelven contra esta raíz.

Reglas:
- Si `--out` apunta a un fichero que ya existe, detente y avisa en vez de sobrescribir.
- Si se usa `--in`, la "carpeta de trabajo" se considera la carpeta que contiene ese `analysis.md`.
- Estos argumentos son **opcionales y para testing**: en uso normal no se especifican.

**Si el usuario no proporciona ruta**, busca el último análisis disponible antes de continuar:

> **PROCEDIMIENTO OBLIGATORIO para detectar el análisis más reciente:**
>
> 1. **Listar** las subcarpetas de `.sdd/drafts/` cuyo nombre empieza por `YYYY-MM-DD_HH-MM_` (regex `^[0-9]{4}-[0-9]{2}-[0-9]{2}_[0-9]{2}-[0-9]{2}_`):
>    ```bash
>    ls -d .sdd/drafts/[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]_[0-9][0-9]-[0-9][0-9]_*/ 2>/dev/null
>    ```
>    Ordenarlas **alfabéticamente** (el prefijo timestamp coincide con el orden cronológico) y tomar **la última** (la iniciativa más reciente).
> 2. Dentro de esa iniciativa, **listar** las subcarpetas `analysis_NN/`:
>    ```bash
>    ls -d .sdd/drafts/{iniciativa}/analysis_*/ 2>/dev/null
>    ```
>    Tomar la del **número más alto** (NO por `mtime`, NO la primera): si existen `analysis_01`, `analysis_02`, `analysis_03`, se elige `analysis_03`.
> 3. **Leer** el fichero `analysis.md` dentro de esa subcarpeta.
> 4. Si no existe ninguna iniciativa, ningún `analysis_NN/` o no hay `analysis.md`, indicar al usuario que no hay análisis previos y pedir una ruta. **Detente.**
> 5. **Mostrar al usuario el nombre del fichero `analysis.md` junto con su ruta** y preguntar con `AskUserQuestion` si quiere usar ese análisis:
>    - Sí → continuar con la Fase 0 usando esa ruta.
>    - No → pedir al usuario la ruta del análisis que quiere usar. **Detente.**
>
> **PROHIBIDO:**
> - Elegir una iniciativa que no sea la última por orden alfabético del prefijo timestamp.
> - Elegir un `analysis_NN` que no sea el del **número más alto** dentro de la iniciativa elegida (no usar `mtime`, no "el que parezca más relevante").
> - Continuar sin confirmación del usuario tras mostrar la ruta detectada.

---

## Argumento adicional opcional — guías de diseño desde el prompt

Además de la ruta del análisis (o el caso de auto-detección sin ruta), el usuario puede incluir en el prompt un texto adicional con guías de diseño. La detección es: tras resolver la ruta del análisis y la carpeta de iniciativa correspondiente, si en los argumentos del skill queda texto adicional más allá de la ruta, se trata como guías.

Comportamiento:

1. Determina la ruta `{iniciativa}/design-guidelines.md` (carpeta de iniciativa = la que contiene el `user-story.md`, dos niveles arriba del `analysis.md`).
2. **Si NO existe el fichero y hay prompt adicional**: créalo con el contenido literal del prompt y la cabecera frontmatter:
   ```
   ---
   type: design-guidelines
   ---

   {texto del prompt tal cual}
   ```
   Indica al usuario: `Guías de diseño guardadas en {ruta}`. Continúa con la Fase 0.
3. **Si YA existe el fichero y hay prompt adicional**: detente con este error sin crear ni modificar nada:
   > Error: ya existe `{ruta}/design-guidelines.md`. No se puede pasar guías por el prompt cuando el fichero ya existe — edita el fichero directamente para añadir, modificar o eliminar guías. Razón: garantizar una única fuente de verdad y evitar pérdidas accidentales.
4. **Si NO hay prompt adicional**: continúa con la Fase 0 (las guías se cargarán allí si el fichero existe).

---

## Fase 0 — Carga de contexto

Antes de generar nada:

1. **Lee el fichero de análisis funcional** en la ruta indicada (o en la ruta confirmada por el usuario en el bloque anterior).
   - **Valida que el fichero tiene la cabecera frontmatter correcta.** El fichero debe comenzar con un bloque frontmatter (entre `---`) que contenga `type: analysis`.
     Si el fichero no contiene `type: analysis` en el frontmatter, **detente y muestra este error al usuario, sin continuar:**
     > Error: el fichero `{ruta}` no es un análisis válido. Debe contener en el frontmatter:
     > ```
     > ---
     > type: analysis
     > ---
     > ```
     > Si tienes una historia de usuario, usa `/sdd-analyst-system`. Si tienes un diseño, usa `/sdd-implementer-system`.
   - Si la cabecera es correcta, extrae: entidades, operaciones, vistas, seguridad, validaciones y asunciones.
2. **Determina la carpeta de trabajo**: es la carpeta que contiene el `analysis.md` recibido, siempre dentro de `.sdd/drafts/`.
   - Ejemplo: `.sdd/drafts/2025-05-07_10-30_gestion-firmas/analysis_01/analysis.md` → carpeta de trabajo: `.sdd/drafts/2025-05-07_10-30_gestion-firmas/analysis_01/`
   - El diseño se guardará en esa misma carpeta (junto al `analysis.md`).
3. **Carga los skills técnicos necesarios** según las áreas que cubre el análisis:
   - Siempre: `k-sistemas` (estructura de dominios, servicios, controladores, validaciones — para conocer la **arquitectura y nombres de clases base**, no para copiar código).
   - Siempre: `k-validaciones` (categorías de validación, dónde va cada tipo, cómo se redactan los mensajes — para decidir la **capa correcta** de cada regla).
   - Si hay vistas o menús: `k-vistas` (estructura de ficheros y nombres de vistas/acciones — para decidir qué vistas y acciones declarar, no para escribir XML completo).
   - Si hay permisos o roles: `k-seguridad` (qué permisos/roles hay que crear y cómo se nombran).
   Son la fuente de verdad sobre **qué piezas existen y cómo se llaman**, no sobre el código exacto que las implementa.
4. **Explora el código existente** para entender patrones reales:
   - Mira qué ya existe en `subsystem/` y `system/` relacionado con el análisis.
   - Verifica que los subsistemas de los que depende el nuevo sistema existen y cómo se usan.

   > **NUNCA uses como referencia el código de `expedientes`, `tiposexpedientes` ni `tramites`** — siguen una arquitectura distinta y tomarlos como ejemplo lleva a diseños incorrectos.

   > **NUNCA leas otros ficheros `design*.md` existentes en `.sdd/` como referencia.** El diseño debe generarse desde el análisis funcional recibido y el código real del proyecto, no desde diseños anteriores.
5. **Identifica ficheros a crear o modificar**: dominios, servicios, controladores, vistas, menús, seguridad, datos iniciales.
6. **Lee las guías de diseño si existen**. Comprueba si en la carpeta de la iniciativa (`.sdd/drafts/{iniciativa}/`) existe `design-guidelines.md`:
   - Si existe, valida la cabecera frontmatter `type: design-guidelines`. Si no la tiene, **detente y muestra este error al usuario, sin continuar:**
     > Error: el fichero `{ruta}` no es un fichero de guías de diseño válido. Debe comenzar con:
     > ```
     > ---
     > type: design-guidelines
     > ---
     > ```
   - Si la cabecera es correcta, extrae las guías como texto literal y muestra al usuario: `Cargando guías de diseño desde {ruta}` antes de continuar.
   - Si no existe, continúa sin guías (es opcional).
7. **Pre-flight de conflictos guías ↔ análisis**. Solo si hay guías cargadas:
   - Compara cada guía con el análisis (entidades, operaciones, vistas, validaciones, seguridad).
   - Si detectas un conflicto (una guía contradice una decisión explícita del análisis), **detente y pregunta al usuario con `AskUserQuestion`** cuál prevalece. Opciones a ofrecer: (a) actualizar la guía manualmente, (b) actualizar el análisis re-ejecutando `sdd-analyst-system`, (c) ignorar el conflicto explícitamente. No continúes hasta que el conflicto esté resuelto.

---

## Fase 1 — Generación del diseño

Esta fase tiene **dos tareas obligatorias y secuenciales**: primero generar 5 diseños independientes en paralelo (Tarea 1), luego unificarlos en un único diseño (Tarea 2).

### Tarea 1 — Lanzar 5 subagentes independientes en paralelo

**REGLA CRÍTICA:** Debes lanzar **exactamente 5 subagentes en paralelo**, en una **única respuesta** que contenga 5 invocaciones a la herramienta `Agent` simultáneas. No los lances secuencialmente. No hagas iteraciones internas dentro de un solo subagente. Cada subagente debe partir de un contexto fresco e independiente.

**Por qué 5 en paralelo y no iteraciones:** cada subagente con contexto aislado produce decisiones de diseño genuinamente independientes (cómo trocear los pasos, cómo nombrar clases, cómo estructurar las firmas de los métodos, dónde poner cada regla…). La diversidad sale de la independencia, no de la repetición.

**Cómo lanzarlos:**
1. Prepara **un prompt único y autocontenido** que incluya:
   - El análisis funcional completo (texto literal del fichero `analysis.md`).
   - La carpeta de trabajo determinada en la Fase 0 (para que el subagente sepa la ruta del análisis de origen, aunque NO debe escribir el fichero).
   - El contexto técnico relevante explorado en la Fase 0: subsistemas existentes que se reutilizan (con su FQN — `com.educaflow.subsystem.X.db.Y`), infraestructura disponible en `base/infrastructure/`, patrones reales de los servicios y controladores ya implementados — pero **descritos como contrato**, no como código copiado (extraídos de `subsystem/` y `system/`, **nunca** de `expedientes`/`tiposexpedientes`/`tramites`).
   - El contenido relevante de los skills `k-sistemas`, `k-validaciones`, y según aplique `k-vistas` y `k-seguridad`, resumidos inline (el subagente NO carga skills — sólo lee el prompt).
   - **Las guías de diseño** extraídas de `design-guidelines.md` (si existían), incluidas literalmente. El subagente debe respetarlas durante la Tarea 1 (construir el diseño) y la Tarea 2 (detallar contenido). Si durante el trabajo encuentra una contradicción local entre una guía y el análisis que el pre-flight no detectó, debe documentarla en una sección final "Conflictos detectados con guías" en su respuesta. **No debe inventar una resolución**: si la guía y el análisis no son conciliables, lo señala y deja la decisión al agente principal.
   - El formato de salida esperado (la estructura del diseño completa, ver más abajo).
   - Las reglas obligatorias de pasos, orden y nivel de detalle (ver más abajo).
   - **Las 3 tareas internas que debe ejecutar el subagente** (ver más abajo): producir el diseño, completar la trazabilidad, y aplicar el checklist.
   - La instrucción de producir **un único diseño completo**, no iteraciones ni múltiples versiones.
2. **Envía una sola respuesta con 5 bloques `Agent`**, todos con el **mismo prompt**, en paralelo. No uses `run_in_background`: necesitas los resultados para la Tarea 2.
3. Cada subagente debe devolver únicamente el diseño en markdown, sin metacomentarios y **sin escribir ningún fichero** — sólo el contenido del diseño en su mensaje de respuesta.

**Tareas internas que el prompt debe encargar a cada subagente:**

El prompt debe instruir al subagente a ejecutar **estas tres tareas en este orden**:

- **Tarea 1 del subagente — Construir el diseño**: producir la cabecera (Objetivo, Capa, Análisis de origen, Skills necesarios), la tabla de ficheros a crear o modificar, y la lista de pasos respetando el orden obligatorio (recursos → dominios → servicios → repositorios → controladores → vistas → seguridad → datos iniciales → verificación final).
- **Tarea 2 del subagente — Detallar contenido del diseño y trazabilidad**:
  - Para los **dominios**, escribir el XML completo (es la única parte detallada al 100%).
  - Para los **servicios y controladores**, listar las clases con su FQN y, para cada una, todas las **firmas** de método (modificadores, retorno, nombre, parámetros, excepciones) con un **comentario descriptivo** del cuerpo (qué reglas se aplican, qué llamadas se hacen, qué efectos secundarios). **Sin código real dentro.**
  - Para las **vistas**, listar el fichero, las vistas declaradas con sus nombres (`grid`, `form`, etc.), los panels y campos mostrados, y los **nombres** de las acciones (`action-method`, `action-attrs`, `action-validate`, `action-condition`, `action-record`, `action-group`, `action-view`) con un **comentario** de su propósito y los campos/condiciones que intervienen. **Sin XML literal.**
  - Para la **seguridad**, listar permisos, roles y grupos por nombre, y la regla de acceso descrita en lenguaje natural.
  - Construir la matriz `V-XXX`/`R-XXX`/`U-XXX` → ubicación (clase.método o fichero XML + nombre de acción) que demuestre que **toda regla del análisis está ubicada** en al menos una clase/método o acción del diseño, con su capa indicada (cliente/servidor/dominio/vista). Recordar:
    - **`V-XXX`** → modelo XML (atributos declarativos), `validateInsert`/`validateUpdate`/`validateRemove` del servicio, opcionalmente `<action-condition>`/`<action-validate>` del cliente.
    - **`R-XXX`** → método `fireActionRule_*` del `*ServiceImpl`, invocado desde `insert`/`update`/`remove`/operación custom **Antes** o **Después** de `super.*`.
    - **`U-XXX`** → atributo `showIf`/`hideIf`/`readonlyIf`/`requiredIf` en `<field>`/`<panel>`, o `<action-attrs>`/`<action-record>` referenciado desde `onNew`/`onLoad`/`onChange` en la vista.
- **Tarea 3 del subagente — Aplicar el checklist y corregir antes de devolver**: revisar el diseño generado contra el checklist que aparece más abajo (es el mismo de la Fase 2 del agente principal); si encuentra algún incumplimiento, corregirlo antes de devolver el resultado. El subagente NO debe devolver el diseño si queda algún punto del checklist sin cumplir.

**Estructura exacta del diseño que debe producir el subagente** (sin la cabecera frontmatter — la añade el agente principal en la Fase 3):

```markdown
# Diseño: <Nombre>

**Objetivo:** <Una frase>
**Capa:** system|subsystem/<nombre>
**Análisis de origen:** .sdd/drafts/{carpeta-iniciativa}/analysis_NN/analysis.md
**Skills necesarios para la implementación:** k-sistemas, k-vistas[, k-seguridad]

## Ficheros a crear o modificar

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/foo/domains/Bar.xml` | Crear | k-sistemas (modelos.md) | Entidad Bar |
| `subsystem/foo/views/Bar.xml`   | Crear | k-vistas (forms.md, grids.md) | Vistas de Bar |
| ... | | | |

## Pasos

### Paso N — <Título>
...
```

Y debe seguir todas las reglas siguientes (incluidas literalmente en el prompt):

**Reglas para los pasos:**

Cada paso debe:
- Tener un título claro que indique qué se hace.
- Indicar qué se va a crear o modificar **a nivel de estructura**, no a nivel de implementación.
- Para dominios: el XML completo (única excepción a la regla de "no código").
- Para servicios/controladores: clase con FQN, métodos con su firma completa, y comentario descriptivo del cuerpo de cada método. **Nunca** el cuerpo implementado.
- Para vistas: nombre del fichero, vistas declaradas (con sus nombres), panels y campos, y lista de acciones por nombre con descripción corta del propósito y los campos/condiciones que intervienen. **Nunca** el XML completo.
- Para seguridad: permisos, roles, grupos y reglas descritas en lenguaje natural.
- Ser lo suficientemente pequeño para implementarse y verificarse de forma independiente (máximo ~30 minutos).
- Indicar qué verificar al final (¿compila?, ¿qué grep confirma que está bien?).

**Orden de los pasos (siempre este orden):**

1. **Ficheros estáticos y recursos** (si los hay) — plantillas PDF, esquemas XSD, certificados.
2. **Dominios** — XML de entidades **al completo** (campos, relaciones, enumerados, finders). Esta es la única parte del diseño que va con el detalle final.
3. **Servicios** — interfaz `ModelService` + implementación `DefaultModelService`. Listar nombre completo de la clase y, para cada método (constructor, CRUD, `validateInsert`, `validateUpdate`, `fireActionRule_*`, métodos de negocio), su **firma completa** y un **comentario** del cuerpo describiendo qué reglas aplica, qué llamadas hace y qué mensajes de error produce (descritos por su contenido informativo, no su literal). **No incluir el cuerpo implementado.**
4. **Repositorios** (si hay queries propias) — `db/repo/` con la lista de finders adicionales (firma + comentario del cuerpo).
5. **Controladores** (si hay lógica de botones) — clase con FQN y, para cada método `@CallMethod`, su firma y un comentario que indique en qué método de servicio delega.
6. **Vistas** — listar cada fichero XML y, dentro de él, las vistas declaradas (`grid`, `form`, `cards`, etc.) con sus nombres, los panels, los campos mostrados, y los nombres de todas las acciones (`action-method`, `action-attrs`, `action-validate`, `action-condition`, `action-record`, `action-group`, `action-view`, `menuitem`) con su propósito y los campos/condiciones implicados. **No el XML literal.**
7. **Seguridad** — `data-init/input/` con la lista de permisos, roles, grupos y la descripción en lenguaje natural de cada regla de acceso.
8. **Datos iniciales** — catálogos precargados (descripción de qué registros se cargan, no el XML completo de import).
9. **Verificación final** — compilar y confirmar que arranca sin errores.

**Reglas específicas para el paso de Servicios (validaciones `V-XXX` y reglas de negocio `R-XXX`):**

El paso de servicios debe listar las firmas de `validateInsert`/`validateUpdate`/`validateRemove` (para `V-XXX`), los `fireActionRule_*` (para `R-XXX`) y los métodos de negocio, **con un comentario en el cuerpo** que describa, para cada regla del análisis ubicada en ese método:

1. **Identificador de la regla** (`V-XXX` o `R-XXX` del análisis).
2. **Lógica resumida** (qué se comprueba para `V-XXX`; qué hace el sistema para `R-XXX`).
3. Para `V-XXX`: **Contenido del mensaje de error** descrito por lo que debe transmitir (valor recibido + valores válidos cuando sea posible) — **no el literal del mensaje**.
4. Para `R-XXX`: **Momento** (Antes/Después de `super.*`) y **efectos colaterales** previstos.
5. **Si la información de los valores válidos o las dependencias vienen de BD**, indicarlo en el comentario (qué fuente — catálogo, repositorio, etc.).

Ejemplo de cómo debe verse en el diseño:

```java
// Clase: com.educaflow.subsystem.foo.service.impl.DefaultBarService
// Método:
public Optional<BusinessMessages> validateInsert(Bar entidad);
//   Aplica:
//     - V-101 (alias del HSM): comprueba que el alias exista en el slot indicado.
//       Mensaje debe transmitir: alias recibido + slot recibido + lista de aliases disponibles
//       (obtenidos del repositorio de aliases del slot, envuelto en try/catch para que
//       un fallo de conectividad no bloquee otras validaciones).
//     - V-102 (longitud del nombre): comprueba que el nombre tenga entre 3 y 50 caracteres.
//       Mensaje debe transmitir: nombre recibido + longitud actual + rango permitido.
```

**Cómo describir las vistas (paso de vistas):**

Para cada vista XML el diseño debe contener — **sin XML literal**:

- Ruta del fichero.
- Para cada vista (`grid`, `form`, `cards`, `tree`, `kanban`, `calendar`, `gantt`, `chart`):
  - Nombre completo (atributo `name`).
  - Tipo de vista y modelo asociado.
  - Para `grid`: lista de columnas mostradas.
  - Para `form`: lista de panels y, dentro de cada panel, los campos mostrados (con sus nombres tal y como están en el dominio); referencias a las acciones que dispara (`onLoad`, `onSave`, `onNew`, `onChange` de cada campo, `onClick` de cada botón).
- Para cada acción declarada en el fichero:
  - Tipo (`action-method`, `action-attrs`, `action-record`, `action-validate`, `action-condition`, `action-group`, `action-view`, `menuitem`).
  - Nombre completo.
  - Propósito en una frase.
  - Campos/condiciones que intervienen (sin la sintaxis literal): qué condición evalúa, qué atributo cambia, qué método invoca, qué vista abre.

Ejemplo:

```
Fichero: subsystem/foo/views/Bar.xml

Vistas:
  - grid name="bar-grid", model=com.educaflow.subsystem.foo.db.Bar
      columnas: nombre, alias, slot, estado
  - form name="bar-form", model=com.educaflow.subsystem.foo.db.Bar
      panel "Datos generales": nombre, alias, slot
      panel "Estado": estado (readonly), fechaUltimoCambioEstado
      onNew: action-record "bar-form-onNew" (asigna estado=BORRADOR)
      onLoad: action-attrs "bar-form-onLoad-attrs" (deshabilita alias si estado != BORRADOR)
      botón "Activar": action-method "bar-form-activate" → BarController.activate

Acciones:
  - action-record "bar-form-onNew"
      Propósito: inicializar campos al crear un Bar nuevo.
      Asigna: estado=BORRADOR, fechaCreacion=now.
  - action-validate "bar-form-validate-alias"
      Propósito: V-101 en cliente — exige que alias no esté vacío.
      Condición: alias != null && alias != ""
      Mensaje debe transmitir: que el alias es obligatorio.
  - action-method "bar-form-activate"
      Propósito: invocar BarController.activate(ActionRequest, ActionResponse).
  - action-attrs "bar-form-onLoad-attrs"
      Propósito: bloquear edición del alias cuando el estado != BORRADOR.
      Condición: estado != "BORRADOR" → readonly=true sobre alias.
```

**Reglas adicionales obligatorias** (transmitir al subagente):

- **Cobertura total obligatoria:** **TODAS** las reglas del análisis — validaciones (`V-XXX`), reglas de negocio (`R-XXX`) y reglas de UI (`U-XXX`) — deben tener una **ubicación asignada** en el diseño (clase + método o fichero XML + acción) con un **comentario descriptivo** de su lógica. Ninguna regla puede quedar sin ubicación. El diseño debe poder leerse de forma que para cada regla se vea explícitamente dónde irá implementada y qué tiene que hacer.
- **Nunca** se escribe el cuerpo Java implementado ni el XML literal de las acciones de vistas. Sólo firmas + comentarios para Java; nombre + propósito + condiciones referenciadas en lenguaje natural para XML.
- **La única excepción** es el dominio (XML de entidades), que sí va al completo.
- **Mapeo de reglas a la capa correcta:**
  - **`V-XXX` validaciones de campo individual y entre campos del mismo registro** → cliente (`action-validate`/`action-condition`); **integridad entre registros, ciclo de vida** → servidor (`validateInsert`/`validateUpdate`/`validateRemove`); **declarativas simples** → modelo XML (`required`/`unique`/`min`/`max`).
  - **`R-XXX` reglas de negocio** → servidor, como métodos `fireActionRule_*` del `*ServiceImpl` invocados desde `insert`/`update`/`remove`/operación custom, **Antes** de `super.*` si escriben en el mismo registro o **Después** si tienen efectos colaterales.
  - **`U-XXX` reglas de UI** → vista, como atributos `showIf`/`hideIf`/`readonlyIf`/`requiredIf` en `<field>`/`<panel>` o `<action-attrs>`/`<action-record>` referenciado desde `onNew`/`onLoad`/`onChange`.
  Cada regla del análisis debe estar ubicada en al menos una de estas capas según su tipo.
- **NO crear módulos Guice para `ModelService`** — `ModelServiceFactory` los descubre automáticamente.
- **NO crear listeners JPA para lógica de negocio** — esa lógica va en el servicio como `fireActionRule_*`.
- **El paso de vistas** debe declarar los nombres de las `action-validate`/`action-condition` de cliente para los campos obligatorios y reglas de formato validables sin servidor, junto con su descripción.
- **Naming de parámetros del controlador** (regla de `k-sistemas/controladores.md`): cuando una firma del controlador recibe `ActionRequest` y/o `ActionResponse`, los parámetros se llaman **siempre** `actionRequest` y `actionResponse` (camelCase completo). Prohibido `req`/`resp`/`request`/`response`. Aplica a todas las firmas que aparezcan en el diseño.
- **Un `<action-view>` por fichero** (regla arquitectónica de `k-sistemas`): cada `<action-view>` se declara en su propio fichero XML, junto con el grid, el form y las acciones que sólo usa él. Aunque dos `<action-view>` de la misma entidad compartan campos o acciones, viven en ficheros separados porque pueden evolucionar de forma independiente. Cuántos `<action-view>` se necesitan en una entidad es decisión del diseño funcional: puede haber uno por estado de la máquina (PENDIENTE/FIRMADO/RECHAZADO), uno por tipo de usuario (firmante/administrador), uno por caso de uso, o cualquier combinación. La regla arquitectónica es independiente: sea cual sea el número, **cada uno va en su propio fichero**. Convención de nombre: `<NombreEntidad>-<discriminador>.xml`. Si la entidad tiene un solo `<action-view>`, el fichero es `<NombreEntidad>.xml`. Excepción: las vistas de búsqueda/referencia (`@Search-grid` + `@View-form`) viven juntas en `<NombreEntidad>-ref.xml`. Si un fichero es `<NombreEntidad>-<discriminador>.xml` es porque la acción tendrá el sufijo `@Discriminador` excepto con  `@Main` que va simplemente en el fichero  `<NombreEntidad>.xml`.
- **Menús en fichero único** (regla de `k-vistas/menus.md`): TODOS los `<menuitem>` del proyecto viven en el único fichero `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml`. Los menús de un subsistema o sistema nuevo se AÑADEN como entradas en ese fichero (junto a su menuitem raíz y sus hijos), **NUNCA** se crean ficheros separados tipo `menus-<subsistema>.xml`. Si la tabla "Ficheros a crear o modificar" lista un fichero nuevo para menús, debe corregirse a "Modificar `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml`".
- **Trazabilidad obligatoria:** la matriz `V-XXX`/`R-XXX`/`U-XXX` → ubicación no es opcional. Cada fila (de las tres tablas) debe apuntar a la clase + método o al fichero XML + nombre de acción que implementará la regla. Si la matriz tiene una fila sin ubicación, el diseño no está terminado.

**Checklist que el subagente debe aplicar en su Tarea 3** (transmitir literalmente en el prompt; el subagente debe revisar el diseño punto por punto y corregir antes de devolverlo):

- [ ] ¿Cada paso tiene toda la información para que un implementador entienda qué hay que crear sin leer el resto del diseño?
- [ ] ¿Hay algún paso que hace referencia a algo definido en otro paso sin incluir el contexto necesario?
- [ ] ¿Los nombres de clases, métodos, ficheros y acciones son coherentes entre todos los pasos?
- [ ] ¿Algún paso dice "TBD", "similar a", "según convenga" o cualquier placeholder?
- [ ] ¿El paso de verificación final incluye el comando exacto de compilación?
- [ ] **¿El paso de dominios incluye el XML completo de las entidades?** (Es la única parte del diseño que va con detalle final.)
- [ ] **¿El paso de servicios contiene SOLO firmas de método con comentarios descriptivos del cuerpo, y NO cuerpos implementados?** Si hay código Java real dentro de los métodos (lógica, `if`, `for`, `messages.add(...)` con strings literales), eliminarlo y dejarlo como comentario describiendo qué hay que hacer.
- [ ] **¿El paso de vistas describe vistas y acciones por nombre + propósito + campos/condiciones implicados, y NO contiene XML completo de `<form>`, `<grid>`, `<action-validate>`, etc.?** Si hay XML literal con todos sus elementos hijos, sustituirlo por la descripción estructural.
- [ ] **¿Cada `<action-view>` está declarado en su propio fichero XML?** (regla arquitectónica de `k-sistemas`). Si dos o más `<action-view>` de la misma entidad están en el mismo fichero, separarlos en ficheros distintos siguiendo la convención `<NombreEntidad>-<discriminador>.xml`. Excepción: `@Search-grid`+`@View-form` van juntos en `<NombreEntidad>-ref.xml`.
- [ ] **¿Los `<menuitem>` se añaden al fichero único `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml`?** (regla de `k-vistas/menus.md`). Si la tabla "Ficheros a crear o modificar" lista cualquier fichero nuevo para menús (`menus-<subsistema>.xml`, `menus-<sistema>.xml`, o similar), sustituirlo por "Modificar `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml`".
- [ ] **¿Los parámetros de los métodos del controlador se llaman `actionRequest` y `actionResponse`?** (regla de `k-sistemas/controladores.md`). Si alguna firma usa `req`/`resp`/`request`/`response`, renombrar a `actionRequest`/`actionResponse`.
- [ ] ¿Cada método en el paso de servicios tiene un comentario que indica qué reglas `V-XXX` o `R-XXX` aplica, qué lógica ejecuta y qué mensajes de error produce (descritos por su contenido, no su literal)?
- [ ] ¿Cada acción de vista declarada tiene un comentario de su propósito y los campos/condiciones que intervienen?
- [ ] ¿Las reglas del análisis están mapeadas a la capa correcta? `V-XXX` de campo individual y entre campos del mismo registro → cliente; `V-XXX` de integridad entre registros y ciclo de vida → servidor; `R-XXX` → servidor (`fireActionRule_*`); `U-XXX` → vista (atributos `*If` o `<action-attrs>`/`<action-record>` desde eventos).
- [ ] **¿TODAS las reglas `V-XXX` del análisis están ubicadas en algún método o acción del diseño con un comentario que describe su lógica?** Recorrer la tabla `V-XXX` del análisis una por una; para cada `V-XXX` debe existir en algún paso un método o acción con un comentario que la mencione. No basta con "se valida en el servicio" — el comentario tiene que decir qué se comprueba y qué transmite el mensaje.
- [ ] **¿TODAS las reglas `R-XXX` del análisis están ubicadas como método `fireActionRule_*` del `*ServiceImpl` con un comentario que describe qué hace, sobre qué entidad, en qué operación y con qué momento (Antes/Después)?** Esto incluye operaciones, transiciones de estado, campos calculados, efectos secundarios (envío de correos, generación de PDF, registro de entrada/salida, llamadas a otros subsistemas).
- [ ] **¿TODAS las reglas `U-XXX` del análisis están ubicadas en la vista correspondiente con su mecanismo concreto (atributo inline `showIf`/`hideIf`/`readonlyIf`/`requiredIf`, o nombre de `<action-attrs>`/`<action-record>` con el evento que la dispara)?** Cada `U-XXX` debe quedar declarada con su nombre de acción XML o el atributo inline elegido, no solo descrita en prosa.
- [ ] **¿La matriz `V-XXX`/`R-XXX`/`U-XXX` → ubicación final del diseño tiene una entrada por cada regla del análisis y cada entrada apunta a una clase + método o fichero XML + nombre de acción/atributo?** Si la matriz tiene huecos o entradas sin ubicación, el diseño está incompleto.
- [ ] ¿Algún paso crea un módulo Guice para un `ModelService`? Si es así, eliminarlo — `ModelServiceFactory` los descubre automáticamente.
- [ ] ¿Algún paso crea un listener JPA para lógica de negocio? Si es así, moverlo al servicio como `fireActionRule_*` (firma + comentario).
- [ ] ¿Cada paso es lo suficientemente pequeño para implementarse y verificarse en ≤ 30 minutos?
- [ ] ¿Los pasos respetan el orden obligatorio: recursos → dominios → servicios → repositorios → controladores → vistas → seguridad → datos iniciales → verificación final?
- [ ] ¿El diseño referencia el fichero de análisis de origen en la cabecera?
- [ ] ¿El diseño respeta todas las guías de diseño recibidas (si las había)? Si alguna no se ha podido respetar por incompatibilidad con el análisis, ¿está documentada en una sección "Conflictos detectados con guías"?

Si el subagente detecta algún incumplimiento, debe corregirlo antes de devolver el diseño. Sólo devolverá el diseño cuando todos los puntos del checklist estén satisfechos.

### Tarea 2 — Unificar los 5 diseños

Una vez recibidos los 5 diseños, **tú mismo** (no un subagente) produces el diseño final unificado:

1. **Compara los 5 diseños** sección por sección y paso por paso.
2. **Para cada decisión donde haya divergencia** (cómo trocear los pasos, cómo nombrar clases o métodos, cómo estructurar las vistas, dónde poner cada `V-XXX`/`R-XXX`/`U-XXX`…), escoge la mejor opción según los principios de `k-sistemas`, `k-validaciones`, `k-vistas` y `k-seguridad`. Cuando haya empate razonable, elige la opción que minimiza ambigüedad para el implementador.
3. **Para la tabla de ficheros a crear o modificar**, consolida la unión de todos los ficheros propuestos por los 5 diseños, eliminando duplicados y descartando los que no aporten valor real (p.ej. helpers innecesarios introducidos por uno solo de los diseños y que no son requeridos por el análisis).
4. **Para los pasos**, escoge el troceo más limpio (cada paso ≤ 30 minutos, autocontenido, con verificación clara al final). Si un diseño trocea mejor un área (por ejemplo dominios) y otro trocea mejor otra (por ejemplo vistas), combina lo mejor de cada uno respetando el orden obligatorio.
5. **Para los dominios** (XML completo), escoge la versión más correcta según `k-sistemas` y la coherencia con subsistemas existentes.
6. **Para las firmas de servicios y controladores**, escoge las firmas y comentarios más claros y completos. Si un diseño tiene comentarios más detallados sobre las reglas que aplica un método, úsalos.
7. **Para la descripción de vistas y acciones**, escoge la versión más estructurada y precisa — la que liste con más claridad los campos por panel y los nombres de las acciones con su propósito.
8. **Para la trazabilidad y cobertura total**, construye una matriz con **tres bloques** (`V-XXX`, `R-XXX`, `U-XXX`) → ubicación + comentario, que cubra **todas** las reglas del análisis. Para cada fila debe poder señalarse el método o acción concreta dentro del paso que implementará esa regla. Si alguna regla queda sin paso o sin ubicación, **completa el diseño antes de presentarlo** — no se permite cerrar la unificación con huecos de cobertura.
9. **Renumera los pasos de forma consecutiva sin huecos**, respetando siempre el orden obligatorio (recursos → dominios → servicios → repositorios → controladores → vistas → seguridad → datos iniciales → verificación final).
10. **Aplica las guías de diseño** como criterio adicional en cualquier decisión de unificación donde haya empate o ambigüedad razonable entre las propuestas de los 5 subagentes. Si una opción respeta una guía y la otra no, escoge la que la respeta.
11. **Consolida los "Conflictos detectados con guías"** que cada subagente haya reportado. Elimina duplicados. Si tras la unificación queda algún conflicto sin resolver, **detente y pregunta al usuario con `AskUserQuestion`** antes de presentar el diseño. Las opciones son las mismas que en el pre-flight: (a) actualizar la guía manualmente, (b) actualizar el análisis re-ejecutando `sdd-analyst-system`, (c) ignorar el conflicto explícitamente.

Si en la unificación detectas algo ambiguo o faltante que ninguno de los 5 diseños resolvió, decide la opción más conservadora (que mantenga la trazabilidad con el análisis y respete los skills) y deja anotado el motivo en una sección "Notas de unificación" al final del diseño, **fuera de los pasos** (no contamina la implementación).

El resultado de la Tarea 2 es el **diseño final** que pasará por la revisión de la Fase 2.

---

## Fase 2 — Revisión del diseño antes de guardarlo

Aunque cada subagente ya aplicó este mismo checklist en su Tarea 3 sobre su propio diseño, debes volver a aplicarlo aquí sobre el **diseño unificado** producido en la Tarea 2 — la unificación puede haber introducido inconsistencias (numeración de pasos, nombres mezclados, descripciones combinadas de varias propuestas) que ningún subagente individual podía detectar.

Antes de guardar, comprueba cada punto sobre el diseño unificado:

- [ ] ¿Cada paso tiene toda la información para que `sdd-implementer-system` entienda qué hay que crear sin leer el resto del diseño?
- [ ] ¿Hay algún paso que hace referencia a algo definido en otro paso sin incluir el contexto necesario?
- [ ] ¿Los nombres de clases, métodos, ficheros y acciones son coherentes entre todos los pasos?
- [ ] ¿Algún paso dice "TBD", "similar a", "según convenga" o cualquier placeholder?
- [ ] ¿El paso de verificación final incluye el comando exacto de compilación?
- [ ] **¿El paso de dominios incluye el XML completo de las entidades?** (Única parte con detalle final.)
- [ ] **¿El paso de servicios contiene SOLO firmas de método con comentarios descriptivos, y NO cuerpos implementados?** Si hay lógica Java real dentro, sustituirla por comentarios. La implementación la hace `sdd-implementer-system`.
- [ ] **¿El paso de vistas describe vistas y acciones por nombre + propósito + campos/condiciones, y NO contiene XML completo?** Si hay XML literal de `<form>`, `<grid>`, `<action-validate>`, etc., sustituirlo por la descripción estructural.
- [ ] **¿Cada `<action-view>` está declarado en su propio fichero XML?** (regla arquitectónica de `k-sistemas`). Si dos o más `<action-view>` de la misma entidad están en el mismo fichero, separarlos en ficheros distintos siguiendo la convención `<NombreEntidad>-<discriminador>.xml`. Excepción: `@Search-grid`+`@View-form` van juntos en `<NombreEntidad>-ref.xml`.
- [ ] **¿Los `<menuitem>` se añaden al fichero único `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml`?** (regla de `k-vistas/menus.md`). Si la tabla "Ficheros a crear o modificar" lista cualquier fichero nuevo para menús (`menus-<subsistema>.xml`, `menus-<sistema>.xml`, o similar), sustituirlo por "Modificar `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml`".
- [ ] **¿Los parámetros de los métodos del controlador se llaman `actionRequest` y `actionResponse`?** (regla de `k-sistemas/controladores.md`). Si alguna firma usa `req`/`resp`/`request`/`response`, renombrar a `actionRequest`/`actionResponse`.
- [ ] **¿Las validaciones del análisis funcional están mapeadas a la capa correcta?** Validaciones de campo individual y entre campos del mismo registro → cliente; integridad entre registros, ciclo de vida y reglas de negocio → servidor. Ver `k-validaciones`.
- [ ] **¿TODAS las reglas `V-XXX` del análisis están ubicadas en algún método o acción del diseño con un comentario que describe su lógica?** Recorrer la tabla `V-XXX` del análisis una por una; para cada regla debe existir en algún paso un método (con su firma y comentario) o una acción XML (con su nombre y propósito) donde se implementará. No basta con "se valida en el servicio" — el comentario debe decir qué se comprueba y qué transmite el mensaje.
- [ ] **¿TODAS las reglas de negocio del análisis (operaciones, transiciones de estado, campos calculados, efectos secundarios) tienen un método o acción ubicada en el diseño con un comentario que describe qué hace?** Cada operación que aparece en el análisis debe tener su firma de método o su acción declarada; cada transición de estado debe tener su validación y su acción posterior ubicadas; cada campo calculado debe tener indicado dónde se calcula; cada efecto secundario (envío de correo, generación de PDF, registro de entrada/salida, llamadas a otros subsistemas) debe estar ubicado en un método o acción.
- [ ] **¿La matriz `V-XXX → ubicación` final del diseño tiene una entrada por cada regla del análisis y cada entrada apunta a una clase + método o fichero XML + nombre de acción?** Verificar que no hay huecos ni entradas sin ubicación.
- [ ] **¿Algún paso crea un módulo Guice para un `ModelService`?** Si es así, eliminarlo — `ModelServiceFactory` los descubre automáticamente.
- [ ] **¿Algún paso crea un listener JPA para lógica de negocio?** Si es así, moverlo al servicio como `fireActionRule_*` (firma + comentario).
- [ ] ¿Cada paso es lo suficientemente pequeño para implementarse y verificarse en ≤ 30 minutos?
- [ ] ¿Los pasos respetan el orden obligatorio: recursos → dominios → servicios → repositorios → controladores → vistas → seguridad → datos iniciales → verificación final?
- [ ] ¿El diseño referencia el fichero de análisis de origen en la cabecera (`.sdd/drafts/{carpeta-iniciativa}/analysis_NN/analysis.md`)?
- [ ] ¿El fichero del diseño se guarda en `.sdd/drafts/{carpeta-iniciativa}/analysis_NN/design_NN.md`?
- [ ] **¿El diseño unificado respeta todas las guías de `design-guidelines.md` (si existían)?** Recorrer las guías una a una; para cada una verificar que el diseño la cumple. Si alguna no se cumple sin razón documentada, corregirlo. Si no es posible cumplirla por incompatibilidad con el análisis, detenerse y preguntar al usuario.

Si encuentras algún problema, corrígelo antes de guardar.

---

## Fase 3 — Guardar el diseño

> **REGLA OBLIGATORIA — ruta del diseño:** se guarda en la **carpeta de trabajo** determinada
> en la Fase 0, con el nombre: `design_NN.md` donde NN es el siguiente número disponible
> (cuenta los ficheros `design_*.md` existentes en la carpeta de trabajo y suma 1; formato de 2 dígitos: 01, 02…).
>
> Ejemplo: `.sdd/drafts/2025-05-07_10-30_gestion-firmas/analysis_01/design_01.md`
>
> Pueden existir varios ficheros `design_*.md` en la misma subcarpeta de análisis (iteraciones sucesivas).
> **Nunca en la raíz del proyecto ni en ninguna otra carpeta.**

> **PROCEDIMIENTO OBLIGATORIO antes de escribir el fichero — evita sobrescribir diseños previos:**
>
> 1. **Listar** los ficheros `design_*.md` existentes en la carpeta de trabajo con un comando Bash explícito (no asumir nada):
>    ```bash
>    ls .sdd/drafts/{carpeta-iniciativa}/analysis_NN/design_*.md 2>/dev/null
>    ```
> 2. **Calcular NN** como el mayor número encontrado + 1, formateado a 2 dígitos. Si no hay ningún fichero, NN = `01`. Ejemplos:
>    - No existe ningún `design_*.md` → crear `design_01.md`.
>    - Existen `design_01.md` y `design_02.md` → crear `design_03.md`.
>    - Existen `design_01.md` y `design_03.md` (con hueco) → crear `design_04.md` (siempre **máximo + 1**, **nunca rellenar huecos**).
> 3. **Verificar** que el fichero `design_NN.md` calculado **NO existe** antes de escribirlo. Si por error existiera, **detente** y avisa al usuario; nunca sobrescribas.
> 4. Escribir `design_NN.md` en la carpeta de trabajo.
>
> **PROHIBIDO:**
> - Usar un número fijo como `design_01.md` sin haber listado primero.
> - Escribir `design_NN.md` sobre un fichero existente — eso destruye un diseño anterior. Si el `Write` te pide leer el fichero existente antes de sobrescribir, **es la señal inequívoca de que has elegido un NN equivocado**: vuelve al paso 1 y recalcula NN, no leas el fichero para luego sobrescribirlo.

El fichero del diseño debe comenzar **obligatoriamente** con la siguiente cabecera frontmatter:

```
---
type: design
---
```

Seguida del contenido del diseño unificado y revisado en las fases anteriores.

---

## Fase 4 — Transición a implementación

Al guardar el diseño, indica al usuario:

```
Diseño guardado en .sdd/drafts/{carpeta-iniciativa}/analysis_NN/design_NN.md

Si quieres iterar sobre este diseño, puedes:
  1. Editar (o crear) .sdd/drafts/{carpeta-iniciativa}/design-guidelines.md con guías
     adicionales. Debe empezar con:
       ---
       type: design-guidelines
       ---
     Las guías persisten a nivel de iniciativa: aplican a todos los análisis y
     diseños de esta iniciativa.
  2. Re-ejecutar:
     /sdd-designer-system .sdd/drafts/{carpeta-iniciativa}/analysis_NN/analysis.md

Para implementar este diseño tal cual ejecuta:
  /sdd-implementer-system .sdd/drafts/{carpeta-iniciativa}/analysis_NN/design_NN.md
```

No lances `sdd-implementer-system` tú mismo. El usuario decide cuándo ejecutarlo.