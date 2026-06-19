# ArchUnit reference

Detailed reference for writing ArchUnit tests. Load this when you need exact
method names, the JUnit wiring details, or a rule type not covered in the quick
examples. Targets ArchUnit **1.4.x**.

## Table of contents

1. Core types and the import step
2. The fluent rule DSL (`classes`, `noClasses`, `methods`, `fields`)
3. Predicates (`that(...)`)
4. Conditions (`should(...)`)
5. Combining and customizing rules
6. Library API: Architectures (layered / onion)
7. Library API: Slices and cycle detection
8. Library API: GeneralCodingRules
9. Library API: PlantUML
10. JUnit integration (4 and 5)
11. Freezing rules for legacy code
12. Configuration and properties
13. Common pitfalls

---

## 1. Core types and the import step

- **`ClassFileImporter`** — reads compiled `.class` bytecode and builds the
  model. Common entry points:
  - `importPackages("com.myapp")` / `importPackages("com.a", "com.b")`
  - `importPackagesOf(MyClass.class)`
  - `importClasspath()` — everything on the classpath (usually too broad)
  - `importPath(Paths.get(...))`
- **`JavaClasses`** — the collection of imported classes; the thing rules are
  checked against. Iterable; you rarely touch it directly when using JUnit.
- **`JavaClass` / `JavaMethod` / `JavaField` / `JavaConstructor`** — the model
  elements you can write predicates and conditions against.
- **`ArchRule`** — a rule that can be `check(JavaClasses)`-ed. Throws an
  `AssertionError` with a readable message listing every violation when it
  fails.

Use `ImportOption` to exclude things, most commonly test classes:

```java
JavaClasses classes = new ClassFileImporter()
    .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
    .importPackages("com.myapp");
```

With the JUnit integration, pass import options via the annotation:

```java
@AnalyzeClasses(
    packages = "com.myapp",
    importOptions = ImportOption.DoNotIncludeTests.class)
```

---

## 2. The fluent rule DSL

Static entry points (import statically from
`com.tngtech.archunit.lang.syntax.ArchRuleDefinition`):

| Entry point | Meaning |
|---|---|
| `classes()` | a positive rule: the matched classes **should** … |
| `noClasses()` | a negative rule: the matched classes should **never** … |
| `methods()` / `noMethods()` | same, at method granularity |
| `fields()` / `noFields()` | same, at field granularity |
| `constructors()` / `noConstructors()` | same, for constructors |
| `members()` / `noMembers()` | any member |
| `codeUnits()` | methods + constructors + static initializers |

Shape of a rule:

```
{classes|noClasses}()
    .that()<predicate>          // optional: narrows the subject set
    .should()<condition>        // required: what must (not) hold
    [.because("rationale")]     // optional: documents intent in the failure msg
    [.allowEmptyShould(true)]   // optional: don't fail if no class matches
```

`classes().should(...)` asserts the condition holds for all matched classes.
`noClasses().should(...)` asserts the condition holds for **none** of them.
Choosing the negative form often reads more naturally for "must not depend on".

---

## 3. Predicates — `that(...)`

Predicates select which elements the rule applies to. They chain with `.and()`
and `.or()`. The most used ones:

By location / name:
- `.resideInAPackage("..service..")`
- `.resideInAnyPackage("..service..", "..api..")`
- `.resideOutsideOfPackage("..internal..")`
- `.haveSimpleName("Foo")` / `.haveSimpleNameEndingWith("Service")`
- `.haveSimpleNameStartingWith("Abstract")` / `.haveSimpleNameContaining("Util")`
- `.haveNameMatching(".*Repository")`

By type characteristics:
- `.areInterfaces()` / `.areNotInterfaces()`
- `.areEnums()` / `.areRecords()` / `.areAnnotations()`
- `.areAssignableTo(SomeBase.class)` / `.areAssignableFrom(...)`
- `.implement(SomeInterface.class)`
- `.areAnnotatedWith(Service.class)` / `.areMetaAnnotatedWith(...)`
- `.arePublic()` / `.areNotPublic()` / `.haveModifier(JavaModifier.FINAL)`

For members specifically: `.areDeclaredInClassesThat()...`,
`.haveRawType(...)`, `.areStatic()`, etc.

