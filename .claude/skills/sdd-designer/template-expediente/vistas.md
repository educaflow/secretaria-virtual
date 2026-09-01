# Parte del diseño — las vistas de un tipo de expediente

Define **el formato de las vistas de un tipo de expediente y las reglas que el diseño MUST cumplir al materializarlas**: el `views.xml` de la raíz de la versión (§2), el `views.xml` de cada fase (§3), las reglas duras verificadas por test (§4), los patrones reutilizables (§5, §6), cómo se materializan en la carpeta `design/` (§7) y el checklist (§8).

Lo lee, según su rol: el **diseñador** (aplica §1–§7 al escribir cada `views.xml` y pasa §8), el **juez** y el **enriquecedor** (criterios sobre vistas al comparar diseños), el **verificador** (reaplica §4 y §8), el **corrector** (ajusta cada corrección a la regla incumplida), y los roles de tests (para saber por qué botón se dispara cada evento).

> **REQUIRED — coherencia con `k-tipo-expediente`.** Este fichero **resume** reglas cuya fuente de verdad es el skill `k-tipo-expediente` (`vistas.md` y `SKILL.md` §3.3). Si se cambia algo aquí o allí, **MUST** mantenerse sincronizados.

---

## 0. REGLA DE GENERALIDAD

**CRITICAL.** Este documento describe **el patrón**, nunca un trámite concreto.

- **MUST NOT** aparecer en la parte normativa el nombre de ningún trámite, fase, estado, evento, campo, enum, perfil ni panel reales. Se usan los **placeholders** de `design-contract.md` §0.1 (`<FASE>`, `<ESTADO>`, `<EVENTO>`, `<PERFIL>`, `<Entidad>`, `<Code>`, `<vN>`, `<campo>`, `<panel>`…).
- **MUST NOT** escribirse ninguna regla que solo valga para un número fijo de fases, estados, eventos, paneles o perfiles: el patrón **MUST** funcionar con **N** de cada cosa, con estados con y sin `profile`, y con firma en cliente, en servidor, en ambas o en ninguna.
- Todo ejemplo **MUST** ir encerrado en un bloque que empiece por `> **Ejemplo** (ilustrativo, NO normativo):`.

---

## 1. Estas vistas NO siguen las reglas generales de vistas del proyecto

**CRITICAL.** Las vistas de un tipo de expediente están escritas en un **formato propio, preprocesado**: llevan tags custom (`<form state=…>`, `<include-panels>`, `<footer>`) que **no son XML de vistas Axelor válido por sí solo**. El `viewProcessorTask` (finalizer de `processResources`) las combina y las convierte en vistas Axelor estándar dentro de `build/resources/main/views`.

Por tanto:

- **MUST NOT** aplicarse aquí las convenciones de `k-vistas` ni las reglas `VAR-` de `agent_docs/view-rules.md`: **las vistas de los tipos de expediente están explícitamente excluidas** de ambos.
- **MUST NOT** aplicarse los tests de `com.educaflow.views`: no cubren estos ficheros.
- **MUST NOT** usarse aquí el patrón `buttons-panel` / `btnSave` / `btnDelete` / `remote-validation*` / PI `sv-*` de las vistas de mantenimiento: en un expediente los botones **son eventos** y el guardado y la validación los conduce el `Tramitador`.
- Las reglas que **sí** aplican son las de §4, verificadas por los tests escritos a mano de `src/test/java/com/educaflow/tiposexpedientes/`.

Las vistas se escriben en **dos sitios**:

| Fichero | Qué contiene |
|---|---|
| `<vN>/views.xml` (raíz de la versión) | **Un** form plantilla `exp-<Entidad>-Templates`: el almacén de paneles con nombre. Más los grids y forms auxiliares de las entidades hija |
| `<vN>/<fase>/views.xml` (una por fase) | Los `<form state=… profile=…>` de los estados **de esa fase**, con sus `<include-panels>` y su `<footer>`. Más las `<action-method>` propias de esa fase |

**MUST NOT** duplicarse el form plantilla en las carpetas de fase, y **MUST NOT** ponerse un `<form state=…>` en la raíz de la versión.

---

## 2. El `views.xml` de la raíz de la versión — el almacén de paneles

