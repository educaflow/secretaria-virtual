# Entidad: DocumentoFirma

Cada uno de los documentos que componen una tarea de firma. Mantiene el documento original que se firma y, si la firma se completa, el documento firmado resultante.

## Modelo de datos

| Campo              | Tipo de dato | Relación                | Notas                                                                          |
|--------------------|--------------|-------------------------|--------------------------------------------------------------------------------|
| tareaFirma         | relación     | → TareaFirma (padre)    | tarea de firma a la que pertenece este documento                               |
| documentoOriginal  | relación     | → Fichero (PDF)         | documento que el firmante debe firmar                                          |
| documentoFirmado   | relación     | → Fichero (PDF)         | resultado de aplicar la firma sobre el original; vacío hasta pasar a FIRMADO   |

## Validaciones (V-XXX)

| ID    | Campo(s)           | Descripción                                       | Condición | Mensaje al usuario                                       |
|-------|--------------------|---------------------------------------------------|-----------|----------------------------------------------------------|
| V-008 | tareaFirma         | La tarea de firma asociada debe estar rellena     | Siempre   | "La tarea de firma asociada al documento es obligatoria."|
| V-009 | documentoOriginal  | El documento original debe estar relleno          | Siempre   | "El documento original a firmar es obligatorio."         |

## Acciones

| Operación          | Cuándo se permite                                                                                  | Validaciones que aplican  | Reglas que dispara  |
|--------------------|----------------------------------------------------------------------------------------------------|---------------------------|---------------------|
| Crear (insert)     | Solo desde el sistema que crea la tarea de firma                                                   | V-008, V-009              | —                   |
| Modificar (update) | Nunca de forma directa; el campo documentoFirmado se rellena al firmar (regla R-005 de TareaFirma) | —                         | —                   |
| Borrar (remove)    | Nunca — el documento es histórico                                                                  | —                         | —                   |

## Reglas de negocio (R-XXX)

*(no hay reglas de negocio propias; el campo documentoFirmado se rellena desde la regla R-005 de TareaFirma)*
