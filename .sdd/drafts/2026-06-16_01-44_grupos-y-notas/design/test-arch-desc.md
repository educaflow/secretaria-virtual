# Tests de arquitectura (ArchUnit)

Descripción de los tests de arquitectura (ArchUnit 1.4.2, JUnit 5) que verifican que las clases del diseño respetan la arquitectura documentada del proyecto. **Solo descripción, sin código**: `/sdd-implementer-system` genera el código a partir de aquí. El catálogo de reglas (`C1`–`C22`) vive en el skill `k-archunit` (`secretaria-virtual-rules.md`); aquí se selecciona y concreta el subconjunto que aplica a este diseño.

## Clase de test
- **Nombre:** `ArquitecturaGruposNotasTest`
- **Ámbito (`@AnalyzeClasses`):** `com.educaflow.system.gruposnotas` (`importOptions = DoNotIncludeTests`). El ámbito se acota al paquete del propio sistema, no a todo `com.educaflow`: así el test falla específicamente por el código nuevo y no por violaciones preexistentes de otros subsistemas (esas las cubre el catálogo global). Las reglas de capa (`C4`, `C5`, `C8`) miran el **destino** de la dependencia, que vive fuera del paquete anclado (FQN completo del destino prohibido).
- **Catálogo de referencia:** `k-archunit/secretaria-virtual-rules.md`.
- **`PAQUETES_EXENTOS`:** no aplica. El diseño vive en `com.educaflow.system.gruposnotas` y no toca `expedientes`/`tiposexpedientes`/`tramites`; el código nuevo cumple las reglas sin exenciones.

---

## Reglas aplicables

### C4 — `systemNoDependeDeSecretariaVirtual`
- **Qué verifica:** ninguna clase del sistema `gruposnotas` depende de la capa de ensamblaje `com.educaflow.secretariavirtual..`.
- **Ámbito:** origen `com.educaflow.system.gruposnotas..`; destino prohibido `com.educaflow.secretariavirtual..` (FQN completo).
- **Sujetos del diseño:** todas las clases del sistema (`Grupo`, `ModuloGrupo`, `AlumnoGrupo`, `Nota`, los 4 servicios y sus impl., los 3 repositorios, `GrupoController`, `NotaController`).
- **Resultado esperado:** PASS (código nuevo).
- **Origen:** `C4` (catálogo) ← `k-sistemas/SKILL.md` §"Reglas de dependencia".

### C5 — `secretariaVirtualNoEsAccedidaPorNadie`
- **Qué verifica:** ninguna clase del sistema (capa `system`) depende de `com.educaflow.secretariavirtual..` (capa más alta de ensamblaje). El sistema modifica `secretariavirtual/menus/menus.xml`, pero eso es un recurso XML, no una dependencia de código Java; ninguna clase Java del diseño puede importar de `secretariavirtual`.
- **Ámbito:** origen `com.educaflow.system.gruposnotas..` (subconjunto de `com.educaflow.system..`, uno de los orígenes de la regla); destino prohibido `com.educaflow.secretariavirtual..`.
- **Sujetos del diseño:** todas las clases Java del sistema.
- **Resultado esperado:** PASS (código nuevo).
- **Origen:** `C5` (catálogo) ← decisión de proyecto: `secretariavirtual` = capa de ensamblaje superior. (Coincide en sujeto/destino con `C4` para este diseño; se mantienen ambas porque el catálogo las define con orígenes documentales distintos.)

### C8 — `sistemasIndependientesEntreSi`
- **Qué verifica:** el sistema `gruposnotas` no depende de ningún otro `system` de `com.educaflow.system..` (cada sistema es eliminable sin afectar a los demás). Sí puede depender de subsistemas (`sistemaeducativo` para `Curso`/`Modulo`/`CursoModulo`, `common` para `Centro`), lo cual la regla permite.
- **Ámbito:** `slices().matching("com.educaflow.system.(*)..")` acotado al sistema del diseño; la rebanada `gruposnotas` no debe depender de otra rebanada `system`.
- **Sujetos del diseño:** todas las clases del sistema.
- **Resultado esperado:** PASS (código nuevo; solo depende de subsistemas y de `base/**`, no de otros `system`).
- **Origen:** `C8` (catálogo) ← `k-sistemas/SKILL.md` §"Reglas de dependencia" ("Un sistema… Nunca de otro sistema").

