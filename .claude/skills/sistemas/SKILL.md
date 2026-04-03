---
name: sistemas y subsistemas
description: Explica la diferencia entre sistemas (system/) y subsistemas (subsystem/), sus reglas de dependencia y la estructura interna de cada uno, con ejemplos reales del proyecto.
---

# Sistemas y Subsistemas

## Concepto

El código de negocio se organiza en dos capas:

- **`subsystem/`** — capacidades reutilizables que pueden ser usadas por cualquier sistema o por otros subsistemas. Ejemplos: `firmas`, `registroentradasalida`, `expedientes`, `certificados`.
- **`system/`** — implementaciones concretas que usan subsistemas. Ejemplos: `tiposexpedientes/comision_servicio`, `importar`, `actas`.

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
├── controllers/      ← controladores Axelor (@CallMethod), si los hay
└── views/            ← vistas XML de Axelor (grids, formularios, menús)
```

También puede haber carpetas adicionales según la naturaleza del subsistema, como `documentospdf/` en `registroentradasalida`.

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
├── controllers/
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
│   ├── PersonaRegistro.java       ← record auxiliar
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

## Módulo Guice

Cada sistema o subsistema tiene exactamente un módulo Guice en `module/` que registra los bindings interfaz → implementación:

```java
// subsystem/firmas/module/FirmaModule.java
public class FirmaModule extends AxelorModule {
    @Override
    protected void configure() {
        bind(FirmaService.class).to(FirmaServiceImpl.class);
    }
}

// subsystem/registroentradasalida/module/RegistroEntradaSalidaModule.java
public class RegistroEntradaSalidaModule extends AxelorModule {
    @Override
    protected void configure() {
        bind(RegistroEntradaService.class).to(RegistroEntradaServiceImpl.class);
        bind(RegistroSalidaService.class).to(RegistroSalidaServiceImpl.class);
    }
}
```

## Paquetes Java

| Capa | Paquete base Java | Paquete de modelos (`db`) |
|---|---|---|
| Subsistema | `com.educaflow.subsystem.<nombre>` | `com.educaflow.subsystem.<nombre>.db` |
| Sistema | `com.educaflow.system.<nombre>` | `com.educaflow.system.<nombre>.db` |

El paquete de modelos se declara en la cabecera de cada fichero `domains/` mediante el tag `<module>`:

```xml
<!-- subsystem/firmas/domains/TareaFirma.xml -->
<module name="firmas" package="com.educaflow.subsystem.firmas.db"/>

