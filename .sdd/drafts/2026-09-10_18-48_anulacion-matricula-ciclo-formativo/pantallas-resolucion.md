# Pantallas de la fase RESOLUCION — Resolución del centro

## Estado PENDIENTE_FIRMA_DIRECTOR

### Pantalla: PENDIENTE_FIRMA_DIRECTOR — perfil DIRECTOR

- **Quién la ve:** el director del centro, mientras el expediente está en RESOLUCION / PENDIENTE_FIRMA_DIRECTOR.
- **Qué ve el usuario, bloque a bloque:**
  - **Datos de identificación del alumno** — apellidos, nombre, NIA, documento de identidad
  - **Matrícula que se anula** — curso académico, centro, ciclo con su grado o nivel, fecha y hora de presentación
  - **Decisión de secretaría** — sentido de la revisión, motivo del rechazo, fecha de la revisión y quién la hizo
  - **Resolución a firmar** — la resolución sin firmar, tal y como quedará firmada
  - **Devolución** — motivo de la devolución
- **Qué puede rellenar:** motivo de la devolución
- **Qué solo puede consultar:** todos los datos del alumno y de la matrícula, la fecha y la hora de presentación, y el sentido, el motivo, la fecha y el autor de la revisión
- **Documentos que se le muestran:** la resolución sin firmar, incrustada en la pantalla a tamaño grande; la solicitud firmada por el alumno y el justificante de presentación, como descarga.
- **Aviso permanente en pantalla:** «Revise la resolución antes de firmarla. Al firmarla se registrará de salida y el expediente quedará cerrado. Si no está conforme, devuélvala a secretaría indicando el motivo»
- **Botones:**
  - **«Firmar la resolución»** — lanza la acción FIRMAR. Pide confirmación con el texto «Va a firmar la resolución. Una vez firmada se registrará de salida y el expediente quedará cerrado».
  - **«Devolver a secretaría»** — lanza la acción DEVOLVER. Pide confirmación con el texto «Va a devolver la resolución a secretaría sin firmarla».

#### Reglas de pantalla

- RUI-PENDIENTE_FIRMA_DIRECTOR-DIRECTOR-001 — La resolución sin firmar se muestra incrustada y ocupando la mayor parte de la pantalla, porque el director tiene que leerla entera antes de firmarla
  - disparador: al abrir la pantalla
  - condición: Siempre
- RUI-PENDIENTE_FIRMA_DIRECTOR-DIRECTOR-002 — El motivo del rechazo solo se muestra cuando el sentido de la revisión es «Rechazar la anulación»
  - disparador: al abrir la pantalla
  - condición: el sentido de la revisión es «Rechazar la anulación»
- RUI-PENDIENTE_FIRMA_DIRECTOR-DIRECTOR-003 — El bloque «Devolución» muestra el motivo de la devolución como texto libre, vacío al abrir la pantalla, con la indicación «Rellene el motivo solo si va a devolver la resolución»
  - disparador: al abrir la pantalla
  - condición: Siempre
- RUI-PENDIENTE_FIRMA_DIRECTOR-DIRECTOR-004 — Los datos de identificación del alumno, los de la matrícula que se anula, la fecha y la hora de presentación y la decisión de secretaría (sentido, motivo del rechazo, fecha y autor de la revisión) se muestran en solo lectura: el director solo puede escribir el motivo de la devolución
  - disparador: al abrir la pantalla
  - condición: Siempre
- RUI-PENDIENTE_FIRMA_DIRECTOR-DIRECTOR-005 — El motivo de la devolución no se marca visualmente como obligatorio, porque solo se exige al pulsar «Devolver a secretaría» y no al firmar; la indicación «Rellene el motivo solo si va a devolver la resolución» es la única señal que ve el director
  - disparador: continuo
  - condición: Siempre
- RUI-PENDIENTE_FIRMA_DIRECTOR-DIRECTOR-006 — Junto a la resolución sin firmar se muestra la indicación «La fecha de la resolución que llevará el documento firmado será la del día en que lo firme»
  - disparador: al abrir la pantalla
  - condición: Siempre
- RUI-PENDIENTE_FIRMA_DIRECTOR-DIRECTOR-007 — La fecha y la hora de presentación se muestran acompañadas de la indicación «Si firma la resolución, la anulación surtirá efecto desde esta fecha»
  - disparador: continuo
  - condición: el sentido de la revisión es «Aceptar la anulación»

### Pantalla: PENDIENTE_FIRMA_DIRECTOR — resto de perfiles (solo consulta)

- **Quién la ve:** cualquier perfil con acceso al expediente distinto del DIRECTOR (el propio alumno, la administrativa, el secretario del centro, el supervisor o el administrador), mientras el expediente está en RESOLUCION / PENDIENTE_FIRMA_DIRECTOR.
- **Qué ve el usuario, bloque a bloque:**
  - **Datos de identificación del alumno** — apellidos, nombre, NIA, documento de identidad, dirección, teléfono, población, provincia, código postal
  - **Matrícula que se anula** — curso académico, centro, ciclo con su grado o nivel
  - **Presentación** — la fecha y la hora de presentación y el justificante de presentación
  - **Solicitud firmada** — la solicitud de anulación firmada por el alumno
