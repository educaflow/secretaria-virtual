# Reglas de arquitectura — secretaría virtual

Catálogo de las **decisiones de arquitectura verificables** del proyecto, en formato ADR (plantilla de Michael Nygard adaptada: *Contexto / Decisión / Verificación / Cumplimiento*, sin estado de ciclo de vida). Este fichero **NO contiene código ArchUnit**: describe **qué** debe verificarse; el código lo escribe `/code-create-arch-tests` en `src/test/java/com/educaflow/architecture` (una clase de test por categoría), como **proyección** de este catálogo. Para cambiar un test se edita este fichero y se regenera, nunca se editan los `.java` a mano.

> **Mantener coherencia con [`architecture.md`](architecture.md).** Este fichero **cataloga como reglas verificables** las invariantes de la arquitectura; `architecture.md` la **describe** en prosa (paquetes, responsabilidades, sistemas/subsistemas, expedientes). Si cambias una regla aquí, **MUST** comprobar si hay que actualizar la descripción de `architecture.md`, y viceversa. Los dos ficheros describen la misma arquitectura desde ángulos distintos y no deben divergir.

## Fuente de verdad

Las reglas se derivan de la **arquitectura documentada**, NO de lo que el código hace hoy (el código puede tener bugs; codificar "lo que el código hace" produciría reglas erróneas). El origen de cada regla es:

- `CLAUDE.md` raíz (capas `com.educaflow`, multicentro, sistemas/subsistemas).
- Skill `k-sistemas` (`SKILL.md`, `servicios.md`, `controladores.md`): capas, nomenclatura, Controller→Service→Repository, `ModelServiceFactory`, repositorios.
- Skill `k-secure-coding`: antipatrones (`Beans.get`, mass-assignment…).
- Skill `k-guice`: módulos `AxelorModule`.

> **CRITICAL — el código puede incumplir una regla correcta.** Si una clase real viola una regla, eso es un **bug del código**, NO una excepción a la regla. **MUST NOT** debilitar una regla para que "pase": el incumplimiento se registra en su apartado **Cumplimiento** y el test se genera congelado (ver más abajo) como deuda conocida.

## Convenciones de verificación (aplican a todas las reglas)

- **Ámbito de análisis:** las clases de producción del paquete `com.educaflow` (los tests quedan fuera del análisis).
- **Paquetes exentos:** `..expedientes..`, `..tiposexpedientes..` y `..tramites..` tienen **arquitectura propia** (EventManager, view_models, carpetas en plural) y quedan fuera de todas las reglas: se excluyen del **sujeto** de cada regla y, en las reglas de dependencias/ciclos, también como **origen y destino** de las dependencias analizadas.
- **Marcas de cumplimiento** (apartado *Cumplimiento* de cada regla):
  - ✅ CUMPLE — el código la cumple hoy; el test se genera tal cual.
  - ⚠️ — cumplimiento previsible pero no verificado; el test se genera tal cual y puede fallar al ejecutar.
  - ❌ INCUMPLE — hay violaciones conocidas; el test se genera **congelado** (freezing de ArchUnit, ver `/k-archunit`) con la violation store versionada en `src/test/resources/archunit_store`: el build sigue verde y solo falla ante violaciones **nuevas**. Arreglar la deuda encoge la store.
- **Mensaje de fallo:** cuando una regla define un *Mensaje*, el test **MUST** usarlo **literal** como justificación/descripción de la regla. **CRITICAL**: la descripción es la clave con la que el freezing identifica cada regla en la store; cambiar el texto re-baseliza sus violaciones.

## Mapa de capas y paquetes reales

```
base/util  ←  base/infrastructure  ←  subsystem  ←  system  ←  secretariavirtual
(más baja)                                                      (más alta, ensamblaje)
```

| Capa                | Paquete                              |
|---------------------|--------------------------------------|
| base/util           | `com.educaflow.base.util`            |
| base/infrastructure | `com.educaflow.base.infrastructure`  |
| subsystem           | `com.educaflow.subsystem.*`          |
| system              | `com.educaflow.system.*`             |
| secretariavirtual   | `com.educaflow.secretariavirtual.*`  |

Estructura interna de cada sistema/subsistema:

