# Pantallas de la fase REVISION — Revisión de la secretaría

## Estado PENDIENTE_REVISION

### Pantalla: PENDIENTE_REVISION — perfil SECRETARIO

- **Quién la ve:** la administrativa del centro, mientras el expediente está en REVISION / PENDIENTE_REVISION.
- **Qué ve el usuario, bloque a bloque:**
  - **Devuelto por el director** — el motivo de la devolución, la fecha de la devolución y quién la hizo
  - **Datos de identificación del alumno** — apellidos, nombre, NIA, documento de identidad, dirección, teléfono, población, provincia, código postal
  - **Matrícula que se anula** — curso académico, centro, ciclo con su grado o nivel
  - **Presentación** — la fecha y la hora de presentación y el justificante de presentación
  - **Solicitud firmada** — la solicitud de anulación firmada por el alumno
  - **Revisión** — sentido de la revisión, motivo del rechazo, qué hay que subsanar
- **Qué puede rellenar:** sentido de la revisión, motivo del rechazo, qué hay que subsanar
- **Qué solo puede consultar:** todos los datos del alumno y de la matrícula, la fecha y la hora de presentación, y el motivo, la fecha y el autor de la devolución
- **Documentos que se le muestran:** la solicitud firmada, incrustada en la pantalla; el justificante de presentación, como descarga.
- **Aviso permanente en pantalla:** «Compruebe los datos de la solicitud. Si hay algún error, pida al alumno que los subsane; si no, decida si procede la anulación y envíe la resolución a la firma del director»
- **Botones:**
  - **«Enviar a la firma del director»** — lanza la acción ENVIAR_A_FIRMA. Pide confirmación con el texto «Va a enviar la resolución a la firma del director».
  - **«Pedir subsanación al alumno»** — lanza la acción SUBSANAR. Pide confirmación con el texto «Va a devolver la solicitud al alumno para que la corrija».

#### Reglas de pantalla

- RUI-PENDIENTE_REVISION-SECRETARIO-001 — El bloque «Devuelto por el director» solo se muestra cuando el director ha devuelto la resolución
  - disparador: continuo
  - condición: hay motivo de la devolución
- RUI-PENDIENTE_REVISION-SECRETARIO-002 — El sentido de la revisión se marca como obligatorio
  - disparador: continuo
  - condición: Siempre
- RUI-PENDIENTE_REVISION-SECRETARIO-003 — El motivo del rechazo solo se muestra cuando el sentido elegido es «Rechazar la anulación»
  - disparador: continuo
  - condición: el sentido de la revisión es «Rechazar la anulación»
- RUI-PENDIENTE_REVISION-SECRETARIO-004 — El motivo del rechazo se marca como obligatorio cuando el sentido elegido es «Rechazar la anulación»
  - disparador: continuo
  - condición: el sentido de la revisión es «Rechazar la anulación»
- RUI-PENDIENTE_REVISION-SECRETARIO-005 — El texto de qué hay que subsanar solo se muestra cuando el sentido elegido es «Pedir subsanación»
  - disparador: continuo
  - condición: el sentido de la revisión es «Pedir subsanación»
- RUI-PENDIENTE_REVISION-SECRETARIO-006 — El texto de qué hay que subsanar se marca como obligatorio cuando el sentido elegido es «Pedir subsanación»
  - disparador: continuo
  - condición: el sentido de la revisión es «Pedir subsanación»
- RUI-PENDIENTE_REVISION-SECRETARIO-009 — Al cambiar el sentido de la revisión a algo distinto de «Rechazar la anulación», el motivo del rechazo que hubiera escrito se borra de la pantalla
  - disparador: al cambiar el sentido de la revisión
  - condición: el sentido de la revisión deja de ser «Rechazar la anulación»
- RUI-PENDIENTE_REVISION-SECRETARIO-010 — Al cambiar el sentido de la revisión a algo distinto de «Pedir subsanación», el texto de qué hay que subsanar que hubiera escrito se borra de la pantalla
  - disparador: al cambiar el sentido de la revisión
  - condición: el sentido de la revisión deja de ser «Pedir subsanación»
- RUI-PENDIENTE_REVISION-SECRETARIO-011 — La solicitud firmada se muestra incrustada, porque la administrativa tiene que cotejar sus datos
  - disparador: al abrir la pantalla
  - condición: Siempre
- RUI-PENDIENTE_REVISION-SECRETARIO-012 — El sentido de la revisión se ofrece como lista con las tres opciones «Aceptar la anulación», «Rechazar la anulación» y «Pedir subsanación» y, cuando la solicitud acaba de presentarse, sin ninguna elegida
  - disparador: al abrir la pantalla
  - condición: el expediente no viene de una devolución del director
- RUI-PENDIENTE_REVISION-SECRETARIO-013 — Los datos de identificación del alumno, los de la matrícula que se anula, la fecha y la hora de presentación y el motivo, la fecha y el autor de la devolución se muestran en solo lectura
  - disparador: al abrir la pantalla
  - condición: Siempre
- RUI-PENDIENTE_REVISION-SECRETARIO-014 — El motivo del rechazo muestra como ayuda la indicación «Este texto se imprimirá tal cual en la resolución que recibirá el alumno»
  - disparador: al abrir la pantalla
  - condición: el sentido de la revisión es «Rechazar la anulación»

### Pantalla: PENDIENTE_REVISION — resto de perfiles (solo consulta)

- **Quién la ve:** cualquier perfil con acceso al expediente distinto del SECRETARIO (el propio alumno, el director, el secretario del centro, el supervisor o el administrador), mientras el expediente está en REVISION / PENDIENTE_REVISION.
- **Qué ve el usuario, bloque a bloque:**
  - **Datos de identificación del alumno** — apellidos, nombre, NIA, documento de identidad, dirección, teléfono, población, provincia, código postal
  - **Matrícula que se anula** — curso académico, centro, ciclo con su grado o nivel
  - **Presentación** — la fecha y la hora de presentación y el justificante de presentación
  - **Solicitud firmada** — la solicitud de anulación firmada por el alumno
- **Qué puede rellenar:** *(nada: toda la pantalla es de solo consulta)*
- **Documentos que se le muestran:** el justificante de presentación, incrustado en la pantalla; la solicitud firmada, como descarga.
- **Aviso permanente en pantalla:** «La solicitud está pendiente de revisión por la secretaría del centro»
- **Botones:**
  - **«Salir»** — cierra el expediente y vuelve al listado, sin cambiar nada.

#### Reglas de pantalla

- RUI-PENDIENTE_REVISION-GENERICA-001 — El sentido de la revisión, el motivo del rechazo, el texto de qué hay que subsanar y la devolución del director no se muestran: la decisión de secretaría no se enseña a nadie hasta que el director firma la resolución
  - disparador: al abrir la pantalla
  - condición: Siempre
- RUI-PENDIENTE_REVISION-GENERICA-002 — La pantalla muestra el aviso permanente «La solicitud está pendiente de revisión por la secretaría del centro», para que quien consulte sepa a quién le toca actuar
  - disparador: al abrir la pantalla
  - condición: Siempre
