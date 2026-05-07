---
name: k-sistemas
description: Este Skill permite diseñar y generar la estructura de carpetas, ficheros y código Java y XML de un sistema o subsistema en el proyecto Axelor — incluye modelos de dominio XML, servicios ModelService/DefaultModelService, controladores con @CallMethod/ModelServiceFactory y sus referencias detalladas.
---

# Sistemas y Subsistemas

## Ficheros de este skill

| Fichero | Contenido |
|---------|-----------|
| `modelos.md` | Cómo definir entidades en XML de dominio: tipos de campo, relaciones, enumerados, herencia, finders y extra-code |
| `servicios.md` | Cómo implementar la capa de servicio: interfaz `ModelService`, implementación `DefaultModelService`, constructor obligatorio, validaciones con `BusinessMessages` y DTOs |
| `controladores.md` | Cómo implementar controladores: tipos de método con `@CallMethod`, `ActionRequestHelper`, `ActionResponseHelper`, `AllowProperties` y señales de respuesta |

---

Un sistema y un subsistema son técnicamente idénticos en estructura. La diferencia es conceptual:

- **Subsistema** — capacidad reutilizable que usan otros subsistemas o sistemas. No desaparecería si se eliminara un sistema concreto. Ejemplos: `firmas`, `registroentradasalida`, `sistemaeducativo`, `common`.
- **Sistema** — funcionalidad concreta ofrecida al usuario. No necesita ser reutilizada por nadie. Se eliminaría sin afectar al resto. Ejemplos: `actas`, `importar`, `tiposexpedientes/comision_servicio`.

## Reglas de dependencia

```
base/util  ←  base/infrastructure  ←  subsystem  ←  system
```

- Un **subsistema** puede depender de `base/` y de otros subsistemas (sin ciclos). Nunca de un sistema.
- Un **sistema** puede depender de `base/` y de subsistemas. **Nunca de otro sistema.**

## Raíz de cada capa

| Capa                | Paquete Java                        | Ruta de ficheros       |
|---------------------|-------------------------------------|------------------------|
| base/util           | `com.educaflow.base.util`           | `base/util/`           |
| base/infrastructure | `com.educaflow.base.infrastructure` | `base/infrastructure/` |
| subsystem           | `com.educaflow.subsystem.<nombre>`  | `subsystem/<nombre>/`  |
| system              | `com.educaflow.system.<nombre>`     | `system/<nombre>/`     |

## Estructura interna

Tanto sistemas como subsistemas comparten la misma estructura de carpetas:

```
<nombre>/
├── domains/            ← modelos de datos (XML de Axelor)
├── service/            ← interfaz del servicio + DTOs + interfaces de callback
│   └── impl/           ← implementación del servicio
├── db/                 ← repositorios JPA y listeners (generados o escritos a mano)
│   └── repo/
├── controller/         ← controladores Axelor (@CallMethod), si los hay
├── module/             ← módulo Guice (solo si hay bindings que no descubre ModelServiceFactory)
├── views/              ← vistas XML de Axelor (grids, formularios, actions, menuitems inline)
├── documentospdf/      ← plantillas PDF propias del sistema/subsistema (opcional)
└── data-init/          ← datos iniciales de BD (opcional)
    └── input/
```

### `domains/`

Contiene los ficheros XML de definición de entidades Axelor (namespace `domain-models`). Un fichero por entidad. El build genera las clases Java correspondientes en `db/` (no se editan manualmente).

También puede contener diagramas de modelo (`.plantuml`, `.png`) para documentación.

**Cuándo consultar `modelos.md`:** al crear o modificar cualquier fichero XML de esta carpeta — tipos de campo, relaciones, enumerados, finders, extra-code en repositorio o en dominio. Ver también `references/models.md` y `references/repositories.md` para la referencia XSD completa. Ante cualquier duda sobre si un atributo existe o qué valores acepta, consultar `references/domain-models.xsd` — es la fuente de verdad oficial del schema.

### `service/`

Contiene:
- La **interfaz** del servicio: `MiEntidadService.java` — extiende `ModelService<MiEntidad>`.
- Los **DTOs de inserción** si son necesarios: `MiEntidadInsertDTO.java` (record Java).
- **Interfaces de callback** si las hay: p.ej. `TareaFirmaNotifier.java`.
- La subpaquete `impl/` con la **implementación**: `MiEntidadServiceImpl.java` — extiende `DefaultModelService<MiEntidad>`.

El paquete `service.impl` es el que busca `ModelServiceFactory` por convención — sin necesidad de módulo Guice.

**Cuándo consultar `servicios.md`:** al crear o modificar la interfaz o la implementación del servicio — constructor obligatorio, métodos `insert`/`update`/`remove`, validaciones con `BusinessMessages`, DTOs de inserción, cómo obtener otro servicio con `ModelServiceFactory`.

### `db/`

La carpeta `db/` está reservada para las clases Java generadas automáticamente por el build de Axelor a partir de los XML de `domains/`. **No se editan manualmente.**