### C9 — `controladorNoAccedeARepositorio`
- **Qué verifica:** ningún controlador depende de `..db.repo..`; delega el acceso a datos en el servicio.
- **Ámbito:** `com.educaflow.system.gruposnotas.controller..`; destino prohibido `..db.repo..` (resuelto a `com.educaflow.system.gruposnotas.db.repo..` dentro del ámbito anclado).
- **Sujetos del diseño:** `GrupoController`, `NotaController`. (Ambos solo inyectan `ModelServiceFactory` y delegan en `GrupoService`/`NotaService`.)
- **Resultado esperado:** PASS (código nuevo).
- **Origen:** `C9` (catálogo) ← `k-sistemas/controladores.md` §"NO lógica de negocio, I/O o acceso a BD en el controlador".

### C10 — `controladorNoUsaJpaRepository`
- **Qué verifica:** ningún controlador depende de `com.axelor.db.JpaRepository`; cargar entidades es del servicio.
- **Ámbito:** `com.educaflow.system.gruposnotas.controller..`; destino prohibido FQN `com.axelor.db.JpaRepository`.
- **Sujetos del diseño:** `GrupoController`, `NotaController`. (Extraen modelo/original con `ModelServiceFactory` y los helpers de request, sin `JpaRepository.of(...)`.)
- **Resultado esperado:** PASS (código nuevo).
- **Origen:** `C10` (catálogo) ← `k-sistemas/controladores.md` §"NO lee entidades con `JpaRepository.of(...)`".

### C11 — `repositorioNoDependeDeServicioNiControlador`
- **Qué verifica:** ningún repositorio depende de `..service..` ni `..controller..`; la dependencia es Controller → Service → Repository, nunca al revés.
- **Ámbito:** `com.educaflow.system.gruposnotas.db.repo..`; destinos prohibidos `..service..` y `..controller..`.
- **Sujetos del diseño:** `GrupoRepository`, `AlumnoGrupoRepository`, `NotaRepository`. (Sus finders/contadores usan solo `all().filter(...).bind(...)` sobre entidades de `..db..`.)
- **Resultado esperado:** PASS (código nuevo).
- **Origen:** `C11` (catálogo) ← `k-sistemas/SKILL.md` §`db/` + `controladores.md`/`servicios.md`.

### C12 — `servicioNoDependeDeControlador`
- **Qué verifica:** ningún servicio depende de `..controller..`; la dependencia es Controller → Service, nunca al revés.
- **Ámbito:** `com.educaflow.system.gruposnotas.service..` (incluye `service.impl..`); destino prohibido `..controller..`.
- **Sujetos del diseño:** interfaces `GrupoService`, `ModuloGrupoService`, `AlumnoGrupoService`, `NotaService` e impl. `GrupoServiceImpl`, `ModuloGrupoServiceImpl`, `AlumnoGrupoServiceImpl`, `NotaServiceImpl`.
- **Resultado esperado:** PASS (código nuevo).
- **Origen:** `C12` (catálogo) ← `k-sistemas/controladores.md` ("el controlador llama al servicio").

### C13 — `entidadesDominioSonPojos`
- **Qué verifica:** las entidades de `..db..` (excluyendo `..db.repo..`) no dependen de `..service..` ni `..controller..`; son POJOs y la lógica de negocio vive en el servicio.
- **Ámbito:** `com.educaflow.system.gruposnotas.db..` excluyendo `..db.repo..`; destinos prohibidos `..service..` y `..controller..`.
- **Sujetos del diseño:** `Grupo`, `ModuloGrupo`, `AlumnoGrupo`, `Nota` (generadas por Axelor desde `domains/*.xml`). **Relevante:** el campo calculado `AlumnoGrupo.notaMedia` (CC-001) tiene su algoritmo **inline en el cuerpo CDATA** del propio campo del dominio y referencia únicamente `Nota` y `ValorNota` (mismo paquete `..db..`); por diseño explícito **no** depende de `..service..`, lo que esta regla protege.
- **Resultado esperado:** PASS (código nuevo; el CDATA de `notaMedia` no llama a ningún servicio).
- **Origen:** `C13` (catálogo) ← `k-sistemas/SKILL.md` §`db/` ("lógica de negocio en el servicio, NO en la entidad").

