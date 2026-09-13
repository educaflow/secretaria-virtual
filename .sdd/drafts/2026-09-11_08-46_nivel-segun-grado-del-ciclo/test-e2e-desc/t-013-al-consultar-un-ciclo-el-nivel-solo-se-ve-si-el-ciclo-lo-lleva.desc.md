---
type: test-e2e
id: T-013
---

# T-013 — Al consultar un ciclo, el nivel solo se ve si el ciclo lo lleva

**Origen ESC:** ESC-010
**Verifica:** U-consulta-ciclo-001, U-ciclos-001, V-Ciclo-002
**Pantalla principal:** screen-consulta-ciclo.md
**Tipo:** UI

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

## Precondiciones

- El estado descrito en «Estado inicial de la base de datos».

## Pasos

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

## Resultado esperado

- El sistema abre la consulta del ciclo "Guía, Información y Asistencia Turística" y muestra además el campo "Nivel" con el valor "Ciclos Formativos de Grado Superior".
