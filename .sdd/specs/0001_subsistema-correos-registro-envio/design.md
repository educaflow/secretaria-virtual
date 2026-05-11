---
type: design
---

# Plan: Subsistema Correos

**Objetivo:** Crear el subsistema `subsystem/correos` que envía emails vía SMTP global de forma síncrona, registra cada envío (entidad `Correo`), permite reintentar manualmente los fallidos y expone tres vistas (todos, por centro, propios) con seguridad multicentro.
**Capa:** subsystem/correos
**Análisis de origen:** `.sdd/drafts/2026-05-07_22-09_subsistema-correos-registro-envio/analysis_05/analysis.md`
**Skills necesarios para la implementación:** k-sistemas, k-vistas, k-seguridad

---

## Decisiones de diseño previas

### D1 — Naming de action-views alineado con `menus.xml` actual
`menus.xml` ya tiene tres `<menuitem>` que apuntan a `subsysCorreos.Correo@All-action`, `@Centro-action` y `@Propios-action`. Para no tocar el fichero de menús (que ya está en `master`), `views/Correo.xml` define exactamente esas tres `action-view`, cada una con su `<domain>` propio y reutilizando dos formularios:
- `@All-action` → grid `@Main-grid` + form `@Main-form`, sin domain (admin ve todos).
- `@Centro-action` → grid `@Main-grid` + form `@Main-form`, `<domain>self.centro = :__user__centroActivo</domain>` con `<context name="__user__centroActivo" expr="eval:__user__.centroActivo"/>` (supervisor ve los de su centro). Hibernate no admite `:__user__.centroActivo` (puntos en nombre de parámetro).
- `@Propios-action` → grid `@CarpetaCiudadana-grid` + form `@CarpetaCiudadana-form`, `<domain>self.dniDestinatario = :__user__dni</domain>` con `<context name="__user__dni" expr="eval:__user__.dni"/>` (lectura propia).

Las grids/forms internos sí siguen la nomenclatura del análisis (`@Main-*` y `@CarpetaCiudadana-*`).

### D2 — Permisos: mantener el patrón `Correo.all` del proyecto
El resto del proyecto usa el patrón `<Entidad>.all` para los permisos sin condición destinados al grupo `admins` (`User.all`, `Centro.all`, `Expediente.all`, `TareaFirma.all`, etc.). El `auth.xml` actual ya wira `Correo.all` en `admins`, `Correo.centro` en `center-admins` y `Correo.propio` en `users`. Se mantiene esa nomenclatura.

`auth-correos.xml` se reescribe con los **tres** permisos (`Correo.all`, `Correo.centro`, `Correo.propio`) y se elimina la definición duplicada de `Correo.all` que está dentro de `auth.xml` (la definición canónica vivirá en `auth-correos.xml`, los wirings de grupo siguen en `auth.xml`).

### D3 — Repositorio personalizado para encapsular consultas JPA (I-3)
Se crea `CorreoRepository extends AbstractCorreoRepository` con un método `findUserByDni(String dni)` que encapsula la consulta JPA contra `User`. Así el servicio nunca contiene `.filter().bind().fetchOne()` inline. El XML del dominio incluye además un `<finder-method name="findByDniDestinatario">` para usos futuros.

### D4 — Mapeo cliente/servidor de validaciones

| ID                                 | Cliente (action-condition)                   | Servidor (validateInsert/Update / método servicio)    |
|------------------------------------|----------------------------------------------|-------------------------------------------------------|
| V-001 centro obligatorio           | sí (`<check field="centro"/>`)               | sí (`validateInsert`)                                 |
| V-002 email obligatorio            | sí (`<check field="email"/>`)                | sí (`validateInsert`)                                 |
| V-003 email formato                | sí (regex en `if`)                           | sí (`EMAIL_PATTERN` en `validateInsert`)              |
| V-004 DNI/NIE formato              | sí (regex en `if`)                           | sí (`DniUtil.isValid` en `validateInsert`)            |
| V-005 asunto obligatorio           | sí (`<check field="asunto"/>`)               | sí (`validateInsert`)                                 |
| V-006 cruzada htmlBody/textBody    | sí (action-condition con `if`)               | sí (`validateInsert`)                                 |
| V-007 transición reenvío           | UI (`showIf="estado == 'FALLIDO'"` en botón) | sí (`reenviarCorreo` lanza `BusinessException`)       |
| V-008 coherencia centro/expediente | no                                           | sí (`validateInsert`)                                 |
| V-009 inmutabilidad post-creación  | UI form readonly                             | sí (`validateUpdate`, comparación campo a campo)      |

> Nota: la UI propia del subsistema no expone form de creación al usuario final (los correos se crean por código vía `enviarCorreo(...)` desde otros subsistemas). Las validaciones de cliente quedan declaradas para coherencia con el patrón del proyecto y para cualquier futura form que invoque `enviarCorreo`. La red de seguridad real está siempre en el servidor.

### D5 — Comportamiento ante fallo SMTP (I-1, I-2)
La captura de la excepción se hace en un helper privado `intentarEnvio(correo)`. Si `MailSender.send` lanza, el correo queda con `estado=FALLIDO`, `mensajeError = Throwable.getMessage()` (sin stacktrace) y se persiste igualmente — sin rollback. Si un `MetaFile` adjunto fue borrado entre creación y reenvío (A9), `MetaFileUtil.downloadContent` lanzará y caerá en la misma rama de fallo.

### D6 — Sin módulo Guice ni listener JPA
`ModelServiceFactory` descubre `CorreoServiceImpl` por convención (`service.impl.*ServiceImpl`). `MailSender` ya está bindado en `SecretariaVirtualModule` y se inyecta como campo. No hay lógica en listeners.

---

## Ficheros a crear o modificar

