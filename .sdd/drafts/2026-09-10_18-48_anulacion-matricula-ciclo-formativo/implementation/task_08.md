---
type: implementation-task
template: expediente
---

# Tarea 08 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-tipo-expediente
- k-secure-coding
- k-code-quality

Escribe `InitialEventManagerImpl.java` **sobre el esqueleto** que dejó `CreateFilesTask` en

```
src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/InitialEventManagerImpl.java
```

FQCN: `com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.InitialEventManagerImpl`. Supertipo: `implements InitialEventManager<AnulacionMatriculaCicloFormativoV1>` (el parámetro de tipo es obligatorio: es lo que `ExpedienteLocator.getModelClass` lee en runtime). Va en la **raíz de la carpeta de versión**, nunca en una subcarpeta de fase.

**CRITICAL — la especificación de `## 8` que va abajo es contrato fijo y la superficie es cerrada: MUST NOT crearse ningún método, clase, campo ni acción que esa especificación no liste**, ni reordenarse las asignaciones (el orden es normativo).

Para obtener el usuario autenticado **MUST** usarse `SecurityUtil.getUser()`; **NUNCA** `AuthUtils.getUser()`.

## Fila de la tabla `## 6. Ficheros a crear o modificar` del diseño

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/InitialEventManagerImpl.java` | Crear | `k-tipo-expediente` | Lo especifica `## 8. Especificación del InitialEventManagerImpl` |

## Paso del diseño (verbatim)

### Paso 8 — `InitialEventManagerImpl.java`

Fichero: `…/v1/InitialEventManagerImpl.java`. FQCN: `com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.InitialEventManagerImpl`.

Su especificación quirúrgica está en **`## 8. Especificación del InitialEventManagerImpl`**; **MUST** leerse el `design.md` entero, no solo este paso.

Supertipo: `implements InitialEventManager<AnulacionMatriculaCicloFormativoV1>` (parámetro de tipo obligatorio: es lo que `ExpedienteLocator.getModelClass` lee en runtime).

**Verificación:** compila; declara **exactamente un** `void triggerInitialEvent(AnulacionMatriculaCicloFormativoV1, EventContext)`; existe una asignación por cada fila de la tabla de §8, en ese orden; `personaSolicitante` y `personaInteresada` quedan no nulos.


## `## 8. Especificación del InitialEventManagerImpl` (verbatim, ÍNTEGRA)

## 8. Especificación del InitialEventManagerImpl

Fichero: `…/v1/InitialEventManagerImpl.java`, **en la raíz de la carpeta de versión**.

```java
package com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1;

public class InitialEventManagerImpl implements InitialEventManager<AnulacionMatriculaCicloFormativoV1> {
    @Override
    public void triggerInitialEvent(AnulacionMatriculaCicloFormativoV1 expediente, EventContext eventContext) throws BusinessException { … }
}
```

Asignaciones, **en este orden** (el orden es normativo):

| # | Setter | Fuente del valor | Por qué |
|---|---|---|---|
| 1 | *(construir)* `Persona persona` | `new Persona()` con `setNombre(expediente.getUsuarioRegistrador().getNombre())`, `setApellidos(…getApellidos())` y `setDni(…getDni())` | Los datos de identidad del alumno quedan congelados en el expediente aunque después cambien en su ficha (CC-001) |
| 2 | `setPersonaInteresada(persona)` | la `Persona` del paso 1 | El interesado es el propio alumno; `createRegistroEntrada` lo lee del expediente y **revienta con NPE si es nulo** |
| 3 | `setPersonaSolicitante(persona)` | la **misma** `Persona` del paso 1 | El solicitante es el propio alumno (se da por supuesto que es mayor de edad y firma él mismo) |
| 4 | `setCursoAcademico(...)` | `expediente.getCentro().getCurso()` formateado como `curso + "/" + (curso + 1)`; `null` si el centro no tiene curso | CC-003: el impreso lo pide y el alumno no debe poder elegir otro |
| 5 | `setNombreCentro(expediente.getCentro().getName())` | el nombre del centro del expediente | CC-004: se imprime en «Expone» y en el pie dirigido al director |
| 6 | `setLocalidadCentro(...)` | `expediente.getCentro().getMunicipio()` → su `getName()`; `null` si el centro no tiene municipio | CC-004: se imprime en «Expone» y es el **lugar** de la línea de lugar y fecha (CC-005). Los dos centros de demo ya tienen municipio, que les da el catálogo `Centro.xml` del subsistema `common` (§14 nota 16) |

