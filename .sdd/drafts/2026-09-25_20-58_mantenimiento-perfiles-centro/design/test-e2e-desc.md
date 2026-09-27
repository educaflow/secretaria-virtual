# Tests E2E

Tests concretos end-to-end materializados a partir de los escenarios (`ESC-NNN`) de las historias de usuario del `specification.md` y de las V/R/U del diseño.

Cada test es **independiente** (no depende del estado dejado por otro) y **trazable** (declara qué `ESC-NNN` materializa y qué V/R/U verifica). `/sdd-debug-with-test-e2e-desc` lo ejecuta contra la aplicación real tras la implementación (bucle de auto-corrección).

---

## Estado inicial de la base de datos

Estado previo (datos maestros gestionados por otros subsistemas y datos de demo) del que parten **todos** los tests. Ningún test puede presuponer más estado que este; cada test lo referencia en sus `Precondiciones`.

- Centros «CIPFP Mislata» y «CIPFP Batoi».
- Tipos de usuario del catálogo, entre ellos «Profesor», «Alumno», «Administrativo» y «Supervisor del centro».
- Cargos del catálogo, entre ellos «Director», «Secretario» y «Jefe de estudios».
- Trámites «Trámite de prueba», «Justificación de falta del profesorado» y «Anulación de matrícula en ciclo formativo».
- Usuarios de demo: «Supervisor1 CIPFP Mislata» es supervisor **solo** de «CIPFP Mislata»; «Director CIPFP Mislata» tiene el cargo «Director» en «CIPFP Mislata» y no es supervisor; «Profesor1 CIPFP Mislata» es profesor de «CIPFP Mislata»; «Profesor1 CIPFP Batoi» es profesor de «CIPFP Batoi».
- «Profesor1 CIPFP Mislata» no tiene ningún perfil que le permita crear el trámite «Anulación de matrícula en ciclo formativo» en «CIPFP Mislata».
- La tabla de perfiles de trámites por centro está **vacía**.

**Usuarios de acceso** (login y contraseña que `/sdd-debug-with-test-e2e-desc` usará para iniciar sesión):

| Login | Contraseña | Rol / Tipo | Centro |
|---|---|---|---|
| admin | admin | Administrador | — (todos) |
| supervisor1@mislata.es | demo1234 | Supervisor del centro | CIPFP Mislata |
| director@mislata.es | demo1234 | Profesor con cargo Director | CIPFP Mislata |
| profesor1@mislata.es | demo1234 | Profesor | CIPFP Mislata |

---

## T-001 — El supervisor da un perfil a un cargo

**Origen ESC:** ESC-001
**Verifica:** V-AceProfileCentro-008, U-perfiles-tramites-mi-centro-001
**Pantalla principal:** screen-perfiles-tramites-mi-centro.md
**Tipo:** happy

### Precondiciones
- El estado descrito en «Estado inicial de la base de datos».

### Pasos
1. **Dado** que el supervisor inicia sesión con usuario «supervisor1@mislata.es» y contraseña «demo1234».
2. **Cuando** abre «Mi centro → Perfiles de trámites» y pulsa «Nuevo».
3. **Y** elige el centro «CIPFP Mislata», el trámite «Trámite de prueba», el perfil «Tramitador» y el cargo «Jefe de estudios», sin rellenar tipo de usuario ni usuario.
4. **Y** pulsa «Guardar».
5. **Entonces** vuelve al listado.

### Resultado esperado
- El listado muestra una fila con centro «CIPFP Mislata», trámite «Trámite de prueba», perfil «Tramitador» y cargo «Jefe de estudios», con el tipo de usuario y el usuario vacíos.

---

## T-002 — El supervisor da un perfil a un usuario concreto

**Origen ESC:** ESC-002
**Verifica:** V-AceProfileCentro-006, U-perfiles-tramites-mi-centro-003
**Pantalla principal:** screen-perfiles-tramites-mi-centro.md
**Tipo:** happy

