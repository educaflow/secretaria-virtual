---
type: implementation-task
---

# Tarea 05 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas
- k-secure-coding
- k-code-quality

## Ficheros que cubre esta tarea (filas de la tabla "Ficheros a crear o modificar" de `design.md`)

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/subsystem/correos/service/AdjuntoService.java` | Crear | k-sistemas (servicios.md) | Interfaz del servicio de `Adjunto` |
| `src/main/java/com/educaflow/subsystem/correos/service/impl/AdjuntoServiceImpl.java` | Crear | k-sistemas (servicios.md) | Implementación del servicio de `Adjunto` |

## Texto del diseño (verbatim, `design.md`, Paso 3 — parte de `AdjuntoService`/`AdjuntoServiceImpl`)

#### `com.educaflow.subsystem.correos.service.AdjuntoService`

```java
package com.educaflow.subsystem.correos.service;

public interface AdjuntoService extends com.axelor.db.modelservice.ModelService<com.educaflow.subsystem.correos.db.Adjunto> {
    // Sin acciones propias: solo se sobrescriben validateInsert/Update/Remove y allowPropertiesInsert
    // (ver k-sistemas/servicios.md — no hace falta redeclarar insert/update/remove ni sus
    // validate*/allowProperties* si no se añade una acción nueva).
}
```

#### `com.educaflow.subsystem.correos.service.impl.AdjuntoServiceImpl`

```java
package com.educaflow.subsystem.correos.service.impl;

public class AdjuntoServiceImpl extends com.axelor.db.modelservice.DefaultModelService<Adjunto> implements AdjuntoService {

    public AdjuntoServiceImpl(Class<Adjunto> model, Repository<Adjunto> repository) { super(model, repository); }

    // (sin bloque de "Acciones": Adjunto no tiene ninguna acción propia más allá de las
    // sobrescrituras de update/remove — ver k-sistemas/servicios.md, bloque 1 vacío es válido)

    @Override
    public Adjunto update(Adjunto nuevo, Adjunto original);
    //   Inmutable tras su creación (Acción "Modificar" con Input AllowProperties vacío en
    //   entity-Adjunto.md). Lanza incondicionalmente UnsupportedOperationException.

    @Override
    public void remove(Adjunto adjunto);
    //   "Como un correo nunca se borra, sus adjuntos tampoco" (intro de entity-Correo.md, análogo a
    //   RES-Correo-003). Lanza incondicionalmente UnsupportedOperationException.

    /****************************************************************************************/
    /******************************** Métodos de Validación *********************************/
    /****************************************************************************************/

    @Override
    public java.util.Optional<BusinessMessages> validateInsert(Adjunto adjunto);
    //   Aplica (en este orden):
    //     - V-Adjunto-001 (Origen spec: VAL-Adjunto-001) adjunto.getCorreo() != null. Mensaje: "el
    //       adjunto debe pertenecer a un correo".
    //     - V-Adjunto-002 (Origen spec: VAL-Adjunto-002) si el usuario actual NO es Administrador,
    //       adjunto.getCorreo().getCentro() MUST estar entre los centros del usuario (mismo mecanismo
    //       que V-Correo-013 de CorreoServiceImpl). Mensaje: "no puede añadir adjuntos a correos de
    //       un centro que no es suyo".
    //     - V-Adjunto-003 (Origen spec: VAL-Adjunto-003) adjunto.getCorreo().getFechaCreacion() ==
    //       null. fechaCreacion es un campo servidor que CorreoServiceImpl solo asigna DESPUÉS de
    //       validar todo el árbol (ModelServiceValidationWalker valida los detalles ANTES de que el
    //       insert() del maestro llegue a fireActionRule_AsignarValoresIniciales) — por eso
    //       fechaCreacion == null distingue con fiabilidad "el correo se está creando ahora, en esta
    //       misma petición" de "el correo ya existía de antes" (ver Notas y supuestos: es una
    //       inferencia sobre el orden de ejecución del framework, MUST verificarse empíricamente al
    //       implementar). Mensaje: "no se pueden añadir adjuntos a un correo ya existente".
    //     - V-Adjunto-004 (Origen spec: VAL-Adjunto-004) nombreFichero obligatorio.
    //     - V-Adjunto-005 (Origen spec: VAL-Adjunto-005) contenido obligatorio.
    //     - V-Adjunto-006 (Origen spec: RES-Adjunto-001) recorrer adjunto.getCorreo().getAdjuntos()
    //       (JPA.edit ya ha ensamblado el grafo completo, incluidas las referencias bidireccionales,
    //       antes de que ModelServiceValidationWalker valide cada detalle) y comprobar que ningún
    //       hermano tiene el mismo nombreFichero (trim, comparación exacta; se compara contra los
    //       demás, nunca contra sí mismo). Mensaje: "ya existe un adjunto con ese nombre en el correo"
    //       (defensa en profundidad además del <unique-constraint> declarativo de Adjunto.xml).
    //       Ubicada aquí, en la entidad dueña de la restricción (k-validaciones/validaciones.md
    //       "las validaciones del detalle se escriben una vez, en el servicio del detalle"), no en
    //       CorreoServiceImpl.

