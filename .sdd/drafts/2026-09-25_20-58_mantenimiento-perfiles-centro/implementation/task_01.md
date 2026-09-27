---
type: implementation-task
template: system
---

# Tarea 01 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas

# Diseño: Mantenimiento de perfiles de trámites por centro

**Objetivo:** que el supervisor (en los centros que supervisa) y el administrador (en cualquier centro) puedan ver, crear, modificar y borrar las filas de `AceProfileCentro`, con las reglas que garantizan que cada fila dice sin ambigüedad a quién se da el perfil.
**Capa:** subsystem/security
**Especificación de origen:** .sdd/drafts/2026-09-25_20-58_mantenimiento-perfiles-centro/specification.md
**Skills necesarios para la implementación:** k-sistemas, k-validaciones, k-code-quality, k-secure-coding, k-vistas, k-datainit

Las decisiones difíciles y sus alternativas están en `decisiones.md` (D1–D6); este documento es coherente con ellas.

## Ficheros a crear o modificar

Rutas relativas a `src/main/java/com/educaflow/`.

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/security/domains/AceProfileCentro.xml` | Modificar (sin delta) | k-sistemas (modelos.md) | La spec no cambia el modelo: el fichero del diseño es **idéntico** al real (copiarlo es un no-op). Se incluye porque el contrato exige un `domains/<Entidad>.xml` por entidad. |

> **Nota para `/sdd-implementer`:** los XML de `domains/`, `views/` y `menus.xml` ya están materializados en la carpeta `design/`. **MUST NOT** modificarlos, reescribirlos ni regenerarlos: se **copian verbatim** a su ubicación final (`menus.xml` se fusiona en el `menus.xml` único del proyecto). El código Java es lo único que se implementa a partir de las firmas y comentarios del diseño. Los fragmentos de `auth-security.xml`, `input-config.xml` y `CLAUDE.md` del Paso 7 se aplican **añadiendo** a lo que ya hay (no se borra nada).

**Instrucción de materialización (XML ya materializado):** el fichero ya está materializado en `design/domains/AceProfileCentro.xml` y se **copia literalmente** a `src/main/java/com/educaflow/subsystem/security/domains/AceProfileCentro.xml`, **sin regenerarlo** (ver `implementation.md` §1). La fila es `Acción: Modificar (sin delta)`: el destino **ya existe**; antes de sobrescribir aplica la **comprobación de conservación** de `implementation.md` §3 (el `design.md` no tiene sección `## Eliminaciones declaradas`, así que no se elimina nada).

## Pasos

### Paso 1 — Dominio `AceProfileCentro` (sin cambios)

Fichero: `design/domains/AceProfileCentro.xml`.

- **Preexistente (se conserva):** la entidad completa (`perfil`, `tipoUsuario`, `cargo`, `usuario`, `centro`, `tramite`, `repository="abstract"`), incluidos los `required="true"` de `perfil`, `centro` y `tramite`, que el formulario hereda (U-…-004 / U-…-003 de cada pantalla).
- **Delta:** ninguno. La unicidad (RES-007) **no** se declara con `<unique-constraint>` (decisiones D4: PostgreSQL trata los `NULL` como distintos y el mensaje no sería el de la spec).

**Verificar:** `diff design/domains/AceProfileCentro.xml src/main/java/com/educaflow/subsystem/security/domains/AceProfileCentro.xml` no muestra diferencias.

## Trazabilidad Origen spec → V/R/U → ubicación (filas que citan este fichero)

| ID | Origen spec | Ubicación | Qué comprueba |
|----|-------------|-----------|---------------|
| V-AceProfileCentro-001 | RES-AceProfileCentro-001 | `AceProfileCentroServiceImpl.validarObligatorios` (desde `validateInsert`/`validateUpdate` → `validarFila`, fase 1); `required` en `domains/AceProfileCentro.xml` | Centro indicado. |
| V-AceProfileCentro-002 | RES-AceProfileCentro-002 | ídem | Trámite indicado. |
| V-AceProfileCentro-003 | RES-AceProfileCentro-003 | ídem | Perfil indicado. |
