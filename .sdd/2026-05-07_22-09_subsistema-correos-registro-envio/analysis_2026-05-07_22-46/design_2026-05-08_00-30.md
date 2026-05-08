---
type: design
analysis-file: analysis.md
---

# Plan: subsistema correos

**Objetivo:** Crear el subsistema `correos` para envío y registro de correos electrónicos con persistencia del estado de envío y reenvío manual en caso de error.
**Capa:** `subsystem/correos`
**Análisis de origen:** user-stories/2026-05-07_22-09_subsistema-correos-registro-envio/analysis_2026-05-07_22-46/analysis.md
**Skills necesarios para la implementación:** k-sistemas, k-vistas, k-seguridad

---

## Ficheros a crear o modificar

| Fichero | Acción | Descripción |
|---------|--------|-------------|
| `src/main/java/com/educaflow/subsystem/correos/domains/Correo.xml` | Crear | Entidad Correo + enum EstadoCorreo |
| `src/main/java/com/educaflow/subsystem/correos/service/CorreoService.java` | Crear | Interfaz del servicio |
| `src/main/java/com/educaflow/subsystem/correos/service/impl/CorreoServiceImpl.java` | Crear | Implementación con envío en background |
| `src/main/java/com/educaflow/subsystem/correos/controller/CorreoController.java` | Crear | Controlador: reenviar |
| `src/main/java/com/educaflow/subsystem/correos/views/Correo.xml` | Crear | Grids, formulario inteligente, actions, menuitems |
| `src/main/resources/data-init/input/auth-correos.xml` | Crear | Permisos Correo.centro y Correo.propio |
| `src/main/resources/data-init/input-config.xml` | Modificar | Añadir bloque auth-correos.xml antes de auth.xml |
| `src/main/resources/data-init/input/auth.xml` | Modificar | Añadir Correo.all + asignar permisos a grupos |
| `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` | Modificar | Añadir menú Notificaciones con submenús de correos |

---

## Pasos

### Paso 1 — Dominio: entidad `Correo` y enum `EstadoCorreo`

**Fichero a crear:** `src/main/java/com/educaflow/subsystem/correos/domains/Correo.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<domain-models xmlns="http://axelor.com/xml/ns/domain-models"
               xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
               xsi:schemaLocation="http://axelor.com/xml/ns/domain-models https://axelor.com/xml/ns/domain-models/domain-models_8.1.xsd">

    <module name="correos" package="com.educaflow.subsystem.correos.db"/>

    <entity name="Correo">
        <many-to-one name="centro"        ref="com.educaflow.subsystem.common.db.Centro"                  required="true" title="Centro"/>
        <string      name="para"          required="true"  title="Para"/>
        <string      name="de"            title="De"/>
        <string      name="asunto"        required="true"  title="Asunto"/>
        <string      name="cuerpoHtml"    large="true" multiline="true" title="Cuerpo HTML"/>
        <string      name="cuerpoTexto"   large="true" multiline="true" title="Cuerpo texto plano"/>
        <string      name="dni"           required="true"  title="DNI"/>
        <many-to-one name="usuario"       ref="com.axelor.auth.db.User"                                    title="Usuario"/>
        <many-to-one name="expediente"    ref="com.educaflow.subsystem.expedientes.db.Expediente"          title="Expediente"/>
        <enum        name="estado"        ref="EstadoCorreo" required="true"                               title="Estado"/>
        <datetime    name="enviadoEn"     title="Enviado en"/>
        <datetime    name="ultimoFalloEn" title="Último fallo en"/>
        <many-to-many name="adjuntos"     ref="com.axelor.meta.db.MetaFile"                                title="Adjuntos"/>
    </entity>

    <enum name="EstadoCorreo">
        <item name="PENDIENTE" title="Pendiente"/>
        <item name="ENVIADO"   title="Enviado"/>
        <item name="ERROR"     title="Error"/>
    </enum>

</domain-models>
```

**Verificación:** Compilar con `./gradlew clean build --info`. Las clases `Correo`, `EstadoCorreo` y `AbstractCorreoRepository` deben aparecer en `build/src-gen/`. No debe haber errores de compilación.

---

