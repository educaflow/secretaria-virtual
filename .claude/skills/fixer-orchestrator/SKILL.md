---
name: fixer-orchestrator
description: Usa este skill para orquestar subagentes en un bucle de revisar y arreglar cosas. Se crea un ciclo de revisión y correción (fix) de los menús mediantes subagentes hasta que todo esté corecto. Este Skill debe ser llamado con el código que se debe corregir y además con uno o más skill que se debe usar como conocimiento para corregir el código. El skill se encargará de orquestar el proceso de revisión y corrección del código mediante subagentes que usarán los skills indicados para revisar y corregir el código. 
---

# fixer-orchestrator

- Eres un expertor revisor y corrector de código. Tu tarea es revisar código para dectar errores y corregirlos. Para ello debes seguir un proceso iterativo de revisión y corrección hasta que el código esté perfecto.
- El conocimiento específico para revisar y corregir el código te lo deben pasar en el prompt como el nombre de un o más skill que deberás cargar. 
- Si no te indican ningún skill no harás nada e indicarás que no se te ha indicado el skill a usar  
- Tu tarea es orquestar subagentes que usen estos skills para revisar y corregir el código.
- También te debe indicar la ubicación del código a revisar y corregir. Si no te indican la ubicación, no harás nada e indicarás que no se te ha indicado el código a revisar.

Ejecuta este bucle:

1. Lanza un subagente con su propio contexto que revise el código. Este subgente debe hacer lo siguiente:
    - Carga los skills que te han indicado en el prompt y que son necesarios para revisar y corregir el código
    - Revisa el código buscando errores, inconsistencias o mejoras. Para ello debe comparar el código con el conocimiento que ha cargado en los skills
    - NO debe modificar ningún archivo. Debe responder de una de estas dos formas:
    - Si no encuentra errores, inconsistencias o mejoras debe decir exactamente: **OK-No hay problemas**
    - Si encuentra errores, inconsistencias o mejoras debe:
        - Responder explicando cada error, inconsistencia o mejora encontrada 
        - Debe hacerlo en formato  siguiente:
         ```text
         BEGIN:----
         Descripción del error, inconsistencia o mejora encontrada 1
         END:----
         
         BEGIN:----
         Descripción del error, inconsistencia o mejora encontrada 2
         END:----
         ```
        - Una vez generada la lista, este agente debe lanzar un segundo subagente con su propio contexto que hagas las correcciones. Este subgente debe hacer lo siguiente:
            - Carga los skills que han indicado en el prompt y que son necesarios para revisar y corregir el código
            - Debe saber la ubicación del código que hay que corregir.
            - Recibe la lista de errores, inconsistencias o mejoras encontrada por el primer subagente. 
            - Para cada error, inconsistencia o mejora debe realizar la corección necesaria en el código para resolverlo. Para ello debe usar el mismo conocimiento que el primer subagente (los skills que has cargado).
        - Una vez el segundo subagente ha terminado de realizar las correcciones, terminará también el subagente que revisa el código, pero no debe devolver nada al fixer-orchestrator, ya que el fixer-orchestrator no necesita ninguna información y así no se aumenta el tamaño del contexto principal.
2. Si el subagente respondió "OK-No hay problemas", termina aquí y no hagas nada más sino vuelve al paso 1.

Si tras 30 iteraciones todavía no se ha mostrado "OK-No hay problemas",
detente y reporta que no has podido seguir corrigiendo el código.