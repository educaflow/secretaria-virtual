---
type: implementation-task
template: system
---

# Tarea 06 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

## Fichero ya materializado en el diseño — se FUSIONA, no se regenera

La porción de menú **ya está materializada** por el diseñador en:

`/workspace/secretaria-virtual/.sdd/drafts/2026-09-08_02-53_nombre-apellidos-certificado-digital/design/menus.xml`

**MUST** fusionarse en el `menus.xml` único del proyecto:

`/workspace/secretaria-virtual/src/main/java/com/educaflow/secretariavirtual/menus/menus.xml`

En esta iniciativa la fusión es un **no-op**: el `<menuitem>` de «Certificados digitales» ya existe idéntico en el destino. **MUST NOT** duplicar la entrada, renombrarla ni reordenar el fichero. **MUST NOT** regenerar la porción del diseño.

La fila es `Acción: Modificar`: el fichero destino **ya existe** y **no debe perder nada**. Antes de escribir nada aplica la **comprobación de conservación** de `implementation.md` §3; si el `<menuitem>` del diseño ya está presente y es idéntico, la acción correcta es **dejar el fichero como está** y darlo por hecho.

## Fila de la tabla «Ficheros a crear o modificar»

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` | Modificar | k-vistas (menus.md) | Sin cambios reales: el `<menuitem>` de «Certificados digitales» se mantiene tal cual (ver `design/menus.xml`) |

> **Nota para `/sdd-implementer`:** los XML de `domains/`, `views/` y `menus.xml` ya están materializados en la carpeta `design/`. **MUST NOT** modificarlos, reescribirlos ni regenerarlos: se **copian verbatim** a su ubicación final (`menus.xml` se fusiona en el `menus.xml` único del proyecto y, en esta iniciativa, la fusión es un no-op porque el `<menuitem>` ya existe idéntico). El código Java es lo único que se implementa a partir de las firmas y comentarios del diseño.

## Texto del diseño (verbatim)

### Paso 7 — Menús

**Fichero del diseño:** `design/menus.xml` → se fusiona en `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` (**Modificar**).

Esta iniciativa **no cambia ningún menú**: `screen-certificados-digitales.md` declara «Administración SV → Certificados digitales — lo ve el Administrador; lleva a esta pantalla (sin cambios)». El `design/menus.xml` reproduce ese único `<menuitem>` verbatim para dejar constancia de que el delta no lo toca; la fusión es un **no-op** y `/sdd-implementer` **MUST NOT** duplicar la entrada.

**Verificar:** `grep -c 'administracionSv-certificadosDigitales-menuitem' src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` devuelve `1`.

### Paso 8 — Seguridad

Sin cambios. El certificado digital **no pertenece a ningún centro**, así que no hay filtro multi-centro que aplicar ni riesgo de IDOR cross-tenant en esta pantalla. El acceso lo da el `<menuitem>` con `groups="admins"` sobre el grupo de administradores de Axelor, que ya existe; no hay ningún `auth-*.xml` que conceda permisos sobre `CertificadoDigital` y no hace falta añadirlo (no se crea ninguna entidad nueva).

Regla de acceso, en lenguaje natural: **el Administrador** ve, crea, modifica y borra certificados digitales de cualquier persona, sin restricción por centro. Ningún otro tipo de usuario llega a la pantalla.

La superficie de seguridad que **sí** cambia es la frontera de confianza del bean, y se cierra en el Paso 4.3: la entidad pasa de `createAllowAllProperties()` heredado a whitelists explícitas en `insert` y `update`. Ver `## Frontera de confianza — AllowProperties por acción`.

**Verificar:** con el usuario `admin` la pantalla es accesible y operativa; ningún otro menú expone la entidad.

### Paso 9 — Datos iniciales

Sin cambios. La iniciativa no añade catálogos ni datos maestros: los recursos que usan los escenarios (`firma/mi_certificado.p12` y `firma/instalar_certificado_criptografico/secretario.p12`) ya están dentro del WAR, y el usuario `secretario@mislata.es` con documento `29050788V`, nombre «Secretario» y apellidos «CIPFP Mislata» ya está en `src/main/resources/data-demo/input/usuarios-demo.xml`. No se crea ninguna carpeta `data-init` en `subsystem/criptografia`.

**Verificar:** `grep -n '29050788V' src/main/resources/data-demo/input/usuarios-demo.xml` devuelve la línea del secretario.
