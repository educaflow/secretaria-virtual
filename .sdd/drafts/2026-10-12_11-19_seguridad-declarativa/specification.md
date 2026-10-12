---
type: specification
template: free
---

# Objetivo

Que cada usuario de la secretaría virtual solo pueda ver y hacer, en menús, pantallas, acciones y datos, exactamente lo que se le ha concedido de forma explícita, y que todo intento de ir más allá sea rechazado y quede registrado.

# Contexto y alcance

**Por qué es una feature libre:** la seguridad afecta a todas las partes de la aplicación a la vez (menús, pantallas, acciones y datos de todos los sistemas, incluidos los expedientes y la tramitación) y a cómo se construye la aplicación; no es un sistema ni un tipo de expediente.

**Partes de la aplicación afectadas** (en lenguaje de negocio, solo lo que cambia):

- **Menú principal**: la visibilidad de cada entrada pasa a regirse por la nueva definición de seguridad. Cada tipo de usuario y cargo ve **los mismos menús que hoy**; lo que cambia es que quien no ve un menú tampoco puede abrir su pantalla por otro camino.
- **Todas las pantallas y acciones** (botones, asistentes, listados, formularios) de todas las partes de la aplicación: solo pueden ejecutarse por quien tiene concedido el acceso; forzar la aplicación para ejecutar una acción no concedida produce un error de acceso, no hace nada y queda registrado.
- **Los datos de todas las partes** (notificaciones, tareas de firma, registros de entrada y salida, usuarios, centros, certificados, sistema educativo, importaciones, expedientes…): quién puede ver, crear, modificar y borrar cada tipo de dato se define de forma explícita siguiendo los principios de este documento; el detalle por cada tipo de dato se fija durante la implementación con la revisión del responsable del proyecto.
- **Expedientes**: fuera del administrador, nadie puede crear, modificar ni borrar un expediente directamente; solo la tramitación los cambia. Hoy cualquier usuario con sesión puede, forzando la aplicación, cambiar el estado, el centro o cualquier dato de un expediente.
- **Entradas sin sesión**: solo siguen abiertas la descarga del documento del registro de salida mediante el código de su QR y el alta de nuevos usuarios. Todo lo demás exige sesión.
- **Alta de usuarios sin sesión**: no cambia su funcionamiento (está a medio hacer y se completará aparte), pero queda sujeta al principio de que el visitante no puede fijar su tipo de usuario, cargos, perfiles ni centros.
- **Registro de seguridad**: toda denegación se anota en un fichero de log propio de seguridad, separado del resto de la aplicación.
- **Trámite de prueba**: el trámite «Trámite de prueba» se elimina de la aplicación antes de aplicar la nueva seguridad (era solo un ejemplo de desarrollo).
- **Documentos adjuntos (ficheros)**: **no cambian** en esta iniciativa. Hoy cualquier usuario con sesión que conozca el número de un documento puede descargarlo; es un riesgo conocido que se resolverá en otra iniciativa (ver Fuera de alcance).

**Dependencias funcionales:** los tipos de usuario y cargos de cada usuario en cada centro (gestión de centro), los perfiles de tramitación de cada trámite (quién tramita qué en cada unidad tramitadora) y la máquina de estados de la tramitación de expedientes.

# Actores

- **Administrador**: ve y edita todos los datos de todos los centros (incluidos los expedientes) y puede ejecutar cualquier acción de cualquier pantalla; en el menú solo ve las entradas que se le conceden expresamente (las de administración, las comunes a todos, «Notificaciones → Del centro» como gestor de notificaciones y, dentro de «Tramitación», «Trámite en papel», «Pendientes de mí» y «Todos los centros»), no las propias de cada tipo de usuario ni las de cada unidad tramitadora.
- **Supervisor**: gestiona su centro; ve las firmas y notificaciones de su centro, el registro de entrada y salida y la pantalla «Mi centro».
- **Profesor** y sus cargos (**Director**, **Jefe de estudios**, **Secretario**, **Vicedirector**, **Vicesecretario**): presentan sus trámites y firman; según su cargo o perfil de tramitación ven además la tramitación de su unidad y las notificaciones del centro.
- **Administrativo**: tramita los trámites de alumnado (unidad de Secretaría) y gestiona las notificaciones del centro.
- **Alumno**, **Familiar**, **Exalumno**, **Exprofesor**, **Externo**: usuarios que presentan sus trámites, firman lo suyo y ven sus notificaciones; no tienen acceso a nada de gestión.
- **Visitante sin sesión**: solo puede dar de alta un usuario nuevo y descargar un documento del registro de salida con el código de su QR.
- **Responsable del proyecto**: define la seguridad de cada parte al construir la aplicación y revisa el acceso a cada tipo de dato.
- **Responsable del servidor**: consulta el fichero de log de seguridad.
- **El sistema**: rechaza lo no concedido y lo anota en el registro de seguridad.

# Historias de usuario

## HU-001 — Como usuario de cualquier tipo quiero seguir viendo los mismos menús que hoy para que el cambio de seguridad no me quite ni me dé accesos

- ESC-001 — Un profesor sin cargo ve solo los menús comunes:
  1. El profesor «profesor1@mislata.es» inicia sesión con contraseña «demo1234».
  2. El sistema muestra en el menú principal «Mis trámites» (con «Nuevo trámite», «Pendientes de mí», «En tramitación» y «Finalizados»), «Firmas» (con «Pendientes», «Firmadas» y «Rechazadas») y «Notificaciones» (con «Recibidas»).
  3. El sistema no muestra «Tramitación», «Registro», «Mi centro», «Administración», «Criptografía», «Sistema educativo» ni «Desarrollador», ni las entradas «Firmas → Del centro», «Firmas → Todas», «Notificaciones → Del centro» ni «Notificaciones → Todas».
- ESC-002 — Un supervisor ve los menús de gestión de su centro:
  1. El supervisor «supervisor1@mislata.es» inicia sesión con contraseña «demo1234».
  2. El sistema muestra en el menú principal «Firmas → Del centro», «Registro» (con «Entrada» y «Salida»), «Notificaciones → Del centro» y «Mi centro» (con «Centro», «Usuarios» y «Perfiles de trámites»).
  3. El sistema muestra también los menús comunes: «Mis trámites» (con «Nuevo trámite», «Pendientes de mí», «En tramitación» y «Finalizados»), «Firmas» (con «Pendientes», «Firmadas» y «Rechazadas») y «Notificaciones → Recibidas».
  4. El sistema no muestra «Tramitación», «Administración», «Criptografía», «Sistema educativo», «Desarrollador», «Firmas → Todas» ni «Notificaciones → Todas».
- ESC-003 — Un administrativo ve la tramitación de Secretaría:
  1. El administrativo «administrativo1@mislata.es» inicia sesión con contraseña «demo1234».
  2. El sistema muestra en el menú principal «Tramitación» con «Trámite en papel», «Pendientes de mí» y «Secretaría» (con «Abiertos» y «Cerrados»), «Registro» (con «Entrada» y «Salida») y «Notificaciones → Del centro».
  3. El sistema muestra también los menús comunes: «Mis trámites» (con «Nuevo trámite», «Pendientes de mí», «En tramitación» y «Finalizados»), «Firmas» (con «Pendientes», «Firmadas» y «Rechazadas») y «Notificaciones → Recibidas».
  4. El sistema no muestra «Tramitación → Jefatura de estudios», «Tramitación → Todos los centros», «Mi centro», «Administración», «Criptografía», «Sistema educativo», «Desarrollador», ni las entradas «Firmas → Del centro», «Firmas → Todas» ni «Notificaciones → Todas».