### Precondiciones
- El estado descrito en «Estado inicial de la base de datos».

### Pasos
1. **Dado** que el supervisor inicia sesión con usuario «supervisor1@mislata.es» y contraseña «demo1234».
2. **Cuando** abre «Mi centro → Perfiles de trámites» y pulsa «Nuevo».
3. **Y** elige el centro «CIPFP Mislata», el trámite «Justificación de falta del profesorado», el perfil «Colaborador» y el usuario «Profesor1 CIPFP Mislata», sin rellenar tipo de usuario ni cargo.
4. **Y** pulsa «Guardar».
5. **Entonces** vuelve al listado.

### Resultado esperado
- El listado muestra una fila con centro «CIPFP Mislata», trámite «Justificación de falta del profesorado», perfil «Colaborador» y usuario «Profesor1 CIPFP Mislata», con el tipo de usuario y el cargo vacíos.

---

## T-003 — El supervisor cambia el perfil de una fila y el centro no se puede cambiar

**Origen ESC:** ESC-003
**Verifica:** V-AceProfileCentro-008, U-perfiles-tramites-mi-centro-002
**Pantalla principal:** screen-perfiles-tramites-mi-centro.md
**Tipo:** happy

### Precondiciones
- El estado descrito en «Estado inicial de la base de datos».

### Pasos
1. **Dado** que el supervisor inicia sesión con usuario «supervisor1@mislata.es» y contraseña «demo1234».
2. **Cuando** abre «Mi centro → Perfiles de trámites» y pulsa «Nuevo».
3. **Y** elige el centro «CIPFP Mislata», el trámite «Trámite de prueba», el perfil «Creador» y el tipo de usuario «Alumno», sin rellenar cargo ni usuario.
4. **Y** pulsa «Guardar» y vuelve al listado.
5. **Y** abre la fila con centro «CIPFP Mislata», trámite «Trámite de prueba», perfil «Creador» y tipo de usuario «Alumno».
6. **Entonces** el formulario muestra el centro «CIPFP Mislata» sin posibilidad de cambiarlo.
7. **Cuando** cambia el perfil a «Afectado».
8. **Y** pulsa «Guardar» y vuelve al listado.

### Resultado esperado
- El listado muestra la fila del centro «CIPFP Mislata» con trámite «Trámite de prueba», perfil «Afectado» y tipo de usuario «Alumno».
- El listado ya no muestra ninguna fila con perfil «Creador» y tipo de usuario «Alumno».

---

## T-004 — El supervisor borra una fila

**Origen ESC:** ESC-004
**Verifica:** V-AceProfileCentro-008
**Pantalla principal:** screen-perfiles-tramites-mi-centro.md
**Tipo:** happy

### Precondiciones
- El estado descrito en «Estado inicial de la base de datos».

### Pasos
1. **Dado** que el supervisor inicia sesión con usuario «supervisor1@mislata.es» y contraseña «demo1234».
2. **Cuando** abre «Mi centro → Perfiles de trámites» y pulsa «Nuevo».
3. **Y** elige el centro «CIPFP Mislata», el trámite «Anulación de matrícula en ciclo formativo», el perfil «Auditor» y el tipo de usuario «Administrativo», sin rellenar cargo ni usuario.
4. **Y** pulsa «Guardar» y vuelve al listado.
5. **Y** abre la fila con centro «CIPFP Mislata», trámite «Anulación de matrícula en ciclo formativo», perfil «Auditor» y tipo de usuario «Administrativo».
6. **Y** pulsa «Borrar».
7. **Y** confirma el borrado.

### Resultado esperado
- El listado ya no muestra ninguna fila con trámite «Anulación de matrícula en ciclo formativo», perfil «Auditor» y tipo de usuario «Administrativo».

---

## T-005 — El supervisor solo ve y usa sus centros

**Origen ESC:** ESC-005
**Verifica:** U-perfiles-tramites-mi-centro-001
**Pantalla principal:** screen-perfiles-tramites-mi-centro.md
**Tipo:** UI

