# Contrato del diseño de un tipo de expediente

Define **qué produce el diseño de un trámite y de su tipo de expediente, y cómo**: la estructura de la carpeta `design/`, la estructura obligatoria del `design.md`, qué se materializa verbatim y qué se describe, el orden de los pasos, la especificación quirúrgica de las clases Java/Kotlin y el checklist.

Lo lee, según su rol: el **diseñador** (produce el diseño completo en su carpeta `design_<n>/`), el **juez** (criterios para comparar dos diseños), el **enriquecedor** (qué ventajas incorporar y qué defectos sanear), el **verificador** (qué *debería* existir y con qué forma), el **corrector** (la regla a la que ajustar cada corrección), el **test-unitarios** (§15.2), el **verificador-test-unitarios** y el **corrector-test-unitarios**.

El contrato de las vistas vive aparte, en `vistas.md`.

> **REQUIRED — coherencia con los skills técnicos.** Este contrato **resume** reglas cuya fuente de verdad son `k-tramite`, `k-tipo-expediente` (`SKILL.md`, `modelo.md`, `phaseeventmanager.md`, `validator.md`, `vistas.md`, `documentos.md`, `versionado.md`), `k-validaciones`, `k-datainit` y `k-secure-coding`. Solo el diseñador carga esos skills; los demás roles solo ven este contrato. Si se cambia algo aquí o allí, **MUST** mantenerse sincronizados.

---

## 0. REGLA DE GENERALIDAD — léela antes que nada

**CRITICAL.** Este contrato describe **el patrón**, nunca un trámite concreto.

- **MUST NOT** aparecer en la parte normativa el nombre de ningún trámite, fase, estado, evento, campo, enum, perfil ni panel reales. Se usan **placeholders**.
- **MUST NOT** escribirse ninguna regla que solo valga para un número fijo de fases, estados, eventos, documentos PDF o perfiles. El patrón **MUST** funcionar con **1, 2, 3 o N fases**; con **0, 1 o N** documentos PDF; con firma **en cliente, en servidor, en ambas o en ninguna**; con **0, 1 o N** registros de entrada/salida; con estados **con y sin** `profile`; con eventos **con y sin** guarda.
- Todo ejemplo **MUST** ir encerrado en un bloque que empiece por `> **Ejemplo** (ilustrativo, NO normativo):`.

### 0.1 Placeholders

| Placeholder | Significado | Forma |
|---|---|---|
| `<tramite>` | carpeta del trámite bajo `com/educaflow/tramites/` (puede llevar segmentos de agrupación) | `snake_case` |
| `<Code>` | `<code>` del `TramiteInstance.xml` | `UpperCamelCase`, sin guiones ni underscores |
| `<vN>` | carpeta de versión (`v1`, `v2`, …), posiblemente bajo segmentos de agrupación | `v` + número |
| `<VN>` | la versión en UpperCamel (`V1`, `V2`, …) | |
| `<Entidad>` | entidad JPA y **code del tipo de expediente** | `<Code><VN>` |
| `<basePackageName>` | paquete de la carpeta de versión | lo que sigue a `/java/`, con `/`→`.` |
| `<FASE>` / `<fase>` | `name` de una fase / su carpeta | `UPPER_SNAKE_CASE` / `toLowerCase` |
| `<ESTADO>` / `<Estado>` | `name` de un estado / en UpperCamel | `UPPER_SNAKE_CASE` / `UpperCamel` |
| `<EVENTO>` / `<Evento>` | `name` de un evento / en UpperCamel | `UPPER_SNAKE_CASE` / `UpperCamel` |
| `<PERFIL>` | valor del enum `Profile`: `CREADOR`, `RESPONSABLE`, `SECRETARIO`, `DIRECTOR`, `AUDITOR` | `UPPER_SNAKE_CASE` |
| `<Campo>` | campo de la entidad, en UpperCamel para el getter | |
| `<doc>` | nombre base de un documento de `documentospdf/` | `camelCase` |

---

## 1. Qué produce el diseño (estructura de salida)

El diseñador escribe, dentro de su carpeta `design_<n>/` (que al ganar el torneo se renombra a `design/`), **exactamente** esta estructura:

```
design/
├── design.md                          ← índice (frontmatter type: design)
├── TramiteInstance.xml                ← XML materializado, listo para copiar
├── TipoExpedienteInstance.xml         ← XML materializado
├── domains.xml                        ← XML materializado
├── views.xml                          ← XML materializado (form plantilla de la raíz de la versión)
├── estados.puml                       ← materializado
├── fases/<fase>/views.xml             ← XML materializado, uno por CADA fase
├── documentospdf/<doc>.xml            ← XML materializado, uno por documento (0..N)
├── documentospdf/_<fragmento>.xml     ← XML materializado, 0..N fragmentos reutilizables
├── permisos.xml                       ← fragmento a fusionar en permisos-demo.xml
└── test-e2e-desc.md                   ← tests E2E en Given/When/Then (§15.1)
```

(`test-unit-desc.md` lo añade después el rol `test-unitarios` del motor; el diseñador **MUST NOT** escribirlo. Los logs `log_best.txt`, `log_revision.txt` y `log_revision_unit-test.txt` son del motor: no son contenido de diseño.)

### 1.1 Principio rector — materializar vs describir

| Tipo de artefacto | Qué hace el diseño | Qué hace `/sdd-implementer` |
|---|---|---|
| **XML y `.puml`** | Los **materializa verbatim**: el fichero de `design/` es el artefacto final, ya escrito y completo | Lo **copia literalmente** a su ruta destino |
| **Java (`.java`) y Kotlin (`.kt`)** | **MUST NOT** materializarlos. Se **describen con precisión quirúrgica** en `design.md` (§9, §10, §11) | Escribe el código a partir de esa descripción |

- **MUST NOT** existir ningún `.java` ni `.kt` dentro de `design/`.
- **MUST NOT** existir en `design/` ningún `i18n_es.csv` ni `i18n_ca.csv` (los genera el build; escribirlos a mano está **prohibido** y es un fallo bloqueante), ningún `estados.png` (lo genera `GenerateDocs`), ningún `States.java` (lo genera `GenerateStatesTask`) ni ningún fichero de data-init de trámites/tipos (los genera el build).
- **MUST NOT** existir en `design/` la carpeta `documentospdf/originales/` ni ningún `.pdf` binario: un impreso oficial de partida es material aportado a mano, no producto del diseño.
- Los XML materializados **MUST** estar completos y ser válidos: no llevan `TODO`, ni `...`, ni placeholders sin resolver, ni atributos vacíos de esqueleto (`<button name="">` es un fallo bloqueante).

### 1.2 Qué NO va en el diseño

- **MUST NOT** incluir cuerpos de método Java/Kotlin implementados (nada de `if`/`for`/`switch` reales dentro de un método). La especificación de §10 y §11 es **declarativa**: listas ordenadas de acciones y de reglas, no código.
- **EXCEPCIÓN — el DSL del validador (§11) SÍ se escribe con su sintaxis literal.** Es declarativo, y su literalidad (nombres de reglas y **argumentos exactos**) es justo lo que se implementa sin margen de interpretación.
- **MUST NOT** inventar fases, estados, eventos, campos ni documentos que la especificación no pida.
- **MUST NOT** escribir nada fuera de la carpeta de la iniciativa. Todo cambio en el árbol real (incluido `permisos-demo.xml`) se **describe**; lo aplica `/sdd-implementer`.

---

## 2. Estructura obligatoria de `design.md`

**MUST** tener estas secciones, con estos títulos y **en este orden**:

```markdown
---
type: design
---

# Diseño: <nombre del trámite>

## 1. Objetivo
## 2. Identidad del trámite y del tipo
## 3. Máquina de estados
## 4. Modelo
## 5. Documentos PDF
## 6. Ficheros a crear o modificar
## 7. Pasos
## 8. Especificación del InitialEventManagerImpl
## 9. Especificación de los PhaseEventManagerImpl
## 10. Especificación de los StateEventValidatorImpl
## 11. Reparto de reglas
## 12. Asignación de perfiles
## 13. Tests
## 14. Notas y supuestos
## 15. Checklist del diseñador
```

Los apartados §3–§15 de **este contrato** definen el contenido exigido a cada una de esas secciones. La numeración de las secciones del `design.md` es la de arriba; las referencias cruzadas de este contrato usan los nombres de sección, no el número.

---

## 3. Sección «Objetivo»

**Una frase**: qué permite hacer el trámite y a quién. Nada más.

---

## 4. Sección «Identidad del trámite y del tipo»

Una tabla con **exactamente** estas filas:

