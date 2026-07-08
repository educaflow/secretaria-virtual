---
type: implementation-task
---

# Tarea 04 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas
- k-secure-coding
- k-code-quality

## Ficheros que cubre esta tarea (filas de la tabla "Ficheros a crear o modificar" de `design.md`)

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/subsystem/correos/service/CorreoService.java` | Crear | k-sistemas (servicios.md) | Interfaz del servicio de `Correo` |
| `src/main/java/com/educaflow/subsystem/correos/service/impl/CorreoServiceImpl.java` | Crear | k-sistemas (servicios.md) | Implementación del servicio de `Correo` |

> **Nota para `/sdd-implementer`:** los XML de `domains/`, `views/` y `menus.xml` ya están materializados en la carpeta `design/`. **MUST NOT** modificarlos, reescribirlos ni regenerarlos: se **copian verbatim** a su ubicación final. El código Java es lo único que se implementa a partir de las firmas y comentarios de este diseño.

## Texto del diseño (verbatim, `design.md`, Paso 3 — parte de `CorreoService`/`CorreoServiceImpl`)

### `com.educaflow.subsystem.correos.service.CorreoService`

```java
package com.educaflow.subsystem.correos.service;

public interface CorreoService extends com.axelor.db.modelservice.ModelService<com.educaflow.subsystem.correos.db.Correo> {

    // Envía (o reintenta) el correo indicado por su id. Misma función para el envío inicial (la
    // invoca R-Correo-001 tras el alta) y para el reintento (la invoca R-Correo-002 tras "reenviar").
    // Invocable también de forma programática desde otros subsistemas (design-guidelines).
    void enviarCorreo(Long correoId);

    // Devuelve todos los correos en estado FAIL (para un futuro reenvío en bloque; design-guidelines).
    java.util.List<com.educaflow.subsystem.correos.db.Correo> listarCorreosEnFail();

    // Acción propia: reintenta el envío de un correo en FAIL. Invocada desde CorreoController.reenviar.
    com.educaflow.subsystem.correos.db.Correo reenviar(com.educaflow.subsystem.correos.db.Correo entidad,
                                                        com.educaflow.subsystem.correos.db.Correo entidadOriginal);

    java.util.Optional<com.axelor.db.modelservice.BusinessMessages> validateReenviar(
            com.educaflow.subsystem.correos.db.Correo entidad,
            com.educaflow.subsystem.correos.db.Correo entidadOriginal);

    com.axelor.db.modelservice.AllowProperties allowPropertiesReenviar();
}
```

### `com.educaflow.subsystem.correos.service.impl.CorreoServiceImpl`

```java
package com.educaflow.subsystem.correos.service.impl;

public class CorreoServiceImpl extends com.axelor.db.modelservice.DefaultModelService<Correo> implements CorreoService {

    @jakarta.inject.Inject
    com.educaflow.base.infrastructure.mail.MailSender mailSender;

    @jakarta.inject.Inject
    com.educaflow.subsystem.correos.infrastructure.CorreoAsyncExecutor correoAsyncExecutor;

    // Constructor obligatorio — ModelServiceFactory lo invoca por reflexión
    public CorreoServiceImpl(Class<Correo> model, Repository<Correo> repository) { super(model, repository); }

    @Override
    public Correo insert(Correo correo);
    //   Sobrescribe insert para encadenar las R- propias del alta. NUNCA llama a super.insert
    //   (ver k-sistemas/servicios.md "Persistir: siempre repository, nunca super.*").
    //   Secuencia: validateInsert(correo).ifPresent(throwIfInvalid);
    //              fireActionRule_AsignarValoresIniciales(correo);      // Antes
    //              correo = repository.save(correo);
    //              fireActionRule_ProgramarEnvioAsincrono(correo);      // Después (R-Correo-001, compleja)
    //              return correo;

