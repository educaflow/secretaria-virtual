package com.educaflow.base.infrastructure.mapper;

import com.axelor.db.Model;
import com.axelor.db.modelservice.AllowProperties;
import org.apache.commons.beanutils.PropertyUtils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.ParameterizedType;
import java.util.*;

public class BeanMapperModel {

    private final ModelLoader modelLoader;

    public BeanMapperModel() {
        this(new DefaultModelLoader());
    }

    public BeanMapperModel(ModelLoader modelLoader) {
        this.modelLoader = Objects.requireNonNull(modelLoader, "modelLoader no puede ser null");
    }

    public Object getEntityCloned(Class<? extends Model> clazz, Model entity) {
        return getEntityCloned(clazz, entity, null, null, AllowProperties.createAllowAllProperties(), new InstanceModelList());
    }
    public Object getEntityCloned(Class<? extends Model> clazz, Model entity, String mappedBy, Model mappedByModel) {
        return getEntityCloned(clazz, entity, mappedBy, mappedByModel, AllowProperties.createAllowAllProperties(), new InstanceModelList());
    }

    public void copyEntityToEntity(Class<? extends Model> clazz, Model entity, Model entityDest, AllowProperties allowProperties) {
        copyEntityToEntity(clazz, entity, entityDest, allowProperties, null, null, new InstanceModelList());
    }
    public void copyEntityToEntity(Class<? extends Model> clazz, Model entity, Model entityDest, AllowProperties allowProperties, String mappedBy, Model mappedByModel) {
        copyEntityToEntity(clazz, entity, entityDest, allowProperties, mappedBy, mappedByModel, new InstanceModelList());
    }

    public void copyMapToEntity(Class<? extends Model> clazz, Map<String, Object> entityMap, Model entityDest, AllowProperties allowProperties) {
        copyMapToEntity(clazz, entityMap, entityDest, allowProperties, null, null, new InstanceModelList());
    }
    public void copyMapToEntity(Class<? extends Model> clazz, Map<String, Object> entityMap, Model entityDest, AllowProperties allowProperties, String mappedBy, Model mappedByModel) {
        copyMapToEntity(clazz, entityMap, entityDest, allowProperties, mappedBy, mappedByModel, new InstanceModelList());
    }





    /****************************************************************************************************/
    /*********************** Funciones privadas que realizan realmente el trabajo ***********************/
    /****************************************************************************************************/

