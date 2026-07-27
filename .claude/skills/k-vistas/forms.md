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
- **MUST** — un botón principal queda pegado al borde derecho: su `colOffset + colSpan` **suma exactamente 12**. Cuando el panel tiene un único botón principal (p.ej. **Salir** en un `Ref@…-form` de solo lectura) con `colSpan="2"`, el `colOffset` **MUST** ser `10` (10+2=12), no 8.
- Las acciones secundarias (borrar, imprimir, etc.) están a la izquierda del todo
- Realmente no es necesario que estén exactamente estos botones sino que podría haber otros botones con otras acciones. Pero hay que distingir claramente las acciones principales de las secundarias y para eso se siguen estas pautas de colocación.
- El panel de botones siempre debe incluir Borrar, Cancelar y Guardar salvo que se indique lo contrario o haya algo en el negocio que te haga pensar que no es necesario.

## Botones condicionales por estado (showIf en botones)

Un `<button>` oculto con `showIf` **reserva sus columnas igual que un campo** (§huecos en el grid). Dos gemelos con `colOffset` en un panel plano nunca renderizan bien: el oculto reserva su sitio y el visible salta de fila.

Reglas:

- **MUST**: si el `buttons-panel` tiene botones con `showIf` (salvo la excepción de abajo), agrupa los botones de cada estado en un **panel anidado por estado** dentro del `buttons-panel`, con el `showIf` **en el panel** (un panel oculto sí colapsa del todo) y los botones **sin** `showIf`.
- Los `showIf` de los paneles de estado **MUST** ser expresiones booleanas **mutuamente excluyentes** entre sí (y entre todas cubrir todos los estados del form): si dos pueden ser verdaderas a la vez, se renderizan dos filas de botones simultáneas.
  - ✅ CORRECTO: `(id == null) && (cid == null)` / `(id != null) || (cid != null)` (una es la negación exacta de la otra)
  - ✅ CORRECTO: `((id != null) || (cid != null)) && (estado == 'FAIL')` / `((id != null) || (cid != null)) && (estado != 'FAIL')` (partición del mismo estado por el mismo campo)
  - ❌ INCORRECTO: `id == null` / `estado == 'FAIL'` (un registro nuevo cuyo estado sea FAIL cumple ambas → dos filas de botones a la vez)
- Cada panel de estado cumple por sí solo las reglas de colocación: secundarios a la izquierda, principales pegados al borde derecho (`colOffset + colSpan = 12`), fila que suma 12.
- **Excepción** (no hace falta panel): un botón condicional situado al **principio del panel y sin `colOffset`** (p.ej. el `btnDelete` canónico o un `btnReenviar` en primera posición): al ocultarse su hueco queda pegado al borde izquierdo y no desplaza a los demás.
- `btnDelete*` conserva **siempre** su `showIf="(id!=null) || (cid!=null)"` canónico aunque viva dentro de un panel de estado (lo exige `VAR-5.1` de `view-rules.md`); dentro de su panel es siempre verdadero y no crea hueco.
- Si el mismo botón lógico aparece en varios paneles de estado, cada `name` **MUST** ser único y empezar por el `{btnXxx}` del `onClick` (regla de gemelos): `btnSaveAlta`/`btnSaveEdicion`, `btnDelete`/`btnDeleteFail`.

- ✅ CORRECTO (form modal de detalle con dos estados):
  ```xml
  <panel name="buttons-panel" title="" colSpan="12" showFrame="false">
      <panel name="buttonsAlta" title="" colSpan="12" showFrame="false"
             showIf="(id == null) &amp;&amp; (cid == null)">
          <button name="btnCancelAlta" title="Cancelar" onClick="…-btnCancel-action" colSpan="2" colOffset="8" outline="true"/>
          <button name="btnSaveAlta" title="Guardar" onClick="…-btnSave-action" colSpan="2"/>
      </panel>
      <panel name="buttonsEdicion" title="" colSpan="12" showFrame="false"
             showIf="(id != null) || (cid != null)">
          <button name="btnDelete" title="Borrar" onClick="…-btnDelete-action"
                  css="btn-danger" colSpan="2" outline="true" showIf="(id!=null) || (cid!=null)"/>
          <button name="btnCancelSalir" title="Salir" onClick="…-btnCancel-action" colSpan="2" colOffset="6" outline="true"/>
          <button name="btnSaveEdicion" title="Guardar" onClick="…-btnSave-action" colSpan="2"/>
      </panel>
  </panel>
  ```
  ASCII Layout por estado:
  ```
  Alta:    ........ccgg   ← Cancelar(2, offset8) + Guardar(2)
  Edición: bb......ssgg   ← Borrar(2) + offset(6) + Salir(2) + Guardar(2)
  ```