| Elemento          | Paquete           | Convención de nombre                 | Tipo base                 |
|-------------------|-------------------|--------------------------------------|---------------------------|
| Controlador       | `..controller..`  | `<Entidad>Controller`                | —                         |
| Interfaz servicio | `..service..`     | `<Entidad>Service`                   | `ModelService<T>`         |
| Impl. servicio    | `..service.impl..`| `<Entidad>ServiceImpl`               | `DefaultModelService<T>`  |
| Repositorio       | `..db.repo..`     | `<Entidad>Repository` / `<E>Listener`| —                         |
| Entidad dominio   | `..db..`          | `<Entidad>` (generada)               | POJO                      |
| DTO               | `..service..`     | `<algo>DTO`                          | `record`                  |
| Módulo Guice      | `..module..`      | `<Subsistema>Module`                 | `AxelorModule`            |

Tipos del framework Axelor a los que se refieren las reglas: `com.axelor.app.AxelorModule`, `com.axelor.db.modelservice.ModelService`, `com.axelor.db.modelservice.DefaultModelService`, `com.axelor.db.JpaRepository`, `com.axelor.inject.Beans`.

---

# Categoría 1 — Dependencias entre capas

Modelo en capas con dependencia **solo ascendente**: una capa solo puede ser usada por las capas superiores.

> **Decisión de verificación de la categoría:** la estratificación se verifica con **reglas negativas individuales** (C1–C5), no con una única regla consolidada de "arquitectura en capas": los mensajes de fallo resultan más específicos. (El antiguo identificador `C6` era esa variante consolidada y está **retirado**; no reutilizar el número.)

### C1 — `base.util` no depende de ningún otro paquete `com.educaflow`

**Decisión.** `base/util` es la capa más baja: solo puede usar el JDK y librerías externas, nunca otro paquete `com.educaflow`.

**Verificación.**
- Sujeto: clases de `com.educaflow.base.util..`.
- Condición: ninguna depende de clases de `com.educaflow..` que estén **fuera** de `com.educaflow.base.util..`.
- Exenciones: no aplican.
- Mensaje: «base/util es la capa más baja: no puede depender de ningún otro paquete com.educaflow».

**Cumplimiento.** ✅ CUMPLE.

### C2 — `base.infrastructure` solo depende (dentro de educaflow) de `base.util`

**Decisión.** `base/infrastructure` puede usar `base/util` y a sí misma, nunca `subsystem`, `system` ni `secretariavirtual`.

**Verificación.**
- Sujeto: clases de `com.educaflow.base.infrastructure..`.
- Condición: ninguna depende de clases de `com.educaflow.subsystem..`, `com.educaflow.system..` ni `com.educaflow.secretariavirtual..`.
- Exenciones: no aplican.
- Mensaje: «base/infrastructure solo puede depender, dentro de com.educaflow, de base/util».

**Cumplimiento.** ✅ CUMPLE.

### C3 — `subsystem` no depende de `system` ni de `secretariavirtual`

**Decisión.** Un subsistema puede depender de `base/**` y de otros subsistemas (sin ciclos, ver C7), nunca de un sistema ni del ensamblaje.

**Verificación.**
- Sujeto: clases de `com.educaflow.subsystem..`, excluidos los paquetes exentos.
- Condición: ninguna depende de clases de `com.educaflow.system..` ni `com.educaflow.secretariavirtual..`.
- Mensaje: «un subsystem nunca depende de un system ni del ensamblaje secretariavirtual».

**Cumplimiento.** ✅ CUMPLE.

### C4 — `system` no depende de `secretariavirtual`

**Decisión.** Un sistema puede depender de `base/**` y de subsistemas, nunca del ensamblaje (capa más alta). La independencia entre sistemas la cubre C8.

**Verificación.**
- Sujeto: clases de `com.educaflow.system..`, excluidos los paquetes exentos.
- Condición: ninguna depende de clases de `com.educaflow.secretariavirtual..`.
- Mensaje: «un system nunca depende del ensamblaje secretariavirtual (capa más alta)».

**Cumplimiento.** ✅ CUMPLE.

### C5 — `secretariavirtual` es la capa más alta: nadie depende de ella

**Contexto.** `secretariavirtual` (menús, arranque, vistas globales) ensambla la aplicación; si otra capa dependiera de ella, dejaría de ser el punto de ensamblaje.

**Decisión.** Ninguna otra capa (`base`, `subsystem`, `system`) puede depender de `secretariavirtual`.

**Verificación.**
- Sujeto: clases de `com.educaflow.base..`, `com.educaflow.subsystem..` y `com.educaflow.system..`, excluidos los paquetes exentos.
- Condición: ninguna depende de clases de `com.educaflow.secretariavirtual..`.
- Mensaje: «secretariavirtual es la capa más alta: ninguna otra capa puede depender de ella».

**Cumplimiento.** ✅ CUMPLE.

