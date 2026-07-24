# Contrato del diseño de un sistema

Define **qué produce el diseño de un sistema y cómo**: la conversión de las reglas del spec a la taxonomía técnica, la clasificación de campos, las reglas arquitectónicas y de seguridad, la estructura del diseño, el orden de los pasos y el checklist. Lo lee, según su rol (`README.md` §2): el **diseñador** para producir el diseño completo en su carpeta `design_<n>/`; el **juez** como criterios para comparar dos diseños; el **verificador** para saber qué *debería* existir; y el **corrector** para conocer la regla a la que debe ajustar cada corrección. Las partes condicionales y las reglas de verificación viven en ficheros aparte (ver `reglas-complejas.md`, `tests-e2e.md`, `validacion.md`), referenciados desde `README.md`.

> **REQUIRED — coherencia con los skills técnicos.** Los §2, §3 y §5 **resumen** reglas cuya fuente de verdad son `k-validaciones`, `k-sistemas` y `k-secure-coding` (solo el diseñador carga esos skills; el juez, el verificador y el corrector solo ven este contrato). Si se modifica algo de validaciones/reglas/capas aquí o en esos skills, **MUST** mantenerse sincronizados — como exige `CLAUDE.md` para `architecture.md` ↔ `architecture-rules.md`.

---

## 1. Qué produce el diseño (estructura de salida)

El diseñador escribe, dentro de su carpeta `design_<n>/`, un diseño completo y autosuficiente:

- `design.md` — índice del diseño, con frontmatter `type: design`. Contiene firmas Java, comentarios descriptivos, matriz de trazabilidad `Origen spec` → V/R/U → ubicación y un **resumen estructural** de cada fichero XML. **No** duplica el XML completo: cada XML vive en su fichero.
- `domains/<Entidad>.xml` — uno por entidad. XML completo, válido contra `domain-models.xsd` (ver `validacion.md`).
- `views/<Fichero>.xml` — uno por `<action-view>` (regla "un `<action-view>` por fichero", §6). XML completo, válido contra `object-views.xsd`.
- `menus.xml` — XML con los `<menuitem>` a añadir al fichero único del proyecto. Válido contra `object-views.xsd`.
- `test-e2e-desc.md` — tests E2E (ver `tests-e2e.md`), si el spec tiene escenarios.
- `rules/R-<Entidad>-NNN.md` — solo para reglas de negocio complejas (ver `reglas-complejas.md`).

(Cuando el diseño gana el torneo, su carpeta `design_<n>/` se renombra a `design/`.) Los ficheros XML de `domains/`, `views/` y `menus.xml` están **completos y materializados**: `/sdd-implementer` los **copia verbatim** a su ubicación final en `src/main/...` (los `<menuitem>` de `menus.xml` se fusionan en el fichero único del proyecto). **MUST NOT** modificarlos, reescribirlos ni regenerarlos durante la implementación: se colocan en su sitio **tal cual están**. El diseño no inventa nada que no se vaya a usar tal cual.

### 1.1 Diseño vs implementación: qué SÍ va y qué NO va

Un diseño describe **la estructura** del software (qué ficheros existen, qué clases, qué métodos con qué firma, qué vistas, qué acciones, dónde va cada regla) y materializa **como ficheros XML reales** todas las partes declarativas. **No contiene el código Java de implementación** — eso lo escribe `/sdd-implementer`.

| Va en… | Contenido |
|--------|-----------|
| `design.md` | Lista de ficheros a crear/modificar en el proyecto real; FQN de cada clase y firma completa de cada método con comentario descriptivo del cuerpo (qué reglas aplica, qué llamadas hace, qué efectos colaterales); resumen estructural de cada XML generado; matriz de trazabilidad `Origen spec` → V/R/U → ubicación. |
| `domains/*.xml` | XML completo de cada entidad (campos, tipos, relaciones, enumerados, finders). Es declarativo y va al 100%. |
| `views/*.xml` | XML completo de `<grid>`, `<form>`, `<cards>`, `<action-method>`, `<action-attrs>`, `<action-validate>`, `<action-condition>`, `<action-record>`, `<action-group>`, `<action-view>` — con todos sus campos, panels, condiciones y mensajes literales. |
| `menus.xml` | XML completo de los `<menuitem>` a añadir al `menus.xml` único del proyecto. |
| `test-e2e-desc.md` | Tests E2E `T-NNN` en lenguaje de negocio Given/When/Then (sin código ni selectores). |

**MUST NOT** en cualquier parte del diseño:

- **MUST NOT** incluir cuerpos de métodos Java implementados. Nada de `validateInsert` con su lógica, nada de `for`/`if` reales, nada de `messages.add(...)` con strings literales dentro de un método. Solo firmas + comentario descriptivo.
- **MUST NOT** incluir mensajes de error literales para validaciones Java — se describe el contenido que debe transmitir (valor recibido, dominio válido), no el literal. (Los literales de `<action-validate>` XML sí se escriben porque el XML va completo; y los mensajes que cita `test-e2e-desc.md` se toman tal cual del spec/vista.)
- **MUST NOT** inventar elementos que no estén en la especificación. Si el spec no menciona una pantalla, un campo o una regla, **MUST NOT** añadirse.
- **MUST NOT** usar como referencia el código de `expedientes`/`tiposexpedientes`/`tramites` ni JPQL real.

