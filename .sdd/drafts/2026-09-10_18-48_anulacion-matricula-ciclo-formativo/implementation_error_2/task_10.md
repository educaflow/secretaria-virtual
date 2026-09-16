---
type: implementation-task
template: expediente
---

# Tarea 10 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-tipo-expediente
- k-validaciones
- k-secure-coding
- k-code-quality
- k-i18n

## Qué hay que hacer

Materializar **los tres ficheros de la fase `SOLICITUD`** de la versión `v1`, en `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/solicitud/`:

1. `PhaseEventManagerImpl.java` (FQCN `…v1.solicitud.PhaseEventManagerImpl`) — se **escribe sobre el esqueleto** que dejó `CreateFilesTask`, siguiendo la subsección de `## 9` copiada abajo.
2. `StateEventValidatorImpl.kt` (paquete `…v1.solicitud`) — se **escribe sobre el esqueleto** que dejó `CreateFilesTask`, siguiendo la subsección de `## 10` copiada abajo.
3. `views.xml` — **NO se escribe**: se **copia literalmente** de `design/fases/solicitud/views.xml` a `…/v1/solicitud/views.xml`, **sobrescribiendo** el esqueleto. **MUST NOT** modificarlo, reescribirlo ni regenerarlo.

**La especificación del diseño es contrato fijo y la superficie es cerrada: MUST NOT crearse ningún método, clase, campo, acción, panel ni botón que la especificación no liste**, ni alterarse el orden de las acciones numeradas de un `trigger*`, ni los argumentos literales del DSL del validador.

Reglas duras de esta tarea:

- **MUST** usarse `SecurityUtil.getUser()` (`com.educaflow.base.util.SecurityUtil`), **NUNCA** `AuthUtils.getUser()`.
- El `import` de `States` **MUST** ser el de **esta misma versión** (`com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.States`), nunca el de otro tipo ni el de otra versión.
- **MUST NOT** factorizarse ningún `trigger*`/`onEnter*` en una superclase: el dispatcher usa `getDeclaredMethods()`. Lo común vive en `ControlDeAcceso` y `DevolucionDelDirector` (tarea 09), a las que se **delega**.
- **MUST NOT** declararse `triggerInitialEvent` ni `triggerExit` en un `PhaseEventManagerImpl`.
- El conjunto de `field(...)` de cada método del validador es la **whitelist de campos que el cliente puede dictar** en esa pareja (estado, evento): **MUST NOT** añadirse ninguno de más ni quitarse ninguno de los listados.

## Filas de la tabla `## 6. Ficheros a crear o modificar` del diseño para esta fase (verbatim)

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/solicitud/PhaseEventManagerImpl.java` | Crear | `k-tipo-expediente` | Lo especifica `## 9. Especificación de los PhaseEventManagerImpl` (fase `SOLICITUD`) |
| `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/solicitud/StateEventValidatorImpl.kt` | Crear | `k-tipo-expediente`, `k-secure-coding` | Lo especifica `## 10. Especificación de los StateEventValidatorImpl` (fase `SOLICITUD`) |
| `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/solicitud/views.xml` | Crear | `k-tipo-expediente` | Copia de `design/fases/solicitud/views.xml` |

## Pasos del diseño de esos tres ficheros (verbatim)

### Paso 10 — `PhaseEventManagerImpl.java` de cada fase

Ficheros: `…/v1/solicitud/PhaseEventManagerImpl.java`, `…/v1/revision/PhaseEventManagerImpl.java` y `…/v1/resolucion/PhaseEventManagerImpl.java` (FQCN = paquete de la fase + `.PhaseEventManagerImpl`).

Su especificación quirúrgica está en **`## 9. Especificación de los PhaseEventManagerImpl`**, con una subsección por fase.

Supertipo: `extends PhaseEventManager<AnulacionMatriculaCicloFormativoV1>`, con constructor `@Inject` que recibe `AnulacionMatriculaCicloFormativoV1Repository` y llama a `super(AnulacionMatriculaCicloFormativoV1.class)`.

**Verificación:** compilan; por cada fase hay **un** `@WhenEvent trigger<Evento>` por cada evento de su lista de cobertura (§9.4) y **un** `@OnEnterState onEnter<Estado>` por cada estado de esa lista, y ninguno de más; ningún `triggerInitialEvent` ni `triggerExit`; el `import` de `States` es el de esta misma versión.