| Dato | Valor |
|---|---|
| `code` del trámite | `<Code>` |
| Nombre visible (`<name>`) | … |
| `tipoTramite` | … (**MUST** existir en `TipoTramites.xml` del data-init de expedientes) |
| Carpeta del trámite | `src/main/java/com/educaflow/tramites/<tramite>/` |
| Carpeta de la versión | `src/main/java/com/educaflow/tramites/<tramite>/<…segmentos…>/<vN>/` |
| `<defaultTipoExpediente>` | el **nombre de la carpeta** de versión (`<vN>`), **nunca** la ruta ni el code del tipo |
| `code` / entidad del tipo | `<Entidad>` = `<Code><VN>` |
| FQN de la entidad | `com.educaflow.subsystem.expedientes.db.<Entidad>` |
| `basePackageName` | `<basePackageName>` |
| Clase `States` (generada) | `<basePackageName>.States` |
| Form plantilla | `exp-<Entidad>-Templates` |

Reglas:

- **MUST NOT** llevar el `<Code>` guiones, underscores ni espacios: es el prefijo del nombre de la entidad y del patrón de nombres de vista. El `snake_case` es para la **carpeta**, no para el `code`.
- **MUST NOT** anidar un trámite dentro de otro.
- La carpeta de versión se descubre **a cualquier profundidad**; los segmentos intermedios son solo agrupación. Su **nombre** MUST ser único bajo su trámite.

---

## 5. Sección «Máquina de estados»

### 5.1 Una tabla por fase

Una subsección `### Fase <FASE> — <title de la fase>` por cada fase, en el **orden en que se declaran** en el `TipoExpedienteInstance.xml` (el orden se conserva y se verifica), con una tabla de una fila por estado:

| estado | title | perfil | eventos | inicial | closed |
|---|---|---|---|---|---|
| `<ESTADO>` | … | `<PERFIL>` o `—` | `<EVENTO>,<EVENTO>` o `—` | `sí`/`—` | `sí`/`—` |

Reglas:

- **MUST** haber **exactamente un** estado con `inicial = sí` en **todo el tipo** (no uno por fase).
- La columna `eventos` **MUST** reflejar el atributo `events` en su **orden literal**.
- **MUST NOT** aparecer `EXIT` en la columna `eventos`: `ExpedienteController` lo intercepta antes del `Tramitador` y sería código muerto. `EXIT` es un **botón puro de UI** (ver `vistas.md`).
- `DELETE` **sí** se declara en `events` del estado desde el que se pueda borrar.
- La columna `perfil` **MUST** contener un valor del enum `Profile` (`CREADOR`, `RESPONSABLE`, `SECRETARIO`, `DIRECTOR`, `AUDITOR`) o `—`.
- **A1 — nombres reservados. MUST NOT** nombrar un estado o un evento de forma que el método derivado (`onEnter<Estado>`, `trigger<Evento>`, `getForState<Estado>InEvent<Evento>`) coincida con un método público de `PhaseEventManager`, `StateEventValidator` o `InitialEventManager` (se compara solo el nombre, sin firma). En particular **MUST NOT** llamarse un estado `STATE`, ni un evento `INITIAL_EVENT`.
- El `title` de la fase **no es documental**: es el texto que el usuario ve en la cabecera de **todos** los formularios.

### 5.2 Tabla de transiciones — la fuente de verdad de los `updateState`

Una única tabla, con **una fila por transición**:

| fase origen | estado origen | evento | guarda | fase destino | estado destino |
|---|---|---|---|---|---|
| `<FASE>` | `<ESTADO>` | `<EVENTO>` | `<campo>=<VALOR>` o `—` | `<FASE>` | `<ESTADO>` |

**CRITICAL — esta tabla es la fuente de verdad de los `eventContext.updateState(...)` de la sección «Especificación de los PhaseEventManagerImpl» y de las transiciones del `estados.puml`.** Las tres representaciones (tabla, `design.md` §9, `estados.puml`) **MUST** coincidir exactamente.

Reglas:

- **MUST** haber una fila por cada pareja (estado, evento) declarada, más la fila del arranque.
- **Arranque**: fila con `fase origen`/`estado origen` = `[*]`, evento `—`, destino = el estado `inicial`.
- **`DELETE`**: fila con `fase destino`/`estado destino` = `[*]`. El expediente se elimina; su `triggerDelete` **MUST NOT** llamar a `updateState`.
- Una transición **puede cruzar de fase**: no requiere nada especial (`updateState(States.<OtraFase>.<ESTADO>)`); su `onEnter` lo atiende el manager de la fase **destino**.
- Un evento **puede ramificar** a varios destinos: entonces hay **varias filas** con el mismo (origen, evento) y distinta `guarda`. La guarda **MUST** ser una condición sobre **un campo** de la entidad (típicamente un enum) y **MUST** cubrir todos los valores posibles o declarar explícitamente el `default` como error.
- Un evento **puede no cambiar de estado** (destino = origen, guarda `—`): es legítimo y significa que su `trigger*` **no** llama a `updateState`.
- Un mismo evento declarado en **varios estados** de la misma fase produce **varias filas** y **un solo** `trigger<Evento>` (sección «Especificación de los PhaseEventManagerImpl»), pero **un método de validación por estado** (sección «Especificación de los StateEventValidatorImpl»). Esta asimetría es deliberada.

### 5.3 `estados.puml` materializado

**MUST** existir `design/estados.puml` y **MUST** ser una proyección fiel de §5.1 y §5.2:

- Cada fase se dibuja como un **estado compuesto** `state <FASE> { ... }`.
- **CRITICAL — el alias de todo estado MUST ser `<FASE>_<ESTADO>`**: en PlantUML el identificador es **global**, mientras que en el XML el nombre solo es único dentro de su fase; dos estados homónimos en fases distintas se fundirían en un único nodo.
- **MUST NOT** usarse ningún alias que no corresponda a un estado declarado: PlantUML **no da error**, crea un nodo nuevo en silencio.
- **MUST** aparecer **todo** estado del XML.
- **CRITICAL — el diagrama lleva exactamente una agrupación `state <FASE> { ... }` por cada fase declarada en el `TipoExpedienteInstance.xml`**, sean **una, dos o N**, y **todas** las transiciones de la tabla de transiciones (§5.2), ni una más ni una menos. El número de bloques y de flechas no lo fija ningún esqueleto: lo fija la máquina de estados del tipo que se está diseñando.
- Etiqueta de la transición: `<EVENTO>` si no hay guarda, `<EVENTO>[<campo>=<VALOR>]` si la hay.
- Un estado `closed` se anota `<FASE>_<ESTADO> : closed`.
- El arranque es `[*] --> <FASE>_<ESTADO INICIAL>`; `DELETE` es `<FASE>_<ESTADO> -> [*] : DELETE`.

> **Ejemplo** (ilustrativo, NO normativo) de esqueleto. Se dibujan **dos** fases con un estado cada una **solo** para poder mostrar el alias cualificado y una transición entre fases; no es la forma obligatoria. Un tipo de **una** sola fase lleva **un** bloque `state`, uno de **N** fases lleva **N**, y cada fase lleva tantas líneas `state "<ESTADO…>" as …` como estados declare:
>
> ```
> @startuml
> state <FASE1> {
>     state "<ESTADO1.1>" as <FASE1>_<ESTADO1.1>
>     state "<ESTADO1.2>" as <FASE1>_<ESTADO1.2>
> }
> ' … un bloque state <FASEn> { … } por cada fase restante, si el tipo tiene más de una
> state <FASEn> {
>     state "<ESTADOn.1>" as <FASEn>_<ESTADOn.1>
> }
>
> [*] --> <FASE1>_<ESTADO INICIAL>
> <FASE1>_<ESTADO1.1> -> [*] : DELETE
> <FASE1>_<ESTADO1.1> --> <FASE1>_<ESTADO1.2> : <EVENTO>
> <FASE1>_<ESTADO1.2> --> <FASEn>_<ESTADOn.1> : <EVENTO>[<campo>=<VALOR>]
> ' … una línea por cada fila de la tabla de transiciones de §5.2
>
> <FASEn>_<ESTADOn.1> : closed
> @enduml
> ```

**MUST NOT** incluirse `estados.png` en el diseño: lo renderiza `GenerateDocs`, que `./run.sh` ya ejecuta.

### 5.4 `TipoExpedienteInstance.xml` materializado

**MUST** existir `design/TipoExpedienteInstance.xml`, con raíz `<TipoExpediente>` y un único `<fases>`:

```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<TipoExpediente>
    <fases>
        <fase name="<FASE>" title="<Título de la fase>">
            <state name="<ESTADO>" events="<EVENTO>,<EVENTO>" profile="<PERFIL>" title="<Título>" initial="true"/>
            <state name="<ESTADO>" events="" profile="<PERFIL>" title="<Título>" closed="true"/>
        </fase>
    </fases>
</TipoExpediente>
```

