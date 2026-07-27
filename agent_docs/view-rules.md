# Reglas de vistas — secretaría virtual

Catálogo de las **convenciones verificables de las vistas Axelor** del proyecto (los XML bajo `**/views/*.xml`), en formato ADR (*Decisión / Verificación*).
Este fichero **NO contiene código**: describe **qué** debe verificarse; el código lo escriben a mano los tests JUnit 5 planos de `src/test/java/com/educaflow/views` (una clase por categoría), que leen cada XML con JAXP (DOM + XPath) y comprueban cada regla.
Es el equivalente para vistas de [`architecture-rules.md`](architecture-rules.md), que hace lo propio para el código Java con ArchUnit — con una diferencia técnica: **ArchUnit analiza bytecode y no sirve para XML**, así que las vistas se verifican con tests JUnit normales, no con `@ArchTest`.

> **Mantener coherencia con los skills `k-vistas`.** Este fichero **cataloga como reglas verificables** las convenciones que los skills `k-vistas` (`SKILL.md`, `forms.md`, `grids.md`, `actions.md`, `menus.md`) y el verificador de `/sdd-designer` (`sdd-designer/template-system/{design-contract,vistas,validacion}.md`) describen en prosa. Si cambias una regla aquí, **MUST** comprobar si hay que actualizar los skills, y viceversa. No deben divergir.

## Fuente de verdad

Las reglas se derivan de las **convenciones documentadas** en los skills `k-vistas`, NO de lo que los XML hacen hoy (los XML pueden tener bugs; codificar "lo que el XML hace" produciría reglas erróneas).
**CRITICAL — un XML puede incumplir una regla correcta.**
Si un fichero real viola una regla, es un **bug del XML**, NO una excepción a la regla.

## Convenciones de verificación (aplican a todas las reglas)

- **Identificador de regla:** `VAR-<categoría>.<n>` (**V**iew **A**rchitecture **R**ules).
  La numeración `<n>` **reinicia en cada categoría**: una regla nueva se apila al final de la suya sin renumerar el resto.
  Las referencias cruzadas usan el ID completo (p.ej. `VAR-2.6`).
- **Estructura de cada regla** — dos campos con roles que **NO se solapan**:
  - **`Decisión`** = **solo el motivo** (el *por qué*; empieza por «Para…» / «Porque…»).
    **NO** reescribe la norma en prosa.
    Si el motivo es una convención arbitraria que no se puede deducir, se marca `(motivo pendiente)`.
  - **`Verificación`** = la norma **testeable y autónoma**: **Sujeto** (qué nodos selecciona el test) + **Condición** [+ **Exenciones** si aplica].
    **Todo** detalle técnico (atributos, valores literales, patrones de nombre) vive **aquí**, nunca en la `Decisión`.
- **Ejemplos:** cada regla incluye un **Correcto** ✅ y un **Incorrecto** ❌, como fragmentos mínimos (solo lo relevante para la regla).
- **Ámbito de análisis:** los ficheros `src/main/java/com/educaflow/system/<x>/views/*.xml`, `src/main/java/com/educaflow/subsystem/<x>/views/*.xml` y `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml`.

- **Paquetes exentos:** las reglas aplican a **todos los sistemas y subsistemas EXCEPTO `gestioncentro`, `expedientes`, `tiposexpedientes` y `tramites`** (framework propio de expediente/tramitación), que quedan **fuera del sujeto** de todas las reglas.

## Glosario de términos

Vocabulario común para leer las reglas, del fichero a la PI:

- **Fichero de vista**
  XML bajo `**/views/*.xml`, raíz `object-views`.
  Agrupa las vistas (`grid`/`form`/`tree`/`chart`/etc.), el `action-view` y las acciones de **una** variante de **una** entidad y, si aplica, de sus detalles.
  Un fichero puede contener **varios bloques** (el del maestro y el de cada detalle).

- **`name` = `{contexto}-[{descripcion}-]{tipo}`.**
  Cada `name` de vista o acción se compone de tres partes: el **contexto** (qué bloque/pantalla), una **descripción** opcional (qué hace) y el **tipo**.

  - **`contexto`** = `{marcadorCapa}{Módulo}.{Variante}@{Entidad}[.{EntidadDetalle}…]`.
  Es la clave que **identifica el bloque/pantalla**: todos los `name` con el mismo contexto forman un bloque (ver «Bloque» más abajo). Sus partes:
    - **marcador de capa**: `sys` (sistema) o `subsys` (subsistema) pegado al nombre del módulo/carpeta → `subsysCorreos`, `subsysFirma`, `sysActas`.
    - **`Variante`** (entre el marcador de módulo y el `@`, p.ej. `Main`, `Mis`, `Centro`, `Pendiente`, `Ref`).
    Dice **para qué sirve el bloque** y así distingue **las variantes de la misma entidad**: `Correo` tiene las variantes `Main` (administración), `Mis` (mis correos) y `Centro`; `TareaFirma` tiene `Pendiente`, `Firmado`, `Rechazado`, `Todos`.
    Cada variante vive en su propio fichero. Variante especial `Ref` (grid + form) → vistas de referencia.
    - **ruta de entidad** (tras el `@`) separada por puntos, de **maestro a detalle**: `Correo`, `Correo.Adjunto`, `TareaFirma.DocumentoFirma`. El nº de segmentos = **nivel/profundidad** maestro-detalle (`Correo` = 1, `Correo.Adjunto` = 2).

  - **`descripcion`** (opcional, entre el primer `-` y el `tipo`).
  Explica qué hace la vista o acción, p.ej. `btnSave`, `set-ciclo-parent`, `Remote-reenviar`, `Local-validateSave`. Las vistas puras no la llevan (`Main@…-grid`, `Main@…-form`); es propia sobre todo de las acciones.

  - **`tipo`** (último segmento).
  Tipo de vista o acción: `grid`, `form`, `tree`, `chart`, `action`.

**Ejemplos de descomposición** (vista pura, detalle y acción con descripción):

| `name`                                                     | contexto                            | marcadorCapa | Módulo          | Variante | Entidad  | EntidadDetalle | descripcion          | tipo     |
|------------------------------------------------------------|-------------------------------------|--------------|-----------------|----------|----------|----------------|----------------------|----------|
| `subsysCorreos.Main@Correo-grid`                           | `subsysCorreos.Main@Correo`         | `subsys`     | `Correos`       | `Main`   | `Correo` | —              | —                    | `grid`   |
| `subsysCorreos.Main@Correo.Adjunto-form`                   | `subsysCorreos.Main@Correo.Adjunto` | `subsys`     | `Correos`       | `Main`   | `Correo` | `Adjunto`      | —                    | `form`   |
| `subsysCorreos.Main@Correo-btnSave-action`                 | `subsysCorreos.Main@Correo`         | `subsys`     | `Correos`       | `Main`   | `Correo` | —              | `btnSave`            | `action` |
| `sysGestionCentro.Cargos@Centro-form`                      | `sysGestionCentro.Cargos@Centro`    | `sys`        | `GestionCentro` | `Cargos` | `Centro` | —              | —                    | `form`   |
| `sysGestionCentro.Cargos@Centro-set-cargo-director-action` | `sysGestionCentro.Cargos@Centro`    | `sys`        | `GestionCentro` | `Cargos` | `Centro` | —              | `set-cargo-director` | `action` |

- **Bloque** (informalmente **«pantalla»**): todos los elementos cuyo `name` comparte el mismo `contexto`.
  Con las PI (Categoría 3) los bloques quedan además **delimitados físicamente**: cada bloque empieza en su `<?sv-view?>` y acaba en el siguiente `<?sv-view?>` (o el fin del fichero).

- **Clase de bloque.** La clase de un bloque (y de su form) se deduce **mecánicamente** de su contexto — ninguna regla necesita "adivinarla":

  | Clase | Cómo se reconoce | Ejemplo |
  |---|---|---|
  | **maestro** | Variante ≠ `Ref` y ruta de entidad de 1 segmento | `Main@Correo`, `Pendiente@TareaFirma` |
  | **detalle** (modal) | ruta de entidad de ≥2 segmentos | `Main@Correo.Adjunto` |
  | **referencia** | Variante `Ref` | `Ref@Adjunto` |

  Rasgos funcionales de cada clase (el detalle normativo lo fijan las Categorías 5 a 7):
  - **maestro**: se abre desde el menú por su `action-view` y guarda/borra **contra el servidor**.
  - **detalle** (también llamado **hijo** o **modal**): se abre en modal desde el `<panel-related>` de su maestro y opera sobre la colección **en memoria** del padre (el walker valida los detalles en cascada al guardar el maestro).
    **Detalle de solo lectura**: form de detalle sin ningún `<button>` (p.ej. `Pendiente@TareaFirma.DocumentoFirma`); queda exento de las reglas de botones y de `onNew`.
  - **referencia** (`Ref-{Entidad}.xml`, `Ref@…-grid` / `Ref@…-form`): grid/form de **solo lectura** de una entidad relacionada, abiertos **embebidos** desde un `<field>` relacional de otro form; sin `action-view` ni entrada de menú, y su único botón es «Salir».

- **Elemento de alto nivel vs acción.**
  Alto nivel = `action-view`/`grid`/`form`/`chart`/`tree` (lo que ve el usuario).
  Acción = `action-group`/`action-record`/`action-attrs`/`action-method`/`action-script`/`action-validate`/`action-condition`.

