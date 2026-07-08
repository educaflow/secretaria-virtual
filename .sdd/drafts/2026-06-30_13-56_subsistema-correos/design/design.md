---
type: design
---

# Diseño: Subsistema de correos

**Objetivo:** crear el subsistema `correos`, que envía correos electrónicos dejando constancia permanente de cada envío (contenido + resultado de cada intento), con envío asíncrono, reintento manual y tres pantallas (administración, centro, destinatario).
**Capa:** subsystem/correos
**Especificación de origen:** .sdd/drafts/2026-06-30_13-56_subsistema-correos/specification.md
**Skills necesarios para la implementación:** k-sistemas, k-code-quality, k-secure-coding, k-vistas, k-guice, k-datainit

## Ficheros a crear o modificar

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/resources/axelor-config.properties` | Modificar | — | Añadir `mail.address.from` y `mail.send.pool-size` (ver Notas y supuestos). `correos.envio.cron` ya existía y **no** se usa en este diseño (fuera de alcance el job periódico). |
| `src/main/java/com/educaflow/base/infrastructure/mail/Mail.java` | Modificar | k-code-quality | Ampliar el record con `cc`/`bcc` reales (constructor de compatibilidad de 6 argumentos) — ver Paso 1 y `design/rules/R-Correo-001.md` |
| `src/main/java/com/educaflow/base/infrastructure/mail/impl/JavaMailHelper.java` | Modificar | k-code-quality | Añadir las cabeceras MIME `Message.RecipientType.CC`/`BCC` cuando `mail.cc()`/`mail.bcc()` no vienen vacías — ver Paso 1 |
| `src/main/java/com/educaflow/base/infrastructure/mail/impl/MailSenderImpl.java` | Modificar | k-code-quality | Enviar a `message.getAllRecipients()` en vez de solo `RecipientType.TO`, para que CC/BCC se entreguen de verdad por SMTP — ver Paso 1 |
| `src/main/java/com/educaflow/subsystem/correos/domains/Correo.xml` | Crear | k-sistemas (modelos.md) | Entidad `Correo` |
| `src/main/java/com/educaflow/subsystem/correos/domains/Adjunto.xml` | Crear | k-sistemas (modelos.md) | Entidad `Adjunto` |
| `src/main/java/com/educaflow/subsystem/correos/service/CorreoService.java` | Crear | k-sistemas (servicios.md) | Interfaz del servicio de `Correo` |
| `src/main/java/com/educaflow/subsystem/correos/service/impl/CorreoServiceImpl.java` | Crear | k-sistemas (servicios.md) | Implementación del servicio de `Correo` |
| `src/main/java/com/educaflow/subsystem/correos/service/AdjuntoService.java` | Crear | k-sistemas (servicios.md) | Interfaz del servicio de `Adjunto` |
| `src/main/java/com/educaflow/subsystem/correos/service/impl/AdjuntoServiceImpl.java` | Crear | k-sistemas (servicios.md) | Implementación del servicio de `Adjunto` |
| `src/main/java/com/educaflow/subsystem/correos/controller/CorreoController.java` | Crear | k-sistemas (controladores.md) | `@CallMethod` de `reenviar` |
| `src/main/java/com/educaflow/subsystem/correos/infrastructure/CorreoAsyncExecutor.java` | Crear | k-guice | Executor gestionado para el envío asíncrono |
| `src/main/java/com/educaflow/subsystem/correos/infrastructure/PostCommitRunner.java` | Crear | — | Utilidad estática: ejecutar una tarea tras el commit de la transacción actual |
| `src/main/java/com/educaflow/subsystem/correos/infrastructure/CorreoEventObserver.java` | Crear | k-guice | Observador de arranque/parada de la aplicación (ciclo de vida del executor) |
| `src/main/java/com/educaflow/subsystem/correos/module/CorreosModule.java` | Crear | k-guice | Módulo Guice: bindings de `MailSender`, `CorreoAsyncExecutor`, `CorreoEventObserver` |
| `src/main/java/com/educaflow/subsystem/correos/module/MailSenderProvider.java` | Crear | k-guice | `Provider<MailSender>` que lee la configuración SMTP |
| `src/main/java/com/educaflow/subsystem/correos/module/CorreoAsyncExecutorProvider.java` | Crear | k-guice | `Provider<CorreoAsyncExecutor>` que lee `mail.send.pool-size` |
| `src/main/java/com/educaflow/subsystem/correos/views/Correo.xml` | Crear | k-vistas (forms.md, grids.md) | Pantalla «Administración de correos» (`@Main`) + vistas embebidas de `Adjunto` en alta (`@Main`) |
| `src/main/java/com/educaflow/subsystem/correos/views/Correo-Centro.xml` | Crear | k-vistas (forms.md, grids.md) | Pantalla «Correos de mi centro» (`@Centro`) |
| `src/main/java/com/educaflow/subsystem/correos/views/Correo-Mis.xml` | Crear | k-vistas (forms.md, grids.md) | Pantalla «Mis correos» (`@Mis`) |
| `src/main/java/com/educaflow/subsystem/correos/views/Adjunto-ref.xml` | Crear | k-vistas (forms.md) | Vistas de solo lectura/referencia de `Adjunto` (`@Search-grid`/`@View-form`), compartidas por Centro y Mis |
| `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` | Modificar | k-vistas (menus.md) | Añadir menú «Correos» (2 hojas) y menú raíz «Mis correos» |
| `src/main/java/com/educaflow/subsystem/correos/data-init/input-config.xml` | Crear | k-datainit | Manifiesto de datos iniciales (solo permisos, sin datos maestros) |
| `src/main/java/com/educaflow/subsystem/correos/data-init/input/auth-correos.xml` | Crear | k-datainit | Permisos de `Correo`/`Adjunto` (ver paso 10, Seguridad) |

> **Nota para `/sdd-implementer`:** los XML de `domains/`, `views/` y `menus.xml` ya están materializados en la carpeta `design/`. **MUST NOT** modificarlos, reescribirlos ni regenerarlos: se **copian verbatim** a su ubicación final (`menus.xml` se fusiona en el `menus.xml` único del proyecto). El código Java es lo único que se implementa a partir de las firmas y comentarios de este diseño.

## Pasos

### Paso 1 — Configuración e infraestructura de correo (cc/bcc reales)

Añadir a `axelor-config.properties` (junto a `correos.envio.cron`, ya existente):

```properties
mail.address.from = secretariavirtual@fpmislata.com
mail.send.pool-size = 2
```

**Ampliar `base/infrastructure/mail` para soportar `cc`/`bcc` reales** (en vez de fusionar `para`+`enCopia`+`enCopiaOculta` en la única lista `Mail.to()`, que expondría cada destinatario "en copia oculta" a todos los demás en la cabecera `To` — ver `design/rules/R-Correo-001.md`, sección "Notas de esta regla", para el análisis completo). Cambios:

```java
// Fichero: src/main/java/com/educaflow/base/infrastructure/mail/Mail.java
package com.educaflow.base.infrastructure.mail;

