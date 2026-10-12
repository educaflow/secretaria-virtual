# `tramites/shared` — lo común a varios tipos de expediente

Aquí vive lo que **más de un tipo de expediente** necesita y que **no es de ningún subsistema**: los paneles comunes, el modelo común y el código que trabaja con ellos.

Cada carpeta es **un propósito** y junta lo suyo: sus paneles (`common-template-views.xml`), su modelo (`domains.xml`), sus traducciones y su código Java/Kotlin.
Una carpeta puede tener solo XML (`presentadoenpapel/`), solo código (`notificaciones/`) o las dos cosas (`expedienteresoluble/`, `firma/`).
En la raíz quedan los paneles que no son de ningún propósito (`common-template-views.xml`, `system-template-views.xml`).

Es el único sitio de `tramites/` con código Java/Kotlin fuera de una carpeta de trámite.

## Qué puede entrar

Una pieza de código entra aquí **solo si cumple las siete**:

1. La usa —o está pensada para usarla— **más de un tipo de expediente**.
   Lo que solo sirve a un trámite se queda en su carpeta de versión.
2. **Necesita un `subsystem`.**
   Si le basta `base`, su sitio es `base/util` (utilidades de bajo nivel, sin estado) o `base/infrastructure` (clases completas reutilizables en cualquier proyecto).
3. **No es dominio propio de un subsistema.**
   Si lo es, va a ese subsistema. Ejemplo real: «firmar un PDF en el servidor con el certificado de un DNI» se fue a `subsystem/criptografia` y **no** está aquí; aquí solo quedó el pegamento que lo conecta con el `trigger*` de un tipo de expediente.
4. **No la necesita `subsystem/tramitador`.**
   Si el motor de tramitación tuviera que conocerla, es que no es de aquí — y el motor **MUST** mantenerse lo más pequeño posible, porque todo lo que se le añade lo heredan todos los tipos de expediente.
5. **Solo se acopla a una entidad si está en la carpeta de esa entidad.**
   `expedienteresoluble/` declara `ExpedienteResoluble` en su `domains.xml`, así que su código la recibe y usa sus getters y setters directamente.
   Las demás carpetas (`firma/`, `notificaciones/`) no conocen ninguna entidad concreta: los campos entran como parámetros o como *getters* `KCallable`, igual que hace `FirmaPdf` en `base.infrastructure.validation.rules`, o reciben un `Expediente`.
6. Va en la **carpeta de su propósito** (`firma/`, …).
   **MUST NOT** existir un `Utils`/`Helpers` cajón de sastre: lo que no encaje en un propósito ya existente abre una carpeta nueva.
7. **No es una función que agrupe reglas del catálogo del DSL de validación.**
   Las reglas de un evento se escriben en el `StateEventValidatorImpl` de cada tipo, para que se vea en él toda la validación, aunque varios tipos repitan el mismo bloque.

## Dirección de las dependencias

`tramites → subsystem → base`, y nunca al revés.

**MUST NOT** existir ningún import de `com.educaflow.tramites..` desde `base/**`, `subsystem/**`, `system/**` ni `secretariavirtual/**`.
La regla de arquitectura **C2** lo verifica para `base.infrastructure`.

`com.educaflow.tramites..` es paquete **exento** del resto de reglas ArchUnit (tiene arquitectura propia), así que aquí casi nada se comprueba solo: las siete condiciones de arriba se sostienen por revisión, no por test.

## Contenido

- **`expedienteresoluble/`** — la entidad `ExpedienteResoluble` (`domains.xml`), que extienden los tipos que se tramitan con solicitud, verificación y resolución firmada, y sus paneles.
- **`registro/`** — lo que hacen igual todos los `ExpedienteResoluble` al asentar sus documentos en el registro.
  `RegistroEntradaSalidaHelper`: asentar la solicitud firmada en el registro de entrada, guardar el resguardo, notificarlo y dar por atendida la subsanación (`registroEntradaSolicitud`); asentar la resolución firmada en el registro de salida, guardar el documento registrado y notificarlo (`registroSalidaResolucion`).
  Desde qué estado se dispara un evento que sale de más de uno (el `BACK`) lo pregunta cada tipo al motor: `ExpedienteUtil.getState(original) == States.<Fase>.<ESTADO>`.
  Antes de llamar a `registroEntradaSolicitud` / `registroSalidaResolucion` el tipo deja en el expediente el `pdfSolicitudFirmada` / `pdfResolucionFirmada` que se registra, con su `fechaHoraPdf*Firmada` ya puesta: el helper guarda el documento que devuelve el registro y la fecha del asiento (`fechaHoraPdfJustificanteRegistroEntrada`, `fechaHoraPdfResolucionFirmadaRegistroSalida`), y no toca la de la firma.
  Depende de `subsystem/tramitador` (`EventContext`), de `subsystem/registroentradasalida` (el `RegistroEntrada`/`RegistroSalida` que devuelve `EventContext`) y de `subsystem/notificaciones` (a través de `notificaciones/`).
