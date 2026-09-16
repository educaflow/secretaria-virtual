---
type: implementation-task
template: expediente
---

# Tarea 12 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-tipo-expediente
- k-validaciones
- k-secure-coding
- k-code-quality
- k-i18n

Implementa **la fase `RESOLUCION`** completa: sus tres ficheros de `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/resolucion/`.

| Fichero | Acción | Cómo se materializa |
|---|---|---|
| `…/v1/resolucion/PhaseEventManagerImpl.java` | Crear | Se escribe **sobre el esqueleto** que dejó `CreateFilesTask`, según `## 9` (subsección de esta fase), abajo verbatim |
| `…/v1/resolucion/StateEventValidatorImpl.kt` | Crear | Se escribe **sobre el esqueleto** que dejó `CreateFilesTask`, según `## 10` (subsección de esta fase), abajo verbatim |
| `…/v1/resolucion/views.xml` | Crear | **Copia literal** de `design/fases/resolucion/views.xml` a `…/v1/resolucion/views.xml`, **sobrescribiendo** el esqueleto. **MUST NOT** modificarlo, reescribirlo ni regenerarlo |

**CRITICAL — las especificaciones de `## 9` y `## 10` que van abajo son contrato fijo y la superficie es cerrada: MUST NOT crearse ningún método, clase, campo, acción ni regla que no listen**, ni reordenarse las listas numeradas de acciones (el orden es normativo). Un `trigger*` por cada evento de la lista de cobertura y un `onEnter*` por cada estado de la fase, **ninguno de más**; en el validador, un `getForState<Estado>InEvent<Evento>` por cada pareja declarada salvo las de `DELETE`.

El conjunto de `field(...)` de cada método del validador es, además de la validación, **la lista de campos que el cliente puede dictar en ese evento** (`AllowProperties`): **MUST NOT** añadirse ningún `field(...)` de un campo clasificado `servidor` en §4 (la única excepción documentada del tipo es `pdfSolicitudFirmada` en `PRESENTAR`).

Para obtener el usuario autenticado **MUST** usarse `SecurityUtil.getUser()`; **NUNCA** `AuthUtils.getUser()`.

## Filas de la tabla `## 6. Ficheros a crear o modificar` del diseño

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/resolucion/PhaseEventManagerImpl.java` | Crear | `k-tipo-expediente` | Lo especifica `## 9. Especificación de los PhaseEventManagerImpl` (fase `RESOLUCION`) |
| `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/resolucion/StateEventValidatorImpl.kt` | Crear | `k-tipo-expediente`, `k-secure-coding` | Lo especifica `## 10. Especificación de los StateEventValidatorImpl` (fase `RESOLUCION`) |
| `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/resolucion/views.xml` | Crear | `k-tipo-expediente` | Copia de `design/fases/resolucion/views.xml` |

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
| RESOLUCION | `PENDIENTE_FIRMA_DIRECTOR` | `DIRECTOR` | `-datos-alumno-resumen`, `-matricula-director`, `-decision-secretaria`, `-resolucion-a-firmar`, `-solicitud-firmada-descarga`, `-justificante-descarga`, `devolucion` | `DEVOLVER` / `FIRMAR` |
| RESOLUCION | `PENDIENTE_FIRMA_DIRECTOR` | — | `-datos-alumno`, `-matricula`, `-presentacion`, `-solicitud-firmada-descarga` | — / `EXIT` |
| RESOLUCION | `ACEPTADA` | — | `-datos-alumno`, `-matricula`, `-presentacion-descarga-aceptada`, `-resolucion-firmada`, `-solicitud-firmada-descarga` | — / `EXIT` |
| RESOLUCION | `RECHAZADA` | — | `-datos-alumno`, `-matricula`, `-presentacion-descarga`, `-resolucion-firmada`, `-solicitud-firmada-descarga` | — / `EXIT` |


