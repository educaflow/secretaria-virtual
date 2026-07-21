# Estructura y patrones básicos de los formularios en el proyecto

## Plantilla básica de un formulario

```xml
<form name="subsysSistemaEducativo.Main@Ciclo-form" title="Ciclo" model="com.educaflow.subsystem.sistemaeducativo.db.Ciclo"
      width="large" canAttach="false" canBack="false" canDelete="false" canNew="false" canSave="false" canMore="false" canBackOnSave="true">
    <panel name="Ciclo" title="Ciclo">
        <field name="code"/>
        <field name="name"/>
    </panel>

   <panel name="otroPanel" title="Otro panel">
      <field name="centro" form-view="subsysCentro.Ref@Centro-form" grid-view="subsysCentro.Ref@Centro-grid"  />
      <field name="grado" colOffset="6" colSpan="4"              grid-view="subsysSistemaEducativo.Ref@Grado-grid"  domain="(self.code='D' OR self.code='E')" />
      <field name="nivel" colSpan="4"                            grid-view="subsysSistemaEducativo.Ref@Nivel-grid"  showIf="grado.code=='D'" requiredIf="grado.code=='D'" domain="(self.code='D' OR self.code='E')"/>       
   </panel>

    <panel-related name="modulos" field="modulos" title="Módulos" newButtonTitle="Añadir un nuevo módulo"
        grid-view="subsysSistemaEducativo.Main@Ciclo.Curso.CursoModulo-grid" form-view="subsysSistemaEducativo.Main@Ciclo.Curso.CursoModulo-form"
        colSpan="12" showFooter="false" canEdit="false" canRemove="false" forceEdit="true"
    />
   
    <panel name="buttons-panel" title="" colSpan="12" showFrame="false" >
        <button name="btnDelete" title="Borrar" onClick="subsysSistemaEducativo.Main@Ciclo-btnDelete-action" css="btn-danger" colSpan="2"  outline="true" showIf="(id!=null) || (cid!=null)"/>
        <button name="btnCancel" title="Cancelar" onClick="subsysSistemaEducativo.Main@Ciclo-btnCancel-action"  colSpan="2" colOffset="6" outline="true"   />
        <button name="btnSave" title="Guardar" onClick="subsysSistemaEducativo.Main@Ciclo-btnSave-action"  colSpan="2"  />
    </panel>
    
</form>
```

