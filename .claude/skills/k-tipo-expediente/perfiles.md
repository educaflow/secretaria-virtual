# Perfiles: `CREADOR`, `TRAMITADOR` y `presentadoEnPapel`

Cómo se elige el perfil con el que nace un expediente y qué significa `presentadoEnPapel`.
  Complementa `SKILL.md` §2.2 (el atributo `profile` de un `<state>`) y `vistas.md` §2 (el `profile` de un `<form>`).

---

## 1. Solo dos perfiles son especiales

De todo el enum `Profile` (`subsystem/expedientes/domains/Profile.xml`), **solo `CREADOR` y `TRAMITADOR` tienen significado propio** en el alta de un expediente: son las dos formas de presentarlo.

Los demás (`COLABORADOR`, `AFECTADO`, `SECRETARIO`, `DIRECTOR`, `AUDITOR`…) **no son especiales**: son etiquetas corrientes que dan el turno en un estado y eligen qué vista se pinta, y nada más.

- **MUST NOT** darles trato particular en el código de un tipo de expediente.
- **MUST NOT** usarlos para decidir si el expediente se presenta en papel: no intervienen en el alta.
- Un estado puede declarar cualquiera de ellos en su `profile` con total normalidad.

---

## 2. La equivalencia de la que sale todo

```text
presentadoEnPapel == true   ⟺  actúa el TRAMITADOR  (le entregaron la solicitud en papel y la registra)
presentadoEnPapel == false  ⟺  actúa el CREADOR     (presenta el expediente telemáticamente)
```

Las dos caras son el mismo hecho, así que el `profile` del `ContextoTramitacion` **MUST** ir siempre en sintonía con su `presentadoEnPapel`.

Dónde lee cada cosa el tipo de expediente:

| Dónde | Cómo se consulta |
|---|---|
| `InitialEventManagerImpl` (el alta) | `expediente.getPresentadoEnPapel()` — `Tramitador` lo copia del `ContextoTramitacion` al expediente antes de llamarlo. **`InitialEventContext` NO tiene `getProfile()`** |
| `PhaseEventManagerImpl` (ya tramitando) | `eventContext.getProfile()`, el perfil del actor que dispara el evento (`phaseeventmanager.md`) |

---

## 3. Cuándo se le pregunta al usuario

`presentadoEnPapel` es **solo un desempate**: hace falta únicamente cuando el usuario tiene los dos perfiles sobre el trámite en ese centro, porque entonces puede actuar de las dos formas y no hay manera de deducir cuál.

| Perfiles del usuario sobre el trámite | ¿Se le pregunta? | `presentadoEnPapel` | Actúa como |
|---|---|---|---|
| Solo `CREADOR` | no | `false` (deducido) | `CREADOR` |
| Solo `TRAMITADOR` | no | `true` (deducido) | `TRAMITADOR` |
| `CREADOR` + `TRAMITADOR` | **sí** | lo elige él | `true` → `TRAMITADOR` / `false` → `CREADOR` |
| Ninguno de los dos | no | — | no puede crear el expediente |

- **MUST NOT** pedir ni mirar `presentadoEnPapel` cuando el usuario tiene un solo perfil: su valor ya está determinado por ese perfil.
- Quien no tiene ninguno de los dos no da de alta expedientes del trámite, y el trámite ni se le ofrece.

Lo resuelve `ContextoTramitacionService` y no hay que reimplementarlo en un tipo de expediente.

---

## 4. Qué implica al escribir el tipo

1. **El estado inicial puede depender de cómo se presentó**: un tipo que admita las dos formas arranca en un estado distinto según `presentadoEnPapel` — en papel se suele empezar adjuntando la solicitud escaneada y después copiar sus datos, mientras que telemáticamente se empieza por la entrada de datos.
2. Cada estado por el que pueda pasar el `TRAMITADOR` **MUST** tener su vista, igual que los del `CREADOR` (`vistas.md` §2).
3. `presentadoEnRepresentacion` es **ortogonal** a todo esto: dice para quién es el expediente (`personaSolicitante` ≠ `personaInteresada`), no cómo se presentó, y sale del flag `permitidoPresentarEnRepresentacion` del trámite.

- ✅ CORRECTO: `if (expediente.getPresentadoEnPapel()==true) { initialEventContext.updateState(...); }` (en el evento inicial)
- ❌ INCORRECTO: `initialEventContext.getProfile()` (no existe; en el alta se lee del expediente, §2)
- ❌ INCORRECTO: tratar `AFECTADO` o `COLABORADOR` como si decidieran algo del alta (no son especiales, §1)
