# Tests E2E

Tests concretos end-to-end materializados a partir de los escenarios (`ESC-NNN`) de las historias de usuario del `specification.md` y de las V/R/U del diseño.

Cada test es **independiente** (no depende del estado dejado por otro) y **trazable** (declara qué `ESC-NNN` materializa y qué V/R/U verifica). `/sdd-debug-with-test-e2e-desc` lo ejecuta contra la aplicación real tras la implementación (bucle de auto-corrección).

---

## Estado inicial de la base de datos

Estado previo (datos maestros del subsistema `sistemaeducativo`, cargados por su `data-init`) del que parten **todos** los tests. Ningún test puede presuponer más estado que este; cada test lo referencia en sus `Precondiciones`.

- **Grados**: «Ciclo formativo» (código `D`) y «Curso de especialización» (código `E`).
- **Niveles**: «Ciclos formativos de grado Básico» (`GB`), «Ciclos Formativos de Grado Medio» (`GM`) y «Ciclos Formativos de Grado Superior» (`GS`), los tres del grado «Ciclo formativo». El grado «Curso de especialización» no tiene ningún nivel.
- **Familias profesionales**: el catálogo completo, con «Informática y Comunicaciones», «Hostelería y Turismo», «Seguridad y Medio Ambiente», «Electricidad y Electrónica» e «Instalación y Mantenimiento» entre otras.
- **Leyes educativas**: entre ellas «Ley Orgánica 3/2022, de 31 de marzo, de Ordenación e Integración de la Formación Profesional».
- **Ciclos**: «Desarrollo de Aplicaciones Web», «Desarrollo de Aplicaciones Multiplataforma», «Administración de Sistemas Informáticos en Red» (código `ASIR`), «Sistemas Microinformáticos y Redes», «Gestión de Alojamientos Turísticos», «Guía, Información y Asistencia Turística» y «Agencia de Viajes y Gestión de Eventos». Todos son del grado «Ciclo formativo»; todos menos «Sistemas Microinformáticos y Redes» (nivel «Ciclos Formativos de Grado Medio») llevan el nivel «Ciclos Formativos de Grado Superior».
- **Cursos**: dos por ciclo, entre ellos «1º DAW» y «2º DAW» del ciclo «Desarrollo de Aplicaciones Web».
- **Módulos de cada curso**: entre ellos «Itinerario Personal para la Empleabilidad I» en el curso «1º DAW». Existe además el módulo «Programación» en el catálogo de módulos.

**Usuarios de acceso** (login y contraseña que `/sdd-debug-with-test-e2e-desc` usará para iniciar sesión):

| Login | Contraseña | Rol / Tipo | Centro |
|---|---|---|---|
| admin | admin | Administrador | — |

---

## T-001 — Alta de un ciclo formativo con su nivel

**Origen ESC:** ESC-001
**Verifica:** U-ciclos-001, U-ciclos-002, V-Ciclo-001
**Pantalla principal:** screen-ciclos.md
**Tipo:** happy

### Precondiciones
- El estado descrito en «Estado inicial de la base de datos».

### Pasos
1. **Dado** que el administrador ha iniciado sesión con usuario «admin» y contraseña «admin».
2. **Cuando** abre el menú "Sistema educativo" → "Ciclos" y pulsa "Añadir un nuevo ciclo".
3. **Y** rellena el campo "Código" con "TSPRL" y el campo "Nombre" con "Prevención de Riesgos Profesionales".
4. **Y** elige la familia profesional "Seguridad y Medio Ambiente".
5. **Y** elige el grado "Ciclo formativo".
6. **Entonces** el sistema muestra el campo "Nivel" y lo señala como obligatorio.
7. **Cuando** elige el nivel "Ciclos Formativos de Grado Superior".
8. **Y** pulsa "Guardar".

### Resultado esperado
- El sistema guarda el ciclo y vuelve al listado de ciclos.
- La fila "Prevención de Riesgos Profesionales" aparece con el grado "Ciclo formativo" y el nivel "Ciclos Formativos de Grado Superior".

---

