# Ciclo de vida del expediente

## Resumen

- **Fases:** SOLICITUD (Solicitud del préstamo), VALORACION (Valoración de la jefatura de estudios), ENTREGA (Entrega del equipo)
- **Estado en el que nace el expediente:** SOLICITUD / DATOS_PETICION — hay exactamente uno en todo el trámite.
- **Estados que cierran el expediente:** VALORACION / DENEGADO y ENTREGA / ENTREGADO — desde ellos ya no se puede lanzar ninguna acción.
- **Desde qué estado se puede borrar el expediente:** SOLICITUD / DATOS_PETICION, y solo el perfil CREADOR.

## Al crear el expediente

Al crear el expediente, antes de mostrar la primera pantalla, el sistema rellena solo:

| Dato | Con qué valor | Por qué |
|---|---|---|
| curso académico | el curso académico en vigor en la fecha en que se crea el expediente | El préstamo es siempre para el curso en marcha y el alumno no debe poder elegir otro. |
| nombre y apellidos del solicitante | los del alumno que crea el expediente | Se imprimen en la solicitud y en el acta de entrega. |
| documento de identidad del solicitante | el del alumno que crea el expediente | Se imprime en la solicitud y es con el que se comprueba la firma. |

- **Quién queda registrado como interesado y como solicitante:** el propio alumno que crea el expediente, en ambos papeles.
- **Con qué documento de identidad se firmará en el equipo del interesado:** el del alumno que crea el expediente.
- El expediente nace en el estado SOLICITUD / DATOS_PETICION.

---

## Fase SOLICITUD — Solicitud del préstamo

### Estado DATOS_PETICION — Datos de la petición

- **Quién actúa (tiene el turno):** CREADOR
- **Cierra el expediente:** no
- **Qué consulta el usuario en este estado:** sus datos personales y el curso académico, ya rellenos y sin poder cambiarlos. Si el expediente ha vuelto aquí porque la jefatura de estudios pidió una corrección, ve además el texto de qué hay que corregir.
- **Qué datos introduce el usuario en este estado:**
  - equipo solicitado — qué se pide prestado; una de estas dos opciones: «Portátil» o «Tableta».
  - motivo de la petición — por qué lo pide; una de estas tres opciones: «No dispongo de equipo en casa», «Avería de mi equipo» u «Otros».
  - explicación del motivo — texto libre en el que el alumno explica su situación; solo tiene sentido cuando el motivo elegido es «Otros».
  - fecha prevista de devolución — cuándo se compromete a devolver el equipo.
  - documento acreditativo — el fichero que aporta para acreditar su situación.
- **Qué acciones puede lanzar:** CONTINUAR, y además puede borrar el expediente.

#### Acción CONTINUAR — botón «Siguiente»

- **Quién la lanza:** CREADOR
- **Pide confirmación antes de ejecutarse:** no
- **Datos que el usuario envía al lanzarla:**
  - equipo solicitado
  - motivo de la petición
  - explicación del motivo
  - fecha prevista de devolución
  - documento acreditativo
