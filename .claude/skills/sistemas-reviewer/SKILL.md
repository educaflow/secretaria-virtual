---
name: sistemas-reviewer
description: Revisa que un sistema o subsistema completo creado o modificado cumple todas las reglas de /sistema-knowledge y /sistemas-knowledge — estructura de carpetas, capas, paquetes, dependencias y coherencia entre artefactos.
---

# sistemas-reviewer

## Propósito

Verificar que un sistema o subsistema creado o modificado sigue las reglas definidas en `/sistema-knowledge` y `/sistemas-knowledge`. Este reviewer verifica la estructura global; para el contenido concreto de cada artefacto delega en los reviewers especializados.

## Qué leer

1. La estructura de carpetas del sistema/subsistema (`ls -R {ruta}`).
2. Los ficheros de dominio en `domains/`.
3. Los ficheros de servicio en `service/` e `service/impl/`.
4. Los controladores en `controller/`.
5. Las vistas en `views/`.
6. Los menús en `secretariavirtual/menus/` si los hay.
7. Los skills `/sistema-knowledge` y `/sistemas-knowledge`.

## Ubicación correcta

- [ ] Si la funcionalidad es reutilizable por otros sistemas/subsistemas → está en `subsystem/<nombre>/`.
- [ ] Si es funcionalidad concreta para el usuario, sin reutilización prevista → está en `system/<nombre>/`.
- [ ] La capa elegida es coherente con la distinción conceptual de `/sistema-knowledge`.

## Reglas de dependencia entre capas

```
base/util ← base/infrastructure ← subsystem ← system
```

- [ ] Un subsistema NO importa de ningún `system/`.
- [ ] Un sistema NO importa de otro `system/`.
- [ ] No hay ciclos de dependencia entre subsistemas (si A usa B, B no usa A).

## Estructura de carpetas

- [ ] Existe `domains/` con los ficheros XML de entidades.
- [ ] Existe `service/` con la interfaz del servicio (si hay lógica de negocio).
- [ ] Existe `service/impl/` con la implementación (si hay servicio).
- [ ] Si hay repositorios o listeners personalizados, están en `db/repo/` — no en `db/` directamente.
- [ ] Los ficheros en `db/` (fuera de `repo/`) son generados por el build y NO se han editado manualmente.
- [ ] Si hay controladores, están en `controller/` (singular).
- [ ] Las vistas están en `views/`.
- [ ] Si existe `module/`, solo contiene bindings que `ModelServiceFactory` no puede descubrir automáticamente.
- [ ] Si no hay módulo Guice necesario, la carpeta `module/` no existe (o no tiene contenido).

## Paquetes Java

- [ ] Las entidades están en `com.educaflow.{layer}.{nombre}.db`.
- [ ] Los servicios (interfaz) están en `com.educaflow.{layer}.{nombre}.service`.
- [ ] Las implementaciones están en `com.educaflow.{layer}.{nombre}.service.impl`.
- [ ] Los controladores están en `com.educaflow.{layer}.{nombre}.controller` (singular).

## Coherencia entre artefactos

- [ ] Cada entidad del dominio tiene su fichero XML en `domains/`.
- [ ] Los controladores solo referencian entidades y servicios del mismo sistema/subsistema o de capas inferiores.
- [ ] Las vistas referencian entidades con el FQCN correcto según el paquete `db` de la entidad.
- [ ] Los menús (si existen) referencian `<action-view>` que están en los ficheros de `views/`, no en los ficheros de menú.
- [ ] No se han creado ni modificado `i18n_*.csv` manualmente.

## Verificación de artefactos individuales — delegar en reviewers especializados

- [ ] Los modelos de dominio cumplen las reglas → usar `/modelos-reviewer`.
- [ ] Los servicios cumplen las reglas → usar `/servicios-reviewer`.
- [ ] Los controladores cumplen las reglas → usar `/controladores-reviewer`.
- [ ] Las vistas, grids, formularios y acciones cumplen las reglas → usar `/vistas-reviewer`, `/grids-reviewer`, `/formularios-reviewer`, `/actions-reviewer`.
- [ ] Los menús cumplen las reglas → usar `/menus-reviewer`.
- [ ] La coherencia de referencias entre vistas → usar `/checkvistas-knowledge`.

## Checklist final

- [ ] La ubicación (`subsystem/` vs `system/`) es correcta según el tipo de funcionalidad
- [ ] No hay importaciones que violen las reglas de dependencia entre capas
- [ ] La estructura de carpetas sigue la convención de `/sistema-knowledge`
- [ ] Los paquetes Java son `com.educaflow.{layer}.{nombre}.{subcarpeta}`
- [ ] Los ficheros en `db/` (fuera de `repo/`) no se han editado manualmente
- [ ] No existe `module/` si `ModelServiceFactory` puede descubrir todos los servicios automáticamente
- [ ] Los menús (si existen) están en `secretariavirtual/menus/` y no en `views/`
- [ ] No se han creado ni modificado `i18n_*.csv` manualmente
