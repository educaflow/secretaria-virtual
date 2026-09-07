---
type: design
template: expediente
---

# Diseño: Justificación de falta del profesorado

> **Iniciativa de MODIFICACIÓN de una versión existente.** El diseño es un **delta** sobre la carpeta de versión
> `src/main/java/com/educaflow/tramites/profesores/justificacion_falta_profesorado/actual/v1`, que se modifica **en su sitio**.
> Las secciones cuyo contenido no cambia se marcan `*(sin cambios)*`; «Identidad del trámite y del tipo», «Ficheros a crear o modificar» y «Pasos» van siempre completas.

## 1. Objetivo

Permitir que el profesorado que presenta una justificación de falta firme su solicitud en el servidor con el certificado digital que la secretaría virtual custodie para su documento de identidad, en lugar de tener que firmarla siempre desde su propio equipo con AutoFirma.

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

## 4. Modelo

El delta **no crea ninguna entidad ni ningún enum**: reutiliza el enum `com.educaflow.subsystem.firmas.db.SituacionFirma` que ya existe en el subsistema de firmas. Solo añade **dos campos transitorios** a la entidad del tipo.

### 4.1 Campos que añade el delta a `JustificacionFaltaProfesoradoV1`

| nombre | tipo | ref | title | para qué sirve | quién lo rellena |
|---|---|---|---|---|---|
| `claveFirmaDocumentoEntrada` | `string` (`transient="true"`, `password="true"`) | — | Firma de la solicitud | La contraseña del fichero del certificado o el PIN del dispositivo criptográfico que teclea quien firma, cuando la secretaría virtual **no** custodia esa clave. Dato de un solo uso | `usuario` |
| `situacionFirmaDocumentoEntrada` | `enum` (`transient="true"`, **derivado**, con cuerpo) | `com.educaflow.subsystem.firmas.db.SituacionFirma` | Situación de firma de la solicitud | Cómo se va a firmar la solicitud: se deduce de `dniFirmaDocumentoEntrada` y del certificado digital **habilitado** dado de alta para ese DNI (CC-001) | `servidor` |

Los demás campos de la entidad *(sin cambios)*.

Reglas que hay que hacer explícitas:

- **`claveFirmaDocumentoEntrada` es `transient`**: nunca llega a base de datos, así que la clave no se persiste ni queda en el histórico. Es el mismo mecanismo que `TareaFirma.claveFirma`.
- **El nombre del campo es contrato del subsistema, no una convención tácita.** `ExpedienteController` lo necesita para devolverlo vacío tras un intento cancelado (RUI-011, §9.1), y **MUST NOT** incrustarse a mano allí: el nombre vive en **una sola** constante pública,
  `com.educaflow.subsystem.expedientes.services.internal.FirmaDocumentoEntradaService.CAMPO_CLAVE_FIRMA_DOCUMENTO_ENTRADA = "claveFirmaDocumentoEntrada"` (§9.1).
  Todo tipo de expediente que quiera firma en servidor **MUST** llamar así a su campo transitorio de la clave: es el nombre que esa constante declara y el único que el subsistema sabe vaciar.
- **El `title` del campo es deliberadamente neutro (`Firma de la solicitud`) y NO nombra la clave.** `FieldValidationRules` antepone **siempre** el `title` del `field(...)` del que cuelga cada regla y el visor pinta `«<title>: <mensaje>»`; como el DSL no permite colgar una regla fuera de un `field(...)`, las reglas VAL-…-001 y VAL-…-008 —que no hablan de la clave— colgarían de una etiqueta «Clave del certificado digital» que no viene a cuento (§14).
  La pantalla no se ve afectada: la vista sobrescribe el `title` por «Contraseña» y por «PIN» en el panel de cada situación (§10.2, `fases/recepcion/views.xml`).
- **`situacionFirmaDocumentoEntrada` es un campo derivado**: su cuerpo es `return com.educaflow.subsystem.firmas.util.SituacionFirmaBuilder.buildByDni(getDniFirmaDocumentoEntrada());`, se **recalcula en cada lectura** y no se persiste. Es la única excepción a la regla «todo campo `servidor` está asignado por alguna acción de §8 o §9»: lo asigna su propio getter, y por eso **MUST NOT** aparecer en ningún `field(...)` del validador ni ser editable en ninguna vista. Que se recalcule en cada lectura es justo lo que hace que las comprobaciones de `PRESENTAR` se hagan contra la situación **real del servidor en el momento de presentar** y no contra lo que la pantalla tuviera pintado.
- El getter que Axelor genera para un campo derivado solo captura `NullPointerException`, así que `SituacionFirmaBuilder.buildByDni` **MUST NOT** propagar ninguna excepción (§9.1).
- **MUST NOT** ponerse `required="true"` en ninguno de los dos campos: la obligatoriedad de la clave es **por pareja (estado, evento)** y vive en el DSL del validador (§10).
- **MUST NOT** redeclararse ningún campo heredado de `Expediente`; `dniFirmaDocumentoEntrada` se **referencia**, nunca se redeclara.

> **Por qué los dos campos van en el `domains.xml` del tipo y no en `Expediente`.** Sería más reutilizable declararlos en `Expediente`, junto a `dniFirmaDocumentoEntrada`, pero **no funcionaría**: `FieldValidationRules.getLabel` resuelve la etiqueta del campo con `clazz.getDeclaredField(...)` sobre la **clase concreta** del expediente, que **no** recorre superclases, así que un `field(model::getClaveFirmaDocumentoEntrada)` sobre un campo heredado revienta con `«El campo … no existe en la clase …»`. Cada tipo que quiera firma en servidor declara estos dos campos en su propio `domains.xml` (seis líneas); todo lo demás —el cálculo de la situación, la firma y las reglas de validación— es compartido (§9.1, §10.1).

### 4.2 `design/domains.xml` materializado

`design/domains.xml` es el fichero real de la versión **más** los dos campos anteriores. Se copia verbatim y sobrescribe.

## 5. Documentos PDF

*(sin cambios)* — el trámite sigue generando exactamente los mismos dos documentos, con los mismos ficheros de definición, el mismo contenido y el mismo recuadro de firma. **MUST NOT** tocarse `documentospdf/solicitud.xml`, `documentospdf/resolucion.xml` ni `documentospdf/_template.xml`, ni el bloque `<extra-code-model>` del `domains.xml`.

| fichero | constante | en qué transición se genera | en qué campo se guarda | quién lo firma | se registra |
|---|---|---|---|---|---|
| `documentospdf/solicitud.xml` | `SOLICITUD` | `RECEPCION.ENTRADA_DATOS` + `GUARDAR_DATOS` (se genera) / `RECEPCION.PENDIENTE_PRESENTACION` + `PRESENTAR` (se firma) | `pdfSolicitud` (`servidor`) → `pdfSolicitudFirmado` (`servidor`) | **cambia**: `cliente (AutoFirma__!!)` cuando no hay certificado custodiado; `servidor: DNI dniFirmaDocumentoEntrada` cuando sí lo hay | `entrada` |
| `documentospdf/resolucion.xml` | `RESOLUCION` | `TRAMITACION.PENDIENTE_RESOLUCION` + `RESOLVER` | `pdfResolucion` (`servidor`) | `servidor: DIRECTOR` *(sin cambios)* | `salida` |

Fragmentos: `documentospdf/_template.xml`, incluido por `solicitud.xml` y por `resolucion.xml` *(sin cambios)*.

El recuadro y la página de la firma de la solicitud son **los mismos** se firme donde se firme: `x=100, y=20, ancho=600, alto=100`, página `1` — los ocho argumentos que ya usa hoy la `<action-method>` de AutoFirma. En el camino de servidor los aporta la constante `POSICION_FIRMA_SOLICITUD` del `PhaseEventManagerImpl` de `RECEPCION` (§9.2).

