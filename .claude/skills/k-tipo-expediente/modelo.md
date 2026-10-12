# El modelo del tipo de expediente (`domains.xml`)

Entidad JPA del expediente de esta versión. Va en la **raíz de la carpeta de versión** y es una sola para todo el tipo (las fases no reparten el modelo). El esqueleto lo genera `CreateFilesTask` (`SKILL.md` §3.1); tú añades los campos. Para los tipos de campo y relaciones de Axelor, ver `k-sistemas` (`modelos.md`); aquí solo lo específico de los tipos de expediente.

## 1. Estructura fija

```xml
<domain-models ...>
    <module name="expedientes" package="com.educaflow.subsystem.expedientes.db"/>
    <entity name="MiTramiteV1" extends="Expediente">
        <!-- tus campos -->
        <extra-code-model><![CDATA[ ... generado por el build ... ]]></extra-code-model>
    </entity>
    <!-- tus enums y, si hacen falta, entidades auxiliares -->
</domain-models>
```

- **MUST NOT** cambiar el `<module>`: todas las entidades de expediente se generan en `com.educaflow.subsystem.expedientes.db` y el código y los tests las buscan ahí.
- El `name` de la entidad **MUST** ser exactamente el `code` del tipo (code del trámite + `VN`); si no, el build falla.
- **MUST** mantener `extends="Expediente"` — o `extends="ExpedienteResoluble"` si el trámite se tramita con **resolución firmada y registro de salida**. `ExpedienteResoluble` (`tramites/shared/expedienteresoluble/domains.xml`) es la entidad base **compartida** de esos trámites: **no es un trámite** (no tiene `TipoExpedienteInstance`), y cada tipo "resoluble" declara `extends="ExpedienteResoluble"` en lugar de `extends="Expediente"` (p. ej. `tramites/profesores/justificacion_falta_profesorado/actual/v1/domains.xml` y `tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/domains.xml`). Misma unidad de persistencia que `Expediente` (JOINED): el `<module>` es el mismo y la clase generada vive igualmente en `com.educaflow.subsystem.expedientes.db`, aunque el XML esté en `tramites/shared/`.
  - Con `extends="ExpedienteResoluble"` el tipo **hereda y MUST NOT redeclarar** sus 15 campos: los 6 `many-to-one` a `MetaFile` `pdfSolicitud`, `pdfSolicitudFirmada`, `pdfJustificanteRegistroEntrada`, `pdfResolucion`, `pdfResolucionFirmada`, `pdfResolucionFirmadaRegistroSalida`, y los 6 `<datetime>` `fechaHoraPdfSolicitud`, `fechaHoraPdfSolicitudFirmada`, `fechaHoraPdfJustificanteRegistroEntrada`, `fechaHoraPdfResolucion`, `fechaHoraPdfResolucionFirmada`, `fechaHoraPdfResolucionFirmadaRegistroSalida`, y los 3 de la verificación, `resultadoVerificacion` (enum compartido `ResultadoVerificacion`, declarado en ese mismo `domains.xml`), `textoSubsanacion` y `fechaVerificacion` (`<date>`, la fija el `triggerVerificar` de cada tipo).
  - **MUST** asignar el `fechaHoraPdf<X>` justo después de asignar su `pdf<X>`: la hora actual si se le da un PDF —o, si el PDF sale de un asiento del registro, su `getFecha()`—, `null` si se vacía. Los paneles `expedienteresoluble.ver-pdf*`/`descargar-pdf*` (`vistas.md` §9) la pintan junto al PDF. Las de `pdfSolicitudFirmada` y `pdfJustificanteRegistroEntrada` las fija ya `EntradaHelper.presentar` (`recetas/presentacion.md` §1.1).
    - ❌ INCORRECTO: `expediente.setPdfResolucion(null);` sin `expediente.setFechaHoraPdfResolucion(null);` (el panel pinta la fecha de un PDF que ya no está)
    - ❌ INCORRECTO: un campo propio `fechaHoraPresentacion` (es `fechaHoraPdfJustificanteRegistroEntrada`, ya heredado)
  - Si el tipo incluye en sus vistas **cualquier** panel `expedienteresoluble.*` (los visores de los PDF o los de la verificación, `vistas.md` §3.2 y §9), **MUST** declarar `extends="ExpedienteResoluble"`.
  - En cualquiera de las dos bases, el tipo **MUST** seguir declarando lo que su base no trae —con `Expediente`, también los cinco comunes (§4.1)— y todos sus campos específicos (en los ejemplos reales de arriba: el periodo y el motivo de la falta, el `tipoResolucion` con sus motivos y el `justificante`; el `ciclo`, el `cursoAcademico` y su `tipoResolucion`).
  - Si el trámite **no** termina en resolución firmada con registro de salida, la base es `Expediente` y entonces los `pdf*` del ciclo de resolución que necesite **MUST** declararlos cada tipo (§4 y §4.1), junto con sus propios paneles: **MUST NOT** usar los `expedienteresoluble.*`.