Package matcher syntax:
- `..pkg..` — `pkg` and any sub-package, anywhere in the name.
- `com.app.*` — exactly one segment after `com.app`.
- `com.app..` — `com.app` and everything beneath it.

---

## 4. Conditions — `should(...)`

Conditions express the requirement. Negate with `should().not...` or use the
`noClasses()` form.

Dependency conditions (the workhorses):
- `.dependOnClassesThat().resideInAPackage("..db..")`
- `.onlyDependOnClassesThat().resideInAnyPackage("..service..", "java..")`
- `.accessClassesThat()...` / `.onlyBeAccessed().byAnyPackage(...)`
- `.onlyAccessClassesThat()...`
- `.callMethodWhere(...)` / `.accessField(...)`

Structural conditions:
- `.beAnnotatedWith(Service.class)`
- `.implement(SomeInterface.class)` / `.beAssignableTo(...)`
- `.haveSimpleNameEndingWith("Service")`
- `.resideInAPackage("..service..")`
- `.bePublic()` / `.bePackagePrivate()` / `.beFinal()`
- `.haveOnlyFinalFields()`
- `.beInterfaces()`

Each `should...That()` opens another predicate clause on the *target* classes,
so you can write fully directional rules (subject ↔ target).

---

## 5. Combining and customizing rules

- **Boolean composition on predicates**: `.and()`, `.or()`.
- **Rationale**: `.because("the persistence layer is an implementation detail")`
  — appended to failure messages.
- **Empty matches**: by default a rule whose subject set is empty *fails* (to
  catch typos in package names). Override with `.allowEmptyShould(true)` or the
  global property `archunit.fail.on.empty.should=false`.
- **Custom predicates/conditions**: implement `DescribedPredicate<JavaClass>` or
  `ArchCondition<JavaClass>` when the built-ins aren't enough, then plug them in
  via `.that(myPredicate)` / `.should(myCondition)`. ArchUnit navigates class
  hierarchies and (meta-)annotations reflectively, so custom meta-annotations
  that group several configurations are possible.

---

## 6. Library API — Architectures

Import from `com.tngtech.archunit.library.Architectures`.

### Layered architecture

```java
layeredArchitecture().consideringAllDependencies()
    .layer("Controller").definedBy("..controller..")
    .layer("Service").definedBy("..service..")
    .layer("Persistence").definedBy("..persistence..")

    .whereLayer("Controller").mayNotBeAccessedByAnyLayer()
    .whereLayer("Service").mayOnlyBeAccessedByLayers("Controller")
    .whereLayer("Persistence").mayOnlyBeAccessedByLayers("Service");
```

Tips: `consideringAllDependencies()` is the modern, strict default;
`consideringOnlyDependenciesInLayers()` restricts the analysis to the declared
layers. `optionalLayer(...)` allows a layer to be absent.

### Onion / hexagonal (ports and adapters)

```java
onionArchitecture()
    .domainModels("..domain.model..")
    .domainServices("..domain.service..")
    .applicationServices("..application..")
    .adapter("rest", "..adapter.rest..")
    .adapter("persistence", "..adapter.persistence..");
```

The onion rule enforces that the domain depends on nothing outward, application
depends only on domain, and adapters sit on the outside.

---

## 7. Library API — Slices and cycle detection

Import from `com.tngtech.archunit.library.dependencies.SlicesRuleDefinition`.

Slices group classes by a captured part of the package name (`$1` etc.). Two
canonical rules:

```java
// No cyclic dependencies between feature slices
SlicesRuleDefinition.slices()
    .matching("com.myapp.(*)..")
    .should().beFreeOfCycles();

// Slices must not depend on each other at all
SlicesRuleDefinition.slices()
    .matching("com.myapp.(*)..")
    .should().notDependOnEachOther();
```

This is the standard way to keep features/modules decoupled.

---

## 8. Library API — GeneralCodingRules

Import from `com.tngtech.archunit.library.GeneralCodingRules`. Ready-made rules
for common hygiene checks, e.g.:

- `NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS` — no `System.out` / `System.err`.
- `NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS`
- `NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING`
- `NO_CLASSES_SHOULD_USE_JODATIME`
- `NO_CLASSES_SHOULD_USE_FIELD_INJECTION` (catches `@Autowired` etc. on fields)