### Paso 2 — Servicio: interfaz `CorreoService` e implementación `CorreoServiceImpl`

**Contexto necesario:**
- `MailSender` está en `com.educaflow.base.infrastructure.mail.MailSender` con método `void send(Mail mail)`.
- `Mail` es un record: `Mail(List<String> to, String from, String subject, String htmlBody, String textBody, List<Attach> attachs)`.
- `Attach` es un record: `Attach(String fileName, byte[] data, String mimeType)`.
- `SmtpCredentialSimplePassword` es un record en `com.educaflow.base.infrastructure.mail.impl.SmtpCredentialSimplePassword`. El campo `userName()` contiene la dirección del remitente (`de`).
- `MetaFileUtil.downloadContent(MetaFile)` devuelve `byte[]` y está en `com.educaflow.base.util.MetaFileUtil`.
- `MetaFile.getFileType()` devuelve el mimeType del adjunto.
- El envío se hace en un hilo background que gestiona su propia transacción JPA via `JPA.em().getTransaction()`.
- Las consultas JPQL van en repositorios, pero la búsqueda de `User` por `dni` se hace con `JpaRepository.of(User.class)` ya que es una entidad de otra capa sin repositorio propio en `correos`.

**Fichero 1 — Interfaz:** `src/main/java/com/educaflow/subsystem/correos/service/CorreoService.java`

```java
package com.educaflow.subsystem.correos.service;

import com.axelor.db.modelservice.ModelService;
import com.educaflow.base.infrastructure.validation.messages.BusinessMessages;
import com.educaflow.subsystem.correos.db.Correo;

import java.util.Optional;

public interface CorreoService extends ModelService<Correo> {

    Optional<BusinessMessages> validateInsert(Correo correo);
    Optional<BusinessMessages> validateUpdate(Correo correo, Correo correoOriginal);

    void reenviar(Correo correo);
}
```

**Fichero 2 — Implementación:** `src/main/java/com/educaflow/subsystem/correos/service/impl/CorreoServiceImpl.java`

```java
package com.educaflow.subsystem.correos.service.impl;

import com.axelor.auth.db.User;
import com.axelor.db.JPA;
import com.axelor.db.JpaRepository;
import com.axelor.db.Repository;
import com.axelor.db.modelservice.DefaultModelService;
import com.axelor.meta.db.MetaFile;
import com.educaflow.base.infrastructure.mail.Attach;
import com.educaflow.base.infrastructure.mail.Mail;
import com.educaflow.base.infrastructure.mail.MailSender;
import com.educaflow.base.infrastructure.mail.impl.SmtpCredentialSimplePassword;
import com.educaflow.base.infrastructure.validation.messages.BusinessMessages;
import com.educaflow.base.util.MetaFileUtil;
import com.educaflow.subsystem.correos.db.Correo;
import com.educaflow.subsystem.correos.db.EstadoCorreo;
import com.educaflow.subsystem.correos.service.CorreoService;
import jakarta.inject.Inject;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CorreoServiceImpl extends DefaultModelService<Correo> implements CorreoService {

    @Inject
    MailSender mailSender;

    @Inject
    SmtpCredentialSimplePassword smtpCredentialSimplePassword;

    public CorreoServiceImpl(Class<Correo> model, Repository<Correo> repository) {
        super(model, repository);
    }

    @Override
    public Correo insert(Correo correo) {
        correo.setDe(smtpCredentialSimplePassword.userName());
        correo.setEstado(EstadoCorreo.PENDIENTE);
        fireActionRule_AutoResolverUsuario(correo);
        correo = super.insert(correo);

        final Long correoId = correo.getId();
        Thread thread = new Thread(() -> fireActionRule_EnviarCorreo(correoId));
        thread.setDaemon(true);
        thread.start();

        return correo;
    }

    @Override
    public void reenviar(Correo correo) {
        final Long correoId = correo.getId();
        Thread thread = new Thread(() -> fireActionRule_EnviarCorreo(correoId));
        thread.setDaemon(true);
        thread.start();
    }

    @Override
    public Optional<BusinessMessages> validateInsert(Correo correo) {
        return Optional.empty();
    }

    @Override
    public Optional<BusinessMessages> validateUpdate(Correo correo, Correo correoOriginal) {
        return Optional.empty();
    }

    /*************************************************************************************/
    /********************************    Action Rules    *********************************/
    /*************************************************************************************/

    private void fireActionRule_AutoResolverUsuario(Correo correo) {
        if (correo.getDni() == null || correo.getDni().isBlank()) {
            return;
        }
        User usuario = JpaRepository.of(User.class).all()
                .filter("self.dni = :dni")
                .bind("dni", correo.getDni())
                .fetchOne();
        correo.setUsuario(usuario);
    }

    private void fireActionRule_EnviarCorreo(Long correoId) {
        // Este método se ejecuta en un hilo background con su propia transacción JPA.
        try {
            JPA.em().getTransaction().begin();
            Correo correo = JpaRepository.of(Correo.class).find(correoId);
            try {
                Mail mail = buildMail(correo);
                mailSender.send(mail);
                correo.setEstado(EstadoCorreo.ENVIADO);
                correo.setEnviadoEn(LocalDateTime.now());
            } catch (Exception e) {
                correo.setEstado(EstadoCorreo.ERROR);
                correo.setUltimoFalloEn(LocalDateTime.now());
            }
            JpaRepository.of(Correo.class).save(correo);
            JPA.em().getTransaction().commit();
        } catch (Exception e) {
            if (JPA.em().getTransaction().isActive()) {
                JPA.em().getTransaction().rollback();
            }
        } finally {
            JPA.em().close();
        }
    }

    private Mail buildMail(Correo correo) {
        List<Attach> attachs = new ArrayList<>();
        if (correo.getAdjuntos() != null) {
            for (MetaFile metaFile : correo.getAdjuntos()) {
                byte[] data = MetaFileUtil.downloadContent(metaFile);
                attachs.add(new Attach(metaFile.getFileName(), data, metaFile.getFileType()));
            }
        }
        return new Mail(
            List.of(correo.getPara()),
            correo.getDe(),
            correo.getAsunto(),
            correo.getCuerpoHtml(),
            correo.getCuerpoTexto(),
            attachs
        );
    }
}
```