```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<object-views xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xmlns="http://axelor.com/xml/ns/object-views"
              xsi:schemaLocation="http://axelor.com/xml/ns/object-views https://axelor.com/xml/ns/object-views/object-views_8.1.xsd">

    <form name="exp-<Entidad>-Templates" width="large" title="<título del tipo>"
          model="com.educaflow.subsystem.expedientes.db.<Entidad>" groups="admins,users">

        <panel name="<panel>" title="…"> … </panel>
        <panel name="<panel>-view" title="…"> … </panel>
        …
    </form>
</object-views>
```

### 2.1 El form plantilla

- **MUST** llamarse **exactamente** `exp-<Entidad>-Templates`, donde `<Entidad>` es el code del tipo (`<Code><VN>`) — el mismo que interpola `PhaseEventManager.getViewName`.
- **MUST** haber **exactamente uno** en el `views.xml` de la raíz. Dos o más → error de build claro; **cero** → error explícito al preprocesar el `views.xml` de cualquier fase.
- El `<Entidad>` del form plantilla **MUST** ser el del **propio** tipo. Es la comprobación que caza el code que se queda sin actualizar al duplicar una versión.
- **Nunca se muestra tal cual**: es un almacén de paneles con nombre.
- El `<Code>` **MUST NOT** llevar guiones ni underscores: el patrón del nombre se evalúa como substring.

### 2.2 Atributos heredados

Los forms de estado **heredan** los atributos del form plantilla (`model`, `width`, `groups`, `title`).

- Declarar un atributo con valor **en blanco lo elimina** del resultado (`width=""` quita el `width="large"` heredado).
- El título por defecto, si no se declara, es el `<Entidad>` humanizado.

### 2.3 Regla del almacén (CRITICAL)

**El form plantilla MUST contener TODOS los paneles que CUALQUIER fase incluya.** Vive en la raíz y no en cada fase precisamente porque los paneles se comparten entre fases y duplicarlos obligaría a mantenerlos sincronizados a mano.

- Todo panel incluible **MUST** tener `name` y **MUST** ser **hijo directo** del form plantilla.
- Es incluible **cualquier tag que empiece por `panel`**: `panel`, `panel-related`, `panel-tabs`.
- Un panel referenciado desde un `<include-panels>` que no exista → **el build falla** (`"No existe el panel con nombre:…"`).
- Búsqueda del panel: **primero** en el form plantilla del propio tipo, si no en los `template-views.xml` globales de `tramites/shared/`. **MUST NOT** nombrarse un panel local igual que uno global salvo que se quiera **sobrescribirlo a propósito**: lo tapa **en silencio**.
- Un panel del almacén que **ninguna** fase incluye es código muerto: **MUST NOT** declararse.

### 2.4 Panel editable y panel de solo lectura

Un mismo grupo de campos suele necesitar dos presentaciones. Hay **dos recursos**, y elegir mal es el error típico:

| Recurso | Cuándo usarlo |
|---|---|
| **Prefijo `-`** en el `<include-panels>` (`-<panel>`) | El layout de lectura es **el mismo** que el de edición y basta con poner los campos a `readonly`. **PREFERENTE** |
| **Panel gemelo `<panel>-view`** declarado aparte en la plantilla | La lectura pide **otro layout** (otros `colSpan`, otros `title`, campos que sobran). Se declara ya maquetado para lectura y con sus `<field>` a `readonly="true"` |

- Un gemelo `-view` se incluye normalmente **también con guion** (`-<panel>-view`), por si algún `<field>` se quedó sin el `readonly` explícito.
- **MUST NOT** declararse un gemelo `-view` si el prefijo `-` basta: es duplicación a mantener a mano.

### 2.5 Contenido Axelor normal dentro de los paneles

Los `<field>` de la plantilla admiten los atributos Axelor normales: `colSpan`, `colOffset`, `readonly`, `showTitle`, `title`, `widget` (`SwitchSelect` con `x-direction`, `binary-link` con `x-accept=".pdf"`), `showIf`/`hideIf` por valor de otro campo, y en campos de referencia `grid-view`/`form-view`/`domain`/`onChange`. También caben `<help variant="info">` condicionados con `showIf`.

**CRITICAL** — `readonly`, `showIf` y `hidden` son **UX, nunca defensa**. Lo que el cliente puede dictar lo decide **solo** el conjunto de `field(...)` del validador de la pareja (estado, evento). Ver `design-contract.md` §12.3.

### 2.6 Maestro-detalle (entidades hija)

Si el `domains.xml` declara entidades hija (`one-to-many` del expediente):