### Paso 11 — `StateEventValidatorImpl.kt` de cada fase

Ficheros: `…/v1/solicitud/StateEventValidatorImpl.kt`, `…/v1/revision/StateEventValidatorImpl.kt` y `…/v1/resolucion/StateEventValidatorImpl.kt` (paquete de la fase).

Su especificación quirúrgica está en **`## 10. Especificación de los StateEventValidatorImpl`**, con una subsección por fase. Interfaz: `StateEventValidator`.

**Verificación:** compilan; hay exactamente un `@BeanValidationRulesForStateAndEvent getForState<Estado>InEvent<Evento>()` por cada fila «sí» o «`rules { }` vacío» de la tabla de cobertura de §10, y **ninguno** para `DELETE`; ningún `field(...)` menciona un campo clasificado `servidor` en §4 (salvo `pdfSolicitudFirmada`, la excepción documentada).

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

## `### 9.1 Fase SOLICITUD` de `## 9. Especificación de los PhaseEventManagerImpl`, ÍNTEGRA (verbatim)

### 9.1 Fase SOLICITUD

```java
package com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.solicitud;

public class PhaseEventManagerImpl extends PhaseEventManager<AnulacionMatriculaCicloFormativoV1> {
    @Inject
    public PhaseEventManagerImpl(AnulacionMatriculaCicloFormativoV1Repository repository) {
        super(AnulacionMatriculaCicloFormativoV1.class);
        this.repository = repository;
    }
}
```

- FQCN: `…v1.solicitud.PhaseEventManagerImpl`.
- Dependencias a inyectar:
  - `AnulacionMatriculaCicloFormativoV1Repository repository` — por constructor, como exige el patrón.
  - `@Inject FirmaServidorHelper firmaServidorHelper` — firma la solicitud en el servidor cuando el alumno tiene certificado custodiado.
- Constantes de clase:
  - `private static final Rectangulo POSICION_FIRMA_SOLICITUD = new Rectangulo(320, 60, 250, 70);`
  - `private static final int PAGINA_FIRMA_SOLICITUD = 1;`
  - **MUST** ser exactamente los cuatro números y la página que la `<action-method>` `exp-AnulacionMatriculaCicloFormativoV1-firmarSolicitud-action` pasa a `firmarDocumento`, para que la firma caiga en el mismo sitio se firme donde se firme.
- `import com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.States;` — el `States` de **esta** versión.

**`triggerContinuar`**

1. `ERROR_NEGOCIO(ControlDeAcceso.exigeSerElCreador(expediente, "Solo puede modificar sus propias solicitudes"))` — VAL-DATOS_SOLICITUD-CONTINUAR-016
2. `ASIGNAR(fechaSolicitud = LocalDate.now(Convert.defaultZoneId))` — RN-001 / CC-005 (el **lugar** es `localidadCentro`, que ya se fijó al crear el expediente)
3. `GENERAR_PDF(SOLICITUD)`
4. `CREAR_METAFILE → pdfSolicitud` — RN-002 / CC-006 (sustituye a la de un intento anterior)
5. `LIMPIAR(pdfSolicitudFirmada)` — al regenerar la solicitud la firma anterior deja de corresponder al documento, así que **MUST** invalidarse: si no, tras `SUBSANAR` → `CONTINUAR` (o tras `VOLVER` → `CONTINUAR`) el expediente llegaría a `PENDIENTE_FIRMA` con la solicitud nueva y la firma vieja puesta, y en la rama de AutoFirma `PRESENTAR` la daría por buena sin volver a firmar. Sin condición: si no había firma, limpiarla es idempotente
6. `UPDATE_STATE(States.Solicitud.PENDIENTE_FIRMA)`

**`triggerVolver`**

1. `ERROR_NEGOCIO(ControlDeAcceso.exigeSerElCreador(expediente, "Solo puede volver atrás en sus propias solicitudes"))` — **guarda de autoría del diseño** (§11, «Las cuatro guardas de autoría»): el motor exime al administrador de `checkPerfilDelEstado`, así que sin ella el administrador puede disparar `VOLVER` sobre la solicitud de otro alumno, contra HU-010/ESC-025 (solo consulta) y ESC-020/ESC-032/ESC-045 (los no creadores la ven en solo lectura)
2. `ASIGNAR(claveCertificado = null)` — la clave del certificado no debe sobrevivir en el objeto
3. `UPDATE_STATE(States.Solicitud.DATOS_SOLICITUD)`

