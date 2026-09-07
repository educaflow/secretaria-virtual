# Tests unitarios — Justificación de falta del profesorado (`JustificacionFaltaProfesoradoV1`)

## No aplican tests unitarios de clases

Para este artefacto **no se describe ningún test unitario** de las clases del tipo de expediente, y **no se añade ningún test nuevo** al proyecto. No es una omisión: es una decisión de contrato, por dos motivos.

**1. La conformidad ya la cubren tests existentes, escritos a mano.** Los tests genéricos de `src/test/java/com/educaflow/tiposexpedientes/` recorren automáticamente todos los tipos de expediente del árbol, así que cubren este tipo por construcción, sin tocar nada. Para este diseño comprueban:

- que las dos fases declaradas, `RECEPCION` y `TRAMITACION`, tienen su `PhaseEventManagerImpl` y su `StateEventValidatorImpl` en la carpeta de la fase en minúsculas;
- que hay exactamente un `InitialEventManagerImpl` en la raíz de la versión, con un único `triggerInitialEvent`, parametrizado con `JustificacionFaltaProfesoradoV1`, la primera `<entity>` del `domains.xml`;
- que en cada fase hay un `trigger<Evento>` por cada evento de la fase y un `onEnter<Estado>` por cada estado, ninguno de más — en `RECEPCION`, los `trigger` de `DELETE`, `GUARDAR_DATOS`, `BACK` y `PRESENTAR` y los `onEnter` de `ENTRADA_DATOS` y `PENDIENTE_PRESENTACION`;
- que en cada fase hay un `getForState<Estado>InEvent<Evento>` por cada pareja (estado, evento) salvo las de `DELETE`, ninguno de más;
- que el `TipoExpedienteInstance.xml` tiene exactamente un estado inicial, el `events` escrito en todos los estados y perfiles válidos;
- que el `estados.puml` dibuja todos los estados con el alias `<FASE>_<ESTADO>` y sin alias fantasma;
- que los `views.xml` de cada fase tienen el form genérico de cada estado, el form con perfil donde procede, sin duplicados, y que todo evento tiene su botón —incluidos los **dos** botones `PRESENTAR` excluyentes por `showIf` del `<form state="PENDIENTE_PRESENTACION" profile="CREADOR">`—, con su `onClick` terminando en `subsysExpedientes-event-action`;
- que no se referencia la clase `States` de otro tipo ni de otra versión, y que el `<defaultTipoExpediente>` (`v1`) apunta a una carpeta de versión que existe.

Esos tests **se escriben a mano** y los `.java` son su fuente de verdad: este diseño **no propone crearlos, modificarlos, ampliarlos ni regenerarlos**.

**2. Las clases del tipo no son unitariamente testeables con sentido.** `InitialEventManagerImpl`, `PhaseEventManagerImpl` y `StateEventValidatorImpl` no tienen lógica propia aislable: dependen del `Tramitador`, del `EventContext`, de la persistencia, de la generación y firma de PDF y de la clase `States` generada por el build. Un test unitario tendría que mockear todo eso y acabaría verificando el mock, no el trámite.

A estos dos motivos se suma una tercera restricción **explícita del diseño**: `design.md` §13 y las guías de diseño excluyen expresamente escribir tests en esta iniciativa (ni unitarios, ni de arquitectura, ni E2E), así que **ninguna** de las clases que el delta crea o modifica en los subsistemas lleva test nuevo (tabla siguiente).

**Dónde se verifica el comportamiento real.** End-to-end, en [`test-e2e-desc.md`](./test-e2e-desc.md): cada transición de la máquina de estados que el delta atraviesa, cada perfil que la dispara, cada validación que debe impedir avanzar y cada estado de solo lectura. Y en el paso 12 del diseño, con `./run.sh` y el recorrido en runtime de las seis situaciones de firma.

## Clases del tipo — excluidas y por qué