Cada form lleva además, **fuera** del `<include-panels>`, un `<panel showFrame="false">` con el `<help>` del aviso permanente que la especificación fija para esa pantalla. **Excepción:** la indicación de RUI-ACEPTADA-GENERICA-002 **no** va en ese panel de avisos, sino dentro del panel `presentacion-descarga-aceptada`, junto a la fecha y la hora de presentación a las que acompaña (mismo patrón que `matricula-director` para RUI-PENDIENTE_FIRMA_DIRECTOR-DIRECTOR-007).

Las acciones propias van en el `views.xml` de su fase: en `solicitud/`, las cuatro de la firma más el `<action-attrs>` `…-DATOS_SOLICITUD-onLoad-action` que marca como obligatorios en pantalla los siete campos del alumno (RUI-DATOS_SOLICITUD-CREADOR-002); en `revision/`, la que limpia el motivo/el texto al cambiar el sentido.

**Verificación:** `diff` vacío en los tres; ningún `<button name="">` sin rellenar; cada estado tiene su form genérico con botón `EXIT`; los estados con `profile` y eventos tienen además su form de perfil; la suma de `colSpan` de los botones de cada footer no pasa de 12.


## Tabla de transiciones de `## 3`, filtrada a los estados de esta fase (verbatim)

| fase origen | estado origen | evento | guarda | fase destino | estado destino |
|---|---|---|---|---|---|
| `RESOLUCION` | `PENDIENTE_FIRMA_DIRECTOR` | `FIRMAR` | `sentidoRevision=ACEPTAR` | `RESOLUCION` | `ACEPTADA` |
| `RESOLUCION` | `PENDIENTE_FIRMA_DIRECTOR` | `FIRMAR` | `sentidoRevision=RECHAZAR` | `RESOLUCION` | `RECHAZADA` |
| `RESOLUCION` | `PENDIENTE_FIRMA_DIRECTOR` | `DEVOLVER` | — | `REVISION` | `PENDIENTE_REVISION` |

`ENVIAR_A_FIRMA` y `SUBSANAR` tienen **un solo destino cada uno**, así que no llevan guarda: lo que la especificación describe como «condición» de cada uno (que el sentido sea aceptar/rechazar o que sea subsanar) es una **validación** que impide disparar el evento, no una ramificación de la transición, y vive en el DSL del validador (§10). El único evento ramificado es `FIRMAR`, cuyo discriminador es `sentidoRevision`; sus dos ramas cubren los dos únicos valores posibles en ese estado (`SUBSANAR` no puede llegar aquí, porque el validador de `ENVIAR_A_FIRMA` lo rechaza) y el `default` del `switch` es un error de programación.

## `### 9.3 Fase RESOLUCION` (verbatim, ÍNTEGRA)

### 9.3 Fase RESOLUCION

```java
package com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.resolucion;

public class PhaseEventManagerImpl extends PhaseEventManager<AnulacionMatriculaCicloFormativoV1> { … }
```

- FQCN: `…v1.resolucion.PhaseEventManagerImpl`.
- Dependencias a inyectar:
  - `AnulacionMatriculaCicloFormativoV1Repository repository` — por constructor.
  - `@Inject AlmacenClaveResolver almacenClaveResolver` — el certificado del Director del centro.
- Constantes de clase:
  - `private static final Rectangulo POSICION_FIRMA_RESOLUCION = new Rectangulo(150, 317, 300, 53);` — medido sobre el PDF generado: el recuadro vacío bajo «El director / la directora del centro» va de `y 315,28` a `y 372,00` (§5 y §14 nota 12)
- `import …v1.States;` e `import …v1.ControlDeAcceso;`. **MUST NOT** aparecer `import com.educaflow.subsystem.expedientes.db.Profile;`: la guarda de perfil de las dos acciones 1 **no recibe ningún perfil** —se lo pregunta al `<state>` a través de `States` (§9.0.1)—, así que ningún `PhaseEventManagerImpl` nombra el enum.