IMPORTANTE:
 - En <form> deben estar todos los atributos que se han indicado en la plantilla (width, canAttach, canBack, canDelete, canNew, canSave, canMore, canBackOnSave) con los valores indicados. `canBackOnSave="true"` solo aplica al form principal (no al modal de entidad hija). El form modal **no lleva `canBackOnSave`** y **sí lleva `onNew`** para inyectar la referencia al padre.
 - En <panel-related> deben estar todos los atributos que se han indicado en la plantilla (newButtonTitle, colSpan, showFooter, canEdit, canRemove, forceEdit) con los valores indicados.
 - El botón Borrar debe tener `showIf="(id!=null) || (cid!=null)"` — `id` es el ID del registro ya guardado; `cid` es el ID temporal de un registro nuevo todavía no guardado.
 - El `<action-group>` del botón `btnSave` **MUST** incluir la acción global `remote-validationSave-action`, después `<action name="save"/>` y **terminar con `<action name="back"/>`** (o `force-back`); el del botón `btnDelete` **MUST** incluir la acción global `remote-validationDelete-action` y terminar con `<action name="delete"/>` (`save`/`delete`/`back` son acciones predefinidas del framework de Axelor; las `remote-validation*` son las acciones globales de validación remota de `DefaultModelController` — ver `k-validaciones/validaciones.md` §4-§5 y `[[actions.md]]`). **MUST NOT** llamar a un `<action-method>` propio (`Remote-…-action`) para validar, persistir o borrar en save/delete: Axelor ya expone el endpoint REST `/ws/rest/<FQN>` que aplica `validate*` con `AllowProperties`. Ver `[[controladores.md]]` del skill `k-sistemas` y `[[k-secure-coding]]`.
 - **El `back` tras `save` es OBLIGATORIO** aunque el form lleve `canBackOnSave="true"`: si el usuario pulsa Guardar sin cambiar nada, `save` es un no-op y `canBackOnSave` NO cierra la ventana; el `<action name="back"/>` explícito la cierra siempre. (`force-back` fuerza el cierre descartando cambios pendientes de un sub-form.)
 - Los nombres de los `onClick` de los botones siguen el patrón `{marcadorMódulo}.Main@{EntidadJerárquica}-{btnXxx}-action`, donde `{EntidadJerárquica}` puede incluir la jerarquía de entidades separadas por punto (p.ej. `Ciclo.Curso`). Por ejemplo: `subsysSistemaEducativo.Main@Ciclo-btnDelete-action` para la entidad raíz, o `subsysSistemaEducativo.Main@Ciclo.Curso-btnDelete-action` para la entidad hija.
 - **Botones gemelos**: dos botones con `showIf` excluyentes pueden compartir el mismo `action-group`; el `name` de cada botón **MUST** empezar por el `{btnXxx}` del `onClick`. Ejemplo: `btnCancelAlta` (`showIf="(id == null) && (cid == null)"`, título "Cancelar") y `btnCancelSalir` (`showIf="(id != null) || (cid != null)"`, título "Salir") comparten `…-btnCancel-action`.
 - Los nombres de los paneles siguen el patrón del nombre de la entidad (p.ej. `name="Ciclo"`), no nombres genéricos como `nombrePanel1`.
 - En campos relacionales: `form-view` apunta al `Ref@…-form` de la entidad (p.ej. `subsysCentro.Ref@Centro-form`) y `grid-view` apunta al `Ref@…-grid` (p.ej. `subsysCentro.Ref@Centro-grid`).

## Form modal (entidad hija en `panel-related`)

Cuando una entidad hija se edita desde un `<panel-related>`, su formulario es un **modal** con diferencias importantes respecto al form principal:

```xml
<form name="subsysSistemaEducativo.Main@Ciclo.Curso-form" title="Curso" model="com.educaflow.subsystem.sistemaeducativo.db.Curso"
      width="large"
      onNew="subsysSistemaEducativo.Main@Ciclo.Curso-onNew-action"
      canAttach="false" canBack="false" canDelete="false" canNew="false" canSave="false" canMore="false">
    <panel name="Curso" title="">
        <field name="ciclo" showIf="false"/>   <!-- campo padre, oculto pero presente en el modelo -->
        <field name="code" colSpan="3"/>
        <field name="name" colSpan="6" colOffset="3"/>
    </panel>

    <panel name="buttons-panel" title="" colSpan="12" showFrame="false">
        <button name="btnDelete" title="Borrar" onClick="subsysSistemaEducativo.Main@Ciclo.Curso-btnDelete-action"
                css="btn-danger" colSpan="2" outline="true" showIf="(id!=null) || (cid!=null)"/>
        <button name="btnCancel" title="Cancelar" onClick="subsysSistemaEducativo.Main@Ciclo.Curso-btnCancel-action"
                colSpan="2" colOffset="6" outline="true"/>
        <button name="btnSave" title="Guardar" onClick="subsysSistemaEducativo.Main@Ciclo.Curso-btnSave-action"
                colSpan="2"/>
    </panel>
</form>
```

Diferencias respecto al form principal:
- **Sin `canBackOnSave`** — el cierre del modal lo gestiona `save-modal` en el action-group del botón guardar.
- **Con `onNew`** — inyecta la referencia al padre cuando se crea un registro nuevo.
- **Campo padre con `showIf="false"`** — está en el modelo pero no es visible al usuario.

