---
type: implementation-task
---

# Tarea 04 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas
- k-secure-coding
- k-code-quality
- k-i18n

Esta tarea implementa el **servicio Correo** completo: la interfaz, su DTO de alta programática, la implementación y sus colaboradores auxiliares (`CorreoMailFactory`, `ResultadoEnvio`).

**Nota de contrato fijo:** los XML de dominios (`Correo.xml`, `AdjuntoCorreo.xml`) y de vistas ya están copiados en `src/main/...` y son contrato fijo. Las firmas Java deben coincidir con las entidades generadas y con las acciones declaradas en las vistas. **NO** regenerar ni editar esos XML; si detectas un error en un XML, DETENTE y notifica.

Filas de la tabla "Ficheros a crear o modificar":

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/correos/service/CorreoService.java` | Crear | k-sistemas (servicios.md) | Interfaz `extends ModelService<Correo>` + tripletas de acciones propias. |
| `subsystem/correos/service/CorreoInsertDTO.java` | Crear | k-sistemas, k-secure-coding | `record` DTO para el alta programática. |
| `subsystem/correos/service/impl/CorreoServiceImpl.java` | Crear | k-sistemas, k-secure-coding | Implementación `extends DefaultModelService<Correo>`. |
| `subsystem/correos/service/impl/CorreoMailFactory.java` | Crear | k-code-quality | Construye el `Mail`/`Attach` desde un `Correo` (ver `rules/R-Correo-006.md`). |
| `subsystem/correos/service/impl/ResultadoEnvio.java` | Crear | k-code-quality | `record` interno del resultado de un intento (ver `rules/R-Correo-006.md`). |

---

## Paso 2 — Servicios

### `CorreoService` (interfaz) — `extends ModelService<Correo>`

```java
public interface CorreoService extends ModelService<Correo> {
    // Alta programática (otro subsistema). El DTO ES la whitelist (k-secure-coding §3.5).
    Correo insert(CorreoInsertDTO dto);
    Optional<BusinessMessages> validateInsert(CorreoInsertDTO dto);

    // Reenviar (FALLIDO -> PENDIENTE) — invocada desde @CallMethod del controlador.
    Correo reenviar(Correo correo, Correo correoOriginal);
    Optional<BusinessMessages> validateReenviar(Correo correo, Correo correoOriginal);
    AllowProperties allowPropertiesReenviar();

    // Envío asíncrono — invocada por el Job (NO desde @CallMethod -> sin allowProperties).
    void enviarCorreosPendientes();

    // Lectura: propone el email del User con ese DNI (campo cliente, editable). U-correo-001 / R-Correo-004.
    String proponerEmailPorDni(String dni);
}
```

`insert(Correo)` / `update` / `remove` / `validateInsert(Correo)` / `validateUpdate` / `allowPropertiesInsert` se **sobrescriben** en la `*Impl` (añaden lógica real); **NO** se re-declaran en la interfaz (vienen de `ModelService<T>`).

### `CorreoServiceImpl` — firmas + comentarios (SIN cuerpos)

Bloques en orden: (1) acciones, (2) validación, (3) allowProperties, (4) action rules, (5) otras.

```java
public class CorreoServiceImpl extends DefaultModelService<Correo> implements CorreoService {

    @Inject private MailSender mailSender;                    // NO ModelService -> @Inject + binding en CorreosModule
    @Inject private CorreoMailFactory correoMailFactory;      // colaborador de construcción del Mail
    @Inject private ModelServiceFactory modelServiceFactory;  // por si hay que resolver otro ModelService

    public CorreoServiceImpl(Class<Correo> model, Repository<Correo> repository) { super(model, repository); }

    /* ===== (1) ACCIONES ===== */

    // insert(Correo) — alta MANUAL (Administrador, vía REST /ws/rest). Sobrescrito porque dispara R-Correo-001 y R-Correo-002.
    //   Cuerpo: validateInsert(correo).ifPresent(throwIfInvalid); fireActionRule_InicializarCorreo(correo); fireActionRule_AltaManualSinCentro(correo); return repository.save(correo);
    //   MUST validar como PRIMERA línea: al persistir con repository (NUNCA super.insert) nadie valida por ti.
    @Override public Correo insert(Correo correo);

    // insert(CorreoInsertDTO) — alta PROGRAMÁTICA. validateInsert(dto).ifPresent(throwIfInvalid) como primera línea, construye el Correo,
    //   dispara R-Correo-001 y R-Correo-003 (centro + referencia del invocador), y persiste con repository.save(correo).
    @Override public Correo insert(CorreoInsertDTO dto);