**`triggerFirmar`**

1. `ERROR_NEGOCIO(ControlDeAcceso.exigeMismoCentroQueElExpediente(expediente, "Solo puede firmar resoluciones de su propio centro"))` — VAL-PENDIENTE_FIRMA_DIRECTOR-FIRMAR-001 — y, a continuación, `ERROR_NEGOCIO(ControlDeAcceso.exigeOstentarElPerfilDelEstado(expediente, "Solo el director del centro puede firmar la resolución"))` — **guarda de perfil del diseño** (§11, «Las ocho guardas de identidad»). Es la guarda **más crítica de las cuatro nuevas**: `FIRMAR` firma la resolución con el certificado del Director del centro, la registra de salida y cierra el expediente, y sin ella el administrador —al que `checkPerfilDelEstado` exime— podría resolver por el director cualquier expediente de su centro activo, contra HU-010/ESC-025. **No se le pasa ningún perfil**: le pregunta a `States` el que declara `PENDIENTE_FIRMA_DIRECTOR` (hoy `DIRECTOR`) y exige ese (§9.0.1). Las **dos** van en la acción 1, antes de resolver el almacén de claves
2. `SERVICIO(almacenClaveResolver.getDirector(expediente.getCentro())) → almacenDirector` (**variable local** del trigger, §14 nota 24); `ERROR_NEGOCIO(la resolución del almacén falla, "El centro no tiene configurada la firma del Director; avise al administrador")`; `ERROR_NEGOCIO(almacenDirector == null, "El centro no tiene configurada la firma del Director; avise al administrador")` — VAL-PENDIENTE_FIRMA_DIRECTOR-FIRMAR-002

   - «La resolución del almacén falla» es el caso en que `getDirector` termina con `RuntimeException` porque **el recurso del certificado del cargo falta o no se puede leer** (`AlmacenClaveFichero` rechaza un `InputStream` nulo con «El fileCertificate no puede ser null»). Ese fallo se transmite al usuario con ese mensaje, conservando la excepción original como causa.
   - **El ámbito del fallo capturado es SOLO esta resolución**: no cubre el `FIRMAR_SERVIDOR` de la acción 5 ni ninguna otra acción del trigger, que fallan con su propio mensaje. **MUST NOT** ampliarse para envolver la firma.
   - **Las dos comprobaciones son complementarias y las dos MUST escribirse.** `almacenClaveResolver.getDirector(centro)` **nunca devuelve `null`** hoy —construye siempre un `AlmacenClaveFichero`—, así que escribir **solo** la comparación con `null` dejaría una condición inalcanzable y la regla de la especificación de hecho sin implementar; y escribir solo la primera dejaría sin cubrir el día en que el resolver pase a depender del centro y devuelva `null` para un centro sin certificado configurado.
   - El almacén se resuelve **una sola vez** y se reutiliza en la acción 5.
3. `ASIGNAR(fechaResolucion = LocalDate.now(Convert.defaultZoneId))` y `ASIGNAR(firmadoPor = SecurityUtil.getUser())` — RN-017 / CC-012; van **antes** de generar el PDF porque la fecha se estampa en él
4. `GENERAR_PDF(RESOLUCION)` — RN-022: se **regenera** (no se recupera la guardada) para que lleve la fecha de la resolución recién anotada
5. `FIRMAR_SERVIDOR(cargo=DIRECTOR, rect=POSICION_FIRMA_RESOLUCION)` — RN-018: sobre el `DocumentoPdf` del paso 4, con el `almacenDirector` del paso 2 (**no** se vuelve a resolver)
6. `CREAR_METAFILE → pdfTemporal` — RN-018 (la resolución firmada queda guardada en el expediente; el campo que la recibe es `pdfResolucionFirmada`, acción 7)
7. `REGISTRO_SALIDA(documento=pdfTemporal, anexos=[]) → pdfResolucionFirmada` — RN-019; el campo recibe `registroSalida.getDocumento()`, la resolución ya registrada
8. `LIMPIAR(pdfResolucion)` — RN-023: en el expediente cerrado queda una única versión de la resolución
9. `SERVICIO(DevolucionDelDirector.borrar(expediente))` — RN-020, sin condición (idempotente)
10. `UPDATE_STATE segun sentidoRevision: ACEPTAR → States.Resolucion.ACEPTADA; RECHAZAR → States.Resolucion.RECHAZADA; SUBSANAR → error; default → error` — el `default` lanza `IllegalArgumentException`: llegar aquí con `SUBSANAR` o con el sentido vacío es un error de programación, porque el validador de `ENVIAR_A_FIRMA` solo deja pasar `ACEPTAR` y `RECHAZAR`