### 1.2 XML real vs descripción markdown

Los XML son **ficheros reales** dentro de `design_<n>/` (p.ej. `design_<n>/domains/Bar.xml`), escritos directamente por el diseñador. El `design.md` **no** los lleva inline: contiene, por cada fichero XML, un **resumen estructural** corto (qué vistas declara, qué acciones, propósito); el XML completo vive en su fichero.

Para el código Java es al revés: **no** se generan ficheros `.java` — solo firmas y comentarios dentro del `design.md`.

---

## 2. Conversión spec → taxonomía técnica V/R/U (con trazabilidad)

El spec ya trae sus reglas **clasificadas y numeradas** en categorías de negocio. El diseño las **convierte** a la taxonomía técnica V/R/U y las ubica en su capa. La numeración V/R/U es **local** por entidad o pantalla, empezando en `001`; el prefijo (`V-<Entidad>-NNN`, `U-<slug-pantalla>-NNN`) garantiza unicidad global.

**Mapeo spec → V/R/U** (correlación natural; la decisión final depende del **efecto real**: bloquea → V, actúa → R, cambia formulario → U):

- `RES-<Entidad>-NNN` (restricción, invariante de entidad) → **V** aplicable a todas las acciones: declarativa en el modelo si un atributo la cubre (única, obligatoria, min/max); si no es declarable (p.ej. comparación entre campos), en `validate*` de **todas** las operaciones aplicables (ver `k-validaciones/restricciones.md`).
- `VAL-<Entidad>-NNN` (validación de una acción) → **V**, anclada a la operación correspondiente.
- `RN-<Entidad>-NNN` (regla de negocio) → **R**. El atributo `fase` del spec (`antes_de_commit`/`después_de_commit`) orienta el momento `Antes`/`Después` de la R.
- `RUI-<pantalla>-<vista>-NNN` (regla de UI) → **U**, anclada a la vista de la pantalla que su propio ID indica (en el spec cada regla de UI pertenece a **una** vista; si la misma conducta existe en varias vistas, el spec trae una `RUI-` por vista y cada una se materializa como su propia U).
- `CC-<Entidad>-NNN` (campo calculado) → campo con **origen `servidor`** + una **R** con momento `Antes` que lo asigna/recalcula (si `momento: escritura`), o campo derivado de solo lectura (si `momento: lectura`). Si `sobreescribible` lista roles, documentarlo en la R.

**Trazabilidad obligatoria — columna/atributo `Origen spec`:**

- Cada V/R/U del diseño declara su `Origen spec`: la lista de IDs `RES-`/`VAL-`/`RN-`/`RUI-`/`CC-` que la originaron (`VAL-SolicitudCertificado-001` o `RN-SolicitudCertificado-002, RES-SolicitudCertificado-001`), o `—` si el diseño la añadió por necesidad técnica (no provenía de ninguna regla del spec — señal de "repásala").
- **Cobertura inversa**: cada `RES-`/`VAL-`/`RN-`/`RUI-`/`CC-` del spec **MUST** aparecer como Origen de al menos una V/R/U (o, para un `CC-` de lectura, de un campo del modelo); en otro caso **MUST** listarse en la sección **"Reglas del spec descartadas"** del `design.md` con justificación.
- Si el efecto real de una regla contradice su categoría en el spec (p.ej. una `RUI-` que en realidad bloquea), mapéala según su efecto real y documenta el motivo.

- ✅ CORRECTO: `V-SolicitudCertificado-002` con Origen spec `VAL-SolicitudCertificado-004, RES-SolicitudCertificado-002`
- ❌ INCORRECTO: `V-001` (sin entidad), `V-solicitudCertificado-001` (entidad no en PascalCase), `U-MisSolicitudes-001` (slug de pantalla debe ir en kebab-case), Origen spec `VAL-004` (ID del spec sin entidad: formato global antiguo), Origen spec `VAL-SolicitudCertificado-4` (sin 3 dígitos), celda vacía (debe ir `—` si fue añadida por el diseño)

---

## 3. Clasificación `cliente`/`servidor` por campo (apoyada en el spec)

El diseño clasifica el **origen del valor** de cada campo en `cliente` (lo aporta el usuario; validable con V; permitido en `AllowProperties`) o `servidor` (lo dicta el servidor — timestamps, estados iniciales, contadores, snapshots, valores calculados — asignado/recalculado **incondicionalmente** en `*ServiceImpl.insert/update`). Ver `[[k-secure-coding]]` §3.1.

La clasificación **no se inventa**: se deriva del spec, que ya da la información de negocio:

- Un campo listado en alguna línea `**Input AllowProperties:**` de una acción → `cliente` para esa acción.
- Un `CC-` (campo calculado) → siempre `servidor`.
- Un campo que **nunca** aparece en ninguna línea `Input AllowProperties` y que el servidor fija (estado, auditoría, snapshots) → `servidor`.
- Un campo **inmutable** (aparece en `Crear` pero no en `Modificar`) → `cliente` en alta, excluido de la whitelist de `update`.

**Coherencia obligatoria:** cada campo `servidor` **DEBE** estar respaldado por al menos una `R-<Entidad>-NNN` con momento `Antes` que lo asigna — salvo los derivados de solo lectura (`CC-` con `momento: lectura`), que no se persisten (documentar el cálculo en notas). Un campo `cliente` **NO** debe aparecer asignado por una R-Antes-de-Crear (eso lo convertiría implícitamente en `servidor`).