1. En el form plantilla, un `<panel-related name="<panel>" field="<campo one-to-many>" grid-view="…" form-view="…"/>` **con `name`**, para poder incluirlo por estado.
2. El **grid y el form del hijo** se declaran en el `views.xml` de la **raíz de la versión** (son de todo el tipo, no de una fase) como vistas Axelor normales, con la convención `exp-<Entidad>-<EntidadHija>-grid` / `exp-<Entidad>-<EntidadHija>-form`.
3. El form del hijo puede usar `<include-panels header="false">` y un **`<footer/>` vacío**: los hijos no disparan eventos.
4. Validación al confirmar el popup del hijo: `onValidate="subsysExpedientes-validate-on-save-child-action"`.
5. Puede haber **varios `form-view` del mismo hijo**: se declara un `panel-related` con `name` distinto por cada combinación y cada estado incluye el suyo.
6. **CRITICAL** — el prefijo `-` **no hace nada** sobre un `panel-related` (no tiene `<field>` propios). Un maestro-detalle de solo lectura **MUST** controlarse con los `canNew`/`canEdit`/`canDelete` del grid del hijo.

---

## 3. El `views.xml` de cada fase — un form por estado (y perfil)

```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<object-views xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xmlns="http://axelor.com/xml/ns/object-views"
              xsi:schemaLocation="http://axelor.com/xml/ns/object-views https://axelor.com/xml/ns/object-views/object-views_8.1.xsd">

    <form state="<ESTADO>" profile="<PERFIL>">
        <include-panels>
            -<panel>
            <panel>
        </include-panels>
        <footer>
            <buttons-left>
                <button name="<EVENTO>" colSpan="2" title="…" onClick="subsysExpedientes-event-action"/>
            </buttons-left>
            <buttons-right>
                <button name="<EVENTO>" colSpan="2" title="…" onClick="subsysExpedientes-event-action"/>
            </buttons-right>
        </footer>
    </form>

    <form state="<ESTADO>">
        <include-panels>
            -<panel>
        </include-panels>
        <footer>
            <buttons-left/>
            <buttons-right>
                <button name="EXIT" colSpan="2" title="Salir" onClick="subsysExpedientes-event-action"/>
            </buttons-right>
        </footer>
    </form>
</object-views>
```

### 3.1 `<form state= profile=>` y el nombre de la vista generada

| Fuente | Nombre de vista generado |
|---|---|
| `<form state="<ESTADO>" profile="<PERFIL>">` | `exp-<Entidad>-<FASE>-<ESTADO>-<PERFIL>-form` |
| `<form state="<ESTADO>">` | `exp-<Entidad>-<FASE>-<ESTADO>-form` |

- En `state` va **solo el nombre del estado**, tal cual está en el `TipoExpedienteInstance.xml`. **MUST NOT** meterse la fase: la deduce el preprocesador del **nombre de la carpeta**, y es él quien la añade como segmento del nombre.
- El `state` **MUST** ser un estado **de la propia fase**; si no, el build falla diciendo qué fases hay.
- **Resolución en runtime**: primero la vista del **perfil actuante**; si no existe, la genérica; si no hay ninguna → excepción `"No existe la vista en el expediente…"`.
- El `profile` de un form es el del **actor que mira**, no necesariamente el del estado: un `profile` distinto del del estado es legítimo. Lo que **MUST** cumplir es estar en la **unión de perfiles del tipo** (los que usa algún estado del `TipoExpedienteInstance.xml`); si no, el build falla con `"El perfil '…' no lo usa ningún estado de …"`.

### 3.2 `<include-panels>` y el prefijo `-`

- Cada línea es el `name` de un panel del form plantilla (o de los globales de `tramites/shared/`).
- **Prefijo `-`** = el panel se copia con **todos sus `<field>` a `readonly="true"`**. Sin prefijo = editable.
- **CRITICAL — los límites del prefijo `-`**: solo afecta a los `<field>` descendientes. **NO desactiva los `<button>`** que el panel contenga y **no hace nada sobre un `panel-related`**. **MUST NOT** confiarse en él para "desactivar" ninguno de los dos.
- Un panel **repetido** en la lista se incluye **una vez** y gana el flag readonly de la **última** aparición, **sin aviso**.
- El orden de las líneas **es** el orden de los paneles en la vista final.

### 3.3 La cabecera gratis y `header`