- **Rol de una acción.**
  El **marcador** con que empieza la `descripcion` declara el rol de toda acción (salvo el `action-view`, que no lleva marcador),
    y ese rol fija a la vez qué elementos XML puede usar y bajo qué PI de sección vive.
  Es la **única** tabla rol↔marcador↔elemento↔sección: la nomenclatura (`VAR-2.2`), las secciones (`VAR-3.3`) y el vocabulario de PI (Categoría 3) se definen contra ella.

  | Rol | La `descripcion` empieza por | Elementos admitidos | Sección (PI) |
  |---|---|---|---|
  | **principal** (botón/evento) | `btn{X}` u `on{Evento}` | `action-group` | `<?sv-primary-actions?>` |
  | **validación local** | `Local-` | `action-validate` / `action-condition` / `action-group` (combinador de validaciones) | `<?sv-validations?>` |
  | **regla de campo** | `set-{campo}-{valor}` (campo) o `set-{campo}.{atributo}-{valor}` (atributo) | `action-record` (campo) / `action-attrs` (atributo) | `<?sv-rules?>` |
  | **remota** | `Remote-` | `action-method` / `action-script` | `<?sv-remotes?>` |

- **Acciones globales/predefinidas.**
  Acciones sin `@` (sin contexto), compartidas por todas las vistas: `save`, `back`, `force-back`, `delete`, `close`, `save-modal`, `delete-modal`, `new`, `validate`, `remote-validationSave-action`, `remote-validationDelete-action`.

- **PI o «Processing Instruction»**: instrucciones dentro del XML que no forman parte de los datos de él. Tienen el formato `<?target data?>`.
  A diferencia de un comentario, una PI **es un nodo del DOM**, así que se localiza y valida con el mismo JAXP+XPath (`processing-instruction('sv-view')`, …) que el resto de reglas.

---

# Categoría 1 — Fichero y ubicación

## VAR-1.1 — Cabecera de fichero de vistas
**Decisión.**
  Para que Axelor reconozca y valide el fichero como vistas:
    el `xmlns`/`schemaLocation` fijan el esquema (versión 8.1) contra el que se valida;
    sin ellos el fichero no se procesa como vista.
**Verificación.**
  Sujeto: cada fichero `views/*.xml` no exento.
  Condición: el elemento raíz es `object-views`, con `xmlns="http://axelor.com/xml/ns/object-views"` y `xsi:schemaLocation` a `object-views_8.1.xsd`.

**Correcto** ✅
```xml
<object-views xmlns="http://axelor.com/xml/ns/object-views"
              xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xsi:schemaLocation="http://axelor.com/xml/ns/object-views
                https://axelor.com/xml/ns/object-views/object-views_8.1.xsd">
```
**Incorrecto** ❌
```xml
<object-views>                    <!-- sin xmlns ni schemaLocation -->
<views>                           <!-- raíz equivocada -->
```

## VAR-1.2 — Nombre de fichero = `{Variante}-{Entidad}.xml`
**Decisión.**
  Para saber qué contiene el fichero de una vista (qué variante y qué entidad) sin abrirlo,
  y para que la regla `VAR-2.1` pueda derivar el contexto de los `name` desde el nombre del fichero.
**Verificación.**
  Sujeto: cada fichero `views/*.xml` no exento.
  Condición:
    (a) el nombre del fichero es `{Variante}-{Entidad}.xml` — dos palabras en formato upper camel case separadas por un guion, sin espacios ni otros caracteres;
    (b) la `{Entidad}` existe como entidad en el XML del modelo del módulo (`../domains`).
  Exenciones:
    los ficheros de **overrides de vistas del framework Axelor** — aquellos cuyos elementos de alto nivel llevan todos un `name` sin `@` (p.ej. `user-preferences-form-view.xml`) — no forman bloques (`VAR-3.1`) y no siguen el patrón;
    y los ficheros cuyas vistas apuntan todas a **modelos del framework** (ningún `model` empieza por `com.educaflow`) quedan exentos de (b): su `{Entidad}` es el nombre en español de la entidad del framework (`Usuario`→`User`, `Anexos`→`MetaFile`), coherente con la exención de `VAR-2.3`.

**Correcto** ✅ — `Main-Correo.xml`, `Mis-Correo.xml`, `Pendiente-DocumentoFirma.xml`, `Ref-FamiliaProfesional.xml`
**Incorrecto** ❌ — `CicloForm.xml`, `correo-admin.xml`, `vistasCiclo.xml`, `CORREO-admin.xml`, `vistas-Ciclo.xml`

## VAR-1.3 — Todos los `<menuitem>` viven solo en `menus.xml`
**Decisión.**
  Para que todo el árbol de menús viva en un único sitio y no quede disperso por las vistas.
**Verificación.**
  Sujeto: cada fichero del ámbito de análisis.
  Condición: ningún fichero de `views/` contiene un `<menuitem>`;
    el único fichero con `<menuitem>` es `secretariavirtual/menus/menus.xml`.

**Correcto** ✅ — `views/Main-Ciclo.xml` sin `<menuitem>`; el menú se declara en `menus.xml`.
**Incorrecto** ❌
```xml
<!-- dentro de views/Main-Ciclo.xml -->
<menuitem name="sistemaEducativo-ciclos-menuitem" title="Ciclos" action="Main@…-action"/>
```

---

# Categoría 2 — Nomenclatura

## VAR-2.1 — El contexto de cada `name` se deriva de la ubicación
**Decisión.**
  Para que los nombres sean únicos, trazables y predecibles:
    la carpeta fija el marcador de capa y el módulo, el nombre del fichero fija la variante y la entidad,
    así que cualquier vista o acción se localiza (y se genera) sin ambigüedad y los copy-paste con contexto equivocado se detectan solos.
**Verificación.**
  Sujeto: el `name` de cada hijo directo de `<object-views>` de un fichero `{Variante}-{Entidad}.xml` bajo `system/<x>/views/` o `subsystem/<x>/views/`.
  Condición: el `name` empieza por `{marcadorCapa}{Módulo}.{Variante}@{Entidad}`, donde —
    `marcadorCapa` = `subsys` si el fichero está bajo `subsystem/`, `sys` si está bajo `system/`;
    `Módulo` = el nombre de la carpeta `<x>` (comparación case-insensitive);
    `Variante` y `Entidad` = las dos palabras del nombre del fichero;
    y tras ese prefijo sigue `-` (elemento del bloque maestro) o `.` (ruta de detalle `.{EntidadDetalle}[…]` y después `-`).
  Exenciones: acciones globales/predefinidas sin `@` (`VAR-2.5`) y nombres Axelor adaptados (p.ej. `user-preferences-form`).
    (Los nombres `exp-` del framework de expedientes no necesitan exención: viven en paquetes exentos, fuera del sujeto.)

**Correcto** ✅ — en `subsystem/correos/views/Main-Correo.xml`, todos los `name` empiezan por `subsysCorreos.Main@Correo`:
```xml
<grid         name="subsysCorreos.Main@Correo-grid" .../>
<form         name="subsysCorreos.Main@Correo.Adjunto-form" .../>   <!-- detalle: extiende con .Adjunto -->
<action-group name="subsysCorreos.Main@Correo-btnSave-action" .../>
```
**Incorrecto** ❌ — en ese mismo `subsystem/correos/views/Main-Correo.xml`:
```xml
<grid name="correos.Main@Correo-grid" .../>              <!-- falta el marcadorCapa `subsys` -->
<grid name="sysCorreos.Main@Correo-grid" .../>           <!-- marcadorCapa `sys` en un subsistema -->
<form name="subsysNotificaciones.Main@Correo-form" .../> <!-- Módulo no casa con la carpeta `correos` -->
<form name="subsysCorreos.Mis@Correo-form" .../>         <!-- Variante `Mis` no casa con el fichero `Main-…` -->
<grid name="subsysCorreos.Main@Mensaje-grid" .../>       <!-- Entidad no casa con el fichero `…-Correo` -->
```

## VAR-2.2 — El `name` dice el tipo y el rol del elemento
**Decisión.**
  Para identificar por el propio nombre qué es cada cosa y qué hace sin abrir el elemento,
  y para que la regla de secciones (`VAR-3.3`) pueda clasificar las acciones por **rol** usando solo el nombre.
**Verificación.**
  Sujeto: cada elemento de alto nivel o acción con `name` que contenga `@`.
  Condición:
    (a) el último segmento (`tipo`) casa con el elemento: `grid`/`form`/`tree`/`chart` llevan su homónimo (`-grid`, `-form`, `-tree`, `-chart`); el `action-view` y toda acción llevan `-action`;
    (b) toda acción salvo el `action-view` es de los elementos admitidos por el **rol** que declara su `descripcion` (tabla «Rol de una acción» del glosario).
  Exenciones: acciones globales (`VAR-2.5`) y nombres Axelor adaptados.