- ESC-004 — Un jefe de estudios ve la tramitación de Jefatura de estudios:
  1. El jefe de estudios «jefeestudios1@mislata.es» inicia sesión con contraseña «demo1234».
  2. El sistema muestra en el menú principal «Tramitación» con «Trámite en papel», «Pendientes de mí» y «Jefatura de estudios» (con «Abiertos» y «Cerrados»), y «Notificaciones → Del centro».
  3. El sistema muestra también los menús comunes: «Mis trámites», «Firmas» (con «Pendientes», «Firmadas» y «Rechazadas») y «Notificaciones → Recibidas».
  4. El sistema no muestra «Tramitación → Secretaría», «Tramitación → Todos los centros», «Registro», «Mi centro», «Administración», «Criptografía», «Sistema educativo», «Desarrollador», ni las entradas «Firmas → Del centro», «Firmas → Todas» ni «Notificaciones → Todas».
- ESC-005 — Un alumno solo ve lo suyo:
  1. El alumno «alumno1@mislata.es» inicia sesión con contraseña «demo1234».
  2. El sistema muestra en el menú principal «Mis trámites», «Firmas» (con «Pendientes», «Firmadas» y «Rechazadas») y «Notificaciones → Recibidas».
  3. El sistema no muestra «Tramitación», «Registro», «Notificaciones → Del centro», «Mi centro» ni «Administración».
- ESC-006 — El administrador ve los menús de administración y los comunes:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. El sistema muestra en el menú principal «Administración» (con «Centros», «Usuarios», «Ficheros importación» y «Perfiles de trámites por centro»), «Criptografía» (con «Dispositivos criptográficos» y «Certificados digitales»), «Sistema educativo», «Desarrollador», «Firmas → Todas», «Notificaciones → Todas», «Notificaciones → Del centro», «Tramitación» (con «Trámite en papel», «Pendientes de mí» y «Todos los centros», este con «Abiertos», «Cerrados» y «Todos») y «Registro» (con «Entrada» y «Salida»).
  3. El sistema muestra también los menús comunes: «Mis trámites» (con «Nuevo trámite», «Pendientes de mí», «En tramitación» y «Finalizados»), «Firmas» (con «Pendientes», «Firmadas» y «Rechazadas») y «Notificaciones → Recibidas».
  4. El sistema no muestra «Mi centro», «Firmas → Del centro», «Tramitación → Secretaría» ni «Tramitación → Jefatura de estudios».
- ESC-030 — El director ve la tramitación de Secretaría y de Jefatura de estudios:
  1. El director «director@mislata.es» inicia sesión con contraseña «demo1234».
  2. El sistema muestra en el menú principal «Tramitación» con «Trámite en papel», «Pendientes de mí», «Secretaría» (con «Abiertos» y «Cerrados») y «Jefatura de estudios» (con «Abiertos» y «Cerrados»), «Registro» (con «Entrada» y «Salida») y «Notificaciones → Del centro».
  3. El sistema muestra también los menús comunes: «Mis trámites» (con «Nuevo trámite», «Pendientes de mí», «En tramitación» y «Finalizados»), «Firmas» (con «Pendientes», «Firmadas» y «Rechazadas») y «Notificaciones → Recibidas».
  4. El sistema no muestra «Tramitación → Todos los centros», «Mi centro», «Administración», «Firmas → Del centro» ni «Firmas → Todas».
- ESC-031 — Un familiar ve solo los menús comunes:
  1. El familiar «familiar1@mislata.es» inicia sesión con contraseña «demo1234».
  2. El sistema muestra en el menú principal «Mis trámites», «Firmas» (con «Pendientes», «Firmadas» y «Rechazadas») y «Notificaciones → Recibidas».
  3. El sistema no muestra «Tramitación», «Registro», «Notificaciones → Del centro», «Mi centro» ni «Administración».

## HU-002 — Como responsable del centro quiero que quien no ve una pantalla tampoco pueda abrirla ni ejecutar sus acciones forzando la aplicación para que la seguridad no dependa de que un botón esté oculto

- ESC-007 — Un profesor no puede abrir la pantalla de tramitación de Secretaría por su dirección:
  1. El administrativo «administrativo1@mislata.es» inicia sesión con contraseña «demo1234».
  2. Abre «Tramitación → Secretaría → Abiertos» y copia la dirección que muestra el navegador.
  3. Cierra sesión.
  4. El profesor «profesor1@mislata.es» inicia sesión con contraseña «demo1234».
  5. Pega en el navegador la dirección copiada de la pantalla «Abiertos» de Secretaría.
  6. El sistema muestra el error genérico de acceso de la aplicación y no muestra ningún expediente.
  7. El profesor abre «Mis trámites → En tramitación».
  8. El sistema muestra la pantalla con normalidad, sin ningún error: la denegación no ha afectado a su sesión.
- ESC-008 — Un alumno no puede ejecutar una acción de gestión mediante una petición directa:
  1. El alumno «alumno1@mislata.es» inicia sesión con contraseña «demo1234».
  2. Envía a la aplicación, fuera de las pantallas, la petición que abre el listado «Firmas → Del centro» del centro «CIPFP Mislata».
  3. El sistema responde con el error genérico de acceso y no devuelve ninguna tarea de firma.
- ESC-009 — Una acción de gestión denegada no deja ningún efecto:
  1. El alumno «alumno1@mislata.es» inicia sesión con contraseña «demo1234».
  2. Abre «Mis trámites → Nuevo trámite», elige el centro «CIPFP Mislata» y el trámite «Anulación de matrícula en ciclo formativo» y pulsa «Crear expediente».
  3. En «Datos del alumno/a» rellena el NIA con «12345678», la dirección con «C/ Dolores Ibarruri, 32», el municipio con «Mislata» y el código postal con «46920»; en «Matrícula que se anula» elige el ciclo formativo «Desarrollo de Aplicaciones Web».
  4. Pulsa «Siguiente» y después «Firmar y presentar la solicitud», y confirma el aviso.
  5. El sistema muestra el «Justificante de presentación» y genera al alumno el aviso por correo del registro de entrada, que queda en las notificaciones del centro «CIPFP Mislata».
  6. El alumno cierra sesión.
  7. El supervisor «supervisor1@mislata.es» inicia sesión con contraseña «demo1234» y abre «Notificaciones → Del centro».
  8. Abre la notificación del aviso de registro de entrada dirigida a «alumno1@mislata.es», anota su estado y su fecha del último intento de envío y copia la dirección que muestra el navegador.
  9. El supervisor cierra sesión.
  10. El profesor «profesor1@mislata.es» inicia sesión con contraseña «demo1234».
  11. Envía a la aplicación, fuera de las pantallas, la petición de la acción «Reenviar» sobre la notificación cuya dirección copió el supervisor, acción que solo pueden ejecutar quienes gestionan las notificaciones del centro.
  12. El sistema responde con el error genérico de acceso.
  13. El profesor cierra sesión.
  14. El supervisor «supervisor1@mislata.es» inicia sesión con contraseña «demo1234».
  15. Abre «Notificaciones → Del centro» y abre la notificación del aviso de registro de entrada dirigida a «alumno1@mislata.es».
  16. El sistema muestra la notificación con el mismo estado y la misma fecha del último intento de envío que el supervisor anotó, sin ningún intento de envío nuevo.
- ESC-010 — El administrador puede ejecutar una acción de datos aunque no la tenga en su menú:
  1. El alumno «alumno1@mislata.es» inicia sesión con contraseña «demo1234».
  2. Abre «Mis trámites → Nuevo trámite», elige el centro «CIPFP Mislata» y el trámite «Anulación de matrícula en ciclo formativo» y pulsa «Crear expediente».
  3. En «Datos del alumno/a» rellena el NIA con «12345678», la dirección con «C/ Dolores Ibarruri, 32», el municipio con «Mislata» y el código postal con «46920»; en «Matrícula que se anula» elige el ciclo formativo «Desarrollo de Aplicaciones Web».
  4. Pulsa «Siguiente» y después «Firmar y presentar la solicitud», y confirma el aviso.
  5. El sistema muestra el «Justificante de presentación» y genera al alumno el aviso por correo del registro de entrada.
  6. El alumno cierra sesión.
  7. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  8. Abre «Notificaciones → Todas».
  9. Abre la notificación del aviso de registro de entrada dirigida a «alumno1@mislata.es» del centro «CIPFP Mislata».
  10. Pulsa «Reenviar».
  11. Si la notificación está en estado «Fallido», el sistema vuelve a intentar el envío y refresca la ficha sin ningún error de acceso; si no está en «Fallido», el sistema muestra el aviso «Solo se pueden reenviar notificaciones que han fallado», y en ningún caso el error genérico de acceso.

