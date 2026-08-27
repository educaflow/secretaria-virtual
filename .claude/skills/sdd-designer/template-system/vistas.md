# Parte del diseño — vistas (`views/*.xml` + `menus.xml`)

Define **el contrato de las vistas del diseño**: las reglas de creación que los ficheros `views/*.xml` y `menus.xml` de la carpeta de diseño deben cumplir (§1), el checklist del diseñador (§2) y las comprobaciones y detectores del verificador, incluida la **auditoría de layout (ASCII Layout)** (§3). La **técnica** no se repite aquí: la fuente de verdad de las convenciones (nombres de vistas y acciones, plantillas de form/grid, PI `sv-*`, procedimiento **ASCII Layout**) es el skill `k-vistas` — en particular `forms.md`.

Lo lee, según su rol (`README.md` §2): el **diseñador** (aplica §1 y pasa §2 al producir cada vista); el **juez** y el **enriquecedor** (criterios sobre vistas al comparar diseños o decidir mejoras); el **verificador** (aplica §3); el **corrector** (solo si un fallo/mejora afecta a una vista, para ajustar la corrección a la regla de §1 incumplida).

> **REQUIRED — coherencia con `k-vistas`.** Los apartados de este fichero **resumen** reglas cuya fuente de verdad son los skills `k-vistas` (`SKILL.md`, `forms.md`, `actions.md`, `menus.md`) y `agent_docs/view-rules.md`. Si se modifica algo aquí o allí, **MUST** mantenerse sincronizados — el mismo mandato que la cabecera de `design-contract.md` establece para los skills técnicos.

---

## 1. Reglas de creación

### 1.1 Organización de ficheros: un `<action-view>` por fichero

Cada `<action-view>` vive en su propio fichero `views/{Variante}-{Entidad}.xml` (nomenclatura de `k-vistas` SKILL.md), junto con el grid, el form y las acciones que solo usa él; el fichero contiene el bloque maestro y, si aplica, los bloques de sus detalles. Excepción: las vistas de referencia `Ref@…-grid` + `Ref@…-form` no tienen `<action-view>` y van juntas en `views/Ref-{Entidad}.xml`.

- ✅ CORRECTO: `Main-Bar.xml` (el `<action-view>` de mantenimiento con su grid, form, acciones y los bloques de sus detalles `Bar.Hijo`).
- ✅ CORRECTO: `Pendiente-Bar.xml` (variante discriminada por estado, con su propio `<action-view>`).
- ✅ CORRECTO: `Ref-Bar.xml` (`Ref@Bar-grid` + `Ref@Bar-form` juntos, sin `<action-view>`).
- ❌ INCORRECTO: `Bar.xml`, `Bar-Pendiente.xml` o `Bar-ref.xml` (nomenclatura antigua; no siguen el patrón `{Variante}-{Entidad}.xml`)
- ❌ INCORRECTO: `Main-Bar.xml` con dos `<action-view>` dentro (regla "uno por fichero" violada)

### 1.2 Menús en el fichero único del proyecto

**Todos** los `<menuitem>` del proyecto viven en el único fichero `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` (regla de `k-vistas/menus.md`). Los menús del subsistema nuevo se **añaden** allí; **MUST NOT** crearse ficheros `menus-<subsistema>.xml`. En la tabla "Ficheros a crear o modificar" del `design.md`, los menús aparecen como **Modificar** ese fichero único. El diseño produce un `menus.xml` con la **porción** a fusionar.

- ✅ CORRECTO: fila en la tabla `Modificar | src/main/java/com/educaflow/secretariavirtual/menus/menus.xml | k-vistas (menus.md) | Añadir menú del subsistema foo`
- ❌ INCORRECTO: fila `Crear | src/main/java/com/educaflow/subsystem/foo/menus/menus-foo.xml` (crea un fichero de menús nuevo por subsistema)

### 1.3 Marcadores de bloque y sección (PI `sv-*`)

Cada bloque de un fichero de vistas lleva las **cinco PI** una vez cada una y en orden — `<?sv-view?>` → `<?sv-primary-actions?>` → `<?sv-validations?>` → `<?sv-rules?>` → `<?sv-remotes?>` — aunque alguna sección quede vacía, y cada acción va tras la PI de su sección (`k-vistas` SKILL.md §"Marcadores de bloque"; `agent_docs/view-rules.md` Categoría 3). **MUST NOT** rotular bloques con banners de comentarios.

### 1.4 Botones de formulario: patrón `buttons-panel`, nunca la toolbar nativa