---

## 4. Cobertura total de las reglas del spec

**REQUIRED**: **todas** las reglas del spec — `RES-`/`VAL-`/`RN-`/`RUI-`/`CC-` — deben quedar **ubicadas** en el diseño (convertidas a una V/R/U con una entrada en la matriz de trazabilidad apuntando a un método o acción concreta, con un comentario que describa su lógica) **o** listadas en "Reglas del spec descartadas" con justificación. Si alguna regla no tiene ni ubicación ni justificación, el diseño está incompleto.

---

## 5. Mapeo de capas

Cada categoría de regla tiene su capa de implementación:

- **`V-<Entidad>-NNN`** (validación) — **el servidor es siempre la fuente de verdad** (`k-validaciones`): una V que solo exista en cliente no existe (la siguiente llamada por API la salta).
  - Capa servidor (**obligatoria** para toda V): atributo declarativo del modelo XML (`required`, `unique`, `min`, `max`) si lo cubre; en otro caso `validateInsert`/`validateUpdate`/`validateRemove` del `*ServiceImpl`. Las vistas la invocan antes de guardar/borrar vía las acciones globales `remote-validationSave-action`/`remote-validationDelete-action` de `DefaultModelController`.
  - Capa cliente (**opcional**, solo UX): duplicar en `<action-validate>`/`<action-condition>` las V de campo individual o entre campos del mismo registro, para feedback sin roundtrip. **MUST NOT** ser la única capa de una V.
  - **Excepción — entidad detalle editada en form modal (`panel-related`): la capa cliente es REQUIRED y lo más completa posible.** `save-modal`/`delete-modal` no llaman al servidor y el modal **MUST NOT** llevar `remote-validation*` (el maestro puede no existir en BD); la validación de servidor del detalle solo corre al guardar el **maestro** (`ModelServiceValidationWalker`). Por eso el `Local-validate*` del modal **MUST** duplicar **todas** las V del detalle evaluables en cliente (obligatorios, formatos, comparaciones entre campos y con `__parent__`) — es el único aviso al usuario antes de cerrar el modal. La capa servidor del detalle sigue existiendo (en el `validate*` del servicio del detalle); solo cambia cuándo corre. Ver `k-vistas/forms.md` §"Form modal" y `k-validaciones/validaciones.md` §3.
- **`R-<Entidad>-NNN`** (regla de negocio): servidor, como método `fireActionRule_*` del `*ServiceImpl` invocado desde `insert`/`update`/`remove`/operación custom, **Antes** de `repository.save/remove` si escribe en el mismo registro o **Después** si tiene efectos colaterales. La persistencia es siempre `repository.save/remove` — **MUST NOT** `super.insert/update/remove` (ver `k-sistemas/servicios.md`).
- **`U-<slug-pantalla>-NNN`** (regla de UI): vista, como atributo `showIf`/`hideIf`/`readonlyIf`/`requiredIf` en `<field>`/`<panel>`, o `<action-attrs>`/`<action-record>` referenciado desde `onNew`/`onLoad`/`onChange`.

---

## 6. Reglas arquitectónicas obligatorias

- **Un `<action-view>` por fichero** (regla de `k-sistemas`): cada `<action-view>` vive en su propio fichero `<NombreEntidad>[-<discriminador>].xml` junto con el grid, el form y las acciones que solo usa él. Excepción: las vistas de búsqueda/referencia (`Ref@…-grid` + `Ref@…-form`) van juntas en `<NombreEntidad>-ref.xml`. Si la entidad tiene un único `<action-view>` principal, el fichero es `<NombreEntidad>.xml`.

  - ✅ CORRECTO: `Bar.xml` (entidad con un solo `<action-view>` principal).
  - ✅ CORRECTO: `Bar-Pendiente.xml` (un `<action-view>` discriminado por estado).
  - ✅ CORRECTO: `Bar-ref.xml` (`Ref@…-grid` + `Ref@…-form` juntos).
  - ❌ INCORRECTO: `BarGridPendiente.xml` (sin guion-discriminador; concatena entidad y rol)
  - ❌ INCORRECTO: `Bar.xml` con dos `<action-view>` dentro (regla "uno por fichero" violada)

- **Menús en fichero único** (regla de `k-vistas/menus.md`): **todos** los `<menuitem>` del proyecto viven en el único fichero `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml`. Los menús del subsistema nuevo se **añaden** allí; **MUST NOT** crearse ficheros `menus-<subsistema>.xml`. En la tabla "Ficheros a crear o modificar" del `design.md`, los menús aparecen como **Modificar** `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml`. El diseño produce un `menus.xml` con la **porción** a fusionar.

  - ✅ CORRECTO: fila en la tabla `Modificar | src/main/java/com/educaflow/secretariavirtual/menus/menus.xml | k-vistas (menus.md) | Añadir menú del subsistema foo`
  - ❌ INCORRECTO: fila `Crear | src/main/java/com/educaflow/subsystem/foo/menus/menus-foo.xml` (crea un fichero de menús nuevo por subsistema)
