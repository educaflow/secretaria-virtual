# Las vistas del tipo de expediente (`views.xml`)

**CRITICAL**: las vistas de los tipos de expediente **NO siguen las normas de `k-vistas` ni `view-rules.md`** (están excluidas). Se escriben en un formato propio que el preprocesador de EducaFlowBuildTools (`viewProcessorTask`) convierte en vistas Axelor estándar durante el build. Los ejemplos usan el trámite inventado `MiTramite` (`SKILL.md`).

## 1. Las dos piezas, en dos sitios distintos

1. **Un form plantilla** (Axelor normal) `exp-<Code>-Templates`: **almacén de paneles con nombre**. Nunca se muestra tal cual; sus atributos (`model`, `width`, `groups`…) se heredan y sus paneles se copian a las vistas finales. Va en el **`views.xml` de la raíz de la versión**, uno para todo el tipo de expediente.
2. **Un form por estado (y opcionalmente perfil)** con los tags custom `<form state=...>`, `<include-panels>` y `<footer>`. Van en el **`views.xml` de la carpeta de su fase**.

El form plantilla está en la raíz y no en cada fase porque los paneles se comparten entre fases (los datos del interesado, los de la solicitud…) y duplicarlos obligaría a mantenerlos sincronizados a mano. En la raíz caben también los grids y forms auxiliares con nombre propio (los de las entidades hija, §8).

```xml
<!-- <vN>/views.xml -->
<form name="exp-MiTramiteV1-Templates" width="large" title="..."
      model="com.educaflow.subsystem.expedientes.db.MiTramiteV1" groups="admins,users">
    <panel name="datos-solicitud" title="Datos de la solicitud"> ... </panel>
    <panel name="justificante-upload" title="Adjuntar justificante"> ... </panel>
    ...
</form>
```

```xml
<!-- <vN>/entrada/views.xml -->
<form state="ENTRADA_DATOS" profile="CREADOR">
    <include-panels>
        -subsanacion                 <!-- panel común (§3.2), no está en la plantilla del tipo -->
        -datos-interesado            <!-- con guion: se incluye con TODOS sus fields readonly -->
        datos-solicitud              <!-- sin guion: editable -->
        justificante-upload
    </include-panels>
    <footer>
        <buttons-left>
            <button name="DELETE" colSpan="3" css="btn-danger" outline="true" icon="trash" title="Borrar el expediente"
                    onClick="subsysTramitador-event-action" prompt="¿Está seguro que desea borrar el expediente?"/>
        </buttons-left>
        <buttons-right>
            <button name="GUARDAR_DATOS" colSpan="3" title="Siguiente" onClick="subsysTramitador-event-action"/>
        </buttons-right>
    </footer>
</form>
```

## 2. Dos forms por estado y cómo elige el runtime

En el atributo `state` va el **nombre del estado** tal cual, igual que en el `TipoExpedienteInstance.xml`; la **fase la deduce el preprocesador del nombre de la carpeta** del fichero, y es él quien la añade como segmento del `name` de la vista generada (`SKILL.md` §1.5).

- `<form state="X" profile="Y">` → `exp-<Code>-<FASE>-<X>-<PROFILE>-form` (la del perfil que "tiene el turno", editable).
- `<form state="X">` (sin perfil) → `exp-<Code>-<FASE>-<X>-form` (la genérica, normalmente todo readonly + botón `EXIT` + el panel `avisoEstadoExpediente` de §6.1).
- El runtime busca primero la del perfil actuante y si no existe usa la genérica; si no hay ninguna → excepción "No existe la vista en el expediente". El build no lo comprueba, pero los **tests** sí (`SKILL.md` §3.3): la genérica es obligatoria en todo estado, y la del perfil en los estados que tienen `profile` y eventos de usuario (los de `systemEvents` no cuentan: un estado que solo espera al servidor no necesita la vista de su dueño).
- El esqueleto genera dos **cáscaras idénticas y vacías** por estado (`<include-panels>` sin paneles y un único `<button name="">` sin rellenar en cada una): no distingue cuál es cuál, eso lo escribes tú — la del perfil dueño editable y la genérica de solo lectura. Es justo ese esqueleto sin rellenar el que caza el test Y1 (`SKILL.md` §3.3).
  Además, la del perfil **solo se genera si el estado declara `profile`**; en un estado sin `profile` el esqueleto trae solo la genérica.
