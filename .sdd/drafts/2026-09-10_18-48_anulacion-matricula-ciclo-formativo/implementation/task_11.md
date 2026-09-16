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

Implementa **la fase `REVISION`** completa: sus tres ficheros de `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/revision/`.

| Fichero | Acción | Cómo se materializa |
|---|---|---|
| `…/v1/revision/PhaseEventManagerImpl.java` | Crear | Se escribe **sobre el esqueleto** que dejó `CreateFilesTask`, según `## 9` (subsección de esta fase), abajo verbatim |
| `…/v1/revision/StateEventValidatorImpl.kt` | Crear | Se escribe **sobre el esqueleto** que dejó `CreateFilesTask`, según `## 10` (subsección de esta fase), abajo verbatim |
| `…/v1/revision/views.xml` | Crear | **Copia literal** de `design/fases/revision/views.xml` a `…/v1/revision/views.xml`, **sobrescribiendo** el esqueleto. **MUST NOT** modificarlo, reescribirlo ni regenerarlo |

**CRITICAL — las especificaciones de `## 9` y `## 10` que van abajo son contrato fijo y la superficie es cerrada: MUST NOT crearse ningún método, clase, campo, acción ni regla que no listen**, ni reordenarse las listas numeradas de acciones (el orden es normativo). Un `trigger*` por cada evento de la lista de cobertura y un `onEnter*` por cada estado de la fase, **ninguno de más**; en el validador, un `getForState<Estado>InEvent<Evento>` por cada pareja declarada salvo las de `DELETE`.

El conjunto de `field(...)` de cada método del validador es, además de la validación, **la lista de campos que el cliente puede dictar en ese evento** (`AllowProperties`): **MUST NOT** añadirse ningún `field(...)` de un campo clasificado `servidor` en §4 (la única excepción documentada del tipo es `pdfSolicitudFirmada` en `PRESENTAR`).

Para obtener el usuario autenticado **MUST** usarse `SecurityUtil.getUser()`; **NUNCA** `AuthUtils.getUser()`.

## Filas de la tabla `## 6. Ficheros a crear o modificar` del diseño

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/revision/PhaseEventManagerImpl.java` | Crear | `k-tipo-expediente` | Lo especifica `## 9. Especificación de los PhaseEventManagerImpl` (fase `REVISION`) |
| `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/revision/StateEventValidatorImpl.kt` | Crear | `k-tipo-expediente`, `k-secure-coding` | Lo especifica `## 10. Especificación de los StateEventValidatorImpl` (fase `REVISION`) |
| `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/revision/views.xml` | Crear | `k-tipo-expediente` | Copia de `design/fases/revision/views.xml` |

## Pasos del diseño (verbatim)

### Paso 10 — `PhaseEventManagerImpl.java` de cada fase

Ficheros: `…/v1/solicitud/PhaseEventManagerImpl.java`, `…/v1/revision/PhaseEventManagerImpl.java` y `…/v1/resolucion/PhaseEventManagerImpl.java` (FQCN = paquete de la fase + `.PhaseEventManagerImpl`).

Su especificación quirúrgica está en **`## 9. Especificación de los PhaseEventManagerImpl`**, con una subsección por fase.

Supertipo: `extends PhaseEventManager<AnulacionMatriculaCicloFormativoV1>`, con constructor `@Inject` que recibe `AnulacionMatriculaCicloFormativoV1Repository` y llama a `super(AnulacionMatriculaCicloFormativoV1.class)`.

**Verificación:** compilan; por cada fase hay **un** `@WhenEvent trigger<Evento>` por cada evento de su lista de cobertura (§9.4) y **un** `@OnEnterState onEnter<Estado>` por cada estado de esa lista, y ninguno de más; ningún `triggerInitialEvent` ni `triggerExit`; el `import` de `States` es el de esta misma versión; y **los ocho `trigger*` abren con una guarda de `ControlDeAcceso`** — los cuatro de `SOLICITUD` con `exigeSerElCreador` y los cuatro de `REVISION`/`RESOLUCION` con `exigeMismoCentroQueElExpediente` **seguida de** `exigeOstentarElPerfilDelEstado` (§11, «Las ocho guardas de identidad»), **ninguna de ellas con un literal de perfil como argumento** y sin `import` de `Profile` en ningún `PhaseEventManagerImpl`.


### Paso 11 — `StateEventValidatorImpl.kt` de cada fase

Ficheros: `…/v1/solicitud/StateEventValidatorImpl.kt`, `…/v1/revision/StateEventValidatorImpl.kt` y `…/v1/resolucion/StateEventValidatorImpl.kt` (paquete de la fase).

Su especificación quirúrgica está en **`## 10. Especificación de los StateEventValidatorImpl`**, con una subsección por fase. Interfaz: `StateEventValidator`.

**Verificación:** compilan; hay exactamente un `@BeanValidationRulesForStateAndEvent getForState<Estado>InEvent<Evento>()` por cada fila «sí» o «`rules { }` vacío» de la tabla de cobertura de §10, y **ninguno** para `DELETE`; ningún `field(...)` menciona un campo clasificado `servidor` en §4 (salvo `pdfSolicitudFirmada`, la excepción documentada).


### Paso 12 — `views.xml` de cada fase

Los ficheros están materializados en `design/fases/solicitud/views.xml`, `design/fases/revision/views.xml` y `design/fases/resolucion/views.xml`. **Cópialos literalmente** a `…/v1/solicitud/views.xml`, `…/v1/revision/views.xml` y `…/v1/resolucion/views.xml`, **sobrescribiendo** los esqueletos. **MUST NOT** modificarlos, reescribirlos ni regenerarlos.

**Resumen estructural por fase** (`(estado, perfil) → paneles → botones`):


| fase | estado | profile | paneles incluidos | botones (izq / der) |
|---|---|---|---|---|
| REVISION | `PENDIENTE_REVISION` | `SECRETARIO` | `-devolucion-view`, `-datos-alumno`, `-matricula`, `-presentacion-descarga`, `-solicitud-firmada-visor`, `revision` | `SUBSANAR` / `ENVIAR_A_FIRMA` |
| REVISION | `PENDIENTE_REVISION` | — | `-datos-alumno`, `-matricula`, `-presentacion`, `-solicitud-firmada-descarga` | — / `EXIT` |


Cada form lleva además, **fuera** del `<include-panels>`, un `<panel showFrame="false">` con el `<help>` del aviso permanente que la especificación fija para esa pantalla. **Excepción:** la indicación de RUI-ACEPTADA-GENERICA-002 **no** va en ese panel de avisos, sino dentro del panel `presentacion-descarga-aceptada`, junto a la fecha y la hora de presentación a las que acompaña (mismo patrón que `matricula-director` para RUI-PENDIENTE_FIRMA_DIRECTOR-DIRECTOR-007).