| Clase | Motivo de la exclusión |
|---|---|
| `com.educaflow.tramites.profesores.justificacion_falta_profesorado.actual.v1.InitialEventManagerImpl` | Sin lógica aislable: inicializa la entidad y su efecto lo ejerce el `Tramitador`. Además el delta **no la toca** (§8). Cubierta por los tests existentes (forma) y por `test-e2e-desc.md` (comportamiento). |
| `com.educaflow.tramites.profesores.justificacion_falta_profesorado.actual.v1.recepcion.PhaseEventManagerImpl` | Sin lógica aislable: `triggerPresentar` y `triggerBack` dependen del `Tramitador`, del `EventContext`, del `JustificacionFaltaProfesoradoV1Repository`, de la generación y firma del PDF y de la clase `States` generada. |
| `com.educaflow.tramites.profesores.justificacion_falta_profesorado.actual.v1.recepcion.StateEventValidatorImpl` | Declarativa: no ejecuta nada; sus reglas las interpreta el `Tramitador` para rechazar el evento y construir el `AllowProperties`. |
| `com.educaflow.tramites.profesores.justificacion_falta_profesorado.actual.v1.tramitacion.PhaseEventManagerImpl` | Sin lógica aislable, y además el delta **no la toca** (§9.3). |
| `com.educaflow.tramites.profesores.justificacion_falta_profesorado.actual.v1.tramitacion.StateEventValidatorImpl` | Declarativa, y además el delta **no la toca** (§10.3). |

Clases de subsistema que el delta crea o modifica (§6) y que tampoco llevan test nuevo, por la restricción explícita de `design.md` §13 y de las guías de diseño:

| Clase | Motivo de la exclusión |
|---|---|
| `com.educaflow.subsystem.firmas.util.SituacionFirmaBuilder` | El diseño excluye escribir tests en esta iniciativa. Su nuevo `buildByDni(String)` es además el cuerpo que ya tenía `build(User)`, y consulta el certificado digital del DNI contra base de datos. |
| `com.educaflow.subsystem.firmas.util.FirmaEnServidorHelper` | El diseño excluye escribir tests en esta iniciativa. Es una **extracción sin cambio de comportamiento** de lógica que hoy vive en `TareaFirmaServiceImpl`, y su `isClaveCertificadoIncorrecta` resuelve el `CertificadoDigitalService` con `Beans.get(...)` y lee el certificado de base de datos. |
| `com.educaflow.subsystem.firmas.service.impl.TareaFirmaServiceImpl` | El diseño excluye escribir tests en esta iniciativa. El cambio es un refactor de delegación, sin cambio de comportamiento ni de literales. |
| `com.educaflow.subsystem.criptografia.service.AlmacenClaveResolver` | El diseño excluye escribir tests en esta iniciativa. La nueva sobrecarga `getByDNI(String, String)` es un delegador fino sobre `CertificadoDigitalService`. |
| `com.educaflow.subsystem.expedientes.services.internal.FirmaDocumentoEntradaService` | El diseño excluye escribir tests en esta iniciativa. `firmarDocumentoEntradaEnServidor` depende del `AlmacenClaveResolver` y de la firma real del `DocumentoPdf`. |
| `com.educaflow.subsystem.expedientes.controllers.ExpedienteController` | El diseño excluye escribir tests en esta iniciativa. El cambio es una línea en el `catch (BusinessException)` de `triggerEvent`, cuyo efecto solo se observa en la respuesta al cliente (RUI-011). |
| `FirmaDocumentoEntradaRules.kt` (paquete `com.educaflow.subsystem.expedientes.services.validation.rules`) | El diseño excluye escribir tests en esta iniciativa. Son reglas del DSL: declaran, y su efecto lo produce el `Tramitador` al interpretarlas. |

## Tests nuevos a crear

**Ninguno.** Este diseño no añade ningún fichero bajo `src/test/`.