    @Override
    public Correo update(Correo nuevo, Correo original);
    //   El Correo es inmutable tras su creación (ver intro de entity-Correo.md y "Acción: Modificar"
    //   con Input AllowProperties vacío — Origen spec: —, ver Notas y supuestos). Lanza incondicionalmente
    //   UnsupportedOperationException(I18n.get("El correo es inmutable tras su creación.")) — patrón gemelo
    //   de validateUpdate (k-secure-coding §9.2).

    @Override
    public void remove(Correo correo);
    //   RES-Correo-003 ("un correo nunca se puede borrar"): lanza incondicionalmente
    //   UnsupportedOperationException(I18n.get("Los correos no se pueden borrar.")).

    @Override
    public Correo reenviar(Correo entidad, Correo entidadOriginal);
    //   Secuencia: validateReenviar(entidad, entidadOriginal).ifPresent(throwIfInvalid);
    //              fireActionRule_ProgramarReenvioAsincrono(entidadOriginal);   // Después (R-Correo-002, compleja)
    //              return entidadOriginal;
    //   MUST NOT llamar a repository.save: esta acción no cambia ningún campo de forma síncrona
    //   (todo el cambio de estado ocurre dentro de enviarCorreo, ver rules/R-Correo-002.md y
    //   "Notas y supuestos" — excepción documentada al patrón general).

    @Override
    public void enviarCorreo(Long correoId);
    //   Método compartido por R-Correo-001 y R-Correo-002. Diseño detallado completo en
    //   design/rules/R-Correo-001.md (diagrama de secuencia, construcción del Mail, tratamiento
    //   de errores). Se ejecuta SIEMPRE envuelto en com.axelor.db.JPA.runInTransaction(() -> { ... })
    //   (reentrante: si ya hay transacción activa, no abre una nueva).
    //   Secuencia dentro del runInTransaction:
    //     1. Correo correo = repository.find(correoId); si es null o su estado ya es SUCCESS, return
    //        (idempotencia — SUCCESS es terminal).
    //     2. fireActionRule_RegistrarIntentoEnvio(correo);   // CC-Correo-002/003/005, Antes
    //     3. Mail mail = construirMail(correo);              // ver "Otras funciones"
    //     4. try { mailSender.send(mail); fireActionRule_MarcarEnvioCorrecto(correo); }
    //        catch (RuntimeException ex) { fireActionRule_MarcarEnvioFallido(correo, ex); }
    //     5. repository.save(correo);

    @Override
    public java.util.List<Correo> listarCorreosEnFail();
    //   return ((CorreoRepository) repository).findByEstado(EstadoCorreo.FAIL); — delega en el
    //   finder-method del dominio (k-sistemas/servicios.md "las consultas con filtros van en el
    //   repositorio"); no hace falta una clase CorreoRepository propia porque el finder-method ya
    //   genera el método en el repositorio autogenerado — el cast es al tipo generado por Axelor.

    /****************************************************************************************/
    /******************************** Métodos de Validación *********************************/
    /****************************************************************************************/

