---
type: implementation-task
template: expediente
---

# Tarea 11 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-tipo-expediente
- k-validaciones
- k-secure-coding
- k-code-quality
- k-i18n

## Qué hay que hacer

Materializa los tres ficheros de la fase `REVISION`, en `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/revision/`:

1. **`PhaseEventManagerImpl.java`** (FQCN `…v1.revision.PhaseEventManagerImpl`) — escríbelo **sobre el esqueleto** que dejó `CreateFilesTask`, según la especificación de §9 copiada abajo.
2. **`StateEventValidatorImpl.kt`** (paquete `…v1.revision`) — escríbelo **sobre el esqueleto** que dejó `CreateFilesTask`, según la especificación de §10 copiada abajo.
3. **`views.xml`** — **cópialo literalmente** de `design/fases/revision/views.xml` a `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/revision/views.xml`, **sobrescribiendo** el esqueleto. Es **contrato fijo**: **MUST NOT** modificarse, reescribirse ni regenerarse.

Las especificaciones de §9 y §10 que van copiadas abajo son **contrato fijo** y la **superficie es cerrada**: **MUST NOT** crearse ningún método, clase, campo ni acción que no esté en su lista de cobertura, ni añadirse reglas, campos en un `field(...)`, argumentos ni acciones que la especificación no declare; ni alterarse el orden de las acciones numeradas de cada `trigger*`.

- El `import` de `States` **MUST** ser el de **esta** versión (`…anulacion_matricula_ciclo_formativo.v1.States`).
- **MUST NOT** factorizarse ningún `trigger*`/`onEnter*` en una superclase: el dispatcher usa `getDeclaredMethods()`.
- El conjunto de `field(...)` de cada pareja (estado, evento) es la **whitelist de campos que el cliente puede dictar** en ese evento: **MUST NOT** añadirse ninguno de más ni quitarse ninguno de los declarados.
- Para el usuario autenticado **MUST** usarse `SecurityUtil.getUser()`, nunca `AuthUtils.getUser()`.

## Filas de la tabla «## 6. Ficheros a crear o modificar» del diseño (verbatim)

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/revision/PhaseEventManagerImpl.java` | Crear | `k-tipo-expediente` | Lo especifica `## 9. Especificación de los PhaseEventManagerImpl` (fase `REVISION`) |
| `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/revision/StateEventValidatorImpl.kt` | Crear | `k-tipo-expediente`, `k-secure-coding` | Lo especifica `## 10. Especificación de los StateEventValidatorImpl` (fase `REVISION`) |
| `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/revision/views.xml` | Crear | `k-tipo-expediente` | Copia de `design/fases/revision/views.xml` |

## `### Paso 10 — PhaseEventManagerImpl.java de cada fase` del diseño (verbatim)

### Paso 10 — `PhaseEventManagerImpl.java` de cada fase

Ficheros: `…/v1/solicitud/PhaseEventManagerImpl.java`, `…/v1/revision/PhaseEventManagerImpl.java` y `…/v1/resolucion/PhaseEventManagerImpl.java` (FQCN = paquete de la fase + `.PhaseEventManagerImpl`).

Su especificación quirúrgica está en **`## 9. Especificación de los PhaseEventManagerImpl`**, con una subsección por fase.

Supertipo: `extends PhaseEventManager<AnulacionMatriculaCicloFormativoV1>`, con constructor `@Inject` que recibe `AnulacionMatriculaCicloFormativoV1Repository` y llama a `super(AnulacionMatriculaCicloFormativoV1.class)`.

**Verificación:** compilan; por cada fase hay **un** `@WhenEvent trigger<Evento>` por cada evento de su lista de cobertura (§9.4) y **un** `@OnEnterState onEnter<Estado>` por cada estado de esa lista, y ninguno de más; ningún `triggerInitialEvent` ni `triggerExit`; el `import` de `States` es el de esta misma versión.


## `### Paso 11 — StateEventValidatorImpl.kt de cada fase` del diseño (verbatim)

### Paso 11 — `StateEventValidatorImpl.kt` de cada fase

Ficheros: `…/v1/solicitud/StateEventValidatorImpl.kt`, `…/v1/revision/StateEventValidatorImpl.kt` y `…/v1/resolucion/StateEventValidatorImpl.kt` (paquete de la fase).