| # | Fichero                                                                             | Acción     | Skill       | Descripción                                                                                                          |
|---|-------------------------------------------------------------------------------------|------------|-------------|----------------------------------------------------------------------------------------------------------------------|
| 1 | `src/main/java/com/educaflow/subsystem/correos/domains/Correo.xml`                  | Crear      | k-sistemas  | Entidad `Correo` con todos sus campos, enum `EstadoCorreo`, finder por DNI.                                          |
| 2 | `src/main/java/com/educaflow/subsystem/correos/db/repo/CorreoRepository.java`       | Crear      | k-sistemas  | Repositorio personalizado con `findUserByDni`.                                                                       |
| 3 | `src/main/java/com/educaflow/subsystem/correos/service/CorreoEnviarDTO.java`        | Crear      | k-sistemas  | Record con argumentos de `enviarCorreo`.                                                                             |
| 4 | `src/main/java/com/educaflow/subsystem/correos/service/CorreoService.java`          | Crear      | k-sistemas  | Interfaz `extends ModelService<Correo>`.                                                                             |
| 5 | `src/main/java/com/educaflow/subsystem/correos/service/impl/CorreoServiceImpl.java` | Crear      | k-sistemas  | Implementación con validaciones, `enviarCorreo` y `reenviarCorreo`.                                                  |
| 6 | `src/main/java/com/educaflow/subsystem/correos/controller/CorreoController.java`    | Crear      | k-sistemas  | Métodos `validateSave` y `reenviar` para el form.                                                                    |
| 7 | `src/main/java/com/educaflow/subsystem/correos/views/Correo.xml`                    | Crear      | k-vistas    | Grids, forms, action-views, action-groups, action-conditions, action-methods.                                        |
| 8 | `src/main/resources/data-init/input/auth-correos.xml`                               | Reescribir | k-seguridad | Permisos `Correo.all`, `Correo.centro`, `Correo.propio`.                                                             |
| 9 | `src/main/resources/data-init/input/auth.xml`                                       | Modificar  | k-seguridad | Eliminar la definición duplicada de `Correo.all` (queda solo en auth-correos.xml). Los wirings de grupos no cambian. |

> **No tocar** `src/main/resources/data-init/input-config.xml` — el `<input file="auth-correos.xml">` ya existe antes de `auth.xml`.
> **No tocar** `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` — los tres `menuitem` y sus `action="..."` ya están bien.
> **Nunca crear** `i18n_es.csv` / `i18n_ca.csv`: se generan automáticamente.

---

## Pasos

### Paso 1 — Crear el modelo de dominio `Correo.xml`

**Ruta:** `src/main/java/com/educaflow/subsystem/correos/domains/Correo.xml`

**Contenido:**

```xml
<?xml version="1.0"?>
<domain-models xmlns="http://axelor.com/xml/ns/domain-models"
               xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
               xsi:schemaLocation="http://axelor.com/xml/ns/domain-models https://axelor.com/xml/ns/domain-models/domain-models_8.1.xsd">

    <module name="correos" package="com.educaflow.subsystem.correos.db"/>

    <entity name="Correo" repository="abstract">
        <many-to-one name="centro" ref="com.educaflow.subsystem.common.db.Centro" required="true" title="Centro"/>
        <many-to-one name="expediente" ref="com.educaflow.subsystem.expedientes.db.Expediente" title="Expediente"/>

        <string name="email" required="true" title="Email destinatario" max="255"/>
        <string name="dniDestinatario" title="DNI/NIE destinatario" max="16"/>
        <many-to-one name="usuarioDestinatario" ref="com.axelor.auth.db.User" title="Usuario destinatario"/>

        <string name="asunto" required="true" title="Asunto" max="255"/>
        <string name="htmlBody" large="true" multiline="true" title="Cuerpo HTML"/>
        <string name="textBody" large="true" multiline="true" title="Cuerpo texto plano"/>

        <one-to-many name="adjuntos" ref="com.axelor.meta.db.MetaFile" title="Adjuntos"/>

        <enum name="estado" ref="EstadoCorreo" required="true" title="Estado"/>
        <datetime name="fechaUltimoIntento" required="true" title="Fecha del último intento"/>
        <integer name="numIntentos" required="true" default="1" title="Nº intentos"/>
        <string name="mensajeError" large="true" multiline="true" title="Mensaje de error"/>

        <finder-method name="findByDniDestinatario" using="dniDestinatario" all="true"/>
    </entity>

    <enum name="EstadoCorreo">
        <item name="ENVIADO" title="Enviado"/>
        <item name="FALLIDO" title="Fallido"/>
    </enum>

</domain-models>
```

**Notas:**
- `repository="abstract"` permite extender con `CorreoRepository` en el paso 2.
- `default="1"` en `numIntentos` cubre I-4 también para inserción manual de admin.

**Verificación:**
- `./gradlew compileJava` compila.
- Existen `build/src-gen/.../correos/db/Correo.java`, `EstadoCorreo.java`, `AbstractCorreoRepository.java`.

---

### Paso 2 — Crear el repositorio personalizado `CorreoRepository`

**Ruta:** `src/main/java/com/educaflow/subsystem/correos/db/repo/CorreoRepository.java`

**Contenido:**

```java
package com.educaflow.subsystem.correos.db.repo;

import com.axelor.auth.db.User;
import com.axelor.db.JpaRepository;

public class CorreoRepository extends AbstractCorreoRepository {

    public CorreoRepository() {
        super();
    }

    /**
     * Busca un User por su DNI. Devuelve null si dni es null/vacío o no hay coincidencia.
     * I-3: las consultas JPA viven en el repositorio, nunca inline en el servicio.
     */
    public User findUserByDni(String dni) {
        if (dni == null || dni.isBlank()) {
            return null;
        }
        return JpaRepository.of(User.class)
                .all()
                .filter("self.dni = :dni")
                .bind("dni", dni)
                .fetchOne();
    }
}
```

**Verificación:**
- `./gradlew compileJava` compila.
- `AbstractCorreoRepository` existe y `CorreoRepository` lo extiende.

---

### Paso 3 — Crear el DTO `CorreoEnviarDTO`

**Ruta:** `src/main/java/com/educaflow/subsystem/correos/service/CorreoEnviarDTO.java`

**Contenido:**

```java
package com.educaflow.subsystem.correos.service;

import com.axelor.meta.db.MetaFile;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.expedientes.db.Expediente;

import java.util.List;

public record CorreoEnviarDTO(
        Centro centro,
        Expediente expediente,
        String email,
        String dniDestinatario,
        String asunto,
        String htmlBody,
        String textBody,
        List<MetaFile> adjuntos
) {
}
```

**Verificación:** compila como parte de `./gradlew build`.

---

### Paso 4 — Crear la interfaz `CorreoService`

**Ruta:** `src/main/java/com/educaflow/subsystem/correos/service/CorreoService.java`

**Contenido:**