public record Mail(java.util.List<String> to, java.util.List<String> cc, java.util.List<String> bcc,
                    String from, String subject, String htmlBody, String textBody,
                    java.util.List<Attach> attachs) {

    public Mail(java.util.List<String> to, String from, String subject, String htmlBody,
                String textBody, java.util.List<Attach> attachs);
    // Constructor de compatibilidad (firma de 6 argumentos, sin cc/bcc) — delega en el canónico
    // con cc=List.of() y bcc=List.of(). Preserva sin cambios el único llamador real existente
    // (com.educaflow.subsystem.registroentradasalida.service.impl.RegistroSalidaServiceImpl,
    // que sigue construyendo Mail con new Mail(to, from, subject, body, body, attachs)).
}
```

```java
// Fichero: src/main/java/com/educaflow/base/infrastructure/mail/impl/JavaMailHelper.java
// Método: getMessage(Mail mail, Session session) [firma existente, sin cambios]
//   Tras la línea existente message.setRecipients(Message.RecipientType.TO, getAddresses(mail.to())),
//   añadir (mismo helper getAddresses(List<String>) ya usado para TO):
//     if (mail.cc() != null && !mail.cc().isEmpty()) {
//         message.setRecipients(jakarta.mail.Message.RecipientType.CC, getAddresses(mail.cc()));
//     }
//     if (mail.bcc() != null && !mail.bcc().isEmpty()) {
//         message.setRecipients(jakarta.mail.Message.RecipientType.BCC, getAddresses(mail.bcc()));
//     }
```

```java
// Fichero: src/main/java/com/educaflow/base/infrastructure/mail/impl/MailSenderImpl.java
// Método: send(Mail mail) [firma existente, sin cambios]
//   Cambiar la línea real de envío SMTP:
//     ANTES:    transport.sendMessage(message, message.getRecipients(jakarta.mail.Message.RecipientType.TO));
//     DESPUÉS:  transport.sendMessage(message, message.getAllRecipients());
//   MUST cambiar esta línea: el envío efectivo por SMTP usa la lista de destinatarios pasada a
//   sendMessage(...), no las cabeceras del Message — si no se cambia, cc/bcc quedarían escritos
//   en las cabeceras MIME pero NUNCA se entregarían de verdad a esos destinatarios.
```

**Verificar:** `grep -c "correos.envio" src/main/resources/axelor-config.properties` devuelve `3`; `grep -n "cc, java.util.List<String> bcc" src/main/java/com/educaflow/base/infrastructure/mail/Mail.java` encuentra la firma ampliada; `grep -n "getAllRecipients" src/main/java/com/educaflow/base/infrastructure/mail/impl/MailSenderImpl.java` confirma el cambio de envío.

### Paso 2 — Dominios

Fichero `domains/Correo.xml` (completo en `design/domains/Correo.xml`). Resumen estructural: entidad `Correo` con los datos del destinatario (`dniDestinatario`, `nombre`, `apellidos`, texto libre, sin ficha de persona), las direcciones (`para`/`enCopia`/`enCopiaOculta`, cada una una lista de direcciones separadas por comas en un único `<string large="true">`), `asunto`/`cuerpo`, `centro` (`many-to-one` a `com.educaflow.subsystem.common.db.Centro`), `historialEstado` opcional (`many-to-one` a `com.educaflow.subsystem.expedientes.db.HistorialEstado`), `adjuntos` (`one-to-many` a `Adjunto`, `mappedBy="correo"`), los campos de resultado del envío (`estado` enum `EstadoCorreo`, `fechaCreacion`, `fechaPrimerIntentoEnvio`, `fechaUltimoIntentoEnvio`, `fechaEnvio`, `numeroReintentos`, `descripcionUltimoFallo`) y el campo derivado de solo lectura `nombreExpediente` (CC-Correo-007, `formula="true"` con subselect SQL sobre `expedientes_historial_estado`/`expedientes_expediente` — no cuerpo Java, porque el cálculo navega dos relaciones a otras tablas y se usa como columna de grid; ver `k-validaciones/reglas-negocio.md` §2.2). Enum `EstadoCorreo` con `PENDIENTE`/`SUCCESS`/`FAIL`. `finder-method findByEstado`.

Fichero `domains/Adjunto.xml` (completo en `design/domains/Adjunto.xml`). Resumen: entidad `Adjunto` con `nombreFichero`, `contenido` (`many-to-one` a `com.axelor.meta.db.MetaFile`, patrón estándar de adjuntos del proyecto — ver `k-sistemas/modelos.md`), `correo` (`many-to-one` a `Correo`), y `<unique-constraint columns="correo,nombreFichero"/>` (RES-Adjunto-001, declarativo).

**Ninguno de los campos "obligatorios" del negocio (`dniDestinatario`, `nombre`, `apellidos`, `para`, `asunto`, `cuerpo`, `centro`, `nombreFichero`, `contenido`) lleva `required="true"`.** Ver "Notas y supuestos" — la obligatoriedad la exige el `VAL-` correspondiente en `validateInsert`, no el atributo declarativo, para que el mensaje de negocio (no un genérico de JPA) sea siempre el que se muestra.

**Verificar:** `bash .claude/skills/sdd-designer/template-system/validate.sh <carpeta> ` (o el `xmllint` equivalente) valida ambos ficheros contra `domain-models.xsd`.

### Paso 3 — Servicios

#### `com.educaflow.subsystem.correos.service.CorreoService`

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

#### `com.educaflow.subsystem.correos.service.impl.CorreoServiceImpl`

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
    //   con Input AllowProperties vacío — Origen spec: —, ver Notas y supuestos).
    //   Lanza incondicionalmente UnsupportedOperationException(I18n.get("El correo es inmutable
    //   tras su creación.")) — patrón gemelo de validateUpdate (k-secure-coding §9.2).

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

#### `com.educaflow.subsystem.correos.service.AdjuntoService`

```java
package com.educaflow.subsystem.correos.service;

public interface AdjuntoService extends com.axelor.db.modelservice.ModelService<com.educaflow.subsystem.correos.db.Adjunto> {
    // Sin acciones propias: solo se sobrescriben validateInsert/Update/Remove y allowPropertiesInsert
    // (ver k-sistemas/servicios.md — no hace falta redeclarar insert/update/remove ni sus
    // validate*/allowProperties* si no se añade una acción nueva).
}
```

#### `com.educaflow.subsystem.correos.service.impl.AdjuntoServiceImpl`

```java
package com.educaflow.subsystem.correos.service.impl;

public class AdjuntoServiceImpl extends com.axelor.db.modelservice.DefaultModelService<Adjunto> implements AdjuntoService {

    public AdjuntoServiceImpl(Class<Adjunto> model, Repository<Adjunto> repository) { super(model, repository); }

    // (sin bloque de "Acciones": Adjunto no tiene ninguna acción propia más allá de las
    // sobrescrituras de update/remove — ver k-sistemas/servicios.md, bloque 1 vacío es válido)

    @Override
    public Adjunto update(Adjunto nuevo, Adjunto original);
    //   Inmutable tras su creación (Acción "Modificar" con Input AllowProperties vacío en
    //   entity-Adjunto.md). Lanza incondicionalmente UnsupportedOperationException.

    @Override
    public void remove(Adjunto adjunto);
    //   "Como un correo nunca se borra, sus adjuntos tampoco" (intro de entity-Correo.md, análogo a
    //   RES-Correo-003). Lanza incondicionalmente UnsupportedOperationException.

