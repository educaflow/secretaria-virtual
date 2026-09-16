# Ciclo de vida del expediente

## Resumen

- **Fases:** SOLICITUD (Solicitud de anulación), REVISION (Revisión de la secretaría), RESOLUCION (Resolución del centro)
- **Estado en el que nace el expediente:** SOLICITUD / DATOS_SOLICITUD — hay exactamente uno en todo el trámite.
- **Estados que cierran el expediente:** RESOLUCION / ACEPTADA y RESOLUCION / RECHAZADA — desde ellos ya no se puede lanzar ninguna acción.
- **Desde qué estado se puede borrar el expediente:** SOLICITUD / DATOS_SOLICITUD, y solo el perfil CREADOR.

## Al crear el expediente

Al crear el expediente, antes de mostrar la primera pantalla, el sistema rellena solo:

| Dato | Con qué valor | Por qué |
|---|---|---|
| nombre y apellidos del alumno | los del alumno que crea el expediente | Se imprimen en la solicitud y en la resolución; el alumno no los puede cambiar. |
| documento de identidad (DNI/NIE) del alumno | el del alumno que crea el expediente | Se imprime en la solicitud y es con el que se comprueba su firma. |
| NIA, dirección, teléfono, población y código postal | los que consten en la ficha del alumno; los que no consten quedan vacíos | Para ahorrarle teclearlos; el alumno los revisa y los puede corregir, porque el impreso los exige y la ficha puede estar incompleta. |
| curso académico | el curso académico en vigor del centro, presentado como «2024/2025» | El impreso lo pide y el alumno no debe poder elegir otro. |
| nombre y localidad del centro | los del centro del expediente; si el centro no tiene localidad informada, queda vacía | Se imprimen en el bloque «Expone» y en el pie dirigido al director del centro. |

- **Quién queda registrado como interesado y como solicitante:** el propio alumno que crea el expediente, en ambos papeles.
- **Con qué documento de identidad se firmará en el equipo del interesado:** el del alumno que crea el expediente.
- El expediente nace en el estado SOLICITUD / DATOS_SOLICITUD.

---

## Fase SOLICITUD — Solicitud de anulación

### Estado DATOS_SOLICITUD — Datos de la solicitud

- **Quién actúa (tiene el turno):** CREADOR
- **Cierra el expediente:** no
- **Qué consulta el usuario en este estado:** su nombre y apellidos, su documento de identidad, el curso académico y el centro, ya rellenos y sin poder cambiarlos. Si el expediente ha vuelto aquí porque secretaría pidió una subsanación, ve además el texto de qué hay que subsanar.
- **Qué datos introduce el usuario en este estado:**
  - NIA — el número de identificación del alumno; 8 dígitos. Viene precargado de su ficha si consta.
  - dirección — la calle y el número donde vive; texto libre. Viene precargada de su ficha si consta.
  - teléfono — un teléfono de contacto; 9 dígitos. Viene precargado de su ficha si consta.
  - población — la localidad donde vive; texto libre. Viene precargada de su ficha si consta.
  - provincia — la provincia donde vive; texto libre.
  - código postal — 5 dígitos. Viene precargado de su ficha si consta.
  - ciclo — el ciclo formativo (o curso de especialización) en el que está matriculado, elegido de la lista de ciclos del catálogo del sistema educativo, que es común a todos los centros. En la ventana de búsqueda puede filtrarlo por familia profesional, grado, nivel y nombre. El grado y el nivel del ciclo no se teclean: los lleva puesto el propio ciclo elegido.
- **Qué acciones puede lanzar:** CONTINUAR, y además puede borrar el expediente.

#### Acción CONTINUAR — botón «Siguiente»

- **Quién la lanza:** CREADOR
- **Pide confirmación antes de ejecutarse:** no
- **Datos que el usuario envía al lanzarla:**
  - NIA
  - dirección
  - teléfono
  - población
  - provincia
  - código postal
  - ciclo