Su especificación quirúrgica está en **`## 10. Especificación de los StateEventValidatorImpl`**, con una subsección por fase. Interfaz: `StateEventValidator`.

**Verificación:** compilan; hay exactamente un `@BeanValidationRulesForStateAndEvent getForState<Estado>InEvent<Evento>()` por cada fila «sí» o «`rules { }` vacío» de la tabla de cobertura de §10, y **ninguno** para `DELETE`; ningún `field(...)` menciona un campo clasificado `servidor` en §4 (salvo `pdfSolicitudFirmada`, la excepción documentada).


## `### Paso 12 — views.xml de cada fase` del diseño (verbatim)

### Paso 12 — `views.xml` de cada fase

Los ficheros están materializados en `design/fases/solicitud/views.xml`, `design/fases/revision/views.xml` y `design/fases/resolucion/views.xml`. **Cópialos literalmente** a `…/v1/solicitud/views.xml`, `…/v1/revision/views.xml` y `…/v1/resolucion/views.xml`, **sobrescribiendo** los esqueletos. **MUST NOT** modificarlos, reescribirlos ni regenerarlos.

**Resumen estructural por fase** (`(estado, perfil) → paneles → botones`):

| fase | estado | profile | paneles incluidos | botones (izq / der) |
|---|---|---|---|---|
| SOLICITUD | `DATOS_SOLICITUD` | `CREADOR` | `-subsanacion`, `datos-alumno`, `matricula` | `DELETE` / `CONTINUAR` |
| SOLICITUD | `DATOS_SOLICITUD` | — | `-subsanacion`, `-datos-alumno`, `-matricula` | — / `EXIT` |
| SOLICITUD | `PENDIENTE_FIRMA` | `CREADOR` | `-subsanacion`, `-solicitud-visor` + paneles de situación de firma | `VOLVER` / `PRESENTAR` ×2 |
| SOLICITUD | `PENDIENTE_FIRMA` | — | `-datos-alumno`, `-matricula`, `-solicitud-descarga` | — / `EXIT` |
| REVISION | `PENDIENTE_REVISION` | `SECRETARIO` | `-devolucion-view`, `-datos-alumno`, `-matricula`, `-presentacion-descarga`, `-solicitud-firmada-visor`, `revision` | `SUBSANAR` / `ENVIAR_A_FIRMA` |
| REVISION | `PENDIENTE_REVISION` | — | `-datos-alumno`, `-matricula`, `-presentacion`, `-solicitud-firmada-descarga` | — / `EXIT` |
| RESOLUCION | `PENDIENTE_FIRMA_DIRECTOR` | `DIRECTOR` | `-datos-alumno-resumen`, `-matricula-director`, `-decision-secretaria`, `-resolucion-a-firmar`, `-solicitud-firmada-descarga`, `-justificante-descarga`, `devolucion` | `DEVOLVER` / `FIRMAR` |
| RESOLUCION | `PENDIENTE_FIRMA_DIRECTOR` | — | `-datos-alumno`, `-matricula`, `-presentacion`, `-solicitud-firmada-descarga` | — / `EXIT` |
| RESOLUCION | `ACEPTADA` | — | `-datos-alumno`, `-matricula`, `-presentacion-descarga-aceptada`, `-resolucion-firmada`, `-solicitud-firmada-descarga` | — / `EXIT` |
| RESOLUCION | `RECHAZADA` | — | `-datos-alumno`, `-matricula`, `-presentacion-descarga`, `-resolucion-firmada`, `-solicitud-firmada-descarga` | — / `EXIT` |

Cada form lleva además, **fuera** del `<include-panels>`, un `<panel showFrame="false">` con el `<help>` del aviso permanente que la especificación fija para esa pantalla. **Excepción:** la indicación de RUI-ACEPTADA-GENERICA-002 **no** va en ese panel de avisos, sino dentro del panel `presentacion-descarga-aceptada`, junto a la fecha y la hora de presentación a las que acompaña (mismo patrón que `matricula-director` para RUI-PENDIENTE_FIRMA_DIRECTOR-DIRECTOR-007).

