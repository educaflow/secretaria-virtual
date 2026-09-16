---
type: implementation-task
template: expediente
---

# Tarea 04 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas
- k-validaciones
- k-datainit
- k-vistas
- k-secure-coding
- k-code-quality
- k-i18n

## Qué hay que hacer

Aplica los siete cambios del catálogo compartido `subsystem/sistemaeducativo` que el diseño describe en «Cambios en el catálogo del sistema educativo» y en el `### Paso 4`. Va **antes** del modelo y de las vistas del tipo de expediente porque `Ciclo.gradoNivel` es lo que ambos consultan.

Ficheros (todos con acción `Modificar`; se insertan **solo** los cambios indicados, conservando todo lo demás):

- `src/main/java/com/educaflow/subsystem/sistemaeducativo/domains/Nivel.xml`
- `src/main/java/com/educaflow/subsystem/sistemaeducativo/domains/Ciclo.xml`
- `src/main/java/com/educaflow/subsystem/sistemaeducativo/data-init/input-config.xml`
- `src/main/java/com/educaflow/subsystem/sistemaeducativo/data-init/input/Nivel.xml`
- `src/main/java/com/educaflow/subsystem/sistemaeducativo/data-init/input/Ciclo.xml`
- `src/main/java/com/educaflow/subsystem/sistemaeducativo/views/Main-Nivel.xml`
- `src/main/java/com/educaflow/subsystem/sistemaeducativo/views/Ref-Ciclo.xml`

La especificación del diseño es **contrato fijo** y la **superficie es cerrada**: **MUST NOT** crearse ningún campo, clase, método, columna, fila de data-init ni cambio de vista que el diseño no declare aquí.

## Filas de la tabla «## 6. Ficheros a crear o modificar» del diseño (verbatim)

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/subsystem/sistemaeducativo/domains/Nivel.xml` | Modificar | `k-sistemas` | Campo `nombreCorto` (§4, «Cambios en el catálogo») |
| `src/main/java/com/educaflow/subsystem/sistemaeducativo/domains/Ciclo.xml` | Modificar | `k-sistemas`, `k-validaciones` | Campo derivado `gradoNivel` (§4, «Cambios en el catálogo») |
| `src/main/java/com/educaflow/subsystem/sistemaeducativo/data-init/input-config.xml` | Modificar | `k-datainit` | Bind de `@nombreCorto` en el `<input>` de `Nivel.xml` |
| `src/main/java/com/educaflow/subsystem/sistemaeducativo/data-init/input/Nivel.xml` | Modificar | `k-datainit` | `nombreCorto` de `GB`, `GM` y `GS` |
| `src/main/java/com/educaflow/subsystem/sistemaeducativo/data-init/input/Ciclo.xml` | Modificar | `k-datainit` | Alta del curso de especialización `IABD` |
| `src/main/java/com/educaflow/subsystem/sistemaeducativo/views/Main-Nivel.xml` | Modificar | `k-vistas` | Mostrar `nombreCorto` en el mantenimiento de niveles |
| `src/main/java/com/educaflow/subsystem/sistemaeducativo/views/Ref-Ciclo.xml` | Modificar | `k-vistas` | Columnas `grado` y `nivel` en `subsysSistemaEducativo.Ref@Ciclo-grid` |

## `### Paso 4 — Catálogo del sistema educativo` del diseño (verbatim)

### Paso 4 — Catálogo del sistema educativo

Aplica los siete cambios de §4 «Cambios en el catálogo del sistema educativo» sobre `subsystem/sistemaeducativo`: `domains/Nivel.xml`, `domains/Ciclo.xml`, `data-init/input-config.xml`, `data-init/input/Nivel.xml`, `data-init/input/Ciclo.xml`, `views/Main-Nivel.xml` y `views/Ref-Ciclo.xml`. Va **antes** del modelo y de las vistas del tipo porque `Ciclo.gradoNivel` es lo que ambos consultan.

**CRITICAL — este paso toca un catálogo compartido que YA tiene tests E2E persistidos.** `src/test/e2e/subsystem/sistemaeducativo/` contiene **15** tests (`t-001`…`t-015`) que pilotan justo las pantallas y los datos que este paso modifica: el mantenimiento de niveles (`Main-Nivel.xml`, que gana `nombreCorto` en el grid y en el form), el selector de ciclo (`Ref-Ciclo.xml`, que gana las columnas `grado` y `nivel`) y el catálogo de ciclos y niveles del data-init. **Los ejecuta Playwright contra la aplicación arrancada, así que `./run.sh` NO los ejecuta**: hay que lanzarlos aparte.