Declare them as `@ArchTest` fields directly.

---

## 9. Library API — PlantUML

ArchUnit can verify the codebase against a PlantUML component diagram, so the
diagram becomes the executable source of truth:

```java
URL diagram = getClass().getResource("/architecture.puml");

ArchRule rule = classes().should(adhereToPlantUmlDiagram(
        diagram, consideringAllDependencies()));
```

Useful when architects already maintain a diagram and want drift detection.

---

## 10. JUnit integration

Both JUnit 4 and JUnit 5 are supported with extended features beyond plain
usage: automatic caching of imported classes (via an internal `ClassCache`) and
removal of import/check boilerplate.

### Annotations

- `@AnalyzeClasses(packages = "...", packagesOf = X.class, importOptions = ...)`
  — declares what to import for the whole test class.
- `@ArchTest` — marks a rule to run. Applies to:
  - `static final ArchRule` fields, and
  - `static void method(JavaClasses classes)` methods (the runner/engine injects
    the cached classes).

### How each version wires up

- **JUnit 4**: driven by `ArchUnitRunner`. Add
  `@RunWith(ArchUnitRunner.class)`. It discovers `@ArchTest` members in
  `@AnalyzeClasses` classes and manages the shared class cache.
- **JUnit 5**: driven by a custom `TestEngine` (`ArchUnitTestEngine`) on the
  JUnit Platform — no runner annotation. The engine clears the cache after the
  test class finishes.

### Caching behavior

The class cache works two ways: it reuses classes across several rules in the
same test class, and across test classes that request the same import. On large
codebases this avoids re-scanning bytecode repeatedly and is the main reason to
use the integration over manual `ClassFileImporter` calls.

### Sharing rules across classes/modules

Group reusable rules in a holder class and pull them in with `ArchTests.in`:

```java
@ArchTest
static final ArchTests codingRules = ArchTests.in(CodingRulesLibrary.class);
```

This lets a shared module define rules once and every microservice depend on it.

---

## 11. Freezing rules for legacy code

`FreezingArchRule` records the *current* set of violations as an accepted
baseline (a "violation store") and then only fails the build on **new**
violations. Ideal for introducing ArchUnit into an existing codebase without a
red build on day one.

```java
@ArchTest
static final ArchRule frozen = FreezingArchRule.freeze(
    noClasses().that().resideInAPackage("..service..")
        .should().dependOnClassesThat().resideInAPackage("..controller.."));
```

The first run stores existing violations; subsequent runs fail only if new ones
appear. As violations are fixed, the store shrinks (it won't let removed
violations creep back). The default store is a text file under
`archunit_store`; the location and behavior are configurable.

---

## 12. Configuration and properties

ArchUnit reads `archunit.properties` from the classpath, and some settings can
be set as system properties (`-Darchunit....`). Useful ones:

- `archunit.junit.testFilter=ruleFieldName` (or system property
  `-Darchunit.junit.testFilter=...`) — run only specific `@ArchTest` rules.
- `archunit.fail.on.empty.should=false` — globally allow empty subject sets.
- `archunit.freeze.store.default.path=...` and related `archunit.freeze.*`
  keys — control the freezing violation store.
- Resolution of missing classes / how dependencies to non-imported types are
  handled can be tuned via `archunit.resolveMissingDependenciesFromClassPath`.

---

## 13. Common pitfalls

- **Empty matches pass silently?** They don't — by default an empty subject set
  *fails*. If a rule "passes" unexpectedly, check the package matcher first.
- **`..` vs `.`** — `..service..` matches sub-packages; `.service.` does not.
  Most "rule matches nothing" bugs are a missing `..`.
- **Test classes polluting results** — exclude them with
  `ImportOption.DoNotIncludeTests` unless you intend to test test code.
- **Forgetting `static`** — `@ArchTest` fields/methods must be `static`.
- **Leaving `@RunWith(ArchUnitRunner.class)` on a JUnit 5 test** — remove it;
  it's JUnit 4 only and breaks the JUnit 5 engine.
- **Mixed artifact versions** — keep every `com.tngtech.archunit:*` artifact on
  the same version.