**`triggerPresentar`**

1. `ERROR_NEGOCIO(ControlDeAcceso.exigeSerElCreador(expediente, "Solo puede presentar sus propias solicitudes"))` — **guarda de autoría del diseño** (§11, «Las cuatro guardas de autoría»): el motor exime al administrador de `checkPerfilDelEstado`, así que es la única capa que le impide presentar la solicitud de otro alumno. Va **la primera**, antes incluso de leer la situación de firma, para que ningún efecto ocurra si el usuario no es el creador
2. `SERVICIO(CertificadoDigitalHelper.getSituacionFirmaByDni(SecurityUtil.getUser().getDni()))` → la situación de firma del usuario autenticado; **MUST NOT** leerse del formulario
3. `SERVICIO(firmaServidorHelper.firmarEnServidor(dni, situacionFirma, expediente.getClaveCertificado(), expediente.getPdfSolicitud(), POSICION_FIRMA_SOLICITUD, PAGINA_FIRMA_SOLICITUD)) → pdfSolicitudFirmada` — **solo si** `situacionFirma.isFirmaEnServidor()` (notación condicional declarada en §14 nota 24); en la rama de AutoFirma el campo ya llegó validado por `FirmaPdf` y no hay nada que firmar
4. `REGISTRO_ENTRADA(documento=pdfSolicitudFirmada, anexos=[]) → pdfJustificanteRegistroEntrada` — RN-003 y RN-004; el trámite no aporta anexos
5. `ASIGNAR(fechaHoraPresentacion = LocalDateTime.now(Convert.defaultZoneId))` — RN-005 / CC-007
6. `LIMPIAR(textoSubsanacion)` — RN-006; se hace **sin condición**: si el expediente no venía de una subsanación ese campo ya está vacío y limpiarlo es idempotente, así que no hace falta una rama que decida cuándo aplica
7. `LIMPIAR(sentidoRevision, motivoRechazo)` y, a continuación, `SERVICIO(DevolucionDelDirector.borrar(expediente))` — RN-007 (la decisión de secretaría y la devolución del director dejan de aplicar a la solicitud que vuelve a presentarse), las dos sin condición y con el mismo carácter idempotente
8. `UPDATE_STATE(States.Revision.PENDIENTE_REVISION)`
9. `ASIGNAR(claveCertificado = null)` — **en un `finally`** que envuelve los pasos 3 a 8 (notación de control de flujo declarada en §14 nota 24)

**`triggerDelete`**

1. `ERROR_NEGOCIO(ControlDeAcceso.exigeSerElCreador(expediente, "Solo puede borrar sus propias solicitudes"))` — VAL-DATOS_SOLICITUD-BORRADO-001. Es el **único** sitio posible: el `Tramitador` se salta la validación para `DELETE`, pero **sí** llama a `triggerDelete` antes de `repository.remove`, y una `BusinessException` aquí aborta el borrado.
2. `UPDATE_STATE: ninguno` — el expediente se elimina justo después y el `onEnterState` no llega a ejecutarse.

**`onEnter*`**

- `onEnterDatosSolicitud` — vacío.
- `onEnterPendienteFirma` — vacío.

#### 9.1.4 Lista de cobertura de la fase SOLICITUD

- Eventos (unión de los `events` de `DATOS_SOLICITUD` y `PENDIENTE_FIRMA`): `DELETE` → `triggerDelete`; `CONTINUAR` → `triggerContinuar`; `VOLVER` → `triggerVolver`; `PRESENTAR` → `triggerPresentar`.
- Estados: `DATOS_SOLICITUD` → `onEnterDatosSolicitud`; `PENDIENTE_FIRMA` → `onEnterPendienteFirma`.
- **MUST NOT** factorizarse ningún `trigger*`/`onEnter*` en una superclase: el dispatcher usa `getDeclaredMethods()`.

## `### 10.1 Fase SOLICITUD` de `## 10. Especificación de los StateEventValidatorImpl`, ÍNTEGRA (verbatim)

### 10.1 Fase SOLICITUD