Las acciones propias van en el `views.xml` de su fase: en `solicitud/`, las cuatro de la firma más el `<action-attrs>` `…-DATOS_SOLICITUD-onLoad-action` que marca como obligatorios en pantalla los siete campos del alumno (RUI-DATOS_SOLICITUD-CREADOR-002); en `revision/`, la que limpia el motivo/el texto al cambiar el sentido.

**Verificación:** `diff` vacío en los tres; ningún `<button name="">` sin rellenar; cada estado tiene su form genérico con botón `EXIT`; los estados con `profile` y eventos tienen además su form de perfil; la suma de `colSpan` de los botones de cada footer no pasa de 12.


## Tabla de transiciones de `## 3`, filtrada a los estados de esta fase (verbatim)

| fase origen | estado origen | evento | guarda | fase destino | estado destino |
|---|---|---|---|---|---|
| `REVISION` | `PENDIENTE_REVISION` | `ENVIAR_A_FIRMA` | — | `RESOLUCION` | `PENDIENTE_FIRMA_DIRECTOR` |
| `REVISION` | `PENDIENTE_REVISION` | `SUBSANAR` | — | `SOLICITUD` | `DATOS_SOLICITUD` |

`ENVIAR_A_FIRMA` y `SUBSANAR` tienen **un solo destino cada uno**, así que no llevan guarda: lo que la especificación describe como «condición» de cada uno (que el sentido sea aceptar/rechazar o que sea subsanar) es una **validación** que impide disparar el evento, no una ramificación de la transición, y vive en el DSL del validador (§10). El único evento ramificado es `FIRMAR`, cuyo discriminador es `sentidoRevision`; sus dos ramas cubren los dos únicos valores posibles en ese estado (`SUBSANAR` no puede llegar aquí, porque el validador de `ENVIAR_A_FIRMA` lo rechaza) y el `default` del `switch` es un error de programación.

## `### 9.2 Fase REVISION` (verbatim, ÍNTEGRA)

### 9.2 Fase REVISION

```java
package com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.revision;

public class PhaseEventManagerImpl extends PhaseEventManager<AnulacionMatriculaCicloFormativoV1> { … }
```

- FQCN: `…v1.revision.PhaseEventManagerImpl`.
- Dependencias a inyectar: `AnulacionMatriculaCicloFormativoV1Repository repository` (por constructor). Ninguna más.
- Constantes de clase: ninguna.
- `import …v1.States;`, `import …v1.ReglasAnulacionMatricula;` (el dueño del predicado de §9.0.3) e `import …v1.ControlDeAcceso;`. **MUST NOT** aparecer `import com.educaflow.subsystem.expedientes.db.Profile;`: la guarda de perfil de las dos acciones 1 **no recibe ningún perfil** —se lo pregunta al `<state>` a través de `States` (§9.0.1)—, así que ningún `PhaseEventManagerImpl` nombra el enum.

**`triggerEnviarAFirma`**

1. `ERROR_NEGOCIO(ControlDeAcceso.exigeMismoCentroQueElExpediente(expediente, "Solo puede revisar solicitudes de su propio centro"))` — VAL-PENDIENTE_REVISION-ENVIAR_A_FIRMA-005 — y, a continuación, `ERROR_NEGOCIO(ControlDeAcceso.exigeOstentarElPerfilDelEstado(expediente, "Solo la secretaría del centro puede revisar esta solicitud"))` — **guarda de perfil del diseño** (§11, «Las ocho guardas de identidad»): el motor exime al administrador de `checkPerfilDelEstado` en **cualquier** estado, así que sin ella el administrador con centro activo en el del expediente —el que le da el Paso 15— podría enviar a la firma la solicitud de otro, contra HU-010/ESC-025 (solo consulta). **No se le pasa ningún perfil**: la guarda le pregunta a `States` cuál declara `PENDIENTE_REVISION` (hoy `SECRETARIO`) y exige ese (§9.0.1), de modo que el `TipoExpedienteInstance.xml` sigue siendo el único sitio donde se dice qué perfil atiende el estado. Las **dos** van en la acción 1, antes de cualquier efecto, y son complementarias (§9.0.1)
2. `ASIGNAR(fechaRevision = LocalDate.now(Convert.defaultZoneId))` — RN-008 / CC-009
3. `ASIGNAR(revisadoPor = SecurityUtil.getUser())` — RN-008 / CC-009
4. `LIMPIAR(motivoRechazo)` **si** `ReglasAnulacionMatricula.esRechazo(expediente) == false` (notación condicional declarada en §14 nota 24) — RN-009; va **antes** de generar el PDF para que la resolución no arrastre el motivo de un ciclo anterior. **La condición NO se escribe aquí: se le pregunta a su dueño.** «El motivo del rechazo aplica si y solo si el sentido es `RECHAZAR`» es una sola decisión y su dueño único en servidor es la función estática `ReglasAnulacionMatricula.esRechazo(…)` de la raíz de la versión (§9.0.3, §4 y §11); este trigger, el validador (§10.2) y las **dos** expresiones del PDF de la resolución (la fórmula de estimación/desestimación y el motivo, §5 y §14 nota 5) la **consultan**, y ninguno de los cuatro vuelve a comparar contra el enum. **MUST NOT** escribirse aquí `sentidoRevision != RECHAZAR` ni, mucho menos, `== ACEPTAR`: lo primero duplica al dueño y lo segundo, además, cambia el significado en cuanto exista un cuarto sentido. **MUST NOT** buscarse el predicado como método de la entidad (`expediente.esRechazo()`): el `<extra-code-model>` lo reescribe el build y por eso el dueño vive en la raíz de la versión (§9.0.3)
5. `LIMPIAR(textoSubsanacion)` — RN-010, sin condición (idempotente)
6. `SERVICIO(DevolucionDelDirector.borrar(expediente))` — RN-011, sin condición (idempotente)
7. `GENERAR_PDF(RESOLUCION)`
8. `CREAR_METAFILE → pdfResolucion` — RN-012 / CC-010 (sustituye a la de un envío anterior)
9. `UPDATE_STATE(States.Resolucion.PENDIENTE_FIRMA_DIRECTOR)`

**`triggerSubsanar`**

1. `ERROR_NEGOCIO(ControlDeAcceso.exigeMismoCentroQueElExpediente(expediente, "Solo puede revisar solicitudes de su propio centro"))` — VAL-PENDIENTE_REVISION-SUBSANAR-004 — y, a continuación, `ERROR_NEGOCIO(ControlDeAcceso.exigeOstentarElPerfilDelEstado(expediente, "Solo la secretaría del centro puede revisar esta solicitud"))` — **guarda de perfil del diseño** (§11, «Las ocho guardas de identidad»), por el mismo motivo y con la misma forma —**sin literal de perfil**, preguntándole a `States` el que declara `PENDIENTE_REVISION`—: sin ella el administrador podría devolver al alumno la solicitud de otro para subsanarla
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