- **`pdfTemporal` (acciones 6 y 7) es una variable local del trigger**, el `MetaFile` intermedio que se le pasa al registro de salida: **MUST NOT** declararse en `domains.xml` ni aparecer en la tabla de campos de §4. El único campo de la entidad que se asigna aquí es `pdfResolucionFirmada`, que recibe `registroSalida.getDocumento()`.

**`triggerDevolver`**

1. `ERROR_NEGOCIO(ControlDeAcceso.exigeMismoCentroQueElExpediente(expediente, "Solo puede devolver resoluciones de su propio centro"))` — VAL-PENDIENTE_FIRMA_DIRECTOR-DEVOLVER-003 — y, a continuación, `ERROR_NEGOCIO(ControlDeAcceso.exigeOstentarElPerfilDelEstado(expediente, "Solo el director del centro puede devolver la resolución a la secretaría"))` — **guarda de perfil del diseño** (§11, «Las ocho guardas de identidad»), por el mismo motivo que `FIRMAR` y con la misma forma, **sin literal de perfil**
2. `ASIGNAR(fechaDevolucion = LocalDate.now(Convert.defaultZoneId))` y `ASIGNAR(devueltoPor = SecurityUtil.getUser())` — RN-021 / CC-011
3. `UPDATE_STATE(States.Revision.PENDIENTE_REVISION)`

**`onEnter*`**

- `onEnterPendienteFirmaDirector` — vacío.
- `onEnterAceptada` — vacío.
- `onEnterRechazada` — vacío.

#### 9.3.3 Lista de cobertura de la fase RESOLUCION

- Eventos: `FIRMAR` → `triggerFirmar`; `DEVOLVER` → `triggerDevolver`.
- Estados: `PENDIENTE_FIRMA_DIRECTOR` → `onEnterPendienteFirmaDirector`; `ACEPTADA` → `onEnterAceptada`; `RECHAZADA` → `onEnterRechazada` (los estados cerrados también necesitan su `onEnter`).


## `### 10.3 Fase RESOLUCION` (verbatim, ÍNTEGRA)

### 10.3 Fase RESOLUCION

```kotlin
package com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.resolucion

import com.educaflow.subsystem.expedientes.db.AnulacionMatriculaCicloFormativoV1 as model
```

Tabla de cobertura:

| estado | evento | método | ¿reglas? |
|---|---|---|---|
| `PENDIENTE_FIRMA_DIRECTOR` | `FIRMAR` | `getForStatePendienteFirmaDirectorInEventFirmar` | `rules { }` vacío |
| `PENDIENTE_FIRMA_DIRECTOR` | `DEVOLVER` | `getForStatePendienteFirmaDirectorInEventDevolver` | sí |

**`getForStatePendienteFirmaDirectorInEventFirmar`** → `rules { }` (vacío, declarado explícitamente). El director **no envía ningún dato** al firmar: la firma la pone el centro y él solo la autoriza con el botón. Dejarlo vacío es también lo que impide que el cliente dicte ningún campo en este evento.

**`getForStatePendienteFirmaDirectorInEventDevolver`**

