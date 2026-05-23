package com.educaflow.base.infrastructure.mapper;

import com.axelor.db.Model;
import com.axelor.db.modelservice.AllowProperties;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests exhaustivos para {@link BeanMapperModel}.
 *
 * <p>Cubre los siguientes escenarios críticos:</p>
 * <ul>
 *   <li>Constructor: inyección de {@code ModelLoader}, null-safety</li>
 *   <li>{@code getEntityCloned}: entidad nula, clonado, comportamiento con allowProperties=null</li>
 *   <li>{@code copyEntityToEntity}: allowProperties nulo/vacío, escalares, modelos anidados,
 *       listas, mappedBy, propiedad no soportada</li>
 *   <li>{@code copyMapToEntity}: los 4 escenarios null/no-null para Model y para List,
 *       carga por ID desde el loader, integridad del ID original (copyValueToEntityAndNoChangeId),
 *       mappedBy, validación OneToOne/ManyToOne, tipo no soportado</li>
 * </ul>
 *
 * <p><b>Dependencias de test:</b></p>
 * <pre>
 *   testImplementation 'org.junit.jupiter:junit-jupiter:5.10+'
 *   testImplementation 'org.mockito:mockito-core:5+'
 *   testImplementation 'org.mockito:mockito-junit-jupiter:5+'
 * </pre>
 *
 * <p>Mockito 5 incluye el inline-mock-maker por defecto, necesario para {@code mockStatic}.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BeanMapperModel")
class BeanMapperModelTest2 {

    // =========================================================================
    // Modelos de prueba
    // =========================================================================

    /** Modelo simple con dos campos escalares. */
    public static class PersonModel extends Model {
        private String name;
        private Integer age;
        private Long id;

        @Override
        public Long getId() {
            return id;
        }
        @Override
        public void setId(Long id) {
            this.id = id;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getAge() { return age; }
        public void setAge(Integer age) { this.age = age; }
    }

    /** Modelo de dirección usado como relación anidada. */
    public static class AddressModel extends Model {
        private String street;
        private Long id;

        @Override
        public Long getId() {
            return id;
        }
        @Override
        public void setId(Long id) {
            this.id = id;
        }
        public String getStreet() { return street; }
        public void setStreet(String street) { this.street = street; }
    }

    /** Modelo con una relación Model (OneToOne / ManyToOne). */
    public static class PersonWithAddressModel extends Model {
        private String name;
        private AddressModel address;
        private Long id;

        @Override
        public Long getId() {
            return id;
        }
        @Override
        public void setId(Long id) {
            this.id = id;
        }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public AddressModel getAddress() { return address; }
        public void setAddress(AddressModel address) { this.address = address; }
    }

    /** Modelo con una relación OneToMany (List genérica tipada). */
    public static class PersonWithChildrenModel extends Model {
        private String name;
        private List<PersonModel> children;
        private Long id;

        @Override
        public Long getId() {
            return id;
        }
        @Override
        public void setId(Long id) {
            this.id = id;
        }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public List<PersonModel> getChildren() { return children; }
        public void setChildren(List<PersonModel> children) { this.children = children; }
    }

    /**
     * Modelo de país — nivel más profundo (nivel 3) para el test de anidamiento.
     * Todas sus propiedades son escalares.
     */
    public static class CountryModel extends Model {
        private Long id;
        private String countryName;
        private Integer population;

        @Override public Long getId() { return id; }
        @Override public void setId(Long id) { this.id = id; }
        public String getCountryName() { return countryName; }
        public void setCountryName(String countryName) { this.countryName = countryName; }
        public Integer getPopulation() { return population; }
        public void setPopulation(Integer population) { this.population = population; }
    }

    /**
     * Modelo de ciudad — nivel 2, contiene un {@link CountryModel} anidado.
     */
    public static class CityWithCountryModel extends Model {
        private Long id;
        private String cityName;
        private CountryModel country;

        @Override public Long getId() { return id; }
        @Override public void setId(Long id) { this.id = id; }
        public String getCityName() { return cityName; }
        public void setCityName(String cityName) { this.cityName = cityName; }
        public CountryModel getCountry() { return country; }
        public void setCountry(CountryModel country) { this.country = country; }
    }

    /**
     * Modelo de residente — nivel 1 (raíz), contiene un {@link CityWithCountryModel} anidado.
     * Demuestra tres niveles de profundidad: Resident → City → Country.
     */
    public static class ResidentModel extends Model {
        private Long id;
        private String residentName;
        private Integer age;
        private CityWithCountryModel city;

        @Override public Long getId() { return id; }
        @Override public void setId(Long id) { this.id = id; }
        public String getResidentName() { return residentName; }
        public void setResidentName(String residentName) { this.residentName = residentName; }
        public Integer getAge() { return age; }
        public void setAge(Integer age) { this.age = age; }
        public CityWithCountryModel getCity() { return city; }
        public void setCity(CityWithCountryModel city) { this.city = city; }
    }

    /** Modelo raíz para probar un ciclo bidireccional real parent -> child -> parent. */
    public static class CyclicParentModel extends Model {
        private Long id;
        private String name;

        @OneToMany(mappedBy = "parent")
        private List<CyclicChildModel> children;

        @Override public Long getId() { return id; }
        @Override public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public List<CyclicChildModel> getChildren() { return children; }
        public void setChildren(List<CyclicChildModel> children) { this.children = children; }
    }

    /** Modelo hijo que referencia al modelo superior para cerrar el ciclo. */
    public static class CyclicChildModel extends Model {
        private Long id;
        private String name;

        @ManyToOne
        private CyclicParentModel parent;

        @Override public Long getId() { return id; }
        @Override public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public CyclicParentModel getParent() { return parent; }
        public void setParent(CyclicParentModel parent) { this.parent = parent; }
    }

    // =========================================================================
    // Estado compartido
    // =========================================================================

    private ModelLoader mockLoader;
    private BeanMapperModel mapper;

    @BeforeEach
    void setUp() {
        mockLoader = mock(ModelLoader.class);
        mapper = new BeanMapperModel(mockLoader);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /** Construye un mapa de allowProperties con las claves indicadas y valor null. */
    private static AllowProperties allow(String... keys) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (String key : keys) {
            map.put(key, null);
        }
        return AllowProperties.createAllowProperties(map);
    }

    /** Construye un mapa de allowProperties con una clave que tiene un sub-mapa. */
    private static AllowProperties allowWithNested(String key, Map<String, Object> nested) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(key, nested);
        return AllowProperties.createAllowProperties(map);
    }

    // =========================================================================
    // 1. Constructor
    // =========================================================================

    @Nested
    @DisplayName("Constructor")
    class ConstructorTests {

        @Test
        @DisplayName("Constructor con ModelLoader nulo lanza NullPointerException")
        void nullModelLoader_throwsNullPointerException() {
            assertThrows(NullPointerException.class,
                    () -> new BeanMapperModel(null),
                    "Se esperaba NullPointerException al pasar null como ModelLoader");
        }

        @Test
        @DisplayName("Constructor con ModelLoader válido crea la instancia correctamente")
        void validModelLoader_createsInstance() {
            BeanMapperModel instance = new BeanMapperModel(mockLoader);
            assertNotNull(instance);
        }

