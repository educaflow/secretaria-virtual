# review-contract — developer-code-reviewer

Contrato de revisión que consume el motor `/skill-orquestador-reviewer`. Declara **qué** se revisa; el **cómo** (bucle, tokens, severidades, límites) lo pone el motor.

## Alcance

Entran: el código Java/Kotlin escrito a mano bajo `src/main/java/**` y `src/test/java/**` (entidades no generadas, servicios, controladores, repositorios, módulos Guice, utilidades, tests).

**MUST NOT** entrar ni corregirse:

- Vistas XML (`**/views/*.xml`, `menus.xml`) → **STOP** y remite a `/developer-view-reviewer`.
- Modelos de dominio: **todo** XML con raíz `<domain-models>`, se llame como se llame su carpeta (`**/domains/*.xml`, `**/domains.xml`, y también los modelos auxiliares tipo `views-models/`, `view_models/`) → **STOP** y remite a `/developer-model-reviewer`. **MUST** mirar el elemento raíz, no la ruta.
- Entidades generadas (`build/src-gen/**`): son un artefacto derivado; lo que hay que revisar es el modelo del que salen → **STOP** y remite a `/developer-model-reviewer`.
- `src/test/java/com/educaflow/views` y `src/test/java/com/educaflow/architecture`: son **proyecciones** de `agent_docs/view-rules.md` y `agent_docs/architecture-rules.md`. Para cambiarlos se edita el markdown y se re-ejecuta `/developer-create-view-tests` o `/developer-create-arch-tests`; **MUST NOT** editarlos a mano.
- Cualquier otro artefacto (properties, `data-init`, recursos): no hay revisor para ese tipo de fichero → **STOP** y dilo, sin remitir a ningún skill.

## Skills obligatorios

- Los que indique el usuario en la invocación: a diferencia de los skills hermanos, este contrato **no tiene lista fija de conocimiento** y son el criterio principal. Si el usuario no aporta ninguno → **STOP**: sin criterio no hay revisión posible.
- **REQUIRED** `k-secure-coding`: si la revisión toca entidades, servicios, controladores o cualquier endpoint nuevo, **MUST** añadirlo aunque la invocación no lo liste. Solo se omite si la revisión es estrictamente sobre código sin frontera de confianza (utilidad pura sin acceso a entidades, refactor de tests, etc.).

## Ejes de revisión

1. **Cumplimiento del conocimiento cargado** — comparar el código con los skills que se han pasado (convenciones de estructura, nombres, patrones y arquitectura que declaren).
2. **Requisitos** — si la invocación trae descripción/requisitos, que el código haga lo que dicen; este eje va por delante de los demás.
3. **Corrección funcional** — errores reales: nulos, condiciones invertidas, transacciones, concurrencia, recursos sin cerrar, excepciones tragadas.
4. **Frontera de confianza** — lo que exija `k-secure-coding` cuando aplique.
5. **Calidad técnica** — lo que exija el skill de calidad que se haya pasado (métodos, clases, idiomas modernos del stack).

## Pasos obligatorios del revisor

NINGUNO.

## Pasos obligatorios del corrector

NINGUNO.

## Puertas

NINGUNA. La compilación y los tests del proyecto no se ejecutan dentro de este skill: los lanza quien lo invoca, con `./run.sh` (ver `agent_docs/deploy.md`).

## Clasificación específica

- Toda violación de `k-secure-coding` es **BLOCKING**, aunque el código funcione.

## Informe

NADA que añadir al informe estándar del motor.
