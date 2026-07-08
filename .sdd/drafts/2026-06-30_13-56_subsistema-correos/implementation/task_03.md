---
type: implementation-task
---

# Tarea 03 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas

## Ficheros que cubre esta tarea (fila de la tabla "Ficheros a crear o modificar" de `design.md`)

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/subsystem/correos/domains/Adjunto.xml` | Crear | k-sistemas (modelos.md) | Entidad `Adjunto` |

## Instrucción de materialización — XML ya materializado, NO regenerar

El fichero **ya está completo y validado** en `design/domains/Adjunto.xml` de esta iniciativa. **MUST** copiarlo **literalmente** (`cp`, sin reescribir ni reformatear) a `src/main/java/com/educaflow/subsystem/correos/domains/Adjunto.xml` (crear la carpeta con `mkdir -p` si no existe). **MUST NOT** regenerarlo desde el resumen de abajo: el resumen es solo contexto, la fuente de verdad es el propio fichero XML de `design/`.

## Texto del diseño (verbatim, `design.md`, Paso 2 — parte de `Adjunto.xml`)

Fichero `domains/Adjunto.xml` (completo en `design/domains/Adjunto.xml`). Resumen: entidad `Adjunto` con `nombreFichero`, `contenido` (`many-to-one` a `com.axelor.meta.db.MetaFile`, patrón estándar de adjuntos del proyecto — ver `k-sistemas/modelos.md`), `correo` (`many-to-one` a `Correo`), y `<unique-constraint columns="correo,nombreFichero"/>` (RES-Adjunto-001, declarativo).

**Ninguno de los campos "obligatorios" del negocio (`dniDestinatario`, `nombre`, `apellidos`, `para`, `asunto`, `cuerpo`, `centro`, `nombreFichero`, `contenido`) lleva `required="true"`.** Ver "Notas y supuestos" — la obligatoriedad la exige el `VAL-` correspondiente en `validateInsert`, no el atributo declarativo, para que el mensaje de negocio (no un genérico de JPA) sea siempre el que se muestra.

**Verificar:** `bash .claude/skills/sdd-designer/template-system/validate.sh <carpeta>` (o el `xmllint` equivalente) valida ambos ficheros contra `domain-models.xsd`.

**MUST NOT** crear ningún otro campo/relación que no esté en `design/domains/Adjunto.xml`.