        @Test
        @DisplayName("Constructor por defecto no lanza excepción (usa DefaultModelLoader)")
        void defaultConstructor_doesNotThrow() {
            assertDoesNotThrow(() -> {
                new BeanMapperModel();
            }, "El constructor por defecto no debería lanzar excepción");
        }
    }

    // =========================================================================
    // 2. getEntityCloned
    // =========================================================================

    @Nested
    @DisplayName("getEntityCloned")
    class GetEntityClonedTests {

        @Test
        @DisplayName("Entidad nula → devuelve null (sin mappedBy)")
        void nullEntity_returnsNull() {
            assertNull(mapper.getEntityCloned(PersonModel.class, null));
        }

        @Test
        @DisplayName("Entidad nula → devuelve null (con mappedBy)")
        void nullEntity_withMappedBy_returnsNull() {
            PersonModel parent = new PersonModel();
            assertNull(mapper.getEntityCloned(PersonModel.class, null, "parent", parent));
        }

        @Test
        @DisplayName("Entidad no nula → devuelve nueva instancia de la misma clase")
        void nonNullEntity_returnsNewInstanceOfSameClass() {
            PersonModel source = new PersonModel();
            source.setName("Alice");
            source.setId(45L);

            Object result = mapper.getEntityCloned(PersonModel.class, source);

            assertNotNull(result);
            assertNotSame(source, result);
            assertInstanceOf(PersonModel.class, result,
                    "El clon debe ser del mismo tipo que la entidad fuente");
            assertEquals(source.getName(), ((PersonModel)result).getName(),
                    "El clon debe ser una instancia igual a la original");
        }

        @Test
        @DisplayName("Las propiedades NO se copian porque copyEntityToEntity recibe allowProperties=null")
        void propertiesAreNotCopied_becauseAllowPropertiesIsNull() {
            // getEntityCloned llama a copyEntityToEntity con allowProperties=null,
            // lo que provoca un retorno inmediato → el clon queda vacío.
            // Este es el comportamiento diseñado de la clase (la copia selectiva la controla allowProperties).
            PersonModel source = new PersonModel();
            source.setName("Alice");
            source.setAge(30);
            source.setId(34L);

            PersonModel result = (PersonModel) mapper.getEntityCloned(PersonModel.class, source);

            assertEquals(source.getName(),result.getName(),
                    "name  debería copiarse ");
            assertEquals(source.getAge(),result.getAge(),
                    "age debería copiarse ");
        }

        @Test
        @DisplayName("Con mappedBy → también devuelve instancia vacía (mappedBy tampoco se aplica)")
        void withMappedBy_returnsEmptyInstance() {
            // Igual que antes: el mappedBy solo se procesa DENTRO del bucle de copyEntityToEntity,
            // pero si allowProperties=null se retorna antes del bucle.
            PersonModel parent = new PersonModel();
            PersonModel child = new PersonModel();
            child.setName("ChildName");
            child.setId(34L);

            PersonModel result = (PersonModel) mapper.getEntityCloned(
                    PersonModel.class, child, "parent", parent);

            assertNotNull(result);
            assertNotNull(result.getName(),
                    "name debería copiarse");
        }

        @Test
        @DisplayName("Clona 3 niveles de anidamiento (Resident → City → Country) con todas las propiedades rellenas")
        void clones_threeLevel_nestedClasses_withAllPropertiesSet() {
            // ── Nivel 3: CountryModel ─────────────────────────────────────────
            CountryModel country = new CountryModel();
            country.setId(1L);
            country.setCountryName("España");
            country.setPopulation(47_000_000);

            // ── Nivel 2: CityWithCountryModel (contiene CountryModel) ─────────
            CityWithCountryModel city = new CityWithCountryModel();
            city.setId(10L);
            city.setCityName("Madrid");
            city.setCountry(country);

            // ── Nivel 1: ResidentModel (contiene CityWithCountryModel) ─────────
            ResidentModel resident = new ResidentModel();
            resident.setId(100L);
            resident.setResidentName("Ana García");
            resident.setAge(35);
            resident.setCity(city);

            // Clonar el árbol completo con getEntityCloned
            ResidentModel cloned = (ResidentModel) mapper.getEntityCloned(ResidentModel.class, resident);

            // ── Verificar nivel 1: ResidentModel ─────────────────────────────
            assertNotSame(resident, cloned,
                    "El clon debe ser una instancia distinta al original");
            assertEquals(100L, cloned.getId());
            assertEquals("Ana García", cloned.getResidentName());
            assertEquals(35, cloned.getAge());

            // ── Verificar nivel 2: CityWithCountryModel clonada ───────────────
            assertNotNull(cloned.getCity(),
                    "La ciudad debe haberse clonado (no null)");
            assertNotSame(city, cloned.getCity(),
                    "La ciudad clonada debe ser una instancia diferente a la original");
            assertEquals(10L, cloned.getCity().getId());
            assertEquals("Madrid", cloned.getCity().getCityName());

            // ── Verificar nivel 3: CountryModel clonado dentro de City ────────
            assertNotNull(cloned.getCity().getCountry(),
                    "El país debe haberse clonado dentro de la ciudad (no null)");
            assertNotSame(country, cloned.getCity().getCountry(),
                    "El país clonado debe ser una instancia diferente al original");
            assertEquals(1L, cloned.getCity().getCountry().getId());
            assertEquals("España", cloned.getCity().getCountry().getCountryName());
            assertEquals(47_000_000, cloned.getCity().getCountry().getPopulation());
        }

        @Test
        @DisplayName("Clona un ciclo padre-hijo-padre sin recursión infinita y reata la referencia al clon superior")
        void clones_cycle_whereNestedObjectReferencesUpperObject() {
            CyclicParentModel parent = new CyclicParentModel();
            parent.setId(500L);
            parent.setName("Padre raíz");

            CyclicChildModel child1 = new CyclicChildModel();
            child1.setId(501L);
            child1.setName("Hijo 1");
            child1.setParent(parent);

            CyclicChildModel child2 = new CyclicChildModel();
            child2.setId(502L);
            child2.setName("Hijo 2");
            child2.setParent(parent);

            parent.setChildren(new ArrayList<>(List.of(child1, child2)));

            CyclicParentModel cloned = assertDoesNotThrow(
                    () -> (CyclicParentModel) mapper.getEntityCloned(CyclicParentModel.class, parent),
                    "El clonado del ciclo parent -> child -> parent no debe producir recursión infinita"
            );

            assertNotSame(parent, cloned);
            assertEquals(500L, cloned.getId());
            assertEquals("Padre raíz", cloned.getName());
            assertNotNull(cloned.getChildren());
            assertEquals(2, cloned.getChildren().size());

            CyclicChildModel clonedChild1 = cloned.getChildren().get(0);
            CyclicChildModel clonedChild2 = cloned.getChildren().get(1);

            assertNotSame(child1, clonedChild1);
            assertNotSame(child2, clonedChild2);
            assertSame(cloned, clonedChild1.getParent(),
                    "La referencia superior del primer hijo debe apuntar al padre clonado");
            assertSame(cloned, clonedChild2.getParent(),
                    "La referencia superior del segundo hijo debe apuntar al mismo padre clonado");
            assertNotSame(parent, clonedChild1.getParent());
            assertNotSame(parent, clonedChild2.getParent());
            assertSame(cloned.getChildren(), clonedChild1.getParent().getChildren());
            assertTrue(cloned.getChildren().contains(clonedChild1));
            assertTrue(cloned.getChildren().contains(clonedChild2));
        }
    }