- **Comprobaciones que deben pasar antes de dejarla ejecutarse:**
  - VAL-DATOS_SOLICITUD-CONTINUAR-001 — El NIA está relleno
    - mensaje: "Debe indicar su NIA"
  - VAL-DATOS_SOLICITUD-CONTINUAR-002 — El NIA tiene exactamente 8 dígitos
    - mensaje: "El NIA debe tener 8 dígitos"
  - VAL-DATOS_SOLICITUD-CONTINUAR-003 — La dirección está rellena
    - mensaje: "Debe indicar su dirección"
  - VAL-DATOS_SOLICITUD-CONTINUAR-004 — La dirección tiene entre 5 y 150 caracteres
    - mensaje: "La dirección debe tener entre 5 y 150 caracteres"
  - VAL-DATOS_SOLICITUD-CONTINUAR-005 — El teléfono está relleno
    - mensaje: "Debe indicar un teléfono de contacto"
  - VAL-DATOS_SOLICITUD-CONTINUAR-006 — El teléfono tiene 9 dígitos y empieza por 6, 7, 8 o 9
    - mensaje: "El teléfono debe tener 9 dígitos y empezar por 6, 7, 8 o 9"
  - VAL-DATOS_SOLICITUD-CONTINUAR-007 — La población está rellena
    - mensaje: "Debe indicar su población"
  - VAL-DATOS_SOLICITUD-CONTINUAR-008 — La población tiene entre 2 y 100 caracteres
    - mensaje: "La población debe tener entre 2 y 100 caracteres"
  - VAL-DATOS_SOLICITUD-CONTINUAR-009 — La provincia está rellena
    - mensaje: "Debe indicar su provincia"
  - VAL-DATOS_SOLICITUD-CONTINUAR-010 — La provincia tiene entre 2 y 50 caracteres
    - mensaje: "La provincia debe tener entre 2 y 50 caracteres"
  - VAL-DATOS_SOLICITUD-CONTINUAR-011 — El código postal está relleno
    - mensaje: "Debe indicar su código postal"
  - VAL-DATOS_SOLICITUD-CONTINUAR-012 — El código postal tiene exactamente 5 dígitos
    - mensaje: "El código postal debe tener 5 dígitos"
  - VAL-DATOS_SOLICITUD-CONTINUAR-013 — El ciclo está elegido
    - mensaje: "Debe indicar el ciclo formativo"
  - VAL-DATOS_SOLICITUD-CONTINUAR-014 — *(eliminada: el nombre del ciclo ya no se escribe a mano)*
  - VAL-DATOS_SOLICITUD-CONTINUAR-015 — *(eliminada: el nombre del ciclo ya no se escribe a mano)*
  - VAL-DATOS_SOLICITUD-CONTINUAR-016 — Quien continúa es el propio alumno que creó el expediente
    - mensaje: "Solo puede modificar sus propias solicitudes"
  - VAL-DATOS_SOLICITUD-CONTINUAR-017 — El alumno no tiene otro expediente de este trámite en curso (que no esté cerrado ni sea este mismo) para el mismo ciclo y el mismo curso académico
    - mensaje: "Ya tiene una solicitud de anulación en curso para este ciclo"
- **Qué produce la acción, en este orden:**
  1. RN-001 — Anotar el lugar de la solicitud (la localidad del centro) y la fecha de la solicitud (la fecha de hoy), que se imprimen al pie del documento
  2. RN-002 — Generar la solicitud de anulación de matrícula con los datos del alumno, del centro y del ciclo (su nombre y el grado o nivel que le corresponde), y guardarla en el expediente sustituyendo a la que hubiera de un intento anterior
- **A qué estado lleva:** SOLICITUD / PENDIENTE_FIRMA

#### Borrado del expediente — botón «Borrar el expediente»

- **Quién lo lanza:** CREADOR
- **Pide confirmación antes de ejecutarse:** sí, con el texto «Se va a eliminar el expediente y no podrá recuperarlo»
- **Datos que el usuario envía al lanzarlo:** *(ninguno)*
- **Comprobaciones que deben pasar antes de dejarlo ejecutarse:**
  - VAL-DATOS_SOLICITUD-BORRADO-001 — Quien borra el expediente es el propio alumno que lo creó
    - mensaje: "Solo puede borrar sus propias solicitudes"
- **Qué produce:** *(nada: si el expediente había sido presentado antes de una subsanación, la constancia de aquella entrada sigue en el registro del centro, que no forma parte del expediente)*
- **A qué estado lleva:** *(el expediente desaparece)*

### Estado PENDIENTE_FIRMA — Pendiente de firma y presentación