Cuando la spec dice "los botones estándar" (o `*(solo los botones estándar: Guardar, Cancelar, Borrar)*`) se refiere a un término de **negocio**: el trío de acciones que todo formulario de mantenimiento tiene por defecto. Su traducción **técnica** en este proyecto es **siempre** el patrón fijo de `k-vistas/forms.md` — **nunca** los atributos nativos del `<form>` de Axelor:

- Los atributos `canAttach`/`canBack`/`canDelete`/`canNew`/`canSave`/`canMore` del `<form>` **MUST** ir a `false` (la toolbar nativa **MUST NOT** usarse para guardar/cancelar/borrar).
- El formulario **MUST** llevar un `<panel name="buttons-panel">` con `<button name="btnDelete">`, `<button name="btnCancel">`, `<button name="btnSave">`, cada uno con su `<action-group>` propio que termina en la acción real del framework (`delete`/`back`/`save` en el form principal; `delete-modal`/`close`/`save-modal` en un form modal).
- Cualquier validación de servidor antes de guardar se engancha en el `<action-group>` del **botón** `btnSave` (antes de `<action name="save"/>`), **no** en el atributo `onSave` del `<form>`.

- ✅ CORRECTO: `<form ... canAttach="false" canBack="false" canDelete="false" canNew="false" canSave="false" canMore="false" canBackOnSave="true">` + `<panel name="buttons-panel">` con los tres `<button>` (ver `k-vistas/forms.md`).
- ❌ INCORRECTO: `<form ... canBack="true" canDelete="true" canSave="true" onSave="...">` sin `buttons-panel` (usa la toolbar nativa de Axelor en vez del patrón del proyecto, aunque "funcione")

### 1.5 Validación remota global y cierre `save` → `back` (form principal)

La validación remota de save/delete son las **acciones globales** `remote-validationSave-action`/`remote-validationDelete-action` (las define una única vez `DefaultModelController` — ver `k-validaciones/validaciones.md` §5), **nunca** un `<action-method>` de validación por entidad:

- En el form **principal**, el `<action-group>` de `btnSave` incluye `remote-validationSave-action` antes de `save`, y el de `btnDelete` incluye `remote-validationDelete-action` antes de `delete`.
- **MUST NOT** declarar en `views/*.xml` un `<action-method>` de validación por entidad (`…-Remote-validateSave-action`) ni métodos `validateSave`/`validateDelete` en el controlador de la entidad. Solo las **operaciones custom** (`aprobar`, `rechazar`…) llevan su `Remote-validate<Operacion>-action` y su `@CallMethod` propios.
- El `<action-group>` de `btnSave` **MUST** terminar con `<action name="back"/>` (o `force-back`) **después** de `<action name="save"/>`: cierra la ventana aunque `save` sea un no-op (nada cambiado) y `canBackOnSave` no dispare (ver `k-vistas/forms.md`).

- ✅ CORRECTO: `<action-group name="….btnSave-action">` con `Local-validateSave-action` (opcional) → `remote-validationSave-action` → `save` → `back` (o `force-back`).
- ❌ INCORRECTO: `<action-method name="subsysFoo.Main@Bar-Remote-validateSave-action">` llamando a `BarController.validateSave` (patrón sustituido por la acción global)

### 1.6 Forms modales de detalle (`save-modal`/`delete-modal`)

En el form **modal** de un detalle editado desde un `panel-related` (razón de capas en `design-contract.md` §5):

- **MUST NOT** usarse `remote-validation*` en ningún `<action-group>` del modal (el maestro puede no existir aún en BD; `save-modal`/`delete-modal` no llaman al servidor).
- El `Local-validate*` del modal **MUST** duplicar **todas** las V del detalle evaluables en cliente (obligatorios, formatos, comparaciones entre campos y con `__parent__`) — es el único aviso al usuario antes de cerrar el modal. Ver `k-vistas/forms.md` §"Form modal" y `k-validaciones/validaciones.md` §3.

### 1.7 Maquetación del `<form>`: ASCII Layout

Antes de escribir cada `<form>`, el diseñador **MUST** maquetar cada panel siguiendo el «Procedimiento de maquetación (ASCII Layout)» de `k-vistas/forms.md`: agrupar los campos por semántica (relacionados en la misma fila), dimensionar cada `colSpan` con la tabla de proporcionalidad (**no** inflarlo: un código/número corto son 2–3 columnas, no 6 ni 12), dibujar cada fila en la rejilla de 12 columnas (**cada fila suma 12**), alinear los bordes de columna entre filas, colocar los botones (secundarios a la izquierda, principales a la derecha) y, si hay `showIf`, dibujar **un ASCII Layout por estado**.