## HU-003 — Como usuario quiero seguir trabajando con mis trámites, firmas y notificaciones igual que antes para que la nueva seguridad no rompa lo que ya funcionaba

- ESC-011 — Un alumno presenta un trámite y lo tramita Secretaría (no-regresión de la tramitación):
  1. El alumno «alumno1@mislata.es» inicia sesión con contraseña «demo1234».
  2. Abre «Mis trámites → Nuevo trámite», elige el centro «CIPFP Mislata» y el trámite «Anulación de matrícula en ciclo formativo» y pulsa «Crear expediente».
  3. En «Datos del alumno/a» rellena el NIA con «12345678», la dirección con «C/ Dolores Ibarruri, 32», el municipio con «Mislata» y el código postal con «46920»; en «Matrícula que se anula» elige el ciclo formativo «Desarrollo de Aplicaciones Web».
  4. Pulsa «Siguiente».
  5. El sistema muestra la solicitud en PDF y el panel «Firma de la solicitud» indicando que se firmará en el servidor con su certificado digital.
  6. Pulsa «Firmar y presentar la solicitud» y confirma el aviso «Va a firmar y presentar la solicitud de anulación. Una vez presentada no podrá modificarla».
  7. El sistema muestra el panel «Justificante de presentación» con la fecha y hora de presentación; el alumno anota el número del expediente.
  8. Abre «Mis trámites → En tramitación».
  9. El sistema muestra el expediente con la fase «Verificación» y el estado «Pendiente de verificación».
  10. El alumno cierra sesión.
  11. El administrativo «administrativo1@mislata.es» inicia sesión con contraseña «demo1234».
  12. Abre «Tramitación → Secretaría → Abiertos».
  13. El sistema muestra el expediente con el número anotado.
  14. Abre el expediente, elige en «Resultado de la verificación» la opción «La solicitud es correcta» y pulsa «Siguiente».
  15. El sistema muestra el expediente con la fase «Resolución» y el estado «Pendiente de resolución».
- ESC-012 — Un profesor presenta un trámite y lo tramita Jefatura de estudios (no-regresión de la tramitación):
  1. El profesor «profesor1@mislata.es» inicia sesión con contraseña «demo1234».
  2. Abre «Mis trámites → Nuevo trámite», elige el centro «CIPFP Mislata» y el trámite «Justificación de falta del profesorado» y pulsa «Crear expediente».
  3. En «Datos de la falta» elige «Tipo de jornada faltada» «Un día completo», pone como «Fecha» la de hace dos días y como «Motivo falta» «Traslado de domicilio», y en «Adjuntar justificante» adjunta un PDF pequeño en «Foto o PDF del justificante».
  4. Pulsa «Siguiente».
  5. Pulsa «Firmar y presentar la solicitud» y confirma el aviso.
  6. El sistema muestra el panel «Justificante de presentación» con la fecha y hora de presentación y el expediente queda en la fase «Verificación», estado «Pendiente de verificación»; el profesor anota el número del expediente.
  7. El profesor cierra sesión.
  8. El jefe de estudios «jefeestudios1@mislata.es» inicia sesión con contraseña «demo1234».
  9. Abre «Tramitación → Jefatura de estudios → Abiertos».
  10. El sistema muestra el expediente con el número anotado y permite abrirlo sin error de acceso.
- ESC-013 — Un usuario ve sus firmas pendientes y las firma (no-regresión de firmas):
  1. El alumno «alumno1@mislata.es» inicia sesión con contraseña «demo1234».
  2. Abre «Mis trámites → Nuevo trámite», elige el centro «CIPFP Mislata» y el trámite «Anulación de matrícula en ciclo formativo» y pulsa «Crear expediente».
  3. En «Datos del alumno/a» rellena el NIA con «12345678», la dirección con «C/ Dolores Ibarruri, 32», el municipio con «Mislata» y el código postal con «46920»; en «Matrícula que se anula» elige el ciclo formativo «Desarrollo de Aplicaciones Web».
  4. Pulsa «Siguiente», después «Firmar y presentar la solicitud» y confirma el aviso; anota el número del expediente del «Justificante de presentación».
  5. El alumno cierra sesión.
  6. El administrativo «administrativo1@mislata.es» inicia sesión con contraseña «demo1234», abre «Tramitación → Pendientes de mí» y abre el expediente con ese número.
  7. Elige «La solicitud es correcta» y pulsa «Siguiente».
  8. Elige «Aceptar la anulación», pulsa «Enviar a la firma» y confirma el aviso.
  9. El administrativo cierra sesión.
  10. El secretario «secretario@mislata.es» inicia sesión con contraseña «demo1234», abre en «Firmas → Pendientes» la tarea de firma de la resolución de ese expediente, pulsa «Firmar todos los documentos» y después «Firmar todos los documentos y finalizar».
  11. El secretario cierra sesión.
  12. El director «director@mislata.es» inicia sesión con contraseña «demo1234».
  13. Abre «Firmas → Pendientes».
  14. El sistema muestra la tarea de firma de la resolución de ese expediente, sin error de acceso.
  15. Abre la tarea, pulsa «Firmar todos los documentos» y después «Firmar todos los documentos y finalizar».
  16. Abre «Firmas → Firmadas».
  17. El sistema muestra la tarea recién firmada en el listado.
  18. Abre «Firmas → Rechazadas».
  19. El sistema muestra el listado sin error de acceso.
- ESC-014 — Un usuario ve sus notificaciones recibidas (no-regresión de notificaciones):
  1. El alumno «alumno2@mislata.es» inicia sesión con contraseña «demo1234».
  2. Abre «Notificaciones → Recibidas».
  3. El sistema muestra el listado de sus notificaciones sin error de acceso.
- ESC-015 — El supervisor gestiona su centro (no-regresión de gestión de centro):
  1. El supervisor «supervisor1@mislata.es» inicia sesión con contraseña «demo1234».
  2. Abre «Mi centro → Usuarios».
  3. El sistema muestra el listado de usuarios del centro «CIPFP Mislata», entre ellos «profesor1@mislata.es» y «alumno1@mislata.es», sin error de acceso.
  4. Abre «Mi centro → Perfiles de trámites».
  5. El sistema muestra la pantalla sin error de acceso.
  6. Abre «Registro → Entrada».
  7. El sistema muestra el registro de entrada del centro sin error de acceso.
