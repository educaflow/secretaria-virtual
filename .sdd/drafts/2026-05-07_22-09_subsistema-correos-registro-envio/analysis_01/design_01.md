---
type: design
---

# Plan: Subsistema Correos

**Objetivo:** Crear el subsistema `correos` que registra y envía correos electrónicos con historial de estado, adjuntos y trazabilidad hacia expedientes y usuarios.
**Capa:** subsystem/correos
**Análisis de origen:** prompts/2026-05-07_22-09_subsistema-correos-registro-envio/analisis_2026-05-07_22-14/analisis.md
**Skills necesarios para la implementación:** k-sistemas, k-vistas, k-seguridad

---

## Ficheros a crear o modificar

| Fichero | Acción | Descripción |
|---------|--------|-------------|
| `subsystem/correos/domains/Correo.xml` | Crear | Entidad Correo + enum EstadoCorreo |
| `subsystem/correos/service/CorreoService.java` | Crear | Interfaz del servicio |
| `subsystem/correos/service/CorreoInsertDTO.java` | Crear | DTO de inserción |
| `subsystem/correos/service/impl/CorreoServiceImpl.java` | Crear | Implementación del servicio con envío SMTP |
| `subsystem/correos/controller/CorreoController.java` | Crear | Controlador para botón "Reintentar envío" |
| `subsystem/correos/views/Correo.xml` | Crear | Grid, form, actions, action-view |
| `resources/data-init/input/auth-correos.xml` | Crear | Permisos condicionales para Correo |
| `resources/data-init/input/auth.xml` | Modificar | Añadir Correo.all a admins; Correo.centro + Correo.propio a users |
| `resources/data-init/input-config.xml` | Modificar | Registrar auth-correos.xml |
| `secretariavirtual/menus/menus.xml` | Modificar | Añadir menuitem "Correos enviados" |

---

## Pasos

### Paso 1 — Dominio: Correo.xml

Crear el fichero:
`src/main/java/com/educaflow/subsystem/correos/domains/Correo.xml`

```xml
<domain-models xmlns="http://axelor.com/xml/ns/domain-models"
               xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
               xsi:schemaLocation="http://axelor.com/xml/ns/domain-models https://axelor.com/xml/ns/domain-models/domain-models_8.1.xsd">

    <module name="correos" package="com.educaflow.subsystem.correos.db"/>

    <entity name="Correo">
        <string name="para"        title="Para"           required="true"/>
        <string name="asunto"      title="Asunto"         required="true"/>
        <string name="cuerpoHtml"  title="Cuerpo HTML"    large="true" multiline="true"/>
        <string name="cuerpoTexto" title="Cuerpo texto"   large="true" multiline="true"/>
        <enum   name="estadoCorreo" ref="EstadoCorreo"    title="Estado" required="true"/>
        <datetime name="fechaEnvio" title="Fecha de envío"/>
        <string name="errorMensaje" title="Error de envío" large="true" multiline="true"/>
        <string name="dniDestinatario" title="DNI destinatario"/>
        <long   name="expedienteId" title="ID expediente"/>
        <many-to-one name="usuario" ref="com.axelor.auth.db.User"                     title="Usuario"/>
        <many-to-one name="centro"  ref="com.educaflow.subsystem.common.db.Centro"    title="Centro" required="true"/>
        <many-to-many name="adjuntos" ref="com.axelor.meta.db.MetaFile"               title="Adjuntos"/>
    </entity>

    <enum name="EstadoCorreo">
        <item name="PENDIENTE" title="Pendiente"/>
        <item name="ENVIADO"   title="Enviado"/>
        <item name="ERROR"     title="Error de envío"/>
    </enum>

</domain-models>
```

**Verificar:** `grep -r "EstadoCorreo" src/main/java/com/educaflow/subsystem/correos/` muestra el fichero creado. El build genera `Correo.java` y `EstadoCorreo.java` en `db/`.

---

### Paso 2 — Servicio: interfaz CorreoService + CorreoInsertDTO

