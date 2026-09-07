# Pantallas de la fase RECEPCION — Recepción

Este fichero es un **delta**: declara solo las pantallas que la modificación cambia. Las que no aparecen aquí se conservan tal cual.

## Estado ENTRADA_DATOS

*(sin cambios)* — ni la pantalla del perfil CREADOR ni la de solo consulta se tocan.

## Estado PENDIENTE_PRESENTACION

### Pantalla: PENDIENTE_PRESENTACION — perfil CREADOR

- **Quién la ve:** el CREADOR, es decir el profesor que creó el expediente, mientras está en RECEPCION / PENDIENTE_PRESENTACION.
- **Qué ve el usuario, bloque a bloque:**
  - **Solicitud** — la solicitud que el sistema ha generado, incrustada en la pantalla para poder leerla antes de firmarla. *(sin cambios)*
  - **Firma de la solicitud** — **bloque nuevo**, que sustituye al aviso fijo que había hasta ahora. Contiene el aviso que corresponde a la situación del profesor y, solo cuando esa situación lo exige, el campo donde teclea la clave de su certificado. En cada momento se ve **exactamente uno** de los cinco avisos posibles.
- **Qué puede rellenar:** la clave de su certificado digital, en el campo «Contraseña» o en el campo «PIN» según dónde esté guardado su certificado, y solo cuando la secretaría virtual no custodia esa clave. En cualquier otra situación no hay nada que rellenar en la pantalla.
- **Qué solo puede consultar:** la solicitud generada y el aviso sobre cómo se va a firmar.
- **Documentos que se le muestran:** la solicitud generada, incrustada en la pantalla. *(sin cambios)*
- **Aviso permanente en pantalla:** ya no hay un aviso permanente único. El aviso depende de la situación del profesor y lo fijan las reglas de pantalla RUI-PENDIENTE_PRESENTACION-CREADOR-001 a RUI-PENDIENTE_PRESENTACION-CREADOR-005.
- **Botones:**
  - **«Atrás»** — lanza la acción BACK. Sin confirmación. *(sin cambios)*
  - **«Firmar con AutoFirma y Presentar la solicitud»** — lanza la acción PRESENTAR firmando en el equipo del propio profesor. Pide confirmación con el texto «¿Esta seguro que desea presentar la documentación? No podrá deshacer esta acción». *(texto y confirmación sin cambios; lo nuevo es que ahora solo se muestra en una de las situaciones)*
  - **«Firmar y Presentar la solicitud»** — **botón nuevo**; lanza la misma acción PRESENTAR, firmando en el servidor. Pide confirmación con el mismo texto «¿Esta seguro que desea presentar la documentación? No podrá deshacer esta acción».

  Los dos botones de firmar son **excluyentes** y puede no verse ninguno: cuando el profesor no tiene documento de identidad, la pantalla solo ofrece «Atrás».

#### Reglas de pantalla

- RUI-PENDIENTE_PRESENTACION-CREADOR-001 — Se muestra el aviso «Para presentar la solicitud debe tener la aplicación de AutoFirma instalada y un certificado digital válido», con el enlace de descarga de esa aplicación.
  - disparador: continuo
  - condición: el profesor tiene documento de identidad y la secretaría virtual **no** custodia ningún certificado digital habilitado para él.
- RUI-PENDIENTE_PRESENTACION-CREADOR-002 — Se muestra el aviso «La solicitud se firmará en el servidor con su certificado digital.» y no se pide ninguna clave.
  - disparador: continuo
  - condición: la secretaría virtual custodia un certificado digital habilitado para el profesor y **también** custodia su clave (la contraseña del fichero o el PIN del dispositivo).
- RUI-PENDIENTE_PRESENTACION-CREADOR-003 — Se muestra el aviso «La solicitud se firmará en el servidor con su certificado digital. Introduzca la contraseña de su certificado.» junto al campo «Contraseña», marcado como obligatorio.
  - disparador: continuo
  - condición: la secretaría virtual custodia un certificado digital habilitado del profesor guardado en un fichero, pero **no** custodia su contraseña.