**Verificación:** Compilar. No debe haber errores. `ModelServiceFactory` descubre `CorreoServiceImpl` automáticamente por convención de paquetes — no crear ningún módulo Guice.

---

### Paso 3 — Controlador: `CorreoController`

**Contexto necesario:**
- `ActionRequestHelper` y `ActionResponseHelper` están en `com.educaflow.base.infrastructure.axelorhelper`.
- `@CallMethod` está en `com.axelor.meta.CallMethod`.
- `modelServiceFactory.resolve(Correo.class)` devuelve el servicio (sin repositorio explícito, igual que en `TareaFirmaController`).
- Para error simple de texto usar `actionResponse.setError(message)` directamente (no existe `doResponseError` en `ActionResponseHelper`).
- Para reload usar `actionResponse.setReload(true)` directamente.
- `requestHelper.getOriginalModel()` (sin parámetros) carga el modelo desde BD clonado; devuelve `null` si `id == null`.
- La validación de reenvío es: `estado` debe ser `EstadoCorreo.ERROR`. Mensaje: `"No se puede reenviar el correo '{asunto}' porque su estado actual es '{estado}'. Solo se pueden reenviar correos en estado ERROR."`

**Fichero:** `src/main/java/com/educaflow/subsystem/correos/controller/CorreoController.java`