Crear `src/main/java/com/educaflow/subsystem/correos/service/CorreoInsertDTO.java`:

```java
package com.educaflow.subsystem.correos.service;

import com.axelor.auth.db.User;
import com.axelor.meta.db.MetaFile;
import com.educaflow.subsystem.common.db.Centro;

import java.util.List;
import java.util.Objects;

public record CorreoInsertDTO(
        String para,
        String asunto,
        String cuerpoHtml,
        String cuerpoTexto,
        String dniDestinatario,
        Long expedienteId,
        User usuario,
        Centro centro,
        List<MetaFile> adjuntos
) {
    public CorreoInsertDTO {
        Objects.requireNonNull(para,    "para no puede ser null");
        Objects.requireNonNull(asunto,  "asunto no puede ser null");
        Objects.requireNonNull(centro,  "centro no puede ser null");
        if (adjuntos == null) adjuntos = List.of();
    }
}
```

Crear `src/main/java/com/educaflow/subsystem/correos/service/CorreoService.java`:

```java
package com.educaflow.subsystem.correos.service;

import com.axelor.db.modelservice.ModelService;
import com.educaflow.base.infrastructure.validation.messages.BusinessException;
import com.educaflow.subsystem.correos.db.Correo;

public interface CorreoService extends ModelService<Correo> {

    Correo enviar(CorreoInsertDTO dto) throws BusinessException;

    Correo reenviar(Long correoId) throws BusinessException;
}
```

**Verificar:** `grep -r "CorreoService" src/main/java/com/educaflow/subsystem/correos/service/` muestra los dos ficheros.

---

### Paso 3 — Servicio: implementación CorreoServiceImpl

Crear `src/main/java/com/educaflow/subsystem/correos/service/impl/CorreoServiceImpl.java`:

