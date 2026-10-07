# Catálogo de requisitos de una feature libre

Catálogo de referencia para comprobar si a la spec le **faltan requisitos (`REQ-`)**.
Una feature transversal tiene menos estructura que un sistema o un expediente (no hay entidades ni estados que recorrer campo a campo), así que lo que se olvida es distinto: lo que pasa **alrededor** de la feature.
Cada apartado es una pregunta que hacerse sobre la spec; cada fila, un requisito que suele faltar.

Es una ayuda **no exhaustiva**: declara igualmente lo que el negocio necesite aunque no figure aquí.

## Seguridad y alcance

| Pregunta | Requisito que suele faltar | Ejemplo |
|---|---|---|
| ¿Quién administra la feature? | Solo ese rol puede configurarla, activarla o desactivarla | «Solo el administrador puede crear o desactivar un aviso global» |
| ¿Qué ve un rol **sin** permiso? | No ve la pantalla ni el menú, y si llega por otro camino no puede actuar | «Un profesor no ve el menú de avisos globales» |
| ¿Respeta el multicentro? | Un usuario solo ve y afecta a lo de sus centros; el administrador, a todo | «La configuración de un centro no se aplica a otro» |
| ¿Hay datos que **no** pueda dictar la interfaz? | Lo que fija el sistema (fechas, autor, estado) no lo envía el usuario (`AllowProperties`) | «La fecha de publicación la pone el sistema» |

## Datos existentes y despliegue

| Pregunta | Requisito que suele faltar | Ejemplo |
|---|---|---|
| ¿Qué pasa con lo que **ya hay** en la aplicación cuando se despliega? | Comportamiento de los registros, usuarios o configuración anteriores | «Los usuarios que ya tenían sesión ven el aviso al cambiar de pantalla» |
| ¿Hace falta un valor inicial? | Qué valor tiene la feature antes de que nadie la configure | «Sin ningún aviso creado, no se muestra ningún banner» |
| ¿Hay que precargar algo? | Datos iniciales, plantillas, ficheros | «El texto por defecto del pie de página se precarga al arrancar» |

## Fallo y degradación

| Pregunta | Requisito que suele faltar | Ejemplo |
|---|---|---|
| ¿Qué ve el usuario si la feature **falla**? | El mensaje literal y que el resto de la aplicación sigue funcionando | «Si el servicio de firma no responde, el sistema muestra «…» y la pantalla sigue operativa» |
| ¿Qué pasa si la entrada es inválida o está vacía? | La validación y su mensaje | «Si el texto está vacío al guardar, el sistema no guarda y muestra «…»» |
| ¿Puede fallar a medias? | Qué queda hecho y qué no si algo se interrumpe | «Si falla el envío a un destinatario, el resto se envía igualmente» |

## Resto de la aplicación

| Pregunta | Requisito que suele faltar | Ejemplo |
|---|---|---|
| ¿Qué pasa en las pantallas que **no** se tocan? | Que no cambian, o que la feature también les afecta | «El banner se muestra también en las pantallas de expedientes» |
| ¿Rompe algo que ya funcionaba? | No-regresión explícita de la parte rozada | «El inicio de sesión sigue funcionando igual para los usuarios sin aviso pendiente» |
| ¿Afecta a los dos idiomas? | Los textos nuevos se muestran en el idioma del usuario | «El texto del banner lo escribe el administrador; los rótulos de la pantalla se traducen» |

## Límites y comportamiento

| Pregunta | Requisito que suele faltar | Ejemplo |
|---|---|---|
| ¿Hay límites de tamaño, cantidad o tiempo? | El límite y qué pasa al superarlo | «El texto del aviso no supera 500 caracteres» |
| ¿Cuándo deja de aplicar? | Caducidad, desactivación, orden de prioridad | «Un aviso con fecha de fin pasada deja de mostrarse automáticamente» |
| ¿Qué pasa con varios a la vez? | Orden, exclusión, acumulación | «Si hay varios avisos activos se muestran todos, el más reciente primero» |
| ¿Queda rastro? | Qué se registra y quién lo puede consultar | «Se guarda quién creó el aviso y cuándo» |
