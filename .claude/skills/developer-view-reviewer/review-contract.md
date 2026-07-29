# review-contract — developer-view-reviewer

Contrato de revisión que consume el motor `/skill-orquestador-reviewer`. Declara **qué** se revisa; el **cómo** (bucle, tokens, severidades, límites) lo pone el motor.

## Alcance

Entran: las vistas Axelor bajo `**/views/*.xml` y los `menus.xml`.

**MUST NOT** entrar ni corregirse:

- Código Java/Kotlin → **STOP** y remite a `/developer-code-reviewer`.
- Modelos de dominio (`**/domains/*.xml`, `**/domains.xml`) → **STOP** y remite a `/developer-model-reviewer`.
- Los `.java` de test bajo `src/test/java/com/educaflow/views`: son una proyección de `agent_docs/view-rules.md` y se regeneran con `/developer-create-view-tests`.
- Ficheros cuya raíz es `<domain-models>` aunque la carpeta se llame `views-models/` o `view_models/`: **no son vistas**, son modelos → **STOP** y remite a `/developer-model-reviewer`. **MUST** mirar el elemento raíz, no el nombre de la carpeta.
- Otros ficheros con raíz `<object-views>` que **no** están en una carpeta `views/`: los `views.xml` de los tipos de expediente, `shared/template-views.xml` y los `actions-*.xml` de controladores. Quedan fuera **a propósito**: el alcance normativo de `agent_docs/view-rules.md` y de sus tests es `**/views/*.xml` + `menus.xml`, y ampliarlo aquí desalinearía revisor y tests → **STOP** y dilo, sin remitir a ningún skill.
- Cualquier otro artefacto (properties, `data-init`, recursos): no hay revisor para ese tipo de fichero → **STOP** y dilo, sin remitir a ningún skill.

## Skills obligatorios

1. **MUST** `k-vistas` — conocimiento normativo de las vistas y criterio de revisión y corrección.
2. **REQUIRED** `k-secure-coding`: si alguna vista revisada es un `<form>` que escribe en BD o expone acciones que llegan al servidor.
3. Los adicionales que indique el usuario.

## Ejes de revisión

**CRITICAL — antes de nada, mira si la vista está en un paquete exento.** `agent_docs/view-rules.md` deja **fuera del sujeto de todas sus reglas** los paquetes `gestioncentro`, `expedientes` y `tramites` (framework propio de expediente/tramitación), y el soporte de los tests lo implementa (`PAQUETES_EXENTOS` en `src/test/java/com/educaflow/views/support/ViewFiles.java`). Eso parte la revisión en dos:

- Vista **no exenta** → las reglas del catálogo le aplican, y hay un sub-paquete de tests por cada categoría testeada de `view-rules.md` (hoy: fichero/ubicación, nomenclatura, bloques, integridad referencial, plantillas, forms, botones, grids, `action-view` y menús). **MUST NOT** gastar la revisión en re-verificar a mano una categoría que tenga sub-paquete de test. Sí quedan para ti las que `view-rules.md` marca como catalogadas **pero no testeadas** (hoy, charts y trees): ahí no hay red. **MUST** comprobar en `src/test/java/com/educaflow/views/` qué sub-paquetes existen en vez de fiarte de esta lista, que envejece.
- Vista **exenta** → el catálogo **no le aplica**: `view-rules.md` la deja fuera del sujeto de sus reglas por ser framework propio de expediente/tramitación. **MUST NOT** reportar incumplimientos del catálogo en estas vistas — serían falsos positivos, y el corrector los "arreglaría" rompiendo el framework sin ningún test que lo detecte. Su revisión son **solo** los ejes de abajo.

En ambos casos, el núcleo de tu trabajo es lo que XPath no puede ver:

1. **Maquetación** — la auditoría ASCII Layout de cada `<form>` (ver `## Pasos obligatorios del revisor`). Un form con `colSpan`/`colOffset` disparatados es válido para el XSD y para los tests, y aun así se ve mal.
2. **Coherencia con los requisitos de la invocación** — solo si quien invoca aportó descripción/requisitos: que la vista muestre lo que piden, con los campos, filtros y acciones correctos. Si no los aportó, este eje **no aplica**: **MUST NOT** buscar una especificación en el proyecto ni inventarse la intención de la vista.
3. **Sensatez de acciones y condiciones** — `showIf`/`hideIf`/`requiredIf`/`readonlyIf`, `onNew`/`onLoad`/`onSave`, action-groups: que hagan lo que dicen y que no dejen estados imposibles.
4. **Frontera de confianza** — **CRITICAL**: `readonly`/`showIf`/`hidden` en el XML **NO son defensa**; solo ocultan. Axelor expone un endpoint REST por entidad que no pasa por la vista ni por el controlador, así que un `curl` ignora esos atributos. Todo campo o acción que el cliente no debe poder dictar **MUST** estar defendido en el servidor, y una vista que dé por segura una restricción puramente visual es un hallazgo (`k-secure-coding`). Su corrección vive en Java, fuera del alcance de este skill: se emite como `UNCLEAR`, ver `## Clasificación específica`.

## Pasos obligatorios del revisor

**MUST** aplicar a cada `<form>` la «Dirección de auditoría: reconstruir el ASCII Layout de un XML existente» de `k-vistas/forms.md`:

1. Por cada `<panel>`, `<panel-related>` y `buttons-panel`, reconstruir el ASCII Layout desde los `colSpan`/`colOffset` **reales** del XML, con la notación de ese fichero.
2. Si hay elementos con `showIf`, dibujar **un ASCII Layout por estado**, con los paneles condicionales en bloques separados.
3. Pasar sobre el dibujo reconstruido el «Checklist de maquetación» de `k-vistas/forms.md`.
4. Reportar cada incumplimiento citando la regla del checklist e incluyendo el ASCII Layout reconstruido como evidencia.

**MUST NOT** auditar un layout "a ojo" leyendo los `colSpan` sueltos: sin reconstruir el dibujo no se ven los huecos, los bordes desalineados ni las filas que no suman 12.

## Pasos obligatorios del corrector

1. Si la corrección toca los `colSpan`/`colOffset` de un `<form>`, **MUST** volver a dibujar el ASCII Layout resultante y pasarle el «Checklist de maquetación» de `k-vistas/forms.md` **antes** de dar la corrección por buena. **MUST NOT** reajustar anchuras "a ojo": produce un layout nuevo igual de incumplidor que solo se detecta en la iteración siguiente.
2. **MUST** comprobar por **regresión** contra el XSD cada XML que toques: que tu corrección **no introduce errores nuevos**, no que el fichero valide del todo. Ejecuta el comando **desde la raíz del proyecto** (`secretaria-virtual`), ya que la ruta del esquema es relativa a ella, **antes y después** de tu cambio, y compara los errores:

   ```bash
   xmllint --noout --schema ../axelor-open-platform/axelor-core/src/main/resources/object-views.xsd <fichero.xml>
   ```

   - Errores **nuevos** respecto al estado previo → los ha introducido tu corrección: **MUST NOT** dejarlos. Corrige hasta volver al conjunto de errores previo, o revierte y repórtalo como `PUSHBACK`.
   - Errores **preexistentes** → **MUST NOT** tocarlos ni reportarlos: el XSD de `axelor-open-platform` es más estricto que el runtime real, así que hay ficheros del proyecto que lo incumplen y funcionan. Arreglarlos no es el encargo.
   - Si el comando falla porque **no encuentra el esquema**, es un problema de directorio de trabajo, **no** un XML inválido: corrige el directorio y repite. **MUST NOT** emitir `PUSHBACK` por ese caso.

## Puertas

Comando (puerta de entrada y cada puerta de salida):

```bash
./gradlew test --tests 'com.educaflow.views.*'
```

Los fallos se leen en `build/test-results/test/*.xml` (cada `<testcase>` con `<failure>`/`<error>`). Clasifica cada fallo aplicando estas reglas **en este orden**; la primera que case gana:

1. El test es correcto pero **la regla de `agent_docs/view-rules.md` es discutible**, no la vista → **STOP** y pregunta al usuario: se edita el markdown y se re-ejecuta `/developer-create-view-tests`. **MUST NOT** editar los `.java` de test a mano, ni gastar reentradas intentando "corregir" una vista que está bien. Esta regla se evalúa **primero**, también cuando el fichero es de la ubicación revisada.
2. Afecta a un fichero **de la ubicación revisada** → problema `BLOCKING`, con el nombre del test y el mensaje de fallo.
3. Afecta a otra vista → **informativo**; anótalo para el informe y **MUST NOT** corregirlo (está fuera del encargo).

## Clasificación específica

**Hallazgos de frontera de confianza (`k-secure-coding`)**: **MUST** emitirse como `UNCLEAR`, nunca con severidad, aunque la vista se renderice bien.

- Motivo: la defensa real vive en `*ServiceImpl.insert/update` y en `validate*`, que están **fuera del `## Alcance`** de este skill. Clasificarlos con severidad se los pasaría al corrector, que no puede tocar Java y acabaría devolviendo un `PUSHBACK` — un ciclo perdido y una etiqueta engañosa ("corrección rechazada por incorrecta"), cuando lo que ocurre es que el arreglo es de otro skill.
- El texto del bloque **MUST** nombrar el elemento de servidor a verificar y el siguiente paso concreto, no quedarse en el diagnóstico.
  - ✅ CORRECTO: «`readonly` en `estado` no es defensa. Verifica que `ExpedienteServiceImpl.update` sobrescribe ese campo incondicionalmente; si no, ejecuta `/developer-code-reviewer <ruta del servicio> k-secure-coding`.»
  - ❌ INCORRECTO: «El campo `estado` es inseguro.» (sin el elemento de servidor ni el siguiente paso: quien lea el informe no sabe qué hacer)
- Si además la vista tiene un defecto propio y corregible en el XML (p. ej. expone un campo que no pinta nada en ese formulario), eso **sí** va aparte con su severidad normal: son dos hallazgos distintos.

Además:

- Un error **nuevo** contra `object-views.xsd` introducido por una corrección es **BLOCKING**; uno **preexistente** no se reporta (ver `## Pasos obligatorios del corrector`).

## Informe

Añade el resultado de cada pasada de tests (la de entrada y la de cada paso por la puerta de salida) y la lista de fallos informativos en vistas ajenas a la ubicación revisada, si los hubo.