- **MUST NOT** crear módulos Guice para `ModelService` — `ModelServiceFactory` los descubre automáticamente.
- **Cableado Guice no trivial**: para un objeto que NO es `ModelService` y cuya construcción no es trivial (necesita un `Provider`, binding explícito, o dependencias de configuración/runtime y no de otros beans inyectables), el diseño **MUST** describir su módulo `module/<Subsistema>Module.java` siguiendo `[[k-guice]]` (forma de binding y, si procede, `Provider`). El caso `ModelService` sigue sin módulo.
- **MUST NOT** crear listeners JPA para lógica de negocio — esa lógica va en el servicio como `fireActionRule_*`.
- **Botones de formulario = patrón `buttons-panel` de `k-vistas/forms.md`, nunca la toolbar nativa de Axelor.** Cuando la spec dice "los botones estándar" (o `*(solo los botones estándar: Guardar, Cancelar, Borrar)*`) se refiere a un término de **negocio**: el trío de acciones que todo formulario de mantenimiento tiene por defecto. Su traducción **técnica** en este proyecto es **siempre** el patrón fijo de `k-vistas/forms.md` — **nunca** los atributos nativos del `<form>` de Axelor. En concreto:
  - Los atributos `canAttach`/`canBack`/`canDelete`/`canNew`/`canSave`/`canMore` del `<form>` **MUST** ir a `false` (la toolbar nativa **MUST NOT** usarse para guardar/cancelar/borrar).
  - El formulario **MUST** llevar un `<panel name="buttons-panel">` con `<button name="btnDelete">`, `<button name="btnCancel">`, `<button name="btnSave">`, cada uno con su `<action-group>` propio que termina en la acción real del framework (`delete`/`back`/`save` en el form principal; `delete-modal`/`close`/`save-modal` en un form modal).
  - En el form **principal**, el `<action-group>` de `btnSave` **MUST** terminar con `<action name="back"/>` (o `force-back`) **después** de `<action name="save"/>`: cierra la ventana aunque `save` sea un no-op (nada cambiado) y `canBackOnSave` no dispare. Ver `k-vistas/forms.md`.
  - Cualquier validación de servidor antes de guardar se engancha en el `<action-group>` del **botón** `btnSave` (antes de `<action name="save"/>`), **no** en el atributo `onSave` del `<form>`.
- **Validación remota de save/delete = acciones globales, nunca por entidad.** En el **form principal**, el `<action-group>` de `btnSave` incluye la acción global `remote-validationSave-action` antes de `save`, y el de `btnDelete` la acción global `remote-validationDelete-action` antes de `delete` (las define una única vez `DefaultModelController` — ver `k-validaciones/validaciones.md` §5). **MUST NOT** declarar en `views/*.xml` un `<action-method>` de validación por entidad (`…-Remote-validateSave-action`) ni métodos `validateSave`/`validateDelete` en el controlador de la entidad. Solo las **operaciones custom** (`aprobar`, `rechazar`…) llevan su `Remote-validate<Operacion>-action` y su `@CallMethod` propios. En el form **modal** de un detalle (`save-modal`/`delete-modal`) **MUST NOT** usarse `remote-validation*`: allí la validación previa al cierre es la cliente, lo más completa posible (§5).

  - ✅ CORRECTO: `<action-group name="….btnSave-action">` con `Local-validateSave-action` (opcional) → `remote-validationSave-action` → `save` → `back` (o `force-back`).
  - ❌ INCORRECTO: `<action-method name="subsysFoo.Main@Bar-Remote-validateSave-action">` llamando a `BarController.validateSave` (patrón sustituido por la acción global).

  - ✅ CORRECTO: `<form ... canAttach="false" canBack="false" canDelete="false" canNew="false" canSave="false" canMore="false" canBackOnSave="true">` + `<panel name="buttons-panel">` con los tres `<button>` (ver `k-vistas/forms.md`).
  - ❌ INCORRECTO: `<form ... canBack="true" canDelete="true" canSave="true" onSave="...">` sin `buttons-panel` — usa la toolbar nativa de Axelor en vez del patrón del proyecto, aunque "funcione".
- **Maquetación del `<form>` = ASCII Layout (`k-vistas/forms.md`).** Antes de escribir cada `<form>`, el diseñador **MUST** maquetar cada panel siguiendo el «Procedimiento de maquetación (ASCII Layout)» de `k-vistas/forms.md`: agrupar los campos por semántica (relacionados en la misma fila), dimensionar cada `colSpan` con la tabla de proporcionalidad (**no** inflarlo: un código/número corto son 2–3 columnas, no 6 ni 12), dibujar cada fila en la rejilla de 12 columnas (**cada fila suma 12**), alinear los bordes de columna entre filas y colocar los botones (secundarios a la izquierda, principales a la derecha). **MUST** incluir el **ASCII Layout** de los paneles no triviales en el resumen estructural de la vista dentro del `design.md`, para poder revisar el layout sin abrir el XML. **MUST NOT** poner `colSpan="6"`/`"12"` por defecto ni dejar campos cortos solos en una fila con hueco injustificado.
  - ✅ CORRECTO: en el `design.md`, junto al resumen de `Bar.xml`, un bloque ` ```aaa...bbbbbb ← code(3)+colOffset(3)+name(6)``` ` y el `<form>` con `colSpan`/`colOffset` que coinciden con él.
  - ❌ INCORRECTO: un `<form>` con todos los `<field>` a `colSpan="6"` o sin `colSpan`, sin ASCII Layout y con campos cortos ocupando media fila.
