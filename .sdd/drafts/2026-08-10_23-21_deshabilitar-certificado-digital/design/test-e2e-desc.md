# Tests E2E

Tests concretos end-to-end materializados a partir de los escenarios (`ESC-NNN`) de las historias de usuario del `specification.md` y de las V/R/U del diseño.

Cada test es **independiente** (no depende del estado dejado por otro) y **trazable** (declara qué `ESC-NNN` materializa y qué V/R/U verifica). `/sdd-debug-with-test-e2e-desc` lo ejecuta contra la aplicación real tras la implementación (bucle de auto-corrección).

---

## Estado inicial de la base de datos

Estado previo (datos maestros gestionados por otros subsistemas) del que parten **todos** los tests. Ningún test puede presuponer más estado que este; cada test lo referencia en sus `Precondiciones`.

- El certificado de ejemplo que ya viene dentro de la aplicación: ruta classpath `firma/mi_certificado.p12`, contraseña `nadanada` (recurso del WAR, no un dato de BD).
- No existe ninguna entrada de certificado digital con el DNI «85432016B». Como todos los tests usan ese mismo DNI y algunos dejan la entrada creada al terminar, cada test restablece esta precondición al empezar: si el listado «Certificados digitales» muestra una fila con el DNI «85432016B», el administrador la abre, pulsa «Borrar» y confirma, antes de ejecutar sus pasos.

**Usuarios de acceso** (login y contraseña que `/sdd-debug-with-test-e2e-desc` usará para iniciar sesión):

| Login | Contraseña | Rol / Tipo | Centro |
|---|---|---|---|
| admin | admin | Administrador | — |

---

## T-001 — Alta con «Habilitado» marcado por defecto

**Origen ESC:** ESC-001
**Verifica:** U-certificados-digitales-001
**Pantalla principal:** screen-certificados-digitales.md
**Tipo:** UI

### Precondiciones
- El usuario `admin` ha iniciado sesión.
- No existe ninguna entrada con el DNI «85432016B» (si existe de una ejecución anterior, se borra desde el listado como describe el «Estado inicial de la base de datos»).

### Pasos
1. **Dado** que el administrador está en la pantalla «Certificados digitales» (menú «Administración SV» → «Certificados digitales»).
2. **Cuando** pulsa «Añadir certificado digital».
3. **Entonces** el formulario de alta muestra la casilla «Habilitado» marcada.
4. **Cuando** rellena el campo «DNI» con «85432016B».
5. **Y** elige en «Tipo de certificado» la opción «Usar un fichero con el certificado que ya está dentro del del WAR».
6. **Y** rellena el campo «Ruta classpath» con «firma/mi_certificado.p12» y el campo «Contraseña» con «nadanada».
7. **Y** pulsa «Guardar».

### Resultado esperado
- El sistema guarda la entrada y vuelve al listado «Certificados digitales».
- El listado muestra la fila del DNI «85432016B» con la columna «Habilitado» marcada.

---

## T-002 — Deshabilitar una entrada

**Origen ESC:** ESC-002
**Verifica:** —
**Pantalla principal:** screen-certificados-digitales.md
**Tipo:** happy

### Precondiciones
- El usuario `admin` ha iniciado sesión.
- No existe ninguna entrada con el DNI «85432016B» (si existe de una ejecución anterior, se borra desde el listado como describe el «Estado inicial de la base de datos»).

### Pasos
1. **Dado** que el administrador está en la pantalla «Certificados digitales» (menú «Administración SV» → «Certificados digitales»).
2. **Cuando** pulsa «Añadir certificado digital», rellena «DNI» con «85432016B», elige en «Tipo de certificado» la opción «Usar un fichero con el certificado que ya está dentro del del WAR», rellena «Ruta classpath» con «firma/mi_certificado.p12» y «Contraseña» con «nadanada», y pulsa «Guardar».
3. **Entonces** el sistema guarda la entrada y vuelve al listado.
4. **Cuando** abre la fila del DNI «85432016B».
5. **Y** desmarca la casilla «Habilitado».
6. **Y** pulsa «Guardar».

### Resultado esperado
- El sistema guarda el cambio y vuelve al listado «Certificados digitales».
- El listado muestra la fila del DNI «85432016B» con la columna «Habilitado» sin marcar.

---

## T-003 — Modificar una entrada sin tocar «Habilitado» la mantiene habilitada

**Origen ESC:** ESC-003
**Verifica:** —
**Pantalla principal:** screen-certificados-digitales.md
**Tipo:** happy

### Precondiciones
- El usuario `admin` ha iniciado sesión.
- No existe ninguna entrada con el DNI «85432016B» (si existe de una ejecución anterior, se borra desde el listado como describe el «Estado inicial de la base de datos»).

