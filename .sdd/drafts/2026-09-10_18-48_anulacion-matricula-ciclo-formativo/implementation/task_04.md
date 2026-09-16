---
type: implementation-task
template: expediente
---

# Tarea 04 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas
- k-validaciones
- k-secure-coding
- k-datainit
- k-vistas
- k-code-quality

Aplica los **ocho** cambios del catálogo del sistema educativo (`subsystem/sistemaeducativo`) que el diseño declara. Son del **catálogo**, no del trámite, y van **antes** del modelo y de las vistas del tipo porque `Ciclo.gradoNivel` es lo que ambos consultan.

**Nota del descomponedor (decisión documentada):** este bloque no pertenece al inventario estándar de un tipo de expediente, pero la tabla `## 6` del diseño lo lista y el `### Paso 4` lo ordena en esta posición (después de `CreateFilesTask` y antes del `domains.xml`); por eso es una tarea propia aquí, con sus ocho ficheros agrupados, sin alterar el orden relativo del resto de bloques.

La especificación del cambio es **contrato fijo** y la **superficie es cerrada**: **MUST NOT** crearse ningún método, clase, campo, columna, vista ni fila de datos que el diseño no liste.

## Filas de la tabla `## 6. Ficheros a crear o modificar` del diseño

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/subsystem/sistemaeducativo/domains/Nivel.xml` | Modificar | `k-sistemas` | Campo `nombreCorto` (§4, «Cambios en el catálogo») |
| `src/main/java/com/educaflow/subsystem/sistemaeducativo/service/impl/NivelServiceImpl.java` | Modificar | `k-sistemas`, `k-secure-coding` | Añadir `nombreCorto` a la whitelist de `allowPropertiesInsert`/`allowPropertiesUpdate` (el `Map.of` de `allowPropertiesEditables()`). Sin esto el campo se descarta en silencio al guardar y el mantenimiento de niveles no puede darle valor (§4, «Cambios en el catálogo», y Paso 4) |
| `src/main/java/com/educaflow/subsystem/sistemaeducativo/domains/Ciclo.xml` | Modificar | `k-sistemas`, `k-validaciones` | Campo derivado `gradoNivel` (§4, «Cambios en el catálogo») |
| `src/main/java/com/educaflow/subsystem/sistemaeducativo/data-init/input-config.xml` | Modificar | `k-datainit` | Bind de `@nombreCorto` en el `<input>` de `Nivel.xml` |
| `src/main/java/com/educaflow/subsystem/sistemaeducativo/data-init/input/Nivel.xml` | Modificar | `k-datainit` | `nombreCorto` de `GB`, `GM` y `GS` |
| `src/main/java/com/educaflow/subsystem/sistemaeducativo/data-init/input/Ciclo.xml` | Modificar | `k-datainit` | Alta **solo** del curso de especialización `IABD`, el único que exige la especificación (§4, «Cambios en el catálogo»). Los ocho ciclos que necesita el reparto de parejas (alumno, ciclo) de `test-e2e-desc.md` **NO** van aquí: son configuración del entorno de pruebas (§4 y §14 nota 23) |
| `src/main/java/com/educaflow/subsystem/sistemaeducativo/views/Main-Nivel.xml` | Modificar | `k-vistas` | Mostrar `nombreCorto` en el mantenimiento de niveles |
| `src/main/java/com/educaflow/subsystem/sistemaeducativo/views/Ref-Ciclo.xml` | Modificar | `k-vistas` | Columnas `grado` y `nivel` en `subsysSistemaEducativo.Ref@Ciclo-grid` |

## Paso del diseño (verbatim)

### Paso 4 — Catálogo del sistema educativo

Aplica los **ocho** cambios de §4 «Cambios en el catálogo del sistema educativo» sobre `subsystem/sistemaeducativo`: `domains/Nivel.xml`, `domains/Ciclo.xml`, `data-init/input-config.xml`, `data-init/input/Nivel.xml`, `data-init/input/Ciclo.xml`, `views/Main-Nivel.xml`, `views/Ref-Ciclo.xml` y `service/impl/NivelServiceImpl.java`. Va **antes** del modelo y de las vistas del tipo porque `Ciclo.gradoNivel` es lo que ambos consultan.

**CRITICAL — el campo nuevo `nombreCorto` NO basta con declararlo y pintarlo: hay que abrirle la whitelist del `ModelService`.** `NivelServiceImpl.allowPropertiesEditables()` —de donde salen `allowPropertiesInsert()` y `allowPropertiesUpdate()`— devuelve hoy `AllowProperties.createAllowProperties(Map.of("code", …, "name", …, "grado", …))`, una whitelist **cerrada**: lo que no está en ella se descarta **en silencio** al guardar, sin error ni aviso. Si `nombreCorto` no entra ahí, el campo se pinta en `Main-Nivel.xml`, el usuario lo escribe, el formulario guarda «bien»… y el valor no llega nunca a la fila. **MUST** añadirse `"nombreCorto", Map.of()` a ese `Map.of`, dejando intacto el resto del servicio (sus `validateInsert`/`validateUpdate` no cambian: el campo **no** es obligatorio, §4). El fallo **no** lo destapa comprobar los nombres cortos que siembra el data-init, porque el data-init **no** pasa por el `ModelService`.

**CRITICAL — este paso toca un catálogo compartido que YA tiene tests E2E persistidos.** `src/test/e2e/subsystem/sistemaeducativo/` contiene **16** ficheros `.spec.ts` —`t-001`…`t-015` **más** `crear-ley-educativa.spec.ts`, que el comando de abajo también ejecuta— y pilotan justo las pantallas y los datos que este paso modifica: el mantenimiento de niveles (`Main-Nivel.xml`, que gana `nombreCorto` en el grid y en el form), el selector de ciclo (`Ref-Ciclo.xml`, que gana las columnas `grado` y `nivel`) y el catálogo de ciclos y niveles del data-init. **Los ejecuta Playwright contra la aplicación arrancada, así que `./run.sh` NO los ejecuta**: hay que lanzarlos aparte.

```
npx playwright test src/test/e2e/subsystem/sistemaeducativo
```

- Se lanzan con la aplicación **arrancada** (el `baseURL` de `playwright.config.ts` es `http://localhost:8080`).
- Los **16 MUST** quedar en verde. Este paso da de alta **un** ciclo nuevo (`IABD`; los ocho del reparto de tests son configuración del entorno, §4 y §14 nota 23), así que lo primero que hay que comprobar es que ninguno de los 16 cuenta filas: ninguno afirma en su código el número total de ciclos ni el número de columnas del grid de niveles —el único `toHaveCount` sobre columnas es el de `t-012`, y va sobre `Main@Ciclo-grid`, que este paso **no** toca—, así que lo esperado es que pasen sin cambios. Si alguno falla, el fallo es una **regresión de este paso** y se arregla en el código de la aplicación.
- **MUST NOT** editarse ningún fichero de `src/test/e2e/**` para hacerlos pasar: son snapshots «as-tested» de otra iniciativa y llevan escrito «NO editar a mano». Lo que sí queda desactualizado es **prosa** de sus `.desc.md`, y de eso habla §14 nota 22.

