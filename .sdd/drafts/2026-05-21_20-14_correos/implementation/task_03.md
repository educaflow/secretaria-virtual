---
type: implementation-task
---

# Tarea 03 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas

Fila de la tabla "Ficheros a crear o modificar":

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/common/domains/User.xml` | **Modificar** | k-sistemas (modelos.md) | Añadir `<finder-method name="findByDni" ...>` para resolver email por DNI (si no existe). |

Descripción de diseño (Paso 4 — Controladores):

> Para resolver el email por DNI, `proponerEmailPorDni` usa un finder `findByDni` sobre `User`. Se añade `<finder-method name="findByDni" using="String:dni" filter="self.dni = :dni"/>` a `subsystem/common/domains/User.xml` (espejo del patrón existente en `CertificadoDigital`), salvo que ya exista una vía equivalente en `registrousuario`.

Nota de unificación 6: **Resolución email por DNI**: `CorreoService.proponerEmailPorDni` usa un finder `findByDni` sobre `User` (a añadir en `subsystem/common/domains/User.xml`, espejo de `CertificadoDigital.findByDni`), salvo que `registrousuario` ofrezca una vía equivalente.

**Importante:** comprueba primero si ya existe un `findByDni` (o vía equivalente) en `User.xml` o en `registrousuario` antes de añadirlo. Si ya existe, no dupliques el finder.