## `### 10.2 Fase REVISION` (verbatim, ÍNTEGRA)

### 10.2 Fase REVISION

```kotlin
package com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.revision

import com.educaflow.subsystem.expedientes.db.AnulacionMatriculaCicloFormativoV1 as model
import com.educaflow.subsystem.expedientes.db.SentidoRevisionAnulacionMatriculaCicloFormativoV1 as SentidoRevision
import com.educaflow.base.infrastructure.validation.rules.NoAdmitido
import com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.ReglasAnulacionMatricula
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
    +ifValueIn(ReglasAnulacionMatricula::esRechazo, listOf(true)) {
        +Required("Debe indicar el motivo del rechazo")
        +MinLength(10, "El motivo del rechazo debe tener entre 10 y 1000 caracteres")
        +MaxLength(1000, "El motivo del rechazo debe tener entre 10 y 1000 caracteres")
    }
}
field(model::getTextoSubsanacion) {
}
```

- **La rama de `motivoRechazo` pregunta al dueño, no reescribe la condición.** `ifValueIn(ReglasAnulacionMatricula::esRechazo, listOf(true))` consulta la función estática `esRechazo(…)` de la raíz de la versión (§9.0.3, §4 y §11), que es el dueño único en servidor de «cuándo aplica el motivo del rechazo»; **MUST NOT** volver a escribirse aquí `ifValueIn(model::getSentidoRevision, listOf(SentidoRevision.RECHAZAR))`, ni buscarse el predicado como método de la entidad (`model::esRechazo`), que el build borraría del `<extra-code-model>`. El DSL lo admite sin cambios: `IfValueIn` recibe un `KFunction<*>` cualquiera y lo invoca con `dependentField.call(bean)`, así que una función de **un** parámetro —el expediente— vale igual que un getter; `ReglasAnulacionMatricula` es un `object`, de modo que `ReglasAnulacionMatricula::esRechazo` es ya una referencia ligada al objeto cuyo único argumento es el bean. Y **no** toca la whitelist del evento: `AllowPropertiesFactory` solo recoge los `field(...)`, nunca el campo del que depende una rama, así que `esRechazo` —que además no es una propiedad— no entra en `AllowProperties`. La rama de `sentidoRevision` de este mismo método sigue usando `getSentidoRevision`, porque expresa **otra** decisión (que `SUBSANAR` no se admite en este evento), no la del motivo del rechazo.
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


## `### 10.4 Frontera de confianza` (verbatim)

### 10.4 Frontera de confianza

- Ningún `field(...)` menciona un campo clasificado `servidor` en §4 — ni `codePhase`, `codeState`, `abierto`, `centro` o `usuarioRegistrador`, ni ninguno de los cinco `MetaFile` que rellena el servidor, ni las fechas y autores de revisión, devolución y resolución. La **única excepción** es `pdfSolicitudFirmada`, que **también está clasificado `servidor`** y aparece en el validador únicamente porque es el destino de la firma en cliente y ese `field(...)` es el único sitio donde se puede comprobar la firma (§4).
- Cada campo editable en la vista de un estado aparece en el `field(...)` del evento que se dispara desde esa vista: `nia`, `direccion`, `telefono`, `poblacion`, `provincia`, `codigoPostal` y `ciclo` en `CONTINUAR`; `claveCertificado` y `pdfSolicitudFirmada` en `PRESENTAR`; `sentidoRevision`, `motivoRechazo` y `textoSubsanacion` en `ENVIAR_A_FIRMA` y en `SUBSANAR`; `motivoDevolucion` en `DEVOLVER`.
- **CRITICAL** — esta puerta **NO** protege el endpoint REST automático `POST /ws/rest/com.educaflow.subsystem.expedientes.db.AnulacionMatriculaCicloFormativoV1`, que Axelor publica para toda entidad y que **no pasa por el `Tramitador`**. Son dos puertas distintas: **MUST NOT** darse por protegida esta entidad porque su tramitación valide por evento, y **MUST NOT** introducirse un `ModelService` deny-all como parche (se retiró a propósito, ver `CLAUDE.md`).
- `readonly`, `showIf` y `hidden` de las vistas son **UX, nunca defensa**.


## `### 9.0 Clases auxiliares compartidas de la versión` — REFERENCIA de API (verbatim)

**Nota del descomponedor:** estas tres clases **ya las implementa la tarea 09**; van aquí solo como referencia de su API, porque los `trigger*` y el validador de esta fase las llaman. **MUST NOT** reimplementarlas, duplicarlas ni modificarlas en esta tarea.

### 9.0 Clases auxiliares compartidas de la versión

**Tres** piezas sin estado en la **raíz de la versión** (fuera de las carpetas de fase), a las que los `trigger*` de las tres fases —y, la tercera, también el validador y los documentos PDF— **delegan**: dos clases `final` Java con constructor privado (§9.0.1 y §9.0.2) y un `object` Kotlin (§9.0.3), este último en el mismo fichero `ReglasAnulacionMatricula.kt` que la regla del DSL de §10.0. **MUST NOT** convertirse ninguna en una superclase: el dispatcher usa `getDeclaredMethods()` (§9.1.4).

**CRITICAL — la raíz de la versión es el sitio, y el `<extra-code-model>` del `domains.xml` NO lo es.** Todo código propio del tipo que no quepa en un `trigger*`, en un `onEnter*` o en el validador vive en una de estas clases. **MUST NOT** escribirse dentro del `<extra-code-model>`: el build lo **reescribe entero** en cada compilación (`RichDomainXmlTask` → `DomainXmlFile.addExtraCodeToDomainXml`), de modo que solo sobreviven ahí el enum `TipoDocumentoPdf` y `getDocumentoPdf`, que son lo que genera su plantilla.

#### 9.0.1 `ControlDeAcceso`

Fichero `…/v1/ControlDeAcceso.java`. Concentra las comprobaciones de **quién es el usuario autenticado**: si es el creador, si actúa sobre un expediente de su propio centro y si **ostenta el perfil** que el estado declara. La usan los **ocho** `trigger*` de las tres fases como **guarda en su primera línea**; ninguna vive en el validador (ver §11 y `decisiones.md` D4 y D13).

