# Modelo: <Nombre>

<!-- SOLO si el modelo ya existe en el código (iniciativa con línea `**Modifica:**`), añadir la línea siguiente. Con ella, este fichero declara SOLO el delta: los campos, estados, restricciones, campos calculados y reglas NUEVOS o CAMBIADOS. Todo lo no mencionado del modelo real MUST conservarse tal cual. MUST NOT copiar aquí los campos/reglas existentes que no cambian — el código es la fuente de verdad del estado actual. Excepción: la línea `Input AllowProperties` de cada acción que el delta toque declara la lista RESULTANTE COMPLETA (es una whitelist cerrada de seguridad, no admite semántica aditiva); las acciones no declaradas se conservan, y las propiedades preexistentes de la lista no se re-declaran en «Campos». -->
**Modelo existente:** sí

<Descripción en lenguaje de negocio: qué representa este modelo, qué papel juega, su ciclo de vida resumido y si extiende o reutiliza algo existente. Si es un modelo existente: qué cambia y por qué.>

## Campos

<!-- Campos funcionalmente relevantes, SIN tipo de dato y SIN campos técnicos (IDs, FKs internas, auditoría, versiones, flags). Un campo por viñeta: nombre conceptual y qué representa. Un campo NO declara aquí "obligatorio" ni "inmutable": lo obligatorio va como VAL/RES y lo no editable lo gobierna AllowProperties. Para un campo de estado, sus valores van en "Estados y transiciones"; para otro enum, menciónalos en la descripción. -->

- **<nombre_campo>** — <qué representa>
- **<nombre_campo>** — <…>

## Estados y transiciones

<!-- Solo si el modelo tiene ciclo de vida. Si no lo tiene, elimina esta sección entera. -->

- Estado inicial: <ESTADO>
- <ESTADO_A> → <ESTADO_B>: <qué acción o circunstancia provoca la transición>
- <ESTADO terminal>: <por qué es terminal>

## Restricciones

<!-- Restricciones de la entidad: deben cumplirse SIEMPRE, en todas las acciones. Si solo aplican a una acción concreta, van como validación de esa acción. Si no hay, elimina esta sección. -->

- RES-<Entidad>-NNN — <condición que siempre debe cumplirse>

## Campos calculados

<!-- Valores que calcula el servidor, nunca el cliente. Si no hay, elimina esta sección. -->

- CC-<Entidad>-NNN — <nombre_campo>
  - momento: lectura | escritura
  - sobreescribible: nunca | [ROL1, ROL2]
  - cálculo: <descripción>

## Acción: <NombreAcción>

<!-- Un encabezado `## Acción: <Nombre>` por cada acción de la entidad (Crear, Modificar, <acción de negocio>…) que tenga algo que declarar. Crear y Modificar se declaran SIEMPRE (al menos su AllowProperties). El resto de acciones aparece solo si recibe datos del formulario o tiene validaciones o reglas de negocio. Dentro de cada acción, las etiquetas en este orden: Input AllowProperties, validaciones, reglas de negocio (omite la que no aplique). -->

**Input AllowProperties:** <propiedades que el UI puede enviar> | (ninguna — <motivo, p.ej. la entidad es inmutable tras crearse>)

<!-- Obligatorio en Crear y Modificar. En el resto de acciones, solo si recibe datos del formulario (si no recibe datos, omite esta línea). Las propiedades listadas deben existir en "Campos" / "Campos calculados". Un campo calculado o inmutable no va en la acción que no lo permite. Lista siempre cerrada: propiedades explícitas o "(ninguna)", nunca "todas". -->

**Validaciones:**

- VAL-<Entidad>-NNN — <aserción que debe cumplirse; si no se da, bloquea la acción>
  - condición: <opcional: cuándo aplica la validación (estado u otra circunstancia)>
  - actor: <opcional>
  - mensaje: <opcional>

**Reglas de negocio:**

- RN-<Entidad>-NNN — <operación automática que el sistema ejecuta tras confirmarse la acción>
  - fase: antes_de_commit | después_de_commit
  - estado: <opcional: el estado de la entidad para que la regla aplique>
  - condición: <opcional>