- RUI-PENDIENTE_PRESENTACION-CREADOR-004 — Se muestra el aviso «La solicitud se firmará en el servidor con su certificado digital. Introduzca el PIN de su dispositivo criptográfico.» junto al campo «PIN», marcado como obligatorio.
  - disparador: continuo
  - condición: la secretaría virtual custodia un certificado digital habilitado del profesor que está en un dispositivo criptográfico, pero **no** custodia su PIN.
- RUI-PENDIENTE_PRESENTACION-CREADOR-005 — Se muestra el aviso «No es posible firmar la solicitud porque su usuario no tiene un documento de identidad. Póngase en contacto con el administrador.» y no se muestra ningún campo de clave.
  - disparador: continuo
  - condición: el profesor no tiene documento de identidad.
- RUI-PENDIENTE_PRESENTACION-CREADOR-006 — Se muestra el botón «Firmar con AutoFirma y Presentar la solicitud».
  - disparador: continuo
  - condición: el profesor tiene documento de identidad y la secretaría virtual **no** custodia ningún certificado digital habilitado para él.
- RUI-PENDIENTE_PRESENTACION-CREADOR-007 — Se muestra el botón «Firmar y Presentar la solicitud».
  - disparador: continuo
  - condición: la secretaría virtual custodia un certificado digital habilitado para el profesor, esté donde esté guardado y custodie o no su clave.
- RUI-PENDIENTE_PRESENTACION-CREADOR-008 — El contenido del campo de la clave («Contraseña» o «PIN») se muestra enmascarado, de forma que lo tecleado no se puede leer en pantalla.
  - disparador: continuo
  - condición: Siempre que ese campo se muestre.
- RUI-PENDIENTE_PRESENTACION-CREADOR-009 — El campo de la clave («Contraseña» o «PIN») aparece vacío.
  - disparador: al abrir la pantalla
  - condición: Siempre. Se aplica también al volver a este estado después de haber pulsado «Atrás», de modo que una clave tecleada antes nunca reaparece.
- RUI-PENDIENTE_PRESENTACION-CREADOR-010 — No se muestra ningún campo de clave, ni «Contraseña» ni «PIN».
  - disparador: continuo
  - condición: el profesor tiene documento de identidad y la secretaría virtual **no** custodia ningún certificado digital habilitado para él, es decir cuando la solicitud se va a firmar en su propio equipo.
- RUI-PENDIENTE_PRESENTACION-CREADOR-011 — Después de un intento de presentar que se cancela —porque falla una comprobación o porque falla la firma en el servidor— el campo de la clave vuelve a mostrarse vacío, de modo que el profesor tiene que teclearla de nuevo para reintentarlo.
  - disparador: al volver a mostrarse la pantalla tras un intento de presentar cancelado
  - condición: Siempre que ese campo se muestre.

Ninguna de estas reglas es una defensa: que un botón no se vea o que un campo esté oculto no impide enviar nada. Lo que de verdad impide firmar mal son las comprobaciones de la acción PRESENTAR y el hecho de que, cuando corresponde firmar en el servidor, la solicitud firmada la produzca el propio servidor (ver [estados.md](./estados.md)).

### Pantalla: PENDIENTE_PRESENTACION — resto de perfiles (solo consulta)

*(sin cambios salvo la regla de pantalla de abajo)* — quien mira el expediente sin tener el turno lo sigue viendo en solo consulta, con la solicitud generada y un único botón «Salir».

#### Reglas de pantalla

- RUI-PENDIENTE_PRESENTACION-GENERICA-001 — **Nueva.** No se muestra nada del bloque «Firma de la solicitud»: ni el aviso sobre cómo se va a firmar, ni el campo «Contraseña», ni el campo «PIN», ni el botón «Firmar y Presentar la solicitud», ni el botón «Firmar con AutoFirma y Presentar la solicitud».
  - disparador: continuo
  - condición: Siempre, porque quien ve esta pantalla no es quien firma la solicitud.