    @Override
    public java.util.Optional<BusinessMessages> validateInsert(Correo correo);
    //   Aplica (en este orden), acumulando en BusinessMessages (ninguna lanza excepción por sí sola):
    //     - V-Correo-001 (Origen spec: VAL-Correo-001) dniDestinatario obligatorio.
    //       Mensaje debe transmitir: "el DNI del destinatario es obligatorio" (sin valor recibido,
    //       no hay nada que mostrar).
    //     - V-Correo-002 (Origen spec: VAL-Correo-015) dniDestinatario válido: solo si el anterior
    //       pasó, comprobar com.educaflow.base.util.DniUtil.isValid(dniDestinatario). Mensaje
    //       literal exacto: "El DNI del destinatario no es válido; compruebe la letra" (sin
    //       interpolar el DNI recibido — ver test-unit-desc.md).
    //     - V-Correo-003 (Origen spec: VAL-Correo-006) nombre obligatorio.
    //     - V-Correo-004 (Origen spec: VAL-Correo-007) apellidos obligatorios.
    //     - V-Correo-005 (Origen spec: VAL-Correo-002) para: al menos una dirección tras separar
    //       por comas (ver "Otras funciones", separarDirecciones). Mensaje: debe indicar al menos
    //       un destinatario en el «para».
    //     - V-Correo-006 (Origen spec: VAL-Correo-011) cada dirección de "para" tiene formato válido
    //       (com.educaflow.base.util.EMailUtil.isValid por cada dirección separada). Mensaje literal
    //       exacto: "El «para» debe contener direcciones de correo válidas (por ejemplo,
    //       usuario@dominio.com)" (sin interpolar la dirección recibida — ver test-unit-desc.md).
    //     - V-Correo-007 (Origen spec: VAL-Correo-012) si "enCopia" tiene valor, cada dirección
    //       válida (mismo mecanismo que V-Correo-006). Mensaje literal exacto: "El «en copia» debe
    //       contener direcciones de correo válidas" (sin interpolar la dirección recibida — ver
    //       test-unit-desc.md).
    //     - V-Correo-008 (Origen spec: VAL-Correo-013) si "enCopiaOculta" tiene valor, cada dirección
    //       válida. Mensaje literal exacto: "El «en copia oculta» debe contener direcciones de correo
    //       válidas" (sin interpolar la dirección recibida — ver test-unit-desc.md).
    //     - V-Correo-009 (Origen spec: VAL-Correo-003) asunto obligatorio.
    //     - V-Correo-010 (Origen spec: VAL-Correo-016) asunto <= 255 caracteres. Mensaje literal
    //       exacto: "El asunto no puede superar 255 caracteres" (sin interpolar la longitud
    //       recibida — ver test-unit-desc.md).
    //     - V-Correo-011 (Origen spec: VAL-Correo-004) cuerpo obligatorio.
    //     - V-Correo-012 (Origen spec: VAL-Correo-005) centro obligatorio.
    //     - V-Correo-013 (Origen spec: VAL-Correo-008) si el usuario actual NO es Administrador
    //       (com.educaflow.base.util.SecurityUtil.isAdmin(SecurityUtil.getUser())), el centro indicado
    //       MUST ser uno de los centros del usuario (recorrer SecurityUtil.getUser().getCentroUsuarios()
    //       comparando su centro con el indicado). Mensaje: "no puede crear correos para un centro
    //       que no es suyo".
    //     - V-Correo-014 (Origen spec: VAL-Correo-014) si "historialEstado" tiene valor, comprobar
    //       que existe de verdad con JpaRepository.of(HistorialEstado.class).find(id) != null (lookup
    //       en BD, permitido en validate* — ver k-validaciones/validaciones.md tabla capa servidor).
    //   Mensajes en el orden en que la spec los describe; ninguno bloquea la comprobación de los demás.
    //   NOTA: la comprobación de unicidad de nombreFichero entre adjuntos (RES-Adjunto-001) NO vive
    //   aquí — vive en AdjuntoServiceImpl.validateInsert (V-Adjunto-006), la entidad dueña de la
    //   restricción (ver k-validaciones/validaciones.md "las validaciones del detalle se escriben
    //   una vez, en el servicio del detalle").

    @Override
    public java.util.Optional<BusinessMessages> validateUpdate(Correo nuevo, Correo original);
    //   V-Correo-015 (Origen spec: —, ver Notas y supuestos): no hay condición — SIEMPRE devuelve un
    //   único mensaje ("El correo es inmutable tras su creación."). Patrón gemelo de update()
    //   (k-secure-coding §9.2): entidad que nunca admite la operación.

    @Override
    public java.util.Optional<BusinessMessages> validateRemove(Correo correo);
    //   V-Correo-016 (Origen spec: RES-Correo-003): SIEMPRE devuelve un único mensaje ("Los correos
    //   no se pueden borrar.").

