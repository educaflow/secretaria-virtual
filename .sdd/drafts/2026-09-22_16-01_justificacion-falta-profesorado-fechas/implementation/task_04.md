---
type: implementation-task
template: expediente
---

# Tarea 04 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-tipo-expediente
- k-secure-coding
- k-code-quality

La especificación del diseño es **contrato fijo** y la **superficie es cerrada**: **MUST NOT** crearse ningún método, clase, campo ni acción que la especificación no liste.

## Fichero que cubre esta tarea (fila de la sección 6 del diseño)

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/tramites/profesores/justificacion_falta_profesorado/actual/v1/InitialEventManagerImpl.java` | Modificar | `k-tipo-expediente` | Especificado en `## 8. Especificación del InitialEventManagerImpl` |

## Paso del diseño (verbatim)

### Paso 4 — `InitialEventManagerImpl.java`

Fichero destino: `…/actual/v1/InitialEventManagerImpl.java`.
FQCN: `com.educaflow.tramites.profesores.justificacion_falta_profesorado.actual.v1.InitialEventManagerImpl`.
Especificación: `## 8. Especificación del InitialEventManagerImpl`.
Supertipo: `implements InitialEventManager<JustificacionFaltaProfesoradoV1>`.

Verificación: compila; el método `triggerInitialEvent` ya no referencia `setAnyo` ni `LocalDate`; existe exactamente la asignación de la tabla de la sección 8; el fichero ya no importa `java.time.LocalDate` ni `com.educaflow.base.util.Convert`.

(Ruta destino resuelta: `src/main/java/com/educaflow/tramites/profesores/justificacion_falta_profesorado/actual/v1/InitialEventManagerImpl.java`. Acción **Modificar**: el fichero ya existe en el árbol y se edita sobre él.)

## 8. Especificación del InitialEventManagerImpl (verbatim, íntegra)

Fichero: `actual/v1/InitialEventManagerImpl.java`, en la **raíz** de la carpeta de versión.

```java
package com.educaflow.tramites.profesores.justificacion_falta_profesorado.actual.v1;

public class InitialEventManagerImpl implements InitialEventManager<JustificacionFaltaProfesoradoV1> {
    @Override
    public void triggerInitialEvent(InitialEventContext<JustificacionFaltaProfesoradoV1> initialEventContext) throws BusinessException { … }
}
```

(Esta es la firma **real** de la interfaz `InitialEventManager` del árbol: un único parámetro `InitialEventContext<T>`, del que se obtiene el expediente con `getExpediente()`. El delta no la cambia.)

Asignaciones, **en orden**:

| # | Setter | Fuente del valor | Por qué |
|---|---|---|---|
| 1 | `initialEventContext.updateState(States.Recepcion.ENTRADA_DATOS)` | constante de la clase `States` de **esta** versión | El XML maestro no declara estado inicial: sin esta llamada el `Tramitador` aborta el alta |

**Delta:** desaparece la primera asignación que había hoy, `setAnyo(LocalDate.now(Convert.defaultZoneId).getYear())`, y con ella los `import` de `java.time.LocalDate` y `com.educaflow.base.util.Convert`. Ya no se precarga ningún dato sobre cuándo se produjo la falta: `fechaInicio` nace vacía y la rellena el profesor.

La sección **MUST** dejar explícito además:

- Lo que el `Tramitador` ya rellena **antes** de llamar a este método y **MUST NOT** reasignarse: `tipoExpediente`, `centro`, `usuarioRegistrador`, `name`, `numeroExpediente`.
- Lo que hace **después** y tampoco es cosa de este método: comprobar el perfil del estado inicial, crear el `HistorialEstado` y llamar al `onEnterState`.
- `triggerPresentar` **sí** llama a `createRegistroEntrada`, así que `personaSolicitante` y `personaInteresada` **MUST** llegar no nulos a ese evento. Hoy no los rellena este método —los deja el `Tramitador` a partir del contexto de tramitación— y el delta **no cambia eso**: quitar el `setAnyo` no puede dejarlos nulos.

Dependencias a inyectar: **ninguna**. La clase no tiene constructor propio ni `@Inject`.

## Fila del reparto de reglas (sección 11) que apunta a este fichero

| Tipo de regla | Capa | Cómo se escribe |
|---|---|---|
| Inicialización del expediente | **`triggerInitialEvent`** | sección 8 |

## Nota del diseño aplicable a este fichero (sección 14, verbatim)

- **La firma `triggerInitialEvent(InitialEventContext<T>)` del árbol real tiene un solo parámetro**, mientras que la plantilla de `design-contract.md` §10 muestra dos (`(<Entidad>, InitialEventContext)`). Manda el árbol: la interfaz `InitialEventManager` del `subsystem/tramitador` declara un único parámetro. El delta no la cambia.