```kotlin
package com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.solicitud

import com.educaflow.subsystem.expedientes.db.AnulacionMatriculaCicloFormativoV1 as model
import com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.SinOtraSolicitudEnCursoParaElMismoCiclo
import com.educaflow.tramites.util.firma.ClaveCertificadoValida
import com.educaflow.tramites.util.firma.ifSituacionFirma

class StateEventValidatorImpl : StateEventValidator { … }
```

Tabla de cobertura:

| estado | evento | método | ¿reglas? |
|---|---|---|---|
| `DATOS_SOLICITUD` | `CONTINUAR` | `getForStateDatosSolicitudInEventContinuar` | sí |
| `DATOS_SOLICITUD` | `DELETE` | **ninguno** (exento) | — |
| `PENDIENTE_FIRMA` | `VOLVER` | `getForStatePendienteFirmaInEventVolver` | `rules { }` vacío |
| `PENDIENTE_FIRMA` | `PRESENTAR` | `getForStatePendienteFirmaInEventPresentar` | sí |

**`getForStateDatosSolicitudInEventContinuar`**

```kotlin
field(model::getNia) {
    +Required("Debe indicar su NIA")
    +Pattern("^\\d{8}$", "El NIA debe tener 8 dígitos")
}
field(model::getDireccion) {
    +Required("Debe indicar su dirección")
    +MinLength(5, "La dirección debe tener entre 5 y 150 caracteres")
    +MaxLength(150, "La dirección debe tener entre 5 y 150 caracteres")
}
field(model::getTelefono) {
    +Required("Debe indicar un teléfono de contacto")
    +Pattern("^[6789]\\d{8}$", "El teléfono debe tener 9 dígitos y empezar por 6, 7, 8 o 9")
}
field(model::getPoblacion) {
    +Required("Debe indicar su población")
    +MinLength(2, "La población debe tener entre 2 y 100 caracteres")
    +MaxLength(100, "La población debe tener entre 2 y 100 caracteres")
}
field(model::getProvincia) {
    +Required("Debe indicar su provincia")
    +MinLength(2, "La provincia debe tener entre 2 y 50 caracteres")
    +MaxLength(50, "La provincia debe tener entre 2 y 50 caracteres")
}
field(model::getCodigoPostal) {
    +Required("Debe indicar su código postal")
    +Pattern("^\\d{5}$", "El código postal debe tener 5 dígitos")
}
field(model::getCiclo) {
    +Required("Debe indicar el ciclo formativo")
    +SinOtraSolicitudEnCursoParaElMismoCiclo()
}
```

**`getForStatePendienteFirmaInEventVolver`** → `rules { }` (vacío, declarado explícitamente: el evento no lleva datos).

**`getForStatePendienteFirmaInEventPresentar`**

```kotlin
field(model::getClaveCertificado) {
    +ifSituacionFirma({ it.isFirmaEnServidor() }) {
        +ClaveCertificadoValida()
    }
}
field(model::getPdfSolicitudFirmada) {
    +ifSituacionFirma({ !it.isFirmaEnServidor() }) {
        +Required("Debe firmar la solicitud antes de presentarla")
        +FirmaPdf(model::getPdfSolicitud, "La firma no es válida o no corresponde a su documento de identidad")
    }
}
```

- Las dos ramas son **complementarias por construcción** (`isFirmaEnServidor()` / su negación), así que toda situación de firma cae en exactamente una; un usuario sin DNI cae en la de AutoFirma y `FirmaPdf` la rechaza con el mensaje **propio de esa rama** («su usuario no tiene un documento de identidad válido»), que el segundo argumento **no** sustituye: solo sustituye el de firma inválida (Paso 5).
- **Los dos `field(...)` MUST estar siempre**, aunque el trámite «solo vaya a usar» un camino: son la whitelist del evento y la rama que aplica depende del usuario que presente.
- Ningún otro campo entra en la whitelist de `PRESENTAR`: los datos ya se validaron en `CONTINUAR` y en este estado no se editan.

## `### 10.4 Frontera de confianza` (verbatim)

### 10.4 Frontera de confianza