    @Override
    public java.util.Optional<BusinessMessages> validateReenviar(Correo entidad, Correo entidadOriginal);
    //   Aplica sobre entidadOriginal (el estado real en BD; entidad solo trae el id, ver
    //   allowPropertiesReenviar):
    //     - V-Correo-017 (Origen spec: VAL-Correo-009) entidadOriginal.getEstado() == FAIL. Mensaje:
    //       "solo se pueden reenviar correos que han fallado".
    //     - V-Correo-018 (Origen spec: VAL-Correo-010) si el usuario actual NO es Administrador,
    //       entidadOriginal.getCentro() MUST estar entre los centros del usuario (mismo mecanismo
    //       que V-Correo-013). Mensaje: "no puede reenviar correos de un centro que no es suyo".

    /**************************************************************************************/
    /********************************   AllowProperties   *********************************/
    /**************************************************************************************/

    @Override
    public AllowProperties allowPropertiesInsert();
    //   Whitelist (createAllowProperties) — ver sección "Frontera de confianza" más abajo para la
    //   tabla completa: dniDestinatario, nombre, apellidos, para, enCopia, enCopiaOculta, asunto,
    //   cuerpo, centro, historialEstado, adjuntos{nombreFichero, contenido}.

    @Override
    public AllowProperties allowPropertiesReenviar();
    //   Whitelist vacía: createAllowProperties(Map.of()) — reenviar no acepta ningún dato del
    //   cliente más allá del id (que ActionRequestHelper resuelve siempre, con independencia de la
    //   whitelist).

    /*************************************************************************************/
    /********************************    Action Rules    *********************************/
    /*************************************************************************************/

    private void fireActionRule_AsignarValoresIniciales(Correo correo);
    //   Aplica R-Correo-003 (Origen spec: CC-Correo-001, CC-Correo-005; el estado inicial PENDIENTE
    //   lo describe la propia RN-Correo-001 como precondición de su primera frase). Asignación
    //   INCONDICIONAL (campos servidor, ver k-secure-coding §3.3):
    //     correo.setEstado(EstadoCorreo.PENDIENTE);
    //     correo.setFechaCreacion(LocalDateTime.now());
    //     correo.setNumeroReintentos(0);
    //     correo.setFechaPrimerIntentoEnvio(null);
    //     correo.setFechaUltimoIntentoEnvio(null);
    //     correo.setFechaEnvio(null);
    //     correo.setDescripcionUltimoFallo(null);
    //   MUST NOT añadir guarda if (correo.getEstado() == null): el cliente NO puede dictar estos
    //   campos aunque vengan rellenos en el JSON del endpoint REST genérico.

    private void fireActionRule_ProgramarEnvioAsincrono(Correo correo);
    //   Implementa R-Correo-001. Diseño detallado en design/rules/R-Correo-001.md.

    private void fireActionRule_ProgramarReenvioAsincrono(Correo correo);
    //   Implementa R-Correo-002. Diseño detallado en design/rules/R-Correo-002.md.

    private void fireActionRule_RegistrarIntentoEnvio(Correo correo);
    //   Aplica R-Correo-004 (Origen spec: CC-Correo-002, CC-Correo-003, CC-Correo-005). Parte del
    //   mecanismo compartido de envío — ver design/rules/R-Correo-001.md, diagrama de "enviarCorreo".
    //   Asignación INCONDICIONAL:
    //     correo.setFechaUltimoIntentoEnvio(LocalDateTime.now());
    //     if (correo.getFechaPrimerIntentoEnvio() == null) { correo.setFechaPrimerIntentoEnvio(LocalDateTime.now()); }
    //       (este "if" NO es el antipatrón de mass-assignment: no depende de lo que mande el
    //        cliente — correoId es el único parámetro externo — sino de si YA existe un primer
    //        intento registrado en BD; es la propia semántica de CC-Correo-002: "se fija una sola
    //        vez, en el primer intento")
    //     correo.setNumeroReintentos(correo.getNumeroReintentos() + 1);

