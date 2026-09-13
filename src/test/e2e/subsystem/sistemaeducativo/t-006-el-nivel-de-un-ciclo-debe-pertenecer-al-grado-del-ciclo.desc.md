---
type: test-e2e
id: T-006
---

<!-- ARTEFACTO GENERADO por /sdd-create-tests-e2e — NO editar a mano.
     Snapshot "as-tested": copia de la descripción que pasó al depurar con /sdd-debug-with-test-e2e-desc.
     Fuente: .sdd/drafts/2026-09-11_08-46_nivel-segun-grado-del-ciclo/test-e2e-desc/t-006-el-nivel-de-un-ciclo-debe-pertenecer-al-grado-del-ciclo.desc.md
     Iniciativa: 2026-09-11_08-46_nivel-segun-grado-del-ciclo
     Test: T-006  |  Origen ESC: ESC-012
     Para regenerar: /sdd-create-tests-e2e (sobrescribe desde la fuente). -->

# T-006 — El nivel de un ciclo debe pertenecer al grado del ciclo

**Origen ESC:** ESC-012
**Verifica:** V-Ciclo-003, U-ciclos-001
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

## Resultado esperado

- El sistema no guarda el ciclo y muestra el mensaje "El nivel indicado no pertenece al grado del ciclo".
- Al abrir de nuevo el menú "Sistema educativo" → "Ciclos", el listado sigue mostrando la fila con el nombre "Certificado profesional avanzado de informática", sin el cambio que no se pudo guardar.
