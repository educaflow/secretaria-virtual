# Tests E2E

Escenarios concretos de prueba end-to-end materializados a partir de los flujos principales (`F-NNN`) del `specification.md` y de las V/R/U inferidas en `entity-*.md` / `screen-*.md`.

Cada test es **independiente** (no depende del estado dejado por otro test) y **trazable** (cada uno declara qué `F-NNN` materializa y qué V/R/U verifica).

`/sdd-implementer-system` lee este fichero tras escribir el código Java, traduce cada escenario a comandos `playwright-cli` al vuelo y ejecuta un **bucle de auto-corrección**: si un test falla, vuelve a invocar a `code-implementer` con el reporte para que arregle el código.

---



## T-001 — Administrador crea un nuevo correo y lo ve en el listado

**Origen F:** F-002
**Verifica:** V-TareaCorreo-001, V-TareaCorreo-002, V-TareaCorreo-003, V-TareaCorreo-004, R-TareaCorreo-001
**Pantalla principal:** screen-todos.md
**Tipo:** happy

### Precondiciones
- El usuario Administrador ha iniciado sesión.
- La pantalla "Todos los correos" no contiene ninguna fila con asunto "Notificación matrícula T-001".

### Pasos
1. **Dado** que el usuario está en la pantalla "Todos los correos".
2. **Cuando** pulsa el botón "Nuevo correo" del toolbar.
3. **Entonces** se abre el formulario "Detalle de correo" en modo edición sobre una TareaCorreo nueva.
4. **Y** introduce en "asunto" el valor "Notificación matrícula T-001".
5. **Y** introduce en "cuerpo" el valor "Su matrícula ha sido tramitada correctamente.".
6. **Y** introduce en "DNI del destinatario" el valor "24362574P".
7. **Y** introduce en "dirección de correo del destinatario" el valor "alumno@example.com".
8. **Y** pulsa el botón "Guardar y enviar".

### Resultado esperado
- El sistema vuelve a la pantalla "Todos los correos".
- En el listado aparece una fila nueva con asunto "Notificación matrícula T-002", DNI del destinatario "24362574P", fecha de creación correspondiente al instante actual y estado "PENDIENTE".
- Al abrir el detalle, el cuerpo es "Su matrícula ha sido tramitada correctamente." y el número de intentos es 0.


## T-002 — Administrador crea un nuevo correo pero falla porque en DNI es erroneo

**Origen F:** F-002
**Verifica:** V-TareaCorreo-001, V-TareaCorreo-002, V-TareaCorreo-003, V-TareaCorreo-004, R-TareaCorreo-001
**Pantalla principal:** screen-todos.md
**Tipo:** happy

### Precondiciones
- El usuario Administrador ha iniciado sesión.
- La pantalla "Todos los correos" no contiene ninguna fila con asunto "Notificación matrícula T-002".

### Pasos
1. **Dado** que el usuario está en la pantalla "Todos los correos".
2. **Cuando** pulsa el botón "Nuevo correo" del toolbar.
3. **Entonces** se abre el formulario "Detalle de correo" en modo edición sobre una TareaCorreo nueva.
4. **Y** introduce en "asunto" el valor "Notificación matrícula T-002".
5. **Y** introduce en "cuerpo" el valor "Su matrícula ha sido tramitada correctamente.".
6. **Y** introduce en "DNI del destinatario" el valor "24362574Q".
7. **Y** introduce en "dirección de correo del destinatario" el valor "alumno@example.com".
8. **Y** pulsa el botón "Guardar y enviar".

### Resultado esperado
- El sistema indica que el DNI introducido no es válido y no permite guardar ni enviar el correo.