**Verificación:** compila; el cuerpo de `Ciclo.gradoNivel` lee los **campos** `nivel` y `grado` (nunca sus getters); `NivelServiceImpl.allowPropertiesEditables()` lista **cuatro** propiedades (`code`, `name`, `nombreCorto`, `grado`) y, con la aplicación arrancada, **dar de alta un Nivel nuevo con nombre corto desde «Sistema educativo → Niveles» (`Main-Nivel.xml`) y luego editárselo deja el valor persistido** tras recargar la ficha —es la única comprobación que ve el agujero de la whitelist, porque un descarte de propiedad no da error—; el grid `subsysSistemaEducativo.Ref@Ciclo-grid` tiene cuatro columnas; tras arrancar, el catálogo tiene **ocho** ciclos —los siete de siempre más el curso de especialización `IABD`— y los tres niveles con su nombre corto; y `npx playwright test src/test/e2e/subsystem/sistemaeducativo` termina con los **16 ficheros `.spec.ts` en verde**. Los ocho ciclos del reparto de tests **MUST NOT** aparecer en `data-init/input/Ciclo.xml`: se dan de alta por la pantalla de mantenimiento antes de lanzar la suite de este trámite (§4 y §14 nota 23).


## `## 4. Modelo` — «Cambios en el catálogo del sistema educativo» (verbatim)

### Cambios en el catálogo del sistema educativo (`subsystem/sistemaeducativo`)

Son del catálogo, no del trámite, y cualquier otro trámite que imprima un ciclo los reutiliza:

