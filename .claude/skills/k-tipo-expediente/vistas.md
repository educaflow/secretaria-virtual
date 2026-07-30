# Las vistas del tipo de expediente (`views.xml`)

**CRITICAL**: las vistas de los tipos de expediente **NO siguen las normas de `k-vistas` ni `view-rules.md`** (están excluidas). Se escriben en un formato propio que el preprocesador de EducaFlowBuildTools (`viewProcessorTask`) convierte en vistas Axelor estándar durante el build. Ejemplo real: `tramites/justificacion_falta_profesorado/v1/views.xml`.

## 1. Las dos piezas del fichero

1. **Un form plantilla** (Axelor normal) `exp-<Code>-Templates`: **almacén de paneles con nombre**. Nunca se muestra tal cual; sus atributos (`model`, `width`, `groups`…) se heredan y sus paneles se copian a las vistas finales.
2. **Un form por estado (y opcionalmente perfil)** con los tags custom `<form state=...>`, `<include-panels>` y `<footer>`.

```xml
<form name="exp-JustificacionFaltaProfesoradoV1-Templates" width="large" title="..."
      model="com.educaflow.subsystem.expedientes.db.JustificacionFaltaProfesoradoV1" groups="admins,users">
    <panel name="datos-falta" title="Datos de la falta"> ... </panel>
    <panel name="justificante-upload" title="Adjuntar justificante"> ... </panel>
    ...
</form>

<form state="ENTRADA_DATOS" profile="CREADOR">
    <include-panels>
        -datos-profesor          <!-- con guion: se incluye con TODOS sus fields readonly -->
        datos-falta              <!-- sin guion: editable -->
        justificante-upload
    </include-panels>
    <footer>
        <buttons-left>
            <button name="DELETE" colSpan="2" css="btn-danger" outline="true" icon="trash" title="Borrar el expediente"
                    onClick="subsysExpedientes-event-action" prompt="¿Está seguro que desea borrar el expediente?"/>
        </buttons-left>
        <buttons-right>
            <button name="GUARDAR_DATOS" colSpan="2" title="Siguiente" onClick="subsysExpedientes-event-action"/>
        </buttons-right>
    </footer>
</form>
```

## 2. Dos forms por estado y cómo elige el runtime

- `<form state="X" profile="Y">` → `exp-<Code>-<STATE>-<PROFILE>-form` (la del perfil que "tiene el turno", editable).
- `<form state="X">` (sin perfil) → `exp-<Code>-<STATE>-form` (la genérica, normalmente todo readonly + botón `EXIT`).
- El runtime busca primero la del perfil actuante y si no existe usa la genérica; si no hay ninguna → excepción "No existe la vista en el expediente" (solo en runtime, el build no lo comprueba).
- El esqueleto ya genera ambas por estado; escribe la del perfil dueño editable y la genérica de solo lectura.

## 3. `<include-panels>`

- Cada línea es el `name` de un panel; prefijo `-` = copia con todos sus `<field>` a `readonly="true"`.
- Búsqueda: **primero** en el form plantilla del propio fichero, si no en los `template-views.xml` globales (`tramites/shared/`). Un panel local con el mismo nombre que uno global lo **sobreescribe en silencio** (incluidos los de cabecera/footer).
- Por defecto se antepone la cabecera global (`subsysExpedientes-template-header-panel`); `header="false"` la quita. **CRITICAL**: el atributo es `header`, NO el `includeHeader` que trae el esqueleto (ese se ignora); solo admite `true`/`false` (otro valor → "Valor fuera de rango"); listar la cabecera explícitamente en la lista permite reconfigurarla (p.ej. readonly).
- Un panel referenciado que no existe → el build falla ("No existe el panel con nombre:...").
- Un panel repetido en la lista se incluye una vez (gana el flag readonly de la última aparición), sin aviso.
- Los `<include-panels>` y `<footer>` se expanden **en cualquier punto del documento**, no solo dentro de forms con `state`.

## 4. `<footer>`