La subcarpeta `db/repo/` sí se edita manualmente cuando se necesita:
- **Repository personalizado**: hereda de `Abstract<Entidad>Repository` (generado) para añadir queries propias.
- **Listener JPA**: intercepta eventos de ciclo de vida (prePersist, preUpdate, etc.).

Si no hay repositorios ni listeners propios, la carpeta puede existir vacía con un `.gitkeep`.

> **REGLA CRÍTICA — lógica de negocio en el servicio, NO en listeners:**
> Los listeners JPA se usan **rarísimamente** y solo para casos técnicos muy específicos (auditoría externa, sincronización con sistemas de terceros). La lógica de negocio **NUNCA va en un listener** — va siempre en el servicio mediante métodos `fireActionRule_*` llamados desde `insert()`/`update()`/`remove()`. Si tienes dudas, usa el servicio.

**Cuándo consultar `modelos.md` y `references/repositories.md`:** al añadir finders o extra-code en los XML de `domains/` para generar métodos en el repositorio abstracto, o al crear un repositorio personalizado en `db/repo/`.

### `controller/`

Contiene los controladores Java: clases con métodos `@CallMethod` que reciben `ActionRequest`/`ActionResponse` y delegan en los servicios. Solo existen si las vistas necesitan lógica de negocio disparada por botones o eventos de formulario.

Convención de paquete: `com.educaflow.{layer}.{nombre}.controller` (singular).

**Cuándo consultar `controladores.md`:** al crear o modificar cualquier controlador — tipos de método (`ActionRequest`/`ActionResponse` vs. retorno directo), uso de `ActionRequestHelper`/`ActionResponseHelper`, `AllowProperties`, señales de respuesta (`setSignal`). Ver también `references/ActionRequest.md`, `references/ActionResponse.md` y `references/action-method.md` para la API completa.

### `module/`

Módulo Guice que registra bindings interfaz → implementación cuando `ModelServiceFactory` no los puede descubrir automáticamente (p.ej. servicios que no son `ModelService`, implementaciones de terceros).

> **REGLA CRÍTICA — NUNCA crear un módulo para implementaciones de `ModelService`:** `ModelServiceFactory` descubre automáticamente cualquier clase en el paquete `service.impl.*ServiceImpl`. Crear un módulo Guice para registrar un `ModelService` es un error — el servicio quedaría registrado dos veces y rompería la factoría. Solo crear `module/` cuando haya bindings que genuinamente no pueden descubrirse por convención (interfaces no relacionadas con `ModelService`, decoradores, servicios de infraestructura).

### `views/`

Ficheros XML de vistas Axelor (namespace `object-views`). Cada fichero puede contener grids, formularios y actions para una o varias entidades del sistema/subsistema.

Convención de nombres de ficheros: `<NombreEntidad>.xml` (si hay pocas vistas) o agrupar por funcionalidad.

También contiene `i18n_es.csv` e `i18n_ca.csv` (generados automáticamente por el build — **no se crean a mano**).

**Cuándo consultar el skill `k-vistas`:** al crear o modificar cualquier fichero XML de esta carpeta — grids (`@Main-grid`, `@Search-grid`), formularios (`@Main-form`, `@View-form`), paneles, `panel-related`, `panel-tabs`, acciones (`action-group`, `action-method`, `action-record`, `action-attrs`), menús y árbol. `k-vistas` es la referencia completa para todo lo relacionado con vistas.

### `documentospdf/` (opcional)

Plantillas PDF (`.pdf`, `.odt`) y recursos gráficos (`.png`, logos) usados para generar documentos PDF dentro del servicio. Se cargan como recursos del classpath.

### `data-init/` (opcional)

Datos iniciales de base de datos cargados al arrancar la aplicación:
- `input-config.xml` — configura las fuentes de datos a importar.
- `input/*.xml` — registros concretos a insertar (roles, permisos, tipos, etc.).

## Ejemplos

### `subsystem/firmas`

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

### `subsystem/registroentradasalida`

```
registroentradasalida/
├── domains/
│   ├── RegistroEntrada.xml
│   └── RegistroSalida.xml
├── service/
│   ├── RegistroEntradaService.java
│   ├── RegistroEntradaInsertDTO.java
│   ├── RegistroSalidaService.java
│   ├── RegistroSalidaInsertDTO.java
│   ├── PersonaRegistro.java       ← record DTO
│   └── impl/
│       ├── RegistroEntradaServiceImpl.java
│       └── RegistroSalidaServiceImpl.java
├── db/
│   └── repo/
│       ├── RegistroEntradaRepository.java
│       ├── RegistroEntradaListener.java   ← EXCEPCIÓN: auditoría hacia sistema externo (no es el patrón habitual)
│       ├── RegistroSalidaRepository.java
│       └── RegistroSalidaListener.java    ← EXCEPCIÓN: auditoría hacia sistema externo (no es el patrón habitual)
├── documentospdf/
│   └── registro_entrada_plantilla.pdf
└── views/
    ├── registro_entrada.xml
    └── registro_salida.xml
```