- **MUST** escribirse siempre `events`, aunque sea `events=""`: omitirlo equivale en silencio a vacío.
- **MUST NOT** usarse un `<states>` suelto en la raíz: es el formato anterior a las fases y el parseo aborta.
- **MUST NOT** usarse `ambitoCreador`/`ambitoResponsable`/`ambitoAuditor`: hoy son inertes.
- Un tag desconocido lo **ignora JAXB en silencio**: cualquier typo aplica el default sin avisar.

### 5.5 `TramiteInstance.xml` materializado

**MUST** existir `design/TramiteInstance.xml`, con raíz `<Tramite>` y los datos como **elementos**, no atributos:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Tramite>
    <code><Code></code>
    <name><nombre visible></name>
    <tipoTramite><TIPO_TRAMITE></tipoTramite>
    <defaultTipoExpediente><vN></defaultTipoExpediente>
    <help><![CDATA[ ... HTML ... ]]></help>
</Tramite>
```

`<publico>` y `<privado>` son opcionales. Si el `<help>` contiene `]]>`, el generador lanza `RuntimeException`.

---

## 6. Sección «Modelo»

### 6.1 Tabla de campos de la entidad

| nombre | tipo | ref | title | para qué sirve | quién lo rellena |
|---|---|---|---|---|---|
| `<campo>` | `string`/`integer`/`decimal`/`boolean`/`date`/`time`/`datetime`/`enum`/`many-to-one`/`one-to-many` | FQN o nombre del `ref`, o `—` | … o `—` | … | `usuario` \| `servidor` |

**CRITICAL — la columna «quién lo rellena» es normativa y decide la frontera de confianza.**

- `usuario` = el valor lo aporta la persona desde el formulario en algún evento. **Solo estos campos pueden aparecer en un `field(...)` del validador** (§11).
- `servidor` = lo dicta el servidor (PDFs generados por un `trigger*`, resguardos de registro, campos calculados, snapshots). **MUST NOT** aparecer en ningún `field(...)`.
- **Motivo (CRITICAL):** el `Tramitador` construye el `AllowProperties` de cada evento **a partir de las reglas del validador**. Un campo que no está en ningún `field(...)` de esa pareja (estado, evento) **no se copia** desde la petición; darle entrada lo hace **escribible por el cliente**. Declarar los campos es exactamente lo que los hace editables, así que la columna no es documentación: es la lista de permisos.
- **MUST NOT** confiarse en `readonly`, `showIf` o `hidden` de la vista como defensa: no lo son.
- **MUST NOT** listar en esta tabla los campos heredados de `Expediente`: `tipoExpediente`, `name`, `numeroExpediente`, `codePhase`, `namePhase`, `codeState`, `nameState`, `fechaUltimoEstado`, `abierto`, `historialEstados`, `centro`, `usuarioRegistrador`, `personaSolicitante`, `personaInteresada`, `dniFirmaDocumentoEntrada`. Redeclararlos en el `domains.xml` está prohibido. (Sí pueden **referenciarse** desde §8, §9, §11 y las vistas.)
- Todo campo `servidor` **MUST** estar respaldado por al menos una acción de las secciones «Especificación del InitialEventManagerImpl» o «Especificación de los PhaseEventManagerImpl» del `design.md` que lo asigne; si ninguna lo asigna, o sobra el campo o falta la acción.

> **Ejemplo** (ilustrativo, NO normativo): un campo `many-to-one` a `com.axelor.meta.db.MetaFile` que un `trigger*` rellena con el PDF que acaba de generar es `servidor`; el `MetaFile` que la persona sube desde el formulario es `usuario`.

### 6.2 Tabla de enums

Una fila por ítem:

| enum | numeric | ítem | title | value |
|---|---|---|---|---|
| `<Nombre><Entidad>` | `sí`/`—` | `<ITEM>` | … | … o `—` |

- **CRITICAL** — todos los tipos de expediente comparten el paquete `com.educaflow.subsystem.expedientes.db`, así que el `name` de todo enum propio **MUST** llevar `<Entidad>` como **sufijo** para no colisionar entre tipos ni entre versiones.
- `value` solo aplica si el enum es `numeric="true"`.

### 6.3 `domains.xml` materializado

**MUST** existir `design/domains.xml`:

- **MUST** declarar `<module name="expedientes" package="com.educaflow.subsystem.expedientes.db"/>`. **MUST NOT** cambiarse: el paquete está hardcodeado en los build-tools y en los tests.
- **CRITICAL — la entidad del tipo MUST ser la PRIMERA `<entity>` del fichero** en orden de documento. Es convenio puro, no hay marca. **MUST NOT** ponerse una entidad auxiliar por delante.
- **MUST** ser `<entity name="<Entidad>" extends="Expediente">`. Sin el `extends`, la inyección del `<extra-code-model>` avisa pero **no detiene el build**.
- Los enums van como `<enum>` hermanos de la entidad, con el sufijo de §6.2.
- **MUST** ser `<extra-code-model>` y no `<extra-code>`: el primero inyecta en la entidad, el segundo en el `Repository`.

**Regla de obligatoriedad (CRITICAL, específica de expedientes).** **MUST NOT** usarse `required="true"` en el `domains.xml` de un tipo de expediente para un campo que la persona rellena durante la tramitación: el expediente existe en BD desde el estado inicial, con todos esos campos vacíos, y un `NOT NULL` lo haría inguardable. La obligatoriedad de un campo es **por pareja (estado, evento)** y vive en el DSL del validador (`+Required()`), nunca en el modelo. Ver §11.

- ✅ CORRECTO: `<string name="<campo>"/>` en el modelo + `field(model::get<Campo>) { +Required() }` en la pareja (estado, evento) donde se pide.
- ❌ INCORRECTO: `<string name="<campo>" required="true"/>` (rompe el alta del expediente y los estados intermedios).

**Bloque `<extra-code-model>`.** Si el tipo genera **al menos un** PDF, el `domains.xml` **MUST** llevar este bloque, con **una constante por cada `documentospdf/<doc>.xml`** (nombre base en `UPPER_SNAKE_CASE`) y su ruta de recurso absoluta:

```xml
<extra-code-model>
<![CDATA[

    public enum TipoDocumentoPdf {
        <DOC>("/com/educaflow/tramites/<tramite>/<…>/<vN>/documentospdf/<doc>.pdf");

        private final String fileName;

        // Constructor privado del enum
        TipoDocumentoPdf(String fileName) {
            this.fileName = fileName;
        }

        // Getter
        public String getFileName() {
            return fileName;
        }
    }


    public com.educaflow.base.infrastructure.pdf.DocumentoPdf getDocumentoPdf(TipoDocumentoPdf tipoDocumentoPdf) {
        return com.educaflow.subsystem.expedientes.services.internal.ExpedienteUtil.getDocumentoPdf(this, tipoDocumentoPdf.getFileName());
    }

]]>
</extra-code-model>
```

- Si el tipo genera **cero** PDFs, **MUST NOT** llevar el bloque.
- Los ficheros con prefijo `_` (fragmentos) **MUST NOT** tener constante: no generan PDF propio.
- El bloque lo **reescribe el build** en cada compilación: **MUST NOT** personalizarse más allá de las constantes que correspondan a los documentos reales.

---

## 7. Sección «Documentos PDF»

Una tabla, con una fila por documento generable (0..N filas; si el tipo no genera ninguno, la sección lo dice en una frase y no lleva tabla):

| fichero | constante | en qué transición se genera | en qué campo se guarda | quién lo firma | se registra |
|---|---|---|---|---|---|
| `documentospdf/<doc>.xml` | `<DOC>` | `<FASE>.<ESTADO>` + `<EVENTO>` | `<campo>` (`servidor`) | `cliente (AutoFirma__!!)` \| `servidor: DIRECTOR` \| `servidor: SECRETARIO` \| `servidor: DNI <expr>` \| `—` | `entrada` \| `salida` \| `—` |

Y una lista aparte de los **fragmentos** `documentospdf/_<fragmento>.xml` con quién los incluye.

Reglas:

- **CRITICAL — la carpeta MUST llamarse `documentospdf`.** `documentos/` renderiza pero **no** se escanea para el enum: el documento queda muerto sin aviso. **MUST NOT** usarse.
- Los nombres de fichero **MUST** ir en `camelCase`, sin espacios ni guiones. Se convierten en la constante del enum (`UPPER_SNAKE_CASE`).
- **MUST NOT** convivir un `<doc>.xml` y un `<doc>.pdf` con el mismo nombre: el build aborta por ambigüedad.
- Un fichero cuyo nombre empieza por `_` es un **fragmento**: raíz `<fragmento>`, **no** genera PDF propio y **no** tiene constante.
- `<include href="_<fragmento>.xml"/>` va **solo** como hijo directo de `<documento>`/`<fragmento>`, **nunca** dentro de una `<seccion>`.
- Los `colspan` de cada `<fila>` **MUST** sumar 12 o un múltiplo de 12; un elemento **MUST NOT** cruzar el límite de 12.
- `<valenciano>` y `<castellano>` **MUST** ir como **elementos hijos**, nunca como atributos. Omitir `<valenciano>` → lo traduce el build; ponerlo **vacío** → solo castellano. **MUST NOT** quitarse un `<valenciano>` ya escrito para que lo retraduzca el build.
- **CRITICAL — los fallos de evaluación de las expresiones son SILENCIOSOS**: cada `nombreCampo` se evalúa como **Groovy** con el contexto `{ self = el expediente, now = LocalDateTime.now() }`; una expresión que revienta no rompe el build ni el evento, se escribe en el log y **el campo queda vacío**. El diseño **MUST** usar solo rutas `self.*` que existan en la entidad (o en `Expediente`), y el paso de verificación final **MUST** exigir revisar el PDF generado en runtime.

Raíces admitidas:

```xml
<documento xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
           xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/educaflow/EducaFlowBuildTools/master/src/main/resources/com/educaflow/common/buildtools/xml2pdf/documento.xsd">
    <include href="_<fragmento>.xml"/>
    <seccion>
        <castellano>…</castellano>
        <fila>
            <campo nombreCampo="self.<campo>" colspan="12"><castellano>…</castellano></campo>
            <check nombreCampo="<expresión booleana>" colspan="12"><castellano>…</castellano></check>
            <texto colspan="12"><castellano>…</castellano></texto>
        </fila>
    </seccion>