- **Comprobaciones que deben pasar antes de dejarla ejecutarse:**
  - VAL-DATOS_PETICION-CONTINUAR-001 — El equipo solicitado está elegido
    - mensaje: "Debe indicar qué equipo solicita"
  - VAL-DATOS_PETICION-CONTINUAR-002 — El motivo de la petición está elegido
    - mensaje: "Debe indicar el motivo de la petición"
  - VAL-DATOS_PETICION-CONTINUAR-003 — La explicación del motivo está rellena
    - condición: el motivo elegido es «Otros»
    - mensaje: "Debe explicar el motivo cuando elige «Otros»"
  - VAL-DATOS_PETICION-CONTINUAR-004 — La explicación del motivo tiene entre 10 y 300 caracteres
    - condición: el motivo elegido es «Otros»
    - mensaje: "La explicación debe tener entre 10 y 300 caracteres"
  - VAL-DATOS_PETICION-CONTINUAR-005 — La explicación del motivo no está escrita entera en mayúsculas
    - condición: el motivo elegido es «Otros»
    - mensaje: "La explicación no puede escribirse entera en mayúsculas, porque se imprime tal cual en la solicitud"
  - VAL-DATOS_PETICION-CONTINUAR-006 — La fecha prevista de devolución está rellena
    - mensaje: "Debe indicar la fecha prevista de devolución"
  - VAL-DATOS_PETICION-CONTINUAR-007 — La fecha prevista de devolución es posterior a hoy y no pasa del último día del curso académico en vigor
    - mensaje: "La fecha prevista de devolución debe ser posterior a hoy y anterior al fin del curso"
  - VAL-DATOS_PETICION-CONTINUAR-008 — El documento acreditativo está adjunto
    - mensaje: "Debe adjuntar el documento que acredita su situación"
  - VAL-DATOS_PETICION-CONTINUAR-009 — El documento acreditativo es un PDF o una imagen (PNG o JPEG)
    - mensaje: "El documento acreditativo debe ser un PDF o una imagen"
  - VAL-DATOS_PETICION-CONTINUAR-010 — El documento acreditativo no supera los 5 MB
    - mensaje: "El documento acreditativo no puede superar los 5 MB"
- **Qué produce la acción, en este orden:**
  1. RN-001 — Borrar el texto de qué hay que corregir, si el expediente venía de una petición de corrección, para que no se arrastre al siguiente intento
     - condición: el expediente había vuelto a este estado desde la valoración
  2. RN-002 — Generar el documento de solicitud de préstamo con los datos del alumno y de la petición, y guardarlo en el expediente
- **A qué estado lleva:** SOLICITUD / PENDIENTE_FIRMA

#### Borrado del expediente — botón «Borrar el expediente»

- **Quién lo lanza:** CREADOR
- **Pide confirmación antes de ejecutarse:** sí, con el texto «Se va a eliminar el expediente y no podrá recuperarlo»
- **Datos que el usuario envía al lanzarlo:** *(ninguno)*
- **Comprobaciones que deben pasar antes de dejarlo ejecutarse:** *(ninguna)*
- **Qué produce:** *(nada)*
- **A qué estado lleva:** *(el expediente desaparece)*

### Estado PENDIENTE_FIRMA — Pendiente de firma y presentación

- **Quién actúa (tiene el turno):** CREADOR
- **Cierra el expediente:** no
- **Qué consulta el usuario en este estado:** el documento de solicitud que se acaba de generar, incrustado en la pantalla, para que lo revise antes de firmarlo.
- **Qué datos introduce el usuario en este estado:** *(ninguno: solo revisa, vuelve atrás o firma)*
- **Qué acciones puede lanzar:** VOLVER, PRESENTAR

#### Acción VOLVER — botón «Atrás»

- **Quién la lanza:** CREADOR
- **Pide confirmación antes de ejecutarse:** no
- **Datos que el usuario envía al lanzarla:** *(ninguno)*
- **Comprobaciones que deben pasar antes de dejarla ejecutarse:** *(ninguna)*
- **Qué produce la acción:** *(nada: la solicitud generada se conservará hasta que el alumno vuelva a continuar, momento en que se regenera)*
- **A qué estado lleva:** SOLICITUD / DATOS_PETICION

#### Acción PRESENTAR — botón «Firmar y presentar»

- **Quién la lanza:** CREADOR
- **Pide confirmación antes de ejecutarse:** sí, con el texto «Una vez presentada la petición no podrá modificarla»
- **Datos que el usuario envía al lanzarla:**
  - solicitud firmada — el documento de solicitud una vez firmado por el alumno en su propio equipo. Es una excepción a la regla general: aunque el documento lo produce el sistema, la versión firmada llega desde el equipo del alumno y es el único sitio donde se puede comprobar la firma.
