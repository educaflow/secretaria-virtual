# Catálogo de cobertura de historias de usuario y escenarios

Catálogo de referencia para comprobar si a la spec le **faltan historias de usuario (`HU-`) o escenarios (`ESC-`)**. No inventa funcionalidad nueva: cada comprobación se deduce de lo que la spec **ya declara** (actores, modelos, acciones, pantallas, seguridad, validaciones, estados) y detecta lo declarado que ningún escenario ejercita.

Es una ayuda **no exhaustiva**: se pueden proponer historias o escenarios que no respondan a ninguna fila si la spec los sugiere.

## Cobertura de historias de usuario

| Comprobación                                                              | De dónde se deduce                  | Ejemplo de hueco                                                       |
|-----------------------------------------------------------------------------|---------------------------------------|---------------------------------------------------------------------------|
| Cada actor declarado tiene al menos una historia desde su punto de vista   | Apartado Actores + Seguridad          | "El Supervisor tiene acceso en Seguridad pero ninguna HU lo protagoniza"  |
| Cada pantalla declarada aparece en al menos una historia/escenario         | Tabla Pantallas                       | "La pantalla «Mis correos» no la recorre ningún escenario"                |
| Cada acción de negocio de una entidad (más allá de Crear/Modificar) tiene su historia o escenario | Acciones de los `entity-*.md` | "La acción Reenviar no aparece en ninguna historia"          |
| Un rol de consulta (el usuario final que solo ve sus datos) tiene su historia de consulta | Seguridad                | "El destinatario puede ver sus correos pero no hay HU de consulta"        |

## Cobertura de escenarios dentro de cada historia

| Comprobación                                                              | De dónde se deduce                  | Ejemplo de hueco                                                       |
|-----------------------------------------------------------------------------|---------------------------------------|---------------------------------------------------------------------------|
| Camino feliz: la historia tiene un escenario donde todo va bien            | La propia HU                          | "HU-002 solo tiene escenarios de error"                                    |
| Un escenario de error por cada validación relevante, con su mensaje literal | `VAL-`/`RES-` de los `entity-*.md`   | "VAL-Correo-003 (asunto obligatorio) no tiene escenario que muestre su error" |
| Cada rama o estado del ciclo de vida se ejercita (éxito y fallo)           | «Estados y transiciones»              | "Nada prueba que el correo quede en FAIL cuando el envío falla"            |
| Alcance multicentro: un rol de un centro no ve los datos de otro           | Seguridad (alcance por centro)        | "Nada prueba que el supervisor de Mislata no vea los correos de Batoi"     |
| Visibilidad restringida: quien no cumple la condición no ve el dato/botón  | Seguridad + `RUI-`                    | "Nada prueba que el destinatario no vea los correos en estado FAIL"        |
| Lo persistido se puede consultar después (reabrir/listar tras crear)       | Pantallas de consulta                 | "Se crea el correo pero ningún paso comprueba que aparece en el listado"   |