- ESC-016 — El administrador administra todos los centros (no-regresión de administración):
  1. El alumno «alumno1@mislata.es» inicia sesión con contraseña «demo1234».
  2. Abre «Mis trámites → Nuevo trámite», elige el centro «CIPFP Mislata» y el trámite «Anulación de matrícula en ciclo formativo» y pulsa «Crear expediente».
  3. En «Datos del alumno/a» rellena el NIA con «12345678», la dirección con «C/ Dolores Ibarruri, 32», el municipio con «Mislata» y el código postal con «46920»; en «Matrícula que se anula» elige el ciclo formativo «Desarrollo de Aplicaciones Web».
  4. Pulsa «Siguiente», después «Firmar y presentar la solicitud» y confirma el aviso.
  5. El alumno cierra sesión.
  6. El alumno «alumno1@batoi.es» inicia sesión con contraseña «demo1234» y presenta de la misma forma una «Anulación de matrícula en ciclo formativo» en el centro «CIPFP Batoi» (NIA «12345678», dirección «C/ Societat Unió Musical, 8», municipio «Alcoi», código postal «03800», ciclo «Desarrollo de Aplicaciones Web»).
  7. El alumno cierra sesión.
  8. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  9. Abre «Administración → Centros».
  10. El sistema muestra «CIPFP Mislata» y «CIPFP Batoi».
  11. Abre «Administración → Usuarios» y abre el usuario «profesor1@batoi.es».
  12. El sistema muestra su ficha.
  13. Cambia el «Nombre» a «Profesor1 Modificado» y pulsa «Guardar».
  14. El sistema guarda sin error de acceso y muestra el nombre «Profesor1 Modificado».
  15. Vuelve a poner el «Nombre» «Profesor1» y pulsa «Guardar».
  16. Abre «Tramitación → Todos los centros → Todos».
  17. El sistema muestra los dos expedientes recién presentados, con la columna «Centro» mostrando «CIPFP Mislata» y «CIPFP Batoi».
- ESC-032 — Quien tramita abre «Trámite en papel», «Pendientes de mí» y «Registro → Salida» (no-regresión de la tramitación):
  1. El administrativo «administrativo1@mislata.es» inicia sesión con contraseña «demo1234».
  2. Abre «Tramitación → Pendientes de mí».
  3. El sistema muestra el listado sin error de acceso.
  4. Abre «Tramitación → Trámite en papel».
  5. El sistema muestra la pantalla para iniciar un trámite en papel, permite elegir el centro «CIPFP Mislata» y ofrece el trámite «Anulación de matrícula en ciclo formativo», sin error de acceso.
  6. Abre «Registro → Salida».
  7. El sistema muestra el registro de salida del centro sin error de acceso.
- ESC-033 — El supervisor modifica un usuario de su centro y el cambio se conserva (no-regresión de gestión de centro):
  1. El supervisor «supervisor1@mislata.es» inicia sesión con contraseña «demo1234».
  2. Abre «Mi centro → Usuarios» y abre el usuario «profesor2@mislata.es».
  3. Cambia el nombre a «Profesor2 Modificado» y pulsa «Guardar».
  4. El sistema guarda sin error de acceso.
  5. Cierra sesión y vuelve a iniciar sesión con «supervisor1@mislata.es» y contraseña «demo1234».
  6. Abre «Mi centro → Usuarios» y abre el usuario «profesor2@mislata.es».
  7. El sistema muestra el nombre «Profesor2 Modificado».
  8. Vuelve a poner el nombre «Profesor2» y pulsa «Guardar».

## HU-004 — Como responsable del centro quiero que nadie vea ni modifique datos de otro centro ni datos ajenos para cumplir el principio de que cada centro solo ve lo suyo

- ESC-017 — Un supervisor no ve los usuarios de otro centro:
  1. El supervisor «supervisor1@mislata.es» inicia sesión con contraseña «demo1234».
  2. Abre «Mi centro → Usuarios».
  3. El sistema muestra, entre otros, los usuarios «profesor1@mislata.es» y «alumno1@mislata.es» del centro «CIPFP Mislata».
  4. El sistema no muestra «profesor1@batoi.es», «alumno1@batoi.es» ni «supervisor1@batoi.es», ni ningún otro usuario cuyo único centro sea «CIPFP Batoi».
- ESC-018 — Un supervisor no puede abrir un dato de otro centro por su dirección:
  1. El alumno «alumno1@batoi.es» inicia sesión con contraseña «demo1234».
  2. Abre «Mis trámites → Nuevo trámite», elige el centro «CIPFP Batoi» y el trámite «Anulación de matrícula en ciclo formativo» y pulsa «Crear expediente».
  3. En «Datos del alumno/a» rellena el NIA con «12345678», la dirección con «C/ Societat Unió Musical, 8», el municipio con «Alcoi» y el código postal con «03800»; en «Matrícula que se anula» elige el ciclo formativo «Desarrollo de Aplicaciones Web».
  4. Pulsa «Siguiente», después «Firmar y presentar la solicitud» y confirma el aviso.
  5. El sistema muestra el «Justificante de presentación»; con ello se genera la notificación de registro de entrada dirigida al alumno.
  6. El alumno cierra sesión.
  7. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  8. Abre «Notificaciones → Todas», abre la notificación del centro «CIPFP Batoi» cuyo destinatario es «Alumno1 CIPFP Batoi» (la de registro de entrada recién generada) y copia la dirección que muestra el navegador.
  9. Cierra sesión.
  10. El supervisor «supervisor1@mislata.es» inicia sesión con contraseña «demo1234».
  11. Pega en el navegador la dirección copiada.
  12. El sistema no muestra la notificación y muestra el error genérico de acceso.
  13. El supervisor abre «Notificaciones → Del centro».
  14. El sistema muestra solo notificaciones del centro «CIPFP Mislata» y no muestra la notificación de «Alumno1 CIPFP Batoi».
- ESC-019 — Un supervisor de dos centros ve los datos de ambos:
  1. El alumno «alumno1@mislata.es» inicia sesión con contraseña «demo1234», abre «Mis trámites → Nuevo trámite», elige el centro «CIPFP Mislata» y el trámite «Anulación de matrícula en ciclo formativo», pulsa «Crear expediente», rellena el NIA «12345678», la dirección «C/ Dolores Ibarruri, 32», el municipio «Mislata», el código postal «46920» y el ciclo «Desarrollo de Aplicaciones Web», pulsa «Siguiente», pulsa «Firmar y presentar la solicitud», confirma el aviso y cierra sesión.
  2. El administrativo «administrativo1@mislata.es» inicia sesión con contraseña «demo1234», abre «Tramitación → Secretaría → Abiertos», abre ese expediente, elige «La solicitud es correcta» y pulsa «Siguiente», elige «Aceptar la anulación», pulsa «Enviar a la firma» y confirma el aviso.
  3. El sistema pone la resolución a firmar al secretario del centro y el administrativo cierra sesión.
  4. El alumno «alumno1@batoi.es» inicia sesión con contraseña «demo1234» y repite la presentación en el centro «CIPFP Batoi» (dirección «C/ Societat Unió Musical, 8», municipio «Alcoi», código postal «03800»); cierra sesión.
  5. El administrativo «administrativo1@batoi.es» inicia sesión con contraseña «demo1234», tramita ese expediente igual (verificación correcta, «Aceptar la anulación» y «Enviar a la firma») y cierra sesión.
  6. El supervisor «supervisordoscentros@mislata.es» inicia sesión con contraseña «demo1234».
  7. Abre «Firmas → Del centro».
  8. El sistema muestra una tarea de firma con centro «CIPFP Mislata» y firmante «Secretario CIPFP Mislata» y otra con centro «CIPFP Batoi» y firmante «Secretario CIPFP Batoi», distinguidas por la columna «centro».
- ESC-020 — Un alumno no puede leer las notificaciones de otro usuario mediante una petición directa:
  1. El alumno «alumno2@mislata.es» inicia sesión con contraseña «demo1234», abre «Mis trámites → Nuevo trámite», elige el centro «CIPFP Mislata» y el trámite «Anulación de matrícula en ciclo formativo», pulsa «Crear expediente», rellena el NIA «12345678», la dirección «C/ Dolores Ibarruri, 32», el municipio «Mislata», el código postal «46920» y el ciclo «Desarrollo de Aplicaciones Web», pulsa «Siguiente», pulsa «Firmar y presentar la solicitud», confirma el aviso y cierra sesión.
  2. El alumno «alumno1@mislata.es» inicia sesión con contraseña «demo1234».
  3. Envía a la aplicación, fuera de las pantallas, la petición que lista todas las notificaciones del centro «CIPFP Mislata».
  4. El sistema no devuelve ninguna notificación cuyo destinatario sea «Alumno2 CIPFP Mislata» (la de registro de entrada recién generada) y solo devuelve, si las hay, las dirigidas a «alumno1@mislata.es».