- **Comprobaciones que deben pasar antes de dejarla ejecutarse:**
  - VAL-PENDIENTE_FIRMA-PRESENTAR-001 — La solicitud firmada está presente
    - mensaje: "Debe firmar la solicitud antes de presentarla"
  - VAL-PENDIENTE_FIRMA-PRESENTAR-002 — La solicitud firmada lleva una única firma válida, hecha con un certificado de confianza, que no altera el texto del documento generado y que corresponde al documento de identidad del alumno solicitante
    - mensaje: "La firma no es válida o no corresponde a su documento de identidad"
- **Qué produce la acción, en este orden:**
  1. RN-003 — Dejar constancia de la entrada de la solicitud firmada, con el documento acreditativo aportado como anexo
  2. RN-004 — Firmar el justificante de presentación con la firma del Secretario del centro y guardarlo en el expediente para que el alumno pueda consultarlo
  3. RN-005 — Anotar la fecha y la hora de presentación
- **A qué estado lleva:** VALORACION / PENDIENTE_VALORACION

---

## Fase VALORACION — Valoración de la jefatura de estudios

### Estado PENDIENTE_VALORACION — Pendiente de valoración

- **Quién actúa (tiene el turno):** RESPONSABLE
- **Cierra el expediente:** no
- **Qué consulta el usuario en este estado:** los datos del alumno y de la petición, en solo lectura; la solicitud firmada, incrustada en la pantalla; y el documento acreditativo que el alumno aportó.
- **Qué datos introduce el usuario en este estado:**
  - sentido de la valoración — qué se decide; una de estas tres opciones: «Conceder el préstamo», «Denegar el préstamo» o «Pedir que se corrija».
  - motivo de la denegación — texto libre en el que se explica por qué se deniega; solo tiene sentido cuando se deniega.
  - qué hay que corregir — texto libre en el que se le dice al alumno qué debe cambiar; solo tiene sentido cuando se pide una corrección.
- **Qué acciones puede lanzar:** VALORAR

#### Acción VALORAR — botón «Valorar la petición»

- **Quién la lanza:** RESPONSABLE
- **Pide confirmación antes de ejecutarse:** sí, con el texto «Va a resolver la petición y no podrá deshacerlo»
- **Datos que el usuario envía al lanzarla:**
  - sentido de la valoración
  - motivo de la denegación
  - qué hay que corregir
- **Comprobaciones que deben pasar antes de dejarla ejecutarse:**
  - VAL-PENDIENTE_VALORACION-VALORAR-001 — El sentido de la valoración está elegido
    - mensaje: "Debe indicar qué decide sobre la petición"
  - VAL-PENDIENTE_VALORACION-VALORAR-002 — El motivo de la denegación está relleno
    - condición: el sentido de la valoración es «Denegar el préstamo»
    - mensaje: "Debe indicar el motivo de la denegación"
  - VAL-PENDIENTE_VALORACION-VALORAR-003 — El motivo de la denegación tiene entre 10 y 500 caracteres
    - condición: el sentido de la valoración es «Denegar el préstamo»
    - mensaje: "El motivo de la denegación debe tener entre 10 y 500 caracteres"
  - VAL-PENDIENTE_VALORACION-VALORAR-004 — El texto de qué hay que corregir está relleno
    - condición: el sentido de la valoración es «Pedir que se corrija»
    - mensaje: "Debe indicar al alumno qué tiene que corregir"
  - VAL-PENDIENTE_VALORACION-VALORAR-005 — Quien valora pertenece al mismo centro que el expediente
    - mensaje: "Solo puede valorar las peticiones de su propio centro"
- **Qué produce la acción, en este orden:**
  1. RN-006 — Anotar la fecha de la valoración y quién la hizo
  2. RN-007 — Generar el documento de resolución de denegación con el motivo indicado y firmarlo con la firma del Director del centro
     - condición: el sentido de la valoración es «Denegar el préstamo»
  3. RN-008 — Dejar constancia de la salida de la resolución de denegación y guardarla en el expediente
     - condición: el sentido de la valoración es «Denegar el préstamo»
  4. RN-009 — Borrar el motivo de la denegación, para que no quede escrito cuando la petición no se ha denegado
     - condición: el sentido de la valoración no es «Denegar el préstamo»