- Por defecto se antepone el panel global `subsysExpedientes-template-header-panel`, que pinta "Creado por", la **fase**, el **estado**, la fecha del último estado y el botón "Ver el historial de estados" (popup con el historial y los registros de entrada/salida). **No hay que declarar nada** para tenerla.
- El texto de la fase que ve el usuario es el `title` de la `<fase>` del XML maestro (o su `name` humanizado): **ese `title` no es documental**.
- `header="false"` la quita: `<include-panels header="false">`. Listarla explícitamente en la lista permite reconfigurarla (p.ej. en readonly).
- **CRITICAL — el atributo es `header`, NO `includeHeader`**, que el preprocesador **ignora en silencio**. Solo admite `true`/`false`; otro valor → `"Valor fuera de rango"`.

### 3.4 `<footer>` y los botones

- El `<footer>` se sustituye por el panel global `subsysExpedientes-template-footer-panel`, que además **pinta los mensajes de error de validación**. Los botones van dentro de `<buttons-left>` / `<buttons-right>`.
- **CRITICAL — el `name` de cada botón ES el evento que dispara.** MUST coincidir exactamente con un `<EVENTO>` del atributo `events` del estado, o ser `EXIT`.
- Los botones admiten los atributos Axelor normales: `title`, `colSpan`, `colOffset`, `prompt`, `css`, `outline`, `icon`.
- El `colSpan` por defecto es el `itemSpan` del panel footer (default `1`).
- Al **primer** botón de `<buttons-right>` se le asigna **siempre** `colOffset = 12 − suma de colSpan` de todos los botones, **sobrescribiendo** cualquier valor manual. Si la suma pasa de 12, el offset sale **negativo sin aviso**: la suma de los `colSpan` del footer **MUST NOT** pasar de 12.
- Un bloque sin botones se escribe `<buttons-left/>` (vacío), no se omite.
- `EXIT` y `DELETE` responden con `refresh-app`: recargan la aplicación entera, no navegan.

### 3.5 Contenido Axelor adicional en un form de estado

Un form de estado puede llevar, **fuera** del `<include-panels>`, paneles Axelor normales propios de ese estado (típicamente un `<panel showFrame="false">` con un `<help variant="info">` de aviso). Es legítimo y **MUST NOT** convertirse en un panel del almacén si solo lo usa ese estado.

### 3.6 Acciones globales disponibles

| Acción | Llama a | Se usa en |
|---|---|---|
| `subsysExpedientes-event-action` | `ExpedienteController.triggerEvent` | el `onClick` de **todos** los botones del footer |
| `subsysExpedientes-validate-on-save-child-action` | `ExpedienteController.validateChild` | el `onValidate` del form de una entidad hija |
| `subsysExpedientes-event-new-action` / `-event-view-action` | `triggerInitialEvent` / `viewExpediente` | las usa el subsistema; **MUST NOT** referenciarse desde un `views.xml` de tipo |

Las `<action-method>` **propias** de una fase se declaran en el `views.xml` de **esa** fase, junto a su botón: los nombres de acción son globales, y tenerlas al lado es lo que hace que copiar la fase se lleve todo.

---

## 4. Reglas duras (verificadas por test)

| ID | Regla |
|---|---|
| **X1** | **MUST** existir un `<form state="<ESTADO>">` **sin `profile`** por **CADA** estado de la fase. Es la red de seguridad: el perfil actuante puede ser cualquiera de los del tipo, y sin ella navegar al estado lanza `"No existe la vista en el expediente"`. Es la vista de **solo lectura**, con botón `EXIT` |
| **X2** | **MUST** existir `<form state="<ESTADO>" profile="<PERFIL>">` en **todo estado que tenga `profile` Y al menos un evento**. Sin ella, ese usuario caería en la vista genérica de solo lectura y **el expediente se quedaría atascado sin ningún error**. Es la vista **editable**, la del actor que "tiene el turno" |
| **X3** | **MUST NOT** haber dos forms de la misma fase con el mismo `(state, profile)`: producen el mismo nombre de vista y **Axelor se queda con la última**; las demás no se pintan nunca |
| **Y1** | El `name` de **todo** `<button>` del `<footer>` **MUST** ser un evento declarado en **ese** estado, o uno de los comunes (`DELETE`, `EXIT`). Un `<button name="">` — el esqueleto sin rellenar — es **violación** |
| **Y2** | **Todo** evento declarado en un `<state>` **MUST** tener un botón que lo dispare en **alguno** de los forms de ese estado (se mira la **unión** genérico + perfil, no form a form). Si no, el usuario no puede llegar a él aunque su `trigger*` exista |
| **Y3** | El `onClick` de **todo** botón del footer **MUST** incluir `subsysExpedientes-event-action` |

