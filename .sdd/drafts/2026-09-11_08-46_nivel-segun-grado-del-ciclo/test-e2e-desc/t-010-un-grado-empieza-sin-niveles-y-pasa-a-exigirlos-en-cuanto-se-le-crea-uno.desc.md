---
type: test-e2e
id: T-010
---

# T-010 — Un grado empieza sin niveles y pasa a exigirlos en cuanto se le crea uno

**Origen ESC:** ESC-008
**Verifica:** U-ciclos-001, U-ciclos-002, U-ciclos-005, V-Ciclo-001, V-Ciclo-002, V-Nivel-001
**Pantalla principal:** screen-ciclos.md
**Tipo:** error

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

## Resultado esperado

- El sistema no guarda el ciclo y muestra el mensaje "El nivel es obligatorio para el grado indicado".