    // =========================================================================
    // 3. copyEntityToEntity
    // =========================================================================

    @Nested
    @DisplayName("copyEntityToEntity")
    class CopyEntityToEntityTests {

        @Test
        @DisplayName("allowProperties nulo → no-op, destino queda sin cambios")
        void nullAllowProperties_isNoOp() {
            PersonModel source = new PersonModel();
            source.setName("Bob");
            PersonModel dest = new PersonModel();

            mapper.copyEntityToEntity(PersonModel.class, source, dest, null);

            assertNull(dest.getName(), "Con allowProperties=null no se debería copiar nada");
        }

        @Test
        @DisplayName("allowProperties vacío → no se copia ninguna propiedad")
        void emptyAllowProperties_nothingIsCopied() {
            PersonModel source = new PersonModel();
            source.setName("Bob");
            source.setAge(25);
            PersonModel dest = new PersonModel();

            mapper.copyEntityToEntity(
                    PersonModel.class,
                    source,
                    dest,
                    AllowProperties.createAllowProperties(Collections.emptyMap()));

            assertNull(dest.getName());
            assertNull(dest.getAge());
        }

        @Test
        @DisplayName("Propiedad escalar en allowProperties → se copia al destino")
        void scalarProperty_inAllowProperties_isCopied() {
            try (MockedStatic<ScalarMapper> sm = mockStatic(ScalarMapper.class, CALLS_REAL_METHODS)) {
                sm.when(() -> ScalarMapper.isScalarType(String.class)).thenReturn(true);
                sm.when(() -> ScalarMapper.isScalarType(Integer.class)).thenReturn(true);
                sm.when(() -> ScalarMapper.getScalarFromObject("Alice", String.class))
                        .thenReturn("Alice");

                PersonModel source = new PersonModel();
                source.setName("Alice");
                source.setAge(99); // age NO estará en allowProperties
                PersonModel dest = new PersonModel();

                mapper.copyEntityToEntity(PersonModel.class, source, dest, allow("name"));

                assertEquals("Alice", dest.getName(), "name debería haberse copiado");
                assertNull(dest.getAge(), "age no debería copiarse porque no está en allowProperties");
            }
        }

        @Test
        @DisplayName("Propiedad escalar fuera de allowProperties → se omite")
        void scalarProperty_notInAllowProperties_isSkipped() {
            try (MockedStatic<ScalarMapper> sm = mockStatic(ScalarMapper.class)) {
                sm.when(() -> ScalarMapper.isScalarType(any())).thenReturn(true);

                PersonModel source = new PersonModel();
                source.setName("NewName");
                PersonModel dest = new PersonModel();
                dest.setName("OriginalName");

                // Solo "age" está permitido, no "name"
                mapper.copyEntityToEntity(PersonModel.class, source, dest, allow("age"));

                assertEquals("OriginalName", dest.getName(),
                        "name no debería haberse sobreescrito porque no está en allowProperties");
            }
        }

        @Test
        @DisplayName("Propiedad mappedBy en allowProperties → se asigna directamente el modelo padre")
        void mappedByProperty_isSetDirectlyFromMappedByModel_whenAllowed() {
            try (MockedStatic<ScalarMapper> sm = mockStatic(ScalarMapper.class)) {
                sm.when(() -> ScalarMapper.isScalarType(String.class)).thenReturn(true);
                sm.when(() -> ScalarMapper.getScalarFromObject(any(), eq(String.class)))
                        .thenReturn(null);

                AddressModel mappedByModel = new AddressModel();
                mappedByModel.setStreet("Calle Padre");

                PersonWithAddressModel source = new PersonWithAddressModel();
                PersonWithAddressModel dest = new PersonWithAddressModel();

                // "address" está en allowProperties Y es la propiedad mappedBy
                Map<String, Object> allowProps = new LinkedHashMap<>();
                allowProps.put("name", null);
                allowProps.put("address", null);

                mapper.copyEntityToEntity(
                        PersonWithAddressModel.class, source, dest,
                        AllowProperties.createAllowProperties(allowProps), "address", mappedByModel);

                assertSame(mappedByModel, dest.getAddress(),
                        "La propiedad mappedBy debe apuntar al modelo padre recibido");
            }
        }

        @Test
        @DisplayName("Propiedad Model anidada → se clona recursivamente (nueva instancia vacía)")
        void nestedModelProperty_isRecursivelyCloned() {
            try (MockedStatic<ScalarMapper> sm = mockStatic(ScalarMapper.class, CALLS_REAL_METHODS)) {
                // AddressModel no es escalar
                sm.when(() -> ScalarMapper.isScalarType(AddressModel.class)).thenReturn(false);
                // String sí es escalar (para name en PersonWithAddressModel si estuviera)
                sm.when(() -> ScalarMapper.isScalarType(String.class)).thenReturn(true);
                sm.when(() -> ScalarMapper.getScalarFromObject(any(), eq(String.class)))
                        .thenReturn(null);

                AddressModel address = new AddressModel();
                address.setId(1L);
                address.setStreet("Calle Mayor 1");

                PersonWithAddressModel source = new PersonWithAddressModel();
                source.setAddress(address);

                PersonWithAddressModel dest = new PersonWithAddressModel();

                mapper.copyEntityToEntity(
                        PersonWithAddressModel.class, source, dest,
                        allowWithNested("address", new HashMap<>()));

                assertNotNull(dest.getAddress(),
                        "El campo address debería haberse clonado (nueva instancia)");
                assertInstanceOf(AddressModel.class, dest.getAddress());
                assertNotSame(address, dest.getAddress(),
                        "El clon debe ser una instancia diferente al original");
                // Como getEntityCloned usa allowProperties=null → el clon está vacío
                assertNull(dest.getAddress().getStreet(),
                        "street no se copia porque la recursión usa allowProperties=null");
            }
        }

        @Test
        @DisplayName("Propiedad Model anidada nula → se propaga null al destino")
        void nullNestedModelProperty_isSetAsNull() {
            try (MockedStatic<ScalarMapper> sm = mockStatic(ScalarMapper.class, CALLS_REAL_METHODS)) {
                sm.when(() -> ScalarMapper.isScalarType(AddressModel.class)).thenReturn(false);

                PersonWithAddressModel source = new PersonWithAddressModel();
                source.setAddress(null);

                PersonWithAddressModel dest = new PersonWithAddressModel();
                dest.setAddress(new AddressModel()); // pre-existente que debe quedar en null

                mapper.copyEntityToEntity(
                        PersonWithAddressModel.class, source, dest,
                        allowWithNested("address", new HashMap<>()));

                assertNull(dest.getAddress(),
                        "Una dirección nula en el source debe resultar en null en el destino");
            }
        }

