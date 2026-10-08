---
type: design
template: system
---

# Diseño: Subsistema «Notificaciones»

**Objetivo:** Fusionar `subsystem/correos` y `subsystem/sms` en un subsistema `notificaciones` con una entidad base `Notificacion` (JOINED) de la que heredan `Correo` y `Sms`, un listado común polimórfico y un ciclo de envío compartido que admite canales nuevos sin tocar lo existente.
**Capa:** subsystem/notificaciones
**Especificación de origen:** .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/specification.md
**Skills necesarios para la implementación:** k-sistemas, k-code-quality, k-secure-coding, k-vistas, k-validaciones, k-datainit, k-guice (el diseño fusiona dos módulos Guice con `Provider`)

Las decisiones difíciles, con sus alternativas, están en [`decisiones.md`](decisiones.md) (D1–D9); este documento las cita por su número.

## Ficheros a crear o modificar

Rutas relativas a `src/main/java/com/educaflow/` salvo que empiecen por `src/`, `agent_docs/` o `.claude/`.

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/notificaciones/domains/Notificacion.xml` | Crear | k-sistemas (modelos.md) | Entidad base `Notificacion` (JOINED) + enums `TipoNotificacion` y `EstadoNotificacion` |
| `subsystem/notificaciones/domains/Correo.xml` | Crear | k-sistemas (modelos.md) | `Correo extends Notificacion` |
| `subsystem/notificaciones/domains/Sms.xml` | Crear | k-sistemas (modelos.md) | `Sms extends Notificacion` |
| `subsystem/notificaciones/domains/Adjunto.xml` | Crear | k-sistemas (modelos.md) | `Adjunto` (de un `Correo`) |
| `subsystem/notificaciones/util/GestorNotificacionesUtil.java` | Crear | k-code-quality, k-secure-coding | Único dueño de «gestor del centro» (D4): le preguntan el menú, el reenvío, el `<domain>` y los permisos (`@ScriptAllowed`) |
| `subsystem/notificaciones/service/NotificacionCanalService.java` | Crear | k-sistemas (servicios.md) | Interfaz genérica de las acciones comunes a todos los canales |
| `subsystem/notificaciones/service/NotificacionService.java` | Crear | k-sistemas (servicios.md) | Servicio de `Notificacion` con las factorías `createCorreo`/`createSms` |
| `subsystem/notificaciones/service/CorreoService.java` | Crear | k-sistemas (servicios.md) | Servicio del canal correo |
| `subsystem/notificaciones/service/SmsService.java` | Crear | k-sistemas (servicios.md) | Servicio del canal SMS |
| `subsystem/notificaciones/service/AdjuntoService.java` | Crear | k-sistemas (servicios.md) | Servicio de `Adjunto` (sin acciones propias) |
| `subsystem/notificaciones/service/impl/NotificacionCanalServiceImpl.java` | Crear | k-sistemas, k-secure-coding | Base abstracta del ciclo de vida de un canal (D1) |
| `subsystem/notificaciones/service/impl/NotificacionServiceImpl.java` | Crear | k-sistemas, k-secure-coding | Factorías + cierre de la puerta REST de `Notificacion` |
| `subsystem/notificaciones/service/impl/CorreoServiceImpl.java` | Crear | k-sistemas, k-validaciones, k-secure-coding | Canal correo (sustituye a `correos/service/impl/CorreoServiceImpl`) |
| `subsystem/notificaciones/service/impl/SmsServiceImpl.java` | Crear | k-sistemas, k-validaciones, k-secure-coding | Canal SMS (sustituye a `sms/service/impl/SmsServiceImpl`) |
| `subsystem/notificaciones/service/impl/AdjuntoServiceImpl.java` | Crear | k-sistemas, k-validaciones, k-secure-coding | Adjunto (sustituye a `correos/service/impl/AdjuntoServiceImpl`) |
| `subsystem/notificaciones/controller/NotificacionController.java` | Crear | k-sistemas (controladores.md) | Apertura polimórfica de forms, alta y `<domain>` de «Del centro» |
| `subsystem/notificaciones/controller/CorreoController.java` | Crear | k-sistemas (controladores.md) | Reenvío del correo (movido de `correos/controller`) |
| `subsystem/notificaciones/controller/SmsController.java` | Crear | k-sistemas (controladores.md) | Reenvío del SMS (movido de `sms/controller`) |
| `subsystem/notificaciones/module/NotificacionesModule.java` | Crear | k-guice | Fusiona `CorreosModule`, `MailSenderProvider`, `SmsModule` y `SmsSenderProvider` (los providers pasan a métodos `@Provides`; los ficheros antiguos se eliminan con `subsystem/correos/**` y `subsystem/sms/**`) |
| `subsystem/notificaciones/views/Main-Notificacion.xml` | Crear | k-vistas (grids.md, actions.md) | «Todas» |
| `subsystem/notificaciones/views/Centro-Notificacion.xml` | Crear | k-vistas (grids.md, actions.md) | «Del centro» |
| `subsystem/notificaciones/views/Mis-Notificacion.xml` | Crear | k-vistas (grids.md, actions.md) | «Recibidas» |
| `subsystem/notificaciones/views/Eleccion-Notificacion.xml` | Crear | k-vistas (forms.md, actions.md) | Elección de canal (popup) |
| `subsystem/notificaciones/views/Main-Correo.xml` | Crear | k-vistas (forms.md, actions.md) | Form del correo en «Todas» (alta y detalle) + modal de adjunto |
| `subsystem/notificaciones/views/Centro-Correo.xml` | Crear | k-vistas (forms.md) | Form del correo en «Del centro» |
| `subsystem/notificaciones/views/Mis-Correo.xml` | Crear | k-vistas (forms.md) | Form del correo en «Recibidas» |
| `subsystem/notificaciones/views/Main-Sms.xml` | Crear | k-vistas (forms.md, actions.md) | Form del SMS en «Todas» (alta y detalle) |
| `subsystem/notificaciones/views/Centro-Sms.xml` | Crear | k-vistas (forms.md) | Form del SMS en «Del centro» |
| `subsystem/notificaciones/views/Mis-Sms.xml` | Crear | k-vistas (forms.md) | Form del SMS en «Recibidas» |
| `subsystem/notificaciones/views/Ref-Adjunto.xml` | Crear | k-vistas (forms.md) | «Adjunto en consulta» |
| `subsystem/expedientes/views/Ref-HistorialEstado.xml` | Crear | k-vistas (SKILL.md «Vistas de referencia», grids.md, forms.md) | Fichero completo materializado en `design/views/Ref-HistorialEstado.xml` (se copia verbatim aquí, no a `subsystem/notificaciones/views/`): vistas de solo lectura `subsysExpedientes.Ref@HistorialEstado-grid`/`-form`, selector y consulta del «Estado del expediente». Excepción acotada a README §4.2 autorizada por el usuario (D8): no se toca nada más de `expedientes` (ni dominio, ni servicio, ni otras vistas) |
| `subsystem/notificaciones/data-init/input-config.xml` | Crear | k-datainit | Binding de los permisos del subsistema |
| `subsystem/notificaciones/data-init/input/auth-notificaciones.xml` | Crear | k-datainit, k-secure-coding | Permisos de `Notificacion`, `Correo`, `Sms` y `Adjunto` |
| `base/infrastructure/axelorhelper/ActionResponseHelper.java` | Modificar | k-sistemas (controladores.md) | + `doResponseViewFormEnPopup` (D3) |
| `base/infrastructure/controller/DefaultModelController.java` | Modificar | k-sistemas (controladores.md) | + `refreshTab` (D3) |
| `base/infrastructure/controller/DefaultModelController.xml` | Modificar | k-vistas (actions.md) | Fichero completo materializado en `design/views/DefaultModelController.xml` (se copia verbatim aquí, no a `subsystem/notificaciones/views/`): + acción global `remote-refreshTab-action` (D3) |
| `base/util/MetaFileUtil.java` | Modificar | k-code-quality | + `fileSizeOnDisk(MetaFile)` (lo usan adjunto y correo) |
| `secretariavirtual/menus/menus.xml` | Modificar | k-vistas (menus.md) | Quita `correos-*` y `sms-*`; añade `notificaciones-*` |
| `subsystem/security/service/impl/MenuSecurityServiceImpl.java` | Modificar | k-vistas (menus.md) | `case` de `notificaciones-delCentro-menuitem` en lugar de `correos-delCentro-menuitem` |
| `tramites/util/verificacion/VerificacionHelper.java` | Modificar | k-sistemas | Usa `NotificacionService.createCorreo()`, rellena el motivo y liga el estado actual del expediente |
| `tramites/util/CLAUDE.md` | Modificar | — | «Depende de `subsystem/correos`» → «Depende de `subsystem/notificaciones`» |
| `src/main/resources/data-init/input/auth.xml` | Modificar | k-datainit | Grupo `users`: los 4 permisos `Correo.*`/`Adjunto.*` → los 8 de notificaciones; quita el comentario huérfano `<!-- Correos -->` |
| `agent_docs/view-rules.md` | Modificar | — | Glosario «Acciones globales/predefinidas» + `remote-refreshTab-action`; `VAR-7.3`: la admite entre las globales; `VAR-7.2`: rama «form sin grid al que volver» también para el maestro con `btnSave`, con `remote-refreshTab-action` entre `save` y `close` (D3) |
| `src/test/java/com/educaflow/views/botones/Categoria7BotonesTest.java` | Modificar | developer-create-view-tests | Regenerado desde `view-rules.md` (`VAR-7.2` y `VAR-7.3`; nunca a mano) |
| `src/test/java/com/educaflow/views/support/Index.java` | Modificar | developer-create-view-tests | Regenerado desde el glosario de `view-rules.md` (`PREDEFINIDAS` + `remote-refreshTab-action`; nunca a mano) |
| `.claude/skills/k-vistas/forms.md` | Modificar | k-skill | Tabla «form principal vs modal» + sección «Form abierto por código en popup», incluida la frase de `popup="reload"` (coherencia con `VAR-7.2` y D3) |
| `.claude/skills/k-vistas/actions.md` | Modificar | k-skill | § `<action-group>` — secuencia de acciones principales: la excepción «form abierto por código en popup sin grid → `save` → `remote-refreshTab-action` → `close`» junto al `force-back` obligatorio, y `remote-refreshTab-action` en el comentario de las acciones globales; § Convenciones de nombres para las acciones, la «Excepción» pasa a tres globales (`remote-validationSave-action`, `remote-validationDelete-action`, `remote-refreshTab-action`) (coherencia con `VAR-7.2`/`VAR-7.3`) |
| `.claude/skills/k-validaciones/validaciones.md` | Modificar | k-skill | §4 («cierre: MUST ser `force-back`»): la misma excepción |
| `.claude/skills/k-sistemas/controladores.md` | Modificar | k-skill | La línea de `force-back` tras guardar: la misma excepción, sin la frase de `popup="reload"` (D3) |
| `.claude/skills/sdd-designer/template-system/vistas.md` | Modificar | k-skill | §1.5 y detector e) de §3: admiten `save` → `remote-refreshTab-action` → `close` en el form sin grid |
| `src/test/java/com/educaflow/subsystem/security/service/impl/MenuSecurityServiceImplTest.java` | Modificar | — | Casos del menú nuevo (detalle en `test-unit-desc.md`) |
| `src/test/java/com/educaflow/tramites/util/verificacion/VerificacionHelperTest.java` | Modificar | — | Adaptado a `NotificacionService` (detalle en `test-unit-desc.md`) |
| `subsystem/correos/**` | Eliminar | — | Subsistema retirado (todo su árbol) |
| `subsystem/sms/**` | Eliminar | — | Subsistema retirado (todo su árbol) |
| `src/test/java/com/educaflow/subsystem/correos/**`, `src/test/java/com/educaflow/subsystem/sms/**` | Eliminar | — | Sustituidos por los tests de `notificaciones` (`test-unit-desc.md`) |
| `src/test/e2e/subsystem/correos/**`, `src/test/e2e/subsystem/sms/**` | Eliminar | — | Supersedidos (D6, `## Tests E2E supersedidos`) |

> **Nota para `/sdd-implementer`:** los XML de `domains/`, `views/` y `menus.xml` ya están materializados en la carpeta `design/`. **MUST NOT** modificarlos, reescribirlos ni regenerarlos: se **copian verbatim** a su ubicación final (`menus.xml` se fusiona en el `menus.xml` único del proyecto; `views/DefaultModelController.xml` va a `base/infrastructure/controller/DefaultModelController.xml`, Paso 8). El código Java es lo único que se implementa a partir de las firmas y comentarios del diseño.

Para una clase Java con `Acción: Modificar` se declaran solo las firmas nuevas o cambiadas; **el resto de la clase se conserva**.

## Pasos

### Paso 1 — Dominios (y retirada de los dominios antiguos)

Copiar `design/domains/{Notificacion,Correo,Sms,Adjunto}.xml` a `subsystem/notificaciones/domains/` y **borrar** en el mismo paso los árboles completos `subsystem/correos/` y `subsystem/sms/` (si conviven, Axelor ve dos entidades `Correo` y dos `Adjunto`; todo lo que contienen lo sustituyen los Pasos 3–13).
En el mismo paso se borran sus tests unitarios (`src/test/java/com/educaflow/subsystem/correos/` y `…/sms/`; los nuevos los describe `test-unit-desc.md`) y sus E2E supersedidos (`src/test/e2e/subsystem/correos/` y `…/sms/`, ver `## Tests E2E supersedidos`).
Desde aquí el proyecto no compila hasta terminar el Paso 9 (las referencias externas se arreglan en los Pasos 7 y 12): los Pasos 1–9 se verifican juntos con `./gradlew -q compileJava` al final del 9.

Resumen estructural:

- **`Notificacion.xml`** — `module notificaciones` (`com.educaflow.subsystem.notificaciones.db`). Entidad `Notificacion` con `strategy="JOINED"` (D2).
  Campos: `tipoNotificacion` (enum, servidor), `name` («Motivo», cliente), `dniDestinatario`, `nombre`, `apellidos` (cliente), `destino` (campo función: `computeDestino()` que la base rechaza con `IllegalStateException` y cada subclase sobrescribe), `centro` (m2o `Centro`, cliente), `historialEstado` (m2o `HistorialEstado`, «Estado del expediente», cliente), `nombreExpediente` (`formula`, «Expediente»), `estado`, `fechaCreacion`, `fechaPrimerIntentoEnvio`, `fechaUltimoIntentoEnvio`, `fechaEnvio`, `numeroReintentos`, `descripcionUltimoFallo` (servidor).
  Enum `TipoNotificacion` (`CORREO` «Correo», `SMS` «SMS__!!») con `extra-code-model`: `getClaseNotificacion()` (`switch` exhaustivo) y `static deClase(Class<?>)` (lanza `IllegalStateException` si la clase no es de ningún canal) — el **único** sitio que sabe qué clase es cada canal (D2).
  Enum `EstadoNotificacion` (`PENDIENTE` «Pendiente», `ENVIADO` «Enviado», `FALLIDO` «Fallido»).
  `required="true"` solo en los campos de las RES (k-validaciones/restricciones.md §1): `name` (RES-Notificacion-001), `dniDestinatario` (RES-Notificacion-014) y `centro` (RES-Notificacion-013); `validateInsert` conserva sus comprobaciones solo para dar el mensaje personalizado. Los VAL- (nombre, apellidos, para, asunto, cuerpo, teléfono, mensaje) no llevan `required` en el dominio, y ningún campo lleva `max` declarativo.
- **`Correo.xml`** — `Correo extends Notificacion`: `para`, `enCopia`, `enCopiaOculta`, `asunto`, `cuerpo` (large, multiline), `adjuntos` (o2m `Adjunto` mappedBy `correo`); finder `findByEstado` (con el tipo explícito en `using`, porque `estado` es heredado, `all="true"`); `extra-code-model` con `computeDestino()` → `getPara()` (CC-Correo-001).
- **`Sms.xml`** — `Sms extends Notificacion`: `telefono`, `mensaje` (multiline); `extra-code-model` con `computeDestino()` → `getTelefono()` (CC-Sms-001).
- **`Adjunto.xml`** — `nombreFichero`, `contenido` (m2o `MetaFile`), `correo` (m2o `Correo`), los tres `required="true"` (RES-Adjunto-002/003/001), y `unique-constraint correo,nombreFichero`.

Verificación: la generación de código de Axelor (parte de `./gradlew -q compileJava`) produce `build/src-gen/…/notificaciones/db/{Notificacion,Correo,Sms,Adjunto,TipoNotificacion,EstadoNotificacion}.java` y `db/repo/{Notificacion,Correo,Sms,Adjunto}Repository.java`, con `computeDestino()` sobrescrito en `Correo` y `Sms`; `grep -rl "subsystem\.correos\|subsystem\.sms" src/main/java` solo devuelve `tramites/util/verificacion/VerificacionHelper.java` (Paso 7).

### Paso 2 — Utilidad de gestor del centro

```java
// Clase: com.educaflow.subsystem.notificaciones.util.GestorNotificacionesUtil   (final, constructor privado)
// @ScriptAllowed — con el comentario de por qué (como en MenuSecurityService): los conditionParams de los
// permisos *.propio-centro-gestion (Paso 13) la invocan desde Groovy y la ScriptPolicy de Axelor solo admite
// clases anotadas.
// Único dueño de «gestor del centro» de notificaciones (D4): menú, reenvío, <domain> de «Del centro» y
// permisos le preguntan; no hay ninguna copia JPQL de la clasificación.

private static final Set<TipoUsuarioCodigo> TIPOS_GESTORES;   // EnumSet.of(SUPERVISOR, ADMINISTRATIVO)
private static final Set<CargoCodigo> CARGOS_GESTORES;        // EnumSet.of(DIRECTOR, JEFE_ESTUDIOS, SECRETARIO)

public static boolean esGestorEnAlgunCentro(User user);
//   true si alguno de user.getCentroUsuarios() cumple gestiona(...). false si la colección es null o vacía.

public static List<Long> idsCentrosGestionados(User user);
//   Ids de los centros de los CentroUsuario del usuario que cumplen gestiona(...), sin repetidos.
//   Si no gestiona ninguno o user es null → List.of(-1L) (un IN vacío no es portable y -1 no es el id de
//   ninguna fila). Es el ÚNICO sitio con ese centinela: lo usan tal cual el <domain>, los permisos y el reenvío
//   (V-Notificacion-015: idsCentrosGestionados(user).contains(centro.getId())).

private static boolean gestiona(CentroUsuario centroUsuario);
//   Algún CentroUsuarioTipoUsuario con tipoUsuario.codigo ∈ TIPOS_GESTORES, o algún CentroUsuarioCargo con
//   cargo.code ∈ CARGOS_GESTORES. Colecciones null = vacías; tipos/cargos null se ignoran.
```

Verificación: con el bloque de los Pasos 1–9; tests unitarios en `test-unit-desc.md` (un caso por tipo y por cargo, vicesecretario y profesor = no gestor, varios centros).

### Paso 3 — Interfaces de servicio y base abstracta del canal

**`NotificacionCanalService`** (`com.educaflow.subsystem.notificaciones.service`):

```java
public interface NotificacionCanalService<T extends Notificacion> extends ModelService<T> {
    T reenviar(T entidad, T entidadOriginal);


    Optional<BusinessMessages> validateReenviar(T entidad, T entidadOriginal);


    AllowProperties allowPropertiesReenviar();
}
```

**`CorreoService extends NotificacionCanalService<Correo>`**: `List<Correo> listarCorreosEnFail();` ⏎⏎⏎ `Optional<BusinessMessages> validateListarCorreosEnFail();` (se conserva de `correos`, como piden las guías).
**`SmsService extends NotificacionCanalService<Sms>`**: sin métodos propios.
**`AdjuntoService extends ModelService<Adjunto>`**: sin métodos propios.

**`NotificacionCanalServiceImpl<T extends Notificacion>`** (`…service.impl`, `public abstract class … extends DefaultModelService<T> implements NotificacionCanalService<T>`) — patrón nuevo (D1). Cumple C16 (nombre `*ServiceImpl` en `service.impl`); `ModelServiceFactory` nunca la instancia porque no hay entidad `NotificacionCanal`.

```java
@Inject EjecutorAsincrono ejecutorAsincrono;

/** Los mensajes «de su canal» que pide la spec; cada canal construye el suyo con I18n.get("literal"). */
protected record TextosCanal(String centroAjeno, String estadoExpedienteInexistente, String inmutable,
                             String noBorrable, String soloFallidas, String reenvioCentroAjeno) {}

