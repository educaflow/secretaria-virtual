# Catálogo de cobertura del ciclo de vida

Catálogo de referencia para comprobar si a la spec le faltan **escenarios que recorran el ciclo de vida**: estados a los que ningún escenario llega, acciones que ningún escenario lanza, ramas que nadie ejercita. No inventa funcionalidad: cada comprobación se deduce de lo que `estados.md` y los `pantallas-<fase>.md` **ya declaran**.

Es una ayuda **no exhaustiva**: propón también escenarios que no respondan a ninguna fila si el ciclo de vida declarado los sugiere.

> Toda candidata de este catálogo es una **historia de usuario o un escenario** (`HU-`/`ESC-`), redactado con sus pasos numerados, sus valores concretos y cuentas reales de los datos de demo.

## Estados

| Comprobación | De dónde se deduce | Ejemplo de hueco |
|---|---|---|
| A cada estado declarado llega al menos un escenario | Las secciones `### Estado …` de `estados.md` | «Ningún escenario deja el expediente en el estado que cierra el trámite tras denegarse» |
| El estado en el que nace el expediente lo recorre un escenario desde la creación | `## Al crear el expediente` | «Ningún escenario empieza creando el expediente y comprueba qué datos aparecen ya rellenos» |
| Cada estado que cierra el expediente se comprueba como cerrado (sin acciones disponibles) | Columna «Cierra el expediente» | «Se llega al estado final pero nadie comprueba que ya no queda ningún botón de acción» |
| Un estado al que se puede volver hacia atrás se recorre también en la segunda visita | Transiciones cuyo destino es un estado anterior | «Nadie comprueba qué ve el interesado cuando vuelve a rellenar los datos tras pedírsele una corrección» |

## Acciones y transiciones

| Comprobación | De dónde se deduce | Ejemplo de hueco |
|---|---|---|
| Cada acción declarada la lanza al menos un escenario | «Qué acciones puede lanzar» de cada estado | «La acción de volver atrás no la ejercita ningún escenario» |
| Cada fila de la tabla de transiciones tiene su escenario | `## Tabla de transiciones` | «La transición que cruza de una fase a otra no aparece en ningún escenario» |
| Cada **rama** de una acción que ramifica según un dato tiene su propio escenario | Las ramas «si <dato> vale <VALOR> → …» | «La acción resuelve de tres formas distintas y solo se prueba una» |
| La acción que **no cambia de estado** se comprueba como tal | Transiciones con destino igual al origen | «Nadie comprueba que tras esa acción el expediente sigue donde estaba» |
| El **borrado** del expediente tiene su escenario, y también su ausencia donde no está permitido | «Desde qué estado se puede borrar» | «Se declara que solo se borra desde el primer estado, pero nadie comprueba que después ya no se puede» |
| Cada comprobación declarada tiene un escenario que la hace fallar, con su mensaje literal | Las `VAL-` de cada pareja (estado, acción) | «Se exige un motivo al denegar y ningún escenario intenta denegar sin motivo» |

## Perfiles y pantallas

| Comprobación | De dónde se deduce | Ejemplo de hueco |
|---|---|---|
| Cada perfil declarado protagoniza al menos un escenario | Tabla «Actores y perfiles» del índice | «Hay un perfil que entrega el material y ningún escenario lo tiene como protagonista» |
| La pantalla de **solo consulta para el resto de perfiles** se comprueba en al menos un estado | La pantalla obligatoria de cada estado | «Nadie comprueba que quien no tiene el turno abre el expediente en solo lectura y solo puede salir» |
| Quien no tiene el turno **no** ve el botón de la acción | Botones de cada pantalla | «Nada prueba que el interesado no puede resolver su propio expediente» |
| Un documento que se muestra en pantalla se comprueba visible en algún escenario | «Documentos que se le muestran» | «Se genera un documento y ningún escenario comprueba que el interesado lo ve» |
| Alcance por centro: quien es de otro centro no ve el expediente | Apartado «Seguridad» del índice | «Nada prueba que el tramitador de un centro no ve los expedientes del otro» |

## Efectos

| Comprobación | De dónde se deduce | Ejemplo de hueco |
|---|---|---|
| Cada documento generado se comprueba en el escenario de la acción que lo genera | `documentos.md` + «Qué produce la acción» | «Se genera un documento al continuar y ningún escenario comprueba que aparece» |
| Cada registro de entrada o de salida se comprueba en su escenario | «Registros de entrada y salida» del índice | «Se registra la entrada al presentar y nadie comprueba que el interesado recibe su justificante» |
| Cada firma (en el equipo del interesado o del centro) se recorre en un escenario | «Quién lo firma y dónde» de `documentos.md` | «Nadie recorre el paso de firmar en el equipo del interesado» |
| Cada aviso enviado se comprueba en un escenario | «Avisos que se envían» del índice | «Se avisa al interesado de la resolución y ningún escenario lo comprueba» |
| Los datos que se limpian al repetir un intento se comprueban limpios | «Qué produce la acción» | «Se vuelve atrás tras una corrección y nadie comprueba que el motivo anterior ya no se muestra» |
