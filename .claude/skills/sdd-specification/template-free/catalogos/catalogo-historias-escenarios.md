# Catálogo de cobertura de historias de usuario y escenarios (feature libre)

Catálogo de referencia para comprobar si a la spec le **faltan historias de usuario (`HU-`) o escenarios (`ESC-`)**.
No inventa funcionalidad nueva: cada comprobación se deduce de lo que la spec **ya declara** («Contexto y alcance», Actores, Requisitos, Seguridad) y detecta lo declarado que ningún escenario ejercita.

Es una ayuda **no exhaustiva**: se pueden proponer historias o escenarios que no respondan a ninguna fila si la spec los sugiere.

## Cobertura de historias de usuario

| Comprobación | De dónde se deduce | Ejemplo de hueco |
|---|---|---|
| Cada actor declarado tiene al menos una historia desde su punto de vista | Actores + Seguridad | «El Supervisor administra la feature según Seguridad pero ninguna HU lo protagoniza» |
| Cada **parte afectada** de «Contexto y alcance» aparece en al menos un escenario | Contexto y alcance | «La feature cambia la pantalla de correos del centro y ningún escenario la abre» |
| Quien **administra** la feature (la configura, la activa, la desactiva) tiene su historia | Seguridad | «Alguien activa el aviso global pero no hay HU de administración» |
| Quien **sufre** la feature (el usuario final al que le cambia algo) tiene su historia | Actores + Contexto | «Todos los usuarios ven el banner, pero no hay HU desde el punto de vista de un profesor» |

## Cobertura de escenarios dentro de cada historia

| Comprobación | De dónde se deduce | Ejemplo de hueco |
|---|---|---|
| Camino feliz: la historia tiene un escenario donde todo va bien | La propia HU | «HU-002 solo tiene escenarios de error» |
| Un escenario de error por cada requisito con `mensaje` | Requisitos | «REQ-003 (texto obligatorio) no tiene escenario que muestre su error» |
| Un escenario por cada `condición` de un requisito (se cumple / no se cumple) | Requisitos | «REQ-005 aplica solo si el aviso está activo, pero nada prueba el caso inactivo» |
| **No-regresión**: por cada parte existente que la feature roza, un escenario que comprueba que lo que ya funcionaba sigue funcionando | Contexto y alcance | «Se cambia el inicio de sesión y ningún escenario comprueba que un usuario normal sigue entrando» |
| Alcance multicentro: un rol de un centro no ve ni afecta a los datos de otro | Seguridad (alcance por centro) | «Nada prueba que el supervisor de Mislata no vea la configuración de Batoi» |
| Sin permiso: quien no está declarado en Seguridad no puede hacer la acción ni ver la pantalla | Seguridad (deny by default) | «Nada prueba que un alumno no puede abrir la pantalla de avisos globales» |
| Datos existentes: lo que ya había en la aplicación se comporta como dice el REQ de «datos existentes» | Requisitos (etiqueta `datos existentes`) | «REQ-004 dice qué pasa con los registros antiguos pero ningún escenario lo prueba» |
| Fallo: lo que ve el usuario cuando la feature no puede hacer su trabajo | Requisitos (fallo) | «Si el servicio externo no responde, el REQ dice que se avisa, pero no hay escenario» |
| Lo persistido se puede consultar después (reabrir o listar tras crear o cambiar) | Historias | «Se guarda la configuración pero ningún paso comprueba que se conserva al volver a entrar» |