<!-- subsystem/registroentradasalida/domains/RegistroEntrada.xml -->
<module name="registroentradasalida" package="com.educaflow.subsystem.registroentradasalida.db"/>
```

El atributo `name` del `<module>` coincide con el nombre del sistema o subsistema. El `package` siempre termina en `.db` y es donde Axelor genera las clases Java de las entidades.

## Convención de nombres de vistas

> Para crear o modificar el contenido de los ficheros XML de vistas usa los skills `/vistas`, `/formularios`, `/grids` y `/actions`.

Los ficheros de vistas se ubican en la carpeta `views/` del sistema o subsistema.

Normalmente los ficheros de vistas se nombran siguiendo la convención:
- `subsystem/<nombre>/views/<nombre>.xml`
- `system/<nombre>/views/<nombre>.xml`

Aunque si hay muchas vistas también se pueden organizar en varios ficheros según su funcionalidad o entidad dentro de la carpeta views.

### Prefijos

- Subsistemas: `subsys{Subsistema}` (PascalCase sin separador), p.ej. `subsysFirma`, `subsysRegistroEntradaSalida`
- Sistemas: `sys{Sistema}` (PascalCase sin separador), p.ej. `sysImportar`
- Excepción: el prefijo `exp-` se reserva exclusivamente para las vistas del framework de tipos de expediente

Las entidades se separan con `.` (punto) y los calificadores/estado/tipo con `-` (guión).

### Forms (`-form`)

| Caso | Patrón | Ejemplo |
|------|--------|---------|
| Pantalla principal editable | `subsys{Subsistema}.{Entidad}-form` | `subsysSistemaEducativo.Ciclo-form` |
| Solo lectura | `subsys{Subsistema}.{Entidad}-view-form` | `subsysSistemaEducativo.Ciclo-view-form` |
| Con estado | `subsys{Subsistema}.{Entidad}-{estado}-form` | `subsysFirma.TareaFirma-pendiente-form` |
| Entidad anidada | `subsys{Subsistema}.{EntidadPadre}.{EntidadHija}-form` | `subsysSistemaEducativo.Ciclo.Curso-form` |
| Entidad anidada con estado | `subsys{Subsistema}.{EntidadPadre}.{EntidadHija}-{estado}-form` | `subsysFirma.TareaFirma.DocumentoFirma-pendiente-form` |

### Grids (`-grid`)

| Caso | Patrón | Ejemplo |
|------|--------|---------|
| Grid principal | `subsys{Subsistema}.{Entidad}-grid` | `subsysSistemaEducativo.Ciclo-grid` |
| Selector embebido | `subsys{Subsistema}.{Entidad}-search-grid` | `subsysSistemaEducativo.Ciclo-search-grid` |
| Con estado | `subsys{Subsistema}.{Entidad}-{estado}-grid` | `subsysFirma.TareaFirma-pendiente-grid` |
| Entidad anidada | `subsys{Subsistema}.{EntidadPadre}.{EntidadHija}-grid` | `subsysSistemaEducativo.Ciclo.Curso-grid` |
| Entidad anidada con estado | `subsys{Subsistema}.{EntidadPadre}.{EntidadHija}-{estado}-grid` | `subsysFirma.TareaFirma.DocumentoFirma-pendiente-grid` |

### Actions (`-action`)

Todos los tipos de action terminan en `-action`. El tipo XML (`action-view`, `action-record`, `action-method`, `action-group`) se diferencia por el contenido, no por el nombre.

**Abrir vista** (`action-view`):

| Caso | Patrón | Ejemplo |
|------|--------|---------|
| CRUD completo (grid+form) | `subsys{Subsistema}.{Entidad}-mantenimiento-action` | `subsysSistemaEducativo.Ciclo-mantenimiento-action` |
| Con estado/filtro | `subsys{Subsistema}.{Entidad}-{estado}-action` | `subsysFirma.TareaFirma-pendiente-action` |

**Botones** (handler directo de `onClick`, llevan el segmento `button`):

| Patrón | Ejemplo |
|--------|---------|
| `subsys{Subsistema}.{Entidad}-{estado}-form-button-{nombreBoton}-action` | `subsysFirma.TareaFirma-pendiente-form-button-paso1InicioFirmar-action` |

**Operaciones internas** (llamadas desde `onLoad`, `onSave`, `serial:`, etc.; sin segmento `button`):

| Tipo | Patrón | Ejemplo |
|------|--------|---------|
| Operación de negocio | `subsys{Subsistema}.{Entidad}-{estado}-form-{operacion}-action` | `subsysFirma.TareaFirma-pendiente-form-firmar-action` |
| Asignar valor a campo | `subsys{Subsistema}.{Entidad}-{estado}-form-set-{campo}-{valor}-action` | `subsysFirma.TareaFirma-pendiente-form-set-pasoActual-paso1Inicio-action` |
| Evento de campo (`onNew`, `onChange`) | `subsys{Subsistema}.{EntidadPadre}.{EntidadHija}-{evento}-action` | `subsysSistemaEducativo.Ciclo.Curso-onNew-action` |

### Menuitems (`-menuitem`)

El prefijo es la **sección de navegación** (no el subsistema de la vista que abre).

| Nivel | Patrón | Ejemplo |
|-------|--------|---------|
| Sección raíz | `subsys{Seccion}-menuitem` | `subsysFirma-menuitem` |
| Entrada directa (concepto en minúsculas) | `subsys{Seccion}-{concepto}-menuitem` | `subsysFirma-pendiente-menuitem` |
| Entrada directa (entidad PascalCase) | `subsys{Seccion}.{Entidad}-menuitem` | `subsysSistemaEducativo.Ciclo-menuitem` |
| Subsección | `subsys{Seccion}-{calificador}-menuitem` | `subsysSistemaEducativo-raw-menuitem` |
| Entrada en subsección | `subsys{Seccion}-{calificador}-{Entidad}-menuitem` | `subsysSistemaEducativo-raw-Ciclo-menuitem` |

Los ficheros de menú globales van en `secretariavirtual/menus/` con prefijo numérico: `{NNN}_{descripcion}.xml`.

### Calificadores habituales

`pendiente`, `firmado`, `rechazado`, `todos`, `view` (solo lectura), `search` (selector).

No usar `main` — si solo hay una pantalla principal no necesita calificador.

## Workflow para crear un sistema o subsistema

Cuando se crea o modifica un sistema/subsistema, seguir este orden:

1. **Analiza** qué hay que crear: dominio, servicios, controladores, vistas, menús
2. **Lee los ficheros existentes** antes de generar nada — modelo, vistas del sistema o subsistema, menús, etc.
3. **Decide el patrón estructural** de vistas a aplicar (ver sección siguiente)
4. **Genera o modifica el dominio** usando el skill `/modelos`
5. **Genera o modifica los servicios** usando el skill `/servicios`
6. **Genera o modifica los controladores** usando el skill `/controladores`
7. **Genera o modifica las vistas** usando los skills `/vistas`, `/formularios`, `/grids` y `/actions`
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
- El patrón aplica en cualquier profundidad: cada nivel tiene su `panel-related`, su form hijo con `onNew` y su `action-record` que asigna `__parent__`. El nombre de la vista refleja todos los niveles: `subsysSistemaEducativo.Ciclo.Curso.CursoModulo-grid`.

El grid y el form del `panel-related` se preceden de este bloque de comentarios (relleno con `*`, `-->` alineados):
```xml
<!-- ************************************************************************************ -->
<!-- ********************* Vistas de ModeloMaestro -> ModeloDetalle ********************* -->
<!-- ************************************************************************************ -->
```
Si hay anidamiento, el texto del comentario refleja todos los niveles: `Vistas de ModeloMaestro -> ModeloDetalle1 -> ModeloDetalle2`.

### Patrón 2 — Botones

- Si el botón solo llama a un **método Java** del controller → `action-method` directamente en `onClick`.
- Si necesita varias acciones en secuencia → `action-group` como intermediario.
- Para firma con AutoFirma: `serial:accionAutoFirma,accionGuardar` en `onClick`.

| Tipo | Cuándo usarlo | Ejemplo |
|------|--------------|---------|
| `action-group` | Handler directo de botón; orquesta otras acciones | `subsysFirma.TareaFirma-pendiente-form-button-paso1InicioFirmar-action` |
| `action-method` | Llama a un método Java/Kotlin del controller | `subsysFirma.TareaFirma-pendiente-form-button-paso2RechazadoGuardar-action` |
| `action-record` | Asigna valores a campos (sin código Java) | `subsysFirma.TareaFirma-pendiente-form-set-pasoActual-paso1Inicio-action` |

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

### Patrón 3 — Fichero de menú

- El fichero de menú (`secretariavirtual/menus/{NNN}_menuitem_{nombre}.xml`) contiene **solo `<menuitem>`**.
- La `<action-view>` que abre la vista vive en el fichero de vistas del subsistema/sistema, no en el de menú.
- El menuitem raíz no tiene `action`, solo `title` y `order`.
- Los menuitems hijo apuntan a la `action-view` definida en el fichero de vistas.

### Patrón 4 — Selector en campos many-to-one

```xml
<!-- Abre un grid de búsqueda específico al seleccionar -->
<field name="familiaProfesional"
       grid-view="subsysSistemaEducativo.FamiliaProfesional-search-grid" />