### Precondiciones
- El estado descrito en «Estado inicial de la base de datos».

### Pasos
1. **Dado** que el administrador inicia sesión con usuario «admin» y contraseña «admin».
2. **Cuando** abre «Administración → Perfiles de trámites por centro» y pulsa «Nuevo».
3. **Y** elige el centro «CIPFP Batoi», el trámite «Trámite de prueba», el perfil «Director» y el cargo «Director», sin rellenar tipo de usuario ni usuario.
4. **Y** pulsa «Guardar».
5. **Y** el administrador cierra sesión.
6. **Y** el supervisor inicia sesión con usuario «supervisor1@mislata.es» y contraseña «demo1234».
7. **Y** abre «Mi centro → Perfiles de trámites».
8. **Entonces** el listado no muestra ninguna fila del centro «CIPFP Batoi».
9. **Cuando** pulsa «Nuevo» y despliega el selector de centro.

### Resultado esperado
- El selector de centro ofrece «CIPFP Mislata» y no ofrece «CIPFP Batoi».

---

## T-006 — Otros usuarios del centro no ven las pantallas de perfiles

**Origen ESC:** ESC-020
**Verifica:** —
**Pantalla principal:** screen-perfiles-tramites-mi-centro.md
**Tipo:** UI

### Precondiciones
- El estado descrito en «Estado inicial de la base de datos».

### Pasos
1. **Dado** que el director inicia sesión con usuario «director@mislata.es» y contraseña «demo1234».
2. **Cuando** despliega el menú.

### Resultado esperado
- El menú no muestra la opción «Mi centro → Perfiles de trámites».
- El menú no muestra la opción «Administración → Perfiles de trámites por centro».

---

## T-007 — El supervisor no ve la pantalla del administrador

**Origen ESC:** ESC-021
**Verifica:** —
**Pantalla principal:** screen-perfiles-tramites-todos-centros.md
**Tipo:** UI

### Precondiciones
- El estado descrito en «Estado inicial de la base de datos».

### Pasos
1. **Dado** que el supervisor inicia sesión con usuario «supervisor1@mislata.es» y contraseña «demo1234».
2. **Cuando** despliega el menú.

### Resultado esperado
- El menú muestra la opción «Mi centro → Perfiles de trámites».
- El menú no muestra la opción «Administración → Perfiles de trámites por centro».

---

## T-008 — El perfil concedido por el supervisor surte efecto

**Origen ESC:** ESC-022
**Verifica:** —
**Pantalla principal:** screen-perfiles-tramites-mi-centro.md
**Tipo:** happy

### Precondiciones
- El estado descrito en «Estado inicial de la base de datos».

### Pasos
1. **Dado** que el profesor inicia sesión con usuario «profesor1@mislata.es» y contraseña «demo1234».
2. **Cuando** abre «Mis trámites → Nuevo trámite» y elige el centro «CIPFP Mislata».
3. **Entonces** el sistema no ofrece el trámite «Anulación de matrícula en ciclo formativo».
4. **Cuando** el profesor cierra sesión.
5. **Y** el supervisor inicia sesión con usuario «supervisor1@mislata.es» y contraseña «demo1234».
6. **Y** abre «Mi centro → Perfiles de trámites» y pulsa «Nuevo».
7. **Y** elige el centro «CIPFP Mislata», el trámite «Anulación de matrícula en ciclo formativo», el perfil «Creador» y el usuario «Profesor1 CIPFP Mislata», sin rellenar tipo de usuario ni cargo.
8. **Y** pulsa «Guardar» y vuelve al listado.
9. **Entonces** el listado muestra una fila con centro «CIPFP Mislata», trámite «Anulación de matrícula en ciclo formativo», perfil «Creador» y usuario «Profesor1 CIPFP Mislata».
10. **Cuando** el supervisor cierra sesión.
11. **Y** el profesor inicia sesión con usuario «profesor1@mislata.es» y contraseña «demo1234».
12. **Y** abre «Mis trámites → Nuevo trámite» y elige el centro «CIPFP Mislata».