```java
public final class ControlDeAcceso {
    public static void exigeSerElCreador(AnulacionMatriculaCicloFormativoV1 expediente, String mensaje) throws BusinessException;
    public static void exigeMismoCentroQueElExpediente(AnulacionMatriculaCicloFormativoV1 expediente, String mensaje) throws BusinessException;
    public static void exigeOstentarElPerfilDelEstado(AnulacionMatriculaCicloFormativoV1 expediente, String mensaje) throws BusinessException;
}
```

- `exigeSerElCreador` compara el `id` de `expediente.getUsuarioRegistrador()` con el de `SecurityUtil.getUser()`; si no coinciden (o alguno es nulo) lanza `new BusinessException(I18n.get(mensaje))`.
- `exigeMismoCentroQueElExpediente` compara el `id` de `expediente.getCentro()` con el de `SecurityUtil.getUser().getCentroActivo()`; si no coinciden (o alguno es nulo) lanza `new BusinessException(I18n.get(mensaje))`.
- `exigeOstentarElPerfilDelEstado` **no recibe ningún perfil: lo pregunta**. Son **dos** preguntas seguidas, cada una a su dueño, y la guarda no responde ninguna de las dos por su cuenta:
  1. **«¿Qué perfil atiende este estado?»** → su dueño es el atributo `profile` del `<state>` del `TipoExpedienteInstance.xml`, proyectado en la clase generada `States`. Se consulta con `States.INSTANCE.getState(expediente.getCodePhase(), expediente.getCodeState())` y, sobre el `State` que devuelve, `getProfile()` — **exactamente** la misma llamada que hace `Tramitador.checkPerfilDelEstado` (`State.getProfile()`, `subsystem/expedientes/services/eventmanager/State.java`). **MUST NOT** escribirse aquí ningún literal de perfil: si mañana `PENDIENTE_REVISION` pasa de `profile="SECRETARIO"` a otro valor, la guarda exige el nuevo sin que nadie toque el trigger, igual que le pasa al motor.
  2. **«¿Ostenta este usuario ese perfil sobre este expediente?»** → su dueño es `PerfilesUsuarioService.getPerfilesSobreExpediente(expediente, SecurityUtil.getUser())` (`subsystem/security`), **el mismo** al que pregunta `checkPerfilDelEstado` para autorizar cada evento y el punto de consulta de las dos bandejas nuevas (Paso 16.0): la comprobación es `getPerfilesSobreExpediente(…).contains(perfilDelEstado.name()) == false → lanza`. **MUST NOT** reescribirse aquí ninguna consulta sobre `Ace`, `CentroUsuarioTipoUsuario` ni `CentroUsuarioCargo`: sería una cuarta escritura del dueño.

  Si la respuesta a (2) es que no, lanza `new BusinessException(I18n.get(mensaje))`.
  - **Estado sin `profile`: no exige nada y devuelve el control**, que es literalmente lo que hace el motor («Un estado **sin perfil** no exige ninguno», `checkPerfilDelEstado`). Hoy los tres estados con eventos declaran perfil, así que esa rama no se ejecuta en ningún `trigger*` de este tipo; se escribe para que la guarda siga diciendo lo mismo que el motor el día en que un estado deje de declararlo, en vez de fallar cerrado sobre una exigencia que el XML ya no hace.
  - **La pareja `(codePhase, codeState)` que no resuelva a ningún estado es un error de programación, no un caso de negocio**: `States.INSTANCE.getState(…)` devuelve `Optional<State>` y, si viene vacío, la guarda lanza `IllegalStateException` (no `BusinessException`). No puede ocurrir por el camino normal — el `Tramitador` acaba de resolver ese mismo estado para autorizar el evento y llegar hasta el trigger—, y **MUST NOT** tratarse como «no hay perfil que exigir»: eso sería el retorno defensivo que abre la puerta justo cuando falta el dato con el que decidir.
  - **`States` no se importa**: la clase generada de esta versión vive en el **mismo paquete** que `ControlDeAcceso` (`…v1`). Lo que sí usa el fichero son `com.educaflow.subsystem.expedientes.services.eventmanager.State` y `com.educaflow.subsystem.expedientes.db.Profile`, los dos como **tipos internos** del método; ningún llamante los nombra.
  - El servicio se resuelve dentro del método con `Beans.get(PerfilesUsuarioService.class)` —su binding ya existe (`SecurityModule`), así que **MUST NOT** tocarse ningún módulo Guice—, igual que hace `CertificadoDigitalHelper` (`subsystem/criptografia/util`), que es el precedente del proyecto para un helper estático que necesita un bean. Así las tres guardas se llaman igual desde los ocho `trigger*` y ningún `PhaseEventManagerImpl` gana un `@Inject` solo para esto.
  - **NO exime al administrador, y ese es exactamente su motivo de existir**: `Tramitador.checkPerfilDelEstado` hace `return` para el administrador en **cualquier** estado (`SecurityUtil.isAdmin`, `subsystem/expedientes/services/tramitacion/Tramitador.java` línea 232), así que el perfil que el estado declara **no** es una barrera para él (§11, §14 nota 25). La guarda es, por tanto, **la misma exigencia del motor sin la exención**: pregunta lo mismo, a los mismos dos dueños, y la única diferencia es que no hace `return` para el administrador. Esa es también la razón de que su nombre diga «el perfil del estado» y no un perfil concreto — y de que **MUST NOT** ganar nunca un parámetro de perfil: en cuanto lo tuviera, el trámite tendría una segunda opinión sobre qué perfil atiende cada estado.
  - **Es complementaria de `exigeMismoCentroQueElExpediente`, no la sustituye ni la duplica**, y por eso los cuatro `trigger*` de `REVISION` y `RESOLUCION` llevan las **dos**: los `Ace` que el data-init crea a partir de un tipo de usuario o de un cargo llevan `centro` **a nulo**, y `AceRepository.findNombresPerfilesByExpediente` los acota con `aa.centro IS NULL OR aa.centro = :centro` contra el **centro activo del usuario**, nunca contra el **centro del expediente**. Es decir: la administrativa de otro centro **sí** ostenta `SECRETARIO` sobre un expediente ajeno, y quien la para es la guarda de centro (§14 nota 9). Al revés, el administrador **sí** pasa la guarda de centro en cuanto su centro activo es el del expediente, y quien le para es la guarda de perfil. Ninguna de las dos sabe nada de la otra.