- **Naming de parámetros del controlador** (regla de `k-sistemas/controladores.md`): cuando una firma del controlador recibe `ActionRequest`/`ActionResponse`, los parámetros **MUST** llamarse `actionRequest` y `actionResponse` (camelCase completo).

  - ✅ CORRECTO: `public void miAccion(ActionRequest actionRequest, ActionResponse actionResponse)`
  - ❌ INCORRECTO: `public void miAccion(ActionRequest req, ActionResponse resp)` (abreviado)
  - ❌ INCORRECTO: `public void miAccion(ActionRequest request, ActionResponse response)` (sin prefijo `action`)

---

## 7. Estructura del diseño que produce el diseñador

El diseñador escribe en su carpeta `design_<n>/` el diseño completo. El **índice** `design.md` tiene esta estructura:

```markdown
# Diseño: <Nombre>

**Objetivo:** <Una frase>
**Capa:** system|subsystem/<nombre>
**Especificación de origen:** .sdd/drafts/{carpeta-iniciativa}/specification.md
**Skills necesarios para la implementación:** k-sistemas, k-code-quality, k-secure-coding, k-vistas (+ `k-guice` si el diseño incluye módulos Guice; + `k-scheduler` si incluye jobs programados)

## Ficheros a crear o modificar

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/foo/domains/Bar.xml` | Crear | k-sistemas (modelos.md) | Entidad Bar |
| `subsystem/foo/views/Bar.xml`   | Crear | k-vistas (forms.md, grids.md) | Vistas de Bar |
| `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` | Modificar | k-vistas (menus.md) | Añadir menú del subsistema |
| ... | | | |

> **Nota para `/sdd-implementer`:** los XML de `domains/`, `views/` y `menus.xml` ya están materializados en la carpeta `design/`. **MUST NOT** modificarlos, reescribirlos ni regenerarlos: se **copian verbatim** a su ubicación final (`menus.xml` se fusiona en el `menus.xml` único del proyecto). El código Java es lo único que se implementa a partir de las firmas y comentarios del diseño.

## Pasos

### Paso N — <Título>
...

## Frontera de confianza — AllowProperties por acción
...

## Trazabilidad Origen spec → V/R/U → ubicación
...

## Tests
- **Tests unitarios** (JUnit + Mockito): descritos en `test-unit-desc.md` (lo materializa una fase posterior del pipeline).

## Reglas del spec descartadas
...

