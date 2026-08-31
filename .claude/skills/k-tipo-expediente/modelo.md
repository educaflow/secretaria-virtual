# El modelo del tipo de expediente (`domains.xml`)

Entidad JPA del expediente de esta versión. Va en la **raíz de la carpeta de versión**: es una sola para todo el tipo, **no** se reparte por fases (las fases solo agrupan la máquina de estados y sus vistas, no el modelo). El esqueleto lo genera `./gradlew CreateFilesTask` (`SKILL.md` §3.1), no el build; tú añades los campos. Para los tipos de campo y relaciones generales de Axelor, ver `k-sistemas` (`modelos.md`); aquí solo lo específico de los tipos de expediente.

## 1. Estructura fija

```xml
<domain-models ...>
    <module name="expedientes" package="com.educaflow.subsystem.expedientes.db"/>
    <entity name="MiTramiteV1" extends="Expediente">
        <!-- tus campos -->
        <extra-code-model><![CDATA[ ... generado por el build ... ]]></extra-code-model>
    </entity>
    <!-- tus enums -->
</domain-models>
```

- **MUST NOT** cambiar el `<module>`: el paquete `com.educaflow.subsystem.expedientes.db` está **hardcodeado** en los generadores de esqueletos de `EducaFlowBuildTools` (`PhaseEventManagerFile`, `InitialEventManagerFile`) y en los tests E1/I* (`SKILL.md` §3.3), que componen el FQCN de la entidad como `com.educaflow.subsystem.expedientes.db.<code>`.
  (La clase runtime `PhaseEventManager` **no** hace esa composición: resuelve la entidad por su parámetro de tipo, vía `ExpedienteLocator.getModelClass`.)
- El `name` de la entidad **MUST** ser exactamente el `code` derivado del tipo (code del trámite + `VN`); si no, el build falla ("No se encontró el tag '<entity>' con name=...").
- La entidad del expediente **MUST** ser la **primera** `<entity>` del fichero: es así como se la identifica cuando el `domains.xml` declara varias (las entidades auxiliares del tipo van después).
  Esa misma entidad **MUST** parametrizar el `InitialEventManagerImpl` de la raíz (`implements InitialEventManager<…>`) y el `PhaseEventManagerImpl` de cada fase (`extends PhaseEventManager<…>`), que es de donde la lee el runtime (`SKILL.md` §1.6); lo comprueba el test M1 (`SKILL.md` §3.3).
- **MUST** mantener `extends="Expediente"`: hereda la infraestructura común y habilita la inyección del `extra-code-model`.

## 2. Campos heredados de `Expediente`

No los redeclares (fuente de verdad: `subsystem/expedientes/domains/Expediente.xml`): `tipoExpediente`, `name`, `numeroExpediente`, `codePhase`/`namePhase`/`codeState`/`nameState`, `fechaUltimoEstado`, `abierto`, `historialEstados`, `centro`, `usuarioRegistrador`, `personaSolicitante`, `personaInteresada` (ambas `Persona`) y `dniFirmaDocumentoEntrada`.

`codePhase` y `codeState` guardan la pareja que identifica al estado (`SKILL.md` §1.5); `namePhase` y `nameState` guardan sus textos visibles (el `title` de la fase y el del estado, o sus `name` humanizados), que son los que ve el usuario en los listados.

## 3. Enums propios

- **MUST** sufijar el nombre del enum con el nombre completo de la entidad (con versión): todos los enums de todos los tipos comparten el paquete `db` y sin sufijo colisionarían entre trámites y entre versiones.
- Se admiten enums `numeric="true"` con `value=` por item.

- ✅ CORRECTO: `<enum name="TipoResolucionMiTramiteV1">`
- ✅ CORRECTO: `<enum name="MesMiTramiteV1" numeric="true"><item name="ENERO" title="Enero" value="1"/>…`
- ❌ INCORRECTO: `<enum name="TipoResolucion">` (colisiona con cualquier otro tipo de expediente que necesite el mismo concepto)
- ❌ INCORRECTO: `<enum name="TipoResolucionMiTramite">` (sin el sufijo de versión: colisiona con la siguiente versión del propio trámite)

## 4. Campos `MetaFile` para los PDF

Cada PDF que el expediente guarde es un `many-to-one` a `com.axelor.meta.db.MetaFile`:

```xml
<many-to-one name="justificante" title="Foto o PDF del justificante" ref="com.axelor.meta.db.MetaFile" />
<many-to-one name="pdfSolicitud" title="PDF de la solicitud" ref="com.axelor.meta.db.MetaFile" />
<many-to-one name="pdfSolicitudFirmado" title="PDF de la solicitud" ref="com.axelor.meta.db.MetaFile" />
<many-to-one name="pdfJustificanteRegistroEntrada" title="Justificante del registro de entrada" ref="com.axelor.meta.db.MetaFile" />
<many-to-one name="pdfResolucion" title="Resolución" ref="com.axelor.meta.db.MetaFile" />
```

- Para la firma del usuario con AutoFirma se necesita el **par** original/firmado (`pdfSolicitud`/`pdfSolicitudFirmado`) — ver `phaseeventmanager.md` §6.5.
- Guardar los documentos sellados que devuelven los registros de entrada/salida también requiere su campo (`pdfJustificanteRegistroEntrada`, `pdfResolucion`).

## 5. `<extra-code-model>` — generado, no editar

- **MUST NOT** editar ese bloque: `RichDomainXmlTask` lo reescribe en cada build con el enum `TipoDocumentoPdf` (una constante por cada `.pdf` de `documentospdf/`, generado o versionado) y el método `getDocumentoPdf(...)`.
- Si el tipo no tiene PDFs, el bloque se escribe igualmente vacío.
- La inyección del extra-code va en un try/catch que **avisa pero no detiene el build**: si falta `extends="Expediente"` te quedas sin enum `TipoDocumentoPdf` en silencio (salvo el aviso en el log).

## 6. Anti-patrones

- **MUST NOT** cambiar el paquete del `<module>` ni el `name` de la entidad respecto a los derivados.
- **MUST NOT** editar `<extra-code-model>` a mano.
- **MUST NOT** crear enums sin el sufijo entidad+versión.
- **MUST NOT** redeclarar campos que ya hereda de `Expediente`.
- **MUST NOT** poner una entidad auxiliar por delante de la del expediente: la primera `<entity>` es la que se toma como entidad del tipo.
