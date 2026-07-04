# Arquitectura — secretaría virtual

Descripción de **cómo está organizado** el proyecto: paquetes, responsabilidades y
casos especiales. Es documentación de **orientación** (el *qué es* y *qué hace cada
parte*), no de reglas verificables.

> **Mantener coherencia con [`architecture-rules.md`](architecture-rules.md).** Este
> fichero **describe** la arquitectura; `architecture-rules.md` **cataloga como reglas
> verificables** (decisiones estilo ADR, sin código) las invariantes de esa misma
> arquitectura (dependencias entre capas, Controller→Service→Repository,
> nomenclatura/ubicación), de las que `/create-arch-tests` genera los tests ArchUnit.
> Si cambias algo aquí,
> **MUST** comprobar si hay que actualizar `architecture-rules.md`, y viceversa. Los dos
> ficheros describen la misma arquitectura desde ángulos distintos y no deben divergir.

## Paquetes principales

Todo el proyecto cuelga del paquete `com.educaflow`. Bajo él existen 5 grandes paquetes:

- `base.util` — Utilidades de bajo nivel para no repetir pequeños trozos de código.
  Ejemplos: `JsonUtil`, `MetaFileUtil`, `ActionRequestHelper`, `AllowProperties`,
  `AxelorViewUtil`, `TextUtil`, `Convert`, `DniUtil`, `ReflectionUtil`, `SecurityUtil`,
  `CryptoUtil`, `XmlUtil`. El catálogo completo de clases y sus métodos está en
  [`src/main/java/com/educaflow/base/util/CLAUDE.md`](../src/main/java/com/educaflow/base/util/CLAUDE.md).
- `base.infrastructure` — Clases completas y reutilizables en cualquier proyecto (PDF,
  validación, criptografía, autofirma, mail, mapper, etc.). El catálogo de paquetes está
  en [`src/main/java/com/educaflow/base/infrastructure/CLAUDE.md`](../src/main/java/com/educaflow/base/infrastructure/CLAUDE.md).
- `subsystem` — Subsistemas con una función completa dentro de la aplicación: `firmas`,
  `expedientes`, `registroentradasalida`, `pdfutilities`, `common`, `certificados`,
  `importer`, `sistemaeducativo`, `security`.
- `system` — Sistemas completos con una función completa dentro de la aplicación.
- `secretariavirtual` — Donde están los menús y las tareas de inicialización.

El mapa de capas (`base/util ← base/infrastructure ← subsystem ← system ← secretariavirtual`)
y la convención de nombres por elemento (Controller/Service/Repository/Module/DTO) son la
base de las reglas verificables: el detalle canónico vive en
[`architecture-rules.md`](architecture-rules.md).

## Sistemas y subsistemas

Un sistema y un subsistema siguen **exactamente la misma estructura** interna. La
diferencia es de **dependencias**:

- De un **sistema** no depende nadie.
- De un **subsistema** sí depende alguien: puede depender de él un sistema u otro subsistema.
- **No puede haber relaciones cíclicas** entre ellos.

## Expedientes

Los expedientes son la parte más importante de la secretaría virtual y la más compleja;
por ello siguen una **arquitectura diferente** al resto de la aplicación.

> **Pendiente de documentar.** Esta sección está aún por hacer: cuando se defina/implemente
> la arquitectura de expedientes, desarrolla aquí su estructura propia (modelos, trámites,
> tipos de expediente, gestor de eventos, etc.) y revisa si introduce invariantes nuevas que
> deban reflejarse en [`architecture-rules.md`](architecture-rules.md).