### Resultado esperado
- El sistema ofrece el trámite «Anulación de matrícula en ciclo formativo».

---

## T-009 — No se guarda una fila sin destinatario

**Origen ESC:** ESC-006
**Verifica:** V-AceProfileCentro-004
**Pantalla principal:** screen-perfiles-tramites-mi-centro.md
**Tipo:** error

### Precondiciones
- El estado descrito en «Estado inicial de la base de datos».

### Pasos
1. **Dado** que el supervisor inicia sesión con usuario «supervisor1@mislata.es» y contraseña «demo1234».
2. **Cuando** abre «Mi centro → Perfiles de trámites» y pulsa «Nuevo».
3. **Y** elige el centro «CIPFP Mislata», el trámite «Trámite de prueba» y el perfil «Tramitador», sin rellenar tipo de usuario, cargo ni usuario.
4. **Y** pulsa «Guardar».

### Resultado esperado
- El sistema muestra el mensaje «Indica a quién se da el perfil: un tipo de usuario, un cargo o un usuario» y no guarda la fila.

---

## T-010 — No se guarda una fila con más de un destinatario

**Origen ESC:** ESC-007
**Verifica:** V-AceProfileCentro-005
**Pantalla principal:** screen-perfiles-tramites-mi-centro.md
**Tipo:** error

### Precondiciones
- El estado descrito en «Estado inicial de la base de datos».

### Pasos
1. **Dado** que el supervisor inicia sesión con usuario «supervisor1@mislata.es» y contraseña «demo1234».
2. **Cuando** abre «Mi centro → Perfiles de trámites» y pulsa «Nuevo».
3. **Y** elige el centro «CIPFP Mislata», el trámite «Trámite de prueba», el perfil «Tramitador», el tipo de usuario «Profesor» y el cargo «Jefe de estudios».
4. **Y** pulsa «Guardar».

### Resultado esperado
- El sistema muestra el mensaje «Indica solo uno: un tipo de usuario, un cargo o un usuario» y no guarda la fila.

---

## T-011 — El selector de usuario solo ofrece usuarios del centro de la fila

**Origen ESC:** ESC-008
**Verifica:** U-perfiles-tramites-mi-centro-003
**Pantalla principal:** screen-perfiles-tramites-mi-centro.md
**Tipo:** UI

### Precondiciones
- El estado descrito en «Estado inicial de la base de datos».

### Pasos
1. **Dado** que el supervisor inicia sesión con usuario «supervisor1@mislata.es» y contraseña «demo1234».
2. **Cuando** abre «Mi centro → Perfiles de trámites» y pulsa «Nuevo».
3. **Y** elige el centro «CIPFP Mislata».
4. **Y** despliega el selector de usuario y escribe «Profesor1» en la búsqueda.

### Resultado esperado
- El selector ofrece «Profesor1 CIPFP Mislata» y no ofrece «Profesor1 CIPFP Batoi».

---

## T-012 — No se guarda una asignación repetida

**Origen ESC:** ESC-009
**Verifica:** V-AceProfileCentro-007
**Pantalla principal:** screen-perfiles-tramites-mi-centro.md
**Tipo:** error

### Precondiciones
- El estado descrito en «Estado inicial de la base de datos».

### Pasos
1. **Dado** que el supervisor inicia sesión con usuario «supervisor1@mislata.es» y contraseña «demo1234».
2. **Cuando** abre «Mi centro → Perfiles de trámites» y pulsa «Nuevo».
3. **Y** elige el centro «CIPFP Mislata», el trámite «Trámite de prueba», el perfil «Creador» y el tipo de usuario «Profesor», sin rellenar cargo ni usuario.
4. **Y** pulsa «Guardar».
5. **Y** vuelve al listado y pulsa «Nuevo».
6. **Y** elige exactamente los mismos valores: centro «CIPFP Mislata», trámite «Trámite de prueba», perfil «Creador» y tipo de usuario «Profesor», sin rellenar cargo ni usuario.
7. **Y** pulsa «Guardar».
8. **Entonces** el sistema muestra el mensaje «Ya existe esa asignación de perfil» y no guarda la segunda fila.
9. **Cuando** pulsa «Cancelar» y vuelve al listado.

