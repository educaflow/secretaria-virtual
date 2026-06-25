---
type: implementation-task
---

# Tarea 17 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-datainit
- k-secure-coding

Crea los datos iniciales de permisos del sistema (manifiesto de binding + permisos de acceso por centro/alumno).

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `system/gruposnotas/data-init/input-config.xml` | Crear | k-datainit | Manifiesto de binding de los permisos |
| `system/gruposnotas/data-init/input/auth-gruposnotas.xml` | Crear | k-datainit | Permisos del sistema (acceso por centro / por alumno) |

Estos ficheros van en `src/main/java/com/educaflow/system/gruposnotas/data-init/` (manifiesto) y `.../data-init/input/` (permisos). No están materializados en `design/`: se escriben siguiendo `k-datainit` a partir de las reglas de acceso del diseño.

Del diseño, Paso 7 — Seguridad. Reglas de acceso (en lenguaje natural; el XML de import lo materializa esta tarea):

- **Grupo, ModuloGrupo, Nota, AlumnoGrupo — supervisor (rol/condición por centro):** permisos `<permission>` con `condition="self.centro = ?"` / `conditionParams="__user__.centroActivo"` para `Grupo` (acceso solo a grupos de su centro activo, ESC-021). Para `ModuloGrupo`, `Nota`, `AlumnoGrupo` la condición se expresa navegando al centro del grupo (p.ej. `self.grupo.centro = ?` para ModuloGrupo y AlumnoGrupo; `self.moduloGrupo.grupo.centro = ?` para Nota). Permisos `create/read/write/remove` para el supervisor.
- **Administrador (grupo `admins`):** acceso completo (sin condición de centro) a las cuatro entidades (gestiona cualquier centro y puede reabrir). Se concede por el `groups="admins"` del menú de administración y por permisos sin `condition`.
- **Alumno:** acceso de **solo lectura** restringido a SUS datos: `AlumnoGrupo` con `condition="self.alumno = ?"` / `conditionParams="__user__"`; `Nota` con condición navegando `self.alumnoGrupo.alumno = ?`; `ModuloGrupo`/`Grupo` de solo lectura limitados a los grupos a los que pertenece (ESC-026, ESC-015). Solo `read`.

> El detalle exacto de roles/grupos Axelor y la sintaxis de `condition`/`conditionParams` se materializa en `auth-gruposnotas.xml` siguiendo `k-datainit`. No se usa `k-seguridad` (obsoleto): las condiciones por centro/usuario se modelan como en `auth-gestioncentro.xml` (`condition="self.centro = ?"`, `conditionParams="__user__.centroActivo"`).

Del diseño, Paso 8 — Datos iniciales. No hay catálogos de negocio propios precargados (los datos maestros — centros, cursos, módulos, usuarios, tipos de usuario — los gestionan `common` y `sistemaeducativo`). El único contenido de `data-init/` es el fichero de **permisos** `auth-gruposnotas.xml` del Paso 7, con su `input-config.xml` (manifiesto de binding del `<permission>`), siguiendo `k-datainit`.

> Multicentro / acceso (Notas y supuestos punto 8): el supervisor solo ve los grupos de su centro; el alumno solo ve sus propios grupos; estos permisos lo refuerzan en servidor con `condition`/`conditionParams`. Se evita siempre `:__user__.campo` con punto (k-secure-coding §4).