- La entidad del expediente **MUST** ser la **primera** `<entity>` del fichero; las auxiliares (entidades hija) van después.
  Es la que **MUST** parametrizar el `InitialEventManagerImpl` (`implements InitialEventManager<MiTramiteV1>`) y el `PhaseEventManagerImpl` de cada fase (`extends PhaseEventManager<MiTramiteV1>`): lo comprueba el test M1.
- **MUST NOT** editar el `<extra-code-model>`: el build lo reescribe en cada compilación con el enum `TipoDocumentoPdf` (una constante por documento de `documentospdf/`) y el método `getDocumentoPdf(...)` (`phaseeventmanager.md` §6.1).

## 2. Campos heredados de `Expediente`

**MUST NOT** redeclararlos (fuente de verdad: `subsystem/expedientes/domains/Expediente.xml`):

| Campo | Qué es | Quién lo escribe |
|---|---|---|
| `tipoExpediente`, `centro`, `usuarioRegistrador`, `name`, `numeroExpediente` | Datos del alta | el motor, al crear |
| `presentadoEnPapel`, `presentadoEnRepresentacion` | Cómo y para quién se presentó (`perfiles.md`) | el motor, al crear; el cliente **no** puede cambiarlos |
| `personaSolicitante`, `personaInteresada` | Las dos personas (§2.1) | el motor, al crear; el tipo completa sus datos |
| `codePhase`/`namePhase`, `codeState`/`nameState`, `perfilEstado`, `fechaUltimoEstado`, `abierto` | El estado actual | el motor, en cada `updateState` |
| `historialEstados` | El historial | el motor, en cada evento |
| `claveCertificado` | Clave del certificado al firmar en servidor; `transient`, nunca se guarda (`recetas/firma.md` §1) | el usuario, al firmar |

### 2.1 Las personas del expediente

Los datos de una persona (nombre, apellidos, DNI, NIA, email, teléfono, dirección, municipio, CP) viven **solo** en `personaSolicitante` y `personaInteresada`.

- **MUST NOT** declarar en el `domains.xml` del tipo un campo que se llame como uno de `Persona`: lo caza el test M2.
- Cómo nacen, según lo que eligió el usuario al crear el expediente:

| Modo | `personaSolicitante` | `personaInteresada` |
|---|---|---|
| Telemático, «para mí» | datos del usuario | datos del usuario |
| Telemático, en representación | datos del usuario | **vacía** |
| En papel (lo registra el `TRAMITADOR`) | **vacía** | **vacía** |

- «Datos del usuario» son su nombre, apellidos, DNI, email y teléfono (los de `User`).
- Lo que nace vacío **MUST** pedirse y validarse en el estado de entrada de datos (nombre, apellidos y DNI como mínimo): si no, el registro de entrada sale sin interesado o sin solicitante.
  En papel y «para mí» solo se teclea el interesado: el motor copia su identificación y su contacto (email y teléfono) en el solicitante.
- El email y el teléfono de `personaSolicitante` son a donde se le avisa del expediente (correos y SMS de `tramites/util`).
  En papel **MUST** pedirse también (sin `Required`: no todo el mundo los tiene) y validarse el teléfono con `Phone()`; en papel y «para mí» se piden en el interesado.
- Para validar o completar datos de una persona, el validador los declara **anidados** sobre la relación, y así entran en la whitelist del evento:

```kotlin
field(model::getPersonaInteresada) {
    field(Persona::getNombre) { +Required() }
    field(Persona::getNia) { +Required(); +Nia() }
}
```

- Una `Lambda` dentro de un campo anidado recibe la `Persona`, no el expediente: la función de `<Code>Util` **MUST** tomar una `Persona`.
- La identificación que el modo no deja teclear —y, si presenta el usuario, el contacto del solicitante— la restaura el motor en cada evento, y rechaza que la petición cambie la `Persona` por otra: no hace falta protegerla en el tipo.
- Las vistas de las personas: `vistas.md` §3.1.

## 3. Enums propios

- **MUST** sufijar el nombre del enum con el nombre completo de la entidad (con versión): todos los enums de todos los tipos comparten el paquete `db`.
- Se admiten enums `numeric="true"` con `value=` por item.