- **MUST** usarse `SecurityUtil.getUser()`, nunca `AuthUtils.getUser()`.
- Las tres son **totales**: para cualquier entrada, o dejan pasar o fallan con motivo; no devuelven «vale» ante un dato ausente. En `exigeOstentarElPerfilDelEstado` eso incluye el usuario sin sesión, sin centro activo o sin `centroUsuarioActivo`: el dueño de (2) devuelve entonces un conjunto vacío y la guarda **falla cerrado**, igual que el punto de consulta del Paso 16.0. El estado sin `profile` **no** es una excepción a esto: ahí no hay ninguna exigencia que evaluar porque el XML no la declara, no un dato que falte.
- El mensaje llega por parámetro porque la especificación fija uno distinto en cada evento; la **decisión** (quién puede) está en un solo sitio.
- **Por qué `exigeSerElCreador` sigue existiendo aparte y NO se funde con la guarda de perfil.** La tentación es evidente: los dos estados de `SOLICITUD` declaran `profile="CREADOR"` y `PerfilesUsuarioServiceImpl.getPerfilesSobreExpediente` añade `CREADOR` **por autoría** (compara `expediente.getUsuarioRegistrador().getId()` con el del usuario), así que hoy las dos guardas dejan pasar exactamente a la misma persona. No se funden por dos motivos, y el primero es de código real, no de estilo: `AceRepository.findNombresPerfilesByExpediente` excluye `CREADOR` **solo en la rama del trámite** (`aa.tramite = :tramite AND aa.perfil.name <> 'CREADOR'`); las ramas `aa.expediente = :expediente` y `aa.tipoExpediente = :tipoExpediente` **no lo excluyen**, y el data-init tiene bindings para crear esos `Ace` (`asignacionesTipoUsuarioTipoExpediente` y `asignacionesCargoTipoExpediente` de `data-demo/input-config.xml`). Una sola fila con `perfilName="CREADOR"` y `tipoExpedienteCode` haría que «ostenta CREADOR» dejara de significar «es el autor» — y la guarda de `DELETE` pasaría a dejar borrar la solicitud de otro **sin que nadie tocara el trigger**. Hoy no ocurre porque `permisos.xml` asigna `CREADOR` por `tramiteCode`, que es justo la rama que lo excluye; pero eso es una propiedad del **dato**, no del código. El segundo motivo es que la spec enuncia VAL-DATOS_SOLICITUD-CONTINUAR-016 y VAL-DATOS_SOLICITUD-BORRADO-001 sobre la **autoría** («sus propias solicitudes»), cuyo dueño es `Expediente.usuarioRegistrador`, no sobre el perfil que atiende el estado: fundirlas le daría a la autoría un segundo dueño —el data-init de permisos— en vez de quitarle uno. El razonamiento completo, con las dos alternativas, está en `decisiones.md` **D13**.

#### 9.0.2 `DevolucionDelDirector`

Fichero `…/v1/DevolucionDelDirector.java`, con la misma forma que `ControlDeAcceso`. Es el **dueño único** de qué campos forman «la devolución del director»: la terna `motivoDevolucion` + `fechaDevolucion` + `devueltoPor`, que cuatro `trigger*` de tres fases distintas tienen que dejar limpia.

```java
public final class DevolucionDelDirector {
    public static void borrar(AnulacionMatriculaCicloFormativoV1 expediente);
}
```

- **Por qué se llama así.** La clase nombra **la cosa** de la que es dueña —la devolución que el director hace a secretaría— y el método dice **qué le hace**: `DevolucionDelDirector.borrar(expediente)` se lee entero en la línea de llamada, sin abrir la clase. **MUST NOT** nombrarse con una palabra del vocabulario del dominio de este trámite —`ciclo`, `Ciclo`, `grado`, `nivel`, `matrícula`— porque aquí «ciclo» es siempre **el ciclo formativo** que se anula (campo `ciclo`, entidad `Ciclo`, panel «Matrícula que se anula», regla `SinOtraSolicitudEnCursoParaElMismoCiclo`): un nombre como `CicloDeRevision` se leería espontáneamente como «algo del ciclo formativo» y obligaría a abrir el fichero para descubrir que no lo es.
- `borrar` pone a `null` `motivoDevolucion`, `fechaDevolucion` y `devueltoPor`. Nada más: no transiciona, no valida y no lanza.
- Es **idempotente** y **total**: si el expediente no venía de una devolución, esos campos ya están vacíos y volver a limpiarlos no cambia nada, así que ningún llamante necesita preguntarse si le toca.
- Lo llaman `triggerPresentar` (RN-007), `triggerEnviarAFirma` (RN-011), `triggerSubsanar` (RN-015) y `triggerFirmar` (RN-020). **El motivo de que exista es exactamente ese**: sin él la lista de los tres campos estaría escrita literalmente en cuatro sitios y quien añadiera un cuarto dato a la devolución tendría que acordarse de los cuatro, fallando en silencio si se dejara uno.
- **MUST NOT** añadírsele un segundo método para limpiar la decisión de secretaría (ni equivalente): esa limpieza tiene **un solo** punto de llamada y, además, no habla de la devolución del director, que es lo único de lo que esta clase es dueña. Una operación entra aquí cuando trata de la devolución del director **y** la comparten **dos o más** triggers.

#### 9.0.3 `ReglasAnulacionMatricula.esRechazo` — el dueño de «la revisión rechaza la anulación»

Fichero `…/v1/ReglasAnulacionMatricula.kt` (el mismo de la regla del DSL de §10.0, que es Kotlin y vive también en la raíz de la versión). Es el **dueño único en servidor** de la clasificación «la revisión rechaza si y solo si `sentidoRevision == RECHAZAR`», de la que se siguen sus dos consecuencias: la resolución **desestima** y el **motivo del rechazo** aplica.

```kotlin
object ReglasAnulacionMatricula {
    @JvmStatic
    fun esRechazo(expediente: AnulacionMatriculaCicloFormativoV1): Boolean =
        expediente.sentidoRevision == SentidoRevisionAnulacionMatriculaCicloFormativoV1.RECHAZAR
}
```