## Notas y supuestos
<decisiones tomadas ante ambigüedades del spec, para que el juez y el verificador las vean>
```

### 7.1 Tareas internas del diseñador (en orden)

1. **Leer la especificación dos veces.** 1.ª pasada: enmarcar el alcance (entidades, pantallas). 2.ª pasada: **convertir cada regla del spec** (`RES-`/`VAL-`/`RN-`/`RUI-`/`CC-`) a su V/R/U (§2) y **clasificar cada campo** `cliente`/`servidor` (§3, apoyándose en las líneas `Input AllowProperties` y los `CC-` del spec).
2. **Escribir el `design.md`**: cabecera (Objetivo, Capa, Especificación de origen, Skills necesarios), tabla de ficheros a crear o modificar, y lista de pasos respetando el orden obligatorio (§8).
3. **Escribir los ficheros reales del diseño** en `design_<n>/`:
   - **Dominios** — un fichero `design_<n>/domains/<Entidad>.xml` por entidad, XML completo, válido contra `domain-models.xsd`. En el `design.md`, un resumen estructural corto de cada uno.
   - **Servicios y controladores** — en el `design.md`, clases con FQN y, para cada una, todas las firmas de método (modificadores, retorno, parámetros, excepciones) con comentario descriptivo del cuerpo (qué reglas aplica, qué llamadas hace, qué efectos colaterales). **Sin código Java real dentro.**
   - **Vistas** — un fichero `design_<n>/views/<Fichero>.xml` por `<action-view>`, XML completo válido contra `object-views.xsd`. En el `design.md`, un resumen estructural corto.
   - **Menús** — `design_<n>/menus.xml`, válido contra `object-views.xsd`.
   - **Seguridad** — en el `design.md`, permisos, roles, grupos por nombre y la regla de acceso en lenguaje natural.
   - **Trazabilidad** — matriz con tres bloques (`V-<Entidad>-NNN`, `R-<Entidad>-NNN`, `U-<slug>-NNN`), cada fila con su **`Origen spec`** (IDs `RES-`/`VAL-`/`RN-`/`RUI-`/`CC-` o `—`) y su **ubicación** (clase.método o fichero XML + nombre de acción), demostrando que **toda regla del spec está ubicada** según el mapeo de capas de §5. Las reglas del spec que no se mapeen van a "Reglas del spec descartadas" con justificación.
   - **Partes condicionales** — `rules/R-*.md` (ver `reglas-complejas.md`) y `test-e2e-desc.md` (ver `tests-e2e.md`), cuando apliquen.
4. **Aplicar el checklist (§9) y corregir antes de terminar.** El diseñador NO debe dar el diseño por terminado hasta que todos los puntos del checklist estén satisfechos.

---

## 8. Reglas para los pasos

Cada paso del `design.md` debe:

- Tener un título claro.
- Indicar qué se va a crear o modificar **a nivel de estructura**, no a nivel de implementación.
- Para **dominios**: escribir el fichero `design_<n>/domains/<Entidad>.xml` (XML completo, válido contra `domain-models.xsd`) y resumirlo en el paso.
- Para **servicios/controladores**: clase con FQN y, para cada método, firma completa + comentario descriptivo del cuerpo. **MUST NOT** incluir el cuerpo implementado.
- Para **vistas**: escribir el fichero `design_<n>/views/<Fichero>.xml` (XML completo, válido contra `object-views.xsd`) y resumirlo en el paso.
- Para **menús**: escribir `design_<n>/menus.xml` con los `<menuitem>`.
- Para **seguridad**: permisos, roles, grupos y reglas descritas en lenguaje natural.
- Ser lo suficientemente pequeño para implementarse y verificarse de forma independiente (≤ 30 minutos).
- Indicar qué verificar al final (¿compila?, ¿qué grep confirma que está bien?).

**Orden obligatorio de los pasos:**

1. **Ficheros estáticos y recursos** (si los hay) — plantillas PDF, esquemas XSD, certificados.
2. **Dominios** — un fichero `design_<n>/domains/<Entidad>.xml` por entidad.
3. **Servicios** — interfaz `<Entidad>Service` (extiende `ModelService<Entidad>`) + implementación `<Entidad>ServiceImpl` (extiende `DefaultModelService<Entidad>`). Firma completa + comentario del cuerpo para cada método (constructor, CRUD, `validateInsert`/`validateUpdate`/`validateRemove`, `fireActionRule_*`, métodos de negocio).
4. **Repositorios** (si hay queries propias) — `db/repo/` con la lista de finders adicionales (firma + comentario del cuerpo).
5. **Controladores** (si hay lógica de botones) — clase con FQN; para cada `@CallMethod`, firma y comentario que indique en qué método de servicio delega. Parámetros llamados **siempre** `actionRequest` y `actionResponse` (§6).
6. **Módulos Guice** (si hay cableado DI no trivial, §6) — `module/<Subsistema>Module.java`: forma de binding de cada objeto no trivial y, si procede, el `Provider`, según `[[k-guice]]`. Omitir si todo es `ModelService`.
7. **Jobs programados** (si el spec implica una tarea recurrente) — clase del job + entrada `MetaSchedule` (cron, descripción), según `[[k-scheduler]]`.
8. **Vistas** — un fichero XML por `<action-view>` (regla "un `<action-view>` por fichero").
9. **Menús** — modificación del `menus.xml` único del proyecto; en `design_<n>/menus.xml` la porción a fusionar.
10. **Seguridad** — `data-init/input/` con la lista de permisos, roles, grupos y la descripción en lenguaje natural de cada regla de acceso.
11. **Datos iniciales** — catálogos precargados (descripción de qué registros se cargan, no el XML de import).
12. **Verificación final** — compilar y confirmar que arranca sin errores. Comando exacto.

### 8.1 Detalle del paso de servicios (cómo documentar V y R)

Cada firma de `validateInsert`/`validateUpdate`/`validateRemove` (para V-) y de `fireActionRule_*` (para R-) lleva un comentario que describe, **para cada regla ubicada en ese método**:

1. **Identificador** (`V-<Entidad>-NNN` o `R-<Entidad>-NNN`) y su **`Origen spec`** (IDs `RES-`/`VAL-`/`RN-`/`CC-` o `—`).
2. **Lógica resumida** — qué se comprueba (V) o qué hace el sistema (R).
3. Para V: **contenido del mensaje de error** descrito por lo que debe transmitir (valor recibido + dominio válido). **No el literal.**
4. Para R: **momento** (Antes/Después de `repository.save/remove` — nunca `super.*`) y **efectos colaterales** previstos.
5. Si los valores válidos o las dependencias vienen de BD, indicar la fuente (catálogo, repositorio, etc.).

Ejemplo:

```java
// Clase: com.educaflow.subsystem.foo.service.impl.BarServiceImpl
// Método:
public Optional<BusinessMessages> validateInsert(Bar entidad);
//   Aplica:
//     - V-Bar-001 (Origen spec: VAL-Bar-007) alias del HSM: comprueba que el alias exista en el
//       slot indicado. Mensaje debe transmitir: alias recibido + slot recibido + lista de
//       aliases disponibles (del repositorio de aliases del slot, en try/catch para que un
//       fallo de conectividad no bloquee otras validaciones).
//     - V-Bar-002 (Origen spec: RES-Bar-003) longitud del nombre: comprueba 3..50 caracteres.
//       Mensaje debe transmitir: nombre recibido + longitud actual + rango.
```

### 8.2 Detalle del paso de servicios (cómo documentar campos `servidor` — defensa anti mass-assignment)

Para cada R-<Entidad>-NNN con momento `Antes` que asigna un campo clasificado como `servidor` (§3), el comentario del `fireActionRule_*` correspondiente **MUST** documentar explícitamente:

1. Que la asignación es **incondicional** (sin `if (campo == null)`). Ver `[[k-secure-coding]]` §3.3.
2. El origen del valor (`LocalDateTime.now()`, `AuthUtils.getUser().getCentro()`, constante del enum, etc.).
3. Que el cliente NO puede dictar este campo aunque venga relleno en el JSON del endpoint REST genérico.

✅ CORRECTO (comentario en `design.md`):

```java
private void fireActionRule_AsignarFechaCreacion(Bar bar);
//   Aplica R-Bar-001 (Origen spec: CC-Bar-002, campo `fechaCreacion` clasificado `servidor`):
//   asignación INCONDICIONAL `bar.setFechaCreacion(LocalDateTime.now())`.
//   MUST NOT añadir guarda `if (bar.getFechaCreacion() == null)`: permitiría que un
//   atacante por el endpoint REST genérico cuele una fecha falsificada (ver k-secure-coding §3.3).
```

❌ INCORRECTO:

```java
private void fireActionRule_AsignarFechaCreacion(Bar bar);
//   Si fechaCreacion es null, asignar LocalDateTime.now().
```

Para campos inmutables tras crear (típico `fechaCreacion`, `numeroSecuencial`), el `design.md` **MUST** excluirlos de la whitelist `allowPropertiesUpdate` para que el cliente no pueda enviarlos (ver `[[k-secure-coding]]` §3.2, forma whitelist). El spec lo refleja porque esos campos aparecen en la línea `Input AllowProperties` de `Crear` pero **no** en la de `Modificar`.

### 8.3 Sección "Frontera de confianza — AllowProperties por acción"

El `design.md` **MUST** llevar una sección `## Frontera de confianza — AllowProperties por acción` siempre que el diseño declare al menos una acción del servicio invocada desde un `@CallMethod`, con una tabla por cada una de esas acciones. La tabla materializa la decisión de seguridad sobre qué campos del bean acepta esa acción, partiendo de las líneas `**Input AllowProperties:**` del `entity-*.md` del spec. (Si el diseño no tiene ningún `@CallMethod`, la sección se omite.)

