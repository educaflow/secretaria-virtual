---
type: test-e2e
id: T-004
---

<!-- ARTEFACTO GENERADO por /sdd-create-tests-e2e — NO editar a mano.
     Snapshot "as-tested": copia de la descripción que pasó al depurar con /sdd-debug-with-test-e2e-desc.
     Fuente: .sdd/drafts/2026-09-11_08-46_nivel-segun-grado-del-ciclo/test-e2e-desc/t-004-al-cambiar-el-grado-a-uno-sin-niveles-el-nivel-se-vacia.desc.md
     Iniciativa: 2026-09-11_08-46_nivel-segun-grado-del-ciclo
     Test: T-004  |  Origen ESC: ESC-004
     Para regenerar: /sdd-create-tests-e2e (sobrescribe desde la fuente). -->

# T-004 — Al cambiar el grado a uno sin niveles, el nivel se vacía

**Origen ESC:** ESC-004
**Verifica:** U-ciclos-003, U-ciclos-001, V-Ciclo-002
**Pantalla principal:** screen-ciclos.md
**Tipo:** happy

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

## Resultado esperado

- El sistema guarda el ciclo y vuelve al listado, donde la fila "Mecatrónica Industrial" muestra el grado "Curso de especialización" y la columna "Nivel" vacía.
- Al pulsar de nuevo la fila "Mecatrónica Industrial", el formulario muestra el grado "Curso de especialización" y no muestra el campo "Nivel".