**Correcto** ✅
```xml
<grid             name="subsysCorreos.Main@Correo-grid" .../>
<action-view      name="subsysCorreos.Main@Correo-action" .../>
<action-group     name="subsysCorreos.Main@Correo-btnSave-action"/>
<action-group     name="subsysCorreos.Main@Correo.Adjunto-onNew-action"/>
<action-group     name="…Main@LeyEducativa-Local-validateSave-action"/>   <!-- grupo que combina validaciones -->
<action-condition name="subsysCorreos.Main@Correo-Local-validateSave-action"/>
<action-method    name="subsysCorreos.Main@Correo-Remote-reenviar-action"/>
<action-record    name="subsysCorreos.Main@Correo.Adjunto-set-correo-parent-action"/>
```
**Incorrecto** ❌ — `<grid name="Main@…-form">` (grid con tipo `form`); `<form name="Main@…">` (sin tipo); `<action-group name="Main@…-btnSave">` (acción sin `-action`); `<action-group name="…@Correo-guardar-action">` (sin marcador `btn`/`on`/`Local-`); `<action-method name="…@Correo-Local-reenviar-action">` (remota marcada como `Local-`)

## VAR-2.3 — La entidad del bloque = clase del `model`
**Decisión.**
  Para saber a qué modelo pertenece un bloque solo por su contexto (sin abrir el `model=`),
  detectar copias/pegados que quedaron con la entidad equivocada,
  y garantizar que un bloque (action-view, grid y form juntos) no mezcla entidades.
**Verificación.**
  Sujeto: cada `grid`/`form`/`tree`/`chart`/`action-view`/`action-method`/`action-script`/`action-record` con `name` (con `@`) y atributo `model`.
  Condición: el último segmento de la ruta de entidad del contexto (la ruta va tras la `@`, antes del primer `-`) == último segmento del `model`.
  Exención: elementos cuyo `model` no pertenece a `com.educaflow` (entidades del framework Axelor):
    su entidad del contexto es deliberadamente el nombre adaptado al español (p.ej. `Usuario`→`com.axelor.auth.db.User`, `Anexos`→`com.axelor.meta.db.MetaFile`).

**Correcto** ✅
```xml
<grid name="subsysCorreos.Main@Correo-grid" model="com.educaflow.subsystem.correos.db.Correo"/>
<form name="subsysCorreos.Main@Correo.Adjunto-form" model="com.educaflow.subsystem.correos.db.Adjunto"/>
```
**Incorrecto** ❌
```xml
<grid name="subsysCorreos.Main@Mensaje-grid" model="com.educaflow.subsystem.correos.db.Correo"/>
```

## VAR-2.4 — Coherencia acción↔método en `action-method`
**Decisión.**
  Para que el nombre de la acción diga qué método del controlador invoca (trazabilidad nombre↔método).
**Verificación.**
  Sujeto: cada `<action-method>` con `name` que contenga `-Remote-{X}-action` y un `<call … method="Y">`.
  Condición: `X == Y` (ignorando los argumentos de `Y` si los lleva, p.ej. `enviarCorreo(id,nombre)`).

**Correcto** ✅
```xml
<action-method name="subsysCorreos.Main@Correo-Remote-reenviar-action">
    <call class="…CorreoController" method="reenviar"/>
</action-method>
```
**Incorrecto** ❌ — mismo `name` `…-Remote-reenviar-action` pero `<call … method="enviar"/>`

## VAR-2.5 — La validación remota de save/delete es única y global
**Decisión.**
  Para no reimplementar por entidad la validación de guardado/borrado:
    existe una única en `DefaultModelController` y las vistas de negocio solo la referencian;
    redeclararla, crear variantes por entidad o reexponer sus métodos rompería esa unicidad.
**Verificación.**
  Sujeto: cada `<action-method>` y cada `name`/`<action name>` de los ficheros de `views/`.
  Condición:
    (a) las cadenas `remote-validationSave-action`/`remote-validationDelete-action` aparecen **solo** como `<action name="…"/>` (referencia dentro de un `action-group`);
    (b) ningún `name` contiene `Remote-validationSave`, `Remote-validationDelete`, `Remote-validateSave` ni `Remote-validateDelete` (variantes por entidad de la validación global);
    (c) ningún `<call>` tiene `method="validationSave"` ni `method="validationDelete"`.

**Correcto** ✅ — `<action name="remote-validationSave-action"/>`, `<action name="remote-validationDelete-action"/>` (referencias, sin `@` ni `model`)
**Incorrecto** ❌
```xml
<!-- Redeclarar la global -->
<action-method name="remote-validationSave-action" model="…db.Correo">
    <call class="…CorreoController" method="validationSave"/>
</action-method>

<!-- Variante por entidad que reexpone la validación global -->
<action-method name="subsysCorreos.Main@Correo-Remote-validateSave-action">
    <call class="…CorreoController" method="validationSave"/>
</action-method>
```

## VAR-2.6 — Unicidad global de `name`
**Decisión.**
  Porque los `name` de vistas y acciones son identificadores globales en Axelor:
    un duplicado hace que una definición pise a la otra en silencio,
    y la integridad referencial (`VAR-4.1`) solo tiene sentido si cada nombre identifica una única cosa.
**Verificación.**
  Sujeto: el conjunto de todos los `name` de hijos directos de `<object-views>` de todos los ficheros del ámbito (incluido `menus.xml`).
  Condición: no hay dos elementos con el mismo `name`.

**Correcto** ✅ — `subsysCorreos.Main@Correo-grid` declarado una sola vez en todo el proyecto.
**Incorrecto** ❌ — dos `<action-group name="subsysCorreos.Main@Correo-btnSave-action">` (aunque sea en ficheros distintos).

---

# Categoría 3 — Bloques y secciones (Processing Instructions)

Cada fichero de vistas se estructura en **bloques** (glosario) y, dentro de cada bloque, la zona de acciones se parte en cuatro **secciones**, todo marcado con **Processing Instructions** (glosario), no con comentarios.
Vocabulario cerrado de PI: `<?sv-view?>` —que abre el bloque y hace de cabecera de sus vistas de alto nivel— más las **cuatro PI de sección** de la tabla «Rol de una acción» del glosario (una sección por rol; qué cuelga de cada una lo fijan `VAR-3.2` y `VAR-3.3`).
Los `Ref-*.xml` **no son especiales**: sus bloques llevan también las cinco PI (aunque las secciones de acción queden vacías).

## VAR-3.1 — Las cinco PI, una vez cada una y en orden, en todo bloque
**Decisión.**
  Para que la estructura de bloques y secciones de cada fichero quede marcada siempre **completa**, en la misma secuencia y repetida por bloque, y sea así predecible y localizable con XPath (no con comentarios).
**Verificación.**
  Sujeto: cada **bloque** de un fichero en alcance.
  Condición: el bloque presenta sus cinco PI **exactamente una vez cada una y en este orden**, aunque una sección quede vacía —
    `<?sv-view?>` → `<?sv-primary-actions?>` → `<?sv-validations?>` → `<?sv-rules?>` → `<?sv-remotes?>` —
    y el bloque siguiente **vuelve a empezar** por `<?sv-view?>` con la misma secuencia.
  Aplica a **todo** bloque, sin exención: también los `Ref` y las vistas de utilidad llevan las cinco PI.
  Fuera del sujeto: los elementos de alto nivel cuyo `name` **no contiene `@`** (overrides del framework Axelor como `user-preferences-form`) no forman bloque, así que no llevan PI.

**Correcto** ✅ — dos bloques consecutivos (maestro y detalle), cada uno con sus cinco PI en orden:
```xml
<?sv-view?>
<grid name="subsysSistemaEducativo.Main@Ciclo-grid" …/>
<form name="subsysSistemaEducativo.Main@Ciclo-form" …/>
<?sv-primary-actions?>
<action-group name="subsysSistemaEducativo.Main@Ciclo-btnSave-action">…</action-group>
<?sv-validations?>
<?sv-rules?>
<?sv-remotes?>

<?sv-view?>
<grid name="subsysSistemaEducativo.Main@Ciclo.Curso-grid" …/>
<form name="subsysSistemaEducativo.Main@Ciclo.Curso-form" …/>
<?sv-primary-actions?>
<?sv-validations?>
<?sv-rules?>
<?sv-remotes?>
```
**Incorrecto** ❌ — un bloque que no abre con `<?sv-view?>`; una PI fuera de orden (`<?sv-rules?>` antes que `<?sv-validations?>`); una PI repetida; o un bloque `Ref` al que le falta alguna de las cuatro secciones de acción.

## VAR-3.2 — `<?sv-view?>` encabeza las vistas de alto nivel del bloque
**Decisión.**
  Para que las vistas visibles del bloque queden juntas bajo el marcador de bloque, antes de la zona de acciones, y en un orden fijo.
**Verificación.**
  Sujeto: los elementos entre `<?sv-view?>` y el `<?sv-primary-actions?>` del mismo bloque.
  Condición: son **solo** elementos de alto nivel (`action-view`/`grid`/`form`/`tree`/`chart`), ninguna acción, y en el orden
    `action-view` → `grid` (opcional) → `form`/`tree`/`chart`.
  Del `action-view`:
    hay **0 o 1** en el bloque, y solo puede aparecer en el **bloque maestro**; los bloques de detalle y los de referencia no lo llevan;
    cuando existe, su `name` es el del bloque más `-action` (`{contexto}-action`).

**Correcto** ✅
```xml
<?sv-view?>
<action-view name="subsysSistemaEducativo.Main@Ciclo-action" …/>
<grid        name="subsysSistemaEducativo.Main@Ciclo-grid" …/>
<form        name="subsysSistemaEducativo.Main@Ciclo-form" …/>
<?sv-primary-actions?>
```
**Incorrecto** ❌ — un `<action-record>` (acción) colocado entre `<?sv-view?>` y `<?sv-primary-actions?>`; el `grid` después del `form`; dos `<action-view>` en el bloque; o un `<action-view>` en un bloque de detalle o de referencia.

