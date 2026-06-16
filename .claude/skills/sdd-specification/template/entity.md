# Modelo: <Nombre>

<Descripción en lenguaje de negocio: qué representa este modelo, qué papel juega, su ciclo de vida resumido y si extiende o reutiliza algo existente.>

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

- RES-NNN — <condición que siempre debe cumplirse>

## Campos calculados

<!-- Valores que calcula el servidor, nunca el cliente. Si no hay, elimina esta sección. -->

- CC-NNN — <nombre_campo>
  - momento: lectura | escritura
  - sobreescribible: nunca | [ROL1, ROL2]
  - cálculo: <descripción>

## Acción: <NombreAcción>

<!-- Un encabezado `## Acción: <Nombre>` por cada acción de la entidad (Crear, Modificar, <acción de negocio>…) que tenga algo que declarar. Crear y Modificar se declaran SIEMPRE (al menos su AllowProperties). El resto de acciones aparece solo si recibe datos del formulario o tiene validaciones o reglas de negocio. Dentro de cada acción, las etiquetas en este orden: Input AllowProperties, validaciones, reglas de negocio (omite la que no aplique). -->

**Input AllowProperties:** <propiedades que el UI puede enviar> | (ninguna — <motivo, p.ej. la entidad es inmutable tras crearse>)

<!-- Obligatorio en Crear y Modificar. En el resto de acciones, solo si recibe datos del formulario (si no recibe datos, omite esta línea). Las propiedades listadas deben existir en "Campos" / "Campos calculados". Un campo calculado o inmutable no va en la acción que no lo permite. Lista siempre cerrada: propiedades explícitas o "(ninguna)", nunca "todas". -->

**Validaciones:**

- VAL-NNN — <aserción que debe cumplirse; si no se da, bloquea la acción>
  - condición: <opcional: cuándo aplica la validación (estado u otra circunstancia)>
  - actor: <opcional>
  - mensaje: <opcional>

**Reglas de negocio:**

- RN-NNN — <operación automática que el sistema ejecuta tras confirmarse la acción>
  - fase: antes_de_commit | después_de_commit
  - estado: <opcional: el estado de la entidad para que la regla aplique>
  - condición: <opcional>