**MUST** incluir el **ASCII Layout** de los paneles no triviales en el resumen estructural de la vista dentro del `design.md`, para poder revisar el layout sin abrir el XML. **MUST NOT** poner `colSpan="6"`/`"12"` por defecto ni dejar campos cortos solos en una fila con hueco injustificado.

- ✅ CORRECTO: en el `design.md`, junto al resumen de `Main-Bar.xml`, un bloque ` ```aaa...bbbbbb ← code(3)+colOffset(3)+name(6)``` ` y el `<form>` con `colSpan`/`colOffset` que coinciden con él.
- ❌ INCORRECTO: un `<form>` con todos los `<field>` a `colSpan="6"` o sin `colSpan`, sin ASCII Layout y con campos cortos ocupando media fila.

### 1.8 Vistas existentes que se modifican

Una vista existente que el diseño cambia (fila `Acción: Modificar`) sigue la regla del **fichero completo resultante** de `design-contract.md` §1.3: el fichero de `design_<n>/views/` es el real de `src/main/...` como base más el delta, conservando todo lo no cubierto por el delta o por "Eliminaciones declaradas". La auditoría **ASCII Layout** (§1.7 y §3.g) se aplica al `<form>` **resultante** — los campos preexistentes también se dibujan; recolocarlos sin causa en el delta es un fallo de mínima intrusión.

---

## 2. Checklist de vistas

El diseñador lo aplica antes de dar el diseño por terminado (**MUST NOT** terminarlo con algún punto sin cumplir); el verificador lo reaplica mediante §3.

- [ ] ¿Cada `<action-view>` está en su propio fichero `views/{Variante}-{Entidad}.xml` (§1.1)? ¿Las vistas `Ref@…` van juntas en `Ref-{Entidad}.xml`?
- [ ] ¿La tabla "Ficheros a crear o modificar" del `design.md` lista los menús como "Modificar `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml`", no como un fichero nuevo `menus-<subsistema>.xml` (§1.2)?
- [ ] ¿Cada bloque de cada fichero de vistas lleva las cinco PI `sv-*` una vez y en orden, con cada acción tras la PI de su sección (§1.3)?
- [ ] ¿Cada `<form>` tiene `canAttach`/`canBack`/`canDelete`/`canNew`/`canSave`/`canMore` a `false` y un `<panel name="buttons-panel">` con `btnDelete`/`btnCancel`/`btnSave` (§1.4)? ¿Ninguna validación de servidor cuelga de un `onSave` del `<form>`?
- [ ] ¿Los `action-group` de `btnSave`/`btnDelete` del form **principal** usan las acciones globales `remote-validation*` (§1.5), sin ningún `<action-method>` de validación por entidad para save/delete? ¿El de `btnSave` termina con `back`/`force-back` después de `save`?
- [ ] ¿En cada form **modal** de detalle: (a) ningún `action-group` incluye `remote-validation*`, y (b) el `Local-validate*` duplica todas las V del detalle evaluables en cliente (§1.6)?
- [ ] ¿Cada `<form>` está maquetado según el **ASCII Layout** de `k-vistas/forms.md` (§1.7) — dibujado **antes** del XML, campos agrupados por semántica, `colSpan` proporcional y no inflado, cada fila suma 12, bordes alineados, botones bien colocados, un dibujo por estado `showIf` — y el `design.md` incluye el ASCII Layout de los paneles no triviales, coherente con los `colSpan`/`colOffset` del XML?

---

## 3. Verificación de las vistas

Lo aplica el **verificador** sobre `design/views/*.xml`, `design/menus.xml` y los resúmenes estructurales del `design.md` (lo invoca `validacion.md` §2.f). Cada punto que no se cumpla es un **fallo** a reportar con su ubicación; el **corrector** ajusta cada corrección a la regla de §1 correspondiente. La validación XSD ya la cubre `validate.sh` (`validacion.md` §1) — aquí van las comprobaciones de convención y layout.

- **a) Estructura de ficheros y PI** (§1.1, §1.2, §1.3). Un `<action-view>` por fichero con nomenclatura `{Variante}-{Entidad}.xml`; `Ref@…` juntos en `Ref-{Entidad}.xml`; los menús como modificación del fichero único; las cinco PI `sv-*` por bloque, en orden.
- **b) Botones de formulario** (§1.4). Cada `<form>` con los `can*` a `false` y su `buttons-panel`; un `<form>` con algún `can(Back|Delete|Save)="true"` o con `onSave` en vez de validación en el `action-group` de `btnSave` es un **fallo bloqueante**, aunque funcione. Detector rápido:
  ```bash
  grep -nE '<form .*can(Back|Delete|Save)="true"' .sdd/drafts/{iniciativa}/design/views/*.xml
  ```
  Cualquier coincidencia es un fallo a reportar.