- **`firma/`** — firmar cualquier documento de un expediente (el de entrada o cualquier otro), en el servidor o en el equipo del usuario, y los paneles con los que se firma.
  En el equipo del usuario: el controlador que monta la petición para AutoFirma__!! (`FirmaClienteController`), al que la vista llama con una `<action-method>` encadenada con `serial:` antes del evento.
  En el servidor: las reglas del DSL de validación que gobiernan cuándo y con qué clave se puede firmar (`FirmaServidorRules.kt`) y `FirmaExpedienteHelper.firmarSolicitudEnServidor` / `firmarResolucionEnServidor(dniFirmante, expediente, nombreCampoFirma)`: recalculan la situación de firma del DNI y, si le corresponde firmar en el servidor, firman `pdfSolicitud` / `pdfResolucion` con `claveCertificado` por `FirmaEnServidorService.firmar` de `subsystem/criptografia` (que ya comprueba el DNI, la situación y la clave) y dejan el resultado en `pdfSolicitudFirmada` / `pdfResolucionFirmada`; con AutoFirma__!! no hacen nada, porque el firmado ya llegó del equipo del usuario. El DNI lo pasa el `trigger*`, de `SecurityUtil.getUser().getDni()`.
  La fecha de la firma (`fechaHoraPdfSolicitudFirmada`, `fechaHoraPdfResolucionFirmada`) se saca del propio PDF, la de su última firma, firmado en el servidor o en el equipo del usuario: `FirmaExpedienteHelper.actualizarFechaHoraPdf*Firmada`, que cada tipo llama cuando el documento ya tiene todas sus firmas (en una resolución que firman el secretario y después el director, solo tras la del director).
  No la pone el registro: la fecha del registro es otra.
  En papel la solicitud firmada es el escaneado que subió el `TRAMITADOR`, sin firma electrónica: su fecha la pone el tipo en el `triggerContinuar` con el `getCreatedOn()` del `MetaFile`, que es cuándo se subió.
  La situación de firma de quien firma se la da a la vista `CertificadoDigitalController.getSituacionFirma`, de `subsystem/criptografia`, no esta carpeta.
  La situación de firma **MUST NOT** ser un campo de ningún `domains.xml`: solo la necesita la pantalla para elegir qué panel pinta, así que el formulario la pide en su `onLoad` al controlador y el servidor la recalcula del DNI cada vez que valida o firma.
  Depende de `subsystem/criptografia`. **MUST NOT** depender de `subsystem/firmas`, que es la bandeja de tareas de firma —lo que el usuario final ve para firmar lo que le ponen delante— y podría desaparecer.
  Sí es legítimo que un trámite use `subsystem/firmas` cuando lo que quiere es poner un documento a la firma de alguien.
- **`notificaciones/`** — los avisos al solicitante de su expediente, por correo y por SMS. Vale para cualquier `Expediente`.
  `NotificacionesExpedienteHelper`: de cada documento que se asienta en el registro, un correo con el documento registrado adjunto y un SMS con el número de registro (`notificarRegistroEntrada`, que llama `RegistroEntradaSalidaHelper.registroEntradaSolicitud`, y `notificarRegistroSalida`, que llama `RegistroEntradaSalidaHelper.registroSalidaResolucion` y que **MUST** llamar todo `trigger*` que use `createRegistroSalida` directamente, justo después); y de lo que tiene que subsanar tras la verificación (`notificarSubsanacion`, que llama el `triggerVerificar` de cada tipo con su `textoSubsanacion`).
  El aviso es una cortesía: un canal cuyo destinatario no tiene dato (sin correo o sin móvil, p. ej. en papel si no se indicaron) no se envía y el evento sigue.
  Cualquier otro fallo al insertar la notificación es un error de programación y para el evento: no se captura la validación del servicio para seguir adelante (ver `k-code-quality`, `disenyo.md`, «La validación del servicio no es control de flujo»).
  No está en `subsystem/registroentradasalida` porque `notificaciones` depende de `expedientes`, que depende de `registroentradasalida`: sería un ciclo (regla C7).
  Depende de `subsystem/notificaciones` y de `subsystem/registroentradasalida`.
- **`presentadoenpapel/`** — los paneles del camino en papel (la solicitud escaneada).
- **`solicitanteinteresada/`** — los paneles de la persona solicitante y la interesada.

A qué estado se pasa después **MUST** decidirlo cada tipo: es el único que puede nombrar los estados de su `States`, así que aquí no hay ningún `updateState`.