```java
package com.educaflow.subsystem.correos.service;

import com.axelor.db.modelservice.ModelService;
import com.educaflow.base.infrastructure.validation.messages.BusinessException;
import com.educaflow.base.infrastructure.validation.messages.BusinessMessages;
import com.educaflow.subsystem.correos.db.Correo;

import java.util.Optional;

public interface CorreoService extends ModelService<Correo> {

    Optional<BusinessMessages> validateInsert(Correo correo);

    Optional<BusinessMessages> validateUpdate(Correo correo, Correo original);

    Optional<BusinessMessages> validateRemove(Correo correo);

    /**
     * Envía un correo de forma síncrona y persiste el registro de envío.
     * Aplica V-001..V-006 + V-008 sobre los argumentos antes de intentar el envío.
     * NO hace rollback si SMTP falla (I-1): captura la excepción, marca el Correo
     * como FALLIDO con mensajeError = Throwable.getMessage() (I-2) y devuelve el
     * Correo persistido.
     */
    Correo enviarCorreo(CorreoEnviarDTO dto) throws BusinessException;

    /**
     * Reintenta el envío de un correo previamente FALLIDO.
     * Aplica V-007. NO hace rollback si SMTP vuelve a fallar.
     */
    Correo reenviarCorreo(Long correoId) throws BusinessException;
}
```

**Verificación:** compila.

---

### Paso 5 — Crear la implementación `CorreoServiceImpl`

**Ruta:** `src/main/java/com/educaflow/subsystem/correos/service/impl/CorreoServiceImpl.java`

**Reglas que aplica:**
- Extiende `DefaultModelService<Correo>` con constructor `(Class<Correo>, Repository<Correo>)`.
- `super.insert` / `super.update` para persistir; nunca `repository.save()`.
- `MailSender` inyectado como campo `@Inject` (no en el constructor).
- Captura excepción SMTP, marca FALLIDO sin rollback (I-1).
- `mensajeError` solo `Throwable.getMessage()` (I-2).
- Validaciones devuelven `Optional<BusinessMessages>`, no lanzan.
- V-009 implementado en `validateUpdate` campo a campo.
- Resolución de User por DNI delegada al repositorio (I-3).

**Contenido:**