## VAR-3.3 — Cada sección contiene solo las acciones de su rol
**Decisión.**
  Para que cada acción del bloque viva bajo el marcador de la sección que le corresponde y las cuatro secciones sean predecibles.
  El criterio es el **rol** de la acción —que su propio nombre declara (`VAR-2.2`)—, no solo el tipo de elemento: por eso un `action-group` que combina validaciones (`Local-…`) va en la sección de validaciones, no en la de acciones principales.
**Verificación.**
  Sujeto: los elementos entre la PI de cada sección y la siguiente PI del bloque (en la última sección, hasta el siguiente `<?sv-view?>` o el fin del fichero).
  Condición: cada sección contiene 0..n acciones, todas del **rol** de esa sección (tabla «Rol de una acción» del glosario).

**Correcto** ✅
```xml
<?sv-primary-actions?>
<action-group name="subsysCorreos.Main@Correo-btnSave-action">…</action-group>
<action-group name="subsysCorreos.Main@Correo.Adjunto-onNew-action">…</action-group>
<?sv-validations?>
<action-condition name="subsysCorreos.Main@Correo-Local-validateSave-action">…</action-condition>
<?sv-rules?>
<action-record name="subsysSistemaEducativo.Main@Ciclo.Curso-set-ciclo-parent-action" …/>
<?sv-remotes?>
<action-method name="subsysCorreos.Main@Correo-Remote-reenviar-action">…</action-method>
```
**Incorrecto** ❌ — un `<action-method>` bajo `<?sv-primary-actions?>` (va bajo `<?sv-remotes?>`); un `<action-group name="…-Local-validateSave-action">` bajo `<?sv-primary-actions?>` (va bajo `<?sv-validations?>`); un `<action-record>` bajo `<?sv-validations?>` (va bajo `<?sv-rules?>`).

## VAR-3.4 — Vocabulario de PI cerrado; sin banners de comentarios
**Decisión.**
  Para que las PI sean la única fuente de verdad de la estructura y no haya que mantener comentarios paralelos sincronizados: los banners de asteriscos desaparecen.
**Verificación.**
  Sujeto: cada PI y cada nodo comentario de un fichero en alcance.
  Condición:
    (a) el `target` de toda PI es uno de `sv-view`, `sv-primary-actions`, `sv-validations`, `sv-rules`, `sv-remotes` (no hay otras `sv-*` ni PIs de target desconocido);
    (b) no hay ningún nodo comentario que haga de **banner de bloque/sección** (corridas de asteriscos, o el texto `: Vistas`/`: Acciones`).

**Correcto** ✅ — la estructura la marcan solo las PI:
```xml
<?sv-view?>
<grid …/>
<form …/>
<?sv-primary-actions?>
```
**Incorrecto** ❌
```xml
<!-- ****************************** Ciclo : Vistas ****************************** -->
<?sv-otra-cosa?>
```

## VAR-3.5 — Contexto uniforme dentro de cada bloque
**Decisión.**
  Para que todo lo que hay entre dos `<?sv-view?>` pertenezca de verdad a la misma pantalla:
    las acciones de una entidad quedan inequívocamente asociadas a su bloque y no se cuelan elementos de otro contexto.
**Verificación.**
  Sujeto: los elementos con `name` que contiene `@` de cada bloque (entre su `<?sv-view?>` y el siguiente).
  Condición: todos comparten el mismo contexto (`{marcadorCapa}{Módulo}.{Variante}@{ruta de entidad}`, es decir todo lo anterior al primer `-`).
  Exención: las referencias `<action name="…">` **dentro** de un `action-group` no son sujetos (pueden apuntar a globales o a acciones del bloque).

**Correcto** ✅ — dentro del bloque `subsysSistemaEducativo.Main@Ciclo.Curso`:
```xml
grid          subsysSistemaEducativo.Main@Ciclo.Curso-grid
form          subsysSistemaEducativo.Main@Ciclo.Curso-form
action-group  subsysSistemaEducativo.Main@Ciclo.Curso-btnSave-action
action-record subsysSistemaEducativo.Main@Ciclo.Curso-set-ciclo-parent-action
```
**Incorrecto** ❌ — dentro de ese mismo bloque, un `…Main@Curso-btnSave-action` (pierde `Ciclo.`) o un `…Main@Ciclo-Remote-reenviar-action` (contexto del bloque maestro).

## VAR-3.6 — Los bloques van de maestro a detalle
**Decisión.**
  Para que el fichero se lea de raíz a hoja sin ambigüedad:
    primero la pantalla que se abre desde el menú, luego sus detalles, y cada detalle cuelga de un bloque ya presentado.
**Verificación.**
  Sujeto: la secuencia de bloques de cada fichero en alcance, con sus rutas de entidad (nº de segmentos = nivel).
  Condición:
    (a) el primer bloque es el **maestro**: nivel 1 y su `Variante@Entidad` casa con el nombre del fichero (`VAR-1.2`);
    (b) recorriendo los bloques en orden, el nivel es monótono no decreciente (igual profundidad permitida para detalles hermanos);
    (c) la ruta de entidad de todo bloque de nivel ≥2, quitándole el último segmento, es la ruta de un bloque anterior del fichero;
    (d) no hay dos bloques con el mismo contexto.

**Correcto** ✅ — bloques `Ciclo` (1 nivel) → `Ciclo.Curso` (2) → `Ciclo.Curso.CursoModulo` (3).
**Incorrecto** ❌ — un bloque `…Ciclo.Curso` (2 niveles) seguido de otro `…Ciclo` (1 nivel); o un bloque `…Ciclo.Curso.CursoModulo` sin que exista antes el bloque `…Ciclo.Curso`.

---

# Categoría 4 — Integridad referencial

## VAR-4.1 — Toda referencia resuelve a un elemento existente del tipo esperado
**Decisión.**
  Para que ninguna vista apunte a algo inexistente (referencia rota que fallaría en runtime)
  ni a algo de un tipo equivocado (un evento que dispara una acción suelta en vez de su orquestador, un menú que abre algo que no es un `action-view`).
**Verificación.**
  Cross-XML global (recorre `menus.xml` + todos los `views/`).
  Sujeto: cada valor de los atributos/nodos de referencia de la tabla.
  Condición: el valor resuelve a un elemento declarado (en cualquier fichero del ámbito) **del tipo esperado**, o a una acción global/predefinida donde la tabla lo admite —

  | Referencia                                        | Debe resolver a |
  |---------------------------------------------------|-----------------|
  | `grid-view` (de `field` o `panel-related`)         | `<grid>` |
  | `form-view` (de `field` o `panel-related`)         | `<form>` |
  | `<view type="grid" name="…">` de un `action-view`  | `<grid>` |
  | `<view type="form" name="…">` de un `action-view`  | `<form>` |
  | `action` de un `<menuitem>`                        | `<action-view>` |
  | `action` de un `<panel-dashlet>`                   | `<action-view>` |
  | eventos `on*`/`onClick` de `form`/`field`/`button` | `<action-group>` (ver nota `serial:`) |
  | `<action name="…">` dentro de un `action-group`    | cualquier acción declarada o global/predefinida |
  | `<dataset type="rpc">…</dataset>` de un `<chart>`  | `<action-method>` |
  | `expr="action:{name}"` de un `<field>`             | acción declarada (normalmente `<action-method>`) |

  Nota `serial:`: en un evento con prefijo `serial:` (AutoFirma), el **último** segmento de la lista debe resolver a un `<action-group>`; los segmentos anteriores, a acciones declaradas.

**Correcto** ✅ — `onClick="Main@…-btnSave-action"` y existe `<action-group name="Main@…-btnSave-action">`; `action="…Main@Ciclo-action"` en un menú y existe ese `<action-view>`.
**Incorrecto** ❌ — `grid-view="…Ref@FamiliaProfesional-listado"` sin ningún `<grid>` con ese `name` (rota); `onClick="…-Local-validateSave-action"` apuntando a una `action-condition` suelta (tipo equivocado: los eventos referencian `action-group`).

## VAR-4.2 — Toda acción declarada está referenciada (sin acciones huérfanas)
**Decisión.**
  Para que no queden acciones muertas: si nadie invoca una acción declarada, o sobra o se olvidó cablearla.
  Es la inversa de `VAR-4.1` (que exige que quien referencia apunte a algo existente): esta exige que todo lo declarado tenga quien lo referencie.
  Hace innecesario prohibir el `action-view` en los ficheros `Ref`: un `action-view` sin menú que lo abra ya es una acción huérfana.
**Verificación.**
  Cross-XML global (recorre `menus.xml` + todos los `views/`).
  Sujeto: cada acción declarada con `name` que contiene `@` — `action-view`, `action-group`, `action-method`, `action-script`, `action-record`, `action-attrs`, `action-validate`, `action-condition`.
  Condición: su `name` aparece referenciado en **al menos uno** de los sitios de la tabla de `VAR-4.1`.
  Exención: acciones globales/predefinidas (que no se declaran en los ficheros) y los `action-view` de utilidad que abre el servidor por código (controlador vía `openView`), no visibles en el XML.