Los action-groups de los botones del form modal usan acciones específicas del framework (predefinidas por Axelor). El `<action-group>` del botón `btnSave` es `Local-validateSave-action` (solo si el detalle tiene validaciones evaluables en cliente) → `<action name="save-modal"/>`; el del botón `btnDelete` termina con `<action name="delete-modal"/>`. **MUST NOT** llamar a un `<action-method>` propio para persistir o borrar — igual que en el form principal, ver `[[controladores.md]]` del skill `k-sistemas`:

- Botón Borrar: `<action name="delete-modal"/>` (no `delete`)
- Botón Cancelar: `<action name="close"/>` (no `back`)
- Botón Guardar: `<action name="save-modal"/>` (no `save`)

**CRITICAL — en el form modal de un detalle, la validación cliente es la ÚNICA antes de cerrar (y por eso MUST ser lo más completa posible):**

- `save-modal`/`delete-modal` **no llaman al servidor**: solo confirman/quitan el registro en la colección en memoria del form padre. No se ejecuta ninguna validación de servidor al cerrar el modal.
- **MUST NOT** incluir `remote-validationSave-action`/`remote-validationDelete-action` en los action-groups del form modal: el maestro puede no existir todavía en BD y la validación de servidor del detalle fallaría espuriamente.
- Las validaciones reales de servidor del detalle se ejecutan **cuando se guarda el maestro** (`ModelServiceValidationWalker` recorre los detalles al hacer `save` del form raíz).
- Por ello el `Local-validateSave-action` del form modal **MUST** duplicar **todas** las validaciones del detalle evaluables en cliente (obligatorios, formatos, comparaciones entre campos, comparaciones con el padre vía `__parent__`), aunque repitan las del servidor: es la única forma de avisar al usuario **antes** de cerrar la ventana, en vez de con un error del maestro al guardar al final. Patrones listos en `k-validaciones/examples/ejemplos-validaciones.md` (P1–P6; P4 para comparar con el padre).

### Tabla comparativa: form principal vs form modal

| Aspecto                | Form principal | Form modal                                |
|------------------------|----------------|-------------------------------------------|
| `canBackOnSave`        | `true`         | ausente                                   |
| `onNew`                | ausente        | presente (inyecta el padre)               |
| Campo padre            | no existe      | `showIf="false"`                          |
| `<action-view>` propio | sí             | no (lo abre el `panel-related` del padre) |
| Botón Borrar acción    | `delete`       | `delete-modal`                            |
| Botón Cancelar acción  | `back`         | `close`                                   |
| Botón Guardar acción   | `save` → `back`/`force-back` | `save-modal`                |
| Validación remota (`remote-validation*`) | sí, antes de `save`/`delete` | **no** (el maestro puede no existir en BD) |
| Validación cliente (`Local-validate*`)   | opcional (solo UX)           | **MUST, lo más completa posible** (única validación antes de cerrar el modal) |

## Botones principales y secundarios
- Los botones principales (guardar, cancelar, etc) están a la derecha del todo
- Las acciones secundarias (borrar, imprimir, etc.) están a la izquierda del todo
- Realmente no es necesario que estén exactamente estos botones sino que podría haber otros botones con otras acciones. Pero hay que distingir claramente las acciones principales de las secundarias y para eso se siguen estas pautas de colocación.
- El panel de botones siempre debe incluir Borrar, Cancelar y Guardar salvo que se indique lo contrario o haya algo en el negocio que te haga pensar que no es necesario.

## Nombre de los formularios
El nombre de las vistas de formularios es: `{marcadorMódulo}.[Main|Ref|otra variante]@{Entidad}[.{EntidadHija}]*-form`

Una excepción a esta convención es el caso de las vistas del framework de tipos de expediente, expedientes o trámites. En ese caso aun no se ha definido una convención de nombres específica, pero se ha decidido reservar el marcador `exp-` para todas las vistas relacionadas con ese framework, de esa forma se pueden identificar fácilmente y no se solapan con las vistas de los subsistemas o sistemas funcionales. Por ejemplo, una vista de formulario para un tipo de expediente podría llamarse `exp-TipoExpediente@Main-form`.
Otra excepción es el caso de formularios del propio Axelor que se modifican para adecuarlos a las necesidades del proyecto, en ese caso se pueden mantener los nombres originales de Axelor. Un ejemplo es el formulario 'user-preferences-form'