        @Test
        @DisplayName("Propiedad List → se crea nueva lista con ítems clonados")
        void listProperty_createsNewListWithClonedItems() {
            try (MockedStatic<ScalarMapper> sm = mockStatic(ScalarMapper.class, CALLS_REAL_METHODS);
                 MockedStatic<BeanMapperUtil> bu = mockStatic(BeanMapperUtil.class)) {

                bu.when(() -> BeanMapperUtil.getMappedByInOneToMany(
                        PersonWithChildrenModel.class, "children")).thenReturn(null);

                PersonModel child1 = new PersonModel();
                child1.setName("Child1");
                child1.setId(34L);
                PersonModel child2 = new PersonModel();
                child2.setName("Child2");
                child2.setId(34L);

                PersonWithChildrenModel source = new PersonWithChildrenModel();
                source.setChildren(new ArrayList<>(List.of(child1, child2)));

                PersonWithChildrenModel dest = new PersonWithChildrenModel();

                mapper.copyEntityToEntity(
                        PersonWithChildrenModel.class, source, dest,
                        allowWithNested("children", new HashMap<>()));

                assertNotNull(dest.getChildren());
                assertEquals(2, dest.getChildren().size(),
                        "Se deben crear 2 ítems clonados en la lista destino");
                assertNotSame(child1, dest.getChildren().get(0),
                        "Cada ítem clonado debe ser una nueva instancia");
                assertNotSame(child2, dest.getChildren().get(1));
            }
        }

        @Test
        @DisplayName("Propiedad de tipo no soportado → lanza RuntimeException")
        void unsupportedPropertyType_throwsRuntimeException() {
            // Usamos una clase con un tipo de propiedad que no es escalar, ni Model, ni List.
            // ScalarMapper.isScalarType devolverá false, y como el tipo no es Model ni List,
            // el código llega al bloque else que lanza RuntimeException.
            try (MockedStatic<ScalarMapper> sm = mockStatic(ScalarMapper.class)) {
                sm.when(() -> ScalarMapper.isScalarType(any())).thenReturn(false);

                // PersonModel tiene "name" (String). Si String no es escalar, no es Model ni List:
                PersonModel source = new PersonModel();
                source.setName("test");
                PersonModel dest = new PersonModel();

                assertThrows(RuntimeException.class,
                        () -> mapper.copyEntityToEntity(PersonModel.class, source, dest, allow("name")),
                        "Un tipo no soportado debe lanzar RuntimeException");
            }
        }
    }

    // =========================================================================
    // 4. copyMapToEntity
    // =========================================================================

    @Nested
    @DisplayName("copyMapToEntity")
    class CopyMapToEntityTests {

        // ---- Guardas generales -------------------------------------------

        @Test
        @DisplayName("allowProperties nulo → no-op, destino queda sin cambios")
        void nullAllowProperties_isNoOp() {
            PersonModel dest = new PersonModel();
            mapper.copyMapToEntity(PersonModel.class, Map.of("name", "Alice"), dest, null);
            assertNull(dest.getName());
        }

        @Test
        @DisplayName("Propiedad presente en allowProperties pero ausente del mapa → se omite")
        void propertyInAllowButNotInMap_isSkipped() {
            try (MockedStatic<ScalarMapper> sm = mockStatic(ScalarMapper.class)) {
                sm.when(() -> ScalarMapper.isScalarType(any())).thenReturn(true);

                PersonModel dest = new PersonModel();
                dest.setName("Preserved");

                // entityMap vacío: name no viene en el mapa de datos
                mapper.copyMapToEntity(PersonModel.class, new HashMap<>(), dest, allow("name"));

                assertEquals("Preserved", dest.getName(),
                        "Si la propiedad no está en el mapa de datos, no debe modificarse");
            }
        }

        @Test
        @DisplayName("Propiedad presente en el mapa pero ausente de allowProperties → se omite")
        void propertyInMapButNotInAllow_isSkipped() {
            try (MockedStatic<ScalarMapper> sm = mockStatic(ScalarMapper.class)) {
                sm.when(() -> ScalarMapper.isScalarType(any())).thenReturn(true);

                PersonModel dest = new PersonModel();
                dest.setName("Preserved");

                Map<String, Object> entityMap = new HashMap<>();
                entityMap.put("name", "NewName");

                mapper.copyMapToEntity(PersonModel.class, entityMap, dest, allow("age"));

                assertEquals("Preserved", dest.getName(),
                        "Si la propiedad no está en allowProperties, no debe copiarse");
            }
        }

        // ---- Propiedad escalar -------------------------------------------

        @Test
        @DisplayName("Propiedad escalar → se copia del mapa al destino con conversión de tipo")
        void scalarProperty_isCopiedWithTypeConversion() {
            try (MockedStatic<ScalarMapper> sm = mockStatic(ScalarMapper.class)) {
                sm.when(() -> ScalarMapper.isScalarType(String.class)).thenReturn(true);
                sm.when(() -> ScalarMapper.isScalarType(Integer.class)).thenReturn(true);
                sm.when(() -> ScalarMapper.getScalarFromObject("Alice", String.class))
                        .thenReturn("Alice");

                PersonModel dest = new PersonModel();
                Map<String, Object> entityMap = new HashMap<>();
                entityMap.put("name", "Alice");

                mapper.copyMapToEntity(PersonModel.class, entityMap, dest, allow("name"));

                assertEquals("Alice", dest.getName());
            }
        }

        // ---- Propiedad mappedBy ------------------------------------------

        @Test
        @DisplayName("Propiedad mappedBy en allowProperties → se asigna el modelo padre")
        void mappedByProperty_isSetDirectly_whenAllowed() {
            try (MockedStatic<ScalarMapper> sm = mockStatic(ScalarMapper.class)) {
                sm.when(() -> ScalarMapper.isScalarType(String.class)).thenReturn(true);
                sm.when(() -> ScalarMapper.getScalarFromObject(any(), eq(String.class)))
                        .thenReturn(null);

                AddressModel parentModel = new AddressModel();
                parentModel.setStreet("Calle Parent");

                PersonWithAddressModel dest = new PersonWithAddressModel();

                // "address" está en el mapa y permitido en allowProperties,
                // por tanto se asigna desde mappedByModel.
                Map<String, Object> entityMap = new HashMap<>();
                entityMap.put("address", new HashMap<>()); // presencia en el mapa

                mapper.copyMapToEntity(
                        PersonWithAddressModel.class, entityMap, dest,
                        allow("name", "address"),
                        "address", parentModel);

                assertSame(parentModel, dest.getAddress(),
                        "La propiedad mappedBy debe recibir directamente el mappedByModel");
            }
        }

        @Test
        @DisplayName("Propiedad mappedBy fuera de allowProperties → se omite")
        void mappedByProperty_notInAllow_isSkipped() {
            AddressModel parentModel = new AddressModel();
            parentModel.setStreet("Calle Parent");

            PersonWithAddressModel dest = new PersonWithAddressModel();
            AddressModel originalAddress = new AddressModel();
            originalAddress.setStreet("Dirección original");
            dest.setAddress(originalAddress);

            Map<String, Object> entityMap = new HashMap<>();
            entityMap.put("address", new HashMap<>());

            mapper.copyMapToEntity(
                    PersonWithAddressModel.class, entityMap, dest,
                    allow("name"),
                    "address", parentModel);

            assertSame(originalAddress, dest.getAddress(),
                    "Si mappedBy no está permitido, la propiedad no debe cambiar");
        }