**Correcto** ✅ — el `action-view` `…Main@Ciclo-action` lo abre un `<menuitem action="…Main@Ciclo-action">`; el `action-group` `…-btnSave-action` lo abre `onClick="…-btnSave-action"`.
**Incorrecto** ❌ — un `<action-view name="…Ref@FamiliaProfesional-action">` en un `Ref-*.xml` que ningún `<menuitem>` abre; o una `action-method` que ningún `action-group`/evento/chart invoca.

---

# Categoría 5 — Plantillas canónicas

Las dos reglas de esta categoría son **genéricas**: fijan por **tipo de elemento** (y por **clase de bloque**, glosario) la plantilla de atributos que toda vista repite.
Las categorías siguientes contienen las reglas **estructurales** de cada tipo de elemento, no las de atributos.

## VAR-5.1 — Núcleo canónico de atributos por tipo de elemento
**Decisión.**
  Para que cada tipo de elemento se comporte y se vea igual en todas las pantallas, con una única plantilla por elemento:
  - `<form>`: toda interacción pasa por los botones controlados del `buttons-panel` y sus `action-group` (secuencias de `VAR-7.2`);
    con la toolbar nativa viva, o el guardado colgado del `onSave`, el usuario podría guardar o borrar saltándose las validaciones y el cierre controlado.
    Se conserva solo `canBackOnSave`, y únicamente donde aporta (por eso depende de la clase):
      en el maestro hace que al guardar con éxito la vista vuelva sola al grid (el cierre lo remata el `back` explícito del `btnSave`, ver `VAR-7.2`);
      en un detalle el cierre lo gestiona `save-modal` y en una referencia no se guarda nada, así que en ambos sobra.
  - `<grid>`: todo listado es de solo consulta/navegación uniforme (no editable en línea, sin selector ni búsqueda avanzada/refresco, ordenado, sin título redundante) y sin el concepto de archivado (confuso para el usuario final y no usado en el proyecto).
  - `<panel-related>`: la rejilla embebida de un detalle se comporta igual en todas las pantallas (ancho completo, sin edición en línea ni borrado directo desde la rejilla — todo pasa por el modal — y abriendo en edición).
  - `buttons-panel`: los botones de acción están siempre en el mismo sitio y con el mismo aspecto (agrupados al final del form, sin marco).
  - `btnDelete`: no ofrecer borrar sobre un registro que aún no existe (sin `id`/`cid`) y señalar visualmente que es una acción **destructiva**, distinguible de guardar/cancelar.
**Verificación.**
  Sujeto: cada `<form>`, `<grid>`, `<panel-related>`, `<panel name="buttons-panel">` y `<button>` cuyo `name` empieza por `btnDelete` — de cualquier clase, variante y nivel.
  Condición: el elemento cumple la fila de su tipo —

  | Elemento | Atributos canónicos |
  |---|---|
  | `<form>` | `canAttach`/`canBack`/`canDelete`/`canNew`/`canSave`/`canMore`, si están presentes, valen `false` (ninguno vale `true`); sin atributo `onSave`; `canBackOnSave="true"` **solo** en el form maestro con `btnSave` (ausente en el maestro de solo consulta, el detalle y la referencia) |
  | `<grid>` | presentes `editable="false"`, `edit-icon="false"`, `x-selector="none"`, `canEdit="false"`, `canDelete="false"`, `canSave="false"`, `title=""` y `orderBy`; `canAdvanceSearch` y `canRefresh` ausentes o a `"false"`; sin atributo `archived` |
  | `<panel-related>` | presentes `colSpan="12"`, `showFooter="false"`, `canEdit="false"`, `canRemove="false"` y `forceEdit="true"` (la coherencia `canNew`/`newButtonTitle` la verifica `VAR-8.2`; que sus `grid-view`/`form-view` existan, `VAR-4.1`) |
  | `buttons-panel` | `title` vacío, `colSpan="12"` y `showFrame="false"` |
  | `btnDelete…` | `showIf="(id!=null) \|\| (cid!=null)"` (tolerando espacios), `css="btn-danger"` y `outline="true"` |

**Correcto** ✅
```xml
<form name="subsysCorreos.Main@Correo-form" canAttach="false" canBack="false" canDelete="false"
      canNew="false" canSave="false" canMore="false" canBackOnSave="true">
<grid name="subsysSistemaEducativo.Main@Ciclo-grid" title="" orderBy="name"
      editable="false" edit-icon="false" x-selector="none"
      canEdit="false" canDelete="false" canSave="false"
      canAdvanceSearch="false" canRefresh="false" canNew="true" newButtonTitle="Añadir un nuevo ciclo">
<panel-related name="adjuntos" field="adjuntos" title="Adjuntos"
    grid-view="subsysCorreos.Main@Correo.Adjunto-grid" form-view="subsysCorreos.Main@Correo.Adjunto-form"
    colSpan="12" newButtonTitle="Añadir adjunto" showFooter="false" canEdit="false" canRemove="false" forceEdit="true"/>
<panel name="buttons-panel" title="" colSpan="12" showFrame="false">
    <button name="btnDelete" title="Borrar" onClick="Main@…-btnDelete-action"
            showIf="(id!=null) || (cid!=null)" css="btn-danger" outline="true"/>
</panel>
```
**Incorrecto** ❌ — `<form … canSave="true" canNew="true">` (deja viva la toolbar nativa); `<form … onSave="…-Local-validateSave-action">` (la validación cuelga del `btnSave-action`, no del `onSave`); el form maestro con `btnSave` sin `canBackOnSave`; un form de detalle o referencia con `canBackOnSave="true"`; un `<grid>` sin `orderBy` o con `archived="true"`; un `<panel-related>` con `canEdit="true"` o sin `forceEdit="true"`; un `buttons-panel` con `showFrame="true"`; un `btnDelete` con `css="btn-primary"` y sin `showIf`.

## VAR-5.2 — La clase referencia es de solo lectura
**Decisión.**
  Porque una vista de referencia existe solo para buscar/consultar la entidad relacionada desde el form que la embebe:
    nada de lo que muestra debe poder crearse ni editarse como efecto colateral.
**Verificación.**
  Sujeto: cada `<grid>` y cada `<form>` de clase **referencia** (variante `Ref`).
  Condición:
    (a) el grid tiene `canNew="false"` y `canViewOnClick="true"` (y no `canEditOnClick`, coherente con `VAR-8.1`);
    (b) todos los `<field>` del form tienen `readonly="true"`.

**Correcto** ✅
```xml
<grid name="subsysSistemaEducativo.Ref@FamiliaProfesional-grid" …
      canNew="false" canViewOnClick="true">
<form name="subsysSistemaEducativo.Ref@FamiliaProfesional-form" …>
    <panel name="FamiliaProfesional" title="">
        <field name="code" readonly="true"/>
        <field name="name" readonly="true"/>
    </panel>
</form>
```
**Incorrecto** ❌ — un `Ref@…-grid` con `canNew="true"` o con `canEditOnClick="true"`; un `<field name="name"/>` sin `readonly="true"` dentro del `Ref@…-form`.

---

# Categoría 6 — Forms

Las reglas de esta categoría usan la **clase de bloque** del glosario (maestro / detalle / referencia), que se deduce mecánicamente del contexto del `name`.
Los atributos canónicos del form, el `buttons-panel` y el `panel-related` los fija `VAR-5.1`; aquí va su estructura.

## VAR-6.1 — Panel de botones
**Decisión.**
  Para que los botones de acción estén agrupados en el mismo sitio en todas las pantallas, por consistencia de UX;
  y para que toda vista de referencia tenga una forma explícita de salir.
**Verificación.**
  Sujeto: cada `<form>`.
  Condición:
    (a) los forms de clase **maestro** y **referencia** contienen exactamente un `<panel name="buttons-panel">`;
      los de **detalle** también, salvo los de solo lectura (sin botones);
    (b) todo `buttons-panel` contiene 1..n `<button>` cuyo `name` empieza por `btn` (sus atributos canónicos los fija `VAR-5.1`);
    (c) ningún `<button>` del form vive fuera del `buttons-panel`.

**Correcto** ✅
```xml
<panel name="buttons-panel" title="" colSpan="12" showFrame="false">
    <button name="btnDelete" .../>
    <button name="btnCancel" .../>
    <button name="btnSave"   .../>
</panel>
```
**Incorrecto** ❌ — un form maestro sin `buttons-panel`; un `Ref@…-form` sin botón de salir; o botones sueltos fuera del `buttons-panel`.

## VAR-6.2 — Campos dentro de panel
**Decisión.**
  Porque la maquetación en rejilla (`colSpan`/`colOffset`) y el encuadre visual de Axelor se definen a nivel de panel:
    un `<field>` colgando suelto del form rompe el layout y no queda agrupado semánticamente.
**Verificación.**
  Sujeto: hijos directos de `<form>`.
  Condición: no hay `<field>` como hijo directo.

**Correcto** ✅
```xml
<form …>
    <panel name="Correo"><field name="asunto"/></panel>
</form>
```
**Incorrecto** ❌
```xml
<form …>
    <field name="asunto"/>            <!-- campo suelto, hijo directo del form -->
</form>
```

## VAR-6.3 — Campos relacionales apuntan a las vistas de referencia
**Decisión.**
  Para que el usuario no pueda editar ni crear la entidad relacionada como efecto colateral desde un form ajeno:
    se le muestran sus vistas de **referencia** (solo lectura y búsqueda), no las de mantenimiento.