### Marcador de módulo

El **marcador de módulo** es la cabecera del prefijo (todo lo anterior al `@`): el marcador de capa (`subsys`/`sys`) pegado al nombre del módulo/carpeta.

- Subsistemas: `subsys{Subsistema}` (PascalCase sin separador), p.ej. `subsysFirma`, `subsysRegistroEntradaSalida`
- Sistemas: `sys{Sistema}` (PascalCase sin separador), p.ej. `sysImportar`
- Excepción: el marcador `exp-` se reserva exclusivamente para las vistas del framework de tipos de expediente
- Las entidades de la ruta de entidad se separan con `.` (punto), y el prefijo se separa del sufijo con `@`


| Caso                             | Patrón                                                 | Ejemplo                                             |
|----------------------------------|--------------------------------------------------------|-----------------------------------------------------|
| Pantalla principal               | `{marcadorMódulo}.Main@{Entidad}-form`                        | `subsysSistemaEducativo.Main@Ciclo-form`            |
| Pantalla de Solo lectura         | `{marcadorMódulo}.Ref@{Entidad}-form`                        | `subsysSistemaEducativo.Ref@Ciclo-form`             |
| Otra pantalla distinta           | `{marcadorMódulo}.{Variante}@{Entidad}-form`                    | `subsysSistemaEducativo.Pendiente@Ciclo-form`       |
| Entidad anidada                  | `{marcadorMódulo}.Main@{EntidadPadre}.{EntidadHija}-form`     | `subsysSistemaEducativo.Main@Ciclo.Curso-form`      |
| Entidad anidada de otra pantalla | `{marcadorMódulo}.{Variante}@{EntidadPadre}.{EntidadHija}-form` | `subsysSistemaEducativo.Pendiente@Ciclo.Curso-form` |

**IMPORTANTE: Es obligatorio seguir esta convención de nombres para facilitar la trazabilidad, la lectura y el mantenimiento del código.**

## Layout y diseño de formularios
    - El layout de los formularios se organiza principalmente con paneles (`<panel>`) y paneles relacionados (`<panel-related>`).
    - Dentro de los paneles se colocan los campos (`<field>`) y otros widgets (botones, secciones de ayuda, etc.).
    - Se sigue una maquetación de 12 columnas usando `colSpan` y `colOffset` para distribuir los campos en el espacio del panel.
    - Se usan condicionales (`showIf`, `hideIf`, `requiredIf`, `readonlyIf`) para mostrar, ocultar o modificar la interactividad de campos y paneles según el estado del formulario o los datos.
    - Se pueden incluir secciones de ayuda dentro de los paneles usando `<help variant="info|warning|...">` para guiar al usuario.
    - Los botones de acción se colocan dentro de un panel específico (normalmente al final del formulario) y se configuran con `onClick` para lanzar las acciones correspondientes.

> **CRITICAL — `readonly`/`showIf`/`hideIf`/`hidden`/`required` NO son defensas de seguridad.** Estos atributos solo afectan al cliente. Un atacante con Postman/curl ignora la UI y envía cualquier campo al endpoint REST genérico `POST /ws/rest/<FQN>`. La defensa real vive en el servidor: `AllowProperties` en el controller + asignación incondicional de campos `servidor` en `*ServiceImpl.insert/update` + `validateInsert`/`validateUpdate`. Si necesitas que un campo no se pueda enviar desde el cliente, **MUST** consultar `[[k-secure-coding]]` §1-§2 — no basta con marcarlo `readonly` o `hidden` en la vista.

## CRÍTICO: Campos condicionales y el problema de los huecos en el grid

