---
type: design
---

# Plan: Subsistema de Correos

**Objetivo:** Implementar el subsistema `correos` que registra y envía correos electrónicos con trazabilidad completa y reintento manual desde la UI.
**Capa:** `subsystem/correos`
**Análisis de origen:** `prompts/2026-05-07_22-09_subsistema-correos-registro-envio/analisis_2026-05-07_22-14/analisis.md`
**Skills necesarios para la implementación:** `k-sistemas`, `k-vistas`

---

## Contexto técnico relevante

- `MailSender` ya está registrado como `@Singleton` en `SecretariaVirtualModule` — se puede inyectar con `@Inject` en cualquier servicio.
- El remitente (`from`) se obtiene de `AppSettings.get().get("mail.smtp.user")` (patrón de `SecretariaVirtualModule`).
- `__user__.centroActivo` es el campo que identifica el centro activo del usuario actual (patrón de `auth-gestioncentro.xml`).
- No existe ningún `many-to-many` con `MetaFile` en el proyecto; el plan introduce el primero. Si Axelor no lo admite en la versión actual, sustituir por `one-to-many`.

---

## Ficheros a crear o modificar

| Fichero | Acción | Descripción |
|---------|--------|-------------|
| `src/main/java/com/educaflow/subsystem/correos/domains/Correo.xml` | Crear | Entidad Correo + enum EstadoCorreo |
| `src/main/java/com/educaflow/subsystem/correos/service/CorreoInsertDTO.java` | Crear | DTO para la operación `enviar()` |
| `src/main/java/com/educaflow/subsystem/correos/service/CorreoService.java` | Crear | Interfaz del servicio |
| `src/main/java/com/educaflow/subsystem/correos/service/impl/CorreoServiceImpl.java` | Crear | Implementación: envío SMTP + validaciones |
| `src/main/java/com/educaflow/subsystem/correos/controller/CorreoController.java` | Crear | Controlador para el botón "Reintentar envío" |
| `src/main/java/com/educaflow/subsystem/correos/views/Correo.xml` | Crear | Grid + formulario solo lectura + botón Reintentar |
| `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` | Modificar | Añadir "Correos enviados" bajo Administración SV |
| `src/main/resources/data-init/input/auth-correos.xml` | Crear | Permisos de acceso por nivel de usuario |

---

## Pasos

### Paso 1 — Dominio: Correo.xml

Crear el fichero `src/main/java/com/educaflow/subsystem/correos/domains/Correo.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<domain-models xmlns="http://axelor.com/xml/ns/domain-models"
               xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
               xsi:schemaLocation="http://axelor.com/xml/ns/domain-models
               https://axelor.com/xml/ns/domain-models/domain-models_8.1.xsd">

    <module name="correos" package="com.educaflow.subsystem.correos.db"/>

    <enum name="EstadoCorreo" numeric="true">
        <item name="PENDIENTE" value="0" title="Pendiente"/>
        <item name="ENVIADO"   value="1" title="Enviado"/>
        <item name="ERROR"     value="2" title="Error"/>
    </enum>

    <entity name="Correo" repository="abstract">
        <string    name="para"            required="true"                                                  title="Para"/>
        <string    name="asunto"          required="true"                                                  title="Asunto"/>
        <string    name="cuerpoHtml"      large="true"                                                     title="Cuerpo HTML"/>
        <string    name="cuerpoTexto"     large="true"                                                     title="Cuerpo texto plano"/>
        <enum      name="estado"          ref="EstadoCorreo" required="true"                               title="Estado"/>
        <date-time name="fechaEnvio"                                                                       title="Fecha de envío"/>
        <string    name="errorMensaje"                                                                     title="Mensaje de error"/>
        <string    name="dniDestinatario"                                                                  title="DNI destinatario"/>
        <long      name="expedienteId"                                                                     title="ID expediente"/>
        <many-to-one  name="usuario"   ref="com.axelor.auth.db.User"                                      title="Usuario"/>
        <many-to-one  name="centro"    ref="com.educaflow.subsystem.common.db.Centro" required="true"     title="Centro"/>
        <many-to-many name="adjuntos"  ref="com.axelor.meta.db.MetaFile"                                  title="Adjuntos"/>
    </entity>

</domain-models>
```

