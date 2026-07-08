---
type: implementation-task
---

# Tarea 02 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas

## Ficheros que cubre esta tarea (fila de la tabla "Ficheros a crear o modificar" de `design.md`)

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/subsystem/correos/domains/Correo.xml` | Crear | k-sistemas (modelos.md) | Entidad `Correo` |

## Instrucción de materialización — XML ya materializado, NO regenerar

El fichero **ya está completo y validado** en `design/domains/Correo.xml` de esta iniciativa. **MUST** copiarlo **literalmente** (`cp`, sin reescribir ni reformatear) a `src/main/java/com/educaflow/subsystem/correos/domains/Correo.xml` (crear la carpeta con `mkdir -p` si no existe). **MUST NOT** regenerarlo desde el resumen de abajo: el resumen es solo contexto para quien materialice, la fuente de verdad es el propio fichero XML de `design/`.

## Texto del diseño (verbatim, `design.md`, Paso 2 — parte de `Correo.xml`)

Fichero `domains/Correo.xml` (completo en `design/domains/Correo.xml`). Resumen estructural: entidad `Correo` con los datos del destinatario (`dniDestinatario`, `nombre`, `apellidos`, texto libre, sin ficha de persona), las direcciones (`para`/`enCopia`/`enCopiaOculta`, cada una una lista de direcciones separadas por comas en un único `<string large="true">`), `asunto`/`cuerpo`, `centro` (`many-to-one` a `com.educaflow.subsystem.common.db.Centro`), `historialEstado` opcional (`many-to-one` a `com.educaflow.subsystem.expedientes.db.HistorialEstado`), `adjuntos` (`one-to-many` a `Adjunto`, `mappedBy="correo"`), los campos de resultado del envío (`estado` enum `EstadoCorreo`, `fechaCreacion`, `fechaPrimerIntentoEnvio`, `fechaUltimoIntentoEnvio`, `fechaEnvio`, `numeroReintentos`, `descripcionUltimoFallo`) y el campo derivado de solo lectura `nombreExpediente` (CC-Correo-007, `formula="true"` con subselect SQL sobre `expedientes_historial_estado`/`expedientes_expediente` — no cuerpo Java, porque el cálculo navega dos relaciones a otras tablas y se usa como columna de grid; ver `k-validaciones/reglas-negocio.md` §2.2). Enum `EstadoCorreo` con `PENDIENTE`/`SUCCESS`/`FAIL`. `finder-method findByEstado`.

**Ninguno de los campos "obligatorios" del negocio (`dniDestinatario`, `nombre`, `apellidos`, `para`, `asunto`, `cuerpo`, `centro`, `nombreFichero`, `contenido`) lleva `required="true"`.** Ver "Notas y supuestos" — la obligatoriedad la exige el `VAL-` correspondiente en `validateInsert`, no el atributo declarativo, para que el mensaje de negocio (no un genérico de JPA) sea siempre el que se muestra.

**Verificar:** `bash .claude/skills/sdd-designer/template-system/validate.sh <carpeta>` (o el `xmllint` equivalente) valida ambos ficheros contra `domain-models.xsd`.

### Nota y supuesto aplicable (verbatim, `design.md`)

1. **Ningún campo "obligatorio" de negocio lleva `required="true"` declarativo.** El flujo de guardado de Axelor persiste primero el bean tal cual (`JPA.manage`/`persist`+`flush`) y **solo después** invoca `modelService.insert` (donde vive `validateInsert`) — ver `k-sistemas/modelos.md` "REGLA CRÍTICA — Campos rellenados por el sistema...". Si un campo de negocio (`asunto`, `centro`, etc.) llevara `required="true"`, un envío incompleto fallaría con un `ConstraintViolationException` genérico de JPA **antes** de que `validateInsert` tuviera ocasión de dar el mensaje amigable que pide cada `VAL-` de la especificación (p.ej. "El asunto es obligatorio"). Por eso `asunto`/`cuerpo`/`para`/`enCopia`/`enCopiaOculta` se declaran `large="true"` (columna de texto sin límite físico) en vez de dejar el límite por defecto de un `<string>`: así ni siquiera una entrada de 256 caracteres (ESC-023) trunca a nivel de base de datos antes de que `V-Correo-010` pueda dar su mensaje ("El asunto no puede superar 255 caracteres").

**MUST NOT** crear ningún otro campo/relación que no esté en `design/domains/Correo.xml`.
