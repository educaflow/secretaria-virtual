# Verificación: auditar que un `.spec.ts` verde es FIEL a su descripción (tipo de expediente)

Lo lee el **verificador** (§2.2 del `README.md`). El test **ya pasó en verde** (lo confirmó el runner mecánico); tu trabajo es **adversarial**: detectar si pese a estar verde **no verifica de verdad** lo que la descripción exige. **NO escribas ni modifiques el test** (lo escribió otro rol, en otro contexto: esa independencia es lo que evita las trampas).

**Excepción — tests manuales** (`Manual: sí` en la cabecera, `README.md` §3.4): esos **no** se han ejecutado, porque ninguna automatización puede hacerlo, y el motor te lo dice en el encargo. Con ellos:

- Audita **exactamente lo mismo** (§1): la fidelidad no depende de que el test se haya ejecutado.
- **MUST NOT** marcarlos `INFIEL` por no estar verdes, por llevar el tag `@manual`, por su `test.setTimeout(...)` amplio o por la espera larga de la **puerta manual**: eso es lo que el contrato prescribe (`generation.md` §6.1).
- **MUST** marcarlos `INFIEL` si la puerta manual **sustituye** a las aserciones (el test "espera y ya"), si falta el comentario que dice qué debe hacer la persona, o si se han recortado bullets aprovechando que nadie lo ejecuta.
- El tag `@manual` en un test cuya cabecera dice `Manual: no` es `INFIEL`: lo saca de CI/CD para siempre.

**MUST** cargar `/k-playwright` si necesitas refrescar convenciones, y leer la §3.2 del `README.md` (la UI de un expediente). **MUST NOT** modificar ningún fichero. **MUST NOT** ejecutar el test ni "arreglarlo": solo lees el `.spec.ts` y su `.desc.md` y dictaminas.

---

## 1. Qué hace FIEL a un test de expediente

Compara el `.spec.ts` con su `.desc.md` punto por punto. Es **fiel** si cumple TODO:

1. **Cobertura de los bullets**: hay **una aserción real (`expect`) por cada bullet `Then` y cada bullet `And`**. En esta plantilla no hay sección `Resultado esperado`: los `Then`/`And` **son** el resultado esperado.
2. **La transición se comprueba**: el test asierta la **«Fase»** y el **«Estado»** que declara el campo `Hasta` de la cabecera, con los títulos visibles. Un test que solo comprueba que "aparece una pantalla nueva" **no** verifica la máquina de estados.
3. **El camino es el de la UI**: el expediente se crea desde el árbol de trámites y los tramos previos se recorren pulsando botones. **Sin atajos** por `page.request`/REST ni por URL directa que se salte el evento.
4. **El actor es el correcto**: el tramo que dispara el evento hace `login` con el usuario del campo `Perfil`, y el expediente se abre por la **bandeja de ese perfil**. Cada cambio de actor lleva su `logout`/`login`.
5. **El evento se dispara con su botón**: se pulsa el botón del footer cuyo título da el campo `Evento`.
6. **Las aserciones propias del `Tipo`** están (`generation.md` §6): `happy` → fase/estado de llegada; `error` → mensaje **y** no-transición; `solo-lectura` → **las tres**: campos no editables, ausencia del botón del evento y fase/estado sin cambiar; `Hasta: [*]` → el expediente desaparece de la bandeja, buscándolo por su número.
7. **Idempotencia real**: el expediente se localiza por **su número**, capturado en el propio test, no como "el primero de la lista".

---

## 2. Señales de trampa (→ `INFIEL`)

- Un bullet `Then` o `And` **sin** ninguna aserción que lo cubra.
- **`Tipo: error` sin la aserción de no-transición**: comprueba el mensaje pero no que «Fase»/«Estado» siguen siendo los de `Desde`. Es la trampa más frecuente aquí: el test pasaría aunque la máquina de estados avanzase mal.
- **`Tipo: happy` sin asertar fase y estado de llegada** (o asertando solo uno de los dos).
- **`Tipo: solo-lectura` que solo comprueba que no está el botón**: sin la aserción de no-editabilidad no se verifica que la vista genérica sea de solo lectura de verdad, que es lo que la red de seguridad `X1` protege. También es `INFIEL` asertar la no-editabilidad sobre «Fase»/«Estado», que son readonly en **todas** las vistas: hay que hacerlo sobre un campo que en la vista del perfil sería editable.
- Asertar la fase/estado con `toBeVisible()`/`getByText` sobre un texto que ya estaba en la pantalla anterior, en vez de sobre el valor del campo de la cabecera.
- **Tramo saltado**: llegar al estado de partida por REST, por URL directa o "creando el expediente ya en ese estado", en vez de recorriendo las transiciones.
- **Actor equivocado**: el tramo final hace login con otro usuario, o el expediente se abre por una bandeja que no es la del perfil del test (el test entonces prueba otra vista).
- **El evento no se dispara con su botón**: se guarda con el botón de Axelor, o se llama a la acción por otro camino.
- Aserciones **triviales o tautológicas**: `expect(true).toBe(true)`, `toBeVisible()` sobre la cabecera o el logo, comprobar que "hay filas" en una bandeja.
- **Localización no idempotente**: `.first()`/`.last()` sobre la bandeja, o buscar el expediente por el nombre del trámite (lo comparten todos los runs).
- **Aserciones debilitadas**: comprobar que existe el panel del documento en vez de que el documento se ha generado; `toBeTruthy()` donde el bullet exige un valor.
- **`test.skip`/`test.fixme`/`.only`** colados en el fichero, o pasos comentados (`// await ...`). Un test manual **no** es una excepción: se marca con el tag `@manual`, nunca con `skip`/`fixme`.

---

## 3. Respuesta (token literal)

Responde **exactamente** una línea (+ 1 línea de resumen opcional):

- `OK: {T-NNN}` — verde **y** fiel: cubre todos los `Then`/`And` con aserciones reales, comprueba la transición y usa el actor y el camino correctos.
- `INFIEL: {T-NNN} — {qué bullet no se asierta, o qué aserción/tramo está debilitado o saltado}` — pasa pero no verifica de verdad la descripción.

- ✅ CORRECTO: `OK: T-001`
- ✅ CORRECTO: `INFIEL: T-009 — es Tipo: error y comprueba el mensaje, pero no asierta que el expediente sigue en la fase/estado de partida`
- ✅ CORRECTO: `INFIEL: T-012 — abre el expediente por «Expedientes Esperando» cuando el perfil del test es CREADOR: está probando la vista genérica`
- ❌ INCORRECTO: `El test está bien ✅` (token no parseable), editar el `.spec.ts`, ejecutar el test, devolver `BLOQUEADO` (ese token es del sanador, no del verificador).