```kotlin
field(model::getMotivoDevolucion) {
    +Required("Debe indicar a secretaría por qué devuelve la resolución")
    +MinLength(10, "El motivo de la devolución debe tener entre 10 y 1000 caracteres")
    +MaxLength(1000, "El motivo de la devolución debe tener entre 10 y 1000 caracteres")
}
```


## `### 10.4 Frontera de confianza` (verbatim)

### 10.4 Frontera de confianza

- Ningún `field(...)` menciona un campo clasificado `servidor` en §4 — ni `codePhase`, `codeState`, `abierto`, `centro` o `usuarioRegistrador`, ni ninguno de los cinco `MetaFile` que rellena el servidor, ni las fechas y autores de revisión, devolución y resolución. La **única excepción** es `pdfSolicitudFirmada`, que **también está clasificado `servidor`** y aparece en el validador únicamente porque es el destino de la firma en cliente y ese `field(...)` es el único sitio donde se puede comprobar la firma (§4).
- Cada campo editable en la vista de un estado aparece en el `field(...)` del evento que se dispara desde esa vista: `nia`, `direccion`, `telefono`, `poblacion`, `provincia`, `codigoPostal` y `ciclo` en `CONTINUAR`; `claveCertificado` y `pdfSolicitudFirmada` en `PRESENTAR`; `sentidoRevision`, `motivoRechazo` y `textoSubsanacion` en `ENVIAR_A_FIRMA` y en `SUBSANAR`; `motivoDevolucion` en `DEVOLVER`.
- **CRITICAL** — esta puerta **NO** protege el endpoint REST automático `POST /ws/rest/com.educaflow.subsystem.expedientes.db.AnulacionMatriculaCicloFormativoV1`, que Axelor publica para toda entidad y que **no pasa por el `Tramitador`**. Son dos puertas distintas: **MUST NOT** darse por protegida esta entidad porque su tramitación valide por evento, y **MUST NOT** introducirse un `ModelService` deny-all como parche (se retiró a propósito, ver `CLAUDE.md`).
- `readonly`, `showIf` y `hidden` de las vistas son **UX, nunca defensa**.


## `### 9.0 Clases auxiliares compartidas de la versión` — REFERENCIA de API (verbatim)

**Nota del descomponedor:** estas tres clases **ya las implementa la tarea 09**; van aquí solo como referencia de su API, porque los `trigger*` de esta fase las llaman. **MUST NOT** reimplementarlas, duplicarlas ni modificarlas en esta tarea.

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


## `## 11. Reparto de reglas` — tabla de reparto, guardas de identidad y filas que aplican a esta fase (verbatim)

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