## T-002 — Alta de un curso de especialización sin nivel

**Origen ESC:** ESC-002
**Verifica:** U-ciclos-001, V-Ciclo-002
**Pantalla principal:** screen-ciclos.md
**Tipo:** happy

### Precondiciones
- El estado descrito en «Estado inicial de la base de datos».

### Pasos
1. **Dado** que el administrador ha iniciado sesión con usuario «admin» y contraseña «admin».
2. **Cuando** abre el menú "Sistema educativo" → "Ciclos" y pulsa "Añadir un nuevo ciclo".
3. **Y** rellena el campo "Código" con "CEIABD" y el campo "Nombre" con "Inteligencia Artificial y Big Data (especialización)".
4. **Y** elige la familia profesional "Informática y Comunicaciones".
5. **Y** elige el grado "Curso de especialización".
6. **Entonces** el sistema no muestra el campo "Nivel".
7. **Cuando** pulsa "Guardar".

### Resultado esperado
- El sistema guarda el ciclo y vuelve al listado de ciclos.
- La fila "Inteligencia Artificial y Big Data (especialización)" aparece con el grado "Curso de especialización" y la columna "Nivel" vacía.

---

## T-003 — Un ciclo formativo sin nivel no se puede guardar

**Origen ESC:** ESC-003
**Verifica:** V-Ciclo-001, U-ciclos-001
**Pantalla principal:** screen-ciclos.md
**Tipo:** error

### Precondiciones
- El estado descrito en «Estado inicial de la base de datos».

### Pasos
1. **Dado** que el administrador ha iniciado sesión con usuario «admin» y contraseña «admin».
2. **Cuando** abre el menú "Sistema educativo" → "Ciclos" y pulsa "Añadir un nuevo ciclo".
3. **Y** rellena el campo "Código" con "TSAF" y el campo "Nombre" con "Automatización y Robótica Industrial".
4. **Y** elige la familia profesional "Electricidad y Electrónica".
5. **Y** elige el grado "Ciclo formativo" y deja el campo "Nivel" vacío.
6. **Y** pulsa "Guardar".

### Resultado esperado
- El sistema no guarda el ciclo y muestra el mensaje "El nivel es obligatorio para el grado indicado".
- Al abrir de nuevo el menú "Sistema educativo" → "Ciclos", el listado no muestra ninguna fila "Automatización y Robótica Industrial".

---

## T-004 — Al cambiar el grado a uno sin niveles, el nivel se vacía

**Origen ESC:** ESC-004
**Verifica:** U-ciclos-003, U-ciclos-001, V-Ciclo-002
**Pantalla principal:** screen-ciclos.md
**Tipo:** happy

### Precondiciones
- El estado descrito en «Estado inicial de la base de datos».

### Pasos
1. **Dado** que el administrador ha iniciado sesión con usuario «admin» y contraseña «admin».
2. **Cuando** abre el menú "Sistema educativo" → "Ciclos" y pulsa "Añadir un nuevo ciclo".
3. **Y** rellena el campo "Código" con "TSMER" y el campo "Nombre" con "Mecatrónica Industrial".
4. **Y** elige la familia profesional "Instalación y Mantenimiento".
5. **Y** elige el grado "Ciclo formativo" y el nivel "Ciclos Formativos de Grado Superior".
6. **Y** pulsa "Guardar".
7. **Entonces** el sistema guarda el ciclo y vuelve al listado, donde "Mecatrónica Industrial" aparece con el grado "Ciclo formativo" y el nivel "Ciclos Formativos de Grado Superior".
8. **Cuando** pulsa la fila "Mecatrónica Industrial".
9. **Entonces** el sistema abre el formulario del ciclo con el grado "Ciclo formativo" y el campo "Nivel" con el valor "Ciclos Formativos de Grado Superior".
10. **Cuando** cambia el grado a "Curso de especialización".
11. **Entonces** el sistema vacía el nivel y deja de mostrar el campo "Nivel".
12. **Cuando** pulsa "Guardar".

