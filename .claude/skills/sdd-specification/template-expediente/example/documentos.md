# Documentos del trámite

## Resumen

| Documento | Cuándo se genera | Quién lo firma | Se registra |
|---|---|---|---|
| Solicitud de préstamo | al lanzar CONTINUAR desde SOLICITUD / DATOS_PETICION | el propio alumno, en su equipo, con su certificado digital, al presentarla | de entrada, al presentarla |
| Resolución de denegación | al lanzar VALORAR desde VALORACION / PENDIENTE_VALORACION, solo cuando se deniega | el centro, con la firma del Director | de salida |
| Acta de entrega | al lanzar ENTREGAR desde ENTREGA / PENDIENTE_ENTREGA | el centro, con la firma del Secretario | no se registra |

---

## Documento: Solicitud de préstamo

- **Qué es:** el impreso con el que el alumno pide formalmente el préstamo. Es el documento que el alumno firma y con el que queda constancia de que presentó su petición; es también el que lee la jefatura de estudios para valorarla.
- **Cuándo se genera:** al lanzar la acción CONTINUAR desde el estado SOLICITUD / DATOS_PETICION. Se vuelve a generar cada vez que el alumno vuelve atrás y continúa de nuevo, de modo que siempre refleja los últimos datos introducidos.
- **Quién lo firma y dónde:** el propio alumno, en su equipo, con su certificado digital, al lanzar la acción PRESENTAR desde SOLICITUD / PENDIENTE_FIRMA. El sistema comprueba después que la firma es válida, que es una sola, que el certificado es de confianza, que no ha alterado el texto del documento y que corresponde al documento de identidad del alumno solicitante.
- **Dónde se estampa la firma:** en la esquina inferior derecha de la única página, bajo el texto «Firma del solicitante».
- **Se registra:** de entrada, al presentarla. El documento principal del registro es esta solicitud ya firmada, y va con el documento acreditativo que el alumno adjuntó como único anexo.
- **Qué datos del expediente aparecen en él:**
  - nombre y apellidos del solicitante — encabezando el impreso
  - documento de identidad del solicitante — junto al nombre
  - curso académico — en la cabecera, como «Curso»
  - equipo solicitado — como dos casillas, «Portátil» y «Tableta», de las que se marca la elegida
  - motivo de la petición — como tres casillas, de las que se marca la elegida
  - explicación del motivo — como texto libre bajo las casillas; queda en blanco si el motivo elegido no es «Otros»
  - fecha prevista de devolución — como fecha
- **Textos fijos que lleva impresos:** el encabezado con el nombre del centro, el título «Solicitud de préstamo de equipo informático», el compromiso de devolución en la fecha indicada y en el mismo estado de conservación, la cláusula de protección de datos y el pie con el espacio para la firma.
- **Idiomas:** se emite en castellano y en valenciano.
- **A quién se le muestra y dónde:** al alumno, incrustado a tamaño grande, en la pantalla del estado SOLICITUD / PENDIENTE_FIRMA, para que lo revise antes de firmarlo; a la jefatura de estudios, ya firmado e incrustado, en la pantalla del estado VALORACION / PENDIENTE_VALORACION; a la administrativa, como descarga, en la pantalla del estado ENTREGA / PENDIENTE_ENTREGA.

---

## Documento: Resolución de denegación

- **Qué es:** la resolución motivada con la que el centro comunica al alumno que no le concede el préstamo. Es el documento que el alumno puede usar para acreditar la respuesta del centro.
- **Cuándo se genera:** al lanzar la acción VALORAR desde el estado VALORACION / PENDIENTE_VALORACION, **solo** cuando el sentido de la valoración es «Denegar el préstamo». Si se concede o se pide una corrección no se genera nada.
- **Quién lo firma y dónde:** el centro, con la firma del Director. La pone el servidor con el certificado custodiado de ese cargo y el usuario no interviene.
- **Dónde se estampa la firma:** en el centro de la mitad inferior de la única página, bajo el texto «El Director».
- **Se registra:** de salida, con esta resolución como documento principal y sin anexos.
- **Qué datos del expediente aparecen en él:**
  - nombre y apellidos del solicitante — en el encabezado, como destinatario
  - documento de identidad del solicitante — junto al nombre
  - curso académico — en la cabecera
  - equipo solicitado — en el cuerpo, como referencia de lo que se pedía
  - motivo de la denegación — en el cuerpo, como texto libre, precedido de «Por el siguiente motivo:»
  - fecha de la valoración — en el pie, como fecha de la resolución
- **Textos fijos que lleva impresos:** el encabezado con el nombre del centro, el título «Resolución de la solicitud de préstamo de equipo informático», la fórmula de denegación y el pie de recurso con el plazo y el órgano ante el que se puede recurrir.
- **Idiomas:** se emite en castellano y en valenciano.
- **A quién se le muestra y dónde:** a cualquiera con acceso al expediente, incrustado, en la pantalla del estado VALORACION / DENEGADO.

---

## Documento: Acta de entrega

- **Qué es:** el acta que deja constancia de qué equipo concreto se entregó, en qué estado y en qué fecha. Es lo que el centro conserva para saber qué equipo tiene cada alumno.
- **Cuándo se genera:** al lanzar la acción ENTREGAR desde el estado ENTREGA / PENDIENTE_ENTREGA.
- **Quién lo firma y dónde:** el centro, con la firma del Secretario. La pone el servidor con el certificado custodiado de ese cargo y el usuario no interviene. El alumno no firma el acta.
- **Dónde se estampa la firma:** en la esquina inferior izquierda de la única página, bajo el texto «Por el centro».
- **Se registra:** no se registra. Es un documento interno del centro, no una comunicación oficial con el alumno.
- **Qué datos del expediente aparecen en él:**
  - nombre y apellidos del solicitante — encabezando el acta
  - documento de identidad del solicitante — junto al nombre
  - curso académico — en la cabecera
  - equipo solicitado — como el tipo de equipo entregado
  - número de inventario del equipo entregado — destacado, como identificación del equipo
  - observaciones del estado del equipo — como texto libre
  - fecha prevista de devolución — en el cuerpo, como compromiso del alumno
  - fecha de entrega y quién la registró — en el pie
- **Textos fijos que lleva impresos:** el encabezado con el nombre del centro, el título «Acta de entrega de equipo informático», el compromiso de devolución en la fecha y el estado indicados y el pie con el espacio para la firma del centro.
- **Idiomas:** se emite solo en castellano.
- **A quién se le muestra y dónde:** a cualquiera con acceso al expediente, incrustado, en la pantalla del estado ENTREGA / ENTREGADO.

---

## Trozos comunes a varios documentos

- **Cabecera del centro** — el logotipo, el nombre y la dirección del centro, más el curso académico — lo usan: Solicitud de préstamo, Resolución de denegación, Acta de entrega
- **Identificación del solicitante** — nombre y apellidos y documento de identidad del alumno — lo usan: Solicitud de préstamo, Resolución de denegación, Acta de entrega
- **Cláusula de protección de datos** — el texto legal sobre el tratamiento de los datos personales — lo usan: Solicitud de préstamo, Acta de entrega