```java
package com.educaflow.subsystem.correos.service.impl;

import com.axelor.app.AppSettings;
import com.axelor.auth.db.User;
import com.axelor.db.Repository;
import com.axelor.db.modelservice.DefaultModelService;
import com.axelor.meta.db.MetaFile;
import com.educaflow.base.infrastructure.mail.Attach;
import com.educaflow.base.infrastructure.mail.Mail;
import com.educaflow.base.infrastructure.mail.MailSender;
import com.educaflow.base.infrastructure.validation.messages.BusinessException;
import com.educaflow.base.infrastructure.validation.messages.BusinessMessage;
import com.educaflow.base.infrastructure.validation.messages.BusinessMessages;
import com.educaflow.base.util.DniUtil;
import com.educaflow.base.util.MetaFileUtil;
import com.educaflow.subsystem.correos.db.Correo;
import com.educaflow.subsystem.correos.db.EstadoCorreo;
import com.educaflow.subsystem.correos.db.repo.CorreoRepository;
import com.educaflow.subsystem.correos.service.CorreoEnviarDTO;
import com.educaflow.subsystem.correos.service.CorreoService;
import com.google.inject.Inject;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

public class CorreoServiceImpl extends DefaultModelService<Correo> implements CorreoService {

    /** A6: regex RFC 5322 simplificada. */
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    @Inject
    private MailSender mailSender;

    public CorreoServiceImpl(Class<Correo> model, Repository<Correo> repository) {
        super(model, repository);
    }

    /* ================================================================== */
    /* ===================== Operaciones de negocio ===================== */
    /* ================================================================== */

    @Override
    public Correo enviarCorreo(CorreoEnviarDTO dto) throws BusinessException {
        // 1) Construir entidad desde DTO
        Correo correo = new Correo();
        correo.setCentro(dto.centro());
        correo.setExpediente(dto.expediente());
        correo.setEmail(dto.email());
        correo.setDniDestinatario(dto.dniDestinatario());
        correo.setAsunto(dto.asunto());
        correo.setHtmlBody(dto.htmlBody());
        correo.setTextBody(dto.textBody());
        if (dto.adjuntos() != null) {
            correo.setAdjuntos(new ArrayList<>(dto.adjuntos()));
        }

        // 2) V-001..V-006 + V-008
        Optional<BusinessMessages> errors = validateInsert(correo);
        if (errors.isPresent()) {
            throw new BusinessException(errors.get());
        }

        // 3) Resolver usuarioDestinatario por DNI (I-3, A1).
        //    Se usa el campo heredado `repository` de DefaultModelService (no hay getRepository()).
        User usuario = ((CorreoRepository) repository).findUserByDni(dto.dniDestinatario());
        correo.setUsuarioDestinatario(usuario);

        // 4) Marcas iniciales del intento
        correo.setNumIntentos(1);
        correo.setFechaUltimoIntento(LocalDateTime.now());

        // 5) Intentar envío SMTP síncrono (I-1: nunca rollback)
        intentarEnvio(correo);

        // 6) Persistir mediante super.insert (regla del proyecto)
        return super.insert(correo);
    }

    @Override
    public Correo reenviarCorreo(Long correoId) throws BusinessException {
        Correo correo = repository.find(correoId);
        if (correo == null) {
            throw new BusinessException(new BusinessMessage("id",
                    "No se ha encontrado el correo con id '" + correoId + "'."));
        }

        // V-007
        if (correo.getEstado() != EstadoCorreo.FALLIDO) {
            throw new BusinessException(new BusinessMessage("estado",
                    "Solo se pueden reenviar correos en estado FALLIDO. " +
                    "El correo está en estado '" + correo.getEstado() + "'."));
        }

        // Snapshot del estado original ANTES de modificar (para super.update)
        Correo correoOriginal = clonarSnapshot(correo);

        // Marcas del nuevo intento
        correo.setNumIntentos((correo.getNumIntentos() == null ? 0 : correo.getNumIntentos()) + 1);
        correo.setFechaUltimoIntento(LocalDateTime.now());

        // Intentar envío (I-1: nunca rollback)
        intentarEnvio(correo);

        return super.update(correo, correoOriginal);
    }

    /* ================================================================== */
    /* ====================== Métodos de validación ===================== */
    /* ================================================================== */

    @Override
    public Optional<BusinessMessages> validateInsert(Correo correo) {
        BusinessMessages messages = new BusinessMessages();

        // V-001
        if (correo.getCentro() == null) {
            messages.add(new BusinessMessage("centro", "El centro es obligatorio."));
        }

        // V-002
        if (correo.getEmail() == null || correo.getEmail().isBlank()) {
            messages.add(new BusinessMessage("email", "El email del destinatario es obligatorio."));
        } else if (!EMAIL_PATTERN.matcher(correo.getEmail()).matches()) {
            // V-003
            messages.add(new BusinessMessage("email",
                    "El email '" + correo.getEmail() + "' no tiene un formato válido."));
        }

        // V-004
        if (correo.getDniDestinatario() != null && !correo.getDniDestinatario().isBlank()
                && !DniUtil.isValid(correo.getDniDestinatario())) {
            messages.add(new BusinessMessage("dniDestinatario",
                    "El DNI/NIE '" + correo.getDniDestinatario() +
                    "' no es válido. Compruebe el formato y la letra de control."));
        }

        // V-005
        if (correo.getAsunto() == null || correo.getAsunto().isBlank()) {
            messages.add(new BusinessMessage("asunto", "El asunto es obligatorio."));
        }

        // V-006
        boolean htmlVacio = correo.getHtmlBody() == null || correo.getHtmlBody().isBlank();
        boolean textVacio = correo.getTextBody() == null || correo.getTextBody().isBlank();
        if (htmlVacio && textVacio) {
            messages.add(new BusinessMessage("htmlBody",
                    "Debe proporcionar al menos un cuerpo del mensaje (HTML o texto plano)."));
        }

        // V-008 (asunción A7*: coherencia centro/expediente)
        if (correo.getExpediente() != null && correo.getCentro() != null
                && correo.getExpediente().getCentro() != null
                && !Objects.equals(correo.getExpediente().getCentro().getId(),
                                   correo.getCentro().getId())) {
            String label = correo.getExpediente().getId() != null
                    ? correo.getExpediente().getId().toString()
                    : "(sin id)";
            messages.add(new BusinessMessage("expediente",
                    "El expediente '" + label + "' pertenece a un centro distinto del indicado en el correo."));
        }

        return messages.isEmpty() ? Optional.empty() : Optional.of(messages);
    }

    @Override
    public Optional<BusinessMessages> validateUpdate(Correo correo, Correo original) {
        BusinessMessages messages = new BusinessMessages();

        // V-009 — inmutabilidad de los campos de entrada.
        // Los campos de servicio (estado, fechaUltimoIntento, numIntentos, mensajeError)
        // no se comprueban: cambian legítimamente en reenviarCorreo.
        comprobarInmutable(messages, "centro", idOf(original.getCentro()), idOf(correo.getCentro()));
        comprobarInmutable(messages, "expediente", idOf(original.getExpediente()), idOf(correo.getExpediente()));
        comprobarInmutable(messages, "email", original.getEmail(), correo.getEmail());
        comprobarInmutable(messages, "dniDestinatario", original.getDniDestinatario(), correo.getDniDestinatario());
        comprobarInmutable(messages, "usuarioDestinatario",
                idOf(original.getUsuarioDestinatario()), idOf(correo.getUsuarioDestinatario()));
        comprobarInmutable(messages, "asunto", original.getAsunto(), correo.getAsunto());
        comprobarInmutable(messages, "htmlBody", original.getHtmlBody(), correo.getHtmlBody());
        comprobarInmutable(messages, "textBody", original.getTextBody(), correo.getTextBody());
        if (!sameMetaFiles(original.getAdjuntos(), correo.getAdjuntos())) {
            messages.add(new BusinessMessage("adjuntos",
                    "Los correos no son editables una vez registrados; el campo 'adjuntos' no se puede modificar."));
        }

        return messages.isEmpty() ? Optional.empty() : Optional.of(messages);
    }

    @Override
    public Optional<BusinessMessages> validateRemove(Correo correo) {
        // No hay reglas de borrado; los permisos restringen quién puede borrar (admin).
        return Optional.empty();
    }

    /* ================================================================== */
    /* =========================== Helpers ============================== */
    /* ================================================================== */

    /**
     * Intenta enviar el correo de forma síncrona y actualiza el estado en el propio
     * objeto. Nunca lanza excepción: I-1 (sin rollback) + I-2 (mensaje humano sin
     * stacktrace). Si un MetaFile adjunto fue borrado (A9), MetaFileUtil.downloadContent
     * lanzará y caerá aquí como fallo SMTP.
     */
    private void intentarEnvio(Correo correo) {
        try {
            String from = AppSettings.get().get("mail.smtp.from");
            List<Attach> attachs = new ArrayList<>();
            if (correo.getAdjuntos() != null) {
                for (MetaFile mf : correo.getAdjuntos()) {
                    byte[] data = MetaFileUtil.downloadContent(mf);
                    attachs.add(new Attach(mf.getFileName(), data, mf.getFileType()));
                }
            }
            Mail mail = new Mail(
                    List.of(correo.getEmail()),
                    from,
                    correo.getAsunto(),
                    correo.getHtmlBody(),
                    correo.getTextBody(),
                    attachs);

            mailSender.send(mail);

            correo.setEstado(EstadoCorreo.ENVIADO);
            correo.setMensajeError(null);
        } catch (Throwable t) {
            correo.setEstado(EstadoCorreo.FALLIDO);
            correo.setMensajeError(extraerMensaje(t));
        }
    }

    private static String extraerMensaje(Throwable t) {
        String msg = t.getMessage();
        if (msg == null || msg.isBlank()) {
            msg = t.getClass().getSimpleName();
        }
        return msg;
    }

    private Correo clonarSnapshot(Correo c) {
        Correo o = new Correo();
        o.setId(c.getId());
        o.setCentro(c.getCentro());
        o.setExpediente(c.getExpediente());
        o.setEmail(c.getEmail());
        o.setDniDestinatario(c.getDniDestinatario());
        o.setUsuarioDestinatario(c.getUsuarioDestinatario());
        o.setAsunto(c.getAsunto());
        o.setHtmlBody(c.getHtmlBody());
        o.setTextBody(c.getTextBody());
        o.setAdjuntos(c.getAdjuntos() != null ? new ArrayList<>(c.getAdjuntos()) : new ArrayList<>());
        o.setEstado(c.getEstado());
        o.setFechaUltimoIntento(c.getFechaUltimoIntento());
        o.setNumIntentos(c.getNumIntentos());
        o.setMensajeError(c.getMensajeError());
        return o;
    }

    private void comprobarInmutable(BusinessMessages messages, String campo, Object antes, Object ahora) {
        if (!Objects.equals(antes, ahora)) {
            messages.add(new BusinessMessage(campo,
                    "Los correos no son editables una vez registrados; el campo '" +
                    campo + "' no se puede modificar."));
        }
    }

    private static Long idOf(com.axelor.db.Model m) {
        return m == null ? null : m.getId();
    }

    private static boolean sameMetaFiles(List<MetaFile> a, List<MetaFile> b) {
        int sa = a == null ? 0 : a.size();
        int sb = b == null ? 0 : b.size();
        if (sa != sb) return false;
        if (sa == 0) return true;
        List<Long> ia = a.stream().map(MetaFile::getId).sorted().toList();
        List<Long> ib = b.stream().map(MetaFile::getId).sorted().toList();
        return ia.equals(ib);
    }
}
```

