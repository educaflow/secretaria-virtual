---
type: implementation-task
template: system
---

# Tarea 32 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

## Vista Mis-Sms.xml

Las decisiones difíciles, con sus alternativas, están en [`decisiones.md`](decisiones.md) (D1–D6); este documento las cita por su número.

## Ficheros a crear o modificar (extracto del diseño)

Rutas relativas a `src/main/java/com/educaflow/` salvo que empiecen por `src/`, `agent_docs/` o `.claude/`.

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/notificaciones/views/Mis-Sms.xml` | Crear | k-vistas (forms.md) | Form del SMS en «Recibidas» |

> **Nota para `/sdd-implementer`:** los XML de `domains/`, `views/` y `menus.xml` ya están materializados en la carpeta `design/`. **MUST NOT** modificarlos, reescribirlos ni regenerarlos: se **copian verbatim** a su ubicación final (`menus.xml` se fusiona en el `menus.xml` único del proyecto). El código Java es lo único que se implementa a partir de las firmas y comentarios del diseño.

**Materialización:** el fichero ya está materializado y validado en `design/views/Mis-Sms.xml`. Se **copia literalmente** (`cp`) a `src/main/java/com/educaflow/subsystem/notificaciones/views/Mis-Sms.xml` (creando la carpeta con `mkdir -p` si no existe), **sin regenerarlo, reescribirlo ni reformatearlo** (`implementation.md` §1). Acción: `Crear`.

## Paso del diseño

### Paso 10 — Vistas

Copiar `design/views/*.xml` a `subsystem/notificaciones/views/` (las vistas antiguas se borraron en el Paso 1).

**`Mis-Sms.xml`**:

```
  tttt.....fff   ← telefono(4, phone) + fechaEnvio(3, offset 5)
  mmmmmmmmmmmm   ← mensaje(12, Text)
  Botones: ..........ss   ← Salir(2, offset 10)
```

Verificación: `./gradlew -q test --tests 'com.educaflow.views.*'` tras el Paso 11 (con `VAR-7.2` ya ampliada).