protected NotificacionCanalServiceImpl(Class<T> model, Repository<T> repository);   // super(model, repository)

// ---------- Acciones (antes del primer header) ----------
@Override public T insert(T notificacion);
//   validateInsert(notificacion).ifPresent(BusinessMessages::throwIfInvalid);
//   fireActionRule_AsignarValoresIniciales(notificacion)        R-Notificacion-001 (Antes)
//   aplicarReglasDelCanalAntesDeGuardar(notificacion)           gancho: R-Correo-001 / R-Sms-001 (Antes)
//   notificacion = repository.save(notificacion)               (los adjuntos se guardan en cascada: R-Adjunto-001)
//   fireActionRule_ProgramarEnvioAsincrono(notificacion)        R-Notificacion-002 (Después)
//   return notificacion. MUST NOT super.insert.

@Override public T update(T nueva, T original);
//   V-Notificacion-012 (§9.2 de k-secure-coding): sin flujo normal, siempre
//   throw new UnsupportedOperationException(textosDelCanal().inmutable()).

@Override public void remove(T notificacion);
//   V-Notificacion-013: siempre throw new UnsupportedOperationException(textosDelCanal().noBorrable()).

@Override public T reenviar(T entidad, T entidadOriginal);
//   validateReenviar(entidad, entidadOriginal).ifPresent(BusinessMessages::throwIfInvalid);
//   fireActionRule_ProgramarEnvioAsincrono(entidadOriginal)     R-Notificacion-004 (Después del commit)
//   return entidadOriginal. MUST NOT repository.save: reenviar no cambia nada de forma síncrona;
//   el cambio de estado lo hace enviar() (R-Notificacion-003).

protected abstract void aplicarReglasDelCanalAntesDeGuardar(T notificacion);
//   Gancho: reglas del canal que transforman datos antes de guardar (Correo: copiar adjuntos; Sms:
//   normalizar teléfono). Abstracto a propósito: cada canal decide explícitamente, aunque sea «nada».

protected abstract void enviarPorCanal(T notificacion);
//   Gancho: envía por el canal; lanza RuntimeException si falla (R-Notificacion-003, paso 5).

protected abstract TextosCanal textosDelCanal();
//   Gancho: los seis mensajes del canal, construidos con I18n.get("literal") en cada llamada (para que
//   el script de i18n los encuentre y se traduzcan al idioma del usuario).

// ---------- Métodos de Validación ----------
@Override public Optional<BusinessMessages> validateInsert(T notificacion);
//   Solo la secuencia de llamadas sobre un BusinessMessages, en este orden: validarMotivo, validarDniDestinatario,
//   validarNombreYApellidos, validarCentro, validarHistorialEstado, validateDatosDelCanal(notificacion, messages).
//   Devuelve Optional.empty() si no hay mensajes.

// Privados (bloque de validación). Los cuatro últimos se MUEVEN aquí desde los privados homónimos de
// correos/CorreoServiceImpl y sms/SmsServiceImpl (validarDestinatario de SMS se funde en los dos primeros),
// ampliados con las V nuevas; validarMotivo es nuevo (campo nuevo):
private void validarMotivo(T notificacion, BusinessMessages messages);
//   V-Notificacion-001 (RES-Notificacion-001) name vacío o solo espacios → mensaje de motivo obligatorio;
//   si no, V-Notificacion-002 (RES-Notificacion-002) más de 255 caracteres → mensaje de longitud máxima del motivo.
private void validarDniDestinatario(T notificacion, BusinessMessages messages);
//   V-Notificacion-003 (RES-Notificacion-014) DNI vacío → mensaje de DNI obligatorio; si no, V-Notificacion-004
//   (VAL-Correo-002, VAL-Sms-002) DniUtil.isValid falso → mensaje de DNI no válido (letra).
private void validarNombreYApellidos(T notificacion, BusinessMessages messages);
//   V-Notificacion-005/006 (VAL-*-003/004) nombre / apellidos vacíos → sus mensajes de obligatorio; si no,
//   V-Notificacion-007/008 (VAL-Correo-015/016, VAL-Sms-011/012) más de 255 → sus mensajes de longitud.
private void validarCentro(T notificacion, BusinessMessages messages);
//   V-Notificacion-009 (RES-Notificacion-013) centro null → mensaje de centro obligatorio; si no, y el usuario
//   no es administrador (SecurityUtil.isAdmin(SecurityUtil.getUser())) y no pertenece al centro
//   (user.perteneceAlCentro(centro)) → V-Notificacion-010 (VAL-Correo-014, VAL-Sms-010) textosDelCanal().centroAjeno().
private void validarHistorialEstado(T notificacion, BusinessMessages messages);
//   V-Notificacion-011 (RES-Notificacion-007) solo si centro y historialEstado no son null: el historialEstado
//   sin id (cáscara del cliente), sin expediente, o con expediente de otro centro →
//   textosDelCanal().estadoExpedienteInexistente(). Sin releer por repositorio (JPA.edit ya resolvió la referencia).

@Override public Optional<BusinessMessages> validateUpdate(T nueva, T original);
//   V-Notificacion-012 (RES-Notificacion-004): siempre Optional.of(single(textosDelCanal().inmutable())).

@Override public Optional<BusinessMessages> validateRemove(T notificacion);
//   V-Notificacion-013 (RES-Notificacion-005): siempre Optional.of(single(textosDelCanal().noBorrable())).

@Override public Optional<BusinessMessages> validateReenviar(T entidad, T entidadOriginal);
//   Sobre entidadOriginal (el estado real en BD; entidad no trae nada: allowPropertiesReenviar es deny-all).
//   Empieza con Objects.requireNonNull(entidadOriginal, "entidadOriginal no puede ser null"): que falte es un error
//   de programación del llamante (assert, sin I18n ni regla), no un mensaje de negocio.
//     - V-Notificacion-014 (VAL-Notificacion-001) estado != FALLIDO → textosDelCanal().soloFallidas().
//     - V-Notificacion-015 (VAL-Notificacion-002) ni SecurityUtil.isAdmin(user) ni
//       GestorNotificacionesUtil.idsCentrosGestionados(user).contains(entidadOriginal.getCentro().getId())
//       → textosDelCanal().reenvioCentroAjeno().

protected abstract void validateDatosDelCanal(T notificacion, BusinessMessages messages);
//   Gancho: añade a messages las V propias del canal.

// ---------- AllowProperties ----------
@Override public AllowProperties allowPropertiesInsert();
//   createAllowProperties(campos comunes cliente {name, dniDestinatario, nombre, apellidos, centro, historialEstado}
//   + allowPropertiesDelCanal()). Fuera: tipoNotificacion, destino, nombreExpediente y todos los del envío.

@Override public AllowProperties allowPropertiesUpdate();   // createDenyAllProperties() — inmutable
@Override public AllowProperties allowPropertiesRemove();   // createDenyAllProperties() — no se borra
@Override public AllowProperties allowPropertiesReenviar(); // createDenyAllProperties() — actúa sobre lo guardado

protected abstract Map<String, Object> allowPropertiesDelCanal();
//   Gancho: los campos cliente propios del canal, en el formato de AllowProperties.createAllowProperties.

// ---------- Action Rules ----------
private void fireActionRule_AsignarValoresIniciales(T notificacion);
//   R-Notificacion-001 (Origen spec: RN-Notificacion-001, CC-Notificacion-001, CC-Notificacion-004,
//   RES-Notificacion-003, RES-Correo-001, RES-Sms-001). Momento: Antes de repository.save.
//   Asignaciones INCONDICIONALES (k-secure-coding §3.3; MUST NOT `if (campo == null)`: el cliente no puede dictar
//   ninguno de estos campos aunque lleguen rellenos por el REST genérico):
//     tipoNotificacion = TipoNotificacion.deClase(EntityHelper.getEntityClass(notificacion));
//     estado = PENDIENTE; fechaCreacion = LocalDateTime.now(Convert.defaultZoneId); numeroReintentos = 0;
//     fechaPrimerIntentoEnvio = fechaUltimoIntentoEnvio = fechaEnvio = descripcionUltimoFallo = null.

