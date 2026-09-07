---
type: implementation-task
template: expediente
---

# Tarea 09 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- `k-tipo-expediente`
- `k-validaciones`
- `k-secure-coding`
- `k-code-quality`

Materializa el delta de la fase `RECEPCION` del tipo `JustificacionFaltaProfesoradoV1`: el `PhaseEventManagerImpl.java`, el `StateEventValidatorImpl.kt` y el `views.xml` de la fase.

Dentro de `k-tipo-expediente` lee `phaseeventmanager.md` (para el manager), `validator.md` (para el validador) y `vistas.md` (para las vistas).

## Nota del descomponedor (decisiones de esta descomposición)

El contrato de descomposición manda **una sola tarea por fase**, que cubre **todos** los ficheros que la tabla §6 declara para esa fase; por eso los tres pasos del diseño (9, 10 y 11) van juntos en esta tarea. La fase `TRAMITACION` **no** tiene tarea: el delta no toca ninguno de sus ficheros.

Las piezas compartidas que este código usa —`FirmaDocumentoEntradaService`, `FirmaEnServidorHelper` y las cuatro reglas del DSL de `FirmaDocumentoEntradaRules.kt`— se implementan en las tareas 03, 06 y 08 y su especificación está en §9.1 y §10.1 del `design.md`: **MUST NOT** reimplementarse aquí ni duplicarse su lógica.

El «Paso 12 — Verificación final» del diseño se copia abajo como criterio de aceptación, pero **NO** forma parte de esta tarea: **MUST NOT** arrancarse la aplicación con `./run.sh` aquí. La compilación la verifica el motor después de implementar todas las tareas, y la comprobación en runtime la hace el usuario.

