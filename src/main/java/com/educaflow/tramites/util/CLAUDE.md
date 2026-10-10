# `tramites/util` — código común a varios tipos de expediente

Aquí vive lo que **más de un tipo de expediente** necesita y que **no es de ningún subsistema**.

Es el único sitio de `tramites/` con código Java/Kotlin fuera de una carpeta de trámite.
Todo lo demás de `tramites/` (`shared/`, `views/`, `view_models/`) son recursos XML compartidos.

## Qué puede entrar

Una pieza entra aquí **solo si cumple las siete**:

1. La usa —o está pensada para usarla— **más de un tipo de expediente**.
   Lo que solo sirve a un trámite se queda en su carpeta de versión.
2. **Necesita un `subsystem`.**
   Si le basta `base`, su sitio es `base/util` (utilidades de bajo nivel, sin estado) o `base/infrastructure` (clases completas reutilizables en cualquier proyecto).
3. **No es dominio propio de un subsistema.**
   Si lo es, va a ese subsistema. Ejemplo real: «firmar un PDF en el servidor con el certificado de un DNI» se fue a `subsystem/criptografia` y **no** está aquí; aquí solo quedó el pegamento que lo conecta con el `trigger*` de un tipo de expediente.
4. **No la necesita `subsystem/tramitador`.**
   Si el motor de tramitación tuviera que conocerla, es que no es de aquí — y el motor **MUST** mantenerse lo más pequeño posible, porque todo lo que se le añade lo heredan todos los tipos de expediente.
5. **No se acopla a una entidad concreta.**
   Los campos entran como parámetros o como *getters* `KCallable`, igual que hace `FirmaPdf` en `base.infrastructure.validation.rules`.
6. Va en un **subpaquete por propósito** (`firma/`, …).
   **MUST NOT** existir un `Utils`/`Helpers` cajón de sastre: lo que no encaje en un propósito ya existente abre uno nuevo.
7. **No es una función que agrupe reglas del catálogo del DSL de validación.**
   Las reglas de un evento se escriben en el `StateEventValidatorImpl` de cada tipo, para que se vea en él toda la validación, aunque varios tipos repitan el mismo bloque.

## Dirección de las dependencias

`tramites → subsystem → base`, y nunca al revés.

**MUST NOT** existir ningún import de `com.educaflow.tramites..` desde `base/**`, `subsystem/**`, `system/**` ni `secretariavirtual/**`.
La regla de arquitectura **C2** lo verifica para `base.infrastructure`.

`com.educaflow.tramites..` es paquete **exento** del resto de reglas ArchUnit (tiene arquitectura propia), así que aquí casi nada se comprueba solo: las siete condiciones de arriba se sostienen por revisión, no por test.

## Contenido

- **`firma/`** — firmar cualquier documento de un expediente (el de entrada o cualquier otro), en el servidor o en el equipo del usuario.
  En el equipo del usuario: el controlador que monta la petición para AutoFirma__!! (`FirmaClienteController`), al que la vista llama con una `<action-method>` encadenada con `serial:` antes del evento.
  En el servidor: las reglas del DSL de validación que gobiernan cuándo y con qué clave se puede firmar, el ayudante que el `trigger*` usa para firmar y el controlador que le dice a la vista en qué situación de firma está quien firma.
  La situación de firma **MUST NOT** ser un campo de ningún `domains.xml`: solo la necesita la pantalla para elegir qué panel pinta, así que el formulario la pide en su `onLoad` al controlador y el servidor la recalcula del DNI cada vez que valida o firma.
  Depende de `subsystem/criptografia`. **MUST NOT** depender de `subsystem/firmas`, que es la bandeja de tareas de firma —lo que el usuario final ve para firmar lo que le ponen delante— y podría desaparecer.
  Sí es legítimo que un trámite use `subsystem/firmas` cuando lo que quiere es poner un documento a la firma de alguien.
- **`entrada/`** — lo que hacen igual todos los tipos de expediente en su fase común `ENTRADA`.
  `EntradaHelper`: firmar la solicitud en el servidor si a quien presenta le corresponde (`firmarSolicitudSiEsEnServidor`), asentarla en el registro de entrada y guardar el resguardo (`presentar`), y las dos ayudas de los `trigger*` de la fase (`estaEn`, para los eventos que salen de más de un estado, y `exigePresentadoEnPapel`, para los que son de un solo modo de presentación).
  `CamposEntrada`: el descriptor con el que cada tipo le dice al helper cuáles son sus campos.
  Depende de `subsystem/criptografia` (a través de `firma/`), de `subsystem/tramitador` (`EventContext`, `State`) y de `subsystem/registroentradasalida` (el `RegistroEntrada` que devuelve `EventContext`).
- **`verificacion/`** — lo que hacen igual todos los tipos de expediente en su fase común `VERIFICACION`.
  `VerificacionHelper.avisarDeSubsanacion`: el correo que avisa a quien presentó de que tiene que subsanar; es una cortesía, así que si no hay a quién escribir no se envía y no bloquea la verificación.
  Depende de `subsystem/notificaciones`.
- **`registro/`** — el aviso al solicitante de cada documento de su expediente que se asienta en el registro de entrada o de salida.
  `AvisoRegistroHelper`: un correo con el documento registrado adjunto y un SMS con el número de registro (`avisarDeRegistroEntrada`, que llama `EntradaHelper.presentar`, y `avisarDeRegistroSalida`, que **MUST** llamar todo `trigger*` justo después de `createRegistroSalida`).
  Es una cortesía: cada canal que no supera su validación (sin correo o sin móvil, p. ej. en papel si no se indicaron) no se envía y no bloquea el evento.
  No está en `subsystem/registroentradasalida` porque `notificaciones` depende de `expedientes`, que depende de `registroentradasalida`: sería un ciclo (regla C7).
  Depende de `subsystem/notificaciones` y de `subsystem/registroentradasalida`.

`entrada/` y `verificacion/` cumplen la condición 5 de dos formas, porque los campos con los que trabajan (`pdfSolicitud`, `pdfSolicitudFirmada`, `pdfJustificanteRegistroEntrada`, `resultadoVerificacion`, `textoSubsanacion`) los declara cada tipo en su `domains.xml` y no son de `Expediente`:

- como **parámetros** (`avisarDeSubsanacion(expediente, textoSubsanacion)`);
- como el descriptor **`CamposEntrada<T>`**, un record de referencias a los getters y setters del tipo que cada uno construye una vez, como constante `CAMPOS_ENTRADA` de su `<Code>Util`.

A qué estado se pasa después **MUST** decidirlo cada tipo: es el único que puede nombrar los estados de su `States`, así que aquí no hay ningún `updateState`.