</documento>
```

(Un fragmento es idéntico salvo que su raíz es `<fragmento>`.)

---

## 8. Sección «Ficheros a crear o modificar»

Una tabla con **exactamente** estas columnas y este título de sección (los usa el descomponedor de `/sdd-implementer`):

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|

Reglas:

- **MUST** haber **una fila por cada fichero real del árbol** que la implementación crea o modifica; ni una de más ni una de menos. Las rutas son **relativas a la raíz del proyecto**.
- **MUST** incluir la fila de `src/main/resources/data-demo/input/permisos-demo.xml` con acción **`Modificar`**.
- **MUST NOT** aparecer ningún `i18n_*.csv`, `estados.png`, `States.java`, data-init generado, ni ningún fichero bajo `build/`.
- La columna `Acción` es `Crear` o `Modificar`. Una fila `Crear` cuyo destino ya exista en el árbol real es un diseño **erróneo**: debe ser `Modificar`, y entonces el XML del diseño **MUST** ser el fichero real como base **más** el delta (se copia verbatim y sobrescribe).
- La columna `Skill` nombra el skill que gobierna ese fichero.
- La columna `Descripción` dice, para un XML, **de qué fichero de `design/` se copia**; para un `.java`/`.kt`, **qué sección de `design.md` lo especifica**.

Inventario mínimo (rutas relativas a `src/main/java/com/educaflow/tramites/`, salvo las dos últimas filas):

| Fichero | Acción | Skill |
|---|---|---|
| `<tramite>/TramiteInstance.xml` | Crear | `k-tramite` |
| `<tramite>/…/<vN>/TipoExpedienteInstance.xml` | Crear | `k-tipo-expediente` |
| `<tramite>/…/<vN>/domains.xml` | Crear | `k-tipo-expediente` (`modelo.md`) |
| `<tramite>/…/<vN>/InitialEventManagerImpl.java` | Crear | `k-tipo-expediente` (`phaseeventmanager.md`) |
| `<tramite>/…/<vN>/views.xml` | Crear | `k-tipo-expediente` (`vistas.md`) |
| `<tramite>/…/<vN>/estados.puml` | Crear | `k-tipo-expediente` |
| `<tramite>/…/<vN>/<fase>/PhaseEventManagerImpl.java` | Crear | `k-tipo-expediente` (`phaseeventmanager.md`) |
| `<tramite>/…/<vN>/<fase>/StateEventValidatorImpl.kt` | Crear | `k-tipo-expediente` (`validator.md`), `k-secure-coding` |
| `<tramite>/…/<vN>/<fase>/views.xml` | Crear | `k-tipo-expediente` (`vistas.md`) |
| `<tramite>/…/<vN>/documentospdf/<doc>.xml` | Crear | `k-tipo-expediente` (`documentos.md`) |
| `src/main/resources/data-demo/input/permisos-demo.xml` | **Modificar** | `k-datainit` |

Las tres filas de fase se repiten **por cada fase**; la de `documentospdf` **por cada documento y fragmento**. Si el tipo no genera PDFs, esas filas no existen.

---

## 9. Sección «Pasos»

Una subsección `### Paso N — <fichero o grupo>` por cada fila (o grupo homogéneo de filas) de la tabla anterior, **en el orden obligatorio de creación**:

| Paso | Contenido |
|---|---|
| 1 | `TramiteInstance.xml` |
| 2 | `TipoExpedienteInstance.xml` **completo** (todas las fases, todos los estados, todos los `events`) |
| 3 | **Ejecutar `CreateFilesTask`** (§9.1) |
| 4 | `domains.xml` |
| 5 | `InitialEventManagerImpl.java` |
| 6 | `views.xml` de la raíz de la versión |
| 7 | `<fase>/PhaseEventManagerImpl.java` — uno por fase |
| 8 | `<fase>/StateEventValidatorImpl.kt` — uno por fase |
| 9 | `<fase>/views.xml` — uno por fase |
| 10 | `estados.puml` |
| 11 | `documentospdf/*.xml` (si los hay) |
| 12 | `permisos-demo.xml` |
| 13 | Verificación final: `./run.sh` |

**MUST NOT** alterarse ese orden. Los pasos 7, 8 y 9 se instancian **una vez por fase**; el 11, una vez por documento (o uno solo que los agrupe).

### 9.1 Paso 3 — CreateFilesTask (CRITICAL)

**MUST** existir un paso dedicado, **exactamente** en esa posición (después del `TipoExpedienteInstance.xml`, antes de rellenar nada), cuyo cuerpo es:

```
./gradlew -q CreateFilesTask -Ptipo=src/main/java/com/educaflow/tramites/<tramite>/<…>/<vN>
```

El paso **MUST** declarar además:

- Que la tarea **lee** el `TipoExpedienteInstance.xml`, así que el paso 2 tiene que estar **completo** antes: cada `<fase>` declarada produce su subcarpeta.
- Qué crea exactamente: en la raíz de la versión `domains.xml`, `views.xml` e `InitialEventManagerImpl.java`; en **cada** `<fase>/`, `PhaseEventManagerImpl.java`, `StateEventValidatorImpl.kt` y `views.xml`.
- Que imprime una línea `CREADO <ruta>` por fichero creado — el paso **MUST** indicar que la verificación es que aparezca una línea `CREADO` por cada uno de esos ficheros.
- Que es **idempotente y nunca pisa lo ya escrito**.
- **MUST NOT** usarse `-Pfase` (acota a una fase y entonces **no** genera los ficheros de la raíz de la versión); y si alguna vez se usara, **MUST** ir siempre junto con `-Ptipo`.
- **MUST NOT** engancharse al build: compilar no debe escribir en `src/main/java`.

### 9.2 Pasos de ficheros XML y `.puml`

El cuerpo del paso es, literalmente, de esta forma:

> El fichero está materializado en `design/<x>`. **Cópialo literalmente** a `<ruta destino en el árbol>`, **sobrescribiendo** el esqueleto que dejó `CreateFilesTask` si lo hay. **MUST NOT** modificarlo, reescribirlo ni regenerarlo.

Más una **verificación** concreta (qué comprobar: que el fichero existe, que el `diff` con el del diseño es vacío, y para los `views.xml` de fase que ningún `<button name="">` quedó sin rellenar).

### 9.3 Pasos de ficheros Java/Kotlin

El cuerpo del paso **MUST**:

1. Nombrar el fichero destino y el FQCN de la clase.
2. Apuntar, con el título exacto de la sección, a su especificación quirúrgica: `## 8. Especificación del InitialEventManagerImpl`, `## 9. …` o `## 10. …`.
3. Declarar el supertipo/interfaz y el parámetro de tipo.
4. Declarar la verificación (compila; existe un método por cada elemento de la lista de cobertura de §10/§11).

**MUST NOT** duplicar la especificación en el paso: una sola fuente de verdad, en §8/§9/§10 del `design.md`. Dos copias divergen.

> **REQUIRED para `/sdd-implementer`:** toda tarea que materialice un `.java` o un `.kt` **MUST** leer el `design.md` **entero** (secciones 8, 9 y 10 incluidas), no solo su `## Paso N`.