**Verificación:**
- `./gradlew compileJava` compila sin errores.
- No existe módulo Guice para este servicio (`ModelServiceFactory` lo descubre).

---

### Paso 6 — Crear el controlador `CorreoController`

**Ruta:** `src/main/java/com/educaflow/subsystem/correos/controller/CorreoController.java`

**Contenido:**

```java
package com.educaflow.subsystem.correos.controller;

import com.axelor.db.modelservice.ModelServiceFactory;
import com.axelor.meta.CallMethod;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.educaflow.base.infrastructure.axelorhelper.ActionRequestHelper;
import com.educaflow.base.infrastructure.axelorhelper.ActionResponseHelper;
import com.educaflow.base.infrastructure.validation.messages.BusinessException;
import com.educaflow.base.infrastructure.validation.messages.BusinessMessages;
import com.educaflow.base.util.AllowProperties;
import com.educaflow.subsystem.correos.db.Correo;
import com.educaflow.subsystem.correos.service.CorreoService;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;

import java.util.Optional;

public class CorreoController {

    @Inject
    private ModelServiceFactory modelServiceFactory;

    /**
     * Validación remota llamada desde el `Remote-validateSave-action` del action-group
     * del form Main. Aunque el form actual es solo lectura, se mantiene por consistencia
     * con el patrón del proyecto y para soportar futuras forms de creación.
     */
    @CallMethod
    public void validateSave(ActionRequest actionRequest, ActionResponse actionResponse) {
        final CorreoService correoService = (CorreoService) modelServiceFactory.resolve(Correo.class);

        ActionRequestHelper<Correo> requestHelper = new ActionRequestHelper(actionRequest, Correo.class);
        ActionResponseHelper responseHelper = new ActionResponseHelper(actionResponse);

        AllowProperties allowProperties = AllowProperties.createAllowAllProperties();
        Correo correo = requestHelper.getModel(allowProperties);

        Optional<BusinessMessages> result;
        if (requestHelper.getId() == null) {
            result = correoService.validateInsert(correo);
        } else {
            Correo original = requestHelper.getOriginalModel();
            result = correoService.validateUpdate(correo, original);
        }

        if (result.isPresent()) {
            responseHelper.doResponseBusinessMessagesAsError(result.get());
        }
    }

    /**
     * Disparado por el botón "Reenviar" del form Main cuando estado=FALLIDO.
     * Llama a `correoService.reenviarCorreo(id)`. Si V-007 falla, muestra el mensaje
     * como diálogo de error. En caso contrario refresca el form para mostrar
     * estado, numIntentos, fechaUltimoIntento y mensajeError actualizados.
     */
    @CallMethod
    @Transactional
    public void reenviar(ActionRequest actionRequest, ActionResponse actionResponse) {
        final CorreoService correoService = (CorreoService) modelServiceFactory.resolve(Correo.class);

        ActionRequestHelper<Correo> requestHelper = new ActionRequestHelper(actionRequest, Correo.class);
        ActionResponseHelper responseHelper = new ActionResponseHelper(actionResponse);

        Long id = requestHelper.getId();
        if (id == null) {
            return;
        }

        try {
            correoService.reenviarCorreo(id);
            actionResponse.setReload(true);
        } catch (BusinessException ex) {
            responseHelper.doResponseBusinessMessagesAsError(ex.getBusinessMessages());
        }
    }
}
```

**Verificación:** `./gradlew compileJava` compila.

---

### Paso 7 — Crear las vistas `Correo.xml`

**Ruta:** `src/main/java/com/educaflow/subsystem/correos/views/Correo.xml`

**Contenido:**