- ❌ INCORRECTO (gemelos con offset en panel plano):
  ```xml
  <panel name="buttons-panel" title="" colSpan="12" showFrame="false">
      <button name="btnCancelAlta"  title="Cancelar" colSpan="2" colOffset="6" showIf="id == null"/>
      <button name="btnCancelSalir" title="Salir"    colSpan="2" colOffset="6" showIf="id != null"/>
      <button name="btnSave"        title="Guardar"  colSpan="2" showIf="id == null"/>
  </panel>
  ```
  (el gemelo oculto reserva offset+span y empuja a `btnSave` a una segunda fila)

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

Los elementos ocultos con `showIf` en Axelor —**campos Y botones**— siguen ocupando espacio en el grid CSS porque tienen asignación explícita de columnas. Esto genera **huecos visuales** cuando un elemento está oculto y, si lo que viene después ya no cabe en la fila, **desplaza el resto a la fila siguiente**. Para los botones del `buttons-panel`, ver §Botones condicionales por estado.

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

## Maquetación de un formulario: dibuja el ASCII Layout ANTES del XML (OBLIGATORIO)

> **Nomenclatura:** llamamos **ASCII Layout** al dibujo del layout de un panel en ASCII (una letra por campo repetida `colSpan` veces, puntos para las columnas vacías del `colOffset`). Es el nombre único de este artefacto en todo el proyecto — úsalo siempre así, no "boceto ASCII" ni "notación ASCII".

**CRITICAL — MUST** diseñar el layout de cada panel de un `<form>` como **ASCII Layout** **antes** del XML y **MUST NOT** saltar directamente a poner `colSpan`/`colOffset` en los `<field>`. El **ASCII Layout** es el **paso de diseño**; el XML es solo su traducción mecánica. Saltarse este paso es la causa de los layouts lamentables: campos sueltos en filas medio vacías, `colSpan` inflados y bordes desalineados.

Por **cada panel** del formulario, en este orden:

1. **Lista los campos del panel y agrúpalos por semántica** (§Agrupación semántica): los relacionados van en la **misma fila** (fecha inicio + fecha fin, slot + PIN, fichero + contraseña…).
2. **Dimensiona cada campo** con la tabla de proporcionalidad (§Proporcionalidad): el `colSpan` refleja **label + valor típico**, no el espacio libre. Un código o número corto son **2–3** columnas, **no** 6 ni 12.
3. **Dibuja cada fila del ASCII Layout en la rejilla de 12 columnas** (una letra por campo repetida `colSpan` veces; `.` = columna vacía por `colOffset`). Cada fila **MUST** sumar **exactamente 12**.
4. **Alinea los bordes de columna entre filas** (§Alineación vertical), en especial con los paneles condicionales anidados.
5. **Coloca los botones** en su `buttons-panel`: los **secundarios** (Borrar) a la izquierda, los **principales** (Cancelar, Guardar) a la derecha, con el `colOffset` que los empuje (§Botones principales y secundarios, §Representar `colOffset`).
6. **Si el panel tiene elementos con `showIf`** (campos o botones), dibuja **un ASCII Layout por estado** — nunca uno solo mezclando estados. Recuerda que lo oculto **reserva sus columnas**: los grupos condicionales van en paneles anidados con el `showIf` en el panel (§huecos en el grid, §Botones condicionales por estado).
7. **Pasa el checklist de abajo.** Solo cuando el ASCII Layout lo cumple **todo**, tradúcelo a `<field colSpan="…" colOffset="…">`.