```java
package com.educaflow.subsystem.correos.service.impl;

import com.axelor.app.AppSettings;
import com.axelor.auth.db.repo.UserRepository;
import com.axelor.db.Repository;
import com.axelor.db.modelservice.DefaultModelService;
import com.axelor.inject.Beans;
import com.axelor.meta.db.MetaFile;
import com.educaflow.base.infrastructure.mail.Attach;
import com.educaflow.base.infrastructure.mail.Mail;
import com.educaflow.base.infrastructure.mail.MailSender;
import com.educaflow.base.infrastructure.validation.messages.BusinessException;
import com.educaflow.base.infrastructure.validation.messages.BusinessMessage;
import com.educaflow.base.infrastructure.validation.messages.BusinessMessages;
import com.educaflow.base.util.MetaFileUtil;
import com.educaflow.subsystem.correos.db.Correo;
import com.educaflow.subsystem.correos.db.EstadoCorreo;
import com.educaflow.subsystem.correos.service.CorreoInsertDTO;
import com.educaflow.subsystem.correos.service.CorreoService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CorreoServiceImpl extends DefaultModelService<Correo> implements CorreoService {

    public CorreoServiceImpl(Class<Correo> model, Repository repository) {
        super(Correo.class, repository);
    }

    @Override
    public Correo enviar(CorreoInsertDTO dto) throws BusinessException {
        Correo correo = new Correo();
        correo.setPara(dto.para());
        correo.setAsunto(dto.asunto());
        correo.setCuerpoHtml(dto.cuerpoHtml());
        correo.setCuerpoTexto(dto.cuerpoTexto());
        correo.setDniDestinatario(dto.dniDestinatario());
        correo.setExpedienteId(dto.expedienteId());
        correo.setUsuario(dto.usuario());
        correo.setCentro(dto.centro());
        correo.setEstadoCorreo(EstadoCorreo.PENDIENTE);
        if (dto.adjuntos() != null) {
            correo.setAdjuntos(new ArrayList<>(dto.adjuntos()));
        }

        correo = super.insert(correo);

        fireActionRule_EnviarCorreo(correo);

        return correo;
    }

    @Override
    public Correo reenviar(Long correoId) throws BusinessException {
        Correo correo = repository.find(correoId);

        BusinessMessages messages = new BusinessMessages();
        if (correo.getEstadoCorreo() != EstadoCorreo.ERROR) {
            messages.add(new BusinessMessage("estadoCorreo",
                    "Solo se puede reintentar el envío de un correo en estado ERROR. Estado actual: '"
                            + correo.getEstadoCorreo().name() + "'."));
            messages.throwIfNotEmpty();
        }

        fireActionRule_EnviarCorreo(correo);

        return correo;
    }

    /**************************************************************************/
    /************************** Validaciones ***********************************/
    /**************************************************************************/

    @Override
    protected void validateInsert(Correo correo, BusinessMessages messages) {
        if (correo.getPara() == null || correo.getPara().isBlank()) {
            messages.add(new BusinessMessage("para", "El campo 'para' es obligatorio."));
        } else {
            String email = correo.getPara().trim();
            int atPos = email.indexOf('@');
            boolean formatoValido = atPos > 0
                    && atPos < email.length() - 1
                    && email.indexOf('.', atPos) > atPos + 1;
            if (!formatoValido) {
                messages.add(new BusinessMessage("para",
                        "El email '" + correo.getPara() + "' no tiene un formato válido."));
            }
        }

        if (correo.getAsunto() == null || correo.getAsunto().isBlank()) {
            messages.add(new BusinessMessage("asunto", "El campo 'asunto' es obligatorio."));
        }

        boolean sinCuerpo = (correo.getCuerpoHtml() == null || correo.getCuerpoHtml().isBlank())
                && (correo.getCuerpoTexto() == null || correo.getCuerpoTexto().isBlank());
        if (sinCuerpo) {
            messages.add(new BusinessMessage("cuerpoHtml",
                    "Debe informar al menos el cuerpo HTML o el cuerpo en texto plano."));
        }
    }

    /**************************************************************************/
    /************************** Action Rules ***********************************/
    /**************************************************************************/

    private void fireActionRule_EnviarCorreo(Correo correo) {
        EstadoCorreo estadoAnterior  = correo.getEstadoCorreo();
        String      errorAnterior    = correo.getErrorMensaje();
        LocalDateTime fechaAnterior  = correo.getFechaEnvio();

        // Construir original para super.update (estado antes del intento)
        Correo original = new Correo();
        original.setId(correo.getId());
        original.setEstadoCorreo(estadoAnterior);
        original.setErrorMensaje(errorAnterior);
        original.setFechaEnvio(fechaAnterior);

        try {
            MailSender mailSender = Beans.get(MailSender.class);
            String from = AppSettings.get().get("mail.smtp.user");

            List<Attach> attachs = new ArrayList<>();
            if (correo.getAdjuntos() != null) {
                for (MetaFile metaFile : correo.getAdjuntos()) {
                    attachs.add(new Attach(
                            metaFile.getFileName(),
                            MetaFileUtil.downloadContent(metaFile),
                            metaFile.getFileType()
                    ));
                }
            }

            Mail mail = new Mail(
                    List.of(correo.getPara()),
                    from,
                    correo.getAsunto(),
                    correo.getCuerpoHtml(),
                    correo.getCuerpoTexto(),
                    attachs
            );

            mailSender.send(mail);

            correo.setEstadoCorreo(EstadoCorreo.ENVIADO);
            correo.setFechaEnvio(LocalDateTime.now());
            correo.setErrorMensaje(null);

        } catch (Exception e) {
            correo.setEstadoCorreo(EstadoCorreo.ERROR);
            correo.setFechaEnvio(LocalDateTime.now());
            correo.setErrorMensaje(e.getMessage());
        }

        super.update(correo, original);
    }
}
```

**Notas de implementación:**
- `Beans.get(MailSender.class)` recupera la instancia Singleton registrada en `SecretariaVirtualModule`.
- `MetaFileUtil.downloadContent(metaFile)` existe en `base/util/MetaFileUtil.java`.
- `super.update(correo, original)` llama a `validateUpdate`. Como la entidad es inmutable desde la UI, `validateUpdate` no añade validaciones (no es necesario sobreescribirlo).
- **Nota:** el `Transport.sendMessage(...)` en `MailSenderImpl` está actualmente comentado. El envío real se habilitará cuando ese código se descomente — la estructura del servicio es correcta.

