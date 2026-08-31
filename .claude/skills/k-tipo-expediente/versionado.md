# Versionado — duplicar un tipo para crear la versión siguiente

Receta para crear `v(N+1)` a partir de `vN`.
La versión nueva nace idéntica a la anterior y se modifica después; la anterior queda intacta para los expedientes existentes.

**CRITICAL**: la carpeta de la versión nueva **NO** tiene por qué ser hermana de la anterior.
Los trámites pueden vivir en subcarpetas y las versiones también: nada en el generador obliga a que `v(N+1)` cuelgue de la misma carpeta que `vN`.
La carpeta de versión es simplemente la que contiene un `TipoExpedienteInstance.xml`, y se busca con `Files.walk` **a cualquier profundidad** bajo el trámite; lo único que se exige es que su **nombre** sea único bajo ese trámite (`/k-tramite` §4, test T1).
Son disposiciones igual de válidas, por ejemplo, `mi_tramite/v1` → `mi_tramite/v2` (hermanas y colgando del trámite) y `agrupacion/mi_tramite/actual/v1` → `agrupacion/mi_tramite/futuro/v2` (ni hermanas ni con el mismo segmento intermedio).
Por eso la receta parametriza **rutas completas** de origen y destino, nunca `v1`/`v2` sueltos: **MUST** mirar dónde está de verdad la carpeta de `vN` antes de copiarla, en vez de dar por hecha ninguna de las dos formas.

## 1. Por qué copiar la carpeta sin más NO funciona

1. **JPA**: el *entity name* (nombre simple de la clase) debe ser único en toda la unidad de persistencia; dos versiones no pueden compartir nombre de entidad (por eso la entidad lleva el sufijo `VN` y cada versión tiene su tabla).
2. **Enums**: todos los enums de dominio comparten el paquete `db`; sin el sufijo entidad+versión colisionan.
3. **Vistas**: los nombres de vista son un espacio global en Axelor; `exp-<Code>-...` necesita el `Code` versionado.

Consecuencia: la copia requiere una sustitución **mecánica** del sufijo de versión en varios ficheros. Es 100% determinista.

## 2. Receta

Con `<Code>` = code del trámite (en los ejemplos, `MiTramite`), desde `vN` hacia `v(N+1)`:

### 2.1 Copiar excluyendo los generados

La carpeta a copiar incluye ahora **una subcarpeta por fase**, así que se copia el árbol entero salvo lo generado:

```bash
# Rutas de las carpetas de versión, relativas a src/main/java/com/educaflow/tramites,
# tal cual están (o van a estar) en el árbol. Ajusta ambas al caso concreto.
ORIGEN=agrupacion/mi_tramite/actual/v1
DESTINO=agrupacion/mi_tramite/futuro/v2

cd src/main/java/com/educaflow/tramites
mkdir -p "$(dirname "$DESTINO")"
cp -r "$ORIGEN" "$DESTINO"
find "$DESTINO" -name 'i18n_*.csv' -delete   # también los de las carpetas de fase
rm -f "$DESTINO/estados.png"
```

Ejemplos de cómo se fijan `ORIGEN` y `DESTINO`:

- ✅ Versiones hermanas, sin segmento intermedio: `ORIGEN=mi_tramite/v1`, `DESTINO=mi_tramite/v2`.
- ✅ Versiones en subcarpetas distintas: `ORIGEN=agrupacion/mi_tramite/actual/v1`, `DESTINO=agrupacion/mi_tramite/futuro/v2`.
- ❌ `cd <tramite> && cp -r v1 v2`: da por hecho que la carpeta de versión cuelga directamente del trámite y que la nueva será hermana de la vieja. Puede ser cierto o no; nada obliga a que lo sea, y si no lo es el `cp` copia otra cosa o falla.

**MUST NOT** dejar copiados: `i18n_es.csv`/`i18n_ca.csv` (los regenera el build en cada carpeta nueva, **incluidas las de fase**), `estados.png` (lo renderiza el build del `.puml`), ficheros de lock (`.~lock.*#`) ni cualquier otro resto temporal.

`States.java` **no** se copia porque no está en la carpeta: es generado y vive en `build/src-gen-states/`.

### 2.2 Sustituciones (en los ficheros copiados)

```bash
PKG_ORIGEN=$(echo "$ORIGEN"  | tr '/' '.')
PKG_DESTINO=$(echo "$DESTINO" | tr '/' '.')

sed -i -e "s/<Code>V1/<Code>V2/g" \
       -e "s|$PKG_ORIGEN|$PKG_DESTINO|g" \
       -e "s|/$ORIGEN/|/$DESTINO/|g" \
       $(find "$DESTINO" -name '*.xml' -o -name '*.java' -o -name '*.kt')
```

| Sustitución | Dónde aparece |
|---|---|
| `<Code>V1` → `<Code>V2` | `domains.xml` (entity + **todos los enums** y sus `ref`), el `InitialEventManagerImpl.java` de la raíz (entidad, tanto en el `implements InitialEventManager<…>` como en el parámetro), el `PhaseEventManagerImpl.java` **de cada fase** (entidad, repositorio, imports de enums), el `StateEventValidatorImpl.kt` **de cada fase** (imports), el `views.xml` de la raíz (form plantilla, `model`, grids/forms de hijos) y los de cada fase (`action-method` y su referencia en el botón), `documentospdf/*.xml` (**expresiones Groovy de los checks con FQCN de enum**) |
| paquete `$PKG_ORIGEN` → `$PKG_DESTINO` | línea `package` de cada `.java` y `.kt` (incluido el `InitialEventManagerImpl.java` de la raíz), y el **`import <…>.States`** de cada `PhaseEventManagerImpl` |
| ruta `/$ORIGEN/` → `/$DESTINO/` | `<extra-code-model>` de `domains.xml` (se regeneraría igualmente, pero así queda coherente) |