```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<object-views xmlns="http://axelor.com/xml/ns/object-views"
              xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xsi:schemaLocation="http://axelor.com/xml/ns/object-views https://axelor.com/xml/ns/object-views/object-views_8.1.xsd">

    <!-- ******************************************************************************* -->
    <!-- ****************************** Correo: Vistas ********************************* -->
    <!-- ******************************************************************************* -->

    <!-- ============== ACTION-VIEW @All (admins, sin filtro) ============== -->
    <action-view name="subsysCorreos.Correo@All-action" title="Todos los correos"
                 model="com.educaflow.subsystem.correos.db.Correo">
        <view type="grid" name="subsysCorreos.Correo@Main-grid"/>
        <view type="form" name="subsysCorreos.Correo@Main-form"/>
        <view-param name="show-toolbar-grid" value="false"/>
        <view-param name="show-toolbar-form" value="false"/>
        <view-param name="reload-grid" value="true"/>
    </action-view>

    <!-- ============== ACTION-VIEW @Centro (center-admins, filtro centro activo) ============== -->
    <action-view name="subsysCorreos.Correo@Centro-action" title="Correos del centro"
                 model="com.educaflow.subsystem.correos.db.Correo">
        <view type="grid" name="subsysCorreos.Correo@Main-grid"/>
        <view type="form" name="subsysCorreos.Correo@Main-form"/>
        <view-param name="show-toolbar-grid" value="false"/>
        <view-param name="show-toolbar-form" value="false"/>
        <view-param name="reload-grid" value="true"/>
        <domain>self.centro = :__user__centroActivo</domain>
        <context name="__user__centroActivo" expr="eval:__user__.centroActivo"/>
    </action-view>

    <!-- ============== ACTION-VIEW @Propios (users, filtro DNI) ============== -->
    <action-view name="subsysCorreos.Correo@Propios-action" title="Mis correos"
                 model="com.educaflow.subsystem.correos.db.Correo">
        <view type="grid" name="subsysCorreos.Correo@CarpetaCiudadana-grid"/>
        <view type="form" name="subsysCorreos.Correo@CarpetaCiudadana-form"/>
        <view-param name="show-toolbar-grid" value="false"/>
        <view-param name="show-toolbar-form" value="false"/>
        <view-param name="reload-grid" value="true"/>
        <domain>self.dniDestinatario = :__user__dni</domain>
        <context name="__user__dni" expr="eval:__user__.dni"/>
    </action-view>

    <!-- ============== GRID Main (admin/supervisor) ============== -->
    <grid name="subsysCorreos.Correo@Main-grid" title="Correos"
          model="com.educaflow.subsystem.correos.db.Correo"
          editable="false" edit-icon="false" x-selector="none"
          canNew="false" canEdit="false" canDelete="false" canSave="false"
          canViewOnClick="true" orderBy="-fechaUltimoIntento" allowSearchFields="true">
        <field name="fechaUltimoIntento"/>
        <field name="email"/>
        <field name="dniDestinatario"/>
        <field name="asunto"/>
        <field name="estado"/>
        <field name="centro"/>
        <field name="expediente"/>
        <field name="numIntentos"/>
    </grid>

    <!-- ============== GRID CarpetaCiudadana (users) ============== -->
    <grid name="subsysCorreos.Correo@CarpetaCiudadana-grid" title="Mis correos"
          model="com.educaflow.subsystem.correos.db.Correo"
          editable="false" edit-icon="false" x-selector="none"
          canNew="false" canEdit="false" canDelete="false" canSave="false"
          canViewOnClick="true" orderBy="-fechaUltimoIntento">
        <field name="fechaUltimoIntento"/>
        <field name="email"/>
        <field name="asunto"/>
        <field name="estado"/>
        <field name="expediente"/>
    </grid>

    <!-- ============== FORM Main (admin/supervisor) — solo lectura + botón Reenviar ============== -->
    <form name="subsysCorreos.Correo@Main-form" title="Correo"
          model="com.educaflow.subsystem.correos.db.Correo"
          width="large"
          canAttach="false" canBack="false" canDelete="false" canNew="false"
          canSave="false" canMore="false" canBackOnSave="true"
          onSave="subsysCorreos.Correo@Main-onSave-action">

        <panel name="panelDestinatario" title="Destinatario" colSpan="12" readonly="true">
            <field name="email"               colSpan="6"/>
            <field name="dniDestinatario"     colSpan="3"/>
            <field name="usuarioDestinatario" colSpan="3"/>
        </panel>

        <panel name="panelContenido" title="Contenido" colSpan="12" readonly="true">
            <field name="asunto"   colSpan="12"/>
            <field name="htmlBody" colSpan="12" widget="html"  showIf="htmlBody != null"/>
            <field name="textBody" colSpan="12" widget="Text"  showIf="textBody != null"/>
            <field name="adjuntos" colSpan="12"/>
        </panel>

        <panel name="panelContexto" title="Contexto" colSpan="12" readonly="true">
            <field name="centro"     colSpan="6"/>
            <field name="expediente" colSpan="6"/>
        </panel>

        <panel name="panelEnvio" title="Envío" colSpan="12" readonly="true">
            <field name="estado"             colSpan="3"/>
            <field name="fechaUltimoIntento" colSpan="3"/>
            <field name="numIntentos"        colSpan="3"/>
            <field name="mensajeError"       colSpan="12" widget="Text" showIf="mensajeError != null"/>
        </panel>

        <panel name="panelBotones" colSpan="12" showFrame="false">
            <button name="btnVolver"   title="Salir" onClick="back"
                    css="btn-secondary" colSpan="3" colOffset="6" outline="true"/>
            <button name="btnReenviar" title="Reenviar"
                    onClick="subsysCorreos.Correo@Main-btnReenviar-action"
                    css="btn-primary" colSpan="3"
                    showIf="estado == 'FALLIDO' &amp;&amp; id != null"/>
        </panel>

    </form>

    <!-- ============== FORM CarpetaCiudadana — solo lectura, sin botón Reenviar ============== -->
    <form name="subsysCorreos.Correo@CarpetaCiudadana-form" title="Mi correo"
          model="com.educaflow.subsystem.correos.db.Correo"
          width="large"
          canAttach="false" canBack="false" canDelete="false" canNew="false"
          canSave="false" canMore="false" canBackOnSave="true">

        <panel name="panelDestinatario" title="Destinatario" colSpan="12" readonly="true">
            <field name="email"           colSpan="6"/>
            <field name="dniDestinatario" colSpan="3"/>
        </panel>

        <panel name="panelContenido" title="Contenido" colSpan="12" readonly="true">
            <field name="asunto"   colSpan="12"/>
            <field name="htmlBody" colSpan="12" widget="html" showIf="htmlBody != null"/>
            <field name="textBody" colSpan="12" widget="Text" showIf="textBody != null"/>
            <field name="adjuntos" colSpan="12"/>
        </panel>

        <panel name="panelEnvio" title="Envío" colSpan="12" readonly="true">
            <field name="estado"             colSpan="3"/>
            <field name="fechaUltimoIntento" colSpan="3"/>
        </panel>

        <panel name="panelBotones" colSpan="12" showFrame="false">
            <button name="btnVolver" title="Salir" onClick="back"
                    css="btn-secondary" colSpan="3" colOffset="9" outline="true"/>
        </panel>

    </form>

    <!-- *********** Correo : Acciones de las tareas principales *********** -->
    <action-group name="subsysCorreos.Correo@Main-onSave-action">
        <action name="subsysCorreos.Correo@Main-Local-validateSave-action"/>
        <action name="subsysCorreos.Correo@Main-Remote-validateSave-action"/>
    </action-group>

    <action-group name="subsysCorreos.Correo@Main-btnReenviar-action">
        <action name="subsysCorreos.Correo@Main-Local-validateReenviar-action"/>
        <action name="subsysCorreos.Correo@Main-Remote-reenviar-action"/>
    </action-group>

    <!-- *********** Correo : Acciones de Validaciones en local *********** -->
    <action-condition name="subsysCorreos.Correo@Main-Local-validateSave-action">
        <!-- V-001 -->
        <check field="centro" error="El centro es obligatorio."/>
        <!-- V-002 -->
        <check field="email" error="El email del destinatario es obligatorio."/>
        <!-- V-003 -->
        <check field="email"
               if="email != null &amp;&amp; !email.matches('^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$')"
               error="El email no tiene un formato válido."/>
        <!-- V-004 (regex aproximada en cliente; el servidor refuerza con DniUtil.isValid) -->
        <check field="dniDestinatario"
               if="dniDestinatario != null &amp;&amp; !dniDestinatario.isEmpty() &amp;&amp; !dniDestinatario.matches('^[0-9]{8}[A-Za-z]$|^[XYZxyz][0-9]{7}[A-Za-z]$')"
               error="El DNI/NIE no tiene un formato válido. Compruebe el formato y la letra de control."/>
        <!-- V-005 -->
        <check field="asunto" error="El asunto es obligatorio."/>
        <!-- V-006 -->
        <check field="htmlBody"
               if="(htmlBody == null || htmlBody.isEmpty()) &amp;&amp; (textBody == null || textBody.isEmpty())"
               error="Debe proporcionar al menos un cuerpo del mensaje (HTML o texto plano)."/>
    </action-condition>

    <action-validate name="subsysCorreos.Correo@Main-Local-validateReenviar-action">
        <error message="Solo se pueden reenviar correos en estado FALLIDO."
               if="estado != 'FALLIDO'"/>
    </action-validate>

    <!-- *********** Correo : Acciones básicas que cambian campos simples *********** -->
    <!-- (no aplica) -->

    <!-- *********** Correo : Acciones de llamadas Remotas al servidor *********** -->
    <action-method name="subsysCorreos.Correo@Main-Remote-validateSave-action"
                   model="com.educaflow.subsystem.correos.db.Correo">
        <call class="com.educaflow.subsystem.correos.controller.CorreoController" method="validateSave"/>
    </action-method>

    <action-method name="subsysCorreos.Correo@Main-Remote-reenviar-action"
                   model="com.educaflow.subsystem.correos.db.Correo">
        <call class="com.educaflow.subsystem.correos.controller.CorreoController" method="reenviar"/>
    </action-method>

</object-views>
```