- **Qué puede rellenar:** *(nada: toda la pantalla es de solo consulta)*
- **Documentos que se le muestran:** el justificante de presentación, incrustado en la pantalla; la solicitud firmada, como descarga.
- **Aviso permanente en pantalla:** «La resolución está pendiente de la firma del director»
- **Botones:**
  - **«Salir»** — cierra el expediente y vuelve al listado, sin cambiar nada.

#### Reglas de pantalla

- RUI-PENDIENTE_FIRMA_DIRECTOR-GENERICA-001 — El sentido de la revisión, el motivo del rechazo, la fecha y el autor de la revisión y la resolución sin firmar no se muestran: la decisión de secretaría no se enseña a nadie hasta que el director firma la resolución
  - disparador: al abrir la pantalla
  - condición: Siempre

---

## Estado ACEPTADA

### Pantalla: ACEPTADA — resto de perfiles (solo consulta)

- **Quién la ve:** cualquier perfil con acceso al expediente, incluido el propio alumno, mientras el expediente está en RESOLUCION / ACEPTADA.
- **Qué ve el usuario, bloque a bloque:**
  - **Datos de identificación del alumno** — apellidos, nombre, NIA, documento de identidad, dirección, teléfono, población, provincia, código postal
  - **Matrícula que se anula** — curso académico, centro, ciclo con su grado o nivel
  - **Presentación** — la fecha y la hora de presentación (desde la que surte efecto la anulación) y el justificante de presentación
  - **Resolución** — la fecha de la resolución, quién la firmó y la resolución firmada
- **Qué puede rellenar:** *(nada: toda la pantalla es de solo consulta)*
- **Documentos que se le muestran:** la resolución firmada, incrustada en la pantalla; la solicitud firmada y el justificante de presentación, como descarga.
- **Aviso permanente en pantalla:** «El expediente está cerrado»
- **Botones:**
  - **«Salir»** — cierra el expediente y vuelve al listado, sin cambiar nada.

#### Reglas de pantalla

- RUI-ACEPTADA-GENERICA-001 — La resolución firmada se muestra incrustada, porque es el documento que acredita la anulación
  - disparador: al abrir la pantalla
  - condición: Siempre
- RUI-ACEPTADA-GENERICA-002 — La fecha y la hora de presentación se muestran acompañadas de la indicación «La anulación de la matrícula surte efecto desde esta fecha»
  - disparador: al abrir la pantalla
  - condición: Siempre
- RUI-ACEPTADA-GENERICA-003 — El sentido de la revisión, la fecha y el autor de la revisión y el motivo de la devolución del director no se muestran tampoco con el expediente cerrado: la respuesta del centro se lee en la resolución firmada, y la devolución del director es una incidencia interna entre secretaría y dirección
  - disparador: al abrir la pantalla
  - condición: Siempre

---

## Estado RECHAZADA

### Pantalla: RECHAZADA — resto de perfiles (solo consulta)

- **Quién la ve:** cualquier perfil con acceso al expediente, incluido el propio alumno, mientras el expediente está en RESOLUCION / RECHAZADA.
- **Qué ve el usuario, bloque a bloque:**
  - **Datos de identificación del alumno** — apellidos, nombre, NIA, documento de identidad, dirección, teléfono, población, provincia, código postal
  - **Matrícula que se anula** — curso académico, centro, ciclo con su grado o nivel
  - **Presentación** — la fecha y la hora de presentación y el justificante de presentación
  - **Resolución** — el motivo del rechazo, la fecha de la resolución, quién la firmó y la resolución firmada
- **Qué puede rellenar:** *(nada: toda la pantalla es de solo consulta)*
- **Documentos que se le muestran:** la resolución firmada, incrustada en la pantalla; la solicitud firmada y el justificante de presentación, como descarga.
- **Aviso permanente en pantalla:** «El expediente está cerrado»
- **Botones:**
  - **«Salir»** — cierra el expediente y vuelve al listado, sin cambiar nada.

#### Reglas de pantalla

- RUI-RECHAZADA-GENERICA-001 — La resolución firmada se muestra incrustada, porque es el documento que acredita la respuesta del centro
  - disparador: al abrir la pantalla
  - condición: Siempre
- RUI-RECHAZADA-GENERICA-002 — El sentido de la revisión, la fecha y el autor de la revisión y el motivo de la devolución del director no se muestran tampoco con el expediente cerrado; de la decisión de secretaría solo se enseña el motivo del rechazo, que es el que figura en la resolución firmada
  - disparador: al abrir la pantalla
  - condición: Siempre