- **Por qué aquí y no en el `<extra-code-model>` de la entidad.** Ese bloque lo **reescribe entero el build** en cada compilación: `RichDomainXmlTask` (`MainModelXml` → `DomainXmlFile.addExtraCodeToDomainXml`) localiza la entity con `extends="Expediente"`, sustituye su `<extra-code-model>` por el que genera `extra-code-domain-xml.template` —que solo emite el enum `TipoDocumentoPdf` y `getDocumentoPdf`— y reescribe el `domains.xml` en `src/main/java`. Un método escrito ahí **desaparece en el primer build**, y con él se caen sus cuatro consumidores: el `trigger*` y el validador dejan de compilar y las dos expresiones Groovy del PDF fallan **en silencio** (log + campo vacío), dejando la resolución sin fórmula y sin motivo (ESC-014, ESC-015, ESC-030). Por eso el predicado vive en una clase de la raíz de la versión, que el build no toca.
- **`@JvmStatic` es obligatorio**, porque hay tres tipos de consumidor: Java (`ReglasAnulacionMatricula.esRechazo(expediente)` en el `trigger*`), el DSL del validador (`ifValueIn(ReglasAnulacionMatricula::esRechazo, listOf(true))`, que `IfValueIn` admite porque hace `dependentField.call(bean)` sobre cualquier `KFunction`: una función de un parámetro vale igual que un getter) y Groovy, en las dos expresiones del documento, que la invocan por su **FQCN** porque en un `${…}` no hay `import`: `com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.ReglasAnulacionMatricula.esRechazo(self)`.
- Es **total** y sin estado: para cualquier expediente devuelve `true` o `false` y no lanza; con el sentido sin elegir devuelve `false`, que es lo que sus cuatro consumidores esperan.
- Imports del fichero: la entidad `AnulacionMatriculaCicloFormativoV1` y el enum `SentidoRevisionAnulacionMatriculaCicloFormativoV1`, los dos de `com.educaflow.subsystem.expedientes.db` (el paquete de la clase generada), además de los que ya necesita la regla del DSL de §10.0.
- Lleva el porqué en su KDoc: que es el dueño único en servidor de la clasificación, cuáles son sus cuatro consumidores, y que **MUST NOT** moverse al `<extra-code-model>` porque el build lo reescribe.
- **MUST NOT** aparecer en ninguna `AllowProperties`: no es una propiedad de la entidad, así que el DSL no lo mete en la whitelist del evento (`AllowPropertiesFactory` solo recoge los `field(...)`).
- **MUST NOT** duplicarse la comparación contra el enum en ninguno de los cuatro consumidores (§11), ni convertirse el predicado en un campo `transient` con `depends` (`decisiones.md` D10).


## `## 11. Reparto de reglas` — tabla de reparto, dueño de «la revisión rechaza», guardas de identidad y filas que aplican a esta fase (verbatim)

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

**Dueño único de «la revisión rechaza la anulación» — un predicado en el servidor, no una convención de redacción.** La decisión es una sola —*la revisión rechaza si y solo si `sentidoRevision == RECHAZAR`*, de donde se siguen sus dos consecuencias: la resolución **desestima** la solicitud y el **motivo del rechazo** aplica— y su **dueño** es la función estática `ReglasAnulacionMatricula.esRechazo(expediente)`, declarada en `…/v1/ReglasAnulacionMatricula.kt`, en la raíz de la versión (§9.0.3 y §4). **MUST NOT** declararse en el `<extra-code-model>` del `domains.xml`: el build reescribe ese bloque entero en cada compilación y el predicado desaparecería, dejando sin compilar al trigger y al validador y en silencio a las dos expresiones del PDF. Los **cuatro** puntos que se evalúan en el **servidor** (en tres ficheros: `resolucion.xml` lo consulta dos veces, una por cada consecuencia) no la escriben: se la **preguntan**.

| Sitio | Capa | Cómo consulta al dueño |
|---|---|---|
| acción 4 de `triggerEnviarAFirma` (§9.2) | Java, servidor | `if (ReglasAnulacionMatricula.esRechazo(expediente) == false) { … }` |
| rama de `motivoRechazo` en `getForStatePendienteRevisionInEventEnviarAFirma` (§10.2) | DSL del validador, servidor | `ifValueIn(ReglasAnulacionMatricula::esRechazo, listOf(true)) { … }` |
| expresión de la **fórmula** de estimación/desestimación en `documentospdf/resolucion.xml` (§5, §14 nota 5) | Groovy, servidor | `${<FQCN>.esRechazo(self) ? "…desestima…" : "…estima…"}`, con `<FQCN>` = `com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.ReglasAnulacionMatricula` (en un `${…}` no hay `import`) |
| expresión del **motivo** en `documentospdf/resolucion.xml` (§5) | Groovy, servidor | `${<FQCN>.esRechazo(self) ? … : ""}`, con el mismo `<FQCN>` |

**MUST NOT** aparecer en ninguno de esos cuatro una comparación contra el enum (`sentidoRevision == RECHAZAR`, `!= RECHAZAR` y, mucho menos, `== ACEPTAR`): eso devolvería la decisión a varios dueños. Que el DSL lo admita no es una suposición — `IfValueIn` recibe un `KFunction<*>` y lo invoca con `dependentField.call(bean)`, así que un método sin argumentos vale igual que un getter; y `AllowPropertiesFactory` solo recoge los `field(...)`, nunca el dependiente de una rama, así que consultar el predicado **no** mete nada en la whitelist del evento.

**Los TRES literales de cliente sobre el RECHAZO, y por qué son tres y no dos.** Son exactamente los que se evalúan **en el navegador, sobre el registro del formulario sin guardar**, mientras el usuario está cambiando `sentidoRevision`. **Esta lista es el inventario completo, y hay que mirarla entera**: **MUST NOT** existir en el diseño ningún literal de cliente sobre `sentidoRevision` que no esté aquí o en el párrafo «El par simétrico de `SUBSANAR`» de más abajo, y quien añada uno **MUST** añadirlo a la lista que le corresponda.

1. el `showIf="sentidoRevision=='RECHAZAR'"` de los paneles que enseñan el motivo del rechazo (`revision`, `decision-secretaria` y `resolucion-firmada` del `views.xml` de la raíz, Paso 7), con el `requiredIf="sentidoRevision=='RECHAZAR'"` de `motivoRechazo` en `revision` — RUI-PENDIENTE_REVISION-SECRETARIO-004 y las RUI de las pantallas que muestran la decisión;
2. el `if="sentidoRevision != 'RECHAZAR'"` del primer `<field>` del `<action-record>` `…-limpiar-revision-action` (`fases/revision/views.xml`, Paso 12) — RUI-PENDIENTE_REVISION-SECRETARIO-009, y
3. el `showIf="sentidoRevision=='ACEPTAR'"` del `<help>` «Si firma la resolución, la anulación surtirá efecto desde esta fecha» del panel `matricula-director` del `views.xml` de la raíz de la versión (Paso 7) — **RUI-PENDIENTE_FIRMA_DIRECTOR-DIRECTOR-007**, cuya condición la especificación fija literalmente como «el sentido de la revisión es *Aceptar la anulación*».

Ahí el predicado del servidor **no sirve**: `ReglasAnulacionMatricula.esRechazo(…)` se calcula sobre el estado persistido y no está recalculado mientras el usuario teclea, y convertirlo en un `transient` con `depends` para que el cliente lo recibiera crearía justo lo que se quiere evitar — un campo del modelo en el que el cliente no puede confiar (`decisiones.md` D10). Por eso el dueño es una **función** y no un campo: no lo ve ninguna vista.

