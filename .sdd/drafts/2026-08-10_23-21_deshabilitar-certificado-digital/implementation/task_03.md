---
type: implementation-task
---

# Tarea 03 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas
- k-validaciones
- k-secure-coding
- k-code-quality

## Fichero de esta tarea (de la tabla "Ficheros a crear o modificar" del diseño)

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/subsystem/criptografia/service/impl/CertificadoDigitalServiceImpl.java` | Modificar | k-sistemas (servicios.md), k-validaciones (validaciones.md), k-secure-coding | V-CertificadoDigital-001: en `getAlmacenClaveByDni` una entrada deshabilitada se trata igual que una inexistente (mismo error) |

## Texto del diseño (verbatim)

### Paso 2 — Servicio: entrada deshabilitada = entrada inexistente

Clase: `com.educaflow.subsystem.criptografia.service.impl.CertificadoDigitalServiceImpl` — **Acción: Modificar**. Solo cambia el método siguiente; **el resto de la clase se conserva** (incluidos `validateInsert`/`validateUpdate`/`validateCertificado`, `remove`, `validateGetAlmacenClaveByDni` y la ausencia de sobrescritura de `insert`/`update`).

```java
// Método (la firma NO cambia; cambia el criterio de "entrada inexistente" de su cuerpo):
public AlmacenClave getAlmacenClaveByDni(String dni);
//   Aplica:
//     - V-CertificadoDigital-001 (Origen spec: VAL-CertificadoDigital-001): tras recuperar la
//       entrada con el finder existente findByDni(dni), la condición que hoy detecta
//       "no existe entrada" se amplía para tratar igual una entrada recuperada cuyo
//       getEnabled() devuelva FALSE. Hecho verificado (Paso 1): el getter generado por AOP
//       colapsa NULL a Boolean.FALSE, y el default="true" del dominio NO rellena las filas
//       preexistentes; gracias al backfill del Paso 1 ninguna fila legacy queda a NULL, por lo
//       que "getEnabled() == FALSE => tratar como inexistente" es correcto — ver Notas.
//       En ambos casos se lanza LA MISMA
//       RuntimeException que hoy lanza el caso "no existe", con el mismo mensaje, que debe
//       transmitir: que no existe certificado para el DNI recibido (sin revelar que la entrada
//       existe pero está deshabilitada — exigencia de la spec y de design-guidelines.md).
//     - El resto del método (validateGetAlmacenClaveByDni como primera línea y el switch por
//       TipoUbicacionCertificado que construye el AlmacenClave) se conserva tal cual.
```

Con esto `AlmacenClaveResolver.getByDNI` (que delega en este método) queda cubierto sin tocarlo; los demás métodos de `AlmacenClaveResolver` (`getDirector`, `getSecretario`, `getDummy`) **no se tocan** (design-guidelines.md).

**Verificación:** `./gradlew compileJava` compila; `grep -n "enabled" src/main/java/com/educaflow/subsystem/criptografia/service/impl/CertificadoDigitalServiceImpl.java` muestra la condición dentro de `getAlmacenClaveByDni` y en ningún otro método.

### Trazabilidad Origen spec → V/R/U → ubicación (fila que aplica a este fichero)

| V | Origen spec | Ubicación | Lógica |
|---|---|---|---|
| V-CertificadoDigital-001 | VAL-CertificadoDigital-001 | `CertificadoDigitalServiceImpl.getAlmacenClaveByDni` (Paso 2) | La entrada del DNI buscado debe estar habilitada: una entrada con `enabled` a FALSE se trata exactamente igual que la inexistencia de entrada — misma excepción y mismo mensaje («no existe certificado para ese DNI»). Capa servidor: es la única capa (la operación no tiene vista; la invocan los procesos de firma/sellado vía `AlmacenClaveResolver.getByDNI`). |

### Frontera de confianza — AllowProperties (verbatim del diseño, Nota 9)

9. **Sin sección «Frontera de confianza — AllowProperties por acción».** El diseño no declara ningún `@CallMethod` (no existe `CertificadoDigitalController`; el alta/edición/borrado van por el flujo genérico `remote-validationSave-action`/`save`/`delete` de `DefaultModelController` + `ModelService`), y `design-contract.md` §8.3 manda omitir la sección en ese caso. La decisión de seguridad del flujo genérico queda cerrada aquí: se mantiene el `createAllowAllProperties` heredado de `DefaultModelService` (no se sobrescribe — mínima intrusión), admisible según `k-secure-coding` §3.2 porque los 9 campos persistentes de `CertificadoDigital` son todos de origen **cliente**, aparecen en las líneas `Input AllowProperties` tanto de `Crear` como de `Modificar` de `entity-CertificadoDigital.md`, y no hay ningún campo `servidor` ni inmutable — allow-all equivale a la whitelist completa.

### Notas y supuestos que aplican (verbatim del diseño)

1. **Dónde vive V-CertificadoDigital-001 (cuerpo de `getAlmacenClaveByDni`, no `validateGetAlmacenClaveByDni`).** `design-guidelines.md` exige que una entrada deshabilitada produzca **el mismo error** que la inexistencia de entrada. La inexistencia se detecta hoy en el cuerpo del método (RuntimeException «No existe certificado para el DNI: …»), no en `validateGetAlmacenClaveByDni` (que devuelve `BusinessMessages` y hoy está vacío). Poner la comprobación en `validate*` produciría un error de tipo distinto (BusinessException) al del caso inexistente, violando la guía; por eso la condición de inexistencia del cuerpo se **amplía** para incluir la entrada deshabilitada. `validateGetAlmacenClaveByDni` se conserva sin cambios.

2. **Por qué no un finder «findByDni habilitado».** Se valoró añadir un finder que filtre `enabled = true` (la entrada deshabilitada ni se recuperaría). Se descarta porque `validateCertificado` (unicidad de DNI) debe seguir viendo también las entradas deshabilitadas — habría que mantener dos finders en paralelo — y porque el criterio «deshabilitada = inexistente» se decide de todas formas en el cuerpo de `getAlmacenClaveByDni` para lanzar **el mismo error** que el caso inexistente (Nota 1): el filtro en la consulta no ahorraría nada y duplicaría el punto de decisión (mínima intrusión). El grid **tampoco** filtra por `enabled`: las entradas deshabilitadas siguen listándose (con la columna «Habilitado» sin marcar) y pueden abrirse, editarse y borrarse, como exigen ESC-005 y ESC-006.

3. **Semántica de NULL: el mecanismo real es getter + backfill.** Hecho verificado (Paso 1): el getter generado por AOP colapsa `enabled` a NULL en `Boolean.FALSE`, así que a nivel de entidad **no existe** un tercer estado observable — un NULL persistido se **leería como deshabilitada**, no como habilitada. La semántica normativa «NULL/no indicado cuenta como habilitada» (RN-CertificadoDigital-001 y «Fuera de alcance» del spec) se garantiza por dos vías: para las entidades **nuevas**, el `default="true"` del dominio inicializa el atributo a `TRUE`; para las filas **preexistentes**, el backfill del Paso 1 pone a `TRUE` la columna donde estaba NULL. Tras el backfill ninguna fila queda a NULL, y la condición del Paso 2 («`enabled` a FALSE vía getter ⇒ tratar como inexistente») es correcta. Caso residual: un cliente REST que enviara **explícitamente** `"enabled": null` persistiría NULL y esa entrada se leería como deshabilitada; ninguna interfaz del spec lo produce (la casilla del form siempre envía true/false) y RN-CertificadoDigital-001 cubre el campo **ausente** (default), no el null explícito — se acepta como comportamiento fuera de las interfaces contempladas.