### Pasos
1. **Dado** que el administrador está en la pantalla «Certificados digitales» (menú «Administración SV» → «Certificados digitales»).
2. **Cuando** pulsa «Añadir certificado digital», rellena «DNI» con «85432016B», elige en «Tipo de certificado» la opción «Usar un fichero con el certificado que ya está dentro del del WAR», rellena «Ruta classpath» con «firma/mi_certificado.p12» y «Contraseña» con «nadanada», y pulsa «Guardar».
3. **Entonces** el sistema guarda la entrada y vuelve al listado.
4. **Cuando** abre la fila del DNI «85432016B».
5. **Y** cambia el campo «Contraseña» a «otraclave» sin tocar la casilla «Habilitado».
6. **Y** pulsa «Guardar».

### Resultado esperado
- El sistema guarda el cambio y vuelve al listado «Certificados digitales».
- El listado muestra la fila del DNI «85432016B» con la columna «Habilitado» marcada.

---

## T-004 — Alta con «Habilitado» desmarcado

**Origen ESC:** ESC-004
**Verifica:** —
**Pantalla principal:** screen-certificados-digitales.md
**Tipo:** happy

### Precondiciones
- El usuario `admin` ha iniciado sesión.
- No existe ninguna entrada con el DNI «85432016B» (si existe de una ejecución anterior, se borra desde el listado como describe el «Estado inicial de la base de datos»).

### Pasos
1. **Dado** que el administrador está en la pantalla «Certificados digitales» (menú «Administración SV» → «Certificados digitales»).
2. **Cuando** pulsa «Añadir certificado digital».
3. **Y** rellena «DNI» con «85432016B», elige en «Tipo de certificado» la opción «Usar un fichero con el certificado que ya está dentro del del WAR», rellena «Ruta classpath» con «firma/mi_certificado.p12» y «Contraseña» con «nadanada».
4. **Y** desmarca la casilla «Habilitado».
5. **Y** pulsa «Guardar».

### Resultado esperado
- El sistema guarda la entrada y vuelve al listado «Certificados digitales».
- El listado muestra la fila del DNI «85432016B» con la columna «Habilitado» sin marcar.

---

## T-005 — Volver a habilitar una entrada deshabilitada

**Origen ESC:** ESC-005
**Verifica:** —
**Pantalla principal:** screen-certificados-digitales.md
**Tipo:** happy

### Precondiciones
- El usuario `admin` ha iniciado sesión.
- No existe ninguna entrada con el DNI «85432016B» (si existe de una ejecución anterior, se borra desde el listado como describe el «Estado inicial de la base de datos»).

### Pasos
1. **Dado** que el administrador está en la pantalla «Certificados digitales» (menú «Administración SV» → «Certificados digitales»).
2. **Cuando** pulsa «Añadir certificado digital», rellena «DNI» con «85432016B», elige en «Tipo de certificado» la opción «Usar un fichero con el certificado que ya está dentro del del WAR», rellena «Ruta classpath» con «firma/mi_certificado.p12» y «Contraseña» con «nadanada», y pulsa «Guardar».
3. **Entonces** el sistema guarda la entrada y vuelve al listado.
4. **Cuando** abre la fila del DNI «85432016B», desmarca la casilla «Habilitado» y pulsa «Guardar».
5. **Entonces** el sistema guarda el cambio y vuelve al listado.
6. **Cuando** abre de nuevo la fila del DNI «85432016B».
7. **Entonces** el formulario muestra la casilla «Habilitado» sin marcar.
8. **Cuando** marca la casilla «Habilitado».
9. **Y** pulsa «Guardar».

### Resultado esperado
- El sistema guarda el cambio y vuelve al listado «Certificados digitales».
- El listado muestra la fila del DNI «85432016B» con la columna «Habilitado» marcada.

---

## T-006 — Borrar una entrada deshabilitada sigue funcionando

**Origen ESC:** ESC-006
**Verifica:** —
**Pantalla principal:** screen-certificados-digitales.md
**Tipo:** happy

### Precondiciones
- El usuario `admin` ha iniciado sesión.
- No existe ninguna entrada con el DNI «85432016B» (si existe de una ejecución anterior, se borra desde el listado como describe el «Estado inicial de la base de datos»).

### Pasos
1. **Dado** que el administrador está en la pantalla «Certificados digitales» (menú «Administración SV» → «Certificados digitales»).
2. **Cuando** pulsa «Añadir certificado digital», rellena «DNI» con «85432016B», elige en «Tipo de certificado» la opción «Usar un fichero con el certificado que ya está dentro del del WAR», rellena «Ruta classpath» con «firma/mi_certificado.p12» y «Contraseña» con «nadanada», y pulsa «Guardar».
3. **Entonces** el sistema guarda la entrada y vuelve al listado.
4. **Cuando** abre la fila del DNI «85432016B», desmarca la casilla «Habilitado» y pulsa «Guardar».
5. **Entonces** el sistema guarda el cambio y vuelve al listado.
6. **Cuando** abre la fila del DNI «85432016B».
7. **Y** pulsa «Borrar».
8. **Entonces** el sistema pide confirmar el borrado.
9. **Cuando** el administrador confirma.

### Resultado esperado
- El sistema borra la entrada y vuelve al listado «Certificados digitales».
- El listado ya no muestra ninguna fila con el DNI «85432016B».