private void fireActionRule_ProgramarEnvioAsincrono(T notificacion);
//   R-Notificacion-002 / R-Notificacion-004 (Origen spec: RN-Notificacion-002, RN-Notificacion-007). Momento: Después.
//   ejecutorAsincrono.ejecutarTrasCommit(() -> this.enviar(id)) pasando solo el id. Detalle: rules/R-Notificacion-003.md.

private void fireActionRule_RegistrarIntentoEnvio(T notificacion);     // R-Notificacion-003 (RN-Notificacion-004)
private void fireActionRule_MarcarEnvioCorrecto(T notificacion);       // R-Notificacion-003 (RN-Notificacion-005)
private void fireActionRule_MarcarEnvioFallido(T notificacion, RuntimeException excepcion); // R-Notificacion-003 (RN-Notificacion-006)
//   Detalle de las tres en design/rules/R-Notificacion-003.md (asignaciones incondicionales; la fecha del primer
//   intento se asigna cuando numeroReintentos == 0, antes de incrementarlo — sin `if (campo == null)`).

// ---------- Otras funciones ----------
void enviar(Long notificacionId);
//   Package-private (lo prueban los tests). Implementa R-Notificacion-003; diseño detallado en
//   design/rules/R-Notificacion-003.md (transacción propia, PESSIMISTIC_WRITE sobre `model`, idempotente con ENVIADO).
```

Verificación (al cerrar el bloque 1–9): compila; `awk` de headers de `k-sistemas/servicios.md` sobre el fichero (tres líneas iguales por header); `./gradlew -q test --tests '*ordenmetodos*'`.

### Paso 4 — Canal correo y adjunto

**`CorreoServiceImpl extends NotificacionCanalServiceImpl<Correo> implements CorreoService`**:

```java
@Inject Provider<MailSender> mailSenderProvider;   // Provider: se resuelve dentro del envío (RN-Correo-003)

public CorreoServiceImpl(Class<Correo> model, Repository<Correo> repository);   // super(model, repository)

// ---------- Acciones ----------
@Override public List<Correo> listarCorreosEnFail();
//   validateListarCorreosEnFail().ifPresent(throwIfInvalid); ((CorreoRepository) repository).findByEstado(FALLIDO).fetch().

@Override protected void aplicarReglasDelCanalAntesDeGuardar(Correo correo);   // fireActionRule_CopiarAdjuntos(correo)
@Override protected void enviarPorCanal(Correo correo);                        // mailSenderProvider.get().send(construirMail(correo))
@Override protected TextosCanal textosDelCanal();
//   new TextosCanal(...) con I18n.get de los mensajes que la spec da para el correo, en este orden:
//   centroAjeno = VAL-Correo-014; estadoExpedienteInexistente = RES-Notificacion-007 (versión correo);
//   inmutable = RES-Notificacion-004 (correo); noBorrable = RES-Notificacion-005 (correo);
//   soloFallidas = VAL-Notificacion-001 (correo); reenvioCentroAjeno = VAL-Notificacion-002 (correo).
//   Literales tal cual los da la spec (son los que hoy usa correos), dentro de I18n.get("…") para el script de i18n.

// ---------- Métodos de Validación ----------
@Override public Optional<BusinessMessages> validateListarCorreosEnFail();   // sin condiciones: Optional.empty()

@Override protected void validateDatosDelCanal(Correo correo, BusinessMessages messages);
//   Solo llama a los privados que se CONSERVAN de correos/CorreoServiceImpl, ampliados con las V nuevas:
//   validarDirecciones (V-Correo-001..005 y la nueva V-Correo-010), validarAsuntoYCuerpo (V-Correo-006/007/009 y la
//   nueva V-Correo-008) y uno nuevo, validarTamanoTotalAdjuntos (V-Correo-011). Reparto de las V:
//   - V-Correo-001 (VAL-Correo-005) separarDirecciones(para) vacía → mensaje de «para» sin destinatario.
//   - V-Correo-002 (VAL-Correo-006) más de una dirección en el «para» → mensaje de una sola dirección (use «en copia»).
//   - V-Correo-003 (VAL-Correo-007) exactamente una y !EMailUtil.isValid → mensaje de formato del «para».
//   - V-Correo-004 / V-Correo-005 (VAL-Correo-008/009) «en copia» / «en copia oculta» con alguna dirección no válida.
//   - V-Correo-010 (VAL-Correo-018) solo si «en copia» o «en copia oculta» no están en blanco: alguna dirección
//     repetida en la unión de las tres listas (comparación tras trim y en minúsculas) → mensaje de dirección repetida.
//   - V-Correo-006 (VAL-Correo-010) asunto vacío o solo espacios; si no, V-Correo-007 (VAL-Correo-011) > 255
//     y V-Correo-008 (VAL-Correo-017) contiene \p{Cntrl} (incluye saltos de línea) → sus mensajes.
//   - V-Correo-009 (VAL-Correo-012) cuerpo vacío o solo espacios.
//   - V-Correo-011 (VAL-Correo-019) solo si hay adjuntos: suma de MetaFileUtil.fileSizeOnDisk(contenido) de los
//     adjuntos con contenido > 25 MB → mensaje de tamaño total. Se mide en disco: fileSize del MetaFile lo dicta el cliente.

// ---------- AllowProperties ----------
@Override protected Map<String, Object> allowPropertiesDelCanal();
//   {para, enCopia, enCopiaOculta, asunto, cuerpo, adjuntos: {nombreFichero, contenido}}
//   (el `correo` del adjunto no: lo fija el mappedBy al guardar en cascada).

// ---------- Action Rules ----------
private void fireActionRule_CopiarAdjuntos(Correo correo);
//   R-Correo-001 (Origen spec: RN-Adjunto-001, RN-Adjunto-003). Momento: Antes de repository.save.
//   Para cada adjunto: copia = MetaFileUtil.cloneMetaFile(contenido); copia.setFileName(nombreFichero.trim());
//   adjunto.setContenido(copia). Así lo que viaja y se descarga no cambia si el original se toca, y la descarga
//   usa el nombre del adjunto.

// ---------- Otras funciones ----------
private List<String> separarDirecciones(String direcciones);   // split por comas, trim, sin vacíos (RN-Correo-004)
private Mail construirMail(Correo correo);
//   RN-Correo-001: to/cc/bcc = separarDirecciones(...), from = AppSettings "mail.address.from", asunto, cuerpo como
//   texto plano (html null), adjuntos como Fichero(nombreFichero, MetaFileUtil.downloadContent, fileType).
```

**`AdjuntoServiceImpl extends DefaultModelService<Adjunto> implements AdjuntoService`** (lógica de `correos` + V nuevas):

```java
public AdjuntoServiceImpl(Class<Adjunto> model, Repository<Adjunto> repository);

@Override public Adjunto update(Adjunto nuevo, Adjunto original);   // V-Adjunto-004: throw UnsupportedOperationException(inmutable)
@Override public void remove(Adjunto adjunto);                      // V-Adjunto-005: throw UnsupportedOperationException(no se borran)

@Override public Optional<BusinessMessages> validateInsert(Adjunto adjunto);
//   Solo la secuencia de llamadas a los privados que se CONSERVAN de correos/AdjuntoServiceImpl (validarCorreoObligatorio,
//   validarCentroDelCorreo, validarCorreoNoExistente, validarNombreFichero, validarCaracteresNombreFichero,
//   validarContenido, validarTamanoContenido, validarNombreUnicoEnCorreo), ampliados con las V nuevas
//   (V-Adjunto-010/011 en los de nombre, V-Adjunto-012 en el de tamaño). Reparto de las V:
//   - V-Adjunto-001 (RES-Adjunto-001) correo null.
//   - V-Adjunto-006 (VAL-Adjunto-001) correo indicado, usuario no admin y no pertenece a correo.centro.
//   - V-Adjunto-007 (VAL-Adjunto-002) correo indicado con correo.getId() != null (el correo ya está persistido;
//     k-validaciones/validaciones.md §2, «Maestro-detalle»).
//   - V-Adjunto-002 (RES-Adjunto-002) nombreFichero vacío o solo espacios; si no:
//       V-Adjunto-009 (VAL-Adjunto-004) contiene «/», «\» o \p{Cntrl};
//       V-Adjunto-010 (VAL-Adjunto-005) más de 255 caracteres;
//       V-Adjunto-011 (VAL-Adjunto-006) trim es «.» o «..».
//   - V-Adjunto-003 (RES-Adjunto-003) contenido null; si no:
//       V-Adjunto-008 (VAL-Adjunto-003) MetaFileUtil.fileSizeOnDisk > 10 MB;
//       V-Adjunto-012 (VAL-Adjunto-007) fileSizeOnDisk == 0.
//   - V-Adjunto-013 (RES-Correo-002) otro adjunto del mismo correo con el mismo nombre (trim) → mensaje de nombre repetido
//     (defensa en profundidad de la unique-constraint).
@Override public Optional<BusinessMessages> validateUpdate(Adjunto nuevo, Adjunto original);   // siempre inmutable
@Override public Optional<BusinessMessages> validateRemove(Adjunto adjunto);                   // siempre «no se borran»
@Override public AllowProperties allowPropertiesInsert();    // {nombreFichero, contenido, correo} (correo: padre cliente, k-secure-coding §3.6)
@Override public AllowProperties allowPropertiesUpdate();    // createDenyAllProperties()
@Override public AllowProperties allowPropertiesRemove();    // createDenyAllProperties()
```

**`MetaFileUtil`** (Modificar, el resto se conserva): `public static long fileSizeOnDisk(MetaFile metaFile)` — `Files.size(MetaFiles.getPath(metaFile))`, `IOException` → `UncheckedIOException`.

Verificación (al cerrar el bloque 1–9): compila.

### Paso 5 — Canal SMS

**`SmsServiceImpl extends NotificacionCanalServiceImpl<Sms> implements SmsService`**:

```java
@Inject Provider<SmsSender> smsSenderProvider;   // se resuelve dentro del envío (RN-Sms-002)

public SmsServiceImpl(Class<Sms> model, Repository<Sms> repository);

@Override protected void aplicarReglasDelCanalAntesDeGuardar(Sms sms);   // fireActionRule_NormalizarTelefono(sms)
@Override protected void enviarPorCanal(Sms sms);
//   smsSenderProvider.get().send(new com.educaflow.base.infrastructure.sms.Sms(sms.getTelefono(), sms.getMensaje()))
@Override protected TextosCanal textosDelCanal();
//   Ídem con los mensajes que la spec da para el SMS: centroAjeno = VAL-Sms-010; el resto, la versión SMS de
//   RES-Notificacion-007, RES-Notificacion-004, RES-Notificacion-005, VAL-Notificacion-001 y VAL-Notificacion-002.

@Override protected void validateDatosDelCanal(Sms sms, BusinessMessages messages);
//   Solo llama a los privados que se CONSERVAN de sms/SmsServiceImpl: validarTelefono y validarMensaje.
//   - V-Sms-001 (VAL-Sms-005) teléfono vacío; si no, V-Sms-002 (VAL-Sms-006) !new NumeroTelefono(t).esMovilDeEspana().
//   - V-Sms-003 (VAL-Sms-007) mensaje vacío o solo espacios (es además la guarda de MensajeSmsUtil, que no admite
//     blancos); si no, V-Sms-004 (VAL-Sms-008) !MensajeSmsUtil.cabeEnUnSms(mensaje).

@Override protected Map<String, Object> allowPropertiesDelCanal();   // {telefono, mensaje}

private void fireActionRule_NormalizarTelefono(Sms sms);
//   R-Sms-001 (Origen spec: RN-Sms-001, RES-Sms-002). Momento: Antes de repository.save. Incondicional:
//   telefono = new NumeroTelefono(telefono).enFormatoE164() (validateInsert ya garantizó que es un móvil válido).
```

Verificación (al cerrar el bloque 1–9): compila.

### Paso 6 — Servicio de `Notificacion` (factorías y puerta REST de la base)

**`NotificacionService extends ModelService<Notificacion>`**:

```java
Correo createCorreo();
Sms createSms();


Optional<BusinessMessages> validateCreateCorreo();
Optional<BusinessMessages> validateCreateSms();
```

**`NotificacionServiceImpl extends DefaultModelService<Notificacion> implements NotificacionService`**:

```java
@Inject ModelServiceFactory modelServiceFactory;   // la factoría, no un ModelService (C21)

public NotificacionServiceImpl(Class<Notificacion> model, Repository<Notificacion> repository);

@Override public Notificacion insert(Notificacion notificacion);
//   V-Notificacion-017: la operación nunca se admite (§9.2 de k-secure-coding): solo
//   throw new UnsupportedOperationException(mensaje de V-Notificacion-017), igual que update/remove de los canales.
@Override public Notificacion update(Notificacion nueva, Notificacion original);
//   Delegación al dueño: servicioDelCanal(original).update(nueva, original) (lanza con el texto de su canal).
@Override public void remove(Notificacion notificacion);   // servicioDelCanal(notificacion).remove(notificacion)
@Override public Correo createCorreo();
//   validateCreateCorreo().ifPresent(throwIfInvalid); new Correo() con
//   tipoNotificacion = TipoNotificacion.deClase(Correo.class). NO lo inserta (quien llama rellena y usa CorreoService).
@Override public Sms createSms();   // ídem con Sms

@Override public Optional<BusinessMessages> validateInsert(Notificacion notificacion);
//   V-Notificacion-017 (Origen spec: —; «Input AllowProperties: ninguna» de la acción Crear de Notificacion):
//   siempre rechaza; el mensaje transmite que una notificación se da de alta siempre como uno de sus canales.
//   (Por /ws/rest/…Notificacion JPA.edit crea siempre una Notificacion «genérica»: nunca es un canal.)
@Override public Optional<BusinessMessages> validateUpdate(Notificacion nueva, Notificacion original);
//   servicioDelCanal(original).validateUpdate(nueva, original) → V-Notificacion-012 con el texto de su canal.
@Override public Optional<BusinessMessages> validateRemove(Notificacion notificacion);
//   servicioDelCanal(notificacion).validateRemove(notificacion) → V-Notificacion-013.
@Override public Optional<BusinessMessages> validateCreateCorreo();   // sin condiciones: Optional.empty()
@Override public Optional<BusinessMessages> validateCreateSms();      // sin condiciones: Optional.empty()

@Override public AllowProperties allowPropertiesInsert();   // createDenyAllProperties()
@Override public AllowProperties allowPropertiesUpdate();   // createDenyAllProperties()
@Override public AllowProperties allowPropertiesRemove();   // createDenyAllProperties()

