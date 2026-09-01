# Pantallas de la fase SOLICITUD — Solicitud del préstamo

## Estado DATOS_PETICION

### Pantalla: DATOS_PETICION — perfil CREADOR

- **Quién la ve:** el alumno que pidió el préstamo, mientras el expediente está en SOLICITUD / DATOS_PETICION.
- **Qué ve el usuario, bloque a bloque:**
  - **Solicitante** — nombre y apellidos, documento de identidad, curso académico
  - **Qué hay que corregir** — el texto que la jefatura de estudios le pidió corregir
  - **Datos de la petición** — equipo solicitado, motivo de la petición, explicación del motivo, fecha prevista de devolución
  - **Documentación** — documento acreditativo
- **Qué puede rellenar:** equipo solicitado, motivo de la petición, explicación del motivo, fecha prevista de devolución, documento acreditativo
- **Qué solo puede consultar:** nombre y apellidos, documento de identidad, curso académico, y el texto de qué hay que corregir
- **Documentos que se le muestran:** *(ninguno)*
- **Aviso permanente en pantalla:** «Para presentar la petición necesitará firmarla con su certificado digital desde este mismo ordenador»
- **Botones:**
  - **«Siguiente»** — lanza la acción CONTINUAR. Sin confirmación.
  - **«Borrar el expediente»** — elimina el expediente. Pide confirmación con el texto «Se va a eliminar el expediente y no podrá recuperarlo».

#### Reglas de pantalla

- RUI-DATOS_PETICION-CREADOR-001 — El bloque «Qué hay que corregir» solo se muestra cuando la jefatura de estudios ha pedido una corrección
  - disparador: continuo
  - condición: hay texto de qué hay que corregir
- RUI-DATOS_PETICION-CREADOR-002 — La explicación del motivo solo se muestra cuando el motivo elegido es «Otros»
  - disparador: continuo
  - condición: el motivo de la petición es «Otros»
- RUI-DATOS_PETICION-CREADOR-003 — La explicación del motivo se marca como obligatoria cuando el motivo elegido es «Otros»
  - disparador: continuo
  - condición: el motivo de la petición es «Otros»
- RUI-DATOS_PETICION-CREADOR-004 — Al cambiar el motivo de la petición a algo distinto de «Otros», la explicación que hubiera escrita se borra de la pantalla
  - disparador: al cambiar el motivo de la petición
  - condición: el motivo de la petición deja de ser «Otros»
- RUI-DATOS_PETICION-CREADOR-005 — El equipo solicitado, el motivo de la petición, la fecha prevista de devolución y el documento acreditativo se marcan como obligatorios
  - disparador: continuo
  - condición: Siempre
- RUI-DATOS_PETICION-CREADOR-006 — El documento acreditativo solo admite ficheros PDF o imágenes al elegirlo
  - disparador: continuo
  - condición: Siempre

### Pantalla: DATOS_PETICION — resto de perfiles (solo consulta)

- **Quién la ve:** cualquier perfil con acceso al expediente distinto del CREADOR, mientras el expediente está en SOLICITUD / DATOS_PETICION.
- **Qué ve el usuario, bloque a bloque:**
  - **Solicitante** — nombre y apellidos, documento de identidad, curso académico
  - **Qué hay que corregir** — el texto que la jefatura de estudios pidió corregir
  - **Datos de la petición** — equipo solicitado, motivo de la petición, explicación del motivo, fecha prevista de devolución
  - **Documentación** — documento acreditativo
- **Qué puede rellenar:** *(nada: toda la pantalla es de solo consulta)*
- **Documentos que se le muestran:** *(ninguno)*
- **Botones:**
  - **«Salir»** — cierra el expediente y vuelve al listado, sin cambiar nada.

#### Reglas de pantalla

- RUI-DATOS_PETICION-GENERICA-001 — El bloque «Qué hay que corregir» solo se muestra cuando la jefatura de estudios ha pedido una corrección
  - disparador: continuo
  - condición: hay texto de qué hay que corregir

---

## Estado PENDIENTE_FIRMA

### Pantalla: PENDIENTE_FIRMA — perfil CREADOR

- **Quién la ve:** el alumno que pidió el préstamo, mientras el expediente está en SOLICITUD / PENDIENTE_FIRMA.
- **Qué ve el usuario, bloque a bloque:**
  - **Solicitud a firmar** — el documento de solicitud tal y como quedará presentado
- **Qué puede rellenar:** *(nada: solo revisa el documento y decide si vuelve atrás o lo firma)*
- **Qué solo puede consultar:** el documento de solicitud
- **Documentos que se le muestran:** el documento de solicitud, incrustado en la pantalla a tamaño grande, para que pueda leerlo entero antes de firmarlo.
- **Aviso permanente en pantalla:** «Revise la solicitud antes de firmarla. Necesitará su certificado digital instalado en este ordenador y la aplicación de firma del ciudadano»
- **Botones:**
  - **«Atrás»** — lanza la acción VOLVER y devuelve el expediente a la pantalla de datos. Sin confirmación.
  - **«Firmar y presentar»** — lanza la acción PRESENTAR: primero el alumno firma el documento en su propio equipo y a continuación se presenta. Pide confirmación con el texto «Una vez presentada la petición no podrá modificarla».

#### Reglas de pantalla

- RUI-PENDIENTE_FIRMA-CREADOR-001 — El documento de solicitud se muestra incrustado y ocupando la mayor parte de la pantalla, porque el alumno tiene que leerlo entero
  - disparador: al abrir la pantalla
  - condición: Siempre

### Pantalla: PENDIENTE_FIRMA — resto de perfiles (solo consulta)

- **Quién la ve:** cualquier perfil con acceso al expediente distinto del CREADOR, mientras el expediente está en SOLICITUD / PENDIENTE_FIRMA.
- **Qué ve el usuario, bloque a bloque:**
  - **Solicitante** — nombre y apellidos, documento de identidad, curso académico
  - **Datos de la petición** — equipo solicitado, motivo de la petición, explicación del motivo, fecha prevista de devolución
  - **Solicitud generada** — el documento de solicitud, todavía sin firmar
- **Qué puede rellenar:** *(nada: toda la pantalla es de solo consulta)*
- **Documentos que se le muestran:** el documento de solicitud, como descarga.
- **Botones:**
  - **«Salir»** — cierra el expediente y vuelve al listado, sin cambiar nada.

#### Reglas de pantalla

- *(ninguna)*
