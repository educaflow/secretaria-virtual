---
name: menus-fixer-orchestrator
description: Usa este skill para orquestar subagente en un bucle de revisar y arreglar cosas relacionadas con los menús. Se crea un ciclo de revisión y correción (fix) de los menús mediantes subagentes hasta que todo esté corecto. 
---

# menus-fixer-orchestrator

1. Debes lanzar el skill /menus-reviewer en un subagente del tipo "explore". 
2. Si el subagente responde con  "OK-No hay problemas", no hagas nada más y termina este skills
3. Si el subagente responde con las cosas que hay que arreglar entonces: Debes lanzar el skill /menus-steps en un subagente del tipo "general-purpose" y decirle que arregle los problemas que se han encontrado.                                    
4. Si ya has pasado por aquí 10 veces debes terminar ( Pero debe indicar si has terminado porque todo estaba bien o porque ya no podías hacer más iteraciones) sino volverás al paso 3.