    /****************************************************************************************/
    /******************************** Métodos de Validación *********************************/
    /****************************************************************************************/

    @Override
    public java.util.Optional<BusinessMessages> validateInsert(Adjunto adjunto);
    //   Aplica (en este orden):
    //     - V-Adjunto-001 (Origen spec: VAL-Adjunto-001) adjunto.getCorreo() != null. Mensaje: "el
    //       adjunto debe pertenecer a un correo".
    //     - V-Adjunto-002 (Origen spec: VAL-Adjunto-002) si el usuario actual NO es Administrador,
    //       adjunto.getCorreo().getCentro() MUST estar entre los centros del usuario (mismo mecanismo
    //       que V-Correo-013 de CorreoServiceImpl). Mensaje: "no puede añadir adjuntos a correos de
    //       un centro que no es suyo".
    //     - V-Adjunto-003 (Origen spec: VAL-Adjunto-003) adjunto.getCorreo().getFechaCreacion() ==
    //       null. fechaCreacion es un campo servidor que CorreoServiceImpl solo asigna DESPUÉS de
    //       validar todo el árbol (ModelServiceValidationWalker valida los detalles ANTES de que el
    //       insert() del maestro llegue a fireActionRule_AsignarValoresIniciales) — por eso
    //       fechaCreacion == null distingue con fiabilidad "el correo se está creando ahora, en esta
    //       misma petición" de "el correo ya existía de antes" (ver Notas y supuestos: es una
    //       inferencia sobre el orden de ejecución del framework, MUST verificarse empíricamente al
    //       implementar). Mensaje: "no se pueden añadir adjuntos a un correo ya existente".
    //     - V-Adjunto-004 (Origen spec: VAL-Adjunto-004) nombreFichero obligatorio.
    //     - V-Adjunto-005 (Origen spec: VAL-Adjunto-005) contenido obligatorio.
    //     - V-Adjunto-006 (Origen spec: RES-Adjunto-001) recorrer adjunto.getCorreo().getAdjuntos()
    //       (JPA.edit ya ha ensamblado el grafo completo, incluidas las referencias bidireccionales,
    //       antes de que ModelServiceValidationWalker valide cada detalle) y comprobar que ningún
    //       hermano tiene el mismo nombreFichero (trim, comparación exacta; se compara contra los
    //       demás, nunca contra sí mismo). Mensaje: "ya existe un adjunto con ese nombre en el correo"
    //       (defensa en profundidad además del <unique-constraint> declarativo de Adjunto.xml).
    //       Ubicada aquí, en la entidad dueña de la restricción (k-validaciones/validaciones.md
    //       "las validaciones del detalle se escriben una vez, en el servicio del detalle"), no en
    //       CorreoServiceImpl.

    @Override
    public java.util.Optional<BusinessMessages> validateUpdate(Adjunto nuevo, Adjunto original);
    //   V-Adjunto-007 (Origen spec: —, ver Notas y supuestos): SIEMPRE rechaza.

    @Override
    public java.util.Optional<BusinessMessages> validateRemove(Adjunto adjunto);
    //   V-Adjunto-008 (Origen spec: —, análogo a RES-Correo-003): SIEMPRE rechaza.

    /**************************************************************************************/
    /********************************   AllowProperties   *********************************/
    /**************************************************************************************/

    @Override
    public AllowProperties allowPropertiesInsert();
    //   createAllowProperties(Map.of("nombreFichero", Map.of(), "contenido", Map.of(), "correo", Map.of()))
    //   — Adjunto no tiene ningún campo servidor (ver Frontera de confianza).
}
```

### Paso 4 — Repositorios

Ninguno propio: el finder `findByEstado` de `Correo` (paso 2) genera su método directamente en el repositorio autogenerado (`com.educaflow.subsystem.correos.db.repo.CorreoRepository`); no hace falta escribir una clase Java (k-sistemas/modelos.md, `<finder-method>`).

### Paso 5 — Controladores

#### `com.educaflow.subsystem.correos.controller.CorreoController`

```java
package com.educaflow.subsystem.correos.controller;

public class CorreoController {

    @com.google.inject.Inject
    private com.axelor.db.modelservice.ModelServiceFactory modelServiceFactory;

    @com.axelor.meta.CallMethod
    public void validateReenviar(ActionRequest actionRequest, ActionResponse actionResponse);
    //   Resuelve CorreoService; ActionRequestHelper<Correo> con getOriginalModel(); llama a
    //   correoService.validateReenviar(entidadOriginal, entidadOriginal) [se compara consigo misma:
    //   no hay "nuevo" distinto de "original" en esta acción, ver k-sistemas/controladores.md patrón
    //   type1]; si Optional.isPresent(), actionResponseHelper.doResponseBusinessMessagesAsError(...).
    //   Delega en CorreoService.validateReenviar — NO valida inline (k-sistemas/controladores.md
    //   "NO valida inline con throw new BusinessException").

    @com.axelor.meta.CallMethod
    @com.google.inject.persist.Transactional
    public void reenviar(ActionRequest actionRequest, ActionResponse actionResponse);
    //   final CorreoService correoService = (CorreoService) modelServiceFactory.resolve(Correo.class);
    //   ActionRequestHelper<Correo> actionRequestHelper = new ActionRequestHelper(actionRequest, Correo.class);
    //   Correo entidadOriginal = actionRequestHelper.getOriginalModel();
    //   Correo entidad = actionRequestHelper.getModel(correoService.allowPropertiesReenviar());
    //   correoService.reenviar(entidad, entidadOriginal);
    //   actionResponse.setNotify(I18n.get("El reenvío del correo se ha puesto en marcha."));
    //     (satisface RUI-correos-centro-formulario-004; no molesta en la pantalla de administración,
    //      que no exige nada al respecto)
    //   actionResponse.setSignal("refresh-tab", null);
}
```

No hay `AdjuntoController`: `Adjunto` no tiene ninguna acción propia (su alta la cubre el endpoint REST automático dentro del alta en cascada del `Correo`; su descarga la resuelve directamente el widget `binary-link` contra el endpoint de `MetaFile`, sin pasar por ningún controlador propio — ver `k-sistemas/modelos.md` y las vistas del paso 8).

**Verificar:** `grep -n "actionRequest, ActionResponse actionResponse" src/main/java/com/educaflow/subsystem/correos/controller/CorreoController.java` — los parámetros se llaman siempre `actionRequest`/`actionResponse`.

### Paso 6 — Módulo Guice

El envío asíncrono necesita tres piezas que **no** son `ModelService` y cuya construcción no es trivial (dependen de configuración): `MailSender` (credenciales SMTP), `CorreoAsyncExecutor` (tamaño de pool) y el observador de ciclo de vida. Ver `[[k-guice]]`.

#### `com.educaflow.subsystem.correos.infrastructure.CorreoAsyncExecutor`

```java
package com.educaflow.subsystem.correos.infrastructure;

public class CorreoAsyncExecutor {

    private final java.util.concurrent.ExecutorService executorService;

