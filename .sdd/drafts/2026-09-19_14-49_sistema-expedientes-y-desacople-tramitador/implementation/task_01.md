---
type: implementation-task
template: system
---

# Tarea 01 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas

**Alcance de esta tarea:** materializar el dominio XML `NuevoExpediente` (fila `Crear` de la tabla). El fichero **ya está materializado y validado** en `/home/logongas/Documentos/desarrollo/educaflow/secretaria-virtual/.sdd/drafts/2026-09-19_14-49_sistema-expedientes-y-desacople-tramitador/design/domains/NuevoExpediente.xml`. **MUST** copiarlo **literalmente** (`cp`) a `src/main/java/com/educaflow/system/expedientes/domains/NuevoExpediente.xml` **sin regenerarlo, reformatearlo ni editarlo** (`implementation.md` §1). La acción es `Crear`: el destino **no debe existir** (si existe, `CONFLICT`). Las demás partes del «Paso 1» (borrar el modelo `ContextoTramitacion`) las cubre la tarea 03.

**Del diseño — fila de la tabla «Ficheros a crear o modificar» (verbatim)**

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/system/expedientes/domains/NuevoExpediente.xml` | Crear | k-sistemas (modelos.md) | Modelo de pantalla de «Nuevo expediente» (copia verbatim de `design/domains/NuevoExpediente.xml`) |

**Del diseño — Paso 1 (verbatim; la parte «Borrar» la cubre la tarea 03)**

### Paso 1 — Dominio `NuevoExpediente` y retirada del modelo `ContextoTramitacion`

**Crear** `system/expedientes/domains/NuevoExpediente.xml` copiando `design/domains/NuevoExpediente.xml`.

Resumen estructural de `domains/NuevoExpediente.xml` (módulo `sysexpedientes`, paquete `com.educaflow.system.expedientes.db`, entidad `persistable="false"`):

| Campo | Tipo | Origen | Spec |
|---|---|---|---|
| `tramite` | many-to-one `Tramite` | cliente | campo «trámite» |
| `centro` | many-to-one `Centro` | cliente | campo «centro» |
| `presentadoEnPapel` | boolean | cliente | campo «presentado en papel» |
| `presentadoEnRepresentacion` | boolean `nullable="true"` | cliente | campo «presentado en representación» («mientras no contesta, no tiene valor») |
| `nombreTramite` | string | servidor | CC-NuevoExpediente-001 |
| `ayudaTramite` | string `large` | servidor | CC-NuevoExpediente-002 |
| `centrosDisponibles` | many-to-many `Centro` | servidor | CC-NuevoExpediente-003 |
| `hayUnSoloCentroDisponible` | boolean | servidor | Condición «hay un solo centro disponible» de RUI-nuevo-expediente-formulario-003/004 (campo añadido por el diseño) |
| `hayQuePreguntarPresentacion` | boolean | servidor | CC-NuevoExpediente-004 |
| `sePuedeCrearParaMi` | boolean | servidor | CC-NuevoExpediente-005 |
| `sePuedeCrearEnRepresentacion` | boolean | servidor | CC-NuevoExpediente-006 |
| `hayQuePreguntarParaQuien` | boolean | servidor | Condición «se puede crear para uno mismo y en representación» de RUI-nuevo-expediente-formulario-011/012/013/016 (campo añadido por el diseño) |
| `presentadoEnPapelDeducido` | boolean `nullable="true"` | servidor | CC-NuevoExpediente-007 (`true` en papel, `false` telemático, `null` sin valor) |
| `presentadoEnPapelVigente` | boolean | servidor | Forma de presentar vigente (la deducida si la hay; si no, la de la ventana), que la vista copia a `presentadoEnPapel` en RUI-007 (campo añadido por el diseño, D4) |

`nullable="true"` es necesario en los dos booleanos que admiten «sin valor»: sin él el getter generado por Axelor devuelve `FALSE` cuando el valor es `null`, y `V-ContextoTramitacion-004` no podría distinguir «para mí» de «sin contestar».
Ningún campo lleva `profile`: el perfil lo decide el servidor (R-NuevoExpediente-001), así que la ventana no tiene por dónde enviarlo.
`hayUnSoloCentroDisponible`, `hayQuePreguntarParaQuien` y `presentadoEnPapelVigente` existen para que cada condición o decisión de la vista tenga un único dueño: el servidor la calcula una vez y todos los atributos y acciones de la vista que dependen de ella la consultan, en vez de repetir la expresión en JS y en Groovy.

Verificación: `validate.sh` da `VALIDACION-XML: OK` para `domains/NuevoExpediente.xml`; `grep -rn "views-models/ContextoTramitacion" src` sin resultados.
La compilación **no** se comprueba en los Pasos 1 a 5: hasta el Paso 6 quedan usos del modelo anterior (`ExpedienteController.triggerInitialEvent`, `ContextoTramitacionController`) que se retiran allí; el primer `./gradlew compileJava` que debe pasar es el del Paso 6.

**Del diseño — Trazabilidad de campos calculados (CC) que este dominio declara (verbatim)**

### Campos calculados (CC, `momento: lectura`)

Son campos `servidor` de un modelo que no se persiste: no hay columna ni `formula`, porque su cálculo necesita al usuario y sus perfiles (servicios); los calcula la acción correspondiente y se devuelven a la vista.

| CC | Campo del modelo | Lo calcula |
|---|---|---|
| CC-NuevoExpediente-001 | `nombreTramite` | R-NuevoExpediente-002 |
| CC-NuevoExpediente-002 | `ayudaTramite` | R-NuevoExpediente-002 |
| CC-NuevoExpediente-003 | `centrosDisponibles` | R-NuevoExpediente-003 |
| CC-NuevoExpediente-004 | `hayQuePreguntarPresentacion` | R-NuevoExpediente-004 |
| CC-NuevoExpediente-005 | `sePuedeCrearParaMi` | R-NuevoExpediente-005 |
| CC-NuevoExpediente-006 | `sePuedeCrearEnRepresentacion` | R-NuevoExpediente-005 |
| CC-NuevoExpediente-007 | `presentadoEnPapelDeducido` | R-NuevoExpediente-004 |

Campos `servidor` añadidos por el diseño (no son CC del spec: son la condición o el valor de una regla de UI, calculados una sola vez para que la vista no los repita): `hayUnSoloCentroDisponible` (R-NuevoExpediente-003), `presentadoEnPapelVigente` (R-NuevoExpediente-004) y `hayQuePreguntarParaQuien` (R-NuevoExpediente-005).

**Del diseño — Notas y supuestos aplicables (verbatim)**

5. **Nombre de módulo del dominio:** `sysexpedientes` (paquete `com.educaflow.system.expedientes.db`), distinto del `expedientes` del subsistema para no mezclar los dos módulos mientras conviven con el mismo nombre de carpeta (como `gestioncentro` usa `gestion`).