### Resultado esperado
- El listado muestra una sola fila con centro «CIPFP Mislata», trámite «Trámite de prueba», perfil «Creador» y tipo de usuario «Profesor».

---

## T-013 — No se guarda una fila sin trámite

**Origen ESC:** ESC-016
**Verifica:** V-AceProfileCentro-002, U-perfiles-tramites-mi-centro-004
**Pantalla principal:** screen-perfiles-tramites-mi-centro.md
**Tipo:** error

### Precondiciones
- El estado descrito en «Estado inicial de la base de datos».

### Pasos
1. **Dado** que el supervisor inicia sesión con usuario «supervisor1@mislata.es» y contraseña «demo1234».
2. **Cuando** abre «Mi centro → Perfiles de trámites» y pulsa «Nuevo».
3. **Y** elige el centro «CIPFP Mislata», el perfil «Tramitador» y el cargo «Jefe de estudios», sin elegir trámite.
4. **Y** pulsa «Guardar».

### Resultado esperado
- El sistema muestra el mensaje «El trámite es obligatorio» y no guarda la fila.

---

## T-014 — No se guarda una fila sin perfil

**Origen ESC:** ESC-017
**Verifica:** V-AceProfileCentro-003, U-perfiles-tramites-mi-centro-004
**Pantalla principal:** screen-perfiles-tramites-mi-centro.md
**Tipo:** error

### Precondiciones
- El estado descrito en «Estado inicial de la base de datos».

### Pasos
1. **Dado** que el supervisor inicia sesión con usuario «supervisor1@mislata.es» y contraseña «demo1234».
2. **Cuando** abre «Mi centro → Perfiles de trámites» y pulsa «Nuevo».
3. **Y** elige el centro «CIPFP Mislata», el trámite «Trámite de prueba» y el cargo «Jefe de estudios», sin elegir perfil.
4. **Y** pulsa «Guardar».

### Resultado esperado
- El sistema muestra el mensaje «El perfil es obligatorio» y no guarda la fila.

---

## T-015 — El centro sale prellenado y, si se vacía, no se guarda

**Origen ESC:** ESC-018
**Verifica:** U-perfiles-tramites-mi-centro-005, V-AceProfileCentro-001, U-perfiles-tramites-mi-centro-004
**Pantalla principal:** screen-perfiles-tramites-mi-centro.md
**Tipo:** error

### Precondiciones
- El estado descrito en «Estado inicial de la base de datos».

### Pasos
1. **Dado** que el supervisor inicia sesión con usuario «supervisor1@mislata.es» y contraseña «demo1234».
2. **Cuando** abre «Mi centro → Perfiles de trámites» y pulsa «Nuevo».
3. **Entonces** el formulario muestra el centro ya rellenado con «CIPFP Mislata».
4. **Cuando** vacía el centro y elige el trámite «Trámite de prueba», el perfil «Tramitador» y el cargo «Jefe de estudios».
5. **Y** pulsa «Guardar».

### Resultado esperado
- El sistema muestra el mensaje «El centro es obligatorio» y no guarda la fila.

---

## T-016 — No se guarda una modificación que repite una asignación

**Origen ESC:** ESC-019
**Verifica:** V-AceProfileCentro-007
**Pantalla principal:** screen-perfiles-tramites-mi-centro.md
**Tipo:** error

### Precondiciones
- El estado descrito en «Estado inicial de la base de datos».