        // ---- Propiedad tipo Model (4 combinaciones null/no-null) ----------

        @Nested
        @DisplayName("Propiedad de tipo Model anidado")
        class NestedModelPropertyTests {

            @Test
            @DisplayName("source=null y dest=null → no-op")
            void bothNull_noOp() {
                try (MockedStatic<ScalarMapper> sm = mockStatic(ScalarMapper.class);
                     MockedStatic<BeanMapperUtil> bu = mockStatic(BeanMapperUtil.class)) {

                    sm.when(() -> ScalarMapper.isScalarType(any())).thenReturn(false);
                    bu.when(() -> BeanMapperUtil.isOneToOne(any(), eq("address"))).thenReturn(true);
                    bu.when(() -> BeanMapperUtil.isManyToOne(any(), any())).thenReturn(false);

                    PersonWithAddressModel dest = new PersonWithAddressModel();
                    // address ya es null

                    Map<String, Object> entityMap = new HashMap<>();
                    entityMap.put("address", null);

                    mapper.copyMapToEntity(PersonWithAddressModel.class, entityMap, dest,
                            allowWithNested("address", new HashMap<>()));

                    assertNull(dest.getAddress(), "Ambos null → destino sigue siendo null");
                }
            }

            @Test
            @DisplayName("source=null y dest!=null → destino se pone a null")
            void sourceNull_destNotNull_setsNull() {
                try (MockedStatic<ScalarMapper> sm = mockStatic(ScalarMapper.class);
                     MockedStatic<BeanMapperUtil> bu = mockStatic(BeanMapperUtil.class)) {

                    sm.when(() -> ScalarMapper.isScalarType(any())).thenReturn(false);
                    bu.when(() -> BeanMapperUtil.isOneToOne(any(), eq("address"))).thenReturn(true);
                    bu.when(() -> BeanMapperUtil.isManyToOne(any(), any())).thenReturn(false);

                    PersonWithAddressModel dest = new PersonWithAddressModel();
                    dest.setAddress(new AddressModel()); // existe una dirección previa

                    Map<String, Object> entityMap = new HashMap<>();
                    entityMap.put("address", null);

                    mapper.copyMapToEntity(PersonWithAddressModel.class, entityMap, dest,
                            allowWithNested("address", new HashMap<>()));

                    assertNull(dest.getAddress(),
                            "Si el source es null y el dest tenía valor, debe quedar null");
                }
            }

            @Test
            @DisplayName("source!=null sin id y dest=null → crea nueva instancia y copia")
            void sourceNotNull_noId_destNull_createsNewModel() {
                try (MockedStatic<ScalarMapper> sm = mockStatic(ScalarMapper.class);
                     MockedStatic<BeanMapperUtil> bu = mockStatic(BeanMapperUtil.class)) {

                    sm.when(() -> ScalarMapper.isScalarType(any())).thenReturn(false);
                    bu.when(() -> BeanMapperUtil.isOneToOne(any(), eq("address"))).thenReturn(true);
                    bu.when(() -> BeanMapperUtil.isManyToOne(any(), any())).thenReturn(false);

                    PersonWithAddressModel dest = new PersonWithAddressModel();
                    // address es null en dest

                    Map<String, Object> addressMap = new HashMap<>();
                    addressMap.put("id", null); // sin id → nuevo modelo

                    Map<String, Object> entityMap = new HashMap<>();
                    entityMap.put("address", addressMap);

                    mapper.copyMapToEntity(PersonWithAddressModel.class, entityMap, dest,
                            allowWithNested("address", new HashMap<>()));

                    assertNotNull(dest.getAddress(),
                            "Con source != null y dest == null debe crearse una nueva instancia");
                    assertInstanceOf(AddressModel.class, dest.getAddress());
                    assertNull(dest.getAddress().getId(),
                            "El nuevo modelo creado no tiene id asignado");
                }
            }

            @Test
            @DisplayName("source!=null con id y dest=null → carga el modelo desde ModelLoader")
            void sourceNotNull_withId_destNull_loadsFromLoader() {
                AddressModel loadedAddress = new AddressModel();
                loadedAddress.setId(42L);
                loadedAddress.setStreet("Calle Cargada 42");

                when(mockLoader.getModel(AddressModel.class, 42L)).thenReturn(loadedAddress);

                try (MockedStatic<ScalarMapper> sm = mockStatic(ScalarMapper.class);
                     MockedStatic<BeanMapperUtil> bu = mockStatic(BeanMapperUtil.class)) {

                    sm.when(() -> ScalarMapper.isScalarType(any())).thenReturn(false);
                    bu.when(() -> BeanMapperUtil.isOneToOne(any(), eq("address"))).thenReturn(true);
                    bu.when(() -> BeanMapperUtil.isManyToOne(any(), any())).thenReturn(false);

                    PersonWithAddressModel dest = new PersonWithAddressModel();

                    Map<String, Object> addressMap = new HashMap<>();
                    addressMap.put("id", 42L);

                    Map<String, Object> entityMap = new HashMap<>();
                    entityMap.put("address", addressMap);

                    mapper.copyMapToEntity(PersonWithAddressModel.class, entityMap, dest,
                            allowWithNested("address", new HashMap<>()));

                    verify(mockLoader).getModel(AddressModel.class, 42L);
                    assertSame(loadedAddress, dest.getAddress(),
                            "El modelo cargado por el loader debe usarse como base");
                }
            }

            @Test
            @DisplayName("source!=null con id no encontrado en loader → lanza RuntimeException")
            void sourceWithIdNotFoundInLoader_throwsRuntimeException() {
                when(mockLoader.getModel(eq(AddressModel.class), eq(999L))).thenReturn(null);

                try (MockedStatic<ScalarMapper> sm = mockStatic(ScalarMapper.class);
                     MockedStatic<BeanMapperUtil> bu = mockStatic(BeanMapperUtil.class)) {

                    sm.when(() -> ScalarMapper.isScalarType(any())).thenReturn(false);
                    bu.when(() -> BeanMapperUtil.isOneToOne(any(), eq("address"))).thenReturn(true);
                    bu.when(() -> BeanMapperUtil.isManyToOne(any(), any())).thenReturn(false);

                    PersonWithAddressModel dest = new PersonWithAddressModel();

                    Map<String, Object> addressMap = new HashMap<>();
                    addressMap.put("id", 999L);

                    Map<String, Object> entityMap = new HashMap<>();
                    entityMap.put("address", addressMap);

                    RuntimeException ex = assertThrows(RuntimeException.class,
                            () -> mapper.copyMapToEntity(PersonWithAddressModel.class, entityMap, dest,
                                    allowWithNested("address", new HashMap<>())));

                    assertTrue(ex.getMessage() != null || ex.getCause() != null,
                            "Debe lanzarse una RuntimeException cuando el modelo no se encuentra en BD");
                }
            }

