# Plantilla — fichero `screen-<nombre>.md` (análisis de una pantalla)

Esta plantilla describe la estructura obligatoria del fichero de análisis de una pantalla. Cada análisis funcional contiene **un fichero por cada pantalla** identificada (ver `examples/ciclo/screen-ciclos.md` o `examples/firmas/screen-pendiente.md` para ejemplos completos). El nombre del fichero es `screen-<nombre-kebab-case>.md`.

Una "pantalla" es la unidad de navegación que el usuario percibe como un todo. Si una pantalla contiene `panel-related` con sub-grids y sub-formularios anidados (relación maestro-detalle), **toda la jerarquía vive en el mismo fichero** y se enumera como Grid 1 → Formulario 1 → Grid 2 → Formulario 2 → … No se abren ficheros separados para las pantallas anidadas.

> **Importante**: la plantilla describe **QUÉ ve el usuario** en la pantalla y **qué puede hacer**. No deben aparecer nombres de vistas Axelor (`@Main-action`, `@Search-grid`), atributos XML (`canNew`, `showIf`, `readonlyIf`), ni nombres de acciones del framework. Si dudas, mira `SKILL.md` § "Frontera entre análisis y diseño".

---

# Pantalla: "<Nombre funcional de la pantalla, tal como lo verá el usuario>"

## Identidad

- **Quién la usa:** *(roles o tipos de usuario que pueden acceder. Si no se sabe, indicar "(pendiente)")*.
- **Qué muestra:** *(en lenguaje natural: el filtro aplicado, el modo (consulta/edición), qué información presenta. Ej.: "las tareas de firma del usuario actual cuyo estado es PENDIENTE")*.

## Menú

Sección **opcional**. Una pantalla que no se alcanza desde el menú (p.ej. se abre desde otra pantalla, popup, o llamada externa) puede omitir esta sección.

| Propiedad        | Valor                                                                           |
|------------------|---------------------------------------------------------------------------------|
| Ruta jerárquica  | *(ej.: "Notificaciones" → "Correos" → "Mis correos")*                           |
| Título visible   | *(texto del ítem de menú)*                                                      |
| Quién lo ve      | *(roles que ven este ítem de menú; puede ser un subconjunto de "Quién la usa")* |

---

## Grid N — "<Título del grid>"

*(Se numera 1, 2, 3… secuencialmente dentro de la pantalla. Si la pantalla solo tiene formulario sin grid, se omite esta sección.)*

### Propiedades

| Propiedad                   | Valor                                                                                                                   |
|-----------------------------|-------------------------------------------------------------------------------------------------------------------------|
| Columnas (en orden)         | *(nombres funcionales de las columnas, separados por comas, en el orden de izquierda a derecha)*                        |
| Ordenación por defecto      | *(`<campo> ascendente/descendente` o `—`)*                                                                              |
| ¿Permite buscar?            | *(`SÍ — <descripción de los filtros y búsqueda libre>` o `NO`)*                                                         |
| Formulario que abre el onclick         | *(`Formulario N — <Título> (<modo>)` o `Formulario N — <Título> (<modo>, como ventana modal)` o `—`)*                   |
| Botones del toolbar         | *(lista de títulos de los botones que aparecen sobre el grid — incluido el botón "Nuevo" — separados por comas; o `—`)* |
| Botones de las columnas     | *(lista de títulos de los botones que aparecen como columna adicional en cada fila, separados por comas; o `—`)*        |

**Valores admitidos por propiedad**:

- **Columnas (en orden)**: nombres funcionales. No usar nombres técnicos del modelo (`fechaSolicitud` → `fecha de solicitud`).
- **Ordenación por defecto**: `<campo> ascendente`, `<campo> descendente`, o `—`.
- **¿Permite buscar?**: `SÍ — <qué se puede filtrar y por dónde>`, `SÍ — búsqueda libre por todos los campos visibles`, o `NO`.
- **Formulario que abre el onclick**: el formulario al que se navega al hacer click en una fila. Modos: `en modo edición`, `en modo solo lectura`. Si se abre como ventana modal sobre la pantalla actual, añadir `, como ventana modal`. Si el grid no abre nada al hacer click, poner `—`.
- **Botones del toolbar**: títulos visibles de los botones que aparecen encima del grid, separados por comas. Incluye el botón "Nuevo" (con su título personalizado, p.ej. `"Añadir un nuevo ciclo"`) y cualquier botón extra del toolbar (`"Importar"`, `"Exportar"`, etc.). `—` si no hay ninguno (ni siquiera "Nuevo"). El detalle de cada botón se describe en la tabla `Botones`.
  - **Decisión obligatoria sobre el botón "Nuevo"**: en cada grid el análisis debe **decidir explícitamente** si existe el botón "Nuevo" o no. Si existe, se lista aquí con su título personalizado. Si no existe, hay que justificar el motivo — bien dentro de esta propiedad (p.ej. `— (las tareas las crean otros sistemas, no se permite crear desde aquí)`), bien en la tabla `Acciones` de la entidad donde la operación "Crear (insert)" estará marcada como `Nunca — <motivo>`. No vale dejarlo implícito.
