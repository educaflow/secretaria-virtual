# Entidad: TareaFirma

Tarea de firma asignada a un usuario firmante. Agrupa uno o varios documentos que deben firmarse juntos.

## Modelo de datos

| Campo                | Tipo de dato | Relación                                | Origen del valor | Notas                                                                  |
|----------------------|--------------|-----------------------------------------|------------------|------------------------------------------------------------------------|
| firmante             | relación     | → Usuario                               | servidor         | usuario que debe firmar; lo aporta el sistema solicitante              |
| documentosFirma      | lista        | → DocumentoFirma (uno a varios)         | servidor         | documentos asociados a la tarea; los crea el sistema solicitante       |
| fechaSolicitud       | fecha-hora   | —                                       | servidor         | momento en que se crea la tarea                                        |
| fechaResolucion      | fecha-hora   | —                                       | servidor         | momento en que se firma o se rechaza                                   |
| firmaRapida          | booleano     | —                                       | servidor         | si "sí", se firma automáticamente con el certificado del usuario       |
| estadoTareaFirma     | enum         | valores: PENDIENTE, FIRMADO, RECHAZADO  | servidor         | estado actual de la tarea                                              |
| motivoFirma          | texto        | —                                       | servidor         | texto que indica por qué hay que firmar; lo aporta el sistema solicitante |
| motivoRechazo        | texto largo  | —                                       | cliente          | texto que el firmante introduce al rechazar                            |
| fqcnFirmaNotifier    | texto        | —                                       | servidor         | identificador del sistema externo que ha solicitado la firma           |
| fqcnCallBackData     | texto        | —                                       | servidor         | identificador del tipo de datos de retorno                             |
| callBackData         | texto largo  | —                                       | servidor         | datos que el sistema solicitante envía y recibe al finalizar           |
| x, y, width, height  | decimal      | —                                       | servidor         | geometría (posición y tamaño) de la firma sobre el documento PDF       |
| page                 | entero       | —                                       | servidor         | página del documento donde se estampa la firma                         |

## Validaciones (V-TareaFirma-NNN)

| ID    | Campo(s)                            | Descripción                                                  | Condición                | Mensaje al usuario                                                  | Origen spec |
|-------|-------------------------------------|--------------------------------------------------------------|--------------------------|---------------------------------------------------------------------|-------------|
| V-TareaFirma-001 | firmante                            | El firmante debe estar relleno                               | Siempre                  | "El firmante es obligatorio."                                       | —           |
| V-TareaFirma-002 | fechaSolicitud                      | La fecha de la solicitud debe estar rellena                  | Siempre                  | "La fecha de la solicitud es obligatoria."                          | —           |
| V-TareaFirma-003 | estadoTareaFirma                    | El estado debe estar relleno                                 | Siempre                  | "El estado de la tarea es obligatorio."                             | —           |
| V-TareaFirma-004 | motivoFirma                         | El motivo de la firma debe estar relleno                     | Siempre                  | "El motivo de la firma es obligatorio."                             | —           |
| V-TareaFirma-005 | x, y, width, height, page           | La geometría (posición, tamaño y página) debe estar rellena  | Siempre                  | "La posición y el tamaño de la firma en el documento son obligatorios." | —           |
| V-TareaFirma-006 | motivoRechazo                       | El motivo de rechazo debe estar relleno al rechazar          | Al rechazar la firma     | "El motivo de rechazo es obligatorio para rechazar la firma."       | —           |
| V-TareaFirma-007 | documentosFirma[].documentoFirmado  | Todos los documentos firmados deben ser válidos              | Al finalizar la firma    | "Alguno de los documentos firmados no es válido."                   | —           |

## Acciones

| Operación                       | Cuándo se permite                                                       | Validaciones que aplican           | Reglas que dispara |
|---------------------------------|-------------------------------------------------------------------------|------------------------------------|--------------------|
| Crear (insert)                  | Solo desde otro sistema que solicita la firma (sin alta manual)         | V-TareaFirma-001, V-TareaFirma-002, V-TareaFirma-003, V-TareaFirma-004, V-TareaFirma-005  | R-TareaFirma-006   |
| Modificar (update)              | Nunca de forma directa; solo a través de las operaciones de negocio     | —                                  | —                  |
| Borrar (remove)                 | Nunca — la tarea es un registro histórico                               | —                                  | —                  |
| Marcar como rechazada           | Solo si estado = PENDIENTE                                              | V-TareaFirma-006                              | R-TareaFirma-001, R-TareaFirma-002       |
| Marcar como firmada             | Solo si estado = PENDIENTE y la firma se ha completado                  | V-TareaFirma-007                              | R-TareaFirma-003, R-TareaFirma-004       |
| Firmar documentos con AutoFirma | Solo si estado = PENDIENTE                                              | —                                  | R-TareaFirma-005              |

## Reglas de negocio (R-TareaFirma-NNN)

| ID    | Descripción                                                                                                                          | Entidad     | Método                          | Momento | Más información                                                                | Origen spec |
|-------|--------------------------------------------------------------------------------------------------------------------------------------|-------------|---------------------------------|---------|--------------------------------------------------------------------------------|-------------|
| R-TareaFirma-001 | Cambia el estado a RECHAZADO, guarda el motivo de rechazo y fija la fecha de resolución al momento actual                            | TareaFirma  | Marcar como rechazada           | Antes   | Escribe sobre el propio registro                                                | —           |
| R-TareaFirma-002 | Notifica al sistema solicitante (fqcnFirmaNotifier) que la firma fue rechazada                                                       | TareaFirma  | Marcar como rechazada           | Después | Efecto colateral hacia el sistema externo que solicitó la firma                 | —           |
| R-TareaFirma-003 | Cambia el estado a FIRMADO y fija la fecha de resolución al momento actual                                                           | TareaFirma  | Marcar como firmada             | Antes   | Escribe sobre el propio registro                                                | —           |
| R-TareaFirma-004 | Notifica al sistema solicitante que la firma se ha completado                                                                        | TareaFirma  | Marcar como firmada             | Después | Efecto colateral hacia el sistema externo que solicitó la firma                 | —           |
| R-TareaFirma-005 | Lanza el cliente de AutoFirma en el navegador del usuario para que firme todos los documentos de la tarea con su certificado digital | TareaFirma  | Firmar documentos con AutoFirma | Después | Al volver, guarda cada documento firmado en su DocumentoFirma asociado          | —           |
| R-TareaFirma-006 | Al crear la tarea, asigna los datos de la solicitud recibidos del sistema solicitante (firmante, motivo, geometría, datos de retorno y documentos) e inicializa el estado a PENDIENTE y la fecha de solicitud al momento actual | TareaFirma  | insert                          | Antes   | La tarea solo la crean otros sistemas; el usuario nunca dicta estos valores     | —           |
