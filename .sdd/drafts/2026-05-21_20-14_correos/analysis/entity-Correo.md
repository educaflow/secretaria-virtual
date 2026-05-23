# Entidad: Correo

Registro de un correo electrónico que la aplicación envía o ha intentado enviar a un único destinatario identificado por su DNI. Una vez creado, sus datos de envío (asunto, cuerpo, DNI, email, adjuntos, centro y referencia al expediente) son inmutables; solo evolucionan, por acción del servidor, su estado, su número de intentos, la fecha del último intento, el motivo del último fallo y la fecha de envío.

## Modelo de datos

| Campo | Tipo de dato | Relación | Origen del valor | Notas |
|-------|--------------|----------|------------------|-------|
| asunto | texto | — | `cliente` | Obligatorio en el alta. Inmutable una vez creado. |
| cuerpo | HTML enriquecido | — | `cliente` | Obligatorio en el alta. Texto enriquecido. Inmutable una vez creado. |
| dniDestinatario | texto | — | `cliente` | Obligatorio en el alta. Identifica al destinatario. No es relación a usuario: se guarda como dato propio. Inmutable una vez creado. |
| emailDestinatario | texto | — | `cliente` | Obligatorio en el alta. En el alta manual se propone automáticamente si el DNI corresponde a un usuario; el Administrador puede confirmarlo o escribirlo a mano. Queda fijo tras crearse. |
| fechaCreacion | fecha-hora | — | `servidor` | Asignada automáticamente al registrar el Correo. |
| fechaEnvio | fecha-hora | — | `servidor` | Vacía hasta que un intento termina con éxito; entonces se fija al momento del envío. |
| estado | enum | valores: PENDIENTE, ENVIADO, FALLIDO | `servidor` | Inicial PENDIENTE. Evoluciona solo por reglas del servidor (envío automático, reenvío manual). |
| numeroIntentos | entero | — | `servidor` | Inicial 0. Se incrementa en cada intento automático de envío. |
| fechaUltimoIntento | fecha-hora | — | `servidor` | Vacía hasta el primer intento; se actualiza con el momento de cada intento. |
| motivoUltimoFallo | texto largo | — | `servidor` | Descripción del error del último intento fallido; vacío si el último intento no falló. |
| centro | relación | → Centro | `servidor` | En el alta manual queda vacío (el Administrador no pertenece a un centro). En el alta programática lo dicta el subsistema invocador. Nunca lo aporta el cliente desde la interfaz. Inmutable una vez creado. |
| referenciaHistorialEstadoExpediente | relación | → historial de estado de expediente | `servidor` | Opcional. Solo asignable programáticamente por el subsistema invocador; nunca desde la interfaz. Inmutable una vez creado. |
| adjuntos | lista | → AdjuntoCorreo (uno a varios, hijos) | `cliente` | Cero o más adjuntos aportados en el alta. Inmutables una vez creados. |

Estados y transiciones: inicial PENDIENTE; PENDIENTE→ENVIADO (intento con éxito); PENDIENTE→FALLIDO (intento con error); FALLIDO→PENDIENTE (reenvío manual del Administrador); ENVIADO es terminal.

## Validaciones (V-Correo-NNN)

| ID | Campo(s) | Descripción | Condición | Mensaje al usuario | Origen EARS |
|----|----------|-------------|-----------|---------------------|-------------|
| V-Correo-001 | dniDestinatario | El DNI del destinatario es obligatorio en el alta. | dniDestinatario vacío al crear | "El DNI del destinatario es obligatorio." | E-UB-002, E-UN-001 |
| V-Correo-002 | emailDestinatario | El email del destinatario es obligatorio en el alta. | emailDestinatario vacío al crear | "El email del destinatario es obligatorio." | E-UB-003, E-UN-001 |
| V-Correo-003 | asunto | El asunto es obligatorio en el alta. | asunto vacío al crear | "El asunto es obligatorio." | E-UB-004, E-UN-001 |
| V-Correo-004 | cuerpo | El cuerpo es obligatorio en el alta. | cuerpo vacío al crear | "El cuerpo del correo es obligatorio." | E-UB-004, E-UN-001 |
| V-Correo-005 | asunto, cuerpo, dniDestinatario, emailDestinatario, centro, referenciaHistorialEstadoExpediente, adjuntos | Tras crearse, no se pueden modificar los datos de envío del Correo. | intento de cambiar cualquiera de estos campos en un Correo ya existente | "Los datos de un correo ya creado no se pueden modificar." | E-ST-004, E-UN-005 |
| V-Correo-006 | referenciaHistorialEstadoExpediente | La referencia al historial de estado de expediente no puede asignarse ni modificarse desde la interfaz (solo es asignable programáticamente). | intento de asignar o cambiar la referencia desde la interfaz | "La referencia al expediente solo puede asignarla el sistema, no puede establecerse manualmente." | E-UN-009 |
| V-Correo-007 | estado | Solo se puede reenviar un Correo que esté en estado FALLIDO. | solicitud de reenvío sobre un Correo cuyo estado es '{valor}' distinto de FALLIDO | "Solo se pueden reenviar correos en estado FALLIDO; el correo está en estado '{valor}'." | E-EV-007, E-ST-003, E-UN-004 |