**REQUIRED — muestra el ASCII Layout** (en el chat, o en el diseño si estás en el pipeline SDD) para poder revisar el layout de un vistazo **antes** de que exista el XML.

**Ejemplo** (entidad con código, nombre, dos fechas relacionadas y un motivo largo):
```
aaa...bbbbbb   ← code(3) + colOffset(3) + name(6)     [identificación]
ccccccdddddd   ← fechaInicio(6) + fechaFin(6)         [relacionadas → misma fila]
eeeeeeeeeeee   ← motivo(12)                            [texto libre multilinea → fila propia]
```

### Checklist de maquetación (por panel, antes de escribir el XML)

- [ ] ¿Cada fila suma **exactamente 12** columnas (campos + `colOffset`)?
- [ ] ¿Los campos semánticamente relacionados están en la **misma fila** y en su **orden natural**?
- [ ] ¿Ningún `colSpan` está **inflado** respecto a su label + valor típico (tabla de proporcionalidad)?
- [ ] ¿Ningún campo queda **solo en una fila** con mucho hueco a la derecha sin un motivo real (§alerta)?
- [ ] ¿Los **bordes de columna se alinean** entre filas y con los paneles condicionales anidados?
- [ ] ¿Los botones **secundarios a la izquierda** y los **principales a la derecha**, con el `colOffset` correcto?
- [ ] Si hay `showIf`: ¿dibujaste un ASCII Layout **por cada estado**, cada estado cumple todo lo anterior, y los elementos condicionales están en **paneles anidados por estado** (o son la excepción del borde izquierdo)?
- [ ] ¿Dibujaste el ASCII Layout **antes** del XML y los `colSpan`/`colOffset` finales **coinciden** con él?

## Principios de diseño visual de formularios

### Agrupación semántica de campos
Campos relacionados semánticamente deben ir en la misma fila:
- Nombre + Apellidos → misma fila
- Fecha de inicio + Fecha de fin → misma fila
- Slot + PIN (acceso a dispositivo) → misma fila
- Slot + Alias (identificación de certificado en dispositivo) → misma fila
- Fichero (subida) + Contraseña (para abrirlo) → misma fila
- Ruta de un fichero + Contraseña → misma fila o filas contiguas en el mismo panel
- Para + En copia + En copia oculta (destinatarios de un correo) → misma fila, o filas contiguas en el mismo panel si no caben en 12 columnas

**El orden dentro del grupo también es parte de la regla**, no solo la vecindad. Un grupo bien agrupado pero desordenado se lee mal igual. El orden lo fija, por este orden de prioridad:

1. **La convención del dominio**, cuando existe: Nombre antes que Apellidos; Para, luego En copia, luego En copia oculta; Provincia antes que Municipio.
2. **La secuencia temporal o lógica**: Fecha de inicio antes que Fecha de fin; Slot antes que PIN (primero eliges el dispositivo, luego te autenticas).
3. **De lo general a lo particular**: País → Provincia → Municipio → Código postal.

- ✅ CORRECTO: `nombre(4) + apellidos(8)` — convención del dominio.
- ❌ INCORRECTO: `apellidos(8) + nombre(4)` (mismo grupo y misma fila, pero invertido respecto a cómo se nombra a una persona en castellano).
- ❌ INCORRECTO: `fechaFin(6) + fechaInicio(6)` (invierte la secuencia temporal).
- ❌ INCORRECTO: `para(12)` / `enCopiaOculta(6) + enCopia(6)` (rompe el orden convencional de los destinatarios y separa a uno de los tres).

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

### Representar `colOffset` en el ASCII Layout

En el ASCII Layout cada campo es **una letra repetida tantas veces como su `colSpan`** (el cambio de letra marca el borde entre campos), y el punto `.` representa una **columna vacía**. El `colOffset` son columnas vacías **antes** del campo: celdas que **fluyen como cualquier celda**, consumiéndose desde la posición actual. Se dibujan como puntos delante de su letra. Tiene tres usos, y el ASCII Layout deja ver cuál está ocurriendo.