- **A qué estado lleva:**
  - si el sentido de la valoración es «Conceder el préstamo» → ENTREGA / PENDIENTE_ENTREGA
  - si el sentido de la valoración es «Denegar el préstamo» → VALORACION / DENEGADO
  - si el sentido de la valoración es «Pedir que se corrija» → SOLICITUD / DATOS_PETICION

### Estado DENEGADO — Préstamo denegado

- **Quién actúa (tiene el turno):** *(ninguno: nadie puede hacer nada aquí)*
- **Cierra el expediente:** sí
- **Qué consulta el usuario en este estado:** los datos de la petición, el motivo de la denegación y la resolución de denegación registrada de salida.
- **Qué datos introduce el usuario en este estado:** *(ninguno: el estado es de solo consulta)*
- **Qué acciones puede lanzar:** *(ninguna)*

---

## Fase ENTREGA — Entrega del equipo

### Estado PENDIENTE_ENTREGA — Pendiente de entrega

- **Quién actúa (tiene el turno):** SECRETARIO
- **Cierra el expediente:** no
- **Qué consulta el usuario en este estado:** los datos del alumno y de la petición, en solo lectura, y qué equipo se concedió.
- **Qué datos introduce el usuario en este estado:**
  - número de inventario del equipo entregado — el código con el que el centro identifica el equipo que se entrega.
  - observaciones del estado del equipo — texto libre con el estado en que se entrega y los accesorios que lleva.
- **Qué acciones puede lanzar:** ENTREGAR

#### Acción ENTREGAR — botón «Registrar la entrega»

- **Quién la lanza:** SECRETARIO
- **Pide confirmación antes de ejecutarse:** sí, con el texto «Va a dar por entregado el equipo y no podrá deshacerlo»
- **Datos que el usuario envía al lanzarla:**
  - número de inventario del equipo entregado
  - observaciones del estado del equipo
- **Comprobaciones que deben pasar antes de dejarla ejecutarse:**
  - VAL-PENDIENTE_ENTREGA-ENTREGAR-001 — El número de inventario del equipo entregado está relleno
    - mensaje: "Debe indicar el número de inventario del equipo entregado"
  - VAL-PENDIENTE_ENTREGA-ENTREGAR-002 — El número de inventario tiene la forma «INV-» seguido del año de cuatro cifras, un guion y cuatro cifras más
    - mensaje: "El número de inventario debe tener la forma INV-2026-0001"
  - VAL-PENDIENTE_ENTREGA-ENTREGAR-003 — Las observaciones del estado del equipo no superan los 300 caracteres
    - mensaje: "Las observaciones no pueden superar los 300 caracteres"
  - VAL-PENDIENTE_ENTREGA-ENTREGAR-004 — Quien registra la entrega pertenece al mismo centro que el expediente
    - mensaje: "Solo puede registrar entregas de su propio centro"
- **Qué produce la acción, en este orden:**
  1. RN-010 — Anotar la fecha de entrega y quién la registró
  2. RN-011 — Generar el acta de entrega con los datos del alumno, del equipo y de las observaciones, y firmarla con la firma del Secretario del centro
  3. RN-012 — Guardar el acta de entrega firmada en el expediente para que el alumno pueda consultarla
- **A qué estado lleva:** ENTREGA / ENTREGADO

### Estado ENTREGADO — Equipo entregado

- **Quién actúa (tiene el turno):** *(ninguno: nadie puede hacer nada aquí)*
- **Cierra el expediente:** sí
- **Qué consulta el usuario en este estado:** los datos de la petición, el número de inventario del equipo entregado, las observaciones y el acta de entrega firmada.
- **Qué datos introduce el usuario en este estado:** *(ninguno: el estado es de solo consulta)*
- **Qué acciones puede lanzar:** *(ninguna)*

---

## Tabla de transiciones