- **MUST** ser un estado **de la propia fase**: si pones el `state` de un estado de otra fase, el build falla diciéndolo. Los formularios de un estado van siempre en el `views.xml` de su fase.
- El `profile` del form es el del **actor que mira la vista**, no necesariamente el del estado: un `<form state="X" profile="Y">` con `Y` distinto del `profile` de `X` es legítimo y el build lo admite (hay listados que abren con perfil `TRAMITADOR` expedientes en estados de perfil `CREADOR`). Lo que **MUST** cumplir es estar en la **unión de perfiles del tipo** — los que usa algún estado del `TipoExpedienteInstance.xml` —; si no, el build falla ("El perfil '…' no lo usa ningún estado de …"), porque esa vista no se pintaría nunca (§10).
- **MUST NOT** haber dos forms de la misma fase con el mismo `(state, profile)`: producen el mismo `name` de vista y el segundo tapa al primero en silencio. Lo caza el test X3 (`SKILL.md` §3.3).

## 3. `<include-panels>`

- Cada línea es el `name` de un panel; prefijo `-` = copia con todos sus `<field>` a `readonly="true"`.
- Es incluible **cualquier elemento cuyo tag empiece por `panel`** (`panel`, `panel-related`, `panel-tabs`) que sea hijo directo del form plantilla y tenga `name`.
- **CRITICAL — el prefijo `-` solo afecta a los `<field>`** descendientes del panel: **NO desactiva los `<button>`** que el panel contenga ni hace nada sobre un **`panel-related`** (no tiene fields): el grid de hijos sigue permitiendo lo que digan sus `canNew`/`canEdit`/`canDelete`. Para un maestro-detalle de solo lectura, controla esos flags en el grid del hijo (§8).
- Búsqueda: **primero** en el form plantilla del tipo (el `views.xml` de la raíz de la versión), si no en `tramites/shared/template-views.xml`. Un panel local con el mismo nombre que uno global lo **sobreescribe en silencio** (incluidos los de cabecera y footer).
- Por defecto se antepone la cabecera global (`subsysExpedientes-template-header-panel`): pinta "Creado por", la **fase** y el **estado** actuales, la fecha del último estado, a quién le toca y el botón "Ver el historial de estados" (con los registros de entrada/salida de cada estado). Todo gratis, sin declarar nada.
  - `header="false"` en `<include-panels>` la quita.
  - **CRITICAL**: el atributo es `header`, NO `includeHeader`, que se **ignora en silencio**. Solo admite `true`/`false`.
- Un panel referenciado que no existe → el build falla ("No existe el panel con nombre:...").
- Un panel repetido en la lista se incluye una vez (gana el flag readonly de la última aparición), sin aviso.
- Los `<include-panels>` y `<footer>` se expanden **en cualquier punto del documento**, no solo dentro de forms con `state`.

### 3.1 Paneles comunes de las personas

`tramites/shared/template-views.xml` trae estos paneles, incluibles desde cualquier tipo, y sus dos acciones:

