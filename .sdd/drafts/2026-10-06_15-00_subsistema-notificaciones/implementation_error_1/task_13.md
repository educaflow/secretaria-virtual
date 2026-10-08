---
type: implementation-task
template: system
---

# Tarea 13 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas
- k-secure-coding
- k-code-quality
- k-validaciones

## Servicio de Adjunto (AdjuntoService + Impl)

Las decisiones difíciles, con sus alternativas, están en [`decisiones.md`](decisiones.md) (D1–D6); este documento las cita por su número.

## Ficheros a crear o modificar (extracto del diseño)

Rutas relativas a `src/main/java/com/educaflow/` salvo que empiecen por `src/`, `agent_docs/` o `.claude/`.

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/notificaciones/service/AdjuntoService.java` | Crear | k-sistemas (servicios.md) | Servicio de `Adjunto` (sin acciones propias) |
| `subsystem/notificaciones/service/impl/AdjuntoServiceImpl.java` | Crear | k-sistemas, k-validaciones, k-secure-coding | Adjunto (sustituye a `correos/service/impl/AdjuntoServiceImpl`) |

## Pasos del diseño

### Paso 3 — Interfaces de servicio y base abstracta del canal

**`AdjuntoService extends ModelService<Adjunto>`**: sin métodos propios.

### Paso 4 — Canal correo y adjunto

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

Verificación (al cerrar el bloque 1–9): compila.

## Frontera de confianza — AllowProperties por acción

### `insert` de cada entidad (puerta REST `/ws/rest/<FQN>` y `save` de los forms)

No se invoca desde un `@CallMethod`, pero es la frontera real del alta y se documenta igual.

**`AdjuntoServiceImpl.allowPropertiesInsert`**: `nombreFichero`, `contenido`, `correo` (padre cliente, validado por V-Adjunto-001/006/007, k-secure-coding §3.6).
**`NotificacionServiceImpl.allowPropertiesInsert/Update/Remove`** y **`*.allowPropertiesUpdate/Remove`** de los canales y del adjunto: `createDenyAllProperties` (inmutables, sin borrado; alta genérica imposible).

## Trazabilidad Origen spec → V/R/U → ubicación

### V

| ID | Origen spec | Ubicación |
|---|---|---|
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

> **Nota del descomponedor (decisión ante ambigüedad):** el Paso 1 del diseño borra `subsystem/correos/**`, `subsystem/sms/**` y sus tests (tareas 05–07) antes de esta tarea. Donde el diseño dice que algo «se conserva», «se mueve» o tiene «el mismo cuerpo» que en `correos`/`sms`, el código original se lee del último commit con `git show HEAD:<ruta>` (p.ej. `git show HEAD:src/main/java/com/educaflow/subsystem/correos/service/impl/CorreoServiceImpl.java`). Solo sirve de referencia: **MUST NOT** restaurar esos ficheros.