### Resultado esperado
- El sistema guarda el ciclo y vuelve al listado, donde la fila "Mecatrónica Industrial" muestra el grado "Curso de especialización" y la columna "Nivel" vacía.
- Al pulsar de nuevo la fila "Mecatrónica Industrial", el formulario muestra el grado "Curso de especialización" y no muestra el campo "Nivel".

---

## T-005 — El nivel solo se elige entre los del grado del ciclo

**Origen ESC:** ESC-005
**Verifica:** U-ciclos-004, U-niveles-002, U-niveles-001
**Pantalla principal:** screen-ciclos.md
**Tipo:** UI

### Precondiciones
- El estado descrito en «Estado inicial de la base de datos».

### Pasos
1. **Dado** que el administrador ha iniciado sesión con usuario «admin» y contraseña «admin».
2. **Cuando** abre el menú "Sistema educativo" → "Grados" y pulsa "Nuevo Grado".
3. **Y** rellena el campo "Código" con "G" y el campo "Nombre" con "Certificado de profesionalidad", y pulsa "Guardar".
4. **Y** abre el menú "Sistema educativo" → "Niveles" y pulsa "Añadir un nuevo nivel".
5. **Y** rellena el campo "Código" con "CPR1" y el campo "Nombre" con "Certificado de profesionalidad de nivel 1", elige el grado "Certificado de profesionalidad" y pulsa "Guardar".
6. **Y** abre el menú "Sistema educativo" → "Ciclos" y pulsa "Añadir un nuevo ciclo".
7. **Y** rellena el campo "Código" con "TSDAW" y el campo "Nombre" con "Desarrollo Web Avanzado", y elige la familia profesional "Informática y Comunicaciones".
8. **Y** elige el grado "Ciclo formativo".
9. **Y** abre el selector del campo "Nivel".

### Resultado esperado
- El selector ofrece exactamente "Ciclos formativos de grado Básico", "Ciclos Formativos de Grado Medio" y "Ciclos Formativos de Grado Superior".
- El selector no ofrece "Certificado de profesionalidad de nivel 1", que pertenece a otro grado.

---

## T-006 — El nivel de un ciclo debe pertenecer al grado del ciclo

**Origen ESC:** ESC-012
**Verifica:** V-Ciclo-003, U-ciclos-001
**Pantalla principal:** screen-ciclos.md
**Tipo:** error

### Precondiciones
- El estado descrito en «Estado inicial de la base de datos».

### Pasos
1. **Dado** que el administrador ha iniciado sesión con usuario «admin» y contraseña «admin».
2. **Cuando** abre el menú "Sistema educativo" → "Grados", pulsa "Nuevo Grado", rellena el campo "Código" con "H" y el campo "Nombre" con "Certificado profesional avanzado", y pulsa "Guardar".
3. **Entonces** el sistema guarda el grado y vuelve al listado, donde aparece la fila "Certificado profesional avanzado".
4. **Cuando** pulsa "Nuevo Grado", rellena el campo "Código" con "I" y el campo "Nombre" con "Certificado profesional experto", y pulsa "Guardar".
5. **Y** abre el menú "Sistema educativo" → "Niveles", pulsa "Añadir un nuevo nivel", rellena el campo "Código" con "CPA1" y el campo "Nombre" con "Certificado profesional avanzado de nivel 1", elige el grado "Certificado profesional avanzado" y pulsa "Guardar".
6. **Y** pulsa "Añadir un nuevo nivel", rellena el campo "Código" con "CPA2" y el campo "Nombre" con "Certificado profesional avanzado de nivel 2", elige el grado "Certificado profesional avanzado" y pulsa "Guardar".
7. **Y** abre el menú "Sistema educativo" → "Ciclos" y pulsa "Añadir un nuevo ciclo".
8. **Y** rellena el campo "Código" con "CPAINF" y el campo "Nombre" con "Certificado profesional avanzado de informática", y elige la familia profesional "Informática y Comunicaciones".
9. **Y** elige el grado "Certificado profesional avanzado".
10. **Entonces** el sistema muestra el campo "Nivel" y lo señala como obligatorio.
11. **Cuando** elige el nivel "Certificado profesional avanzado de nivel 1" y pulsa "Guardar".
12. **Entonces** el sistema guarda el ciclo y vuelve al listado, donde "Certificado profesional avanzado de informática" aparece con el grado "Certificado profesional avanzado" y el nivel "Certificado profesional avanzado de nivel 1".
13. **Cuando** abre el menú "Sistema educativo" → "Niveles", pulsa la fila "Certificado profesional avanzado de nivel 1", cambia el grado a "Certificado profesional experto" y pulsa "Guardar".
14. **Entonces** el sistema guarda el nivel y vuelve al listado, donde "Certificado profesional avanzado de nivel 1" aparece con el grado "Certificado profesional experto" y "Certificado profesional avanzado de nivel 2" conserva el grado "Certificado profesional avanzado".
15. **Cuando** abre el menú "Sistema educativo" → "Ciclos" y pulsa la fila "Certificado profesional avanzado de informática".
16. **Entonces** el formulario del ciclo muestra el grado "Certificado profesional avanzado", sigue mostrando el campo "Nivel" —porque ese grado conserva el nivel "Certificado profesional avanzado de nivel 2"— y en ese campo muestra el valor guardado "Certificado profesional avanzado de nivel 1".
17. **Cuando** cambia el nombre a "Certificado profesional avanzado de informática (LOFP)" y pulsa "Guardar".

