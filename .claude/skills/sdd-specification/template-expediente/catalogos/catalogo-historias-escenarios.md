# Catálogo de cobertura de historias de usuario y escenarios

Catálogo de referencia para comprobar si a la spec le **faltan historias de usuario (`HU-`) o escenarios (`ESC-`)**. No inventa funcionalidad nueva: cada comprobación se deduce de lo que la spec **ya declara** (perfiles, estados, transiciones, documentos, firmas, validaciones) y detecta lo declarado que ningún escenario ejercita.

Es una ayuda **no exhaustiva**: se pueden proponer historias o escenarios que no respondan a ninguna fila si la spec los sugiere.

## Cobertura de historias de usuario

| Comprobación | De dónde se deduce | Ejemplo de hueco |
|---|---|---|
| Cada **perfil** de la tabla del índice protagoniza al menos una historia | Actores y perfiles | "El RESPONSABLE resuelve pero ninguna HU lo protagoniza" |
| El **interesado que solo consulta** (mientras otro tiene el turno) tiene su historia de consulta | Vistas genéricas | "Nada cuenta que el alumno puede ver su expediente mientras se resuelve" |
| Si el trámite se inicia **de oficio**, hay una historia desde el punto de vista del centro y otra desde el interesado que alega | El trámite (quién inicia) | "Solo hay historias del director; nada del alumno en la audiencia" |
| Cada **documento** aparece en alguna historia (se genera, se firma, se recibe) | Tabla Documentos | "Nadie llega a ver la resolución emitida" |

## Cobertura de escenarios

| Comprobación | De dónde se deduce | Ejemplo de hueco |
|---|---|---|
| **Cada transición** (incluidas todas sus **ramas**) la recorre al menos un escenario | Fichas TR- de estados.md | "La rama SUBSANAR_DATOS de RESOLVER no la ejercita ningún ESC" |
| Camino feliz completo: un escenario recorre el expediente del inicio a un estado cerrado de éxito | La máquina | "Ningún ESC llega a ACEPTADO" |
| El camino de **rechazo** también se recorre | La máquina | "Ningún ESC termina en RECHAZADO" |
| El **bucle de subsanación** se recorre entero: pedir, corregir, re-presentar y resolver | La máquina | "Se pide subsanación pero ningún ESC re-presenta" |
| Un escenario de **error por cada validación relevante**, con su mensaje literal | VAL-TR- / RES- | "VAL-TR-003-002 (justificante obligatorio) no tiene escenario que muestre su error" |
| Las transiciones **automáticas** tienen un escenario que llega a su suceso (la última firma del portafirmas, el vencimiento del plazo) | Fichas TR- automáticas | "Nada prueba que el expediente avanza cuando el director firma" |
| Cada **firma** se ejercita: quién firma, cómo, y qué versión del documento queda guardada | Fichas FIR- | "Nadie firma con AutoFirma en ningún ESC" |
| El **borrado** del expediente en el estado que lo permite (y que no se puede en los demás) | Tabla de estados | "Nada prueba el borrado en ENTRADA_DATOS" |
| **Ámbito y turnos**: quien no tiene el turno ve pero no actúa; un usuario de otro centro (o sin perfil) no ve el expediente | Perfiles y ámbitos | "Nada prueba que el secretario de Batoi no ve los expedientes de Mislata" |
| El **cambio de turno** entre perfiles se hace con cambio de sesión explícito y apertura desde la bandeja | Reglas de los escenarios | "El ESC pasa del alumno al secretario sin cerrar sesión" |
| Lo tramitado se puede **consultar después**: reabrir el expediente cerrado y ver su historial y documentos | Vistas genéricas de estados cerrados | "Tras resolver, ningún paso vuelve a abrir el expediente" |
