# Catálogo de reglas de pantalla

Catálogo de referencia para identificar las **reglas de pantalla** (`RUI-`) que faltan en un `pantallas-<fase>.md`. Recórrelo pantalla a pantalla (cada pareja estado + perfil) y, dentro de cada una, bloque a bloque, dato a dato y botón a botón.

Es una ayuda **no exhaustiva**: si el negocio necesita una regla de pantalla que no figura aquí, decláralo igualmente.

> **Frontera.** Una regla de pantalla **solo cambia lo que el usuario ve o puede editar**. Si impide que la acción se ejecute, es una comprobación (`VAL-`, en `estados.md`). Si produce, guarda o envía algo, es una regla de negocio (`RN-`).
>
> **CRITICAL — una regla de pantalla nunca es una defensa.** Ocultar un dato o ponerlo en solo lectura no impide que alguien lo envíe: lo único que decide qué se guarda es la lista «Datos que el usuario envía al lanzarla» de la acción. Toda regla de pantalla que oculte o bloquee algo importante **MUST** tener detrás su comprobación o su exclusión de esa lista.

## Visibilidad

| Comprobación | De dónde se deduce | Ejemplo de hueco |
|---|---|---|
| Un dato o bloque solo se muestra cuando otro dato tiene cierto valor | Datos que solo aplican en algunos casos | «El número de cuenta solo se muestra cuando se elige cobrar por transferencia» |
| Un bloque solo se muestra cuando el expediente viene de un camino concreto | Transiciones que vuelven a un estado anterior | «El aviso de qué hay que corregir solo se muestra si se volvió atrás» |
| Un documento solo se muestra cuando ya se ha generado | «Documentos que se le muestran» | «Se enseña un recuadro vacío antes de que el documento exista» |
| Un botón solo se muestra a quien tiene el turno | «Botones» de la pantalla | «El interesado ve el botón de resolver su propio expediente» |

## Edición

| Comprobación | De dónde se deduce | Ejemplo de hueco |
|---|---|---|
| Toda la pantalla del resto de perfiles es de solo consulta | La pantalla obligatoria de cada estado | «Quien mira sin tener el turno puede escribir en los datos» |
| Un dato que ya se aportó en un estado anterior se muestra en solo lectura | «Qué solo puede consultar» | «Los datos presentados se vuelven a poder cambiar en la valoración» |
| Un dato se marca visualmente como obligatorio | Las `VAL-` de obligatoriedad de la acción del botón | «Se exige el motivo y el usuario no sabe que es obligatorio hasta que falla» |
| Un dato se marca obligatorio solo cuando otro tiene cierto valor | Las `VAL-` condicionales | «El número de cuenta no se marca obligatorio al elegir el cobro por transferencia» |
| Un dato deja de poder editarse cuando el expediente ya está cerrado | Estados que cierran el expediente | «El expediente cerrado sigue admitiendo cambios en pantalla» |

## Valores por defecto y opciones

| Comprobación | De dónde se deduce | Ejemplo de hueco |
|---|---|---|
| Al abrir el estado por primera vez, un dato aparece propuesto con un valor | `## Datos que rellena el sistema` con momento «al crear» | «El curso en vigor no aparece propuesto y el usuario tiene que buscarlo» |
| Al cambiar un dato, otro se rellena o se limpia en consecuencia | Datos dependientes entre sí | «Se cambia el tipo de ayuda y el importe de la ayuda anterior sigue escrito» |
| Las opciones de un dato se limitan según otro dato del expediente | Datos que apuntan a otra ficha | «Se ofrecen opciones que no aplican a lo elegido antes» |
| Las opciones de un dato se limitan al centro del usuario | «Seguridad» (alcance por centro) | «Se pueden elegir fichas de otro centro» |

## Avisos, ayudas y confirmaciones

| Comprobación | De dónde se deduce | Ejemplo de hueco |
|---|---|---|
| Antes de una acción irreversible se pide confirmación, con su texto | «Pide confirmación antes de ejecutarse» | «Se presenta sin avisar de que ya no se podrá deshacer» |
| Antes de borrar el expediente se pide confirmación | «Desde qué estado se puede borrar» | «Se borra el expediente de un clic» |
| La pantalla muestra un aviso fijo con lo que el usuario necesita saber o tener preparado | «Aviso permanente en pantalla» | «No se advierte de que hace falta un certificado digital instalado» |
| Al abrir un expediente cerrado se informa de que ya no se puede hacer nada | Estados que cierran el expediente | «El usuario busca un botón que no existe» |
| Un dato de formato poco evidente lleva una ayuda que explica cómo escribirlo | Las `VAL-` de formato | «Se exige una lista separada por comas y no se dice en ninguna parte» |

## Presentación de documentos

| Comprobación | De dónde se deduce | Ejemplo de hueco |
|---|---|---|
| Un documento que el usuario debe leer entero se muestra incrustado en la pantalla | «Documentos que se le muestran» | «Solo se ofrece un enlace de descarga para el documento que hay que revisar antes de firmar» |
| Un documento de apoyo se muestra más pequeño o solo como descarga | «Documentos que se le muestran» | «Un anexo secundario ocupa toda la pantalla» |
