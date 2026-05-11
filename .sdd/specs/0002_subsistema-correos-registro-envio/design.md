---
type: design
---

# Diseño: Subsistema Correos — Registro y envío

**Objetivo:** Crear el subsistema `subsystem/correos` que envía correos vía SMTP global de forma síncrona, registra cada envío como entidad inmutable `Correo` (estado ENVIADO/FALLIDO) y permite reintentar manualmente los fallidos, con vistas Admin/Supervisor (Main) y Carpeta Ciudadana, y permisos multicentro.
**Capa:** subsystem/correos
**Análisis de origen:** .sdd/drafts/2026-05-07_22-09_subsistema-correos-registro-envio/analysis_05/analysis.md
**Skills necesarios para la implementación:** k-sistemas, k-vistas, k-seguridad

---

## Decisiones de diseño previas

### D1 — Naming de action-views alineado con `menus.xml` actual

`menus.xml` ya contiene tres `<menuitem>` que apuntan a `subsysCorreos.Correo@All-action`, `@Centro-action` y `@Propios-action`. El fichero de vistas declarará exactamente esas tres `action-view` para no tocar `menus.xml`. Las grids/forms internas siguen la nomenclatura del análisis (`@Main-grid|form` y `@CarpetaCiudadana-grid|form`).

### D2 — Permisos: convención `Entidad.{all|centro|propio}`

Tres permisos:
- `Correo.all` (sin condición) → grupo `admins`.
- `Correo.centro` con `self.centro = ?` y `__user__.centroActivo` → grupo `center-admins`. `write=true` porque el reenvío es un `update` de la fila.
- `Correo.propio` con `self.dniDestinatario = ?` y `__user__.dni` → grupo `users` (Profesor/Exprofesor/Alumno/Exalumno). Externo y Familiar no reciben permiso (A4*).

Las tres declaraciones viven en `auth-correos.xml` (su binding ya está en `input-config.xml`). Los wirings de grupo viven en `auth.xml`.

### D3 — Repositorio personalizado para encapsular consultas JPA (I-3)

Se crea `CorreoRepository extends AbstractCorreoRepository` con `findUserByDni(String)`. El servicio nunca escribe consultas JPA inline.

### D4 — Mapeo cliente/servidor de validaciones

| ID | Cliente (vista, action-condition / action-validate) | Servidor (servicio) |
|----|------------------------------------------------------|---------------------|
| V-001 centro obligatorio | sí (check sobre `centro`) | sí (`validateInsert`) |
| V-002 email obligatorio | sí (check sobre `email`) | sí (`validateInsert`) |
| V-003 email formato (A6) | sí (regex aproximada en `if`) | sí (`validateInsert`, regex RFC 5322 simplificada) |
| V-004 DNI/NIE formato (A5) | sí (regex aproximada en `if`) | sí (`validateInsert`, `DniUtil.isValid`) |
| V-005 asunto obligatorio | sí (check sobre `asunto`) | sí (`validateInsert`) |
| V-006 al menos uno cuerpo | sí (`if` cruzado htmlBody+textBody) | sí (`validateInsert`) |
| V-007 transición FALLIDO → reenvío | sí (`action-validate` cliente + `showIf` en botón) | sí (`reenviarCorreo` lanza `BusinessException`) |
| V-008 coherencia centro/expediente (A7) | no | sí (`validateInsert`) |
| V-009 inmutabilidad post-creación | UI: form Main solo lectura | sí (`validateUpdate`) |

> Aunque la UI propia del subsistema no expone form de creación al usuario final (los correos se crean programáticamente vía `CorreoService.enviarCorreo` desde otros subsistemas), las validaciones de cliente quedan declaradas para consistencia con el patrón del proyecto. La red de seguridad real está en el servidor.

### D5 — Comportamiento ante fallo SMTP (I-1, I-2)

La captura de la excepción se hace en un helper privado del servicio. Si `MailSender.send` lanza, el correo queda con `estado=FALLIDO` y `mensajeError = mensaje humano de la excepción` (sin stacktrace). El correo se persiste igualmente (sin rollback). Si un `MetaFile` adjunto fue borrado entre creación y reenvío (A9), `MetaFileUtil.downloadContent` lanzará y caerá en la misma rama de fallo.

### D6 — Sin módulo Guice ni listener JPA

`ModelServiceFactory` descubre `CorreoServiceImpl` por convención (`service.impl.*ServiceImpl`). `MailSender` ya está bindado en `SecretariaVirtualModule`. No hay listeners JPA con lógica de negocio.

---

## Ficheros a crear o modificar

| # | Fichero | Acción | Skill | Descripción |
|---|---------|--------|-------|-------------|
| 1 | `src/main/java/com/educaflow/subsystem/correos/domains/Correo.xml` | Crear | k-sistemas | Entidad `Correo` con todos sus campos, enum `EstadoCorreo`, finder por DNI. |
| 2 | `src/main/java/com/educaflow/subsystem/correos/db/repo/CorreoRepository.java` | Crear | k-sistemas | Repositorio personalizado con `findUserByDni`. |
| 3 | `src/main/java/com/educaflow/subsystem/correos/service/CorreoEnviarDTO.java` | Crear | k-sistemas | Record DTO con los argumentos de `enviarCorreo`. |
| 4 | `src/main/java/com/educaflow/subsystem/correos/service/CorreoService.java` | Crear | k-sistemas | Interfaz `extends ModelService<Correo>`. |
| 5 | `src/main/java/com/educaflow/subsystem/correos/service/impl/CorreoServiceImpl.java` | Crear | k-sistemas | Implementación con validaciones + `enviarCorreo` + `reenviarCorreo`. |
| 6 | `src/main/java/com/educaflow/subsystem/correos/controller/CorreoController.java` | Crear | k-sistemas | Métodos `validateSave` y `reenviar` para el form Main. |
| 7 | `src/main/java/com/educaflow/subsystem/correos/views/Correo.xml` | Crear | k-vistas | Action-views, grids, forms, action-groups, validaciones cliente, action-methods. |
| 8 | `src/main/resources/data-init/input/auth-correos.xml` | Modificar | k-seguridad | Permisos `Correo.all`, `Correo.centro`, `Correo.propio`. |
| 9 | `src/main/resources/data-init/input/auth.xml` | Modificar | k-seguridad | Wirings: `admins`→`Correo.all`, `center-admins`→`Correo.centro`, `users`→`Correo.propio`. |