    @Override
    public java.util.Optional<BusinessMessages> validateUpdate(Adjunto nuevo, Adjunto original);
    //   V-Adjunto-007 (Origen spec: —, ver Notas y supuestos): SIEMPRE rechaza.

    @Override
    public java.util.Optional<BusinessMessages> validateRemove(Adjunto adjunto);
    //   V-Adjunto-008 (Origen spec: —, análogo a RES-Correo-003): SIEMPRE rechaza.

    /**************************************************************************************/
    /********************************   AllowProperties   *********************************/
    /**************************************************************************************/

    @Override
    public AllowProperties allowPropertiesInsert();
    //   createAllowProperties(Map.of("nombreFichero", Map.of(), "contenido", Map.of(), "correo", Map.of()))
    //   — Adjunto no tiene ningún campo servidor (ver Frontera de confianza).
}
```

## Frontera de confianza — AllowProperties por acción (verbatim, `design.md`)

### `AdjuntoServiceImpl.insert` (invocado a través del endpoint REST automático, dentro del alta en cascada de `Correo`)

Entidad: `Adjunto`. **Forma elegida**: `createAllowProperties` (whitelist), aunque también sería válida `createAllowAllProperties` (Adjunto no tiene ningún campo `servidor` — se opta por la whitelist explícita por claridad y trazabilidad).
**Origen spec:** `Input AllowProperties` de la acción `Crear` de `entity-Adjunto.md`.

| Campo | Origen | En whitelist | Justificación / Ubicación de la asignación |
|---|---|---|---|
| `nombreFichero` | cliente | sí | Input directo del formulario |
| `contenido` | cliente | sí | Input directo del formulario (subida) |
| `correo` | cliente | sí | Referencia al padre embebido (k-secure-coding §3.6): la fija la UI vía `onNew`/`__parent__`, pero la defensa real es `AdjuntoServiceImpl.validateInsert` (V-Adjunto-001/002/003), no la UI |

## Trazabilidad Origen spec → V → ubicación (verbatim, `design.md`, filas de `Adjunto`)

| V | Origen spec | Ubicación |
|---|---|---|
| V-Adjunto-001 | VAL-Adjunto-001 | `AdjuntoServiceImpl.validateInsert` |
| V-Adjunto-002 | VAL-Adjunto-002 | `AdjuntoServiceImpl.validateInsert` |
| V-Adjunto-003 | VAL-Adjunto-003 | `AdjuntoServiceImpl.validateInsert` |
| V-Adjunto-004 | VAL-Adjunto-004 | `AdjuntoServiceImpl.validateInsert` |
| V-Adjunto-005 | VAL-Adjunto-005 | `AdjuntoServiceImpl.validateInsert` |
| V-Adjunto-006 | RES-Adjunto-001 | `AdjuntoServiceImpl.validateInsert` (+ `Adjunto.xml` `<unique-constraint>` declarativo) — movida desde `CorreoServiceImpl`, la entidad dueña de la restricción |
| V-Adjunto-007 | — (inmutabilidad, Acción Modificar sin `Input AllowProperties` de `entity-Adjunto.md`) | `AdjuntoServiceImpl.validateUpdate` |
| V-Adjunto-008 | — (análogo a RES-Correo-003, intro de `entity-Correo.md`: "sus adjuntos tampoco [se borran]") | `AdjuntoServiceImpl.validateRemove` |

### Nota y supuesto aplicable (verbatim, `design.md`)

13. **RES-Adjunto-001 (unicidad de `nombreFichero` entre adjuntos del mismo correo) vive en `AdjuntoServiceImpl.validateInsert` (V-Adjunto-006), no en `CorreoServiceImpl`.** Es la entidad dueña de la restricción; `ModelServiceValidationWalker` invoca siempre el `validate*` del servicio del propio detalle para cada hijo de una composición maestro-detalle, y en ese momento `JPA.edit` ya ha ensamblado el grafo completo (incluidas las referencias bidireccionales), por lo que `adjunto.getCorreo().getAdjuntos()` ya contiene a todos los hermanos de la misma petición.

## Superficie cerrada

**MUST** crear únicamente `AdjuntoService` (interfaz) y `AdjuntoServiceImpl` (implementación) con exactamente los métodos/firmas listados arriba. **MUST NOT** crear un `AdjuntoController` (Paso 5 del diseño dice explícitamente "No hay `AdjuntoController`"). **MUST NOT** inventar métodos o clases auxiliares no listados. Si detectas que hace falta algo no listado, **detente y reporta** `BLOCKED`.