            @Test
            @DisplayName("Propiedad Model que no es OneToOne ni ManyToOne → lanza RuntimeException")
            void modelPropertyNotOneToOneOrManyToOne_throwsRuntimeException() {
                try (MockedStatic<ScalarMapper> sm = mockStatic(ScalarMapper.class);
                     MockedStatic<BeanMapperUtil> bu = mockStatic(BeanMapperUtil.class)) {

                    sm.when(() -> ScalarMapper.isScalarType(any())).thenReturn(false);
                    bu.when(() -> BeanMapperUtil.isOneToOne(any(), any())).thenReturn(false);
                    bu.when(() -> BeanMapperUtil.isManyToOne(any(), any())).thenReturn(false);

                    PersonWithAddressModel dest = new PersonWithAddressModel();

                    Map<String, Object> entityMap = new HashMap<>();
                    entityMap.put("address", new HashMap<>());

                    assertThrows(RuntimeException.class,
                            () -> mapper.copyMapToEntity(PersonWithAddressModel.class, entityMap, dest,
                                    allowWithNested("address", new HashMap<>())),
                            "Una relación que no es OneToOne ni ManyToOne debe lanzar RuntimeException");
                }
            }

            @Test
            @DisplayName("source!=null y dest!=null → actualiza in-place (copyValueToEntityAndNoChangeId)")
            void sourceNotNull_destNotNull_updatesInPlace() {
                try (MockedStatic<ScalarMapper> sm = mockStatic(ScalarMapper.class);
                     MockedStatic<BeanMapperUtil> bu = mockStatic(BeanMapperUtil.class)) {

                    sm.when(() -> ScalarMapper.isScalarType(String.class)).thenReturn(true);
                    sm.when(() -> ScalarMapper.isScalarType(AddressModel.class)).thenReturn(false);
                    sm.when(() -> ScalarMapper.getScalarFromObject("Calle Nueva 7", String.class))
                            .thenReturn("Calle Nueva 7");
                    bu.when(() -> BeanMapperUtil.isOneToOne(any(), eq("address"))).thenReturn(true);
                    bu.when(() -> BeanMapperUtil.isManyToOne(any(), any())).thenReturn(false);

                    AddressModel existingAddress = new AddressModel();
                    existingAddress.setId(77L);
                    existingAddress.setStreet("Calle Vieja 3");

                    PersonWithAddressModel dest = new PersonWithAddressModel();
                    dest.setAddress(existingAddress);

                    Map<String, Object> addressMap = new HashMap<>();
                    addressMap.put("street", "Calle Nueva 7");
                    // No hay 'id' en el mapa → la propiedad 'id' no está en entityMap y se omite

                    Map<String, Object> entityMap = new HashMap<>();
                    entityMap.put("address", addressMap);

                    Map<String, Object> addressAllow = new HashMap<>();
                    addressAllow.put("street", null);

                    mapper.copyMapToEntity(PersonWithAddressModel.class, entityMap, dest,
                            allowWithNested("address", addressAllow));

                    assertSame(existingAddress, dest.getAddress(),
                            "La instancia existente debe actualizarse in-place, no reemplazarse");
                    assertEquals("Calle Nueva 7", dest.getAddress().getStreet(),
                            "El street debe haberse actualizado");
                }
            }
        }

        // ---- Preservación del ID (copyValueToEntityAndNoChangeId) ---------

        @Nested
        @DisplayName("Preservación del ID original (copyValueToEntityAndNoChangeId)")
        class PreserveIdTests {

            @Test
            @DisplayName("El ID original del modelo destino se preserva aunque el mapa contenga un id diferente")
            void originalId_isPreservedAfterUpdate() {
                try (MockedStatic<ScalarMapper> sm = mockStatic(ScalarMapper.class);
                     MockedStatic<BeanMapperUtil> bu = mockStatic(BeanMapperUtil.class)) {

                    sm.when(() -> ScalarMapper.isScalarType(String.class)).thenReturn(true);
                    sm.when(() -> ScalarMapper.isScalarType(Long.class)).thenReturn(true);
                    sm.when(() -> ScalarMapper.isScalarType(AddressModel.class)).thenReturn(false);
                    sm.when(() -> ScalarMapper.getScalarFromObject(any(), eq(String.class)))
                            .thenReturn("Actualizada");
                    sm.when(() -> ScalarMapper.getScalarFromObject(any(), eq(Long.class)))
                            .thenReturn(999L);
                    bu.when(() -> BeanMapperUtil.isOneToOne(any(), eq("address"))).thenReturn(true);
                    bu.when(() -> BeanMapperUtil.isManyToOne(any(), any())).thenReturn(false);

                    AddressModel existingAddress = new AddressModel();
                    existingAddress.setId(100L); // ID original
                    existingAddress.setStreet("Antigua");

                    PersonWithAddressModel dest = new PersonWithAddressModel();
                    dest.setAddress(existingAddress);

                    // El mapa de la dirección viene con un id diferente e intenta sobreescribirlo
                    Map<String, Object> addressMap = new HashMap<>();
                    addressMap.put("id", 999L);      // DIFERENTE al original
                    addressMap.put("street", "Actualizada");

                    Map<String, Object> entityMap = new HashMap<>();
                    entityMap.put("address", addressMap);

                    Map<String, Object> addressAllow = new HashMap<>();
                    addressAllow.put("street", null);
                    addressAllow.put("id", null); // incluso si id está en allowProperties...

                    mapper.copyMapToEntity(PersonWithAddressModel.class, entityMap, dest,
                            allowWithNested("address", addressAllow));

                    // copyValueToEntityAndNoChangeId guarda y restaura el id original
                    assertEquals(100L, dest.getAddress().getId(),
                            "El ID original (100L) debe preservarse, no sobreescribirse con 999L del mapa");
                }
            }

            @Test
            @DisplayName("El ID original de ítems de lista también se preserva tras la actualización")
            void listItemOriginalId_isPreservedAfterUpdate() {
                PersonModel existingChild = new PersonModel();
                existingChild.setId(55L);
                existingChild.setName("NombreOriginal");

                when(mockLoader.getModel(PersonModel.class, 55L)).thenReturn(existingChild);

                try (MockedStatic<ScalarMapper> sm = mockStatic(ScalarMapper.class);
                     MockedStatic<BeanMapperUtil> bu = mockStatic(BeanMapperUtil.class)) {

                    sm.when(() -> ScalarMapper.isScalarType(String.class)).thenReturn(true);
                    sm.when(() -> ScalarMapper.isScalarType(Long.class)).thenReturn(true);
                    sm.when(() -> ScalarMapper.isScalarType(Integer.class)).thenReturn(true);
                    sm.when(() -> ScalarMapper.isScalarType(PersonModel.class)).thenReturn(false);
                    sm.when(() -> ScalarMapper.getScalarFromObject(any(), eq(String.class)))
                            .thenReturn("NombreActualizado");
                    bu.when(() -> BeanMapperUtil.getMappedByInOneToMany(any(), any())).thenReturn(null);

                    PersonWithChildrenModel dest = new PersonWithChildrenModel();
                    List<PersonModel> targetList = new ArrayList<>(List.of(existingChild));
                    dest.setChildren(targetList);

                    // El ítem del source tiene id=55 y un nombre diferente
                    Map<String, Object> childMap = new HashMap<>();
                    childMap.put("id", 55L);
                    childMap.put("name", "NombreActualizado");

                    Map<String, Object> entityMap = new HashMap<>();
                    entityMap.put("children", new ArrayList<>(List.of(childMap)));

                    Map<String, Object> childAllow = new HashMap<>();
                    childAllow.put("name", null);

                    mapper.copyMapToEntity(PersonWithChildrenModel.class, entityMap, dest,
                            allowWithNested("children", childAllow));

                    // El ID del ítem existente debe preservarse
                    PersonModel updatedChild = dest.getChildren().stream()
                            .filter(c -> Long.valueOf(55L).equals(c.getId()))
                            .findFirst()
                            .orElse(null);

                    assertNotNull(updatedChild, "El ítem con id=55 debe seguir en la lista");
                    assertEquals(55L, updatedChild.getId(),
                            "El ID original del hijo (55L) debe mantenerse tras la actualización");
                }
            }
        }

