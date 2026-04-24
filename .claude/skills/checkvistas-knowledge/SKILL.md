---
name: checkvistas-knowledge
description: Para comprobar y corregir que el nombre de las vistas es correcto y que las referencias a acciones desde botones , menús y otras acciones existen realmente. Además de que referencias a grid y form son correctas.
---

Eres un experto en revisar ficheros XML de vistas de Axelor para el proyecto EducaFlow Secretaría Virtual. Tu misión es encontrar errores de nomenclatura y referencias rotas. Por último debes corregir lo que sea incorrecto y generar un informe detallado de los errores corregidos.

## Qué debes comprobar

### 1. Convenciones de nombres de vistas

Cada elemento debe seguir su patrón. Los prefijos son:
- Subsistemas: `subsys{Subsistema}` en PascalCase, p.ej. `subsysFirma`, `subsysSistemaEducativo`
- Sistemas: `sys{Sistema}` en PascalCase, p.ej. `sysImportar`
- Expedientes: prefijo `exp-` reservado exclusivamente para el framework de expedientes
- Excepción: formularios propios de Axelor como `user-preferences-form` pueden mantener su nombre original

**Grid** → `{Prefijo}.{Entidad}[.{EntidadHija}]*@{Vista}-grid`
- Ejemplos válidos: `subsysSistemaEducativo.Ciclo@Main-grid`, `subsysSistemaEducativo.Ciclo@Search-grid`, `subsysSistemaEducativo.Ciclo.Curso@Main-grid`

**Form** → `{Prefijo}.{Entidad}[.{EntidadHija}]*@{Vista}-form`
- Ejemplos válidos: `subsysSistemaEducativo.Ciclo@Main-form`, `subsysSistemaEducativo.Ciclo@View-form`, `subsysSistemaEducativo.Ciclo.Curso@Main-form`

**action-view** → `{Prefijo}.{Entidad}[.{EntidadHija}]*@{Vista}-action`
- Ejemplo válido: `subsysSistemaEducativo.LeyEducativa@Main-action`, `subsysFirma.TareaFirma@Pendiente-action`

**action-group** → `{Prefijo}.{Entidad}[.{EntidadHija}]*@{Vista}-{nombreBoton}-action`
- Ejemplo válido: `subsysSistemaEducativo.LeyEducativa@Main-btnGuardar-action`

**action-validate / action-condition** → `{Prefijo}.{Entidad}[.{EntidadHija}]*@{Vista}-Local-{operación}-action`
- Ejemplo válido: `subsysSistemaEducativo.LeyEducativa@Main-Local-validateSave-action`

**action-method / action-script** → `{Prefijo}.{Entidad}[.{EntidadHija}]*@{Vista}-Remote-{operación}-action`
- Ejemplo válido: `subsysSistemaEducativo.LeyEducativa@Main-Remote-validateSave-action`

**action-record** → `{Prefijo}.{Entidad}[.{EntidadHija}]*@{Vista}-set-{campo}-{valor}-action` o `{Prefijo}.{Entidad}[.{EntidadHija}]-onNew-action`
- Ejemplo válido: `subsysFirma.TareaFirma@Pendiente-set-pasoActual-paso1Inicio-action`

**action-attrs** → `{Prefijo}.{Entidad}[.{EntidadHija}]*@{Vista}-set-{campo}.{atributo}-{valor}-action`
- Ejemplo válido: `subsysSistemaEducativo.LeyEducativa@Main-set-orderDate.readOnly-confirmed-action`

**menuitem raíz** → `{Prefijo}-menuitem`
- Ejemplo válido: `subsysFirma-menuitem`, `subsysSistemaEducativo-menuitem`

**menuitem hijo** → `{Prefijo}.{Entidad}[.{EntidadHija}]*@{Vista}-menuitem`
- Ejemplo válido: `subsysFirma.TareaFirma@Pendiente-menuitem`, `subsysSistemaEducativo.Ciclo@Main-menuitem`

### 2. Referencias a acciones que deben existir

Debes comprobar que toda referencia a una acción apunta a una acción que realmente está definida en algún fichero XML del proyecto:

- **Botones** `<button onClick="nombreAccion"/>`: cada nombre de acción referenciado en `onClick` debe existir como `name` de alguna `<action-view>`, `<action-method>`, `<action-group>`, `<action-validate>`, `<action-condition>`, `<action-record>`, `<action-attrs>` o `<action-script>`.
    - Nota: Los valores especiales `save`, `validate`, `close`, `back`, `force-back`, `delete`, `delete-modal`, `save-modal`, `new` son acciones del framework de Axelor y son siempre válidas.
    - Nota: También son válidas si las acciones se separan por comas (p.ej. `save,force-back`) — cada parte se evalúa por separado.
    - También se permite "serial:" an principio de las acciones. Ej: `<button onClick="serial:nombreAccion1,nombreAccion2"/>`

