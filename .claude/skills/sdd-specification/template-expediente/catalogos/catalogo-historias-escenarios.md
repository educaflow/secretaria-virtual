# Catálogo de cobertura de historias de usuario y escenarios

Catálogo de referencia para comprobar si a la spec le faltan **historias de usuario (`HU-`) o escenarios (`ESC-`)** desde el punto de vista de las personas que usan el trámite. Complementa a `catalogo-cobertura-estados.md`, que mira el ciclo de vida; este mira a los actores, al trámite como servicio y a lo que ocurre alrededor del expediente.

No inventa funcionalidad: cada comprobación se deduce de lo que la spec **ya declara**. Es una ayuda **no exhaustiva**.

## Cobertura de historias de usuario

| Comprobación | De dónde se deduce | Ejemplo de hueco |
|---|---|---|
| Cada perfil declarado tiene al menos una historia desde su punto de vista | Tabla «Actores y perfiles» del índice | «El perfil que entrega el material aparece en Seguridad pero ninguna historia lo protagoniza» |
| Cada rol con acceso en Seguridad tiene su historia, aunque solo consulte | Apartado «Seguridad» | «Las familias pueden consultar el expediente y no hay ninguna historia de consulta» |
| El inicio del trámite (elegirlo en la lista y leer su ayuda) tiene su historia | Apartado «El trámite» | «Nadie recorre la ayuda del trámite antes de crear el expediente» |
| Quien tramita tiene una historia de resolución por cada sentido posible | «A qué estado lleva» de la acción que resuelve | «Solo hay historia para conceder, no para denegar ni para pedir una corrección» |
| Quien inicia el expediente tiene una historia de seguimiento (ver en qué punto está lo suyo) | Pantallas de solo consulta | «El interesado presenta y nunca vuelve a mirar su expediente» |

## Cobertura de escenarios dentro de cada historia

| Comprobación | De dónde se deduce | Ejemplo de hueco |
|---|---|---|
| Camino feliz: la historia tiene un escenario donde todo sale bien de principio a fin | La propia `HU-` | «Solo hay escenarios de error para esa historia» |
| Un escenario de error por cada comprobación relevante, con el mensaje literal | Las `VAL-` de `estados.md` | «Se exige que el dato tenga cierto formato y ningún escenario lo introduce mal» |
| El expediente se abandona a medias y se retoma después | Estados intermedios con perfil | «Nadie sale del expediente sin terminar y vuelve a entrar en él» |
| El expediente se recorre de punta a punta en un solo escenario | La tabla de transiciones | «Hay escenarios sueltos por tramo pero ninguno hace el recorrido completo» |
| Los datos que el sistema rellena solo se comprueban con su valor esperado | `## Datos que rellena el sistema` | «Nadie comprueba que el curso académico aparece ya relleno al crear el expediente» |
| Un dato que el usuario no debería poder cambiar se comprueba inalterable | «Qué solo puede consultar» de cada pantalla | «Nada prueba que el interesado no puede cambiar lo que ya presentó» |
| Alcance multicentro: un usuario de un centro no ve los expedientes del otro | «Seguridad» (alcance por centro) | «Nada prueba que el tramitador de un centro no ve los expedientes del otro» |
| Lo que queda guardado se puede volver a consultar después | Pantallas de consulta | «Se presenta el expediente pero ningún paso comprueba que aparece luego en la lista» |