### Resultado esperado
- El sistema no guarda el ciclo y muestra el mensaje "El nivel indicado no pertenece al grado del ciclo".
- Al abrir de nuevo el menú "Sistema educativo" → "Ciclos", el listado sigue mostrando la fila con el nombre "Certificado profesional avanzado de informática", sin el cambio que no se pudo guardar.

---

## T-007 — Editar un ciclo sin tocar el grado ni el nivel

**Origen ESC:** ESC-013
**Verifica:** V-Ciclo-001, V-Ciclo-003, U-ciclos-001
**Pantalla principal:** screen-ciclos.md
**Tipo:** happy

### Precondiciones
- El estado descrito en «Estado inicial de la base de datos».

### Pasos
1. **Dado** que el administrador ha iniciado sesión con usuario «admin» y contraseña «admin».
2. **Cuando** abre el menú "Sistema educativo" → "Ciclos" y pulsa la fila "Administración de Sistemas Informáticos en Red".
3. **Entonces** el formulario muestra el código "ASIR", la familia profesional "Informática y Comunicaciones", el grado "Ciclo formativo" y el nivel "Ciclos Formativos de Grado Superior".
4. **Cuando** cambia el nombre a "Administración de Sistemas Informáticos en Red (LOFP)" y pulsa "Guardar".

### Resultado esperado
- El sistema guarda el ciclo y vuelve al listado.
- La fila aparece con el nombre "Administración de Sistemas Informáticos en Red (LOFP)" y conserva el grado "Ciclo formativo" y el nivel "Ciclos Formativos de Grado Superior".

---

## T-008 — Alta y borrado de un nivel indicando su grado

**Origen ESC:** ESC-006
**Verifica:** V-Nivel-001, U-niveles-001
**Pantalla principal:** screen-niveles.md
**Tipo:** happy

### Precondiciones
- El estado descrito en «Estado inicial de la base de datos».

### Pasos
1. **Dado** que el administrador ha iniciado sesión con usuario «admin» y contraseña «admin».
2. **Cuando** abre el menú "Sistema educativo" → "Niveles" y pulsa "Añadir un nuevo nivel".
3. **Y** rellena el campo "Código" con "GMD" y el campo "Nombre" con "Ciclos Formativos de Grado Medio Dual", y elige el grado "Ciclo formativo".
4. **Y** pulsa "Guardar".
5. **Entonces** el sistema guarda el nivel y vuelve al listado, donde "Ciclos Formativos de Grado Medio Dual" aparece con el grado "Ciclo formativo".
6. **Cuando** pulsa la fila "Ciclos Formativos de Grado Medio Dual" y pulsa "Borrar".

