---
name: menus-builder-orchestrator
description: Usa este skill para orquestar subagentes para crear o modificar entradas de menú (menuitem) en Axelor siguiendo el plan establecido y revisando el trabajo realizado.
---

# menus-builder-orchestrator

1. Lanza un subagente del tipo "plan" para preparar un plan de lo que tienes que hacer y usa todos los skills que consideres necesarios para realizar el plan. 
2. Debes implementar el plan, para ello:
  2.1. Debes lanzar el skill /menus-steps en un subagente del tipo "general-purpose" para realizar el trabajo que hay que hacer según el plan preparado en el paso anterior.
  2.2. Debes lanzar el skill /menus-reviewer en un subagente del tipo "explore" para revisar el trabajo realizado.
  2.3. Si el subagente responde con  "OK-No hay problemas", sigue con el siguiente paso sino vuelve al paso 2.1 para arreglar lo que haya que arreglar.
3. Ahora debes revisar si se ha seguido el plan inicial preparado en el paso 1. Si no se ha completado vuelve al paso 2 para arreglar lo que no se ha hecho según el plan.
