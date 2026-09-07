---
type: design-guidelines
---

- Esta iniciativa es el **mismo cambio** que ya se implementó para las tareas de firma del subsistema `firmas` en la iniciativa `.sdd/drafts/2026-09-01_11-21_firma-en-servidor`. **MUST** reutilizarse ese mecanismo en vez de inventar uno nuevo: `com.educaflow.subsystem.firmas.util.SituacionFirmaBuilder` (o su equivalente generalizado a partir de un DNI en lugar de un `User`), el enum `SituacionFirma`, y el overload `CertificadoDigitalService.getAlmacenClaveByDni(dni, claveAcceso)` que ya acepta la clave tecleada.
- El punto exacto que hay que cambiar es la vista de la fase de recepción del tipo de expediente: `src/main/java/com/educaflow/tramites/profesores/justificacion_falta_profesorado/actual/v1/recepcion/views.xml`, donde hoy el botón `PRESENTAR` encadena `exp-JustificacionFaltaProfesoradoV1-firmarDocumentacionParaPresentar-action` (que llama a `FirmaController.firmarDocumentoEntrada`, es decir AutoFirma en el equipo del usuario) con `subsysExpedientes-event-action`.
- La firma en el servidor debe apoyarse en `com.educaflow.subsystem.expedientes.controllers.FirmaController` (el mismo controlador que ya sirve la firma con AutoFirma para los documentos de entrada de cualquier expediente), de forma que el mecanismo quede disponible para todos los tipos de expediente y no solo para este trámite. El DNI que determina la situación de firma y con el que se firma es `Expediente.dniFirmaDocumentoEntrada`, el mismo que ya usa `firmarDocumentoEntrada`.
- El campo donde el usuario teclea la clave debe ser **transitorio** y de tipo contraseña, como `TareaFirma.claveFirma`: nunca llega a base de datos y nunca vuelve al cliente en la respuesta.
- La comprobación de la firma que ya hace el validador de estado (`FirmaPdf(pdfSolicitud, dniFirmaDocumentoEntrada)`) se conserva tal cual: sigue siendo la defensa del camino de firma en el equipo del usuario.
- Aplicar `k-secure-coding`: cuando corresponda firmar en el servidor, lo que el cliente envíe en el documento firmado debe descartarse, y la clave tecleada nunca debe escribirse en logs ni en mensajes de error.
- **No se escriben tests** en esta iniciativa (ni unitarios, ni de arquitectura, ni E2E): queda expresamente fuera por decisión del usuario.
- **MUST NOT** tocarse nada de git (ni commits, ni ramas, ni stash) durante toda la iniciativa.
