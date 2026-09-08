---
type: implementation-task
template: system
---

# Tarea 02 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas

## Fichero ya materializado en el diseño — se COPIA LITERALMENTE

El XML de este dominio **ya está materializado y validado con `xmllint`** por el diseñador en:

`/workspace/secretaria-virtual/.sdd/drafts/2026-09-08_02-53_nombre-apellidos-certificado-digital/design/domains/CertificadoDigital.xml`

**MUST** copiarse **literalmente** (byte a byte, fichero completo resultante) a su ruta destino:

`/workspace/secretaria-virtual/src/main/java/com/educaflow/subsystem/criptografia/domains/CertificadoDigital.xml`

**MUST NOT** regenerarlo, reescribirlo ni reinterpretarlo a partir del texto de abajo: el texto del diseño es la explicación del delta, el XML del `design/` es el contrato.

La fila es `Acción: Modificar`: el fichero destino **ya existe**. Antes de sobrescribirlo aplica la **comprobación de conservación** de `implementation.md` §3 (el XML del diseño es el fichero completo resultante y debe conservar todo lo preexistente que el «Resumen estructural» de abajo lista como conservado; si algo preexistente no estuviera en el XML del diseño, **detente y repórtalo** en vez de fusionar a mano).

## Fila de la tabla «Ficheros a crear o modificar»

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/subsystem/criptografia/domains/CertificadoDigital.xml` | Modificar | k-sistemas (modelos.md) | Añade `nombre`, `apellidos` y `nombreTomadoDelUsuario`; quita `unique` de `dni`; sustituye el finder `findByDni` por `findByDniHabilitados` |

> **Nota para `/sdd-implementer`:** los XML de `domains/`, `views/` y `menus.xml` ya están materializados en la carpeta `design/`. **MUST NOT** modificarlos, reescribirlos ni regenerarlos: se **copian verbatim** a su ubicación final (`menus.xml` se fusiona en el `menus.xml` único del proyecto y, en esta iniciativa, la fusión es un no-op porque el `<menuitem>` ya existe idéntico). El código Java es lo único que se implementa a partir de las firmas y comentarios del diseño.

## Texto del diseño (verbatim)

### Paso 2 — Dominio `CertificadoDigital`

**Fichero del diseño:** `design/domains/CertificadoDigital.xml` → `src/main/java/com/educaflow/subsystem/criptografia/domains/CertificadoDigital.xml` (**Modificar**, fichero completo resultante).

**Resumen estructural:**

- **Preexistente (se conserva):** el `<module name="criptografia">`; los campos `dni` (`required`), `tipoCertificado`, `fichero`, `password`, `dispositivoCriptografico`, `alias`, `rutaClasspath`, `rutaSistemaArchivos`, `enabled`; y el `<enum name="TipoUbicacionCertificado">` con sus cuatro `<item>` (títulos verbatim, incluido `CLASSPATH`).
- **Delta:**
  - `dni`: se retira `unique="true"` (RES-CertificadoDigital-002). Sigue siendo `required="true"`.
  - `+ <string name="nombre" title="Nombre">` — nombre de pila del titular. Sin `required`: la obligatoriedad es **condicional** (solo cuando no hay usuario con ese DNI), y las validaciones corren **antes** que las action rules que lo rellenan, así que un `required` declarativo rechazaría altas legítimas.
  - `+ <string name="apellidos" title="Apellidos">` — ídem.
  - `+ <boolean name="nombreTomadoDelUsuario" default="false" title="Nombre tomado del usuario">` — materializa `CC-CertificadoDigital-001`. El getter que genera Axelor para un `<boolean>` es null-safe (devuelve `Boolean.FALSE` cuando el valor es nulo), de modo que las filas anteriores a este cambio se comportan como «escrito por el administrador», tal y como pide el spec.
  - `− <finder-method name="findByDni" …>` (ver `## Eliminaciones declaradas`).
  - `+ <finder-method name="findByDniHabilitados" using="String:dni" filter="self.dni = :dni AND self.enabled = true" all="true"/>` — con `all="true"` el método generado devuelve un `Query<CertificadoDigital>`, así que sirve tanto para la lectura (`.fetchOne()`, el certificado vigente de la persona) como para la validación de RES-CertificadoDigital-001 (`.fetch()`, para comprobar si el habilitado que ya existe es otro registro). El nombre arranca con el prefijo **`findBy`** que `domain-models.xsd` fija como convención para `<finder-method>` («As a convention, always use `findBy` prefix») y que siguen los finders ya existentes del proyecto (`findByDni` en `User.xml`, `findByEstado` en `Correo.xml`); el sufijo `Habilitados` indica el filtro extra por `enabled`, y **no** es un parámetro más del método (la firma generada sigue siendo `findByDniHabilitados(String dni)`).

**Verificar:** `./gradlew compileJava` genera `CertificadoDigital` con `getNombre`/`getApellidos`/`getNombreTomadoDelUsuario` y `CertificadoDigitalRepository.findByDniHabilitados(String)`; `grep -n 'unique' src/main/java/com/educaflow/subsystem/criptografia/domains/CertificadoDigital.xml` no devuelve nada.

### Eliminaciones declaradas que afectan a este fichero (verbatim)

| Elemento preexistente eliminado | Fichero | ID de spec que lo justifica |
|---|---|---|
| Atributo `unique="true"` del campo `dni` | `src/main/java/com/educaflow/subsystem/criptografia/domains/CertificadoDigital.xml` | RES-CertificadoDigital-002 |
| `<finder-method name="findByDni" using="String:dni" filter="self.dni = :dni"/>` | `src/main/java/com/educaflow/subsystem/criptografia/domains/CertificadoDigital.xml` | RES-CertificadoDigital-002 — con el DNI ya no único, un finder que devuelve «el» certificado de un DNI devolvería uno arbitrario (`fetchOne` aplica `LIMIT 1`, sin error). Lo sustituye `findByDniHabilitados` |

### Reglas materializadas en el modelo de dominio (verbatim)

| Regla del spec | Ubicación | Cómo |
|---|---|---|
| RES-CertificadoDigital-002 | `domains/CertificadoDigital.xml` (campo `dni` sin `unique`) + `V2__certificado_digital_dni_no_unico.sql` + eliminación del bloque de unicidad en `validateCertificado` | La restricción se **retira**: en el modelo (deja de declararse), en la base de datos ya creada (migración Flyway) y en el servicio (desaparece el rechazo por DNI repetido). Su contrapartida positiva es V-CertificadoDigital-005, que la sustituye por «solo uno habilitado» |
| CC-CertificadoDigital-001 | `domains/CertificadoDigital.xml` (campo `nombreTomadoDelUsuario`, `momento: escritura`, clasificado `servidor`) | Campo persistido, asignado por R-CertificadoDigital-001 (alta) y restaurado por R-CertificadoDigital-003 (modificación); nunca `sobreescribible` por el cliente (fuera de las dos whitelists) |