**1. Dejar hueco a la izquierda del campo (misma fila).** Si `colOffset` + `colSpan` **caben** en las columnas que quedan libres en la fila, el campo se queda en ella y el offset aparece como puntos por delante:
```
aaa...bbbbbb   ← code(3) + colOffset(3) + name(6)                              [3+3+6 = 12]
aa......bbcc   ← btnBorrar(2) + colOffset(6) + btnCancelar(2) + btnGuardar(2)  [2+6+2+2 = 12]
```
El segundo es el patrón del panel de botones: el `colOffset` empuja Cancelar+Guardar a la derecha y deja Borrar solo a la izquierda, sin ningún campo intermedio.

**2. Saltar a la fila siguiente.** Las celdas vacías del `colOffset` se consumen desde la posición actual y, si completan la fila, el campo cae al **principio** de la fila siguiente. Con un `colOffset` igual a las columnas libres, el campo abre la fila nueva en la columna 1:
```
aaaaaa······   ← centro(6); el colOffset(6) de grado consume las 6 columnas libres y completa la fila
bbbbcccc····   ← grado(4) arranca la fila siguiente en la columna 1; nivel(4) le sigue
```
Este es el `colOffset` de la fila `grado` en la plantilla del Ciclo (arriba). El offset **NO se re-aplica** en la fila nueva: **MUST NOT** usarlo esperando alinear el campo a la derecha de la fila siguiente — para dejar hueco a la izquierda de un campo, el offset se pone en la misma fila donde se quiere el hueco (uso 1).

**3. Dejar un campo solo en su fila (el offset del campo SIGUIENTE completa la fila).** Para que un campo quede solo en su fila **sin inflar su `colSpan`**, el campo **siguiente** lleva un `colOffset` igual a las columnas libres que quedan en la fila: el offset consume esas columnas, la fila queda completa y el campo empieza la fila siguiente desde la izquierda.
```
ccc·········   ← centro(3) queda solo: las 9 columnas libres las consume el colOffset(9) del siguiente
dddnnnnaaaaa   ← dniDestinatario(3, colOffset=9) + nombre(4) + apellidos(5) → arranca la fila nueva en la columna 1
```
- ✅ CORRECTO: `<field name="centro" colSpan="3"/>` seguido de `<field name="dniDestinatario" colSpan="3" colOffset="9"/>` (3 usadas + 9 de offset = fila completa; dni abre la fila siguiente)
- ❌ INCORRECTO: `<field name="centro" colSpan="12"/>` para dejarlo solo (infla el campo a 12 columnas cuando su contenido pide 3; el hueco se logra con el offset del siguiente, no engordando el campo)

### Proporcionalidad al contenido: label + tipo de dato

El `colSpan` debe reflejar el espacio que ocupan **tanto el título del campo como el valor** que el usuario va a introducir o ver. Un label corto + un valor corto = pocas columnas, aunque el formulario tenga espacio libre.

| Tipo de campo                    | Ejemplos de título                | Ejemplos de valor        | colSpan orientativo |
|----------------------------------|-----------------------------------|--------------------------|---------------------|
| Número/código muy corto          | "Slot", "Nº"                      | 0, 1, 2                  | **2**               |
| PIN / código corto               | "PIN", "CVV"                      | 1234, AB12               | **3**               |
| DNI / código identificador       | "DNI", "Código"                   | 12345678Z                | **3**               |
| Fecha                            | "Fecha inicio"                    | 01/01/2025               | **3**               |
| Nombre corto / identificador     | "Nombre", "Alias"                 | "DNIe", "HSM prod"       | **6–8**             |
| Nombre o descripción media       | "Descripción", "Asunto"           | texto moderado           | **8–10**            |
| Ruta de fichero / path           | "Ruta librería", "Ruta classpath" | /usr/lib/.../opensc.so   | **9–10**            |
| Selector enum largo              | "Tipo", "Estado"                  | "Opción con texto largo" | **4–6**             |
| Widget compacto (binary-link)    | "Fichero"                         | [botón subir]            | **3–4**             |
| Campo de texto libre / multiline | "Motivo", "Observaciones"         | texto largo              | **12**              |

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