- Ningún `field(...)` menciona un campo clasificado `servidor` en §4 — ni `codePhase`, `codeState`, `abierto`, `centro` o `usuarioRegistrador`, ni ninguno de los cinco `MetaFile` que rellena el servidor, ni las fechas y autores de revisión, devolución y resolución. La **única excepción** es `pdfSolicitudFirmada`, que **también está clasificado `servidor`** y aparece en el validador únicamente porque es el destino de la firma en cliente y ese `field(...)` es el único sitio donde se puede comprobar la firma (§4).
- Cada campo editable en la vista de un estado aparece en el `field(...)` del evento que se dispara desde esa vista: `nia`, `direccion`, `telefono`, `poblacion`, `provincia`, `codigoPostal` y `ciclo` en `CONTINUAR`; `claveCertificado` y `pdfSolicitudFirmada` en `PRESENTAR`; `sentidoRevision`, `motivoRechazo` y `textoSubsanacion` en `ENVIAR_A_FIRMA` y en `SUBSANAR`; `motivoDevolucion` en `DEVOLVER`.
- **CRITICAL** — esta puerta **NO** protege el endpoint REST automático `POST /ws/rest/com.educaflow.subsystem.expedientes.db.AnulacionMatriculaCicloFormativoV1`, que Axelor publica para toda entidad y que **no pasa por el `Tramitador`**. Son dos puertas distintas: **MUST NOT** darse por protegida esta entidad porque su tramitación valide por evento, y **MUST NOT** introducirse un `ModelService` deny-all como parche (se retiró a propósito, ver `CLAUDE.md`).
- `readonly`, `showIf` y `hidden` de las vistas son **UX, nunca defensa**.

## Tabla de transiciones de `## 3` filtrada a los estados de esta fase (verbatim)

| fase origen | estado origen | evento | guarda | fase destino | estado destino |
|---|---|---|---|---|---|
| `[*]` | `[*]` | — | — | `SOLICITUD` | `DATOS_SOLICITUD` |
| `SOLICITUD` | `DATOS_SOLICITUD` | `CONTINUAR` | — | `SOLICITUD` | `PENDIENTE_FIRMA` |
| `SOLICITUD` | `DATOS_SOLICITUD` | `DELETE` | — | `[*]` | `[*]` |
| `SOLICITUD` | `PENDIENTE_FIRMA` | `VOLVER` | — | `SOLICITUD` | `DATOS_SOLICITUD` |
| `SOLICITUD` | `PENDIENTE_FIRMA` | `PRESENTAR` | — | `REVISION` | `PENDIENTE_REVISION` |

## Filas y párrafos de `## 11. Reparto de reglas` que aplican (verbatim)

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

**Las cuatro guardas de autoría, y por qué son cuatro y no dos.** `CONTINUAR`, `DELETE`, `VOLVER` y `PRESENTAR` —los cuatro eventos de los dos estados cuyo `profile` es `CREADOR`— llevan **todos** la guarda `ControlDeAcceso.exigeSerElCreador` en su primera línea. Las dos primeras las pide la especificación (VAL-DATOS_SOLICITUD-CONTINUAR-016 y VAL-DATOS_SOLICITUD-BORRADO-001); las dos últimas las **añade el diseño**, y la razón es la misma que hace falta para las dos primeras: `Tramitador.checkPerfilDelEstado` **exime al administrador** (`SecurityUtil.isAdmin`), así que el perfil del estado no basta para garantizar que quien dispara el evento sea el creador. Sin ellas, un administrador que abriera la solicitud de otro alumno por «Expedientes Pendientes» —que fija `_profile=CREADOR`— podría disparar `VOLVER` y `PRESENTAR` sobre ella, contra HU-010/ESC-025 (el administrador solo consulta, en solo lectura) y ESC-020/ESC-032/ESC-045 (los no creadores ven la pantalla en solo lectura). **Esto anula expresamente la indicación de `design-guidelines.md`** («no se han añadido comprobaciones de autoría a VOLVER ni a PRESENTAR: serían una copia de lo que el motor ya garantiza»): el motor **no** lo garantiza para el administrador, que es exactamente el caso abierto, y la propia guía reconoce esa excepción al justificar por qué sí se conservan las otras dos. La desviación queda declarada aquí y en §14 nota 25.

**Por qué las comprobaciones de identidad no van al validador.** El DSL cuelga cada regla de un campo, y esas comprobaciones no hablan de ningún campo. Además, dos de ellas caen donde el validador no llega o no debe crecer: `DELETE` se salta la validación entera en el `Tramitador` (su método del validador no existe y nunca se invocaría), y `FIRMAR` no admite ningún dato del formulario, así que darle un `field(...)` solo para colgar la regla metería ese campo en la whitelist del evento. La regla de reparto queda enunciable en una frase: **si la regla habla del valor de un campo, validador; si habla de quién eres, guarda del trigger**. Ver `decisiones.md` D4.