**Verificación.**
  Sujeto: cada `<field>` con `form-view`/`grid-view`.
  Condición: el `form-view` acaba en `Ref@…-form` y el `grid-view` en `Ref@…-grid`
    (que además existen, por `VAR-4.1`).

**Correcto** ✅
```xml
<field name="familiaProfesional"
       grid-view="subsysSistemaEducativo.Ref@FamiliaProfesional-grid"
       form-view="subsysSistemaEducativo.Ref@FamiliaProfesional-form"/>
```
**Incorrecto** ❌ — `form-view="subsysSistemaEducativo.Main@FamiliaProfesional-form"` (apunta al form de mantenimiento, no a `Ref@…-form`)

## VAR-6.4 — El form de detalle enlaza el campo padre (oculto) con `onNew`
**Decisión.**
  Para que al crear un detalle nuevo quede enlazado a su maestro desde el primer momento (el `onNew` dispara la asignación del campo padre);
  y porque ese campo existe solo para la relación (lo fija el `onNew`), no debe mostrarse al usuario.
**Verificación.**
  Sujeto: cada `<form>` de clase detalle, salvo los de solo lectura.
  Condición, con `{campoPadre}` = el penúltimo segmento de la ruta de entidad del bloque en lowerCamelCase (`Correo.Adjunto` → `correo`):
    (a) tiene atributo `onNew`, y el `action-group` que referencia incluye una acción `{contexto}-set-{campoPadre}-parent-action`;
    (b) existe un `<field name="{campoPadre}" showIf="false">`.

**Correcto** ✅
```xml
<form name="…Main@Correo.Adjunto-form" onNew="subsysCorreos.Main@Correo.Adjunto-onNew-action" …>
    <!-- … -->
    <field name="correo" showIf="false"/>
</form>
<action-group name="subsysCorreos.Main@Correo.Adjunto-onNew-action">
    <action name="subsysCorreos.Main@Correo.Adjunto-set-correo-parent-action"/>
</action-group>
```
**Incorrecto** ❌ — el form de detalle sin atributo `onNew`; cuyo grupo `onNew` no incluye el `set-{campoPadre}-parent-action`; o con el campo padre visible (`<field name="correo"/>` sin `showIf="false"`) o ausente.

## VAR-6.5 — Nombres de panel no genéricos
**Decisión.**
  Por legibilidad y mantenimiento:
    un `name` de panel semántico (entidad o rol) documenta qué agrupa, frente a genéricos anónimos como `panel1`.
**Verificación.**
  Sujeto: cada `<panel>` con `name` (excepto `buttons-panel`).
  Condición: el `name` no casa con el patrón `(?i)^(panel|nombrePanel)[0-9]*$`.

**Correcto** ✅ — `<panel name="Correo">`, `<panel name="Envio">`, `<panel name="panelModoX">`
**Incorrecto** ❌ — `<panel name="panel1">`, `<panel name="nombrePanel">`

---

# Categoría 7 — Botones y secuencias de acciones

Las secuencias de los botones estándar (`btnSave`/`btnDelete`/`btnCancel`) dependen de la **clase** del form (glosario): el maestro va al servidor, el detalle opera en la colección en memoria del padre (acciones `-modal`, sin `remote-validation*` porque el maestro puede no existir aún en BD y el walker valida los detalles en cascada al guardar), y la referencia solo cierra.

## VAR-7.1 — `onClick` de botón → su `action-group` `btn{X}`
**Decisión.**
  Para que cada botón enlace de forma unívoca y trazable con el `action-group` que lo gobierna;
  dos botones gemelos con `showIf` excluyentes (p.ej. `btnCancelAlta`/`btnCancelSalir`) pueden compartir el mismo grupo, por lo que el vínculo exigido es de prefijo, no de igualdad.
  El prefijo `serial:` (firma con AutoFirma, ver nota `serial:` de `VAR-4.1`) encadena varios action-group en un mismo `onClick` porque un paso intermedio es una llamada asíncrona que debe **esperarse** antes de seguir; aun así **todos** los segmentos son `action-group` (no acciones sueltas) y quedan atados al botón por prefijo.
**Verificación.**
  Sujeto: cada `<button>` de un `<form>` con `onClick`.
  El `onClick` designa uno o varios action-group; su **segmento canónico** es el último: en un `onClick` normal, el valor completo; en un `onClick` con prefijo `serial:`, el último segmento de la lista separada por comas.
  Condición:
    (a) el segmento canónico es `{contexto del bloque}-btn{X}-action` (su contexto es **el del bloque del form del botón**, su descripción empieza por `btn` y su tipo es `action`) y resuelve a un `<action-group>` (`VAR-4.1`);
    (b) el `name` del botón **empieza por** `btn{X}` (la descripción del segmento canónico);
    (c) si el `onClick` lleva prefijo `serial:`, **cada** segmento anterior resuelve también a un `<action-group>` cuyo `name` empieza por `{contexto}-btn{X}-` (mismo botón; descripción `btn{X}-{sufijo}`), de modo que todos los grupos encadenados quedan atados al botón.

**Correcto** ✅ — `<button name="btnSave" onClick="subsysCorreos.Main@Correo-btnSave-action"/>`;
  `<button name="btnCancelAlta" onClick="…-btnCancel-action"/>` y `<button name="btnCancelSalir" onClick="…-btnCancel-action"/>` (gemelos que comparten grupo);
  `<button name="btnPaso2FirmarGuardar" onClick="serial:…-btnPaso2FirmarGuardar-firmarConAutoFirma-action,…-btnPaso2FirmarGuardar-action"/>` (serial: el intermedio y el canónico son ambos `<action-group>` con el prefijo `btnPaso2FirmarGuardar` del botón)
**Incorrecto** ❌ — `<button name="btnSave" onClick="guardar"/>` (sin contexto); `<button name="btnSave" onClick="…-btnDelete-action"/>` (el nombre del botón no empieza por `btnDelete`); en el form del bloque `…Main@Ciclo.Curso`, un `<button name="btnSave" onClick="…Main@Ciclo-btnSave-action"/>` (contexto de otro bloque); un `serial:` cuyo segmento intermedio apunta directamente a un `<action-method>` (`…-Remote-…-action`) en vez de a un `<action-group>`, o a un grupo cuyo `name` no empieza por el `btn{X}` del segmento canónico

## VAR-7.2 — Secuencia de los botones estándar según la clase del form
**Decisión.**
  Para guardar, borrar y cancelar de forma segura y uniforme:
    en el **maestro**, primero las validaciones (local y remota), luego la acción predefinida, y tras `save` un cierre explícito (si el usuario pulsa Guardar sin cambiar nada `save` es un no-op y `canBackOnSave` no cierra la ventana — `back`/`force-back` sí);
    en el **detalle**, las variantes `-modal` operan sobre la colección en memoria del padre y la validación remota no aplica (ver preámbulo de la categoría);
    en la **referencia** solo se puede salir, sin tocar el registro.
**Verificación.**
  Sujeto: el `<action-group>` referenciado por el `onClick` de cada botón estándar (`name` que empieza por `btnSave`/`btnDelete`/`btnCancel`), según la clase de su form —

  | Botón | maestro | detalle | referencia |
  |---|---|---|---|
  | `btnSave` | [`Local-…`]* → `remote-validationSave-action` → `save` → `back`\|`force-back` (inmediatamente tras `save`) | [`Local-…`]* → `save-modal`; **sin** ninguna `remote-validation*` | no existe |
  | `btnDelete` | [`remote-validationDelete-action`] → `delete` (termina en `delete`) | termina en `delete-modal`; **sin** ninguna `remote-validation*` | no existe |
  | `btnCancel` | contiene `back` (o `force-back`) | contiene `close` | contiene `close` |

**Correcto** ✅
```xml
<!-- maestro -->
<action-group name="subsysCorreos.Main@Correo-btnSave-action">
    <action name="subsysCorreos.Main@Correo-Local-validateSave-action"/>
    <action name="remote-validationSave-action"/>
    <action name="save"/>
    <action name="back"/>
</action-group>
<action-group name="subsysCorreos.Main@Correo-btnDelete-action">
    <action name="remote-validationDelete-action"/>
    <action name="delete"/>
</action-group>
<action-group name="subsysCorreos.Main@Correo-btnCancel-action">
    <action name="back"/>
</action-group>
<!-- detalle -->
<action-group name="subsysCorreos.Main@Correo.Adjunto-btnSave-action">
    <action name="subsysCorreos.Main@Correo.Adjunto-Local-validateSave-action"/>
    <action name="save-modal"/>
</action-group>
```
**Incorrecto** ❌ — en el maestro, `save` sin `back`/`force-back` después, `save` antes de la validación, o un `btnCancel` con `close`; en el detalle, `save`+`back` o `delete` (secuencias de maestro), una `remote-validation*` antes de `save-modal`/`delete-modal`, o un `btnCancel` con `back`.

## VAR-7.3 — Los grupos de save/delete no llaman a controladores propios
**Decisión.**
  Para que la persistencia y el borrado los haga siempre la plataforma (el endpoint REST del modelo, con sus `validate*`/`AllowProperties`) y no un controlador propio que se saltaría esas defensas.