    public CorreoAsyncExecutor(int tamanoPool);
    //   Crea Executors.newFixedThreadPool(tamanoPool, threadFactory); threadFactory produce hilos
    //   daemon=true con nombre "correo-envio-N" (red de seguridad: si el hook de parada no llegara a
    //   ejecutarse, estos hilos no bloquean la parada de la JVM/Tomcat — design-guidelines exige
    //   evitar leaks de memoria/hilos).

    public void submit(Runnable tarea);
    //   executorService.submit(() -> { try { tarea.run(); } catch (RuntimeException ex) {
    //     log.error("Fallo no controlado en el envío asíncrono de un correo", ex); } });
    //   MUST NOT dejar que una excepción no capturada mate el hilo del pool.

    public void detener();
    //   executorService.shutdown(); if (!executorService.awaitTermination(10, TimeUnit.SECONDS))
    //     executorService.shutdownNow();
}
```

#### `com.educaflow.subsystem.correos.infrastructure.PostCommitRunner`

```java
package com.educaflow.subsystem.correos.infrastructure;

public final class PostCommitRunner {
    private PostCommitRunner() {}

    public static void runAfterCommit(Runnable tarea);
    //   org.hibernate.Session session = com.axelor.db.JPA.em().unwrap(org.hibernate.Session.class);
    //   session.getTransaction().registerSynchronization(new jakarta.transaction.Synchronization() {
    //     public void beforeCompletion() {}
    //     public void afterCompletion(int status) {
    //       if (status == jakarta.transaction.Status.STATUS_COMMITTED) { tarea.run(); }
    //     }
    //   });
    //   Diseño completo (por qué hace falta) en design/rules/R-Correo-001.md.
}
```

#### `com.educaflow.subsystem.correos.infrastructure.CorreoEventObserver`

```java
package com.educaflow.subsystem.correos.infrastructure;

public class CorreoEventObserver {

    @jakarta.inject.Inject
    private CorreoAsyncExecutor correoAsyncExecutor;

    public void onAppStart(@com.axelor.event.Observes com.axelor.events.StartupEvent event);
    //   log.info("Executor de envío de correos listo"); (el pool ya existe: lo crea el Provider la
    //   primera vez que Guice construye el CorreoAsyncExecutor)

    public void onAppShutdown(@com.axelor.event.Observes com.axelor.events.ShutdownEvent event);
    //   correoAsyncExecutor.detener();
}
```

#### `com.educaflow.subsystem.correos.module.MailSenderProvider`

```java
package com.educaflow.subsystem.correos.module;

public class MailSenderProvider implements jakarta.inject.Provider<com.educaflow.base.infrastructure.mail.MailSender> {
    @Override
    public com.educaflow.base.infrastructure.mail.MailSender get();
    //   com.axelor.app.AppSettings settings = com.axelor.app.AppSettings.get();
    //   var credencial = new SmtpCredentialSimplePassword(settings.get("mail.smtp.host"),
    //                                                      settings.get("mail.smtp.user"),
    //                                                      settings.get("mail.smtp.password"));
    //   return new MailSenderImpl(credencial);
    //   (mismo patrón que el ejemplo canónico de [[k-guice]] §3.3; centraliza la lectura de
    //   AppSettings/credenciales en el Provider, no dispersa por el servicio — k-secure-coding §8)
}
```

#### `com.educaflow.subsystem.correos.module.CorreoAsyncExecutorProvider`

```java
package com.educaflow.subsystem.correos.module;

public class CorreoAsyncExecutorProvider implements jakarta.inject.Provider<CorreoAsyncExecutor> {
    @Override
    public CorreoAsyncExecutor get();
    //   int tamanoPool = com.axelor.app.AppSettings.get().getInt("mail.send.pool-size", 2);
    //   return new CorreoAsyncExecutor(tamanoPool);
}
```

#### `com.educaflow.subsystem.correos.module.CorreosModule`

```java
package com.educaflow.subsystem.correos.module;

