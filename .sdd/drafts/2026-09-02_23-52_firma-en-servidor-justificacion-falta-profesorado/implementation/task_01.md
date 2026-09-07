---
type: implementation-task
template: expediente
---

# Tarea 01 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- `k-tipo-expediente`
- `k-validaciones`
- `k-secure-coding`

Copia el fichero `domains.xml` ya materializado en el diseño al árbol del proyecto.

Dentro de `k-tipo-expediente` lee `modelo.md`.

## Nota del descomponedor (decisiones de esta descomposición)

Esta es una **iniciativa de MODIFICACIÓN** de la versión existente `src/main/java/com/educaflow/tramites/profesores/justificacion_falta_profesorado/actual/v1/`, así que **todos** los bloques de tareas son condicionales y solo existe tarea para los ficheros que la tabla §6 del `design.md` lista. Por eso **no hay** tarea de `TramiteInstance.xml`, ni de `TipoExpedienteInstance.xml`/`estados.puml`, ni de `CreateFilesTask` (el delta no añade fases; el propio diseño lo prohíbe expresamente en §14), ni de `views.xml` de la raíz de la versión, ni de `InitialEventManagerImpl.java`, ni de `documentospdf/`, ni de `permisos-demo.xml`.

## Fichero de esta tarea — fila verbatim de la tabla §6 del `design.md`

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/tramites/profesores/justificacion_falta_profesorado/actual/v1/domains.xml` | Modificar | `k-tipo-expediente` (`modelo.md`) | Cópialo verbatim de `design/domains.xml`. Añade los dos campos transitorios de §4.1 |

## Identidad del trámite y del tipo (verbatim del `design.md` §2)

## 2. Identidad del trámite y del tipo

| Dato | Valor |
|---|---|
| `code` del trámite | `JustificacionFaltaProfesorado` |
| Nombre visible (`<name>`) | Justificación de falta del profesorado |
| `tipoTramite` | `PROFESOR` |
| Carpeta del trámite | `src/main/java/com/educaflow/tramites/profesores/justificacion_falta_profesorado/` |
| Carpeta de la versión | `src/main/java/com/educaflow/tramites/profesores/justificacion_falta_profesorado/actual/v1/` |
| `<defaultTipoExpediente>` | `v1` |
| `code` / entidad del tipo | `JustificacionFaltaProfesoradoV1` |
| FQN de la entidad | `com.educaflow.subsystem.expedientes.db.JustificacionFaltaProfesoradoV1` |
| `basePackageName` | `com.educaflow.tramites.profesores.justificacion_falta_profesorado.actual.v1` |
| Clase `States` (generada) | `com.educaflow.tramites.profesores.justificacion_falta_profesorado.actual.v1.States` |
| Form plantilla | `exp-JustificacionFaltaProfesoradoV1-Templates` |
| Modificación de | `src/main/java/com/educaflow/tramites/profesores/justificacion_falta_profesorado/actual/v1/` |

## Paso del diseño (verbatim del `design.md` §7)

### Paso 1 — `domains.xml` del tipo

El fichero está materializado en `design/domains.xml`. **Cópialo literalmente** a `src/main/java/com/educaflow/tramites/profesores/justificacion_falta_profesorado/actual/v1/domains.xml`, **sobrescribiendo** el fichero actual. **MUST NOT** modificarlo, reescribirlo ni regenerarlo.

**Verificación:** el fichero existe; `diff` con el del diseño vacío; el fichero declara los dos campos nuevos, ninguno con `required="true"`, y la entidad `JustificacionFaltaProfesoradoV1` sigue siendo la **primera** `<entity>` con `extends="Expediente"`.

## Especificación del modelo (verbatim del `design.md` §4)

## 4. Modelo

El delta **no crea ninguna entidad ni ningún enum**: reutiliza el enum `com.educaflow.subsystem.firmas.db.SituacionFirma` que ya existe en el subsistema de firmas. Solo añade **dos campos transitorios** a la entidad del tipo.

### 4.1 Campos que añade el delta a `JustificacionFaltaProfesoradoV1`

| nombre | tipo | ref | title | para qué sirve | quién lo rellena |
|---|---|---|---|---|---|
| `claveFirmaDocumentoEntrada` | `string` (`transient="true"`, `password="true"`) | — | Firma de la solicitud | La contraseña del fichero del certificado o el PIN del dispositivo criptográfico que teclea quien firma, cuando la secretaría virtual **no** custodia esa clave. Dato de un solo uso | `usuario` |
| `situacionFirmaDocumentoEntrada` | `enum` (`transient="true"`, **derivado**, con cuerpo) | `com.educaflow.subsystem.firmas.db.SituacionFirma` | Situación de firma de la solicitud | Cómo se va a firmar la solicitud: se deduce de `dniFirmaDocumentoEntrada` y del certificado digital **habilitado** dado de alta para ese DNI (CC-001) | `servidor` |

Los demás campos de la entidad *(sin cambios)*.

Reglas que hay que hacer explícitas:

- **`claveFirmaDocumentoEntrada` es `transient`**: nunca llega a base de datos, así que la clave no se persiste ni queda en el histórico. Es el mismo mecanismo que `TareaFirma.claveFirma`.
- **El nombre del campo es contrato del subsistema, no una convención tácita.** `ExpedienteController` lo necesita para devolverlo vacío tras un intento cancelado (RUI-011, §9.1), y **MUST NOT** incrustarse a mano allí: el nombre vive en **una sola** constante pública,
  `com.educaflow.subsystem.expedientes.services.internal.FirmaDocumentoEntradaService.CAMPO_CLAVE_FIRMA_DOCUMENTO_ENTRADA = "claveFirmaDocumentoEntrada"` (§9.1).
  Todo tipo de expediente que quiera firma en servidor **MUST** llamar así a su campo transitorio de la clave: es el nombre que esa constante declara y el único que el subsistema sabe vaciar.
- **El `title` del campo es deliberadamente neutro (`Firma de la solicitud`) y NO nombra la clave.** `FieldValidationRules` antepone **siempre** el `title` del `field(...)` del que cuelga cada regla y el visor pinta `«<title>: <mensaje>»`; como el DSL no permite colgar una regla fuera de un `field(...)`, las reglas VAL-…-001 y VAL-…-008 —que no hablan de la clave— colgarían de una etiqueta «Clave del certificado digital» que no viene a cuento (§14).
  La pantalla no se ve afectada: la vista sobrescribe el `title` por «Contraseña» y por «PIN» en el panel de cada situación (§10.2, `fases/recepcion/views.xml`).
- **`situacionFirmaDocumentoEntrada` es un campo derivado**: su cuerpo es `return com.educaflow.subsystem.firmas.util.SituacionFirmaBuilder.buildByDni(getDniFirmaDocumentoEntrada());`, se **recalcula en cada lectura** y no se persiste. Es la única excepción a la regla «todo campo `servidor` está asignado por alguna acción de §8 o §9»: lo asigna su propio getter, y por eso **MUST NOT** aparecer en ningún `field(...)` del validador ni ser editable en ninguna vista. Que se recalcule en cada lectura es justo lo que hace que las comprobaciones de `PRESENTAR` se hagan contra la situación **real del servidor en el momento de presentar** y no contra lo que la pantalla tuviera pintado.
- El getter que Axelor genera para un campo derivado solo captura `NullPointerException`, así que `SituacionFirmaBuilder.buildByDni` **MUST NOT** propagar ninguna excepción (§9.1).
- **MUST NOT** ponerse `required="true"` en ninguno de los dos campos: la obligatoriedad de la clave es **por pareja (estado, evento)** y vive en el DSL del validador (§10).
- **MUST NOT** redeclararse ningún campo heredado de `Expediente`; `dniFirmaDocumentoEntrada` se **referencia**, nunca se redeclara.

> **Por qué los dos campos van en el `domains.xml` del tipo y no en `Expediente`.** Sería más reutilizable declararlos en `Expediente`, junto a `dniFirmaDocumentoEntrada`, pero **no funcionaría**: `FieldValidationRules.getLabel` resuelve la etiqueta del campo con `clazz.getDeclaredField(...)` sobre la **clase concreta** del expediente, que **no** recorre superclases, así que un `field(model::getClaveFirmaDocumentoEntrada)` sobre un campo heredado revienta con `«El campo … no existe en la clase …»`. Cada tipo que quiera firma en servidor declara estos dos campos en su propio `domains.xml` (seis líneas); todo lo demás —el cálculo de la situación, la firma y las reglas de validación— es compartido (§9.1, §10.1).

### 4.2 `design/domains.xml` materializado

`design/domains.xml` es el fichero real de la versión **más** los dos campos anteriores. Se copia verbatim y sobrescribe.


## Documentos PDF (verbatim del `design.md` §5)

## 5. Documentos PDF

*(sin cambios)* — el trámite sigue generando exactamente los mismos dos documentos, con los mismos ficheros de definición, el mismo contenido y el mismo recuadro de firma. **MUST NOT** tocarse `documentospdf/solicitud.xml`, `documentospdf/resolucion.xml` ni `documentospdf/_template.xml`, ni el bloque `<extra-code-model>` del `domains.xml`.

| fichero | constante | en qué transición se genera | en qué campo se guarda | quién lo firma | se registra |
|---|---|---|---|---|---|
| `documentospdf/solicitud.xml` | `SOLICITUD` | `RECEPCION.ENTRADA_DATOS` + `GUARDAR_DATOS` (se genera) / `RECEPCION.PENDIENTE_PRESENTACION` + `PRESENTAR` (se firma) | `pdfSolicitud` (`servidor`) → `pdfSolicitudFirmado` (`servidor`) | **cambia**: `cliente (AutoFirma__!!)` cuando no hay certificado custodiado; `servidor: DNI dniFirmaDocumentoEntrada` cuando sí lo hay | `entrada` |
| `documentospdf/resolucion.xml` | `RESOLUCION` | `TRAMITACION.PENDIENTE_RESOLUCION` + `RESOLVER` | `pdfResolucion` (`servidor`) | `servidor: DIRECTOR` *(sin cambios)* | `salida` |

Fragmentos: `documentospdf/_template.xml`, incluido por `solicitud.xml` y por `resolucion.xml` *(sin cambios)*.

El recuadro y la página de la firma de la solicitud son **los mismos** se firme donde se firme: `x=100, y=20, ancho=600, alto=100`, página `1` — los ocho argumentos que ya usa hoy la `<action-method>` de AutoFirma. En el camino de servidor los aporta la constante `POSICION_FIRMA_SOLICITUD` del `PhaseEventManagerImpl` de `RECEPCION` (§9.2).

## Reparto de reglas — filas que ubican una regla en el modelo (verbatim del `design.md` §11)

| Tipo de regla | Capa | Cómo se escribe |
|---|---|---|
| Tipo, longitud máxima de columna, referencia, enumerado | **modelo XML** (`domains.xml`) | atributos de `<string>`/`<enum>`; aquí, `transient`, `password` y el cuerpo del campo derivado |

| Regla | Capa | Dónde |
|---|---|---|
| CC-001 — cómo se va a firmar la solicitud | modelo XML (campo derivado) | `situacionFirmaDocumentoEntrada` en `domains.xml` (§4.1), calculado por `SituacionFirmaBuilder.buildByDni` (§9.1) |
| CC-004 — clave con la que se abre el certificado | modelo XML + servicio | `claveFirmaDocumentoEntrada` (§4.1); que **la custodiada gane a la tecleada** ya lo decide `CertificadoDigitalService.getAlmacenClaveByDni(dni, clave)` (§9.1). **Cubierta solo en las situaciones de certificado en fichero:** en `DISPOSITIVO_CON_PIN` / `DISPOSITIVO_SIN_PIN` ese mismo método **descarta** la clave recibida, así que la parte «y solo si no la custodia, la que el profesor teclea» de CC-004 queda **cableada pero no operativa** para dispositivo criptográfico (§14 «Situaciones sin escenario») |
| RUI-…-CREADOR-009 — la clave aparece vacía al abrir | modelo XML | el campo es `transient`: nace nulo en cada carga del formulario. **No hace falta ninguna acción de vista** |

## Notas y supuestos que aplican a esta tarea (verbatim del `design.md` §14)

- **Por qué los dos campos transitorios van en el `domains.xml` del tipo y no en `Expediente`.** Explicado en §4.1: `FieldValidationRules.getLabel` usa `getDeclaredField`, que no recorre superclases, así que un campo heredado no puede aparecer en un `field(...)` del validador. Es la razón por la que `dniFirmaDocumentoEntrada` (que sí está en `Expediente`) solo se usa como **argumento** de `FirmaPdf` y nunca como `field(...)`. Alternativa descartada: parchear `getLabel` para que recorra la jerarquía — es un cambio en `base.infrastructure` que afecta a todas las validaciones del proyecto y queda fuera del alcance de esta iniciativa.
- **Enum compartido entre módulos de dominio.** `situacionFirmaDocumentoEntrada` referencia `com.educaflow.subsystem.firmas.db.SituacionFirma` desde un `domains.xml` del módulo `expedientes`. El generador de Axelor usa el `ref` de un `<enum>` **tal cual** como tipo Java, así que un FQN de otro módulo funciona; es la primera vez que el proyecto lo hace, y por eso el paso 12 lo comprueba (basta con que compile y con que el valor llegue al formulario). Alternativa descartada: duplicar el enum en el subsistema de expedientes — las guías piden reutilizar el de firmas.
- **Coste del campo derivado.** `BeanMapperModel.getEntityCloned` clona el expediente con «allow all» al principio de cada evento, así que llamará al getter derivado y hará una consulta de `CertificadoDigital` por evento de este tipo de expediente (ninguna si el DNI es nulo o inválido). Es el mismo coste que ya paga `TareaFirma.situacionFirma` y se ha considerado asumible.
- **Sin `permisos.xml` y sin `CreateFilesTask`.** El delta no añade perfiles ni asignaciones, así que no hay fragmento de permisos; y no añade ninguna fase nueva, así que **MUST NOT** ejecutarse `CreateFilesTask`: no hay ningún esqueleto que generar y la tarea no pinta nada en este delta.
- **Git.** Las guías prohíben expresamente tocar nada de git durante toda la iniciativa (ni commits, ni ramas, ni stash).

## Instrucciones

- El fichero está **materializado** en `/home/logongas/Documentos/desarrollo/educaflow/secretaria-virtual/.sdd/drafts/2026-09-02_23-52_firma-en-servidor-justificacion-falta-profesorado/design/domains.xml`. **Cópialo literalmente** a `src/main/java/com/educaflow/tramites/profesores/justificacion_falta_profesorado/actual/v1/domains.xml`, **sobrescribiendo** el fichero actual. **MUST NOT** modificarlo, reescribirlo ni regenerarlo, y **MUST NOT** transcribirlo a mano: se copia tal cual.
- Acción de la fila §6: **Modificar**.
- **MUST NOT** crearse ni tocarse ningún `i18n_es.csv` / `i18n_ca.csv`, ningún `States.java`, ningún `estados.png` ni nada bajo `build/`: son generados.
- **La especificación de arriba es contrato fijo y la superficie es cerrada**: MUST NOT crearse ningún método, clase, campo, constante ni acción que esa especificación no liste, y MUST NOT modificarse ningún fichero que no sea el de esta tarea.