### Pasos
1. **Dado** que el supervisor inicia sesión con usuario «supervisor1@mislata.es» y contraseña «demo1234».
2. **Cuando** abre «Mi centro → Perfiles de trámites» y pulsa «Nuevo».
3. **Y** elige el centro «CIPFP Mislata», el trámite «Trámite de prueba», el perfil «Creador» y el tipo de usuario «Alumno», y pulsa «Guardar».
4. **Y** vuelve al listado y pulsa «Nuevo».
5. **Y** elige el centro «CIPFP Mislata», el trámite «Trámite de prueba», el perfil «Afectado» y el tipo de usuario «Alumno», sin rellenar cargo ni usuario.
6. **Y** pulsa «Guardar».
7. **Y** vuelve al listado y abre la fila con perfil «Afectado» y tipo de usuario «Alumno».
8. **Y** cambia el perfil a «Creador» y pulsa «Guardar».
9. **Entonces** el sistema muestra el mensaje «Ya existe esa asignación de perfil» y no guarda el cambio.
10. **Cuando** pulsa «Cancelar» y vuelve al listado.

### Resultado esperado
- El listado muestra la fila con perfil «Afectado» y tipo de usuario «Alumno».
- El listado muestra una sola fila con perfil «Creador» y tipo de usuario «Alumno».

---

## T-017 — El administrador da de alta perfiles en varios centros

**Origen ESC:** ESC-010
**Verifica:** —
**Pantalla principal:** screen-perfiles-tramites-todos-centros.md
**Tipo:** happy

### Precondiciones
- El estado descrito en «Estado inicial de la base de datos».

### Pasos
1. **Dado** que el administrador inicia sesión con usuario «admin» y contraseña «admin».
2. **Cuando** abre «Administración → Perfiles de trámites por centro» y pulsa «Nuevo».
3. **Y** elige el centro «CIPFP Batoi», el trámite «Trámite de prueba», el perfil «Secretario» y el cargo «Secretario», sin rellenar tipo de usuario ni usuario.
4. **Y** pulsa «Guardar».
5. **Y** pulsa «Nuevo».
6. **Y** elige el centro «CIPFP Mislata», el trámite «Trámite de prueba», el perfil «Secretario» y el cargo «Secretario», sin rellenar tipo de usuario ni usuario.
7. **Y** pulsa «Guardar».
8. **Y** vuelve al listado.

### Resultado esperado
- El listado muestra dos filas con trámite «Trámite de prueba», perfil «Secretario» y cargo «Secretario»: una del centro «CIPFP Batoi» y otra del centro «CIPFP Mislata».

---

## T-018 — El administrador modifica una fila y el centro no se puede cambiar

**Origen ESC:** ESC-011
**Verifica:** U-perfiles-tramites-todos-centros-001
**Pantalla principal:** screen-perfiles-tramites-todos-centros.md
**Tipo:** happy

### Precondiciones
- El estado descrito en «Estado inicial de la base de datos».

### Pasos
1. **Dado** que el administrador inicia sesión con usuario «admin» y contraseña «admin».
2. **Cuando** abre «Administración → Perfiles de trámites por centro» y pulsa «Nuevo».
3. **Y** elige el centro «CIPFP Batoi», el trámite «Trámite de prueba», el perfil «Creador» y el tipo de usuario «Profesor», sin rellenar cargo ni usuario.
4. **Y** pulsa «Guardar».
5. **Y** vuelve al listado y abre la fila recién creada.
6. **Entonces** el formulario muestra el centro «CIPFP Batoi» sin posibilidad de cambiarlo.
7. **Cuando** cambia el perfil a «Colaborador» y pulsa «Guardar».
8. **Y** vuelve al listado.

### Resultado esperado
- La fila del centro «CIPFP Batoi» muestra trámite «Trámite de prueba», perfil «Colaborador» y tipo de usuario «Profesor».

---

## T-019 — El administrador borra una fila

**Origen ESC:** ESC-012
**Verifica:** —
**Pantalla principal:** screen-perfiles-tramites-todos-centros.md
**Tipo:** happy

### Precondiciones
- El estado descrito en «Estado inicial de la base de datos».

