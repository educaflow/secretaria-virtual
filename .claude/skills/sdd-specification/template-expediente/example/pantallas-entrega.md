# Pantallas de la fase ENTREGA — Entrega del equipo

## Estado PENDIENTE_ENTREGA

### Pantalla: PENDIENTE_ENTREGA — perfil SECRETARIO

- **Quién la ve:** la administrativa del centro del expediente, mientras el expediente está en ENTREGA / PENDIENTE_ENTREGA.
- **Qué ve el usuario, bloque a bloque:**
  - **Solicitante** — nombre y apellidos, documento de identidad, curso académico
  - **Préstamo concedido** — equipo solicitado, fecha prevista de devolución, fecha de la valoración y quién la hizo
  - **Entrega** — número de inventario del equipo entregado, observaciones del estado del equipo
- **Qué puede rellenar:** número de inventario del equipo entregado, observaciones del estado del equipo
- **Qué solo puede consultar:** todos los datos del solicitante y del préstamo concedido
- **Documentos que se le muestran:** la solicitud firmada, como descarga, por si necesita comprobar algo de la petición.
- **Aviso permanente en pantalla:** «Compruebe el documento de identidad del alumno antes de entregarle el equipo»
- **Botones:**
  - **«Registrar la entrega»** — lanza la acción ENTREGAR. Pide confirmación con el texto «Va a dar por entregado el equipo y no podrá deshacerlo».

#### Reglas de pantalla

- RUI-PENDIENTE_ENTREGA-SECRETARIO-001 — El número de inventario del equipo entregado se marca como obligatorio
  - disparador: continuo
  - condición: Siempre
- RUI-PENDIENTE_ENTREGA-SECRETARIO-002 — El número de inventario lleva una ayuda que explica cómo se escribe, con el ejemplo «INV-2026-0001»
  - disparador: continuo
  - condición: Siempre

### Pantalla: PENDIENTE_ENTREGA — resto de perfiles (solo consulta)

- **Quién la ve:** cualquier perfil con acceso al expediente distinto del SECRETARIO, incluido el alumno, mientras el expediente está en ENTREGA / PENDIENTE_ENTREGA.
- **Qué ve el usuario, bloque a bloque:**
  - **Solicitante** — nombre y apellidos, documento de identidad, curso académico
  - **Préstamo concedido** — equipo solicitado, fecha prevista de devolución, fecha de la valoración y quién la hizo
- **Qué puede rellenar:** *(nada: toda la pantalla es de solo consulta)*
- **Documentos que se le muestran:** el justificante de presentación, como descarga.
- **Botones:**
  - **«Salir»** — cierra el expediente y vuelve al listado, sin cambiar nada.

#### Reglas de pantalla

- RUI-PENDIENTE_ENTREGA-GENERICA-001 — Al abrir la pantalla se informa al alumno de que su préstamo está concedido y de que debe pasar por conserjería a recoger el equipo
  - disparador: al abrir la pantalla
  - condición: Siempre

---

## Estado ENTREGADO

### Pantalla: ENTREGADO — resto de perfiles (solo consulta)

Este estado cierra el expediente y nadie tiene el turno en él, así que solo tiene esta pantalla.

- **Quién la ve:** cualquier perfil con acceso al expediente, incluido el alumno, mientras el expediente está en ENTREGA / ENTREGADO.
- **Qué ve el usuario, bloque a bloque:**
  - **Solicitante** — nombre y apellidos, documento de identidad, curso académico
  - **Préstamo concedido** — equipo solicitado, fecha prevista de devolución
  - **Entrega** — número de inventario del equipo entregado, observaciones del estado del equipo, fecha de entrega y quién la registró
  - **Acta de entrega** — el acta firmada por el centro
- **Qué puede rellenar:** *(nada: toda la pantalla es de solo consulta)*
- **Documentos que se le muestran:** el acta de entrega, incrustada en la pantalla.
- **Botones:**
  - **«Salir»** — cierra el expediente y vuelve al listado, sin cambiar nada.

#### Reglas de pantalla

- RUI-ENTREGADO-GENERICA-001 — Al abrir la pantalla se informa de que el expediente está cerrado y de que ya no se puede hacer nada sobre él
  - disparador: al abrir la pantalla
  - condición: Siempre