Las acciones propias van en el `views.xml` de su fase: en `solicitud/`, las cuatro de la firma más el `<action-attrs>` `…-DATOS_SOLICITUD-onLoad-action` que marca como obligatorios en pantalla los siete campos del alumno (RUI-DATOS_SOLICITUD-CREADOR-002); en `revision/`, la que limpia el motivo/el texto al cambiar el sentido.

**Verificación:** `diff` vacío en los tres; ningún `<button name="">` sin rellenar; cada estado tiene su form genérico con botón `EXIT`; los estados con `profile` y eventos tienen además su form de perfil; la suma de `colSpan` de los botones de cada footer no pasa de 12.


## Resumen estructural `(estado, perfil) → paneles → botones` de la fase REVISION (verbatim, filas de la tabla del Paso 12)

| fase | estado | profile | paneles incluidos | botones (izq / der) |
|---|---|---|---|---|
| REVISION | `PENDIENTE_REVISION` | `SECRETARIO` | `-devolucion-view`, `-datos-alumno`, `-matricula`, `-presentacion-descarga`, `-solicitud-firmada-visor`, `revision` | `SUBSANAR` / `ENVIAR_A_FIRMA` |
| REVISION | `PENDIENTE_REVISION` | — | `-datos-alumno`, `-matricula`, `-presentacion`, `-solicitud-firmada-descarga` | — / `EXIT` |

## Tabla de transiciones de §3, filas cuyo estado origen es de la fase REVISION (verbatim)

| fase origen | estado origen | evento | guarda | fase destino | estado destino |
|---|---|---|---|---|---|
| `REVISION` | `PENDIENTE_REVISION` | `ENVIAR_A_FIRMA` | — | `RESOLUCION` | `PENDIENTE_FIRMA_DIRECTOR` |
| `REVISION` | `PENDIENTE_REVISION` | `SUBSANAR` | — | `SOLICITUD` | `DATOS_SOLICITUD` |

`ENVIAR_A_FIRMA` y `SUBSANAR` tienen **un solo destino cada uno**, así que no llevan guarda: lo que la especificación describe como «condición» de cada uno (que el sentido sea aceptar/rechazar o que sea subsanar) es una **validación** que impide disparar el evento, no una ramificación de la transición, y vive en el DSL del validador (§10). El único evento ramificado es `FIRMAR`, cuyo discriminador es `sentidoRevision`; sus dos ramas cubren los dos únicos valores posibles en ese estado (`SUBSANAR` no puede llegar aquí, porque el validador de `ENVIAR_A_FIRMA` lo rechaza) y el `default` del `switch` es un error de programación.

## `## 9. Especificación de los PhaseEventManagerImpl` — subsección de la fase REVISION, ÍNTEGRA (verbatim)

### 9.2 Fase REVISION

```java
package com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.revision;

public class PhaseEventManagerImpl extends PhaseEventManager<AnulacionMatriculaCicloFormativoV1> { … }
```

- FQCN: `…v1.revision.PhaseEventManagerImpl`.
- Dependencias a inyectar: `AnulacionMatriculaCicloFormativoV1Repository repository` (por constructor). Ninguna más.
- Constantes de clase: ninguna.
- `import …v1.States;`

**`triggerEnviarAFirma`**