**Verificar:**
```bash
./gradlew generateCode 2>&1 | grep -i "error"
# No debe haber errores
grep -r "EstadoCorreo" build/src-gen/
# Debe encontrar el enum generado
grep -r "class Correo " build/src-gen/
# Debe encontrar la entidad generada
grep -r "AbstractCorreoRepository" build/src-gen/
# Debe encontrar el repositorio abstracto generado
```

---

### Paso 2 — DTO de inserción: CorreoInsertDTO.java

Crear `src/main/java/com/educaflow/subsystem/correos/service/CorreoInsertDTO.java`:

```java
package com.educaflow.subsystem.correos.service;

import java.util.List;

public record CorreoInsertDTO(
        String para,
        String asunto,
        String cuerpoHtml,
        String cuerpoTexto,
        Long centroId,
        String dniDestinatario,
        Long expedienteId,
        Long usuarioId,
        List<Long> adjuntoIds
) {
}
```

Los campos son opcionales en el record (sin `requireNonNull`) porque la validación ocurre en el servicio, donde se pueden acumular múltiples errores con mensajes descriptivos.

**Verificar:**
```bash
./gradlew compileJava 2>&1 | grep -i "error"
```

---

### Paso 3 — Interfaz del servicio: CorreoService.java

Crear `src/main/java/com/educaflow/subsystem/correos/service/CorreoService.java`:

```java
package com.educaflow.subsystem.correos.service;

import com.axelor.db.modelservice.ModelService;
import com.educaflow.base.infrastructure.validation.messages.BusinessMessages;
import com.educaflow.subsystem.correos.db.Correo;

import java.util.Optional;

public interface CorreoService extends ModelService<Correo> {

    // Operación principal: crea el registro y envía vía SMTP. Solo llamado por código.
    Correo enviar(CorreoInsertDTO dto);

    // Reintento manual desde la UI. Solo aplicable cuando estado == ERROR.
    Correo reenviar(Long correoId);

    // Validaciones para el insert interno (campos del Correo ya construido)
    Optional<BusinessMessages> validateInsert(Correo correo);

    // validateUpdate y validateRemove devuelven vacío: la entidad es inmutable desde la UI
    Optional<BusinessMessages> validateUpdate(Correo correo, Correo original);
    Optional<BusinessMessages> validateRemove(Correo correo);

    // Validación de precondición para el botón Reintentar
    Optional<BusinessMessages> validateReenviar(Correo correo);
}
```

**Verificar:** Compila sin errores. Extiende `ModelService<Correo>`.

---

### Paso 4 — Implementación del servicio: CorreoServiceImpl.java

Crear `src/main/java/com/educaflow/subsystem/correos/service/impl/CorreoServiceImpl.java`:

```java
package com.educaflow.subsystem.correos.service.impl;

import com.axelor.app.AppSettings;
import com.axelor.db.JpaRepository;
import com.axelor.db.Repository;
import com.axelor.db.modelservice.DefaultModelService;
import com.axelor.meta.db.MetaFile;
import com.educaflow.base.infrastructure.mail.Mail;
import com.educaflow.base.infrastructure.mail.MailSender;
import com.educaflow.base.infrastructure.validation.messages.BusinessMessage;
import com.educaflow.base.infrastructure.validation.messages.BusinessMessages;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.correos.db.Correo;
import com.educaflow.subsystem.correos.db.EstadoCorreo;
import com.educaflow.subsystem.correos.service.CorreoInsertDTO;
import com.educaflow.subsystem.correos.service.CorreoService;
import com.google.inject.Inject;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

public class CorreoServiceImpl extends DefaultModelService<Correo> implements CorreoService {

    @Inject
    private MailSender mailSender;

    // Constructor obligatorio — ModelServiceFactory lo invoca por reflexión
    public CorreoServiceImpl(Class<Correo> model, Repository<Correo> repository) {
        super(model, repository);
    }

    @Override
    public Correo enviar(CorreoInsertDTO dto) {
        // 1. Validar y resolver centroId (validación de BD antes de construir la entidad)
        Centro centro = null;
        if (dto.centroId() == null) {
            throw new IllegalArgumentException("El campo 'centroId' es obligatorio.");
        }
        centro = JpaRepository.of(Centro.class).find(dto.centroId());
        if (centro == null) {
            throw new IllegalArgumentException(
                "El centro con ID '" + dto.centroId() + "' no existe.");
        }

        // 2. Resolver usuario (opcional)
        com.axelor.auth.db.User usuario = null;
        if (dto.usuarioId() != null) {
            usuario = JpaRepository.of(com.axelor.auth.db.User.class).find(dto.usuarioId());
        }

        // 3. Resolver adjuntos (opcionales — solo los que existan en BD)
        List<MetaFile> adjuntos = new ArrayList<>();
        if (dto.adjuntoIds() != null) {
            for (Long id : dto.adjuntoIds()) {
                MetaFile mf = JpaRepository.of(MetaFile.class).find(id);
                if (mf != null) {
                    adjuntos.add(mf);
                }
            }
        }

        // 4. Construir entidad con estado PENDIENTE
        Correo correo = new Correo();
        correo.setPara(dto.para());
        correo.setAsunto(dto.asunto());
        correo.setCuerpoHtml(dto.cuerpoHtml());
        correo.setCuerpoTexto(dto.cuerpoTexto());
        correo.setDniDestinatario(dto.dniDestinatario());
        correo.setExpedienteId(dto.expedienteId());
        correo.setEstado(EstadoCorreo.PENDIENTE);
        correo.setCentro(centro);
        correo.setUsuario(usuario);
        correo.setAdjuntos(new HashSet<>(adjuntos));

        // 5. Persistir en estado PENDIENTE — validateInsert valida campos del correo
        correo = super.insert(correo);

        // 6. Intentar envío SMTP y actualizar estado
        return fireActionRule_intentarEnvio(correo);
    }

    @Override
    public Correo reenviar(Long correoId) {
        Correo correo = repository.find(correoId);
        if (correo == null) {
            throw new IllegalArgumentException(
                "El correo con ID '" + correoId + "' no existe.");
        }
        return fireActionRule_intentarEnvio(correo);
    }

    @Override
    public Optional<BusinessMessages> validateInsert(Correo correo) {
        BusinessMessages messages = new BusinessMessages();

        // Validación de 'para': obligatorio + formato email básico
        if (correo.getPara() == null || correo.getPara().isBlank()) {
            messages.add(new BusinessMessage("para", "El campo 'para' es obligatorio."));
        } else {
            String para = correo.getPara().trim();
            int atIdx = para.indexOf('@');
            boolean emailValido = atIdx > 0
                    && atIdx < para.length() - 1
                    && para.indexOf('.', atIdx) > atIdx;
            if (!emailValido) {
                messages.add(new BusinessMessage("para",
                    "El email '" + correo.getPara() + "' no tiene un formato válido."));
            }
        }

        // Validación de 'asunto': obligatorio
        if (correo.getAsunto() == null || correo.getAsunto().isBlank()) {
            messages.add(new BusinessMessage("asunto", "El campo 'asunto' es obligatorio."));
        }

        // Al menos uno de los cuerpos debe estar informado
        boolean tieneCuerpo =
            (correo.getCuerpoHtml()   != null && !correo.getCuerpoHtml().isBlank()) ||
            (correo.getCuerpoTexto()  != null && !correo.getCuerpoTexto().isBlank());
        if (!tieneCuerpo) {
            messages.add(new BusinessMessage("cuerpoHtml",
                "Debe informar al menos el cuerpo HTML o el cuerpo en texto plano."));
        }

        return messages.isEmpty() ? Optional.empty() : Optional.of(messages);
    }

    @Override
    public Optional<BusinessMessages> validateUpdate(Correo correo, Correo original) {
        // La entidad es inmutable desde la UI — sin validaciones de actualización
        return Optional.empty();
    }

    @Override
    public Optional<BusinessMessages> validateRemove(Correo correo) {
        // No se permite borrar correos desde la UI
        return Optional.empty();
    }

    @Override
    public Optional<BusinessMessages> validateReenviar(Correo correo) {
        BusinessMessages messages = new BusinessMessages();

        if (correo.getEstado() != EstadoCorreo.ERROR) {
            String estadoActual = correo.getEstado() != null
                ? correo.getEstado().name()
                : "null";
            messages.add(new BusinessMessage("estado",
                "Solo se puede reintentar el envío de un correo en estado ERROR. "
                + "Estado actual: '" + estadoActual + "'."));
        }

        return messages.isEmpty() ? Optional.empty() : Optional.of(messages);
    }

    /*************************************************************************************/
    /********************************    Action Rules    *********************************/
    /*************************************************************************************/

    private Correo fireActionRule_intentarEnvio(Correo correo) {
        try {
            String from = AppSettings.get().get("mail.smtp.user");
            Mail mail = new Mail(
                List.of(correo.getPara()),
                from,
                correo.getAsunto(),
                correo.getCuerpoHtml(),
                correo.getCuerpoTexto(),
                List.of()   // adjuntos como Attach: pendiente si se requiere envío de ficheros vía SMTP
            );
            mailSender.send(mail);
            correo.setEstado(EstadoCorreo.ENVIADO);
            correo.setFechaEnvio(LocalDateTime.now());
            correo.setErrorMensaje(null);
        } catch (Exception e) {
            correo.setEstado(EstadoCorreo.ERROR);
            correo.setErrorMensaje(
                e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        }
        repository.save(correo);
        return correo;
    }
}
```

