---
type: implementation-task
template: system
---

# Tarea 04 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas
- k-secure-coding
- k-code-quality

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
| `subsystem/security/controller/AceProfileCentroController.java` | Crear | k-sistemas (controladores.md) | Método de tipo 3 que da a la vista los ids de los centros supervisados. |

## Pasos

### Paso 4 — Controlador `AceProfileCentroController`

Clase nueva `com.educaflow.subsystem.security.controller.AceProfileCentroController`, con `@Inject ModelServiceFactory modelServiceFactory` como única dependencia. No expone `insert`/`update`/`remove` ni validaciones de save/delete (las dan `save`/`delete` y las acciones globales `remote-validation*`).

```java
@CallMethod
public List<Long> idsCentrosSupervisados();
//   Tipo 3 (sin ActionRequest/ActionResponse). La invoca el <context name="idsCentrosSupervisados"
//   expr="call:…AceProfileCentroController:idsCentrosSupervisados()"/> de
//   subsysSecurity.Centro@AceProfileCentro-action, igual que BandejaController.idsPendientesDeMi().
//   Resuelve AceProfileCentroService con modelServiceFactory.resolve(AceProfileCentro.class), llama a
//   getCentrosSupervisados() y devuelve los ids de esos centros; si la lista está vacía devuelve el
//   centinela NINGUNO = List.of(-1L), igual que BandejaController.NINGUNO, para que el `domain`
//   `self.id IN (:idsCentrosSupervisados)` nunca quede en `IN ()`. Sin más lógica: solo traduce
//   entidades a ids para el contexto de la vista.
```

**Verificar:** `./gradlew compileJava`; el nombre del método coincide con el de la expresión `call:` de `Centro-AceProfileCentro.xml`.

## Frontera de confianza — AllowProperties por acción (aplicable)

## Frontera de confianza — AllowProperties por acción

La única acción propia invocada desde un `@CallMethod` es `getCentrosSupervisados()`, que **no recibe entidad** (sin `allowProperties`, `k-sistemas/servicios.md`). Las acciones que sí reciben la entidad del cliente son `insert`/`update`/`remove`, a las que se llega por el endpoint REST automático (acciones `save`/`delete` de la vista) y por las globales `remote-validation*`; sus whitelists son las que defienden la entidad.

### `AceProfileCentroServiceImpl.getCentrosSupervisados` (invocado desde `AceProfileCentroController.idsCentrosSupervisados`)

Entidad: ninguna. **Forma elegida**: no aplica (no hay mapa del cliente que filtrar: la acción trabaja sobre `SecurityUtil.getUser()`).
**Origen spec:** RUI-perfiles-tramites-mi-centro-formulario-001 y -005 (pantalla del supervisor).

## Notas y supuestos (aplicables)

- **El contexto del `<action-view>` llega al `domain` del campo.** axelor-front (`usePrepareContext`) mezcla el contexto del `action-view` en el contexto del formulario, que es el `_domainContext` que usan los selectores; es el mismo mecanismo por el que `sysVentanilla` usa `_centrosIds`.
- **Supervisor sin ningún centro.** Normalmente no ve el menú; si abre la acción por URL, el controlador devuelve el centinela `List.of(-1L)` (el mismo `NINGUNO` de `BandejaController`): el selector de centro sale vacío y V-AceProfileCentro-008 le rechaza cualquier alta. El prellenado (`idsCentrosSupervisados.size() == 1`) no cambia: con el centinela el único id es `-1`, `find(-1)` da `null` y es lo mismo que no prellenar.