```java
package com.educaflow.subsystem.correos.controller;

import com.axelor.db.modelservice.ModelServiceFactory;
import com.axelor.meta.CallMethod;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.educaflow.base.infrastructure.axelorhelper.ActionRequestHelper;
import com.educaflow.base.infrastructure.axelorhelper.ActionResponseHelper;
import com.educaflow.base.infrastructure.validation.messages.BusinessMessages;
import com.educaflow.base.util.AllowProperties;
import com.educaflow.subsystem.correos.db.Correo;
import com.educaflow.subsystem.correos.db.EstadoCorreo;
import com.educaflow.subsystem.correos.service.CorreoService;
import jakarta.inject.Inject;

import java.util.Optional;

public class CorreoController {

    @Inject
    private ModelServiceFactory modelServiceFactory;

    @CallMethod
    public void reenviar(ActionRequest actionRequest, ActionResponse actionResponse) {
        final CorreoService service = (CorreoService) modelServiceFactory.resolve(Correo.class);

        ActionRequestHelper<Correo> requestHelper = new ActionRequestHelper<>(actionRequest, Correo.class);

        AllowProperties allowProperties = AllowProperties.createAllowAllProperties();
        Correo correo = requestHelper.getModel(allowProperties);

        if (correo.getEstado() != EstadoCorreo.ERROR) {
            actionResponse.setError(
                "No se puede reenviar el correo '" + correo.getAsunto()
                + "' porque su estado actual es '" + correo.getEstado()
                + "'. Solo se pueden reenviar correos en estado ERROR."
            );
            return;
        }

        service.reenviar(correo);
        actionResponse.setReload(true);
    }
}
```

**Verificación:** Compilar. No debe haber errores.

---

### Paso 4 — Vistas: `Correo.xml`

**Diseño:**
- Prefijo de vistas: `subsysCorreos`
- Tres `action-view` con dominios diferentes para los tres menús
- Dos grids: `@Main-grid` (canNew=true, para admins/center-admins) y `@Propios-grid` (canNew=false, para users)
- Un formulario `@Main-form` inteligente con dos paneles condicionales:
  - Panel "crear" (`showIf="id == null"`): campos editables para nuevo correo
  - Panel "detalle" (`showIf="id != null"`): todos los campos readonly
- El campo `expediente` NO lleva `grid-view` ni `form-view` porque las vistas `subsysExpedientes.Expediente@Search-grid` y `subsysExpedientes.Expediente@View-form` no existen en el subsistema de expedientes.

**Fichero a crear:** `src/main/java/com/educaflow/subsystem/correos/views/Correo.xml`