## Acciones

| Operación | Cuándo se permite | Validaciones que aplican | Reglas que dispara |
|-----------|-------------------|--------------------------|--------------------|
| Crear (insert) | Solo si el usuario es Administrador (alta manual) o si la solicita otro subsistema (alta programática) | V-Correo-001, V-Correo-002, V-Correo-003, V-Correo-004, V-Correo-006 | R-Correo-001, R-Correo-002, R-Correo-003, R-Correo-004 |
| Modificar (update) | Nunca — los datos de envío del Correo son inmutables; su estado, intentos, fechas y motivo solo los cambia el servidor mediante las operaciones de envío automático y reenvío | V-Correo-005, V-Correo-006 | — |
| Borrar (remove) | Siempre (arrastra el borrado de sus AdjuntoCorreo) | — | R-Correo-008 |
| Reenviar | Solo si el usuario es Administrador y el Correo está en estado FALLIDO | V-Correo-007 | R-Correo-005 |
| Intento de envío automático | Solo si el Correo está en estado PENDIENTE (lo ejecuta la tarea periódica) | — | R-Correo-006, R-Correo-007 |

## Reglas de negocio (R-Correo-NNN)

| ID | Descripción | Entidad | Método | Momento | Más información | Origen EARS |
|----|-------------|---------|--------|---------|-----------------|-------------|
| R-Correo-001 | Al crear un Correo, el sistema fija su fecha de creación al momento actual y su estado inicial a PENDIENTE, con número de intentos a 0 y sin fecha de envío ni de último intento ni motivo de fallo. | Correo | Crear | Antes | Asigna fechaCreacion, estado, numeroIntentos (campos `servidor`). | E-UB-001, E-EV-001, E-EV-002 |
| R-Correo-002 | En el alta manual (Administrador), el sistema deja el Correo sin centro asociado. | Correo | Crear | Antes | Asigna centro = vacío en el alta manual (campo `servidor`). | E-EV-001, E-UB-008 |
| R-Correo-003 | En el alta programática, el sistema fija el centro y, si se aporta, la referencia al historial de estado del expediente con los valores que indica el subsistema invocador. | Correo | Crear | Antes | Asigna centro y referenciaHistorialEstadoExpediente (campos `servidor`) en el alta programática. | E-EV-002, E-OP-001 |
| R-Correo-004 | En el alta manual, al introducir el DNI del destinatario el sistema propone el email del usuario con ese DNI si existe, y lo deja vacío si no existe. | Correo | Crear | Antes | Propuesta de emailDestinatario; el valor final lo confirma/edita el Administrador (campo `cliente`). | E-EV-003, E-EV-009 |
| R-Correo-005 | Al reenviar un Correo en estado FALLIDO, el sistema lo devuelve al estado PENDIENTE para que la próxima ejecución de la tarea periódica lo reintente. | Correo | Reenviar | Después | Asigna estado = PENDIENTE (campo `servidor`). | E-EV-007, E-ST-002 |
| R-Correo-006 | En el intento de envío automático de un Correo PENDIENTE, el sistema lo entrega a la infraestructura de correo, incrementa el número de intentos y registra la fecha del último intento. | Correo | Intento de envío automático | Antes | Asigna numeroIntentos y fechaUltimoIntento (campos `servidor`). Un único intento por cada entrada en PENDIENTE. | E-EV-004, E-ST-001, E-UB-010, E-UB-011 |
| R-Correo-007 | Según el resultado del intento, el sistema marca el Correo como ENVIADO y registra la fecha de envío si tuvo éxito, o como FALLIDO y registra el motivo del fallo si terminó con error. | Correo | Intento de envío automático | Después | Asigna estado, fechaEnvio o motivoUltimoFallo (campos `servidor`). | E-EV-005, E-EV-006 |
| R-Correo-008 | Al borrar un Correo, el sistema borra en cascada todos sus AdjuntoCorreo. | Correo | Borrar | Después | Integridad referencial CASCADE desde el padre hacia los hijos. | — |