> **No tocar** `src/main/resources/data-init/input-config.xml` (el `<input file="auth-correos.xml">` ya existe).
> **No tocar** `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` (los `menuitem` ya apuntan a las action-views correctas).
> **Nunca crear** `i18n_es.csv` / `i18n_ca.csv`: se generan automáticamente.

---

## Pasos

### Paso 1 — Crear el modelo de dominio `Correo.xml`

**Skill:** k-sistemas (modelos.md).
**Ruta:** `src/main/java/com/educaflow/subsystem/correos/domains/Correo.xml`

XML completo (única excepción a la regla "sin código"):

```xml
<?xml version="1.0"?>
<domain-models xmlns="http://axelor.com/xml/ns/domain-models"
               xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
               xsi:schemaLocation="http://axelor.com/xml/ns/domain-models https://axelor.com/xml/ns/domain-models/domain-models_8.1.xsd">

    <module name="correos" package="com.educaflow.subsystem.correos.db"/>

    <entity name="Correo" repository="abstract">
        <many-to-one name="centro"     ref="com.educaflow.subsystem.common.db.Centro"          required="true" title="Centro"/>
        <many-to-one name="expediente" ref="com.educaflow.subsystem.expedientes.db.Expediente" title="Expediente"/>

        <string      name="email"               required="true" max="255" title="Email destinatario"/>
        <string      name="dniDestinatario"                     max="16"  title="DNI/NIE destinatario"/>
        <many-to-one name="usuarioDestinatario" ref="com.axelor.auth.db.User"                  title="Usuario destinatario"/>

        <string name="asunto"   required="true" max="255"             title="Asunto"/>
        <string name="htmlBody" large="true"    multiline="true"      title="Cuerpo HTML"/>
        <string name="textBody" large="true"    multiline="true"      title="Cuerpo texto plano"/>

        <one-to-many name="adjuntos" ref="com.axelor.meta.db.MetaFile" title="Adjuntos"/>

        <enum     name="estado"             ref="EstadoCorreo" required="true" title="Estado"/>
        <datetime name="fechaUltimoIntento"                    required="true" title="Fecha del último intento"/>
        <integer  name="numIntentos"        required="true" default="1"        title="Nº intentos"/>
        <string   name="mensajeError"       large="true" multiline="true"      title="Mensaje de error"/>

        <finder-method name="findByDniDestinatario" using="dniDestinatario" all="true"/>
    </entity>

    <enum name="EstadoCorreo">
        <item name="ENVIADO" title="Enviado"/>
        <item name="FALLIDO" title="Fallido"/>
    </enum>

</domain-models>
```

Notas:
- `repository="abstract"` permite extender con `CorreoRepository` en el paso 2.
- `default="1"` en `numIntentos` cubre I-4 también para inserción manual.
- Identidad por `id` autogenerado (I-4: sin numerador de negocio).

**Verificación:**
- `./gradlew compileJava` compila.
- Existen `build/src-gen/.../correos/db/{Correo,EstadoCorreo,AbstractCorreoRepository}.java`.

---

### Paso 2 — Crear el repositorio personalizado `CorreoRepository`

**Skill:** k-sistemas.
**Clase:** `com.educaflow.subsystem.correos.db.repo.CorreoRepository` (`extends AbstractCorreoRepository`)

Firmas y comentarios:

```java
public CorreoRepository();
//   Constructor por defecto. Llama a super(). Sin dependencias.

public com.axelor.auth.db.User findUserByDni(String dni);
//   I-3: encapsula la única consulta JPA del subsistema (búsqueda de User por DNI)
//   para que el servicio nunca escriba .filter().bind().fetchOne() inline.
//   Reglas:
//     - Si dni es null o blank devuelve null sin consultar.
//     - En otro caso ejecuta el equivalente a:
//         JpaRepository.of(User.class).all().filter("self.dni = :dni").bind("dni", dni).fetchOne()
//       y devuelve el resultado (o null si no hay coincidencia).
//   Usado por CorreoServiceImpl.enviarCorreo (A1: sólo se calcula al crear).
```

**Verificación:**
- Compila.
- `AbstractCorreoRepository` se ha generado a partir del dominio del paso 1.

---

### Paso 3 — Crear el DTO `CorreoEnviarDTO`

**Skill:** k-sistemas.
**Clase:** `com.educaflow.subsystem.correos.service.CorreoEnviarDTO` (record)

Componentes del record (en este orden, sin lógica adicional):

| Tipo | Campo |
|------|-------|
| `com.educaflow.subsystem.common.db.Centro` | `centro` |
| `com.educaflow.subsystem.expedientes.db.Expediente` | `expediente` (opcional) |
| `String` | `email` |
| `String` | `dniDestinatario` (opcional) |
| `String` | `asunto` |
| `String` | `htmlBody` (opcional) |
| `String` | `textBody` (opcional) |
| `java.util.List<com.axelor.meta.db.MetaFile>` | `adjuntos` (opcional) |