### Resultado esperado
- El sistema borra el nivel.
- El listado vuelve a mostrar solo "Ciclos formativos de grado Básico", "Ciclos Formativos de Grado Medio" y "Ciclos Formativos de Grado Superior", los tres con el grado "Ciclo formativo".

---

## T-009 — Un nivel sin grado no se puede guardar

**Origen ESC:** ESC-007
**Verifica:** V-Nivel-001, U-niveles-001
**Pantalla principal:** screen-niveles.md
**Tipo:** error

### Precondiciones
- El estado descrito en «Estado inicial de la base de datos».

### Pasos
1. **Dado** que el administrador ha iniciado sesión con usuario «admin» y contraseña «admin».
2. **Cuando** abre el menú "Sistema educativo" → "Niveles" y pulsa "Añadir un nuevo nivel".
3. **Y** rellena el campo "Código" con "GSD" y el campo "Nombre" con "Ciclos Formativos de Grado Superior Dual", y deja el campo "Grado" vacío.
4. **Y** pulsa "Guardar".
5. **Entonces** el sistema no guarda el nivel y muestra el mensaje "El grado es obligatorio".
6. **Cuando** pulsa "Cancelar" y vuelve al listado de niveles.

### Resultado esperado
- El listado no muestra ninguna fila "Ciclos Formativos de Grado Superior Dual".

---

## T-010 — Un grado empieza sin niveles y pasa a exigirlos en cuanto se le crea uno

**Origen ESC:** ESC-008
**Verifica:** U-ciclos-001, U-ciclos-002, U-ciclos-005, V-Ciclo-001, V-Ciclo-002, V-Nivel-001
**Pantalla principal:** screen-ciclos.md
**Tipo:** error

### Precondiciones
- El estado descrito en «Estado inicial de la base de datos».

### Pasos
1. **Dado** que el administrador ha iniciado sesión con usuario «admin» y contraseña «admin».
2. **Cuando** abre el menú "Sistema educativo" → "Grados", pulsa "Nuevo Grado", rellena el campo "Código" con "F" y el campo "Nombre" con "Certificado profesional", y pulsa "Guardar".
3. **Entonces** el sistema guarda el grado y vuelve al listado, donde aparece la fila "Certificado profesional" con el código "F".
4. **Cuando** abre el menú "Sistema educativo" → "Ciclos", pulsa "Añadir un nuevo ciclo", rellena el campo "Código" con "CPINF" y el campo "Nombre" con "Certificado profesional de informática", y elige la familia profesional "Informática y Comunicaciones".
5. **Y** abre el selector del campo "Grado".
6. **Entonces** el selector ofrece, además de "Ciclo formativo" y "Curso de especialización", el grado "Certificado profesional".
7. **Cuando** elige el grado "Certificado profesional".
8. **Entonces** el sistema no muestra el campo "Nivel".
9. **Cuando** pulsa "Guardar".
10. **Entonces** el sistema guarda el ciclo y vuelve al listado, donde "Certificado profesional de informática" aparece con el grado "Certificado profesional" y la columna "Nivel" vacía.
11. **Cuando** abre el menú "Sistema educativo" → "Niveles", pulsa "Añadir un nuevo nivel", rellena el campo "Código" con "CP1" y el campo "Nombre" con "Certificado profesional de nivel 1", elige el grado "Certificado profesional" y pulsa "Guardar".
12. **Entonces** el sistema guarda el nivel y vuelve al listado, donde "Certificado profesional de nivel 1" aparece con el grado "Certificado profesional".
13. **Cuando** abre el menú "Sistema educativo" → "Ciclos" y pulsa la fila "Certificado profesional de informática".
14. **Entonces** el sistema muestra ahora el campo "Nivel", vacío, y lo señala como obligatorio.
15. **Cuando** pulsa "Guardar" sin elegir ningún nivel.

### Resultado esperado
- El sistema no guarda el ciclo y muestra el mensaje "El nivel es obligatorio para el grado indicado".

---

## T-011 — Los niveles ya cargados pertenecen al grado «Ciclo formativo»