private NotificacionCanalService<Notificacion> servicioDelCanal(Notificacion notificacion);
//   (NotificacionCanalService) modelServiceFactory.resolve(EntityHelper.getEntityClass(notificacion)).
//   La puerta /ws/rest/…Notificacion/{id} carga la instancia de la subclase (JOINED); quien sabe qué hacer con
//   ella es el servicio de su canal. Si la clase real no es un canal (no puede pasar: JOINED solo persiste
//   subclases), IllegalStateException.
```

Verificación (al cerrar el bloque 1–9): compila; un `POST /ws/rest/com.educaflow.subsystem.notificaciones.db.Notificacion` de admin responde error sin crear fila (test unitario en `test-unit-desc.md`).

### Paso 7 — Consumidor externo: aviso de subsanación

**`VerificacionHelper`** (`tramites/util/verificacion`, Modificar; el resto de la clase se conserva):

```java
public boolean avisarDeSubsanacion(Expediente expediente, String textoSubsanacion);
//   Mismo contrato. Resuelve NotificacionService y CorreoService con modelServiceFactory; correo = crearCorreoSubsanacion(...);
//   si CorreoService.validateInsert(correo) devuelve mensajes → log.info (CRLF saneados, como ahora) y false;
//   si no → CorreoService.insert(correo) y true. R-Correo-002 (Origen spec: RN-Correo-002).

private Correo crearCorreoSubsanacion(NotificacionService notificacionService, Expediente expediente, String textoSubsanacion);
//   (deja de ser static: usa la factoría) correo = notificacionService.createCorreo(); rellena como ahora dni, nombre,
//   apellidos, para (email del solicitante), centro, asunto y cuerpo, y además:
//     name = I18n.get("Subsanación del expediente %s").formatted(numeroExpediente)
//     historialEstado = estadoActual(expediente)

private static HistorialEstado estadoActual(Expediente expediente);
//   El HistorialEstado más reciente (máximo por fecha) de expediente.getHistorialEstados(): el del estado en que
//   está el expediente cuando se pide la subsanación, porque el triggerVerificar llama a este helper antes de que
//   el Tramitador añada el historial del estado nuevo.
```

`tramites/util/CLAUDE.md`: la línea «Depende de `subsystem/correos`.» pasa a «Depende de `subsystem/notificaciones`.».

Verificación (al cerrar el bloque 1–9): compila; `grep -rn "subsystem.correos" src/main/java` vacío.

### Paso 8 — Controladores y ayudante de respuesta

**`ActionResponseHelper`** (Modificar; el resto se conserva):

```java
public void doResponseViewFormEnPopup(String viewName, Class<? extends Model> modelClass, Long id, String title);
//   builder = vistaForm(viewName, modelClass, title)
//     .param("popup", "true")
//     .param("popup-save", "false");     // el listado de debajo lo refresca remote-refreshTab-action (D3)
//   id != null → .context("_showRecord", id) (detalle); id == null → form en alta.
//   response.setView(builder.map()).

private ActionView.ActionViewBuilder vistaForm(String viewName, Class<? extends Model> modelClass, String title);
//   Se extrae del doResponseViewForm existente la parte común (sin _profile ni forceTitle):
//   ActionView.define(title).model(modelClass.getName()).add("form", viewName)
//     .param("forceEdit", "true").param("show-confirm", "false").param("show-toolbar", "false").
//   doResponseViewForm pasa a usarlo y añade encima forceTitle, _profile y su rama _showRecord/newEntity (mismo
//   comportamiento que hoy); así la lista de params comunes vive en un único sitio.
```

**`NotificacionController`** (`…controller`, `@Inject` nada: no llama a servicios):

```java
@CallMethod public void abrirTodas(ActionRequest actionRequest, ActionResponse actionResponse);      // abrirEnPopup(..., "Main")
@CallMethod public void abrirDelCentro(ActionRequest actionRequest, ActionResponse actionResponse);  // abrirEnPopup(..., "Centro")
@CallMethod public void abrirRecibida(ActionRequest actionRequest, ActionResponse actionResponse);   // abrirEnPopup(..., "Mis")
@CallMethod public void continuarAlta(ActionRequest actionRequest, ActionResponse actionResponse);
//   tipo = tipoDelContexto(actionRequest); ActionResponseHelper.doResponseViewFormEnPopup(
//   nombreDelForm("Main", tipo.getClaseNotificacion()), tipo.getClaseNotificacion(), null, I18n.get("Notificación")).
@CallMethod public void prepararAlta(ActionRequest actionRequest, ActionResponse actionResponse);
//   onNew de Main@Correo-form / Main@Sms-form: actionResponse.setValue("tipoNotificacion",
//   TipoNotificacion.deClase(new ActionRequestHelper<>(actionRequest).getModelClass())). Solo presentación: el
//   valor que cuenta lo vuelve a asignar R-Notificacion-001 al guardar (U-notificaciones-todas-003/022).

/* header «Acciones de Validaciones» (vacío) */
/* header «Métodos privados» */
private void abrirEnPopup(ActionRequest actionRequest, ActionResponse actionResponse, String variante);
//   tipo = tipoDelContexto(actionRequest); id = new ActionRequestHelper<>(actionRequest).getId();
//   doResponseViewFormEnPopup(nombreDelForm(variante, tipo.getClaseNotificacion()), tipo.getClaseNotificacion(), id, I18n.get("Notificación")).
//   No carga la fila: el popup la pide por REST, donde Axelor aplica los permisos del usuario (sin IDOR).
private static TipoNotificacion tipoDelContexto(ActionRequest actionRequest);
//   TipoNotificacion.valueOf(String del context "tipoNotificacion"). Sin tipo → IllegalStateException
//   (todas las filas lo tienen; la elección de canal no deja continuar sin él, U-notificaciones-todas-002).
private static String nombreDelForm(String variante, Class<? extends Notificacion> clase);
//   "subsysNotificaciones." + variante + "@" + clase.getSimpleName() + "-form" — el ÚNICO sitio con la convención
//   variante + clase → form (la misma de k-vistas, que verifican VAR-2.1/2.3).
```

**`DefaultModelController`** (`base/infrastructure/controller/`, Modificar; el resto se conserva): es el dueño único de «refrescar el listado de la pestaña desde un form en popup» (D3).

```java
@CallMethod public void refreshTab(ActionRequest actionRequest, ActionResponse actionResponse);
//   actionResponse.setSignal("refresh-tab", null). No lee el contexto ni llama a servicios: axelor-front manda
//   refresh-tab a getActiveTabId(-1), la pestaña ignorando los popups (por eso llega al listado aunque el alta
//   se abra sobre la elección de canal).
```

`DefaultModelController.xml` (Modificar): el fichero completo resultante está materializado en `design/views/DefaultModelController.xml` y se **copia verbatim** a `base/infrastructure/controller/DefaultModelController.xml` (no a `subsystem/notificaciones/views/`, Paso 10).
- Preexistente (se conserva): cabecera `object-views`, el comentario de uso de las acciones globales y las dos `<action-method>` `remote-validationSave-action` y `remote-validationDelete-action`.
- Delta: la acción global sin `model` `remote-refreshTab-action` (`DefaultModelController.refreshTab`), tras las dos anteriores, y en el comentario de cabecera el uso «form abierto por código en popup (sin grid): `btnSave` = … → `save` → `remote-refreshTab-action` → `close`; y al final de cualquier remoto que cambie la fila desde ese popup».
Es la única pieza de refresco del listado: la usan el `btnSave` y el `btnReenviar` de `Main@Correo-form`/`Main@Sms-form` (el `btnReenviar` de `Centro@` delega en el de `Main@`, Paso 10).

**`CorreoController`** y **`SmsController`**: se mueven a `com.educaflow.subsystem.notificaciones.controller` con la misma lógica (`reenviar` y `validateReenviar` vía `ModelServiceFactory.resolve(Correo.class|Sms.class)`, `allowPropertiesReenviar`, aviso «El reenvío del correo/SMS se ha puesto en marcha.»). Cambian paquete e imports y una cosa más: como el form está abierto en popup (D3), tras el aviso `reenviar` responde `actionResponse.setReload(true)`, que recarga el form del popup (ESC-039/040/050/074: «recarga el formulario»), y **deja de** responder `setSignal("refresh-tab")`: el listado de la pestaña de debajo lo refresca `remote-refreshTab-action`, que va la última en el `btnReenviar` de `Main@` de cada canal (Paso 10). Implementan U-notificaciones-todas-011/031 y U-notificaciones-centro-005/011.

**`CorreoController`** (`com.educaflow.subsystem.notificaciones.controller`, Crear; ídem **`SmsController`** con `Sms`/`SmsService` y el aviso «El reenvío del SMS se ha puesto en marcha.»):

```java
@CallMethod @Transactional public void reenviar(ActionRequest actionRequest, ActionResponse actionResponse);
//   resuelve CorreoService con ModelServiceFactory; reenviar(entidad, entidadOriginal) con allowPropertiesReenviar;
//   setNotify con el aviso del canal («El reenvío del correo se ha puesto en marcha.»); setReload(true);
//   sin setSignal("refresh-tab") (el listado lo refresca remote-refreshTab-action, D3).

