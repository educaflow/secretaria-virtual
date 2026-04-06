package com.educaflow.base.infrastructure.mapper;

import com.axelor.db.Model;
import com.axelor.meta.db.MetaFile;
import com.educaflow.base.util.AllowProperties;
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

            if (clazz != null && clazz.getName().startsWith("com.axelor.meta.db.")) {
                //return entity;
            }

            if (instanceModelList.existsInstance(clazz, entity.getId())) {
                return instanceModelList.getInstance(clazz, entity.getId());
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

                    //Obtener valor real
                    Object value = ScalarMapper.getScalarFromObject(rawValue, propertyDescriptor.getPropertyType());

                    PropertyUtils.setProperty(entityDest, propertyDescriptor.getName(), value);
                }

            }

            instanceModelList.addInstanceModel(clazz,entity);

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
                    if (propertyDescriptor.getWriteMethod() == null) {
                        continue;
                    }
                    if (allowProperties.allowProperty(propertyDescriptor.getName()) == false) {
                        continue;
                    }

                    if (entityMap.containsKey(propertyDescriptor.getName()) == false) {
                        continue;
                    }

                    if (propertyDescriptor.getName().equals(mappedBy)) {
                        PropertyUtils.setProperty(entityDest, propertyDescriptor.getName(), mappedByModel);
                    } else if (ScalarMapper.isScalarType(propertyDescriptor.getPropertyType())) {
                        Object rawValue = entityMap.get(propertyDescriptor.getName());

                        //Obtener valor real
                        Object value = ScalarMapper.getScalarFromObject(rawValue, propertyDescriptor.getPropertyType());

                        PropertyUtils.setProperty(entityDest, propertyDescriptor.getName(), value);
                    }

                } catch (Exception ex) {
                   throw new RuntimeException("Nombre de la propiedad:"+propertyDescriptor.getName() ,ex);
                }

            }

            instanceModelList.addInstanceModel(clazz,entityDest);

            for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
                try {
                    if (propertyDescriptor.getWriteMethod() == null) {
                        continue;
                    }
                    if (allowProperties.allowProperty(propertyDescriptor.getName()) == false) {
                        continue;
                    }

                    if (entityMap.containsKey(propertyDescriptor.getName()) == false) {
                        continue;
                    }

                    if (propertyDescriptor.getName().equals(mappedBy)) {
                        //No hacer nada porque ya se copió en el bucle anterior
                    } else if (ScalarMapper.isScalarType(propertyDescriptor.getPropertyType())) {
                        //No hacer nada porque ya se copió en el bucle anterior
                    } else if (Model.class.isAssignableFrom(propertyDescriptor.getPropertyType())) {
                        if ((BeanMapperUtil.isOneToOne(clazz, propertyDescriptor.getName())==false) && (BeanMapperUtil.isManyToOne(clazz, propertyDescriptor.getName())==false)) {
                            throw new RuntimeException("Si una propiedad es un modelo debe ser One-to-one o Many-to-one: " + propertyDescriptor.getPropertyType());
                        }


                        Map<String,Object> rawValue = (Map<String,Object>)entityMap.get(propertyDescriptor.getName());
                        Model valueDest = (Model) PropertyUtils.getProperty(entityDest, propertyDescriptor.getName());
                        AllowProperties innerAllowProperties = allowProperties.innerAllowProperties(propertyDescriptor.getName());

                        if ((rawValue == null) && (valueDest == null)) {
                            //No hacer nada
                        } else if ((rawValue == null) && (valueDest != null)) {
                            PropertyUtils.setProperty(entityDest, propertyDescriptor.getName(), null);
                        } else if ((rawValue != null) && (valueDest == null)) {
                            valueDest = getInitialModelFromMap(rawValue,(Class<? extends Model>) propertyDescriptor.getPropertyType());
                            copyValueToEntityAndNoChangeId((Class<? extends Model>) propertyDescriptor.getPropertyType(), rawValue, valueDest, innerAllowProperties, null, null,instanceModelList);
                            PropertyUtils.setProperty(entityDest, propertyDescriptor.getName(), valueDest);
                        } else if ((rawValue != null) && (valueDest != null)) {
                            Long rawValueId = rawValue.get("id") != null ? ((Number) rawValue.get("id")).longValue() : null;
                            if (MetaFile.class.isAssignableFrom(propertyDescriptor.getPropertyType()) && rawValueId != null && !rawValueId.equals(valueDest.getId())) {
                                // El usuario reemplazó el fichero: cargar el nuevo MetaFile y reemplazar la referencia
                                valueDest = getInitialModelFromMap(rawValue, (Class<? extends Model>) propertyDescriptor.getPropertyType());
                                PropertyUtils.setProperty(entityDest, propertyDescriptor.getName(), valueDest);
                            } else {
                                copyValueToEntityAndNoChangeId((Class<? extends Model>) propertyDescriptor.getPropertyType(), rawValue, valueDest, innerAllowProperties, null, null, instanceModelList);
                            }
                        } else {
                            throw new RuntimeException("Error de lógica");
                        }
                    } else if (List.class.isAssignableFrom(propertyDescriptor.getPropertyType())) {
                        List<Object> listSource = (List<Object>) entityMap.get(propertyDescriptor.getName());
                        List<Model> listTarget = (List<Model>) PropertyUtils.getProperty(entityDest, propertyDescriptor.getName());
                        Class<? extends Model> tipoListaClass = (Class<? extends Model>) ((ParameterizedType) propertyDescriptor.getReadMethod().getGenericReturnType()).getActualTypeArguments()[0];
                        String mappedByRelation = BeanMapperUtil.getMappedByInOneToMany(clazz, propertyDescriptor.getName());
                        AllowProperties innerAllowProperties = allowProperties.innerAllowProperties(propertyDescriptor.getName());

                        if ((listSource == null) && (listTarget == null)) {
                            //No hacer nada
                        } else if ((listSource == null) && (listTarget != null)) {
                            PropertyUtils.setProperty(entityDest, propertyDescriptor.getName(), null);
                        } else if ((listSource != null) && (listTarget == null)) {
                            List<Model> listValues = null;
                            if (listSource!=null) {
                                listValues = new ArrayList<>();
                                for (Object rawValue : listSource) {
                                    Model itemValue = getInitialModelFromMap((Map<String, Object>) rawValue, tipoListaClass);
                                    copyValueToEntityAndNoChangeId(tipoListaClass, rawValue, itemValue, innerAllowProperties, mappedByRelation, entityDest, instanceModelList);
                                    listValues.add(itemValue);
                                }
                            }
                            PropertyUtils.setProperty(entityDest, propertyDescriptor.getName(), listValues);
                        } else if ((listSource != null) && (listTarget != null)) {
                            ModelListCompare modelListCompare = new ModelListCompare(listSource, listTarget);

                            for (Object rawValue : modelListCompare.getSourceWhereOnlySource()) {
                                Model itemValue = getInitialModelFromMap((Map<String,Object>)rawValue,tipoListaClass);
                                copyValueToEntityAndNoChangeId(tipoListaClass, rawValue, itemValue, innerAllowProperties, mappedByRelation, entityDest, instanceModelList);
                                listTarget.add(itemValue);
                            }
                            for (int i = 0; i < modelListCompare.getTargetWhereSourceAndTarget().size(); i++) {
                                Model itemValue = modelListCompare.getTargetWhereSourceAndTarget().get(i);
                                Object rawValue = modelListCompare.getSourceWhereSourceAndTarget().get(i);
                                copyValueToEntityAndNoChangeId(tipoListaClass, rawValue, itemValue, innerAllowProperties, mappedByRelation, entityDest, instanceModelList);
                            }
                            for (int i = 0; i < modelListCompare.getTargetWhereOnlyTarget().size(); i++) {
                                Model itemValue = modelListCompare.getTargetWhereOnlyTarget().get(i);
                                listTarget.remove(itemValue);
                            }


                        } else {
                            throw new RuntimeException("Error de lógica");
                        }
                    } else if (Set.class.isAssignableFrom(propertyDescriptor.getPropertyType())) {
                        Set<Object> collectionSetSource = (Set<Object>) entityMap.get(propertyDescriptor.getName());
                        Set<Model> collectionSetTarget = (Set<Model>) PropertyUtils.getProperty(entityDest, propertyDescriptor.getName());
                        Class<? extends Model> tipoSetClass = (Class<? extends Model>) ((ParameterizedType) propertyDescriptor.getReadMethod().getGenericReturnType()).getActualTypeArguments()[0];
                        String mappedByRelation = null; //BeanMapperUtil.getMappedByInManyToMany(clazz, propertyDescriptor.getName());
                        AllowProperties innerAllowProperties = allowProperties.innerAllowProperties(propertyDescriptor.getName());

                        if ((collectionSetSource == null) && (collectionSetTarget == null)) {
                            //No hacer nada
                        } else if ((collectionSetSource == null) && (collectionSetTarget != null)) {
                            PropertyUtils.setProperty(entityDest, propertyDescriptor.getName(), null);
                        } else if ((collectionSetSource != null) && (collectionSetTarget == null)) {
                            Set<Model> setValues = null;
                            if (collectionSetSource!=null) {
                                setValues = new LinkedHashSet<>();
                                for (Object rawValue : collectionSetSource) {
                                    Model itemValue = getInitialModelFromMap((Map<String, Object>) rawValue, tipoSetClass);
                                    copyValueToEntityAndNoChangeId(tipoSetClass, rawValue, itemValue, innerAllowProperties, mappedByRelation, entityDest, instanceModelList);
                                    setValues.add(itemValue);
                                }
                            }
                            PropertyUtils.setProperty(entityDest, propertyDescriptor.getName(), setValues);
                        } else if ((collectionSetSource != null) && (collectionSetTarget != null)) {
                            ModelSetCompare modelSetCompare = new ModelSetCompare(collectionSetSource, collectionSetTarget);

                            for (Object rawValue : modelSetCompare.getSourceWhereOnlySource()) {
                                Model itemValue = getInitialModelFromMap((Map<String,Object>)rawValue,tipoSetClass);
                                copyValueToEntityAndNoChangeId(tipoSetClass, rawValue, itemValue, innerAllowProperties, mappedByRelation, entityDest, instanceModelList);
                                collectionSetTarget.add(itemValue);
                            }

                            for(Model itemValue:modelSetCompare.getTargetWhereSourceAndTarget()) {
                                Object rawValue = BeanMapperUtil.findInCollectionById(modelSetCompare.getSourceWhereSourceAndTarget(), itemValue.getId());
                                copyValueToEntityAndNoChangeId(tipoSetClass, rawValue, itemValue, innerAllowProperties, mappedByRelation, entityDest, instanceModelList);
                            }

                            for(Model itemValue:modelSetCompare.getTargetWhereOnlyTarget()) {
                                BeanMapperUtil.removeInCollectionById(collectionSetTarget, itemValue.getId());
                            }
                        } else {
                            throw new RuntimeException("Error de lógica");
                        }

                    } else {
                        throw new RuntimeException("Unsupported property type: " + propertyDescriptor.getPropertyType());
                    }

                } catch (Exception ex) {
                    throw new RuntimeException("Nombre de la propiedad:"+propertyDescriptor.getName() ,ex);
                }

            }


        } catch (Exception ex) {
            throw new RuntimeException(clazz.getName(), ex);
        }
    }


    private void copyValueToEntityAndNoChangeId(Class<? extends Model> clazz, Object rawValue, Model valueDest, AllowProperties allowProperties, String mappedBy, Model mappedByModel, InstanceModelList instanceModelList) {
        Long originalId = valueDest.getId();

        if (rawValue instanceof Model) {
            Model rawValueModel = (Model) rawValue;
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

        if (values.get("id") != null) {
            Model loadedModel=getModel(clazz,  ((Number) values.get("id")).longValue());
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