- **Quién actúa (tiene el turno):** CREADOR
- **Cierra el expediente:** no
- **Qué consulta el usuario en este estado:** la solicitud de anulación que se acaba de generar, incrustada en la pantalla, para que la revise antes de firmarla.
- **Qué datos introduce el usuario en este estado:** *(ninguno: solo revisa, vuelve atrás o firma)*
- **Qué acciones puede lanzar:** VOLVER, PRESENTAR

#### Acción VOLVER — botón «Atrás»

- **Quién la lanza:** CREADOR
- **Pide confirmación antes de ejecutarse:** no
- **Datos que el usuario envía al lanzarla:** *(ninguno)*
- **Comprobaciones que deben pasar antes de dejarla ejecutarse:** *(ninguna)*
- **Qué produce la acción:** *(nada: la solicitud generada se conserva hasta que el alumno vuelva a continuar, momento en que se regenera)*
- **A qué estado lleva:** SOLICITUD / DATOS_SOLICITUD

#### Acción PRESENTAR — botón «Firmar y presentar la solicitud»

- **Quién la lanza:** CREADOR
- **Pide confirmación antes de ejecutarse:** sí, con el texto «Va a firmar y presentar la solicitud de anulación. Una vez presentada no podrá modificarla»
- **Datos que el usuario envía al lanzarla:**
  - solicitud firmada — la solicitud de anulación una vez firmada por el alumno en su propio equipo. Es una excepción a la regla general: aunque el documento lo produce el sistema, la versión firmada llega desde el equipo del alumno y es el único sitio donde se puede comprobar la firma.
- **Comprobaciones que deben pasar antes de dejarla ejecutarse:**
  - VAL-PENDIENTE_FIRMA-PRESENTAR-001 — La solicitud firmada está presente
    - mensaje: "Debe firmar la solicitud antes de presentarla"
  - VAL-PENDIENTE_FIRMA-PRESENTAR-002 — La solicitud firmada lleva una única firma válida, hecha con un certificado de confianza, que no altera el texto de la solicitud generada y que corresponde al documento de identidad del alumno
    - mensaje: "La firma no es válida o no corresponde a su documento de identidad"
- **Qué produce la acción, en este orden:**
  1. RN-003 — Dejar constancia de la entrada de la solicitud firmada, sin anexos
  2. RN-004 — Guardar en el expediente el justificante de presentación que produce el registro de entrada, sustituyendo al de una presentación anterior si el expediente venía de una subsanación
  3. RN-005 — Anotar la fecha y la hora de presentación (si venía de una subsanación, la de esta última presentación, que es la que fija desde cuándo surte efecto la anulación)
  4. RN-006 — Borrar el texto de qué hay que subsanar, para que secretaría reciba la solicitud corregida sin la petición ya atendida
     - condición: el expediente venía de una petición de subsanación
  5. RN-007 — Borrar el sentido de la revisión, el motivo del rechazo y el motivo de la devolución de un ciclo de revisión anterior, para que secretaría decida de nuevo desde cero sobre la solicitud corregida
     - condición: el expediente venía de una petición de subsanación
- **A qué estado lleva:** REVISION / PENDIENTE_REVISION

---

## Fase REVISION — Revisión de la secretaría

### Estado PENDIENTE_REVISION — Pendiente de revisión

- **Quién actúa (tiene el turno):** SECRETARIO
- **Cierra el expediente:** no
- **Qué consulta el usuario en este estado:** los datos del alumno y del ciclo, en solo lectura; la solicitud firmada, incrustada en la pantalla; el justificante de presentación y la fecha de presentación. Si el director ha devuelto la resolución, ve además el motivo de la devolución y el sentido y el motivo que había elegido antes.
- **Qué datos introduce el usuario en este estado:**
  - sentido de la revisión — qué decide secretaría; una de estas tres opciones: «Aceptar la anulación», «Rechazar la anulación» o «Pedir subsanación».
  - motivo del rechazo — texto libre en el que se explica por qué no procede la anulación; solo tiene sentido cuando se rechaza. Se imprime en la resolución.
  - qué hay que subsanar — texto libre en el que se le dice al alumno qué datos debe corregir; solo tiene sentido cuando se pide subsanación.
- **Qué acciones puede lanzar:** ENVIAR_A_FIRMA, SUBSANAR

#### Acción ENVIAR_A_FIRMA — botón «Enviar a la firma del director»

