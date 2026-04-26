---
name: sistemas-knowledge
description: Este Skill permite diseñar y generar la estructura de carpetas, ficheros y código Java y XML de un sistema o subsistema en el proyecto Axelor
---

# Sistemas y Subsistemas

Este Skill permite diseñar y generar la estructura de carpetas, ficheros y código Java y XML de un sistema o subsistema en el proyecto Axelor, siguiendo las reglas de dependencia, estructura interna y patrones de vistas definidos en este documento.

## Reglas de dependencia

```
base/util  ←  base/infrastructure  ←  subsystem  ←  system
```

- Un **subsistema** puede depender de `base/` y de otros subsistemas (sin ciclos). Nunca de un sistema.
- Un **sistema** puede depender de `base/` y de subsistemas. **Nunca de otro sistema.**

## Estructura interna

Tanto sistemas como subsistemas comparten la misma estructura de carpetas:

```
<nombre>/
├── domains/          ← entidades JPA (ficheros XML de Axelor)
├── service/          ← interfaz del servicio + DTOs de entrada
│   └── impl/         ← implementación del servicio
├── db/               ← repositorios JPA y listeners
│   └── repo/
├── module/           ← módulo Guice (bindings interfaz → impl)
├── controller/       ← controladores Axelor (@CallMethod), si los hay
└── views/            ← vistas XML de Axelor (grids, formularios, menús)
```

También puede haber carpetas adicionales según la naturaleza del subsistema, como `documentospdf/` para plantillas PDF propias del subsistema.

## Ejemplo: `subsystem/firmas`

```
firmas/
├── domains/
│   ├── TareaFirma.xml
│   └── DocumentoFirma.xml
├── service/
│   ├── FirmaService.java          ← interfaz
│   ├── DatosFirma.java            ← record DTO de entrada
│   ├── FirmaNotifier.java         ← interfaz de callback
│   └── impl/
│       └── FirmaServiceImpl.java
├── db/                            ← (vacío, repos generados por Axelor)
├── module/
│   └── FirmaModule.java
├── controller/
│   └── FirmarController.java
└── views/
    ├── firma-pendiente.xml
    ├── firma-firmado.xml
    ├── firma-rechazado.xml
    └── firma-todos.xml
```

## Ejemplo: `subsystem/registroentradasalida`

```
registroentradasalida/
├── domains/
│   ├── RegistroEntrada.xml
│   └── RegistroSalida.xml
├── service/
│   ├── RegistroEntradaService.java
│   ├── RegistroSalidaService.java
│   ├── DatosRegistroEntrada.java  ← record DTO
│   ├── DatosRegistroSalida.java   ← record DTO
│   ├── PersonaRegistro.java       ← record DTO
│   └── impl/
│       ├── RegistroEntradaServiceImpl.java
│       └── RegistroSalidaServiceImpl.java
├── db/
│   └── repo/
│       ├── RegistroEntradaRepository.java
│       ├── RegistroEntradaListener.java
│       ├── RegistroSalidaRepository.java
│       └── RegistroSalidaListener.java
├── documentospdf/                 ← plantillas PDF propias del subsistema
│   └── registro_entrada_plantilla.pdf
├── module/
│   └── RegistroEntradaSalidaModule.java
└── views/
    ├── registro_entrada.xml
    └── registro_salida.xml
```



## Ficheros de vistas XML

> Para crear o modificar el contenido de los ficheros XML de vistas usa los skills `/vistas-steps`, `/formularios-steps`, `/grids-steps` y `/actions-steps`.

Los ficheros de vistas se ubican en la carpeta `views/` del sistema o subsistema. Los ficheros de vistas se nombran siguiendo la convención:
- `subsystem/<nombre>/views/<NombreEntidad>.xml`
- `system/<nombre>/views/<NombreEntidad>.xml`

Aunque si hay muchas vistas también se pueden organizar en varios ficheros según su funcionalidad dentro de la carpeta views.

## Nombre de las vistas y acciones


El nombre de las vistas de acción es:      `{Prefijo}.{Entidad}@[Main|otro nombre][-{mas cosas}]*-action`