**Verificación.**
  Sujeto: los `<action name>` de cada `<action-group>` de `btnSave`/`btnDelete`.
  Condición: cada uno es —
    una acción `Local-…` del mismo contexto,
    una de las globales `remote-validationSave-action`/`remote-validationDelete-action`,
    o una predefinida (`save`, `delete`, `back`, `force-back`, `save-modal`, `delete-modal`, `close`).
  Ningún `Remote-…-action` propio.

**Correcto** ✅ — el btnSave usa `<action name="save"/>` (acción predefinida)
**Incorrecto** ❌ — el btnSave usa `<action name="subsysCorreos.Main@Correo-Remote-guardar-action"/>` que persiste en el controlador

## VAR-7.4 — `Remote-validate{Op}` inmediatamente antes de `Remote-{Op}`
**Decisión.**
  Para que toda operación custom valide en servidor justo antes de ejecutarse,
  y la pareja validación↔operación sea reconocible por nombre (`validate{Op}` valida a `{Op}`).
**Verificación.**
  Sujeto: cada `<action-group>` que contenga una acción `…-Remote-{Op}-action` (con `{Op}` que no empiece por `validate`).
  Condición: si existe (en el ámbito) la acción `…-Remote-validate{Op}-action` del mismo contexto,
    el grupo la incluye **inmediatamente antes** de `…-Remote-{Op}-action` (`{Op}` capitalizado tras `validate`).

**Correcto** ✅
```xml
<action-group name="subsysCorreos.Main@Correo-btnReenviar-action">
    <action name="subsysCorreos.Main@Correo-Remote-validateReenviar-action"/>
    <action name="subsysCorreos.Main@Correo-Remote-reenviar-action"/>
</action-group>
```
**Incorrecto** ❌ — el grupo invoca `…-Remote-reenviar-action` sin su `…-Remote-validateReenviar-action` declarado antes, o con otra acción intercalada entre ambas.

## VAR-7.5 — Botones condicionales en paneles de estado, no gemelos en panel plano
**Decisión.**
  Un `<button>` oculto con `showIf` **reserva sus columnas** en el grid igual que un campo oculto:
    los gemelos con `colOffset` en un `buttons-panel` plano dejan huecos en medio de la fila,
    y si el total de columnas declarado por todos los botones supera 12, los visibles saltan de fila.
  Por eso los botones condicionales van en **paneles de estado anidados** dentro del `buttons-panel`
    (el `showIf` en el panel, que sí colapsa del todo; los botones dentro sin `showIf`),
    y en un panel plano solo se tolera el tramo inicial de botones condicionales pegado al borde izquierdo
    (p.ej. el `btnDelete` canónico), cuyo hueco al ocultarse no desplaza a nadie.
**Verificación.**
  Sujeto: cada `<panel name="buttons-panel">` y sus `<button>` **hijos directos**
    (los botones dentro de paneles anidados del `buttons-panel` quedan fuera del sujeto).
  Condición:
    (a) la suma de `colOffset` + `colSpan` de **todos** los botones hijos directos es ≤ 12
      (ocultos incluidos, porque reservan su sitio; `colSpan` ausente cuenta como 6 —el valor por defecto de Axelor— y `colOffset` ausente como 0);
    (b) todo botón hijo directo con `showIf` cumple:
      no tiene `colOffset`,
      y todos sus hermanos `<button>` anteriores del panel llevan también `showIf` sin `colOffset`
      (los condicionales solo pueden ser el tramo inicial pegado al borde izquierdo).
  El `showIf` canónico de `btnDelete*` (`VAR-5.1`) no está exento: si convive con gemelos u offsets condicionales, va dentro de su panel de estado (donde deja de ser hijo directo).

**Correcto** ✅
```xml
<!-- plano: el único condicional es el tramo inicial (btnDelete); 2+6+2+2 = 12 -->
<panel name="buttons-panel" title="" colSpan="12" showFrame="false">
    <button name="btnDelete" title="Borrar" colSpan="2" showIf="(id!=null) || (cid!=null)" css="btn-danger" outline="true" onClick="…"/>
    <button name="btnCancel" title="Cancelar" colSpan="2" colOffset="6" outline="true" onClick="…"/>
    <button name="btnSave" title="Guardar" colSpan="2" onClick="…"/>
</panel>
<!-- estados: cada combinación visible vive en su panel anidado y suma 12 por sí sola -->
<panel name="buttons-panel" title="" colSpan="12" showFrame="false">
    <panel name="buttonsAlta" title="" colSpan="12" showFrame="false" showIf="(id == null) &amp;&amp; (cid == null)">
        <button name="btnCancelAlta" title="Cancelar" colSpan="2" colOffset="8" outline="true" onClick="…"/>
        <button name="btnSaveAlta" title="Guardar" colSpan="2" onClick="…"/>
    </panel>
    <panel name="buttonsEdicion" title="" colSpan="12" showFrame="false" showIf="(id != null) || (cid != null)">
        <button name="btnDelete" title="Borrar" colSpan="2" showIf="(id!=null) || (cid!=null)" css="btn-danger" outline="true" onClick="…"/>
        <button name="btnCancelSalir" title="Salir" colSpan="2" colOffset="6" outline="true" onClick="…"/>
        <button name="btnSaveEdicion" title="Guardar" colSpan="2" onClick="…"/>
    </panel>
</panel>
```
**Incorrecto** ❌ — gemelos condicionales en panel plano: `btnCancelAlta` (`colSpan="2" colOffset="6" showIf="id == null"`) y `btnCancelSalir` (`colSpan="2" colOffset="6" showIf="id != null"`) junto a `btnSave` (`colSpan="2" showIf="id == null"`) suman 2+6+2+6+2+2 = 20 > 12 (a) y ambos gemelos llevan `colOffset` siendo condicionales (b): el gemelo oculto reserva offset+span y empuja a `btnSave` a una segunda fila.

---

# Categoría 8 — Grids

Los atributos canónicos de todo grid los fija `VAR-5.1` (y los de la clase referencia, `VAR-5.2`); aquí van las reglas estructurales.

## VAR-8.1 — Comportamiento de clic único
**Decisión.**
  Porque al pulsar una fila el grid o abre para editar o abre en solo lectura, nunca ambas (serían comportamientos contradictorios) y nunca ninguna (fila muerta).
**Verificación.**
  Sujeto: cada `<grid>`.
  Condición: tiene **exactamente uno** de `canEditOnClick="true"` / `canViewOnClick="true"` (nunca ambos, nunca ninguno).
  (Que en la clase referencia el presente sea `canViewOnClick="true"` lo fija `VAR-5.2`.)

**Correcto** ✅ — `Main@…-grid` con `canEditOnClick="true"`; un grid de variante de consulta (p.ej. `Firmado@…-grid`) o un `Ref@…-grid` con `canViewOnClick="true"`
**Incorrecto** ❌ — `<grid canEditOnClick="true" canViewOnClick="true">`; un grid sin ninguno de los dos

## VAR-8.2 — Coherencia `canNew`/`newButtonTitle` (grids y `panel-related`)
**Decisión.**
  Para que el botón de «nuevo» siempre tenga texto cuando está activo y no quede un `newButtonTitle` huérfano cuando no se puede crear.
  La regla es la misma para un `<grid>` y para la rejilla embebida de un `<panel-related>`; solo cambia el defecto de `canNew` cuando está ausente.
**Verificación.**
  Sujeto: cada `<grid>` y cada `<panel-related>`.
  Condición, donde «puede crear» = `canNew="true"`, o `canNew` ausente en un `<panel-related>` (su defecto es crear; en un `<grid>` ausente = no crear):
    (a) puede crear ⇒ `newButtonTitle` presente y no vacío;
    (b) no puede crear ⇒ `newButtonTitle` ausente o vacío.
  (Que un `Ref@…-grid` no pueda crear lo fija `VAR-5.2`.)

**Correcto** ✅ — `canNew="true" newButtonTitle="Añadir un nuevo ciclo"`; `canNew="false"` sin `newButtonTitle`; un `<panel-related … newButtonTitle="Añadir adjunto" …>` (sin `canNew`: su defecto es crear)
**Incorrecto** ❌ — `canNew="false" newButtonTitle="Añadir…"`; `canNew="true"` sin `newButtonTitle`

## VAR-8.3 — `<help>` como primer hijo del grid
**Decisión.**
  Porque el mensaje de ayuda del listado es una etiqueta hija (no un atributo) y debe preceder a las columnas para que el fichero se lea en el mismo orden en que se renderiza.
**Verificación.**
  Sujeto: cada `<grid>` con hijo `<help>`.
  Condición: el `<help>` es el **primer** hijo del `<grid>`; no existe atributo `help` en el `<grid>`.

**Correcto** ✅
```xml
<grid name="…Main@Ciclo-grid" …>
    <help>Aquí se listan todos los ciclos que hay en el sistema</help>
    <field name="code"/>
</grid>
```
**Incorrecto** ❌ — el `<help>` después de los `<field>`; o el texto en un atributo `help="…"` del `<grid>`.

---

# Categoría 9 — `action-view`

## VAR-9.1 — Estructura del `action-view`: `model` presente y grid antes que form
**Decisión.**
  Para que el action-view declare explícitamente sobre qué entidad abre sus vistas
  (la coherencia del `model` con el bloque la verifica `VAR-2.3`; que las vistas referenciadas existan, `VAR-4.1`);
  y porque el form se abre desde el grid, no al revés: si están ambos, el grid debe declararse primero para que la navegación funcione (no es obligatorio que estén ambos).