- ESC-034 — Un supervisor no puede modificar un usuario de otro centro mediante una petición directa:
  1. El supervisor «supervisor1@mislata.es» inicia sesión con contraseña «demo1234».
  2. Envía a la aplicación, fuera de las pantallas, una petición de guardado directo sobre el usuario «profesor1@batoi.es» del centro «CIPFP Batoi» que cambia su nombre a «Cambiado».
  3. El sistema rechaza la petición con el error genérico de acceso.
  4. El supervisor cierra sesión.
  5. El administrador inicia sesión con usuario «admin» y contraseña «admin», abre «Administración → Usuarios» y abre el usuario «profesor1@batoi.es».
  6. El sistema muestra la ficha del usuario con su nombre original «Profesor1 CIPFP Batoi», sin ningún cambio.
- ESC-035 — Quien tramita en un centro no ve los expedientes presentados en otro:
  1. El alumno «alumno1@batoi.es» inicia sesión con contraseña «demo1234».
  2. Abre «Mis trámites → Nuevo trámite» y elige el centro «CIPFP Batoi» y el trámite «Anulación de matrícula en ciclo formativo», y pulsa «Crear expediente».
  3. En «Datos del alumno/a» rellena el NIA con «12345678», la dirección con «C/ Societat Unió Musical, 8», el municipio con «Alcoi» y el código postal con «03800»; en «Matrícula que se anula» elige el ciclo formativo «Desarrollo de Aplicaciones Web», y pulsa «Siguiente».
  4. Pulsa «Firmar y presentar la solicitud» y confirma el aviso de que una vez presentada no podrá modificarla.
  5. El sistema muestra el «Justificante de presentación»; el alumno anota el número del expediente y copia la dirección de su ficha.
  6. El alumno cierra sesión.
  7. El administrativo «administrativo1@mislata.es» inicia sesión con contraseña «demo1234».
  8. Abre «Tramitación → Secretaría → Abiertos».
  9. El sistema no muestra ningún expediente con el número anotado, ni ningún expediente del centro «CIPFP Batoi».
  10. Pega en el navegador la dirección de la ficha de ese expediente.
  11. El sistema muestra el error genérico de acceso y no muestra el expediente.

## HU-005 — Como responsable del centro quiero que un expediente solo pueda cambiar a través de su tramitación para que nadie altere su estado, su centro o sus datos saltándose el procedimiento

- ESC-021 — Un alumno no puede modificar su propio expediente fuera de la tramitación:
  1. El alumno «alumno1@mislata.es» inicia sesión con contraseña «demo1234».
  2. Abre «Mis trámites → Nuevo trámite», elige el centro «CIPFP Mislata» y el trámite «Anulación de matrícula en ciclo formativo» y pulsa «Crear expediente».
  3. En «Datos del alumno/a» rellena el NIA con «12345678», la dirección con «C/ Dolores Ibarruri, 32», el municipio con «Mislata» y el código postal con «46920»; en «Matrícula que se anula» elige el ciclo formativo «Desarrollo de Aplicaciones Web».
  4. Pulsa «Siguiente».
  5. Pulsa «Firmar y presentar la solicitud» y confirma el aviso.
  6. El sistema muestra el «Justificante de presentación» y el alumno anota el número del expediente.
  7. Envía a la aplicación, fuera de las pantallas, una petición de guardado directo sobre el expediente con ese número que cambia su estado a «Anulación aceptada» (fase «Cierre»), su centro a «CIPFP Batoi» y su presentador a «alumno2@mislata.es».
  8. El sistema rechaza la petición con el error genérico de acceso.
  9. El alumno abre «Mis trámites → En tramitación».
  10. El sistema muestra el expediente con ese número en el estado «Pendiente de verificación», en el centro «CIPFP Mislata» y presentado por «alumno1@mislata.es».
- ESC-022 — Nadie salvo la tramitación (y el administrador) puede crear un expediente directamente:
  1. El administrativo «administrativo1@mislata.es» inicia sesión con contraseña «demo1234».
  2. Abre «Tramitación → Secretaría → Abiertos» y anota cuántos expedientes muestra y cuáles son del trámite «Anulación de matrícula en ciclo formativo».
  3. Envía a la aplicación, fuera de las pantallas, una petición de creación directa de un expediente del trámite «Anulación de matrícula en ciclo formativo» en el centro «CIPFP Mislata», con el alumno «Alumno1 CIPFP Mislata» (DNI «95591733F») como interesado, el ciclo formativo «Desarrollo de Aplicaciones Web» y el estado «Pendiente de verificación».
  4. El sistema rechaza la petición con el error genérico de acceso.
  5. Abre «Tramitación → Secretaría → Abiertos».
  6. El sistema muestra exactamente los mismos expedientes que anotó, sin ninguno nuevo.
- ESC-023 — La tramitación sigue moviendo el expediente (no-regresión):
  1. El alumno «alumno1@mislata.es» inicia sesión con contraseña «demo1234».
  2. Abre «Mis trámites → Nuevo trámite», elige el centro «CIPFP Mislata» y el trámite «Anulación de matrícula en ciclo formativo» y pulsa «Crear expediente».
  3. En «Datos del alumno/a» rellena el NIA con «12345678», la dirección con «C/ Dolores Ibarruri, 32», el municipio con «Mislata» y el código postal con «46920»; en «Matrícula que se anula» elige el ciclo formativo «Desarrollo de Aplicaciones Web».
  4. Pulsa «Siguiente».
  5. Pulsa «Firmar y presentar la solicitud» y confirma el aviso.
  6. El sistema muestra el «Justificante de presentación» y el alumno anota el número del expediente.
  7. El alumno cierra sesión.
  8. El administrativo «administrativo1@mislata.es» inicia sesión con contraseña «demo1234» y abre «Tramitación → Secretaría → Abiertos».
  9. Abre el expediente con el número anotado, que está en el estado «Pendiente de verificación».
  10. En «Verificación de la solicitud» marca «La solicitud es correcta» y pulsa «Siguiente».
  11. El sistema muestra el expediente en el estado «Pendiente de resolución».
- ESC-036 — Nadie salvo el administrador puede borrar un expediente directamente:
  1. El alumno «alumno1@mislata.es» inicia sesión con contraseña «demo1234».
  2. Abre «Mis trámites → Nuevo trámite», elige el centro «CIPFP Mislata» y el trámite «Anulación de matrícula en ciclo formativo» y pulsa «Crear expediente».
  3. En «Datos del alumno/a» rellena el NIA con «12345678», la dirección con «C/ Dolores Ibarruri, 32», el municipio con «Mislata» y el código postal con «46920»; en «Matrícula que se anula» elige el ciclo formativo «Desarrollo de Aplicaciones Web».
  4. Pulsa «Siguiente», después «Firmar y presentar la solicitud» y confirma el aviso.
  5. El sistema muestra el «Justificante de presentación» y el alumno anota el número del expediente.
  6. El alumno cierra sesión.
  7. El administrativo «administrativo1@mislata.es» inicia sesión con contraseña «demo1234».
  8. Envía a la aplicación, fuera de las pantallas, una petición de borrado directo del expediente con ese número.
  9. El sistema rechaza la petición con el error genérico de acceso.
  10. Abre «Tramitación → Secretaría → Abiertos».
  11. El sistema sigue mostrando el expediente con el número anotado, en el estado «Pendiente de verificación».

## HU-006 — Como visitante sin sesión quiero seguir pudiendo darme de alta y descargar un documento con el código de su QR para que las entradas públicas no se cierren