**Notas de implementación:**
- `@Inject MailSender mailSender` funciona porque `MailSender` está registrado como `@Singleton` en `SecretariaVirtualModule`.
- `repository.save(correo)` en `fireActionRule_intentarEnvio` es una actualización interna de estado (resultado SMTP), no un CRUD estándar del usuario, por lo que no pasa por `validateUpdate`.
- Los adjuntos no se envían vía SMTP en esta versión inicial (se pasa `List.of()` como `Attach`). Para enviar adjuntos, hay que leer los bytes del `MetaFile` desde el sistema de ficheros de Axelor.

**Verificar:**
```bash
./gradlew compileJava 2>&1 | grep -i "error"
# La clase está en service.impl — ModelServiceFactory la descubre automáticamente
grep -r "CorreoServiceImpl" build/classes/
```

---

### Paso 5 — Controlador: CorreoController.java

Crear `src/main/java/com/educaflow/subsystem/correos/controller/CorreoController.java`:

```java
package com.educaflow.subsystem.correos.controller;

import com.axelor.db.JpaRepository;
import com.axelor.db.Repository;
import com.axelor.db.modelservice.ModelServiceFactory;
import com.axelor.meta.CallMethod;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.educaflow.base.infrastructure.axelorhelper.ActionRequestHelper;
import com.educaflow.base.infrastructure.axelorhelper.ActionResponseHelper;
import com.educaflow.base.infrastructure.validation.messages.BusinessMessages;
import com.educaflow.base.util.AllowProperties;
import com.educaflow.subsystem.correos.db.Correo;
import com.educaflow.subsystem.correos.service.CorreoService;
import com.google.inject.Inject;

import java.util.Optional;

public class CorreoController {

    @Inject
    private ModelServiceFactory modelServiceFactory;

    @CallMethod
    public void reenviar(ActionRequest actionRequest, ActionResponse actionResponse) {
        final Repository<Correo> repository = JpaRepository.of(Correo.class);
        final CorreoService correoService =
            (CorreoService) modelServiceFactory.resolve(Correo.class, repository);

        ActionRequestHelper<Correo> requestHelper =
            new ActionRequestHelper(actionRequest, Correo.class);
        ActionResponseHelper responseHelper = new ActionResponseHelper(actionResponse);

        // Cargar desde BD para validar el estado real, no el estado enviado por el cliente
        Long correoId = requestHelper.getId();
        Correo correo = repository.find(correoId);

        Optional<BusinessMessages> validationResult = correoService.validateReenviar(correo);
        if (validationResult.isPresent()) {
            responseHelper.doResponseBusinessMessagesAsError(validationResult.get());
            return;
        }

        correoService.reenviar(correoId);
        actionResponse.setReload(true);
    }
}
```

**Verificar:**
```bash
./gradlew compileJava 2>&1 | grep -i "error"
```

---

### Paso 6 — Vistas: Correo.xml

Crear `src/main/java/com/educaflow/subsystem/correos/views/Correo.xml`.

Antes de escribir el fichero, calcular la longitud de la línea del comentario de cabecera:
- Línea 2: `<!-- ` (5) + `******************************` (30) + ` Correo : Vistas ` (17) + `******************************` (30) + ` -->` (4) = **86 caracteres**
- Líneas 1 y 3: `<!-- ` (5) + 77 asteriscos + ` -->` (4) = 86 caracteres