- **Quién la lanza:** SECRETARIO
- **Pide confirmación antes de ejecutarse:** sí, con el texto «Va a enviar la resolución a la firma del director»
- **Datos que el usuario envía al lanzarla:**
  - sentido de la revisión
  - motivo del rechazo
  - qué hay que subsanar
- **Comprobaciones que deben pasar antes de dejarla ejecutarse:**
  - VAL-PENDIENTE_REVISION-ENVIAR_A_FIRMA-001 — El sentido de la revisión está elegido
    - mensaje: "Debe indicar el sentido de la revisión"
  - VAL-PENDIENTE_REVISION-ENVIAR_A_FIRMA-002 — El sentido de la revisión es «Aceptar la anulación» o «Rechazar la anulación»
    - mensaje: "Para pedir una subsanación use el botón «Pedir subsanación al alumno»"
  - VAL-PENDIENTE_REVISION-ENVIAR_A_FIRMA-003 — El motivo del rechazo está relleno
    - condición: el sentido de la revisión es «Rechazar la anulación»
    - mensaje: "Debe indicar el motivo del rechazo"
  - VAL-PENDIENTE_REVISION-ENVIAR_A_FIRMA-004 — El motivo del rechazo tiene entre 10 y 1000 caracteres
    - condición: el sentido de la revisión es «Rechazar la anulación»
    - mensaje: "El motivo del rechazo debe tener entre 10 y 1000 caracteres"
  - VAL-PENDIENTE_REVISION-ENVIAR_A_FIRMA-005 — Quien revisa pertenece al mismo centro que el expediente
    - mensaje: "Solo puede revisar solicitudes de su propio centro"
- **Qué produce la acción, en este orden:**
  1. RN-008 — Anotar la fecha de la revisión y quién la hizo
  2. RN-009 — Borrar el motivo del rechazo, para que no quede escrito cuando la anulación se acepta
     - condición: el sentido de la revisión es «Aceptar la anulación»
  3. RN-010 — Borrar el texto de qué hay que subsanar, si quedó escrito sin llegar a pedirse
     - condición: hay texto de qué hay que subsanar
  4. RN-011 — Borrar el motivo de la devolución del director, y con él la fecha de la devolución y quién la hizo, porque secretaría ya ha decidido de nuevo
     - condición: el expediente venía de una devolución del director
  5. RN-012 — Generar la resolución, todavía sin firmar, con el sentido («Se estima» o «Se desestima»), el motivo del rechazo si lo hay y la fecha de presentación, y guardarla en el expediente sustituyendo a la de un envío anterior
- **A qué estado lleva:** RESOLUCION / PENDIENTE_FIRMA_DIRECTOR

#### Acción SUBSANAR — botón «Pedir subsanación al alumno»

- **Quién la lanza:** SECRETARIO
- **Pide confirmación antes de ejecutarse:** sí, con el texto «Va a devolver la solicitud al alumno para que la corrija»
- **Datos que el usuario envía al lanzarla:**
  - sentido de la revisión
  - motivo del rechazo
  - qué hay que subsanar
- **Comprobaciones que deben pasar antes de dejarla ejecutarse:**
  - VAL-PENDIENTE_REVISION-SUBSANAR-001 — El sentido de la revisión es «Pedir subsanación»
    - mensaje: "Para pedir una subsanación elija el sentido «Pedir subsanación»"
  - VAL-PENDIENTE_REVISION-SUBSANAR-002 — El texto de qué hay que subsanar está relleno
    - mensaje: "Debe indicar al alumno qué tiene que subsanar"
  - VAL-PENDIENTE_REVISION-SUBSANAR-003 — El texto de qué hay que subsanar tiene entre 10 y 1000 caracteres
    - mensaje: "El texto de la subsanación debe tener entre 10 y 1000 caracteres"
  - VAL-PENDIENTE_REVISION-SUBSANAR-004 — Quien revisa pertenece al mismo centro que el expediente
    - mensaje: "Solo puede revisar solicitudes de su propio centro"
- **Qué produce la acción, en este orden:**
  1. RN-013 — Anotar la fecha de la revisión y quién la hizo
  2. RN-014 — Borrar el motivo del rechazo, si quedó escrito sin llegar a rechazarse
     - condición: hay motivo del rechazo
  3. RN-015 — Borrar el motivo de la devolución del director, y con él la fecha de la devolución y quién la hizo, si los había
     - condición: el expediente venía de una devolución del director
  4. RN-016 — Borrar la resolución sin firmar que hubiera de un envío anterior, porque los datos van a cambiar
     - condición: hay una resolución sin firmar guardada