1. `ERROR_NEGOCIO(ControlDeAcceso.exigeMismoCentroQueElExpediente(expediente, "Solo puede revisar solicitudes de su propio centro"))` — VAL-PENDIENTE_REVISION-ENVIAR_A_FIRMA-005
2. `ASIGNAR(fechaRevision = LocalDate.now(Convert.defaultZoneId))` — RN-008 / CC-009
3. `ASIGNAR(revisadoPor = SecurityUtil.getUser())` — RN-008 / CC-009
4. `LIMPIAR(motivoRechazo)` **si** `sentidoRevision != RECHAZAR` — RN-009; va **antes** de generar el PDF para que la resolución no arrastre el motivo de un ciclo anterior. **La condición se escribe así, y no `== ACEPTAR`, a propósito**: «el motivo del rechazo aplica **si y solo si** el sentido es `RECHAZAR`» es una sola decisión, y los cinco sitios que la escriben (este trigger, el `showIf` y el `<action-record>` de la vista, el `ifValueIn` del validador y la expresión del PDF) **MUST** formularla siempre alrededor del valor `RECHAZAR`. Hoy `!= RECHAZAR` y `== ACEPTAR` son equivalentes solo porque el validador de `ENVIAR_A_FIRMA` rechaza `SUBSANAR`; con un cuarto sentido, la primera sigue siendo correcta y la segunda dejaría el motivo escrito sin que nadie avisara
5. `LIMPIAR(textoSubsanacion)` — RN-010, sin condición (idempotente)
6. `SERVICIO(DevolucionDelDirector.borrar(expediente))` — RN-011, sin condición (idempotente)
7. `GENERAR_PDF(RESOLUCION)`
8. `CREAR_METAFILE → pdfResolucion` — RN-012 / CC-010 (sustituye a la de un envío anterior)
9. `UPDATE_STATE(States.Resolucion.PENDIENTE_FIRMA_DIRECTOR)`

**`triggerSubsanar`**

1. `ERROR_NEGOCIO(ControlDeAcceso.exigeMismoCentroQueElExpediente(expediente, "Solo puede revisar solicitudes de su propio centro"))` — VAL-PENDIENTE_REVISION-SUBSANAR-004
2. `ASIGNAR(fechaRevision = LocalDate.now(Convert.defaultZoneId))` — RN-013 / CC-009
3. `ASIGNAR(revisadoPor = SecurityUtil.getUser())` — RN-013 / CC-009
4. `LIMPIAR(motivoRechazo)` — RN-014, sin condición (idempotente)
5. `SERVICIO(DevolucionDelDirector.borrar(expediente))` — RN-015, sin condición (idempotente)
6. `LIMPIAR(pdfResolucion)` — RN-016, sin condición (idempotente): los datos van a cambiar
7. `UPDATE_STATE(States.Solicitud.DATOS_SOLICITUD)`

**`onEnter*`**

- `onEnterPendienteRevision` — vacío.

#### 9.2.3 Lista de cobertura de la fase REVISION

- Eventos: `ENVIAR_A_FIRMA` → `triggerEnviarAFirma`; `SUBSANAR` → `triggerSubsanar`.
- Estados: `PENDIENTE_REVISION` → `onEnterPendienteRevision`.

## `## 10. Especificación de los StateEventValidatorImpl` — subsección de la fase REVISION, ÍNTEGRA (verbatim)

### 10.2 Fase REVISION

```kotlin
package com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.revision

import com.educaflow.subsystem.expedientes.db.AnulacionMatriculaCicloFormativoV1 as model
import com.educaflow.subsystem.expedientes.db.SentidoRevisionAnulacionMatriculaCicloFormativoV1 as SentidoRevision
import com.educaflow.base.infrastructure.validation.rules.NoAdmitido
```

Tabla de cobertura:

| estado | evento | método | ¿reglas? |
|---|---|---|---|
| `PENDIENTE_REVISION` | `ENVIAR_A_FIRMA` | `getForStatePendienteRevisionInEventEnviarAFirma` | sí |
| `PENDIENTE_REVISION` | `SUBSANAR` | `getForStatePendienteRevisionInEventSubsanar` | sí |

**`getForStatePendienteRevisionInEventEnviarAFirma`**

```kotlin
field(model::getSentidoRevision) {
    +Required("Debe indicar el sentido de la revisión")
    +ifValueIn(model::getSentidoRevision, listOf(SentidoRevision.SUBSANAR)) {
        +NoAdmitido("Para pedir una subsanación use el botón «Pedir subsanación al alumno»")
    }
}
field(model::getMotivoRechazo) {
    +ifValueIn(model::getSentidoRevision, listOf(SentidoRevision.RECHAZAR)) {
        +Required("Debe indicar el motivo del rechazo")
        +MinLength(10, "El motivo del rechazo debe tener entre 10 y 1000 caracteres")
        +MaxLength(1000, "El motivo del rechazo debe tener entre 10 y 1000 caracteres")
    }
}
field(model::getTextoSubsanacion) {
}
```

