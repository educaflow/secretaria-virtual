package com.educaflow.base.infrastructure.mapper;

import com.axelor.db.Model;
import com.educaflow.base.infrastructure.junit.JUnitHelper;
import com.axelor.db.modelservice.AllowProperties;
import com.axelor.meta.db.MetaFile;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class BeanMapperModelTest {

    @Test
    void getEntityCloned_shouldReturnNull_whenEntityIsNull() {
        BeanMapperModel mapper = new BeanMapperModel(new FakeModelLoader());

        Object cloned = mapper.getEntityCloned(ParentModel.class, null);

        assertNull(cloned);
    }

    @Test
    void getEntityCloned_shouldCreateNewInstance_whenEntityIsNotNull() {
        BeanMapperModel mapper = new BeanMapperModel(new FakeModelLoader());
        ParentModel source = new ParentModel();
        source.setId(12L);
        source.setName("origen");

        ParentModel cloned = (ParentModel) mapper.getEntityCloned(ParentModel.class, source);

        assertNotNull(cloned);
        assertNotSame(source, cloned);
        assertEquals(source.getId(), cloned.getId());
        assertEquals(source.getName(), cloned.getName());
    }

    @Test
    void getEntityCloned_shouldCloneNestedClassesWithAllPropertiesSet() {
        BeanMapperModel mapper = new BeanMapperModel(new FakeModelLoader());

        // RefModel (clase anidada de segundo nivel con todas las propiedades rellenas)
        RefModel ref = new RefModel();
        ref.setId(1L);
        ref.setCode("REF-001");

        // ParentModel raíz con todas las propiedades escalares rellenas
        ParentModel parent = new ParentModel();
        parent.setId(10L);
        parent.setName("Padre Principal");
        parent.setAge(42);
        parent.setRef(ref);

        // ChildModels (clases anidadas en lista) con todas sus propiedades rellenas
        ChildModel child1 = new ChildModel();
        child1.setId(100L);
        child1.setName("Hijo Primero");
        child1.setParent(parent);

        ChildModel child2 = new ChildModel();
        child2.setId(101L);
        child2.setName("Hijo Segundo");
        child2.setParent(parent);

        parent.setChildren(new ArrayList<>(List.of(child1, child2)));

        // Clonar el árbol completo
        ParentModel cloned = (ParentModel) mapper.getEntityCloned(ParentModel.class, parent);

        // Nivel 1: ParentModel clonado con todos sus escalares
        assertNotSame(parent, cloned);
        assertEquals(10L, cloned.getId());
        assertEquals("Padre Principal", cloned.getName());
        assertEquals(42, cloned.getAge());

        // Nivel 2a: RefModel clonado (clase anidada directa) con todos sus campos
        assertNotNull(cloned.getRef());
        assertNotSame(ref, cloned.getRef());
        assertEquals(1L, cloned.getRef().getId());
        assertEquals("REF-001", cloned.getRef().getCode());

        // Nivel 2b: Lista de ChildModels clonados
        assertNotNull(cloned.getChildren());
        assertNotSame(parent.getChildren(), cloned.getChildren());
        assertEquals(2, cloned.getChildren().size());

        ChildModel clonedChild1 = cloned.getChildren().get(0);
        assertNotSame(child1, clonedChild1);
        assertEquals(100L, clonedChild1.getId());
        assertEquals("Hijo Primero", clonedChild1.getName());
        // La referencia de vuelta al padre (mappedBy) apunta al padre CLONADO, no al original
        assertSame(cloned, clonedChild1.getParent());

        ChildModel clonedChild2 = cloned.getChildren().get(1);
        assertNotSame(child2, clonedChild2);
        assertEquals(101L, clonedChild2.getId());
        assertEquals("Hijo Segundo", clonedChild2.getName());
        assertSame(cloned, clonedChild2.getParent());
    }

    @Test
    void getEntityCloned_shouldHandleCycleWhenNestedObjectReferencesUpperObject() {
        BeanMapperModel mapper = new BeanMapperModel(new FakeModelLoader());

        ParentModel parent = new ParentModel();
        parent.setId(200L);
        parent.setName("Padre cíclico");
        parent.setAge(50);

        ChildModel child = new ChildModel();
        child.setId(201L);
        child.setName("Hijo cíclico");
        child.setParent(parent);

        parent.setChildren(new ArrayList<>(List.of(child)));

        ParentModel cloned = assertDoesNotThrow(
                () -> (ParentModel) mapper.getEntityCloned(ParentModel.class, parent),
                "El clonado de una relación cíclica padre-hijo-padre no debe provocar recursión infinita"
        );

        assertNotSame(parent, cloned);
        assertNotNull(cloned.getChildren());
        assertEquals(1, cloned.getChildren().size());

        ChildModel clonedChild = cloned.getChildren().get(0);
        assertNotSame(child, clonedChild);
        assertEquals(201L, clonedChild.getId());
        assertEquals("Hijo cíclico", clonedChild.getName());

        assertSame(cloned, clonedChild.getParent(),
                "La referencia hacia arriba del hijo debe apuntar al padre clonado");
        assertNotSame(parent, clonedChild.getParent(),
                "La referencia cíclica no debe apuntar al padre original");
        assertSame(cloned.getChildren(), clonedChild.getParent().getChildren(),
                "El ciclo debe cerrarse sobre el árbol clonado");
        assertTrue(clonedChild.getParent().getChildren().contains(clonedChild),
                "El hijo clonado debe pertenecer a la colección del padre clonado");
    }

    @Test
    void getEntityCloned_mismaEntidadReferenciadaDosVeces_ambasReferenciasApuntanAlMismoClon() {
        BeanMapperModel mapper = new BeanMapperModel(new FakeModelLoader());

        ParentModel parent = new ParentModel();
        parent.setId(300L);

        ChildModel child = new ChildModel();
        child.setId(301L);
        child.setName("Hijo repetido");
        child.setParent(parent);

        parent.setChildren(new ArrayList<>(List.of(child, child)));

        ParentModel cloned = (ParentModel) mapper.getEntityCloned(ParentModel.class, parent);

        ChildModel primero = cloned.getChildren().get(0);
        ChildModel segundo = cloned.getChildren().get(1);
        assertNotSame(child, primero);
        assertNotSame(child, segundo);
        assertSame(primero, segundo);
    }

    @Test
    void copyEntityToEntity_shouldBeNoOp_whenAllowPropertiesIsNull() {
        BeanMapperModel mapper = new BeanMapperModel(new FakeModelLoader());
        ParentModel source = new ParentModel();
        source.setName("origen");
        source.setAge(30);

        ParentModel target = new ParentModel();
        target.setName("destino");
        target.setAge(99);

        mapper.copyEntityToEntity(ParentModel.class, source, target, null);

        assertEquals("destino", target.getName());
        assertEquals(99, target.getAge());
    }

    @Test
    void copyEntityToEntity_shouldCopyOnlyAllowedScalarProperties() {
        BeanMapperModel mapper = new BeanMapperModel(new FakeModelLoader());
        ParentModel source = new ParentModel();
        source.setName("nuevo");
        source.setAge(40);

        ParentModel target = new ParentModel();
        target.setName("anterior");
        target.setAge(10);

        Map<String, Object> allowProperties = mapOf(
                "name", true
        );

        mapper.copyEntityToEntity(ParentModel.class, source, target, AllowProperties.createAllowProperties(allowProperties));

        assertEquals("nuevo", target.getName());
        assertEquals(10, target.getAge());
    }

    @Test
    void copyEntityToEntity_shouldNotSetMappedByRelation_whenNotAllowed() {
        BeanMapperModel mapper = new BeanMapperModel(new FakeModelLoader());
        ChildModel source = new ChildModel();
        source.setName("hijo");

        ChildModel target = new ChildModel();
        ParentModel parent = new ParentModel();
        Map<String, Object> allowProperties = mapOf(
                "name", true
        );

        mapper.copyEntityToEntity(ChildModel.class, source, target, AllowProperties.createAllowProperties(allowProperties), "parent", parent);

        assertNull(target.getParent());
        assertEquals("hijo", target.getName());
    }

    @Test
    void copyEntityToEntity_shouldSetMappedByRelation_whenMappedByIsAllowed() {
        BeanMapperModel mapper = new BeanMapperModel(new FakeModelLoader());
        ChildModel source = new ChildModel();
        source.setName("hijo");

        ChildModel target = new ChildModel();
        ParentModel parent = new ParentModel();
        Map<String, Object> allowProperties = mapOf(
                "name", true,
                "parent", true
        );

        mapper.copyEntityToEntity(ChildModel.class, source, target, AllowProperties.createAllowProperties(allowProperties), "parent", parent);

        assertSame(parent, target.getParent());
        assertEquals("hijo", target.getName());
    }

    @Test
    void copyMapToEntity_shouldMapScalarsAndReconcileOneToManyList() {
        BeanMapperModel mapper = new BeanMapperModel(new FakeModelLoader());

        ParentModel target = new ParentModel();
        target.setId(500L);
        target.setName("anterior");

        ChildModel existingChild = child(1L, "old-1", target);
        ChildModel removedChild = child(2L, "old-2", target);
        List<ChildModel> targetChildren = new ArrayList<>();
        targetChildren.add(existingChild);
        targetChildren.add(removedChild);
        target.setChildren(targetChildren);

        List<Object> childrenMap = new ArrayList<>();
        childrenMap.add(mapOf("id", 1L, "name", "updated-1", "parent", new HashMap<>()));  // id=1L hace match con existingChild
        childrenMap.add(mapOf("name", "new-child", "parent", new HashMap<>()));

        Map<String, Object> source = mapOf(
                "name", "nuevo",
                "children", childrenMap
        );

        Map<String, Object> allowProperties = mapOf(
                "name", true,
                "children", mapOf(
                        "name", true,
                        "parent", true
                )
        );

        mapper.copyMapToEntity(ParentModel.class, source, target, AllowProperties.createAllowProperties(allowProperties));

        assertEquals("nuevo", target.getName());
        assertEquals(2, target.getChildren().size());

        ChildModel updated = target.getChildren().stream().filter(c -> Long.valueOf(1L).equals(c.getId())).findFirst().orElseThrow();
        ChildModel created = target.getChildren().stream().filter(c -> c.getId() == null).findFirst().orElseThrow();

        // El id del modelo destino se conserva siempre (copyValueToEntityAndNoChangeId lo restaura).
        assertEquals(1L, updated.getId());
        assertEquals("updated-1", updated.getName());
        assertSame(target, updated.getParent());

        assertEquals("new-child", created.getName());
        assertSame(target, created.getParent());
        assertTrue(target.getChildren().stream().noneMatch(c -> Long.valueOf(2L).equals(c.getId())));
    }

    @Test
    void copyMapToEntity_listaConLosElementosExistentesEnOtroOrden_emparejaCadaUnoPorSuId() {
        BeanMapperModel mapper = new BeanMapperModel(new FakeModelLoader());

        ParentModel target = new ParentModel();
        target.setId(500L);
        target.setChildren(new ArrayList<>(List.of(child(1L, "old-1", target), child(2L, "old-2", target))));

        List<Object> childrenMap = new ArrayList<>();
        childrenMap.add(mapOf("id", 2L, "name", "updated-2"));
        childrenMap.add(mapOf("id", 1L, "name", "updated-1"));

        Map<String, Object> source = mapOf("children", childrenMap);
        Map<String, Object> allowProperties = mapOf("children", mapOf("name", true));

        mapper.copyMapToEntity(ParentModel.class, source, target, AllowProperties.createAllowProperties(allowProperties));

        ChildModel hijo1 = target.getChildren().stream().filter(c -> Long.valueOf(1L).equals(c.getId())).findFirst().orElseThrow();
        ChildModel hijo2 = target.getChildren().stream().filter(c -> Long.valueOf(2L).equals(c.getId())).findFirst().orElseThrow();
        assertEquals("updated-1", hijo1.getName());
        assertEquals("updated-2", hijo2.getName());
    }

    @Test
    void copyMapToEntity_shouldLoadExistingModelById_whenTargetRelationIsNull() {
        FakeModelLoader loader = new FakeModelLoader();
        RefModel loaded = new RefModel();
        loaded.setId(77L);
        loaded.setCode("from-db");
        loader.register(RefModel.class, 77L, loaded);

        BeanMapperModel mapper = new BeanMapperModel(loader);
        ParentModel target = new ParentModel();
        target.setRef(null);

        Map<String, Object> source = mapOf(
                "ref", mapOf("id", 77L, "code", "from-map")
        );
        Map<String, Object> allowProperties = mapOf(
                "ref", mapOf("code", true)
        );

        mapper.copyMapToEntity(ParentModel.class, source, target, AllowProperties.createAllowProperties(allowProperties));

        assertSame(loaded, target.getRef());
        assertEquals(77L, target.getRef().getId());
        assertEquals("from-map", target.getRef().getCode());
        assertEquals(1, loader.callCount(RefModel.class, 77L));
    }

    @Test
    void copyMapToEntity_shouldNullifyRelation_whenSourceHasNullAndTargetHasValue() {
        BeanMapperModel mapper = new BeanMapperModel(new FakeModelLoader());
        ParentModel target = new ParentModel();
        target.setRef(new RefModel());

        Map<String, Object> source = mapOf(
                "ref", null
        );
        Map<String, Object> allowProperties = mapOf(
                "ref", mapOf("code", true)
        );

        mapper.copyMapToEntity(ParentModel.class, source, target, AllowProperties.createAllowProperties(allowProperties));

        assertNull(target.getRef());
    }

    @Test
    void copyMapToEntity_shouldIgnorePropertiesOutsideAllowList() {
        BeanMapperModel mapper = new BeanMapperModel(new FakeModelLoader());
        ParentModel target = new ParentModel();
        target.setName("old");
        target.setAge(15);

        Map<String, Object> source = mapOf(
                "name", "new-name",
                "age", 99
        );
        Map<String, Object> allowProperties = mapOf(
                "name", true
        );

        mapper.copyMapToEntity(ParentModel.class, source, target, AllowProperties.createAllowProperties(allowProperties));

        assertEquals("new-name", target.getName());
        assertEquals(15, target.getAge());
    }

    @Test
    void copyMapToEntity_shouldThrow_whenModelFieldHasNoRelationAnnotation() {
        BeanMapperModel mapper = new BeanMapperModel(new FakeModelLoader());
        UnsupportedHolderModel target = new UnsupportedHolderModel();

        Map<String, Object> source = mapOf(
                "ref", mapOf("code", "x")
        );
        Map<String, Object> allowProperties = mapOf(
                "ref", mapOf("code", true)
        );

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> mapper.copyMapToEntity(UnsupportedHolderModel.class, source, target, AllowProperties.createAllowProperties(allowProperties)));

        assertTrue(ex.getMessage().contains(UnsupportedHolderModel.class.getName()));
        JUnitHelper.assertThrowsCause(RuntimeException.class,
                () -> mapper.copyMapToEntity(UnsupportedHolderModel.class, source, target, AllowProperties.createAllowProperties(allowProperties)));
    }

    @Test
    void copyMapToEntity_shouldThrow_whenLoaderDoesNotFindModelForId() {
        BeanMapperModel mapper = new BeanMapperModel(new FakeModelLoader());
        ParentModel target = new ParentModel();

        Map<String, Object> source = mapOf(
                "ref", mapOf("id", 999L, "code", "x")
        );
        Map<String, Object> allowProperties = mapOf(
                "ref", mapOf("code", true)
        );

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> mapper.copyMapToEntity(ParentModel.class, source, target, AllowProperties.createAllowProperties(allowProperties)));

        assertTrue(ex.getMessage().contains(ParentModel.class.getName()));
    }

    @Test
    void copyMapToEntity_manyToOneConIdDistinto_sustituyeLaReferencia() {
        RefModel anterior = ref(1L, "actual");
        RefModel cargado = ref(2L, "from-db");
        FakeModelLoader loader = new FakeModelLoader();
        loader.register(RefModel.class, 2L, cargado);

        BeanMapperModel mapper = new BeanMapperModel(loader);
        MtoHolderModel target = new MtoHolderModel();
        target.setRef(anterior);

        Map<String, Object> source = mapOf(
                "ref", mapOf("id", 2L, "code", "from-map")
        );
        Map<String, Object> allowProperties = mapOf(
                "ref", mapOf("code", true)
        );

        mapper.copyMapToEntity(MtoHolderModel.class, source, target, AllowProperties.createAllowProperties(allowProperties));

        assertSame(cargado, target.getRef());
        assertEquals(2L, target.getRef().getId());
        assertEquals("actual", anterior.getCode(),
                "La entidad que solo estaba referenciada no debe recibir los valores del mapa del cliente");
        assertEquals(1, loader.callCount(RefModel.class, 2L));
    }

    @Test
    void copyMapToEntity_manyToOneConMismoId_conservaLaReferenciaYCopiaCamposPermitidos() {
        RefModel referenciaActual = ref(1L, "actual");
        FakeModelLoader loader = new FakeModelLoader();

        BeanMapperModel mapper = new BeanMapperModel(loader);
        MtoHolderModel target = new MtoHolderModel();
        target.setRef(referenciaActual);

        Map<String, Object> source = mapOf(
                "ref", mapOf("id", 1L, "code", "from-map")
        );
        Map<String, Object> allowProperties = mapOf(
                "ref", mapOf("code", true)
        );

        mapper.copyMapToEntity(MtoHolderModel.class, source, target, AllowProperties.createAllowProperties(allowProperties));

        assertSame(referenciaActual, target.getRef());
        assertEquals("from-map", target.getRef().getCode());
        assertEquals(1L, target.getRef().getId());
        assertEquals(0, loader.callCount(RefModel.class, 1L),
                "Con el mismo id no hace falta consultar la base de datos");
    }

    @Test
    void copyMapToEntity_manyToOneConIdDistintoYMapaInternoVacio_sustituyeIgualmente() {
        RefModel anterior = ref(1L, "actual");
        RefModel cargado = ref(2L, "from-db");
        FakeModelLoader loader = new FakeModelLoader();
        loader.register(RefModel.class, 2L, cargado);

        BeanMapperModel mapper = new BeanMapperModel(loader);
        MtoHolderModel target = new MtoHolderModel();
        target.setRef(anterior);

        Map<String, Object> source = mapOf(
                "ref", mapOf("id", 2L, "code", "from-map")
        );
        Map<String, Object> allowProperties = mapOf(
                "ref", Map.of()
        );

        mapper.copyMapToEntity(MtoHolderModel.class, source, target, AllowProperties.createAllowProperties(allowProperties));

        assertSame(cargado, target.getRef(),
                "Quien autoriza la propiedad es la entrada externa de la whitelist, no su mapa interno");
        assertEquals(2L, target.getRef().getId());
        assertEquals("from-db", target.getRef().getCode());
        assertEquals("actual", anterior.getCode());
    }

    @Test
    void copyMapToEntity_rawValueSinId_conservaLaReferenciaActual() {
        RefModel referenciaActual = ref(1L, "actual");
        FakeModelLoader loader = new FakeModelLoader();

        BeanMapperModel mapper = new BeanMapperModel(loader);
        MtoHolderModel target = new MtoHolderModel();
        target.setRef(referenciaActual);

        Map<String, Object> source = mapOf(
                "ref", mapOf("code", "from-map")
        );
        Map<String, Object> allowProperties = mapOf(
                "ref", mapOf("code", true)
        );

        mapper.copyMapToEntity(MtoHolderModel.class, source, target, AllowProperties.createAllowProperties(allowProperties));

        assertSame(referenciaActual, target.getRef());
        assertEquals("from-map", target.getRef().getCode());
        assertEquals(1L, target.getRef().getId());
        assertEquals(0, loader.callCount(RefModel.class, 1L));
    }

    @Test
    void copyMapToEntity_metaFileConIdDistinto_sigueSustituyendoLaReferencia() {
        MetaFile anterior = new MetaFile();
        anterior.setId(10L);
        anterior.setFileName("anterior.pdf");

        MetaFile cargado = new MetaFile();
        cargado.setId(11L);
        cargado.setFileName("cargado.pdf");

        FakeModelLoader loader = new FakeModelLoader();
        loader.register(MetaFile.class, 11L, cargado);

        BeanMapperModel mapper = new BeanMapperModel(loader);
        MtoHolderModel target = new MtoHolderModel();
        target.setFichero(anterior);

        Map<String, Object> source = mapOf(
                "fichero", mapOf("id", 11L)
        );
        Map<String, Object> allowProperties = mapOf(
                "fichero", Map.of()
        );

        mapper.copyMapToEntity(MtoHolderModel.class, source, target, AllowProperties.createAllowProperties(allowProperties));

        assertSame(cargado, target.getFichero());
        assertEquals(11L, target.getFichero().getId());
        assertEquals(10L, anterior.getId());
        assertEquals("anterior.pdf", anterior.getFileName());
    }

    @Test
    void copyMapToEntity_valueDestNulo_cargaLaReferenciaPorIdYCopiaCamposPermitidos() {
        RefModel cargado = ref(77L, "from-db");
        FakeModelLoader loader = new FakeModelLoader();
        loader.register(RefModel.class, 77L, cargado);

        BeanMapperModel mapper = new BeanMapperModel(loader);
        MtoHolderModel target = new MtoHolderModel();
        target.setRef(null);

        Map<String, Object> source = mapOf(
                "ref", mapOf("id", 77L, "code", "from-map")
        );
        Map<String, Object> allowProperties = mapOf(
                "ref", mapOf("code", true)
        );

        mapper.copyMapToEntity(MtoHolderModel.class, source, target, AllowProperties.createAllowProperties(allowProperties));

        assertSame(cargado, target.getRef());
        assertEquals(77L, target.getRef().getId());
        assertEquals("from-map", target.getRef().getCode());
        assertEquals(1, loader.callCount(RefModel.class, 77L));
    }

    @Test
    void copyMapToEntity_manyToOneConIdDistintoYWhitelistAllowAll_sustituyeYNoMutaLaReferenciaAnterior() {
        RefModel anterior = ref(1L, "actual");
        RefModel cargado = ref(2L, "from-db");
        FakeModelLoader loader = new FakeModelLoader();
        loader.register(RefModel.class, 2L, cargado);

        BeanMapperModel mapper = new BeanMapperModel(loader);
        MtoHolderModel target = new MtoHolderModel();
        target.setRef(anterior);

        Map<String, Object> source = mapOf(
                "ref", mapOf("id", 2L, "code", "from-map")
        );

        mapper.copyMapToEntity(MtoHolderModel.class, source, target, AllowProperties.createAllowAllProperties());

        assertSame(cargado, target.getRef());
        assertEquals(2L, target.getRef().getId());
        assertEquals("actual", anterior.getCode(),
                "Ni con la whitelist abierta el mapa del cliente edita la entidad que solo se referencia");
        assertEquals("from-db", target.getRef().getCode(),
                "La rama de sustitución no copia campos sobre la entidad cargada");
    }

    @Test
    void copyMapToEntity_idEntranteInexistente_lanzaRuntimeException() {
        RefModel referenciaActual = ref(1L, "actual");
        FakeModelLoader loader = new FakeModelLoader();

        BeanMapperModel mapper = new BeanMapperModel(loader);
        MtoHolderModel target = new MtoHolderModel();
        target.setRef(referenciaActual);

        Map<String, Object> source = mapOf(
                "ref", mapOf("id", 999L, "code", "x")
        );
        Map<String, Object> allowProperties = mapOf(
                "ref", mapOf("code", true)
        );

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> mapper.copyMapToEntity(MtoHolderModel.class, source, target, AllowProperties.createAllowProperties(allowProperties)));

        assertTrue(ex.getMessage().contains(MtoHolderModel.class.getName()));

        assertTrue(algunaCausaContiene(ex, "999"), "Alguna causa encadenada debe mencionar el id 999 que no se encontró");

        assertSame(referenciaActual, target.getRef(), "El destino no debe quedar con una referencia a medias");
        assertEquals("actual", referenciaActual.getCode());
    }

    @Test
    void copyMapToEntity_oneToOneRealConIdDistinto_sustituyeLaReferenciaYNoMutaLaAnterior() {
        RefModel anterior = ref(1L, "actual");
        RefModel cargado = ref(2L, "from-db");
        FakeModelLoader loader = new FakeModelLoader();
        loader.register(RefModel.class, 2L, cargado);

        BeanMapperModel mapper = new BeanMapperModel(loader);
        ParentModel target = new ParentModel();
        target.setRef(anterior);

        Map<String, Object> source = mapOf(
                "ref", mapOf("id", 2L, "code", "from-map")
        );
        Map<String, Object> allowProperties = mapOf(
                "ref", mapOf("code", true)
        );

        mapper.copyMapToEntity(ParentModel.class, source, target, AllowProperties.createAllowProperties(allowProperties));

        assertSame(cargado, target.getRef());
        assertNotSame(anterior, target.getRef());
        assertEquals(2L, target.getRef().getId());
        assertEquals("from-db", target.getRef().getCode());
        assertEquals("actual", anterior.getCode());
        assertEquals(1, loader.callCount(RefModel.class, 2L));
    }

    @Test
    void copyMapToEntity_escalarNoConvertible_lanzaConElNombreDeLaPropiedad() {
        BeanMapperModel mapper = new BeanMapperModel(new FakeModelLoader());
        ParentModel target = new ParentModel();
        target.setAge(15);

        Map<String, Object> source = mapOf("age", "no-es-un-numero");
        Map<String, Object> allowProperties = mapOf("age", true);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> mapper.copyMapToEntity(ParentModel.class, source, target, AllowProperties.createAllowProperties(allowProperties)));

        assertEquals(ParentModel.class.getName(), ex.getMessage());
        assertEquals("Nombre de la propiedad:age", ex.getCause().getMessage());
        assertInstanceOf(IllegalArgumentException.class, ex.getCause().getCause());
        assertEquals(15, target.getAge());
    }

    @Test
    void copyMapToEntity_tipoDePropiedadNoSoportado_lanzaConElNombreDeLaPropiedad() {
        BeanMapperModel mapper = new BeanMapperModel(new FakeModelLoader());
        SetHolderModel target = new SetHolderModel();

        Map<String, Object> source = mapOf("extra", new HashMap<>());
        Map<String, Object> allowProperties = mapOf("extra", true);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> mapper.copyMapToEntity(SetHolderModel.class, source, target, AllowProperties.createAllowProperties(allowProperties)));

        assertEquals(SetHolderModel.class.getName(), ex.getMessage());
        assertEquals("Nombre de la propiedad:extra", ex.getCause().getMessage());
        assertTrue(ex.getCause().getCause().getMessage().startsWith("Unsupported property type: "));
        assertNull(target.getExtra());
    }

    @Test
    void copyMapToEntity_listaOrigenNulaYDestinoNulo_noHaceNada() {
        BeanMapperModel mapper = new BeanMapperModel(new FakeModelLoader());
        ParentModel target = new ParentModel();

        Map<String, Object> source = mapOf("children", null);
        Map<String, Object> allowProperties = mapOf("children", mapOf("name", true));

        mapper.copyMapToEntity(ParentModel.class, source, target, AllowProperties.createAllowProperties(allowProperties));

        assertNull(target.getChildren());
    }

    @Test
    void copyMapToEntity_listaOrigenNulaYDestinoConDatos_poneElDestinoANull() {
        BeanMapperModel mapper = new BeanMapperModel(new FakeModelLoader());
        ParentModel target = new ParentModel();
        target.setChildren(new ArrayList<>(List.of(child(1L, "a", target))));

        Map<String, Object> source = mapOf("children", null);
        Map<String, Object> allowProperties = mapOf("children", mapOf("name", true));

        mapper.copyMapToEntity(ParentModel.class, source, target, AllowProperties.createAllowProperties(allowProperties));

        assertNull(target.getChildren());
    }

    @Test
    void copyMapToEntity_listaOrigenConDatosYDestinoNulo_creaUnaListaNuevaConElMappedBy() {
        BeanMapperModel mapper = new BeanMapperModel(new FakeModelLoader());
        ParentModel target = new ParentModel();

        List<Object> childrenMap = new ArrayList<>();
        childrenMap.add(mapOf("name", "c1", "parent", new HashMap<>()));
        childrenMap.add(mapOf("name", "c2", "parent", new HashMap<>()));
        Map<String, Object> source = mapOf("children", childrenMap);
        Map<String, Object> allowProperties = mapOf("children", mapOf("name", true, "parent", true));

        mapper.copyMapToEntity(ParentModel.class, source, target, AllowProperties.createAllowProperties(allowProperties));

        assertInstanceOf(ArrayList.class, target.getChildren());
        assertEquals(List.of("c1", "c2"), target.getChildren().stream().map(ChildModel::getName).toList());
        assertTrue(target.getChildren().stream().allMatch(c -> Objects.equals(c.getParent(), target) && c.getId() == null));
    }

    @Test
    void copyMapToEntity_referenciaOrigenNulaYDestinoNulo_noHaceNada() {
        FakeModelLoader loader = new FakeModelLoader();
        BeanMapperModel mapper = new BeanMapperModel(loader);
        ParentModel target = new ParentModel();

        Map<String, Object> source = mapOf("ref", null);
        Map<String, Object> allowProperties = mapOf("ref", mapOf("code", true));

        mapper.copyMapToEntity(ParentModel.class, source, target, AllowProperties.createAllowProperties(allowProperties));

        assertNull(target.getRef());
    }

    @Test
    void copyMapToEntity_referenciaSinIdYDestinoNulo_creaUnaInstanciaNueva() {
        BeanMapperModel mapper = new BeanMapperModel(new FakeModelLoader());
        ParentModel target = new ParentModel();

        Map<String, Object> source = mapOf("ref", mapOf("code", "nuevo"));
        Map<String, Object> allowProperties = mapOf("ref", mapOf("code", true));

        mapper.copyMapToEntity(ParentModel.class, source, target, AllowProperties.createAllowProperties(allowProperties));

        assertNotNull(target.getRef());
        assertNull(target.getRef().getId());
        assertEquals("nuevo", target.getRef().getCode());
    }

    @Test
    void copyMapToEntity_setOrigenNuloYDestinoNulo_noHaceNada() {
        BeanMapperModel mapper = new BeanMapperModel(new FakeModelLoader());
        SetHolderModel target = new SetHolderModel();

        Map<String, Object> source = mapOf("tags", null);
        Map<String, Object> allowProperties = mapOf("tags", mapOf("code", true));

        mapper.copyMapToEntity(SetHolderModel.class, source, target, AllowProperties.createAllowProperties(allowProperties));

        assertNull(target.getTags());
    }

    @Test
    void copyMapToEntity_setOrigenNuloYDestinoConDatos_poneElDestinoANull() {
        BeanMapperModel mapper = new BeanMapperModel(new FakeModelLoader());
        SetHolderModel target = new SetHolderModel();
        target.setTags(new LinkedHashSet<>(Set.of(ref(1L, "a"))));

        Map<String, Object> source = mapOf("tags", null);
        Map<String, Object> allowProperties = mapOf("tags", mapOf("code", true));

        mapper.copyMapToEntity(SetHolderModel.class, source, target, AllowProperties.createAllowProperties(allowProperties));

        assertNull(target.getTags());
    }

    @Test
    void copyMapToEntity_setOrigenConDatosYDestinoNulo_creaUnSetNuevoCargandoLosQueTienenId() {
        RefModel cargado = ref(7L, "from-db");
        FakeModelLoader loader = new FakeModelLoader();
        loader.register(RefModel.class, 7L, cargado);
        BeanMapperModel mapper = new BeanMapperModel(loader);
        SetHolderModel target = new SetHolderModel();

        Set<Object> tags = new LinkedHashSet<>();
        tags.add(mapOf("id", 7L, "code", "from-map"));
        tags.add(mapOf("code", "nuevo"));
        Map<String, Object> source = mapOf("tags", tags);
        Map<String, Object> allowProperties = mapOf("tags", mapOf("code", true));

        mapper.copyMapToEntity(SetHolderModel.class, source, target, AllowProperties.createAllowProperties(allowProperties));

        assertInstanceOf(LinkedHashSet.class, target.getTags());
        List<RefModel> resultado = new ArrayList<>(target.getTags());
        assertEquals(2, resultado.size());
        assertSame(cargado, resultado.get(0));
        assertEquals(7L, resultado.get(0).getId());
        assertEquals("from-map", resultado.get(0).getCode());
        assertNull(resultado.get(1).getId());
        assertEquals("nuevo", resultado.get(1).getCode());
        assertEquals(1, loader.callCount(RefModel.class, 7L));
    }

    @Test
    void copyMapToEntity_setOrigenYDestinoConDatos_anhadeActualizaYEliminaSobreElMismoSet() {
        BeanMapperModel mapper = new BeanMapperModel(new FakeModelLoader());
        RefModel comun = ref(1L, "old-1");
        RefModel sobrante = ref(2L, "old-2");
        Set<RefModel> destino = new LinkedHashSet<>(List.of(comun, sobrante));
        SetHolderModel target = new SetHolderModel();
        target.setTags(destino);

        Set<Object> tags = new LinkedHashSet<>();
        tags.add(mapOf("id", 1L, "code", "upd-1"));
        tags.add(mapOf("code", "nuevo"));
        Map<String, Object> source = mapOf("tags", tags);
        Map<String, Object> allowProperties = mapOf("tags", mapOf("code", true));

        mapper.copyMapToEntity(SetHolderModel.class, source, target, AllowProperties.createAllowProperties(allowProperties));

        assertSame(destino, target.getTags());
        assertEquals(2, destino.size());
        assertTrue(destino.contains(comun));
        assertEquals(1L, comun.getId());
        assertEquals("upd-1", comun.getCode());
        assertFalse(destino.contains(sobrante));
        RefModel nuevo = destino.stream().filter(r -> !Objects.equals(r, comun)).findFirst().orElseThrow();
        assertNull(nuevo.getId());
        assertEquals("nuevo", nuevo.getCode());
    }

    @Test
    void copyMapToEntity_setDestinoConDatosYOrigenComoListaDelJson_anhadeActualizaYEliminaSobreElMismoSet() {
        // Del JSON de la petición las colecciones llegan como List, aunque la propiedad sea un Set.
        BeanMapperModel mapper = new BeanMapperModel(new FakeModelLoader());
        RefModel comun = ref(1L, "old-1");
        RefModel sobrante = ref(2L, "old-2");
        Set<RefModel> destino = new LinkedHashSet<>(List.of(comun, sobrante));
        SetHolderModel target = new SetHolderModel();
        target.setTags(destino);

        List<Object> tags = new ArrayList<>();
        tags.add(mapOf("id", 1L, "code", "upd-1"));
        tags.add(mapOf("code", "nuevo"));
        Map<String, Object> source = mapOf("tags", tags);
        Map<String, Object> allowProperties = mapOf("tags", mapOf("code", true));

        mapper.copyMapToEntity(SetHolderModel.class, source, target, AllowProperties.createAllowProperties(allowProperties));

        assertSame(destino, target.getTags());
        assertEquals(2, destino.size());
        assertTrue(destino.contains(comun));
        assertEquals("upd-1", comun.getCode());
        assertFalse(destino.contains(sobrante));
        RefModel nuevo = destino.stream().filter(r -> !Objects.equals(r, comun)).findFirst().orElseThrow();
        assertNull(nuevo.getId());
        assertEquals("nuevo", nuevo.getCode());
    }

    @Test
    void copyMapToEntity_setDestinoConElementoSinId_noLoEliminaAunqueNoEsteEnElOrigen() {
        // Comportamiento actual: removeInCollectionById ignora los id nulos, así que el elemento sin id se queda.
        BeanMapperModel mapper = new BeanMapperModel(new FakeModelLoader());
        RefModel sinId = ref(null, "sin-id");
        Set<RefModel> destino = new LinkedHashSet<>(List.of(sinId));
        SetHolderModel target = new SetHolderModel();
        target.setTags(destino);

        Map<String, Object> source = mapOf("tags", new LinkedHashSet<>());
        Map<String, Object> allowProperties = mapOf("tags", mapOf("code", true));

        mapper.copyMapToEntity(SetHolderModel.class, source, target, AllowProperties.createAllowProperties(allowProperties));

        assertSame(destino, target.getTags());
        assertEquals(Set.of(sinId), destino);
    }

    private static RefModel ref(Long id, String code) {
        RefModel ref = new RefModel();
        ref.setId(id);
        ref.setCode(code);
        return ref;
    }

    private static boolean algunaCausaContiene(Throwable ex, String texto) {
        return Stream.iterate(ex.getCause(), Objects::nonNull, Throwable::getCause)
                .anyMatch(causa -> causa.getMessage() != null && causa.getMessage().contains(texto));
    }

    private static ChildModel child(Long id, String name, ParentModel parent) {
        ChildModel child = new ChildModel();
        child.setId(id);
        child.setName(name);
        child.setParent(parent);
        return child;
    }

    private static Map<String, Object> mapOf(Object... values) {
        if (values.length % 2 != 0) {
            throw new IllegalArgumentException("Número de argumentos inválido para mapOf");
        }

        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            map.put(values[i].toString(), values[i + 1]);
        }
        return map;
    }

    private static class FakeModelLoader implements ModelLoader {
        private final Map<String, Model> models = new HashMap<>();
        private final List<String> calls = new ArrayList<>();

        @Override
        public Model getModel(Class<? extends Model> classModel, Long id) {
            String key = key(classModel, id);
            calls.add(key);
            return models.get(key);
        }

        void register(Class<? extends Model> classModel, Long id, Model model) {
            models.put(key(classModel, id), model);
        }

        int callCount(Class<? extends Model> classModel, Long id) {
            String expectedKey = key(classModel, id);
            int count = 0;
            for (String call : calls) {
                if (call.equals(expectedKey)) {
                    count++;
                }
            }
            return count;
        }

        private String key(Class<? extends Model> classModel, Long id) {
            return classModel.getName() + "#" + id;
        }
    }

    public static class ParentModel extends Model {
        private Long id;
        private String name;
        private Integer age;

        @OneToOne
        private RefModel ref;

        @OneToMany(mappedBy = "parent")
        private List<ChildModel> children;

        @Override
        public Long getId() {
            return id;
        }

        @Override
        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getAge() {
            return age;
        }

        public void setAge(Integer age) {
            this.age = age;
        }

        public RefModel getRef() {
            return ref;
        }

        public void setRef(RefModel ref) {
            this.ref = ref;
        }

        public List<ChildModel> getChildren() {
            return children;
        }

        public void setChildren(List<ChildModel> children) {
            this.children = children;
        }
    }

    public static class ChildModel extends Model {
        private Long id;
        private String name;

        @ManyToOne
        private ParentModel parent;

        @Override
        public Long getId() {
            return id;
        }

        @Override
        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public ParentModel getParent() {
            return parent;
        }

        public void setParent(ParentModel parent) {
            this.parent = parent;
        }
    }

    public static class RefModel extends Model {
        private Long id;
        private String code;

        @Override
        public Long getId() {
            return id;
        }

        @Override
        public void setId(Long id) {
            this.id = id;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }
    }

    public static class UnsupportedHolderModel extends Model {
        private Long id;
        private RefModel ref;

        @Override
        public Long getId() {
            return id;
        }

        @Override
        public void setId(Long id) {
            this.id = id;
        }

        public RefModel getRef() {
            return ref;
        }

        public void setRef(RefModel ref) {
            this.ref = ref;
        }
    }

    public static class MtoHolderModel extends Model {
        private Long id;

        @ManyToOne
        private RefModel ref;

        @ManyToOne
        private MetaFile fichero;

        @Override
        public Long getId() {
            return id;
        }

        @Override
        public void setId(Long id) {
            this.id = id;
        }

        public RefModel getRef() {
            return ref;
        }

        public void setRef(RefModel ref) {
            this.ref = ref;
        }

        public MetaFile getFichero() {
            return fichero;
        }

        public void setFichero(MetaFile fichero) {
            this.fichero = fichero;
        }
    }

    public static class SetHolderModel extends Model {
        private Long id;

        @ManyToMany
        private Set<RefModel> tags;

        private Map<String, Object> extra;

        @Override
        public Long getId() {
            return id;
        }

        @Override
        public void setId(Long id) {
            this.id = id;
        }

        public Set<RefModel> getTags() {
            return tags;
        }

        public void setTags(Set<RefModel> tags) {
            this.tags = tags;
        }

        public Map<String, Object> getExtra() {
            return extra;
        }

        public void setExtra(Map<String, Object> extra) {
            this.extra = extra;
        }
    }
}