| Regla | Capa |
|---|---|
| VAL-DATOS_SOLICITUD-CONTINUAR-001 … 013 | DSL del validador, `getForStateDatosSolicitudInEventContinuar` |
| VAL-DATOS_SOLICITUD-CONTINUAR-014 y 015 | *(eliminadas por la propia especificación: el nombre del ciclo ya no se escribe a mano)* |
| VAL-DATOS_SOLICITUD-CONTINUAR-016 | guarda de `triggerContinuar` (`ControlDeAcceso.exigeSerElCreador`) |
| VAL-DATOS_SOLICITUD-CONTINUAR-017 | DSL del validador, regla `SinOtraSolicitudEnCursoParaElMismoCiclo` sobre `ciclo` |
| VAL-DATOS_SOLICITUD-BORRADO-001 | guarda de `triggerDelete` (único sitio posible: `DELETE` no pasa por el validador) |
| *(sin identificador en la spec)* — autoría en `VOLVER` | guarda de `triggerVolver` (`ControlDeAcceso.exigeSerElCreador`). **La añade el diseño**, no la spec: cierra el único evento de `PENDIENTE_FIRMA` que el administrador podía disparar sobre la solicitud de otro, porque el motor le exime del perfil del estado (§11, «Las cuatro guardas de autoría», y §14 nota 25) |
| *(sin identificador en la spec)* — autoría en `PRESENTAR` | guarda de `triggerPresentar` (`ControlDeAcceso.exigeSerElCreador`). **La añade el diseño**, por el mismo motivo: sin ella, el administrador podría presentar la solicitud de otro alumno (§11, «Las cuatro guardas de autoría», y §14 nota 25) |
| VAL-PENDIENTE_FIRMA-PRESENTAR-001 y 002 | DSL del validador, rama de AutoFirma de `getForStatePendienteFirmaInEventPresentar` |
| RN-001 … RN-007 | acciones de `triggerContinuar` y `triggerPresentar` (§9.1) |
| CC-001, CC-003, CC-004 | `triggerInitialEvent` (§8) |
| CC-002 | **sin origen de datos hoy**: no existe ninguna ficha del alumno de la que copiar; los campos nacen vacíos (§8 y §14) |
| CC-005 … CC-013 | acciones de los `trigger*` (§9) |
| RUI-DATOS_SOLICITUD-CREADOR-004 y 005 | *(eliminadas por la propia especificación: el nombre del ciclo ya no se escribe a mano y el grado ya no se elige aparte, lo lleva puesto el ciclo)* |
| RUI-DATOS_SOLICITUD-CREADOR-001 … 003 y 006 … 012 | vista: `showIf` del panel `subsanacion`, `readonly` de los campos congelados, `help` de NIA/teléfono/CP/ciclo, `grid-view`/`form-view` del ciclo y campo punteado `ciclo.gradoNivel` |
| RUI-DATOS_SOLICITUD-CREADOR-002 | vista: `<action-attrs>` `exp-AnulacionMatriculaCicloFormativoV1-DATOS_SOLICITUD-onLoad-action`, en el `onLoad` **solo** de `DATOS_SOLICITUD`/`CREADOR`, que pone `required=true` a `nia`, `direccion`, `telefono`, `poblacion`, `provincia`, `codigoPostal` y `ciclo`. No se marca en los `<field>` del form plantilla porque `datos-alumno` y `matricula` los comparten ocho forms de solo lectura. La exigencia real está en el validador |
| RUI-DATOS_SOLICITUD-GENERICA-001 y 002 | vista genérica de `DATOS_SOLICITUD` |
| RUI-PENDIENTE_FIRMA-CREADOR-001 y 002 | vista `PENDIENTE_FIRMA` del `CREADOR` (visor a 900 px y panel `subsanacion`) |
| RUI-PENDIENTE_FIRMA-GENERICA-001 | vista genérica de `PENDIENTE_FIRMA` |

Reglas duras respetadas: ningún `required="true"` en el `domains.xml`; ninguna validación de datos de usuario en un `trigger*`; ninguna lógica de negocio en el validador; ninguna inicialización del expediente en un `PhaseEventManagerImpl`.