### C7 — Sin ciclos entre subsistemas

**Decisión.** No puede haber dependencias cíclicas entre subsistemas (`subsystem.A → subsystem.B → A`).

**Verificación.**
- Sujeto: los subsistemas, entendidos como *slices* = subpaquetes de **primer nivel** de `com.educaflow.subsystem`.
- Condición: el grafo de dependencias entre slices está libre de ciclos.
- Exenciones: `..expedientes..` queda fuera del análisis (como origen y como destino).
- Mensaje: el generado por defecto (sin mensaje propio).

**Cumplimiento.** ❌ INCUMPLE (test congelado): ciclo `subsystem.common ↔ subsystem.importacion` por la relación JPA bidireccional `Centro.tareasImportacion` ↔ `TareaImportacion.centro` / `ResultadoImportacion.centro`. Arreglo posible: eliminar la back-reference `Centro.tareasImportacion`.

### C8 — Los sistemas son independientes entre sí

**Contexto.** Cada sistema se debe poder eliminar sin afectar a los demás. Sí pueden depender de subsistemas.

**Decisión.** Ningún `system` puede depender de otro `system`.

**Verificación.**
- Sujeto: los sistemas, entendidos como *slices* = subpaquetes de **primer nivel** de `com.educaflow.system`.
- Condición: los slices no dependen unos de otros.
- Exenciones: `..tiposexpedientes..` y `..tramites..` quedan fuera del análisis (como origen y como destino).
- Mensaje: el generado por defecto (sin mensaje propio).

**Cumplimiento.** ✅ CUMPLE.

---

# Categoría 2 — Estructura interna (Controller → Service → Repository)

Dentro de un sistema/subsistema la dependencia fluye Controller → Service → Repository → Entidad. Todas las reglas de esta categoría excluyen de su sujeto los paquetes exentos.

### C9 — Un controlador no accede a un repositorio (pasa por el servicio)

**Contexto.** El controlador es el punto de entrada y delega TODA la lógica (incluido el acceso a datos) en el servicio.

**Decisión.** Un controlador nunca usa un repositorio directamente.

**Verificación.**
- Sujeto: clases de `..controller..`, excluidos los paquetes exentos.
- Condición: ninguna depende de clases de `..db.repo..`.
- Mensaje: «el controlador delega el acceso a datos en el servicio, nunca usa el repositorio directamente».

**Cumplimiento.** ❌ INCUMPLE (test congelado): `gestioncentro.controller.GestionCentroController` accede a repositorios directamente.

### C10 — Un controlador no usa `com.axelor.db.JpaRepository`

**Contexto.** Cargar entidades (`JpaRepository.of(X.class).find(id)`) es responsabilidad del servicio.

**Decisión.** Un controlador nunca depende de `com.axelor.db.JpaRepository`.

**Verificación.**
- Sujeto: clases de `..controller..`, excluidos los paquetes exentos.
- Condición: ninguna depende de la clase `com.axelor.db.JpaRepository`.
- Mensaje: «cargar entidades es del servicio; el controlador no usa JpaRepository».

**Cumplimiento.** ❌ INCUMPLE (test congelado): `firmas.controller.TareaFirmaController` hace `JpaRepository.of(...).find(...)`.

### C11 — Un repositorio no depende de servicios ni controladores

**Decisión.** La dependencia es Controller → Service → Repository; nunca al revés.

**Verificación.**
- Sujeto: clases de `..db.repo..`, excluidos los paquetes exentos.
- Condición: ninguna depende de clases de `..service..` ni `..controller..`.
- Mensaje: «el repositorio es capa de datos: no conoce servicios ni controladores».

**Cumplimiento.** ✅ CUMPLE.

### C12 — Un servicio no depende de un controlador

**Decisión.** La dependencia es Controller → Service, nunca Service → Controller.

**Verificación.**
- Sujeto: clases de `..service..`, excluidos los paquetes exentos.
- Condición: ninguna depende de clases de `..controller..`.
- Mensaje: «la dependencia es Controller→Service, nunca Service→Controller».

**Cumplimiento.** ✅ CUMPLE.

### C13 — Las entidades de dominio son POJOs (no dependen de service/controller)

**Decisión.** Las entidades de `..db..` (excluyendo `..db.repo..`) no llevan lógica de negocio: esa vive en el servicio.

**Verificación.**
- Sujeto: clases de `..db..`, excluidas las de `..db.repo..` y los paquetes exentos.
- Condición: ninguna depende de clases de `..service..` ni `..controller..`.
- Mensaje: «las entidades de dominio son POJOs; la lógica de negocio vive en el servicio».