- **Menuitems** `<menuitem action="nombreAccion"/>`: el valor del atributo `action` debe existir como `name` de alguna `<action-view>`.

- **Campos** con `onChange`, `onLoad`, `onSave`, `onNew`, `onSelect`: cada acción referenciada debe existir. Si hay varias acciones separadas por comas, cada una se comprueba por separado.

- **action-group** `<action name="..."/>`: cada acción referenciada debe existir.

## Referencias a grid

- En un `<action-view>` el tag hijo `<view type="grid" name="nombreGrid"/>` el nombreGrid debe hacer referencia a un grid
- En un `<panel-related>` el atributo `grid-view` debe hacer referencia a un grid
- En un `<field>` el atributo `grid-view` debe hacer referencia a un grid


## Referencia a form

- En un `<action-view>` el tag hijo `<view type="form" name="nombreForm"/>` el nombreForm debe hacer referencia a un form
- En un `<panel-related>` el atributo `form-view` debe hacer referencia a un form
- En un `<field>` el atributo `form-view` debe hacer referencia a un form



## Proceso de revisión

1. **Busca todos los ficheros XML de vistas** en `src/main/java/` con patrón `**/views/*.xml` y también los ficheros de menú en `**/menus/*.xml`.

2. **Para cada fichero**, extrae todos los elementos con atributo `name` y clasifícalos por tipo de tag (`<grid>`, `<form>`, `<action-view>`, `<action-method>`, `<action-group>`, `<action-validate>`, `<action-condition>`, `<action-record>`, `<action-attrs>`, `<action-script>`, `<menuitem>`).

3. **Comprueba que el nombre de cada elemento** sigue la convención correspondiente según su tipo de tag.

4. **Recoge todas las acciones definidas** (todos los `name` de todos los action-*).

5. **Busca todas las referencias a acciones** (atributos `onClick`, `action` en menuitem, `onChange`, `onLoad`, `onSave`, `onNew`, `onSelect`, y `<action name="..."/>` dentro de action-group). Para cada referencia, comprueba si existe como acción definida.

6. **Recoge todos los grids y forms definidos** y verifica que los referenciados en `<view type="grid|form" name="..."/>`, `<panel-related form-view="..." grid-view="..." >` y `<field form-view="..." grid-view="..." >` existan.

## Formato del informe

Presenta los resultados agrupados por fichero y tipo de error:

```
📋 INFORME DE REVISIÓN DE VISTAS
================================

✅ Ficheros revisados: N
❌ Errores encontrados: N
⚠️  Advertencias: N

--- ERRORES DE NOMENCLATURA ---

[fichero.xml]
  ❌ <grid name="nombreIncorrecto"> — no sigue el patrón {Prefijo}.{Entidad}@{Vista}-grid
  ❌ <action-method name="otroCaso"> — falta prefijo Remote-

--- REFERENCIAS ROTAS ---

[fichero.xml]
  ❌ <button name="btnFirmar" onClick="accionQueNoExiste"> — la acción 'accionQueNoExiste' no está definida en ningún fichero XML
  ❌ <menuitem name="..." action="otraAccionQueNoExiste"> — la action-view 'otraAccionQueNoExiste' no existe
  ❌ <action-view> referencia <view type="grid" name="gridQueNoExiste"> — el grid 'gridQueNoExiste' no está definido

--- RESUMEN ---
[lista de todos los errores con fichero y línea si es posible]
```

Si no hay errores, muestra un mensaje de éxito claro.

## Revisa el informe

- Ahora lee el informe generado y asegurate que cada "ERRORES DE NOMENCLATURA" realmente es así. Por ejemplo, si el error dice que falta el prefijo Remote-, comprueba que efectivamente el nombre no tiene ese prefijo y que debería tenerlo según la convención. Si está bien, corrige el informe
- Para cada "REFERENCIAS ROTAS", verifica que la acción referenciada no existe en ningún fichero XML. Si encuentras que sí existe, corrige el informe

## Corregir los errores

- Para cada error de nomenclatura, corrige el nombre en el XML para que siga la convención. Asegúrate de actualizar también todas las referencias a ese elemento si es necesario.
- Para cada referencia rota, corrige el XML para que apunte a una acción existente. Si la acción no existe, debes mostrar por pantalla en ROJO y negrita la acción que falta para que el equipo de desarrollo la añada.
- Para cada referencia a grid o form que no exista, corrige el XML para que apunte a un grid o form existente. Si el grid o form no existe, muestra por pantalla en ROJO y negrita el grid o form que falta para que el equipo de desarrollo lo añada.
- Después de corregir, genera un nuevo informe para verificar que ya no hay errores. Si aún quedan errores, repite el proceso hasta que el informe muestre que no hay errores.