- El `field(model::getTextoSubsanacion)` **sin reglas** está a propósito: el formulario de la secretaría permite escribir ese texto y, si no entrara en la whitelist del evento, lo que el usuario escribiera se perdería en silencio. No lleva ninguna regla porque en este evento no se exige nada de él (el trigger lo limpia, RN-010).
- La condición de cuándo aplica cada regla se lee **en la rama**, no dentro de la regla.

**`getForStatePendienteRevisionInEventSubsanar`**

```kotlin
field(model::getSentidoRevision) {
    +ifValueNotIn(model::getSentidoRevision, listOf(SentidoRevision.SUBSANAR)) {
        +NoAdmitido("Para pedir una subsanación elija el sentido «Pedir subsanación»")
    }
}
field(model::getTextoSubsanacion) {
    +ifValueIn(model::getSentidoRevision, listOf(SentidoRevision.SUBSANAR)) {
        +Required("Debe indicar al alumno qué tiene que subsanar")
        +MinLength(10, "El texto de la subsanación debe tener entre 10 y 1000 caracteres")
        +MaxLength(1000, "El texto de la subsanación debe tener entre 10 y 1000 caracteres")
    }
}
field(model::getMotivoRechazo) {
}
```

- `ifValueNotIn` cubre también el caso «sentido sin elegir»: `null` no está en la lista, así que la rama aplica y `NoAdmitido` falla con el mensaje de la especificación.
- Las tres reglas de `textoSubsanacion` van **dentro de la rama del sentido** por el mismo motivo, y no sueltas: si el sentido elegido no es «Pedir subsanación» la pantalla ni siquiera muestra ese texto (`showIf` del panel de revisión), así que exigirlo además de rechazar el sentido produciría **dos** mensajes cuando la especificación fija **uno** («Para pedir una subsanación elija el sentido «Pedir subsanación»», ESC-043 / T-027). El campo sigue en la whitelist del evento; lo que la rama declara es **cuándo** aplican VAL-PENDIENTE_REVISION-SUBSANAR-002 y 003, que es exactamente cuando la spec las pide. La condición de cuándo aplica una regla se lee **en la rama**, no dentro de la regla.
- `motivoRechazo` entra en la whitelist sin reglas por el mismo motivo que `textoSubsanacion` en el evento anterior: la pantalla lo puede llevar escrito.

## `### 10.4 Frontera de confianza` del diseño (verbatim)

### 10.4 Frontera de confianza

- Ningún `field(...)` menciona un campo clasificado `servidor` en §4 — ni `codePhase`, `codeState`, `abierto`, `centro` o `usuarioRegistrador`, ni ninguno de los cinco `MetaFile` que rellena el servidor, ni las fechas y autores de revisión, devolución y resolución. La **única excepción** es `pdfSolicitudFirmada`, que **también está clasificado `servidor`** y aparece en el validador únicamente porque es el destino de la firma en cliente y ese `field(...)` es el único sitio donde se puede comprobar la firma (§4).
- Cada campo editable en la vista de un estado aparece en el `field(...)` del evento que se dispara desde esa vista: `nia`, `direccion`, `telefono`, `poblacion`, `provincia`, `codigoPostal` y `ciclo` en `CONTINUAR`; `claveCertificado` y `pdfSolicitudFirmada` en `PRESENTAR`; `sentidoRevision`, `motivoRechazo` y `textoSubsanacion` en `ENVIAR_A_FIRMA` y en `SUBSANAR`; `motivoDevolucion` en `DEVOLVER`.
- **CRITICAL** — esta puerta **NO** protege el endpoint REST automático `POST /ws/rest/com.educaflow.subsystem.expedientes.db.AnulacionMatriculaCicloFormativoV1`, que Axelor publica para toda entidad y que **no pasa por el `Tramitador`**. Son dos puertas distintas: **MUST NOT** darse por protegida esta entidad porque su tramitación valide por evento, y **MUST NOT** introducirse un `ModelService` deny-all como parche (se retiró a propósito, ver `CLAUDE.md`).
- `readonly`, `showIf` y `hidden` de las vistas son **UX, nunca defensa**.

## `## 11. Reparto de reglas` — tabla general y filas que aplican a esta fase (verbatim)