```
npx playwright test src/test/e2e/subsystem/sistemaeducativo
```

- Se lanzan con la aplicación **arrancada** (el `baseURL` de `playwright.config.ts` es `http://localhost:8080`).
- Los **15 MUST** quedar en verde. Ninguno afirma en su código el número total de ciclos ni el número de columnas del grid de niveles —el único `toHaveCount` sobre columnas es el de `t-012`, y va sobre `Main@Ciclo-grid`, que este paso **no** toca—, así que lo esperado es que pasen sin cambios. Si alguno falla, el fallo es una **regresión de este paso** y se arregla en el código de la aplicación.
- **MUST NOT** editarse ningún fichero de `src/test/e2e/**` para hacerlos pasar: son snapshots «as-tested» de otra iniciativa y llevan escrito «NO editar a mano». Lo que sí queda desactualizado es **prosa** de sus `.desc.md`, y de eso habla §14 nota 22.

**Verificación:** compila; el cuerpo de `Ciclo.gradoNivel` lee los **campos** `nivel` y `grado` (nunca sus getters); el grid `subsysSistemaEducativo.Ref@Ciclo-grid` tiene cuatro columnas; tras arrancar, el catálogo tiene el ciclo `IABD` y los tres niveles con su nombre corto; y `npx playwright test src/test/e2e/subsystem/sistemaeducativo` termina con los **15 tests en verde**.


## `### Cambios en el catálogo del sistema educativo (subsystem/sistemaeducativo)` de §4 del diseño (verbatim)

### Cambios en el catálogo del sistema educativo (`subsystem/sistemaeducativo`)

Son del catálogo, no del trámite, y cualquier otro trámite que imprima un ciclo los reutiliza:

| Fichero | Cambio |
|---|---|
| `domains/Nivel.xml` | Campo nuevo `<string name="nombreCorto" title="Nombre corto"/>` (no `required`: la columna ya existe con filas). Es el texto que se imprime en la línea «en el Ciclo Formativo de Grado ____»: «Básico», «Medio», «Superior» |
| `domains/Ciclo.xml` | Campo derivado nuevo `<string name="gradoNivel" transient="true" title="Grado o nivel">` con cuerpo Java: si el ciclo **tiene** nivel devuelve su `nombreCorto` (o su `name` si el nombre corto está vacío), y si **no** tiene nivel devuelve el `name` de su grado. **CRITICAL — el cuerpo MUST leer los CAMPOS `nivel` y `grado` directamente, nunca `getNivel()`/`getGrado()`**: `Mapper.findComputeDependencies` recorre el bytecode y solo registra como dependencia las instrucciones `GETFIELD`, igual que documenta `Grado.admiteNivel` en `subsystem/sistemaeducativo/domains/Grado.xml` |
| `data-init/input-config.xml` | Añadir `<bind node="@nombreCorto" to="nombreCorto"/>` al `<input>` de `Nivel.xml` |
| `data-init/input/Nivel.xml` | `nombreCorto="Básico"` en `GB`, `"Medio"` en `GM`, `"Superior"` en `GS` |
| `data-init/input/Ciclo.xml` | Fila nueva `<ciclo code="IABD" name="Inteligencia Artificial y Big Data" familiaProfesional="190" grado="E"/>` (sin `nivel`), que exige la especificación |
| `views/Main-Nivel.xml` | Mostrar `nombreCorto` en el grid y en el form de mantenimiento de niveles, para que se pueda dar de alta |
| `views/Ref-Ciclo.xml` | Añadir las columnas `grado` y `nivel` al grid `subsysSistemaEducativo.Ref@Ciclo-grid`, para que la ventana de búsqueda del ciclo permita filtrar por ellas (RUI-DATOS_SOLICITUD-CREADOR-008). La fila de filtros por columna se pinta sola en el popup; el autocompletado al teclear sigue buscando solo por `name` |

Con eso, «Desarrollo de Aplicaciones Web» (grado `D`, nivel `GS`) imprime «Superior», «Sistemas Microinformáticos y Redes» (nivel `GM`) imprime «Medio» e «Inteligencia Artificial y Big Data» (grado `E`, sin nivel) imprime «Curso de especialización», que es el `name` de su grado.