### 9.4 Paso 12 — `permisos-demo.xml`

El paso **MUST** decir que es una **fusión**, no una copia: se añaden a `src/main/resources/data-demo/input/permisos-demo.xml` los elementos de `design/permisos.xml`, **dentro del bloque que corresponda a cada uno**, **conservando todo lo preexistente**. Ver §13.

### 9.5 Paso 13 — verificación final

**MUST** ser el último y **MUST** contener el comando exacto `./run.sh` y qué se comprueba:

- `BUILD SUCCESSFUL`, con los tests de `com/educaflow/tiposexpedientes` y `com/educaflow/views` en verde (los ejecuta ese mismo build).
- Que se regeneró `estados.png` (`GenerateDocs` va enganchada a `build` con `finalizedBy`).
- **REQUIRED — comprobación en runtime**: navegar por **todos** los estados con usuarios de los perfiles adecuados. Los tests cubren la forma, no el comportamiento. En particular, lo que **nada** comprueba en build: el `dniFirmaDocumentoEntrada` (revienta al firmar en cliente), `personaSolicitante`/`personaInteresada` (NPE al crear el registro de entrada), las **transiciones** del `.puml`, y las expresiones Groovy de `documentospdf/` (fallo silencioso: log + campo vacío).

---

## 10. Sección «Especificación del InitialEventManagerImpl»

Fichero: `<vN>/InitialEventManagerImpl.java`, **en la raíz de la carpeta de versión, NUNCA en una subcarpeta de fase**.

**MUST** declarar:

```java
package <basePackageName>;

public class InitialEventManagerImpl implements InitialEventManager<<Entidad>> {
    @Override
    public void triggerInitialEvent(<Entidad> expediente, EventContext eventContext) throws BusinessException { … }
}
```

- **MUST** existir **exactamente uno por tipo**, con FQCN `<basePackageName>.InitialEventManagerImpl`.
- **MUST NOT** usarse `InitialEventManager` en crudo: el parámetro de tipo es el **único** sitio donde el tipo declara cuál es su entidad, y es lo que `ExpedienteLocator.getModelClass` lee en runtime.
- **MUST** declarar **exactamente un** `triggerInitialEvent(<Entidad>, EventContext): void`. No lleva anotación.

**Contenido obligatorio de la sección**: una tabla de asignaciones **en orden**:

| # | Setter | Fuente del valor | Por qué |
|---|---|---|---|
| 1 | `set<Campo>(…)` | expresión exacta (constante, `LocalDate.now()…`, un campo de `getUsuarioRegistrador()`, un objeto construido…) | … |

**CRITICAL — el orden es normativo**: es lo que se implementa y lo que se compara.

Reglas que la sección **MUST** hacer explícitas:

- Lo que el `Tramitador` ya rellena **antes** de llamar al `triggerInitialEvent` y que **MUST NOT** reasignarse: `tipoExpediente`, `centro`, `usuarioRegistrador`, `name`, `numeroExpediente`.
- Lo que hace **después** y que tampoco es cosa de este método: fijar el estado inicial, crear el `HistorialEstado` y llamar al `onEnterState`.
- **CRITICAL** — si el tipo firma algún documento **en cliente** (AutoFirma), el método **MUST** dejar `dniFirmaDocumentoEntrada` con un DNI válido: es lo que comprueba `FirmaController.firmarDocumentoEntrada`, y **nada lo verifica en build**.
- **CRITICAL** — si algún `trigger*` llama a `createRegistroEntrada`, el método **MUST** dejar `personaSolicitante` y `personaInteresada` no nulos: `createRegistroEntrada` lanza **NPE** si son `null`, y **nada lo verifica en build**.
- Si el tipo no firma en cliente ni crea registros de entrada, esas dos reglas no aplican y la sección **MUST** decirlo explícitamente.
- **MUST NOT** llamar a `eventContext.updateState(...)`: el estado inicial lo fija el `Tramitador`.

Dependencias a inyectar (`@Inject`), si las hay: lista con tipo y para qué se usa. Si no hay ninguna, decirlo.

---

## 11. Sección «Especificación de los PhaseEventManagerImpl»

Una subsección `### Fase <FASE>` **por cada fase**, con esta forma:

```java
package <basePackageName>.<fase>;

public class PhaseEventManagerImpl extends PhaseEventManager<<Entidad>> {
    @Inject
    public PhaseEventManagerImpl(<Entidad>Repository repository) {
        super(<Entidad>.class);
        this.repository = repository;
    }
}
```

### 11.1 Cabecera de la fase

**MUST** declarar:

- FQCN: `<basePackageName>.<fase>.PhaseEventManagerImpl`. La **carpeta MUST llamarse como la fase en minúsculas**; en cualquier otra es código muerto que aparenta estar vivo.
- Supertipo: `extends PhaseEventManager<<Entidad>>` — **MUST NOT** usarse en crudo.
- Interfaces adicionales implementadas, si las hay, y por qué (con sus métodos).
- Constructor `@Inject` con el `<Entidad>Repository` y `super(<Entidad>.class)`.
- **Dependencias a inyectar** (`@Inject`), una por línea: tipo, nombre y para qué se usa. Toda dependencia usada por algún `trigger*` **MUST** estar declarada aquí.
- **Constantes de clase** que necesiten los `trigger*` (típicamente `private static final Rectangulo <nombre> = new Rectangulo(<x>, <y>, <ancho>, <alto>);` para una posición de firma).
- El `import` de `States`: **MUST** ser el de la **propia versión** (`<basePackageName>.States`). **MUST NOT** referenciarse el `States` de otro tipo o versión: compila y revienta en runtime.

### 11.2 Lista de `trigger<Evento>`

**MUST** haber **exactamente un** `trigger<Evento>` por cada evento de **la unión sin repetir de los `events` de todos los estados de la fase**, incluido `DELETE` si está declarado:

```java
@WhenEvent
public void trigger<Evento>(<Entidad> expediente, <Entidad> original, EventContext eventContext) throws BusinessException
```

- Anotado **`@WhenEvent`**, con **DOS** parámetros de entidad (el actual y el original clonado) y `EventContext`, retorno `void`.
- **MUST NOT** sobrar ningún `@WhenEvent` cuyo nombre no corresponda a un evento de **la propia fase**: el trigger de un evento de otra fase va en el manager de **esa otra** fase.
- **MUST NOT** declararse `triggerInitialEvent` en un `PhaseEventManagerImpl`: el evento inicial es del tipo, no de una fase, y allí no se llamaría nunca.
- **MUST NOT** declararse `triggerExit`: `EXIT` nunca llega al manager.

Por **cada** trigger, una **lista numerada de acciones en orden**. **CRITICAL — el ORDEN de las acciones es normativo**, porque es exactamente lo que se implementa y lo que se compara.

Vocabulario de acciones (uno por línea; el diseño usa esta notación, no código):

| Acción | Notación | Efecto |
|---|---|---|
| Generar PDF | `GENERAR_PDF(<DOC>)` | `expediente.getDocumentoPdf(<Entidad>.TipoDocumentoPdf.<DOC>)` |
| Firmar en servidor | `FIRMAR_SERVIDOR(cargo=<DIRECTOR\|SECRETARIO\|DNI:<expr>\|DUMMY>, rect=<constante>, [pagina=<n>], [motivo=…], [mensaje=…])` | `documentoPdf.firmar(almacenClaveResolver.get<Cargo>(<centro>), new CampoFirma(<constante>)…)` |
| Materializar | `CREAR_METAFILE → <campo>` | `MetaFileHelper.createMetaFile(<documentoPdf>)` y `expediente.set<Campo>(…)` |
| Registro de entrada | `REGISTRO_ENTRADA(documento=<campo>, anexos=[<campo>,…]) → <campo>` | `eventContext.createRegistroEntrada(…)`; el `<campo>` destino recibe `registro.getDocumentoResguardoPresentacion()` |
| Registro de salida | `REGISTRO_SALIDA(documento=<campo>, anexos=[<campo>,…]) → <campo>` | `eventContext.createRegistroSalida(…)`; el `<campo>` destino recibe `registro.getDocumento()` |
| Limpiar campos | `LIMPIAR(<campo>, <campo>)` | `expediente.set<Campo>(null)` para cada uno |
| Asignar | `ASIGNAR(<campo> = <expresión>)` | asignación de un campo `servidor` |
| Transición | `UPDATE_STATE(States.<Fase>.<ESTADO>)` | `eventContext.updateState(...)` |
| Transición ramificada | `UPDATE_STATE segun <campo>: <VALOR> → States.<Fase>.<ESTADO>; …; default → error` | `switch` sobre el discriminador |
| Error de negocio | `ERROR_NEGOCIO(<condición>, <qué transmite el mensaje>)` | `throw new BusinessException(...)` |
| Llamada a otro subsistema | `SERVICIO(<tipo>.<método>(…))` | dependencia inyectada declarada en §11.1 |

