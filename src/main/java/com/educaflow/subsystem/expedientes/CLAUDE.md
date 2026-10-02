# `subsystem/expedientes` — el dominio del expediente

Aquí vive **el expediente como dato**: sus entidades (`domains/`, generadas a `db/`), su persistencia, su servicio de tipos de expediente y sus datos iniciales.

El **motor de tramitación** —la máquina que lleva un expediente de un estado a otro— no está aquí: es [`subsystem/tramitador`](../tramitador/CLAUDE.md).
Son dos subsistemas hermanos a propósito, porque el dominio cambia con el negocio y el motor cambia con la arquitectura, y mezclarlos hacía imposible verificar que el motor se mantiene pequeño.

## Dirección de las dependencias

`tramites → subsystem/tramitador → subsystem/expedientes`, y nunca al revés.

**MUST NOT** existir ningún import de `com.educaflow.subsystem.tramitador..` en este paquete.
Lo verifica la regla **C25** de [`architecture-rules.md`](../../../../../../../agent_docs/architecture-rules.md), cuyo sujeto incluye **a propósito** las entidades generadas de `db/`.

**CRITICAL — la puerta por la que el ciclo entra sin avisar.**
Las entidades de **todos** los tipos de expediente se generan en `com.educaflow.subsystem.expedientes.db`, porque el `<module>` de cada `domains.xml` de trámite declara ese paquete.
Por eso un `<extra-code-model>` que llame al motor —desde el `domains.xml` de aquí o desde el de **cualquier** trámite— no acopla «una entidad»: acopla el dominio entero.
Si el `<extra-code-model>` de una entidad necesita llamar a algo, ese algo **MUST** vivir aquí o en `base/`, nunca en el tramitador.
Es justo lo que hace `util/ExpedienteDocumentoPdfUtil`, el destino del `getDocumentoPdf(...)` que cada tipo de expediente lleva en su `domains.xml`.

Para llegar a la máquina de estados desde fuera está `ExpedienteUtil.getTipoExpedienteStates(tipoExpediente)`, **en el tramitador**; la entidad `TipoExpediente` no la expone, precisamente por esto.

## Qué entra aquí

| Lo que quieres añadir | Dónde va |
|---|---|
| Un campo, una entidad, una relación o sus datos iniciales del expediente | **aquí** |
| Algo que el `<extra-code-model>` de una entidad de expediente necesita llamar | **aquí** (o `base/`), nunca en el tramitador |
| Algo común a todos los expedientes que no es tramitación: ni estados, ni eventos (p. ej. las notas: `util/ExpedienteNotasUtil` y `controller/`) | **aquí** |
| La máquina de estados, los `EventManager`, el `Tramitador`, sus servicios o sus controladores | [`subsystem/tramitador`](../tramitador/CLAUDE.md) |
| Lo que necesita un tipo de expediente concreto para implementarse | su carpeta de versión, o `tramites/util/` si lo comparten varios |