Los campos ocultos con `showIf` en Axelor siguen ocupando espacio en el grid CSS porque tienen asignación explícita de columnas. Esto genera **huecos visuales** cuando un campo está oculto.

**Ejemplo del problema:**
```xml
<!-- MAL: cuando campoA está oculto, campoB aparece desplazado a la derecha, dejando cols vacías a la izquierda -->
<field name="campoA" colSpan="6" showIf="tipo == 'MODO_X'"/>
<field name="campoB" colSpan="6" showIf="tipo == 'MODO_X' || tipo == 'MODO_Y'"/>
```

**Solución: Paneles anidados con `showIf` en el panel (no en el campo)**

Un panel oculto no ocupa espacio vertical (es un bloque que desaparece). Los campos dentro de un panel visible sí ocupan su espacio en el grid horizontal.

```xml
<!-- BIEN: cada modo tiene su propio panel. Cuando el panel está oculto, no deja hueco -->
<panel name="panelModoX" colSpan="12" showIf="tipo == 'MODO_X'" showFrame="false">
    <field name="campoA" colSpan="3"/>
    <field name="campoB" colSpan="9"/>
</panel>
<panel name="panelModoY" colSpan="12" showIf="tipo == 'MODO_Y'" showFrame="false">
    <field name="campoB" colSpan="4"/>
    <field name="campoC" colSpan="8"/>
</panel>
```

**Regla:** Siempre que varios campos se muestren/oculten de forma exclusiva según el valor de otro campo, agrúpalos en paneles anidados con `showIf` en el panel. Nunca uses `showIf` directo en campos de la misma fila cuando alguno puede quedar oculto dejando el otro desplazado.

El mismo campo puede aparecer en varios paneles mutuamente excluyentes (p.ej. `campoB` en panelModoX y panelModoY). Al ser excluyentes, Axelor siempre ve solo uno activo y el binding de datos funciona correctamente.

## Principios de diseño visual de formularios

### Agrupación semántica de campos
Campos relacionados semánticamente deben ir en la misma fila:
- Fecha de inicio + Fecha de fin → misma fila
- Slot + PIN (acceso a dispositivo) → misma fila
- Slot + Alias (identificación de certificado en dispositivo) → misma fila
- Fichero (subida) + Contraseña (para abrirlo) → misma fila
- Ruta de un fichero + Contraseña → misma fila o filas contiguas en el mismo panel

### Alineación vertical entre filas

Siempre que sea posible, los bordes de columna deben repetirse entre filas. Si la primera fila usa un split 4+8, las filas siguientes —especialmente los paneles anidados condicionales— deberían usar ese mismo split. No es una regla rígida: a veces el contenido justifica un split diferente. Pero cuando se puede mantener la alineación, el resultado visual es notablemente mejor.

Si cambias el split de la primera fila, revisa si los paneles condicionales siguen alineados o hay que actualizarlos también.

**Ejemplo con alineación:**
```
aaaabbbbbbbb   ← campoCorto(4) + selector(8)  [fila 1]
ccccdddddddd   ← campoCorto(4) + contenido(8) [panel modo1: border en col 4|5]
cccceeeeeeee   ← campoCorto(4) + contenido(8) [panel modo2: border en col 4|5]
ccccffffffff   ← campoCorto(4) + contenido(8) [panel modo3: border en col 4|5]
```

**Ejemplo sin alineación (peor):**
```
aaaabbbbbbbb   ← campoCorto(4) + selector(8)
ccccccdddddd   ← campoCorto(6) + contenido(6) [border en col 6|7 ≠ 4|5]
```

### Proporcionalidad al contenido: label + tipo de dato

El `colSpan` debe reflejar el espacio que ocupan **tanto el título del campo como el valor** que el usuario va a introducir o ver. Un label corto + un valor corto = pocas columnas, aunque el formulario tenga espacio libre.