**El par simétrico de `SUBSANAR`, que proyecta OTRA clasificación y por eso no tiene dueño en servidor.** `showIf="sentidoRevision=='SUBSANAR'"` y `requiredIf="sentidoRevision=='SUBSANAR'"` en `textoSubsanacion` (panel `revision`, Paso 7) y el `if="sentidoRevision != 'SUBSANAR'"` del **segundo** `<field>` del `<action-record>` `…-limpiar-revision-action` (Paso 12) — RUI-PENDIENTE_REVISION-SECRETARIO-006 y -010. No dicen «la revisión rechaza» sino «la revisión pide subsanación», que es una clasificación **distinta** y que **ningún sitio de servidor consulta**: en el servidor la subsanación no se pregunta, se **dispara** — es el evento `SUBSANAR`, cuyo `trigger*` y cuyo método del validador ya son de ese caso y no tienen nada que clasificar (§9.2 y §10.2). Por eso no hay un `esSubsanacion()`: crearlo sería un predicado sin consumidores. Se formulan alrededor de su propio valor, `SUBSANAR`, por la misma razón que los del rechazo lo hacen alrededor de `RECHAZAR`, y quedan inventariados aquí para que un cuarto sentido de revisión los haga revisar también.

**La convención de redacción del cliente, y su única excepción declarada.** Los literales que proyectan la clasificación del **dueño de servidor** —*la revisión rechaza*— se formulan **siempre alrededor de `RECHAZAR`** (`== 'RECHAZAR'` o `!= 'RECHAZAR'`) y **MUST NOT** formularse alrededor de `ACEPTAR`, para que añadir un cuarto sentido no le cambie el significado a nadie sin que se note. Eso cubre los sitios **1** y **2**. El sitio **3 es la excepción, y lo es porque no proyecta esa clasificación sino otra distinta**: no habla del rechazo ni del motivo, sino de cuándo la anulación surtirá efecto, y la especificación fija su condición como «el sentido de la revisión es *Aceptar la anulación*» (RUI-PENDIENTE_FIRMA_DIRECTOR-DIRECTOR-007). Reescribirlo como `!= 'RECHAZAR'` **cambiaría lo que dice la spec** —lo enseñaría también con `SUBSANAR` y con el sentido vacío—, así que **MUST** quedarse alrededor de `ACEPTAR`. La convención, por tanto, no es universal sobre el campo: **es universal sobre la clasificación del dueño de servidor**, y el sitio 3 queda declarado aquí como el único literal de cliente que no la sigue, con su motivo.

**Qué hay que tocar si se añade un cuarto sentido de revisión:** la función `ReglasAnulacionMatricula.esRechazo(…)` —que es donde vive la clasificación de servidor— y, si el nuevo sentido cambiara lo que ve el usuario, los **tres** literales de cliente de la lista de arriba, **los tres**: el `showIf` de los paneles del motivo (1), el `if` del `<action-record>` (2) y el `showIf` del `<help>` de `matricula-director` (3) — este último es el que más fácilmente se olvida, porque está formulado alrededor de `ACEPTAR` y vive en el `views.xml` de la raíz, no en el de una fase. Y, si el nuevo sentido tocara además la subsanación, el par simétrico de `SUBSANAR` del párrafo anterior. Los **cuatro** consumidores de servidor no se tocan. Ese reparto —un dueño en servidor y tres literales de cliente declarados, dos de ellos siguiendo la convención y el tercero con su excepción escrita— es el que registra `decisiones.md` **D10**.

   **Y para que los literales 1 y 3 puedan evaluarse, el campo tiene que estar en el form:** los paneles que condicionan por `sentidoRevision` lo **pintan ellos mismos** —`revision` y `decision-secretaria` visible, `resolucion-firmada` y `matricula-director` con `hidden="true"`—, porque una condición de vista que cita un campo que ningún panel del form pinta sale siempre falsa en silencio (invariante del Paso 7 y §14 nota 29). Eso **no** añade ningún sitio de cliente más: el campo oculto no formula la condición, solo trae el dato con el que se evalúa.

**Las ocho guardas de identidad: cuatro de autoría y cuatro de perfil, y por qué las ocho.** La exención del administrador es **global** —`Tramitador.checkPerfilDelEstado` hace `return` para él en **cualquier** estado (`SecurityUtil.isAdmin`, `Tramitador.java` línea 232)—, así que el `profile` del estado no basta como barrera en **ninguno** de los tres estados con eventos. Por eso **los ocho eventos del tipo llevan en su acción 1 una guarda que comprueba en el servidor quién dispara**: los cuatro de los estados `CREADOR` (`CONTINUAR`, `DELETE`, `VOLVER`, `PRESENTAR`) con `ControlDeAcceso.exigeSerElCreador`, y los cuatro de los estados `SECRETARIO` y `DIRECTOR` (`ENVIAR_A_FIRMA`, `SUBSANAR`, `FIRMAR`, `DEVOLVER`) con `ControlDeAcceso.exigeOstentarElPerfilDelEstado(expediente, mensaje)`, **además** de la guarda de centro que la especificación sí numera. Sin las cuatro de perfil, el administrador —que pasa la guarda de centro en cuanto su centro activo es el del expediente, y el Paso 15 le asigna precisamente CIPFP Mislata— podría revisar, pedir subsanación, devolver y, sobre todo, **firmar**: cerrar el expediente con la resolución firmada con el certificado del Director y registrada de salida. Eso contradice HU-010/ESC-025 igual que lo contradecía el caso de `VOLVER`/`PRESENTAR`, y es el mismo olor **«ramas que no cubren todos los casos»** de `k-code-quality`: la guarda de centro deja fuera exactamente al único usuario al que el motor exime. **Las cuatro de perfil no escriben ningún perfil: preguntan por los dos lados.** «Qué perfil atiende este estado» tiene un dueño único y ya existente —el atributo `profile` del `<state>` del `TipoExpedienteInstance.xml`, proyectado en la clase generada `States`—, y «si el usuario lo ostenta» tiene otro —`PerfilesUsuarioService.getPerfilesSobreExpediente`—; la guarda consulta a los dos, exactamente como hace `Tramitador.checkPerfilDelEstado` (`State.getProfile()` + el conjunto de perfiles), y su única diferencia con el motor es que **no exime al administrador**. Por eso se llama `exigeOstentarElPerfilDelEstado` y **no recibe ningún parámetro `Profile`**: si lo recibiera, cambiar `profile="SECRETARIO"` por otro valor en el XML dejaría a las dos guardas de `PENDIENTE_REVISION` exigiendo el perfil viejo —divergiendo **en silencio** de lo que el motor exige a todo el mundo menos al administrador, que es justo el caso que la guarda existe para cubrir— y ningún test del build lo vería. **MUST NOT** reintroducirse el literal, ni como argumento ni como constante del `PhaseEventManagerImpl`. La decisión, con sus alternativas, está en `decisiones.md` **D13**.