Reglas de los triggers:

- **LIMIT: un solo `createRegistroEntrada` y un solo `createRegistroSalida` por evento.** Llamar dos veces al mismo lanza `RuntimeException("Ya existe un registro de entrada definido")`.
- El `documentoPdf` de un `create*` **MUST NOT** ser `null`; los anexos se clonan, `null` se admite como lista vacía, y cada `MetaFile` de la lista **MUST** tener `fileName` no nulo.
- Los `create*` **NO** hacen `repository.save`: se persisten por cascada al colgarlos del `HistorialEstado`.
- Un trigger **puede** no llamar a `UPDATE_STATE`: deja el expediente donde estaba. Es legítimo y **MUST** declararse explícitamente (`UPDATE_STATE: ninguno (permanece en <ESTADO>)`).
- **`triggerDelete` MUST NOT** llamar a `UPDATE_STATE`: el expediente se elimina justo después y el `onEnterState` no llega a ejecutarse. Su cuerpo puede quedar **vacío**; existe solo porque `DELETE` está declarado en `events`.
- El destino de cada `UPDATE_STATE` **MUST** coincidir con la tabla de transiciones (§5.2), guarda incluida.
- Cruzar de fase **no requiere nada especial**: `UPDATE_STATE(States.<OtraFase>.<ESTADO>)` y basta.
- **MUST NOT** comprobar el trigger si el evento es disparable desde el estado actual: ya lo comprobó el `Tramitador`.
- **MUST NOT** validar datos del usuario en un trigger: eso es del validador (sección «Especificación de los StateEventValidatorImpl»).
- Al ramificar sobre el **estado actual**, el discriminador se obtiene con `States.INSTANCE.getState(codePhase, codeState)` y el `switch` **solo** puede recibir estados de la **propia fase** (el evento lo atiende el manager de la fase en la que está el expediente). El `default` es **obligatorio**: `State` no es `sealed`.
- Los errores de negocio **MUST** lanzarse como `BusinessException`: el `Tramitador` hace `detach` y los propaga como mensajes de usuario. Cualquier otra excepción se convierte en `RuntimeException`.
- **MUST NOT** usarse `System.out`: logger slf4j.

> **Ejemplo** (ilustrativo, NO normativo): un trigger que genera un PDF, lo firma con el cargo del centro, lo registra de salida y ramifica:
>
> 1. `GENERAR_PDF(INFORME)`
> 2. `FIRMAR_SERVIDOR(cargo=DIRECTOR, rect=POSICION_FIRMA_INFORME)`
> 3. `CREAR_METAFILE → pdfTemporal`
> 4. `REGISTRO_SALIDA(documento=pdfTemporal, anexos=[adjuntoAportado]) → pdfInforme`
> 5. `UPDATE_STATE segun sentidoDecision: APROBAR → States.Revision.APROBADO; DENEGAR → States.Revision.DENEGADO; default → error`
>
> (`INFORME`, `sentidoDecision`, `Revision`, `APROBADO`… son nombres **inventados** para el ejemplo: el patrón vale para cualquier documento, discriminador, fase y estados.)

### 11.3 Lista de `onEnter<Estado>`

**MUST** haber **exactamente un** `onEnter<Estado>` por cada estado de la fase, **incluidos los estados sin eventos y los `closed`**:

```java
@OnEnterState
public void onEnter<Estado>(<Entidad> expediente, EventContext eventContext)
```

- Anotado **`@OnEnterState`**, con **UN** parámetro de entidad y `EventContext`, retorno `void`.
- **CRITICAL — el nombre lleva el estado SIN la fase**: la clase ya vive en el paquete de su fase.
- **MUST NOT** sobrar ningún `@OnEnterState` que no corresponda a un estado de la propia fase.
- El cuerpo es **normalmente vacío**. La sección **MUST** listarlos todos y marcar cada uno como `vacío` o, si no lo está, con su lista numerada de acciones (mismo vocabulario que §11.2, salvo `UPDATE_STATE`).
- **Quién atiende el `onEnter` es el manager de la fase del estado al que se LLEGA**, que puede ser otra distinta de la del evento.

### 11.4 Lista de cobertura de la fase

Al final de cada `### Fase <FASE>`, una lista explícita para que el verificador la cuadre:

- Eventos de la fase (unión de los `events` de sus estados), y su `trigger*` correspondiente.
- Estados de la fase, y su `onEnter*` correspondiente.
- **MUST NOT** factorizarse ningún `trigger*`/`onEnter*` en una superclase compartida entre fases o versiones: el dispatcher usa `getDeclaredMethods()`, así que un método heredado **no cuenta** ni en runtime ni en los tests. Si hay lógica común, se declara el método en cada fase delegando en un helper o servicio.

---

## 12. Sección «Especificación de los StateEventValidatorImpl»

Una subsección `### Fase <FASE>` **por cada fase**:

```kotlin
package <basePackageName>.<fase>

import com.educaflow.subsystem.expedientes.db.<Entidad> as model

class StateEventValidatorImpl : StateEventValidator {

    @BeanValidationRulesForStateAndEvent
    fun getForState<Estado>InEvent<Evento>(): BeanValidationRules {
        return rules {
            field(model::get<Campo>) {
                +Required()
            }
        }
    }
}
```

### 12.1 Cobertura

**MUST** haber **exactamente un** método por cada **pareja (estado, evento)** declarada en la fase, **salvo las del evento `DELETE`**:

- Nombre: `getForState<Estado>InEvent<Evento>` (estado **sin** la fase).
- Anotado **`@BeanValidationRulesForStateAndEvent`**, con **cero parámetros**, devolviendo `BeanValidationRules`.
- **CRITICAL — se recorre estado a estado**: un mismo evento declarado en tres estados son **tres métodos distintos**, mientras que en el `PhaseEventManagerImpl` es **un único** trigger. Es la asimetría clave; olvidarla revienta en runtime con `"No se ha encontrado el método: …"`.
- **MUST NOT** sobrar ningún método anotado cuya pareja no esté declarada en la propia fase.
- `getForState<Estado>InEventDelete` **MUST NOT** escribirse: el `Tramitador` se salta la validación para `DELETE` y nunca lo busca.
- **MUST NOT** devolverse `null`: el `Tramitador` lanza. Un evento sin validaciones **MUST** declararse explícitamente como `rules { }` vacío.

**MUST** incluirse, por fase, una **tabla de cobertura**:

| estado | evento | método | ¿reglas? |
|---|---|---|---|
| `<ESTADO>` | `<EVENTO>` | `getForState<Estado>InEvent<Evento>` | sí / `rules { }` vacío |
| `<ESTADO>` | `DELETE` | **ninguno** (exento) | — |

### 12.2 Contenido de cada método

Por cada método, la lista **ordenada** de `field(...)` y, dentro de cada uno, las reglas del DSL **con sus argumentos literales**, escritas con la sintaxis real:

```kotlin
field(model::get<Campo>) {
    +Required()
    +Pattern("<regex literal>")
}
field(model::get<Campo>) {
    +ifValueIn(model::get<CampoDiscriminador>, listOf(<Enum><Entidad>.<VALOR>, <Enum><Entidad>.<VALOR>)) {
        +Required()
        +GreaterThan(model::get<OtroCampo>)
    }
}
```

Catálogo de reglas disponibles (paquete `com.educaflow.base.infrastructure.validation.rules`), con la forma de sus argumentos:

| Familia | Reglas |
|---|---|
| Obligatoriedad | `Required()` |
| Texto | `Pattern("<regex>")`, `MinLength(<n>)`, `MaxLength(<n>)`, `NoAllUpperCase()` |
| Numéricas | `MinValue(<expr>)`, `MaxValue(<expr>)` |
| Comparables | `GreaterThan(model::get<Campo>)` y equivalentes |
| Fecha/hora, listas, edad | las de `DateTimeRules`, `ListRules`, `AgeRules` |
| Ficheros | `FileType(listOf("<mime>", …))`, `FileMaxSize(<n>, SizeUnit.<UNIDAD>)` |
| PDF / firma | `FirmaPdf(model::get<CampoOriginal>, model::getDniFirmaDocumentoEntrada)` |
| Condicional | `ifValueIn(model::get<Campo>, listOf(<VALORES>)) { … }` |

- Los argumentos **MUST** ir literales, resueltos: nada de `«el rango del año»`, sino la expresión exacta.
- El diseño **MUST** declarar los `import` de los enums propios que aparezcan en un `ifValueIn`.