<!-- Con filtro adicional sobre los valores elegibles -->
<field name="grado"
       grid-view="subsysSistemaEducativo.Grado-search-grid"
       domain="(self.code='D' OR self.code='E')" />

<!-- Con vista readonly al abrir el registro seleccionado -->
<field name="ciclo"
       grid-view="subsysSistemaEducativo.Ciclo-search-grid"
       form-view="subsysSistemaEducativo.Ciclo-view-form" />
```

- `grid-view` → abre el grid `-search-grid` en lugar del por defecto al pulsar la lupa.
- `domain` → filtra los registros elegibles (SQL WHERE sobre `self`).
- `form-view` → al hacer clic sobre el registro ya seleccionado, abre esa vista. Usa `-view-form` cuando el campo debe ser solo lectura al navegar.

### Patrón 5 — Máquina de estados en un form

- El form tiene un campo `<field name="pasoActual" showIf="false" />` que define el estado actual.
- Se crean paneles con `showIf="pasoActual=='paso1Inicio'"` para mostrar/ocultar según el estado.
- Se crean `action-record` que asignan `pasoActual` al nuevo estado (van en la sección de acciones básicas).
- El form tiene `onLoad="subsys{X}.{Entidad}-{estado}-form-set-pasoActual-{pasoInicial}-action"` para inicializar al primer paso al abrir el formulario.

## Quality checks de artefactos Axelor

Antes de dar por terminado cualquier entrega:
- [ ] Todos los `<button onClick="...">` referencian una `action-group` o `action-method` definida
- [ ] Todos los `<menuitem action="...">` referencian una `action-view` definida en el fichero de vistas (no en el de menú)
- [ ] Todas las `<action-view>` referencian vistas (`grid`, `form`) que existen
- [ ] Los `<panel-related>` tienen tanto `form-view` como `grid-view`
- [ ] El grid y el form del `panel-related` están precedidos del bloque de comentarios `Vistas de ModeloPadre -> ModeloHijo` con asteriscos
- [ ] Los forms hijo con `onNew` tienen su `action-record` que asigna `__parent__`
- [ ] Las rutas de paquete siguen la arquitectura: `com.educaflow.subsystem.<nombre>.db.*` o `com.educaflow.system.<nombre>.db.*`

## Cuándo crear un subsistema vs un sistema

Crear un **subsistema** cuando la capacidad es reutilizable (no necesariamente) por múltiples sistemas o por otros subsistemas (ej: firmas, registro de entrada/salida, certificados digitales).

Crear un **sistema** es similar a un subsistema solo no necesita ser reutilizada por nadie (Se podría eliminar sin problemas si se deja de usar). Ejemplos: `tiposexpedientes/comision_servicio`, `importar`, `actas`. 


Pero la diferencia principal entre un sistema y un subsistema es que el sistema suele representar una funcionalidad que se ofrece a los usuarios y que ellos han solicitado. Es decir es una diferencia desde el punto de vista del negocio. Por ejemplo, el registro de entrada/salida no es algo que solicitan sino que es una necesidad que usan los expedientes.
Mientras que el subsistema es una parte más genérica y reutilizable que puede ser usada por varios sistemas. Por ejemplo, el subsistema de `firmas` puede ser usado por múltiples sistemas para gestionar la firma de documentos, mientras que el sistema `comision_servicio` es una funcionalidad concreta que utiliza el subsistema de firmas para gestionar los expedientes de comisión de servicio.
