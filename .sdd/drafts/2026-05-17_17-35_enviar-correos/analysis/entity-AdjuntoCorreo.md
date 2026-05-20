# Entidad: AdjuntoCorreo

Fichero que acompaña a una TareaCorreo. El subsistema guarda una copia propia del fichero, de modo que aunque el original cambie o se borre, lo adjuntado al correo permanece tal cual se envió.

## Modelo de datos

| Campo          | Tipo de dato | Relación                | Notas                                                                                |
|----------------|--------------|-------------------------|--------------------------------------------------------------------------------------|
| nombreFichero  | texto        | —                       | Nombre con el que se muestra el adjunto al descargar.                                |
| contenido      | fichero      | —                       | Copia del contenido del fichero adjunto, almacenada por el propio subsistema.        |
| tareaCorreo    | relación     | → TareaCorreo (padre)   | Cada adjunto pertenece a una única TareaCorreo. La cascada al borrar va en el padre. |

## Validaciones (V-AdjuntoCorreo-NNN)

| ID                    | Campo(s)       | Descripción                                          | Condición   | Mensaje al usuario                                                              | Origen EARS |
|-----------------------|----------------|------------------------------------------------------|-------------|---------------------------------------------------------------------------------|-------------|
| V-AdjuntoCorreo-001   | nombreFichero  | El nombre del fichero adjunto es obligatorio.        | Al crear.   | "El nombre del fichero adjunto es obligatorio."                                 | —           |
| V-AdjuntoCorreo-002   | contenido      | El contenido del fichero adjunto es obligatorio.     | Al crear.   | "El contenido del fichero adjunto es obligatorio."                              | —           |
| V-AdjuntoCorreo-003   | —              | Un AdjuntoCorreo no puede modificarse una vez creado.| Al modificar.| "Los correos enviados son un registro histórico y sus adjuntos no pueden modificarse." | E-UN-004    |
| V-AdjuntoCorreo-004   | —              | Un AdjuntoCorreo no puede borrarse de forma directa. | Al borrar.  | "Los correos enviados son un registro histórico y sus adjuntos no pueden borrarse." | E-UN-005    |

## Acciones

| Operación              | Cuándo se permite                                                                  | Validaciones que aplican                       | Reglas que dispara         |
|------------------------|------------------------------------------------------------------------------------|------------------------------------------------|----------------------------|
| Crear (insert)         | Solo como parte de la creación de la TareaCorreo padre por un Administrador.       | V-AdjuntoCorreo-001, V-AdjuntoCorreo-002       | R-AdjuntoCorreo-001        |
| Modificar (update)     | Nunca — los adjuntos heredan la inmutabilidad del correo enviado.                  | V-AdjuntoCorreo-003                            | —                          |
| Borrar (remove)        | Nunca — no se borran de forma directa; se borran en cascada con su TareaCorreo.    | V-AdjuntoCorreo-004                            | —                          |

## Reglas de negocio (R-AdjuntoCorreo-NNN)

| ID                    | Descripción                                                                                                                          | Entidad        | Método          | Momento | Más información                                                                                                            | Origen EARS |
|-----------------------|--------------------------------------------------------------------------------------------------------------------------------------|----------------|-----------------|---------|----------------------------------------------------------------------------------------------------------------------------|-------------|
| R-AdjuntoCorreo-001   | Al crear un AdjuntoCorreo el sistema almacena una copia propia del contenido del fichero original, desacoplada de la fuente externa. | AdjuntoCorreo  | Crear (insert)  | Antes   | Garantiza que aunque el fichero original cambie o se borre tras la creación de la TareaCorreo, lo adjuntado permanece intacto. | E-EV-001    |