    // update(Correo,Correo) — INMUTABLE: lanza UnsupportedOperationException incondicional (k-secure-coding §9.2).
    @Override public Correo update(Correo correo, Correo correoOriginal);

    // remove(Correo) — R-Correo-008: validateRemove(correo).ifPresent(throwIfInvalid); borra en cascada sus AdjuntoCorreo (borrado de hijos) ANTES de repository.remove(correo).
    @Override public void remove(Correo correo);

    // reenviar — validateReenviar(...).ifPresent(throwIfInvalid) primera línea; dispara R-Correo-005 (estado=PENDIENTE) y persiste con repository.save.
    @Override public Correo reenviar(Correo correo, Correo correoOriginal);

    // enviarCorreosPendientes — envío asíncrono. R-Correo-006 + R-Correo-007.
    //   Diseño detallado en design/rules/R-Correo-006.md.
    //   Recupera findByEstado(PENDIENTE) y procesa cada correo AISLADO (transacción + try/catch por correo;
    //   NUNCA @Transactional global): fireActionRule_RegistrarIntento -> correoMailFactory.build -> mailSender.send
    //   -> fireActionRule_RegistrarResultadoEnvio. Logs sin datos sensibles (id+estado, CRLF saneado).
    @Override public void enviarCorreosPendientes();

    // proponerEmailPorDni — lectura: devuelve el email del User con ese DNI (o null). Sin persistencia.
    //   Resuelve vía finder findByDni del User (ver Paso 4 / modificación de common/User.xml).
    @Override public String proponerEmailPorDni(String dni);

    /******** Métodos de Validación ********/

    // validateInsert(Correo) — V-Correo-001..004 (campos cliente obligatorios) +
    //   V-Correo-006 (rechaza si referenciaHistorialEstadoExpediente llega NO nula por REST: solo asignable programáticamente).
    @Override public Optional<BusinessMessages> validateInsert(Correo correo);

    // validateInsert(CorreoInsertDTO) — validación de los campos del DTO del alta programática.
    @Override public Optional<BusinessMessages> validateInsert(CorreoInsertDTO dto);

    // validateUpdate(Correo,Correo) — V-Correo-005 + V-Correo-006: SIEMPRE rechaza (entidad inmutable, k-secure-coding §9.2).
    //   Mensaje debe transmitir: el correo ya creado no admite modificación de sus datos de envío.
    @Override public Optional<BusinessMessages> validateUpdate(Correo correo, Correo correoOriginal);

    // validateReenviar — V-Correo-007: rechaza si estado != FALLIDO.
    //   Mensaje debe transmitir: el estado actual recibido y que solo se reenvía en FALLIDO.
    //   Además comprueba autorización (solo Administrador) con SecurityUtil (defensa para Vía B; nunca en el controlador).
    @Override public Optional<BusinessMessages> validateReenviar(Correo correo, Correo correoOriginal);

    /******** AllowProperties ********/

    // allowPropertiesInsert — WHITELIST solo de campos cliente: asunto, cuerpo, dniDestinatario, emailDestinatario,
    //   adjuntos (con sub-whitelist nombreFichero, contenido). Los servidor quedan FUERA. Ver §Frontera de confianza.
    @Override public AllowProperties allowPropertiesInsert();

    // allowPropertiesReenviar — whitelist VACÍA (Map.of()): reenviar no acepta ningún campo del cliente.
    @Override public AllowProperties allowPropertiesReenviar();

    /******** Action Rules ********/

    // R-Correo-001: estado=PENDIENTE, fechaCreacion=now, numeroIntentos=0, fechaEnvio/fechaUltimoIntento/motivoUltimoFallo=null (INCONDICIONAL, sin if==null). k-secure-coding §3.3.
    private void fireActionRule_InicializarCorreo(Correo correo);
    // R-Correo-002: centro=null en alta manual (INCONDICIONAL).
    private void fireActionRule_AltaManualSinCentro(Correo correo);
    // R-Correo-003: centro=dto.centro(); referenciaHistorialEstadoExpediente=dto.referenciaHistorial() (INCONDICIONAL, alta programática).
    private void fireActionRule_AltaProgramaticaCentroYReferencia(Correo correo, CorreoInsertDTO dto);
    // R-Correo-005: estado=PENDIENTE (reenvío, INCONDICIONAL).
    private void fireActionRule_ReactivarCorreo(Correo correo);
    // R-Correo-006 / R-Correo-007 — ver design/rules/R-Correo-006.md.
    public void fireActionRule_RegistrarIntento(Correo correo);
    public void fireActionRule_RegistrarResultadoEnvio(Correo correo, boolean exito, String motivo);