- **A qué estado lleva:** SOLICITUD / DATOS_SOLICITUD

---

## Fase RESOLUCION — Resolución del centro

### Estado PENDIENTE_FIRMA_DIRECTOR — Pendiente de la firma del director

- **Quién actúa (tiene el turno):** DIRECTOR
- **Cierra el expediente:** no
- **Qué consulta el usuario en este estado:** los datos del alumno y del ciclo, en solo lectura; el sentido de la revisión y el motivo del rechazo elegidos por secretaría, con la fecha de la revisión y quién la hizo; la resolución sin firmar, incrustada en la pantalla, tal como quedará firmada; y la solicitud firmada por el alumno, como descarga.
- **Qué datos introduce el usuario en este estado:**
  - motivo de la devolución — texto libre en el que el director explica a secretaría por qué no firma la resolución; solo tiene sentido cuando la devuelve.
- **Qué acciones puede lanzar:** FIRMAR, DEVOLVER

#### Acción FIRMAR — botón «Firmar la resolución»

- **Quién la lanza:** DIRECTOR
- **Pide confirmación antes de ejecutarse:** sí, con el texto «Va a firmar la resolución. Una vez firmada se registrará de salida y el expediente quedará cerrado»
- **Datos que el usuario envía al lanzarla:** *(ninguno: la firma la pone el centro y el director solo la autoriza pulsando el botón)*
- **Comprobaciones que deben pasar antes de dejarla ejecutarse:**
  - VAL-PENDIENTE_FIRMA_DIRECTOR-FIRMAR-001 — Quien firma pertenece al mismo centro que el expediente
    - mensaje: "Solo puede firmar resoluciones de su propio centro"
  - VAL-PENDIENTE_FIRMA_DIRECTOR-FIRMAR-002 — El centro dispone de la firma del Director para firmar documentos
    - mensaje: "El centro no tiene configurada la firma del Director; avise al administrador"
- **Qué produce la acción, en este orden:**
  1. RN-017 — Anotar la fecha de la resolución (la fecha de hoy) y quién la firmó
  2. RN-022 — Volver a generar la resolución con la fecha de la resolución recién anotada, manteniendo el sentido, el motivo del rechazo y la fecha de presentación, para que el pie del documento lleve la fecha del día en que se firma
  3. RN-018 — Firmar la resolución con la firma del Director del centro y guardarla firmada en el expediente
  4. RN-019 — Dejar constancia de la salida de la resolución firmada, sin anexos; la resolución que queda en el expediente es la que devuelve el registro de salida, ya registrada (el registro de salida no produce ningún resguardo aparte)
  5. RN-023 — Borrar la resolución sin firmar, para que en el expediente cerrado quede una única versión de la resolución
  6. RN-020 — Borrar el motivo de la devolución, si quedó escrito sin llegar a devolverse
     - condición: hay motivo de la devolución
- **A qué estado lleva:**
  - si el sentido de la revisión vale «Aceptar la anulación» → RESOLUCION / ACEPTADA
  - si el sentido de la revisión vale «Rechazar la anulación» → RESOLUCION / RECHAZADA

#### Acción DEVOLVER — botón «Devolver a secretaría»

- **Quién la lanza:** DIRECTOR
- **Pide confirmación antes de ejecutarse:** sí, con el texto «Va a devolver la resolución a secretaría sin firmarla»
- **Datos que el usuario envía al lanzarla:**
  - motivo de la devolución
- **Comprobaciones que deben pasar antes de dejarla ejecutarse:**
  - VAL-PENDIENTE_FIRMA_DIRECTOR-DEVOLVER-001 — El motivo de la devolución está relleno
    - mensaje: "Debe indicar a secretaría por qué devuelve la resolución"
  - VAL-PENDIENTE_FIRMA_DIRECTOR-DEVOLVER-002 — El motivo de la devolución tiene entre 10 y 1000 caracteres
    - mensaje: "El motivo de la devolución debe tener entre 10 y 1000 caracteres"
  - VAL-PENDIENTE_FIRMA_DIRECTOR-DEVOLVER-003 — Quien devuelve pertenece al mismo centro que el expediente
    - mensaje: "Solo puede devolver resoluciones de su propio centro"
