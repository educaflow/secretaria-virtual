---
type: implementation-task
template: expediente
---

# Tarea 02 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- `k-sistemas`
- `k-secure-coding`
- `k-code-quality`

Modifica `com.educaflow.subsystem.firmas.util.SituacionFirmaBuilder` para que pueda calcular la situación de firma a partir de un DNI.

## Nota del descomponedor (decisiones de esta descomposición)

Esta iniciativa es un **delta** sobre una versión existente y su tabla §6 lista, además de los ficheros del tipo de expediente, varios ficheros de **subsistema** (las «piezas compartidas» de §9.1 y las reglas de §10.1) que no corresponden a ningún bloque de la tabla de tareas del contrato. Decisión tomada: **una tarea por fichero de subsistema**, en el orden de los pasos del diseño, y **todas antes** de la tarea de la fase `RECEPCION`, que es la que las consume (inyecta el servicio y usa las reglas). Así cada fila de §6 queda cubierta por exactamente una tarea.

## Fichero de esta tarea — fila verbatim de la tabla §6 del `design.md`

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/subsystem/firmas/util/SituacionFirmaBuilder.java` | Modificar | `k-sistemas`, `k-secure-coding` | Especificado en §9.1 — nuevo `buildByDni(String)`; `build(User)` pasa a delegar en él; `enmascararDni` pasa a package-private |

## Paso del diseño (verbatim del `design.md` §7)

### Paso 2 — `SituacionFirmaBuilder.java`

- Fichero: `src/main/java/com/educaflow/subsystem/firmas/util/SituacionFirmaBuilder.java`; FQCN `com.educaflow.subsystem.firmas.util.SituacionFirmaBuilder`.
- Especificación quirúrgica: **§9.1**, apartado «`SituacionFirmaBuilder`». **MUST NOT** duplicarla aquí.
- **Verificación:** compila; existe `public static SituacionFirma buildByDni(String dni)`; `build(User)` delega en él y no duplica la lógica; ningún método propaga excepción; `enmascararDni` es visible desde el paquete (`FirmaEnServidorHelper` la reutiliza) y el DNI solo aparece enmascarado en el log.

## Especificación quirúrgica (verbatim del `design.md` §9.1)

### 9.1 Piezas compartidas del subsistema que usan los `trigger*`

Son el **mecanismo reutilizable**: esta iniciativa las escribe (o las extrae de donde ya estaban) una sola vez y quedan disponibles para **cualquier** tipo de expediente que quiera firmar en el servidor su documento de entrada. Ninguna conoce este trámite.

Dos de ellas son **de-duplicación**, no funcionalidad nueva: `FirmaEnServidorHelper` extrae lo que hoy vive dentro de `TareaFirmaServiceImpl`, y `TareaFirmaServiceImpl` pasa a delegar en él. Sin ese par, la firma en servidor del documento de entrada y la regla del DSL serían la segunda y la tercera copia del mismo criterio y de los mismos literales (§14).


#### `SituacionFirmaBuilder` — `com.educaflow.subsystem.firmas.util.SituacionFirmaBuilder`

Se **generaliza** para poder calcular la situación a partir de un DNI y no solo de un `User`.

- **Nuevo** `public static SituacionFirma buildByDni(String dni)`: contiene el cuerpo que hoy tiene `build(User)` a partir del DNI — `SIN_DNI` si `DniUtil.isValid(dni) == false`; `SIN_CERTIFICADO` si `CertificadoDigitalService.getTipoAlmacenClaveByDni(dni)` devuelve `null`; y si no, la traducción `TipoAlmacenClave → SituacionFirma` con el `switch` exhaustivo **sin `default`** que ya existe.
- `public static SituacionFirma build(User firmante)` pasa a ser `return buildByDni(firmante == null ? null : firmante.getDni());`. **MUST NOT** duplicarse la lógica.
- **MUST** conservarse el contrato actual: **nunca** devuelve `null` y **nunca** propaga excepción; ante cualquier error degrada a `SIN_CERTIFICADO`, que es el valor seguro. Es imprescindible aquí porque el getter que Axelor genera para el campo derivado solo captura `NullPointerException`.
- **MUST** conservarse el enmascarado del DNI en el log (`enmascararDni`). Ese método pasa de `private` a **package-private** (`static String enmascararDni(String dni)`) para que `FirmaEnServidorHelper`, que vive en el **mismo paquete**, lo reutilice en vez de escribir un segundo enmascarado. **MUST NOT** hacerse `public`: fuera del paquete nadie lo necesita.

## Instrucciones

- Acción de la fila §6: **Modificar**. Se toca **solo** este fichero.
- **La especificación de arriba es contrato fijo y la superficie es cerrada**: MUST NOT crearse ningún método, clase, campo, constante ni acción que esa especificación no liste, y MUST NOT modificarse ningún fichero que no sea el de esta tarea.
