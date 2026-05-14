# Calidad de clases

## SOLID

### S — Responsabilidad única (SRP)

Una clase debe tener una única razón para cambiar. Si una clase hace A y B, y A puede cambiar por motivos distintos a B, son dos clases.

En este proyecto: los `*ServiceImpl` tienen una única responsabilidad (gestionar el ciclo de vida de una entidad). Si contienen un algoritmo complejo y cohesivo que nada tiene que ver con el ciclo de vida de esa entidad, extraerlo a una clase colaboradora.

### O — Abierto/Cerrado (OCP)

Una clase debe estar abierta para extensión pero cerrada para modificación. En la práctica: no añadir `if (tipo == X)` ni `switch (tipo)` en métodos existentes para manejar nuevos casos; usar polimorfismo o composición.

Si hay un `switch` sobre el tipo de una entidad para ejecutar lógica diferente, valorar si ese tipo debería ser una jerarquía con el comportamiento en cada subclase.

### L — Sustitución de Liskov (LSP)

Una implementación debe poder sustituir a su interfaz sin alterar el comportamiento esperado. Los métodos sobreescritos no deben reducir las garantías de los métodos que sobreescriben ni lanzar excepciones que la interfaz no declara.

### I — Segregación de interfaces (ISP)

Una interfaz no debe tener métodos que los clientes no necesitan. Si una interfaz tiene muchos métodos y la mayoría de clientes solo usan unos pocos, dividirla.

En este proyecto: las interfaces `*Service` solo declaran los métodos que las capas superiores (controladores, otros servicios) realmente necesitan. Los métodos internos de implementación van en `*ServiceImpl` como privados.

### D — Inversión de dependencias (DIP)

Las clases de alto nivel no deben depender de implementaciones concretas, sino de abstracciones (interfaces). En este proyecto lo gestiona Guice: los servicios siempre se inyectan como interfaz (`@Inject MiEntidadService service`), nunca como `MiEntidadServiceImpl`. El binding lo hace `ModelServiceFactory` por convención o un `AxelorModule` cuando es necesario.

---

## Clases colaboradoras

Si un servicio tiene un método que describe un algoritmo cohesivo y autocontenido (más de ~10 líneas de descripción funcional, o que opera sobre un subconjunto de datos bien delimitado), valorar si merece una clase colaboradora propia.

**Criterio:** si el algoritmo es claramente autocontenido y podría reutilizarse en otro contexto, sí merece clase colaboradora. Si es lógica puntual y específica del servicio, no.

**Cuando aplica:** mover el algoritmo a una nueva clase y que el servicio original delegue en ella. Aplicar también R-13 (ver "Utilidades sin estado") si la clase colaboradora no necesita inyección.

---

## Coherencia entre interfaz e implementación

Las firmas de los métodos declarados en la interfaz deben coincidir exactamente en nombre, parámetros y tipo de retorno con los métodos `@Override` de la implementación. Cualquier divergencia es un error.

**Corrección:** alinear las firmas divergentes. Si hay duda sobre cuál es la correcta, decidirlo según la descripción funcional del método y aplicarlo en ambos sitios.

---

## DTOs y records

Un record o DTO se justifica cuando:
- Agrupa **≥ 3 campos** de fuentes distintas que viajan juntos por varias capas.
- Desacopla la capa de servicio de la capa de presentación (evita exponer la entidad JPA directamente).
- Es el contrato de retorno de una operación compleja con múltiples valores de naturaleza heterogénea.

**Violación:** un DTO que solo agrupa 1–2 campos simples del mismo tipo y se usa en un único punto de llamada. En ese caso los parámetros se pasan directamente al método receptor.

**Corrección:** eliminar el DTO y sustituir su uso por los parámetros individuales en la firma del método.

---

## Utilidades sin estado

Una clase cuyas operaciones cumplen **todas** estas condiciones no debe ser un servicio inyectable (`ModelService`, `@Inject`, constructor con repositorio):

1. No accede a repositorios JPA ni a otros servicios inyectados.
2. No tiene estado (no tiene campos de instancia mutables).
3. Sus métodos solo operan sobre sus parámetros de entrada y devuelven un resultado.

**Violación:** el diseño prescribe un servicio inyectable para una clase que solo hace transformaciones sobre sus argumentos (parseo, validación de formato, construcción de strings, cálculos…).

**Correcto:** convertir la clase en una utilidad con métodos `static`, siguiendo el patrón de `DniUtil`, `XmlUtil`, `TextUtil` del paquete `base/util/`. Eliminar la interfaz si existe. Sustituir las referencias `@Inject` por llamadas estáticas en las clases que la usaban.