| Tipo de campo | Ejemplos de título | Ejemplos de valor | colSpan orientativo |
|---|---|---|---|
| Número/código muy corto | "Slot", "Nº" | 0, 1, 2 | **2** |
| PIN / código corto | "PIN", "CVV" | 1234, AB12 | **3** |
| DNI / código identificador | "DNI", "Código" | 12345678Z | **3** |
| Fecha | "Fecha inicio" | 01/01/2025 | **3** |
| Nombre corto / identificador | "Nombre", "Alias" | "DNIe", "HSM prod" | **6–8** |
| Nombre o descripción media | "Descripción", "Asunto" | texto moderado | **8–10** |
| Ruta de fichero / path | "Ruta librería", "Ruta classpath" | /usr/lib/.../opensc.so | **9–10** |
| Selector enum largo | "Tipo", "Estado" | "Opción con texto largo" | **4–6** |
| Widget compacto (binary-link) | "Fichero" | [botón subir] | **3–4** |
| Campo de texto libre / multiline | "Motivo", "Observaciones" | texto largo | **12** |

**Regla clave:** no asignar 12 columnas a un campo solo porque "puede ser largo". Pensar en el valor típico real. Un nombre corto rara vez supera 20 caracteres → 6-8 cols es generoso. Una ruta de fichero puede tener 40 caracteres → 9–10 cols. Un selector enum muestra el texto con scroll interno → no necesita tantas cols como su opción más larga. Un número o código muy corto → 2 cols.

### Un campo solo en una fila es una señal de alerta
Si un campo queda solo en una fila con mucho espacio vacío a su derecha, es probable que:
1. Deba agruparse con otro campo relacionado en la misma fila, o
2. Su `colSpan` sea demasiado pequeño para el espacio disponible, o
3. Sea el resultado de un campo oculto que crea un hueco (usar paneles anidados)

Un campo corto (slot, PIN) que queda solo en una fila con 9 columnas vacías **sigue siendo correcto** si no hay ningún campo semánticamente relacionado con el que agruparlo. No hay que rellenar el espacio a la fuerza.



## `<panel-related>`
Se usa para colecciones relacionales `<one-to-many>` del modelo y muestra una rejilla hija dentro del formulario padre, se acompaña con los atributos `grid-view` y `form-view` específicos.


## Campos (`field`) y widgets clave en este proyecto

`<field>` es la etiqueta **más importante** de un form y vincula un atributo del modelo al formulario. Además de `name`, aquí se define gran parte de la UX mediante atributos y widgets. Un campo siempre debe estar dentro de un panel

### Atributos
- `domain`:Permite restringir los valores disponibles en campos de selección (por ejemplo, campos relacionales o enums) usando expresiones booleanas que hacen referencia a los atributos del campo. Por ejemplo, para mostrar un campo solo si el código es 'D' o 'E', se usaría: `domain="(self.code='D' OR self.code='E')"`
- `showIf`: Permite mostrar un campo solo si se cumple una condición. Por ejemplo, para mostrar un campo solo si el código es 'D', se usaría: `showIf="grado.code=='D'"` (se referencia directamente el campo del formulario, sin prefijo `self.`)
- `widget="binary-link"`: para campos `MetaFile` permite cargar/descargar un fichero.
- `widget="binary"`: Para descargar directamente el `content` del  ̀MetaFile`.
- `x-accept`: para restringir tipos de fichero (por ejemplo PDF o imagen).
- `widget="SwitchSelect"`: Para campos del modelo de tipo enum (horizontal o vertical con `x-direction`).
- `widget="Text"` para textos largos (por ejemplo motivos de rechazo).
- `widget="SuggestBox"` / selección asistida en campos relacionales con `domain`.
- `readonly="true"` para mostrar un campo como solo lectura.
- `colSpan`: Para definir el tamaño del campo. Vease más abajo para entenderlo mejor.
- `colOffset`: Para dejar un espacio a la izquierda del campo. Vease más abajo para entenderlo mejor.
- 
### HTML personalizado para mostrar el contenido de un campo
- Para mostrar el contenido de un campo de forma personalizada (por ejemplo, mostrar un PDF incrustado en el formulario), se puede usar la etiqueta `<viewer>` dentro del `<field>`.

```xml
<field name="new" showTitle="false" readonly="true" colSpan="12">
    <viewer depends="documentoOriginal"><![CDATA[
        <>
        <Box as="iframe" height="500" border="0" src={`ws/rest/com.axelor.meta.db.MetaFile/${documentoOriginal.id}/content/download?inline=true&name=${documentoOriginal.fileName}`}></Box>
        </>
    ]]></viewer>
