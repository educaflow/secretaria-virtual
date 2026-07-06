# Catálogo de tipos de campo calculado

Catálogo de referencia para identificar, al rellenar los **Campos calculados** (`CC-`) de un `entity-*.md`, qué campos de la entidad no los aporta el usuario sino que los dicta el servidor. Recorre la tabla y comprueba, para la entidad, cuáles aplican. Las columnas de `momento` y `sobreescribible` orientan esos atributos; el `cálculo` se redacta siempre en lenguaje de negocio.

Es una ayuda **no exhaustiva**: si el negocio necesita un campo calculado que no figura aquí, decláralo igualmente.

> Pista: un campo que ningún actor rellena en ninguna línea `Input AllowProperties` pero que aparece en pantallas o escenarios es candidato a campo calculado (o a olvido). Y al revés: un `CC-` con `sobreescribible: nunca` no puede aparecer en ninguna línea `Input AllowProperties`.

| Tipo de campo calculado                                                | momento típico | sobreescribible típico | Ejemplo de cálculo                                                        |
|--------------------------------------------------------------------------|-----------------|--------------------------|------------------------------------------------------------------------------|
| Numeración secuencial por ámbito                                        | escritura       | nunca                    | "Secuencial por centro y año, con formato NNNNN/AAAA"                       |
| Fecha de un evento de negocio (creación, envío, resolución…)            | escritura       | nunca                    | "La fecha y hora en que se creó el correo"                                  |
| Persona/usuario que realizó la acción                                   | escritura       | nunca                    | "El usuario que registró la entrada"                                        |
| Estado inicial del ciclo de vida                                        | escritura       | nunca                    | "Al crearse queda en PENDIENTE"                                             |
| Contador de intentos, usos o reenvíos                                   | escritura       | nunca                    | "Número de veces que se ha intentado el envío, empezando en 1"              |
| Total / suma / recuento de los registros hijos                          | escritura o lectura | nunca                | "Suma de (cantidad × precio unitario) de todas las líneas"                  |
| Derivado de otros campos del propio registro                            | lectura         | nunca                    | "El nombre completo es nombre + apellidos"; "la edad se deriva de la fecha de nacimiento" |
| Copia (snapshot) de datos de otra entidad en el momento del alta        | escritura       | nunca                    | "Al crear se copian el nombre y apellidos actuales del destinatario"        |
| Valor fijado por el sistema que ciertos roles pueden forzar             | escritura       | [ROL]                    | "0 por defecto; el administrador puede indicar un descuento distinto"       |

Criterio para `momento`:

- `escritura` — el valor debe quedar **persistido** tal y como estaba en ese instante (numeraciones, fechas de evento, snapshots, contadores): aunque después cambien los datos de los que salió, el valor guardado no cambia.
- `lectura` — el valor debe reflejar **siempre el estado actual** de los datos de los que deriva (nombre completo, edad, totales informativos): se recalcula cada vez que se consulta y no se guarda.