```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<object-views xmlns="http://axelor.com/xml/ns/object-views"
xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
xsi:schemaLocation="http://axelor.com/xml/ns/object-views
https://axelor.com/xml/ns/object-views/object-views_8.1.xsd">

    <!-- ************************************************************************* -->
    <!-- ****************************** Correo : Vistas ****************************** -->
    <!-- ************************************************************************* -->

    <action-view name="subsysCorreos.Correo@Main-action" title="Correos enviados"
                 model="com.educaflow.subsystem.correos.db.Correo">
        <view type="grid" name="subsysCorreos.Correo@Main-grid"/>
        <view type="form" name="subsysCorreos.Correo@Main-form"/>
        <view-param name="show-toolbar-form" value="false"/>
    </action-view>

    <grid name="subsysCorreos.Correo@Main-grid" model="com.educaflow.subsystem.correos.db.Correo"
          title="" orderBy="-fechaEnvio" allowSearchFields="true"
          canAdvanceSearch="false" canRefresh="false" canNew="false"
          editable="false" edit-icon="false" x-selector="none"
          canEdit="false" canDelete="false" canSave="false" canEditOnClick="true"
    >
        <field name="para"/>
        <field name="asunto"/>
        <field name="estado"/>
        <field name="fechaEnvio"/>
        <field name="dniDestinatario"/>
        <field name="centro"/>
    </grid>

    <form name="subsysCorreos.Correo@Main-form" title="Correo"
          model="com.educaflow.subsystem.correos.db.Correo"
          width="large"
          canAttach="false" canBack="false" canDelete="false" canNew="false" canSave="false" canMore="false">

        <panel name="datos" title="Datos del correo">
            <field name="estado"          colSpan="3"  readonly="true"/>
            <field name="fechaEnvio"      colSpan="3"  readonly="true"/>
            <field name="centro"          colSpan="6"  readonly="true"/>
            <field name="para"            colSpan="6"  readonly="true"/>
            <field name="asunto"          colSpan="6"  readonly="true"/>
            <field name="dniDestinatario" colSpan="4"  readonly="true"/>
            <field name="expedienteId"    colSpan="4"  readonly="true"/>
            <field name="usuario"         colSpan="4"  readonly="true"/>
        </panel>

        <panel name="contenido" title="Contenido">
            <field name="cuerpoHtml"  colSpan="12" readonly="true" widget="html"/>
            <field name="cuerpoTexto" colSpan="12" readonly="true"/>
        </panel>

        <panel name="errorPanel" title="Error" showIf="estado == 'ERROR'">
            <field name="errorMensaje" colSpan="12" readonly="true"/>
        </panel>

        <field name="adjuntos" colSpan="12" readonly="true"/>

        <panel name="buttons-panel" title="" colSpan="12" showFrame="false">
            <button name="btnReenviar" title="Reintentar envío"
                    onClick="subsysCorreos.Correo@Main-btnReenviar-action"
                    colSpan="3" showIf="estado == 'ERROR'"/>
        </panel>
    </form>

    <!-- *************** Correo : Acciones de las tareas principales *************** -->
    <action-group name="subsysCorreos.Correo@Main-btnReenviar-action">
        <action name="subsysCorreos.Correo@Main-Remote-reenviar-action"/>
    </action-group>

    <!-- *************** Correo : Acciones de Validaciones en local *************** -->

    <!-- *************** Correo : Acciones básicas que cambian campos simples *************** -->

    <!-- *************** Correo : Acciones de llamadas Remotas al servidor *************** -->
    <action-method name="subsysCorreos.Correo@Main-Remote-reenviar-action"
                   model="com.educaflow.subsystem.correos.db.Correo">
        <call class="com.educaflow.subsystem.correos.controller.CorreoController"
              method="reenviar"/>
    </action-method>

</object-views>
```

**Notas sobre los comentarios de cabecera:**
Después de escribir el fichero, verificar que las líneas 1 y 3 tienen exactamente los mismos caracteres que la línea 2:
```bash
awk 'NR>=9 && NR<=11 {print NR": "length($0)" chars: "$0}' \
  src/main/java/com/educaflow/subsystem/correos/views/Correo.xml
```
Si las líneas 1 y 3 no tienen el mismo length que la línea 2, ajustar el número de asteriscos.

**Verificar:**
```bash
xmllint --noout src/main/java/com/educaflow/subsystem/correos/views/Correo.xml
grep "subsysCorreos.Correo@Main-action" src/main/java/com/educaflow/subsystem/correos/views/Correo.xml
```

---

### Paso 7 — Menú

Modificar `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml`.