```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<object-views xmlns="http://axelor.com/xml/ns/object-views"
xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
xsi:schemaLocation="http://axelor.com/xml/ns/object-views
https://axelor.com/xml/ns/object-views/object-views_8.1.xsd">

    <!-- ************************************************************************** -->
    <!-- ****************************** Correo : Vistas ****************************** -->
    <!-- ************************************************************************** -->

    <action-view name="subsysCorreos.Correo@All-action" title="Todos los correos"
                 model="com.educaflow.subsystem.correos.db.Correo">
        <view type="grid" name="subsysCorreos.Correo@Main-grid"/>
        <view type="form" name="subsysCorreos.Correo@Main-form"/>
        <view-param name="show-toolbar-form" value="false"/>
        <view-param name="forceEdit" value="true"/>
    </action-view>

    <action-view name="subsysCorreos.Correo@Centro-action" title="Correos del centro"
                 model="com.educaflow.subsystem.correos.db.Correo">
        <view type="grid" name="subsysCorreos.Correo@Main-grid"/>
        <view type="form" name="subsysCorreos.Correo@Main-form"/>
        <view-param name="show-toolbar-form" value="false"/>
        <view-param name="forceEdit" value="true"/>
        <domain>self.centro IN (SELECT u.centroActivo FROM com.axelor.auth.db.User u WHERE u = :__user__)</domain>
    </action-view>

    <action-view name="subsysCorreos.Correo@Propios-action" title="Mis correos"
                 model="com.educaflow.subsystem.correos.db.Correo">
        <view type="grid" name="subsysCorreos.Correo@Propios-grid"/>
        <view type="form" name="subsysCorreos.Correo@Main-form"/>
        <view-param name="show-toolbar-form" value="false"/>
        <view-param name="forceEdit" value="true"/>
        <domain>self.usuario = :__user__</domain>
    </action-view>

    <grid name="subsysCorreos.Correo@Main-grid"
          model="com.educaflow.subsystem.correos.db.Correo"
          title="" orderBy="createdOn DESC" newButtonTitle="Nuevo correo"
          allowSearchFields="true" canAdvanceSearch="false" canRefresh="false"
          canNew="true" editable="false" edit-icon="false" x-selector="none"
          canEdit="false" canDelete="false" canSave="false" canEditOnClick="true"
    >
        <field name="estado"    width="120px"/>
        <field name="asunto"/>
        <field name="para"      width="200px"/>
        <field name="dni"       width="120px"/>
        <field name="centro"    width="180px"/>
        <field name="createdOn" width="160px"/>
    </grid>

    <grid name="subsysCorreos.Correo@Propios-grid"
          model="com.educaflow.subsystem.correos.db.Correo"
          title="" orderBy="createdOn DESC"
          allowSearchFields="true" canAdvanceSearch="false" canRefresh="false"
          canNew="false" editable="false" edit-icon="false" x-selector="none"
          canEdit="false" canDelete="false" canSave="false" canEditOnClick="true"
    >
        <field name="estado"    width="120px"/>
        <field name="asunto"/>
        <field name="para"      width="200px"/>
        <field name="dni"       width="120px"/>
        <field name="centro"    width="180px"/>
        <field name="createdOn" width="160px"/>
    </grid>

    <form name="subsysCorreos.Correo@Main-form" title="Correo"
          model="com.educaflow.subsystem.correos.db.Correo"
          width="large"
          onNew="subsysCorreos.Correo@Main-onNew-action"
          canAttach="false" canBack="false" canDelete="false"
          canNew="false" canSave="false" canMore="false" canBackOnSave="true">

        <!-- Panel de creación: solo visible para registros nuevos -->
        <panel name="Correo-crear" title="Nuevo correo" showIf="id == null" colSpan="12">
            <field name="para"        colSpan="6"  required="true"/>
            <field name="asunto"      colSpan="6"  required="true"/>
            <field name="dni"         colSpan="6"  required="true"/>
            <field name="expediente"  colSpan="6"/>
            <field name="cuerpoHtml"  colSpan="12" multiline="true" large="true" widget="html"/>
            <field name="cuerpoTexto" colSpan="12" multiline="true" large="true"/>
            <field name="adjuntos"    colSpan="12"/>
            <field name="centro"      showIf="false"/>
        </panel>

        <!-- Panel de detalle: solo visible para registros existentes, todo readonly -->
        <panel name="Correo-detalle" title="Detalle del correo"
               showIf="id != null" colSpan="12" readonly="true">
            <field name="estado"        colSpan="3"/>
            <field name="enviadoEn"     colSpan="3" showIf="enviadoEn != null"/>
            <field name="ultimoFalloEn" colSpan="3" showIf="ultimoFalloEn != null"/>
            <field name="createdOn"     colSpan="3"/>
            <field name="asunto"        colSpan="8"/>
            <field name="centro"        colSpan="4"/>
            <field name="para"          colSpan="4"/>
            <field name="de"            colSpan="4"/>
            <field name="dni"           colSpan="2"/>
            <field name="usuario"       colSpan="2"/>
            <field name="expediente"    colSpan="4"/>
            <field name="cuerpoHtml"    colSpan="12" readonly="true" widget="html"/>
            <field name="cuerpoTexto"   colSpan="12" showIf="cuerpoTexto != null"/>
            <field name="adjuntos"      colSpan="12" readonly="true"/>
        </panel>

        <!-- Panel de botones -->
        <panel name="buttons-panel" title="" colSpan="12" showFrame="false">
            <button name="btnReenviar" title="Reenviar"
                    onClick="subsysCorreos.Correo@Main-btnReenviar-action"
                    css="btn-warning" colSpan="2" outline="true"
                    showIf="id != null &amp;&amp; estado == 'ERROR'"/>
            <button name="btnCancel" title="Cancelar"
                    onClick="subsysCorreos.Correo@Main-btnCancel-action"
                    colSpan="2" colOffset="6" outline="true"
                    showIf="id == null"/>
            <button name="btnVolver" title="Volver"
                    onClick="back"
                    colSpan="2" colOffset="8" outline="true"
                    showIf="id != null"/>
            <button name="btnSave" title="Enviar"
                    onClick="subsysCorreos.Correo@Main-btnSave-action"
                    colSpan="2"
                    showIf="id == null"/>
        </panel>
    </form>

    <!-- *************** Correo : Acciones de las tareas principales *************** -->
    <action-group name="subsysCorreos.Correo@Main-btnSave-action">
        <action name="subsysCorreos.Correo@Main-Local-validateFields-action"/>
        <action name="subsysCorreos.Correo@Main-Local-validateCuerpo-action"/>
        <action name="save"/>
    </action-group>

    <action-group name="subsysCorreos.Correo@Main-btnCancel-action">
        <action name="back"/>
    </action-group>

    <action-group name="subsysCorreos.Correo@Main-btnReenviar-action">
        <action name="subsysCorreos.Correo@Main-Remote-reenviar-action"/>
    </action-group>

    <action-group name="subsysCorreos.Correo@Main-onNew-action">
        <action name="subsysCorreos.Correo@Main-set-centro-action"/>
    </action-group>

    <!-- *************** Correo : Acciones de Validaciones en local *************** -->
    <action-condition name="subsysCorreos.Correo@Main-Local-validateFields-action">
        <check field="para"/>
        <check field="para"
               if="para != null &amp;&amp; !String(para).match(/^[^\s@]+@[^\s@]+\.[^\s@]+$/)"
               error="El formato del email no es válido. Debe tener el formato usuario@dominio.com"/>
        <check field="asunto"/>
        <check field="dni"/>
        <check field="dni"
               if="dni != null &amp;&amp; !String(dni).match(/^(\d{8}|[XYZxyz]\d{7})[A-Za-z]$/i)"
               error="El formato del DNI/NIE no es válido."/>
        <check field="centro"/>
    </action-condition>

    <action-validate name="subsysCorreos.Correo@Main-Local-validateCuerpo-action">
        <error message="Debe indicar el cuerpo del correo en formato HTML o en texto plano."
               if="(cuerpoHtml == null || cuerpoHtml.isEmpty()) &amp;&amp; (cuerpoTexto == null || cuerpoTexto.isEmpty())"/>
    </action-validate>

    <!-- *************** Correo : Acciones básicas que cambian campos simples *************** -->
    <action-record name="subsysCorreos.Correo@Main-set-centro-action"
                   model="com.educaflow.subsystem.correos.db.Correo">
        <field name="centro" expr="eval: __user__?.centroActivo"/>
    </action-record>

    <!-- *************** Correo : Acciones de llamadas Remotas al servidor *************** -->
    <action-method name="subsysCorreos.Correo@Main-Remote-reenviar-action"
                   model="com.educaflow.subsystem.correos.db.Correo">
        <call class="com.educaflow.subsystem.correos.controller.CorreoController"
              method="reenviar"/>
    </action-method>

</object-views>
```

