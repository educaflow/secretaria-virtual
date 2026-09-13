---
type: test-e2e
id: T-005
---

<!-- ARTEFACTO GENERADO por /sdd-create-tests-e2e — NO editar a mano.
     Snapshot "as-tested": copia de la descripción que pasó al depurar con /sdd-debug-with-test-e2e-desc.
     Fuente: .sdd/drafts/2026-09-11_08-46_nivel-segun-grado-del-ciclo/test-e2e-desc/t-005-el-nivel-solo-se-elige-entre-los-del-grado-del-ciclo.desc.md
     Iniciativa: 2026-09-11_08-46_nivel-segun-grado-del-ciclo
     Test: T-005  |  Origen ESC: ESC-005
     Para regenerar: /sdd-create-tests-e2e (sobrescribe desde la fuente). -->

# T-005 — El nivel solo se elige entre los del grado del ciclo

**Origen ESC:** ESC-005
**Verifica:** U-ciclos-004, U-niveles-002, U-niveles-001
**Pantalla principal:** screen-ciclos.md
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
2. **Cuando** abre el menú "Sistema educativo" → "Grados" y pulsa "Nuevo Grado".
3. **Y** rellena el campo "Código" con "G" y el campo "Nombre" con "Certificado de profesionalidad", y pulsa "Guardar".
4. **Y** abre el menú "Sistema educativo" → "Niveles" y pulsa "Añadir un nuevo nivel".
5. **Y** rellena el campo "Código" con "CPR1" y el campo "Nombre" con "Certificado de profesionalidad de nivel 1", elige el grado "Certificado de profesionalidad" y pulsa "Guardar".
6. **Y** abre el menú "Sistema educativo" → "Ciclos" y pulsa "Añadir un nuevo ciclo".
7. **Y** rellena el campo "Código" con "TSDAW" y el campo "Nombre" con "Desarrollo Web Avanzado", y elige la familia profesional "Informática y Comunicaciones".
8. **Y** elige el grado "Ciclo formativo".
9. **Y** abre el selector del campo "Nivel".

## Resultado esperado

- El selector ofrece exactamente "Ciclos formativos de grado Básico", "Ciclos Formativos de Grado Medio" y "Ciclos Formativos de Grado Superior".
- El selector no ofrece "Certificado de profesionalidad de nivel 1", que pertenece a otro grado.