### 12.3 Frontera de confianza (CRITICAL)

**El validador no es solo un validador: es la lista de campos que el cliente puede dictar en ese evento.**

- **MUST NOT** aparecer en ningún `field(...)` un campo clasificado como `servidor` en §6.1 — ni `codePhase`, `codeState`, `abierto`, `centro`, `usuarioRegistrador`, ni ningún `MetaFile` que genere un `trigger*`, ni ningún resguardo de registro, ni ningún campo calculado. Darles entrada los hace **escribibles por el cliente**.
- Un campo `usuario` que **no** aparezca en la pareja (estado, evento) donde el formulario lo pide **no se copiará**: el usuario escribirá y el valor se perderá en silencio. Cada campo editable en la vista de un estado **MUST** aparecer en el `field(...)` del evento que se dispara desde esa vista.
- **MUST NOT** confiarse en `readonly`/`showIf`/`hidden` de la vista.
- **CRITICAL** — esta puerta **NO** protege el endpoint REST automático `POST /ws/rest/<FQN>`, que Axelor publica para toda entidad y **no pasa por el `Tramitador` en ningún punto**. Son **dos puertas distintas**. **MUST NOT** darse por protegida una entidad de expediente porque su tramitación valide por evento, y **MUST NOT** introducirse un `ModelService` deny-all de expedientes como parche puntual: se retiró a propósito.
- **MUST NOT** ponerse lógica de negocio (transiciones, generación de PDF) en el validador.

### 12.4 Firma en cliente — la pieza del validador

Si un documento se firma en cliente (AutoFirma), el par de campos `MetaFile` (`<campoOrigen>`, `<campoDestino>`) se valida **en el campo destino**:

```kotlin
field(model::get<CampoDestino>) {
    +Required()
    +FirmaPdf(model::get<CampoOrigen>, model::getDniFirmaDocumentoEntrada)
}
```

`FirmaPdf` comprueba en el servidor: exactamente una firma nueva, certificado confiable, que no es sello de tiempo, texto plano idéntico al original y DNI del certificado coincidente. Las otras dos piezas del patrón son el par de campos del `domains.xml` (sección «Modelo») y el botón de la vista (`vistas.md`).

---

## 13. Sección «Reparto de reglas»

**MUST** declarar en qué capa vive cada tipo de regla y **MUST** incluir esta tabla:

| Tipo de regla | Capa | Cómo se escribe |
|---|---|---|
| Tipo, longitud máxima de columna, referencia, enumerado | **modelo XML** (`domains.xml`) | atributos de `<string>`/`<integer>`/`<enum>`/`<many-to-one>` |
| Obligatoriedad de un campo **en un evento** | **DSL del validador** | `+Required()` en la pareja (estado, evento) |
| Formato, rango, longitud, comparación entre campos, tipo/tamaño de fichero, firma | **DSL del validador** | `Pattern`, `MinValue`/`MaxValue`, `MinLength`/`MaxLength`, `GreaterThan`, `FileType`/`FileMaxSize`, `FirmaPdf` |
| Obligatoriedad **condicional** (depende del valor de otro campo) | **DSL del validador** | `ifValueIn(...) { +Required() }` |
| Qué campos puede dictar el cliente en un evento | **DSL del validador** | el conjunto de `field(...)` de esa pareja (§12.3) |
| Efectos: generar PDF, firmar, registrar, transicionar, limpiar, calcular | **`trigger*`** del `PhaseEventManagerImpl` | lista de acciones de §11.2 |
| Inicialización del expediente | **`triggerInitialEvent`** | §10 |
| Mostrar/ocultar/deshabilitar, ayudas, confirmaciones | **vista** | `showIf`/`hideIf`/`readonly`, `<help>`, `prompt` — **solo UX, NUNCA defensa** |

Reglas duras:

- **MUST NOT** usarse `required="true"` en el `domains.xml` de un tipo de expediente (§6.3).
- **MUST NOT** validarse datos de usuario en un `trigger*`.
- **MUST NOT** ponerse lógica de negocio en el validador.
- **MUST NOT** escribirse la inicialización del expediente en un `PhaseEventManagerImpl`.
- **MUST NOT** tratarse una regla de vista como si fuera una validación: si la regla debe impedir algo, su sitio es el validador.

La sección **MUST** listar, además, cada regla funcional de la especificación con la capa a la que se lleva, para que se vea que ninguna se ha quedado sin ubicar. Una regla que el diseño descarta **MUST** aparecer con su justificación.

---

## 14. Sección «Asignación de perfiles»

### 14.1 Tabla

| perfil | actor | tipo de actor | vía | bloque de `permisos-demo.xml` |
|---|---|---|---|---|
| `<PERFIL>` | `<CODE>` | `TipoUsuario` \| `Cargo` \| `CentroUsuario` | `tramiteCode="<Code>"` \| `tipoExpedienteCode="<Entidad>"` | `<asignacionesTipoUsuario>` \| `<asignacionesTipoUsuarioTipoExpediente>` \| `<asignacionesCargoTipoExpediente>` \| `<asignacionesCentroUsuario>` |

### 14.2 Reglas

- **REQUIRED — el perfil del estado INICIAL MUST asignarse por `tramiteCode`.** En la creación todavía no hay expediente, y el `Tramitador` contrasta el perfil contra los `Ace` **sobre el trámite**. Por `tipoExpedienteCode` no se podría crear nada.
- Los perfiles de los estados **posteriores** pueden asignarse por `tramiteCode` o por `tipoExpedienteCode`. **SHOULD** preferirse `tramiteCode`: las asignaciones por `tipoExpedienteCode` hay que duplicarlas en cada versión nueva del trámite.
- El `tipoExpedienteCode` es `<Entidad>` (`<Code><VN>`), **no** el code del trámite.
- **MUST** quedar asignado a alguien **todo** perfil que use algún estado del tipo: un perfil sin actor deja ese estado inalcanzable.
- Un `<perfil name="…">` **MUST** existir en `<perfiles>` antes de referenciarse; pero **MUST NOT** duplicarse uno que ya esté declarado en el `permisos-demo.xml` real.
- `auth-expedientes.xml` ya concede lectura sobre `Expediente`/`Tramite`/`TipoExpediente` **condicionada por `Ace`**: sin fila `Ace` el usuario no ve nada aunque el trámite exista.
- Sobre añadir una `<permission name="<Entidad>.all">` a `auth-expedientes.xml`: **no está verificado** que haga falta, y el diseño **MUST NOT** añadirla por defecto. Si la especificación la exige, **MUST NOT** copiarse el patrón `create/read/write/remove` **sin `condition`**: es un agujero conocido documentado en `CLAUDE.md`.

### 14.3 `design/permisos.xml` — el fragmento

**MUST** existir. Contiene **solo lo que se añade**, con la raíz `<datos>` y únicamente los bloques que llevan contenido nuevo:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<datos>
    <!-- Solo los perfiles que NO existan ya en permisos-demo.xml -->
    <perfiles>
        <perfil name="<PERFIL>"/>
    </perfiles>

    <asignacionesTipoUsuario>
        <asignacion tipoUsuarioCode="<TIPO_USUARIO>" perfilName="<PERFIL>" tramiteCode="<Code>"/>
    </asignacionesTipoUsuario>

    <asignacionesCargoTipoExpediente>
        <asignacion cargoCode="<CARGO>" perfilName="<PERFIL>" tipoExpedienteCode="<Entidad>"/>
    </asignacionesCargoTipoExpediente>