```java
// DTO de entrada de CorreoService.enviarCorreo. Sin métodos personalizados.
// La conversión a entidad Correo se hace en CorreoServiceImpl.enviarCorreo.
```

**Verificación:** compila.

---

### Paso 4 — Crear la interfaz `CorreoService`

**Skill:** k-sistemas (servicios.md).
**Clase:** `com.educaflow.subsystem.correos.service.CorreoService` (interface, `extends ModelService<Correo>`)

Firmas y comentarios:

```java
public Optional<BusinessMessages> validateInsert(Correo correo);
//   Hook estándar de ModelService. La implementación aplica V-001..V-006 + V-008.
//   Devuelve Optional.empty() si no hay errores; Optional.of(messages) en otro caso.

public Optional<BusinessMessages> validateUpdate(Correo correo, Correo original);
//   Hook estándar de ModelService. La implementación aplica V-009 (inmutabilidad)
//   sobre los campos de entrada (centro, expediente, email, dniDestinatario,
//   usuarioDestinatario, asunto, htmlBody, textBody, adjuntos). NO compara
//   estado/fechaUltimoIntento/numIntentos/mensajeError porque cambian legítimamente
//   en reenviarCorreo.

public Optional<BusinessMessages> validateRemove(Correo correo);
//   Hook estándar. Devuelve Optional.empty(); el control de borrado lo da auth.xml
//   (sólo `admins` tiene remove=true).

public Correo enviarCorreo(CorreoEnviarDTO dto) throws BusinessException;
//   Operación de negocio principal. Aplica V-001..V-006 + V-008 sobre los argumentos
//   antes de tocar SMTP. Resuelve usuarioDestinatario por DNI (A1).
//   NO hace rollback si SMTP falla (I-1): captura la excepción, marca el Correo
//   como FALLIDO con mensajeError humano (I-2) y persiste igual.

public Correo reenviarCorreo(Long correoId) throws BusinessException;
//   Reintenta el envío de un correo previamente FALLIDO. Aplica V-007 antes de
//   tocar SMTP. Incrementa numIntentos y actualiza fechaUltimoIntento. NO recalcula
//   usuarioDestinatario (A1). Sin rollback si vuelve a fallar (I-1, I-2).
```

**Verificación:** compila.

---

### Paso 5 — Crear la implementación `CorreoServiceImpl`

**Skill:** k-sistemas (servicios.md, validaciones.md).
**Clase:** `com.educaflow.subsystem.correos.service.impl.CorreoServiceImpl` (`extends DefaultModelService<Correo> implements CorreoService`).

Inyecciones (campos `@Inject` según patrón del proyecto):
- `MailSender mailSender` — binding ya existe en `SecretariaVirtualModule`.

Constantes privadas:
- `EMAIL_PATTERN` — `Pattern` con regex RFC 5322 simplificada (A6); se usa en V-003.

Firmas y comentarios:

```java
public CorreoServiceImpl(Class<Correo> model, Repository<Correo> repository);
//   Constructor obligatorio del patrón ModelService. Llama a super(model, repository).
//   ModelServiceFactory autodescubre esta clase por convención `service.impl.*ServiceImpl`;
//   NUNCA registrar en módulo Guice (k-sistemas).

public Optional<BusinessMessages> validateInsert(Correo correo);
//   Acumula BusinessMessages locales y aplica las siguientes reglas (cada mensaje en el
//   campo correspondiente, redactado para el usuario final transmitiendo el campo y, cuando
//   procede, el valor recibido):
//     - V-001 (centro obligatorio): si correo.getCentro() == null, mensaje contra `centro`
//       que transmite la obligatoriedad.
//     - V-002 (email obligatorio): si email null o blank, mensaje contra `email`.
//     - V-003 (email formato A6): si email no vacío y NO casa con EMAIL_PATTERN, mensaje
//       contra `email` que transmite el valor recibido y que el formato no es válido.
//     - V-004 (DNI/NIE A5): si dniDestinatario no vacío y DniUtil.isValid(dni)==false,
//       mensaje contra `dniDestinatario` que transmite el valor recibido y que falla
//       el formato/dígito de control.
//     - V-005 (asunto obligatorio): si asunto null o blank, mensaje contra `asunto`.
//     - V-006 (cuerpo cruzado): si htmlBody y textBody están ambos vacíos/null, mensaje
//       contra `htmlBody` que transmite que se debe aportar al menos un cuerpo
//       (HTML o texto plano).
//     - V-008 (coherencia centro/expediente A7): si expediente!=null, centro!=null y los
//       ids de centro divergen (expediente.centro.id != centro.id), mensaje contra
//       `expediente` que transmite la identificación del expediente y la incoherencia.
//   Devuelve Optional.empty() si los mensajes acumulados quedan vacíos.

public Optional<BusinessMessages> validateUpdate(Correo correo, Correo original);
//   Aplica V-009 (inmutabilidad post-creación). Para cada campo de entrada del modelo
//   (centro, expediente, email, dniDestinatario, usuarioDestinatario, asunto, htmlBody,
//   textBody, adjuntos) compara correo vs original (Objects.equals para escalares,
//   comparación por id para entidades, comparación por conjunto de ids para la lista de
//   adjuntos). Por cada campo que ha cambiado, añade un mensaje contra ese campo que
//   transmite el nombre del campo modificado y que los correos no son editables tras
//   su registro.
//   Los campos de servicio (estado, fechaUltimoIntento, numIntentos, mensajeError) NO se
//   comprueban: cambian legítimamente en reenviarCorreo.

public Optional<BusinessMessages> validateRemove(Correo correo);
//   Devuelve Optional.empty(). El control de borrado se delega a auth.xml.

public Correo enviarCorreo(CorreoEnviarDTO dto) throws BusinessException;
//   Pasos:
//     1. Construir un Correo nuevo a partir del DTO (centro, expediente, email,
//        dniDestinatario, asunto, htmlBody, textBody, adjuntos copiados a una lista nueva).
//     2. Llamar validateInsert(correo). Si devuelve Optional presente, lanzar
//        BusinessException con esos mensajes (sin tocar SMTP, sin persistir).
//     3. Resolver usuarioDestinatario llamando ((CorreoRepository) repository)
//        .findUserByDni(dto.dniDestinatario()) (I-3 + A1). Si null o no encontrado, queda null.
//     4. correo.setNumIntentos(1). correo.setFechaUltimoIntento(LocalDateTime.now()).
//     5. Invocar el helper privado intentarEnvio(correo) — captura cualquier Throwable
//        (I-1: nunca propaga ni hace rollback) y asigna estado/mensajeError humano (I-2).
//     6. Persistir con super.insert(correo) (no usar repository.save directamente;
//        regla del proyecto). Devolver el correo persistido.

public Correo reenviarCorreo(Long correoId) throws BusinessException;
//   Pasos:
//     1. Cargar correo = repository.find(correoId). Si null, lanzar BusinessException
//        cuyo mensaje contra `id` transmite el id no encontrado.
//     2. V-007: si correo.getEstado() != EstadoCorreo.FALLIDO, lanzar BusinessException
//        cuyo mensaje contra `estado` transmite el estado actual del correo y que sólo
//        FALLIDO se puede reenviar.
//     3. Crear snapshot del original ANTES de mutar (clonarSnapshot) para super.update.
//     4. correo.setNumIntentos(numIntentos+1) (defendido contra null=0).
//        correo.setFechaUltimoIntento(LocalDateTime.now()).
//     5. invocar intentarEnvio(correo) — mismo contrato de I-1/I-2 que en enviarCorreo.
//     6. NO recalcular usuarioDestinatario (A1).
//     7. Devolver super.update(correo, original).

private void intentarEnvio(Correo correo);
//   Helper privado que centraliza el efecto secundario de envío SMTP. Pasos:
//     1. Leer from = AppSettings.get().get("mail.smtp.from") (A2 + A8: leído cada vez,
//        no se persiste en el modelo).
//     2. Convertir cada MetaFile de correo.getAdjuntos() a Attach mediante
//        MetaFileUtil.downloadContent(metaFile) (A9: en memoria; si el MetaFile fue
//        borrado, la descarga lanzará y caerá en el catch como fallo de envío).
//     3. Construir Mail (to=[email], from, subject=asunto, htmlBody, textBody, attachs).
//     4. try { mailSender.send(mail); correo.setEstado(ENVIADO);
//              correo.setMensajeError(null); }
//     5. catch (Throwable t) { correo.setEstado(FALLIDO);
//              correo.setMensajeError(extraerMensaje(t)); }
//   Nunca propaga (I-1) ni produce stacktrace en el mensaje (I-2).

private static String extraerMensaje(Throwable t);
//   I-2: devuelve t.getMessage() si no es null/blank; en otro caso, t.getClass()
//   .getSimpleName(). Nunca incluye stacktrace.

private Correo clonarSnapshot(Correo c);
//   Construye un Correo nuevo copiando todos los campos para usarlo como `original` en
//   super.update. La lista de adjuntos se copia a una lista nueva.

private void comprobarInmutable(BusinessMessages messages, String campo, Object antes, Object ahora);
//   Helper de V-009: si !Objects.equals(antes, ahora), añade un BusinessMessage contra
//   `campo` que transmite el nombre del campo modificado.

private static Long idOf(com.axelor.db.Model m);
//   Devuelve m == null ? null : m.getId(). Usado por comprobarInmutable para
//   relaciones (centro, expediente, usuarioDestinatario).

private static boolean sameMetaFiles(java.util.List<com.axelor.meta.db.MetaFile> a,
                                     java.util.List<com.axelor.meta.db.MetaFile> b);
//   Compara dos listas de MetaFile por conjunto de ids ordenados. Usado por
//   validateUpdate para detectar cambios de adjuntos en V-009.
```

Reglas implícitas que debe respetar la implementación:
- NO crear módulo Guice para `CorreoService` (`ModelServiceFactory` lo descubre).
- NO escribir consultas JPA inline; usar `CorreoRepository` (I-3).
- NO crear listeners JPA con lógica de negocio.
- Las validaciones devuelven `Optional<BusinessMessages>`, NO lanzan (excepto los métodos `enviarCorreo`/`reenviarCorreo` que sí lanzan `BusinessException` cuando proceden).
- Los métodos `enviarCorreo`/`reenviarCorreo` NUNCA hacen rollback ante un fallo SMTP (I-1).

**Verificación:**
- `./gradlew compileJava` compila sin errores.
- En arranque, `ModelServiceFactory.resolve(Correo.class)` devuelve esta impl (verificable revisando logs de descubrimiento).

---

### Paso 6 — Crear el controlador `CorreoController`

**Skill:** k-sistemas (controladores.md).
**Clase:** `com.educaflow.subsystem.correos.controller.CorreoController`

Inyecciones (campo `@Inject`):
- `ModelServiceFactory modelServiceFactory`.

Firmas y comentarios:

```java
@CallMethod
public void validateSave(ActionRequest actionRequest, ActionResponse actionResponse);
//   Disparado por la action-method "subsysCorreos.Correo@Main-Remote-validateSave-action".
//   Patrón del proyecto:
//     1. Resolver CorreoService = (CorreoService) modelServiceFactory.resolve(Correo.class).
//     2. Construir ActionRequestHelper<Correo> y ActionResponseHelper sobre el request/response.
//     3. Leer el modelo del request con AllowProperties.createAllowAllProperties().
//     4. Si requestHelper.getId() == null: result = service.validateInsert(correo).
//        En otro caso: result = service.validateUpdate(correo, requestHelper.getOriginalModel()).
//     5. Si result.isPresent(): responseHelper.doResponseBusinessMessagesAsError(result.get()).
//   Cubre en servidor V-001..V-006 + V-008 (insert) y V-009 (update).
//   Aunque el form Main es solo lectura, el endpoint queda definido para coherencia con
//   el patrón del proyecto y para soportar futuras forms de creación.

@CallMethod
@Transactional
public void reenviar(ActionRequest actionRequest, ActionResponse actionResponse);
//   Disparado por la action-method "subsysCorreos.Correo@Main-Remote-reenviar-action".
//   Pasos:
//     1. Resolver CorreoService.
//     2. id = ActionRequestHelper.getId(); si null, retornar sin acción.
//     3. try { service.reenviarCorreo(id); actionResponse.setReload(true); }
//        catch (BusinessException ex) {
//            responseHelper.doResponseBusinessMessagesAsError(ex.getBusinessMessages());
//        }
//   El mensaje de V-007 lo construye el servicio y lo propaga la BusinessException;
//   el cliente lo recibe como diálogo de error.
//   La anotación @Transactional garantiza la actualización en BD del Correo
//   (super.update en el servicio); I-1 ya está respetado dentro del propio servicio.
```

**Verificación:** `./gradlew compileJava` compila.

---

### Paso 7 — Crear las vistas `Correo.xml`

**Skill:** k-vistas (forms.md, grids.md, actions.md).
**Ruta:** `src/main/java/com/educaflow/subsystem/correos/views/Correo.xml`
**Namespace:** `object-views` Axelor 8.1.

Cabecera del fichero con bloque de comentarios de 3 líneas asteriscos: `Correo : Vistas`.

#### Action-views

- **`subsysCorreos.Correo@All-action`** (admins, sin filtro):
  - title `Todos los correos`. model `com.educaflow.subsystem.correos.db.Correo`.
  - Vistas: `subsysCorreos.Correo@Main-grid` + `subsysCorreos.Correo@Main-form`.
  - view-params: `show-toolbar-grid=false`, `show-toolbar-form=false`, `reload-grid=true`.
  - Sin domain (admin ve todos los centros).

- **`subsysCorreos.Correo@Centro-action`** (center-admins, su centro):
  - title `Correos del centro`. mismas vistas y view-params que `@All-action`.
  - domain: `self.centro = :__user__centroActivo` con `<context name="__user__centroActivo" expr="eval:__user__.centroActivo"/>` (filtro UI por el centro activo del Supervisor). Nombre de parámetro plano (sin punto): Hibernate no admite `:__user__.centroActivo`.

- **`subsysCorreos.Correo@Propios-action`** (users, su DNI):
  - title `Mis correos`. Vistas: `subsysCorreos.Correo@CarpetaCiudadana-grid` + `@CarpetaCiudadana-form`.
  - mismos view-params.
  - domain: `self.dniDestinatario = :__user__dni` con `<context name="__user__dni" expr="eval:__user__.dni"/>` (A3).

#### Grids

- **`subsysCorreos.Correo@Main-grid`** (admin/supervisor):
  - model: Correo. Title: `Correos`.
  - flags: `editable=false`, `edit-icon=false`, `x-selector=none`, `canNew=false`, `canEdit=false`, `canDelete=false`, `canSave=false`, `canViewOnClick=true`, `allowSearchFields=true`.
  - `orderBy="-fechaUltimoIntento"`.
  - Columnas (en orden): `fechaUltimoIntento`, `email`, `dniDestinatario`, `asunto`, `estado`, `centro`, `expediente`, `numIntentos`.

- **`subsysCorreos.Correo@CarpetaCiudadana-grid`** (users):
  - model: Correo. Title: `Mis correos`.
  - mismos flags que Main-grid; `orderBy="-fechaUltimoIntento"`.
  - Columnas: `fechaUltimoIntento`, `email`, `asunto`, `estado`, `expediente` (sin `dniDestinatario`, `centro`, `numIntentos`).

#### Forms

- **`subsysCorreos.Correo@Main-form`** (admin/supervisor):
  - model: Correo. width: large.
  - flags: `canAttach=false`, `canBack=false`, `canDelete=false`, `canNew=false`, `canSave=false`, `canMore=false`, `canBackOnSave=true`.
  - `onSave="subsysCorreos.Correo@Main-onSave-action"`.
  - Panels (todos `readonly=true`):
    - **panelDestinatario** (title `Destinatario`): `email`, `dniDestinatario`, `usuarioDestinatario`.
    - **panelContenido** (title `Contenido`): `asunto`; `htmlBody` con `widget=html` y `showIf="htmlBody != null"`; `textBody` con `widget=Text` y `showIf="textBody != null"`; `adjuntos`.
    - **panelContexto** (title `Contexto`): `centro`, `expediente`.
    - **panelEnvio** (title `Envío`): `estado`, `fechaUltimoIntento`, `numIntentos`; `mensajeError` con `widget=Text` y `showIf="mensajeError != null"`.
    - **panelBotones** (`showFrame=false`):
      - `btnVolver`: title `Salir`, `onClick="back"`, css `btn-secondary`, outline=true.
      - `btnReenviar`: title `Reenviar`, `onClick="subsysCorreos.Correo@Main-btnReenviar-action"`, css `btn-primary`, `showIf="estado == 'FALLIDO' && id != null"`.