- ESC-024 — El alta de usuarios sin sesión sigue abierta:
  1. Sin iniciar sesión, un visitante abre la dirección de la aplicación.
  2. El sistema muestra la pantalla de inicio de sesión con el enlace «Registrarse».
  3. El visitante pulsa «Registrarse».
  4. El sistema muestra el formulario de alta sin pedir que inicie sesión.
  5. El visitante rellena los datos obligatorios del formulario de alta con valores válidos de una persona que no existe en la aplicación y pulsa el botón que envía el alta.
  6. El sistema acepta el alta y continúa su proceso sin pedir sesión ni mostrar el error genérico de acceso.
- ESC-025 — La descarga por QR del registro de salida sigue abierta *(manual: requiere emitir un documento de registro de salida firmado con el certificado del centro)*:
  1. El alumno «alumno1@mislata.es» inicia sesión con contraseña «demo1234».
  2. Abre «Mis trámites → Nuevo trámite», elige el centro «CIPFP Mislata» y el trámite «Anulación de matrícula en ciclo formativo», pulsa «Crear expediente», rellena el NIA «12345678», la dirección «C/ Dolores Ibarruri, 32», el municipio «Mislata», el código postal «46920» y el ciclo «Desarrollo de Aplicaciones Web», pulsa «Siguiente», pulsa «Firmar y presentar la solicitud» y confirma el aviso.
  3. El alumno cierra sesión.
  4. El administrativo «administrativo1@mislata.es» inicia sesión con contraseña «demo1234», abre «Tramitación → Secretaría → Abiertos», abre el expediente recién presentado, elige «La solicitud es correcta» y pulsa «Siguiente», elige «Aceptar la anulación», pulsa «Enviar a la firma» y confirma.
  5. El administrativo cierra sesión.
  6. El secretario «secretario@mislata.es» inicia sesión con contraseña «demo1234», abre «Firmas → Pendientes», abre la tarea de la resolución y firma todos los documentos y finaliza; después cierra sesión.
  7. El director «director@mislata.es» inicia sesión con contraseña «demo1234», abre «Firmas → Pendientes», abre la tarea de la resolución y firma todos los documentos y finaliza.
  8. El sistema anota la resolución firmada en el registro de salida con su código QR; el director copia la dirección del QR.
  9. Sin iniciar sesión, un visitante abre la dirección del QR del documento.
  10. El sistema entrega el documento sin pedir sesión.
- ESC-026 — Ninguna otra pantalla se abre sin sesión:
  1. El profesor «profesor1@mislata.es» inicia sesión con contraseña «demo1234».
  2. Abre «Firmas → Pendientes» y copia la dirección que muestra el navegador.
  3. Cierra sesión.
  4. Sin iniciar sesión, un visitante pega en el navegador la dirección copiada de «Firmas → Pendientes».
  5. El sistema le lleva a la pantalla de inicio de sesión y no muestra ninguna tarea de firma.
  6. El visitante inicia sesión en esa misma pantalla como «profesor1@mislata.es» con contraseña «demo1234».
  7. El sistema muestra «Firmas → Pendientes» con normalidad, sin error de acceso.
- ESC-037 — Una petición directa de datos sin sesión es rechazada:
  1. Sin iniciar sesión, un visitante envía a la aplicación, fuera de las pantallas, la petición que lista las notificaciones del centro «CIPFP Mislata».
  2. El sistema no devuelve ninguna notificación y responde indicando que hace falta iniciar sesión.
  3. El visitante envía, también sin sesión, la petición que lee la ficha del usuario «alumno1@mislata.es» y la petición de guardado directo que cambia su nombre a «Cambiado».
  4. El sistema rechaza ambas indicando que hace falta iniciar sesión y no devuelve ningún dato del usuario.
  5. El administrador inicia sesión con usuario «admin» y contraseña «admin», abre «Administración → Usuarios» y abre el usuario «alumno1@mislata.es».
  6. El sistema muestra su nombre original «Alumno1 CIPFP Mislata», sin ningún cambio.

## HU-007 — Como responsable del centro quiero que toda denegación quede anotada en un registro de seguridad para poder auditar los intentos de acceso indebido

- ESC-027 — Una denegación queda anotada en el registro de seguridad *(manual: requiere leer el fichero de log de seguridad del servidor)*:
  1. El responsable del servidor anota la fecha y hora actuales del servidor, para distinguir la anotación de esta prueba de las anteriores.
  2. El profesor «profesor1@mislata.es» inicia sesión con contraseña «demo1234».
  3. Envía a la aplicación, fuera de las pantallas, la petición que abre el listado «Firmas → Del centro» del centro «CIPFP Mislata».
  4. El sistema responde con el error genérico de acceso y no devuelve ninguna tarea de firma.
  5. El responsable del servidor abre el fichero de log de seguridad de la semana en curso.
  6. El fichero contiene una anotación nueva, posterior a la hora anotada en el paso 1, con la fecha y hora de la petición, el usuario «profesor1@mislata.es», el recurso denegado identificado como el listado «Firmas → Del centro», el motivo de la denegación (que el usuario no tiene concedido ese acceso) y la dirección desde la que llegó la petición.
  7. La anotación no contiene ningún otro dato personal del usuario: ni su nombre, ni sus apellidos, ni su documento, ni su contraseña.
  8. El responsable del servidor abre el fichero de log general de la aplicación.
  9. Ese fichero no contiene la anotación de la denegación: solo está en el fichero de log de seguridad.
- ESC-028 — Las operaciones permitidas no se anotan en el registro de seguridad *(manual: requiere leer el fichero de log de seguridad del servidor)*:
  1. El responsable del servidor anota la fecha y hora actuales del servidor.
  2. El profesor «profesor1@mislata.es» inicia sesión con contraseña «demo1234».
  3. Abre «Firmas → Pendientes».
  4. El sistema muestra el listado de sus tareas de firma pendientes sin error de acceso.
  5. El responsable del servidor abre el fichero de log de seguridad de la semana en curso.
  6. El fichero no contiene ninguna anotación con el usuario «profesor1@mislata.es» posterior a la hora anotada; si el fichero de la semana en curso aún no existe, también se da por cumplido.
- ESC-038 — La denegación se aplica aunque no se pueda escribir en el registro de seguridad *(manual: requiere impedir la escritura del fichero de log de seguridad en el servidor)*:
  1. El responsable del servidor comprueba que existe el fichero de log de seguridad de la semana en curso; si no existe, provoca antes una denegación (los pasos 2 a 4 de ESC-027) para que se cree.
  2. Quita el permiso de escritura al fichero de log de seguridad de la semana en curso.
  3. El profesor «profesor1@mislata.es» inicia sesión con contraseña «demo1234».
  4. Envía a la aplicación, fuera de las pantallas, la petición que abre el listado «Firmas → Del centro» del centro «CIPFP Mislata».
  5. El sistema responde con el error genérico de acceso y no devuelve ninguna tarea de firma.
  6. El profesor abre «Firmas → Pendientes».
  7. El sistema muestra el listado con normalidad, sin ningún error.
  8. El responsable del servidor devuelve el permiso de escritura al fichero de log de seguridad.
  9. El profesor vuelve a enviar a la aplicación, fuera de las pantallas, la petición que abre el listado «Firmas → Del centro» del centro «CIPFP Mislata».
  10. El sistema responde con el error genérico de acceso.
  11. El responsable del servidor abre el fichero de log de seguridad de la semana en curso.
  12. El fichero contiene una anotación nueva de esta última denegación con el usuario «profesor1@mislata.es».

## HU-008 — Como responsable del proyecto quiero que el trámite de prueba desaparezca de la aplicación para que no quede ningún trámite de ejemplo accesible

