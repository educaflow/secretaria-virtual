---
type: implementation-task
template: system
---

# Tarea 09 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-code-quality
- k-secure-coding

## Utilidad GestorNotificacionesUtil

### Fila de la tabla «Ficheros a crear o modificar»

Rutas relativas a `src/main/java/com/educaflow/` salvo que empiecen por `src/`, `agent_docs/` o `.claude/`.

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/notificaciones/util/GestorNotificacionesUtil.java` | Crear | k-code-quality, k-secure-coding | Único dueño de «gestor del centro» (D4): le preguntan el menú, el reenvío, el `<domain>` y los permisos (`@ScriptAllowed`) |

### Paso 2 (verbatim del design.md)

### Paso 2 — Utilidad de gestor del centro

```java
// Clase: com.educaflow.subsystem.notificaciones.util.GestorNotificacionesUtil   (final, constructor privado)
// @ScriptAllowed — con el comentario de por qué (como en MenuSecurityService): los conditionParams de los
// permisos *.propio-centro-gestion (Paso 13) la invocan desde Groovy y la ScriptPolicy de Axelor solo admite
// clases anotadas.
// Único dueño de «gestor del centro» de notificaciones (D4): menú, reenvío, <domain> de «Del centro» y
// permisos le preguntan; no hay ninguna copia JPQL de la clasificación.

private static final Set<TipoUsuarioCodigo> TIPOS_GESTORES;   // EnumSet.of(SUPERVISOR, ADMINISTRATIVO)
private static final Set<CargoCodigo> CARGOS_GESTORES;        // EnumSet.of(DIRECTOR, JEFE_ESTUDIOS, SECRETARIO)

public static boolean esGestorEnAlgunCentro(User user);
//   true si alguno de user.getCentroUsuarios() cumple gestiona(...). false si la colección es null o vacía.

public static List<Long> idsCentrosGestionados(User user);
//   Ids de los centros de los CentroUsuario del usuario que cumplen gestiona(...), sin repetidos.
//   Si no gestiona ninguno o user es null → List.of(-1L) (un IN vacío no es portable y -1 no es el id de
//   ninguna fila). Es el ÚNICO sitio con ese centinela: lo usan tal cual el <domain>, los permisos y el reenvío
//   (V-Notificacion-015: idsCentrosGestionados(user).contains(centro.getId())).

private static boolean gestiona(CentroUsuario centroUsuario);
//   Algún CentroUsuarioTipoUsuario con tipoUsuario.codigo ∈ TIPOS_GESTORES, o algún CentroUsuarioCargo con
//   cargo.code ∈ CARGOS_GESTORES. Colecciones null = vacías; tipos/cargos null se ignoran.
```

Verificación: con el bloque de los Pasos 1–9; tests unitarios en `test-unit-desc.md` (un caso por tipo y por cargo, vicesecretario y profesor = no gestor, varios centros).

### Trazabilidad Origen spec → V/R/U → ubicación (filas que aplican a esta tarea)

#### V

| ID | Origen spec | Ubicación |
|---|---|---|
| V-Notificacion-015 | VAL-Notificacion-002 | `NotificacionCanalServiceImpl.validateReenviar` → `GestorNotificacionesUtil.idsCentrosGestionados(user).contains(...)` |

**Referencia:** la decisión D4 está en `design/decisiones.md`. El uso de esta clase desde el `<domain>` y los permisos está en el Paso 10 (`Centro-Notificacion.xml`) y el Paso 13.
