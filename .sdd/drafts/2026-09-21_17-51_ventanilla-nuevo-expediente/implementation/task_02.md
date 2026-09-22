---
type: implementation-task
template: system
---

# Tarea 02 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas
- k-secure-coding
- k-code-quality

Implementa el repositorio del sistema `system/ventanilla`.

## Fila de la tabla «Ficheros a crear o modificar» del diseño

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/system/ventanilla/db/repo/VentanillaRepository.java` | Crear | k-sistemas (db) | Repositorio del sistema con la única consulta: los trámites del catálogo que se pueden clasificar |

> **Nota para `/sdd-implementer`:** los XML de `domains/`, `views/` y `menus.xml` ya están materializados en la carpeta `design/`. **MUST NOT** modificarlos, reescribirlos ni regenerarlos: se **copian verbatim** a su ubicación final (`menus.xml` se fusiona en el `menus.xml` único del proyecto). El código Java es lo único que se implementa a partir de las firmas y comentarios del diseño.
>
> **Los comentarios de este `design.md` NO se transcriben al código.** Son material de `.sdd/`: explican el diseño, no acompañan al código. **MUST NOT** aparecer en los `.java` los comentarios `//` de estos pasos, ni ningún identificador de la spec o del diseño (`V-`, `R-`, `U-`, `CC-`, `ESC-`, `HU-`, `VAL-`, `RN-`, `RUI-`), ni la justificación de una decisión «para que no se pierda» (`k-code-quality/comentarios.md`): lo que el código no revela por sí solo se dice con el nombre del método o de la variable, y lo demás vive aquí y en `decisiones.md`.

## Diseño (verbatim)

### Paso 4 — Repositorios

```java
// Clase: com.educaflow.system.ventanilla.db.repo.VentanillaRepository extends JpaRepository<Tramite>
//   Constructor protegido sin argumentos: super(Tramite.class).

public List<Tramite> findTramitesEvaluables();
//   Los trámites del catálogo que sus consumidores SABEN tratar: los que tienen tipo de trámite y
//   tipo de expediente por defecto. Los dos m2o son nullable en `Tramite.xml`, y
//   PerfilesUsuarioServiceImpl hace Objects.requireNonNull sobre los dos, así que una sola fila mal
//   dada del catálogo convertiría el asistente entero en un 500 ya en el onNew del paso 1, para
//   TODOS los usuarios; el tipo de trámite lo necesita además el `groupBy="tipoTramite.name"` del
//   grid del paso 2. El filtro es la consulta que justifica el
//   método: descarta las filas con cualquiera de los dos a null.
//   Es el ÚNICO dueño del predicado «el sistema sabe juzgar este trámite», y lo consultan las DOS
//   puertas por las que entra un `Tramite`: la lista que la ventanilla construye ella misma (lo
//   llaman fireActionRule_AsignarTramitesDisponibles y getCentrosCandidatos, que pasan el resultado
//   al cálculo de candidatos getTramitesCandidatos) y el `tramite` que llega del cliente (V-009, en
//   validateTramiteEvaluable, al que invocan validateRecalcular y validateTriggerInitialEvent). MUST NOT
//   reescribirse ese predicado en el servicio.
//   NO filtra por perfiles de inicio (eso es regla de negocio: getTramitesCandidatos) ni ordena
//   (el orden lo fija el `orderBy` del grid).
```

El sistema no tiene entidades persistentes propias, pero sí una consulta: la lectura de los trámites evaluables del catálogo. Va aquí y no inline en el servicio por las dos reglas de `k-sistemas`/`k-code-quality`: **las consultas JPA viven en el repositorio, nunca en la capa de servicio**, y **un sistema no llama al repositorio de otro subsistema**. Es exactamente lo que ya hace el sistema hermano `system/gestioncentro/db/repo/GestionCentroRepository`, un repositorio del propio sistema sobre una entidad de otro subsistema con un método nombrado; el servicio inyecta `VentanillaRepository` y no `subsystem.expedientes.db.repo.TramiteRepository`.

**Verificar al final:** `./gradlew compileJava` compila; `grep -nE "^\s*(public|protected)" src/main/java/com/educaflow/system/ventanilla/db/repo/VentanillaRepository.java` devuelve exactamente dos líneas —el constructor protegido sin argumentos y `findTramitesEvaluables()`—, es decir `findTramitesEvaluables()` es el único método del repositorio.

---