public class CorreosModule extends com.axelor.app.AxelorModule {
    @Override
    protected void configure();
    //   bind(com.educaflow.base.infrastructure.mail.MailSender.class).toProvider(MailSenderProvider.class);
    //   bind(CorreoAsyncExecutor.class).toProvider(CorreoAsyncExecutorProvider.class).in(com.google.inject.Singleton.class);
    //   bind(CorreoEventObserver.class);
    //   MUST NOT bindear CorreoService/AdjuntoService (son ModelService — los resuelve
    //   ModelServiceFactory, ver k-guice §2).
}
```

**Verificar:** `./run.sh` arranca sin `Guice/MissingConstructor` ni errores de bindeo; el log de arranque no muestra excepciones de `CorreosModule`.

### Paso 7 — Jobs programados

No aplica: el reenvío automático periódico está fuera de alcance (`correos.envio.cron` queda reservado en `axelor-config.properties`, sin `MetaSchedule` ni clase `Job` en este diseño).

### Paso 8 — Vistas

Ficheros completos en `design/views/`.

- **`views/Correo.xml`** (resumen): `subsysCorreos.Correo@Main-action` + `@Main-grid` (listado con estado/dni/nombre/apellidos/asunto/para/centro/expediente/fechaCreación/fechaEnvío, orden por fecha de creación descendente, `canNew="true"`) + `@Main-form` (panel «Correo» editable solo en alta —`readonlyIf="(id != null) || (cid != null)"` en todos sus campos, siguiendo la convención real del proyecto para distinguir un registro ya guardado (`id`) de uno en curso de creación en el cliente (`cid`), ver `k-vistas/forms.md`—, dos `panel-related` de `adjuntos` mutuamente excluyentes según `(id == null) && (cid == null)` / `(id != null) || (cid != null)` —uno editable con el `@Main-form` de `Adjunto` para la alta, otro de solo lectura con el `@View-form` compartido para la consulta—, panel «Envío» solo visible con `(id != null) || (cid != null)`, `buttons-panel` con `btnReenviar` (`showIf="estado == 'FAIL'"`), `btnDelete` oculto (`showIf="false"`: RES-Correo-003 hace que nunca se pueda borrar, así que el botón no tiene ninguna combinación de estado en la que deba verse — se documenta oculto en vez de eliminado para que la plantilla estándar de `buttons-panel` — k-vistas/forms.md — se mantenga reconocible) y los pares `btnCancelAlta`/`btnCancelSalir` y `btnSave` (`showIf="(id == null) && (cid == null)"` / `showIf="(id != null) || (cid != null)"`) que materializan RUI-correos-administracion-formulario-003/004/005). Además, embebidas en el mismo fichero: `subsysCorreos.Correo.Adjunto@Main-grid`/`@Main-form` (edición durante la alta, con `onNew` que fija `correo` = `__parent__` — RUI-correos-administracion-formulario-adjunto-001— y validación local de obligatorios — RUI-...-005/006; sus botones `btnCancelAlta`/`btnCancelSalir`/`btnSave` usan deliberadamente solo `id == null`/`id != null`, sin `cid` — ver "Notas y supuestos" sobre por qué extender `cid` a este modal introduciría un fallo real, y solo `btnDelete` sí usa `(id!=null) || (cid!=null)`, que es correcto porque un `Adjunto` recién añadido a la colección **sí** recibe un `cid` inmediatamente).
- **`views/Correo-Centro.xml`** (resumen): `subsysCorreos.Correo@Centro-action` con `<domain>self.centro = :centroActivoUsuario</domain>` (multi-centro real, k-secure-coding §4) + `@Centro-grid` (mismas columnas menos «centro», sin botón nuevo) + `@Centro-form` (100% de solo lectura, mismo panel «Envío», `btnReenviar` igual que en `@Main`, reutilizando el mismo `@CallMethod` — `CorreoController.reenviar` siempre emite el aviso de RUI-correos-centro-formulario-004).
- **`views/Correo-Mis.xml`** (resumen): `subsysCorreos.Correo@Mis-action` con `<domain>self.dniDestinatario = :dniUsuarioActual and self.estado = :estadoExitoso</domain>` + `@Mis-grid` (asunto/para/expediente/fechaEnvío) + `@Mis-form` (solo asunto/cuerpo/para/enCopia/fechaEnvío, sin ningún dato de fallo ni botón de acción, panel de adjuntos de solo lectura).
- **`views/Adjunto-ref.xml`** (resumen): `subsysCorreos.Adjunto@Search-grid`/`@View-form`, de solo lectura, compartidas por `Correo-Centro.xml` y `Correo-Mis.xml` (RUI-correos-centro-formulario-002/003 y la ausencia total de botones de acción en Mis correos). Nombre de fichero y de vistas **sin** el prefijo del padre `Correo.`, siguiendo la excepción de nomenclatura de `design-contract.md` §6 y el precedente real del proyecto (`Curso-ref.xml`, `CursoModulo-ref.xml`).

**Verificar:** `bash validate.sh` valida los 4 ficheros contra `object-views.xsd`; ningún `<form>` tiene `can(Back|Delete|Save)="true"`; los `action-group` de `btnSave`/`btnDelete` del form principal (`@Main`) usan `remote-validationSave-action`/`remote-validationDelete-action`; los del modal de `Adjunto` (`save-modal`/`delete-modal`) no usan ninguna acción `remote-validation*`.

### Paso 9 — Menús

Fichero completo en `design/menus.xml` (porción a fusionar en el `menus.xml` único del proyecto, ver tabla de ficheros). Añade:
- Menú raíz contenedor `misCorreos-menuitem` ("Mis correos", sin `action` ni `parent` — solo `title`/`order`, como todos los demás raíces del `menus.xml` real del proyecto) con un único hijo hoja `misCorreos-verMisCorreos-menuitem` (`parent="misCorreos-menuitem"`, `action="subsysCorreos.Correo@Mis-action"`) — visible para cualquier usuario autenticado (sin `groups`, igual que `registro-menuitem`/`firmarDocumentos-menuitem` del `menus.xml` real).
- Menú raíz `correos-menuitem` ("Correos", `groups="admins,users"`) con dos hijos: `correos-administracion-menuitem` (`groups="admins"`, acción `@Main`) y `correos-centro-menuitem` (`groups="users"`, acción `@Centro`).

**Verificar:** `grep -c "correos" design/menus.xml` ≥ 3; ningún `menuitem` sin `parent` lleva `action` (`grep -n 'menuitem name=".*-menuitem" title=.*action=' design/menus.xml | grep -v 'parent='` no debe encontrar coincidencias — así se excluyen las hojas legítimas que sí llevan `action` junto con `parent`, y solo quedan los raíces); los `order` (15 y 45) no colisionan con los ya existentes en el `menus.xml` real del proyecto (20, 30, 50, 60, 70, 90 a fecha de este diseño).

### Paso 10 — Seguridad

El Administrador (usuario nativo Axelor con el flag de administrador — `AuthUtils.isAdmin(user)`) **no** necesita ningún `Permission` explícito: en este proyecto los usuarios administradores no pasan por el ACL de Axelor (ningún `auth-*.xml` existente del proyecto define permisos para `admins`; es el comportamiento nativo de super-usuario de Axelor). El resto de roles necesita permisos declarativos (`data-init/input/auth-correos.xml`, formato `k-datainit`) con condición JPQL — la única defensa que también protege la Vía B (`/ws/rest/<FQN>` directo), no solo la UI:

| Permission | Objeto | Condición | Quién |
|---|---|---|---|
| `Correo.propio-destinatario` | `Correo` | `self.dniDestinatario = ? and self.estado = 'SUCCESS'`, `conditionParams="__user__.dni"` | Cualquier usuario ve solo sus propios correos ya enviados con éxito |
| `Correo.propio-centro-supervisor` | `Correo` | `self.centro IN (SELECT cu.centro FROM com.educaflow.subsystem.common.db.CentroUsuario cu JOIN cu.centroUsuarioTipoUsuario cut JOIN cut.tipoUsuario tu WHERE cu.usuario = ? AND tu.codigo = 'SUPERVISOR')`, `conditionParams="__user__"` | Solo quien tiene el `TipoUsuario` `SUPERVISOR` en algún centro ve **todos** los correos de ese centro (cualquier estado) — restringe de verdad a Supervisor, no a "cualquier usuario del centro" (ver Notas y supuestos) |
| `Adjunto.propio-destinatario` | `Adjunto` | `self.correo.dniDestinatario = ? and self.correo.estado = 'SUCCESS'`, `conditionParams="__user__.dni"` | Descarga de adjuntos de los propios correos con éxito |
| `Adjunto.propio-centro-supervisor` | `Adjunto` | `self.correo.centro IN (SELECT cu.centro FROM com.educaflow.subsystem.common.db.CentroUsuario cu JOIN cu.centroUsuarioTipoUsuario cut JOIN cut.tipoUsuario tu WHERE cu.usuario = ? AND tu.codigo = 'SUPERVISOR')`, `conditionParams="__user__"` | Descarga de adjuntos de los correos del propio centro (Supervisor) |

Los 4 permisos llevan `can create="false" read="true" write="false" remove="false" export="false"` (ninguno da de alta, edita ni borra: la creación es exclusiva del Administrador, que no pasa por ACL; la escritura/borrado están bloqueados a nivel de servicio para todo el mundo — ver `validateUpdate`/`update`/`validateRemove`/`remove` del paso 3).

`data-init/input-config.xml` (formato estándar `k-datainit`, sin datos maestros — la spec dice explícitamente que este subsistema no aporta datos iniciales propios, solo permisos):

```xml
<?xml version="1.0"?>
<xml-inputs priority="10" xmlns="http://axelor.com/xml/ns/data-import"
            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xsi:schemaLocation="http://axelor.com/xml/ns/data-import
  https://axelor.com/xml/ns/data-import/data-import_8.0.xsd">

    <!-- Permisos del subsistema correos -->
    <input file="auth-correos.xml" root="auth">
        <bind node="permission" type="com.axelor.auth.db.Permission" search="self.name = :name" create="true" update="true">
            <bind node="@name" to="name"/>
            <bind node="@object" to="object"/>
            <bind node="@condition" to="condition"/>
            <bind node="@conditionParams" to="conditionParams"/>
            <bind node="can/@create" to="canCreate"/>
            <bind node="can/@read" to="canRead"/>
            <bind node="can/@write" to="canWrite"/>
            <bind node="can/@remove" to="canRemove"/>
            <bind node="can/@export" to="canExport"/>
        </bind>
    </input>