    /******** Otras funciones ********/
    // (la construcción del Mail vive en CorreoMailFactory, no aquí)
}
```

> **R-Correo-004 (proponer email por DNI)** NO asigna un campo `servidor`: `emailDestinatario` es `cliente` (el Administrador lo confirma/edita). Se materializa como **U-correo-001** vía `CorreoController.onChangeDni` → `proponerEmailPorDni` → `setValue`. No tiene `fireActionRule_`.

`CorreoMailFactory` y `ResultadoEnvio`: ver `design/rules/R-Correo-006.md`.

### `CorreoInsertDTO` (alta programática)

```java
public record CorreoInsertDTO(
        String asunto, String cuerpo, String dniDestinatario, String emailDestinatario,
        Centro centro,                                      // servidor: lo dicta el invocador (R-Correo-003)
        HistorialEstado referenciaHistorial,                // opcional, externo (com.educaflow.subsystem.expedientes.db.HistorialEstado)
        List<AdjuntoCorreoInsertDTO> adjuntos               // opcional
) { /* requireNonNull de los obligatorios: asunto, cuerpo, dniDestinatario, emailDestinatario, centro */ }
```
El DTO **es** la whitelist (k-secure-coding §3.5): no pasa por REST ni por `AllowProperties`. `centro` y `referenciaHistorial` están justificados porque los aporta deliberadamente el subsistema invocador.

---

## Colaboradores auxiliares — detalle en `design/rules/R-Correo-006.md`

Consulta `.sdd/drafts/2026-05-21_20-14_correos/design/rules/R-Correo-006.md` para el detalle completo de `enviarCorreosPendientes`, `CorreoMailFactory`, `ResultadoEnvio`, el aislamiento transaccional por correo y la tabla de errores. Resumen de las clases auxiliares:

**`CorreoMailFactory`** — colaborador que traduce un `Correo` (entidad de dominio) al `Mail`/`Attach` (DTO de infraestructura). Se inyecta en `CorreoServiceImpl`.
```java
package com.educaflow.subsystem.correos.service.impl;

import com.educaflow.base.infrastructure.mail.Mail;
import com.educaflow.subsystem.correos.db.Correo;

/**
 * Construye el DTO Mail de infraestructura a partir de un Correo de dominio.
 * Lee destinatario (emailDestinatario), asunto y cuerpo del Correo, resuelve el 'from' desde
 * la configuración del subsistema mail, y por cada AdjuntoCorreo descarga los bytes del MetaFile
 * (MetaFileUtil.downloadContent) creando un Attach(nombreFichero, bytes, mimeType).
 */
public class CorreoMailFactory {

    /**
     * Devuelve el Mail listo para entregar a MailSender.
     * Puede lanzar RuntimeException si un adjunto es ilegible (MetaFile sin fichero/IO);
     * ese fallo lo captura CorreoServiceImpl y marca el Correo como FALLIDO.
     * El 'from' NO procede del cliente: se toma de la config del subsistema mail.
     */
    public Mail build(Correo correo);
}
```

**`ResultadoEnvio`** — record interno que encapsula el desenlace de un intento, para no pasar `(boolean, String)` sueltos.
```java
package com.educaflow.subsystem.correos.service.impl;

/**
 * Resultado de un intento de envío de un Correo.
 * exito=true -> motivo es null. exito=false -> motivo lleva la descripción saneada del fallo.
 */
public record ResultadoEnvio(boolean exito, String motivo) {
    public static ResultadoEnvio ok();                  // new ResultadoEnvio(true, null)
    public static ResultadoEnvio fallo(String motivo);  // new ResultadoEnvio(false, motivo)
}
```

Firmas + comentario (SIN cuerpo) de los métodos de envío en `CorreoServiceImpl` (de `R-Correo-006.md`):
```java
/**
 * Orquesta el envío batch de todos los Correo en estado PENDIENTE.
 * Recupera findByEstado(PENDIENTE) y procesa CADA correo de forma AISLADA: cada uno en su
 * propia transacción y su propio try/catch, de modo que el fallo de uno NO aborta ni hace
 * rollback de los demás (NO usar un @Transactional global).
 * Por correo: fireActionRule_RegistrarIntento -> CorreoMailFactory.build -> mailSender.send
 * -> fireActionRule_RegistrarResultadoEnvio. Logs sin datos sensibles (id+estado, CRLF saneado).
 * Disparado por EnviarCorreosPendientesJob (cron 'correos.envio.cron', E-UB-012).
 */