**Notas:**
- El form Main es solo lectura (todos los `<panel>` con `readonly="true"`); las validaciones cliente quedan declaradas para coherencia con el patrón del proyecto.
- El botón Reenviar tiene `showIf="estado == 'FALLIDO' &amp;&amp; id != null"` (V-007 a nivel de UI).
- `@Propios-action` filtra por DNI a nivel de `<domain>`. El permiso `Correo.propio` (paso 8) refuerza el filtro a nivel de fila.

**Verificación:**
- `./gradlew clean build --info` carga las vistas sin errores.
- Tras arrancar la app: los menús abren las grids y los formularios.

---

### Paso 8 — Reescribir `auth-correos.xml`

**Ruta:** `src/main/resources/data-init/input/auth-correos.xml`

**Acción:** REESCRIBIR completamente. El placeholder usa `self.usuario = ?` que no es coherente con A3 (filtrado por `dniDestinatario`).

**Contenido nuevo:**

```xml
<?xml version="1.0"?>
<auth>

  <!-- Administrador: ve, crea, edita, borra y reenvía correos de cualquier centro. -->
  <permission name="Correo.all"
              object="com.educaflow.subsystem.correos.db.Correo">
    <can create="true" read="true" write="true" remove="true" export="true"/>
  </permission>

  <!-- Supervisor (center-admins): ve y reenvía correos de su centro.
       NO crea ni borra manualmente (la creación es siempre programática vía CorreoService).
       write=true permite el reenvío. -->
  <permission name="Correo.centro"
              object="com.educaflow.subsystem.correos.db.Correo"
              condition="self.centro = ?"
              conditionParams="__user__.centroActivo">
    <can create="false" read="true" write="true" remove="false" export="false"/>
  </permission>

  <!-- Usuarios (Profesor/Exprofesor/Alumno/Exalumno): solo lectura de sus propios correos
       en Carpeta Ciudadana. Filtrado por DNI según asunción A3. -->
  <permission name="Correo.propio"
              object="com.educaflow.subsystem.correos.db.Correo"
              condition="self.dniDestinatario = ?"
              conditionParams="__user__.dni">
    <can create="false" read="true" write="false" remove="false" export="false"/>
  </permission>

</auth>
```

**Verificación:**
- El XML está bien formado.
- El binding del fichero en `input-config.xml` ya existe (no se toca).

---

### Paso 9 — Limpiar `auth.xml`

**Ruta:** `src/main/resources/data-init/input/auth.xml`

**Cambios:**

1. En la sección `<!-- Correos -->` (líneas 161-164), **eliminar** la definición duplicada del permiso, dejando solo un comentario:

```xml
<!-- Correos -->
<!-- Permisos Correo.all, Correo.centro y Correo.propio definidos en auth-correos.xml -->
```

2. **Mantener intactos** los wirings de los grupos:
   - `<group code="admins">` sigue conteniendo `<permission name="Correo.all"/>` (ya existe).
   - `<group code="users">` sigue conteniendo `<permission name="Correo.propio"/>` (ya existe).
   - `<group code="center-admins">` sigue conteniendo `<permission name="Correo.centro"/>` (ya existe).

**Justificación:** la nomenclatura `Correo.all` es coherente con el resto del proyecto (`User.all`, `Centro.all`, `TareaFirma.all`, etc.). El permiso se define una sola vez en `auth-correos.xml`; `auth.xml` solo lo referencia desde los grupos.

**Verificación:**
- `grep -n "name=\"Correo\\." src/main/resources/data-init/input/auth.xml` muestra exclusivamente las 3 referencias dentro de los `<group>` (no hay declaración `<permission name="Correo.all" object=...>`).
- `grep -n "Correo\\." src/main/resources/data-init/input/auth-correos.xml` muestra las 3 declaraciones `<permission ...>`.

---

### Paso 10 — Verificación final

1. **Compilar:**
   ```
   ./gradlew clean build --info
   ```
   Resultado esperado: **BUILD SUCCESSFUL**, sin errores en validación de XML, sin warnings de clases generadas faltantes.

2. **Comprobar artefactos generados** (no ejecutar más comandos, solo `ls`):
   - `build/src-gen/com/educaflow/subsystem/correos/db/Correo.java`
   - `build/src-gen/com/educaflow/subsystem/correos/db/EstadoCorreo.java`
   - `build/src-gen/com/educaflow/subsystem/correos/db/repo/AbstractCorreoRepository.java`

3. **Comando de arranque manual** (no se ejecuta automáticamente; lo lanza el usuario):
   ```
   ./gradlew --no-daemon run --debug-jvm --port 8080 --context-path /
   ```

