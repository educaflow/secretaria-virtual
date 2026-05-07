---
name: k-seguridad
description: Referencia completa del subsistema de seguridad de EducaFlow — modelo de dominio SecurityActor/AccessProfile/AccessAssignment, Permission/Role/Group de Axelor, reglas JPQL, patrones de condición, separación read/write, EducaFlowAuthResolver y pasos para crear o modificar permisos.
---

# Seguridad en EducaFlow

## Ficheros de este skill

| Fichero | Contenido |
|---------|-----------|
| `modelo.md` | Modelo de dominio del subsistema de seguridad: `SecurityActor`, `TipoUsuario`, `CentroUsuario`, `AccessProfile`, `AccessAssignment`, `EducaFlowAuthResolver` y data-import de `AccessAssignment` |
| `permisos.md` | Referencia de `Permission`/`Role`/`Group` de Axelor: reglas JPQL, patrones de condición (actor, centro, usuario), separación read/write, los 4 permisos de `Expediente` y binding en `input-config.xml` |
| `auth-task.md` | Pasos prácticos para crear o modificar permisos: en qué fichero van, cómo añadirlos, cómo asignarlos a un rol y cómo registrar un fichero `auth-*.xml` nuevo |

---

## Cuándo usar cada fichero

**`modelo.md`** — al trabajar con las entidades del subsistema de seguridad (`AccessAssignment`, `AccessProfile`, `TipoUsuario`, `CentroUsuario`) o al implementar/entender `EducaFlowAuthResolver` y los data-imports de asignaciones.

**`permisos.md`** — al diseñar o revisar permisos: qué condición JPQL usar, cómo separar read y write, qué patrones existen para filtrar por actor/centro/usuario, o cómo funciona el binding en `input-config.xml`.

**`auth-task.md`** — al crear o modificar permisos concretos: guía paso a paso con el fichero destino, la plantilla XML y cómo enlazar con el rol correspondiente.