**Cumplimiento.** ⚠️ Previsiblemente CUMPLE (las entidades las genera Axelor desde los XML de `domains/`); si alguna entidad con `extra-code` referencia un servicio, pasar a ❌.

### C14 — `Beans.get(...)` prohibido en controladores y `*ServiceImpl`

**Contexto.** El service-locator `com.axelor.inject.Beans` oculta dependencias. En esas capas se usa inyección / `ModelServiceFactory`.

**Decisión.** Ni los controladores ni las implementaciones de servicio usan `com.axelor.inject.Beans`.

**Verificación.**
- Sujeto: clases de `..controller..` y `..service.impl..`, excluidos los paquetes exentos. `base/util` queda deliberadamente **fuera** del sujeto: allí algún `Beans.get` de infraestructura puede ser legítimo.
- Condición: ninguna depende de la clase `com.axelor.inject.Beans`.
- Mensaje: «Beans.get es service-locator; se usa inyección / ModelServiceFactory».

**Cumplimiento.** ❌ INCUMPLE (test congelado): `firmas.service.impl.TareaFirmaServiceImpl` usa `Beans.get(...)` (instanciación dinámica de un `TareaFirmaNotifier`).

---

# Categoría 3 — Nomenclatura y ubicación

`ModelServiceFactory` descubre servicios por nombre/paquete exactos, así que estas reglas protegen un contrato real, no solo estilo.

### C15 — Controladores: `..controller..` ⇔ `*Controller`

**Decisión.** Toda clase del paquete `controller` se llama `<Entidad>Controller`, y toda clase llamada `*Controller` vive en un paquete `controller`.

**Verificación.** Dos comprobaciones (a/b), una por dirección:
- **C15a** — Sujeto: clases de `..controller..`, excluidos los paquetes exentos. Condición: su nombre simple termina en `Controller`.
- **C15b** — Sujeto: clases cuyo nombre simple termina en `Controller`, excluidos los paquetes exentos. Condición: residen en `..controller..`.
- Mensaje: el generado por defecto (sin mensaje propio).

**Cumplimiento.** ✅ CUMPLE.

### C16 — Impl. de servicio: `DefaultModelService` ⇒ `*ServiceImpl` en `..service.impl..`

**Contexto.** `ModelServiceFactory` instancia por reflexión `<Entidad>ServiceImpl`; un nombre o paquete distinto hace que `resolve(...)` devuelva `null` en runtime **sin error de compilación**.

**Decisión.** Toda implementación de servicio (subtipo de `DefaultModelService`) se llama `<Entidad>ServiceImpl` y vive en `service.impl`.

**Verificación.**
- Sujeto: clases asignables a `com.axelor.db.modelservice.DefaultModelService` (excluida la propia clase base y los paquetes exentos).
- Condición: su nombre simple termina en `ServiceImpl` **y** residen en `..service.impl..`.
- Mensaje: «ModelServiceFactory descubre la impl por el nombre <Entidad>ServiceImpl en service.impl».

**Cumplimiento.** ✅ CUMPLE.

### C17 — Interfaz de servicio: `ModelService` ⇒ `*Service` en `..service..`

**Decisión.** Toda interfaz de servicio (subtipo de `ModelService`) se llama `<Entidad>Service` y vive en `service`.

**Verificación.**
- Sujeto: interfaces asignables a `com.axelor.db.modelservice.ModelService` (excluida la propia interfaz base y los paquetes exentos).
- Condición: su nombre simple termina en `Service` **y** residen en `..service..`.
- Mensaje: «la interfaz de servicio se llama <Entidad>Service y vive en service».
- Nota: `..service.impl..` es subpaquete de `..service..`, así que la ubicación también es válida para una interfaz auxiliar allí; la impl. la cubre C16.

**Cumplimiento.** ✅ CUMPLE.

### C18 — Repositorios: clases en `..db.repo..` terminan en `Repository` o `Listener`

**Decisión.** En `db/repo` solo hay repositorios (`*Repository`) y, excepcionalmente, listeners (`*Listener`).

**Verificación.**
- Sujeto: clases de `..db.repo..`, excluidos los paquetes exentos.
- Condición: su nombre simple termina en `Repository` **o** en `Listener`.
- Mensaje: «en db/repo solo hay repositorios (*Repository) y, excepcionalmente, listeners (*Listener)».

**Cumplimiento.** ✅ CUMPLE.

