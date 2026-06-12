# Entidad: DocumentoFirma

Cada uno de los documentos que componen una tarea de firma. Mantiene el documento original que se firma y, si la firma se completa, el documento firmado resultante.

## Modelo de datos

| Campo              | Tipo de dato | Relación                | Origen del valor | Notas                                                                          |
|--------------------|--------------|-------------------------|------------------|--------------------------------------------------------------------------------|
| tareaFirma         | relación     | → TareaFirma (padre)    | servidor         | tarea de firma a la que pertenece este documento                               |
| documentoOriginal  | relación     | → Fichero (PDF)         | servidor         | documento que el firmante debe firmar; lo aporta el sistema solicitante        |
| documentoFirmado   | relación     | → Fichero (PDF)         | servidor         | resultado de aplicar la firma sobre el original; vacío hasta pasar a FIRMADO   |

## Validaciones (V-DocumentoFirma-NNN)

| ID    | Campo(s)           | Descripción                                       | Condición | Mensaje al usuario                                       | Origen spec |
|-------|--------------------|---------------------------------------------------|-----------|----------------------------------------------------------|-------------|
| V-DocumentoFirma-001 | tareaFirma         | La tarea de firma asociada debe estar rellena     | Siempre   | "La tarea de firma asociada al documento es obligatoria."| —           |
| V-DocumentoFirma-002 | documentoOriginal  | El documento original debe estar relleno          | Siempre   | "El documento original a firmar es obligatorio."         | —           |

## Acciones

| Operación          | Cuándo se permite                                                                                  | Validaciones que aplican  | Reglas que dispara  |
|--------------------|----------------------------------------------------------------------------------------------------|---------------------------|---------------------|
| Crear (insert)     | Solo desde el sistema que crea la tarea de firma                                                   | V-DocumentoFirma-001, V-DocumentoFirma-002              | R-DocumentoFirma-001 |
| Modificar (update) | Nunca de forma directa; el campo documentoFirmado se rellena al firmar (regla R-TareaFirma-005 de TareaFirma) | —                         | —                   |
| Borrar (remove)    | Nunca — el documento es histórico                                                                  | —                         | —                   |

## Reglas de negocio (R-DocumentoFirma-NNN)

| ID                   | Descripción                                                                                | Entidad        | Método | Momento | Más información                                                              | Origen spec |
|----------------------|--------------------------------------------------------------------------------------------|----------------|--------|---------|------------------------------------------------------------------------------|-------------|
| R-DocumentoFirma-001 | Al crear el documento, asigna la tarea de firma padre y el documento original recibidos del sistema solicitante | DocumentoFirma | insert | Antes   | El documento solo se crea junto con la tarea; el usuario nunca dicta estos valores | —           |

El campo documentoFirmado se rellena desde la regla R-TareaFirma-005 de TareaFirma.