- ✅ CORRECTO: `<enum name="TipoResolucionMiTramiteV1">`
- ❌ INCORRECTO: `<enum name="TipoResolucion">` (colisiona con cualquier otro tipo de expediente)
- ❌ INCORRECTO: `<enum name="TipoResolucionMiTramite">` (sin la versión: colisiona con la siguiente versión del propio trámite)

## 4. Campos `MetaFile` para los PDF

Cada PDF que el expediente guarde es un `many-to-one` a `com.axelor.meta.db.MetaFile`, con un `title` propio:

```xml
<many-to-one name="justificante" title="Foto o PDF del justificante" ref="com.axelor.meta.db.MetaFile" />
<many-to-one name="pdfSolicitud" title="Solicitud" ref="com.axelor.meta.db.MetaFile" />
<many-to-one name="pdfSolicitudFirmada" title="Solicitud firmada" ref="com.axelor.meta.db.MetaFile" />
<many-to-one name="pdfJustificanteRegistroEntrada" title="Justificante de presentación" ref="com.axelor.meta.db.MetaFile" />
<many-to-one name="pdfResolucion" title="Resolución" ref="com.axelor.meta.db.MetaFile" />
```

- Para que el usuario firme un documento hace falta el **par** original/firmado (`pdfSolicitud`/`pdfSolicitudFirmada`): `recetas/firma.md` §1.2.
- Los documentos que devuelven los registros de entrada y salida también necesitan su campo (`pdfJustificanteRegistroEntrada`, `pdfResolucion`).
- Si el tipo extiende de `ExpedienteResoluble` (§1), los `pdf*` de los dos puntos de arriba ya están heredados: **MUST NOT** redeclararlos.

### 4.1 Los campos comunes de las fases `ENTRADA` y `VERIFICACION`

Todo tipo **MUST** declarar estos cinco campos **con exactamente estos nombres** (`SKILL.md` §1.2), salvo que ya los herede: no están en `Expediente`, pero los cinco sí están en `ExpedienteResoluble` (§1), así que un tipo que extienda de ella **MUST NOT** redeclararlos (los paneles comunes de los `common-template-views.xml` de `tramites/shared` los nombran igual, `vistas.md` §3.2) y el código común llega a ellos por el descriptor `CAMPOS_ENTRADA` (`recetas/presentacion.md` §1.1).

```xml
<many-to-one name="pdfSolicitud" title="Solicitud" ref="com.axelor.meta.db.MetaFile" />
<many-to-one name="pdfSolicitudFirmada" title="Solicitud firmada" ref="com.axelor.meta.db.MetaFile" />
<many-to-one name="pdfJustificanteRegistroEntrada" title="Justificante de presentación" ref="com.axelor.meta.db.MetaFile" />
<enum name="resultadoVerificacion" title="Resultado de la verificación" ref="ResultadoVerificacionMiTramiteV1" />
<string name="textoSubsanacion" title="Qué hay que subsanar" large="true" />
...
<enum name="ResultadoVerificacionMiTramiteV1">
    <item name="CORRECTO" title="La solicitud es correcta"/>
    <item name="SUBSANAR" title="Pedir subsanación"/>
</enum>
```

- `pdfSolicitudFirmada` es la solicitud que se registra: la firmada por el usuario o, en papel, la escaneada.
- Con `extends="Expediente"` el enum es **propio del tipo** (§3); con `ExpedienteResoluble` se usa el compartido `ResultadoVerificacion`. En los dos casos **MUST** tener los ítems `CORRECTO` y `SUBSANAR`: el panel común `verificacion` y los `trigger*` los nombran.
- El `title` de cada campo sí es libre.

- ❌ INCORRECTO: `pdfSolicitudFirmado`, `solicitudEscaneada` o `motivoSubsanacion` (nombres propios: los paneles comunes pintarían campos vacíos, sin ningún error)
- ❌ INCORRECTO: un campo aparte para el escaneado del papel (es `pdfSolicitudFirmada`: es la que se registra en los dos modos)

## 5. Anti-patrones

- **MUST NOT** cambiar el `<module>` ni el `name` de la entidad respecto a los derivados.
- **MUST NOT** editar `<extra-code-model>` a mano.
- **MUST NOT** crear enums sin el sufijo entidad+versión.
- **MUST NOT** redeclarar campos que ya hereda de `Expediente` ni de `ExpedienteResoluble`, si extiende de ella (§1).
- **MUST NOT** cambiar el nombre de los campos comunes de las fases `ENTRADA` y `VERIFICACION` (§4.1).
- **MUST NOT** poner una entidad auxiliar por delante de la del expediente.
