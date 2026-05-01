---
name: importacion-orchestrator
description: Usa este skill para orquestar subagentes para crear, modificar, analizar o revisar entradas de importación (import) en Axelor siguiendo el plan establecido y revisando el trabajo realizado.
---

# importacion-orchestrator

1. Debes preguntar al usuario si quiere implementar la solución o mostrar el plan de lo que se va a hacer. Si el usuario quiere mostrar el plan, sigue el paso 2 para preparar el plan pero no implementes nada. Si el usuario quiere implementar la solución, sigue con el paso 2 para preparar el plan y luego implementa el plan siguiendo los pasos indicados en el punto 3.
2. Si el usario quiere mostrar el plan, presentarás un resumen con las clases y métodos que se van a crear o modificar.
3. Lanza un subagente del tipo "plan" para preparar un plan de lo que tienes que hacer y usa todos los skills que consideres necesarios para realizar el plan.
4. Debes implementar el plan, para ello:
   2.1. Debes lanzar el skill /importacion-steps en un subagente del tipo "general-purpose" para realizar el trabajo que hay que hacer según el plan preparado en el paso anterior.
   2.2. Debes lanzar el skill /importacion-reviewer en un subagente del tipo "explore" para revisar el trabajo realizado.
   2.3. Si el subagente responde con  "OK-No hay problemas", sigue con el siguiente paso sino vuelve al paso 2.1 para arreglar lo que haya que arreglar.
5. Ahora debes revisar si se ha seguido el plan inicial preparado en el paso 1. Si no se ha completado vuelve al paso 2 para arreglar lo que no se ha hecho según el plan.
6. Si el usuario ha elegido mostrar el plan en el paso 1, ahora debes mostrar el plan preparado en el paso 2 al usuario para que lo revise. Si el usuario aprueba el plan, sigue con el paso 2 para implementar el plan siguiendo los pasos indicados en el punto 3. Si el usuario no aprueba el plan, vuelve al paso 2 para preparar un nuevo plan.
