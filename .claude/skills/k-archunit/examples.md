# ArchUnit examples

Working, copy-pasteable test classes for common scenarios. All examples use the
**JUnit 5** integration (drop `@RunWith(ArchUnitRunner.class)` on top of the
class to convert any of them to JUnit 4). Adjust the package strings to match the
target project.

Assume these static imports unless noted otherwise:

```java
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.Architectures.onionArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import com.tngtech.archunit.lang.ArchRule;
```

---

## 1. Layered architecture (controller → service → persistence)

The most common starting point: enforce that each layer only talks to the
layers it's allowed to.

```java
@AnalyzeClasses(packages = "com.myapp")
class LayeredArchitectureTest {

    @ArchTest
    static final ArchRule layers_are_respected = layeredArchitecture()
        .consideringAllDependencies()
        .layer("Controller").definedBy("..controller..")
        .layer("Service").definedBy("..service..")
        .layer("Persistence").definedBy("..persistence..")

        .whereLayer("Controller").mayNotBeAccessedByAnyLayer()
        .whereLayer("Service").mayOnlyBeAccessedByLayers("Controller")
        .whereLayer("Persistence").mayOnlyBeAccessedByLayers("Service");
}
```

---

## 2. Plain package dependency rules

When you don't want a full layered model, just forbid a specific direction:

```java
@AnalyzeClasses(packages = "com.myapp")
class PackageDependencyTest {

    @ArchTest
    static final ArchRule services_should_not_access_controllers =
        noClasses()
            .that().resideInAPackage("..service..")
            .should().dependOnClassesThat().resideInAPackage("..controller..")
            .because("the service layer must not know about the web layer");

    @ArchTest
    static final ArchRule domain_should_not_depend_on_infrastructure =
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAPackage("..infrastructure..");
}
```

---

## 3. Naming conventions

```java
@AnalyzeClasses(packages = "com.myapp")
class NamingConventionTest {

    @ArchTest
    static final ArchRule services_end_with_Service =
        classes()
            .that().resideInAPackage("..service..")
            .and().areNotInterfaces()
            .should().haveSimpleNameEndingWith("Service");

    @ArchTest
    static final ArchRule repositories_are_named_correctly =
        classes()
            .that().haveSimpleNameEndingWith("Repository")
            .should().resideInAPackage("..persistence..");

    @ArchTest
    static final ArchRule no_test_classes_in_main =
        noClasses()
            .that().haveSimpleNameEndingWith("Test")
            .should().resideInAPackage("..main..");
}
```

---

## 4. Annotation conventions

```java
@AnalyzeClasses(packages = "com.myapp")
class AnnotationConventionTest {

    @ArchTest
    static final ArchRule services_are_annotated =
        classes()
            .that().resideInAPackage("..service..")
            .and().haveSimpleNameEndingWith("Service")
            .should().beAnnotatedWith(org.springframework.stereotype.Service.class);

    @ArchTest
    static final ArchRule controllers_are_rest_controllers =
        classes()
            .that().resideInAPackage("..controller..")
            .should().beAnnotatedWith(
                org.springframework.web.bind.annotation.RestController.class);
}
```

---

## 5. No cyclic dependencies between features (slices)

```java
@AnalyzeClasses(packages = "com.myapp")
class CycleTest {

    @ArchTest
    static final ArchRule no_cycles_between_features =
        slices()
            .matching("com.myapp.(*)..")
            .should().beFreeOfCycles();
}
```

To forbid any inter-feature dependency at all, swap the last line for
`.should().notDependOnEachOther();`.

---

## 6. Onion / hexagonal architecture

```java
@AnalyzeClasses(packages = "com.myapp")
class OnionArchitectureTest {

    @ArchTest
    static final ArchRule onion = onionArchitecture()
        .domainModels("..domain.model..")
        .domainServices("..domain.service..")
        .applicationServices("..application..")
        .adapter("rest", "..adapter.rest..")
        .adapter("persistence", "..adapter.persistence..");
}
```

---

## 7. Generic coding hygiene rules

Pull in ArchUnit's prepackaged rules directly:

```java
import static com.tngtech.archunit.library.GeneralCodingRules.*;

@AnalyzeClasses(packages = "com.myapp")
class CodingRulesTest {

    @ArchTest
    static final ArchRule no_system_out_or_err = NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;

    @ArchTest
    static final ArchRule no_generic_exceptions = NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS;

    @ArchTest
    static final ArchRule no_jul = NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING;

    @ArchTest
    static final ArchRule no_field_injection = NO_CLASSES_SHOULD_USE_FIELD_INJECTION;
}
```

Or write your own, e.g. forbid field injection explicitly:

```java
@ArchTest
static final ArchRule no_autowired_fields =
    noFields()
        .should().beAnnotatedWith(org.springframework.beans.factory.annotation.Autowired.class)
        .because("prefer constructor injection");
```

---

## 8. Sharing rules across modules

Define rules once in a library class:

```java
public class CommonArchRules {

    @ArchTest
    static final ArchRule no_field_injection =
        com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION;

    @ArchTest
    static final ArchRule no_cycles =
        slices().matching("(**)").should().beFreeOfCycles();
}
```

Then include them from any microservice's test:

```java
@AnalyzeClasses(packages = "com.myapp")
class ArchitectureTest {

    @ArchTest
    static final ArchTests common = ArchTests.in(CommonArchRules.class);
}
```

---

## 9. Freezing violations on a legacy codebase

Introduce a rule without breaking the current build; only new violations fail.

```java
import com.tngtech.archunit.library.freeze.FreezingArchRule;

@AnalyzeClasses(packages = "com.myapp")
class FrozenRulesTest {

    @ArchTest
    static final ArchRule services_dont_use_web_layer =
        FreezingArchRule.freeze(
            noClasses()
                .that().resideInAPackage("..service..")
                .should().dependOnClassesThat().resideInAPackage("..web.."));
}
```

The first run records existing violations as the baseline. Later runs fail only
if something new appears, and the baseline shrinks automatically as violations
are fixed.

---

## 10. Excluding test classes from analysis

```java
import com.tngtech.archunit.core.importer.ImportOption;

@AnalyzeClasses(
    packages = "com.myapp",
    importOptions = ImportOption.DoNotIncludeTests.class)
class ProductionOnlyArchitectureTest {

    @ArchTest
    static final ArchRule rule = classes()
        .that().resideInAPackage("..service..")
        .should().onlyBeAccessed().byAnyPackage("..controller..", "..service..");
}
```

---

## Note on the official examples repo

The official `TNG/ArchUnit-Examples` repository ships `example-junit4`,
`example-junit5` and `example-plain` modules. Its sample rules are intentionally
written to **fail**, to demonstrate how production code violates typical
constraints — so don't copy those expecting green tests; use the patterns above
instead and point the user to the repo for a broader catalog.