**Verificar:** `grep -r "CorreoServiceImpl" src/main/java/com/educaflow/subsystem/correos/` muestra el fichero.

---

### Paso 4 — Controlador: CorreoController

Crear `src/main/java/com/educaflow/subsystem/correos/controller/CorreoController.java`:

```java
package com.educaflow.subsystem.correos.controller;

import com.axelor.db.modelservice.ModelServiceFactory;
import com.axelor.inject.Beans;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.educaflow.base.util.ActionRequestHelper;
import com.educaflow.base.util.ActionResponseHelper;
import com.educaflow.subsystem.correos.db.Correo;
import com.educaflow.subsystem.correos.service.CorreoService;

public class CorreoController {

    public void reenviar(ActionRequest request, ActionResponse response) {
        try {
            Correo correo = ActionRequestHelper.getContext(request, Correo.class);
            CorreoService correoService = (CorreoService) Beans.get(ModelServiceFactory.class).resolve(Correo.class);
            correoService.reenviar(correo.getId());
            response.setReload(true);
        } catch (Exception e) {
            ActionResponseHelper.setError(response, e);
        }
    }
}
```

**Verificar:** `grep -r "CorreoController" src/main/java/com/educaflow/subsystem/correos/` muestra el fichero.

---

### Paso 5 — Vistas: Correo.xml

Crear `src/main/java/com/educaflow/subsystem/correos/views/Correo.xml`:

```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<object-views xmlns="http://axelor.com/xml/ns/object-views"
xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
xsi:schemaLocation="http://axelor.com/xml/ns/object-views
https://axelor.com/xml/ns/object-views/object-views_8.1.xsd">

    <!-- *************************************************************************** -->
    <!-- ****************************** Correo : Vistas **************************** -->
    <!-- *************************************************************************** -->

    <action-view name="subsysCorreos.Correo@Main-action" title="Correos enviados"
                 model="com.educaflow.subsystem.correos.db.Correo">
        <view type="grid" name="subsysCorreos.Correo@Main-grid"/>
        <view type="form" name="subsysCorreos.Correo@Main-form"/>
        <view-param name="show-toolbar-form" value="false"/>
    </action-view>

    <grid name="subsysCorreos.Correo@Main-grid"
          model="com.educaflow.subsystem.correos.db.Correo"
          title="" orderBy="-fechaEnvio"
          allowSearchFields="true" canAdvanceSearch="false" canRefresh="false"
          canNew="false" editable="false" edit-icon="false" x-selector="none"
          canEdit="false" canDelete="false" canSave="false" canEditOnClick="true">
        <field name="para"             title="Para"           width="220px"/>
        <field name="asunto"           title="Asunto"/>
        <field name="estadoCorreo"     title="Estado"         width="120px"/>
        <field name="fechaEnvio"       title="Fecha envío"    width="160px"/>
        <field name="dniDestinatario"  title="DNI destinatario" width="140px"/>
        <field name="centro"           title="Centro"         width="180px"/>
    </grid>

    <form name="subsysCorreos.Correo@Main-form"
          title="Correo enviado"
          model="com.educaflow.subsystem.correos.db.Correo"
          width="large"
          canAttach="false" canBack="false" canDelete="false" canNew="false"
          canSave="false" canMore="false" canEdit="false">

        <panel name="estadoPanel" title="Estado" colSpan="12">
            <field name="estadoCorreo"    colSpan="3"/>
            <field name="fechaEnvio"      colSpan="3"/>
            <field name="centro"          colSpan="3"/>
            <field name="dniDestinatario" colSpan="3"/>
            <field name="expedienteId"    colSpan="3" showIf="expedienteId != null"/>
            <field name="usuario"         colSpan="3" showIf="usuario != null"/>
            <field name="errorMensaje"    colSpan="12" showIf="estadoCorreo == 'ERROR'" readonly="true" css="error"/>
        </panel>

        <panel name="correoPanel" title="Contenido" colSpan="12">
            <field name="para"    colSpan="8"/>
            <field name="asunto"  colSpan="12"/>
            <field name="cuerpoHtml"  colSpan="12" showIf="cuerpoHtml != null" widget="html"/>
            <field name="cuerpoTexto" colSpan="12" showIf="cuerpoTexto != null" widget="text"/>
        </panel>

        <panel-related name="adjuntos" field="adjuntos" title="Adjuntos"
            grid-view="subsysCorreos.Correo.MetaFile@Main-grid"
            form-view="subsysCorreos.Correo.MetaFile@View-form"
            colSpan="12" readonly="true"
            canNew="false" canEdit="false" canRemove="false" canSelect="false"
            showFooter="false"/>

        <panel name="buttons-panel" title="" colSpan="12" showFrame="false">
            <button name="btnReintentar" title="Reintentar envío"
                    onClick="subsysCorreos.Correo@Main-btnReintentar-action"
                    showIf="estadoCorreo == 'ERROR'"
                    colSpan="3" colOffset="9"/>
        </panel>

    </form>

    <!-- *************** Correo : Acciones de las tareas principales *************** -->
    <action-group name="subsysCorreos.Correo@Main-btnReintentar-action">
        <action name="subsysCorreos.Correo@Main-reenviar-action"/>
        <action name="save"/>
    </action-group>

    <!-- *************** Correo : Acciones de Validaciones en local *************** -->

    <!-- *************** Correo : Acciones básicas que cambian campos simples *************** -->

    <!-- *************** Correo : Acciones de llamadas Remotas al servidor *************** -->
    <action-method name="subsysCorreos.Correo@Main-reenviar-action"
                   model="com.educaflow.subsystem.correos.db.Correo">
        <call class="com.educaflow.subsystem.correos.controller.CorreoController" method="reenviar"/>
    </action-method>


    <!-- ******************************************************************************************** -->
    <!-- ****************************** Correo.MetaFile : Vistas *********************************** -->
    <!-- ******************************************************************************************** -->

    <grid name="subsysCorreos.Correo.MetaFile@Main-grid"
          model="com.axelor.meta.db.MetaFile"
          title="" editable="false" edit-icon="false" x-selector="none"
          canNew="false" canEdit="false" canDelete="false" canSave="false" canViewOnClick="true">
        <field name="fileName" title="Fichero"/>
        <field name="fileType" title="Tipo"/>
        <field name="fileSize" title="Tamaño (bytes)" width="120px"/>
    </grid>

    <form name="subsysCorreos.Correo.MetaFile@View-form"
          title="Adjunto"
          model="com.axelor.meta.db.MetaFile"
          width="large"
          canAttach="false" canBack="true" canDelete="false" canNew="false"
          canSave="false" canMore="false">
        <panel name="MetaFile" title="">
            <field name="fileName"  colSpan="6" readonly="true"/>
            <field name="fileType"  colSpan="3" readonly="true"/>
            <field name="fileSize"  colSpan="3" readonly="true"/>
        </panel>
    </form>

</object-views>
```

**Verificar:** `grep -r "subsysCorreos" src/main/java/com/educaflow/subsystem/correos/views/` muestra el fichero. Comprobar que los nombres de vistas y acciones son coherentes entre sí.

---

### Paso 6 — Menú: menus.xml

Modificar `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml`.

Añadir un nuevo menuitem raíz para "Correos" (visible para todos) y su subitem, justo antes del menuitem `registro-menuitem` (order=60):

```xml
<menuitem name="correos-menuitem" title="Correos" order="55"/>
    <menuitem name="correos-enviados-menuitem" parent="correos-menuitem"
              title="Correos enviados"
              action="subsysCorreos.Correo@Main-action"
              order="1"/>
```