## Herramienta: análisis del ASCII Layout — obligatorio antes de escribir el XML

Antes de escribir (o revisar) el XML de un formulario, **siempre** hay que hacer este análisis en dos pasos:

### Paso 1: dibujar el ASCII Layout

Representa cada campo con una letra repetida `colSpan` veces y los `colOffset` con espacios. Usa `·` para columnas vacías al final de una fila. Los paneles condicionales se dibujan en bloques separados.

```
aabb········   ← codigo(2) + selector(2) = 4, 8 vacías
── MODO_X ──────────────────────
ccdddddddddd   ← campoCorto(2) + campoLargo(10) = 12
── MODO_Y ──────────────────────
cceeeeeeeeee   ← campoCorto(2) + otroCampoLargo(10) = 12
```

### Paso 2: analizar si tiene sentido

Con el ASCII Layout delante, razonar explícitamente sobre cada decisión:

- **¿El tamaño de cada campo refleja su dato y su título?**  
  Un código corto (2-3 chars) no necesita más de 2-3 cols. Un selector enum no necesita tantas cols como su opción más larga — el dropdown ya gestiona el ancho internamente. Una ruta de fichero o un texto descriptivo largo sí justifican 8-10 cols.

- **¿La alineación de inicio de columnas es coherente entre filas?**  
  La alineación se mide por dónde **empiezan** los campos, no por dónde acaban. En el ejemplo anterior, la segunda columna siempre empieza en la posición 3 (tras 2 cols del campo izquierdo). El campo derecho de la fila 1 tiene colSpan=2 y los de las filas condicionales tienen colSpan=10 — eso es correcto: cada uno tiene el ancho que su contenido necesita. La segunda columna no tiene que terminar en el mismo sitio en todas las filas, solo tiene que empezar en el mismo sitio.

- **¿Hay filas con espacio vacío excesivo o campos ridículamente estrechos?**  
  Si un campo queda solo con muchas `·` a la derecha y hay otro campo relacionado que podría acompañarlo → reagrupar en la misma fila.  
  Si un campo ocupa 10 cols pero su valor típico es de 5 chars → reducir.

- **¿El resultado visual es agradable?** Imaginarlo renderizado en el navegador. Si algo "no encaja" en el ASCII Layout, tampoco va a encajar en la UI.

### Paso 3: si el análisis no convence → ajustar y redibujar

No pasar al XML hasta que el ASCII Layout y el razonamiento tengan sentido. Es mucho más rápido iterar en texto que en XML.

### Dirección de auditoría: reconstruir el ASCII Layout de un XML existente

La misma herramienta sirve para **auditar** un `<form>` ya escrito (revisión de código, verificación de un diseño del pipeline SDD, auditoría de layout de las vistas existentes). El procedimiento es el inverso al de maquetación:

1. Por cada `<panel>`, `<panel-related>` y `buttons-panel` del form, **reconstruye el ASCII Layout** a partir de los `colSpan`/`colOffset` reales del XML, con la notación de este fichero (una letra por campo repetida `colSpan` veces, `.` por columna vacía de `colOffset`, recordando que el flujo de celdas puede hacer saltar un campo a la fila siguiente — §Representar `colOffset`). Si hay elementos con `showIf`, dibuja **un ASCII Layout por estado**, con los paneles condicionales en bloques separados.
2. **Pasa sobre el dibujo reconstruido el «Checklist de maquetación»** (§Checklist), igual que si el form fuera nuevo.
3. Reporta cada incumplimiento **citando la regla del checklist** e incluyendo el **ASCII Layout reconstruido** como evidencia — el dibujo es lo que permite ver el problema sin renderizar la vista.

**MUST NOT** auditar un layout "a ojo" leyendo los `colSpan` sueltos: sin reconstruir el dibujo no se ven los huecos, los bordes desalineados ni las filas que no suman 12.

## Referencias
Para una referencia completa de todo lo relacionado con formularios , puedes consultar los siguientes documentos:
- `references/form.md`
- `references/widgets.md`
- `references/extensions.md`