</xml-inputs>
```

**Verificar:** con el usuario `supervisor1@mislata.es`, `GET /ws/rest/com.educaflow.subsystem.correos.db.Correo/search` con un filtro vacío solo devuelve correos de «CIPFP Mislata»; con `alumno1@mislata.es` solo devuelve sus propios correos en `SUCCESS`.

### Paso 11 — Datos iniciales

Ninguno propio más allá de los permisos del paso 10 (la spec lo dice explícitamente: se apoya en los datos de demo ya existentes de otros subsistemas — centros, usuarios, DNIs).

### Paso 12 — Verificación final

```bash
./run.sh
```

Compila, ejecuta los tests y arranca en el puerto 8080. Comprobar además: `bash .claude/skills/sdd-designer/template-system/validate.sh .sdd/drafts/2026-06-30_13-56_subsistema-correos/design` → `VALIDACION-XML: OK`.

## Frontera de confianza — AllowProperties por acción

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

### `AdjuntoServiceImpl.insert` (invocado a través del endpoint REST automático, dentro del alta en cascada de `Correo`)

Entidad: `Adjunto`. **Forma elegida**: `createAllowProperties` (whitelist), aunque también sería válida `createAllowAllProperties` (Adjunto no tiene ningún campo `servidor` — se opta por la whitelist explícita por claridad y trazabilidad).
**Origen spec:** `Input AllowProperties` de la acción `Crear` de `entity-Adjunto.md`.

| Campo | Origen | En whitelist | Justificación / Ubicación de la asignación |
|---|---|---|---|
| `nombreFichero` | cliente | sí | Input directo del formulario |
| `contenido` | cliente | sí | Input directo del formulario (subida) |
| `correo` | cliente | sí | Referencia al padre embebido (k-secure-coding §3.6): la fija la UI vía `onNew`/`__parent__`, pero la defensa real es `AdjuntoServiceImpl.validateInsert` (V-Adjunto-001/002/003), no la UI |

## Trazabilidad Origen spec → V/R/U → ubicación

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
| V-Adjunto-001 | VAL-Adjunto-001 | `AdjuntoServiceImpl.validateInsert` |
| V-Adjunto-002 | VAL-Adjunto-002 | `AdjuntoServiceImpl.validateInsert` |
| V-Adjunto-003 | VAL-Adjunto-003 | `AdjuntoServiceImpl.validateInsert` |
| V-Adjunto-004 | VAL-Adjunto-004 | `AdjuntoServiceImpl.validateInsert` |
| V-Adjunto-005 | VAL-Adjunto-005 | `AdjuntoServiceImpl.validateInsert` |
| V-Adjunto-006 | RES-Adjunto-001 | `AdjuntoServiceImpl.validateInsert` (+ `Adjunto.xml` `<unique-constraint>` declarativo) — movida desde `CorreoServiceImpl`, la entidad dueña de la restricción |
| V-Adjunto-007 | — (inmutabilidad, Acción Modificar sin `Input AllowProperties` de `entity-Adjunto.md`) | `AdjuntoServiceImpl.validateUpdate` |
| V-Adjunto-008 | — (análogo a RES-Correo-003, intro de `entity-Correo.md`: "sus adjuntos tampoco [se borran]") | `AdjuntoServiceImpl.validateRemove` |

### R- (reglas de negocio / campos calculados de escritura)

| R | Origen spec | Ubicación |
|---|---|---|
| R-Correo-001 | RN-Correo-001 | `CorreoServiceImpl.fireActionRule_ProgramarEnvioAsincrono` (Después de `repository.save` en `insert`). Detalle: `design/rules/R-Correo-001.md` |
| R-Correo-002 | RN-Correo-002 | `CorreoServiceImpl.fireActionRule_ProgramarReenvioAsincrono` (Después, en `reenviar`). Detalle: `design/rules/R-Correo-002.md` |
| R-Correo-003 | CC-Correo-001, CC-Correo-005 | `CorreoServiceImpl.fireActionRule_AsignarValoresIniciales` (Antes, en `insert`) |
| R-Correo-004 | CC-Correo-002, CC-Correo-003, CC-Correo-004, CC-Correo-005, CC-Correo-006, RES-Correo-002 | `CorreoServiceImpl.fireActionRule_RegistrarIntentoEnvio` + `fireActionRule_MarcarEnvioCorrecto` + `fireActionRule_MarcarEnvioFallido` (dentro de `enviarCorreo`; mecanismo compartido — ver `design/rules/R-Correo-001.md`). RES-Correo-002 ("la fecha de envío solo tiene valor cuando el estado es SUCCESS") queda garantizada por construcción: `fireActionRule_MarcarEnvioCorrecto` es el único punto que fija `fechaEnvio` y `fireActionRule_MarcarEnvioFallido` la fuerza a `null` |

### U- (reglas de UI)

| U | Origen spec | Ubicación |
|---|---|---|
| U-correos-administracion-formulario-001 | RUI-correos-administracion-formulario-001 | `views/Correo.xml` — botón `btnReenviar`, `showIf="estado == 'FAIL'"` |
| U-correos-administracion-formulario-002 | RUI-correos-administracion-formulario-002 | `views/Correo.xml` — panel «Envío», `showIf="(id != null) || (cid != null)"` |
| U-correos-administracion-formulario-003 | RUI-correos-administracion-formulario-003 | `views/Correo.xml` — botón `btnCancelAlta`, `showIf="(id == null) && (cid == null)"` |
| U-correos-administracion-formulario-004 | RUI-correos-administracion-formulario-004 | `views/Correo.xml` — botón `btnCancelSalir`, `showIf="(id != null) || (cid != null)"` |
| U-correos-administracion-formulario-005 | RUI-correos-administracion-formulario-005 | `views/Correo.xml` — botón `btnSave`, `showIf="(id == null) && (cid == null)"` |
| U-correos-administracion-formulario-006 | RUI-correos-administracion-formulario-006 | `views/Correo.xml` — todos los campos del panel «Correo», `readonlyIf="(id != null) || (cid != null)"` |
| U-correos-administracion-formulario-007 | RUI-correos-administracion-formulario-007 | `views/Correo.xml` — campo `descripcionUltimoFallo`, `showIf="estado == 'FAIL'"` |
| U-correos-administracion-formulario-008 | RUI-correos-administracion-formulario-008 | `views/Correo.xml` — campo `fechaEnvio`, `showIf="estado == 'SUCCESS'"` |
| U-correos-administracion-listado-adjuntos-001 | RUI-correos-administracion-listado-adjuntos-001 | `views/Correo.xml` — `panel-related "adjuntos"` (editable) frente a `"adjuntosConsulta"` (solo lectura), `showIf="(id == null) && (cid == null)"` / `showIf="(id != null) || (cid != null)"` |
| U-correos-administracion-formulario-adjunto-001 | RUI-correos-administracion-formulario-adjunto-001 | `views/Correo.xml` — `action-record subsysCorreos.Correo.Adjunto@Main-set-correo-parent-action`, `onNew` |
| U-correos-administracion-formulario-adjunto-002 | RUI-correos-administracion-formulario-adjunto-002 | `views/Correo.xml` — botón `btnCancelAlta` del `Adjunto`, `showIf="id == null"` |
| U-correos-administracion-formulario-adjunto-003 | RUI-correos-administracion-formulario-adjunto-003 | `views/Correo.xml` — botón `btnCancelSalir` del `Adjunto`, `showIf="id != null"` (nunca se alcanza en la práctica: el `Adjunto@Main-form` solo se abre en alta; documentado para cumplir la regla del spec al pie de la letra) |
| U-correos-administracion-formulario-adjunto-004 | RUI-correos-administracion-formulario-adjunto-004 | `views/Correo.xml` — botón `btnSave` del `Adjunto`, `showIf="id == null"` |
| U-correos-administracion-formulario-adjunto-005 | RUI-correos-administracion-formulario-adjunto-005 | `views/Correo.xml` — `field name="nombreFichero" required="true"` |
| U-correos-administracion-formulario-adjunto-006 | RUI-correos-administracion-formulario-adjunto-006 | `views/Correo.xml` — `field name="contenido" required="true"` |
| U-correos-administracion-formulario-adjunto-007 | RUI-correos-administracion-formulario-adjunto-007 | `views/Correo.xml` — `panel-related "adjuntosConsulta"` usa el `@View-form` de solo lectura de `views/Adjunto-ref.xml` |
| U-correos-centro-formulario-001 | RUI-correos-centro-formulario-001 | `views/Correo-Centro.xml` — botón `btnReenviar`, `showIf="estado == 'FAIL'"` |
| U-correos-centro-formulario-002 | RUI-correos-centro-formulario-002 | `views/Correo-Centro.xml` — campo `descripcionUltimoFallo`, `showIf="estado == 'FAIL'"` |
| U-correos-centro-formulario-003 | RUI-correos-centro-formulario-003 | `views/Correo-Centro.xml` — campo `fechaEnvio`, `showIf="estado == 'SUCCESS'"` |
| U-correos-centro-formulario-004 | RUI-correos-centro-formulario-004 | `CorreoController.reenviar` — `actionResponse.setNotify(...)` |

## Tests

- **Tests E2E**: descritos en `test-e2e-desc.md` (T-001 a T-025, cubren ESC-001 a ESC-025).
- **Tests unitarios** (JUnit + Mockito): descritos en `test-unit-desc.md` (lo materializa una fase posterior del pipeline, sobre este `design/` ya ganador).

## Reglas del spec descartadas

Ninguna. Todas las reglas `RES-`/`VAL-`/`RN-`/`RUI-`/`CC-` de `entity-Correo.md` y `entity-Adjunto.md` están ubicadas en la matriz anterior.

## Notas y supuestos

1. **Ningún campo "obligatorio" de negocio lleva `required="true"` declarativo.** El flujo de guardado de Axelor persiste primero el bean tal cual (`JPA.manage`/`persist`+`flush`) y **solo después** invoca `modelService.insert` (donde vive `validateInsert`) — ver `k-sistemas/modelos.md` "REGLA CRÍTICA — Campos rellenados por el sistema...". Si un campo de negocio (`asunto`, `centro`, etc.) llevara `required="true"`, un envío incompleto fallaría con un `ConstraintViolationException` genérico de JPA **antes** de que `validateInsert` tuviera ocasión de dar el mensaje amigable que pide cada `VAL-` de la especificación (p.ej. "El asunto es obligatorio"). Por eso `asunto`/`cuerpo`/`para`/`enCopia`/`enCopiaOculta` se declaran `large="true"` (columna de texto sin límite físico) en vez de dejar el límite por defecto de un `<string>`: así ni siquiera una entrada de 256 caracteres (ESC-023) trunca a nivel de base de datos antes de que `V-Correo-010` pueda dar su mensaje ("El asunto no puede superar 255 caracteres").
2. **`Mail` (base/infrastructure/mail) se amplía con `cc`/`bcc` reales (corregido — ya NO se fusiona en `Mail.to()`).** Por directriz de diseño no se reimplementa el cliente SMTP (`MailSender`/`JavaMailHelper` se reutilizan, ampliados en vez de sustituidos). El Paso 1 añade los componentes `cc`/`bcc` al record `Mail` (con un constructor de compatibilidad de 6 argumentos para no romper al único llamador real existente, `RegistroSalidaServiceImpl`), añade las cabeceras MIME `CC`/`BCC` en `JavaMailHelper.getMessage(...)` cuando vienen no vacías, y cambia `MailSenderImpl.send(...)` para enviar a `message.getAllRecipients()` en vez de solo `RecipientType.TO` (si no, CC/BCC quedarían en las cabeceras pero no se entregarían de verdad por SMTP). `construirMail(Correo)` pasa `para`→`to`, `enCopia`→`cc`, `enCopiaOculta`→`bcc`, cada uno a su propia cabecera real: se preserva la confidencialidad de "en copia oculta" que exigen `VAL-Correo-012`/`013` y la propia semántica de `entity-Correo.md` (antes, fusionar los tres en `to` exponía cada destinatario bcc a todos los demás en la cabecera `To` de cada correo entregado). Ver el análisis completo en `design/rules/R-Correo-001.md`, sección "Notas de esta regla".
3. **Se añade el primer binding real de `MailSender` del proyecto.** La búsqueda en el código existente confirma que `MailSender` no tiene hoy ningún binding de Guice (aunque `RegistroSalidaServiceImpl` ya lo inyecta con `@Inject`, lo que sugiere una carencia previa no relacionada con este subsistema). El `Provider` de `CorreosModule` es, de hecho, el primer binding real de `MailSender` en toda la aplicación — como efecto colateral positivo, también arregla ese hueco para `registroentradasalida`. Si en el futuro se decide que ese binding debe vivir en un módulo más genérico (p.ej. `base/infrastructure`), basta moverlo sin tocar `correos`.
4. **VAL-Adjunto-003 ("el correo no está ya creado") se implementa como `correo.getFechaCreacion() == null`.** Es una inferencia sobre el orden de ejecución del framework (`ModelServiceValidationWalker` valida los detalles del árbol **antes** de que el `insert()` del maestro llegue a ejecutar su propio `fireActionRule_AsignarValoresIniciales`, que es lo único que fija `fechaCreacion`): en el alta legítima (Correo + Adjuntos en la misma petición), `fechaCreacion` todavía es `null` cuando se valida cada `Adjunto`; en un intento de "colar" un adjunto sobre un `Correo` ya existente (Vía B, `POST /ws/rest/.../Adjunto` con un `correo.id` de un correo ya persistido en una petición anterior), `fechaCreacion` ya tiene valor. **MUST** verificar este comportamiento empíricamente en cuanto exista código real (un test que intente el ataque directamente contra el endpoint REST) — si el orden real del framework resultase ser el contrario, la alternativa de respaldo es comparar `correo.getId()` con una consulta directa a `HistorialEstado`/`Correo` en BD dentro de la misma transacción para ver si la fila ya existía antes de esta petición.
5. **"Mis correos" se anida como hoja bajo un menú raíz contenedor (corregido — ya NO es un raíz con `action` propio).** `k-vistas/menus.md` establece que un menuitem raíz **no** lleva `action` ni `parent` (solo `title`/`order`/`groups`); el `menus.xml` real del proyecto lo confirma sin excepción: sus 6 raíces activos (`expedientes-menuitem`, `sistemaEducativo-menuitem`, `administracionSv-menuitem`, `registro-menuitem`, `firmarDocumentos-menuitem`, `desarrollador-menuitem`) son todos contenedores puros, y todo `menuitem` con `action` lleva `parent`. Por eso `misCorreos-menuitem` es un raíz contenedor (sin `action`) con un único hijo hoja `misCorreos-verMisCorreos-menuitem` (`parent="misCorreos-menuitem"`, con la `action`), igual que hace `registro-menuitem`/`firmarDocumentos-menuitem` con sus propios hijos — sin `groups` en ninguno de los dos niveles, visible para cualquier usuario autenticado.
6. **El menú «Correos de mi centro» usa `groups="users"` (no restringe por `TipoUsuario`).** El modelo de grupos nativo de Axelor (`admins`/`users`) no distingue el `TipoUsuario` `SUPERVISOR` de otros tipos de usuario del mismo centro (es una limitación compartida por el resto del proyecto, p.ej. `subsysCommon.Centro.CentroUsuario` usa `groups="admins,users"` de forma similar). La visibilidad exacta del menú es solo UX; la **defensa real** que sí distingue Supervisor de cualquier otro usuario del centro está en el paso 10 (permiso `Correo.propio-centro-supervisor` con subconsulta sobre `TipoUsuario.codigo = 'SUPERVISOR'`), que protege también la Vía B.
7. **`descripcionUltimoFallo` guarda la traza completa** (`Throwable.printStackTrace` sobre un `StringWriter`), tal y como pide `design-guidelines.md`; no se trunca. Si en producción esto resultase demasiado voluminoso, se podría acotar en una iteración posterior (fuera de alcance de este diseño).
8. **`reenviar` no llama a `repository.save`.** Es una excepción documentada al patrón general "toda acción persiste con `repository.save`/`repository.remove`" (k-sistemas/servicios.md): `reenviar` no cambia ningún campo del `Correo` de forma síncrona — todo el cambio de estado (incremento de reintentos, fechas, resultado) ocurre después, dentro de `enviarCorreo`, ejecutado por el executor tras el commit. No hay ningún dato que "guardar" en el momento de `reenviar` en sí.
9. **Botón «Borrar» del formulario de administración.** RES-Correo-003 impide borrar un correo en cualquier circunstancia; el `buttons-panel` estándar del proyecto (k-vistas/forms.md) siempre incluye `btnDelete`, así que se mantiene en la vista con `showIf="false"` (nunca visible) en vez de eliminarlo, dejando constancia expresa de que la ausencia de borrado es intencionada y no un olvido.
10. **Formato de `para`/`enCopia`/`enCopiaOculta`.** El spec no fija el separador entre varias direcciones; se adopta la coma como convención (`separarDirecciones`, recortando espacios), documentada aquí porque condiciona tanto la validación como la construcción del `Mail`.
11. **Validación cliente limitada a los campos obligatorios.** Los formatos (email, DNI/NIE, longitud del asunto) solo se validan en servidor (`validateInsert`), no se duplican en `Local-validateSave-action`: son "recomendados" pero no obligatorios en cliente según `k-validaciones/validaciones.md` §3, y mantener la vista simple reduce el riesgo de que las dos capas diverjan.
12. **Convención `(id != null) || (cid != null)` aplicada al `@Main-form` de `Correo`, pero deliberadamente NO al modal de `Adjunto` (salvo su `btnDelete`, que ya la traía).** `k-vistas/forms.md` documenta esta convención explícitamente solo para el botón Borrar («`id` es el ID del registro ya guardado; `cid` es el ID temporal de un registro nuevo todavía no guardado»); el `Model.getCid()` real de Axelor (`axelor-core`, `com.axelor.db.Model`) confirma que `cid` es el **«collection id»**: se asigna a un registro cuando se añade a un widget de **colección** (un `panel-related` o/m2m editable) para poder identificarlo antes de tener `id`, no a un registro raíz abierto directamente desde un grid de nivel superior. Aplicarlo al `@Main-form` de `Correo` (raíz, nunca embebido en ninguna colección) es seguro pero inerte — su `cid` nunca estará poblado, igual que ocurre con el `btnDelete` de `Ciclo@Main-form`/`Curso@Main-form`/`Grado@Main-form` (raíces reales del proyecto que también lo llevan aunque su `cid` nunca varíe) — y se aplica por consistencia con el estilo documentado. **NO se aplica**, en cambio, a `btnCancelAlta`/`btnCancelSalir`/`btnSave` del modal `Correo.Adjunto@Main-form`: un `Adjunto` nuevo añadido vía "Añadir adjunto" a la colección `adjuntos` **sí** recibe un `cid` inmediatamente (es precisamente el caso para el que existe `cid`), así que `showIf="(id == null) && (cid == null)"` ocultaría el botón «Guardar» —y `showIf="(id != null) || (cid != null)"` mostraría «Salir» en vez de «Cancelar»— desde el instante en que se abre el modal para un adjunto nuevo, antes incluso de rellenarlo: un fallo funcional real, no solo de estilo. Por eso el modal conserva `id == null`/`id != null` (sin `cid`) en esos tres botones — el mismo patrón que usa `TareaImportacion.xml` (panel y botones visibles solo en alta) en el código real del proyecto — y solo `btnDelete` usa `(id!=null) || (cid!=null)`, correcto porque "Borrar" sobre un adjunto recién añadido y aún no guardado (quitarlo de la colección) es una acción válida y esperada.
13. **RES-Adjunto-001 (unicidad de `nombreFichero` entre adjuntos del mismo correo) vive en `AdjuntoServiceImpl.validateInsert` (V-Adjunto-006), no en `CorreoServiceImpl`.** Es la entidad dueña de la restricción; `ModelServiceValidationWalker` invoca siempre el `validate*` del servicio del propio detalle para cada hijo de una composición maestro-detalle, y en ese momento `JPA.edit` ya ha ensamblado el grafo completo (incluidas las referencias bidireccionales), por lo que `adjunto.getCorreo().getAdjuntos()` ya contiene a todos los hermanos de la misma petición.
14. **`btnCancel` ("Salir") en `Correo@Mis-form` (`views/Correo-Mis.xml`) y en `Correo@Centro-form` (`views/Correo-Centro.xml`), pese a que `screen-mis-correos.md` describe ambos formularios como "de solo lectura: sin botones" (desviación deliberada, no un olvido).** Ambas vistas se abren con `canBack="false"` (siguiendo el patrón estándar del proyecto para formularios de detalle de solo consulta) y con `view-param name="show-toolbar-form" value="false"` (oculta la toolbar nativa de Axelor, que es la que normalmente ofrece el botón atrás/cerrar). Sin la toolbar y sin `canBack`, un formulario "sin botones" tal cual pide el spec dejaría al usuario sin ninguna forma de volver al listado salvo el botón "atrás" del navegador, lo que se considera peor UX que añadir un botón explícito. Por eso se añade `btnCancel`/"Salir" (`action-group` con `<action name="back"/>`) en el `buttons-panel` de ambos formularios: es la única acción del panel, no introduce edición ni cambia el carácter de "solo lectura" de la vista, y sustituye estrictamente a la navegación que la toolbar nativa habría dado.