Reglas explícitas:

- Lo que el `Tramitador` ya rellenó **antes** de llamar a este método y que **MUST NOT** reasignarse: `tipoExpediente`, `centro`, `usuarioRegistrador`, `name` y `numeroExpediente`.
- Lo que hace **después** y que tampoco es cosa de este método: fijar el estado inicial, crear el `HistorialEstado` y llamar al `onEnterState` del estado inicial.
- **CRITICAL** — este tipo **sí** crea registro de entrada (`PRESENTAR`), así que las asignaciones 2 y 3 son obligatorias: `createRegistroEntrada` lanza **NPE** si `personaSolicitante` o `personaInteresada` son nulos, y **nada lo verifica en build**.
- **MUST NOT** llamar a `eventContext.updateState(...)`: el estado inicial lo fija el `Tramitador`.
- **El NIA, la dirección, el teléfono, la población, la provincia y el código postal NO se precargan** (CC-002). En el modelo actual **no existe ninguna «ficha del alumno»**: `User` solo guarda nombre, apellidos y DNI, y las filas `Persona` son fotos que crea cada expediente, no datos maestros. El expediente nace con esos seis campos vacíos y el alumno los teclea. Ver §14.

Dependencias a inyectar: **ninguna**.


## `## 11. Reparto de reglas` — filas que ubican una regla en el `triggerInitialEvent` (verbatim)

| Tipo de regla | Capa | Cómo se escribe |
|---|---|---|
| Tipo, longitud máxima de columna, referencia, enumerado | **modelo XML** (`domains.xml`) | atributos de `<string>`/`<date>`/`<enum>`/`<many-to-one>` |
| Obligatoriedad de un campo **en un evento** | **DSL del validador** | `+Required(mensaje)` en la pareja (estado, evento) |
| Formato, rango, longitud, firma | **DSL del validador** | `Pattern`, `MinLength`/`MaxLength`, `FirmaPdf` |
| Obligatoriedad **condicional** | **DSL del validador** | `ifValueIn(...) { +Required(...) }` |
| Valor no admitido en un evento | **DSL del validador** | `ifValueIn`/`ifValueNotIn` + `NoAdmitido(mensaje)`, regla **nueva** del catálogo común que **MUST** ir siempre dentro de una rama: la rama pone la condición, la regla el mensaje (`decisiones.md` D7, §14 nota 21) |
| Qué campos puede dictar el cliente en un evento | **DSL del validador** | el conjunto de `field(...)` de esa pareja |
| Efectos: generar PDF, firmar, registrar, transicionar, limpiar, calcular | **`trigger*`** del `PhaseEventManagerImpl` | listas de acciones de §9 |
| Inicialización del expediente | **`triggerInitialEvent`** | §8 |
| Quién es el usuario autenticado (autoría, centro, **perfil sobre el expediente**) | **guarda en la primera línea del `trigger*`** | `ControlDeAcceso.exige…` → `BusinessException`. La de perfil no reescribe **ninguna** de sus dos preguntas: *qué* perfil se exige se lo pregunta al `<state>` (vía `States`) y *si el usuario lo ostenta*, a `PerfilesUsuarioService.getPerfilesSobreExpediente` (§9.0.1) |
| Mostrar/ocultar/deshabilitar, ayudas, confirmaciones | **vista** | `showIf`/`readonly`, `<help>`, `prompt`, `<action-record>` — **solo UX, NUNCA defensa** |
| Campo calculado de **lectura** | **modelo XML del catálogo** | `Ciclo.gradoNivel`, campo derivado `transient` |
| Clasificación que consultan **varias capas de servidor** (trigger + validador + documento) | **función estática de una clase de la raíz de la versión** | `ReglasAnulacionMatricula.esRechazo(expediente)` (§9.0.3); es una función, no un campo, porque ninguna vista lo consume (ver debajo), y vive fuera del `<extra-code-model>` porque el build reescribe ese bloque entero |

| Regla | Capa |
|---|---|
| CC-001, CC-003, CC-004 | `triggerInitialEvent` (§8) |
| CC-002 | **sin origen de datos hoy**: no existe ninguna ficha del alumno de la que copiar; los campos nacen vacíos (§8 y §14) |