**Notas:**
- Si `widget="html"` en `cuerpoHtml` da errores, sustituir por `multiline="true"` sin widget.
- El `showIf` del botón `btnReenviar` usa `&amp;&amp;` (escape XML de `&&`).
- `createdOn` es el campo heredado de Axelor (equivalente al `creadoEn` del análisis).

**Verificación:** Compilar. Grep para confirmar que todas las actions están definidas:
```bash
grep 'name="subsysCorreos' src/main/java/com/educaflow/subsystem/correos/views/Correo.xml | wc -l
```
Debe retornar al menos 12 líneas.

---

### Paso 5 — Menús: añadir sección "Notificaciones" en `menus.xml`

**Fichero a modificar:** `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml`

Insertar el siguiente bloque **antes** del `<menuitem name="administracionSv-menuitem" ...>` (que tiene `order="50"`), de forma que quede con `order="40"`:

```xml
    <menuitem name="notificaciones-menuitem" title="Notificaciones" order="40"/>
        <menuitem name="notificaciones-correos-menuitem" parent="notificaciones-menuitem" title="Correos" order="1"/>
            <menuitem name="notificaciones-correos-todos-menuitem"
                      parent="notificaciones-correos-menuitem"
                      title="Todos los correos"
                      action="subsysCorreos.Correo@All-action"
                      groups="admins"
                      order="1"/>
            <menuitem name="notificaciones-correos-centro-menuitem"
                      parent="notificaciones-correos-menuitem"
                      title="Correos del centro"
                      action="subsysCorreos.Correo@Centro-action"
                      groups="center-admins"
                      order="2"/>
            <menuitem name="notificaciones-correos-propios-menuitem"
                      parent="notificaciones-correos-menuitem"
                      title="Mis correos"
                      action="subsysCorreos.Correo@Propios-action"
                      groups="users"
                      order="3"/>
```

