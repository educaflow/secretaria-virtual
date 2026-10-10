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
- **MUST** mantener `extends="Expediente"`.
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

### 4.1 Los campos comunes de las fases `ENTRADA` y `VERIFICACION`

Todo tipo **MUST** declarar estos cinco campos **con exactamente estos nombres** (`SKILL.md` §1.2). No están en `Expediente`, pero los paneles comunes de `tramites/shared/template-views.xml` los nombran (`vistas.md` §3.2) y el código común llega a ellos por el descriptor `CAMPOS_ENTRADA` (`recetas/presentacion.md` §1.1).

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
- El enum es **propio de cada tipo** (§3) y **MUST** tener los ítems `CORRECTO` y `SUBSANAR`: el panel común `verificacion` y los `trigger*` los nombran.
- El `title` de cada campo sí es libre.

- ❌ INCORRECTO: `pdfSolicitudFirmado`, `solicitudEscaneada` o `motivoSubsanacion` (nombres propios: los paneles comunes pintarían campos vacíos, sin ningún error)
- ❌ INCORRECTO: un campo aparte para el escaneado del papel (es `pdfSolicitudFirmada`: es la que se registra en los dos modos)

## 5. Anti-patrones

- **MUST NOT** cambiar el `<module>` ni el `name` de la entidad respecto a los derivados.
- **MUST NOT** editar `<extra-code-model>` a mano.
- **MUST NOT** crear enums sin el sufijo entidad+versión.
- **MUST NOT** redeclarar campos que ya hereda de `Expediente`.
- **MUST NOT** cambiar el nombre de los campos comunes de las fases `ENTRADA` y `VERIFICACION` (§4.1).
- **MUST NOT** poner una entidad auxiliar por delante de la del expediente.