> Las reglas de validez (qué forma elegir, qué campos pueden o no estar) viven en `[[k-secure-coding]]` §3. Este apartado solo fija **el formato del documento**; las reglas no se repiten aquí.

**Formato de cada tabla**:

```markdown
### `BarServiceImpl.<accion>` (invocado desde `BarController.<callMethod>`)

Entidad: `Bar`. **Forma elegida**: `createAllowProperties` | `createAllowAllProperties`.
**Origen spec:** `Input AllowProperties` de la acción `<Acción>` de `entity-Bar.md`.

| Campo            | Origen   | En whitelist | Justificación / Ubicación de la asignación              |
|------------------|----------|--------------|---------------------------------------------------------|
| `nombre`         | cliente  | sí           | Input directo del usuario (en `Input AllowProperties`). |
| `fechaCreacion`  | servidor | **NO**       | Asignada en `BarServiceImpl.insert` → `fireActionRule_…`; en `update` no se toca (excluida de la whitelist). |
| `estado`         | servidor | **NO**       | Recalculada en `BarServiceImpl.update` → `fireActionRule_…`. |
```

Si hay alta programática vía DTO (`record`), añadir sub-apartado `### DTO de alta programática` con los campos del record y justificación de cualquier `servidor` que aparezca.

Esta sección es el contrato de seguridad. El verificador la comprueba aplicando `[[k-secure-coding]]` §3, `/sdd-implementer` la usa para generar el `allowPropertiesXxx` real, y los code-reviews humanos la consultan ante cualquier campo nuevo.

---

## 9. Checklist del diseño

El diseñador revisa su diseño contra esta lista y corrige antes de terminar. Si algún punto no se cumple, **MUST NOT** dar el diseño por terminado. (El verificador la vuelve a aplicar; ver `validacion.md`.)

