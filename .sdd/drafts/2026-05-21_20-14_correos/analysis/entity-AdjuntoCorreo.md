# Entidad: AdjuntoCorreo

Copia inmutable de un fichero adjunto asociada a un Correo, tomada en el momento de su creación. Pertenece a un único Correo y nunca cambia después de crearse.

## Modelo de datos

| Campo | Tipo de dato | Relación | Origen del valor | Notas |
|-------|--------------|----------|------------------|-------|
| nombreFichero | texto | — | `cliente` | Obligatorio. Nombre del fichero adjunto aportado en el alta del Correo. Inmutable una vez creado. |
| contenido | fichero | — | `cliente` | Obligatorio. Copia del contenido del fichero adjunto tomada en el momento de la creación. Inmutable una vez creado. |
| correo | relación | → Correo (padre) | `servidor` | Obligatorio. El sistema lo fija al vincular el adjunto a su Correo durante el alta. No lo aporta el cliente. |

## Validaciones (V-AdjuntoCorreo-NNN)

| ID | Campo(s) | Descripción | Condición | Mensaje al usuario | Origen EARS |
|----|----------|-------------|-----------|---------------------|-------------|
| V-AdjuntoCorreo-001 | nombreFichero | El nombre del fichero adjunto es obligatorio. | nombreFichero vacío al crear | "El nombre del fichero adjunto es obligatorio." | E-UB-006, E-OP-002 |
| V-AdjuntoCorreo-002 | contenido | El contenido del fichero adjunto es obligatorio. | contenido vacío al crear | "El contenido del fichero adjunto es obligatorio." | E-UB-006, E-OP-002 |
| V-AdjuntoCorreo-003 | nombreFichero, contenido | Tras crearse, un adjunto no se puede modificar (es una copia inmutable). | intento de cambiar nombreFichero o contenido en un adjunto ya existente | "Un adjunto de correo ya creado no se puede modificar." | E-UB-007, E-UN-005 |

## Acciones

| Operación | Cuándo se permite | Validaciones que aplican | Reglas que dispara |
|-----------|-------------------|--------------------------|--------------------|
| Crear (insert) | Solo como hijo de un Correo, durante el alta de ese Correo | V-AdjuntoCorreo-001, V-AdjuntoCorreo-002 | R-AdjuntoCorreo-001 |
| Modificar (update) | Nunca — el adjunto es una copia inmutable del fichero | V-AdjuntoCorreo-003 | — |
| Borrar (remove) | Solo al borrarse su Correo padre (borrado en cascada desde el padre) | — | — |

## Reglas de negocio (R-AdjuntoCorreo-NNN)

| ID | Descripción | Entidad | Método | Momento | Más información | Origen EARS |
|----|-------------|---------|--------|---------|-----------------|-------------|
| R-AdjuntoCorreo-001 | Al crear el Correo con adjuntos, el sistema guarda cada adjunto como copia inmutable y lo vincula a su Correo padre. | AdjuntoCorreo | Crear | Antes | Asigna la relación correo al padre (campo `servidor`). | E-UB-007, E-OP-002 |
