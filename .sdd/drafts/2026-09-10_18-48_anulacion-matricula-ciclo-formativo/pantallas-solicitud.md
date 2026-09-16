# Pantallas de la fase SOLICITUD — Solicitud de anulación

## Estado DATOS_SOLICITUD

### Pantalla: DATOS_SOLICITUD — perfil CREADOR

- **Quién la ve:** el alumno que solicita la anulación, mientras el expediente está en SOLICITUD / DATOS_SOLICITUD.
- **Qué ve el usuario, bloque a bloque:**
  - **Qué hay que subsanar** — el texto que secretaría le pidió corregir
  - **Datos de identificación del alumno** — apellidos, nombre, NIA, documento de identidad, dirección, teléfono, población, provincia, código postal
  - **Matrícula que se anula** — curso académico, centro, ciclo con su grado o nivel
- **Qué puede rellenar:** NIA, dirección, teléfono, población, provincia, código postal, ciclo
- **Qué solo puede consultar:** apellidos, nombre, documento de identidad, curso académico, centro, y el texto de qué hay que subsanar
- **Documentos que se le muestran:** *(ninguno)*
- **Aviso permanente en pantalla:** «Para presentar la solicitud necesitará firmarla con su certificado digital desde este mismo ordenador»
- **Botones:**
  - **«Siguiente»** — lanza la acción CONTINUAR. Sin confirmación.
  - **«Borrar el expediente»** — elimina el expediente. Pide confirmación con el texto «Se va a eliminar el expediente y no podrá recuperarlo».

#### Reglas de pantalla

- RUI-DATOS_SOLICITUD-CREADOR-001 — El bloque «Qué hay que subsanar» solo se muestra cuando secretaría ha pedido una subsanación
  - disparador: continuo
  - condición: hay texto de qué hay que subsanar
- RUI-DATOS_SOLICITUD-CREADOR-002 — El NIA, la dirección, el teléfono, la población, la provincia, el código postal y el ciclo se marcan como obligatorios
  - disparador: continuo
  - condición: Siempre
- RUI-DATOS_SOLICITUD-CREADOR-003 — Los apellidos, el nombre, el documento de identidad, el curso académico, el centro y el texto de qué hay que subsanar se muestran en solo lectura
  - disparador: al abrir la pantalla
  - condición: Siempre
- RUI-DATOS_SOLICITUD-CREADOR-004 — *(eliminada: el nombre del ciclo ya no se escribe a mano)*
- RUI-DATOS_SOLICITUD-CREADOR-005 — *(eliminada: el grado ya no se elige aparte, lo lleva puesto el ciclo)*
- RUI-DATOS_SOLICITUD-CREADOR-006 — El NIA muestra como ayuda el texto «8 dígitos»
  - disparador: al abrir la pantalla
  - condición: Siempre
- RUI-DATOS_SOLICITUD-CREADOR-007 — El teléfono muestra como ayuda el texto «9 dígitos, empezando por 6, 7, 8 o 9»
  - disparador: al abrir la pantalla
  - condición: Siempre
- RUI-DATOS_SOLICITUD-CREADOR-008 — El ciclo se elige de la lista de ciclos del catálogo del sistema educativo: al teclear en el campo se proponen los ciclos cuyo nombre contiene lo tecleado, y la ventana de búsqueda muestra de cada ciclo su familia profesional, su grado, su nivel y su nombre, y permite filtrar por cada uno de ellos
  - disparador: al abrir la pantalla
  - condición: Siempre
- RUI-DATOS_SOLICITUD-CREADOR-009 — El ciclo muestra como ayuda el texto «Elija el ciclo en el que está matriculado; puede buscarlo por familia profesional, grado, nivel o nombre»
  - disparador: al abrir la pantalla
  - condición: Siempre
- RUI-DATOS_SOLICITUD-CREADOR-010 — El ciclo no viene elegido por defecto: la ficha del alumno no guarda en qué ciclo está matriculado
  - disparador: al abrir la pantalla
  - condición: el expediente se acaba de crear
- RUI-DATOS_SOLICITUD-CREADOR-011 — Una vez elegido el ciclo, junto a él se muestra en solo lectura el grado o nivel que le corresponde, con el mismo texto que llevará la solicitud («Básico», «Medio», «Superior» o «Curso de especialización»), para que el alumno lo compruebe antes de continuar
  - disparador: al cambiar el ciclo
  - condición: hay un ciclo elegido
- RUI-DATOS_SOLICITUD-CREADOR-012 — El código postal muestra como ayuda el texto «5 dígitos»
  - disparador: al abrir la pantalla
  - condición: Siempre

### Pantalla: DATOS_SOLICITUD — resto de perfiles (solo consulta)