| Fichero | Cambio |
|---|---|
| `domains/Nivel.xml` | Campo nuevo `<string name="nombreCorto" title="Nombre corto"/>` (no `required`: es un campo **nuevo** sobre una tabla que ya tiene filas, que quedarían en violación de la restricción). Es el texto que se imprime en la línea «en el Ciclo Formativo de Grado ____»: «Básico», «Medio», «Superior» |
| `domains/Ciclo.xml` | Campo derivado nuevo `<string name="gradoNivel" transient="true" title="Grado o nivel">` con cuerpo Java: si el ciclo **tiene** nivel devuelve su `nombreCorto` (o su `name` si el nombre corto está vacío), y si **no** tiene nivel devuelve el `name` de su grado. **CRITICAL — el cuerpo MUST leer los CAMPOS `nivel` y `grado` directamente, nunca `getNivel()`/`getGrado()`**: `Mapper.findComputeDependencies` recorre el bytecode y solo registra como dependencia las instrucciones `GETFIELD`, igual que documenta `Grado.admiteNivel` en `subsystem/sistemaeducativo/domains/Grado.xml` |
| `data-init/input-config.xml` | Añadir `<bind node="@nombreCorto" to="nombreCorto"/>` al `<input>` de `Nivel.xml` |
| `data-init/input/Nivel.xml` | `nombreCorto="Básico"` en `GB`, `"Medio"` en `GM`, `"Superior"` en `GS` |
| `data-init/input/Ciclo.xml` | **Una** fila nueva: `<ciclo code="IABD" name="Inteligencia Artificial y Big Data" familiaProfesional="190" grado="E"/>` (sin `nivel`), que es la única que exige la especificación en «Datos iniciales». **MUST NOT** darse de alta aquí ningún otro ciclo: el mantenimiento del catálogo está **fuera del alcance** de esta iniciativa (ver debajo de la tabla) |
| `views/Main-Nivel.xml` | Mostrar `nombreCorto` en el grid y en el form de mantenimiento de niveles, para que se pueda dar de alta |
| `service/impl/NivelServiceImpl.java` | Añadir `"nombreCorto"` a la whitelist de `allowPropertiesEditables()`, el método privado del que salen `allowPropertiesInsert()` y `allowPropertiesUpdate()`. Hoy es `AllowProperties.createAllowProperties(Map.of("code", …, "name", …, "grado", …))`, es decir una whitelist **cerrada**: toda propiedad que no esté en ella se **descarta en silencio** al guardar. **CRITICAL — sin esta fila, `nombreCorto` no se puede dar de alta ni editar NUNCA desde `Main-Nivel.xml`**, aunque el campo exista en el modelo y se pinte en la pantalla. No lo destapa sembrar los tres nombres cortos en el data-init, porque el data-init **no** pasa por el `ModelService` |
| `views/Ref-Ciclo.xml` | Añadir las columnas `grado` y `nivel` al grid `subsysSistemaEducativo.Ref@Ciclo-grid`, para que la ventana de búsqueda del ciclo permita filtrar por ellas (RUI-DATOS_SOLICITUD-CREADOR-008). La fila de filtros por columna se pinta sola en el popup; el autocompletado al teclear sigue buscando solo por `name` |

**Los ocho ciclos que necesita la suite E2E NO entran en el catálogo maestro: son configuración del entorno de pruebas.** El catálogo de demo tiene hoy siete ciclos y con ellos no caben las parejas (alumno, ciclo) disjuntas que los 56 tests E2E necesitan para no pisarse entre sí bajo la regla `SinOtraSolicitudEnCursoParaElMismoCiclo` (§10.0). Esa es una necesidad **de la suite de pruebas**, y la especificación declara **fuera de alcance** «el mantenimiento del catálogo de ciclos … común a todos los centros y no forma parte de este trámite», así que **MUST NOT** resolverse dando de alta filas en `subsystem/sistemaeducativo/data-init/input/Ciclo.xml`, que es dato maestro de producción compartido por todos los trámites y todos los centros.

Se resuelven como **configuración del entorno de pruebas**, exactamente igual que los certificados custodiados de §14 nota 11: se dan de alta **por la propia aplicación**, en la pantalla de mantenimiento «Sistema educativo → Ciclos» (`subsysSistemaEducativo.Main@Ciclo-action`, visible para `admins`), antes de lanzar la suite. **No** son un fichero de este proyecto y **no** figuran en la tabla de ficheros de §6. La lista exacta, con su familia profesional, su grado y su nivel, vive en la sección «Aislamiento entre tests» de `test-e2e-desc.md`, que es su única fuente de verdad; ninguno es de la familia `190` «Informática y Comunicaciones» ni de grado `E`, para no alterar lo que ven la ventana de búsqueda de T-002 (familia «Informática y Comunicaciones» + nivel `GS`), el autocompletado de T-047 (ningún nombre nuevo contiene «Microinform») ni el filtro por grado de T-048 (sigue habiendo un único curso de especialización).

**MUST NOT** convertirse esa configuración en un alta de catálogo «porque es más cómodo»: el día en que el catálogo de ciclos se mantenga de verdad (una iniciativa propia, la que la especificación deja fuera de alcance), esos ocho ciclos entrarán por ahí y esta configuración de entorno dejará de hacer falta sin que haya que quitar nada del data-init.

Con `gradoNivel` y los nombres cortos, «Desarrollo de Aplicaciones Web» (grado `D`, nivel `GS`) imprime «Superior», «Sistemas Microinformáticos y Redes» (nivel `GM`) imprime «Medio» e «Inteligencia Artificial y Big Data» (grado `E`, sin nivel) imprime «Curso de especialización», que es el `name` de su grado.