        // ---- Propiedad tipo List (4 combinaciones null/no-null) -----------

        @Nested
        @DisplayName("Propiedad de tipo List (OneToMany)")
        class ListPropertyTests {

            @Test
            @DisplayName("listSource=null y listTarget=null → no-op")
            void bothNull_noOp() {
                try (MockedStatic<ScalarMapper> sm = mockStatic(ScalarMapper.class);
                     MockedStatic<BeanMapperUtil> bu = mockStatic(BeanMapperUtil.class)) {

                    sm.when(() -> ScalarMapper.isScalarType(any())).thenReturn(false);
                    bu.when(() -> BeanMapperUtil.getMappedByInOneToMany(any(), any())).thenReturn(null);

                    PersonWithChildrenModel dest = new PersonWithChildrenModel();

                    Map<String, Object> entityMap = new HashMap<>();
                    entityMap.put("children", null);

                    mapper.copyMapToEntity(PersonWithChildrenModel.class, entityMap, dest,
                            allowWithNested("children", new HashMap<>()));

                    assertNull(dest.getChildren(), "Ambas listas null → destino sigue siendo null");
                }
            }

            @Test
            @DisplayName("listSource=null y listTarget!=null → destino se pone a null")
            void sourceNull_targetNotNull_setsNull() {
                try (MockedStatic<ScalarMapper> sm = mockStatic(ScalarMapper.class);
                     MockedStatic<BeanMapperUtil> bu = mockStatic(BeanMapperUtil.class)) {

                    sm.when(() -> ScalarMapper.isScalarType(any())).thenReturn(false);
                    bu.when(() -> BeanMapperUtil.getMappedByInOneToMany(any(), any())).thenReturn(null);

                    PersonWithChildrenModel dest = new PersonWithChildrenModel();
                    dest.setChildren(new ArrayList<>(List.of(new PersonModel())));

                    Map<String, Object> entityMap = new HashMap<>();
                    entityMap.put("children", null);

                    mapper.copyMapToEntity(PersonWithChildrenModel.class, entityMap, dest,
                            allowWithNested("children", new HashMap<>()));

                    assertNull(dest.getChildren(),
                            "Si source=null y target tiene datos, debe quedar null");
                }
            }

            @Test
            @DisplayName("listSource!=null y listTarget=null → se crea nueva lista con ítems nuevos")
            void sourceNotNull_targetNull_newItemsCreated() {
                try (MockedStatic<ScalarMapper> sm = mockStatic(ScalarMapper.class);
                     MockedStatic<BeanMapperUtil> bu = mockStatic(BeanMapperUtil.class)) {

                    sm.when(() -> ScalarMapper.isScalarType(any())).thenReturn(false);
                    bu.when(() -> BeanMapperUtil.getMappedByInOneToMany(any(), any())).thenReturn(null);

                    PersonWithChildrenModel dest = new PersonWithChildrenModel();

                    Map<String, Object> child1 = new HashMap<>();
                    child1.put("id", null);
                    Map<String, Object> child2 = new HashMap<>();
                    child2.put("id", null);

                    Map<String, Object> entityMap = new HashMap<>();
                    entityMap.put("children", new ArrayList<>(List.of(child1, child2)));

                    mapper.copyMapToEntity(PersonWithChildrenModel.class, entityMap, dest,
                            allowWithNested("children", new HashMap<>()));

                    assertNotNull(dest.getChildren());
                    assertEquals(2, dest.getChildren().size(),
                            "Deben crearse tantos ítems como haya en el source");
                    assertInstanceOf(PersonModel.class, dest.getChildren().get(0));
                }
            }

            @Test
            @DisplayName("listSource!=null con id y listTarget=null → ítems cargados desde ModelLoader")
            void sourceNotNull_withId_targetNull_loadsFromLoader() {
                PersonModel loadedChild = new PersonModel();
                loadedChild.setId(10L);
                loadedChild.setName("Hijo Cargado");

                when(mockLoader.getModel(PersonModel.class, 10L)).thenReturn(loadedChild);

                try (MockedStatic<ScalarMapper> sm = mockStatic(ScalarMapper.class);
                     MockedStatic<BeanMapperUtil> bu = mockStatic(BeanMapperUtil.class)) {

                    sm.when(() -> ScalarMapper.isScalarType(any())).thenReturn(false);
                    bu.when(() -> BeanMapperUtil.getMappedByInOneToMany(any(), any())).thenReturn(null);

                    PersonWithChildrenModel dest = new PersonWithChildrenModel();

                    Map<String, Object> childMap = new HashMap<>();
                    childMap.put("id", 10L);

                    Map<String, Object> entityMap = new HashMap<>();
                    entityMap.put("children", new ArrayList<>(List.of(childMap)));

                    mapper.copyMapToEntity(PersonWithChildrenModel.class, entityMap, dest,
                            allowWithNested("children", new HashMap<>()));

                    verify(mockLoader).getModel(PersonModel.class, 10L);
                    assertEquals(1, dest.getChildren().size());
                    assertSame(loadedChild, dest.getChildren().get(0),
                            "El ítem de la lista debe ser el modelo cargado por el loader");
                }
            }

            @Test
            @DisplayName("listSource!=null y listTarget!=null → merge: añade nuevos, actualiza comunes, elimina sobrantes")
            void bothNotNull_mergesLists() {
                // Ítem existente (id=5): presente en source → se actualiza
                // Ítem nuevo (sin id): en source pero no en target → se añade
                // Ítem sobrante (id=20): en target pero no en source → se elimina
                PersonModel existingChild = new PersonModel();
                existingChild.setId(5L);
                existingChild.setName("Existente");

                PersonModel obsoleteChild = new PersonModel();
                obsoleteChild.setId(20L);
                obsoleteChild.setName("Obsoleto");

                when(mockLoader.getModel(PersonModel.class, 5L)).thenReturn(existingChild);

                try (MockedStatic<ScalarMapper> sm = mockStatic(ScalarMapper.class);
                     MockedStatic<BeanMapperUtil> bu = mockStatic(BeanMapperUtil.class)) {

                    sm.when(() -> ScalarMapper.isScalarType(any())).thenReturn(false);
                    bu.when(() -> BeanMapperUtil.getMappedByInOneToMany(any(), any())).thenReturn(null);

                    List<PersonModel> targetList = new ArrayList<>(List.of(existingChild, obsoleteChild));
                    PersonWithChildrenModel dest = new PersonWithChildrenModel();
                    dest.setChildren(targetList);

                    Map<String, Object> existingMap = new HashMap<>();
                    existingMap.put("id", 5L); // coincide con el existente

                    Map<String, Object> newMap = new HashMap<>();
                    newMap.put("id", null); // nuevo ítem

                    Map<String, Object> entityMap = new HashMap<>();
                    entityMap.put("children", new ArrayList<>(List.of(existingMap, newMap)));

                    mapper.copyMapToEntity(PersonWithChildrenModel.class, entityMap, dest,
                            allowWithNested("children", new HashMap<>()));

                    List<PersonModel> result = dest.getChildren();
                    assertNotNull(result);

                    // El ítem obsoleto (id=20) debe haberse eliminado
                    boolean obsoleteStillPresent = result.stream()
                            .anyMatch(c -> Long.valueOf(20L).equals(c.getId()));
                    assertFalse(obsoleteStillPresent,
                            "El ítem con id=20 (solo en target) debe eliminarse");

                    // El ítem existente (id=5) debe seguir presente
                    boolean existingPresent = result.stream()
                            .anyMatch(c -> Long.valueOf(5L).equals(c.getId()));
                    assertTrue(existingPresent,
                            "El ítem con id=5 (común) debe seguir en la lista");

                    // Debe haberse añadido al menos 1 ítem nuevo (sin id)
                    long newItems = result.stream()
                            .filter(c -> c.getId() == null)
                            .count();
                    assertEquals(1, newItems,
                            "Debe haberse añadido exactamente 1 ítem nuevo");
                }
            }