- **Qué produce la acción, en este orden:**
  1. RN-021 — Anotar la fecha de la devolución y quién la hizo
- **A qué estado lleva:** REVISION / PENDIENTE_REVISION

### Estado ACEPTADA — Anulación aceptada

- **Quién actúa (tiene el turno):** *(ninguno: nadie puede hacer nada aquí)*
- **Cierra el expediente:** sí
- **Qué consulta el usuario en este estado:** los datos del alumno y del ciclo, la fecha de presentación (desde la que surte efecto la anulación), la fecha de la resolución, la resolución firmada y registrada de salida, y la solicitud firmada y el justificante de presentación como descarga.
- **Qué datos introduce el usuario en este estado:** *(ninguno: el estado es de solo consulta)*
- **Qué acciones puede lanzar:** *(ninguna)*

### Estado RECHAZADA — Anulación rechazada

- **Quién actúa (tiene el turno):** *(ninguno: nadie puede hacer nada aquí)*
- **Cierra el expediente:** sí
- **Qué consulta el usuario en este estado:** los datos del alumno y del ciclo, el motivo del rechazo, la fecha de la resolución, la resolución firmada y registrada de salida, y la solicitud firmada y el justificante de presentación como descarga.
- **Qué datos introduce el usuario en este estado:** *(ninguno: el estado es de solo consulta)*
- **Qué acciones puede lanzar:** *(ninguna)*

---

## Tabla de transiciones

| Estado de partida | Acción | Condición | Estado siguiente | Qué produce |
|---|---|---|---|---|
| *(el expediente se crea)* | — | — | SOLICITUD / DATOS_SOLICITUD | Los datos personales del alumno, sus datos de contacto precargados, el curso académico y el centro |
| SOLICITUD / DATOS_SOLICITUD | CONTINUAR | — | SOLICITUD / PENDIENTE_FIRMA | Lugar y fecha de la solicitud; se genera la solicitud de anulación |
| SOLICITUD / DATOS_SOLICITUD | *(borrar el expediente)* | — | *(el expediente desaparece)* | *(nada)* |
| SOLICITUD / PENDIENTE_FIRMA | VOLVER | — | SOLICITUD / DATOS_SOLICITUD | *(nada)* |
| SOLICITUD / PENDIENTE_FIRMA | PRESENTAR | — | REVISION / PENDIENTE_REVISION | Constancia de entrada de la solicitud firmada, justificante de presentación, fecha de presentación; se limpian la subsanación y la revisión anteriores |
| REVISION / PENDIENTE_REVISION | ENVIAR_A_FIRMA | sentido de la revisión = «Aceptar la anulación» o «Rechazar la anulación» | RESOLUCION / PENDIENTE_FIRMA_DIRECTOR | Fecha y autor de la revisión; se limpian residuos; se genera la resolución sin firmar |
| REVISION / PENDIENTE_REVISION | SUBSANAR | sentido de la revisión = «Pedir subsanación» | SOLICITUD / DATOS_SOLICITUD | Fecha y autor de la revisión; se limpian el motivo del rechazo, la devolución y la resolución sin firmar |
| RESOLUCION / PENDIENTE_FIRMA_DIRECTOR | FIRMAR | sentido de la revisión = «Aceptar la anulación» | RESOLUCION / ACEPTADA | Fecha y autor de la resolución; resolución regenerada con esa fecha, firmada por el Director y registrada de salida; se borra la versión sin firmar |
| RESOLUCION / PENDIENTE_FIRMA_DIRECTOR | FIRMAR | sentido de la revisión = «Rechazar la anulación» | RESOLUCION / RECHAZADA | Fecha y autor de la resolución; resolución regenerada con esa fecha, firmada por el Director y registrada de salida; se borra la versión sin firmar |
| RESOLUCION / PENDIENTE_FIRMA_DIRECTOR | DEVOLVER | — | REVISION / PENDIENTE_REVISION | Fecha y autor de la devolución, y el motivo de la devolución |

## Datos que rellena el sistema

- CC-001 — nombre, apellidos y documento de identidad del alumno
  - momento: al crear el expediente
  - sobreescribible: nunca
  - cálculo: se copian del alumno que crea el expediente, para que queden congelados en el expediente aunque después cambien en su ficha