Añadir la siguiente entrada **después** del menuitem `administracionSv-certificadosDigitales-menuitem` (order="11"):

```xml
        <menuitem name="administracionSv-correos-menuitem"
                  parent="administracionSv-menuitem"
                  title="Correos enviados"
                  action="subsysCorreos.Correo@Main-action"
                  order="12"/>
```

El bloque `administracionSv-menuitem` queda:
```xml
<menuitem name="administracionSv-menuitem" groups="admins" title="Administración SV" order="50"/>
    <menuitem name="administracionSv-centro-menuitem" ... order="1"/>
    <menuitem name="administracionSv-importacion-menuitem" ... order="2"/>
    <menuitem name="administracionSv-usuarios-menuitem" ... order="3"/>
    <menuitem name="administracionSv-dispositivosCriptograficos-menuitem" ... order="10"/>
    <menuitem name="administracionSv-certificadosDigitales-menuitem" ... order="11"/>
    <menuitem name="administracionSv-correos-menuitem"
              parent="administracionSv-menuitem"
              title="Correos enviados"
              action="subsysCorreos.Correo@Main-action"
              order="12"/>
```

**Verificar:**
```bash
grep "administracionSv-correos-menuitem" \
  src/main/java/com/educaflow/secretariavirtual/menus/menus.xml
xmllint --noout src/main/java/com/educaflow/secretariavirtual/menus/menus.xml
```

---

### Paso 8 — Seguridad

Crear `src/main/resources/data-init/input/auth-correos.xml`:

```xml
<?xml version="1.0"?>
<auth>

  <!-- Administrador: acceso de lectura y escritura sin filtro de centro ni usuario.
       La escritura (write=true) permite actualizar estado/fechaEnvio/errorMensaje vía reenviar. -->
  <permission name="Correo.leer-admin"
              object="com.educaflow.subsystem.correos.db.Correo"
  >
    <can create="false" read="true" write="true" remove="false" export="false"/>
  </permission>

  <!-- Supervisor, JefeEstudios, Director, Secretario, Administrativo:
       ven y pueden reintentar correos del centro activo del usuario. -->
  <permission name="Correo.leer-centro"
              object="com.educaflow.subsystem.correos.db.Correo"
              condition="self.centro = ?"
              conditionParams="__user__.centroActivo"
  >
    <can create="false" read="true" write="true" remove="false" export="false"/>
  </permission>

  <!-- Resto de usuarios: solo ven sus propios correos (campo usuario).
       write=true necesario si se permite reintentar correos propios. -->
  <permission name="Correo.leer-propio"
              object="com.educaflow.subsystem.correos.db.Correo"
              condition="self.usuario = ?"
              conditionParams="__user__"
  >
    <can create="false" read="true" write="false" remove="false" export="false"/>
  </permission>

</auth>
```

**Nota importante:** los permisos deben asignarse a los roles correctos en `auth.xml` o el fichero de configuración de roles del proyecto. Consultar el skill `k-seguridad` para el patrón de asignación de permisos a roles en este proyecto.

**Verificar:**
```bash
grep "Correo.leer" src/main/resources/data-init/input/auth-correos.xml
xmllint --noout src/main/resources/data-init/input/auth-correos.xml
```

---

### Paso 9 — Verificación final

```bash
# 1. Compilar todo
./gradlew clean build --info 2>&1 | tail -20

# 2. Verificar clases generadas por Axelor del dominio
grep -r "EstadoCorreo" build/src-gen/
grep -r "class Correo " build/src-gen/
grep -r "AbstractCorreoRepository" build/src-gen/

# 3. Verificar que ModelServiceFactory descubre el servicio
grep -r "CorreoServiceImpl" build/classes/

# 4. Arrancar y probar
./gradlew --no-daemon run --port 8080 --context-path /
```

**Prueba manual:**
1. Login como Administrador → Administración SV → "Correos enviados": el listado abre sin errores.
2. El listado muestra columnas: Para, Asunto, Estado, Fecha de envío, DNI destinatario, Centro.
3. Hacer clic en un correo con estado `ERROR`: el formulario abre en solo lectura y muestra el botón "Reintentar envío".
4. Hacer clic en un correo con estado `ENVIADO`: el formulario no muestra el botón "Reintentar envío".
5. Llamar `correoService.enviar(dto)` desde otro servicio: se crea el registro y se intenta el envío SMTP.