/* header «Acciones de Validaciones» */
@CallMethod public void validateReenviar(ActionRequest actionRequest, ActionResponse actionResponse);
//   delega en validateReenviar del servicio del canal (CorreoService) y responde sus mensajes.
```

Verificación (al cerrar el bloque 1–9): compila; `./gradlew -q test --tests '*architecture*'` (C9, C15, C28).

### Paso 9 — Módulo Guice

**`NotificacionesModule extends AxelorModule`** (`…module`): `configure()` sin bindings y dos métodos `@Provides MailSender mailSender()` y `@Provides SmsSender smsSender()` con el mismo cuerpo que los `get()` de los antiguos `MailSenderProvider` y `SmsSenderProvider` (leen `AppSettings`, k-guice §3.3), que desaparecen como clases (guía «se fusionan en `NotificacionesModule`»); sin `bind(...).toProvider(...)`. Los servicios siguen inyectando `Provider<MailSender>`/`Provider<SmsSender>` (Guice lo resuelve con el `@Provides`), así que el envío sigue leyendo la configuración dentro del envío (RN-Correo-003, RN-Sms-002). Ningún binding de `ModelService` (los descubre `ModelServiceFactory`). (Los módulos y providers antiguos se borraron en el Paso 1.) No se instala a mano en ningún sitio (Axelor descubre los `AxelorModule`).

Verificación: cierra el bloque 1–9: `./gradlew -q compileJava` en verde y la aplicación arranca sin `Guice/MissingConstructor`.

### Paso 10 — Vistas

Copiar `design/views/*.xml` a `subsystem/notificaciones/views/` (las vistas antiguas se borraron en el Paso 1), **salvo** `design/views/DefaultModelController.xml`, que no es del subsistema y se copia a `base/infrastructure/controller/` (Paso 8), y `design/views/Ref-HistorialEstado.xml`, que se copia a `subsystem/expedientes/views/` (vista de solo lectura del módulo dueño de `HistorialEstado`, VAR-1.2(b); excepción acotada a README §4.2 autorizada por el usuario, D8).

**Mensajes de las acciones de validación local:** Axelor evalúa el `error` de un `<check>` de `<action-condition>` (**siempre**, aunque el `if` sea falso) y el `message` de un `<error>` de `<action-validate>` (cuando su `if` se cumple) como GString de Groovy (`ActionCondition`/`ActionValidate`, `toExpression(…, true)`).
Por eso, en esos textos una `\` literal MUST ir escapada como `\\` y un `$` literal como `\$`; si no, la acción falla en tiempo de ejecución (status -1) y el popup no se cierra.
Es el caso de `Main@Correo.Adjunto-Local-validateSave-action` («… los caracteres / \\ ni caracteres de control», que se muestra con una sola `\`).

**Un `<check>` por campo en cada `<action-condition>`:** `ActionCondition.evaluate` guarda un único error por campo (`errors.put(field, …)`, con `""` si el `if` es falso), así que con varios `<check>` sobre el mismo campo el último pisa a los anteriores y el front no bloquea.
Por eso `Main@Correo.Adjunto-Local-validateSave-action`, que tiene varias reglas sobre `nombreFichero` y sobre `contenido`, es un `<action-validate>` con un `<error>` por regla, que `ActionValidate` corta en el primero que se cumple: el orden de los `<error>` (obligatorio del nombre, obligatorio del contenido, `/` `\` y de control, 255, «.»/«..», fichero vacío) es la prioridad del aviso.
Los `Local-validateSave` de `Main@Correo` y `Main@Sms` sí son `<action-condition>`, porque tienen un solo `<check>` por campo.
Las vistas se copian verbatim, así que si `Main-Correo.xml` ya estaba materializado hay que volver a lanzar `/sdd-implementer` para rematerializarlo.

**`Main-Notificacion.xml`** — `action-view` «Todas las notificaciones» (solo grid). Grid `Main@Notificacion-grid`: columnas estado, tipo, motivo, DNI, nombre, apellidos, destino, centro, expediente, fecha de creación, fecha de envío; `orderBy="-fechaCreacion"`; `action` = `Remote-abrirTodas` (sin `canEditOnClick`/`canViewOnClick`, VAR-8.1); barra con `btnNuevaNotificacion` → grupo que abre `Eleccion@Notificacion-action`.

**`Centro-Notificacion.xml`** — `action-view` con `<domain>self.centro.id IN (:idsCentrosGestionados)</domain>` y `<context … expr="eval: com.educaflow.subsystem.notificaciones.util.GestorNotificacionesUtil.idsCentrosGestionados(__user__)"/>` (la misma expresión que los `conditionParams` de los permisos; la clase es `@ScriptAllowed`). Grid con columnas centro, tipo, estado, DNI, nombre, apellidos, motivo, destino, expediente, fechas; `hilite` `danger` si FALLIDO; `action` = `Remote-abrirDelCentro`.

**`Mis-Notificacion.xml`** — `action-view` con `<domain>self.dniDestinatario = :dniUsuarioActual and self.estado = :estadoEnviado</domain>` y sus dos `<context>`. Grid tipo, destino, expediente, fecha de envío, `orderBy="-fechaEnvio"`; `action` = `Remote-abrirRecibida`.

**`Eleccion-Notificacion.xml`** — `action-view` de form en popup (`popup`, `popup-save=false`, `show-toolbar=false`, `show-toolbar-form=false`, `forceEdit`). Form `Eleccion@Notificacion-form` (model `Notificacion`), panel «Canal» y botones Cancelar (`close`) y Continuar (`readonlyIf="tipoNotificacion == null"`, grupo `Remote-continuarAlta` → `close`).

```
Canal:    tttt········   ← tipoNotificacion(4, RadioSelect horizontal) — único campo, sin relacionados
Botones:  ········ccxx   ← Cancelar(2, offset 8) + Continuar(2)
```

**`Main-Correo.xml`** — bloque `Main@Correo` (form + acciones) y bloque `Main@Correo.Adjunto` (grid + form modal). Form `Main@Correo-form` (`onNew` → `Remote-prepararAlta`; `canBackOnSave="true"` porque tiene `btnSave`). Acciones (en `<?sv-primary-actions?>`, primero los grupos de los botones en el orden del form y después los eventos): `btnCancel` (`close`), `btnSave` (`Local-validateSave` → `remote-validationSave-action` → `save` → `remote-refreshTab-action` → `close`), `btnReenviar` (`Remote-validateReenviar` → `Remote-reenviar` → `remote-refreshTab-action`), `onNew`, `onChange-centro` (→ `set-historialEstado-null`), `Local-validateSave` (8 obligatorios), `set-historialEstado-null`, `Remote-prepararAlta` (NotificacionController), `Remote-validateReenviar`, `Remote-reenviar` (CorreoController). Detalle: grid de adjuntos (`canEditOnClick`, «Añadir adjunto»), form modal con `btnCancel` (`close`), `btnSave` (`Local-validateSave` → `save-modal`), `btnDelete` (`delete-modal`), `onNew` (`set-correo-parent`) y un `Local-validateSave` (`<action-validate>`, no `<action-condition>`: un `<error>` por cada una de las 6 comprobaciones evaluables en cliente, en orden de prioridad, que se para en el primero que se cumple; ver la nota de arriba).

```
Datos del correo (alta y detalle; en detalle todo readonly por readonlyIf del panel):
  tttnnnnnnnnn   ← tipoNotificacion(3, readonly) + name/motivo(9)
  cccchhhhhhhh   ← centro(4) + historialEstado(8)          [el estado del expediente depende del centro]
                   historialEstado: domain por centro, target-name="nameState", canSuggest="false",
                   grid-view/form-view subsysExpedientes.Ref@HistorialEstado (D8): se elige en el buscador, que muestra el
                   número del expediente, el expediente, la fase, el estado y la fecha
  dddooooaaaaa   ← dniDestinatario(3) + nombre(4) + apellidos(5)
  ppppeeeeffff   ← para(4) + enCopia(4) + enCopiaOculta(4)
  ssssssssss··   ← asunto(10); el colOffset(2) de cuerpo completa la fila
  bbbbbbbbbbbb   ← cuerpo(12, colOffset 2, Text) → arranca la fila siguiente en la columna 1
Adjuntos: panel-related «adjuntos» (alta, con «Añadir adjunto») / «adjuntosConsulta» (detalle, Ref) — excluyentes
Datos del envío (solo detalle):
  eeeeeerrrccc   ← estado(6, RadioSelect) + numeroReintentos(3) + fechaCreacion(3)
  pppuuu...fff   ← fechaPrimerIntento(3) + fechaUltimoIntento(3) + fechaEnvio(3, offset 3) [ENVIADO]
  pppuuu......   ← ídem con fechaEnvio oculta al borde derecho                            [PENDIENTE/FALLIDO]
  dddddddddddd   ← descripcionUltimoFallo(12, Text)                                       [solo FALLIDO]
Botones (paneles de estado excluyentes):
  Alta:          ........ccgg   ← Cancelar(2, offset 8) + Guardar(2)
  Detalle FALL.: rr........ss   ← Reenviar(2) + Salir(2, offset 8)
  Detalle resto: ..........ss   ← Salir(2, offset 10)
Modal de adjunto:
  nnnnnnnnffff   ← nombreFichero(8) + contenido(4, binary-link)   (correo oculto)
  Alta:    ........ccgg   ← Cancelar(2, offset 8) + Guardar(2)
  Edición: bb......ccgg   ← Borrar(2) + Cancelar(2, offset 6) + Guardar(2)
```

**`Centro-Correo.xml`** — form `Centro@Correo-form` de solo lectura; `Centro@Correo-btnReenviar-action` contiene una sola acción, el grupo `Main@Correo-btnReenviar-action` (la secuencia del reenvío, con su `remote-refreshTab-action`, vive solo en `Main@Correo`); Salir `close`.

```
Datos del correo: igual que Main, con historialEstado(8, target-name="nameState", grid-view/form-view subsysExpedientes.Ref@HistorialEstado; D8) al borde derecho y showIf="historialEstado != null"
  cccchhhhhhhh   [ligada a un estado]     cccc········   [sin estado: hueco al borde derecho, nada se desplaza]
Datos del envío:
  eeeeeerrrccc   ← estado(6) + numeroReintentos(3) + fechaCreacion(3)
  panel «Intentos» (showIf estado != PENDIENTE; grupo condicional → panel propio):
    pppuuu...fff [ENVIADO]    pppuuu...... [FALLIDO]    (panel oculto) [PENDIENTE]
  dddddddddddd   ← descripcionUltimoFallo(12) [solo FALLIDO]
Botones:  rr........ss   ← Reenviar(2, showIf FALLIDO, primero y sin offset) + Salir(2, offset 8)  [FALLIDO]
          ..........ss   ← (Reenviar oculto al borde izquierdo) + Salir                          [resto]
```

**`Mis-Correo.xml`** — form `Mis@Correo-form` de solo lectura; adjuntos (Ref) solo si hay alguno; Salir `close`.

```
  sssssssssfff   ← asunto(9) + fechaEnvio(3)
  bbbbbbbbbbbb   ← cuerpo(12, Text)
  ppppppeeeeee   ← para(6) + enCopia(6, showIf enCopia)   [con copia]
  pppppp······   ← enCopia oculto al borde derecho         [sin copia]
  Botones: ..........ss   ← Salir(2, offset 10)
```

**`Main-Sms.xml`** — análogo a `Main-Correo.xml` sin adjuntos; `btnReenviar` añade `Local-confirmarReenvio` (alert) antes de la validación remota.

```
Datos del SMS:
  tttnnnnnnnnn   ← tipoNotificacion(3, readonly) + name/motivo(9)
  cccchhhhhhhh   ← centro(4) + historialEstado(8; mismos atributos que en Main-Correo.xml, D8)
  dddooooaaaaa   ← dniDestinatario(3) + nombre(4) + apellidos(5)
  tttt········   ← telefono(4, phone ES); el colOffset(8) de mensaje completa la fila
  mmmmmmmmmmmm   ← mensaje(12, Text)
Datos del envío y botones: idénticos a Main-Correo.xml
```

**`Centro-Sms.xml`** — form `Centro@Sms-form` de solo lectura; `Centro@Sms-btnReenviar-action` contiene una sola acción, el grupo `Main@Sms-btnReenviar-action` (confirmación, validación, reenvío y `remote-refreshTab-action` viven solo en `Main@Sms`); Salir `close`.

```
Datos del SMS (readonly; historialEstado(8, target-name="nameState", grid-view/form-view subsysExpedientes.Ref@HistorialEstado; D8) al borde derecho con showIf="historialEstado != null"):
  tttnnnnnnnnn   ← tipoNotificacion(3) + name/motivo(9)
  cccchhhhhhhh   ← centro(4) + historialEstado(8)                          [ligada a un estado]
  cccc········   ← historialEstado oculto: hueco al borde derecho, nada se desplaza  [sin estado]
  dddooooaaaaa   ← dniDestinatario(3) + nombre(4) + apellidos(5)
  tttt········   ← telefono(4, phone ES); el colOffset(8) de mensaje completa la fila
  mmmmmmmmmmmm   ← mensaje(12, Text)
Datos del envío y botones: idénticos a Centro-Correo.xml
```
**`Mis-Sms.xml`**:

```
  tttt.....fff   ← telefono(4, phone) + fechaEnvio(3, offset 5)
  mmmmmmmmmmmm   ← mensaje(12, Text)
  Botones: ..........ss   ← Salir(2, offset 10)
```

**`Ref-Adjunto.xml`** — `Ref@Adjunto-grid` (`canViewOnClick`) + `Ref@Adjunto-form` (readonly) con Salir `close`.

```
  nnnnnnnnffff   ← nombreFichero(8) + contenido(4, binary-link: al pulsarlo se descarga)
  ..........ss   ← Salir(2, offset 10)
```

**`Ref-HistorialEstado.xml`** (destino `subsystem/expedientes/views/`) — `subsysExpedientes.Ref@HistorialEstado-grid` (`canViewOnClick`, `orderBy="-fecha"`; columnas `expediente.numeroExpediente` «Num. Exped.», `expediente.name` «Expediente», `namePhase`, `nameState`, `fecha`) + `subsysExpedientes.Ref@HistorialEstado-form` (readonly) con Salir `close`. `model` = `com.educaflow.subsystem.expedientes.db.HistorialEstado`; el fichero se crea en `subsystem/expedientes/views/` (módulo dueño de la entidad, VAR-1.2(b), precedente `subsystem/common/views/Ref-Centro.xml`) con los nombres `subsysExpedientes.Ref@HistorialEstado-grid`/`-form`; es la única pieza que el diseño crea en `expedientes`, por excepción autorizada por el usuario (D8). Lo usa el `historialEstado` de `Main@Correo-form` y `Main@Sms-form` (selector) y el de `Centro@Correo-form` y `Centro@Sms-form` (consulta): en los `Main@` el `domain` del campo (`self.expediente.centro = :centro`) se aplica también al buscador.

```
  nnnnxxxxxxxx   ← expediente.numeroExpediente(4) + expediente.name(8)
  ffffeeeeeddd   ← namePhase(4) + nameState(5) + fecha(3)
  ..........ss   ← Salir(2, offset 10)
```

Verificación: `./gradlew -q test --tests 'com.educaflow.views.*'` tras el Paso 11 (con el glosario, `VAR-7.2` y `VAR-7.3` ya ampliados).

### Paso 11 — Ampliación de `VAR-7.2`/`VAR-7.3` y del glosario (patrón nuevo, D3)

`agent_docs/view-rules.md` cambia en tres sitios, todos por la acción global `remote-refreshTab-action` (D3):

- (a) Glosario «**Acciones globales/predefinidas**»: se añade `remote-refreshTab-action` a la lista (tras `remote-validationDelete-action`).
  Se declara en `DefaultModelController.xml`, fuera de `views/`, así que sin esto `Categoria4IntegridadTest` no la resuelve en los grupos que la usan (2 `btnSave` y 2 `btnReenviar`).
- (b) `VAR-7.3`, condición: «una de las globales `remote-validationSave-action`/`remote-validationDelete-action`» pasa a «una de las globales `remote-validationSave-action`/`remote-validationDelete-action`/`remote-refreshTab-action`».
  Sigue sin admitirse ningún `Remote-…-action` propio: `refreshTab` no persiste nada.
- (c) `VAR-7.2`, se reescribe solo la rama del maestro sin grid al que volver. Texto nuevo de la fila `btnSave`/maestro y de `btnCancel`/maestro de la tabla de **Verificación**:
  - `btnSave` (maestro): «[`Local-…`]* → `remote-validationSave-action` → `save` → `force-back` (inmediatamente tras `save`; **nunca** `back`); **si ningún `<action-view>` que lo abra declara una `<view type="grid">`** (form abierto por código en popup), `… → save → remote-refreshTab-action → close`».
  - `btnCancel` (maestro): «contiene `back`; si ningún `<action-view>` que lo abra (una `<view type="form">` con su `name`) declara una `<view type="grid">`, contiene `close`» (desaparece la condición «no declara `btnSave`»).
  - En la **Decisión**, la frase «el popup con `popup="reload"` refresca el listado de debajo» se sustituye por: el form que abre el servidor por código en popup no tiene grid al que volver: `back`/`force-back` no hacen nada y lo que cierra es `close`, también tras guardar; se abre con `popup="true"`, y el listado de debajo lo refresca `remote-refreshTab-action` (`DefaultModelController.refreshTab`, `refresh-tab`, que llega a la pestaña ignorando los popups), no el cierre del popup, por eso va entre `save` y `close`.

Después: `/developer-create-view-tests` regenera (nunca a mano) `src/test/java/com/educaflow/views/botones/Categoria7BotonesTest.java` (`VAR-7.2` y `VAR-7.3`) y `src/test/java/com/educaflow/views/support/Index.java` (`PREDEFINIDAS`, desde el glosario), y se sincronizan, con `/k-skill`:

- todos los sitios que hoy dicen que tras `save` el cierre MUST ser `force-back`, con la misma excepción «form abierto por código en popup sin grid → `save` → `remote-refreshTab-action` → `close`»: `k-vistas/forms.md` (fila «Botón Guardar/Cancelar» de la tabla comparativa + sección «Form abierto por código en popup»), `k-vistas/actions.md` (§ `<action-group>` — secuencia de acciones principales), `k-validaciones/validaciones.md` (§4, cierre), `k-sistemas/controladores.md` (la línea de `force-back`) y `sdd-designer/template-system/vistas.md` (§1.5 y detector e) de §3);
- `k-vistas/actions.md` § Convenciones de nombres para las acciones: la «Excepción» de las acciones globales pasa a listar tres (`remote-validationSave-action`, `remote-validationDelete-action`, `remote-refreshTab-action`);
- la frase «`popup="reload"` refresca el listado de debajo» de `k-vistas/forms.md` § Form abierto por código en popup («Ese método pone `popup="reload"` (al cerrarse tras guardar, Axelor refresca el listado de debajo)») y de `k-sistemas/controladores.md` § Reglas del controlador (excepción de `force-back`) se sustituye por «`popup="true"`; el listado de debajo lo refresca `remote-refreshTab-action` (`refresh-tab`), no el cierre del popup».

Verificación: `./gradlew -q test --tests 'com.educaflow.views.*'` en verde con las vistas del Paso 10 y sin cambios en el resto del proyecto.

### Paso 12 — Menús y visibilidad

`secretariavirtual/menus/menus.xml`: se eliminan las líneas de `correos-menuitem` (y sus 3 hijas, `order="50"`) y de `sms-menuitem` (y sus 3 hijas, `order="55"`), y en el sitio de `correos-menuitem` se pegan las 4 líneas de `design/menus.xml` (`notificaciones-menuitem`, `order="50"`, con «Recibidas» `admins,users`, «Del centro» `users` y «Todas» `admins`).

`MenuSecurityServiceImpl.isVisible` (Modificar; el resto se conserva): se sustituye `case "correos-delCentro-menuitem" -> supervisor || user.tieneTipoUsuario(TipoUsuarioCodigo.ADMINISTRATIVO);` por `case "notificaciones-delCentro-menuitem" -> GestorNotificacionesUtil.esGestorEnAlgunCentro(user);`.

Verificación: `./gradlew -q test --tests '*Categoria10*'` y `MenuSecurityServiceImplTest`.

### Paso 13 — Seguridad (permisos)

`subsystem/notificaciones/data-init/input-config.xml`: `<xml-inputs priority="10">` con un único `<input file="auth-notificaciones.xml" root="auth">` y el `bind` de `permission` de `correos/data-init/input-config.xml` (name, object, condition, conditionParams, can/*). Sin binding de grupos.

`subsystem/notificaciones/data-init/input/auth-notificaciones.xml` — 8 permisos, todos `read="true"` y el resto `false`. Como `AuthSecurity` no recorre superclases, cada entidad lleva los suyos:

| Permiso | `object` | `condition` | `conditionParams` |
|---|---|---|---|
| `Notificacion.propio-destinatario` | `…notificaciones.db.Notificacion` | `self.dniDestinatario = ? and self.estado = 'ENVIADO'` | `__user__.dni` |
| `Correo.propio-destinatario` | `…db.Correo` | ídem | `__user__.dni` |
| `Sms.propio-destinatario` | `…db.Sms` | ídem | `__user__.dni` |
| `Adjunto.propio-destinatario` | `…db.Adjunto` | `self.correo.dniDestinatario = ? and self.correo.estado = 'ENVIADO'` | `__user__.dni` |
| `Notificacion.propio-centro-gestion` | `…db.Notificacion` | `self.centro.id IN (?)` | GESTOR |
| `Correo.propio-centro-gestion` | `…db.Correo` | `self.centro.id IN (?)` | GESTOR |
| `Sms.propio-centro-gestion` | `…db.Sms` | `self.centro.id IN (?)` | GESTOR |
| `Adjunto.propio-centro-gestion` | `…db.Adjunto` | `self.correo.centro.id IN (?)` | GESTOR |

GESTOR = `com.educaflow.subsystem.notificaciones.util.GestorNotificacionesUtil.idsCentrosGestionados(__user__)` — el único dueño de la clasificación (D4), sin ninguna copia JPQL.
`AuthSecurity.Condition` evalúa con `GroovyScriptHelper` cada `conditionParams` distinto de `__user__` (con el `__user__` ligado), la `ScriptPolicy` lo admite porque la clase lleva `@ScriptAllowed`, y `JPQLFilter` pasa la `List<Long>` tal cual al `IN (?)`; la lista nunca está vacía (centinela `-1L` en el dueño).
La expresión **MUST NOT** llevar comas: `AuthSecurity` parte `conditionParams` por «,».

`src/main/resources/data-init/input/auth.xml`, grupo `users`: las líneas `Correo.propio-destinatario`, `Correo.propio-centro-supervisor`, `Adjunto.propio-destinatario`, `Adjunto.propio-centro-supervisor` se sustituyen por los 8 permisos de la tabla; se quita el comentario `<!-- Correos -->` (sección vacía). El grupo `admins` no necesita ninguno (`AuthSecurity` no filtra al administrador).

Regla de acceso en lenguaje natural:
- **Administrador**: ve y opera todo (sin filtro de permisos); da de alta por «Todas»; reenvía (V-Notificacion-015 lo admite). No modifica ni borra (V-Notificacion-012/013, en el servicio).
- **Gestor del centro**: lee las notificaciones y adjuntos de los centros donde es gestor; reenvía las FALLIDO de esos centros (V-Notificacion-015). No da de alta: no ve «Todas» ni «Nueva notificación», y sus permisos son de solo lectura (`create="false"`), así que tampoco por REST.
- **Destinatario**: lee solo sus notificaciones ENVIADO (por DNI) y sus adjuntos, de cualquier centro.

Verificación: reset de BD y arranque; el log de importación de data-init sin errores.

### Paso 14 — Verificación final

`./run.sh` (compila, ejecuta tests unitarios, ArchUnit, tests de vistas, `cpdCheck`, `crapCheck` y arranca en el 8080). Debe terminar sin errores y la aplicación arrancar.

## Frontera de confianza — AllowProperties por acción

### `CorreoServiceImpl.reenviar` (invocado desde `CorreoController.reenviar`) — heredado de `NotificacionCanalServiceImpl`

Entidad: `Correo`. **Forma elegida**: `createDenyAllProperties`.
**Origen spec:** `Input AllowProperties` de la acción `Reenviar` de `entity-Notificacion.md` (ninguna).

| Campo | Origen | En whitelist | Justificación / Ubicación de la asignación |
|---|---|---|---|
| (todos) | — | **NO** | El reenvío actúa sobre `entidadOriginal` (BD); el estado lo cambia `enviar()` (R-Notificacion-003). |

### `SmsServiceImpl.reenviar` (invocado desde `SmsController.reenviar`) — heredado de `NotificacionCanalServiceImpl`

Entidad: `Sms`. **Forma elegida**: `createDenyAllProperties`. **Origen spec:** ídem. Misma tabla que el correo.

### `insert` de cada entidad (puerta REST `/ws/rest/<FQN>` y `save` de los forms)

No se invoca desde un `@CallMethod`, pero es la frontera real del alta y se documenta igual.

**`CorreoServiceImpl.allowPropertiesInsert`** (heredado; común + `allowPropertiesDelCanal`). Forma: `createAllowProperties`. Origen spec: `Input AllowProperties` de `Crear` de `entity-Correo.md`.

| Campo | Origen | En whitelist | Justificación / Ubicación de la asignación |
|---|---|---|---|
| `name`, `dniDestinatario`, `nombre`, `apellidos`, `centro`, `historialEstado` | cliente | sí | Comunes (base); `centro` e `historialEstado` validados en `validateInsert` (V-Notificacion-010/011). |
| `para`, `enCopia`, `enCopiaOculta`, `asunto`, `cuerpo` | cliente | sí | `allowPropertiesDelCanal` de Correo. |
| `adjuntos` → `nombreFichero`, `contenido` | cliente | sí (anidado) | Validados por `AdjuntoServiceImpl.validateInsert` en cascada; copiados por R-Correo-001. |
| `tipoNotificacion` | servidor | **NO** | R-Notificacion-001 (incondicional, desde `TipoNotificacion.deClase`). |
| `estado`, `fechaCreacion`, `numeroReintentos`, `fechaPrimerIntentoEnvio`, `fechaUltimoIntentoEnvio`, `fechaEnvio`, `descripcionUltimoFallo` | servidor | **NO** | R-Notificacion-001 en el alta; R-Notificacion-003 en el envío. |
| `destino`, `nombreExpediente` | servidor (lectura) | **NO** | Campo función / fórmula: el getter ignora cualquier valor asignado. |

**`SmsServiceImpl.allowPropertiesInsert`**: comunes + `telefono`, `mensaje` (cliente; `telefono` normalizado por R-Sms-001); el resto igual que el correo.
**`AdjuntoServiceImpl.allowPropertiesInsert`**: `nombreFichero`, `contenido`, `correo` (padre cliente, validado por V-Adjunto-001/006/007, k-secure-coding §3.6).
**`NotificacionServiceImpl.allowPropertiesInsert/Update/Remove`** y **`*.allowPropertiesUpdate/Remove`** de los canales y del adjunto: `createDenyAllProperties` (inmutables, sin borrado; alta genérica imposible).

### DTO de alta programática

No hay DTO: la alta programática (`VerificacionHelper`) parte de `NotificacionService.createCorreo()` y pasa por el mismo `CorreoService.validateInsert/insert`, donde R-Notificacion-001 vuelve a fijar todos los campos servidor.

## Trazabilidad Origen spec → V/R/U → ubicación

### V

| ID | Origen spec | Ubicación |
|---|---|---|
| V-Notificacion-001 | RES-Notificacion-001 | `NotificacionCanalServiceImpl.validateInsert`; cortesía: `Main@{Correo,Sms}-Local-validateSave-action` |
| V-Notificacion-002 | RES-Notificacion-002 | `NotificacionCanalServiceImpl.validateInsert` |
| V-Notificacion-003 | RES-Notificacion-014 | `NotificacionCanalServiceImpl.validateInsert`; cortesía: `Local-validateSave` |
| V-Notificacion-004 | VAL-Correo-002, VAL-Sms-002 | `NotificacionCanalServiceImpl.validateInsert` |
| V-Notificacion-005 | VAL-Correo-003, VAL-Sms-003 | `NotificacionCanalServiceImpl.validateInsert`; cortesía: `Local-validateSave` |
| V-Notificacion-006 | VAL-Correo-004, VAL-Sms-004 | `NotificacionCanalServiceImpl.validateInsert`; cortesía: `Local-validateSave` |
| V-Notificacion-007 | VAL-Correo-015, VAL-Sms-011 | `NotificacionCanalServiceImpl.validateInsert` |
| V-Notificacion-008 | VAL-Correo-016, VAL-Sms-012 | `NotificacionCanalServiceImpl.validateInsert` |
| V-Notificacion-009 | RES-Notificacion-013 | `NotificacionCanalServiceImpl.validateInsert`; cortesía: `Local-validateSave` |
| V-Notificacion-010 | VAL-Correo-014, VAL-Sms-010 | `NotificacionCanalServiceImpl.validateInsert` (texto `centroAjeno` del canal) |
| V-Notificacion-011 | RES-Notificacion-007 | `NotificacionCanalServiceImpl.validateInsert` (texto `estadoExpedienteInexistente` del canal) |
| V-Notificacion-012 | RES-Notificacion-004 | `NotificacionCanalServiceImpl.validateUpdate` + `update` (throw); `NotificacionServiceImpl.validateUpdate/update` (delegan); `allowPropertiesUpdate` deny-all |
| V-Notificacion-013 | RES-Notificacion-005 | `NotificacionCanalServiceImpl.validateRemove` + `remove` (throw); `NotificacionServiceImpl.validateRemove/remove` (delegan) |
| V-Notificacion-014 | VAL-Notificacion-001 | `NotificacionCanalServiceImpl.validateReenviar`; expuesta por `CorreoController/SmsController.validateReenviar` (`Main@{Correo,Sms}-Remote-validateReenviar-action`) |
| V-Notificacion-015 | VAL-Notificacion-002 | `NotificacionCanalServiceImpl.validateReenviar` → `GestorNotificacionesUtil.idsCentrosGestionados(user).contains(...)` |
| V-Notificacion-016 | RES-Notificacion-006, RES-Notificacion-008, RES-Notificacion-009, RES-Notificacion-010, RES-Notificacion-011, RES-Notificacion-012 | Garantía por construcción: los campos del envío están fuera de todo `AllowProperties` y solo los asignan, incondicionalmente, R-Notificacion-001 y R-Notificacion-003; `update` prohibido (V-Notificacion-012). Detalle: `rules/R-Notificacion-003.md` § Garantías |
| V-Notificacion-017 | — | `NotificacionServiceImpl.validateInsert` (puerta de validación) + `insert` (solo throw): no se da de alta una notificación genérica |
| V-Correo-001 | VAL-Correo-005 | `CorreoServiceImpl.validateDatosDelCanal`; cortesía: `Main@Correo-Local-validateSave-action` |
| V-Correo-002 | VAL-Correo-006 | `CorreoServiceImpl.validateDatosDelCanal` |
| V-Correo-003 | VAL-Correo-007 | `CorreoServiceImpl.validateDatosDelCanal` |
| V-Correo-004 | VAL-Correo-008 | `CorreoServiceImpl.validateDatosDelCanal` |
| V-Correo-005 | VAL-Correo-009 | `CorreoServiceImpl.validateDatosDelCanal` |
| V-Correo-006 | VAL-Correo-010 | `CorreoServiceImpl.validateDatosDelCanal`; cortesía: `Local-validateSave` |
| V-Correo-007 | VAL-Correo-011 | `CorreoServiceImpl.validateDatosDelCanal` |
| V-Correo-008 | VAL-Correo-017 | `CorreoServiceImpl.validateDatosDelCanal` |
| V-Correo-009 | VAL-Correo-012 | `CorreoServiceImpl.validateDatosDelCanal`; cortesía: `Local-validateSave` |
| V-Correo-010 | VAL-Correo-018 | `CorreoServiceImpl.validateDatosDelCanal` |
| V-Correo-011 | VAL-Correo-019 | `CorreoServiceImpl.validateDatosDelCanal` |
| V-Sms-001 | VAL-Sms-005 | `SmsServiceImpl.validateDatosDelCanal`; cortesía: `Main@Sms-Local-validateSave-action` |
| V-Sms-002 | VAL-Sms-006 | `SmsServiceImpl.validateDatosDelCanal` |
| V-Sms-003 | VAL-Sms-007 | `SmsServiceImpl.validateDatosDelCanal`; cortesía: `Local-validateSave` |
| V-Sms-004 | VAL-Sms-008 | `SmsServiceImpl.validateDatosDelCanal` |
| V-Adjunto-001 | RES-Adjunto-001 | `AdjuntoServiceImpl.validateInsert` |
| V-Adjunto-002 | RES-Adjunto-002 | `AdjuntoServiceImpl.validateInsert`; cliente: `Main@Correo.Adjunto-Local-validateSave-action` |
| V-Adjunto-003 | RES-Adjunto-003 | `AdjuntoServiceImpl.validateInsert`; cliente: `Local-validateSave` del modal (texto de RUI-…-formulario-adjunto-003) |
| V-Adjunto-004 | RES-Adjunto-004 | `AdjuntoServiceImpl.validateUpdate` + `update` (throw); `allowPropertiesUpdate` deny-all |
| V-Adjunto-005 | RES-Adjunto-005 | `AdjuntoServiceImpl.validateRemove` + `remove` (throw) |
| V-Adjunto-006 | VAL-Adjunto-001 | `AdjuntoServiceImpl.validateInsert` |
| V-Adjunto-007 | VAL-Adjunto-002 | `AdjuntoServiceImpl.validateInsert` |
| V-Adjunto-008 | VAL-Adjunto-003 | `AdjuntoServiceImpl.validateInsert` (+ límite de subida de la plataforma, D5) |
| V-Adjunto-009 | VAL-Adjunto-004 | `AdjuntoServiceImpl.validateInsert`; cliente: `Local-validateSave` del modal |
| V-Adjunto-010 | VAL-Adjunto-005 | `AdjuntoServiceImpl.validateInsert`; cliente: `Local-validateSave` del modal |
| V-Adjunto-011 | VAL-Adjunto-006 | `AdjuntoServiceImpl.validateInsert`; cliente: `Local-validateSave` del modal |
| V-Adjunto-012 | VAL-Adjunto-007 | `AdjuntoServiceImpl.validateInsert`; cliente: `Local-validateSave` del modal |
| V-Adjunto-013 | RES-Correo-002 | `AdjuntoServiceImpl.validateInsert` + `unique-constraint correo,nombreFichero` |

### R

| ID | Origen spec | Ubicación | Momento |
|---|---|---|---|
| R-Notificacion-001 | RN-Notificacion-001, CC-Notificacion-001, CC-Notificacion-004, RES-Notificacion-003, RES-Correo-001, RES-Sms-001 | `NotificacionCanalServiceImpl.fireActionRule_AsignarValoresIniciales` | Antes de `repository.save` (insert) |
| R-Notificacion-002 | RN-Notificacion-002 | `NotificacionCanalServiceImpl.fireActionRule_ProgramarEnvioAsincrono` desde `insert` | Después (tras el commit) |
| R-Notificacion-003 | RN-Notificacion-003, RN-Notificacion-004, RN-Notificacion-005, RN-Notificacion-006, RN-Correo-001, RN-Correo-003, RN-Correo-004, RN-Sms-002 | `NotificacionCanalServiceImpl.enviar` + `fireActionRule_RegistrarIntentoEnvio/MarcarEnvioCorrecto/MarcarEnvioFallido` + `enviarPorCanal` de cada canal | Transacción propia. Detalle: `design/rules/R-Notificacion-003.md` |
| R-Notificacion-004 | RN-Notificacion-007 | `NotificacionCanalServiceImpl.fireActionRule_ProgramarEnvioAsincrono` desde `reenviar` | Después (tras el commit) |
| R-Correo-001 | RN-Adjunto-001, RN-Adjunto-003 | `CorreoServiceImpl.fireActionRule_CopiarAdjuntos` (gancho `aplicarReglasDelCanalAntesDeGuardar`) | Antes de `repository.save` |
| R-Correo-002 | RN-Correo-002 | `VerificacionHelper.avisarDeSubsanacion` / `crearCorreoSubsanacion` / `estadoActual` (tramites/util) | En la transacción del evento de verificación |
| R-Sms-001 | RN-Sms-001, RES-Sms-002 | `SmsServiceImpl.fireActionRule_NormalizarTelefono` (gancho) | Antes de `repository.save` |
| R-Adjunto-001 | RN-Adjunto-002 | Estructural: los adjuntos se guardan en cascada con el único `repository.save(correo)` de `insert`, en la misma transacción | Antes de commit |
| CC destino | CC-Notificacion-002, CC-Correo-001, CC-Sms-001 | Campo función `destino` de `Notificacion` + `computeDestino()` en `Correo.xml`/`Sms.xml` | Lectura (se guarda al hacer flush) |
| CC expediente | CC-Notificacion-003 | `Notificacion.nombreExpediente` (`formula`) | Lectura |

### U

| ID | Origen spec | Ubicación |
|---|---|---|
| U-notificaciones-centro-001 | RUI-notificaciones-centro-listado-001 | `Centro@Notificacion-grid` `<hilite color="danger" if="estado == 'FALLIDO'">` |
| U-notificaciones-centro-002 | RUI-notificaciones-centro-formulario-correo-001 | `Centro@Correo-form` `fechaEnvio` `showIf="estado == 'ENVIADO'"` |
| U-notificaciones-centro-003 | RUI-notificaciones-centro-formulario-correo-002 | `Centro@Correo-form` `descripcionUltimoFallo` `showIf="estado == 'FALLIDO'"` |
| U-notificaciones-centro-004 | RUI-notificaciones-centro-formulario-correo-003 | `Centro@Correo-form` `btnReenviar` `showIf="estado == 'FALLIDO'"` |
| U-notificaciones-centro-005 | RUI-notificaciones-centro-formulario-correo-004 | `CorreoController.reenviar` → `setNotify` |
| U-notificaciones-centro-006 | RUI-notificaciones-centro-formulario-correo-005 | `Centro@Correo-form` panel `Intentos` `showIf="estado != 'PENDIENTE'"` |
| U-notificaciones-centro-007 | RUI-notificaciones-centro-formulario-correo-006 | `Centro@Correo-form` `historialEstado` `showIf="historialEstado != null"` |
| U-notificaciones-centro-008 | RUI-notificaciones-centro-formulario-sms-001 | `Centro@Sms-form` `fechaEnvio` `showIf` ENVIADO |
| U-notificaciones-centro-009 | RUI-notificaciones-centro-formulario-sms-002 | `Centro@Sms-form` `descripcionUltimoFallo` `showIf` FALLIDO |
| U-notificaciones-centro-010 | RUI-notificaciones-centro-formulario-sms-003 | `Centro@Sms-form` `btnReenviar` `showIf` FALLIDO |
| U-notificaciones-centro-011 | RUI-notificaciones-centro-formulario-sms-004 | `SmsController.reenviar` → `setNotify` |
| U-notificaciones-centro-012 | RUI-notificaciones-centro-formulario-sms-005 | `Main@Sms-Local-confirmarReenvio-action` (alert) vía `Main@Sms-btnReenviar-action`, al que delega `Centro@Sms-btnReenviar-action` |
| U-notificaciones-centro-013 | RUI-notificaciones-centro-formulario-sms-006 | `Centro@Sms-form` panel `Intentos` `showIf="estado != 'PENDIENTE'"` |
| U-notificaciones-centro-014 | RUI-notificaciones-centro-formulario-sms-007 | `Centro@Sms-form` `historialEstado` `showIf="historialEstado != null"` |
| U-notificaciones-recibidas-001 | RUI-notificaciones-recibidas-formulario-correo-001 | `Mis@Correo-form` `enCopia` `showIf="enCopia"` |
| U-notificaciones-recibidas-002 | RUI-notificaciones-recibidas-formulario-correo-002 | `Mis@Correo-form` panel-related `adjuntos` `showIf="adjuntos && adjuntos.length > 0"` (equivale al «al cargar»: el form es de solo lectura y la colección no cambia) |
| U-notificaciones-todas-001 | RUI-notificaciones-todas-eleccion-canal-001 | `Eleccion@Notificacion-form` `tipoNotificacion` sin valor por defecto (el dominio no declara `default`) |
| U-notificaciones-todas-002 | RUI-notificaciones-todas-eleccion-canal-002 | `Eleccion@Notificacion-form` `btnContinuar` `readonlyIf="tipoNotificacion == null"` |
| U-notificaciones-todas-003 | RUI-notificaciones-todas-formulario-correo-001 | `Main@Correo-form` `tipoNotificacion` `readonly` + `onNew` → `Main@Correo-Remote-prepararAlta-action` (`NotificacionController.prepararAlta`) |
| U-notificaciones-todas-004 | RUI-notificaciones-todas-formulario-correo-002 | `Main@Correo-form` panel `Correo` `readonlyIf="(id != null) \|\| (cid != null)"` |
| U-notificaciones-todas-005 | RUI-notificaciones-todas-formulario-correo-003 | `Main@Correo-form` panel `Envio` `showIf="(id != null) \|\| (cid != null)"` |
| U-notificaciones-todas-006 | RUI-notificaciones-todas-formulario-correo-004 | `Main@Correo-form` `fechaEnvio` `showIf` ENVIADO |
| U-notificaciones-todas-007 | RUI-notificaciones-todas-formulario-correo-005 | `Main@Correo-form` `descripcionUltimoFallo` `showIf` FALLIDO |
| U-notificaciones-todas-008 | RUI-notificaciones-todas-formulario-correo-006 | `Main@Correo-form` paneles `buttonsAlta` / `buttonsFallido` / `buttonsDetalle` |
| U-notificaciones-todas-009 | RUI-notificaciones-todas-formulario-correo-007 | `Main@Correo-form` panel `buttonsFallido` (`btnReenviar`) |
| U-notificaciones-todas-010 | RUI-notificaciones-todas-formulario-correo-008 | `Main@Correo-Local-validateSave-action` en `Main@Correo-btnSave-action` |
| U-notificaciones-todas-011 | RUI-notificaciones-todas-formulario-correo-009 | `CorreoController.reenviar` → `setNotify` |
| U-notificaciones-todas-012 | RUI-notificaciones-todas-formulario-correo-010 | `Main@Correo-form` `historialEstado` `domain="self.expediente.centro = :centro"` |
| U-notificaciones-todas-013 | RUI-notificaciones-todas-formulario-correo-011 | `Main@Correo-onChange-centro-action` → `Main@Correo-set-historialEstado-null-action` |
| U-notificaciones-todas-014 | RUI-notificaciones-todas-formulario-correo-012 | `Main@Correo-form` `requiredIf="(id == null) && (cid == null)"` en motivo, centro, DNI, nombre, apellidos, para, asunto, cuerpo |
| U-notificaciones-todas-015 | RUI-notificaciones-todas-listado-adjuntos-001 | `Main@Correo-form`: `adjuntos` (con «Añadir adjunto») solo en alta y `adjuntosConsulta` (`canNew="false"`) en detalle |
| U-notificaciones-todas-016 | RUI-notificaciones-todas-formulario-adjunto-001 | `Main@Correo.Adjunto-onNew-action` → `set-correo-parent-action` |
| U-notificaciones-todas-017 | RUI-notificaciones-todas-formulario-adjunto-002 | `nombreFichero` `required="true"` + `Main@Correo.Adjunto-Local-validateSave-action` |
| U-notificaciones-todas-018 | RUI-notificaciones-todas-formulario-adjunto-003 | `contenido` `required="true"` + `Local-validateSave` del modal («Debe adjuntar el fichero») |
| U-notificaciones-todas-019 | RUI-notificaciones-todas-formulario-adjunto-004 | Límite de subida de la plataforma `data.upload.max-size = 10` (D5, cambio de spec APLICADO) |
| U-notificaciones-todas-020 | RUI-notificaciones-todas-formulario-adjunto-005 | `Main@Correo.Adjunto-Local-validateSave-action` (regex `/[\/\\\p{Cntrl}]/`) |
| U-notificaciones-todas-021 | RUI-notificaciones-todas-formulario-adjunto-006 | Modal de adjunto: `btnDelete` solo en el panel `buttonsEdicion` (`(id != null) \|\| (cid != null)`) |
| U-notificaciones-todas-022 | RUI-notificaciones-todas-formulario-sms-001 | `Main@Sms-form` `tipoNotificacion` `readonly` + `onNew` → `Main@Sms-Remote-prepararAlta-action` |
| U-notificaciones-todas-023 | RUI-notificaciones-todas-formulario-sms-002 | `Main@Sms-form` panel `Sms` `readonlyIf` |
| U-notificaciones-todas-024 | RUI-notificaciones-todas-formulario-sms-003 | `Main@Sms-form` panel `Envio` `showIf` |
| U-notificaciones-todas-025 | RUI-notificaciones-todas-formulario-sms-004 | `Main@Sms-form` `fechaEnvio` `showIf` ENVIADO |
| U-notificaciones-todas-026 | RUI-notificaciones-todas-formulario-sms-005 | `Main@Sms-form` `descripcionUltimoFallo` `showIf` FALLIDO |
| U-notificaciones-todas-027 | RUI-notificaciones-todas-formulario-sms-006 | `Main@Sms-form` paneles `buttonsAlta` / `buttonsFallido` / `buttonsDetalle` |
| U-notificaciones-todas-028 | RUI-notificaciones-todas-formulario-sms-007 | `Main@Sms-form` panel `buttonsFallido` |
| U-notificaciones-todas-029 | RUI-notificaciones-todas-formulario-sms-008 | `Main@Sms-Local-validateSave-action` en `Main@Sms-btnSave-action` |
| U-notificaciones-todas-030 | RUI-notificaciones-todas-formulario-sms-009 | `Main@Sms-form` `telefono` `widget="phone" x-only-countries="ES"` |
| U-notificaciones-todas-031 | RUI-notificaciones-todas-formulario-sms-010 | `SmsController.reenviar` → `setNotify` |
| U-notificaciones-todas-032 | RUI-notificaciones-todas-formulario-sms-011 | `Main@Sms-form` `historialEstado` `domain="self.expediente.centro = :centro"` |
| U-notificaciones-todas-033 | RUI-notificaciones-todas-formulario-sms-012 | `Main@Sms-onChange-centro-action` → `Main@Sms-set-historialEstado-null-action` |
| U-notificaciones-todas-034 | RUI-notificaciones-todas-formulario-sms-013 | `Main@Sms-form` `requiredIf` en motivo, centro, DNI, nombre, apellidos, teléfono, mensaje |
| U-notificaciones-todas-035 | RUI-notificaciones-todas-formulario-sms-014 | `Main@Sms-Local-confirmarReenvio-action` (alert) en `Main@Sms-btnReenviar-action` |

## Tests

- **Tests unitarios** (JUnit + Mockito): descritos en `test-unit-desc.md` (lo materializa una fase posterior del pipeline). Deben cubrir, como piden las guías, `createCorreo`/`createSms`, que el cliente no puede dictar `tipoNotificacion` (un `Correo` con `tipoNotificacion = SMS` se guarda como `CORREO`), y que `NotificacionController` abre `subsysNotificaciones.<Variante>@<Clase>-form` según el tipo de la fila.
- **Tests E2E**: `test-e2e-desc.md` (T-001…T-076, carpeta `src/test/e2e/subsystem/notificaciones/`).

## Eliminaciones declaradas

| Fichero | Elemento eliminado | Justificación (ID de spec) |
|---|---|---|
| `subsystem/correos/**` | Subsistema completo (dominios `Correo`, `Adjunto`, enum `EstadoCorreo`, servicios, controlador, módulo, vistas, data-init) | `specification.md` § Objetivo («sustituye a los dos subsistemas actuales… que desaparecen») |
| `subsystem/sms/**` | Subsistema completo (dominio `Sms`, enum `EstadoSms`, servicio, controlador, módulo, vistas, data-init) | § Objetivo |
| `secretariavirtual/menus/menus.xml` | `correos-menuitem`, `correos-recibidos-menuitem`, `correos-delCentro-menuitem`, `correos-todos-menuitem`, `sms-menuitem`, `sms-recibidos-menuitem`, `sms-delCentro-menuitem`, `sms-todos-menuitem` | § Objetivo, § Pantallas (menú «Notificaciones») |
| `MenuSecurityServiceImpl` | `case "correos-delCentro-menuitem"` | § Objetivo; sustituido por `notificaciones-delCentro-menuitem` |
| `src/main/resources/data-init/input/auth.xml` | `Correo.propio-destinatario`, `Correo.propio-centro-supervisor`, `Adjunto.propio-destinatario`, `Adjunto.propio-centro-supervisor` del grupo `users`; comentario `<!-- Correos -->` | § Recursos y datos iniciales («sustituyen a los de los antiguos subsistemas») |
| `CorreoServiceImpl` (antiguo) | `validateEnviarCorreo(Long)` (privado, siempre vacío) | Sin regla: `enviar` no recibe datos del cliente (R-Notificacion-003) |

## Tests E2E supersedidos

Todos los de las dos carpetas: sus menús («Correos», «SMS»), pantallas y grids desaparecen (§ Objetivo) y cada comportamiento lo cubre un escenario de esta spec (D6).

- `src/test/e2e/subsystem/correos/t-001-alta-de-un-correo-que-se-envia-con-exito.spec.ts` — ESC-001
- `src/test/e2e/subsystem/correos/t-002-alta-de-un-correo-con-adjunto.spec.ts` — ESC-002
- `src/test/e2e/subsystem/correos/t-003-alta-sin-el-dni-del-destinatario.spec.ts` — ESC-007
- `src/test/e2e/subsystem/correos/t-004-alta-sin-destinatario-en-el-para.spec.ts` — ESC-011
- `src/test/e2e/subsystem/correos/t-005-alta-sin-asunto.spec.ts` — ESC-015
- `src/test/e2e/subsystem/correos/t-006-alta-sin-cuerpo.spec.ts` — ESC-017
- `src/test/e2e/subsystem/correos/t-007-alta-sin-centro.spec.ts` — ESC-018
- `src/test/e2e/subsystem/correos/t-008-alta-sin-el-nombre.spec.ts` — ESC-009
- `src/test/e2e/subsystem/correos/t-009-alta-sin-los-apellidos.spec.ts` — ESC-010
- `src/test/e2e/subsystem/correos/t-010-alta-con-adjunto-sin-nombre-de-fichero.spec.ts` — ESC-019
- `src/test/e2e/subsystem/correos/t-011-alta-con-adjunto-sin-contenido.spec.ts` — ESC-020
- `src/test/e2e/subsystem/correos/t-012-alta-con-para-de-formato-invalido.spec.ts` — ESC-012
- `src/test/e2e/subsystem/correos/t-013-alta-con-el-dni-del-destinatario-invalido.spec.ts` — ESC-008
- `src/test/e2e/subsystem/correos/t-014-alta-con-dos-adjuntos-con-el-mismo-nombre-de-fichero.spec.ts` — ESC-021
- `src/test/e2e/subsystem/correos/t-015-alta-con-el-asunto-demasiado-largo.spec.ts` — ESC-016
- `src/test/e2e/subsystem/correos/t-016-alta-con-en-copia-de-formato-invalido.spec.ts` — ESC-013
- `src/test/e2e/subsystem/correos/t-017-alta-con-en-copia-oculta-de-formato-invalido.spec.ts` — ESC-014
- `src/test/e2e/subsystem/correos/t-019-el-boton-reenviar-no-aparece-si-el-correo-no-ha-fallado.spec.ts` — ESC-039
- `src/test/e2e/subsystem/correos/t-020-el-supervisor-solo-ve-los-correos-de-su-centro.spec.ts` — ESC-041
- `src/test/e2e/subsystem/correos/t-022-el-supervisor-descarga-el-adjunto-de-un-correo-de-su-centro.spec.ts` — ESC-049
- `src/test/e2e/subsystem/correos/t-023-el-destinatario-consulta-un-correo-enviado-con-exito-y-descarga-su-adjunto.spec.ts` — ESC-053
- `src/test/e2e/subsystem/correos/t-025-el-destinatario-no-ve-un-correo-enviado-con-exito-a-otra-persona.spec.ts` — ESC-054
- `src/test/e2e/subsystem/correos/t-026-el-supervisor-de-dos-centros-ve-los-correos-de-ambos.spec.ts` — ESC-046
- `src/test/e2e/subsystem/sms/t-001-alta-de-un-sms-y-resultado-del-envio.spec.ts` — ESC-023
- `src/test/e2e/subsystem/sms/t-002-alta-con-el-telefono-escrito-ya-en-formato-internacional.spec.ts` — ESC-024
- `src/test/e2e/subsystem/sms/t-003-alta-sin-el-dni-del-destinatario.spec.ts` — ESC-025
- `src/test/e2e/subsystem/sms/t-004-alta-con-el-dni-con-la-letra-incorrecta.spec.ts` — ESC-026
- `src/test/e2e/subsystem/sms/t-005-alta-sin-nombre-ni-apellidos.spec.ts` — ESC-027
- `src/test/e2e/subsystem/sms/t-006-alta-sin-telefono.spec.ts` — ESC-028
- `src/test/e2e/subsystem/sms/t-007-alta-con-un-telefono-que-no-es-un-movil-de-espana.spec.ts` — ESC-029
- `src/test/e2e/subsystem/sms/t-008-alta-sin-mensaje.spec.ts` — ESC-030
- `src/test/e2e/subsystem/sms/t-009-mensaje-sin-acentos-en-el-limite-de-un-sms.spec.ts` — ESC-031
- `src/test/e2e/subsystem/sms/t-010-mensaje-con-acentos-en-el-limite-de-un-sms.spec.ts` — ESC-032
- `src/test/e2e/subsystem/sms/t-011-alta-sin-centro.spec.ts` — ESC-033
- `src/test/e2e/subsystem/sms/t-012-el-administrador-consulta-los-sms-de-varios-centros.spec.ts` — ESC-034
- `src/test/e2e/subsystem/sms/t-013-un-sms-ya-creado-no-se-puede-modificar-ni-borrar.spec.ts` — ESC-068
- `src/test/e2e/subsystem/sms/t-014-el-supervisor-ve-solo-los-sms-de-su-centro.spec.ts` — ESC-041
- `src/test/e2e/subsystem/sms/t-015-el-administrativo-ve-los-sms-de-su-centro.spec.ts` — ESC-042
- `src/test/e2e/subsystem/sms/t-016-reenvio-desde-del-centro-por-el-supervisor.spec.ts` — ESC-050
- `src/test/e2e/subsystem/sms/t-017-reenvio-desde-todos-por-el-administrador.spec.ts` — ESC-040
- `src/test/e2e/subsystem/sms/t-018-el-destinatario-ve-sus-sms-enviados-y-no-los-de-otro.spec.ts` — ESC-052, ESC-054
- `src/test/e2e/subsystem/sms/t-019-cancelar-el-alta-de-un-sms.spec.ts` — ESC-060
- `src/test/e2e/subsystem/sms/t-020-datos-del-envio-segun-el-estado-en-el-detalle.spec.ts` — ESC-036
- `src/test/e2e/subsystem/sms/t-021-el-supervisor-de-dos-centros-ve-los-sms-de-ambos-y-filtra-por-centro.spec.ts` — ESC-046
- `src/test/e2e/subsystem/sms/t-022-el-supervisor-no-puede-dar-de-alta-sms.spec.ts` — ESC-048
- `src/test/e2e/subsystem/sms/t-023-reenvio-por-el-administrativo.spec.ts` — ESC-074
- `src/test/e2e/subsystem/sms/t-024-el-destinatario-ve-un-sms-enviado-desde-otro-centro.spec.ts` — ESC-055
- `src/test/e2e/subsystem/sms/t-025-un-usuario-sin-cargo-de-gestion-no-ve-sms-de-los-centros.spec.ts` — ESC-047

(Con ellas se borran sus `.desc.md` hermanos.)

## Reglas del spec descartadas

Ninguna: todas las `RES-`/`VAL-`/`RN-`/`RUI-`/`CC-` están ubicadas en la matriz. RUI-notificaciones-todas-formulario-adjunto-004 queda ubicada en el límite de la plataforma con su texto (cambio de spec APLICADO, D5).

## Notas y supuestos

- **Patrones nuevos** (D1, D2, D3): servicio base genérico para las subclases de una entidad JOINED; campo función polimórfico + enum-catálogo de subclases; form de subclase abierto por código en popup, con la ampliación de `VAR-7.2` del Paso 11. Las recetas que faltan están apuntadas en `decisiones.md`.
- **Cómo se añade un canal** (prueba del segundo desarrollador): entidad `X extends Notificacion` con `computeDestino()`; ítem `X` y su `case` en `TipoNotificacion`; `XServiceImpl extends NotificacionCanalServiceImpl<X>` (el compilador pide los cinco ganchos) y `XService extends NotificacionCanalService<X>`; forms `Main@X-form` (con su `btnSave` terminando en `save` → `remote-refreshTab-action` → `close`, y su `btnReenviar` en `remote-refreshTab-action`), `Centro@X-form` (cuyo `btnReenviar` es solo el grupo de `Main@X`), `Mis@X-form`; `XController` con `reenviar`/`validateReenviar` como `CorreoController` (incluidos `@Transactional` y `setReload(true)`, Paso 8); permisos `X.propio-*`. Los listados, la elección de canal, el envío, el reenvío y la apertura de forms no se tocan.
- **Desviaciones de las guías**:
  - Los E2E de `correos` y `sms` no se mueven: se supersedan (D6).
  - Los controladores de reenvío siguen siendo dos (`CorreoController`, `SmsController`) porque su aviso es distinto por canal y las guías piden moverlos sin cambios; el código común de los servicios sí se extrae (D1).
  - Los forms de correos y SMS no se reutilizan tal cual cambiando solo el prefijo y el modelo: se re-maquetan para encajar los campos nuevos de la spec y para cumplir el ASCII Layout de `k-vistas/forms.md` (p. ej. `Mis@Sms-form`, `Centro@Correo-form` con `asunto` a colSpan 10), y sus botones cierran con `close` porque se abren en popup (D3).
  - `CorreoServiceImpl` pasa a inyectar `Provider<MailSender>` (como ya hacía SMS con `SmsSender`), para que una instalación sin servidor de correo deje el correo FALLIDO (RN-Correo-003) en lugar de fallar al crear el servicio.
- **Mensajes «de su canal»**: RES-Notificacion-007 tiene textos distintos por canal («El historial de estado indicado no existe» / «El estado del expediente indicado no existe») aunque el campo ahora es común y se titula «Estado del expediente»; se respeta la spec.
- **Refresco del listado tras el alta** (D3, resuelto tras la depuración de T-001): el alta se abre en popup **sobre** el popup de la elección de canal, así que el refresco de cierre de popup de Axelor iría a la elección (ya cerrada) y no al listado, y el respaldo `__onPopupReload` tampoco se instala porque hay un popup abierto. Por eso el `btnSave` de `Main@Correo-form`/`Main@Sms-form` lleva entre `save` y `close` la acción global `remote-refreshTab-action` (`DefaultModelController.refreshTab`, `setSignal("refresh-tab")`, que en `axelor-front` va a `getActiveTabId(-1)`, la pestaña ignorando los popups). Un canal nuevo la reutiliza tal cual.
- **Reenvío con el form en popup** (D3, resuelto al diseñar): `refresh-tab` despacha `tab:refresh` a la pestaña activa (el listado de debajo), no al popup, mientras que `setReload(true)` recarga el form del popup; por eso `CorreoController.reenviar`/`SmsController.reenviar` responden `setReload(true)` y el `btnReenviar` de `Main@` termina en `remote-refreshTab-action`, la misma acción que el alta (nota anterior); el de `Centro@` delega en él.
- **`destino` en la base** (D2): `Notificacion.computeDestino()` lanza `IllegalStateException`. El único sitio donde existe una `Notificacion` «genérica» en memoria es el form de elección de canal, y `NotificacionController.continuarAlta` lee `tipoNotificacion` del contexto sin construir el bean ni pedir su destino; si en la depuración apareciera algún camino del framework que serializa ese registro nuevo completo, se resuelve en ese camino, no relajando la base a `return null`.
- **Selector del estado del expediente** (D8): `HistorialEstado` no tiene nombre de presentación y su dominio vive en `expedientes`, que este diseño no modifica, así que sin más el selector solo mostraba ids.
  Se resuelve en la vista: el `historialEstado` de `Main@Correo-form`/`Main@Sms-form` lleva, además del `domain` por centro, `target-name="nameState"`, `canSuggest="false"` y las vistas de referencia `subsysExpedientes.Ref@HistorialEstado-grid`/`-form` de `subsystem/expedientes/views/Ref-HistorialEstado.xml` (vista nueva de solo lectura en el módulo dueño de la entidad, por VAR-1.2(b); única excepción a README §4.2, autorizada por el usuario, D8); el de `Centro@Correo-form`/`Centro@Sms-form` lleva `target-name="nameState"` y las mismas vistas de referencia.
  El usuario elige en el buscador, que muestra número de expediente, expediente, fase, estado y fecha, y el campo enseña el estado («Pendiente de verificación»); la columna «Expediente» de los listados sigue saliendo de `nombreExpediente`.
- **Puerta REST de alta para gestores**: los permisos de los gestores son de solo lectura (`create="false"`), así que un gestor no puede dar de alta por REST; el administrador sí (no tiene filtro de permisos) y pasa por las mismas validaciones del servicio.
- **Duplicados de direcciones** (V-Correo-010): se comparan tras `trim` y en minúsculas (una misma dirección escrita con otra capitalización es la misma).
- **`listarCorreosEnFail`** no tiene uso en producción; se conserva porque lo piden las guías.
- **Frozen store de ArchUnit**: al desaparecer `correos.service.CorreoService.enviarCorreo`, la violación congelada de C23 desaparece sola.