> **Nota sobre los listeners de este ejemplo:** estos listeners existen por razones históricas de integración con un sistema externo de auditoría y son una excepción. En un subsistema nuevo, la lógica de negocio va siempre en `fireActionRule_*` dentro del servicio, nunca en listeners.

### `subsystem/sistemaeducativo`

```
sistemaeducativo/
├── domains/
│   ├── LeyEducativa.xml
│   ├── Nivel.xml
│   ├── Grado.xml
│   ├── FamiliaProfesional.xml
│   ├── Ciclo.xml
│   ├── Curso.xml
│   ├── CursoModulo.xml
│   └── Modulo.xml
├── service/
│   ├── LeyEducativaService.java
│   └── impl/
│       └── LeyEducativaServiceImpl.java
├── controller/
│   └── LeyEducativaController.java
└── views/
    ├── LeyEducativa.xml
    ├── Nivel.xml
    ├── Grado.xml
    ├── FamiliaProfesional.xml
    ├── Ciclo.xml
    ├── Curso.xml
    ├── CursoModulo.xml
    └── Modulo.xml
```

## Advertencia importante

**NUNCA uses como referencia el código de `expedientes`, `tiposexpedientes` ni `tramites`** — siguen una arquitectura completamente distinta al resto del proyecto y tomarlos como ejemplo llevaría a implementaciones incorrectas.

## Workflow para crear un sistema o subsistema

Cuando se crea o modifica un sistema/subsistema, seguir este orden:

1. **Analiza** qué hay que crear: dominio, servicios, controladores, vistas, menús
2. **Lee los ficheros existentes** antes de generar nada — modelo, vistas del sistema o subsistema, menús, etc.
3. **Decide el patrón estructural** de vistas a aplicar (ver skill `k-vistas`)
4. **Genera o modifica el dominio** — consultar `modelos.md` para la sintaxis XML; `references/models.md` y `references/repositories.md` para la referencia completa
5. **Genera o modifica los servicios** — consultar `servicios.md` para la estructura de interfaz e implementación, constructor obligatorio, validaciones y DTOs
6. **Genera o modifica los controladores** — consultar `controladores.md` para tipos de método, `ActionRequestHelper`/`ActionResponseHelper` y señales; `references/ActionRequest.md`, `references/ActionResponse.md` y `references/action-method.md` para la API completa
7. **Genera o modifica las vistas** — consultar el skill `k-vistas` para grids, formularios, actions y menús
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

Crear un **sistema** es similar a un subsistema solo que no necesita ser reutilizada por nadie (Se podría eliminar sin problemas si se deja de usar). Un sistema no depende de otros sistemas pero sí de otros subsistemas. Ejemplos: `tiposexpedientes/comision_servicio`, `importar`, `actas`.

La diferencia principal entre un sistema y un subsistema es que el sistema suele representar una funcionalidad que se ofrece a los usuarios y que ellos han solicitado. Es decir, es una diferencia desde el punto de vista del negocio. Por ejemplo, el registro de entrada/salida no es algo que los usuarios solicitan sino que es una necesidad que usan los expedientes. Mientras que el subsistema es una parte más genérica y reutilizable que puede ser usada por varios sistemas. Por ejemplo, el subsistema de `firmas` puede ser usado por múltiples sistemas para gestionar la firma de documentos, mientras que el sistema `comision_servicio` es una funcionalidad concreta que utiliza el subsistema de firmas para gestionar los expedientes de comisión de servicio.

---

## Conocimiento detallado por área

| Tema | Fichero | Descripción |
|------|---------|-------------|
| Modelos | `modelos.md` | Entidades XML de dominio, tipos de campo, relaciones, enumerados, finders, extra-code en repositorio y en dominio |
| Servicios | `servicios.md` | Interfaz `ModelService`, implementación `DefaultModelService`, constructor obligatorio, validaciones, DTOs y descubrimiento automático |
| Controladores | `controladores.md` | Estructura, tipos de método, `ModelServiceFactory`, `ActionRequestHelper`, `ActionResponseHelper` y reglas de diseño |

## Referencias detalladas

- `references/domain-models.xsd` — **schema XSD oficial de Axelor 8.1**: fuente de verdad para verificar qué atributos y etiquetas son válidos en cualquier elemento de dominio (`<entity>`, `<string>`, `<integer>`, `<many-to-one>`, `<one-to-many>`, `<enum>`, `<finder-method>`, `<extra-code>`, etc.) y si un fichero XML está bien formado. Consultar este fichero ante cualquier duda sobre si un atributo existe o cuáles son sus valores permitidos.
- `references/models.md` — atributos y elementos completos del modelo de dominio XML
- `references/repositories.md` — finders y extra-code en repositorios
- `references/ActionRequest.md` — métodos públicos de `ActionRequest`
- `references/ActionResponse.md` — métodos públicos de `ActionResponse`
- `references/action-method.md` — sintaxis de `<action-method>` para llamar controladores desde vistas
- `references/controller.md` — estructura de controladores Axelor (referencia oficial)
- `examples/domains.xml` — ejemplo real de modelo de dominio con entidad que extiende `Expediente`
- `examples/Expediente.xml` — modelo base `Expediente` con herencia `JOINED`