**Exenciones**: X2 exime los estados **sin `profile`** y los estados **sin eventos**. Y2 exime los estados sin ningún form (eso ya lo reporta X1).

Reglas adicionales que **no** tienen test pero rompen en runtime o en build:

- **MUST NOT** declararse `EXIT` en el atributo `events` de un `<state>` — pero **MUST** ponerse el **botón** `EXIT` en la vista **genérica** de **cada** estado. Es el único modo de salir de un formulario que no tiene eventos, y `ExpedienteController` lo intercepta antes del `Tramitador`.
- **MUST** declararse `DELETE` en `events` del estado desde el que se pueda borrar, poner su botón, y escribir su `triggerDelete`.
- **CRITICAL — un `<object-views>` sin ningún elemento hijo tumba el arranque, y el build NO lo detecta.** La validación XSD la hace el `ViewLoader` al arrancar: el síntoma es `"The content of element 'object-views' is not complete"`, aborta `AppStartup` y la aplicación queda **sin vistas, sin menús y sin data-init**. Los comentarios XML **no** cuentan como contenido. **MUST NOT** dejarse un `views.xml` de fase vacío ni con todos sus forms comentados: o tiene al menos un elemento válido, o se omite el fichero entero.
  - En la práctica, como toda fase tiene al menos un estado y X1 exige su form genérico, **el `views.xml` de toda fase declarada existe y no está vacío**.
- **MUST NOT** aparecer en un `views.xml` de fase un `<form>` sin `state`, ni el form plantilla.
- Todos los XML con raíz `object-views` **MUST** ser parseables.

- ✅ CORRECTO: un estado con `profile="<PERFIL>"` y `events="<EVENTO>"` tiene **dos** forms: el de `<PERFIL>` con los paneles editables y el botón `<EVENTO>`, y el genérico con los mismos paneles en `-` y el botón `EXIT`.
- ✅ CORRECTO: un estado `closed` con `events=""` tiene **solo** el form genérico, con botón `EXIT`.
- ❌ INCORRECTO: un estado con `events="<EVENTO>"` cuyo único form es el genérico con botón `EXIT` (X2/Y2: el evento no tiene botón, el expediente se atasca sin error).
- ❌ INCORRECTO: dejar la cáscara que genera `CreateFilesTask` con `<button name=""/>` (Y1).

---

## 5. Patrón: visor de PDF embebido

Para mostrar un campo `many-to-one` a `MetaFile` dentro de un panel. Se declara **un panel con nombre por cada PDF**, para poder incluirlo estado a estado. El `name` del campo dummy es **libre** — **no** tiene por qué existir en la entidad; el campo real va en el `depends` del `<viewer>`:

```xml
<panel name="<panel>" title="…">
    <field name="<dummy>" showTitle="false" readonly="true" colSpan="12">
        <viewer depends="<campoPdf>"><![CDATA[
            <>
            <Box as="iframe" height="900" border="0"
                 src={`ws/rest/com.axelor.meta.db.MetaFile/${<campoPdf>.id}/content/download?inline=true&name=${<campoPdf>.fileName}`}></Box>
            </>
        ]]></viewer>
    </field>
</panel>
```

- El `depends` **MUST** ser el nombre del campo `MetaFile` real de la entidad.
- La `height` del iframe se ajusta al uso: alta para el documento principal que el usuario lee entero, más baja para un documento de apoyo.
- El panel **MUST** ir en el form plantilla de la raíz, como cualquier otro.

---

## 6. Patrón: firma en cliente (AutoFirma) desde la vista

Es la firma **del ciudadano**, con su certificado, en su máquina. Se reparte en **tres ficheros** y **no hay código de AutoFirma en el `PhaseEventManagerImpl`**:

| Pieza | Fichero | Qué se escribe |
|---|---|---|
| 1. Modelo | `domains.xml` | El **par** de campos `MetaFile`: `<campoOrigen>` y `<campoDestino>` |
| 2. Vista | `views.xml` de **la fase** | Una `<action-method>` al `FirmaController` + el botón encadenado con `serial:` |
| 3. Validación | `StateEventValidatorImpl.kt` | `+Required()` y `+FirmaPdf(model::get<CampoOrigen>, model::getDniFirmaDocumentoEntrada)` sobre el campo **destino** |

