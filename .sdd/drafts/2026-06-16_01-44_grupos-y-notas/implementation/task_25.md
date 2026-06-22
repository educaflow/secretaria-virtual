---
type: implementation-task
---

# Tarea 25 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-archunit

Genera el código de los tests de arquitectura descritos en `design/test-arch-desc.md` en su totalidad.

- La descripción es el contrato: implementa EXACTAMENTE los tests que describe (las reglas seleccionadas del catálogo `k-archunit`: C4, C5, C8, C9, C10, C11, C12, C13, C14, C15, C16, C17, C18, C21, C22, con su ámbito y resultado esperado). **MUST NOT** inventar reglas que la descripción no liste ni omitir ninguna. **MUST NOT** redefinir con otro criterio una regla que el catálogo `k-archunit` ya define; usa la del catálogo (`secretaria-virtual-rules.md`, reglas `C1`–`C22`).
- Clase de test: `ArquitecturaGruposNotasTest`.
- `@AnalyzeClasses`: `com.educaflow.system.gruposnotas` (`importOptions = DoNotIncludeTests`). El ámbito se acota al paquete del propio sistema, no a todo `com.educaflow`. `PAQUETES_EXENTOS`: no aplica.
- Ubicación de salida: `src/test/java/com/educaflow/.../architecture/ArquitecturaGruposNotasTest.java`, según las convenciones de `k-archunit`.
- Stack: ArchUnit 1.4.2 + JUnit 5.
- Las clases de producción y los XML ya están en el árbol (las tareas previas las materializaron): los tests verifican el paquete `com.educaflow.system.gruposnotas`. Si una clase que la descripción cita no existe, **detente y reporta** (BLOCKED).

Reglas a materializar (de `design/test-arch-desc.md`):
- **C4** `systemNoDependeDeSecretariaVirtual`; **C5** `secretariaVirtualNoEsAccedidaPorNadie`; **C8** `sistemasIndependientesEntreSi`.
- **C9** `controladorNoAccedeARepositorio`; **C10** `controladorNoUsaJpaRepository`.
- **C11** `repositorioNoDependeDeServicioNiControlador`; **C12** `servicioNoDependeDeControlador`; **C13** `entidadesDominioSonPojos`.
- **C14** `noBeansGetEnControladorNiServiceImpl`; **C15** `controladoresNombreYUbicacion` (C15a + C15b); **C16** `implServicioNombreYUbicacion`; **C17** `interfazServicioNombreYUbicacion`; **C18** `repositoriosNombre`.
- **C21** `modelServiceNoSeInyecta` (con `allowEmptyShould(true)`); **C22** `noStreamsEstandar`.

Reglas del catálogo **no aplicables** (no materializar): C1, C2, C3, C6, C7, C19, C20 — justificación en `design/test-arch-desc.md` §"Reglas del catálogo no aplicables". Reglas en FREEZE: ninguna.