**Las cuatro guardas de autoría, y por qué son cuatro y no dos.** `CONTINUAR`, `DELETE`, `VOLVER` y `PRESENTAR` —los cuatro eventos de los dos estados cuyo `profile` es `CREADOR`— llevan **todos** la guarda `ControlDeAcceso.exigeSerElCreador` en su primera línea. Las dos primeras las pide la especificación (VAL-DATOS_SOLICITUD-CONTINUAR-016 y VAL-DATOS_SOLICITUD-BORRADO-001); las dos últimas las **añade el diseño**, y la razón es la misma que hace falta para las dos primeras: `Tramitador.checkPerfilDelEstado` **exime al administrador** (`SecurityUtil.isAdmin`), así que el perfil del estado no basta para garantizar que quien dispara el evento sea el creador. Sin ellas, un administrador que abriera la solicitud de otro alumno por «Expedientes Pendientes» —que fija `_profile=CREADOR`— podría disparar `VOLVER` y `PRESENTAR` sobre ella, contra HU-010/ESC-025 (el administrador solo consulta, en solo lectura) y ESC-020/ESC-032/ESC-045 (los no creadores ven la pantalla en solo lectura). **Y siguen siendo una guarda distinta de la de perfil, no un caso particular suyo:** aunque los dos estados declaren `profile="CREADOR"` y el dueño de los perfiles conceda `CREADOR` por autoría, «ser el autor» y «ostentar el perfil que el estado declara» solo coinciden mientras ninguna fila de permisos asigne `CREADOR` por tipo de expediente o por expediente —las dos ramas de `findNombresPerfilesByExpediente` que **no** lo excluyen—, y la spec enuncia estas dos reglas sobre la autoría, cuyo dueño es `Expediente.usuarioRegistrador` (§9.0.1 y `decisiones.md` D13). **Esto anula expresamente la indicación de `design-guidelines.md`** («no se han añadido comprobaciones de autoría a VOLVER ni a PRESENTAR: serían una copia de lo que el motor ya garantiza»): el motor **no** lo garantiza para el administrador, que es exactamente el caso abierto, y la propia guía reconoce esa excepción al justificar por qué sí se conservan las otras dos. La desviación queda declarada aquí y en §14 nota 25.

**Por qué las comprobaciones de identidad no van al validador.** El DSL cuelga cada regla de un campo, y esas comprobaciones —autoría, centro y perfil— no hablan de ningún campo. Además, dos de ellas caen donde el validador no llega o no debe crecer: `DELETE` se salta la validación entera en el `Tramitador` (su método del validador no existe y nunca se invocaría), y `FIRMAR` no admite ningún dato del formulario, así que darle un `field(...)` solo para colgar la regla metería ese campo en la whitelist del evento. La regla de reparto queda enunciable en una frase: **si la regla habla del valor de un campo, validador; si habla de quién eres, guarda del trigger**. Las cuatro guardas de perfil caen en la misma frase y por el mismo lado: preguntan quién eres, no qué has enviado. Ver `decisiones.md` D4 y D13.

| Regla | Capa |
|---|---|
| VAL-PENDIENTE_REVISION-ENVIAR_A_FIRMA-001 … 004 | DSL del validador, `getForStatePendienteRevisionInEventEnviarAFirma` |
| VAL-PENDIENTE_REVISION-ENVIAR_A_FIRMA-005 | guarda de centro de `triggerEnviarAFirma` (`ControlDeAcceso.exigeMismoCentroQueElExpediente`) |
| *(sin identificador en la spec)* — perfil en `ENVIAR_A_FIRMA` | guarda de `triggerEnviarAFirma` (`ControlDeAcceso.exigeOstentarElPerfilDelEstado(…)`, que exige el perfil que `PENDIENTE_REVISION` declara —hoy `SECRETARIO`— sin repetirlo). **La añade el diseño**, no la spec: el motor exime al administrador del perfil del estado y la guarda de centro no le para (§11, «Las ocho guardas de identidad», §14 nota 25 y `decisiones.md` D13) |
| VAL-PENDIENTE_REVISION-SUBSANAR-001 … 003 | DSL del validador, `getForStatePendienteRevisionInEventSubsanar` |
| VAL-PENDIENTE_REVISION-SUBSANAR-004 | guarda de centro de `triggerSubsanar` (`ControlDeAcceso.exigeMismoCentroQueElExpediente`) |
| *(sin identificador en la spec)* — perfil en `SUBSANAR` | guarda de `triggerSubsanar` (`ControlDeAcceso.exigeOstentarElPerfilDelEstado(…)`, mismo estado y mismo perfil declarado). **La añade el diseño**, por el mismo motivo |
| RN-008 … RN-016 | acciones de `triggerEnviarAFirma` y `triggerSubsanar` (§9.2) |
| CC-005 … CC-013 | acciones de los `trigger*` (§9) |
| RUI-PENDIENTE_REVISION-SECRETARIO-001 … 006 y 009 … 014 | *(no existen las 007 y 008: la numeración de la spec salta de 006 a 009)* vista `PENDIENTE_REVISION` del `SECRETARIO` y `<action-record>` `…-limpiar-revision-action`. En concreto, la obligatoriedad visual del **002** es `required="true"` en `sentidoRevision` y la del **004** y el **006** son `requiredIf="sentidoRevision=='RECHAZAR'"` en `motivoRechazo` y `requiredIf="sentidoRevision=='SUBSANAR'"` en `textoSubsanacion`, las tres en el panel `revision` (que solo usa este form). La exigencia real está en el validador |
| RUI-PENDIENTE_REVISION-GENERICA-001 y 002 | vista genérica de `PENDIENTE_REVISION`: no incluye ningún panel de la decisión de secretaría. **NO está garantizado que todo el que no sea la administrativa caiga en ella**: la condición de perfil de la bandeja de secretaría (Paso 16.0) solo **quita el expediente de las dos bandejas nuevas**, es decir, corta el camino normal de navegación; no elige la vista. El `_profile` **viaja en la petición** y en el servidor **nada comprueba que el usuario ostente ese perfil** para elegir la vista (`ExpedienteController.viewExpediente` toma el `_profile` del contexto con `actionRequestHelper.getProfileName()` y solo lo pasa por `checkProfileDelTipoExpediente`, que comprueba que el perfil lo **use** algún estado del tipo; `PhaseEventManager.getViewName` devuelve entonces el form de ese perfil), así que cualquiera con lectura sobre el expediente puede invocar `subsysExpedientes-event-view-action` con `_profile=SECRETARIO` sin pasar por la bandeja. Es el residuo **NO mitigado** de §14 nota 8(d), y cerrarlo es del motor |