**Verificación:** Compilar y confirmar que el XML es válido.

---

### Paso 6 — Seguridad: permisos de `Correo`

**Contexto de seguridad:**
- `admins` → acceso total (`Correo.all` sin condición, en auth.xml)
- `center-admins` → ven y crean correos de su centro (`Correo.centro` con condición `self.centro = ?`)
- `users` → solo ven sus propios correos, no pueden crear (`Correo.propio` con condición `self.usuario = ?`)
- Parámetros: `?` sin índice, nunca `?1`. Se pasan objetos entidad, no IDs.
- Los permisos en `auth-correos.xml` deben ir ANTES del bloque de `auth.xml` en `input-config.xml`.
- En `auth-correos.xml` usar atributos `@condition` y `@conditionParams` (no elementos hijo), o el binding de `input-config.xml` los dejará como `null`.

**Fichero 1 a crear:** `src/main/resources/data-init/input/auth-correos.xml`

```xml
<?xml version="1.0"?>
<auth>

  <!-- Permiso para center-admins: ver y crear los correos de su centro -->
  <permission name="Correo.centro"
              object="com.educaflow.subsystem.correos.db.Correo"
              condition="self.centro = ?"
              conditionParams="__user__.centroActivo">
    <can create="true" read="true" write="false" remove="false" export="false"/>
  </permission>

  <!-- Permiso para users: solo ver sus propios correos, no crear -->
  <permission name="Correo.propio"
              object="com.educaflow.subsystem.correos.db.Correo"
              condition="self.usuario = ?"
              conditionParams="__user__">
    <can create="false" read="true" write="false" remove="false" export="false"/>
  </permission>

</auth>
```

**Fichero 2 a modificar:** `src/main/resources/data-init/input-config.xml`

Añadir el siguiente bloque **antes** del bloque `<input file="auth.xml" ...>`. Insertarlo después del bloque de `auth-gestioncentro.xml`:

```xml
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
```

**Fichero 3 a modificar:** `src/main/resources/data-init/input/auth.xml`

**3a.** Añadir la declaración del permiso `Correo.all` antes del bloque `<group code="admins">`, junto a los demás permisos:

```xml
  <!-- Correos -->
  <permission name="Correo.all" object="com.educaflow.subsystem.correos.db.Correo">
    <can create="true" read="true" write="true" remove="true" export="true"/>
  </permission>
```

**3b.** Añadir `Correo.all` al grupo `admins` (al final de la lista de permisos del grupo):

```xml
    <permission name="Correo.all"/>
```

**3c.** Añadir `Correo.propio` al grupo `users` (al final de la lista de permisos del grupo):

```xml
    <permission name="Correo.propio"/>
```

**3d.** Añadir el grupo `center-admins` con el permiso `Correo.centro` (si no existe ya en auth.xml, añadirlo antes del cierre del fichero `</auth>`):

```xml
  <group code="center-admins">
    <permission name="Correo.centro"/>
  </group>
```

**Verificación:** Compilar. Confirmar que `input-config.xml` tiene `auth-correos.xml` antes de `auth.xml`:
```bash
grep -n "auth-correos\|auth\.xml" src/main/resources/data-init/input-config.xml
```
La línea de `auth-correos.xml` debe aparecer con número menor que la de `auth.xml`.

---

### Paso 7 — Verificación final

```bash
./gradlew clean build --info
```

Confirmar que:
1. La compilación termina sin errores (`BUILD SUCCESSFUL`).
2. Las clases `Correo`, `EstadoCorreo` y `AbstractCorreoRepository` aparecen en `build/src-gen/`.
3. El servicio es descubrible automáticamente: `grep -r "CorreoServiceImpl" build/ | head -3` debe encontrar la clase compilada.
4. Las vistas referencian correctamente el controlador: `grep "CorreoController" src/main/java/com/educaflow/subsystem/correos/views/Correo.xml` debe retornar la línea de `action-method`.