4. **Pruebas manuales tras arrancar:**
   - **Admin** → menú `Notificaciones → Correos → Todos los correos` muestra todos los correos sin filtrar.
   - **Supervisor** (`center-admins`) → menú `Correos del centro` filtra por `centroActivo` (vía domain de `@Centro-action` + permiso `Correo.centro`). No puede crear ni borrar.
   - **Usuario** (`users`) → menú `Mis correos` solo muestra los correos donde `dniDestinatario == User.dni`. Sin botón Reenviar.
   - Abrir un correo en estado `FALLIDO` desde el form Main → aparece botón **Reenviar**; al pulsar, el form recarga con `numIntentos` incrementado y `estado` actualizado.
   - Abrir un correo en estado `ENVIADO` → no aparece botón.
   - Forzar reenvío vía API contra un correo `ENVIADO` → la respuesta es V-007 como diálogo de error.
   - Llamar `enviarCorreo` con SMTP caído (host inválido) → el `Correo` se persiste con `estado=FALLIDO` y `mensajeError` no nulo (I-1).
   - Llamar `enviarCorreo` con email inválido → lanza `BusinessException` con V-003 antes de tocar SMTP.

---

## Trazabilidad V-XXX → paso(s)

| V-ID                                         | Capa cliente                                                                       | Capa servidor                                                                     | Pasos   |
|----------------------------------------------|------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------|---------|
| V-001 (centro obligatorio)                   | `<check field="centro"/>` en `Local-validateSave-action`                           | `validateInsert` en `CorreoServiceImpl`                                           | 5, 7    |
| V-002 (email obligatorio)                    | `<check field="email"/>` en `Local-validateSave-action`                            | `validateInsert`                                                                  | 5, 7    |
| V-003 (email formato RFC simplificada, A6)   | regex en `if` de `<check field="email">`                                           | `EMAIL_PATTERN` en `validateInsert`                                               | 5, 7    |
| V-004 (DNI/NIE formato + dígito control, A5) | regex aproximada en `if` de `<check field="dniDestinatario">`                      | `DniUtil.isValid` en `validateInsert`                                             | 5, 7    |
| V-005 (asunto obligatorio)                   | `<check field="asunto"/>`                                                          | `validateInsert`                                                                  | 5, 7    |
| V-006 (cruzada htmlBody/textBody)            | `<check field="htmlBody">` con `if` cruzado                                        | `validateInsert`                                                                  | 5, 7    |
| V-007 (transición FALLIDO en reenvío)        | `showIf="estado == 'FALLIDO'"` en botón + `action-validate Local-validateReenviar` | `reenviarCorreo` lanza `BusinessException` con V-007                              | 5, 6, 7 |
| V-008 (coherencia centro/expediente, A7*)    | —                                                                                  | `validateInsert` (comparación `expediente.centro.id` vs `centro.id`)              | 5       |
| V-009 (inmutabilidad post-creación)          | UI: form Main solo lectura                                                         | `validateUpdate` con `comprobarInmutable` campo a campo y comparación de adjuntos | 5, 7    |

| Invariante                         | Implementación                                         | Paso  |
|------------------------------------|--------------------------------------------------------|-------|
| I-1 (sin rollback ante fallo SMTP) | `intentarEnvio` captura `Throwable` y persiste FALLIDO | 5     |
| I-2 (mensajeError sin stacktrace)  | `extraerMensaje` usa solo `Throwable.getMessage()`     | 5     |
| I-3 (queries JPA en repositorio)   | `CorreoRepository.findUserByDni`                       | 2, 5  |
| I-4 (id de BD como identidad)      | sin numerador, identidad implícita                     | 1     |
| I-5 (envío síncrono)               | llamada directa `mailSender.send(...)` sin scheduler   | 5     |

| Asunción                                          | Decisión en el plan                                                                     |
|---------------------------------------------------|-----------------------------------------------------------------------------------------|
| A1 (`usuarioDestinatario` solo en `enviarCorreo`) | `reenviarCorreo` no toca el campo                                                       | 5 |
| A2 (`from` no se persiste)                        | leído de `AppSettings.get().get("mail.smtp.from")` en `intentarEnvio`                   | 5 |
| A3 (Carpeta Ciudadana por `dniDestinatario`)      | permiso `Correo.propio` con `self.dniDestinatario = ?` y domain de `@Propios-action`    | 7, 8 |
| A4 (Externo/Familiar sin acceso)                  | sin grupo asignado a permisos `Correo.*`                                                | 8 |
| A5 (DNI con dígito control)                       | `DniUtil.isValid` en servidor                                                           | 5 |
| A6 (regex RFC 5322 simplificada)                  | `EMAIL_PATTERN`                                                                         | 5, 7 |
| A7 (coherencia centro/expediente)                 | implementado en `validateInsert`                                                        | 5 |
| A8 (props SMTP estándar Axelor)                   | leídas vía `AppSettings`; el binding ya existe en `SecretariaVirtualModule`             | 5 |
| A9 (adjunto borrado en reenvío → fallo claro)     | `MetaFileUtil.downloadContent` lanza → captura en `intentarEnvio` → FALLIDO con mensaje | 5 |

---

## Notas de unificación

- **Naming de action-views:** se elige mantener `@All-action`/`@Centro-action`/`@Propios-action` para no tocar `menus.xml` (4 de 5 planes propusieron esta opción). Las grids y forms siguen la nomenclatura del análisis (`@Main-*` y `@CarpetaCiudadana-*`).
- **Nombres de permisos:** se conserva `Correo.all` (en lugar de `Correo.admin`) por coherencia con el resto del proyecto, donde todos los permisos sin condición usan el sufijo `.all`. La definición pasa de `auth.xml` a `auth-correos.xml` (donde están los otros dos permisos).
- **Repositorio personalizado:** se crea `CorreoRepository` con `findUserByDni` para encapsular la consulta a `User` (regla I-3), evitando filter/bind inline en el servicio.
- **API de bytes de MetaFile:** se usa `com.educaflow.base.util.MetaFileUtil.downloadContent(metaFile)` (verificado existente en el código).
- **`BusinessException`:** es checked exception (extends `Exception`); `enviarCorreo` y `reenviarCorreo` declaran `throws BusinessException`.
- **Acceso al repositorio en el servicio:** `DefaultModelService<T>` no expone `getRepository()`; expone el campo protected `repository`. La impl usa `((CorreoRepository) repository).findUserByDni(...)` y `repository.find(id)`.
- **`ActionRequestHelper.getOriginalModel`:** la firma real es sin argumentos; el controlador invoca `requestHelper.getOriginalModel()` (no `getOriginalModel(allowProperties)`).
- **Interfaz `CorreoService`:** declara explícitamente `validateInsert/validateUpdate/validateRemove` como exige `k-sistemas/servicios.md`; sin esas firmas en la interfaz, las anotaciones `@Override` de la impl no compilan.