- **c) Validación remota por entidad** (§1.5). Un `<action-method>` de validación por entidad para save/delete es un fallo (la corrección es sustituirlo por la acción global). Detector:
  ```bash
  grep -nE 'Remote-validate(Save|Delete)-action' .sdd/drafts/{iniciativa}/design/views/*.xml
  ```
  Cualquier coincidencia es un fallo a reportar.
- **d) Coherencia acción ↔ método** (`k-vistas/actions.md` §"Convención de nombres", patrón `Remote-{nombreFuncionJava}`). En cada `<action-method>` el segmento `Remote-{X}` del atributo `name` **MUST** coincidir con el `method="X"` del `<call>`; para la validación de una operación custom eso implica que el método se llame `validate<Operacion>` (nunca `validarAntesDe<Operacion>` ni variantes). Detector:
  ```bash
  for f in .sdd/drafts/{iniciativa}/design/views/*.xml; do
    sed -n 's/.*-Remote-\([A-Za-z0-9_]*\)-action".*/A \1/p; s/.*<call [^>]*method="\([A-Za-z0-9_]*\).*/M \1/p' "$f" \
    | awk -v f="$f" '$1=="A"{e=$2} $1=="M"{if(e!="" && $2!=e) print f": la accion Remote-"e"-action llama al metodo "$2; e=""}'
  done
  ```
  Cualquier línea impresa es un fallo a reportar (la corrección es renombrar el método del controlador —en `design.md` y en el `<call>` si también estuviera mal— para que coincida con el `{nombreFuncionJava}` embebido en el nombre de la acción).
- **e) Cierre tras guardar (`save` → `back`)** (§1.5). En el form **principal**, el `<action-group>` de `btnSave` **MUST** terminar con `back`/`force-back` justo después de `save`. Detector (marca los grupos cuyo `save` no va seguido de `back`/`force-back`):
  ```bash
  for f in .sdd/drafts/{iniciativa}/design/views/*.xml; do
    awk -v f="$f" '
      /<action-group name="[^"]*-btnSave-action"/{inbtn=1; save=0; next}
      inbtn && /<action name="save"\/>/{save=1; next}
      inbtn && /name="(back|force-back)"/{save=0}
      inbtn && /<\/action-group>/{ if(save==1) print f": btnSave termina en save sin back/force-back"; inbtn=0; save=0 }
    ' "$f"
  done
  ```
  Cualquier línea impresa es un fallo a reportar (la corrección es añadir `<action name="back"/>` tras `save`).
- **f) Forms modales de detalle** (§1.6). En cada `<action-group>` que termine en `save-modal`/`delete-modal`: (a) la presencia de `remote-validation*` es un fallo (el maestro puede no existir en BD), y (b) la **ausencia** de un `Local-validate*` que cubra todas las V del detalle evaluables en cliente es un fallo (es la única validación antes de cerrar el modal).
- **g) Auditoría de layout (ASCII Layout)** (§1.7). **MUST** aplicarla a **cada `<form>`** (incluidos los modales de detalle y los `Ref@…-form`), siguiendo la «Dirección de auditoría» de `k-vistas/forms.md` (léelo antes: `.claude/skills/k-vistas/forms.md`):
  1. Por cada `<panel>`, `<panel-related>` y `buttons-panel` del form, **reconstruye el ASCII Layout** a partir de los `colSpan`/`colOffset` reales del XML, con la notación de `forms.md` (una letra por campo repetida `colSpan` veces, `.` por columna vacía de `colOffset`; si hay `showIf`, **un dibujo por estado**, con los paneles condicionales en bloques separados).
  2. **Pasa sobre el dibujo reconstruido el «Checklist de maquetación» de `forms.md`**: cada fila suma exactamente 12, campos relacionados en la misma fila, ningún `colSpan` inflado respecto a la tabla de proporcionalidad, ningún campo solo con hueco injustificado, bordes de columna alineados entre filas y con los paneles condicionales, botones secundarios a la izquierda y principales a la derecha.
  3. **Comprueba la coherencia con el `design.md`**: los ASCII Layout declarados en el resumen estructural **MUST** coincidir con los `colSpan`/`colOffset` del XML (un dibujo que no coincide es un fallo: el diseño miente sobre su layout), y los paneles no triviales sin ASCII Layout declarado son un fallo.
  4. Reporta cada incumplimiento **incluyendo el ASCII Layout reconstruido** como evidencia y citando la regla del checklist incumplida.

  **MUST NOT** validar el layout "a ojo" leyendo `colSpan` sueltos sin reconstruir el dibujo: sin él no se ven los huecos, los bordes desalineados ni las filas que no suman 12.