### C14 — `noBeansGetEnControladorNiServiceImpl`
- **Qué verifica:** ni los controladores ni los `*ServiceImpl` dependen de `com.axelor.inject.Beans` (service-locator); se usa inyección / `ModelServiceFactory`.
- **Ámbito:** `com.educaflow.system.gruposnotas.controller..` y `com.educaflow.system.gruposnotas.service.impl..`; destino prohibido FQN `com.axelor.inject.Beans`.
- **Sujetos del diseño:** `GrupoController`, `NotaController`, `GrupoServiceImpl`, `ModuloGrupoServiceImpl`, `AlumnoGrupoServiceImpl`, `NotaServiceImpl`. (Todos inyectan repositorios y/o `ModelServiceFactory` por constructor/campo, sin `Beans.get`.)
- **Resultado esperado:** PASS (código nuevo).
- **Origen:** `C14` (catálogo) ← `k-sistemas/controladores.md` §"NO obtener servicios con `Beans.get(...)`".

### C15 — `controladoresNombreYUbicacion` (C15a + C15b)
- **Qué verifica:** (a) toda clase en `..controller..` termina en `Controller`; (b) toda clase `*Controller` reside en `..controller..`.
- **Ámbito:** `com.educaflow.system.gruposnotas.controller..`.
- **Sujetos del diseño:** `GrupoController`, `NotaController`.
- **Resultado esperado:** PASS (código nuevo).
- **Origen:** `C15` (catálogo) ← `k-sistemas/controladores.md` §"un controlador por entidad" + `SKILL.md` §`controller/`.

### C16 — `implServicioNombreYUbicacion`
- **Qué verifica:** toda clase asignable a `DefaultModelService` (salvo la propia base de Axelor) termina en `ServiceImpl` y reside en `..service.impl..`; `ModelServiceFactory` la descubre por ese nombre/ubicación exactos.
- **Ámbito:** `com.educaflow.system.gruposnotas.service.impl..`.
- **Sujetos del diseño:** `GrupoServiceImpl`, `ModuloGrupoServiceImpl`, `AlumnoGrupoServiceImpl`, `NotaServiceImpl` (extienden `DefaultModelService<T>`).
- **Resultado esperado:** PASS (código nuevo; nombre y ubicación correctos para que `ModelServiceFactory.resolve(...)` no devuelva `null`).
- **Origen:** `C16` (catálogo) ← `k-sistemas/servicios.md` §"Descubrimiento automático".

### C17 — `interfazServicioNombreYUbicacion`
- **Qué verifica:** toda interfaz asignable a `ModelService` (salvo la propia base de Axelor) termina en `Service` y reside en `..service..`.
- **Ámbito:** `com.educaflow.system.gruposnotas.service..`.
- **Sujetos del diseño:** `GrupoService`, `ModuloGrupoService`, `AlumnoGrupoService`, `NotaService` (extienden `ModelService<T>`).
- **Resultado esperado:** PASS (código nuevo).
- **Origen:** `C17` (catálogo) ← `k-sistemas/servicios.md` §"Estructura de la interfaz".

### C18 — `repositoriosNombre`
- **Qué verifica:** toda clase en `..db.repo..` termina en `Repository` o `Listener`.
- **Ámbito:** `com.educaflow.system.gruposnotas.db.repo..`.
- **Sujetos del diseño:** `GrupoRepository`, `AlumnoGrupoRepository`, `NotaRepository`.
- **Resultado esperado:** PASS (código nuevo).
- **Origen:** `C18` (catálogo) ← `k-sistemas/SKILL.md` §`db/`.

### C21 — `modelServiceNoSeInyecta`
- **Qué verifica:** ningún campo de tipo `ModelService` lleva `@Inject` (`com.google.inject.Inject` ni `jakarta.inject.Inject`); los `ModelService` se resuelven con `ModelServiceFactory.resolve(...)`, no se inyectan. Se describe con `allowEmptyShould(true)` por si el ámbito acotado no tiene ningún campo de ese tipo.
- **Ámbito:** `com.educaflow.system.gruposnotas..` (la regla del catálogo no acota paquete; aquí queda restringida por el `@AnalyzeClasses` del sistema).
- **Sujetos del diseño:** campos inyectados de las impl. y controladores. **Relevante:** los `*ServiceImpl` inyectan **repositorios** y, donde colaboran con otro servicio, `ModelServiceFactory`, **no** campos `ModelService`/`*Service`. En concreto: `GrupoServiceImpl` inyecta `GrupoRepository` y la base abstracta de `ModuloGrupo` (sin `ModelServiceFactory`); `AlumnoGrupoServiceImpl` inyecta `AlumnoGrupoRepository`, `NotaRepository` y `ModelServiceFactory` (este último para resolver `NotaService` al crear las notas); `NotaServiceImpl` inyecta únicamente `NotaRepository`. La colaboración entre servicios (p.ej. `AlumnoGrupoServiceImpl` crea las notas vía `NotaService`) se resuelve en el método con `ModelServiceFactory`, nunca con un campo `@Inject` de tipo `ModelService`/`*Service`.
- **Resultado esperado:** PASS (código nuevo; ningún campo `@Inject` de tipo `ModelService`).
- **Origen:** `C21` (catálogo) ← `k-sistemas/servicios.md` §"Obtener otro servicio" + `controladores.md` §`ModelServiceFactory`.