- ESC-029 — El trámite de prueba ya no se puede elegir:
  1. El profesor «profesor1@mislata.es» inicia sesión con contraseña «demo1234».
  2. Abre «Mis trámites → Nuevo trámite».
  3. Elige el centro «CIPFP Mislata».
  4. El sistema ofrece «Justificación de falta del profesorado» y no ofrece «Trámite de prueba».
  5. Envía a la aplicación, fuera de las pantallas, la petición de creación directa de un expediente del trámite «Trámite de prueba» en el centro «CIPFP Mislata».
  6. El sistema rechaza la petición y no crea ningún expediente.
  7. Abre «Mis trámites → En tramitación» y «Mis trámites → Finalizados».
  8. El sistema no muestra ningún expediente del trámite «Trámite de prueba».
  9. El profesor cierra sesión.
  10. El profesor «profesor1@batoi.es» inicia sesión con contraseña «demo1234».
  11. Abre «Mis trámites → Nuevo trámite» y elige el centro «CIPFP Batoi».
  12. El sistema ofrece «Justificación de falta del profesorado» y no ofrece «Trámite de prueba».
- ESC-039 — El trámite de prueba no aparece en ningún listado:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre «Administración → Perfiles de trámites por centro».
  3. Pulsa «Nuevo» y elige el centro «CIPFP Mislata».
  4. Despliega el campo «Trámite».
  5. El sistema ofrece «Anulación de matrícula en ciclo formativo» y «Justificación de falta del profesorado» y no ofrece «Trámite de prueba».
  6. Cancela sin guardar.
  7. Pulsa «Nuevo» y elige el centro «CIPFP Batoi».
  8. Despliega el campo «Trámite».
  9. El sistema no ofrece «Trámite de prueba».
  10. Cancela sin guardar.
  11. Abre «Tramitación → Todos los centros → Todos» y despliega el filtro de la columna «trámite».
  12. El sistema ofrece como valores de filtro los trámites existentes y no ofrece «Trámite de prueba».
  13. Cierra sesión.
  14. El supervisor «supervisor1@mislata.es» inicia sesión con contraseña «demo1234» y abre «Mi centro → Perfiles de trámites».
  15. Pulsa «Nuevo» y despliega el campo «Trámite».
  16. El sistema ofrece «Anulación de matrícula en ciclo formativo» y «Justificación de falta del profesorado» y no ofrece «Trámite de prueba».
  17. Cancela sin guardar y cierra sesión.
  18. El director «director@mislata.es» inicia sesión con contraseña «demo1234».
  19. Abre «Tramitación → Trámite en papel» y elige el centro «CIPFP Mislata».
  20. El sistema ofrece «Anulación de matrícula en ciclo formativo» y «Justificación de falta del profesorado» y no ofrece «Trámite de prueba».

## HU-009 — Como responsable del proyecto quiero que una definición de seguridad incoherente impida construir la aplicación para que ningún error de permisos llegue a los usuarios

- ESC-040 — Una concesión sobre una pantalla que no existe impide construir la aplicación *(manual: requiere modificar la definición de seguridad y construir la aplicación)*:
  1. El responsable del proyecto construye la aplicación sin ningún cambio en la definición de seguridad y la construcción termina con normalidad.
  2. Añade a la definición de seguridad una concesión al Supervisor sobre una pantalla llamada «Inventario» que no existe en la aplicación.
  3. Construye la aplicación.
  4. La construcción se detiene con error y la aplicación no queda construida ni se puede arrancar.
  5. El mensaje de error nombra la pantalla «Inventario», indica que no corresponde a ninguna pantalla existente, nombra al destinatario de la concesión (Supervisor) y señala el punto exacto de la definición de seguridad donde está declarada.
  6. Retira la concesión sobre «Inventario», dejando la definición de seguridad exactamente como estaba antes del paso 2.
  7. Vuelve a construir la aplicación.
  8. La construcción termina con normalidad.

# Requisitos

- REQ-001 — Un usuario solo puede ver y ejecutar los menús, pantallas, acciones y datos que se le han concedido de forma explícita; todo lo no concedido está prohibido.
  - etiqueta: seguridad
- REQ-002 — El administrador puede ejecutar cualquier acción de datos y ver y editar todos los datos de todos los centros sin que haya que concedérselo; en cambio, en el menú solo ve las entradas que se le conceden expresamente.
  - etiqueta: seguridad
  - actor: Administrador
- REQ-003 — Cada tipo de usuario y cargo ve exactamente las mismas entradas del menú principal que antes del cambio.
  - etiqueta: compatibilidad
- REQ-004 — Una entrada de menú que agrupa otras solo se muestra si el usuario puede ver al menos una de las que agrupa.
  - etiqueta: comportamiento
- REQ-005 — Quien no ve una entrada de menú tampoco puede abrir su pantalla ni ejecutar sus acciones por su dirección, por una petición directa ni por ningún otro camino.
  - etiqueta: seguridad
- REQ-006 — Ante una acción no concedida el sistema responde con el error genérico de acceso de la aplicación, no ejecuta la acción y no deja ningún efecto.
  - etiqueta: comportamiento
- REQ-007 — No existe una pantalla propia de «sin permiso»: el usuario que llega a una denegación ve solo el error genérico.
  - etiqueta: comportamiento
- REQ-008 — Fuera del administrador, ningún usuario ve ni modifica datos de un centro al que no pertenece.
  - etiqueta: seguridad
- REQ-009 — Un usuario que pertenece a varios centros ve los datos de todos ellos, distinguidos por la columna «centro».
  - etiqueta: comportamiento
- REQ-010 — Cada usuario ve sus propias notificaciones, tareas de firma y registros; quien gestiona el centro (supervisor, director, jefe de estudios, secretario, administrativo) ve además los de su centro.
  - etiqueta: seguridad
- REQ-011 — El acceso a cada tipo de dato (ver, crear, modificar, borrar) se decide de forma explícita durante la implementación, parte por parte, siguiendo los principios de este documento y con la revisión del responsable del proyecto; ningún tipo de dato queda con el acceso actual sin revisar.
  - etiqueta: seguridad
- REQ-012 — Fuera del administrador, un expediente solo cambia a través de las acciones de su tramitación; nadie puede crearlo, modificarlo ni borrarlo directamente, ni cambiar su estado, su fase, su centro o su presentador saltándose la tramitación, ni aun forzando la aplicación.
  - etiqueta: seguridad
- REQ-013 — Un expediente que un usuario puede ver se puede seguir viendo, y la tramitación lo sigue moviendo igual que antes.
  - etiqueta: compatibilidad
- REQ-014 — Sin sesión iniciada solo son accesibles el alta de usuarios y la descarga del documento del registro de salida mediante el código de su QR; cualquier otra dirección lleva a la pantalla de inicio de sesión.
  - etiqueta: seguridad
- REQ-015 — Toda denegación se anota en un fichero de log propio de seguridad, separado del resto de la aplicación, con un fichero por semana.
  - etiqueta: seguridad
- REQ-016 — Cada anotación del registro de seguridad contiene la fecha y hora, el usuario, el recurso denegado, el motivo de la denegación y la dirección desde la que llegó la petición, y ningún otro dato personal.
  - etiqueta: seguridad
- REQ-017 — Los ficheros semanales del registro de seguridad se conservan un año.
  - etiqueta: seguridad
- REQ-018 — Las operaciones permitidas no se anotan en el registro de seguridad.
  - etiqueta: comportamiento
- REQ-019 — El registro de seguridad solo lo consulta quien administra el servidor; no hay pantalla en la aplicación para verlo.
  - etiqueta: seguridad
- REQ-020 — La seguridad se define al construir la aplicación: no existe ninguna pantalla para conceder o quitar permisos, y cambiarlos es una modificación de la aplicación.
  - etiqueta: comportamiento
- REQ-021 — Una definición de seguridad incoherente (un permiso sobre algo que no existe, una concesión repetida, un acceso sin sesión fuera de las dos entradas públicas, una concesión sin destinatario…) impide construir la aplicación y el error indica dónde está el problema.
  - etiqueta: comportamiento
- REQ-022 — Toda acción y toda pantalla nuevas que se añadan a la aplicación nacen prohibidas hasta que se declara quién puede usarlas.
  - etiqueta: seguridad
