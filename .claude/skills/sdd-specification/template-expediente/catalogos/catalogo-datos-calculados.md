# Catálogo de datos que rellena el sistema

Catálogo de referencia para identificar los datos (`CC-`) que **nunca aporta el usuario** sino que los pone el sistema. Sirve para dos cosas: no olvidar ninguno, y —sobre todo— **sacarlos de la lista de datos que el usuario envía** en cada acción. Un dato que el sistema calcula pero que además figura en esa lista queda a merced de quien use el expediente: podría enviar el valor que quisiera.

Recórrelo **dato a dato**, cruzando lo que aparece en las pantallas y en los documentos con lo que el usuario realmente teclea.

Es una ayuda **no exhaustiva**: si el negocio necesita un dato calculado que no figura aquí, decláralo igualmente.

| Comprobación | De dónde se deduce | Ejemplo de hueco |
|---|---|---|
| Un dato que aparece en una pantalla o en un documento y que **nadie envía en ninguna acción** | Cruce entre «Qué ve el usuario» y «Datos que el usuario envía» | «El documento imprime el curso académico y el usuario nunca lo escribe» |
| Datos del expediente tomados de quien lo crea (nombre, apellidos, documento de identidad, centro) | `## Al crear el expediente` | «Se declara que la solicitud lleva el DNI del interesado y no se dice de dónde sale» |
| Fechas y momentos en que ocurre algo | Acciones que marcan un hito | «Se muestra la fecha de resolución y nadie la introduce» |
| El curso académico, el año o el periodo en vigor | Datos de contexto que se imprimen | «El documento lleva el curso y no se dice que lo pone el sistema» |
| Los documentos generados y sus justificantes de registro | `documentos.md` | «Los documentos figuran como si el usuario los adjuntara» |
| Un identificador o número del expediente visible para el usuario | Pantallas de consulta | «Se cita el número de expediente y no se declara de dónde sale» |
| Un valor copiado de otra ficha en el momento de la acción, para que quede congelado | Reglas de negocio de copia | «Se guarda el cargo de quien firmó y no se declara» |
| Un dato que se deriva de otros cada vez que se consulta, sin guardarse | Datos que son suma, resta o resumen de otros | «Se muestra el total de días y nadie lo teclea» |
| Un dato que el sistema calcula pero que **alguien concreto** puede corregir a mano | Excepciones declaradas por el negocio | «Se dice que el tramitador puede ajustarlo y no se declara quién ni cuándo» |