| Panel / acción | Qué es |
|---|---|
| `persona-solicitante` | Nombre, apellidos, DNI, email y teléfono de quien presenta, de solo lectura; solo se ve si `presentadoEnRepresentacion` |
| `persona-solicitante-editable` | Lo mismo en un `<editor>`, para el estado en que se teclea: en papel y en representación nadie más lo rellena |
| `persona-interesada` | Un `<editor>` sobre `personaInteresada` con su identificación y sus datos de contacto (email, teléfono, dirección, municipio, CP) |
| `subsysExpedientes-persona-interesada-onLoad-action` | Pone de solo lectura nombre, apellidos y DNI del interesado salvo en representación o en papel |
| `subsysExpedientes-persona-solicitante-onLoad-action` | Pone de solo lectura nombre, apellidos, DNI, email y teléfono del solicitante salvo en papel |

- La acción de cada persona editable **MUST** estar en el `onLoad` del form en el que se edita (en un `<action-group>` si el form ya tenía `onLoad`).
- Qué persona nace vacía en cada modo, y por tanto qué hay que pedir: `modelo.md` §2.1.

Si el tipo necesita otro conjunto de datos (p. ej. el NIA), declara en su form plantilla **su propio** panel con el mismo patrón y usa la misma acción:

```xml
<panel name="datos-interesado" title="Datos del interesado" colSpan="12">
    <field name="personaInteresada" showTitle="false" colSpan="12"
           canNew="false" canSelect="false" canRemove="false">
        <editor x-show-titles="true">
            <field name="nombre" colSpan="6" required="true"/>
            <field name="nia" colSpan="6" required="true"/>
        </editor>
    </field>
    <field name="presentadoEnRepresentacion" hidden="true"/>
    <field name="presentadoEnPapel" hidden="true"/>
</panel>
```

- **CRITICAL**: Axelor pinta **siempre** de solo lectura un campo con punto (`personaInteresada.nia`). Lo que el usuario edita de una persona **MUST** ir dentro de un `<editor>` sobre la relación.
- **MUST NOT** poner en el mismo form un campo con punto `personaInteresada.<campo>` y un `<editor>` sobre `personaInteresada` con ese `<campo>`: el cliente le pasa al campo del editor los atributos (`hidden`, `readonly`) del campo con punto.
- **MUST NOT** usar `_parent` en un `showIf`/`readonlyIf` de un campo del editor: no se evalúa. Lo que depende del expediente se hace desde el form con una `<action-attrs for="personaInteresada.<campo>">`.
- **MUST** haber como máximo **un** `<editor>` sobre `personaInteresada` por form: incluye el panel común o el propio del tipo, no los dos.
- **MUST** poner `canSelect="false"`, `canNew="false"` y `canRemove="false"` en el campo del editor: si no, ofrece buscar, crear o quitar la `Persona` (el servidor lo rechaza igualmente). **MUST NOT** poner `canEdit="false"` ni `canView="false"`: el editor deja de pintarse.
- **MUST** incluir `<field name="presentadoEnRepresentacion" hidden="true"/>` y `<field name="presentadoEnPapel" hidden="true"/>` **al final** del panel: las acciones de `onLoad` evalúan los dos y sin ellos el cliente no los recibe; un campo oculto al principio ocupa su hueco en la rejilla.

### 3.2 Paneles comunes de las fases `ENTRADA` y `VERIFICACION`

`tramites/shared/template-views.xml` trae también los paneles de las dos fases comunes (`SKILL.md` §1.2). **MUST** incluirse desde ahí: **MUST NOT** redeclararlos en el form plantilla del tipo (un panel local con el mismo nombre taparía al común, §3).