    private void fireActionRule_MarcarEnvioCorrecto(Correo correo);
    //   Aplica R-Correo-004 (Origen spec: CC-Correo-004; RES-Correo-002 — fechaEnvio solo con
    //   SUCCESS, garantizado por construcción: es el único punto del código que fija fechaEnvio).
    //   Asignación INCONDICIONAL (campos servidor, ver k-secure-coding §3.3):
    //     correo.setEstado(EstadoCorreo.SUCCESS);
    //     correo.setFechaEnvio(LocalDateTime.now());
    //     correo.setDescripcionUltimoFallo(null);
    //   MUST NOT añadir guarda if (correo.getEstado() == null) ni similar: el cliente NO puede
    //   dictar estos campos aunque vengan rellenos en el JSON del endpoint REST genérico.

    private void fireActionRule_MarcarEnvioFallido(Correo correo, RuntimeException excepcion);
    //   Aplica R-Correo-004 (Origen spec: CC-Correo-006).
    //   Asignación INCONDICIONAL (campos servidor, ver k-secure-coding §3.3):
    //     correo.setEstado(EstadoCorreo.FAIL);
    //     correo.setDescripcionUltimoFallo(trazaCompleta(excepcion));   // ver "Otras funciones"
    //     correo.setFechaEnvio(null);   // RES-Correo-002: nunca hay fecha de envío fuera de SUCCESS
    //   MUST NOT añadir guarda if (correo.getEstado() == null) ni similar: el cliente NO puede
    //   dictar estos campos aunque vengan rellenos en el JSON del endpoint REST genérico.

    /*************************************************************************************/
    /********************************    Otras funciones    ******************************/
    /*************************************************************************************/

    private java.util.List<String> separarDirecciones(String direcciones);
    //   Divide por comas, recorta espacios y descarta vacíos. Usado por validateInsert (V-Correo-005
    //   a 008) y por construirMail.

    private com.educaflow.base.infrastructure.mail.Mail construirMail(Correo correo);
    //   to = separarDirecciones(correo.getPara()); cc = separarDirecciones(correo.getEnCopia());
    //   bcc = separarDirecciones(correo.getEnCopiaOculta()) — cada campo del Correo va a su propia
    //   cabecera MIME real (Mail.cc()/Mail.bcc(), ampliado en Paso 1); ya NO se fusionan en una
    //   única lista "to" (ver design/rules/R-Correo-001.md, "Notas de esta regla").
    //   from = AppSettings.get().get("mail.address.from"); subject = correo.getAsunto();
    //   textBody = htmlBody = correo.getCuerpo(); attachs = correo.getAdjuntos().stream()
    //     .map(a -> new Attach(a.getNombreFichero(),
    //                           MetaFileUtil.downloadContent(a.getContenido()),
    //                           a.getContenido().getFileType()))
    //     .toList();