- **`subsysCorreos.Correo@CarpetaCiudadana-form`** (users):
  - model: Correo. width: large. Mismas flags que Main-form pero sin `onSave`.
  - Panels (todos `readonly=true`):
    - **panelDestinatario**: `email`, `dniDestinatario` (sin `usuarioDestinatario`).
    - **panelContenido**: `asunto`, `htmlBody` (showIf), `textBody` (showIf), `adjuntos`.
    - **panelEnvio**: `estado`, `fechaUltimoIntento` (sin `numIntentos` ni `mensajeError`).
    - **panelBotones**: solo `btnVolver` → `onClick="back"`. Sin `btnReenviar`.

#### Acciones

Agrupadas según convención de comentarios de `k-vistas` en este orden: tareas principales → validaciones locales → cambios campos simples (no aplica) → llamadas remotas.

**Acciones de las tareas principales:**

- **`action-group subsysCorreos.Correo@Main-onSave-action`**
  Propósito: pipeline al guardar en el form Main. Encadena, en orden:
  1. `subsysCorreos.Correo@Main-Local-validateSave-action`
  2. `subsysCorreos.Correo@Main-Remote-validateSave-action`
  3. `save` (paso estándar Axelor).
  Aunque el form actual es de solo lectura, la action-group queda declarada por consistencia con el patrón.

- **`action-group subsysCorreos.Correo@Main-btnReenviar-action`**
  Propósito: pipeline del botón `Reenviar`. Encadena, en orden:
  1. `subsysCorreos.Correo@Main-Local-validateReenviar-action`
  2. `subsysCorreos.Correo@Main-Remote-reenviar-action`.

**Acciones de Validaciones en local:**

- **`action-condition subsysCorreos.Correo@Main-Local-validateSave-action`**
  Propósito: V-001..V-006 en cliente para feedback inmediato. Cada `<check>` con su mensaje propio que transmite campo y, donde aplica, valor recibido y regla violada.
  - V-001: campo `centro` obligatorio.
  - V-002: campo `email` obligatorio.
  - V-003 (A6): `email` casa una regex RFC 5322 simplificada (mensaje transmite que el formato es inválido). Si el campo no está vacío.
  - V-004 (A5): `dniDestinatario`, si presente, casa una regex aproximada de DNI/NIE (el servidor refuerza con `DniUtil.isValid`). Mensaje transmite que el formato es inválido.
  - V-005: campo `asunto` obligatorio.
  - V-006: si `htmlBody` y `textBody` están ambos vacíos. Mensaje transmite que se debe aportar al menos un cuerpo.

- **`action-validate subsysCorreos.Correo@Main-Local-validateReenviar-action`**
  Propósito: V-007 en cliente. `<error>` con `if="estado != 'FALLIDO'"`. Mensaje transmite el estado actual y que sólo correos `FALLIDO` se pueden reenviar.

**Acciones básicas que cambian campos simples:** no aplica (no hay campos derivados desde la UI; estado/fechaUltimoIntento/numIntentos/mensajeError los asigna el servicio).

**Acciones de llamadas Remotas al servidor:**

- **`action-method subsysCorreos.Correo@Main-Remote-validateSave-action`**
  - model: Correo.
  - call: clase `com.educaflow.subsystem.correos.controller.CorreoController`, método `validateSave`.
  - Cubre en servidor V-001..V-006 + V-008 (insert) y V-009 (update).

- **`action-method subsysCorreos.Correo@Main-Remote-reenviar-action`**
  - model: Correo.
  - call: clase `com.educaflow.subsystem.correos.controller.CorreoController`, método `reenviar`.
  - Tras éxito el controlador ejecuta `actionResponse.setReload(true)`. V-007 servidor lo propaga el servicio.

**Verificación:**
- `./gradlew clean build --info` carga las vistas sin errores.
- Tras arrancar la app: los menús abren las grids y los formularios; el botón Reenviar sólo aparece en correos FALLIDO.

---

### Paso 8 — Modificar `auth-correos.xml`

**Skill:** k-seguridad.
**Ruta:** `src/main/resources/data-init/input/auth-correos.xml`

El fichero ya existe (binding declarado en `input-config.xml`). Reescribirlo para que contenga exactamente tres permisos sobre `com.educaflow.subsystem.correos.db.Correo`:

- **`Correo.all`** (Administrador):
  - sin `condition` ni `conditionParams`.
  - `<can create=true read=true write=true remove=true export=true/>`.
  - Justificación: el administrador puede ver, crear (alta administrativa puntual), editar, borrar y reenviar correos de cualquier centro.

- **`Correo.centro`** (Supervisor / center-admin):
  - `condition="self.centro = ?"`, `conditionParams="__user__.centroActivo"`.
  - `<can create=false read=true write=true remove=false export=false/>`.
  - Justificación: lee y reenvía correos de su centro. `write=true` es necesario porque `reenviarCorreo` realiza un `update` (actualiza estado, numIntentos, fechaUltimoIntento, mensajeError). NO crea (la creación es siempre programática vía `CorreoService.enviarCorreo`). NO borra.

- **`Correo.propio`** (Profesor/Exprofesor/Alumno/Exalumno):
  - `condition="self.dniDestinatario = ?"`, `conditionParams="__user__.dni"` (A3).
  - `<can create=false read=true write=false remove=false export=false/>`.
  - Justificación: lectura sólo de los correos cuyo DNI destinatario coincide con el del usuario en sesión, vía Carpeta Ciudadana.

> Externos y Familiares (A4): no se les asigna ninguna de las tres permissions, quedan sin acceso.

**Verificación:**
- El XML es válido frente al schema de seguridad.
- `grep -n "name=\"Correo\\." src/main/resources/data-init/input/auth-correos.xml` muestra exclusivamente las tres declaraciones `<permission ...>` (`Correo.all`, `Correo.centro`, `Correo.propio`).