| Panel | Qué pinta | Campos del tipo que necesita | Se incluye |
|---|---|---|---|
| `subsanacion` | Lo que el verificador ha pedido subsanar; solo se ve mientras `textoSubsanacion` tiene valor | `textoSubsanacion` | con `-`, el primero, en los forms de la fase `ENTRADA` en los que se corrige la solicitud |
| `solicitud-escaneada-upload` | Subida del PDF de la solicitud entregada en papel (`binary-link`, solo `.pdf`) | `pdfSolicitudFirmada` | sin guion, en `PENDIENTE_DOCUMENTO_ESCANEADO` |
| `solicitud-escaneada-view` | Descarga de esa solicitud escaneada; solo se ve si `presentadoEnPapel` | `pdfSolicitudFirmada` | con `-`, en el form `profile="TRAMITADOR"` de `ENTRADA_DATOS` |
| `verificacion` | `resultadoVerificacion` (`RadioSelect`) y, si vale `SUBSANAR`, `textoSubsanacion` | `resultadoVerificacion`, `textoSubsanacion` | sin guion, en `PENDIENTE_VERIFICACION` |
| `pdfSolicitud` | Visor (§9) de la solicitud a firmar | `pdfSolicitud` | con `-`, en `PENDIENTE_PRESENTACION` |
| `pdfSolicitudFirmada` | Visor de la solicitud presentada: la firmada o, en papel, la escaneada | `pdfSolicitudFirmada` | con `-`, donde quien tramita necesite verla |
| `pdfJustificanteRegistroEntrada` | Visor del resguardo de la presentación | `pdfJustificanteRegistroEntrada` | con `-`, en los forms genéricos tras presentar |
| `firma-solicitud` | Los campos de vista `situacionFirma`/`firmaEnServidor` y un panel por situación de firma, con `claveCertificado` donde se pide (`recetas/firma.md` §1.3) | ninguno propio (`claveCertificado` es de `Expediente`) | **sin guion**, en `PENDIENTE_PRESENTACION` |

- **CRITICAL**: estos paneles nombran campos que **no son de `Expediente`**: cada tipo los declara en su `domains.xml` con **ese mismo nombre** (`SKILL.md` §1.2). Si el tipo no declara el campo, el build no falla: el panel pinta un campo vacío.
- El panel `verificacion` compara `resultadoVerificacion=='SUBSANAR'`: el enum del tipo **MUST** tener un ítem con ese nombre.
- Las acciones `exp-<Code>-…` que acompañan a `firma-solicitud` (el `onLoad`, AutoFirma, vaciar la clave) **no** son comunes: se declaran en el `views.xml` de la fase de cada tipo (`recetas/firma.md` §1.3).
- Qué form incluye cada panel, estado a estado: `recetas/presentacion.md`.

- ✅ CORRECTO: `firma-solicitud` (sin guion)
- ❌ INCORRECTO: `-firma-solicitud` (`claveCertificado` queda de solo lectura y no se puede firmar en servidor)
- ❌ INCORRECTO: `<panel name="pdfSolicitud" ...>` en el form plantilla del tipo (tapa en silencio al común)
- ❌ INCORRECTO: incluir `-pdfSolicitudFirmada` en un tipo cuyo campo se llama `pdfSolicitudFirmado` (el visor sale vacío)

## 4. `<footer>`

- Se sustituye por el panel global `subsysExpedientes-template-footer-panel` (que además pinta los mensajes de error de validación) con tus botones dentro de `<buttons-left>`/`<buttons-right>`.
- **El `name` de cada botón es el evento que dispara**; todos usan `onClick="subsysTramitador-event-action"`. Solo llevan botón los eventos de `events` y los comunes: un evento de `systemEvents` **MUST NOT** tener botón (test Y1, `SKILL.md` §2.1). Admiten atributos Axelor normales (`title`, `colSpan`, `prompt`, `css`, `outline`, `icon`).
- **MUST**: todo botón de `<buttons-left>` y `<buttons-right>` lleva `colSpan="3"`, sea cual sea su texto, para que todos los botones de todos los trámites midan lo mismo.
  Lo comprueba el test Y4 (`SKILL.md` §3.3).
  - ✅ CORRECTO: `<button name="EXIT" colSpan="3" title="Salir" onClick="subsysTramitador-event-action"/>`
  - ❌ INCORRECTO: `<button name="EXIT" colSpan="2" .../>` o sin `colSpan`