</datos>
```

- **MUST NOT** ser una copia del `permisos-demo.xml` completo: es un **fragmento a fusionar**, no un fichero que sobrescriba.
- **MUST NOT** contener asignaciones a otros trámites ni a otras versiones.
- El diseñador **MUST** leer el `permisos-demo.xml` real para saber qué `<perfil>` ya existen y no duplicarlos.

---

## 15. Sección «Tests»

### 15.1 `design/test-e2e-desc.md` — lo escribe el diseñador

**MUST** existir. Contiene los tests E2E en **lenguaje de negocio**, Given/When/Then, sin código ni selectores.

- **MUST** haber al menos un test por **cada transición** de la tabla de §5.2 (arranque y `DELETE` incluidos).
- Cada test **MUST** nombrar: el **perfil** con el que actúa el usuario, la **fase** y el **estado** de partida, el **evento** que dispara (el `title` del botón), los datos que introduce y el **estado** al que debe llegar.
- **MUST** mencionarse **todos** los estados del tipo, incluidos los `closed` (al menos como estado de llegada y con la comprobación de que su vista genérica se abre en solo lectura).
- **MUST** haber al menos un test de **validación fallida** por cada pareja (estado, evento) que tenga reglas: el usuario deja un campo obligatorio vacío y el sistema no transiciona.
- **MUST NOT** contener código Playwright ni selectores.

### 15.2 `design/test-unit-desc.md` — lo escribe el rol `test-unitarios`

**CRITICAL — los invariantes de forma de un tipo de expediente ya los cubren tests genéricos existentes, escritos A MANO, en `src/test/java/com/educaflow/tiposexpedientes/`, que recorren automáticamente TODOS los tipos del árbol.**

Por tanto, para esta plantilla:

- **MUST NOT** describirse ni proponerse ningún test nuevo bajo `src/test/java/com/educaflow/tiposexpedientes/`.
- **MUST NOT** proponerse crear un `agent_docs/*-rules.md` ni un skill generador para esos tests.
- **MUST NOT** proponerse regenerarlos con `/developer-create-arch-tests` ni `/developer-create-view-tests`. Las vistas de tipos de expediente están **excluidas** de `agent_docs/view-rules.md` y de los tests de `com.educaflow.views`.
- **MUST NOT** proponerse tests unitarios de los `PhaseEventManagerImpl`/`StateEventValidatorImpl`/`InitialEventManagerImpl`: sus dependencias son el `Tramitador`, la BD y la generación de PDF; su verificación real es `./run.sh` más el recorrido en runtime.

El contenido de `test-unit-desc.md` **MUST** ser, entonces, la **declaración de cobertura**: qué comprueba de este tipo cada familia de tests ya existente (la correspondencia de fases/estados/eventos con `PhaseEventManagerImpl`, `StateEventValidatorImpl` y los `views.xml`, y la del `.puml` con el XML maestro), y la constatación explícita de que **no se añade ningún test nuevo**, con el motivo. Si la especificación exige lógica de negocio pura y aislable que **no** viva en esas tres clases, sí se describe su test unitario.

La sección `## 13. Tests` del `design.md` **MUST** referenciar ambos ficheros.

---

## 16. Sección «Notas y supuestos»

Decisiones tomadas ante ambigüedades de la especificación, para que el juez y el verificador las vean. Cada supuesto, en una línea, con qué se asumió y por qué.

---

## 17. Sección «Checklist del diseñador»

El diseñador la aplica y **corrige antes de terminar**. **MUST NOT** dar el diseño por terminado con algún punto sin cumplir. **LIMIT: 5 pasadas de autocorrección**; si al cabo de 5 sigue habiendo puntos sin cumplir, el diseñador **MUST** dejarlos anotados en «Notas y supuestos» en vez de seguir iterando.

La sección `## 15. Checklist del diseñador` del `design.md` **MUST** reproducir esta lista con sus checkbox marcados.

**Estructura y materialización**

- [ ] ¿Existen todos los ficheros de §1 y **ninguno más**? ¿Ni un `.java`, ni un `.kt`, ni un `i18n_*.csv`, ni un `estados.png`, ni un `.pdf` dentro de `design/`?
- [ ] ¿Hay un `design/fases/<fase>/views.xml` por **cada** fase declarada, con la fase en minúsculas?
- [ ] ¿Hay un `design/documentospdf/<doc>.xml` por cada documento y un `_<fragmento>.xml` por cada fragmento, y ninguno de más?
- [ ] ¿Todos los XML materializados están **completos**, sin `TODO`, sin `...`, sin `<button name="">`?
- [ ] ¿El `design.md` tiene el frontmatter `type: design` y las 15 secciones de §2, con esos títulos y en ese orden?

**Máquina de estados**

- [ ] ¿Exactamente un estado `initial` en todo el tipo?
- [ ] ¿Todo `<state>` lleva escrito su `events`, aunque sea `events=""`?
- [ ] ¿`EXIT` **no** aparece en ningún `events`?
- [ ] ¿Todo `profile` de un estado es un valor del enum `Profile`?
- [ ] ¿Ningún nombre de estado ni de evento produce un método que pise la API base (`STATE`, `INITIAL_EVENT`…)?
- [ ] ¿La tabla de transiciones tiene una fila por cada pareja (estado, evento) declarada, más el arranque, más las de `DELETE` hacia `[*]`?
- [ ] ¿Todo evento ramificado declara sus guardas y su `default`?

**Diagrama**

- [ ] ¿El `estados.puml` dibuja **todos** los estados, cada uno con alias `<FASE>_<ESTADO>`, agrupados por fase como estado compuesto?
- [ ] ¿No hay ningún alias que no corresponda a un estado declarado?
- [ ] ¿Cada transición del `.puml` coincide con una fila de la tabla de transiciones, guarda incluida? ¿Y cada `closed` está anotado?

**Modelo**

- [ ] ¿La entidad es la **primera** `<entity>` del `domains.xml`, con `extends="Expediente"` y `name="<Entidad>"`?
- [ ] ¿El `<module>` es `expedientes` / `com.educaflow.subsystem.expedientes.db`?
- [ ] ¿Ningún campo heredado de `Expediente` está redeclarado?
- [ ] ¿Todo enum propio lleva `<Entidad>` como sufijo?
- [ ] ¿Ningún campo lleva `required="true"`?
- [ ] ¿Todo campo de la tabla de §6.1 tiene su columna «quién lo rellena» resuelta a `usuario` o `servidor`?
- [ ] ¿Todo campo `servidor` está asignado por alguna acción de las secciones «Especificación del InitialEventManagerImpl» o «Especificación de los PhaseEventManagerImpl» del `design.md`?
- [ ] ¿El `<extra-code-model>` existe si y solo si hay documentos PDF, con una constante por documento y su ruta de recurso correcta?

**Clases**

- [ ] ¿El `InitialEventManagerImpl` está en la **raíz** de la versión, parametrizado con la entidad, con exactamente un `triggerInitialEvent`, y sin reasignar lo que ya rellena el `Tramitador`?
- [ ] Si hay firma en cliente, ¿el `triggerInitialEvent` deja `dniFirmaDocumentoEntrada` con un DNI válido?
- [ ] Si hay `createRegistroEntrada`, ¿deja `personaSolicitante` y `personaInteresada` no nulos?
- [ ] Por cada fase: ¿un `trigger<Evento>` por cada evento de la **unión** de los `events` de sus estados, y ninguno de más?
- [ ] Por cada fase: ¿un `onEnter<Estado>` por **cada** estado, incluidos los sin eventos y los `closed`, y ninguno de más?
- [ ] ¿Ningún `PhaseEventManagerImpl` declara `triggerInitialEvent` ni `triggerExit`?
- [ ] ¿Cada `trigger*` tiene su lista **numerada** de acciones, y el `UPDATE_STATE` de cada una coincide con la tabla de transiciones?
- [ ] ¿`triggerDelete` (si existe) **no** llama a `UPDATE_STATE`?
- [ ] ¿Cada fase declara sus `@Inject`, sus constantes y el `import` de `States` de **su propia** versión?
- [ ] ¿Ningún `trigger*`/`onEnter*` se factoriza en una superclase compartida?

**Validador**

- [ ] Por cada fase: ¿un `getForState<Estado>InEvent<Evento>` por **cada pareja** (estado, evento) **salvo `DELETE`**, y ninguno de más?
- [ ] ¿Cada método tiene sus `field(...)` con las reglas y **argumentos literales**, o un `rules { }` vacío declarado explícitamente?
- [ ] ¿Ningún `field(...)` menciona un campo `servidor`?
- [ ] ¿Todo campo editable en la vista de un estado aparece en el `field(...)` del evento que se dispara desde esa vista?
- [ ] ¿Los enums usados en `ifValueIn` tienen su `import` declarado?

**Vistas**

- [ ] ¿El diseño pasa el **checklist de vistas** de `vistas.md` §8?

**Permisos y pasos**

- [ ] ¿El perfil del estado inicial se asigna por `tramiteCode`?
- [ ] ¿Todo perfil usado por algún estado tiene actor?
- [ ] ¿`design/permisos.xml` es un fragmento con solo lo nuevo, sin duplicar `<perfil>` preexistentes?
- [ ] ¿La tabla «Ficheros a crear o modificar» lista **todos** los ficheros reales, con `permisos-demo.xml` como `Modificar`, y ninguno generado?
- [ ] ¿Los pasos siguen el orden obligatorio de §9, con el paso de **`CreateFilesTask` en la posición 3** y su comando exacto?
- [ ] ¿Cada paso de un XML dice «cópialo literalmente» con origen y destino, y cada paso de un `.java`/`.kt` apunta a su sección de especificación sin duplicarla?
- [ ] ¿El paso final lleva `./run.sh` y la comprobación en runtime de los agujeros que el build no ve?

**Tests**

- [ ] ¿`design/test-e2e-desc.md` existe, menciona **cada estado** y **cada transición**, y no lleva código ni selectores?