La pieza 2:

```xml
<action-method name="exp-<Entidad>-<accion>-action">
    <call class="com.educaflow.subsystem.expedientes.controllers.FirmaController"
          method='firmarDocumentoEntrada(id,"<campoOrigen>","<campoDestino>",<x>,<y>,<ancho>,<alto>,<pagina>)'/>
</action-method>

<button name="<EVENTO>" title="…"
        onClick="serial:exp-<Entidad>-<accion>-action,subsysExpedientes-event-action"/>
```

Los **8 argumentos**, en orden:

| Pos | Parámetro | Significado |
|---|---|---|
| 1 | `id` | id del expediente, del contexto de la vista. Se resuelve comprobando `CAN_READ` sobre la clase **obtenida de BD**, no la que diga el cliente. Se escribe literalmente `id` |
| 2 | `"<campoOrigen>"` | nombre del campo `MetaFile` **origen** (el PDF a firmar), entre comillas dobles |
| 3 | `"<campoDestino>"` | nombre del campo `MetaFile` **destino** donde AutoFirma deja el firmado, entre comillas dobles |
| 4 | `<x>` | X de la esquina **inferior izquierda** del rectángulo de firma, en puntos PDF |
| 5 | `<y>` | Y de la esquina inferior izquierda |
| 6 | `<ancho>` | ancho del rectángulo |
| 7 | `<alto>` | alto del rectángulo |
| 8 | `<pagina>` | página donde se estampa |

Reglas:

- El atributo `method` **MUST** ir entre **comillas simples**, porque su contenido lleva comillas dobles.
- La `<action-method>` **MUST** declararse en el `views.xml` de **la fase** que la usa, no en la raíz.
- El `onClick` **MUST** ser `serial:<action-method propia>,subsysExpedientes-event-action` — la lista **MUST** terminar **siempre** en `subsysExpedientes-event-action` (Y3).
- **CRITICAL** — el controlador lanza `RuntimeException` si `dniFirmaDocumentoEntrada` es `null`, está en blanco o no pasa `DniUtil.isValid`. **MUST** haberlo rellenado el `triggerInitialEvent`. **Nada lo verifica en build.**
- El **campo destino** es `servidor` a efectos del modelo pero **sí** aparece en el validador, porque es el único sitio donde se comprueba la firma: lleva `+Required()` + `+FirmaPdf(...)`. Es la única excepción a la regla de «solo campos `usuario` en el validador», y **MUST** documentarse como tal en el `design.md`.
- El mismo mecanismo `serial:` sirve para encadenar **cualquier** acción propia antes del evento; la firma en cliente es solo su uso más común.
- La firma **en servidor** por cargo **no toca la vista**: vive entera en el `trigger*` (ver `design-contract.md`, sección «Especificación de los PhaseEventManagerImpl»).

---

## 7. Cómo se materializan las vistas en el diseño

- `design/views.xml` → se copia verbatim a `<vN>/views.xml`.
- `design/fases/<fase>/views.xml` → se copia verbatim a `<vN>/<fase>/views.xml`. **MUST** haber uno por **cada** fase declarada, con la carpeta en **minúsculas**.
- Ambos **sobrescriben** la cáscara que dejó `CreateFilesTask`.
- El `design.md` **MUST** llevar, en el paso correspondiente, un **resumen estructural** corto de cada `views.xml`: qué paneles declara la plantilla y, por fase, la lista de `(estado, perfil)` con sus paneles incluidos y sus botones. **MUST NOT** volcar el XML inline: el XML vive en su fichero.
- **MUST NOT** materializarse ningún `menus.xml`: un tipo de expediente **no** declara menús propios; se llega a él por el árbol de trámites del subsistema de expedientes.

> **Ejemplo** (ilustrativo, NO normativo) de resumen estructural de una fase en el `design.md`:
>
> | estado | profile | paneles incluidos | botones (izq / der) |
> |---|---|---|---|
> | `<ESTADO>` | `<PERFIL>` | `-<panelA>`, `<panelB>` | `DELETE` / `<EVENTO>` |
> | `<ESTADO>` | — | `-<panelA>`, `-<panelB>-view` | — / `EXIT` |

