---
name: sistema-knowledge
description: Referencia sobre cómo están organizados los sistemas y subsistemas en EducaFlow — estructura de carpetas, contenido de cada carpeta y convenciones de paquetes.
---

# Sistema y Subsistema — estructura y organización

Un sistema y un subsistema son técnicamente idénticos en estructura. La diferencia es conceptual:

- **Subsistema** — capacidad reutilizable que usan otros subsistemas o sistemas. Puede ser usada por múltiples partes del proyecto. No desaparecería si se eliminara un sistema concreto. Ejemplos: `firmas`, `registroentradasalida`, `sistemaeducativo`, `common`.
- **Sistema** — funcionalidad concreta ofrecida al usuario. No necesita ser reutilizada por nadie. Se eliminaría sin afectar al resto. Ejemplos: `actas`, `importar`, `tiposexpedientes/comision_servicio`.

## Reglas de dependencia entre capas

```
base/util  ←  base/infrastructure  ←  subsystem  ←  system
```

- Un **subsistema** puede importar de `base/` y de otros subsistemas. Nunca de un sistema.
- Un **sistema** puede importar de `base/` y de subsistemas. Nunca de otro sistema.
- No hay ciclos: si A usa B, B no puede usar A.

## Raíz de cada capa

| Capa                | Paquete Java                        | Ruta de ficheros       |
|---------------------|-------------------------------------|------------------------|
| base/util           | `com.educaflow.base.util`           | `base/util/`           |
| base/infrastructure | `com.educaflow.base.infrastructure` | `base/infrastructure/` |
| subsystem           | `com.educaflow.subsystem.<nombre>`  | `subsystem/<nombre>/`  |
| system              | `com.educaflow.system.<nombre>`     | `system/<nombre>/`     |

## Estructura de carpetas

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

### `service/`

Contiene:
- La **interfaz** del servicio: `MiEntidadService.java` — extiende `ModelService<MiEntidad>`.
- Los **DTOs de inserción** si son necesarios: `MiEntidadInsertDTO.java` (record Java).
- **Interfaces de callback** si las hay: p.ej. `TareaFirmaNotifier.java`.
- La subpaquete `impl/` con la **implementación**: `MiEntidadServiceImpl.java` — extiende `DefaultModelService<MiEntidad>`.

El paquete `service.impl` es el que busca `ModelServiceFactory` por convención — sin necesidad de módulo Guice.

### `db/`

La carpeta `db/` está reservada para las clases Java generadas automáticamente por el build de Axelor a partir de los XML de `domains/`. **No se editan manualmente.**

La subcarpeta `db/repo/` sí se edita manualmente cuando se necesita:
- **Repository personalizado**: hereda de `Abstract<Entidad>Repository` (generado) para añadir queries propias.
- **Listener JPA**: intercepta eventos de ciclo de vida (prePersist, preUpdate, etc.).

Si no hay repositorios ni listeners propios, la carpeta puede existir vacía con un `.gitkeep`.

### `controller/`

Contiene los controladores Java: clases con métodos `@CallMethod` que reciben `ActionRequest`/`ActionResponse` y delegan en los servicios. Solo existen si las vistas necesitan lógica de negocio disparada por botones o eventos de formulario.

Convención de paquete: `com.educaflow.{layer}.{nombre}.controller` (singular).

### `module/`

Módulo Guice que registra bindings interfaz → implementación cuando `ModelServiceFactory` no los puede descubrir automáticamente (p.ej. servicios que no son `ModelService`, implementaciones de terceros).

Solo se crea si es necesario. Muchos sistemas/subsistemas no lo necesitan.

### `views/`

Ficheros XML de vistas Axelor (namespace `object-views`). Cada fichero puede contener grids, formularios y actions para una o varias entidades del sistema/subsistema.

Convención de nombres de ficheros: `<NombreEntidad>.xml` (si hay pocas vistas) o agrupar por funcionalidad.

También contiene `i18n_es.csv` e `i18n_ca.csv` (generados automáticamente por el build — **no se crean a mano**).

### `documentospdf/` (opcional)

Plantillas PDF (`.pdf`, `.odt`) y recursos gráficos (`.png`, logos) usados para generar documentos PDF dentro del servicio. Se cargan como recursos del classpath.

### `data-init/` (opcional)

Datos iniciales de base de datos cargados al arrancar la aplicación:
- `input-config.xml` — configura las fuentes de datos a importar.
- `input/*.xml` — registros concretos a insertar (roles, permisos, tipos, etc.).

## Ejemplo real: `subsystem/sistemaeducativo`

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

## Ejemplo real: `subsystem/registroentradasalida`

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
│   ├── PersonaRegistro.java
│   └── impl/
│       ├── RegistroEntradaServiceImpl.java
│       └── RegistroSalidaServiceImpl.java
├── db/
│   └── repo/
│       ├── RegistroEntradaRepository.java
│       ├── RegistroEntradaListener.java
│       ├── RegistroSalidaRepository.java
│       └── RegistroSalidaListener.java
├── documentospdf/
│   └── registro_entrada_plantilla.pdf
└── views/
    ├── registro_entrada.xml
    └── registro_salida.xml
```