---

### Paso 9 — Modificar `auth.xml` (wirings)

**Skill:** k-seguridad.
**Ruta:** `src/main/resources/data-init/input/auth.xml`

Cambios:

1. En la sección `<!-- Correos -->` añadir un comentario informativo: *"Permisos `Correo.all`, `Correo.centro` y `Correo.propio` definidos en `auth-correos.xml`"*. NO declarar permisos en este fichero (la declaración canónica vive en `auth-correos.xml`).

2. Añadir/asegurar los wirings en los grupos existentes:
   - `<group code="admins">` → `<permission name="Correo.all"/>`.
   - `<group code="center-admins">` → `<permission name="Correo.centro"/>`.
   - `<group code="users">` → `<permission name="Correo.propio"/>` (este grupo agrupa Profesor, Exprofesor, Alumno, Exalumno).

3. Externos y Familiares: ningún wiring. Si en el futuro se introducen como grupos separados, simplemente no se añade `<permission name="Correo.*"/>` en sus bloques.

**Verificación:**
- `grep -n "Correo\\." src/main/resources/data-init/input/auth.xml` muestra exactamente las tres referencias (una por grupo) y ninguna declaración `<permission name="Correo.*" object=...>` (esta vive en `auth-correos.xml`).
- Tras arrancar la app, no aparecen errores de carga de permisos.

---

### Paso 10 — Datos iniciales

No se requieren registros pre-cargados:
- No hay catálogos del subsistema.
- No hay numerador de negocio (I-4).
- Las propiedades SMTP (A8) viven en `application.properties` / `axelor-config.properties` y son configuración de entorno, no data-init.

Paso vacío explícito para que el implementador no busque qué cargar.

---

### Paso 11 — Verificación final

Ejecutar exactamente:

```
./gradlew clean build --info
```

Resultado esperado: `BUILD SUCCESSFUL`.

Verificaciones adicionales:
1. Ficheros generados:
   - `build/src-gen/com/educaflow/subsystem/correos/db/Correo.java`
   - `build/src-gen/com/educaflow/subsystem/correos/db/EstadoCorreo.java`
   - `build/src-gen/com/educaflow/subsystem/correos/db/repo/AbstractCorreoRepository.java`
2. `ModelServiceFactory` descubre `CorreoServiceImpl` por convención (sin módulo Guice).
3. Las vistas validan contra `object-views_8.1.xsd`.
4. Los wirings de `auth.xml` cargan sin errores en arranque.

Comando de arranque manual (lanzado por el usuario, no por la verificación automatizada):

```
./gradlew --no-daemon run --debug-jvm --port 8080 --context-path /
```

Pruebas manuales tras arrancar:
- **Admin** → menú `Correos → Todos los correos` muestra todos los correos sin filtrar.
- **Supervisor** (`center-admins`) → menú `Correos del centro` filtra por `centroActivo`. No puede crear ni borrar.
- **Usuario** (`users`) → menú `Mis correos` solo muestra los correos donde `dniDestinatario = User.dni`. Sin botón Reenviar.
- Abrir un correo en estado `FALLIDO` desde el form Main → aparece botón **Reenviar**; al pulsar, recarga con `numIntentos` incrementado y `estado` actualizado según resultado SMTP.
- Abrir un correo en estado `ENVIADO` → no aparece botón.
- Forzar reenvío vía API contra un correo `ENVIADO` → la respuesta es V-007 como diálogo de error.
- Llamar `enviarCorreo` con SMTP caído (host inválido) → el `Correo` se persiste con `estado=FALLIDO`, `mensajeError` no nulo y SIN rollback (I-1).
- Llamar `enviarCorreo` con email inválido → lanza `BusinessException` con V-003 antes de tocar SMTP.

---

## Trazabilidad V-XXX → ubicación

### Validaciones (V-001..V-009)

| ID | Capa cliente (vista + acción) | Capa servidor (clase.método) |
|----|-------------------------------|--------------------------------|
| V-001 (centro obligatorio) | `views/Correo.xml` → `subsysCorreos.Correo@Main-Local-validateSave-action` (`<check field="centro">`) | `CorreoServiceImpl.validateInsert` |
| V-002 (email obligatorio) | mismo `action-condition` (`<check field="email">`) | `CorreoServiceImpl.validateInsert` |
| V-003 (email formato A6) | mismo `action-condition` (regex en `if`) | `CorreoServiceImpl.validateInsert` (`EMAIL_PATTERN`) |
| V-004 (DNI/NIE A5) | mismo `action-condition` (regex aproximada en `if`) | `CorreoServiceImpl.validateInsert` (`DniUtil.isValid`) |
| V-005 (asunto obligatorio) | mismo `action-condition` (`<check field="asunto">`) | `CorreoServiceImpl.validateInsert` |
| V-006 (al menos un cuerpo) | mismo `action-condition` (`<check field="htmlBody" if="...">` cruzado) | `CorreoServiceImpl.validateInsert` |
| V-007 (transición de estado) | `subsysCorreos.Correo@Main-Local-validateReenviar-action` + `showIf="estado=='FALLIDO' && id!=null"` en `btnReenviar` | `CorreoServiceImpl.reenviarCorreo` (lanza `BusinessException`) |
| V-008 (centro/expediente A7) | — (sólo servidor; integridad entre registros) | `CorreoServiceImpl.validateInsert` |
| V-009 (inmutabilidad) | UI: `@Main-form` con todos los panels `readonly=true` | `CorreoServiceImpl.validateUpdate` |

### Reglas de negocio, transiciones y campos calculados