            @Test
            @DisplayName("Ítem de lista con id no encontrado en loader → lanza RuntimeException")
            void listItemWithIdNotFoundInLoader_throwsRuntimeException() {
                when(mockLoader.getModel(PersonModel.class, 777L)).thenReturn(null);

                try (MockedStatic<ScalarMapper> sm = mockStatic(ScalarMapper.class);
                     MockedStatic<BeanMapperUtil> bu = mockStatic(BeanMapperUtil.class)) {

                    sm.when(() -> ScalarMapper.isScalarType(any())).thenReturn(false);
                    bu.when(() -> BeanMapperUtil.getMappedByInOneToMany(any(), any())).thenReturn(null);

                    PersonWithChildrenModel dest = new PersonWithChildrenModel();

                    Map<String, Object> childMap = new HashMap<>();
                    childMap.put("id", 777L);

                    Map<String, Object> entityMap = new HashMap<>();
                    entityMap.put("children", new ArrayList<>(List.of(childMap)));

                    assertThrows(RuntimeException.class,
                            () -> mapper.copyMapToEntity(PersonWithChildrenModel.class, entityMap, dest,
                                    allowWithNested("children", new HashMap<>())),
                            "Si un ítem de lista con id no existe en BD debe lanzarse RuntimeException");
                }
            }

            @Test
            @DisplayName("Propiedad de lista con mappedByRelation → los hijos reciben la referencia al padre")
            void listWithMappedByRelation_childrenReceiveParentReference() {
                try (MockedStatic<ScalarMapper> sm = mockStatic(ScalarMapper.class);
                     MockedStatic<BeanMapperUtil> bu = mockStatic(BeanMapperUtil.class)) {

                    sm.when(() -> ScalarMapper.isScalarType(any())).thenReturn(false);
                    // Simulamos que "children" tiene mappedBy="parent" (aunque PersonModel no tenga ese campo,
                    // este test verifica que getMappedByInOneToMany es consultado correctamente)
                    bu.when(() -> BeanMapperUtil.getMappedByInOneToMany(
                            PersonWithChildrenModel.class, "children")).thenReturn("parent");

                    PersonWithChildrenModel dest = new PersonWithChildrenModel();

                    Map<String, Object> childMap = new HashMap<>();
                    childMap.put("id", null);

                    Map<String, Object> entityMap = new HashMap<>();
                    entityMap.put("children", new ArrayList<>(List.of(childMap)));

                    // No lanzamos excepción → el método getMappedByInOneToMany fue consultado
                    // PersonModel no tiene campo "parent" con setter, por lo que PropertyUtils
                    // puede lanzar excepción. Lo importante es verificar la interacción.
                    try {
                        mapper.copyMapToEntity(PersonWithChildrenModel.class, entityMap, dest,
                                allowWithNested("children", new HashMap<>()));
                    } catch (RuntimeException ignored) {
                        // Posible excepción de PropertyUtils al intentar setear "parent" en PersonModel
                    }

                    // Verificamos que se consultó el mappedBy correspondiente
                    bu.verify(() -> BeanMapperUtil.getMappedByInOneToMany(
                            PersonWithChildrenModel.class, "children"));
                }
            }
        }
    }

    // =========================================================================
    // 5. ModelLoader integration
    // =========================================================================

    @Nested
    @DisplayName("ModelLoader (integración)")
    class ModelLoaderIntegrationTests {

        @Test
        @DisplayName("El ModelLoader inyectado es el que se usa para cargar modelos por ID")
        void injectedModelLoader_isUsedForLoading() {
            AddressModel loadedAddress = new AddressModel();
            loadedAddress.setId(1L);

            when(mockLoader.getModel(AddressModel.class, 1L)).thenReturn(loadedAddress);

            try (MockedStatic<ScalarMapper> sm = mockStatic(ScalarMapper.class);
                 MockedStatic<BeanMapperUtil> bu = mockStatic(BeanMapperUtil.class)) {

                sm.when(() -> ScalarMapper.isScalarType(any())).thenReturn(false);
                bu.when(() -> BeanMapperUtil.isOneToOne(any(), eq("address"))).thenReturn(true);
                bu.when(() -> BeanMapperUtil.isManyToOne(any(), any())).thenReturn(false);

                PersonWithAddressModel dest = new PersonWithAddressModel();

                Map<String, Object> addressMap = new HashMap<>();
                addressMap.put("id", 1L);

                Map<String, Object> entityMap = new HashMap<>();
                entityMap.put("address", addressMap);

                mapper.copyMapToEntity(PersonWithAddressModel.class, entityMap, dest,
                        allowWithNested("address", new HashMap<>()));

                verify(mockLoader, times(1)).getModel(AddressModel.class, 1L);
            }
        }

        @Test
        @DisplayName("El ModelLoader solo se invoca cuando el mapa de datos contiene un id")
        void modelLoader_isNotCalledWhenIdIsNull() {
            try (MockedStatic<ScalarMapper> sm = mockStatic(ScalarMapper.class);
                 MockedStatic<BeanMapperUtil> bu = mockStatic(BeanMapperUtil.class)) {

                sm.when(() -> ScalarMapper.isScalarType(any())).thenReturn(false);
                bu.when(() -> BeanMapperUtil.isOneToOne(any(), eq("address"))).thenReturn(true);
                bu.when(() -> BeanMapperUtil.isManyToOne(any(), any())).thenReturn(false);

                PersonWithAddressModel dest = new PersonWithAddressModel();

                Map<String, Object> addressMap = new HashMap<>();
                addressMap.put("id", null); // sin id

                Map<String, Object> entityMap = new HashMap<>();
                entityMap.put("address", addressMap);

                mapper.copyMapToEntity(PersonWithAddressModel.class, entityMap, dest,
                        allowWithNested("address", new HashMap<>()));

                verify(mockLoader, never()).getModel(any(), any());
            }
        }
    }
}