## 6. Ficheros a crear o modificar

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/tramites/profesores/justificacion_falta_profesorado/actual/v1/domains.xml` | Modificar | `k-tipo-expediente` (`modelo.md`) | Cópialo verbatim de `design/domains.xml`. Añade los dos campos transitorios de §4.1 |
| `src/main/java/com/educaflow/subsystem/firmas/util/SituacionFirmaBuilder.java` | Modificar | `k-sistemas`, `k-secure-coding` | Especificado en §9.1 — nuevo `buildByDni(String)`; `build(User)` pasa a delegar en él; `enmascararDni` pasa a package-private |
| `src/main/java/com/educaflow/subsystem/firmas/util/FirmaEnServidorHelper.java` | Crear | `k-sistemas`, `k-code-quality`, `k-secure-coding` | Especificado en §9.1 — **única** fuente de verdad de «la clave no es correcta» y de sus mensajes |
| `src/main/java/com/educaflow/subsystem/firmas/service/impl/TareaFirmaServiceImpl.java` | Modificar | `k-sistemas`, `k-code-quality` | Especificado en §9.1 — sus cuatro métodos privados pasan a **delegar** en `FirmaEnServidorHelper`, sin cambiar comportamiento ni literales |
| `src/main/java/com/educaflow/subsystem/criptografia/service/AlmacenClaveResolver.java` | Modificar | `k-sistemas` | Especificado en §9.1 — nueva sobrecarga `getByDNI(String dni, String claveAcceso)` |
| `src/main/java/com/educaflow/subsystem/expedientes/services/internal/FirmaDocumentoEntradaService.java` | Crear | `k-sistemas`, `k-guice`, `k-secure-coding` | Especificado en §9.1 — firma en servidor del documento de entrada, reutilizable por cualquier tipo de expediente; declara la constante del nombre del campo de la clave |
| `src/main/java/com/educaflow/subsystem/expedientes/controllers/ExpedienteController.java` | Modificar | `k-sistemas`, `k-secure-coding` | Especificado en §9.1 — al cancelarse un evento por una validación, devuelve la clave de firma vacía usando la constante del servicio (RUI-011) |
| `src/main/java/com/educaflow/subsystem/expedientes/services/validation/rules/FirmaDocumentoEntradaRules.kt` | Crear | `k-validaciones`, `k-secure-coding` | Especificado en §10.1 — las cuatro reglas nuevas del DSL de validación |
| `src/main/java/com/educaflow/tramites/profesores/justificacion_falta_profesorado/actual/v1/recepcion/PhaseEventManagerImpl.java` | Modificar | `k-tipo-expediente` (`phaseeventmanager.md`) | Especificado en §9.2 — `triggerPresentar` y `triggerBack` |
| `src/main/java/com/educaflow/tramites/profesores/justificacion_falta_profesorado/actual/v1/recepcion/StateEventValidatorImpl.kt` | Modificar | `k-tipo-expediente` (`validator.md`), `k-secure-coding` | Especificado en §10.2 — `getForStatePendientePresentacionInEventPresentar` |
| `src/main/java/com/educaflow/tramites/profesores/justificacion_falta_profesorado/actual/v1/recepcion/views.xml` | Modificar | `k-tipo-expediente` (`vistas.md`) | Cópialo verbatim de `design/fases/recepcion/views.xml` |

No hay fila de `permisos-demo.xml`: el delta **no** añade ningún perfil ni ninguna asignación (§12). No hay fila de `TramiteInstance.xml`, `TipoExpedienteInstance.xml`, `views.xml` de la raíz de la versión, `estados.puml`, `documentospdf/*` ni de la fase `tramitacion`: el delta no los toca.

**Tampoco hay fila de `FirmaController.java`: este delta NO lo toca.** El controlador se queda **exactamente como está**, sirviendo el camino de AutoFirma (`firmarDocumentoEntrada`, que la `<action-method>` de la vista sigue llamando). El motivo está razonado en §9.1 (apartado «`FirmaDocumentoEntradaService`») y en §14: unos delegadores de firma en servidor en el controlador se quedarían **sin ningún invocador** —el `triggerPresentar` usa el servicio, ningún `.java`/`.kt` de `tramites/` inyecta un controlador y no pueden llevar `@CallMethod`—, es decir, API pública muerta.

## 7. Pasos

### Paso 1 — `domains.xml` del tipo

El fichero está materializado en `design/domains.xml`. **Cópialo literalmente** a `src/main/java/com/educaflow/tramites/profesores/justificacion_falta_profesorado/actual/v1/domains.xml`, **sobrescribiendo** el fichero actual. **MUST NOT** modificarlo, reescribirlo ni regenerarlo.

**Verificación:** el fichero existe; `diff` con el del diseño vacío; el fichero declara los dos campos nuevos, ninguno con `required="true"`, y la entidad `JustificacionFaltaProfesoradoV1` sigue siendo la **primera** `<entity>` con `extends="Expediente"`.

### Paso 2 — `SituacionFirmaBuilder.java`

- Fichero: `src/main/java/com/educaflow/subsystem/firmas/util/SituacionFirmaBuilder.java`; FQCN `com.educaflow.subsystem.firmas.util.SituacionFirmaBuilder`.
- Especificación quirúrgica: **§9.1**, apartado «`SituacionFirmaBuilder`». **MUST NOT** duplicarla aquí.
- **Verificación:** compila; existe `public static SituacionFirma buildByDni(String dni)`; `build(User)` delega en él y no duplica la lógica; ningún método propaga excepción; `enmascararDni` es visible desde el paquete (`FirmaEnServidorHelper` la reutiliza) y el DNI solo aparece enmascarado en el log.

### Paso 3 — `FirmaEnServidorHelper.java`

- Fichero: `src/main/java/com/educaflow/subsystem/firmas/util/FirmaEnServidorHelper.java`; FQCN `com.educaflow.subsystem.firmas.util.FirmaEnServidorHelper`.
- Especificación quirúrgica: **§9.1**, apartado «`FirmaEnServidorHelper`». **MUST NOT** duplicarla aquí.
- **Verificación:** compila; existen los **cuatro** métodos públicos de §9.1 con los mensajes **literales** que hoy tiene `TareaFirmaServiceImpl`; ningún método propaga excepción; ninguna traza contiene la clave ni un fragmento de ella; el DNI solo aparece enmascarado.

### Paso 4 — `TareaFirmaServiceImpl.java`

- Fichero: `src/main/java/com/educaflow/subsystem/firmas/service/impl/TareaFirmaServiceImpl.java`; FQCN `com.educaflow.subsystem.firmas.service.impl.TareaFirmaServiceImpl`.
- Especificación quirúrgica: **§9.1**, apartado «`TareaFirmaServiceImpl`».
- **Verificación:** compila; los cuatro métodos privados (`isClaveIncorrecta`, `motivoFirmaFallida`, `isClaveCertificadoIncorrecta`, `mensajeClaveCertificadoIncorrecta`) son ahora **una sola línea** que delega en `FirmaEnServidorHelper`; **ningún** literal de mensaje ni la constante `LIMITE_CAUSAS_A_RECORRER` quedan duplicados en la clase; `validateFirmarEnServidor` y `firmarDocumentosEnMemoria` **no cambian** de comportamiento ni de mensajes (V-TareaFirma-001..008 intactas).

### Paso 5 — `AlmacenClaveResolver.java`

- Fichero: `src/main/java/com/educaflow/subsystem/criptografia/service/AlmacenClaveResolver.java`; FQCN `com.educaflow.subsystem.criptografia.service.AlmacenClaveResolver`.
- Especificación quirúrgica: **§9.1**, apartado «`AlmacenClaveResolver`».
- **Verificación:** compila; existe `public AlmacenClave getByDNI(String dni, String claveAcceso)`; el `getByDNI(String)` anterior sigue existiendo con el mismo comportamiento.

### Paso 6 — `FirmaDocumentoEntradaService.java`

- Fichero: `src/main/java/com/educaflow/subsystem/expedientes/services/internal/FirmaDocumentoEntradaService.java`; FQCN `com.educaflow.subsystem.expedientes.services.internal.FirmaDocumentoEntradaService`.
- Especificación quirúrgica: **§9.1**, apartado «`FirmaDocumentoEntradaService`». **MUST NOT** duplicarla aquí.
- **Verificación:** compila; existen `isFirmaEnServidor(SituacionFirma)`, `firmarDocumentoEntradaEnServidor(...)` y la constante `CAMPO_CLAVE_FIRMA_DOCUMENTO_ENTRADA`; la detección de «clave incorrecta» y sus mensajes **no** se reimplementan aquí, se piden a `FirmaEnServidorHelper`; ninguna traza ni ningún mensaje de error contiene la clave ni un fragmento de ella; la clase **no** lleva ningún `@CallMethod`.

### Paso 7 — `ExpedienteController.java`

- Fichero: `src/main/java/com/educaflow/subsystem/expedientes/controllers/ExpedienteController.java`; FQCN `com.educaflow.subsystem.expedientes.controllers.ExpedienteController`.
- Especificación quirúrgica: **§9.1**, apartado «`ExpedienteController`».
- **Verificación:** compila; la rama `catch (BusinessException)` de `triggerEvent` devuelve además el valor vacío de la clave de firma **usando la constante** `FirmaDocumentoEntradaService.CAMPO_CLAVE_FIRMA_DOCUMENTO_ENTRADA`, sin ningún literal de nombre de campo en el fichero; el resto del método no cambia.

### Paso 8 — `FirmaDocumentoEntradaRules.kt`

- Fichero: `src/main/java/com/educaflow/subsystem/expedientes/services/validation/rules/FirmaDocumentoEntradaRules.kt`; paquete `com.educaflow.subsystem.expedientes.services.validation.rules`.
- Especificación quirúrgica: **§10.1**. Cada regla implementa `com.educaflow.base.infrastructure.validation.engine.ValidationRule`.
- **Verificación:** compila; existen las **cuatro** reglas con los constructores y los mensajes literales de §10.1; las reglas 1 y 2 llevan su parámetro de mensaje con el **valor por defecto** literal de la especificación; la regla 4 **no** reimplementa la comprobación de la clave, se la pide a `FirmaEnServidorHelper`; ninguna escribe la clave en el log ni la devuelve en un mensaje.

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

## 8. Especificación del `InitialEventManagerImpl`

*(sin cambios)* — `com.educaflow.tramites.profesores.justificacion_falta_profesorado.actual.v1.InitialEventManagerImpl` **no se toca**.

Lo que ya hace y de lo que este delta **depende** (y que por tanto **MUST NOT** romperse):

| # | Setter | Fuente del valor | Por qué |
|---|---|---|---|
| 1 | `setAnyo(...)` | `LocalDate.now().getYear()` | Propone el año en curso |
| 2 | `setPersonaInteresada(...)` | `Persona` con nombre, apellidos y DNI de `getUsuarioRegistrador()` | El interesado es el propio profesor. **Necesario**: sin él `createRegistroEntrada` lanza NPE |
| 3 | `setPersonaSolicitante(...)` | la misma `Persona` | Ídem |
| 4 | `setDniFirmaDocumentoEntrada(...)` | el DNI de esa `Persona` | Es el DNI con el que se firma **y**, a partir de este delta, el que decide **cómo** se firma (CC-001). **Necesario**: sin él AutoFirma lanza y la situación de firma sería `SIN_DNI` |

**MUST NOT** añadirse ninguna asignación nueva: el delta no cambia nada de la creación del expediente. En particular **MUST NOT** inicializarse `claveFirmaDocumentoEntrada` (es transitorio, nace nulo, que es justo lo que exige RUI-009).

## 9. Especificación de los `PhaseEventManagerImpl`

### 9.1 Piezas compartidas del subsistema que usan los `trigger*`

Son el **mecanismo reutilizable**: esta iniciativa las escribe (o las extrae de donde ya estaban) una sola vez y quedan disponibles para **cualquier** tipo de expediente que quiera firmar en el servidor su documento de entrada. Ninguna conoce este trámite.

Dos de ellas son **de-duplicación**, no funcionalidad nueva: `FirmaEnServidorHelper` extrae lo que hoy vive dentro de `TareaFirmaServiceImpl`, y `TareaFirmaServiceImpl` pasa a delegar en él. Sin ese par, la firma en servidor del documento de entrada y la regla del DSL serían la segunda y la tercera copia del mismo criterio y de los mismos literales (§14).

#### `SituacionFirmaBuilder` — `com.educaflow.subsystem.firmas.util.SituacionFirmaBuilder`

Se **generaliza** para poder calcular la situación a partir de un DNI y no solo de un `User`.

- **Nuevo** `public static SituacionFirma buildByDni(String dni)`: contiene el cuerpo que hoy tiene `build(User)` a partir del DNI — `SIN_DNI` si `DniUtil.isValid(dni) == false`; `SIN_CERTIFICADO` si `CertificadoDigitalService.getTipoAlmacenClaveByDni(dni)` devuelve `null`; y si no, la traducción `TipoAlmacenClave → SituacionFirma` con el `switch` exhaustivo **sin `default`** que ya existe.
- `public static SituacionFirma build(User firmante)` pasa a ser `return buildByDni(firmante == null ? null : firmante.getDni());`. **MUST NOT** duplicarse la lógica.
- **MUST** conservarse el contrato actual: **nunca** devuelve `null` y **nunca** propaga excepción; ante cualquier error degrada a `SIN_CERTIFICADO`, que es el valor seguro. Es imprescindible aquí porque el getter que Axelor genera para el campo derivado solo captura `NullPointerException`.
- **MUST** conservarse el enmascarado del DNI en el log (`enmascararDni`). Ese método pasa de `private` a **package-private** (`static String enmascararDni(String dni)`) para que `FirmaEnServidorHelper`, que vive en el **mismo paquete**, lo reutilice en vez de escribir un segundo enmascarado. **MUST NOT** hacerse `public`: fuera del paquete nadie lo necesita.

#### `FirmaEnServidorHelper` — `com.educaflow.subsystem.firmas.util.FirmaEnServidorHelper` (**nuevo**)

Es la pieza que **de-duplica** el criterio de «la clave no es correcta» y los mensajes que lo explican. Hoy ese criterio vive **solo** dentro de `TareaFirmaServiceImpl`; sin esta clase, esta iniciativa lo copiaría dos veces más (en la firma en servidor del documento de entrada y en la regla del DSL) y quedarían **tres** copias del mismo `switch` y de los mismos literales. Las guías de diseño lo prohíben expresamente («MUST reutilizarse ese mecanismo en vez de inventar uno nuevo»).

- Clase `final`, con constructor privado y **solo métodos `static`**: no tiene estado y la construyen tanto un `ModelService` como una regla del DSL (que no pasa por Guice), así que **MUST NOT** depender de inyección.
- Vive en `..firmas.util..`, junto a `SituacionFirmaBuilder`, porque razona sobre `SituacionFirma` y sobre el certificado digital, no sobre expedientes. `..expedientes..` puede depender de `..firmas..` (ya lo hace `PhaseEventManagerImpl` con `TareaFirmaService`), y así **ningún** subsistema pasa a depender de expedientes.
- **MUST NOT** escribir la clave —ni entera ni truncada— en ningún log ni en ningún mensaje; el DNI solo aparece enmascarado (`SituacionFirmaBuilder.enmascararDni`).

Constantes de clase:

- `private static final int LIMITE_CAUSAS_A_RECORRER = 20;` — profundidad máxima al recorrer la cadena de causas de un fallo de firma, para no colgarse con un ciclo. Es **la** declaración de esa constante: **MUST** desaparecer de `TareaFirmaServiceImpl` y **MUST NOT** reaparecer en ninguna otra clase.

Métodos públicos (los cuatro conservan **literalmente** el comportamiento y los textos que hoy tiene `TareaFirmaServiceImpl`):

1. `public static boolean isClaveIncorrecta(Throwable ex)`
   - Recorre la cadena de causas, acotada a `LIMITE_CAUSAS_A_RECORRER` niveles, y devuelve `true` si aparece `java.security.UnrecoverableKeyException` o `javax.security.auth.login.LoginException`. **MUST NOT** decidirse por el texto del mensaje: tanto `CriptografiaUtil.getKeyStore` como `DocumentoPdf.firmar` envuelven el fallo original.

2. `public static String motivoFirmaFallida(RuntimeException ex, SituacionFirma situacionFirma)`
   - Motivo de negocio que se le enseña a quien firma. Si `isClaveIncorrecta(ex) == false` o `situacionFirma == null` → `«ha fallado la firma en el servidor. Póngase en contacto con el administrador»`. Si no, `switch` **exhaustivo y sin `default`**: `DISPOSITIVO_SIN_PIN` → `«el PIN indicado no es correcto»`; `FICHERO_SIN_CLAVE` → `«la contraseña indicada no es correcta»`; `DISPOSITIVO_CON_PIN`, `FICHERO_CON_CLAVE` → `«la clave guardada de su certificado digital no es correcta. Póngase en contacto con el administrador»`; `SIN_DNI`, `SIN_CERTIFICADO` → el motivo genérico.
   - Todos los textos pasan por `I18n.get`.

3. `public static boolean isClaveCertificadoIncorrecta(String dni, String clave, SituacionFirma situacionFirma)`
   - Devuelve `false` salvo con `FICHERO_SIN_CLAVE` y `FICHERO_CON_CLAVE`: en un dispositivo criptográfico la única forma de saber si el PIN es correcto es intentar abrirlo, y los intentos fallidos **bloquean la tarjeta**.
   - Resuelve el `CertificadoDigitalService` con `Beans.get(ModelServiceFactory.class).resolve(CertificadoDigital.class)` —es `static` y también la usa una regla del DSL, que Guice no construye—, pide `getAlmacenClaveByDni(dni, clave)` y, **si es un `AlmacenClaveFichero`**, devuelve `isPasswordValid() == false`.
   - Un certificado que **no se puede leer** (fichero corrupto, ruta que ya no existe, blob ilegible) **NO** es una clave incorrecta: cualquier `RuntimeException` se captura, se registra un `warn` con el DNI enmascarado y se devuelve `false`, para que sea la firma quien lo trate con su motivo genérico y no se acuse a nadie de haber tecleado mal la contraseña.

4. `public static String mensajeClaveCertificadoIncorrecta(SituacionFirma situacionFirma)`
   - `FICHERO_CON_CLAVE` → `«La clave guardada de su certificado digital no es correcta. Póngase en contacto con el administrador»`; cualquier otra → `«La contraseña indicada no es correcta»`. La distinción no es cosmética: el primero no lo puede corregir quien firma.

#### `TareaFirmaServiceImpl` — `com.educaflow.subsystem.firmas.service.impl.TareaFirmaServiceImpl`

Refactor **sin cambio de comportamiento**: es lo que convierte a `FirmaEnServidorHelper` en la única fuente de verdad en vez de en una cuarta copia.

- Sus cuatro métodos privados pasan a ser **una sola línea** que delega:
  - `isClaveIncorrecta(ex)` → `FirmaEnServidorHelper.isClaveIncorrecta(ex)`
  - `motivoFirmaFallida(ex, situacionFirma)` → `FirmaEnServidorHelper.motivoFirmaFallida(ex, situacionFirma)`
  - `isClaveCertificadoIncorrecta(tareaFirma)` → `FirmaEnServidorHelper.isClaveCertificadoIncorrecta(tareaFirma.getFirmante().getDni(), tareaFirma.getClaveFirma(), tareaFirma.getSituacionFirma())` — `getFirmante()` no puede ser nulo en ese punto: `validateFirmarEnServidor` solo llega a esta comprobación si V-TareaFirma-002 ya ha confirmado que el firmante es el usuario autenticado
  - `mensajeClaveCertificadoIncorrecta(situacionFirma)` → `FirmaEnServidorHelper.mensajeClaveCertificadoIncorrecta(situacionFirma)`
- **MUST** borrarse de la clase la constante `LIMITE_CAUSAS_A_RECORRER`, y **MUST NOT** quedar en ella ningún literal de los cuatro mensajes ni ningún `import` que solo sirviera para esa lógica (`UnrecoverableKeyException`, `LoginException`, `AlmacenClaveFichero`).
- **MUST NOT** tocarse nada más: `validateFirmarEnServidor` (V-TareaFirma-001..008), `firmarDocumentosEnMemoria`, los `AllowProperties` ni el `finally` que descarta la clave siguen exactamente igual, con los mismos mensajes.
- **Única diferencia observable**, y es de traza, no de negocio: el `warn` de «no se ha podido comprobar la clave del certificado» pasa a escribirlo el helper con el **DNI enmascarado** en lugar del `id` de la tarea. Se acepta a sabiendas: ninguna comprobación, ningún mensaje de usuario y ningún test dependen de esa línea.

#### `AlmacenClaveResolver` — `com.educaflow.subsystem.criptografia.service.AlmacenClaveResolver`

- **Nuevo** `public AlmacenClave getByDNI(String dni, String claveAcceso)`: resuelve el `CertificadoDigitalService` con el `ModelServiceFactory` ya inyectado y devuelve `certificadoDigitalService.getAlmacenClaveByDni(dni, claveAcceso)`.
- El `getByDNI(String dni)` existente **no cambia** (equivale a pasar `claveAcceso = null`).
- **MUST NOT** loguearse `claveAcceso` ni ninguna parte de ella.
- Recordatorio del contrato de `getAlmacenClaveByDni`: la clave **custodiada gana siempre** a la tecleada (CC-004), y si no hay certificado habilitado para el DNI lanza `RuntimeException`; por eso quien llame **MUST** haber comprobado antes la situación.
- ⚠️ **Recordatorio — la clave tecleada NO llega a un dispositivo criptográfico, y este delta NO lo arregla.** `CertificadoDigitalServiceImpl.getAlmacenClaveByDni(dni, claveAcceso)` usa `claveAcceso` **solo** en la rama `FICHERO_BD` / `CLASSPATH` / `SISTEMA_ARCHIVOS`; en `DISPOSITIVO_PKCS11` devuelve `new AlmacenClaveDispositivo(slot, alias)` y **descarta** `claveAcceso` (el PIN lo aporta `EntornoCriptografico`, desde la configuración del servidor, no desde la pantalla). El nuevo `getByDNI(dni, claveAcceso)` es un delegador fino, así que **hereda** esa limitación tal cual. Consecuencia y alcance en §14 «Situaciones sin escenario»; **MUST NOT** documentarse aquí, ni en el código, ni en las vistas, que el PIN tecleado se usa para firmar.

#### `FirmaDocumentoEntradaService` — `com.educaflow.subsystem.expedientes.services.internal.FirmaDocumentoEntradaService` (**nuevo**)

Es donde vive de verdad la firma en servidor del documento de entrada, reutilizable por **cualquier** tipo de expediente. Va en `..services.internal..` y no en `..controllers..` porque quien la invoca es el `trigger*` de un tipo de expediente —código de dominio—, y un `PhaseEventManagerImpl` no debe depender del paquete de controladores: hoy **ningún** `.java`/`.kt` de `tramites/` referencia `controllers`, y meter la primera dependencia invertiría las capas que describe `k-sistemas`. La guía «la firma en el servidor debe apoyarse en `FirmaController`» se cumple **en su intención** —que el mecanismo quede disponible para todos los tipos de expediente y no solo para este trámite— porque es aquí donde vive, en el subsistema de expedientes y sin conocer el trámite; el porqué de no ponerlo en el controlador está en el apartado siguiente y en §14.

- Clase normal de servicio interno. **No necesita binding**: Guice la construye con un **JIT binding** (*just-in-time*), porque es una clase **concreta**, no abstracta y con **constructor público sin argumentos** (el implícito); al construirla, Guice ejecuta `injectMembers` y rellena el `@Inject` de campo. Por eso el delta **no** crea ni toca ningún módulo Guice —el subsistema de expedientes hoy **no tiene** ningún `module/*Module.java`— y **MUST NOT** añadirse por ella ninguna fila a §6 ni ningún paso a §7 (`k-guice`).
  ⚠️ **No** se cita `AlmacenClaveResolver` como precedente de esto: esa clase **sí** tiene binding explícito (`bind(AlmacenClaveResolver.class);` en `com.educaflow.subsystem.criptografia.module.CriptografiaModule`).
  Si al implementar apareciera un `Guice/MissingConstructor` (p. ej. porque se le acabara añadiendo un constructor con argumentos), la solución **MUST** ser declarar el binding explícito en un módulo Guice del subsistema (creándolo como `com.educaflow.subsystem.expedientes.module.ExpedientesModule` y registrándolo donde el proyecto registre los demás) y añadir su fila a §6 y su paso a §7, nunca quitarle el `@Inject`. Es un cambio **fuera** de la carpeta de diseño: queda anotado aquí para `/sdd-implementer`.
- **MUST NOT** llevar ningún `@CallMethod`: ninguna vista la invoca.

Dependencias a inyectar:

- `@Inject AlmacenClaveResolver almacenClaveResolver` — para obtener el almacén de claves del DNI con la clave que corresponda.

Constantes de clase:

- `public static final String CAMPO_CLAVE_FIRMA_DOCUMENTO_ENTRADA = "claveFirmaDocumentoEntrada";` — el nombre del campo transitorio de la clave, declarado **una sola vez** para todo el subsistema (§4.1). Lo usa `ExpedienteController` para devolverlo vacío (RUI-011) y lo cita el `domains.xml` de cada tipo que quiera firma en servidor.

Métodos:

1. `public boolean isFirmaEnServidor(SituacionFirma situacionFirma)`
   - Devuelve `true` si `situacionFirma` es una de `DISPOSITIVO_CON_PIN`, `DISPOSITIVO_SIN_PIN`, `FICHERO_CON_CLAVE`, `FICHERO_SIN_CLAVE`; `false` para `SIN_DNI` y `SIN_CERTIFICADO` y para `null`.
   - Es **la** definición de «corresponde firmar en el servidor» que usan el trigger y las reglas del validador; **MUST NOT** duplicarse esa lista en ningún otro sitio.

2. `public DocumentoPdf firmarDocumentoEntradaEnServidor(String dni, SituacionFirma situacionFirma, String claveFirma, DocumentoPdf documentoOriginal, CampoFirma campoFirma) throws BusinessException`
   - Acciones, **en este orden**:
     1. `ERROR_NEGOCIO` **no**: guarda de código. Si `isFirmaEnServidor(situacionFirma) == false`, o si `documentoOriginal` es `null`, lanza `IllegalStateException` — es un error de programación del llamante, no algo que el usuario pueda corregir.
     2. `SERVICIO(AlmacenClaveResolver.getByDNI(dni, claveFirma))` — obtiene el almacén de claves. **MUST** pedirse **dentro de la propia acción**, con el DNI y la clave de esta invocación: cada llamada firma un documento con la clave que le corresponde, así que el almacén no se cachea, ni se guarda en un campo de la clase, ni se recibe como parámetro desde fuera.
        El motivo **NO** es el ámbito Guice del servicio —se resuelve por **JIT binding** y, al no llevar `@Singleton`, Guice crea una instancia nueva por punto de inyección—, sino el **dato**: el `AlmacenClave` depende del DNI y de la clave **de esa invocación**, y la clave es un dato de un solo uso (§4.1) que **MUST NOT** sobrevivir a la llamada. La regla vale **con independencia** del ámbito Guice que la clase acabe teniendo.
        Nota: `AlmacenClaveFichero` **sí** es reutilizable —lee el certificado a un `byte[]` en el constructor y su `getFileCertificate()` devuelve un `InputStream` nuevo en cada llamada—, así que **MUST NOT** justificarse esta decisión con un supuesto stream que se consuma al firmar: el motivo es la clave, no el stream.
     3. `SERVICIO(DocumentoPdf.firmar(almacenClave, campoFirma))` — devuelve el PDF firmado.
   - Tratamiento del fallo (RN-002): captura `RuntimeException`, escribe la traza en el log con el DNI **ofuscado** y **NUNCA** con la clave ni con un fragmento de ella, y lanza `BusinessException` con el mensaje `«No se ha podido firmar la solicitud: <motivo>»`, donde `<motivo>` es **exactamente** `FirmaEnServidorHelper.motivoFirmaFallida(ex, situacionFirma)`.
     - **MUST NOT** reimplementarse aquí ni el recorrido de la cadena de causas ni los cuatro textos de motivo: son los del helper (apartado «`FirmaEnServidorHelper`»), que es la misma pieza en la que delega `TareaFirmaServiceImpl`.
   - El texto técnico de la excepción **MUST NOT** llegar al usuario: se queda en el log.
   - **MUST** lanzarse `com.educaflow.base.infrastructure.validation.messages.BusinessException` (construida con `BusinessMessages.single(...)`) y no otra: es la única que el `Tramitador` hace `detach` y propaga como mensaje de usuario, y de eso depende que la acción se cancele entera sin dejar rastro.
   - Es un método **de negocio**: no lo invoca ninguna vista, lo invoca el `trigger*` del tipo de expediente.

#### `FirmaController` — `com.educaflow.subsystem.expedientes.controllers.FirmaController` (**NO se toca**)

**MUST NOT** modificarse en esta iniciativa: no tiene fila en §6 ni paso en §7. Sigue exactamente como está, sirviendo el camino de AutoFirma con su `@CallMethod firmarDocumentoEntrada(...)`, que la `<action-method>` de `fases/recepcion/views.xml` sigue llamando igual.

Motivo, y cómo se concilia con la guía de diseño («la firma en el servidor debe apoyarse en `FirmaController` … de forma que el mecanismo quede disponible para todos los tipos de expediente»): se llegó a plantear añadirle dos **delegadores finos** (`isFirmaEnServidor` y `firmarDocumentoEntradaEnServidor`) para apoyarse en él **literalmente**, y se **descartó** porque quedarían **sin ningún invocador** —API pública muerta—:

- el `triggerPresentar` inyecta y llama al **servicio**, no al controlador (§9.2), y no puede hacer otra cosa sin invertir las capas (`k-sistemas`): hoy **ningún** `.java`/`.kt` de `tramites/` referencia el paquete `controllers`;
- tampoco podrían llevar `@CallMethod` (un endpoint de firma sería invocable desde cualquier estado y permitiría re-firmar saltándose la máquina de estados, `k-secure-coding`), así que ninguna vista los alcanzaría;
- y no queda ningún tercer llamador posible.

**La guía se cumple en su intención, que es que el mecanismo sea reutilizable por todos los tipos de expediente y no exclusivo de este trámite** — y lo es: vive en `FirmaDocumentoEntradaService`, en el subsistema de expedientes, no conoce este trámite y cualquier otro tipo lo usa inyectándolo en su `PhaseEventManagerImpl`. Lo único que cambia respecto a la letra de la guía es **la clase** en la que se apoya (un servicio del subsistema en vez del controlador del subsistema), y ese cambio es lo que permite cumplir a la vez la especificación (la solicitud firmada la produce la propia acción `PRESENTAR`, RN-001/CC-002) y las capas. Queda recogido en §14 «Notas y supuestos».

**Nota para `/sdd-implementer`:** si en el futuro se decidiera exponer también la firma en servidor desde `FirmaController`, MUST hacerse con un invocador real, no «por si acaso».

#### `ExpedienteController` — `com.educaflow.subsystem.expedientes.controllers.ExpedienteController`

Un único cambio, en la rama `catch (BusinessException ex)` de `triggerEvent`: además de `actionResponseHelper.doResponseBusinessMessages(...)`, devolver el valor **vacío** de la clave de firma con `response.setValue(FirmaDocumentoEntradaService.CAMPO_CLAVE_FIRMA_DOCUMENTO_ENTRADA, null)`.

- **MUST NOT** escribirse el nombre del campo como literal en este fichero: el controlador es genérico y el nombre del campo es contrato del subsistema, declarado en una sola constante (§4.1).

- Es lo que hace RUI-011 («tras un intento cancelado el campo de la clave vuelve a mostrarse vacío») y la parte visible de RN-006 («la clave se descarta también cuando la acción se cancela»).
- Es genérico: para un tipo de expediente que no declare ese campo, el valor simplemente no se aplica a ningún widget.
- **MUST NOT** tocarse nada más del método: ni el camino de éxito, ni el de `EXIT`/`DELETE`, ni el `catch (UnauthorizedException)`, que **MUST** seguir sin envolver.

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

### 9.3 Fase TRAMITACION

*(sin cambios)* — `.../v1/tramitacion/PhaseEventManagerImpl.java` **no se toca**. Sigue con su `triggerResolver` (que firma la resolución en servidor con el cargo `DIRECTOR`) y sus tres `onEnter*`.

## 10. Especificación de los `StateEventValidatorImpl`

### 10.1 Reglas nuevas del DSL de validación (subsistema de expedientes)

Fichero nuevo `src/main/java/com/educaflow/subsystem/expedientes/services/validation/rules/FirmaDocumentoEntradaRules.kt`, paquete `com.educaflow.subsystem.expedientes.services.validation.rules`.

Por qué aquí y no en `com.educaflow.base.infrastructure.validation.rules`: estas reglas necesitan el enum `SituacionFirma` (subsistema de firmas) y el `CertificadoDigitalService` (subsistema de criptografía), y `base.infrastructure` **MUST NOT** depender de ningún `subsystem` (regla de arquitectura C2, que declara expresamente que no admite exenciones).

Las cuatro reglas reciben como argumentos los **getters** de los campos que necesitan —igual que `FirmaPdf(documentoOriginalField, dniField)`—, así que no dependen de ninguna entidad concreta y sirven para **cualquier** tipo de expediente. Un getter pasado como argumento **no** entra en el `AllowProperties` del evento (solo entran los `field(...)` de primer nivel), de modo que nombrarlos aquí **no** los hace escribibles por el cliente.

Todas se cuelgan del `field(...)` del campo de la **clave**, así que reciben en `value` la clave tecleada. Todas devuelven `null` cuando no tienen nada que decir.

`FieldValidationRules` antepone **siempre** al mensaje la etiqueta del `field(...)` del que cuelga la regla, y el DSL no permite colgar una regla fuera de un `field(...)`: por eso el `title` de ese campo es neutro (`Firma de la solicitud`, §4.1) y no nombra la clave, que es lo que hace legibles los mensajes de las reglas 1 y 2, que no hablan de ella (§14).

1. `data class DocumentoIdentidadParaFirmarRequerido(val situacionFirmaField: KCallable<*>, val mensaje: String = "No es posible firmar la solicitud porque su usuario no tiene un documento de identidad. Póngase en contacto con el administrador.") : ValidationRule` — **VAL-PENDIENTE_PRESENTACION-PRESENTAR-001**
   - Si `situacionFirmaField.call(bean) == SituacionFirma.SIN_DNI` → `BusinessMessages.single(I18n.get(mensaje))`.
   - En cualquier otra situación, `null`.
   - El segundo parámetro es **opcional y su valor por defecto es el literal exacto que fija la especificación**, así que este trámite lo construye con un solo argumento y no cambia ni una letra de su mensaje; otro tipo de expediente que reutilice la regla puede pasar el suyo sin arrastrar el vocabulario («la solicitud») de este trámite (§14).

2. `data class FirmanteDocumentoEntradaEsUsuarioAutenticado(val dniFirmaField: KCallable<*>, val mensaje: String = "Solo puede presentar la solicitud la persona que la firma") : ValidationRule` — **VAL-PENDIENTE_PRESENTACION-PRESENTAR-008**
   - Obtiene el DNI del expediente con `dniFirmaField.call(bean)` y el del usuario autenticado con `AuthUtils.getUser()`.
   - Si el DNI del expediente es nulo o en blanco → `null` (de eso se ocupa la regla 1).
   - Si no hay usuario autenticado, o su DNI no coincide (comparación exacta) con el del expediente → `BusinessMessages.single(I18n.get(mensaje))`.
   - Mismo criterio que la regla 1: parámetro opcional con el literal de la especificación como valor por defecto.
   - Es **nueva y necesaria**: hasta ahora firmar exigía tener el certificado en el propio equipo, así que nadie podía firmar por otro aunque llegase a lanzar la acción; con la firma en el servidor esa barrera desaparece porque la firma se produce sola a partir del DNI guardado en el expediente.
   - **MUST NOT** escribirse el DNI completo en ningún mensaje ni en ningún log.

3. `data class ClaveFirmaDocumentoEntradaRequerida(val situacionFirmaField: KCallable<*>) : ValidationRule` — **VAL-…-002** y **VAL-…-003**
   - Si la clave (`value`) no es nula ni está en blanco → `null`.
   - `SituacionFirma.FICHERO_SIN_CLAVE` → `BusinessMessages.single(I18n.get("La contraseña es obligatoria"))`.
   - `SituacionFirma.DISPOSITIVO_SIN_PIN` → `BusinessMessages.single(I18n.get("El PIN es obligatorio"))`.
   - Cualquier otra situación → `null`: cuando la secretaría virtual custodia la clave, o cuando se firma en el equipo del profesor, no hay nada que teclear.

4. `data class ClaveFirmaDocumentoEntradaCorrecta(val situacionFirmaField: KCallable<*>, val dniFirmaField: KCallable<*>) : ValidationRule` — **VAL-…-004** y **VAL-…-005**
   - **La regla no decide nada por su cuenta: delega entera en `FirmaEnServidorHelper`** (§9.1), que es la misma pieza en la que delega `TareaFirmaServiceImpl`. Su cuerpo es, literalmente:
     - si `FirmaEnServidorHelper.isClaveCertificadoIncorrecta(dniFirmaField.call(bean), value, situacionFirmaField.call(bean))` → `BusinessMessages.single(FirmaEnServidorHelper.mensajeClaveCertificadoIncorrecta(situacionFirmaField.call(bean)))`; si no, `null`.
   - **MUST NOT** reimplementarse aquí el `instanceof AlmacenClaveFichero` + `isPasswordValid()`, ni los dos mensajes, ni el tratamiento del certificado ilegible: todo eso **es** el helper, y duplicarlo devolvería el diseño a las tres copias que esta iniciativa elimina.
   - Lo que el helper garantiza y esta regla hereda: solo actúa con `FICHERO_SIN_CLAVE` y `FICHERO_CON_CLAVE` —con un certificado en un dispositivo criptográfico la única forma de saber si el PIN es correcto es intentar abrirlo, y los intentos fallidos **bloquean la tarjeta**, así que ahí el error lo da la propia firma (RN-002)—; un certificado que no se puede leer **NO** cuenta como clave incorrecta (se registra un `warn` y se deja pasar); cualquier `RuntimeException` se degrada a «no incorrecta»; y la clave **MUST NOT** aparecer en ningún log.
   - Único cuidado propio de la regla: con `FICHERO_SIN_CLAVE` y la clave **vacía** devuelve `null` sin llamar al helper, porque de eso se ocupa la regla 3 y así un solo intento no produce dos mensajes.
   - Mensajes resultantes: `FICHERO_SIN_CLAVE` → `«La contraseña indicada no es correcta»`; `FICHERO_CON_CLAVE` → `«La clave guardada de su certificado digital no es correcta. Póngase en contacto con el administrador»`. La distinción no es cosmética: el primero lo corrige el profesor, el segundo no.

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

### 10.3 Fase TRAMITACION

*(sin cambios)* — `.../v1/tramitacion/StateEventValidatorImpl.kt` **no se toca**.

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

## 12. Asignación de perfiles

*(sin cambios)* — el delta **no** añade perfiles ni asignaciones, así que **no** hay `design/permisos.xml` y `src/main/resources/data-demo/input/permisos-demo.xml` **no se toca**.

Las asignaciones vigentes, que este diseño da por buenas y de las que dependen los tests E2E:

| perfil | actor | tipo de actor | vía | bloque de `permisos-demo.xml` |
|---|---|---|---|---|
| `CREADOR` | `PROFESOR` | `TipoUsuario` | `tramiteCode="JustificacionFaltaProfesorado"` | `<asignacionesTipoUsuario>` |
| `RESPONSABLE` | `JEFE_ESTUDIOS` | `Cargo` | `tipoExpedienteCode="JustificacionFaltaProfesoradoV1"` | `<asignacionesCargoTipoExpediente>` |

El perfil del estado inicial (`CREADOR`) va por `tramiteCode`, como exige la creación del expediente. La firma en el servidor **no** se restringe por tipo de usuario ni por cargo: la usa cualquier profesor para cuyo documento de identidad exista un certificado digital habilitado, y el control real está en a quién le da de alta un certificado el Administrador (pantalla de certificados digitales del subsistema de criptografía, que este delta **no** toca).

## 13. Tests

- **Tests E2E:** descritos en [`test-e2e-desc.md`](./test-e2e-desc.md) — trece tests Given/When/Then en lenguaje de negocio: uno por cada `ESC-NNN` de la especificación más el del camino de firma en el equipo del profesor, que va marcado como manual, medidos **solo sobre el delta** más el camino existente que el delta atraviesa (no-regresión).
- **Tests unitarios:** declarados en `test-unit-desc.md`.
- **No hay subsección «Tests E2E supersedidos»**: la carpeta espejo de esta versión (`src/test/e2e/tramites/profesores/justificacion_falta_profesorado/actual/v1/`) **no existe** —no hay ningún `t-*.desc.md` persistido de esta versión—, así que el delta no invalida ningún test ya persistido. Por lo mismo, los `T-NNN` de `test-e2e-desc.md` empiezan en `T-001`.
- **CRITICAL — en esta iniciativa NO se escriben tests.** Las guías de diseño lo excluyen expresamente («No se escriben tests en esta iniciativa (ni unitarios, ni de arquitectura, ni E2E)»). `test-e2e-desc.md` se produce porque es contrato del pipeline y porque es **el criterio de aceptación** del paso 12 (verificación en runtime), pero **MUST NOT** ejecutarse `/sdd-debug-with-test-e2e-desc` ni `/sdd-create-tests-e2e` sobre esta iniciativa, ni crearse ningún `.spec.ts`. Los tests de `src/test/java/com/educaflow/tiposexpedientes` y `com/educaflow/views` que ya existen sí se ejecutan, porque los lanza `./run.sh`, y **MUST** seguir en verde.

## 14. Notas y supuestos

- **Dónde vive la firma en el servidor (guías de diseño vs. especificación vs. capas).** Las guías piden apoyarse en `FirmaController`; la especificación exige que la solicitud firmada la produzca **la propia acción** `PRESENTAR` y que se **descarte** lo que el formulario hubiera enviado (RN-001, CC-002, y las guardas nuevas de VAL-…-006/007); y `k-sistemas` pide que el dominio no dependa del paquete de controladores. Las tres se han conciliado así (con **una desviación consciente de la letra** de la guía, detallada al final de esta nota):
  la lógica reutilizable vive en `FirmaDocumentoEntradaService` (`..expedientes.services.internal..`, §9.1), y quien la invoca es el `triggerPresentar` del tipo (§9.2), que es el único punto donde se puede pisar lo que envió el cliente, **inyectando el servicio**.
  Motivo de esa última pieza: hoy **ningún** `.java`/`.kt` de `tramites/` referencia el paquete `controllers`, y estrenar esa dependencia invertiría las capas aunque los tests de arquitectura no la prohíban (`..expedientes..` y `..tramites..` son paquetes **exentos**, `agent_docs/architecture-rules.md`, §Convenciones).
  **Desviación consciente de la letra de la guía, y por qué:** la guía nombra `FirmaController`, y **`FirmaController` NO se toca** en esta iniciativa (§6, §9.1). Se descartó añadirle dos delegadores finos (`isFirmaEnServidor`, `firmarDocumentoEntradaEnServidor`) porque **no tendrían ningún invocador**: el trigger usa el servicio, no pueden llevar `@CallMethod` —un endpoint de firma sería invocable desde cualquier estado y permitiría re-firmar saltándose la máquina de estados— y no hay ningún tercer llamador. Serían API pública muerta, que es exactamente lo que `k-code-quality` prohíbe.
  Lo que la guía **persigue** —«que el mecanismo quede disponible para todos los tipos de expediente y no solo para este trámite»— **sí se cumple íntegramente**: el mecanismo vive en el subsistema de expedientes, no conoce este trámite y cualquier otro tipo lo usa inyectando `FirmaDocumentoEntradaService` en su `PhaseEventManagerImpl`. Lo único que cambia es la **clase de apoyo** (un servicio del subsistema en vez del controlador del subsistema).
  El método del servicio tampoco lleva `@CallMethod`, por el mismo motivo de máquina de estados.
- **Una sola fuente de verdad para «la clave no es correcta» (`FirmaEnServidorHelper`).** El criterio (recorrer la cadena de causas buscando `UnrecoverableKeyException`/`LoginException`, con un límite de niveles), la comprobación previa con `isPasswordValid()` solo en certificados en fichero y los textos que explican el fallo ya existían **dentro** de `TareaFirmaServiceImpl`. Copiarlos aquí habría dejado **tres** copias (la tarea de firma, la firma del documento de entrada y la regla del DSL) del mismo `switch` y de los mismos literales, justo lo que las guías prohíben («MUST reutilizarse ese mecanismo en vez de inventar uno nuevo»).
  Por eso esta iniciativa **extrae** esa lógica a `com.educaflow.subsystem.firmas.util.FirmaEnServidorHelper` y **refactoriza `TareaFirmaServiceImpl` para que delegue en ella** (§9.1): es un cambio en código ya en producción, pero sin cambio de comportamiento ni de mensajes, y es la única forma de que la de-duplicación sea real y no una copia más. La única diferencia observable es una línea de `warn` que pasa a llevar el DNI enmascarado en vez del `id` de la tarea.
- **Por qué los dos campos transitorios van en el `domains.xml` del tipo y no en `Expediente`.** Explicado en §4.1: `FieldValidationRules.getLabel` usa `getDeclaredField`, que no recorre superclases, así que un campo heredado no puede aparecer en un `field(...)` del validador. Es la razón por la que `dniFirmaDocumentoEntrada` (que sí está en `Expediente`) solo se usa como **argumento** de `FirmaPdf` y nunca como `field(...)`. Alternativa descartada: parchear `getLabel` para que recorra la jerarquía — es un cambio en `base.infrastructure` que afecta a todas las validaciones del proyecto y queda fuera del alcance de esta iniciativa.
- **Dos `<button name="PRESENTAR">` en el mismo footer.** Es la única forma de cumplir RUI-…-006/007: el `name` de un botón **es** el evento que dispara (`_signal`), así que los dos botones excluyentes tienen que llamarse igual y distinguirse por `showIf` y por `title`. El preprocesador de vistas copia los botones del `<footer>` verbatim, con todos sus atributos, y `axelor-front` da a cada widget de un formulario una clave propia (`uid` único, no el `name`), así que la duplicidad no colisiona. Aun así, el paso 12 lo comprueba en runtime porque no hay ningún test que lo cubra.
- **`required="true"` en el campo de la clave — riesgo cerrado, se conserva.** Lo pide RUI-…-003/004 («marcado como obligatorio») y es solo la marca visual: la obligatoriedad real la impone `ClaveFirmaDocumentoEntradaRequerida` en el servidor. Se llegó a plantear que el cliente bloqueara el envío del evento con el campo vacío y rompiera ESC-004; **no ocurre**, y consta por tres evidencias:
  1. en `axelor-front`, `views/form/widgets/button/button.tsx` → `handleClick` confirma el `prompt`, hace `commitEditableWidgets()` y llama directamente a `actionExecutor.execute(onClick, …)`: **no valida** los campos `required` del formulario en ningún punto;
  2. `subsysExpedientes-event-action` es una `<action-method>` pura —no hay ningún `save` en la cadena— y es el guardado, no el `action-method`, lo que dispara la validación de obligatoriedad del cliente;
  3. el precedente en producción hace exactamente esto: `subsystem/firmas/views/Pendiente-TareaFirma.xml` pone `required="true"` sobre `claveFirma` y su botón encadena `action-method`s. Es más: **precisamente porque el `required` no frena nada**, allí se añadió una `<action-validate>` explícita en el cliente además de la comprobación del servidor (V-TareaFirma-005/006). Aquí **MUST NOT** añadirse esa `action-validate`: ESC-004 exige que el intento llegue al servidor y devuelva su mensaje.
  Por tanto **MUST** conservarse el `required="true"`; el paso 12 se limita a comprobar ESC-004 como cualquier otro escenario, no como un riesgo abierto.
- **Vocabulario del trámite en unas reglas compartidas: mensaje parametrizado con el literal de la spec por defecto.** Las cuatro reglas viven en el subsistema y sirven para cualquier tipo de expediente, pero los textos que la especificación fija para VAL-…-001 y VAL-…-008 hablan de «la solicitud», que es vocabulario de **este** trámite. Se resuelve con un parámetro opcional de mensaje en esas dos reglas cuyo **valor por defecto es el literal exacto de la especificación** (§10.1): este trámite las construye con un solo argumento y no cambia ni una letra, y otro tipo de expediente que las reutilice pasa el suyo. Las reglas 3 y 4 no lo necesitan: sus textos («La contraseña es obligatoria», «El PIN es obligatorio», «La contraseña indicada no es correcta», «La clave guardada de su certificado digital no es correcta…») no nombran ningún documento, y además son **los mismos** que ya usa la tarea de firma, así que salen del helper compartido.
  Excepción consciente: el prefijo de RN-002 («No se ha podido firmar **la solicitud**: …») se queda tal cual en `FirmaDocumentoEntradaService`, porque la especificación lo fija así y parametrizarlo obligaría a cambiar la firma del método; la parte reutilizable de ese mensaje —el motivo— sí es compartida (`FirmaEnServidorHelper.motivoFirmaFallida`).
- **La etiqueta que el footer antepone a cada mensaje: por eso el `title` del campo de la clave es neutro.** `FieldValidationRules` resuelve la etiqueta con el `title` del `field(...)` del que cuelga la regla y **la antepone siempre**; el visor pinta `«<etiqueta>: <mensaje>»`. Como el DSL no deja colgar una regla fuera de un `field(...)`, las cuatro reglas cuelgan del campo de la clave, y con el `title` anterior («Clave del certificado digital») los mensajes de VAL-…-001 y VAL-…-008 —que no hablan de la clave— habrían salido precedidos de una etiqueta que no viene a cuento. Con el `title` neutro `Firma de la solicitud` (§4.1), lo que ve el usuario es:
  - VAL-…-001 → «Firma de la solicitud: No es posible firmar la solicitud porque su usuario no tiene un documento de identidad. Póngase en contacto con el administrador.»
  - VAL-…-002 → «Firma de la solicitud: La contraseña es obligatoria» (ESC-004)
  - VAL-…-003 → «Firma de la solicitud: El PIN es obligatorio»
  - VAL-…-004 → «Firma de la solicitud: La contraseña indicada no es correcta» (ESC-003)
  - VAL-…-005 → «Firma de la solicitud: La clave guardada de su certificado digital no es correcta. Póngase en contacto con el administrador» (ESC-010)
  - VAL-…-008 → «Firma de la solicitud: Solo puede presentar la solicitud la persona que la firma»
  El cambio **no toca ninguna pantalla**: la vista sobrescribe el `title` por «Contraseña» y por «PIN» en el panel de cada situación, que es lo que los escenarios comprueban.
- **El bloque de firma es un panel por situación, no un panel con seis condiciones.** Se sigue literalmente el precedente en producción `src/main/java/com/educaflow/subsystem/firmas/views/Pendiente-TareaFirma.xml`, donde cada situación de firma tiene **su propio `<panel>`** con su `showIf`, su `<help>` dentro y, si hace falta, su `<field>` de la clave. Así no hay dos `<field name="claveFirmaDocumentoEntrada">` en el mismo panel —que es lo que obliga a hacer un panel único— y añadir o quitar una situación es añadir o quitar un panel. El campo derivado que alimenta los `showIf` va oculto en un panel propio **sin `showIf`** (`firmaSolicitudSituacion`), igual que el `situacionFirma` oculto del precedente, y además **`readonly="true"`** —en el panel y en el propio `<field>`— porque es un campo `servidor` (lo calcula su getter derivado) y ningún campo `servidor` puede quedar editable en la vista (`vistas.md` §8). Ese `readonly` **no es una defensa**, igual que no lo es el `showIf`: lo que impide firmar mal son las reglas del `StateEventValidatorImpl`, y el campo ni siquiera aparece en ningún `field(...)` (§4.1, §10.2). Los siete paneles (los seis de situación más el del campo oculto) son locales del `<form state=… profile="CREADOR">` y **MUST NOT** subir al almacén de paneles de la raíz: solo los usa esa pantalla (`vistas.md` §3.5).
- **`MetaFile` huérfanos al firmar en el servidor.** La acción 3 de `triggerPresentar` crea un `MetaFile` **nuevo** con la solicitud firmada en **cada** intento. Si el evento se cancela después (una validación, un fallo del registro de entrada), el `detach` del `Tramitador` revierte la entidad pero **no** el almacén de adjuntos, así que ese fichero queda subido y sin referencia. Es exactamente lo que ya ocurre hoy con el camino de AutoFirma —que también sube el firmado antes del evento— y la limpieza de adjuntos huérfanos queda **fuera del alcance** de esta iniciativa.
- **Enum compartido entre módulos de dominio.** `situacionFirmaDocumentoEntrada` referencia `com.educaflow.subsystem.firmas.db.SituacionFirma` desde un `domains.xml` del módulo `expedientes`. El generador de Axelor usa el `ref` de un `<enum>` **tal cual** como tipo Java, así que un FQN de otro módulo funciona; es la primera vez que el proyecto lo hace, y por eso el paso 12 lo comprueba (basta con que compile y con que el valor llegue al formulario). Alternativa descartada: duplicar el enum en el subsistema de expedientes — las guías piden reutilizar el de firmas.
- **Coste del campo derivado.** `BeanMapperModel.getEntityCloned` clona el expediente con «allow all» al principio de cada evento, así que llamará al getter derivado y hará una consulta de `CertificadoDigital` por evento de este tipo de expediente (ninguna si el DNI es nulo o inválido). Es el mismo coste que ya paga `TareaFirma.situacionFirma` y se ha considerado asumible.
- **Situaciones sin escenario — y `DISPOSITIVO_*` además NO operativa con el mecanismo que se reutiliza.** `DISPOSITIVO_CON_PIN` y `DISPOSITIVO_SIN_PIN` quedan **especificadas y cableadas** por este diseño (su `<panel>` con el aviso RUI-…-CREADOR-004, el `<field>` del PIN, la validación VAL-…-003 y su inclusión en `FirmaDocumentoEntradaService.isFirmaEnServidor`), pero **MUST NOT** decirse que quedan «programadas»: no lo están del todo, y conviene decirlo sin rodeos.
  - **Lo que sí se hace:** se detecta la situación (CC-001), se muestra el aviso y el campo «PIN», y VAL-…-003 exige que no venga vacío.
  - **Lo que NO se hace:** ese PIN **nunca llega a la firma**. `CertificadoDigitalServiceImpl.getAlmacenClaveByDni(dni, claveAcceso)` solo usa `claveAcceso` en la rama `FICHERO_BD` / `CLASSPATH` / `SISTEMA_ARCHIVOS`; en `DISPOSITIVO_PKCS11` devuelve `new AlmacenClaveDispositivo(slot, alias)` —cuyo constructor no admite PIN— y **descarta** `claveAcceso`. Quien aporta el PIN es `EntornoCriptografico.getDispositivoCriptografico(slot)`, que lo lee de la configuración del servidor (`entornoCriptografico.*`). Por tanto, en `DISPOSITIVO_SIN_PIN` —la única situación de dispositivo que depende de la clave tecleada, porque la secretaría virtual **no** la custodia— la acción `PRESENTAR` **no puede firmar aunque el usuario rellene el campo**: el PIN se pide, se valida y se descarta. `DISPOSITIVO_CON_PIN` no depende de lo tecleado, pero su PIN sale igualmente de la configuración del servidor y no del certificado custodiado.
  - **El hueco NO se cierra en esta iniciativa, y MUST NOT intentarse aquí.** Cerrarlo exigiría cambiar `AlmacenClaveDispositivo` para que acepte el PIN y el camino de `EntornoCriptografico` / `DocumentoPdfImplIText` para que llegue al `KeyStore` PKCS#11: infraestructura criptográfica compartida, ajena a este trámite y **fuera del alcance**. Las guías de diseño obligan a **reutilizar** el mecanismo existente (`getAlmacenClaveByDni(dni, claveAcceso)`) en vez de inventar uno nuevo, y eso es exactamente lo que se hace, con su limitación incluida.
  - **Es la misma desviación consciente** que la iniciativa de referencia `.sdd/drafts/2026-09-01_11-21_firma-en-servidor` documentó en su **nota 13** («en `DISPOSITIVO_SIN_PIN` el PIN tecleado se descarta»). Aquí se **hereda entera**, sin agravarla ni repararla.
  - **Sin test, además, por el entorno:** las dos situaciones de dispositivo exigen un dispositivo criptográfico físico conectado y configurado en el servidor, que no existe en el entorno de pruebas (la propia especificación lo declara fuera de alcance). Lo mismo con `SIN_DNI` (no hay cuenta de profesor de demo sin documento de identidad) y con la firma real con AutoFirma.
- **Sin `permisos.xml` y sin `CreateFilesTask`.** El delta no añade perfiles ni asignaciones, así que no hay fragmento de permisos; y no añade ninguna fase nueva, así que **MUST NOT** ejecutarse `CreateFilesTask`: no hay ningún esqueleto que generar y la tarea no pinta nada en este delta.
- **Git.** Las guías prohíben expresamente tocar nada de git durante toda la iniciativa (ni commits, ni ramas, ni stash).

## 15. Checklist del diseñador

**Estructura y materialización**

- [x] ¿Existen todos los ficheros de §1 y **ninguno más**? ¿Ni un `.java`, ni un `.kt`, ni un `i18n_*.csv`, ni un `estados.png`, ni un `.pdf` dentro de `design/`? — `design.md`, `domains.xml`, `fases/recepcion/views.xml` y `test-e2e-desc.md`; en modo modificación solo van los ficheros que el delta toca.
- [x] ¿Hay un `design/fases/<fase>/views.xml` por **cada** fase declarada, con la fase en minúsculas? — solo `recepcion`, que es la única fase cuyo `views.xml` toca el delta; `tramitacion` no se regenera a propósito.
- [x] ¿Hay un `design/documentospdf/<doc>.xml` por cada documento y ninguno de más? — el delta no toca ningún documento, así que no hay carpeta `documentospdf/`.
- [x] ¿Todos los XML materializados están **completos**, sin `TODO`, sin `...`, sin `<button name="">`?
- [x] ¿El `design.md` tiene el frontmatter `type: design` con la clave `template:` copiada de la spec, y las 15 secciones de §2, con esos títulos y en ese orden?
- [x] **Iniciativa de MODIFICACIÓN:** ¿«Identidad del trámite y del tipo», «Ficheros a crear o modificar» y «Pasos» van **completas**, sin `*(sin cambios)*`?
- [x] ¿La fila `| Modificación de | … |` está y nombra una carpeta de versión que **existe** en el árbol?

**Máquina de estados**

- [x] ¿Exactamente un estado `initial` en todo el tipo? — `RECEPCION / ENTRADA_DATOS`, sin cambios.
- [x] ¿Todo `<state>` lleva escrito su `events`? — sin cambios; el `TipoExpedienteInstance.xml` no se toca.
- [x] ¿`EXIT` **no** aparece en ningún `events`? — sin cambios.
- [x] ¿Todo `profile` de un estado es un valor del enum `Profile`? — sin cambios.
- [x] ¿Ningún nombre de estado ni de evento produce un método que pise la API base? — sin cambios.
- [x] ¿La tabla de transiciones tiene una fila por cada pareja (estado, evento) declarada? — `*(sin cambios)*`; §3 lista las transiciones que el delta atraviesa, tomadas del as-is.
- [x] ¿Todo evento ramificado declara sus guardas y su `default`? — el único ramificado es `RESOLVER`, que el delta no toca.

**Diagrama**

- [x] ¿El `estados.puml` dibuja todos los estados con alias `<FASE>_<ESTADO>`? — el `.puml` real ya lo hace y el delta **no lo toca**, así que no se materializa.
- [x] ¿No hay ningún alias que no corresponda a un estado declarado? — sin cambios.
- [x] ¿Cada transición del `.puml` coincide con una fila de la tabla de transiciones? — sin cambios: no hay transiciones nuevas ni destinos nuevos.

**Modelo**

- [x] ¿La entidad es la **primera** `<entity>` del `domains.xml`, con `extends="Expediente"` y `name="JustificacionFaltaProfesoradoV1"`?
- [x] ¿El `<module>` es `expedientes` / `com.educaflow.subsystem.expedientes.db`?
- [x] ¿Ningún campo heredado de `Expediente` está redeclarado? — `dniFirmaDocumentoEntrada` solo se referencia.
- [x] ¿Todo enum propio lleva `<Entidad>` como sufijo? — el delta no crea ningún enum propio; reutiliza `SituacionFirma` del subsistema de firmas por FQN.
- [x] ¿Ningún campo lleva `required="true"`?
- [x] ¿Todo campo de la tabla de §4.1 tiene su columna «quién lo rellena» resuelta?
- [x] ¿Todo campo `servidor` está asignado por alguna acción de §8 o §9? — `situacionFirmaDocumentoEntrada` es la excepción declarada en §4.1: lo asigna su propio getter derivado.
- [x] ¿El `<extra-code-model>` existe si y solo si hay documentos PDF, con una constante por documento? — sin cambios: `SOLICITUD` y `RESOLUCION`.

**Clases**

- [x] ¿El `InitialEventManagerImpl` está en la raíz de la versión, parametrizado con la entidad, con exactamente un `triggerInitialEvent`? — sin cambios (§8).
- [x] Si hay firma en cliente, ¿el `triggerInitialEvent` deja `dniFirmaDocumentoEntrada` con un DNI válido? — sí, y §8 lo marca como dependencia intocable.
- [x] Si hay `createRegistroEntrada`, ¿deja `personaSolicitante` y `personaInteresada` no nulos? — sí (§8).
- [x] Por cada fase: ¿un `trigger<Evento>` por cada evento de la unión de los `events` de sus estados, y ninguno de más? — §9.2, lista de cobertura.
- [x] Por cada fase: ¿un `onEnter<Estado>` por cada estado, y ninguno de más? — §9.2.
- [x] ¿Ningún `PhaseEventManagerImpl` declara `triggerInitialEvent` ni `triggerExit`?
- [x] ¿Cada `trigger*` tiene su lista **numerada** de acciones, y el `UPDATE_STATE` de cada una coincide con la tabla de transiciones?
- [x] ¿`triggerDelete` **no** llama a `UPDATE_STATE`?
- [x] ¿Cada fase declara sus `@Inject`, sus constantes y el `import` de `States` de su propia versión?
- [x] ¿Ningún `trigger*`/`onEnter*` se factoriza en una superclase compartida?

**Validador**

- [x] Por cada fase: ¿un `getForState<Estado>InEvent<Evento>` por cada pareja (estado, evento) salvo `DELETE`, y ninguno de más? — tabla de cobertura de §10.2.
- [x] ¿Cada método tiene sus `field(...)` con las reglas y **argumentos literales**, o un `rules { }` vacío declarado explícitamente?
- [x] ¿Ningún `field(...)` menciona un campo `servidor`? — salvo `pdfSolicitudFirmado`, la excepción documentada del patrón de firma en cliente (§10.2).
- [x] ¿Todo campo editable en la vista de un estado aparece en el `field(...)` del evento que se dispara desde esa vista? — `claveFirmaDocumentoEntrada` es el único editable en `PENDIENTE_PRESENTACION` y está en el `field(...)` de `PRESENTAR`.
- [x] ¿Los enums usados en `ifValueIn` tienen su `import` declarado? — `SituacionFirma` (§10.2).

**Vistas**

- [x] ¿El diseño pasa el checklist de vistas? — sí; ver abajo.

**Permisos y pasos**

- [x] ¿El perfil del estado inicial se asigna por `tramiteCode`? — sí (§12), sin cambios.
- [x] ¿Todo perfil usado por algún estado tiene actor? — sí (§12).
- [x] ¿`design/permisos.xml` es un fragmento con solo lo nuevo? — no existe: el delta no añade permisos.
- [x] ¿La tabla «Ficheros a crear o modificar» lista todos los ficheros reales y ninguno generado? — once filas, todas reales; sin `permisos-demo.xml` porque no hay permisos nuevos y **sin `FirmaController.java`**, que este delta no toca (§6, §9.1).
- [x] **Iniciativa de MODIFICACIÓN:** ¿hay exactamente un paso de fichero por fila de la tabla §6, en su orden relativo y renumerados sin huecos, sin `CreateFilesTask` (el delta no añade fases) y con `./run.sh` como último paso? — once filas y pasos 1–11, más el paso 12 de verificación final.
- [x] ¿Cada paso de un XML dice «cópialo literalmente» con origen y destino, y cada paso de un `.java`/`.kt` apunta a su sección de especificación sin duplicarla?
- [x] ¿El paso final lleva `./run.sh` y la comprobación en runtime de los agujeros que el build no ve?

**Vistas — checklist detallado**

- [x] `design/views.xml` de la raíz: **no se materializa** porque el delta no lo toca; el form plantilla `exp-JustificacionFaltaProfesoradoV1-Templates` sigue siendo el único y con el `<Entidad>` del propio tipo.
- [x] No se declara ningún panel nuevo en el almacén: el bloque de firma son **siete paneles propios del estado** (uno por situación de firma más el del campo derivado oculto), declarados dentro de su `<form>` (vistas.md §3.5), porque solo los usa esa pantalla.
- [x] **X1** — sigue habiendo un `<form state="…">` sin `profile` por cada estado de la fase.
- [x] **X2** — sigue habiendo el `<form state="…" profile="CREADOR">` de los dos estados con perfil y eventos.
- [x] **X3** — ningún `(state, profile)` duplicado.
- [x] Todo `state` es un estado de la fase `RECEPCION` y ninguno lleva la fase dentro del atributo.
- [x] Todo `profile` de un `<form>` (`CREADOR`) está en la unión de perfiles del tipo.
- [x] **Y1** — todo botón se llama `DELETE`, `GUARDAR_DATOS`, `BACK`, `PRESENTAR` o `EXIT`; ningún `<button name="">`.
- [x] **Y2** — `BACK` y `PRESENTAR` tienen botón en el form del `CREADOR`; `PRESENTAR` lo tiene **dos veces**, excluyentes.
- [x] **Y3** — todo `onClick` incluye `subsysExpedientes-event-action`, y la cadena `serial:` **termina** en ella.
- [x] La vista genérica de cada estado lleva botón `EXIT` y sus paneles en solo lectura (`-pdfSolicitud`).
- [x] `EXIT` no aparece en ningún `events` del `TipoExpedienteInstance.xml`.
- [x] La suma de los `colSpan` de los botones del footer es 10 y no pasa de 12.
- [x] Cada bloque sin botones se escribe `<buttons-left/>`.
- [x] Todos los paneles de `<include-panels>` existen en el form plantilla (`pdfSolicitud`), sin repeticiones.
- [x] Se usa `header` por defecto y nunca `includeHeader`.
- [x] Ningún `views.xml` de fase queda vacío.
- [x] La `<action-method>` de AutoFirma sigue en el `views.xml` de **su** fase, junto al botón que la usa.
- [x] Cada campo editable de la vista aparece en el `field(...)` del validador del evento que se dispara desde ella; ningún campo `servidor` es editable — `situacionFirmaDocumentoEntrada` va **oculto** (`showIf="false"`) y **de solo lectura** (`readonly="true"`, puesto tanto en el `<panel name="firmaSolicitudSituacion">` como en el propio `<field>`). `claveFirmaDocumentoEntrada` aparece **una sola vez por panel** y nunca dos veces en el mismo panel.
- [x] ¿El `design.md` lleva el resumen estructural de la plantilla y, por fase, la tabla `(estado, perfil) → paneles → botones`, coherente con el XML? — sí: en el Paso 11 de §7, subapartado «Resumen estructural de las vistas» (form plantilla + tabla de las cuatro filas `(estado, perfil)` de `RECEPCION`, más el detalle de los siete paneles de firma y de los dos botones `PRESENTAR`), cuadrado contra `design/fases/recepcion/views.xml`.
- [x] El visor de PDF sigue el patrón del `depends` *(sin cambios)*.
- [x] La firma en cliente conserva sus **tres** piezas: par de campos `pdfSolicitud`/`pdfSolicitudFirmado`, `<action-method>` + botón `serial:` con sus **8** argumentos, y `FirmaPdf` en el validador (ahora bajo su guarda).
- [x] No se ha aplicado ninguna regla de `k-vistas` / `view-rules.md` / `buttons-panel` / PI `sv-*` / `remote-validation*` a estos ficheros.
- [x] No se ha materializado ningún `menus.xml`.

**Tests**

- [x] ¿`design/test-e2e-desc.md` existe, cubre el delta y no lleva código ni selectores?
- [x] **Iniciativa de MODIFICACIÓN:** ¿los `T-NNN` empiezan en el primer número libre de la carpeta espejo? — la carpeta espejo no existe, así que empiezan en `T-001` (§13).
- [x] **Iniciativa de MODIFICACIÓN:** ¿está la subsección «Tests E2E supersedidos» con cada test que el delta invalida, o **ausente** porque no invalida ninguno? — ausente: no hay tests persistidos de esta versión (§13).