**Origen ESC:** ESC-014
**Verifica:** V-Nivel-001
**Pantalla principal:** screen-niveles.md
**Tipo:** happy

### Precondiciones
- El estado descrito en «Estado inicial de la base de datos».

### Pasos
1. **Dado** que el administrador ha iniciado sesión con usuario «admin» y contraseña «admin».
2. **Cuando** abre el menú "Sistema educativo" → "Niveles".
3. **Entonces** el listado muestra las filas "Ciclos formativos de grado Básico", "Ciclos Formativos de Grado Medio" y "Ciclos Formativos de Grado Superior", las tres con el grado "Ciclo formativo".
4. **Cuando** pulsa la fila "Ciclos Formativos de Grado Medio".
5. **Entonces** el formulario muestra el código "GM", el nombre "Ciclos Formativos de Grado Medio" y el grado "Ciclo formativo".
6. **Cuando** cambia el nombre a "Ciclos Formativos de Grado Medio (LOFP)" y pulsa "Guardar".
7. **Entonces** el sistema guarda el nivel y vuelve al listado, donde la fila aparece con el nombre "Ciclos Formativos de Grado Medio (LOFP)" y conserva el grado "Ciclo formativo".
8. **Cuando** pulsa de nuevo esa fila, cambia el nombre a "Ciclos Formativos de Grado Medio" y pulsa "Guardar".

### Resultado esperado
- El sistema guarda el nivel y el listado vuelve a mostrar la fila "Ciclos Formativos de Grado Medio" con el grado "Ciclo formativo".

---

## T-012 — El listado de ciclos muestra el grado y el nivel

**Origen ESC:** ESC-009
**Verifica:** —
**Pantalla principal:** screen-ciclos.md
**Tipo:** UI

### Precondiciones
- El estado descrito en «Estado inicial de la base de datos».

### Pasos
1. **Dado** que el administrador ha iniciado sesión con usuario «admin» y contraseña «admin».
2. **Cuando** abre el menú "Sistema educativo" → "Ciclos".

### Resultado esperado
- El listado muestra las columnas en el orden "Código", "Nombre", "Familia profesional", "Grado" y "Nivel".
- La fila "Desarrollo de Aplicaciones Web" muestra el grado "Ciclo formativo" y el nivel "Ciclos Formativos de Grado Superior".
- La fila "Sistemas Microinformáticos y Redes" muestra el grado "Ciclo formativo" y el nivel "Ciclos Formativos de Grado Medio".

---

## T-013 — Al consultar un ciclo, el nivel solo se ve si el ciclo lo lleva

**Origen ESC:** ESC-010
**Verifica:** U-consulta-ciclo-001, U-ciclos-001, V-Ciclo-002
**Pantalla principal:** screen-consulta-ciclo.md
**Tipo:** UI

### Precondiciones
- El estado descrito en «Estado inicial de la base de datos».

### Pasos
1. **Dado** que el administrador ha iniciado sesión con usuario «admin» y contraseña «admin».
2. **Cuando** abre el menú "Sistema educativo" → "Ciclos", pulsa "Añadir un nuevo ciclo", rellena el campo "Código" con "CEIA" y el campo "Nombre" con "Inteligencia Artificial y Big Data", y elige la familia profesional "Informática y Comunicaciones".
3. **Y** elige el grado "Curso de especialización" y pulsa "Guardar".
4. **Entonces** el sistema guarda el ciclo y vuelve al listado, donde "Inteligencia Artificial y Big Data" aparece con el grado "Curso de especialización".
5. **Cuando** abre el menú "Sistema educativo" → "Cursos" y pulsa "Nuevo curso".
6. **Y** rellena el campo "Código" con "CEIA1" y el campo "Nombre" con "1º CEIA", elige el ciclo "Inteligencia Artificial y Big Data" y la ley educativa "Ley Orgánica 3/2022, de 31 de marzo, de Ordenación e Integración de la Formación Profesional", y pulsa "Guardar".
7. **Entonces** el sistema guarda el curso y vuelve al listado, donde "1º CEIA" aparece con el ciclo "Inteligencia Artificial y Big Data".
8. **Cuando** pulsa la fila "1º CEIA" y después pulsa sobre el ciclo "Inteligencia Artificial y Big Data" ya elegido en el curso.
9. **Entonces** el sistema abre la consulta del ciclo en solo lectura, muestra el nombre, la familia profesional y el grado "Curso de especialización", y no muestra el campo "Nivel".
10. **Cuando** cierra la consulta, vuelve al formulario del curso "1º CEIA", cambia el ciclo del curso a "Guía, Información y Asistencia Turística" y pulsa sobre ese ciclo ya elegido.