## 11. Reparto de reglas

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
| Quién es el usuario autenticado (autoría, centro) | **guarda en la primera línea del `trigger*`** | `ControlDeAcceso.exige…` → `BusinessException` |
| Mostrar/ocultar/deshabilitar, ayudas, confirmaciones | **vista** | `showIf`/`readonly`, `<help>`, `prompt`, `<action-record>` — **solo UX, NUNCA defensa** |
| Campo calculado de **lectura** | **modelo XML del catálogo** | `Ciclo.gradoNivel`, campo derivado `transient` |

**Dueño único de «cuándo aplica el motivo del rechazo».** La decisión es una sola —*el motivo del rechazo aplica si y solo si `sentidoRevision == RECHAZAR`*— y la escriben **cinco** sitios: (1) el `showIf` de los paneles que lo enseñan (`revision`, `decision-secretaria` y `resolucion-firmada`), (2) el `<action-record>` `…-limpiar-revision-action` de la vista, (3) la acción 4 de `triggerEnviarAFirma` (§9.2), (4) la rama `ifValueIn(listOf(RECHAZAR))` del validador (§10.2) y (5) la expresión Groovy de `resolucion.xml`. **El dueño de la condición es el valor `RECHAZAR` del enum**: los cinco **MUST** formularla alrededor de él (`== RECHAZAR` o `!= RECHAZAR`), nunca alrededor de `ACEPTAR`, para que añadir un cuarto sentido no cambie el significado de ninguno sin que nadie lo note.

**Por qué las comprobaciones de identidad no van al validador.** El DSL cuelga cada regla de un campo, y esas comprobaciones no hablan de ningún campo. Además, dos de ellas caen donde el validador no llega o no debe crecer: `DELETE` se salta la validación entera en el `Tramitador` (su método del validador no existe y nunca se invocaría), y `FIRMAR` no admite ningún dato del formulario, así que darle un `field(...)` solo para colgar la regla metería ese campo en la whitelist del evento. La regla de reparto queda enunciable en una frase: **si la regla habla del valor de un campo, validador; si habla de quién eres, guarda del trigger**. Ver `decisiones.md` D4.
| Regla | Capa |
|---|---|
| VAL-PENDIENTE_REVISION-ENVIAR_A_FIRMA-001 … 004 | DSL del validador, `getForStatePendienteRevisionInEventEnviarAFirma` |
| VAL-PENDIENTE_REVISION-ENVIAR_A_FIRMA-005 | guarda de `triggerEnviarAFirma` |
| VAL-PENDIENTE_REVISION-SUBSANAR-001 … 003 | DSL del validador, `getForStatePendienteRevisionInEventSubsanar` |
| VAL-PENDIENTE_REVISION-SUBSANAR-004 | guarda de `triggerSubsanar` |
| RN-008 … RN-016 | acciones de `triggerEnviarAFirma` y `triggerSubsanar` (§9.2) |
| RUI-PENDIENTE_REVISION-SECRETARIO-001 … 014 | vista `PENDIENTE_REVISION` del `SECRETARIO` y `<action-record>` `…-limpiar-revision-action`. En concreto, la obligatoriedad visual del **002** es `required="true"` en `sentidoRevision` y la del **004** y el **006** son `requiredIf="sentidoRevision=='RECHAZAR'"` en `motivoRechazo` y `requiredIf="sentidoRevision=='SUBSANAR'"` en `textoSubsanacion`, las tres en el panel `revision` (que solo usa este form). La exigencia real está en el validador |
| RUI-PENDIENTE_REVISION-GENERICA-001 y 002 | vista genérica de `PENDIENTE_REVISION`: no incluye ningún panel de la decisión de secretaría. Que **todo** el que no sea la administrativa caiga en ella lo garantiza la condición de perfil de la bandeja de secretaría (Paso 16.0), sin la cual se abriría con `_profile=SECRETARIO` la pantalla editable |
| CC-005 … CC-013 | acciones de los `trigger*` (§9) |
Reglas duras respetadas: ningún `required="true"` en el `domains.xml`; ninguna validación de datos de usuario en un `trigger*`; ninguna lógica de negocio en el validador; ninguna inicialización del expediente en un `PhaseEventManagerImpl`.