- Cada lado va en su propio panel sin marco de 6 columnas (`colSpan="6" cols="6"`), así que los botones de la izquierda no desplazan a los de la derecha.
- **MUST**: en cada lado solo se ve **un** botón a la vez.
  Puede haber varios si sus `showIf`/`hideIf` son excluyentes, con cualquier condición (`showIf="situacionFirma=='SIN_CERTIFICADO'"` y `showIf="firmaEnServidor"`): un botón oculto no ocupa sitio.
- A cada botón de la derecha se le asigna **siempre** (sobrescribiendo cualquier valor manual) `colOffset = 6 − colSpan`, para alinearlo al margen derecho.
  Los de la izquierda no llevan ningún cálculo.
- Los eventos comunes `EXIT` y `DELETE` responden al cliente con `refresh-app` (se recarga la aplicación entera, no se navega a otra vista).
- Tras el footer de un `<form state=...>` el preprocesador añade siempre el panel global `subsysExpedientes-template-notas-panel`: las notas del expediente (`Expediente.notas`) y el botón «Añadir nota». No se declara ni se incluye: sale solo en todos los forms de estado, y se oculta él mismo al creador del expediente (salvo en papel).
  - El `<footer>` de un form que no es de estado (el de una entidad hija, §8) no lo lleva.
  - Desde un `trigger*` una nota se añade con `ExpedienteNotasUtil.addNote(expediente, mensaje)` (`subsystem/expedientes/util`). Es para lo que se dicen entre sí quienes tramitan: lo que deba ver el creador va en un campo propio del tipo.

Las dos únicas acciones del motor que usa un `views.xml`:

| Acción | Se usa en |
|---|---|
| `subsysTramitador-event-action` | el `onClick` de **todos** los botones del footer |
| `subsysTramitador-validate-on-save-child-action` | el `onValidate` del form de una entidad hija (§8) |

## 5. Herencia y des-herencia de atributos

Los forms de estado heredan los atributos del form plantilla. Declarar un atributo con valor **en blanco** lo **elimina** del resultado (`width=""` quita el `width="large"` heredado). El título por defecto es el `code` humanizado con reglas concretas: separa el camelCase, capitaliza solo la primera palabra y pasa el resto a minúsculas conservando las siglas (`MiTramiteV1` → "Mi tramite v1").

## 6. Contenido Axelor adicional

Un form de estado puede llevar además paneles Axelor normales (fuera de `<include-panels>`), colocados entre `</include-panels>` y `<footer>`.
El caso típico es un `<panel showFrame="false">` con un único `<help variant="info">`: en el form del perfil que actúa, instrucciones de lo que tiene que hacer (nombre libre); en el form de solo `EXIT`, el panel `avisoEstadoExpediente` de §6.1.

### 6.1 Panel `avisoEstadoExpediente`: obligatorio en todo form de solo `EXIT`

Un form cuyo único botón es `EXIT` es una pantalla en la que el usuario no puede hacer nada.
Si no se le explica nada, no sabe si el expediente está atascado, si le toca a él o si ya ha terminado.

- **MUST** llevar un `<panel name="avisoEstadoExpediente" colSpan="12" showFrame="false">` con un único `<help variant="info">`, entre `</include-panels>` y `<footer>`.
  Aplica a todos los forms de solo `EXIT`: los genéricos y también los de perfil (`profile="..."`) que no tengan más botón que `EXIT`.
- El texto **MUST** decir **qué está pendiente y de quién** ("La solicitud está pendiente de que la secretaría del centro la verifique", "La resolución está pendiente de la firma del director").
  **MUST NOT** repetir el nombre de la fase o del estado: ya los pinta la cabecera (§3).
- En un estado final el texto dice que el expediente está cerrado y con qué resultado ("El expediente está cerrado: la anulación ha sido aceptada").
- El `name` es siempre `avisoEstadoExpediente`, igual en todos los forms del tipo: un panel declarado dentro de un `<form state=...>` es local a esa vista, no de la plantilla, así que no colisiona con el de los demás forms.
  No se declara en el form plantilla ni se incluye por `<include-panels>`: el texto es distinto en cada estado.