**Las ocho guardas de identidad: cuatro de autoría y cuatro de perfil, y por qué las ocho.** La exención del administrador es **global** —`Tramitador.checkPerfilDelEstado` hace `return` para él en **cualquier** estado (`SecurityUtil.isAdmin`, `Tramitador.java` línea 232)—, así que el `profile` del estado no basta como barrera en **ninguno** de los tres estados con eventos. Por eso **los ocho eventos del tipo llevan en su acción 1 una guarda que comprueba en el servidor quién dispara**: los cuatro de los estados `CREADOR` (`CONTINUAR`, `DELETE`, `VOLVER`, `PRESENTAR`) con `ControlDeAcceso.exigeSerElCreador`, y los cuatro de los estados `SECRETARIO` y `DIRECTOR` (`ENVIAR_A_FIRMA`, `SUBSANAR`, `FIRMAR`, `DEVOLVER`) con `ControlDeAcceso.exigeOstentarElPerfilDelEstado(expediente, mensaje)`, **además** de la guarda de centro que la especificación sí numera. Sin las cuatro de perfil, el administrador —que pasa la guarda de centro en cuanto su centro activo es el del expediente, y el Paso 15 le asigna precisamente CIPFP Mislata— podría revisar, pedir subsanación, devolver y, sobre todo, **firmar**: cerrar el expediente con la resolución firmada con el certificado del Director y registrada de salida. Eso contradice HU-010/ESC-025 igual que lo contradecía el caso de `VOLVER`/`PRESENTAR`, y es el mismo olor **«ramas que no cubren todos los casos»** de `k-code-quality`: la guarda de centro deja fuera exactamente al único usuario al que el motor exime. **Las cuatro de perfil no escriben ningún perfil: preguntan por los dos lados.** «Qué perfil atiende este estado» tiene un dueño único y ya existente —el atributo `profile` del `<state>` del `TipoExpedienteInstance.xml`, proyectado en la clase generada `States`—, y «si el usuario lo ostenta» tiene otro —`PerfilesUsuarioService.getPerfilesSobreExpediente`—; la guarda consulta a los dos, exactamente como hace `Tramitador.checkPerfilDelEstado` (`State.getProfile()` + el conjunto de perfiles), y su única diferencia con el motor es que **no exime al administrador**. Por eso se llama `exigeOstentarElPerfilDelEstado` y **no recibe ningún parámetro `Profile`**: si lo recibiera, cambiar `profile="SECRETARIO"` por otro valor en el XML dejaría a las dos guardas de `PENDIENTE_REVISION` exigiendo el perfil viejo —divergiendo **en silencio** de lo que el motor exige a todo el mundo menos al administrador, que es justo el caso que la guarda existe para cubrir— y ningún test del build lo vería. **MUST NOT** reintroducirse el literal, ni como argumento ni como constante del `PhaseEventManagerImpl`. La decisión, con sus alternativas, está en `decisiones.md` **D13**.

**Las cuatro guardas de autoría, y por qué son cuatro y no dos.** `CONTINUAR`, `DELETE`, `VOLVER` y `PRESENTAR` —los cuatro eventos de los dos estados cuyo `profile` es `CREADOR`— llevan **todos** la guarda `ControlDeAcceso.exigeSerElCreador` en su primera línea. Las dos primeras las pide la especificación (VAL-DATOS_SOLICITUD-CONTINUAR-016 y VAL-DATOS_SOLICITUD-BORRADO-001); las dos últimas las **añade el diseño**, y la razón es la misma que hace falta para las dos primeras: `Tramitador.checkPerfilDelEstado` **exime al administrador** (`SecurityUtil.isAdmin`), así que el perfil del estado no basta para garantizar que quien dispara el evento sea el creador. Sin ellas, un administrador que abriera la solicitud de otro alumno por «Expedientes Pendientes» —que fija `_profile=CREADOR`— podría disparar `VOLVER` y `PRESENTAR` sobre ella, contra HU-010/ESC-025 (el administrador solo consulta, en solo lectura) y ESC-020/ESC-032/ESC-045 (los no creadores ven la pantalla en solo lectura). **Y siguen siendo una guarda distinta de la de perfil, no un caso particular suyo:** aunque los dos estados declaren `profile="CREADOR"` y el dueño de los perfiles conceda `CREADOR` por autoría, «ser el autor» y «ostentar el perfil que el estado declara» solo coinciden mientras ninguna fila de permisos asigne `CREADOR` por tipo de expediente o por expediente —las dos ramas de `findNombresPerfilesByExpediente` que **no** lo excluyen—, y la spec enuncia estas dos reglas sobre la autoría, cuyo dueño es `Expediente.usuarioRegistrador` (§9.0.1 y `decisiones.md` D13). **Esto anula expresamente la indicación de `design-guidelines.md`** («no se han añadido comprobaciones de autoría a VOLVER ni a PRESENTAR: serían una copia de lo que el motor ya garantiza»): el motor **no** lo garantiza para el administrador, que es exactamente el caso abierto, y la propia guía reconoce esa excepción al justificar por qué sí se conservan las otras dos. La desviación queda declarada aquí y en §14 nota 25.