- [ ] ¿Cada paso tiene toda la información para que un implementador entienda qué hay que crear sin leer el resto del diseño?
- [ ] ¿Los nombres de clases, métodos, ficheros y acciones son coherentes entre todos los pasos?
- [ ] ¿Ningún paso contiene placeholders del tipo "TBD", "similar a", "según convenga"? (si los hay, sustituir por contenido concreto)
- [ ] ¿El paso de verificación final incluye el comando exacto de compilación?
- [ ] ¿Existe un fichero `design_<n>/domains/<Entidad>.xml` por entidad, con XML completo y válido contra `domain-models.xsd`?
- [ ] ¿El paso de servicios contiene SOLO firmas de método con comentarios descriptivos del cuerpo, y NO cuerpos implementados? Si hay código Java real (lógica, `if`, `for`, `messages.add(...)` con literales), eliminarlo y dejarlo como comentario.
- [ ] ¿Existe un fichero `design_<n>/views/<Fichero>.xml` por `<action-view>`, con XML completo y válido contra `object-views.xsd`, y su resumen estructural en el `design.md`?
- [ ] ¿Existe `design_<n>/menus.xml`, válido contra `object-views.xsd`?
- [ ] ¿Cada `<action-view>` está en su propio fichero (§6)? Excepción: `Ref@…-grid`+`Ref@…-form` van juntos en `<NombreEntidad>-ref.xml`.
- [ ] ¿La tabla "Ficheros a crear o modificar" lista los menús como "Modificar `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml`", no como un fichero nuevo `menus-<subsistema>.xml`?
- [ ] ¿Los parámetros de los métodos del controlador se llaman `actionRequest` y `actionResponse`?
- [ ] ¿Cada V/R/U tiene su columna **`Origen spec`** con los IDs `RES-`/`VAL-`/`RN-`/`RUI-`/`CC-` que la originaron (que existen realmente en el spec), o `—` si la añadió el diseño?
- [ ] ¿Cada campo de cada dominio está clasificado `cliente` o `servidor` de forma coherente con las líneas `Input AllowProperties` y los `CC-` del spec? ¿Cada `servidor` está respaldado por una R-Antes (salvo derivados de solo lectura) y ningún `cliente` aparece asignado por una R-Antes-de-Crear?
- [ ] ¿Cada `CC-` del spec está reflejado como campo `servidor` + R-Antes (escritura) o campo derivado de solo lectura (lectura)?
- [ ] ¿Cada método en el paso de servicios tiene un comentario que indica qué reglas `V-`/`R-` aplica (con su `Origen spec`), qué lógica ejecuta y qué transmiten los mensajes de error?
- [ ] ¿Cada acción de vista declarada tiene un comentario de su propósito y los campos/condiciones que intervienen?
- [ ] ¿Las reglas están mapeadas a la capa correcta según §5?
- [ ] ¿El diseño tiene la sección `## Frontera de confianza — AllowProperties por acción` con una tabla por cada acción del servicio invocada desde un `@CallMethod`, en el formato de §8.3, y pasando las reglas de `[[k-secure-coding]]` §3?
- [ ] ¿Cada R-<Entidad>-NNN con momento `Antes` que asigna un campo `servidor` documenta asignación **incondicional** (sin `if (campo == null)`) y referencia `[[k-secure-coding]]` §3.3?
- [ ] ¿Ningún cuerpo de método del diseño contiene el anti-patrón `if (campo == null) setCampo(...)` para campos `servidor`?
- [ ] ¿TODAS las reglas `RES-`/`VAL-`/`RN-`/`RUI-`/`CC-` del spec están mapeadas a una V/R/U ubicada (o a un campo del modelo, para `CC-` de lectura), **o** listadas en "Reglas del spec descartadas" con justificación?
- [ ] ¿La matriz de trazabilidad tiene una entrada por cada V/R/U y cada entrada apunta a una clase + método o fichero XML + nombre de acción/atributo y declara su `Origen spec`?
- [ ] ¿Ningún paso crea un módulo Guice para un `ModelService`? (si lo crea, eliminarlo — §6)
- [ ] ¿Ningún paso crea un listener JPA para lógica de negocio? (si lo crea, moverlo al servicio como `fireActionRule_*`)
- [ ] ¿Cada `<form>` de `views/*.xml` tiene `canAttach`/`canBack`/`canDelete`/`canNew`/`canSave`/`canMore` a `false` y un `<panel name="buttons-panel">` con `btnDelete`/`btnCancel`/`btnSave` (patrón de `k-vistas/forms.md`), en vez de la toolbar nativa de Axelor? ¿Ninguna validación de servidor cuelga de un `onSave` del `<form>` en vez del `action-group` de `btnSave`?
- [ ] ¿Cada `<form>` está maquetado según el **ASCII Layout** de `k-vistas/forms.md` (§6) — campos agrupados por semántica, `colSpan` proporcional al contenido y **no** inflado, cada fila suma 12, bordes alineados, botones secundarios a la izquierda y principales a la derecha — y el `design.md` incluye el **ASCII Layout** de los paneles no triviales, coherente con los `colSpan`/`colOffset` del XML?
- [ ] ¿Los `action-group` de `btnSave`/`btnDelete` del form **principal** usan las acciones globales `remote-validationSave-action`/`remote-validationDelete-action` (§6), sin ningún `<action-method>` de validación por entidad para save/delete ni métodos `validateSave`/`validateDelete` en el controlador? ¿El de `btnSave` termina con `<action name="back"/>` (o `force-back`) después de `save` para cerrar la ventana?
- [ ] ¿En cada form **modal** de detalle (`save-modal`/`delete-modal`): (a) ningún `action-group` del modal incluye `remote-validation*`, y (b) el `Local-validate*` del modal duplica **todas** las V del detalle evaluables en cliente (§5)? Las V del detalle no evaluables en cliente quedan en el `validate*` del servicio del detalle (corren al guardar el maestro).
- [ ] ¿Cada paso es lo suficientemente pequeño para implementarse y verificarse en ≤ 30 minutos?
- [ ] ¿Los pasos respetan el orden obligatorio de §8?
- [ ] ¿El diseño referencia el `specification.md` en la cabecera?
- [ ] ¿El `design.md` tiene la sección `## Tests` que referencia `test-unit-desc.md` (tests unitarios)?
- [ ] ¿El diseño respeta todas las guías de `design-guidelines.md` (si existe)? Si alguna no se ha podido respetar por incompatibilidad con el spec, ¿está documentada en "Notas y supuestos"?

---

## 10. El `design.md`: estructura y secciones obligatorias

El `design.md` es el índice del diseño, con frontmatter `type: design`:

```
---
type: design
---

{contenido del diseño, con resumen estructural por cada XML — no el XML inline}
```

El `design.md` **no contiene** los XML completos inline (esos viven en sus ficheros); en su lugar contiene, por cada fichero XML generado, una entrada con su ruta y el resumen estructural (vistas, acciones, propósito), más la matriz de trazabilidad `Origen spec → V/R/U → ubicación`, la sección "Frontera de confianza — AllowProperties por acción" (§8.3) y, si aplica, "Reglas del spec descartadas". Incluye además una sección "Tests" que referencia `test-unit-desc.md` (tests unitarios), materializado en una fase posterior del pipeline. Las decisiones tomadas ante ambigüedades van en "Notas y supuestos".
