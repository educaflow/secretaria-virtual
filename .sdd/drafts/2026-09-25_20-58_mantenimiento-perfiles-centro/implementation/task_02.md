---
type: implementation-task
template: system
---

# Tarea 02 a implementar

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
| `subsystem/security/db/repo/AceProfileCentroRepository.java` | Modificar | k-sistemas (modelos.md, servicios.md) | + métodos `existeOtraIgual` (RES-007) y `findCentrosSupervisados` (dueño de «centros supervisados», D1). |

**Acción `Modificar`:** la clase ya existe; solo se añaden los dos métodos y se conserva el resto (`findPerfiles`). Las decisiones D1–D6 citadas están en `design/decisiones.md`.

## Pasos

### Paso 3 — Repositorio `AceProfileCentroRepository`

Clase existente `com.educaflow.subsystem.security.db.repo.AceProfileCentroRepository extends AbstractAceProfileCentroRepository`. **Solo se añaden** los dos métodos; el resto de la clase (`findPerfiles`) se conserva.

```java
public List<Centro> findCentrosSupervisados(User usuario);
//   Dueño en el subsistema de «los centros que supervisa el usuario» (decisiones D1): todos los centros en
//   los que tiene el tipo de usuario SUPERVISOR, nunca User.centroActivo. JPQL con parámetro nombrado:
//     SELECT DISTINCT cu.centro FROM CentroUsuario cu JOIN cu.centroUsuarioTipoUsuario cut
//     JOIN cut.tipoUsuario tu WHERE cu.usuario = :usuario AND tu.codigo = 'SUPERVISOR'
//   Copias declaradas del criterio (D1), que cambian con él: las condiciones JPQL de los permisos
//   AceProfileCentro.supervisor y Tramite.supervisor (Paso 7.1). Sin resultados → lista vacía.

public boolean existeOtraIgual(AceProfileCentro fila);
//   true si existe en BD otra fila (id distinto del de `fila`; en un alta `fila.getId()` es null y no se
//   excluye ninguna) con el mismo centro, trámite y perfil y los mismos tres destinatarios, comparando
//   cada destinatario de forma segura frente a nulos: un destinatario vacío solo coincide con otro vacío
//   (`tipoUsuario` null solo iguala a filas con `tipoUsuario` null, etc.).
//   Consulta con parámetros nombrados enlazados (nunca concatenando valores); la forma concreta de la
//   comparación nula la decide el implementador (p. ej. ramas IS NULL por destinatario, o filtrar en
//   memoria los candidatos de mismo centro/trámite/perfil).
```

**Verificar:** `./gradlew compileJava`; `findPerfiles` sigue intacto.

## Trazabilidad Origen spec → V/R/U → ubicación (filas que citan este fichero)

| ID | Origen spec | Ubicación | Qué comprueba |
|----|-------------|-----------|---------------|
| V-AceProfileCentro-007 | RES-AceProfileCentro-007 | `AceProfileCentroServiceImpl.validarAsignacionNoRepetida` → `AceProfileCentroRepository.existeOtraIgual` | Asignación no repetida. |

## Notas y supuestos (aplicables)

- **Las guías de diseño se respetan sin excepción:** solo se tocan `subsystem/security` y `menus.xml` (el enlace de permisos al grupo va en el data-init de `security`, D2); no se tocan `auth-expedientes.xml`, `PerfilesUsuarioService` ni `AceProfileCentroRepository.findPerfiles`; el «centro del supervisor» son todos sus centros `SUPERVISOR`, nunca `User.centroActivo`.
