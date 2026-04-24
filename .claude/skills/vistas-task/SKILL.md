---
name: vistas-task
description: Pasos para crear o modificar ficheros XML de vistas de Axelor — grids, formularios, acciones y menús.
---

# vistas-task

## Vistas de Axelor: creación y modificación

- Este skill sirve para crear o modificar ficheros XML de vistas de Axelor.
- Se siguen las normas definidas en el skill `/vistas-knowledge`.
- Nunca se diseñan vistas sin modelo de dominio definido: leer primero los ficheros `domains/*.xml`.

## Tareas a realizar

1. **Identificar los ficheros de vistas** que hay que crear o modificar y en qué carpeta `views/` van.
2. **Identificar los grids** necesarios y en qué fichero van → usar `/grids-task`.
3. **Identificar los formularios** necesarios y en qué fichero van → usar `/formularios-task`.
4. **Identificar las acciones** necesarias (botones, onSave, onChange, onLoad, action-view...) → usar `/actions-task`.
5. **Identificar los menús** si los hay → usar `/menus-task`.

## Al crear un fichero de vistas nuevo

- Usar exactamente la declaración XML de cabecera definida en `/vistas-knowledge`.
- Nombrar el fichero `<NombreEntidad>.xml` o por funcionalidad si agrupa varias entidades.
- Seguir las convenciones de nombres de vistas y acciones definidas en `/sistemas-knowledge`.

## Al modificar un fichero de vistas existente

- Leer el fichero completo antes de editar.
- No cambiar nombres de vistas o acciones que ya existan y sean referenciados desde otros sitios.
- Verificar que cualquier referencia añadida (grid-view, form-view, action) apunta a un elemento que existe.

## Revisión
- [ ] El XML es válido y usa el namespace `object-views` correcto
- [ ] Los nombres de grids, forms y actions siguen las convenciones del proyecto (`/sistemas-knowledge`)
- [ ] Todas las referencias a actions, grids y forms apuntan a elementos que existen en el proyecto
- [ ] Los ficheros `i18n_*.csv` no se han creado ni modificado a mano
- [ ] Las vistas de menú globales están en `secretariavirtual/menus/`, no en `views/`
- [ ] Ejecutar `/checkvistas-knowledge` para verificar coherencia de nombres y referencias