**Ubicación exacta:** insertar antes de la línea:
```xml
<menuitem name="registro-menuitem"  title="Registro"             order="60"/>
```

**Verificar:** `grep -n "correos-menuitem" src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` muestra ambos menuitems.

---

### Paso 7 — Seguridad: auth-correos.xml + auth.xml + input-config.xml

**7a.** Crear `src/main/resources/data-init/input/auth-correos.xml`:

```xml
<?xml version="1.0"?>
<auth>

  <!-- Permiso para ver correos del propio usuario -->
  <permission name="Correo.propio"
              object="com.educaflow.subsystem.correos.db.Correo"
              condition="self.usuario = ?"
              conditionParams="__user__">
    <can create="false" read="true" write="false" remove="false" export="false"/>
  </permission>

  <!-- Permiso para ver correos del propio centro (cualquier usuario con CentroUsuario en ese centro) -->
  <permission name="Correo.centro"
              object="com.educaflow.subsystem.correos.db.Correo"
              condition="self.centro IN (SELECT cu.centro FROM com.educaflow.subsystem.common.db.CentroUsuario cu WHERE cu.usuario = ?)"
              conditionParams="__user__">
    <can create="false" read="true" write="true" remove="false" export="false"/>
  </permission>

</auth>
```

**Nota:** `Correo.centro` tiene `write="true"` porque el botón "Reintentar envío" actualiza el estado del correo. `Correo.propio` tiene `write="false"` porque los usuarios normales solo pueden ver sus propios correos, no reintentarlos.

**7b.** Modificar `src/main/resources/data-init/input/auth.xml`:

En la sección de permisos (antes de los grupos), añadir el bloque de comentario `<!-- Correos -->` con el permiso sin condición para admins:

```xml
  <!-- Correos -->
  <permission name="Correo.all" object="com.educaflow.subsystem.correos.db.Correo">
    <can create="true" read="true" write="true" remove="true" export="true"/>
  </permission>
```

En el bloque `<group code="admins">`, añadir al final:
```xml
    <permission name="Correo.all"/>
```

En el bloque `<group code="users">`, añadir al final:
```xml
    <permission name="Correo.centro"/>
    <permission name="Correo.propio"/>
```

**Nota de seguridad:** Los admins ven todos los correos vía `Correo.all` (sin condición). Los users ven los de su centro y los propios. La distinción entre "supervisor/director ve su centro" vs "usuario normal ve solo los suyos" usa la misma condición de CentroUsuario — cualquier usuario con un CentroUsuario verá los correos de su centro. Esta es la granularidad que soporta el modelo de seguridad actual.

**7c.** Modificar `src/main/resources/data-init/input-config.xml`:

Añadir la entrada para `auth-correos.xml` **antes** de la entrada de `auth.xml` (los permisos deben declararse antes que los roles que los referencian):

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

**Ubicación exacta:** insertar antes de la línea `<input file="auth.xml" root="auth">`.

**Verificar:** `grep -n "auth-correos" src/main/resources/data-init/input-config.xml` muestra la entrada.

---

### Paso 8 — Verificación final

Compilar el proyecto completo:

```bash
./gradlew clean build --info
```

Verificaciones adicionales:
```bash
# Confirmar que las clases generadas existen
grep -r "EstadoCorreo" src/main/java/com/educaflow/subsystem/correos/

# Confirmar que los ficheros de servicio están en el paquete correcto (service.impl)
find src/main/java/com/educaflow/subsystem/correos -name "*.java" | sort

# Confirmar que no hay módulo Guice creado (no debe existir)
find src/main/java/com/educaflow/subsystem/correos -name "*Module*"

# Confirmar que el permiso aparece en auth.xml
grep -n "Correo" src/main/resources/data-init/input/auth.xml

# Confirmar que el menuitem apunta a la action-view correcta
grep -n "subsysCorreos.Correo@Main-action" src/main/java/com/educaflow/secretariavirtual/menus/menus.xml
```

La compilación debe terminar sin errores. El arranque cargará los permisos desde `auth-correos.xml` y `auth.xml` automáticamente.