@Override
public void enviarCorreosPendientes();

/**
 * R-Correo-006 (Antes del intento). numeroIntentos = numeroIntentos + 1 y fechaUltimoIntento = now,
 * de forma INCONDICIONAL (el servidor es la fuente de verdad; sin if==null; nada procede del cliente).
 * Persiste el cambio para que quede rastro del intento aun si el envío posterior se interrumpe.
 */
public void fireActionRule_RegistrarIntento(Correo correo);

/**
 * R-Correo-007 (Después del intento). Registra el desenlace de forma INCONDICIONAL:
 *  - exito=true  -> estado = ENVIADO, fechaEnvio = now, motivoUltimoFallo = null.
 *  - exito=false -> estado = FALLIDO, motivoUltimoFallo = motivo (descripción saneada del error).
 * Persiste el resultado. No reintenta: un FALLIDO solo vuelve a PENDIENTE por un reenvío explícito (R-Correo-005).
 */
public void fireActionRule_RegistrarResultadoEnvio(Correo correo, boolean exito, String motivo);
```

**Aislamiento transaccional (CRÍTICO, de R-Correo-006.md):** el bucle envuelve el procesamiento de cada `Correo` en su propio `try/catch` y su propia transacción (`com.axelor.db.JPA.runInTransaction` por iteración, o un método `procesarCorreo(Correo)` `@Transactional` invocado por iteración). **NO** un único `@Transactional` global sobre `enviarCorreosPendientes()`.

Defaults de implementación (de R-Correo-006.md):
- **`from`**: propiedad de configuración del subsistema mail (p.ej. `correos.envio.from`).
- **`mimeType` de adjuntos**: `adjunto.getContenido().getFileType()`; fallback `application/octet-stream`.
- **`textBody`**: vacío/derivado (el cuerpo es HTML).
- **Destinatario**: único, `to = List.of(emailDestinatario)`.
- **Motivo del fallo**: `getMessage()` saneado y truncado.

---

## Frontera de confianza — AllowProperties por acción

Clasificación (columna "Origen del valor"):
- **Correo cliente (5):** `asunto`, `cuerpo`, `dniDestinatario`, `emailDestinatario`, `adjuntos`.
- **Correo servidor (8):** `fechaCreacion`, `fechaEnvio`, `estado`, `numeroIntentos`, `fechaUltimoIntento`, `motivoUltimoFallo`, `centro`, `referenciaHistorialEstadoExpediente`.
- **AdjuntoCorreo cliente (2):** `nombreFichero`, `contenido`. **AdjuntoCorreo servidor (1):** `correo`.

### `CorreoServiceImpl.insert(Correo)` (alta manual, vía REST `/ws/rest`)

Entidad `Correo`. **Forma elegida:** `createAllowProperties` (whitelist).

| Campo | Origen | En whitelist | Justificación / Ubicación de la asignación |
|-------|--------|--------------|---------------------------------------------|
| `asunto`, `cuerpo`, `dniDestinatario`, `emailDestinatario` | cliente | sí | Input del usuario. |
| `adjuntos` | cliente | sí (sub-whitelist `nombreFichero`, `contenido`) | Adjuntos aportados en el alta. |
| `estado`, `fechaCreacion`, `numeroIntentos`, `fechaEnvio`, `fechaUltimoIntento`, `motivoUltimoFallo` | servidor | **NO** | Asignados incondicionalmente en `fireActionRule_InicializarCorreo` (R-Correo-001). |
| `centro` | servidor | **NO** | Asignado `null` incondicionalmente en `fireActionRule_AltaManualSinCentro` (R-Correo-002). |
| `referenciaHistorialEstadoExpediente` | servidor | **NO** | El alta manual no la toca; `validateInsert` rechaza si llega no nula por REST (V-Correo-006). |

### `CorreoServiceImpl.insert(CorreoInsertDTO)` (alta programática)

El **DTO es la whitelist** (k-secure-coding §3.5; no pasa por REST). `centro` y `referenciaHistorial` aparecen en el record porque los aporta el subsistema invocador (justificados, R-Correo-003). El resto de campos servidor (estado, fechas, intentos…) se asignan en `fireActionRule_InicializarCorreo` (R-Correo-001), no vienen del DTO.

### `CorreoServiceImpl.reenviar` (`@CallMethod CorreoController.btnReenviar`)

Entidad `Correo`. **Forma:** `createAllowProperties(Map.of())` (whitelist vacía). Ningún campo del cliente; `estado=PENDIENTE` se asigna incondicionalmente (R-Correo-005). Resto de campos servidor: fuera.

### `CorreoServiceImpl.update`

Inmutables: `UnsupportedOperationException` + `validateUpdate` rechaza. Ningún campo aceptado.

Reglas aplicadas (k-secure-coding §3): ninguna whitelist enumera un campo `servidor` ✔; todo campo servidor que la acción fija se asigna incondicional (sin `if==null`) ✔; no se usa `createAllowAllProperties()` ✔; `centro`/`referenciaHistorialEstadoExpediente` solo entran por el DTO programático ✔.

---

## Trazabilidad V/R/U → ubicación (aplicable a esta tarea)

### Validaciones (V-)
| ID | Ubicación |
|----|-----------|
| V-Correo-001 (dni obligatorio) | `CorreoServiceImpl.validateInsert(Correo)` + `required="true"` en `dniDestinatario`. |
| V-Correo-002 (email obligatorio) | `CorreoServiceImpl.validateInsert(Correo)` + `required="true"` en `emailDestinatario`. |
| V-Correo-003 (asunto obligatorio) | `CorreoServiceImpl.validateInsert(Correo)` + `required="true"` en `asunto`. |
| V-Correo-004 (cuerpo obligatorio) | `CorreoServiceImpl.validateInsert(Correo)` + `required="true"` en `cuerpo`. |
| V-Correo-005 (inmutable tras crear) | `CorreoServiceImpl.validateUpdate` (rechaza siempre) + `update` lanza `UnsupportedOperationException`. |
| V-Correo-006 (referencia no asignable desde UI) | `referenciaHistorialEstadoExpediente` fuera de `allowPropertiesInsert` + `validateInsert(Correo)` rechaza si llega no nula; `readonly` en la vista (UX). |
| V-Correo-007 (solo reenvía FALLIDO) | `CorreoServiceImpl.validateReenviar`. |

### Reglas de negocio (R-)
| ID | Ubicación |
|----|-----------|
| R-Correo-001 (init estado/fecha/intentos) | `fireActionRule_InicializarCorreo` (antes de `repository.save`, en ambos `insert`). |
| R-Correo-002 (alta manual sin centro) | `fireActionRule_AltaManualSinCentro` (en `insert(Correo)`). |
| R-Correo-003 (alta programática centro+referencia) | `fireActionRule_AltaProgramaticaCentroYReferencia` (en `insert(CorreoInsertDTO)`). |
| R-Correo-004 (proponer email por DNI) | UI: `CorreoController.onChangeDni` + `CorreoService.proponerEmailPorDni` + `setValue` (campo cliente). Ver U-correo-001. |
| R-Correo-005 (reenvío → PENDIENTE) | `fireActionRule_ReactivarCorreo` (en `reenviar`). |
| R-Correo-006 (registrar intento) | `fireActionRule_RegistrarIntento` (antes del intento). **Detalle: `design/rules/R-Correo-006.md`**. |
| R-Correo-007 (registrar resultado) | `fireActionRule_RegistrarResultadoEnvio` (después del intento). **Detalle: `design/rules/R-Correo-006.md`**. |
| R-Correo-008 (cascada borrado adjuntos) | `CorreoServiceImpl.remove` (borra hijos antes de `repository.remove`); `one-to-many adjuntos mappedBy="correo"`. |

## Paso 8 — Seguridad (aplicable a esta tarea)
- **Multi-centro / IDOR:** el `centro` nunca lo dicta el cliente: alta manual → `null` (R-Correo-002 incondicional); alta programática → del DTO del invocador (R-Correo-003 incondicional).
- **Autorización por rol:** para reenviar, **además** dentro de `validateReenviar`/`reenviar` con `SecurityUtil` (defensa para la Vía B). Nunca en el controlador.
- **Inmutabilidad (k-secure-coding §9.2):** `Correo.update` lanza `UnsupportedOperationException`; `validateUpdate` rechaza siempre.
- **JPQL/dominios:** solo `:__user__.x` y `:param`; cero concatenación.
- **Adjuntos:** el `Attach` se monta con `MetaFileUtil.downloadContent`; nombre visible `nombreFichero`.
- **Logs:** solo `id`/estado del Correo; nunca cuerpo, email completo ni bytes del adjunto; CRLF saneado.
