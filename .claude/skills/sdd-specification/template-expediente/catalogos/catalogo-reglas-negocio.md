# Catálogo de reglas de negocio de una acción

Catálogo de referencia para identificar lo que el sistema **hace automáticamente** (`RN-`) al ejecutarse una acción, una vez superadas sus comprobaciones. Una regla de negocio **nunca bloquea**: si la acción no debe poder ejecutarse, eso es una comprobación (`VAL-`). Si solo cambia lo que el usuario ve en pantalla, es una regla de pantalla (`RUI-`).

Recórrelo **acción a acción** de `estados.md` y, dentro de cada una, en el **orden** en que las cosas deben ocurrir: el orden de la lista es normativo.

Es una ayuda **no exhaustiva**: si el negocio necesita un efecto que no figura aquí, decláralo igualmente.

## Documentos

| Comprobación | De dónde se deduce | Ejemplo de hueco |
|---|---|---|
| La acción genera un documento con los datos del expediente | `documentos.md` + «Documentos que se le muestran» de la pantalla siguiente | «La pantalla siguiente enseña un documento que ninguna acción genera» |
| El documento generado se firma en el servidor: con la firma institucional del centro (la clave del cargo que firma) o con el certificado custodiado de una persona concreta | «Quién lo firma y dónde» | «El documento sale sin firmar y el interesado no puede acreditarlo» |
| El documento generado se guarda en el expediente para poder consultarlo después | «Documentos que se le muestran» de estados posteriores | «El documento se genera y no queda guardado en ninguna parte» |
| El documento se vuelve a generar cuando el interesado corrige y repite | Transiciones que vuelven a un estado anterior | «Tras corregir, se conserva el documento del intento anterior» |

## Registros de entrada y de salida

| Comprobación | De dónde se deduce | Ejemplo de hueco |
|---|---|---|
| La acción deja constancia oficial de la **entrada** de la documentación que aporta el interesado | «Registros de entrada» del índice | «El interesado presenta y no queda constancia registral» |
| La acción deja constancia oficial de la **salida** del documento que el centro emite | «Registros de salida» del índice | «Se resuelve y no se registra la salida de la resolución» |
| El justificante del registro se guarda y se le muestra al interesado | Pantallas de solo consulta de estados posteriores | «Se registra la entrada y el interesado nunca ve su justificante» |
| Los documentos que el interesado aportó acompañan al registro como anexos | «Datos que el usuario envía» que sean ficheros | «El justificante aportado no se incorpora al registro» |

## Avisos

| Comprobación | De dónde se deduce | Ejemplo de hueco |
|---|---|---|
| Se avisa a quien inició el expediente de que ya hay resolución | «Avisos que se envían» del índice | «El interesado tiene que entrar a mirar para enterarse» |
| Se avisa a quien tiene el turno de que le ha llegado algo por tramitar | Estados con perfil que esperan acción | «Nadie sabe que hay expedientes esperando valoración» |
| El aviso lleva la información mínima para saber de qué va sin abrir el expediente | «Avisos que se envían» | «El aviso no dice a qué expediente se refiere» |

## Datos del propio expediente

| Comprobación | De dónde se deduce | Ejemplo de hueco |
|---|---|---|
| La acción anota la fecha o el momento en que ocurrió algo | `## Datos que rellena el sistema` | «No queda registrado cuándo se resolvió» |
| La acción anota quién hizo qué | Perfiles de cada estado | «No queda quién resolvió el expediente» |
| La acción **limpia** los datos de un intento anterior que ya no aplican | Transiciones que vuelven atrás | «El interesado vuelve a presentar y sigue viéndose el motivo de la corrección anterior» |
| La acción copia un dato de otra ficha para que quede congelado en el expediente | Datos que pueden cambiar fuera del expediente | «El documento generado años después mostrará el cargo actual, no el de entonces» |

## Efectos fuera del expediente

| Comprobación | De dónde se deduce | Ejemplo de hueco |
|---|---|---|
| La acción da de alta, modifica o cierra algo en otro sitio del centro | Apartado «Objetivo» y dependencias del trámite | «Se concede el préstamo y el material sigue figurando como disponible» |
| La acción libera o devuelve algo que se había reservado antes | Acciones que revierten a otras | «Se deniega tras haber reservado y la reserva queda viva» |
| La acción deja de tener efecto si el expediente se borra | «Desde qué estado se puede borrar» | «Se borra el expediente y queda una reserva huérfana» |