- REQ-023 — Los expedientes, notificaciones, tareas de firma y registros que ya existen al desplegar el cambio siguen siendo visibles para quienes podían verlos antes.
  - etiqueta: datos existentes
- REQ-024 — Los usuarios que ya existen al desplegar el cambio conservan sus tipos de usuario, cargos y perfiles de tramitación, que son lo que decide su acceso.
  - etiqueta: datos existentes
- REQ-025 — Si no se puede escribir en el registro de seguridad, la denegación se aplica igualmente y la aplicación sigue funcionando.
  - etiqueta: comportamiento
- REQ-026 — Los textos de error que ve el usuario se muestran en su idioma.
  - etiqueta: textos
- REQ-027 — El trámite «Trámite de prueba» deja de existir en la aplicación: no se puede elegir al crear un trámite ni aparece en ningún listado.
  - etiqueta: comportamiento
- REQ-028 — Los documentos adjuntos conservan el comportamiento actual: cualquier usuario con sesión que conozca el número de un documento puede descargarlo. Es un riesgo conocido y aceptado hasta la iniciativa que lo resuelva.
  - etiqueta: compatibilidad
- REQ-029 — Un listado devuelve únicamente los registros que el usuario puede ver, sin producir error aunque existan otros que no puede ver; en cambio, abrir o actuar directamente sobre un registro concreto que no tiene concedido produce el error genérico de acceso.
  - etiqueta: comportamiento
- REQ-030 — Que un listado muestre menos registros de los que existen porque el usuario no puede verlos no es una denegación y no se anota en el registro de seguridad.
  - etiqueta: comportamiento
- REQ-031 — El error genérico de acceso no revela si el dato o la pantalla pedidos existen, a qué centro pertenecen ni qué permiso faltaba; es el mismo error en todos los casos.
  - etiqueta: seguridad
- REQ-032 — El administrador sí puede crear, modificar y borrar un expediente directamente, fuera de la tramitación; es la única excepción a REQ-012.
  - etiqueta: seguridad
  - actor: Administrador
- REQ-033 — Una petición de datos o de acción enviada fuera de las pantallas sin sesión iniciada se rechaza indicando que hace falta iniciar sesión y no devuelve ningún dato ni ejecuta nada.
  - etiqueta: seguridad
  - actor: Visitante sin sesión
- REQ-034 — Una denegación producida sin sesión iniciada se anota igualmente en el registro de seguridad, indicando que no había usuario identificado en lugar del usuario.
  - etiqueta: seguridad
- REQ-035 — Cuando la sesión de un usuario caduca, cualquier acción posterior se trata como realizada sin sesión: las pantallas llevan al inicio de sesión y las peticiones directas se rechazan.
  - etiqueta: comportamiento
- REQ-036 — Cuando cambian el tipo de usuario, los cargos o los perfiles de tramitación de un usuario, su acceso refleja el cambio como muy tarde en su siguiente inicio de sesión, tanto para ganar como para perder menús, pantallas, acciones y datos.
  - etiqueta: comportamiento
- REQ-037 — Un usuario con sesión que todavía no pertenece a ningún centro (por ejemplo, recién dado de alta) ve únicamente las entradas comunes a todos («Mis trámites», «Firmas» y «Notificaciones → Recibidas») y nada de gestión ni de tramitación.
  - etiqueta: seguridad
- REQ-038 — Al darse de alta sin sesión, el visitante no puede fijar su tipo de usuario, sus cargos, sus perfiles de tramitación ni los centros a los que pertenece (`AllowProperties`); esos datos solo los concede después la gestión del centro o la administración.
  - etiqueta: seguridad
  - actor: Visitante sin sesión
- REQ-039 — Quien tramita en varios centros ve y tramita, en cada unidad tramitadora, solo los expedientes de los centros en los que tiene perfil de tramitación para esa unidad, no los de todos sus centros.
  - etiqueta: seguridad
- REQ-040 — Al desplegar el cambio, los expedientes, perfiles de tramitación y demás datos ya existentes del trámite «Trámite de prueba» se eliminan junto con el trámite, y la aplicación arranca con normalidad aunque existieran.
  - etiqueta: datos existentes

# Seguridad

- **Administrador:** ve y edita todos los datos de todos los centros (incluidos los expedientes, también fuera de la tramitación) y ejecuta cualquier acción de datos; en el menú ve las entradas de administración («Administración», «Criptografía», «Sistema educativo», «Desarrollador», «Firmas → Todas», «Notificaciones → Todas», «Notificaciones → Del centro», «Tramitación → Trámite en papel», «Tramitación → Pendientes de mí», «Tramitación → Todos los centros», «Registro») y las comunes a todos. Alcance: todos los centros.
- **Supervisor:** ve «Firmas → Del centro», «Notificaciones → Del centro», «Registro» y «Mi centro», y gestiona los usuarios y perfiles de trámites de su centro; además lo común a todos. Alcance: solo sus centros.
- **Director, Secretario, Jefe de estudios y Administrativo:** ven «Notificaciones → Del centro» y gestionan las notificaciones de su centro; además lo común a todos. Alcance: solo sus centros.
- **Quien tramita trámites de Secretaría** (hoy los administrativos, el director y el secretario): ve «Tramitación» con «Trámite en papel», «Pendientes de mí» y «Secretaría», y «Registro». Alcance: solo los centros donde tiene ese perfil.
- **Quien tramita trámites de Jefatura de estudios** (hoy los jefes de estudios, el director y el secretario): ve «Tramitación» con «Trámite en papel», «Pendientes de mí» y «Jefatura de estudios». Alcance: solo los centros donde tiene ese perfil.
- **Todos los usuarios con sesión** (Profesor, Alumno, Familiar, Exalumno, Exprofesor, Externo y los anteriores, incluidos los que aún no pertenecen a ningún centro): ven «Mis trámites», «Firmas» («Pendientes», «Firmadas», «Rechazadas») y «Notificaciones → Recibidas», y acceden a sus propios expedientes, tareas de firma y notificaciones. Alcance: solo sus propios registros, en sus centros.
- **Visitante sin sesión:** alta de usuarios (sin poder fijar su tipo, cargos, perfiles ni centros) y descarga del documento del registro de salida por el código de su QR. Nada más.
- **Responsable del servidor:** lee el fichero de log de seguridad en el servidor.
- **Administración de la seguridad:** no hay ningún rol que la administre desde la aplicación; se define al construirla (REQ-020).

# Recursos y datos iniciales

- Los tipos de usuario, cargos y perfiles de tramitación de cada usuario en cada centro, tal como se cargan hoy (incluidos los de demo: centros «CIPFP Mislata» y «CIPFP Batoi» y sus usuarios).
- La definición de seguridad de cada parte de la aplicación forma parte de la propia aplicación y se carga al arrancar; no se introduce desde ninguna pantalla.

# Fuera de alcance

- **Documentos adjuntos (ficheros)**: no se toca su acceso. Se acepta el riesgo conocido de que cualquier usuario con sesión pueda descargar un documento por su número; se resolverá en otra iniciativa.
- **Completar el alta de usuarios sin sesión**: está a medio hacer y se termina aparte; aquí solo se le exige no permitir fijar tipo, cargos, perfiles ni centros.
- **Pantalla de consulta del registro de seguridad**: el log solo se lee en el servidor.
- **Conceder permisos a personas concretas** (por nombre o documento) desde la aplicación.
- **Pantalla de administración de permisos**: la seguridad no se cambia desde la aplicación.
- **Qué registro concreto ve cada usuario dentro de su centro**, más allá de los principios de este documento (lo propio, lo del centro para quien gestiona): sigue decidiéndolo cada pantalla y la tramitación, como hoy.
- **La tramitación de expedientes** (estados, eventos, perfiles de tramitación): no cambia; sigue siendo quien decide qué puede hacer cada usuario sobre cada expediente.
- **El gestor documental (DMS)**: no se usa y no se abre.