✅ Correcto:

```xml
<form state="PENDIENTE_VERIFICACION">
    <include-panels>
        -datos-solicitud-view
        -pdfJustificanteRegistroEntrada
    </include-panels>

    <panel name="avisoEstadoExpediente" colSpan="12" showFrame="false">
        <help variant="info" colSpan="12">La solicitud está pendiente de que la secretaría del centro la verifique</help>
    </panel>

    <footer>
        <buttons-left/>
        <buttons-right>
            <button name="EXIT" colSpan="3" title="Salir" onClick="subsysTramitador-event-action"/>
        </buttons-right>
    </footer>
</form>
```

❌ Incorrecto: el mismo form sin el panel (el usuario ve los datos y un botón "Salir" sin saber qué pasa con su solicitud), o con `name="avisoPendienteVerificacionGenerica"` (nombre propio por estado en lugar del fijo), o con el texto "Estado: PENDIENTE_VERIFICACION" (repite la cabecera).

La pantalla del estado en que el usuario firma y presenta un documento (el panel común `firma-solicitud`, el `onLoad` que rellena sus campos de vista y los dos botones `PRESENTAR`) está en la receta `recetas/firma.md` §1.3.

Dentro de los paneles de la plantilla, los `<field>` admiten los atributos Axelor normales; los que se ven en los trámites reales: `widget="RadioSelect"` con `x-direction` en los campos de tipo enumerado (obligatorio: §6.3), `showIf`/`hideIf` por valor de otro campo, `widget="binary-link"` con `x-accept=".pdf"` para restringir el tipo de fichero subido, `<help variant="info">` condicionales con `showIf`, y en campos de referencia `grid-view`/`form-view`/`domain`/`onChange` (las `action-record`/`action-method` propias se declaran en el `views.xml` de la fase cuyo form incluye el panel, no en el de la raíz).

### 6.2 Un campo que solo aparece en una condición necesita su `<field hidden="true"/>`

El cliente solo recibe los campos que la vista declara como `<field>`: los que solo se nombran dentro de un `showIf`/`hideIf`/`readonlyIf`/`requiredIf` no cuentan.
Si el campo de la condición no es además un `<field>` del form, llega `undefined`, la condición evalúa a falso **sin ningún aviso** y el elemento no se pinta nunca.

- **MUST** declarar `<field name="<campo>" hidden="true"/>` en el **mismo panel** que usa el campo en una condición, si ese panel no lo pinta ya como `<field>`.
  En el mismo panel y no en otro: cada form de estado incluye solo algunos paneles, así que el panel no puede contar con que otro lo declare.
- Si el panel ya pinta el campo (aunque sea `readonly`), no hace falta nada más.
- Colócalo donde no descuadre la rejilla: un campo oculto ocupa su hueco (§3.1).

- ✅ CORRECTO:
  ```xml
  <panel name="resolucion-firmada" title="Resolución" colSpan="12">
      <field name="tipoResolucion" hidden="true" widget="RadioSelect" x-direction="horizontal"/>
      <field name="motivoRechazo" colSpan="12" readonly="true" showIf="tipoResolucion=='RECHAZAR'"/>
  </panel>
  ```
- ❌ INCORRECTO: el mismo panel sin el `<field name="tipoResolucion" hidden="true" …/>` (el cliente no recibe `tipoResolucion` y `motivoRechazo` no se ve nunca, tampoco cuando se rechazó).
- ❌ INCORRECTO: confiar en que `tipoResolucion` ya lo pinta el panel `resolucion` (ese panel no se incluye en los forms de los estados finales, que es donde va `resolucion-firmada`).

### 6.3 Campos de tipo enumerado: `RadioSelect`