- **Botones de las columnas**: títulos visibles de los botones que aparecen como columna adicional en cada fila (p.ej. `"Reintentar"`, `"Ver detalle"`), separados por comas. `—` si no hay ninguno. El detalle se describe en la tabla `Botones`.

### Botones

Sección **opcional**. Si el grid no tiene botones (ni Nuevo, ni toolbar, ni de fila), se pone `*(sin botones)*` en lugar de la tabla y se justifica con un motivo breve si no es obvio.

| Botón                  | Qué hace                                            |
|------------------------|-----------------------------------------------------|
| *("<título visible>")* | *(cadena de acciones que dispara, separadas por →)* |

**Convenciones**:

- Cada botón se identifica por su **título visible** entre comillas.
- Para el botón "Nuevo" se pone su título personalizado (p.ej. `"Añadir un nuevo ciclo"`, `"Nuevo correo"`). Su `Qué hace` típicamente es `Abre el formulario para crear un nuevo <entidad>`.
- Botones del toolbar (los que actúan sobre la selección o sin selección) y botones de las columnas (los que aparecen en cada fila y actúan sobre el registro de esa fila) se listan **en la misma tabla**, sin separación. El contexto se aclara en `Qué hace` si hace falta (`Borra las filas seleccionadas`, `Abre el wizard de importación`).
- La visibilidad de un botón **no** se pone aquí: si es condicional, se documenta en la sección `Reglas de UI` de abajo (botones de las columnas) o en la `Reglas de UI` del formulario (botones del formulario).

### Reglas de UI (U-XXX)

Sección **opcional** del grid. Solo puede contener reglas sobre **botones de las columnas** que se muestran u ocultan según los datos de la fila (p.ej. botón "Aprobar" visible solo si la fila está en estado `PENDIENTE`). **Los botones del toolbar no admiten visibilidad condicional** — están siempre o no están. Si no hay reglas, se omite la sección o se pone `*(no aplica)*`.

| ID | Disparador | Efecto | Campo/Panel afectado | Condición |
|----|------------|--------|----------------------|-----------|
| U-XXX | continuo | Mostrar/ocultar | botón "<título>" de la fila | *(condición funcional sobre los datos de la fila)* |

Las reglas usan el mismo formato y pool de numeración global que las del formulario. Solo se admite el disparador `continuo` y el efecto `Mostrar/ocultar` sobre botones de las columnas — cualquier otro caso pertenece a la `Reglas de UI` del formulario.

---

## Formulario N — <Título del formulario>

*(Se numera N empezando por el mismo número del grid que lo abre — Grid 1 abre Formulario 1, Grid 2 abre Formulario 2, etc.)*

### Paneles

Lista de paneles del formulario **en el orden vertical en el que aparecen**. Una fila por panel. Los botones se listan **dentro de la columna `Campos`** por su título (`botón "Borrar"`), aunque su detalle de acción esté en la tabla `Botones` aparte.

| Panel (título)                          | Tipo                | Campos                                                    |
|-----------------------------------------|---------------------|-----------------------------------------------------------|
| *("<título visible>" o "(sin título)")* | *(ver tipos abajo)* | *(campos y botones separados por comas, en orden visual)* |

**Valores admitidos en `Tipo`**:

- `normal` — panel estándar con campos.
- `normal (siempre solo lectura)` — panel cuyos campos están todos en solo lectura permanentemente.
- `anidado → <pantalla X>` — panel que muestra una sub-pantalla anidada (`panel-related`). El destino se identifica por el nombre del Grid de la sub-pantalla dentro del mismo fichero, p.ej. `anidado → Grid 2 ("Cursos")`.
- `botones` — panel que solo contiene botones (típicamente al final del formulario).
- `pestañas` — panel que agrupa pestañas (`panel-tabs`). Cada pestaña hija se lista como un panel `pestaña` aparte.
- `pestaña` — pestaña dentro de un panel `pestañas`.

**Columna `Campos`**:

- Nombres funcionales separados por comas, en orden visual.
- Los botones se incluyen aquí por su título (`botón "Salir"`, `botón "Borrar"`), no por nombre técnico.
- Para paneles `anidado` se pone `—` (la sub-pantalla se describe en su propia sección).
- Para paneles `pestaña` el "campo" suele ser una descripción del contenido (`visor PDF del documento original (incrustado)`).

### Botones

Sección **opcional**. Igual estructura que la del grid: `Botón | Qué hace`. Si el formulario no tiene botones se pone `*(sin botones)*`.

| Botón                  | Qué hace               |
|------------------------|------------------------|
| *("<título visible>")* | *(cadena de acciones)* |

**Convenciones para `Qué hace`**:

La cadena de acciones se separa con `→` cuando hay varios pasos. Cada paso puede ser:

- **Cambio puramente de UI**: `Cambia el asistente al paso "rechazar"`, `Vuelve al paso 1`.
- **Validación**: `Valida V-006`.
- **Operación de la entidad**: `Ejecuta la operación "Marcar como rechazada" (R-001, R-002)` — el nombre coincide con el de la tabla `Acciones` de la entidad.
- **Navegación**: `Cierra el formulario`, `Vuelve a la pantalla anterior`, `Redirige a la pantalla X`.

Si dos botones tienen el **mismo título** dentro del mismo formulario (típico en asistentes con un "Atrás" en cada paso), se desambigua con un calificador entre paréntesis: `"Atrás" (en panel "<X>")`.

### Reglas de UI (U-XXX)

Tabla canónica de `k-validaciones` para reglas de UI. Una regla de UI cambia el aspecto del formulario en función del valor de un campo, del usuario o del padre. **No bloquea ni escribe en BD**. Si no hay reglas, se pone `*(no aplica)*`.

| ID    | Disparador                                               | Efecto                | Campo/Panel afectado       | Condición                                   |
|-------|----------------------------------------------------------|-----------------------|----------------------------|---------------------------------------------|
| U-001 | *(`continuo` / `onNew` / `onLoad` / `onChange:<campo>`)* | *(ver efectos abajo)* | *(campo o panel concreto)* | *(condición funcional en lenguaje natural)* |

**Reglas:**

- **ID global** del análisis: `U-001`, `U-002`, … sin reinicio por pantalla. Mismo pool que para `V-XXX` y `R-XXX` lo es para las suyas.
- **Disparador**:
  - `continuo` — la regla se evalúa permanentemente (atributos `*If` declarativos en el diseño).
  - `onNew` — al abrir el formulario en modo nuevo (valor inicial al crear).
  - `onLoad` — al abrir el formulario para un registro existente.
  - `onChange:<campo>` — al cambiar el valor de un campo concreto.
- **Efecto** (valores típicos):
  - `Mostrar/ocultar` (sobre campo, panel o botón).
  - `Marcar solo lectura` / `Quitar solo lectura`.
  - `Marcar obligatorio` / `Quitar obligatorio`.
  - `Valor por defecto` (con `onNew`).
  - `Filtrar dominio` (sobre un campo relacional).
  - `Cambiar título`.
- **Campo/Panel afectado**: nombre funcional del elemento (`campo "fecha de resolución"`, `panel "Log de errores"`, `botón "Reintentar"`).
- **Condición**: lenguaje natural. **NO** se escriben expresiones de código (`estado != 'BORRADOR'` se escribe como `Visible solo si el estado no es BORRADOR`).
- **Las reglas disparadas por clicks de botón NO van aquí** — se describen en la tabla `Botones` del formulario. La visibilidad de un botón **sí** va aquí si es más restrictiva que la del panel que lo contiene.

---

## Repetición de la jerarquía (cuando hay sub-pantallas anidadas)

Si un panel del Formulario 1 es de tipo `anidado → Grid 2`, después de la sección "Formulario 1" se añade una nueva sección "Grid 2" → "Formulario 2", siguiendo la misma plantilla. Y si el Formulario 2 tiene a su vez un `anidado → Grid 3`, se continúa con "Grid 3" → "Formulario 3". Y así sucesivamente.

Esquema típico de una pantalla con dos niveles de anidación:

```
# Pantalla: "..."
## Identidad
## Menú
---
## Grid 1 — "..."
   ### Propiedades / ### Botones
## Formulario 1 — ...
   ### Paneles / ### Botones / ### Reglas de UI (U-XXX)
---
## Grid 2 — "..."
   ### Propiedades / ### Botones
## Formulario 2 — ...
   ### Paneles / ### Botones / ### Reglas de UI (U-XXX)
---
## Grid 3 — "..."
   ...
```
