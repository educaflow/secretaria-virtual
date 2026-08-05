# El modelo del tipo de expediente (`domains.xml`)

Entidad JPA del expediente de esta versión. El esqueleto lo genera `./gradlew CreateFilesTask` (`SKILL.md` §3.1), no el build; tú añades los campos. Para los tipos de campo y relaciones generales de Axelor, ver `k-sistemas` (`modelos.md`); aquí solo lo específico de los tipos de expediente.

## 1. Estructura fija

```xml
<domain-models ...>
    <module name="expedientes" package="com.educaflow.subsystem.expedientes.db"/>
    <entity name="JustificacionFaltaProfesoradoV1" extends="Expediente">
        <!-- tus campos -->
        <extra-code-model><![CDATA[ ... generado por el build ... ]]></extra-code-model>
    </entity>
    <!-- tus enums -->
</domain-models>
```

- **MUST NOT** cambiar el `<module>` (nombre y paquete están hardcodeados en el check del EventManager: la entidad se busca como `com.educaflow.subsystem.expedientes.db.<code>`).
- El `name` de la entidad **MUST** ser exactamente el `code` derivado del tipo (code del trámite + `VN`); si no, el build falla ("No se encontró el tag '<entity>' con name=...").
- **MUST** mantener `extends="Expediente"`: hereda la infraestructura común y habilita la inyección del `extra-code-model`.

## 2. Campos heredados de `Expediente`

No los redeclares (fuente de verdad: `subsystem/expedientes/domains/Expediente.xml`): `tipoExpediente`, `name`, `numeroExpediente`, `codeState`/`nameState`, `fechaUltimoEstado`, `abierto`, `historialEstados`, `centro`, `usuarioRegistrador`, `personaSolicitante`, `personaInteresada` (ambas `Persona`) y `dniFirmaDocumentoEntrada`.

## 3. Enums propios

- **MUST** sufijar el nombre del enum con el nombre completo de la entidad (con versión): todos los enums de todos los tipos comparten el paquete `db` y sin sufijo colisionarían entre trámites y entre versiones.
- Se admiten enums `numeric="true"` con `value=` por item.

- ✅ CORRECTO: `<enum name="TipoResolucionJustificacionFaltaProfesoradoV1">`
- ✅ CORRECTO: `<enum name="MesJustificacionFaltaProfesoradoV1" numeric="true"><item name="ENERO" title="Enero" value="1"/>…`
- ❌ INCORRECTO: `<enum name="TipoResolucion">` (colisiona con cualquier otro tipo de expediente que necesite el mismo concepto)
- ❌ INCORRECTO: `<enum name="TipoResolucionJustificacionFaltaProfesorado">` (sin versión: colisiona con la siguiente versión del propio trámite)

## 4. Campos `MetaFile` para los PDF

Cada PDF que el expediente guarde es un `many-to-one` a `com.axelor.meta.db.MetaFile`:

```xml
<many-to-one name="justificante" title="Foto o PDF del justificante" ref="com.axelor.meta.db.MetaFile" />
<many-to-one name="pdfSolicitud" title="PDF de la solicitud" ref="com.axelor.meta.db.MetaFile" />
<many-to-one name="pdfSolicitudFirmado" title="PDF de la solicitud" ref="com.axelor.meta.db.MetaFile" />
<many-to-one name="pdfJustificanteRegistroEntrada" title="Justificante del registro de entrada" ref="com.axelor.meta.db.MetaFile" />
<many-to-one name="pdfResolucion" title="Resolución" ref="com.axelor.meta.db.MetaFile" />
```

- Para la firma del usuario con AutoFirma se necesita el **par** original/firmado (`pdfSolicitud`/`pdfSolicitudFirmado`) — ver `eventmanager.md` §6.5.
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