### C19 — Módulos Guice: `AxelorModule` ⇒ `*Module` en `..module..`

**Decisión.** Todo módulo Guice (subtipo de `AxelorModule`) se llama `<Subsistema>Module` y vive en `module/`.

**Verificación.**
- Sujeto: clases asignables a `com.axelor.app.AxelorModule` (excluida la propia clase base y los paquetes exentos).
- Condición: su nombre simple termina en `Module` **y** residen en `..module..`.
- Mensaje: «los módulos Guice se llaman <Subsistema>Module y viven en module/».

**Cumplimiento.** ✅ CUMPLE.

### C20 — DTOs: `*DTO` son `record` y residen en `..service..`

**Decisión.** Los DTOs del proyecto son records de Java y viven junto a la interfaz del servicio.

**Verificación.**
- Sujeto: clases cuyo nombre simple termina en `DTO`, excluidos los paquetes exentos.
- Condición: son records de Java (comprobable por asignabilidad a `java.lang.Record`) **y** residen en `..service..`.
- Mensaje: «los DTOs del proyecto son records de Java y viven junto a la interfaz del servicio».

**Cumplimiento.** ✅ CUMPLE.

---

# Categoría 4 — Inyección de dependencias (Guice)

### C21 — Un `ModelService` nunca se inyecta con `@Inject`

**Contexto.** Los `ModelService` se resuelven con `ModelServiceFactory.resolve(Entidad.class)`, nunca con `@Inject`. Inyectarlos los registraría dos veces y rompería la factoría.

**Decisión.** Ningún campo cuyo tipo sea un `ModelService` lleva `@Inject`.

**Verificación.**
- Sujeto: **campos** (no clases) cuyo tipo es asignable a `com.axelor.db.modelservice.ModelService`.
- Condición: ninguno está anotado con `@Inject` (ni `com.google.inject.Inject` ni `jakarta.inject.Inject`).
- Vacuidad: si no existe ningún campo de ese tipo, la regla se considera **cumplida** (no debe fallar por sujeto vacío).
- Mensaje (descripción de la regla, literal): «Ningún campo de tipo ModelService debe llevar @Inject (se usa ModelServiceFactory)».

**Cumplimiento.** ✅ CUMPLE.

---

# Categoría 5 — Reglas genéricas de higiene

Solo se incluyen reglas genéricas **seguras** para este proyecto (ver la lista de descartadas al final del fichero).

### C22 — No acceder a los streams estándar (`System.out` / `System.err`)

**Decisión.** Se usa logging, no salida por consola.

**Verificación.**
- Sujeto: todas las clases del ámbito de análisis.
- Condición: ninguna accede a los streams estándar (`System.out`, `System.err`, `Throwable.printStackTrace`). Usar la regla predefinida de higiene de ArchUnit para streams estándar.
- Exenciones: no se aplican en esta regla (es global; la regla predefinida no admite recortar el sujeto).
- Mensaje: el de la regla predefinida.

**Cumplimiento.** ❌ INCUMPLE (test congelado): usos de `System.out`/`System.err`/ `printStackTrace()` repartidos por `base/infrastructure` y algún `EventManager`. Limpiar progresivamente.

---

# Reglas genéricas deliberadamente NO incluidas

Decisiones de **no adoptar** estas reglas genéricas de higiene de ArchUnit, porque **chocan con prácticas documentadas** del proyecto y generarían falsos positivos masivos:

- **Prohibir inyección por campo** — el proyecto **usa** inyección por campo (`@Inject` en campos de controladores y servicios, p.ej. `ModelServiceFactory`). Es el patrón correcto aquí, no un antipatrón.
- **Prohibir lanzar excepciones genéricas** — `controladores.md` indica relanzar errores técnicos no esperados como `RuntimeException`; la regla los prohibiría, así que contradice el patrón.

# Fuera del alcance de ArchUnit

Las reglas de `k-secure-coding` son mayormente **de comportamiento**, no estructurales, y no se pueden verificar con ArchUnit (haría falta análisis de flujo de datos). Se citan como recordatorio, sin regla automatizable:

- Mass-assignment / `AllowProperties`: qué campos puede dictar el cliente por acción.
- Asignación incondicional de campos `servidor` en `*ServiceImpl.insert/update`.
- Autorización multicentro / IDOR (cada centro solo ve su información).
- Validación de adjuntos, inyección JPQL/SQL, log injection, manejo de secretos.

Para estas, la defensa vive en revisiones con `k-secure-coding` y en los tests E2E (`/sdd-debug-with-test-e2e-desc`), no en ArchUnit.