El grid `{Prefijo}.{Entidad}@Search-grid` se usa como selector en campos many-to-one para abrir un grid de búsqueda específico en lugar del grid por defecto que se abre al pulsar la lupa.
El form `{Prefijo}.{Entidad}@View-form` se usa para abrir un form de solo lectura al hacer clic sobre el registro ya seleccionado en lugar del form por defecto que se abre al pulsar la lupa.

El grid `{Prefijo}.{Entidad}[.{EntidadHija}]*@Main-grid` se usa para la pantalla principal de listado de esa entidad
El form `{Prefijo}.{Entidad}[.{EntidadHija}]*@Main-form` se usa para la pantalla principal de edición de esa entidad. 
La acción `{Prefijo}.{Entidad}[.{EntidadHija}]*@Main-action` se usa para abrir la pantalla principal de esa entidad desde el menú o desde otras vistas.

### Prefijos

- Subsistemas: `subsys{Subsistema}` (PascalCase sin separador), p.ej. `subsysFirma`, `subsysRegistroEntradaSalida`
- Sistemas: `sys{Sistema}` (PascalCase sin separador), p.ej. `sysImportar`
- Excepción: el prefijo `exp-` se reserva exclusivamente para las vistas del framework de tipos de expediente

Las entidades se separan con `.` (punto) y los nombres de ese formulario o grid con `@` 


### Actions (`-action`)

Todos los tipos de action terminan en `-action`. Los action son alguno de los siguientes tags `action-view`, `action-record`, `action-method`, `action-group`, `action-validate`, `action-script`) 

#### action-view


| Caso                               | Patrón                                         | Ejemplo                                             |
|------------------------------------|------------------------------------------------|-----------------------------------------------------|
| Mantenimiento o pantalla principal | `subsys{Subsistema}.{Entidad}@Main-action`     | `subsysSistemaEducativo.Ciclo@Main-action` |
| Otro mantenimiento o pantalla      | `subsys{Subsistema}.{Entidad}@{Nombre}-action` | `subsysFirma.TareaFirma@Pendiente-action`           |


### Menuitems (`-menuitem`)

Para la convención completa de nombres de menuitems, ver `/menus-knowledge`.



## Workflow para crear un sistema o subsistema

Cuando se crea o modifica un sistema/subsistema, seguir este orden:

1. **Analiza** qué hay que crear: dominio, servicios, controladores, vistas, menús
2. **Lee los ficheros existentes** antes de generar nada — modelo, vistas del sistema o subsistema, menús, etc.
3. **Decide el patrón estructural** de vistas a aplicar (ver sección siguiente)
4. **Genera o modifica el dominio** usando el skill `/modelos-steps`
5. **Genera o modifica los servicios** usando el skill `/servicios-steps`
6. **Genera o modifica los controladores** usando el skill `/controladores-steps`
7. **Genera o modifica las vistas** usando los skills `/vistas-steps`, `/formularios-steps`, `/grids-steps` y `/actions-steps`
8. **Verifica la coherencia** entre todas las referencias (menuitems → actions → vistas → modelo)

## Patrones estructurales de vistas

### Patrón 1 — Maestro-Detalle

Cuando una entidad tiene una relación `one-to-many` que se edita inline, usa `<panel-related>`:

- El form padre incluye `<panel-related field="coleccion" form-view="..." grid-view="..."/>` — **siempre con los dos atributos**.
- El form hijo lleva `onNew="subsys{X}.{Padre}.{Hijo}-onNew-action"` para inicializar la referencia al padre.
- La `action-record` del `onNew` asigna `__parent__` al campo de relación inversa:
  ```xml
  <action-record name="subsysActas.Acta.CalificacionAlumno-onNew-action"
                 model="...CalificacionAlumno">
      <field name="acta" expr="eval: __parent__"/>
  </action-record>
  ```
- Si hay varias colecciones, agrúpalas dentro de `<panel-tabs>`.
- El patrón aplica en cualquier profundidad: cada nivel tiene su `panel-related`, su form hijo con `onNew` y su `action-record` que asigna `__parent__`. El nombre de la vista refleja todos los niveles: `subsysSistemaEducativo.Ciclo.Curso.CursoModulo@Main-grid`.

