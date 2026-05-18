# Entidad: AdjuntoCorreo

Representa un fichero adjunto a una tarea de correo. Es una copia propia e inmutable del fichero original, cuya vida está ligada a la TareaCorreo a la que pertenece.

## Modelo de datos

| Campo | Tipo de dato | Relación | Notas |
|-------|--------------|----------|-------|
| tareaCorreo | relación | → TareaCorreo (padre) | TareaCorreo a la que pertenece el adjunto. |
| nombreFichero | texto | — | Nombre del fichero tal como debe aparecer en el correo. |
| contenidoFichero | fichero | — | Contenido binario del fichero adjuntado. Copia propia inmutable. |

## Validaciones (V-AdjuntoCorreo-NNN)

| ID | Campo(s) | Descripción | Condición | Mensaje al usuario |
|----|----------|-------------|-----------|--------------------|
| V-AdjuntoCorreo-001 | tareaCorreo | El adjunto debe estar asociado a una tarea de correo existente. | tareaCorreo vacío o inexistente | El adjunto debe pertenecer a una tarea de correo. |
| V-AdjuntoCorreo-002 | nombreFichero | El nombre del fichero es obligatorio. | nombreFichero vacío | El nombre del fichero adjunto es obligatorio. |
| V-AdjuntoCorreo-003 | contenidoFichero | El contenido del fichero es obligatorio. | contenidoFichero vacío | El contenido del fichero adjunto '{valor}' es obligatorio. |

## Acciones

| Operación | Cuándo se permite | Validaciones que aplican | Reglas que dispara |
|-----------|-------------------|--------------------------|--------------------|
| Crear (insert) | Solo como parte de la creación de la TareaCorreo a la que pertenece. | V-AdjuntoCorreo-001, V-AdjuntoCorreo-002, V-AdjuntoCorreo-003 | R-AdjuntoCorreo-001 |
| Modificar (update) | Nunca — el adjunto es una copia propia inmutable. | — | — |
| Borrar (remove) | Nunca — los adjuntos deben conservarse junto con su tarea de correo como traza permanente del envío. | — | — |
| Consultar | Cada rol según la visibilidad que tenga sobre la TareaCorreo a la que pertenece. Solo lectura. | — | — |

## Reglas de negocio (R-AdjuntoCorreo-NNN)

| ID | Descripción | Entidad | Método | Momento | Más información |
|----|-------------|---------|--------|---------|-----------------|
| R-AdjuntoCorreo-001 | Al crear, se almacena una copia propia del fichero original, desacoplada de cualquier fichero externo, para que el adjunto no cambie aunque cambie la fuente. | AdjuntoCorreo | Crear | Antes | Garantiza que el contenido del correo enviado quede fielmente conservado. |