### Pasos
1. **Dado** que el administrador inicia sesión con usuario «admin» y contraseña «admin».
2. **Cuando** abre «Administración → Perfiles de trámites por centro» y pulsa «Nuevo».
3. **Y** elige el centro «CIPFP Batoi», el trámite «Anulación de matrícula en ciclo formativo», el perfil «Auditor» y el cargo «Secretario», y pulsa «Guardar».
4. **Y** vuelve al listado y abre la fila del centro «CIPFP Batoi» con trámite «Anulación de matrícula en ciclo formativo», perfil «Auditor» y cargo «Secretario».
5. **Y** pulsa «Borrar».
6. **Y** confirma el borrado cuando el sistema pide confirmación.

### Resultado esperado
- El listado ya no muestra ninguna fila del centro «CIPFP Batoi» con trámite «Anulación de matrícula en ciclo formativo», perfil «Auditor» y cargo «Secretario».

---

## T-020 — El selector de usuario del administrador solo ofrece usuarios del centro de la fila

**Origen ESC:** ESC-013
**Verifica:** U-perfiles-tramites-todos-centros-002
**Pantalla principal:** screen-perfiles-tramites-todos-centros.md
**Tipo:** UI

### Precondiciones
- El estado descrito en «Estado inicial de la base de datos».

### Pasos
1. **Dado** que el administrador inicia sesión con usuario «admin» y contraseña «admin».
2. **Cuando** abre «Administración → Perfiles de trámites por centro» y pulsa «Nuevo».
3. **Y** elige el centro «CIPFP Batoi».
4. **Y** despliega el selector de usuario buscando «Profesor1».

### Resultado esperado
- El selector ofrece «Profesor1 CIPFP Batoi» y no ofrece «Profesor1 CIPFP Mislata».

---

## T-021 — El administrador filtra el listado por centro

**Origen ESC:** ESC-014
**Verifica:** —
**Pantalla principal:** screen-perfiles-tramites-todos-centros.md
**Tipo:** UI

### Precondiciones
- El estado descrito en «Estado inicial de la base de datos».

### Pasos
1. **Dado** que el administrador inicia sesión con usuario «admin» y contraseña «admin».
2. **Cuando** abre «Administración → Perfiles de trámites por centro» y pulsa «Nuevo».
3. **Y** elige el centro «CIPFP Batoi», el trámite «Trámite de prueba», el perfil «Tramitador» y el cargo «Jefe de estudios», y pulsa «Guardar».
4. **Y** pulsa «Nuevo», elige el centro «CIPFP Mislata», el trámite «Trámite de prueba», el perfil «Tramitador» y el cargo «Jefe de estudios», y pulsa «Guardar».
5. **Y** vuelve al listado.
6. **Y** en la búsqueda del listado filtra por centro escribiendo «CIPFP Batoi» y aplica el filtro.

### Resultado esperado
- El listado muestra la fila del centro «CIPFP Batoi» con trámite «Trámite de prueba», perfil «Tramitador» y cargo «Jefe de estudios».
- El listado no muestra ninguna fila del centro «CIPFP Mislata».

---

## T-022 — No se guarda una fila cuyo usuario no pertenece al centro

**Origen ESC:** ESC-015
**Verifica:** V-AceProfileCentro-006
**Pantalla principal:** screen-perfiles-tramites-todos-centros.md
**Tipo:** error

### Precondiciones
- El estado descrito en «Estado inicial de la base de datos».

### Pasos
1. **Dado** que el administrador inicia sesión con usuario «admin» y contraseña «admin».
2. **Cuando** abre «Administración → Perfiles de trámites por centro» y pulsa «Nuevo».
3. **Y** elige el centro «CIPFP Mislata», el trámite «Trámite de prueba», el perfil «Colaborador» y el usuario «Profesor1 CIPFP Mislata».
4. **Y** cambia el centro a «CIPFP Batoi» sin cambiar el usuario.
5. **Y** pulsa «Guardar».

### Resultado esperado
- El sistema muestra el mensaje «El usuario no pertenece al centro» y no guarda la fila.