`TipoExpedienteInstance.xml` y `estados.puml` normalmente no contienen nada versionado (se copian tal cual). Los nombres de **fase** y de **estado** tampoco cambian entre versiones: lo que versiona el nombre de la vista es el `<Code>`, no la fase.

### 2.2.b Copiar una fase suelta a otro tipo de expediente

Para eso están las fases. La subcarpeta de una fase es autocontenida salvo por dos cosas, que hay que llevarse a mano al destino:

1. Los **paneles** que sus `<include-panels>` referencian, que viven en el form plantilla del `views.xml` de la raíz.
2. Los **campos** del `domains.xml` que usan sus validaciones y sus vistas.

Después: añadir la `<fase>` con sus `<state>` al `TipoExpedienteInstance.xml` del destino, y sustituir el `<Code>` y el paquete igual que en §2.2.

### 2.3 Ficheros EXTERNOS a la carpeta — fáciles de olvidar

1. **Permisos por `tipoExpedienteCode`**: toda asignación ligada al tipo (no al trámite) hay que duplicarla para `<Code>V2` (buscar `<Code>V1` en `data-demo/` y en los `data-init`). Las asignaciones por `tramiteCode` no necesitan cambio.
2. **`TareaFirma` pendientes**: si el tipo pone documentos a firmar en el portafirmas, sus filas guardan el **FQCN** del notifier (`fqcnFirmaNotifier`), que apunta a una clase de la carpeta que estás copiando o moviendo. Eso **no** se autocorrige como el `basePackageName` (`phaseeventmanager.md` §6.6): crear una versión nueva no rompe nada —las filas viejas siguen apuntando a la clase vieja, que sigue existiendo—, pero **mover o renombrar** la carpeta de una versión que tenga firmas en marcha sí. Compruébalo antes.
3. **`archunit_store`** (`src/test/resources/archunit_store/`): si el código copiado contiene violaciones congeladas (buscar el paquete `.v1` en el store), la copia introduce la misma violación con el paquete `.v2` — añade la línea homóloga (mismo número de línea si la copia es línea a línea) o, mejor, elimina el código problemático en ambas versiones.

### 2.4 Verificar y compilar

```bash
grep -rn "V1\|\.v1\|/v1/" "$DESTINO"   # MUST devolver 0 resultados
./gradlew clean build                   # o ./run.sh
```

**MUST** conservarse `V1` en el patrón del `grep`: caza el `<Code>` sin actualizar, que al ser los nombres de vista un espacio global generaría las vistas de la versión nueva con el nombre de las viejas y las **pisaría**.
El build también lo detecta desde que el viewprocessor contrasta el `<Code>` del form de plantillas con el del `TipoExpedienteInstance.xml`, pero solo si alguna fase tiene formularios de estado; el `grep` cubre también los tipos que no los tienen.

El build regenera para las carpetas nuevas (la raíz y **cada fase**): `i18n_*.csv` (mismas traducciones automáticas; las correcciones manuales de la columna `message` de `vN` NO se propagan — cópialas a mano si las había), `estados.png`, `<extra-code-model>`, los PDF de los documentos y los data-init del tipo. El tipo nuevo aparece como "<name del trámite> V2".

### 2.5 Activar la versión nueva (cuando toque)

`<defaultTipoExpediente>v2</defaultTipoExpediente>` en el `TramiteInstance.xml` del trámite y recompilar. Hasta entonces la app sigue usando `vN` y `v(N+1)` existe pero inactiva. Los expedientes ya creados siguen siendo de su versión original.

## 3. Checklist

- [ ] ¿`ORIGEN` y `DESTINO` fijados con la ruta completa, sin dar por hecho que son carpetas hermanas?
- [ ] ¿Copiados solo los fuentes (sin `i18n_*.csv` — tampoco los de las carpetas de fase —, `estados.png`, locks)?
- [ ] ¿Está la subcarpeta de **todas** las fases, con sus tres ficheros?
- [ ] ¿`grep` de `V1`/`.v1`/`/v1/` en la carpeta nueva devuelve 0 resultados?
- [ ] ¿Duplicadas las asignaciones de permisos por `tipoExpedienteCode`?
- [ ] ¿Revisado `archunit_store` si el código copiado tenía violaciones congeladas?
- [ ] ¿`./gradlew clean build` en verde?
- [ ] ¿`defaultTipoExpediente` — decidido conscientemente si se activa ya o no?

## 4. Anti-patrones

- **MUST NOT** dejar el `import <…>.v1.States` sin actualizar en un `PhaseEventManagerImpl` de la versión nueva: **compila** (las dos versiones tienen una clase llamada `States`) pero `updateState` reventaría en runtime con "no es del tipo de expediente …", que es justo la barrera que `ExpedienteUtil.updateState` pone para atajarlo. El `grep` de §2.4 lo caza.
- **MUST NOT** "reutilizar" el sufijo: la versión nueva **MUST** tener carpeta y sufijo propios; modificar `vN` en caliente rompe los expedientes existentes de esa versión.
- **MUST NOT** copiar los CSV de i18n de la versión anterior (regla de `CLAUDE.md`: nunca crearlos a mano; se regeneran).
- **MUST NOT** borrar la versión anterior mientras existan expedientes suyos en BD.
- **MUST NOT** asumir que `v(N+1)` es hermana de `vN` ni que comparte segmento intermedio: `sed` no da error cuando un patrón no casa nunca, así que un `cp` corregido a mano más el `sed` original deja la copia con los paquetes de la versión anterior **y compila**; el `grep` de §2.4 es lo que lo caza.