### Resultado esperado
- El sistema abre la consulta del ciclo "Guía, Información y Asistencia Turística" y muestra además el campo "Nivel" con el valor "Ciclos Formativos de Grado Superior".

---

## T-014 — Los cursos de un ciclo se siguen manteniendo desde el propio ciclo

**Origen ESC:** ESC-011
**Verifica:** V-Ciclo-001, V-Ciclo-003, U-ciclos-001
**Pantalla principal:** screen-ciclos.md
**Tipo:** happy

### Precondiciones
- El estado descrito en «Estado inicial de la base de datos».

### Pasos
1. **Dado** que el administrador ha iniciado sesión con usuario «admin» y contraseña «admin».
2. **Cuando** abre el menú "Sistema educativo" → "Ciclos" y pulsa la fila "Desarrollo de Aplicaciones Web".
3. **Entonces** el panel "Cursos" muestra "1º DAW" y "2º DAW".
4. **Cuando** en el panel "Cursos" pulsa "Añadir un nuevo curso".
5. **Y** rellena el campo "Código" con "DAW3" y el campo "Nombre" con "3º DAW", y elige la ley educativa "Ley Orgánica 3/2022, de 31 de marzo, de Ordenación e Integración de la Formación Profesional".
6. **Y** pulsa "Guardar" en el formulario del curso.
7. **Y** pulsa "Guardar" en el formulario del ciclo.

### Resultado esperado
- El sistema guarda el ciclo y vuelve al listado.
- Al pulsar de nuevo la fila "Desarrollo de Aplicaciones Web", el panel "Cursos" muestra "1º DAW", "2º DAW" y "3º DAW".
- El formulario del ciclo sigue mostrando el grado "Ciclo formativo" y el nivel "Ciclos Formativos de Grado Superior".

---

## T-015 — Los módulos de un curso se siguen manteniendo desde el ciclo

**Origen ESC:** ESC-015
**Verifica:** V-Ciclo-001, V-Ciclo-003, U-ciclos-001
**Pantalla principal:** screen-ciclos.md
**Tipo:** happy

### Precondiciones
- El estado descrito en «Estado inicial de la base de datos».

### Pasos
1. **Dado** que el administrador ha iniciado sesión con usuario «admin» y contraseña «admin».
2. **Cuando** abre el menú "Sistema educativo" → "Ciclos" y pulsa la fila "Desarrollo de Aplicaciones Web".
3. **Y** en el panel "Cursos" pulsa la fila "1º DAW".
4. **Entonces** el panel "Módulos" del curso muestra "Itinerario Personal para la Empleabilidad I".
5. **Cuando** en el panel "Módulos" pulsa "Añadir un nuevo módulo" y elige el módulo "Programación".
6. **Y** pulsa "Guardar" en el formulario del módulo.
7. **Y** pulsa "Guardar" en el formulario del curso.
8. **Y** pulsa "Guardar" en el formulario del ciclo.

### Resultado esperado
- El sistema guarda el ciclo y vuelve al listado.
- Al pulsar de nuevo la fila "Desarrollo de Aplicaciones Web" y, en el panel "Cursos", la fila "1º DAW", el panel "Módulos" muestra "Itinerario Personal para la Empleabilidad I" y "Programación".
- El formulario del ciclo sigue mostrando el grado "Ciclo formativo" y el nivel "Ciclos Formativos de Grado Superior".