- CC-002 — NIA, dirección, teléfono, población y código postal (valor propuesto)
  - momento: al crear el expediente
  - sobreescribible: CREADOR
  - cálculo: se copian de la ficha del alumno los que consten; el alumno los revisa y los puede corregir al rellenar la solicitud
- CC-003 — curso académico
  - momento: al crear el expediente
  - sobreescribible: nunca
  - cálculo: el curso académico en vigor del centro, presentado como «2024/2025»
- CC-004 — nombre y localidad del centro
  - momento: al crear el expediente
  - sobreescribible: nunca
  - cálculo: se copian del centro del expediente; la localidad queda vacía si el centro no la tiene informada
- CC-005 — lugar y fecha de la solicitud
  - momento: al lanzar la acción CONTINUAR desde SOLICITUD / DATOS_SOLICITUD
  - sobreescribible: nunca
  - cálculo: el lugar es la localidad del centro y la fecha es la del día en que se genera la solicitud; se vuelven a anotar cada vez que el alumno continúa
- CC-006 — solicitud de anulación generada (sin firmar)
  - momento: al lanzar la acción CONTINUAR desde SOLICITUD / DATOS_SOLICITUD
  - sobreescribible: nunca
  - cálculo: se genera con los datos del alumno, del centro y del ciclo; se vuelve a generar cada vez que el alumno continúa, de modo que siempre refleja los últimos datos
- CC-007 — fecha y hora de presentación
  - momento: al lanzar la acción PRESENTAR desde SOLICITUD / PENDIENTE_FIRMA
  - sobreescribible: nunca
  - cálculo: el momento en que se deja constancia de la entrada; si hubo subsanación, el de la última presentación
- CC-008 — justificante de presentación
  - momento: al lanzar la acción PRESENTAR desde SOLICITUD / PENDIENTE_FIRMA
  - sobreescribible: nunca
  - cálculo: es el resguardo que produce la constancia de entrada; si hubo subsanación, el de la última presentación sustituye al anterior
- CC-009 — fecha de la revisión y quién la hizo
  - momento: al lanzar la acción ENVIAR_A_FIRMA o SUBSANAR desde REVISION / PENDIENTE_REVISION
  - sobreescribible: nunca
  - cálculo: la fecha del día y el nombre de la administrativa que pulsó el botón
- CC-010 — resolución sin firmar
  - momento: al lanzar la acción ENVIAR_A_FIRMA desde REVISION / PENDIENTE_REVISION
  - sobreescribible: nunca
  - cálculo: se genera con los datos del alumno y del ciclo, el sentido de la revisión, el motivo del rechazo si lo hay y la fecha de presentación; se vuelve a generar en cada envío a la firma y, por última vez, al firmar (con la fecha de la resolución); se borra una vez guardada la resolución firmada
- CC-011 — fecha de la devolución y quién la hizo
  - momento: al lanzar la acción DEVOLVER desde RESOLUCION / PENDIENTE_FIRMA_DIRECTOR
  - sobreescribible: nunca
  - cálculo: la fecha del día y el nombre del director que pulsó el botón
- CC-012 — fecha de la resolución y quién la firmó
  - momento: al lanzar la acción FIRMAR desde RESOLUCION / PENDIENTE_FIRMA_DIRECTOR
  - sobreescribible: nunca
  - cálculo: la fecha del día y el nombre del director que pulsó el botón
- CC-013 — resolución firmada
  - momento: al lanzar la acción FIRMAR desde RESOLUCION / PENDIENTE_FIRMA_DIRECTOR
  - sobreescribible: nunca
  - cálculo: la resolución regenerada con la fecha de la resolución, con la firma del Director del centro estampada, tal como la devuelve el registro de salida (ya registrada)
- CC-014 — grado o nivel del ciclo (el texto que rellena «en el Ciclo Formativo de Grado ____» en la solicitud y la referencia de la matrícula en la resolución)
  - momento: cada vez que se consulta (al generar la solicitud y la resolución)
  - sobreescribible: nunca
  - cálculo: se toma del ciclo elegido en el catálogo: si su grado es «Ciclo formativo», el nombre corto de su nivel («Básico», «Medio» o «Superior»); si su grado es «Curso de especialización», el texto «Curso de especialización»