### C22 — `noStreamsEstandar`
- **Qué verifica:** ninguna clase del sistema accede a `System.out` / `System.err`; se usa logging.
- **Ámbito:** `com.educaflow.system.gruposnotas..` (todas las clases Java nuevas).
- **Sujetos del diseño:** todas las clases del sistema (servicios, impl., repositorios, controladores).
- **Resultado esperado:** PASS (código nuevo; la deuda preexistente del catálogo vive en `base/infrastructure`, fuera del ámbito anclado, así que no contamina este test).
- **Origen:** `C22` (catálogo) ← `GeneralCodingRules` (higiene) + CLAUDE.md §logging.

---

## Reglas del catálogo no aplicables
- **C1** — el diseño no crea clases en `com.educaflow.base.util..` (la regla aplica a esa capa, no a `system`).
- **C2** — el diseño no crea clases en `com.educaflow.base.infrastructure..`.
- **C3** — el diseño no crea clases en `com.educaflow.subsystem..` (es un `system`; la regla de capa que aplica es `C4`/`C5`).
- **C6** — versión consolidada `layeredArchitecture()` de `C1`–`C5`; redundante con las reglas individuales de capa ya descritas (`C4`, `C5`). Se describen las individuales por dar mensajes de fallo más específicos sobre el sistema acotado.
- **C7** — sin ciclos **entre subsistemas**; el diseño es un `system`, no un `subsystem` (la independencia entre sistemas la cubre `C8`).
- **C19** — el diseño **no** crea módulo Guice (Paso 6: "No aplica"; los 4 servicios son `ModelService` descubiertos por `ModelServiceFactory` y los repositorios/`ModelServiceFactory` están disponibles sin binding manual).
- **C20** — el diseño **no** define DTOs (`*DTO`); las acciones reciben entidades filtradas por `AllowProperties`, no records DTO.

## Cobertura
- Artefactos del diseño cubiertos:
  - Entidades (`Grupo`, `ModuloGrupo`, `AlumnoGrupo`, `Nota`) → C13 (y C4/C5/C8/C22 como clases del sistema).
  - Interfaces de servicio (`GrupoService`, `ModuloGrupoService`, `AlumnoGrupoService`, `NotaService`) → C12, C17.
  - Impl. de servicio (`GrupoServiceImpl`, `ModuloGrupoServiceImpl`, `AlumnoGrupoServiceImpl`, `NotaServiceImpl`) → C12, C14, C16, C21.
  - Repositorios (`GrupoRepository`, `AlumnoGrupoRepository`, `NotaRepository`) → C11, C18.
  - Controladores (`GrupoController`, `NotaController`) → C9, C10, C14, C15.
  - Todas las clases del sistema → C4, C5, C8, C22.
- Reglas del catálogo aplicadas: C4, C5, C8, C9, C10, C11, C12, C13, C14, C15, C16, C17, C18, C21, C22.
- Reglas específicas del diseño: ninguna (el spec/diseño no impone restricciones estructurales no cubiertas por el catálogo; el control multi-centro/IDOR queda fuera del alcance de ArchUnit y se cubre con `k-secure-coding` y los tests E2E de `test-e2e-desc.md`).
- Reglas no aplicables (justificadas): C1, C2, C3, C6, C7, C19, C20.
- Reglas en FREEZE (deuda preexistente): ninguna (todo el código del sistema es nuevo y debe cumplir; el ámbito acotado a `com.educaflow.system.gruposnotas` excluye la deuda preexistente de otros paquetes).
