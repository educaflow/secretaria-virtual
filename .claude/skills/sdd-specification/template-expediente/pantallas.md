<!-- Plantilla de las PANTALLAS de una fase. Se instancia UNA VEZ POR FASE, con el nombre
     `pantallas-<fase>.md` (`<fase>` = el nombre de la fase en minúsculas, con `_` → `-`).
     Sustituye los placeholders <…> por contenido real y ELIMINA todos los comentarios como este.
     Ver la guía README.md de esta carpeta. -->

# Pantallas de la fase <FASE> — <título que ve el usuario>

<!-- CRITICAL — por cada estado de la fase hay SIEMPRE al menos dos pantallas:
     (a) la del perfil que tiene el turno, con lo que puede rellenar y sus botones de acción; y
     (b) la del resto de perfiles, de solo consulta y con un único botón «Salir».
     Un estado sin perfil (o sin acciones, o que cierra el expediente) solo tiene la (b). -->

## Estado <ESTADO>

### Pantalla: <ESTADO> — perfil <PERFIL>

<!-- La pantalla del perfil que tiene el turno. Repite esta subsección si el estado se presenta
     de forma distinta a más de un perfil. -->

- **Quién la ve:** el <PERFIL>, mientras el expediente está en <FASE> / <ESTADO>.
- **Qué ve el usuario, bloque a bloque:**
  - **<Título del bloque>** — <qué datos agrupa, en lenguaje de negocio>
  - **<Título del bloque>** — <…>
- **Qué puede rellenar:** <los datos editables, uno a uno> | *(nada: es de solo consulta)*
- **Qué solo puede consultar:** <los datos que ve pero no puede tocar>
- **Documentos que se le muestran:** <qué documento se le enseña, y si se ve incrustado en la pantalla o solo como enlace para descargarlo> | *(ninguno)*
- **Aviso permanente en pantalla:** «<texto literal del aviso informativo, si lo hay>» | *(ninguno)*
- **Botones:**
  - **«<texto del botón>»** — lanza la acción <ACCION>. <Pide confirmación con el texto «<texto>» | Sin confirmación.>
  - **«<texto del botón>»** — <…>

#### Reglas de pantalla

<!-- Solo lo que cambia lo que el usuario VE o puede editar. Si impide una operación es una
     comprobación (VAL-, en estados.md); si escribe o produce algo es una regla de negocio (RN-). -->

- RUI-<ESTADO>-<PERFIL>-001 — <qué se muestra, se oculta, se marca obligatorio o pasa a solo lectura>
  - disparador: <continuo | al abrir la pantalla | al cambiar <dato>>
  - condición: <la condición que lo activa, o «Siempre»>
- *(ninguna)*

### Pantalla: <ESTADO> — resto de perfiles (solo consulta)

<!-- OBLIGATORIA en TODOS los estados, sin excepción: es la red de seguridad para quien mira el
     expediente sin tener el turno. Sin ella, ese usuario no puede ni abrir el expediente. -->

- **Quién la ve:** cualquier perfil con acceso al expediente distinto del <PERFIL> <!-- en un estado sin perfil: todos, incluido quien lo creó -->, mientras el expediente está en <FASE> / <ESTADO>.
- **Qué ve el usuario, bloque a bloque:**
  - **<Título del bloque>** — <qué datos agrupa>
- **Qué puede rellenar:** *(nada: toda la pantalla es de solo consulta)*
- **Documentos que se le muestran:** <qué documento se le enseña> | *(ninguno)*
- **Botones:**
  - **«Salir»** — cierra el expediente y vuelve al listado, sin cambiar nada.

#### Reglas de pantalla

<!-- En esta pantalla el identificador lleva la palabra GENERICA en el lugar del perfil. -->

- RUI-<ESTADO>-GENERICA-001 — <qué se muestra u oculta a quien solo consulta>
  - disparador: <continuo | al abrir la pantalla>
  - condición: <la condición que lo activa, o «Siempre»>
- *(ninguna)*

---

<!-- Repite el bloque `## Estado …` por cada estado de esta fase, incluidos los que cierran el
     expediente y los que no tienen ninguna acción. -->
