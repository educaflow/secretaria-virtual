# Entidad: TareaFirma

Tarea de firma asignada a un usuario firmante. Agrupa uno o varios documentos que deben firmarse juntos.

## Modelo de datos

| Campo                | Tipo de dato | Relación                                | Notas                                                                  |
|----------------------|--------------|-----------------------------------------|------------------------------------------------------------------------|
| firmante             | relación     | → Usuario                               | usuario que debe firmar                                                |
| documentosFirma      | lista        | → DocumentoFirma (uno a varios)         | documentos asociados a la tarea                                        |
| fechaSolicitud       | fecha-hora   | —                                       | momento en que se crea la tarea                                        |
| fechaResolucion      | fecha-hora   | —                                       | momento en que se firma o se rechaza                                   |
| firmaRapida          | booleano     | —                                       | si "sí", se firma automáticamente con el certificado del usuario       |
| estadoTareaFirma     | enum         | valores: PENDIENTE, FIRMADO, RECHAZADO  | estado actual de la tarea                                              |
| motivoFirma          | texto        | —                                       | texto que indica por qué hay que firmar                                |
| motivoRechazo        | texto largo  | —                                       | texto que el firmante introduce al rechazar                            |
| fqcnFirmaNotifier    | texto        | —                                       | identificador del sistema externo que ha solicitado la firma           |
| fqcnCallBackData     | texto        | —                                       | identificador del tipo de datos de retorno                             |
| callBackData         | texto largo  | —                                       | datos que el sistema solicitante envía y recibe al finalizar           |
| x, y, width, height  | decimal      | —                                       | geometría (posición y tamaño) de la firma sobre el documento PDF       |
| page                 | entero       | —                                       | página del documento donde se estampa la firma                         |

## Validaciones (V-XXX)

| ID    | Campo(s)                            | Descripción                                                  | Condición                | Mensaje al usuario                                                  |
|-------|-------------------------------------|--------------------------------------------------------------|--------------------------|---------------------------------------------------------------------|
| V-001 | firmante                            | El firmante debe estar relleno                               | Siempre                  | "El firmante es obligatorio."                                       |
| V-002 | fechaSolicitud                      | La fecha de la solicitud debe estar rellena                  | Siempre                  | "La fecha de la solicitud es obligatoria."                          |
| V-003 | estadoTareaFirma                    | El estado debe estar relleno                                 | Siempre                  | "El estado de la tarea es obligatorio."                             |
| V-004 | motivoFirma                         | El motivo de la firma debe estar relleno                     | Siempre                  | "El motivo de la firma es obligatorio."                             |
| V-005 | x, y, width, height, page           | La geometría (posición, tamaño y página) debe estar rellena  | Siempre                  | "La posición y el tamaño de la firma en el documento son obligatorios." |
| V-006 | motivoRechazo                       | El motivo de rechazo debe estar relleno al rechazar          | Al rechazar la firma     | "El motivo de rechazo es obligatorio para rechazar la firma."       |
| V-007 | documentosFirma[].documentoFirmado  | Todos los documentos firmados deben ser válidos              | Al finalizar la firma    | "Alguno de los documentos firmados no es válido."                   |

## Acciones

| Operación                       | Cuándo se permite                                                       | Validaciones que aplican           | Reglas que dispara |
|---------------------------------|-------------------------------------------------------------------------|------------------------------------|--------------------|
| Crear (insert)                  | Solo desde otro sistema que solicita la firma (sin alta manual)         | V-001, V-002, V-003, V-004, V-005  | —                  |
| Modificar (update)              | Nunca de forma directa; solo a través de las operaciones de negocio     | —                                  | —                  |
| Borrar (remove)                 | Nunca — la tarea es un registro histórico                               | —                                  | —                  |
| Marcar como rechazada           | Solo si estado = PENDIENTE                                              | V-006                              | R-001, R-002       |
| Marcar como firmada             | Solo si estado = PENDIENTE y la firma se ha completado                  | V-007                              | R-003, R-004       |
| Firmar documentos con AutoFirma | Solo si estado = PENDIENTE                                              | —                                  | R-005              |

## Reglas de negocio (R-XXX)

| ID    | Descripción                                                                                                                          | Entidad     | Método                          | Momento | Más información                                                                |
|-------|--------------------------------------------------------------------------------------------------------------------------------------|-------------|---------------------------------|---------|--------------------------------------------------------------------------------|
| R-001 | Cambia el estado a RECHAZADO, guarda el motivo de rechazo y fija la fecha de resolución al momento actual                            | TareaFirma  | Marcar como rechazada           | Antes   | Escribe sobre el propio registro                                                |
| R-002 | Notifica al sistema solicitante (fqcnFirmaNotifier) que la firma fue rechazada                                                       | TareaFirma  | Marcar como rechazada           | Después | Efecto colateral hacia el sistema externo que solicitó la firma                 |
| R-003 | Cambia el estado a FIRMADO y fija la fecha de resolución al momento actual                                                           | TareaFirma  | Marcar como firmada             | Antes   | Escribe sobre el propio registro                                                |
| R-004 | Notifica al sistema solicitante que la firma se ha completado                                                                        | TareaFirma  | Marcar como firmada             | Después | Efecto colateral hacia el sistema externo que solicitó la firma                 |
| R-005 | Lanza el cliente de AutoFirma en el navegador del usuario para que firme todos los documentos de la tarea con su certificado digital | TareaFirma  | Firmar documentos con AutoFirma | Después | Al volver, guarda cada documento firmado en su DocumentoFirma asociado          |
