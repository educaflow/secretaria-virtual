# Catálogo de tipos de campo calculado

Catálogo de referencia para identificar, al rellenar los **Campos calculados** (`CC-`) de un `entity-*.md`, qué campos no los aporta el usuario sino que los dicta el servidor. Recorre la tabla y comprueba, para la entidad, cuáles aplican. Las columnas de `momento` y `sobreescribible` orientan esos atributos; el `cálculo` se redacta siempre en lenguaje de negocio.

Es una ayuda **no exhaustiva**: si el negocio necesita un campo calculado que no figura aquí, decláralo igualmente.

> Pista: un campo que no aparece en los «Campos editables» de **ninguna** transición pero que sale en vistas, documentos o escenarios es candidato a campo calculado (o a olvido). Y al revés: un `CC-` con `sobreescribible: nunca` no puede aparecer en los «Campos editables» de ninguna transición.

> Lo que **no** hay que declarar porque ya lo da la plataforma a todo expediente: el número de expediente, el estado y su fecha, el historial de estados, el centro, la persona solicitante e interesada y el DNI de firma (se **precargan** en la creación — ver «Creación del expediente» en `estados.md`).

| Tipo de campo calculado | momento típico | sobreescribible típico | Ejemplo de cálculo |
|---|---|---|---|
| Fecha de un hito de la tramitación (presentación, resolución, pago…) | escritura | nunca | "La fecha en que se presentó la solicitud" |
| Copia (snapshot) de datos del interesado o de un catálogo en el momento del alta | escritura | nunca | "Al crear se copian el ciclo y el curso en que está matriculado el alumno" |
| Total / suma / recuento de los registros hijos | escritura o lectura | nunca | "El total de gastos es la suma de los importes de las líneas" |
| Derivado de otros campos del propio expediente | lectura | nunca | "El nombre completo es nombre + apellidos" |
| El resultado que fija una transición (el sentido de la resolución cuando lo decide un suceso, no una persona) | escritura | nunca | "Si vence el plazo de resolución, la resolución queda como ESTIMADA por silencio" |
| Contador de intentos o vueltas (subsanaciones pedidas) | escritura | nunca | "Número de veces que se ha pedido subsanación" |
| Valor fijado por el sistema que ciertos roles pueden forzar | escritura | [ROL] | "0 por defecto; el administrador puede indicar otro valor" |

Criterio para `momento`:

- `escritura` — el valor debe quedar **persistido** tal y como estaba en ese instante (fechas de hito, snapshots, totales presentados): aunque después cambien los datos de los que salió, el valor guardado no cambia. En un expediente es lo habitual: lo presentado y lo resuelto son fotos de un momento.
- `lectura` — el valor debe reflejar **siempre el estado actual** de los datos de los que deriva: se recalcula al consultar y no se guarda.
