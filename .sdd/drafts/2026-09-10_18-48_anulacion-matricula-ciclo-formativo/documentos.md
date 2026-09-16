# Documentos del trámite

## Resumen

| Documento | Cuándo se genera | Quién lo firma | Se registra |
|---|---|---|---|
| Solicitud de anulación de matrícula | al lanzar CONTINUAR desde SOLICITUD / DATOS_SOLICITUD | el propio alumno, en su equipo, con su certificado digital, al presentarla | de entrada, al presentarla |
| Resolución de la solicitud de anulación de matrícula | al lanzar ENVIAR_A_FIRMA desde REVISION / PENDIENTE_REVISION (sin firmar); se firma al lanzar FIRMAR desde RESOLUCION / PENDIENTE_FIRMA_DIRECTOR | el centro, con la firma del Director | de salida, al firmarla |

Además, el expediente guarda el **justificante de presentación** que produce el registro de entrada del centro al presentar la solicitud. No es un documento que este trámite diseñe: es el resguardo estándar de la plataforma, y aquí solo se dice dónde se muestra (en las pantallas de solo consulta desde REVISION / PENDIENTE_REVISION en adelante, incrustado; en los estados cerrados, como descarga).

---

## Documento: Solicitud de anulación de matrícula

- **Qué es:** el impreso oficial con el que el alumno pide formalmente dejar sin efecto su matrícula en un ciclo formativo. Sigue el modelo del anexo oficial de la administración educativa, sin la zona reservada al sello de registro de entrada (el justificante de registro lo produce la plataforma aparte) y en un solo ejemplar. Es el documento que el alumno firma, con el que queda constancia de que presentó su petición y el que lee secretaría para revisarla.
- **Cuándo se genera:** al lanzar la acción CONTINUAR desde el estado SOLICITUD / DATOS_SOLICITUD. Se vuelve a generar cada vez que el alumno vuelve atrás y continúa de nuevo, y cada vez que corrige tras una subsanación, de modo que siempre refleja los últimos datos introducidos.
- **Quién lo firma y dónde:** el propio alumno, en su equipo, con su certificado digital, al lanzar la acción PRESENTAR desde SOLICITUD / PENDIENTE_FIRMA. El sistema comprueba después que la firma es válida, que es una sola, que el certificado es de confianza, que no ha alterado el texto del documento y que corresponde al documento de identidad del alumno.
- **Dónde se estampa la firma:** en la única página, en la zona inferior del cuerpo del impreso, a la derecha, bajo la línea de lugar y fecha y junto al texto «Firma:».
- **Se registra:** de entrada, al presentarla. El documento principal del registro es esta solicitud ya firmada, sin anexos. Cada presentación (la primera y cada una posterior a una subsanación) produce su propio registro de entrada.
- **Qué datos del expediente aparecen en él:**
  - apellidos — en el bloque A «Datos de identificación del alumno/a», como «Apellidos»
  - nombre — en el bloque A, como «Nombre»
  - NIA — en el bloque A, como «NIA (1)»
  - documento de identidad — en el bloque A, como «DNI/NIE (2)»
  - dirección — en el bloque A, como «Dirección»
  - teléfono — en el bloque A, como «Teléfono»
  - población — en el bloque A, como «Población»
  - provincia — en el bloque A, como «Provincia»
  - código postal — en el bloque A, como «CP»
  - curso académico — en el bloque B «Expone», rellenando «Que en el curso académico ____»
  - nombre del centro — en el bloque B, rellenando «se ha matriculado en el centro ____»
  - localidad del centro — en el bloque B, rellenando «localidad ____»; queda en blanco si el centro no la tiene informada
  - grado o nivel del ciclo — en el bloque B, rellenando «en el Ciclo Formativo de Grado ____»: si el ciclo elegido es de grado «Ciclo formativo», con «Básico», «Medio» o «Superior» según su nivel; si es de grado «Curso de especialización», con el texto «Curso de especialización». Se toma del ciclo elegido, no lo escribe el alumno.
  - ciclo — en el bloque B, rellenando «denominado ____» con el nombre del ciclo elegido
  - lugar de la solicitud — en la línea de lugar y fecha, antes de la coma; queda en blanco si el centro no tiene localidad informada
  - fecha de la solicitud — en la línea de lugar y fecha, como «__ de ____ de ____» con el día, el mes en letras y el año
  - nombre del centro — de nuevo en el pie, rellenando «DIRECTOR/A DEL ____»