La convención de `k-vistas` (`forms.md`, «Campos de tipo enumerado: `RadioSelect`») **sí** aplica a los forms de los trámites, pese a la exclusión general del inicio de este fichero.

- Todo `<field>` de tipo enumerado editable **MUST** llevar `widget="RadioSelect"` y `x-direction` (`horizontal` hasta 4 valores, `vertical` con 5 o más).
- Los de solo lectura quedan libres del `RadioSelect` (pueden pintarse como texto); si lo llevan, el `x-direction` se les exige igual. Un campo es de solo lectura si:
  - lleva `readonly="true"` él o un ancestro hasta el `<form>` (un `<panel readonly="true">`);
  - o su panel de la plantilla se incluye en algún `<include-panels>` y **siempre** con el prefijo `-` (§3); basta una inclusión sin guion, en cualquier estado del tipo (o de cualquier tipo, si el panel es común), para que el campo cuente como editable. Un panel que no se incluye nunca no es de solo lectura.
- Cuentan los campos del tipo y los heredados de `Expediente`, en el form plantilla, en los forms auxiliares y en los paneles comunes.
- Cuentan también los ocultos (los `<field hidden="true"/>` de §6.2) y los de dentro de un `<editor>`, que son de la entidad relacionada.
- No cuentan los `<field>` hijos de un `panel-related`.
- Lo comprueban los tests W1–W3 (`SKILL.md` §3.3).

## 7. Paneles gemelos `-view` para el modo lectura

Cuando la versión de solo lectura de un panel necesita **otro layout** (otros `colSpan`, otros títulos, campos que sobran), el prefijo `-` no basta (reutiliza el layout de edición tal cual): la convención es declarar en la plantilla un **panel gemelo** con sufijo `-view`, ya maquetado para lectura y con sus fields `readonly="true"`, y elegir por estado cuál se incluye (`datos-solicitud` y `datos-solicitud-view` en el form plantilla; los estados de edición incluyen el primero y los de lectura el segundo).

- `-panel` → mismo layout, fields readonly. `panel-view` → layout propio de lectura.
- El gemelo `-view` se incluye normalmente también con `-` (`-datos-solicitud-view`) por si algún field no lleva el readonly explícito.

## 8. Maestro-detalle: entidades hija

El `domains.xml` del tipo puede declarar entidades hija (one-to-many del expediente). En las vistas:

1. En el form plantilla, un **`<panel-related name="..." field="<campo one-to-many>" grid-view="..." form-view="..."/>`** con nombre → incluible por estado como cualquier panel (con la trampa del `-` de §3: nunca queda readonly por el prefijo).
2. El **grid y el form del hijo** se declaran en el `views.xml` de la **raíz de la versión** (junto al form plantilla, no en una fase: son de todo el tipo) como vistas Axelor normales, convención `exp-<Code>-<EntidadHija>-grid` / `-form`.
3. El form del hijo puede usar también `<include-panels header="false">` (sin cabecera de expediente) y **`<footer/>` vacío**: los hijos no disparan eventos.
4. Validación del hijo al confirmar su popup: `onValidate="subsysTramitador-validate-on-save-child-action"` en el form del hijo: le aplica las reglas anidadas que el validador del estado actual declara sobre el campo one-to-many.
5. Puede haber **varios form-view del mismo hijo** (p. ej. uno de edición y otro de firma/lectura): se declara un `panel-related` con nombre distinto por cada combinación y cada estado incluye el suyo.

## 9. Patrón: visor de PDF embebido

Para mostrar un campo `many-to-one` a `MetaFile`, panel con un field *dummy* cuyo `<viewer>` pinta un iframe al download inline. Un panel con nombre por cada PDF, para incluirlo por estado. Los visores de `pdfSolicitud`, `pdfSolicitudFirmada` y `pdfJustificanteRegistroEntrada` **ya son comunes** (§3.2): declara en el form plantilla del tipo solo los de sus propios documentos.