El grid y el form del `panel-related` se preceden de este bloque de comentarios (relleno con `*`, `-->` alineados):
```xml
<!-- ************************************************************************************ -->
<!-- ********************* Vistas de ModeloMaestro -> ModeloDetalle ********************* -->
<!-- ************************************************************************************ -->
```
Si hay anidamiento, el texto del comentario refleja todos los niveles: `Vistas de ModeloMaestro -> ModeloDetalle1 -> ModeloDetalle2`.

### Comentarios 
Organización del fichero de vistas con comentarios (relleno con `*`, `-->` alineados):
```xml
<!-- **********************************************************  -->
<!-- ****************** Acciones de los botones ***************  -->
<!-- **********************************************************  -->
<!-- action-group y action-method de cada botón -->

<!-- **********************************************************  -->
<!-- ********* Acciones básicas que cambian campos ************  -->
<!-- **********************************************************  -->
<!-- action-record que solo asignan valores -->
```

### Patrón 2 — Fichero de menú

- El fichero de menú (`secretariavirtual/menus/{NNN}_menuitem_{nombre}.xml`) contiene **solo `<menuitem>`**.
- La `<action-view>` que abre la vista vive en el fichero de vistas del subsistema/sistema, no en el de menú.
- El menuitem raíz no tiene `action`, solo `title` y `order`.
- Los menuitems hijo apuntan a la `action-view` definida en el fichero de vistas.

### Patrón 3 — Selector en campos many-to-one

```xml
<!-- Abre un grid de búsqueda específico al seleccionar -->
<field name="familiaProfesional"
       grid-view="subsysSistemaEducativo.FamiliaProfesional@Search-grid" />

<!-- Con filtro adicional sobre los valores elegibles -->
<field name="grado"
       grid-view="subsysSistemaEducativo.Grado@Search-grid"
       domain="(self.code='D' OR self.code='E')" />

<!-- Con vista readonly al abrir el registro seleccionado -->
<field name="ciclo"
       grid-view="subsysSistemaEducativo.Ciclo@Search-grid"
       form-view="subsysSistemaEducativo.Ciclo@View-form" />
```

- `grid-view` → abre el grid `@Search-grid` en lugar del por defecto al pulsar la lupa.
- `domain` → filtra los registros elegibles (SQL WHERE sobre `self`).
- `form-view` → al hacer clic sobre el registro ya seleccionado, abre esa vista. Usa `@View-form` cuando el campo debe ser solo lectura al navegar.

### Patrón 4 — Máquina de estados en un form

- El form tiene un campo `<field name="pasoActual" showIf="false" />` que define el estado actual.
- Se crean paneles con `showIf="pasoActual=='paso1Inicio'"` para mostrar/ocultar según el estado.
- Se crean `action-record` que asignan `pasoActual` al nuevo estado (van en la sección de acciones básicas).
- El form tiene `onLoad="subsys{X}.{Entidad}@{Estado}-set-pasoActual-{pasoInicial}-action"` para inicializar al primer paso al abrir el formulario.


## Cuándo crear un subsistema vs un sistema

Crear un **subsistema** cuando la capacidad es reutilizable (no necesariamente) por múltiples sistemas o por otros subsistemas (ej: firmas, registro de entrada/salida, certificados digitales).

Crear un **sistema** es similar a un subsistema solo no necesita ser reutilizada por nadie (Se podría eliminar sin problemas si se deja de usar) un sistema no depende de otros sistemas pero si de otros subsistemas. Ejemplos: `tiposexpedientes/comision_servicio`, `importar`, `actas`. 


Pero la diferencia principal entre un sistema y un subsistema es que el sistema suele representar una funcionalidad que se ofrece a los usuarios y que ellos han solicitado. Es decir es una diferencia desde el punto de vista del negocio. Por ejemplo, el registro de entrada/salida no es algo que solicitan sino que es una necesidad que usan los expedientes.
Mientras que el subsistema es una parte más genérica y reutilizable que puede ser usada por varios sistemas. Por ejemplo, el subsistema de `firmas` puede ser usado por múltiples sistemas para gestionar la firma de documentos, mientras que el sistema `comision_servicio` es una funcionalidad concreta que utiliza el subsistema de firmas para gestionar los expedientes de comisión de servicio.