## Ficheros de esta tarea — filas verbatim de la tabla §6 del `design.md`

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/tramites/profesores/justificacion_falta_profesorado/actual/v1/recepcion/PhaseEventManagerImpl.java` | Modificar | `k-tipo-expediente` (`phaseeventmanager.md`) | Especificado en §9.2 — `triggerPresentar` y `triggerBack` |
| `src/main/java/com/educaflow/tramites/profesores/justificacion_falta_profesorado/actual/v1/recepcion/StateEventValidatorImpl.kt` | Modificar | `k-tipo-expediente` (`validator.md`), `k-secure-coding` | Especificado en §10.2 — `getForStatePendientePresentacionInEventPresentar` |
| `src/main/java/com/educaflow/tramites/profesores/justificacion_falta_profesorado/actual/v1/recepcion/views.xml` | Modificar | `k-tipo-expediente` (`vistas.md`) | Cópialo verbatim de `design/fases/recepcion/views.xml` |

No hay fila de `permisos-demo.xml`: el delta **no** añade ningún perfil ni ninguna asignación (§12). No hay fila de `TramiteInstance.xml`, `TipoExpedienteInstance.xml`, `views.xml` de la raíz de la versión, `estados.puml`, `documentospdf/*` ni de la fase `tramitacion`: el delta no los toca.

**Tampoco hay fila de `FirmaController.java`: este delta NO lo toca.** El controlador se queda **exactamente como está**, sirviendo el camino de AutoFirma (`firmarDocumentoEntrada`, que la `<action-method>` de la vista sigue llamando). El motivo está razonado en §9.1 (apartado «`FirmaDocumentoEntradaService`») y en §14: unos delegadores de firma en servidor en el controlador se quedarían **sin ningún invocador** —el `triggerPresentar` usa el servicio, ningún `.java`/`.kt` de `tramites/` inyecta un controlador y no pueden llevar `@CallMethod`—, es decir, API pública muerta.

## Identidad del trámite y del tipo (verbatim del `design.md` §2)

## 2. Identidad del trámite y del tipo

| Dato | Valor |
|---|---|
| `code` del trámite | `JustificacionFaltaProfesorado` |
| Nombre visible (`<name>`) | Justificación de falta del profesorado |
| `tipoTramite` | `PROFESOR` |
| Carpeta del trámite | `src/main/java/com/educaflow/tramites/profesores/justificacion_falta_profesorado/` |
| Carpeta de la versión | `src/main/java/com/educaflow/tramites/profesores/justificacion_falta_profesorado/actual/v1/` |
| `<defaultTipoExpediente>` | `v1` |
| `code` / entidad del tipo | `JustificacionFaltaProfesoradoV1` |
| FQN de la entidad | `com.educaflow.subsystem.expedientes.db.JustificacionFaltaProfesoradoV1` |
| `basePackageName` | `com.educaflow.tramites.profesores.justificacion_falta_profesorado.actual.v1` |
| Clase `States` (generada) | `com.educaflow.tramites.profesores.justificacion_falta_profesorado.actual.v1.States` |
| Form plantilla | `exp-JustificacionFaltaProfesoradoV1-Templates` |
| Modificación de | `src/main/java/com/educaflow/tramites/profesores/justificacion_falta_profesorado/actual/v1/` |

## Transiciones que atraviesa la fase (verbatim del `design.md` §3)

## 3. Máquina de estados

*(sin cambios)* — el delta no añade, quita ni renombra ninguna fase, ningún estado, ningún evento ni ninguna transición. El `TipoExpedienteInstance.xml` y el `estados.puml` de la versión **no se tocan**.

La acción `PRESENTAR` sigue siendo **una sola** acción del estado `RECEPCION / PENDIENTE_PRESENTACION` con **un único destino**, `TRAMITACION / PENDIENTE_RESOLUCION`: lo que cambia es **qué produce** (quién pone la firma) y **con qué botón** se dispara, no a dónde lleva. Por eso **no hay ramificación de `updateState`** y el `TipoExpedienteInstance.xml` no gana ninguna guarda.

### Transiciones que el delta atraviesa (referencia tomada del as-is)

Sirven de referencia para la cobertura de `test-e2e-desc.md`; salen del `TipoExpedienteInstance.xml` real de la versión, que no se modifica.

| fase origen | estado origen | evento | guarda | fase destino | estado destino | ¿la toca el delta? |
|---|---|---|---|---|---|---|
| `RECEPCION` | `ENTRADA_DATOS` | `GUARDAR_DATOS` | — | `RECEPCION` | `PENDIENTE_PRESENTACION` | no — camino que el delta atraviesa (no-regresión) |
| `RECEPCION` | `PENDIENTE_PRESENTACION` | `PRESENTAR` | existe certificado digital habilitado para `dniFirmaDocumentoEntrada` (firma en el servidor) | `TRAMITACION` | `PENDIENTE_RESOLUCION` | **sí** — la solicitud firmada la produce el servidor |
| `RECEPCION` | `PENDIENTE_PRESENTACION` | `PRESENTAR` | no existe certificado digital habilitado para `dniFirmaDocumentoEntrada` (firma en el equipo del profesor) | `TRAMITACION` | `PENDIENTE_RESOLUCION` | **sí** — camino conservado, pero con guardas nuevas en el validador |
| `RECEPCION` | `PENDIENTE_PRESENTACION` | `BACK` | — | `RECEPCION` | `ENTRADA_DATOS` | **sí** — descarta la clave tecleada |

**Las dos filas de `PRESENTAR` NO son una ramificación de `UPDATE_STATE`.** Comparten **la misma** pareja origen/destino (`RECEPCION / PENDIENTE_PRESENTACION` → `TRAMITACION / PENDIENTE_RESOLUCION`) y **el mismo** `updateState` en el `triggerPresentar` (§9.2, acción 5), que es incondicional. La guarda no elige estado destino: distingue únicamente **qué produce** la acción —quién pone la firma— y de ella cuelgan la condición de las acciones 2–3 del trigger y las reglas del validador (§10.2). Se desdoblan aquí porque así las escribe la especificación (`estados.md` §«Tabla de transiciones») y porque son las filas 2 y 3 de la «Cobertura de transiciones» de [`test-e2e-desc.md`](./test-e2e-desc.md), con las que esta tabla **MUST** cuadrar fila a fila.

## Pasos del diseño (verbatim del `design.md` §7)

### Paso 9 — `recepcion/PhaseEventManagerImpl.java`

- Fichero: `src/main/java/com/educaflow/tramites/profesores/justificacion_falta_profesorado/actual/v1/recepcion/PhaseEventManagerImpl.java`; FQCN `com.educaflow.tramites.profesores.justificacion_falta_profesorado.actual.v1.recepcion.PhaseEventManagerImpl`.
- Especificación quirúrgica: **§9.2 — Fase RECEPCION**. **MUST NOT** duplicarla aquí.
- Supertipo: `extends com.educaflow.subsystem.expedientes.services.eventmanager.PhaseEventManager<JustificacionFaltaProfesoradoV1>`, con el parámetro de tipo puesto.
- **Verificación:** compila; sigue habiendo un `trigger<Evento>` por cada evento de la fase (`DELETE`, `GUARDAR_DATOS`, `BACK`, `PRESENTAR`) y un `onEnter<Estado>` por cada estado (`ENTRADA_DATOS`, `PENDIENTE_PRESENTACION`), y **ninguno de más**; `triggerPresentar` ejecuta las acciones de §9.2 **en ese orden** y su `LIMPIAR(claveFirmaDocumentoEntrada)` está en un `finally` que **envuelve las acciones 2–6**, no detrás del `UPDATE_STATE`; la dependencia inyectada es `FirmaDocumentoEntradaService` y **no** `FirmaController`; `triggerDelete` sigue sin llamar a `updateState`.

### Paso 10 — `recepcion/StateEventValidatorImpl.kt`

- Fichero: `src/main/java/com/educaflow/tramites/profesores/justificacion_falta_profesorado/actual/v1/recepcion/StateEventValidatorImpl.kt`; FQCN `com.educaflow.tramites.profesores.justificacion_falta_profesorado.actual.v1.recepcion.StateEventValidatorImpl`.
- Especificación quirúrgica: **§10.2 — Fase RECEPCION**. **MUST NOT** duplicarla aquí.
- Supertipo: `StateEventValidator`.
- **Verificación:** compila; hay exactamente un método `@BeanValidationRulesForStateAndEvent` por cada pareja (estado, evento) de la fase salvo `DELETE` (tabla de cobertura de §10.2); `getForStatePendientePresentacionInEventPresentar` declara los `field(...)` y las reglas **literales** de §10.2, **sin** pasar ningún mensaje a las reglas 1 y 2 (se quedan con su valor por defecto); ningún `field(...)` menciona un campo `servidor`.

### Paso 11 — `recepcion/views.xml`

El fichero está materializado en `design/fases/recepcion/views.xml`. **Cópialo literalmente** a `src/main/java/com/educaflow/tramites/profesores/justificacion_falta_profesorado/actual/v1/recepcion/views.xml`, **sobrescribiendo** el fichero actual. **MUST NOT** modificarlo, reescribirlo ni regenerarlo.

#### Resumen estructural de las vistas

Índice de lo que declara el XML, para poder cuadrarlo sin abrirlo. **MUST NOT** volcarse el XML aquí: vive en su fichero.

**Form plantilla de la raíz de la versión** — `exp-JustificacionFaltaProfesoradoV1-Templates` (`.../actual/v1/views.xml`, **que el delta NO toca**). El delta **no añade ni quita ningún panel del almacén**; los paneles que ya declara y que la fase `RECEPCION` incluye son: `datos-profesor`, `disconformidad-view`, `datos-falta`, `justificante-upload`, `justificante-view` y `pdfSolicitud`. Los siete paneles de firma **no** están aquí: son **locales** del `<form state="PENDIENTE_PRESENTACION" profile="CREADOR">` porque solo los usa esa pantalla (`vistas.md` §3.5).

**Fase `RECEPCION`** — `design/fases/recepcion/views.xml`, una fila por `(estado, perfil)`:

| estado | profile | paneles incluidos | botones (izq / der) |
|---|---|---|---|
| `ENTRADA_DATOS` | `CREADOR` | `-datos-profesor`, `-disconformidad-view`, `datos-falta`, `justificante-upload` | `DELETE` / `GUARDAR_DATOS` |
| `ENTRADA_DATOS` | — | `-datos-profesor`, `-disconformidad-view`, `-datos-falta`, `-justificante-view` | — / `EXIT` |
| `PENDIENTE_PRESENTACION` | `CREADOR` | `-pdfSolicitud` **+ 7 paneles locales de firma** (ver abajo) | `BACK` / `PRESENTAR` **×2, excluyentes por `showIf`** |
| `PENDIENTE_PRESENTACION` | — | `-pdfSolicitud` | — / `EXIT` |

- El prefijo `-` es «panel en solo lectura»; los **dos** forms genéricos (sin `profile`, uno por estado) llevan **todos** sus paneles con `-` y su único botón `EXIT`.
- **Los 7 paneles locales de firma** del `<form state="PENDIENTE_PRESENTACION" profile="CREADOR">`, en el orden en que están en el XML:

  | panel | `showIf` (sobre `situacionFirmaDocumentoEntrada`) | contenido |
  |---|---|---|
  | `firmaSolicitudSituacion` | — (**sin `showIf`**, `readonly="true"`) | `<field situacionFirmaDocumentoEntrada showIf="false" readonly="true">` — solo alimenta los `showIf` de los demás |
  | `firmaSolicitudSinCertificado` | `== 'SIN_CERTIFICADO'` | `<help>` de AutoFirma |
  | `firmaSolicitudDispositivoConPin` | `== 'DISPOSITIVO_CON_PIN'` | `<help>` («se firmará en el servidor») |
  | `firmaSolicitudDispositivoSinPin` | `== 'DISPOSITIVO_SIN_PIN'` | `<help>` + `<field claveFirmaDocumentoEntrada title="PIN" widget="password" required>` |
  | `firmaSolicitudFicheroConClave` | `== 'FICHERO_CON_CLAVE'` | `<help>` («se firmará en el servidor») |
  | `firmaSolicitudFicheroSinClave` | `== 'FICHERO_SIN_CLAVE'` | `<help>` + `<field claveFirmaDocumentoEntrada title="Contraseña" widget="password" required>` |
  | `firmaSolicitudSinDni` | `== 'SIN_DNI'` | `<help variant="warning">` (no se puede firmar) |

- **Los dos botones `PRESENTAR`** del footer del `CREADOR` (mismo `name`, porque el `name` **es** el evento; se distinguen por `showIf` y `title`, §14):

  | `title` | `showIf` | `onClick` |
  |---|---|---|
  | Firmar con `AutoFirma__!!` y Presentar la solicitud | `== 'SIN_CERTIFICADO'` | `serial:exp-JustificacionFaltaProfesoradoV1-firmarDocumentacionParaPresentar-action,subsysExpedientes-event-action` |
  | Firmar y Presentar la solicitud | `== 'DISPOSITIVO_CON_PIN' \|\| == 'DISPOSITIVO_SIN_PIN' \|\| == 'FICHERO_CON_CLAVE' \|\| == 'FICHERO_SIN_CLAVE'` | `subsysExpedientes-event-action` |

  Con `SIN_DNI` **no se pinta ninguno** de los dos: los `showIf` no cubren ese valor y el expediente se queda donde está.
- `colSpan` del footer del `CREADOR`: `BACK` 2 + `PRESENTAR` 4 + `PRESENTAR` 4 = **10** (≤ 12).
- El `views.xml` de la fase declara además **una** `<action-method>`, `exp-JustificacionFaltaProfesoradoV1-firmarDocumentacionParaPresentar-action` (AutoFirma, 8 argumentos), que el delta **no toca**: está en el `views.xml` de **su** fase, junto al botón que la usa.

**Fase `TRAMITACION`** — su `views.xml` **no se toca** y no se materializa en el diseño; el delta no cambia ninguna de sus pantallas.

**Verificación:** el fichero existe; `diff` con el del diseño vacío; ningún `<button name="">`; el bloque de firma es **un panel por situación** (`firmaSolicitudSinCertificado`, `firmaSolicitudDispositivoConPin`, `firmaSolicitudDispositivoSinPin`, `firmaSolicitudFicheroConClave`, `firmaSolicitudFicheroSinClave`, `firmaSolicitudSinDni`) con el `showIf` en el `<panel>` y **un solo** `<field name="claveFirmaDocumentoEntrada">` por panel (nunca dos en el mismo panel); el panel `firmaSolicitudSituacion` lleva el campo derivado oculto (`showIf="false"`) y en **solo lectura** (`readonly="true"` en el panel y en el campo), y **no** tiene `showIf` de situación; los dos botones `PRESENTAR` llevan `showIf` excluyentes y sus `onClick` terminan en `subsysExpedientes-event-action`; la suma de los `colSpan` del footer es `2+4+4 = 10` (≤ 12); el form genérico de `PENDIENTE_PRESENTACION` sigue sin el bloque de firma y con su único botón `EXIT`; y **cada fila del «Resumen estructural de las vistas» de arriba cuadra con el XML**.

## Especificación del `PhaseEventManagerImpl` — Fase RECEPCION (verbatim del `design.md` §9.2)

### 9.2 Fase RECEPCION

```java
package com.educaflow.tramites.profesores.justificacion_falta_profesorado.actual.v1.recepcion;

public class PhaseEventManagerImpl extends PhaseEventManager<JustificacionFaltaProfesoradoV1> implements TareaFirmaNotifier {
    @Inject
    public PhaseEventManagerImpl(JustificacionFaltaProfesoradoV1Repository repository) {
        super(JustificacionFaltaProfesoradoV1.class);
        this.repository = repository;
    }
}
```

**Cabecera** *(sin cambios salvo lo que se indica)*:

- FQCN: `com.educaflow.tramites.profesores.justificacion_falta_profesorado.actual.v1.recepcion.PhaseEventManagerImpl`.
- Supertipo: `extends PhaseEventManager<JustificacionFaltaProfesoradoV1>`; implementa además `TareaFirmaNotifier` (`notify(TareaFirma, Object)`) *(sin cambios)*.
- Constructor `@Inject` con `JustificacionFaltaProfesoradoV1Repository` y `super(JustificacionFaltaProfesoradoV1.class)` *(sin cambios)*.
- `import` de `States`: `com.educaflow.tramites.profesores.justificacion_falta_profesorado.actual.v1.States` — el de la **propia** versión *(sin cambios)*.
- Dependencias a inyectar:
  - `RegistroEntradaRepository registroEntradaRepository` *(sin cambios)*
  - `ModelServiceFactory modelServiceFactory` *(sin cambios)*
  - **Nueva** `FirmaDocumentoEntradaService firmaDocumentoEntradaService` — la firma en servidor de la solicitud y la decisión de si corresponde (§9.1). **MUST NOT** inyectarse aquí `FirmaController`: un `PhaseEventManagerImpl` es código de dominio y no depende del paquete `controllers` (§9.1, §14).
- Constantes de clase:
  - **Nueva** `private static final Rectangulo POSICION_FIRMA_SOLICITUD = new Rectangulo(100, 20, 600, 100);`
  - **Nueva** `private static final int PAGINA_FIRMA_SOLICITUD = 1;`
  - Son exactamente el recuadro y la página que la `<action-method>` de AutoFirma ya pasa a `firmarDocumentoEntrada`, para que la firma caiga en el mismo sitio se firme donde se firme.

#### `trigger*` de la fase

- **`triggerDelete`** *(sin cambios)* — cuerpo vacío. `UPDATE_STATE: ninguno` (**MUST NOT** llamarlo: el expediente se elimina justo después).
- **`triggerGuardarDatos`** *(sin cambios)* —
  1. `GENERAR_PDF(SOLICITUD)`
  2. `CREAR_METAFILE → pdfSolicitud`
  3. `SERVICIO(TareaFirmaService.insert(TareaFirmaInsertDTO))` — alta de una tarea de firma de prueba; ya está marcada en el código como provisional. **MUST NOT** tocarse en esta iniciativa.
  4. `UPDATE_STATE(States.Recepcion.PENDIENTE_PRESENTACION)`
- **`triggerBack`** — **cambia**:
  1. **Nuevo (RN-007)** `LIMPIAR(claveFirmaDocumentoEntrada)` — descarta la clave que se hubiera tecleado antes de volver atrás. Va **la primera**, para que se descarte aunque algo fallara después.
  2. `UPDATE_STATE segun States.INSTANCE.getState(codePhase, codeState)`: `States.Recepcion.PENDIENTE_PRESENTACION → States.Recepcion.ENTRADA_DATOS`; `default → error` (`IllegalArgumentException`, `State` no es `sealed`) *(sin cambios)*.
- **`triggerPresentar`** — **cambia**. Acciones **en este orden**:
  1. `ASIGNAR(situacionFirma = expediente.getSituacionFirmaDocumentoEntrada())` — variable local; se lee **una sola vez** y esa misma lectura se usa en todo el método, para que la situación no pueda cambiar a mitad de la acción.
  2. `FIRMAR_SERVIDOR(cargo=DNI:expediente.getDniFirmaDocumentoEntrada(), rect=POSICION_FIRMA_SOLICITUD, pagina=PAGINA_FIRMA_SOLICITUD)` — **condición**: solo si `firmaDocumentoEntradaService.isFirmaEnServidor(situacionFirma)`. Se materializa como `firmaDocumentoEntradaService.firmarDocumentoEntradaEnServidor(expediente.getDniFirmaDocumentoEntrada(), situacionFirma, expediente.getClaveFirmaDocumentoEntrada(), MetaFileHelper.getDocumentoPdf(expediente.getPdfSolicitud()), new CampoFirma(POSICION_FIRMA_SOLICITUD).setNumeroPagina(PAGINA_FIRMA_SOLICITUD))` (RN-001). Si falla, el método lanza `BusinessException` y **la acción se cancela entera** (RN-002): no se registra nada y el expediente se queda en `PENDIENTE_PRESENTACION`.
  3. `CREAR_METAFILE → pdfSolicitudFirmado` — **misma condición** que la acción 2. **CRITICAL (RN-001, CC-002)**: la asignación es **incondicional dentro de la rama** y **pisa** lo que el formulario hubiera enviado en `pdfSolicitudFirmado`; es la única forma de que el cliente no pueda colar una solicitud firmada por otro cuando corresponde firmar en el servidor. Cuando **no** corresponde firmar en el servidor no se toca el campo: llega firmado desde el equipo del profesor y lo ha validado `FirmaPdf` (§10.2). Cada intento por el camino de servidor crea un `MetaFile` **nuevo**; si el evento se cancela después, ese fichero queda huérfano en el almacén de adjuntos (§14).
  4. `REGISTRO_ENTRADA(documento=pdfSolicitudFirmado, anexos=[justificante]) → pdfJustificanteRegistroEntrada` (RN-003, RN-004) *(sin cambios)*. **LIMIT**: un solo `createRegistroEntrada` por evento.
  5. `UPDATE_STATE(States.Tramitacion.PENDIENTE_RESOLUCION)` *(sin cambios)* — cruza de fase, no requiere nada especial; el `onEnter` lo atiende el manager de `TRAMITACION`.
  6. `LIMPIAR(disconformidad, resolucion)` (RN-005) *(sin cambios)*.
  - **CRITICAL — el orden 5 antes que 6 es el del as-is y MUST NOT invertirse.** El `recepcion/PhaseEventManagerImpl.triggerPresentar` real hace hoy `eventContext.updateState(States.Tramitacion.PENDIENTE_RESOLUCION)` y **después** `setDisconformidad(null)` / `setResolucion(null)`; el delta **no toca** esas dos acciones y **MUST** dejarlas exactamente donde están. Reordenarlas sería un cambio gratuito en código que el delta declara *(sin cambios)*.
  - **`finally` (envuelve las acciones 2–6)** — **Nuevo (RN-006)** `LIMPIAR(claveFirmaDocumentoEntrada)`.
    **CRITICAL — no es un paso más de la lista: es un envoltorio.** Las acciones 2 a 6 van dentro de un `try` y el `LIMPIAR` en su `finally`, de modo que la clave se descarta **igual** si la solicitud se presenta que si la acción se cancela por un fallo de firma o por una excepción.
    ❌ INCORRECTO: escribir el `LIMPIAR(claveFirmaDocumentoEntrada)` como una séptima sentencia detrás de la acción 6 — entonces un fallo de firma se lleva la ejecución por delante y la clave sobrevive en el objeto, que es justo lo que RN-006 prohíbe.
    La acción 1 (`ASIGNAR` de la variable local) queda **fuera** del `try`: no puede fallar y no ha tocado nada que haya que deshacer.
  - **MUST NOT** comprobar el trigger si el evento es disparable desde el estado actual (ya lo hizo el `Tramitador`) ni validar datos del usuario (eso es del validador, §10.2).
  - **MUST NOT** escribirse la clave —ni entera ni truncada— en ningún log ni en ningún mensaje.

#### `onEnter*` de la fase

- `onEnterEntradaDatos` — **vacío** *(sin cambios)*.
- `onEnterPendientePresentacion` — **vacío** *(sin cambios)*. En particular **MUST NOT** ponerse aquí el vaciado de la clave: el campo es transitorio y nace nulo en cada carga del formulario.

#### Lista de cobertura de la fase RECEPCION

- Eventos de la fase (unión de los `events` de `ENTRADA_DATOS` y `PENDIENTE_PRESENTACION`): `DELETE` → `triggerDelete`; `GUARDAR_DATOS` → `triggerGuardarDatos`; `BACK` → `triggerBack`; `PRESENTAR` → `triggerPresentar`. Ninguno más.
- Estados de la fase: `ENTRADA_DATOS` → `onEnterEntradaDatos`; `PENDIENTE_PRESENTACION` → `onEnterPendientePresentacion`. Ninguno más.
- **MUST NOT** factorizarse ningún `trigger*`/`onEnter*` en una superclase: el dispatcher usa `getDeclaredMethods()`.


## Especificación del `StateEventValidatorImpl` — Fase RECEPCION (verbatim del `design.md` §10.2)

### 10.2 Fase RECEPCION

```kotlin
package com.educaflow.tramites.profesores.justificacion_falta_profesorado.actual.v1.recepcion

import com.educaflow.subsystem.expedientes.db.JustificacionFaltaProfesoradoV1 as model
```

`import` que **añade** el delta:

```kotlin
import com.educaflow.subsystem.firmas.db.SituacionFirma
import com.educaflow.subsystem.expedientes.services.validation.rules.ClaveFirmaDocumentoEntradaCorrecta
import com.educaflow.subsystem.expedientes.services.validation.rules.ClaveFirmaDocumentoEntradaRequerida
import com.educaflow.subsystem.expedientes.services.validation.rules.DocumentoIdentidadParaFirmarRequerido
import com.educaflow.subsystem.expedientes.services.validation.rules.FirmanteDocumentoEntradaEsUsuarioAutenticado
```

Los `import` que ya están **MUST** conservarse tal cual.

#### Tabla de cobertura

| estado | evento | método | ¿reglas? |
|---|---|---|---|
| `ENTRADA_DATOS` | `GUARDAR_DATOS` | `getForStateEntradaDatosInEventGuardarDatos` | sí *(sin cambios)* |
| `ENTRADA_DATOS` | `DELETE` | **ninguno** (exento) | — |
| `PENDIENTE_PRESENTACION` | `BACK` | `getForStatePendientePresentacionInEventBack` | `rules { }` vacío *(sin cambios)* |
| `PENDIENTE_PRESENTACION` | `PRESENTAR` | `getForStatePendientePresentacionInEventPresentar` | sí — **cambia** |

#### `getForStateEntradaDatosInEventGuardarDatos`

*(sin cambios)* — conserva sus nueve `field(...)` (`dias`, `mes`, `anyo`, `tipoJornadaFalta`, `horaInicio`, `horaFin`, `motivoFalta`, `otroMotivo`, `justificante`) con sus reglas y argumentos actuales. **MUST NOT** tocarse.

#### `getForStatePendientePresentacionInEventBack`

*(sin cambios)* — `rules { }` vacío, declarado explícitamente. Consecuencia deliberada: en `BACK` el `AllowProperties` queda vacío y el cliente **no** puede dictar ningún campo, ni siquiera la clave; el `LIMPIAR` de `triggerBack` (RN-007) es defensa en profundidad.

#### `getForStatePendientePresentacionInEventPresentar` — **cambia**

```kotlin
@BeanValidationRulesForStateAndEvent
fun getForStatePendientePresentacionInEventPresentar(): BeanValidationRules {
    return rules {
        field(model::getClaveFirmaDocumentoEntrada) {
            +DocumentoIdentidadParaFirmarRequerido(model::getSituacionFirmaDocumentoEntrada)
            +FirmanteDocumentoEntradaEsUsuarioAutenticado(model::getDniFirmaDocumentoEntrada)
            +ClaveFirmaDocumentoEntradaRequerida(model::getSituacionFirmaDocumentoEntrada)
            +ClaveFirmaDocumentoEntradaCorrecta(model::getSituacionFirmaDocumentoEntrada, model::getDniFirmaDocumentoEntrada)
        }
        field(model::getPdfSolicitudFirmado) {
            +ifValueIn(model::getSituacionFirmaDocumentoEntrada, listOf(SituacionFirma.SIN_CERTIFICADO)) {
                +Required()
                +FirmaPdf(model::getPdfSolicitud, model::getDniFirmaDocumentoEntrada)
            }
        }
    }
}
```

- El **orden** de las reglas dentro del primer `field(...)` es normativo: la que abre el certificado (`ClaveFirmaDocumentoEntradaCorrecta`) va la **última**, porque es la única que hace trabajo criptográfico y no tiene sentido hacerlo sin DNI, sin titularidad o sin clave.
- **VAL-…-006** (`Required`) y **VAL-…-007** (`FirmaPdf`) se conservan **con la guarda nueva** `ifValueIn(situación == SIN_CERTIFICADO)`: siguen siendo **la** defensa del camino de firma en el equipo del profesor, y no se aplican cuando corresponde firmar en el servidor porque entonces la solicitud firmada todavía **no existe** en el momento de validar — la produce la propia acción (§9.2, acciones 2 y 3).

#### Frontera de confianza (CRITICAL)

- Campos que el cliente puede dictar en (`PENDIENTE_PRESENTACION`, `PRESENTAR`): **exactamente dos**, `claveFirmaDocumentoEntrada` y `pdfSolicitudFirmado`. Ningún otro `field(...)` de primer nivel.
- `claveFirmaDocumentoEntrada` es `usuario`: es el único dato que la persona aporta en este estado, es `transient` (no llega a base de datos) y el trigger lo descarta al terminar.
- `pdfSolicitudFirmado` es `servidor` a efectos del modelo pero **sí** aparece en el validador: es la excepción documentada del patrón de **firma en cliente**, porque es el único sitio donde la firma que pone AutoFirma llega al servidor y se puede comprobar. Cuando corresponde firmar en el servidor, lo que el formulario envíe se **descarta** al pisarlo el trigger (§9.2, acción 3).
- **MUST NOT** aparecer en ningún `field(...)`: `situacionFirmaDocumentoEntrada`, `dniFirmaDocumentoEntrada`, `pdfSolicitud`, `pdfJustificanteRegistroEntrada`, `codePhase`, `codeState`, `abierto`, `centro` ni `usuarioRegistrador`. `situacionFirmaDocumentoEntrada` y `dniFirmaDocumentoEntrada` se usan **solo como argumento** de reglas, que no abre la whitelist.
- **MUST NOT** confiarse en los `showIf` ni en el `readonly` de la vista: no son defensa. Que un botón no se vea no impide enviar el evento; lo que impide firmar mal son estas reglas y el hecho de que, en el camino de servidor, la solicitud firmada la produzca el propio servidor.
- **CRITICAL** — esta puerta **NO** protege el endpoint REST automático `POST /ws/rest/com.educaflow.subsystem.expedientes.db.JustificacionFaltaProfesoradoV1`, que Axelor publica para toda entidad y **no pasa por el `Tramitador`**. Son dos puertas distintas y este delta no cambia esa situación. **MUST NOT** introducirse un `ModelService` deny-all de expedientes como parche puntual: se retiró a propósito.


## Reparto de reglas (verbatim del `design.md` §11)

## 11. Reparto de reglas

| Tipo de regla | Capa | Cómo se escribe |
|---|---|---|
| Tipo, longitud máxima de columna, referencia, enumerado | **modelo XML** (`domains.xml`) | atributos de `<string>`/`<enum>`; aquí, `transient`, `password` y el cuerpo del campo derivado |
| Obligatoriedad de un campo **en un evento** | **DSL del validador** | `+Required()` / regla propia en la pareja (estado, evento) |
| Formato, rango, comparación entre campos, tipo/tamaño de fichero, firma | **DSL del validador** | `Pattern`, `MinValue`/`MaxValue`, `GreaterThan`, `FileType`/`FileMaxSize`, `FirmaPdf` |
| Obligatoriedad **condicional** | **DSL del validador** | `ifValueIn(...) { … }` o la condición interna de la regla propia |
| Qué campos puede dictar el cliente en un evento | **DSL del validador** | el conjunto de `field(...)` de esa pareja (§10.2) |
| Efectos: generar PDF, firmar, registrar, transicionar, limpiar | **`trigger*`** del `PhaseEventManagerImpl` | lista de acciones de §9.2 |
| Inicialización del expediente | **`triggerInitialEvent`** | §8 |
| Mostrar/ocultar/deshabilitar, ayudas, confirmaciones | **vista** | `showIf`, `<help>`, `prompt` — **solo UX, NUNCA defensa** |

Reglas duras: **MUST NOT** usarse `required="true"` en el `domains.xml`; **MUST NOT** validarse datos de usuario en un `trigger*`; **MUST NOT** ponerse lógica de negocio en el validador; **MUST NOT** tratarse una regla de vista como si fuera una validación.

### Ubicación de cada regla de la especificación

| Regla | Capa | Dónde |
|---|---|---|
| CC-001 — cómo se va a firmar la solicitud | modelo XML (campo derivado) | `situacionFirmaDocumentoEntrada` en `domains.xml` (§4.1), calculado por `SituacionFirmaBuilder.buildByDni` (§9.1) |
| CC-002 — la solicitud firmada | `trigger*` + validador | `triggerPresentar` acciones 2–3 (camino servidor) y `field(pdfSolicitudFirmado)` con `FirmaPdf` (camino cliente) |
| CC-003 — justificante del registro de entrada | `trigger*` | `triggerPresentar` acción 4 *(sin cambios)* |
| CC-004 — clave con la que se abre el certificado | modelo XML + servicio | `claveFirmaDocumentoEntrada` (§4.1); que **la custodiada gane a la tecleada** ya lo decide `CertificadoDigitalService.getAlmacenClaveByDni(dni, clave)` (§9.1). **Cubierta solo en las situaciones de certificado en fichero:** en `DISPOSITIVO_CON_PIN` / `DISPOSITIVO_SIN_PIN` ese mismo método **descarta** la clave recibida, así que la parte «y solo si no la custodia, la que el profesor teclea» de CC-004 queda **cableada pero no operativa** para dispositivo criptográfico (§14 «Situaciones sin escenario») |
| VAL-…-001 — el profesor tiene documento de identidad | DSL del validador | `DocumentoIdentidadParaFirmarRequerido` |
| VAL-…-002 — la contraseña está rellena | DSL del validador | `ClaveFirmaDocumentoEntradaRequerida` (rama `FICHERO_SIN_CLAVE`) |
| VAL-…-003 — el PIN está relleno | DSL del validador | `ClaveFirmaDocumentoEntradaRequerida` (rama `DISPOSITIVO_SIN_PIN`) |
| VAL-…-004 — la contraseña tecleada abre el certificado | DSL del validador | `ClaveFirmaDocumentoEntradaCorrecta` (rama `FICHERO_SIN_CLAVE`) |
| VAL-…-005 — la clave custodiada abre el certificado | DSL del validador | `ClaveFirmaDocumentoEntradaCorrecta` (rama `FICHERO_CON_CLAVE`) |
| VAL-…-006 — la solicitud está firmada | DSL del validador | `field(pdfSolicitudFirmado) { ifValueIn(SIN_CERTIFICADO) { Required() } }` |
| VAL-…-007 — la firma de la solicitud es válida | DSL del validador | `field(pdfSolicitudFirmado) { ifValueIn(SIN_CERTIFICADO) { FirmaPdf(...) } }` |
| VAL-…-008 — quien lanza la acción es quien firma | DSL del validador | `FirmanteDocumentoEntradaEsUsuarioAutenticado` |
| RN-001 — el servidor firma y descarta lo que envió el formulario | `trigger*` | `triggerPresentar` acciones 2–3 |
| RN-002 — si la firma falla se cancela la acción entera | `trigger*` + pieza compartida | `BusinessException` de `FirmaDocumentoEntradaService.firmarDocumentoEntradaEnServidor`, cuyo motivo lo da `FirmaEnServidorHelper.motivoFirmaFallida` (§9.1); el `Tramitador` hace `detach` y no persiste nada |
| RN-003 — constancia de la entrada | `trigger*` | `triggerPresentar` acción 4 *(sin cambios)* |
| RN-004 — guardar el justificante de la entrada | `trigger*` | `triggerPresentar` acción 4 *(sin cambios)* |
| RN-005 — limpiar disconformidad y motivo del rechazo | `trigger*` | `triggerPresentar` acción 6 *(sin cambios)* |
| RN-006 — descartar la clave al terminar `PRESENTAR` | `trigger*` + controlador | el `finally` de `triggerPresentar` que **envuelve las acciones 2–6** (§9.2) y el `response.setValue(FirmaDocumentoEntradaService.CAMPO_CLAVE_FIRMA_DOCUMENTO_ENTRADA, null)` de `ExpedienteController` cuando la acción se cancela |
| RN-007 — descartar la clave al volver atrás | `trigger*` | `triggerBack` acción 1 (y el `rules { }` vacío de `BACK`, que ni siquiera la copia) |
| RUI-…-CREADOR-001 a 005 — los cinco avisos excluyentes | vista | un `<panel>` por situación de firma, cada uno con su `showIf` sobre `situacionFirmaDocumentoEntrada` y su `<help>` dentro, en `fases/recepcion/views.xml` |
| RUI-…-CREADOR-006 y 007 — los dos botones excluyentes | vista | los dos `<button name="PRESENTAR">` con `showIf` excluyentes |
| RUI-…-CREADOR-008 — la clave se muestra enmascarada | vista | `widget="password"` en el campo |
| RUI-…-CREADOR-009 — la clave aparece vacía al abrir | modelo XML | el campo es `transient`: nace nulo en cada carga del formulario. **No hace falta ninguna acción de vista** |
| RUI-…-CREADOR-010 — sin campo de clave en el camino de AutoFirma | vista | el panel de `SIN_CERTIFICADO` no lleva ningún `<field>` de clave |
| RUI-…-CREADOR-011 — la clave vuelve vacía tras un intento cancelado | controlador | `response.setValue(FirmaDocumentoEntradaService.CAMPO_CLAVE_FIRMA_DOCUMENTO_ENTRADA, null)` en `ExpedienteController` (§9.1) |
| RUI-…-GENERICA-001 — nada del bloque de firma sin el turno | vista | los paneles de firma solo existen en el `<form state="PENDIENTE_PRESENTACION" profile="CREADOR">`; el genérico no los lleva |

Ninguna regla de la especificación se descarta.

## Notas y supuestos que aplican a esta tarea (verbatim del `design.md` §14)

- **Dos `<button name="PRESENTAR">` en el mismo footer.** Es la única forma de cumplir RUI-…-006/007: el `name` de un botón **es** el evento que dispara (`_signal`), así que los dos botones excluyentes tienen que llamarse igual y distinguirse por `showIf` y por `title`. El preprocesador de vistas copia los botones del `<footer>` verbatim, con todos sus atributos, y `axelor-front` da a cada widget de un formulario una clave propia (`uid` único, no el `name`), así que la duplicidad no colisiona. Aun así, el paso 12 lo comprueba en runtime porque no hay ningún test que lo cubra.
- **`required="true"` en el campo de la clave — riesgo cerrado, se conserva.** Lo pide RUI-…-003/004 («marcado como obligatorio») y es solo la marca visual: la obligatoriedad real la impone `ClaveFirmaDocumentoEntradaRequerida` en el servidor. Se llegó a plantear que el cliente bloqueara el envío del evento con el campo vacío y rompiera ESC-004; **no ocurre**, y consta por tres evidencias:
  1. en `axelor-front`, `views/form/widgets/button/button.tsx` → `handleClick` confirma el `prompt`, hace `commitEditableWidgets()` y llama directamente a `actionExecutor.execute(onClick, …)`: **no valida** los campos `required` del formulario en ningún punto;
  2. `subsysExpedientes-event-action` es una `<action-method>` pura —no hay ningún `save` en la cadena— y es el guardado, no el `action-method`, lo que dispara la validación de obligatoriedad del cliente;
  3. el precedente en producción hace exactamente esto: `subsystem/firmas/views/Pendiente-TareaFirma.xml` pone `required="true"` sobre `claveFirma` y su botón encadena `action-method`s. Es más: **precisamente porque el `required` no frena nada**, allí se añadió una `<action-validate>` explícita en el cliente además de la comprobación del servidor (V-TareaFirma-005/006). Aquí **MUST NOT** añadirse esa `action-validate`: ESC-004 exige que el intento llegue al servidor y devuelva su mensaje.
  Por tanto **MUST** conservarse el `required="true"`; el paso 12 se limita a comprobar ESC-004 como cualquier otro escenario, no como un riesgo abierto.
- **El bloque de firma es un panel por situación, no un panel con seis condiciones.** Se sigue literalmente el precedente en producción `src/main/java/com/educaflow/subsystem/firmas/views/Pendiente-TareaFirma.xml`, donde cada situación de firma tiene **su propio `<panel>`** con su `showIf`, su `<help>` dentro y, si hace falta, su `<field>` de la clave. Así no hay dos `<field name="claveFirmaDocumentoEntrada">` en el mismo panel —que es lo que obliga a hacer un panel único— y añadir o quitar una situación es añadir o quitar un panel. El campo derivado que alimenta los `showIf` va oculto en un panel propio **sin `showIf`** (`firmaSolicitudSituacion`), igual que el `situacionFirma` oculto del precedente, y además **`readonly="true"`** —en el panel y en el propio `<field>`— porque es un campo `servidor` (lo calcula su getter derivado) y ningún campo `servidor` puede quedar editable en la vista (`vistas.md` §8). Ese `readonly` **no es una defensa**, igual que no lo es el `showIf`: lo que impide firmar mal son las reglas del `StateEventValidatorImpl`, y el campo ni siquiera aparece en ningún `field(...)` (§4.1, §10.2). Los siete paneles (los seis de situación más el del campo oculto) son locales del `<form state=… profile="CREADOR">` y **MUST NOT** subir al almacén de paneles de la raíz: solo los usa esa pantalla (`vistas.md` §3.5).
- **`MetaFile` huérfanos al firmar en el servidor.** La acción 3 de `triggerPresentar` crea un `MetaFile` **nuevo** con la solicitud firmada en **cada** intento. Si el evento se cancela después (una validación, un fallo del registro de entrada), el `detach` del `Tramitador` revierte la entidad pero **no** el almacén de adjuntos, así que ese fichero queda subido y sin referencia. Es exactamente lo que ya ocurre hoy con el camino de AutoFirma —que también sube el firmado antes del evento— y la limpieza de adjuntos huérfanos queda **fuera del alcance** de esta iniciativa.

## Criterio de aceptación del diseño — NO se ejecuta en esta tarea (verbatim del `design.md` §7, Paso 12)

### Paso 12 — Verificación final

Ejecuta:

```
./run.sh
```

Qué se comprueba:

- `BUILD SUCCESSFUL`, con los tests de `com/educaflow/tiposexpedientes` y `com/educaflow/views` en verde (los ejecuta ese mismo build). En particular X1/X2/X3 (vistas por estado) y Y1/Y2/Y3 (botones del footer) siguen pasando con los **dos** botones `PRESENTAR`.
- Que se regeneró `estados.png` (`GenerateDocs` va enganchada a `build` con `finalizedBy`), aunque el `.puml` no se toque.
- **REQUIRED — comprobación en runtime**, porque los tests cubren la forma y no el comportamiento. Recorrer con usuarios reales las **seis** situaciones de firma y comprobar, expediente en mano:
  - que el campo derivado `situacionFirmaDocumentoEntrada` llega al cliente y que los `showIf` de los **paneles por situación** pintan **exactamente un** panel de firma y **como mucho un** botón de firmar;
  - que **dos botones con el mismo `name="PRESENTAR"`** conviven en el mismo footer y que el que se pulsa envía `_signal = PRESENTAR` (§14);
  - **ESC-004** — que dejando la «Contraseña» vacía y pulsando «Firmar y Presentar la solicitud» el servidor responde «La contraseña es obligatoria» y el expediente sigue en `PENDIENTE_PRESENTACION`;
  - que la solicitud firmada en servidor abre correctamente y lleva la firma en el mismo recuadro y página que la firmada con AutoFirma;
  - que el registro de entrada se crea con la solicitud firmada como documento principal y el justificante como anexo (`personaSolicitante`/`personaInteresada` no nulos: si lo fueran, `createRegistroEntrada` lanza NPE y **nada lo verifica en build**);
  - que el camino de AutoFirma sigue funcionando igual (`dniFirmaDocumentoEntrada` no nulo y válido: si no, `FirmaController.firmarDocumentoEntrada` lanza y **nada lo verifica en build**);
  - que la clave tecleada **no** aparece en `logs/` ni en ninguna respuesta del servidor.

## Instrucciones

- Acción de las tres filas §6: **Modificar**.
- `recepcion/views.xml` está **materializado** en `/home/logongas/Documentos/desarrollo/educaflow/secretaria-virtual/.sdd/drafts/2026-09-02_23-52_firma-en-servidor-justificacion-falta-profesorado/design/fases/recepcion/views.xml`. **Cópialo literalmente** a `src/main/java/com/educaflow/tramites/profesores/justificacion_falta_profesorado/actual/v1/recepcion/views.xml`, **sobrescribiendo** el fichero actual. **MUST NOT** modificarlo, reescribirlo, regenerarlo ni transcribirlo a mano.
- `PhaseEventManagerImpl.java` y `StateEventValidatorImpl.kt` **ya existen**: se modifican **en su sitio**, respetando todo lo que el diseño marca *(sin cambios)*.
- **MUST NOT** ejecutarse `CreateFilesTask`: el delta no añade ninguna fase y no hay ningún esqueleto que generar.
- **La especificación de arriba es contrato fijo y la superficie es cerrada**: MUST NOT crearse ningún método, clase, campo, constante ni acción que esa especificación no liste, y MUST NOT modificarse ningún fichero que no sea el de esta tarea.