---

## 8. Checklist de vistas

El diseñador lo aplica antes de dar el diseño por terminado (**MUST NOT** terminarlo con algún punto sin cumplir); el verificador lo reaplica.

**Raíz de la versión**

- [ ] ¿Existe `design/views.xml` con **exactamente un** `<form name="exp-<Entidad>-Templates">`, con el `<Entidad>` del **propio** tipo?
- [ ] ¿El form plantilla declara `model="com.educaflow.subsystem.expedientes.db.<Entidad>"` y los atributos que se heredan (`width`, `groups`, `title`)?
- [ ] ¿Contiene **todos** los paneles que alguna fase incluye, cada uno con `name` y como **hijo directo**?
- [ ] ¿Ningún panel del almacén queda sin incluir por ninguna fase (código muerto)?
- [ ] ¿Cada gemelo `-view` existe solo porque el layout de lectura es distinto, y no donde bastaba el prefijo `-`?
- [ ] ¿Los `<panel-related>` de maestro-detalle tienen `name`, y los grids/forms del hijo están en la **raíz** con la convención `exp-<Entidad>-<EntidadHija>-grid` / `-form`?
- [ ] ¿No hay ningún `<form state=…>` en el `views.xml` de la raíz?

**Por cada fase**

- [ ] ¿Existe `design/fases/<fase>/views.xml`, con la fase en **minúsculas**, para **cada** fase declarada?
- [ ] **X1** — ¿un `<form state="<ESTADO>">` **sin `profile`** por **cada** estado de la fase, incluidos los `closed` y los que no tienen eventos?
- [ ] **X2** — ¿un `<form state="<ESTADO>" profile="<PERFIL>">` en **todo** estado que tenga `profile` **y** al menos un evento?
- [ ] **X3** — ¿ningún `(state, profile)` duplicado dentro de la fase?
- [ ] ¿Todo `state` es un estado **de esa** fase, y ninguno lleva la fase dentro del atributo?
- [ ] ¿Todo `profile` de un `<form>` está en la **unión de perfiles del tipo**?
- [ ] **Y1** — ¿el `name` de **todo** botón es un evento del propio estado, o `DELETE`, o `EXIT`? ¿Ningún `<button name="">`?
- [ ] **Y2** — ¿**todo** evento declarado en un estado tiene botón en alguno de sus forms (unión genérico + perfil)?
- [ ] **Y3** — ¿**todo** `onClick` incluye `subsysExpedientes-event-action` (y las cadenas `serial:` **terminan** en ella)?
- [ ] ¿La vista **genérica** de cada estado lleva botón `EXIT` y sus paneles en solo lectura?
- [ ] ¿`EXIT` **no** aparece en ningún `events` del `TipoExpedienteInstance.xml`?
- [ ] ¿La suma de los `colSpan` de los botones de cada footer **no** pasa de 12?
- [ ] ¿Cada bloque sin botones se escribe `<buttons-left/>` en vez de omitirse?
- [ ] ¿Todos los paneles referenciados en `<include-panels>` existen en el form plantilla (o en los globales), sin repeticiones?
- [ ] ¿Se usa `header` y **nunca** `includeHeader`?
- [ ] ¿Ningún `views.xml` de fase queda vacío ni con todos sus forms comentados?
- [ ] ¿Cada `<action-method>` propia está en el `views.xml` de **su** fase, junto al botón que la usa?

**Coherencia y patrones**

- [ ] ¿Cada campo editable en la vista de un estado aparece en el `field(...)` del validador del evento que se dispara desde ella? ¿Y ningún campo `servidor` (salvo el destino de una firma en cliente) es editable en la vista?
- [ ] ¿Cada visor de PDF sigue el patrón de §5, con el campo real en `depends`?
- [ ] ¿Cada firma en cliente tiene sus **tres** piezas (par de campos, `<action-method>` + botón `serial:`, `FirmaPdf` en el validador) y sus **8** argumentos completos?
- [ ] ¿El `design.md` lleva el resumen estructural de la plantilla y, por fase, la tabla `(estado, perfil) → paneles → botones`, coherente con el XML?
- [ ] ¿No se ha aplicado ninguna regla de `k-vistas` / `view-rules.md` / `buttons-panel` / PI `sv-*` / `remote-validation*` a estos ficheros?
- [ ] ¿No se ha materializado ningún `menus.xml`?