- **Textos fijos que lleva impresos:** la cabecera institucional, la referencia «ANEXO VII», el título «Solicitud de anulación de matrícula en ciclo formativo», los rótulos de los bloques A «Datos de identificación del alumno/a», B «Expone» y C «Solicita», el texto del bloque C «Que por el presente escrito se considere manifestado mi deseo de anular dicha matrícula y, por tanto, dejarla sin efecto a partir de la fecha en que formalizo esta petición», la línea de lugar y fecha, el rótulo «Firma:», las notas «(1) NIA: Número de identificación del alumno/a» y «(2) DNI / NIE: Documento nacional de identidad – Número de identificación de extranjeros o documento legalmente establecido», la cláusula de protección de datos actualizada a la normativa vigente de protección de datos (la misma que ya usan los demás documentos de la plataforma) y el pie «DIRECTOR/A DEL».
- **Idiomas:** se emite en valenciano y en castellano, con los dos textos en cada rótulo, como el impreso oficial.
- **A quién se le muestra y dónde:** al alumno, incrustada a tamaño grande, en su pantalla del estado SOLICITUD / PENDIENTE_FIRMA, para que la revise antes de firmarla; como descarga (sin firmar) en la pantalla de solo consulta de ese mismo estado; a la administrativa, ya firmada e incrustada, en su pantalla del estado REVISION / PENDIENTE_REVISION; ya firmada y como descarga, en las pantallas de solo consulta de REVISION / PENDIENTE_REVISION y RESOLUCION / PENDIENTE_FIRMA_DIRECTOR, en la pantalla del director y en las de RESOLUCION / ACEPTADA y RESOLUCION / RECHAZADA.

---

## Documento: Resolución de la solicitud de anulación de matrícula

- **Qué es:** la resolución con la que el centro comunica al alumno si su matrícula queda sin efecto (se estima la solicitud) o no (se desestima, con el motivo). Es el documento que el alumno puede usar para acreditar la respuesta del centro y, cuando se estima, para acreditar que su matrícula está anulada desde la fecha de presentación.
- **Cuándo se genera:** al lanzar la acción ENVIAR_A_FIRMA desde el estado REVISION / PENDIENTE_REVISION, todavía sin firmar; se vuelve a generar en cada envío a la firma, de modo que refleja la última decisión de secretaría. Al lanzar la acción FIRMAR desde RESOLUCION / PENDIENTE_FIRMA_DIRECTOR se regenera por última vez con la fecha de la resolución (la del día en que el director firma), con el mismo sentido, motivo y fecha de presentación, y se firma; la versión sin firmar se borra entonces. **No** se genera cuando secretaría pide una subsanación.
- **Quién lo firma y dónde:** el centro, con la firma institucional del Director. La pone el servidor con el certificado custodiado de ese cargo cuando el director pulsa «Firmar la resolución»; el director no interviene en la firma más que autorizándola con el botón.
- **Dónde se estampa la firma:** en la única página, en el centro de la mitad inferior, bajo el texto «El director / la directora del centro».
- **Se registra:** de salida, al firmarla, con esta resolución firmada como documento principal y sin anexos. La resolución que queda en el expediente y se muestra en ACEPTADA y RECHAZADA es la que devuelve el registro de salida, ya registrada; el registro de salida no produce ningún resguardo aparte.
- **Qué datos del expediente aparecen en él:**
  - apellidos y nombre — en el encabezado, como destinatario
  - documento de identidad — junto al nombre
  - NIA — junto al documento de identidad
  - curso académico — en el cuerpo, como referencia de la matrícula
  - nombre del centro — en el cuerpo y en la cabecera
  - ciclo, con su grado o nivel — en el cuerpo, como referencia de la matrícula que se pide anular (el nombre del ciclo y, delante, «Ciclo Formativo de Grado Básico/Medio/Superior» o «Curso de especialización», según el ciclo)
  - fecha y hora de presentación — en el cuerpo, como fecha de la solicitud y, si se estima, como fecha desde la que la matrícula queda sin efecto
  - sentido de la revisión — en el cuerpo, como la fórmula «Se estima la solicitud y la matrícula queda sin efecto a partir de la fecha de presentación» o «Se desestima la solicitud»
  - motivo del rechazo — en el cuerpo, como texto libre precedido de «Por el siguiente motivo:»; solo cuando se desestima
  - fecha de la resolución — en el pie, como fecha de la resolución (la del día en que el director la firma)
- **Textos fijos que lleva impresos:** la cabecera con el nombre del centro, el título «Resolución de la solicitud de anulación de matrícula en ciclo formativo», la fórmula de vistos («Vista la solicitud presentada por…»), la fórmula de estimación o de desestimación, el pie de recurso con el plazo y el órgano ante el que se puede recurrir, y el rótulo «El director / la directora del centro» sobre el recuadro de firma.
- **Idiomas:** se emite en valenciano y en castellano.
- **A quién se le muestra y dónde:** al director, sin firmar e incrustada a tamaño grande, en su pantalla del estado RESOLUCION / PENDIENTE_FIRMA_DIRECTOR; a nadie más mientras está sin firmar; ya firmada e incrustada, a cualquiera con acceso al expediente en las pantallas de RESOLUCION / ACEPTADA y RESOLUCION / RECHAZADA.

---

## Trozos comunes a varios documentos

- **Cabecera institucional** — el logotipo y el nombre de la administración educativa y del centro — lo usan: Solicitud de anulación de matrícula, Resolución de la solicitud de anulación de matrícula
- **Identificación del alumno** — apellidos, nombre, documento de identidad y NIA — lo usan: Solicitud de anulación de matrícula, Resolución de la solicitud de anulación de matrícula
- **Referencia de la matrícula** — curso académico, centro y ciclo con su grado o nivel — lo usan: Solicitud de anulación de matrícula, Resolución de la solicitud de anulación de matrícula
- **Cláusula de protección de datos** — el texto legal sobre el tratamiento de los datos personales, en valenciano y en castellano — lo usa: Solicitud de anulación de matrícula