    private Object getEntityCloned(Class<? extends Model> clazz, Model entity, String mappedBy, Model mappedByModel, AllowProperties allowProperties, InstanceModelList instanceModelList) {
        try {
            if (entity == null) {
                return null;
            }

            Optional<Model> instancia = instanceModelList.getInstance(clazz, entity.getId());
            if (instancia.isPresent()) {
                return instancia.get();
            }


            Model entityDest = clazz.getDeclaredConstructor().newInstance();
            copyEntityToEntity(clazz, entity, entityDest, allowProperties, mappedBy, mappedByModel,instanceModelList);



            return entityDest;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }



    private void copyEntityToEntity(Class<? extends Model> clazz, Model entity, Model entityDest, AllowProperties allowProperties, String mappedBy, Model mappedByModel, InstanceModelList instanceModelList) {
        try {
            if (allowProperties == null) {
                return;
            }

            PropertyDescriptor[] propertyDescriptors = PropertyUtils.getPropertyDescriptors(clazz);

            for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
                if (propertyDescriptor.getWriteMethod() == null) {
                    continue;
                }
                if (allowProperties.allowProperty(propertyDescriptor.getName()) == false) {
                    continue;
                }



                if (propertyDescriptor.getName().equals(mappedBy)) {
                    PropertyUtils.setProperty(entityDest, propertyDescriptor.getName(), mappedByModel);
                } else if (ScalarMapper.isScalarType(propertyDescriptor.getPropertyType())) {
                    Object rawValue = PropertyUtils.getProperty(entity, propertyDescriptor.getName());

                    Object value = ScalarMapper.getScalarFromObject(rawValue, propertyDescriptor.getPropertyType());

                    PropertyUtils.setProperty(entityDest, propertyDescriptor.getName(), value);
                }

            }

            instanceModelList.addInstanceModel(clazz,entityDest);

            for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
                if (propertyDescriptor.getWriteMethod() == null) {
                    continue;
                }
                if (allowProperties.allowProperty(propertyDescriptor.getName()) == false) {
                    continue;
                }



                if (propertyDescriptor.getName().equals(mappedBy)) {
                    //No hacer nada porque ya se copió en el bucle anterior
                } else if (ScalarMapper.isScalarType(propertyDescriptor.getPropertyType())) {
                    //No hacer nada porque ya se copió en el bucle anterior
                } else if (Model.class.isAssignableFrom(propertyDescriptor.getPropertyType())) {
                    AllowProperties innerAllowProperties=allowProperties.innerAllowProperties(propertyDescriptor.getName());
                    Object rawValue = PropertyUtils.getProperty(entity, propertyDescriptor.getName());
                    Object value = getEntityCloned((Class<? extends Model>) propertyDescriptor.getPropertyType(), (Model) rawValue,null,null, innerAllowProperties, instanceModelList);
                    PropertyUtils.setProperty(entityDest, propertyDescriptor.getName(), value);
                } else if (List.class.isAssignableFrom(propertyDescriptor.getPropertyType())) {
                    List<? extends Model> rawValue = (List<? extends Model>) PropertyUtils.getProperty(entity, propertyDescriptor.getName());
                    String mappedByRelation = BeanMapperUtil.getMappedByInOneToMany(clazz, propertyDescriptor.getName());
                    AllowProperties innerAllowProperties=allowProperties.innerAllowProperties(propertyDescriptor.getName());

                    Class<? extends Model> tipoListaClass = (Class<? extends Model>) ((ParameterizedType) propertyDescriptor.getReadMethod().getGenericReturnType()).getActualTypeArguments()[0];
                    List value = null;
                    if (rawValue!=null) {
                        value = new ArrayList();
                        for (Model model : rawValue) {
                            Object itemValue = getEntityCloned(tipoListaClass, model, mappedByRelation, entityDest, innerAllowProperties, instanceModelList);
                            value.add(itemValue);
                        }
                    }

                    PropertyUtils.setProperty(entityDest, propertyDescriptor.getName(), value);
                } else if (Set.class.isAssignableFrom(propertyDescriptor.getPropertyType())) {
                    Set<? extends Model> rawValue = (Set<? extends Model>) PropertyUtils.getProperty(entity, propertyDescriptor.getName());
                    String mappedByRelation = null; //BeanMapperUtil.getMappedByInManyToMany(clazz, propertyDescriptor.getName());
                    AllowProperties innerAllowProperties=allowProperties.innerAllowProperties(propertyDescriptor.getName());

                    Class<? extends Model> tipoSetClass = (Class<? extends Model>) ((ParameterizedType) propertyDescriptor.getReadMethod().getGenericReturnType()).getActualTypeArguments()[0];
                    Set value = null;
                    if (rawValue!=null) {
                        value = new LinkedHashSet();
                        for (Model model : rawValue) {
                            Object itemValue = getEntityCloned(tipoSetClass, model, mappedByRelation, entityDest, innerAllowProperties, instanceModelList);
                            value.add(itemValue);
                        }
                    }

                    PropertyUtils.setProperty(entityDest, propertyDescriptor.getName(), value);


                } else {
                    throw new RuntimeException("Tipo no soportado: " + propertyDescriptor.getPropertyType());
                }

            }

        } catch (Exception ex) {
            throw new RuntimeException(clazz.getName() + " " + entity, ex);
        }
    }

    private void copyMapToEntity(Class<? extends Model> clazz, Map<String, Object> entityMap, Model entityDest, AllowProperties allowProperties, String mappedBy, Model mappedByModel, InstanceModelList instanceModelList) {
        try {
            if (allowProperties == null) {
                return;
            }


            PropertyDescriptor[] propertyDescriptors = PropertyUtils.getPropertyDescriptors(clazz);

            for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
                try {
                    if (isPropertyInMapToCopy(propertyDescriptor, entityMap, allowProperties)) {
                        copyScalarFromMap(propertyDescriptor, entityMap, entityDest, mappedBy, mappedByModel);
                    }
                } catch (Exception ex) {
                   throw new RuntimeException("Nombre de la propiedad:"+propertyDescriptor.getName() ,ex);
                }

            }

            instanceModelList.addInstanceModel(clazz,entityDest);

            for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
                try {
                    if (isPropertyInMapToCopy(propertyDescriptor, entityMap, allowProperties)) {
                        copyRelationFromMap(clazz, propertyDescriptor, entityMap, entityDest, allowProperties, mappedBy, instanceModelList);
                    }
                } catch (Exception ex) {
                    throw new RuntimeException("Nombre de la propiedad:"+propertyDescriptor.getName() ,ex);
                }

            }


        } catch (Exception ex) {
            throw new RuntimeException(clazz.getName(), ex);
        }
    }

    private boolean isPropertyInMapToCopy(PropertyDescriptor propertyDescriptor, Map<String, Object> entityMap, AllowProperties allowProperties) {
        return (propertyDescriptor.getWriteMethod() != null)
                && allowProperties.allowProperty(propertyDescriptor.getName())
                && entityMap.containsKey(propertyDescriptor.getName());
    }

    /**
     * Primera pasada: la propiedad mappedBy y los escalares, que no necesitan que la entidad ya esté en el InstanceModelList.
     */
    private void copyScalarFromMap(PropertyDescriptor propertyDescriptor, Map<String, Object> entityMap, Model entityDest, String mappedBy, Model mappedByModel) throws Exception {
        if (propertyDescriptor.getName().equals(mappedBy)) {
            PropertyUtils.setProperty(entityDest, propertyDescriptor.getName(), mappedByModel);
        } else if (ScalarMapper.isScalarType(propertyDescriptor.getPropertyType())) {
            Object rawValue = entityMap.get(propertyDescriptor.getName());

            Object value = ScalarMapper.getScalarFromObject(rawValue, propertyDescriptor.getPropertyType());

            PropertyUtils.setProperty(entityDest, propertyDescriptor.getName(), value);
        }
    }

    /**
     * Segunda pasada: las relaciones (modelo, List y Set). La mappedBy y los escalares ya se copiaron en la primera.
     */
    private void copyRelationFromMap(Class<? extends Model> clazz, PropertyDescriptor propertyDescriptor, Map<String, Object> entityMap, Model entityDest, AllowProperties allowProperties, String mappedBy, InstanceModelList instanceModelList) throws Exception {
        Class<?> propertyType = propertyDescriptor.getPropertyType();

        if (propertyDescriptor.getName().equals(mappedBy) || ScalarMapper.isScalarType(propertyType)) {
            return;
        }

        if (Model.class.isAssignableFrom(propertyType)) {
            copyModelFromMap(clazz, propertyDescriptor, entityMap, entityDest, allowProperties, instanceModelList);
        } else if (List.class.isAssignableFrom(propertyType)) {
            copyListFromMap(clazz, propertyDescriptor, entityMap, entityDest, allowProperties, instanceModelList);
        } else if (Set.class.isAssignableFrom(propertyType)) {
            copySetFromMap(propertyDescriptor, entityMap, entityDest, allowProperties, instanceModelList);
        } else {
            throw new RuntimeException("Unsupported property type: " + propertyType);
        }
    }

    private void copyModelFromMap(Class<? extends Model> clazz, PropertyDescriptor propertyDescriptor, Map<String, Object> entityMap, Model entityDest, AllowProperties allowProperties, InstanceModelList instanceModelList) throws Exception {
        String propertyName = propertyDescriptor.getName();
        Class<? extends Model> propertyType = (Class<? extends Model>) propertyDescriptor.getPropertyType();

        if ((BeanMapperUtil.isOneToOne(clazz, propertyName)==false) && (BeanMapperUtil.isManyToOne(clazz, propertyName)==false)) {
            throw new RuntimeException("Si una propiedad es un modelo debe ser One-to-one o Many-to-one: " + propertyType);
        }


        Map<String,Object> rawValue = (Map<String,Object>)entityMap.get(propertyName);
        Model valueDest = (Model) PropertyUtils.getProperty(entityDest, propertyName);
        AllowProperties innerAllowProperties = allowProperties.innerAllowProperties(propertyName);

        if (rawValue == null) {
            if (valueDest != null) {
                PropertyUtils.setProperty(entityDest, propertyName, null);
            }
        } else if (valueDest == null) {
            PropertyUtils.setProperty(entityDest, propertyName, createModelFromMap(propertyType, rawValue, innerAllowProperties, null, null, instanceModelList));
        } else if (isOtherModel(rawValue, valueDest)) {
            // El usuario eligió otra entidad: cargarla por su id y reemplazar la referencia,
            // en vez de copiar los campos del mapa dentro de la entidad actualmente referenciada
            PropertyUtils.setProperty(entityDest, propertyName, getInitialModelFromMap(rawValue, propertyType));
        } else {
            copyValueToEntityAndNoChangeId(propertyType, rawValue, valueDest, innerAllowProperties, null, null, instanceModelList);
        }
    }

    private boolean isOtherModel(Map<String, Object> rawValue, Model valueDest) {
        Long rawValueId = BeanMapperUtil.getId(rawValue);
        return (rawValueId != null) && !rawValueId.equals(valueDest.getId());
    }

    private void copyListFromMap(Class<? extends Model> clazz, PropertyDescriptor propertyDescriptor, Map<String, Object> entityMap, Model entityDest, AllowProperties allowProperties, InstanceModelList instanceModelList) throws Exception {
        String propertyName = propertyDescriptor.getName();
        List<Object> listSource = (List<Object>) entityMap.get(propertyName);
        List<Model> listTarget = (List<Model>) PropertyUtils.getProperty(entityDest, propertyName);
        Class<? extends Model> tipoListaClass = getCollectionItemClass(propertyDescriptor);
        String mappedByRelation = BeanMapperUtil.getMappedByInOneToMany(clazz, propertyName);
        AllowProperties innerAllowProperties = allowProperties.innerAllowProperties(propertyName);

        if (listSource == null) {
            if (listTarget != null) {
                PropertyUtils.setProperty(entityDest, propertyName, null);
            }
        } else if (listTarget == null) {
            List<Model> listValues = new ArrayList<>();
            for (Object rawValue : listSource) {
                listValues.add(createModelFromMap(tipoListaClass, rawValue, innerAllowProperties, mappedByRelation, entityDest, instanceModelList));
            }
            PropertyUtils.setProperty(entityDest, propertyName, listValues);
        } else {
            ModelListCompare modelListCompare = new ModelListCompare(listSource, listTarget);

            for (Object rawValue : modelListCompare.getSourceWhereOnlySource()) {
                listTarget.add(createModelFromMap(tipoListaClass, rawValue, innerAllowProperties, mappedByRelation, entityDest, instanceModelList));
            }
            for (Model itemValue : modelListCompare.getTargetWhereSourceAndTarget()) {
                Object rawValue = BeanMapperUtil.findInCollectionById(modelListCompare.getSourceWhereSourceAndTarget(), itemValue.getId()).orElse(null);
                copyValueToEntityAndNoChangeId(tipoListaClass, rawValue, itemValue, innerAllowProperties, mappedByRelation, entityDest, instanceModelList);
            }
            for (Model itemValue : modelListCompare.getTargetWhereOnlyTarget()) {
                listTarget.remove(itemValue);
            }
        }
    }

    private void copySetFromMap(PropertyDescriptor propertyDescriptor, Map<String, Object> entityMap, Model entityDest, AllowProperties allowProperties, InstanceModelList instanceModelList) throws Exception {
        String propertyName = propertyDescriptor.getName();
        Collection<Object> collectionSetSource = (Collection<Object>) entityMap.get(propertyName);
        Set<Model> collectionSetTarget = (Set<Model>) PropertyUtils.getProperty(entityDest, propertyName);
        Class<? extends Model> tipoSetClass = getCollectionItemClass(propertyDescriptor);
        String mappedByRelation = null; //BeanMapperUtil.getMappedByInManyToMany(clazz, propertyDescriptor.getName());
        AllowProperties innerAllowProperties = allowProperties.innerAllowProperties(propertyName);

        if (collectionSetSource == null) {
            if (collectionSetTarget != null) {
                PropertyUtils.setProperty(entityDest, propertyName, null);
            }
        } else if (collectionSetTarget == null) {
            Set<Model> setValues = new LinkedHashSet<>();
            for (Object rawValue : collectionSetSource) {
                setValues.add(createModelFromMap(tipoSetClass, rawValue, innerAllowProperties, mappedByRelation, entityDest, instanceModelList));
            }
            PropertyUtils.setProperty(entityDest, propertyName, setValues);
        } else {
            ModelSetCompare modelSetCompare = new ModelSetCompare(new LinkedHashSet<>(collectionSetSource), collectionSetTarget);

            for (Object rawValue : modelSetCompare.getSourceWhereOnlySource()) {
                collectionSetTarget.add(createModelFromMap(tipoSetClass, rawValue, innerAllowProperties, mappedByRelation, entityDest, instanceModelList));
            }

            for(Model itemValue:modelSetCompare.getTargetWhereSourceAndTarget()) {
                Object rawValue = BeanMapperUtil.findInCollectionById(modelSetCompare.getSourceWhereSourceAndTarget(), itemValue.getId()).orElse(null);
                copyValueToEntityAndNoChangeId(tipoSetClass, rawValue, itemValue, innerAllowProperties, mappedByRelation, entityDest, instanceModelList);
            }

            for(Model itemValue:modelSetCompare.getTargetWhereOnlyTarget()) {
                BeanMapperUtil.removeInCollectionById(collectionSetTarget, itemValue.getId());
            }
        }
    }

    private Class<? extends Model> getCollectionItemClass(PropertyDescriptor propertyDescriptor) {
        return (Class<? extends Model>) ((ParameterizedType) propertyDescriptor.getReadMethod().getGenericReturnType()).getActualTypeArguments()[0];
    }

    /**
     * Crea (o carga por su id) el modelo de un mapa y le copia los valores del mapa sin cambiarle el id.
     */
    private Model createModelFromMap(Class<? extends Model> clazz, Object rawValue, AllowProperties allowProperties, String mappedBy, Model mappedByModel, InstanceModelList instanceModelList) throws Exception {
        Model itemValue = getInitialModelFromMap((Map<String, Object>) rawValue, clazz);
        copyValueToEntityAndNoChangeId(clazz, rawValue, itemValue, allowProperties, mappedBy, mappedByModel, instanceModelList);
        return itemValue;
    }


    private void copyValueToEntityAndNoChangeId(Class<? extends Model> clazz, Object rawValue, Model valueDest, AllowProperties allowProperties, String mappedBy, Model mappedByModel, InstanceModelList instanceModelList) {
        Long originalId = valueDest.getId();

        if (rawValue instanceof Model rawValueModel) {
            copyEntityToEntity(clazz, rawValueModel, valueDest, allowProperties, mappedBy, mappedByModel, instanceModelList);
            } else if (rawValue instanceof Map) {
                Map<String, Object> rawValueMap = (Map<String, Object>) rawValue;
            copyMapToEntity(clazz, rawValueMap, valueDest, allowProperties, mappedBy, mappedByModel, instanceModelList);
        } else {
                throw new RuntimeException("Unsupported property type: " + rawValue.getClass());
        }

        valueDest.setId(originalId);
    }

    /**
     * Crea un modelo a partir de un mapa de valores.
     * Si el mapa contiene un id, se busca el modelo existente, si no, se crea uno nuevo.
     * Porque si ya tiene "id" se asume que es un modelo existente y se busca en la base de datos.
     *
     * @param values Mapa de valores
     * @param clazz Clase del modelo
     * @return Modelo creado o encontrado
     * @throws Exception Si ocurre un error al crear o buscar el modelo
     */
    private Model getInitialModelFromMap(Map<String, Object> values,Class<? extends Model> clazz) throws Exception {
        Model initialModel;

        Long id = BeanMapperUtil.getId(values);
        if (id != null) {
            Model loadedModel=getModel(clazz, id);
            if (loadedModel == null) {
                throw new RuntimeException("No se encontró el modelo con id: " + values.get("id") + " del tipo" + clazz);
            }

            initialModel=loadedModel;
        } else {
            Model emptyModel=clazz.getDeclaredConstructor().newInstance();
            initialModel=emptyModel;
        }


        return initialModel;
    }


    private Model getModel(Class<? extends Model> classModel, Long id) {
        return modelLoader.getModel(classModel, id);
    }




}