- **Quién la ve:** cualquier perfil con acceso al expediente distinto del CREADOR, mientras el expediente está en SOLICITUD / DATOS_SOLICITUD.
- **Qué ve el usuario, bloque a bloque:**
  - **Qué hay que subsanar** — el texto que secretaría pidió corregir
  - **Datos de identificación del alumno** — apellidos, nombre, NIA, documento de identidad, dirección, teléfono, población, provincia, código postal
  - **Matrícula que se anula** — curso académico, centro, ciclo con su grado o nivel
- **Qué puede rellenar:** *(nada: toda la pantalla es de solo consulta)*
- **Documentos que se le muestran:** *(ninguno)*
- **Aviso permanente en pantalla:** «La solicitud todavía no se ha presentado: está pendiente de que el alumno complete o corrija los datos»
- **Botones:**
  - **«Salir»** — cierra el expediente y vuelve al listado, sin cambiar nada.

#### Reglas de pantalla

- RUI-DATOS_SOLICITUD-GENERICA-001 — El bloque «Qué hay que subsanar» solo se muestra cuando secretaría ha pedido una subsanación
  - disparador: continuo
  - condición: hay texto de qué hay que subsanar
- RUI-DATOS_SOLICITUD-GENERICA-002 — La pantalla muestra el aviso permanente «La solicitud todavía no se ha presentado: está pendiente de que el alumno complete o corrija los datos», para que quien la consulte sepa que le toca actuar al alumno
  - disparador: al abrir la pantalla
  - condición: Siempre

---

## Estado PENDIENTE_FIRMA

### Pantalla: PENDIENTE_FIRMA — perfil CREADOR

- **Quién la ve:** el alumno que solicita la anulación, mientras el expediente está en SOLICITUD / PENDIENTE_FIRMA.
- **Qué ve el usuario, bloque a bloque:**
  - **Qué hay que subsanar** — el texto que secretaría le pidió corregir
  - **Solicitud a firmar** — la solicitud de anulación de matrícula tal y como quedará presentada
- **Qué puede rellenar:** *(nada: solo revisa el documento y decide si vuelve atrás o lo firma)*
- **Qué solo puede consultar:** la solicitud de anulación generada y el texto de qué hay que subsanar
- **Documentos que se le muestran:** la solicitud de anulación generada, incrustada en la pantalla a tamaño grande, para que pueda leerla entera antes de firmarla.
- **Aviso permanente en pantalla:** «Revise la solicitud antes de firmarla. Necesitará su certificado digital instalado en este ordenador y la aplicación de firma del ciudadano»
- **Botones:**
  - **«Atrás»** — lanza la acción VOLVER y devuelve el expediente a la pantalla de datos. Sin confirmación.
  - **«Firmar y presentar la solicitud»** — lanza la acción PRESENTAR: primero el alumno firma la solicitud en su propio equipo y a continuación se presenta. Pide confirmación con el texto «Va a firmar y presentar la solicitud de anulación. Una vez presentada no podrá modificarla».

#### Reglas de pantalla

- RUI-PENDIENTE_FIRMA-CREADOR-001 — La solicitud de anulación se muestra incrustada y ocupando la mayor parte de la pantalla, porque el alumno tiene que leerla entera
  - disparador: al abrir la pantalla
  - condición: Siempre
- RUI-PENDIENTE_FIRMA-CREADOR-002 — El bloque «Qué hay que subsanar» se muestra, en solo lectura, cuando secretaría pidió una subsanación que el alumno todavía no ha vuelto a presentar, para que compruebe que la solicitud corregida atiende lo que le pidieron antes de firmarla
  - disparador: continuo
  - condición: hay texto de qué hay que subsanar

### Pantalla: PENDIENTE_FIRMA — resto de perfiles (solo consulta)

- **Quién la ve:** cualquier perfil con acceso al expediente distinto del CREADOR, mientras el expediente está en SOLICITUD / PENDIENTE_FIRMA.
- **Qué ve el usuario, bloque a bloque:**
  - **Datos de identificación del alumno** — apellidos, nombre, NIA, documento de identidad, dirección, teléfono, población, provincia, código postal
  - **Matrícula que se anula** — curso académico, centro, ciclo con su grado o nivel
  - **Solicitud generada** — la solicitud de anulación, todavía sin firmar
- **Qué puede rellenar:** *(nada: toda la pantalla es de solo consulta)*
- **Documentos que se le muestran:** la solicitud de anulación generada, como descarga.
- **Aviso permanente en pantalla:** «La solicitud todavía no se ha presentado: está pendiente de que el alumno la firme»
- **Botones:**
  - **«Salir»** — cierra el expediente y vuelve al listado, sin cambiar nada.

#### Reglas de pantalla

- RUI-PENDIENTE_FIRMA-GENERICA-001 — La pantalla muestra el aviso permanente «La solicitud todavía no se ha presentado: está pendiente de que el alumno la firme», para que quien la consulte no tome la solicitud descargable por una solicitud presentada
  - disparador: al abrir la pantalla
  - condición: Siempre