</field>
``` 

### Layout de los campos: colSpan/colOffset
- Para definir el tamaño de un campo se usa "colSpan" (número de columnas que ocupa) y "colOffset" (espacio "hueco" dejado a la izquierda).
- El proyecto sigue una maquetación de 12 columnas, por lo que un campo con `colSpan="6"` ocuparía la mitad del ancho del panel.
- Esto se usa para organizar campos en la misma línea
- Para centrar un campo en una línea se usaría  `colOffset="3"` y `colSpan="6"`.

Te pongo el siguiente ejemplo para que lo veas más claro:

```xml
<panel title="Datos personales">
    <field name="campo1" colSpan="6"/>
    <field name="campo2" colSpan="6"/>
    <field name="campo3" colSpan="6" colOffset="6" />    
</panel>
```

En el ejemplo 'campo1' y 'campo2' se mostrarían en la misma línea ocupando cada uno la mitad del ancho del panel, mientras que 'campo3' se mostraría en una nueva línea y estaría en la segunda mitad del ancho del panel, dejando un espacio vacío a su izquierda gracias al `colOffset="6"`.

Es importante usar `colSpan` y `colOffset` de manera coherente para lograr una maquetación clara y organizada en el formulario. Se debe pensar en el colSpan para que quepa todo el texto.
Si el texto es largo, se puede usar `colSpan="12"` para que ocupe toda la línea y evitar que se corte. Por ejemplo para campos de fechas sobra con colSpan="2".
Tambien hay que ver que pones en la misma linea, normalmente son campos relacionados, por ejemplo fecha de inicio y fecha de fin, o nombre y apellidos.

**Los campos no tienen que rellenar las 12 columnas**
Una fila no necesita sumar exactamente 12 columnas. Cada campo debe tener el `colSpan` que su label y su dato realmente necesitan. No se añaden columnas de más para rellenar el espacio.

```
nnnnnn·······   ← nombre(6): un nombre corto no necesita 12
rrrrrrrrrss·    ← rutaLarga(9) + codigoCorto(2) = 11: la vacía final es aceptable
ppp·········    ← pin(3): un PIN no necesita 12 aunque haya espacio libre
```

**Distribución proporcional al contenido real del campo**
No hay que dividir el espacio equitativamente entre campos de la misma fila: hay que asignar más espacio al campo cuyo valor ocupa más texto visualmente.

Ejemplo incorrecto (reparto igual sin considerar el contenido):
```xml
<field name="centro"         colSpan="4"/>
<field name="numeroRegistro" colSpan="4"/>
<field name="fecha"          colSpan="4"/>
```
"centro" muestra un nombre largo, mientras que "numeroRegistro" y "fecha" suelen ser valores cortos. Con `colSpan="4"` los tres, "centro" se quedará estrecho y los otros dos tendrán espacio de sobra.

Ejemplo correcto (espacio proporcional al contenido esperado):
```xml
<field name="centro"         colSpan="6"/>
<field name="numeroRegistro" colSpan="3"/>
<field name="fecha"          colSpan="3"/>
```

También es importante tener en cuenta que el uso de `colSpan` y `colOffset` para intentar alinear los campos con los de la fila superior e inferior.

En el ejemplo siguiente se hace mal la alineación de campos ya que ninguno de los campos está alineado con el de arriba.:
```xml
<panel title="Datos personales">
    <field name="campo1" colSpan="4"  />
    <field name="campo2" colSpan="2"  />
    <field name="campo3" colSpan="6"  />    
    <field name="campo4" colSpan="2"  />    
    <field name="campo5" colSpan="6"  />    
    <field name="campo6" colSpan="4"  />    
