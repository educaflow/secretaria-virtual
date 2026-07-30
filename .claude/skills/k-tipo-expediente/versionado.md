# Versionado — duplicar un tipo para crear la versión siguiente

Receta para crear `v(N+1)` a partir de `vN` (verificada con la creación real de `justificacion_falta_profesorado/v2`). La versión nueva nace idéntica a la anterior y se modifica después; la anterior queda intacta para los expedientes existentes.

## 1. Por qué copiar la carpeta sin más NO funciona

1. **JPA**: el *entity name* (nombre simple de la clase) debe ser único en toda la unidad de persistencia; dos versiones no pueden compartir nombre de entidad (por eso la entidad lleva el sufijo `VN` y cada versión tiene su tabla).
2. **Enums**: todos los enums de dominio comparten el paquete `db`; sin el sufijo entidad+versión colisionan.
3. **Vistas**: los nombres de vista son un espacio global en Axelor; `exp-<Code>-...` necesita el `Code` versionado.

Consecuencia: la copia requiere una sustitución **mecánica** del sufijo de versión en varios ficheros. Es 100% determinista.

## 2. Receta

Con `<Code>` = code del trámite (p.ej. `JustificacionFaltaProfesorado`), desde `vN` hacia `v(N+1)`:

### 2.1 Copiar excluyendo los generados

```bash
cd src/main/java/com/educaflow/tramites/<tramite>
mkdir -p v2/documentospdf/originales
cp v1/TipoExpedienteInstance.xml v1/domains.xml v1/views.xml v1/EventManagerImpl.java v1/StateEventValidatorImpl.kt v1/estados.puml v2/
cp v1/documentospdf/*.xml v2/documentospdf/          # y los .pdf VERSIONADOS a mano si los hay
cp v1/documentospdf/originales/* v2/documentospdf/originales/
```

**MUST NOT** copiar: `i18n_es.csv`/`i18n_ca.csv` (los regenera el build en la carpeta nueva), `estados.png` (lo renderiza el build del `.puml`), ficheros de lock (`.~lock.*#`) ni cualquier otro resto temporal.

### 2.2 Sustituciones (en los ficheros copiados)

```bash
sed -i -e 's/<Code>V1/<Code>V2/g' \
       -e 's/<tramite>\.v1/<tramite>.v2/g' \
       -e 's|<tramite>/v1/|<tramite>/v2/|g' \
       v2/*.xml v2/*.java v2/*.kt v2/documentospdf/*.xml
```

| Sustitución | Dónde aparece |
|---|---|
| `<Code>V1` → `<Code>V2` | `domains.xml` (entity + **todos los enums** y sus `ref`), `EventManagerImpl.java` (entidad, repositorio, imports de enums), `StateEventValidatorImpl.kt` (imports), `views.xml` (form plantilla, `model`, `action-method` y su referencia en el botón), `documentospdf/*.xml` (**expresiones Groovy de los checks con FQCN de enum**) |
| paquete `.v1` → `.v2` | línea `package` de `.java` y `.kt` |
| ruta `/v1/` → `/v2/` | `<extra-code-model>` de `domains.xml` (se regeneraría igualmente, pero así queda coherente) |

`TipoExpedienteInstance.xml` y `estados.puml` normalmente no contienen nada versionado (se copian tal cual).

### 2.3 Ficheros EXTERNOS a la carpeta — fáciles de olvidar

1. **Permisos por `tipoExpedienteCode`**: toda asignación ligada al tipo (no al trámite) hay que duplicarla para `<Code>V2` (buscar `<Code>V1` en `data-demo/` y en los `data-init`). Las asignaciones por `tramiteCode` no necesitan cambio.
2. **`archunit_store`** (`src/test/resources/archunit_store/`): si el código copiado contiene violaciones congeladas (buscar el paquete `.v1` en el store), la copia introduce la misma violación con el paquete `.v2` — añade la línea homóloga (mismo número de línea si la copia es línea a línea) o, mejor, elimina el código problemático en ambas versiones.

### 2.4 Verificar y compilar

```bash
grep -rn "V1\|\.v1\|/v1/" v2/        # MUST devolver 0 resultados
./gradlew clean build                 # o ./run.sh
```

El build regenera para la carpeta nueva: `i18n_*.csv` (mismas traducciones automáticas; las correcciones manuales de la columna `message` de `vN` NO se propagan — cópialas a mano si las había), `estados.png`, `<extra-code-model>`, los PDF de los documentos y los data-init del tipo. El tipo nuevo aparece como "<name del trámite> V2".

### 2.5 Activar la versión nueva (cuando toque)

`<defaultTipoExpediente>v2</defaultTipoExpediente>` en el `TramiteInstance.xml` del trámite y recompilar. Hasta entonces la app sigue usando `vN` y `v(N+1)` existe pero inactiva. Los expedientes ya creados siguen siendo de su versión original.

## 3. Checklist

- [ ] ¿Copiados solo los fuentes (sin `i18n_*.csv`, `estados.png`, locks)?
- [ ] ¿`grep` de `V1`/`.v1`/`/v1/` en la carpeta nueva devuelve 0 resultados?
- [ ] ¿Duplicadas las asignaciones de permisos por `tipoExpedienteCode`?
- [ ] ¿Revisado `archunit_store` si el código copiado tenía violaciones congeladas?
- [ ] ¿`./gradlew clean build` en verde?
- [ ] ¿`defaultTipoExpediente` — decidido conscientemente si se activa ya o no?

## 4. Anti-patrones

- **MUST NOT** "reutilizar" el sufijo: la versión nueva **MUST** tener carpeta y sufijo propios; modificar `vN` en caliente rompe los expedientes existentes de esa versión.
- **MUST NOT** copiar los CSV de i18n de la versión anterior (regla de `CLAUDE.md`: nunca crearlos a mano; se regeneran).
- **MUST NOT** borrar la versión anterior mientras existan expedientes suyos en BD.