- El `name` del dummy no existe en la entidad; el campo real va en el `depends` del viewer. Dale un nombre que diga qué muestra (`visorResolucion`), distinto en cada panel.
- El `title` del panel dice qué documento es: cada visor el suyo, no «Documento» en todos.
- Para descargar sin visor basta el propio campo con `widget="binary-link"`.

```xml
<panel name="pdfResolucion" title="Resolución">
    <field name="visorResolucion" showTitle="false" readonly="true" colSpan="12">
        <viewer depends="pdfResolucion"><![CDATA[
            <>
            <Box as="iframe" height="900" border="0" src={`ws/rest/com.axelor.meta.db.MetaFile/${pdfResolucion.id}/content/download?inline=true&name=${pdfResolucion.fileName}`} ></Box>
            </>
        ]]></viewer>
    </field>
</panel>
```

## 10. Comprobaciones del build y trampas

- **MUST** haber exactamente un form plantilla que case con `exp-<Code>-Templates` en el `views.xml` de la raíz de la versión. Dos o más → error claro; **cero** → error explícito al preprocesar el `views.xml` de cualquier fase. El patrón se evalúa como substring y el `<Code>` no puede llevar guiones ni underscores.
- El `<Code>` de ese form plantilla **MUST** ser el del **propio tipo**, no el de otro. Es la comprobación que caza el `<Code>` que se queda sin actualizar al duplicar una versión (`recetas/versionado.md`), que si no seguiría casando el patrón y compilando.
- El `profile` de cualquier `<form>` **MUST** estar en la **unión de perfiles del tipo** (los que usa algún estado de su `TipoExpedienteInstance.xml`); si no, el build falla porque esa vista no se pintaría nunca. Ojo: se valida contra la unión del tipo, **no** contra el `profile` del estado del propio form (§2).
- La carpeta de un `views.xml` con `<form state=...>` **MUST** corresponder a una fase declarada en el `TipoExpedienteInstance.xml` (el nombre de la fase en minúsculas). Si no, el build dice qué fases hay.
- Un `views.xml` con `<form state=...>` **MUST NOT** estar en la raíz de la versión: ahí solo va el form plantilla.
- **CRITICAL — un `<object-views>` sin ningún elemento hijo tumba el arranque**, y el build **no lo detecta**: la aplicación arranca **sin vistas, sin menús y sin data-init** ("The content of element 'object-views' is not complete"). Los comentarios no cuentan como contenido: si una fase se queda sin forms, borra su `views.xml`.

## 11. Anti-patrones

- **MUST NOT** aplicar aquí las reglas VAR de `view-rules.md` ni los tests de vistas: este formato está excluido.
- **MUST NOT** usar `includeHeader` (se ignora): el atributo real es `header`.
- **MUST NOT** meter la fase en el atributo `state`: ahí va solo el nombre del estado y la fase la añade el preprocesador.
- **MUST NOT** duplicar el form plantilla en las carpetas de fase: es uno solo, en la raíz de la versión.
- **MUST NOT** confiar en `readonly`/`showIf` como seguridad: la defensa real es la whitelist del validator (`k-secure-coding`).
- **MUST NOT** nombrar un panel local igual que uno global salvo que quieras sobreescribirlo a propósito; nunca los de las fases comunes (§3.2).
- **MUST NOT** incluir `firma-solicitud` con guion ni poner botón a un evento de sistema.
- **MUST NOT** confiar en el prefijo `-` para "desactivar" un panel con botones ni un `panel-related`: solo pone readonly los `<field>` (§3).
- **MUST NOT** usar en un `showIf`/`hideIf`/`readonlyIf`/`requiredIf` un campo que el panel no declara como `<field>`: sin su `<field hidden="true"/>` la condición evalúa siempre a falso (§6.2).
- **MUST NOT** dejar un form de solo `EXIT` sin el panel `avisoEstadoExpediente` (§6.1): el usuario no sabría en qué situación está su expediente.