**Verificación.**
  Sujeto: cada `<action-view>`.
  Condición:
    (a) tiene atributo `model`;
    (b) si declara `<view type="grid">` y `<view type="form">`, el grid aparece antes que el form.

**Correcto** ✅
```xml
<action-view name="subsysCorreos.Main@Correo-action" model="…db.Correo">
    <view type="grid" name="subsysCorreos.Main@Correo-grid"/>
    <view type="form" name="subsysCorreos.Main@Correo-form"/>
</action-view>
```
**Incorrecto** ❌ — el `<view type="form">` declarado antes que el `<view type="grid">`; o el `<action-view>` sin atributo `model`.

## VAR-9.2 — `view-param` obligatorios: `show-toolbar-form` y `forceEdit`
**Decisión.**
  Para ocultar la toolbar nativa del form y que toda interacción pase por los botones controlados (coherente con `VAR-5.1`);
  y para que al pulsar una fila de un grid editable (`canEditOnClick`) el form se abra directamente en modo edición y no en modo lectura con un clic extra.
**Verificación.**
  Sujeto: cada `<action-view>`.
  Condición:
    (a) si abre un form ⇒ presente `<view-param name="show-toolbar-form" value="false"/>`;
    (b) si su `<view type="grid">` referencia un grid con `canEditOnClick="true"` ⇒ presente `<view-param name="forceEdit" value="true"/>`.

**Correcto** ✅
```xml
<action-view name="…Main@Ciclo-action" model="…db.Ciclo">
    <view type="grid" name="…Main@Ciclo-grid"/>          <!-- grid con canEditOnClick="true" -->
    <view type="form" name="…Main@Ciclo-form"/>
    <view-param name="show-toolbar-form" value="false"/>
    <view-param name="forceEdit" value="true"/>
</action-view>
```
**Incorrecto** ❌ — ese mismo `<action-view>` sin el `view-param` `show-toolbar-form` o sin el `forceEdit`

---

# Categoría 10 — Menús (`menus.xml`)

Sujeto de toda la categoría: el fichero único `secretariavirtual/menus/menus.xml` (que sea el único con `<menuitem>` lo verifica `VAR-1.3`; que el `action` de cada hoja resuelva a un `<action-view>`, `VAR-4.1`).

## VAR-10.1 — Atributos obligatorios: `name`/`title`/`order`/`groups` (con valores canónicos)
**Decisión.**
  Para que cada menú tenga identificador, texto visible y posición explícita (sin depender del orden de aparición),
  y esté siempre restringido explícitamente a un público (administradores, usuarios o ambos) sin quedar visible para cualquiera por olvido;
  los valores de `groups` se fijan a una forma canónica única para poder testearlos y compararlos.
**Verificación.**
  Sujeto: cada `<menuitem>`.
  Condición:
    (a) tiene los atributos `name`, `title`, `order` y `groups`;
    (b) el valor de `groups` es **exactamente** uno de `admins`, `admins,users` o `users` — no se admite ningún otro valor ni la variante desordenada `users,admins`.

**Correcto** ✅ — `<menuitem name="correos-menuitem" title="Correos" groups="admins,users" order="45"/>`
**Incorrecto** ❌ — `<menuitem title="Sistema educativo" groups="admins"/>` (sin `name` ni `order`), `<menuitem name="registro-menuitem" title="Registro" order="60"/>` (sin `groups`), `groups="users,admins"` (orden no canónico), `groups="secretario"` (rol no permitido)

## VAR-10.2 — `order` entero único por submenú
**Decisión.**
  Para que el orden de los ítems de un submenú sea determinista y sin empates.
**Verificación.**
  Sujeto: los `<menuitem>` con el mismo `parent`.
  Condición: sus `order` son enteros distintos.

**Correcto** ✅ — hijos de `sistemaEducativo-menuitem` con `order="1"`, `order="2"`, `order="3"`…
**Incorrecto** ❌ — dos hijos del mismo `parent` con `order="1"`

## VAR-10.3 — Formato: una línea por menú, atributos en orden fijo, sangría por nivel
**Decisión.**
  Para diffs limpios y una lectura uniforme de todos los `<menuitem>` (un menú por línea completa, sin alineaciones en columnas que haya que remaquetar al tocar cualquier atributo),
  y para que la sangría refleje visualmente la jerarquía del árbol de menús.
**Verificación.**
  Sujeto: cada `<menuitem>` (y el texto del fichero).
  Condición:
    (a) no hay dos `<menuitem>` en la misma línea ni un `<menuitem>` partido en varias líneas;
    (b) los atributos presentes respetan el orden relativo `name, parent, title, action, icon, groups, if, order` y se separan con un único espacio (sin espacios extra de alineación);
    (c) sangría = 4 × (profundidad de `parent`).

**Correcto** ✅
```xml
<menuitem name="sistemaEducativo-menuitem" title="Sistema educativo" groups="admins" order="30"/>
    <menuitem name="sistemaEducativo-ciclos-menuitem" parent="sistemaEducativo-menuitem" title="Ciclos" action="…" groups="admins" order="2"/>
    <menuitem name="sistemaEducativo-cursos-menuitem" parent="sistemaEducativo-menuitem" title="Cursos" action="…" groups="admins" order="3"/>
```
**Incorrecto** ❌ — `<menuitem …/><menuitem …/>` en una sola línea; `<menuitem name="…" order="1" title="…" parent="…"/>` (order y title fuera de sitio); `<menuitem name="x-menuitem"    parent="y-menuitem"…` (alineado en columnas); o la hoja `sistemaEducativo-ciclos-menuitem` sin sangría, al mismo nivel que su raíz

## VAR-10.4 — Naming `-menuitem`
**Decisión.**
  Para reconocer los ítems de menú por el sufijo `-menuitem` y ubicar cada hoja bajo su padre por el nombre.
**Verificación.**
  Sujeto: cada `<menuitem>`.
  Condición: `name` termina en `-menuitem`;
    las hojas prefijan el nombre del padre (sin su sufijo `-menuitem`).

**Correcto** ✅ — raíz `sistemaEducativo-menuitem`; hoja `sistemaEducativo-ciclos-menuitem`
**Incorrecto** ❌ — `menu1`, `ciclos` (sin sufijo `-menuitem`); una hoja `ciclos-menuitem` colgando de `sistemaEducativo-menuitem` (sin el prefijo del padre)

---

# Categoría 11 — Charts / Trees

Sin sujetos fuera de los paquetes exentos hoy → catalogadas, no testeadas.

## VAR-11.1 — Chart con `<dataset type="rpc">`
**Decisión.**
  Para que la consulta del chart viva en el controlador (una acción remota) y no como SQL/JPQL embebido en la vista.
**Verificación.**
  Sujeto: cada `<chart>/<dataset>`.
  Condición: `type="rpc"` (que su contenido resuelva a un `<action-method>` existente lo verifica `VAR-4.1`).

**Correcto** ✅
```xml
<chart name="Main@…-chart" title="Correos por estado">
    <dataset type="rpc">subsysCorreos.Main@Correo-Remote-datosPorEstado-action</dataset>
</chart>
```
**Incorrecto** ❌ — `<dataset type="jpql">select count(c) from Correo c …</dataset>`

## VAR-11.2 — `key` de `category`/`series` presentes
**Decisión.**
  Porque el chart necesita saber qué clave del resultado va a cada eje/serie para poder pintarse.
**Verificación.**
  Sujeto: cada `<chart>`.
  Condición: `category` y `series` tienen atributo `key`.

**Correcto** ✅ — `<category key="estado" type="text"/>` y `<series key="total" type="bar"/>`
**Incorrecto** ❌ — `<category type="text"/>` (sin `key`)

## VAR-11.3 — Columnas del tree sin `colSpan`/`multiple`/`required`
**Decisión.**
  Porque `colSpan`/`multiple`/`required` son de forms/campos y no tienen sentido en columnas de un árbol.
**Verificación.**
  Sujeto: cada `<column>` de un `<tree>`.
  Condición: sin `colSpan`, `multiple` ni `required`.

**Correcto** ✅ — `<column name="name" type="string"/>`
**Incorrecto** ❌ — `<column name="name" colSpan="2" required="true"/>`

## VAR-11.4 — Nodos del tree declarados de raíz a hoja
**Decisión.**
  Para que cada nodo se declare antes que los que dependen de él y el árbol se construya sin referencias adelantadas.
**Verificación.**
  Sujeto: los `<node>` de un `<tree>`.
  Condición: cada nodo aparece antes que sus descendientes.

**Correcto** ✅ — `<node model="…FamiliaProfesional"/>` antes que `<node model="…Ciclo" parent="…"/>`
**Incorrecto** ❌ — el nodo hijo `Ciclo` declarado antes que su nodo padre `FamiliaProfesional`

---

# Fuera del alcance de estos tests

Requieren leer el código Java (no solo el XML) y se dejan como posible fase futura con reflexión:

- Que el `method` de un `<action-method>` exista en el controlador y lleve `@CallMethod`.
- Que las claves del `Map` devuelto por una acción RPC de chart coincidan con los `key` del chart.
- Que el `{campoPadre}` de `VAR-6.4` exista como campo many-to-one en el dominio del detalle.
