---
name: k-archunit
description: >-
  Write, configure and troubleshoot architecture tests for Java projects using
  ArchUnit. Use this skill whenever the user mentions ArchUnit, architecture
  tests, architectural fitness functions, enforcing package/layer dependencies,
  detecting cyclic dependencies, layered/onion/hexagonal architecture checks, or
  naming/annotation conventions in Java — even if they don't say "ArchUnit"
  explicitly but describe wanting to assert that "the controller layer must not
  access the repository layer" or similar rules. Also use it when the user asks
  how to integrate ArchUnit with JUnit 4 or JUnit 5, how to add the Maven/Gradle
  dependency, how to cache imported classes, or how to freeze existing
  violations. Covers the core API, the Library API (Architectures, Slices,
  GeneralCodingRules, PlantUML) and the JUnit test integration.
---

# k-archunit

ArchUnit is a free, extensible Java library for testing the architecture of a
codebase. It imports compiled bytecode into an in-memory model of classes and
their relationships, then lets you express architectural rules as plain unit
tests that run inside your normal test framework (JUnit, TestNG, etc.). A rule
either passes or fails like any other test, so architecture becomes part of CI
instead of a wiki page nobody reads.

Use this skill to help the user install ArchUnit, write rules, integrate them
with JUnit, and reach for the right built-in helpers. The current stable version
at the time of writing is **1.4.2**.

## When to reach for the bundled files

This SKILL.md gives you the workflow and the essentials. Two companion files
hold the detail — load them as needed instead of guessing:

- **`reference.md`** — the full API surface: the fluent DSL (`classes()`,
  `noClasses()`, `methods()`, `fields()`), predicates and conditions, the
  Library API (layered/onion architectures, slices/cycle detection,
  `GeneralCodingRules`, PlantUML), the JUnit integration internals, freezing
  rules, and configuration options. Read this when you need exact method names,
  the difference between JUnit 4 and 5 wiring, or a rule the examples don't
  cover.
- **`examples.md`** — copy-pasteable, working test classes for the common
  scenarios (layered architecture, package dependencies, naming and annotation
  conventions, cycle detection, onion architecture, generic coding rules, and
  sharing rules across modules). Read this when the user wants a concrete
  starting point.

## Installation

ArchUnit publishes separate Maven/Gradle artifacts depending on whether you want
the JUnit integration. Pick **one**:

**JUnit 5** (recommended for new projects) — the convenience artifact pulls in
both the API and the test engine with the correct scope:

```xml
<dependency>
    <groupId>com.tngtech.archunit</groupId>
    <artifactId>archunit-junit5</artifactId>
    <version>1.4.2</version>
    <scope>test</scope>
</dependency>
```

**JUnit 4**:

```xml
<dependency>
    <groupId>com.tngtech.archunit</groupId>
    <artifactId>archunit-junit4</artifactId>
    <version>1.4.2</version>
    <scope>test</scope>
</dependency>
```

**Core only** (any test framework, manual import/check):

```xml
<dependency>
    <groupId>com.tngtech.archunit</groupId>
    <artifactId>archunit</artifactId>
    <version>1.4.2</version>
    <scope>test</scope>
</dependency>
```

Gradle equivalent for JUnit 5:

```groovy
testImplementation 'com.tngtech.archunit:archunit-junit5:1.4.2'
```

Always keep the version consistent across every ArchUnit artifact in the build.

## The mental model

1. **Import** the classes you want to analyze into a `JavaClasses` object.
2. **Define** an `ArchRule` using the fluent DSL.
3. **Check** the rule against the imported classes. With the JUnit integration,
   steps 1 and 3 happen automatically.

Without any framework integration it looks like this:

```java
JavaClasses importedClasses = new ClassFileImporter().importPackages("com.myapp");

ArchRule rule = classes()
    .that().resideInAPackage("..service..")
    .should().onlyBeAccessed().byAnyPackage("..controller..", "..service..");

rule.check(importedClasses);
```

The `..` in package matchers means "this package and any sub-package". A single
`*` matches one package segment.

## Recommended workflow when helping the user

1. **Confirm the JUnit version** (4 vs 5) and build tool — it determines the
   dependency and whether a `@RunWith` runner is needed. If unknown, default to
   JUnit 5.
2. **Identify the rule category** they want: layer dependencies, package access,
   naming, annotations, cycles, or a generic coding rule. Map it to the right
   helper (see `reference.md` → Library API) rather than hand-rolling a rule when
   a built-in exists.
3. **Write the test** using the JUnit integration so they get caching and no
   boilerplate (see below and `examples.md`).
4. **For legacy codebases**, suggest *freezing* existing violations so the build
   stays green while preventing new violations (see `reference.md` → Freezing).

## JUnit integration in one screen

ArchUnit works with any framework, but the JUnit support adds two real benefits:
automatic **caching** of imported classes between rules/tests (a big speedup on
large projects), and removal of the manual `importPackages(...)` /
`rule.check(...)` boilerplate.

You annotate a class with `@AnalyzeClasses` to declare what to import, and mark
each rule with `@ArchTest`. Rules are declared as `static final ArchRule` fields
(or static methods taking a `JavaClasses` argument).

**JUnit 5** — the engine is picked up transparently, no runner needed:

```java
@AnalyzeClasses(packages = "com.myapp")
class ArchitectureTest {

    @ArchTest
    static final ArchRule services_should_not_access_controllers =
        noClasses()
            .that().resideInAPackage("..service..")
            .should().dependOnClassesThat().resideInAPackage("..controller..");
}
```

**JUnit 4** — identical, but add the runner:

```java
@RunWith(ArchUnitRunner.class)   // ONLY for JUnit 4
@AnalyzeClasses(packages = "com.myapp")
public class ArchitectureTest {

    @ArchTest
    public static final ArchRule rule = noClasses()
        .that().resideInAPackage("..service..")
        .should().dependOnClassesThat().resideInAPackage("..controller..");
}
```

The single difference between the two is that `@RunWith(ArchUnitRunner.class)`
line. For everything else, see `reference.md` and `examples.md`.