**Por qué las comprobaciones de identidad no van al validador.** El DSL cuelga cada regla de un campo, y esas comprobaciones —autoría, centro y perfil— no hablan de ningún campo. Además, dos de ellas caen donde el validador no llega o no debe crecer: `DELETE` se salta la validación entera en el `Tramitador` (su método del validador no existe y nunca se invocaría), y `FIRMAR` no admite ningún dato del formulario, así que darle un `field(...)` solo para colgar la regla metería ese campo en la whitelist del evento. La regla de reparto queda enunciable en una frase: **si la regla habla del valor de un campo, validador; si habla de quién eres, guarda del trigger**. Las cuatro guardas de perfil caen en la misma frase y por el mismo lado: preguntan quién eres, no qué has enviado. Ver `decisiones.md` D4 y D13.

| Regla | Capa |
|---|---|
| VAL-PENDIENTE_FIRMA_DIRECTOR-FIRMAR-001 y 002 | guardas de `triggerFirmar` (identidad y disponibilidad del certificado del centro; ninguna habla de un campo del formulario). La **002** se implementa capturando el fallo de la resolución del almacén, no comparando con `null`, porque `getDirector` nunca devuelve `null` (§9.3 acción 2 y §14 nota 14) |
| VAL-PENDIENTE_FIRMA_DIRECTOR-DEVOLVER-001 y 002 | DSL del validador, `getForStatePendienteFirmaDirectorInEventDevolver` |
| VAL-PENDIENTE_FIRMA_DIRECTOR-DEVOLVER-003 | guarda de centro de `triggerDevolver` (`ControlDeAcceso.exigeMismoCentroQueElExpediente`) |
| *(sin identificador en la spec)* — perfil en `FIRMAR` y en `DEVOLVER` | guarda de `triggerFirmar` y de `triggerDevolver` (`ControlDeAcceso.exigeOstentarElPerfilDelEstado(…)`, que exige el perfil que `PENDIENTE_FIRMA_DIRECTOR` declara —hoy `DIRECTOR`— sin repetirlo). **Las añade el diseño**, por el mismo motivo; la de `FIRMAR` es la que impide que el administrador cierre el expediente firmando la resolución con el certificado del Director |
| RN-017 … RN-023 | acciones de `triggerFirmar` y `triggerDevolver` (§9.3) |
| CC-005 … CC-013 | acciones de los `trigger*` (§9) |
| RUI-PENDIENTE_FIRMA_DIRECTOR-DIRECTOR-001 … 007 | vista `PENDIENTE_FIRMA_DIRECTOR` del `DIRECTOR` |
| RUI-PENDIENTE_FIRMA_DIRECTOR-GENERICA-001 | vista genérica de `PENDIENTE_FIRMA_DIRECTOR`. Igual que la anterior: la condición de perfil de la bandeja de firma (Paso 16.0) solo **saca el expediente de esa bandeja**; **no garantiza** que todo el que no sea el director caiga en la genérica, porque el `_profile` viaja en la petición y nada en el servidor comprueba que el usuario **ostente** `DIRECTOR` para elegir la vista. Esta regla dice literalmente que «la decisión de secretaría no se enseña a nadie hasta que el director firma la resolución»: es una regla que debe **impedir** algo y hoy se apoya solo en la capa de vista. Residuo **NO mitigado** de §14 nota 8(d); cerrarlo es del motor |
| RUI-ACEPTADA-GENERICA-001 … 003 y RUI-RECHAZADA-GENERICA-001 y 002 | vistas de `ACEPTADA` y `RECHAZADA`. La **002** de `ACEPTADA` es el `<help>` «La anulación de la matrícula surte efecto desde esta fecha» **dentro** del panel `presentacion-descarga-aceptada`, junto a `fechaHoraPresentacion`; ese panel solo lo incluye `ACEPTADA` |