| Estado de partida | Acción | Condición | Estado siguiente | Qué produce |
|---|---|---|---|---|
| *(el expediente se crea)* | — | — | SOLICITUD / DATOS_PETICION | El curso académico y los datos personales del alumno |
| SOLICITUD / DATOS_PETICION | CONTINUAR | — | SOLICITUD / PENDIENTE_FIRMA | Se genera el documento de solicitud y se limpia lo que hubiera que corregir |
| SOLICITUD / DATOS_PETICION | *(borrar el expediente)* | — | *(el expediente desaparece)* | *(nada)* |
| SOLICITUD / PENDIENTE_FIRMA | VOLVER | — | SOLICITUD / DATOS_PETICION | *(nada)* |
| SOLICITUD / PENDIENTE_FIRMA | PRESENTAR | — | VALORACION / PENDIENTE_VALORACION | Constancia de entrada de la solicitud firmada, justificante de presentación y fecha de presentación |
| VALORACION / PENDIENTE_VALORACION | VALORAR | sentido de la valoración = «Conceder el préstamo» | ENTREGA / PENDIENTE_ENTREGA | Fecha y autor de la valoración |
| VALORACION / PENDIENTE_VALORACION | VALORAR | sentido de la valoración = «Denegar el préstamo» | VALORACION / DENEGADO | Fecha y autor de la valoración, resolución de denegación firmada y constancia de su salida |
| VALORACION / PENDIENTE_VALORACION | VALORAR | sentido de la valoración = «Pedir que se corrija» | SOLICITUD / DATOS_PETICION | Fecha y autor de la valoración, y el texto de qué hay que corregir |
| ENTREGA / PENDIENTE_ENTREGA | ENTREGAR | — | ENTREGA / ENTREGADO | Fecha y autor de la entrega, y el acta de entrega firmada |

## Datos que rellena el sistema

- CC-001 — curso académico
  - momento: al crear el expediente
  - sobreescribible: nunca
  - cálculo: el curso académico en vigor en la fecha en que se crea el expediente
- CC-002 — nombre, apellidos y documento de identidad del solicitante
  - momento: al crear el expediente
  - sobreescribible: nunca
  - cálculo: se copian del alumno que crea el expediente, para que queden congelados en el expediente aunque después cambien en su ficha
- CC-003 — documento de solicitud
  - momento: al lanzar la acción CONTINUAR desde SOLICITUD / DATOS_PETICION
  - sobreescribible: nunca
  - cálculo: se genera con los datos del alumno y de la petición; se vuelve a generar cada vez que el alumno continúa, de modo que siempre refleja los últimos datos
- CC-004 — fecha y hora de presentación
  - momento: al lanzar la acción PRESENTAR desde SOLICITUD / PENDIENTE_FIRMA
  - sobreescribible: nunca
  - cálculo: el momento en que se deja constancia de la entrada
- CC-005 — justificante de presentación
  - momento: al lanzar la acción PRESENTAR desde SOLICITUD / PENDIENTE_FIRMA
  - sobreescribible: nunca
  - cálculo: es el resguardo que produce la constancia de entrada, firmado con la firma del Secretario del centro
- CC-006 — fecha de la valoración y quién la hizo
  - momento: al lanzar la acción VALORAR desde VALORACION / PENDIENTE_VALORACION
  - sobreescribible: nunca
  - cálculo: la fecha del día y el nombre y el cargo de quien pulsó el botón
- CC-007 — resolución de denegación
  - momento: al lanzar la acción VALORAR desde VALORACION / PENDIENTE_VALORACION, solo cuando se deniega
  - sobreescribible: nunca
  - cálculo: se genera con el motivo de la denegación, se firma con la firma del Director del centro y se guarda ya registrada de salida
- CC-008 — fecha de entrega y quién la registró
  - momento: al lanzar la acción ENTREGAR desde ENTREGA / PENDIENTE_ENTREGA
  - sobreescribible: nunca
  - cálculo: la fecha del día y el nombre de quien pulsó el botón
- CC-009 — acta de entrega
  - momento: al lanzar la acción ENTREGAR desde ENTREGA / PENDIENTE_ENTREGA
  - sobreescribible: nunca
  - cálculo: se genera con los datos del alumno, del equipo entregado y de las observaciones, y se firma con la firma del Secretario del centro