</panel>
```
Fíjate que en el ejemplo anterior, "campo1" no está alineado con ningún campo de la fila inferior, "campo2" no está alineado con ningún campo de la fila inferior, "campo3" no está alineado con ningún campo de la fila inferior, "campo4" no está alineado con ningún campo de la fila superior, "campo5" no está alineado con ningún campo de la fila superior y "campo6" no está alineado con ningún campo de la fila superior. Esto hace que el formulario se vea desorganizado y dificulta la lectura.

Una mejor forma de hacerlo sería:
```xml
<panel title="Datos personales">
    <field name="campo1" colSpan="4"  />
    <field name="campo2" colSpan="3"  />
    <field name="campo3" colSpan="5"  />    
    <field name="campo4" colSpan="4"  />    
    <field name="campo5" colSpan="3"  />    
    <field name="campo6" colSpan="5"  />    
</panel>
```
En este ejemplo, "campo1" está alineado con "campo4", "campo2" está alineado con "campo5" y "campo3" está alineado con "campo6". Esto hace que el formulario se vea más organizado y facilita la lectura.

## Herramienta: análisis ASCII del layout — obligatorio antes de escribir el XML

Antes de escribir (o revisar) el XML de un formulario, **siempre** hay que hacer este análisis en dos pasos:

### Paso 1: dibujar el ASCII

Representa cada campo con una letra repetida `colSpan` veces y los `colOffset` con espacios. Usa `·` para columnas vacías al final de una fila. Los paneles condicionales se dibujan en bloques separados.

```
aabb········   ← codigo(2) + selector(2) = 4, 8 vacías
── MODO_X ──────────────────────
ccdddddddddd   ← campoCorto(2) + campoLargo(10) = 12
── MODO_Y ──────────────────────
cceeeeeeeeee   ← campoCorto(2) + otroCampoLargo(10) = 12
```

### Paso 2: analizar si tiene sentido

Con el ASCII delante, razonar explícitamente sobre cada decisión:

- **¿El tamaño de cada campo refleja su dato y su título?**  
  Un código corto (2-3 chars) no necesita más de 2-3 cols. Un selector enum no necesita tantas cols como su opción más larga — el dropdown ya gestiona el ancho internamente. Una ruta de fichero o un texto descriptivo largo sí justifican 8-10 cols.

- **¿La alineación de inicio de columnas es coherente entre filas?**  
  La alineación se mide por dónde **empiezan** los campos, no por dónde acaban. En el ejemplo anterior, la segunda columna siempre empieza en la posición 3 (tras 2 cols del campo izquierdo). El campo derecho de la fila 1 tiene colSpan=2 y los de las filas condicionales tienen colSpan=10 — eso es correcto: cada uno tiene el ancho que su contenido necesita. La segunda columna no tiene que terminar en el mismo sitio en todas las filas, solo tiene que empezar en el mismo sitio.

- **¿Hay filas con espacio vacío excesivo o campos ridículamente estrechos?**  
  Si un campo queda solo con muchas `·` a la derecha y hay otro campo relacionado que podría acompañarlo → reagrupar en la misma fila.  
  Si un campo ocupa 10 cols pero su valor típico es de 5 chars → reducir.

- **¿El resultado visual es agradable?** Imaginarlo renderizado en el navegador. Si algo "no encaja" en el ASCII, tampoco va a encajar en la UI.

### Paso 3: si el análisis no convence → ajustar y redibujar

No pasar al XML hasta que el ASCII y el razonamiento tengan sentido. Es mucho más rápido iterar en texto que en XML.

## Referencias
Para una referencia completa de todo lo relacionado con formularios , puedes consultar los siguientes documentos:
- `references/form.md`
- `references/widgets.md`
- `references/extensions.md`