| Regla | Ubicación |
|-------|-----------|
| Operación `enviarCorreo` (orquestación validación → resolución usuario → SMTP → persistencia) | `CorreoServiceImpl.enviarCorreo` |
| Operación `reenviarCorreo` (carga + V-007 + snapshot + incremento numIntentos + SMTP + persistencia) | `CorreoServiceImpl.reenviarCorreo` |
| Transición `(nuevo) → ENVIADO` (éxito SMTP en `enviarCorreo`) | `CorreoServiceImpl.intentarEnvio` (rama try) |
| Transición `(nuevo) → FALLIDO` (fallo SMTP en `enviarCorreo`) | `CorreoServiceImpl.intentarEnvio` (rama catch) |
| Transición `FALLIDO → ENVIADO` (éxito SMTP en `reenviarCorreo`) | `CorreoServiceImpl.intentarEnvio` (rama try) llamado desde `reenviarCorreo` |
| Transición `FALLIDO → FALLIDO` (fallo SMTP en `reenviarCorreo`) | `CorreoServiceImpl.intentarEnvio` (rama catch) llamado desde `reenviarCorreo` |
| Transición prohibida `ENVIADO → *` | `CorreoServiceImpl.reenviarCorreo` (V-007) |
| Campo calculado `usuarioDestinatario` por DNI sólo al crear (A1) | `CorreoServiceImpl.enviarCorreo` (paso 3) usando `CorreoRepository.findUserByDni` |
| Campos calculados `estado` / `mensajeError` | `CorreoServiceImpl.intentarEnvio` |
| Campos calculados `fechaUltimoIntento` / `numIntentos` | `CorreoServiceImpl.enviarCorreo` (numIntentos=1) y `CorreoServiceImpl.reenviarCorreo` (++) |
| `from` desde configuración SMTP (A2 + A8) | `CorreoServiceImpl.intentarEnvio` (`AppSettings.get().get("mail.smtp.from")`) |
| Conversión adjuntos a `Attach` (A9) | `CorreoServiceImpl.intentarEnvio` (`MetaFileUtil.downloadContent`) |

### Invariantes técnicas (I-1..I-5)

| Invariante | Ubicación |
|------------|-----------|
| I-1 (sin rollback ante fallo SMTP) | `CorreoServiceImpl.intentarEnvio` (catch `Throwable` sin re-throw) + `enviarCorreo`/`reenviarCorreo` (super.insert/update siempre se ejecuta) |
| I-2 (`mensajeError` humano sin stacktrace) | `CorreoServiceImpl.extraerMensaje` |
| I-3 (queries JPA en repositorio) | `CorreoRepository.findUserByDni` |
| I-4 (identidad por id BD, sin numerador) | `domains/Correo.xml` (sin campo de numeración) |
| I-5 (envío síncrono inmediato) | `CorreoServiceImpl.intentarEnvio` (llamada directa a `mailSender.send`, sin colas) |

### Seguridad y filtros UI

| Aspecto | Ubicación |
|---------|-----------|
| Acceso Administrador (todo, todos los centros) | `auth-correos.xml` (`Correo.all`) + wiring `admins` en `auth.xml` |
| Acceso Supervisor (lectura + reenvío de su centro) | `auth-correos.xml` (`Correo.centro` con condición JPQL) + wiring `center-admins` en `auth.xml` |
| Acceso usuarios finales (lectura propia por DNI, A3) | `auth-correos.xml` (`Correo.propio` con condición JPQL) + wiring `users` en `auth.xml` + domain en `@Propios-action` |
| Externo / Familiar sin acceso (A4) | `auth.xml` (ausencia de wiring) |
| Filtro centro activo en pantalla Supervisor | `views/Correo.xml` → domain de `subsysCorreos.Correo@Centro-action` |
| Filtro DNI en Carpeta Ciudadana (A3) | `views/Correo.xml` → domain de `subsysCorreos.Correo@Propios-action` con context `__user__dni` |

---

## Notas de unificación

- **Naming de action-views:** se eligió `@All-action`/`@Centro-action`/`@Propios-action` (en lugar de un único `@Main-action` con permisos) para no tocar `menus.xml` (que ya apunta a esos nombres). Los grids/forms internos siguen con `@Main-*` y `@CarpetaCiudadana-*` como en el análisis.
- **Permisos:** se mantiene la convención `Correo.{all|centro|propio}` (3 permisos), sin un permiso adicional `Correo.reenviar.centro`. `Correo.centro` con `write=true` es suficiente para que Supervisor pueda reenviar (la operación es un update).
- **Modelo `EstadoCorreo`:** se usa `<enum>` nativo Axelor 8.1 (no `<selection>`), coherente con el resto de subsistemas y con la mención literal del análisis (enum).
- **Repositorio personalizado:** `CorreoRepository` con `findUserByDni` para encapsular la única consulta JPA (I-3). Sin filter/bind inline en el servicio.
- **DTO de entrada:** record `CorreoEnviarDTO` que reúne los 8 argumentos de `enviarCorreo` para no exponer un método con tantos parámetros.
- **Helpers privados del servicio (`intentarEnvio`, `extraerMensaje`, `clonarSnapshot`, `comprobarInmutable`, `idOf`, `sameMetaFiles`):** se enuncian con firma + comentario para que el implementador sepa que existen y qué papel cumplen, pero su cuerpo lo escribe `sdd-implementer-system` siguiendo `k-sistemas/validaciones.md`.
- **Forms Main solo lectura:** todos los `panel` de `Main-form` y `CarpetaCiudadana-form` están con `readonly=true`. Las validaciones cliente quedan declaradas para coherencia con el patrón del proyecto y para un eventual form de creación administrativa futuro.
- **`i18n_es.csv` / `i18n_ca.csv`:** NO se crean en el diseño; los genera el script automáticamente (regla del proyecto en `CLAUDE.md`).
