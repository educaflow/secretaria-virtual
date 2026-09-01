# Pantallas de la fase VALORACION — Valoración de la jefatura de estudios

## Estado PENDIENTE_VALORACION

### Pantalla: PENDIENTE_VALORACION — perfil RESPONSABLE

- **Quién la ve:** el jefe de estudios del centro del expediente, mientras el expediente está en VALORACION / PENDIENTE_VALORACION.
- **Qué ve el usuario, bloque a bloque:**
  - **Solicitante** — nombre y apellidos, documento de identidad, curso académico
  - **Datos de la petición** — equipo solicitado, motivo de la petición, explicación del motivo, fecha prevista de devolución
  - **Documentación presentada** — la solicitud firmada y el documento acreditativo que aportó el alumno
  - **Valoración** — sentido de la valoración, motivo de la denegación, qué hay que corregir
- **Qué puede rellenar:** sentido de la valoración, motivo de la denegación, qué hay que corregir
- **Qué solo puede consultar:** todos los datos del solicitante y de la petición, y la documentación presentada
- **Documentos que se le muestran:** la solicitud firmada, incrustada en la pantalla a tamaño grande; el documento acreditativo, como descarga.
- **Aviso permanente en pantalla:** *(ninguno)*
- **Botones:**
  - **«Valorar la petición»** — lanza la acción VALORAR. Pide confirmación con el texto «Va a resolver la petición y no podrá deshacerlo».

#### Reglas de pantalla

- RUI-PENDIENTE_VALORACION-RESPONSABLE-001 — El motivo de la denegación solo se muestra cuando el sentido de la valoración es «Denegar el préstamo»
  - disparador: continuo
  - condición: el sentido de la valoración es «Denegar el préstamo»
- RUI-PENDIENTE_VALORACION-RESPONSABLE-002 — El motivo de la denegación se marca como obligatorio cuando el sentido de la valoración es «Denegar el préstamo»
  - disparador: continuo
  - condición: el sentido de la valoración es «Denegar el préstamo»
- RUI-PENDIENTE_VALORACION-RESPONSABLE-003 — El texto de qué hay que corregir solo se muestra cuando el sentido de la valoración es «Pedir que se corrija»
  - disparador: continuo
  - condición: el sentido de la valoración es «Pedir que se corrija»
- RUI-PENDIENTE_VALORACION-RESPONSABLE-004 — El texto de qué hay que corregir se marca como obligatorio cuando el sentido de la valoración es «Pedir que se corrija»
  - disparador: continuo
  - condición: el sentido de la valoración es «Pedir que se corrija»
- RUI-PENDIENTE_VALORACION-RESPONSABLE-005 — El sentido de la valoración se marca como obligatorio
  - disparador: continuo
  - condición: Siempre
- RUI-PENDIENTE_VALORACION-RESPONSABLE-006 — Al abrir la pantalla, el sentido de la valoración aparece sin ninguna opción elegida, para que la decisión sea siempre explícita
  - disparador: al abrir la pantalla
  - condición: Siempre

### Pantalla: PENDIENTE_VALORACION — resto de perfiles (solo consulta)

- **Quién la ve:** cualquier perfil con acceso al expediente distinto del RESPONSABLE, incluido el alumno que presentó la petición, mientras el expediente está en VALORACION / PENDIENTE_VALORACION.
- **Qué ve el usuario, bloque a bloque:**
  - **Solicitante** — nombre y apellidos, documento de identidad, curso académico
  - **Datos de la petición** — equipo solicitado, motivo de la petición, explicación del motivo, fecha prevista de devolución
  - **Justificante de presentación** — el resguardo que acredita que la petición quedó presentada, con su fecha y hora
- **Qué puede rellenar:** *(nada: toda la pantalla es de solo consulta)*
- **Documentos que se le muestran:** el justificante de presentación, incrustado en la pantalla; la solicitud firmada, como descarga. Es aquí donde el alumno consulta su justificante: el trámite no tiene ningún estado dedicado a enseñárselo.
- **Botones:**
  - **«Salir»** — cierra el expediente y vuelve al listado, sin cambiar nada.

#### Reglas de pantalla

- *(ninguna)*

---

## Estado DENEGADO

### Pantalla: DENEGADO — resto de perfiles (solo consulta)

Este estado cierra el expediente y nadie tiene el turno en él, así que solo tiene esta pantalla.

- **Quién la ve:** cualquier perfil con acceso al expediente, incluido el alumno, mientras el expediente está en VALORACION / DENEGADO.
- **Qué ve el usuario, bloque a bloque:**
  - **Solicitante** — nombre y apellidos, documento de identidad, curso académico
  - **Datos de la petición** — equipo solicitado, motivo de la petición, explicación del motivo, fecha prevista de devolución
  - **Resultado** — motivo de la denegación, fecha de la valoración y quién la hizo
  - **Resolución** — la resolución de denegación firmada por el centro y registrada de salida
- **Qué puede rellenar:** *(nada: toda la pantalla es de solo consulta)*
- **Documentos que se le muestran:** la resolución de denegación, incrustada en la pantalla.
- **Botones:**
  - **«Salir»** — cierra el expediente y vuelve al listado, sin cambiar nada.

#### Reglas de pantalla

- RUI-DENEGADO-GENERICA-001 — Al abrir la pantalla se informa de que el expediente está cerrado y de que ya no se puede hacer nada sobre él
  - disparador: al abrir la pantalla
  - condición: Siempre