- Se sustituye por el panel global `subsysExpedientes-template-footer-panel` (que además pinta los mensajes de error de validación) con tus botones dentro de `<buttons-left>`/`<buttons-right>`.
- **El `name` de cada botón es el evento que dispara**; todos usan `onClick="subsysExpedientes-event-action"`. Admiten atributos Axelor normales (`title`, `colSpan`, `prompt`, `css`, `outline`, `icon`).
- El `colSpan` por defecto de cada botón es el `itemSpan` del panel footer (default 1).
- Al primer botón de la derecha se le asigna **siempre** (sobrescribiendo cualquier valor manual) `colOffset = 12 − suma de colSpan` de todos los botones, para alinearlo al margen derecho. Si la suma pasa de 12, el offset sale negativo sin aviso.
- Los eventos comunes `EXIT` y `DELETE` responden al cliente con `refresh-app` (se recarga la aplicación entera, no se navega a otra vista).

## 5. Herencia y des-herencia de atributos

Los forms de estado heredan los atributos del form plantilla. Declarar un atributo con valor **en blanco** lo **elimina** del resultado (`width=""` quita el `width="large"` heredado). El título por defecto es el `code` humanizado con reglas concretas: separa el camelCase, capitaliza solo la primera palabra y pasa el resto a minúsculas conservando las siglas (`JustificacionFaltaProfesoradoV1` → "Justificacion falta profesorado v1").

## 6. Contenido Axelor adicional

Un form de estado puede llevar además paneles Axelor normales (fuera de `<include-panels>`): el expediente real añade tras el footer un `<panel showFrame="false">` con un `<help variant="info">` de ayuda.

## 7. Patrón: visor de PDF embebido

Para mostrar un campo `many-to-one` a `MetaFile`, panel con un field *dummy* cuyo `<viewer>` pinta un iframe al download inline. Un panel con nombre por cada PDF, para incluirlo por estado:

```xml
<panel name="pdfSolicitud" title="Solicitud">
    <field name="new" showTitle="false" readonly="true" colSpan="9">
        <viewer depends="pdfSolicitud"><![CDATA[
            <>
            <Box as="iframe" height="900" border="0" src={`ws/rest/com.axelor.meta.db.MetaFile/${pdfSolicitud.id}/content/download?inline=true&name=${pdfSolicitud.fileName}`} ></Box>
            </>
        ]]></viewer>
    </field>
</panel>
```

## 8. Patrón: botón de firma con AutoFirma

`<action-method>` declarada en el propio `views.xml` que llama al controlador genérico de firmas, encadenada con `serial:` **antes** del evento:

```xml
<action-method name="exp-<Code>-firmarDocumentacionParaPresentar-action">
    <call class="com.educaflow.subsystem.expedientes.controllers.FirmaController"
          method='firmarDocumentoEntrada(id,"pdfSolicitud","pdfSolicitudFirmado",100,20,600,100,1)'/>
</action-method>
...
<button name="PRESENTAR" title="Firmar con AutoFirma__!! y Presentar la solicitud"
        onClick="serial:exp-<Code>-firmarDocumentacionParaPresentar-action,subsysExpedientes-event-action"/>
```

`firmarDocumentoEntrada(id, campoOrigen, campoDestino, x, y, ancho, alto, página)` lanza AutoFirma sobre el MetaFile del campo origen, deja el firmado en el destino y exige firmar con el `dniFirmaDocumentoEntrada` del expediente. Las otras dos piezas del patrón: `modelo.md` §4 y `validator.md` §4.

## 9. Comprobaciones del build y trampas

- **MUST** haber exactamente un form plantilla que case con `exp-<Code>-Templates`. Dos o más → error claro; **cero** → NPE críptico ("Fallo al prerocesar el fichero"). El patrón se evalúa como substring y el `<Code>` no puede llevar guiones ni underscores.
- Todos los XML con raíz `object-views` deben ser parseables.
- El preprocesador re-escribe **todas** las vistas en la copia al build (re-indentado); no afecta al fuente.

## 10. Anti-patrones

- **MUST NOT** aplicar aquí las reglas VAR de `view-rules.md` ni los tests de vistas: este formato está excluido.
- **MUST NOT** usar `includeHeader` (se ignora): el atributo real es `header`.
- **MUST NOT** confiar en `readonly`/`showIf` como seguridad: la defensa real es la whitelist del validator (`k-secure-coding`).
- **MUST NOT** nombrar un panel local igual que uno global salvo que quieras sobreescribirlo a propósito.