    private String trazaCompleta(Throwable excepcion);
    //   java.io.StringWriter sw = new java.io.StringWriter();
    //   excepcion.printStackTrace(new java.io.PrintWriter(sw));
    //   return sw.toString();
}
```

Diseño detallado completo de `fireActionRule_ProgramarEnvioAsincrono` (R-Correo-001, incluye diagrama de secuencia y análisis de alternativas del mecanismo asíncrono): `design/rules/R-Correo-001.md`. Diseño detallado completo de `fireActionRule_ProgramarReenvioAsincrono` (R-Correo-002): `design/rules/R-Correo-002.md`. **MUST** leer ambos ficheros antes de implementar `insert`/`reenviar`/`enviarCorreo`/las dos `fireActionRule_Programar*`.

## Frontera de confianza — AllowProperties por acción (verbatim, `design.md`)

### `CorreoServiceImpl.insert` (invocado a través del endpoint REST automático `/ws/rest/com.educaflow.subsystem.correos.db.Correo`, no de un `@CallMethod` propio)

Entidad: `Correo`. **Forma elegida**: `createAllowProperties` (whitelist).
**Origen spec:** `Input AllowProperties` de la acción `Crear` de `entity-Correo.md`.

| Campo | Origen | En whitelist | Justificación / Ubicación de la asignación |
|---|---|---|---|
| `dniDestinatario` | cliente | sí | Input directo del formulario |
| `nombre` | cliente | sí | Input directo del formulario |
| `apellidos` | cliente | sí | Input directo del formulario |
| `para` | cliente | sí | Input directo del formulario |
| `enCopia` | cliente | sí | Input directo del formulario |
| `enCopiaOculta` | cliente | sí | Input directo del formulario |
| `asunto` | cliente | sí | Input directo del formulario |
| `cuerpo` | cliente | sí | Input directo del formulario |
| `centro` | cliente | sí | Input directo (dropdown); validado en `validateInsert` (V-Correo-013), no sobrescrito — el Administrador puede elegir cualquiera, el resto solo el suyo |
| `historialEstado` | cliente | sí | Input opcional; validado en `validateInsert` (V-Correo-014) |
| `adjuntos.nombreFichero` | cliente | sí | Input del formulario del `Adjunto` embebido |
| `adjuntos.contenido` | cliente | sí | Input del formulario del `Adjunto` embebido (subida de fichero) |
| `estado` | servidor | **NO** | Asignada en `CorreoServiceImpl.insert` → `fireActionRule_AsignarValoresIniciales` |
| `fechaCreacion` | servidor | **NO** | Asignada en `fireActionRule_AsignarValoresIniciales` |
| `fechaPrimerIntentoEnvio` | servidor | **NO** | Asignada (a `null`) en `fireActionRule_AsignarValoresIniciales`; luego en `fireActionRule_RegistrarIntentoEnvio` dentro de `enviarCorreo` |
| `fechaUltimoIntentoEnvio` | servidor | **NO** | Asignada en `fireActionRule_RegistrarIntentoEnvio` |
| `fechaEnvio` | servidor | **NO** | Asignada en `fireActionRule_MarcarEnvioCorrecto` |
| `numeroReintentos` | servidor | **NO** | Asignada en `fireActionRule_AsignarValoresIniciales` (a 0) y en `fireActionRule_RegistrarIntentoEnvio` (incremento) |
| `descripcionUltimoFallo` | servidor | **NO** | Asignada en `fireActionRule_MarcarEnvioCorrecto` (a `null`) y en `fireActionRule_MarcarEnvioFallido` |
| `nombreExpediente` | servidor (derivado) | **NO** | Campo de solo lectura (CC-Correo-007, `momento: lectura`); no se persiste, no puede estar en ninguna whitelist |

### `CorreoServiceImpl.reenviar` (invocado desde `CorreoController.reenviar`)

Entidad: `Correo`. **Forma elegida**: `createAllowProperties` con mapa vacío.
**Origen spec:** la acción `Reenviar` de `entity-Correo.md` no declara ningún `Input AllowProperties` (solo validaciones y una regla de negocio): no hay ningún campo que el cliente pueda dictar en esta acción, todo el cambio de estado lo decide el servidor dentro de `enviarCorreo`.

| Campo | Origen | En whitelist | Justificación |
|---|---|---|---|
| *(ninguno)* | — | — | `allowPropertiesReenviar()` = `createAllowProperties(Map.of())`; `ActionRequestHelper.getModel(...)` sigue resolviendo el `id` con independencia del contenido de la whitelist |

## Trazabilidad Origen spec → V/R → ubicación (verbatim, `design.md`, filas de `Correo`)

### V- (validaciones)

| V | Origen spec | Ubicación |
|---|---|---|
| V-Correo-001 | VAL-Correo-001 | `CorreoServiceImpl.validateInsert` |
| V-Correo-002 | VAL-Correo-015 | `CorreoServiceImpl.validateInsert` |
| V-Correo-003 | VAL-Correo-006 | `CorreoServiceImpl.validateInsert` |
| V-Correo-004 | VAL-Correo-007 | `CorreoServiceImpl.validateInsert` |
| V-Correo-005 | VAL-Correo-002 | `CorreoServiceImpl.validateInsert` |
| V-Correo-006 | VAL-Correo-011 | `CorreoServiceImpl.validateInsert` |
| V-Correo-007 | VAL-Correo-012 | `CorreoServiceImpl.validateInsert` |
| V-Correo-008 | VAL-Correo-013 | `CorreoServiceImpl.validateInsert` |
| V-Correo-009 | VAL-Correo-003 | `CorreoServiceImpl.validateInsert` |
| V-Correo-010 | VAL-Correo-016 | `CorreoServiceImpl.validateInsert` |
| V-Correo-011 | VAL-Correo-004 | `CorreoServiceImpl.validateInsert` |
| V-Correo-012 | VAL-Correo-005 | `CorreoServiceImpl.validateInsert` |
| V-Correo-013 | VAL-Correo-008 | `CorreoServiceImpl.validateInsert` |
| V-Correo-014 | VAL-Correo-014 | `CorreoServiceImpl.validateInsert` |
| V-Correo-015 | — (inmutabilidad, intro de `entity-Correo.md` + Acción Modificar sin `Input AllowProperties`) | `CorreoServiceImpl.validateUpdate` |
| V-Correo-016 | RES-Correo-003 | `CorreoServiceImpl.validateRemove` |
| V-Correo-017 | VAL-Correo-009 | `CorreoServiceImpl.validateReenviar` |
| V-Correo-018 | VAL-Correo-010 | `CorreoServiceImpl.validateReenviar` |
| V-Correo-019 | RES-Correo-001 | `domains/Correo.xml` `<many-to-one name="centro" ref="com.educaflow.subsystem.common.db.Centro">` (declarativo: la clave foránea JPA impide persistir un `centro` que no exista) |

### R- (reglas de negocio / campos calculados de escritura)

| R | Origen spec | Ubicación |
|---|---|---|
| R-Correo-001 | RN-Correo-001 | `CorreoServiceImpl.fireActionRule_ProgramarEnvioAsincrono` (Después de `repository.save` en `insert`). Detalle: `design/rules/R-Correo-001.md` |
| R-Correo-002 | RN-Correo-002 | `CorreoServiceImpl.fireActionRule_ProgramarReenvioAsincrono` (Después, en `reenviar`). Detalle: `design/rules/R-Correo-002.md` |
| R-Correo-003 | CC-Correo-001, CC-Correo-005 | `CorreoServiceImpl.fireActionRule_AsignarValoresIniciales` (Antes, en `insert`) |
| R-Correo-004 | CC-Correo-002, CC-Correo-003, CC-Correo-004, CC-Correo-005, CC-Correo-006, RES-Correo-002 | `CorreoServiceImpl.fireActionRule_RegistrarIntentoEnvio` + `fireActionRule_MarcarEnvioCorrecto` + `fireActionRule_MarcarEnvioFallido` (dentro de `enviarCorreo`; mecanismo compartido — ver `design/rules/R-Correo-001.md`). RES-Correo-002 ("la fecha de envío solo tiene valor cuando el estado es SUCCESS") queda garantizada por construcción: `fireActionRule_MarcarEnvioCorrecto` es el único punto que fija `fechaEnvio` y `fireActionRule_MarcarEnvioFallido` la fuerza a `null` |

## Superficie cerrada

**MUST** crear únicamente `CorreoService` (interfaz) y `CorreoServiceImpl` (implementación) con exactamente los métodos/firmas listados arriba. **MUST NOT** crear una clase `CorreoRepository` propia (el finder-method `findByEstado` ya genera el método en el repositorio autogenerado — Paso 4 del diseño, "Ninguno propio"). **MUST NOT** inventar métodos, endpoints o clases auxiliares no listados aquí. Si detectas que hace falta algo no listado, **detente y reporta** `BLOCKED`.
