# Ejemplo completo de especificación de un trámite

**Este trámite es INVENTADO.** No existe en el proyecto y no se corresponde con ninguno real: se ha construido solo para enseñar el aspecto final de una especificación terminada e instanciada. **MUST NOT** copiarse su contenido al output ni tomarse por norma.

En particular, **MUST NOT** deducirse de aquí ninguna regla sobre **cuántas** cosas tiene un trámite. Este ejemplo tiene 3 fases, 6 estados, 3 perfiles y 3 documentos porque le venía bien al ejemplo; el patrón vale igual con 1 fase, con 2 estados, con un solo perfil o con ningún documento.

El trámite del ejemplo es **«Préstamo de equipo informático al alumnado»**: un alumno pide prestado un portátil o una tableta del centro, la jefatura de estudios valora la petición y, si la concede, el personal administrativo registra la entrega del equipo.

| Fichero | Qué ilustra |
|---|---|
| `specification.md` | El índice: el trámite y su texto de ayuda, los tres perfiles con quién los ostenta, cuatro historias con sus escenarios, los registros de entrada y salida, la seguridad con su alcance por centro y los datos iniciales. |
| `estados.md` | El ciclo de vida completo: qué se rellena al crear el expediente, tres fases, seis estados, una acción que **ramifica en tres**, una que **vuelve atrás**, el **borrado**, dos estados que **cierran** el expediente, la tabla de transiciones y los datos que rellena el sistema. |
| `pantallas-solicitud.md` | Las pantallas de la primera fase: dos estados con perfil, con sus pantallas editables y sus pantallas de solo consulta, un documento incrustado, un aviso permanente y reglas de pantalla condicionales. |
| `pantallas-valoracion.md` | Las pantallas de la fase de valoración: un estado con perfil y **un estado cerrado**, que solo tiene la pantalla de solo consulta. |
| `pantallas-entrega.md` | Las pantallas de la última fase: la entrega y el estado final. |
| `documentos.md` | Tres documentos que cubren las **tres formas de firmar**: uno que firma el propio interesado en su equipo, uno que firma el centro con la firma del Director y otro con la del Secretario; dos se registran (uno de entrada, otro de salida) y uno no. |

Fíjate, al leerlo, en tres cosas que se comprueban siempre:

1. **No hay ni una palabra de tecnología**: ni ficheros, ni clases, ni atributos, ni tipos, ni nombres de campo. Sí hay nombres de fase, de estado, de acción y de perfil, porque son vocabulario del negocio del trámite.
2. **Cada estado tiene su pantalla de solo consulta** para el resto de perfiles, con su botón «Salir», incluidos los estados que cierran el expediente.
3. **La lista «Datos que el usuario envía» de cada acción cuadra, dato a dato, con el «Qué puede rellenar» de la pantalla** desde la que se lanza — y no lleva ningún dato que rellene el sistema.
