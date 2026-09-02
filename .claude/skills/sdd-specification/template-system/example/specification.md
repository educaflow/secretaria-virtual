---
type: specification
template: system
---

# Objetivo

Permitir que los alumnos soliciten certificados académicos a su centro y que la secretaría los resuelva, emitiéndolos o rechazándolos. Es un **subsistema**. Depende funcionalmente del envío de notificaciones por correo y del registro de entrada/salida de documentos.

# Actores

- **Alumno**: solicita certificados y consulta el estado de sus solicitudes.
- **Secretario**: resuelve las solicitudes de certificado de su centro.

# Historias de usuario

## HU-001 — Como Alumno quiero solicitar un certificado académico para acreditar mis estudios ante terceros

- ESC-001 — Solicitud correcta:
  1. El alumno inicia sesión.
  2. Abre «Mis certificados» y crea una solicitud nueva eligiendo el tipo «Certificado de matrícula».
  3. La envía.
  4. El sistema la muestra en su listado con estado PENDIENTE.
- ESC-002 — Solicitud sin tipo:
  1. El alumno inicia sesión.
  2. Abre «Mis certificados» y crea una solicitud nueva sin elegir tipo.
  3. La envía.
  4. El sistema muestra «El tipo de certificado es obligatorio».

## HU-002 — Como Secretario quiero resolver las solicitudes recibidas para emitir o rechazar los certificados de mi centro

- ESC-003 — Emisión con y sin email del alumno:
  1. El alumno inicia sesión, crea una solicitud de «Certificado de matrícula» y la envía.
  2. El secretario inicia sesión, abre «Solicitudes de certificado» y entra en la solicitud pendiente.
  3. Pulsa Emitir; el sistema comprueba que está PENDIENTE y que el secretario pertenece a la secretaría del centro, genera el documento a partir de la plantilla y pasa la solicitud a EMITIDA.
  4. Si el alumno tiene email registrado: el sistema le envía un correo con el certificado adjunto.
  5. Si no tiene email registrado: el sistema no envía ningún correo y la solicitud queda EMITIDA igualmente.
- ESC-004 — Rechazo sin motivo:
  1. El alumno inicia sesión, crea una solicitud y la envía.
  2. El secretario inicia sesión y abre la solicitud recibida.
  3. Pulsa Rechazar sin indicar motivo.
  4. El sistema muestra «El motivo es obligatorio».

## HU-003 — Como Alumno quiero consultar el estado de mis solicitudes para saber si mi certificado está disponible

- ESC-005 — Consulta:
  1. El alumno inicia sesión, crea una solicitud y la envía.
  2. Vuelve al listado «Mis certificados».
  3. El sistema muestra la solicitud con estado PENDIENTE.

# Modelos

| Fichero | Modelo | Qué representa |
|---|---|---|
| [entity-SolicitudCertificado.md](./entity-SolicitudCertificado.md) | SolicitudCertificado | Petición de un alumno para obtener un certificado académico, con su ciclo de resolución. |
| [entity-AdjuntoSolicitud.md](./entity-AdjuntoSolicitud.md) | AdjuntoSolicitud | Documento que el alumno aporta junto a una solicitud de certificado. |
| [entity-TipoCertificado.md](./entity-TipoCertificado.md) | TipoCertificado | Catálogo de certificados que el centro puede emitir. |

Una SolicitudCertificado referencia un único TipoCertificado del catálogo. El catálogo de tipos existe con independencia de las solicitudes. Una SolicitudCertificado contiene cero o varios AdjuntoSolicitud (composición: los adjuntos se borran al borrar la solicitud).

# Pantallas

| Fichero | Pantalla | Para qué sirve |
|---|---|---|
| [screen-mis-solicitudes.md](./screen-mis-solicitudes.md) | Mis certificados (alumno) | Listado del alumno con sus solicitudes y el formulario de alta/detalle de una solicitud. |
| [screen-solicitudes-centro.md](./screen-solicitudes-centro.md) | Solicitudes del centro (secretaría) | Listado de las solicitudes del centro y el formulario de resolución (Emitir/Rechazar). |

# Seguridad

- **Administrador:** ve las solicitudes de todos los centros, en solo lectura.
- **Supervisor:** ve las solicitudes de su centro, en solo lectura.
- **Alumno:** crea solicitudes y ve solo las suyas.
- **Exalumno:** ve solo las suyas; no puede crear nuevas.
- **Familiar:** ve las solicitudes de los alumnos a su cargo; no puede crear.
- **Cargos Secretario y Vicesecretario:** resuelven (emiten/rechazan) las solicitudes de su centro.
- **Cargos Director, Jefes de estudio, Administrativas y Conserjes:** consultan las solicitudes de su centro en solo lectura.

# Recursos y datos iniciales

- Plantilla PDF de cada tipo de certificado.
- Catálogo de tipos de certificado precargado al arrancar (matrícula, notas, traslado).

# Fuera de alcance

- Firma digital del certificado emitido.
- Pago de tasas asociado a la solicitud.
