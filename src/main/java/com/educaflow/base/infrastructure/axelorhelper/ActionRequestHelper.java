package com.educaflow.base.infrastructure.axelorhelper;

import com.axelor.db.Model;
import com.axelor.db.JpaRepository;
import com.axelor.db.mapper.Mapper;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.Context;
import com.educaflow.base.infrastructure.mapper.BeanMapperModel;
import com.axelor.db.modelservice.AllowProperties;
import com.educaflow.base.util.Convert;
import com.educaflow.base.util.TextUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class ActionRequestHelper<T extends Model> {
    private final ActionRequest request;
    private final Class<T> expectedModelClass;

    public ActionRequestHelper(ActionRequest request) {
        this(request, null);
    }
    public ActionRequestHelper(ActionRequest request, Class<T> expectedModelClass) {
        this.request = request;
        this.expectedModelClass = expectedModelClass;

        if (expectedModelClass != null) {
            Class<? extends Model> actualModelClass = this.getModelClass();

            if (actualModelClass == null) {
                throw new RuntimeException("El _model del ActionRequest es null");
            }

            if (expectedModelClass.equals(actualModelClass)==false) {
                throw new RuntimeException("El classModel no coincide con el _model del requestData: " + expectedModelClass.getCanonicalName() + " != " + actualModelClass.getCanonicalName());
            }
        }
    }

    public Map<String, Object> getRequestData() {
        Map<String, Object> requestcontext = (Map<String, Object>) request.getData().get("context");

        if (requestcontext == null) {
            throw new RuntimeException("requestcontext es null");
        }

        Context context = request.getContext();

        if (context == null) {
            return new HashMap<>(requestcontext);
        }

        // El mapa crudo es el JSON tal cual lo envió el cliente; lo que han escrito las acciones anteriores del mismo
        // action-group solo está en el Context (ahí lo vuelca ActionGroup.evaluate), por eso se superpone encima
        // recorriendo su entrySet(), que devuelve el valor guardado y no el bean del proxy que daría su get(nombre).
        Map<String, Object> requestData = new HashMap<>(requestcontext);
        requestData.putAll(getContextData(context));

        return requestData;
    }

    private Map<String, Object> getContextData(Context context) {
        Map<String, Object> contextData = new HashMap<>();
        Mapper mapper = Mapper.of(context.getContextClass());

        // El Context tambien lleva las variables <context> de un action-view y proxies sin id: lo que no es propiedad de la entidad se omite para que no dispare el fail-fast de toRequestValue.
        for (Map.Entry<String, Object> entry : context.entrySet()) {
            if (mapper.getProperty(entry.getKey()) == null) {
                continue;
            }
            contextData.put(entry.getKey(), toRequestValue(entry.getKey(), entry.getValue()));
        }

        return contextData;
    }

    private Object toRequestValue(String key, Object value) {
        if (value instanceof Model model) {
            // Un Model llega aquí solo si una acción anterior del mismo action-group lo dejó en el Context con
            // context.put(...) (ActionGroup.evaluate); si esa entidad es nueva y aún no tiene id no hay forma de
            // representarla como {"id": ...}. Hoy ningún action-group del proyecto asigna una entidad sin guardar a
            // un campo relacional antes de llegar aquí, así que esto no debería saltar; si empieza a saltar, es que
            // se ha introducido ese patrón y hay que guardar la entidad antes o rediseñar la cadena de acciones.
            if (model.getId() == null) {
                throw new RuntimeException("No se puede representar como dato de la petición el valor del campo '" + key + "': es un " + model.getClass().getName() + " sin id");
            }
            return Map.of("id", model.getId());
        }

        if (value instanceof Collection<?> collection) {
            return toRequestCollection(key, collection);
        }

        return value;
    }

    private Collection<Object> toRequestCollection(String key, Collection<?> collection) {
        Collection<Object> requestCollection = (collection instanceof Set) ? new LinkedHashSet<>() : new ArrayList<>();

        for (Object element : collection) {
            requestCollection.add(toRequestValue(key, element));
        }

        return requestCollection;
    }

    public Long getId() {
        Map<String, Object> requestData = getRequestData();
        Object idObject = requestData.get("id");

        if (idObject == null) {
            return null;
        } else {
            return Convert.objectToLong(idObject);
        }
    }

    public long getParentId() {
        Map<String, Object> requestData = getRequestData();
        Object idObject = ((Map<String, Object>) requestData.get("_parent")).get("id");

        if (idObject == null) {
            throw new RuntimeException("idObject es null");
        }

        return Convert.objectToLong(idObject);
    }

    public String getEventName() {
        String eventName = (String) getRequestData().get("_signal");

        if (eventName == null) {
            throw new RuntimeException("eventName is null");
        }
        if (TextUtil.isIdentifier(eventName) == false) {
            throw new RuntimeException("_signal no es un nombre válido: debe empezar por letra y llevar solo letras sin acentos, dígitos y guiones bajos.");
        }

        return eventName;
    }

    public String getProfileName() {
        String profileName = (String) getRequestData().get("_profile");
        if (profileName == null) {
            throw new RuntimeException("_profile is null");
        }
        if (profileName.isBlank()) {
            throw new RuntimeException("_profile is blank");
        }
        if (TextUtil.isIdentifier(profileName) == false) {
            throw new RuntimeException("_profile no es un nombre válido: debe empezar por letra y llevar solo letras sin acentos, dígitos y guiones bajos.");
        }

        return profileName;
    }

    public String getParentSource() {

        String parentSource=(String)((Map<String,Object>) getRequestData().get("_parent")).get("_source");

        return parentSource;

    }

    public Class<? extends Model> getModelClass() {
        Map<String, Object> requestData = getRequestData();
        String modelName = (String) requestData.get("_model");

        if (modelName == null) {
            throw new RuntimeException("modelName is null");
        }

        try {
            return (Class<? extends Model>) Class.forName(modelName);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("No se pudo encontrar la clase del modelo: " + modelName, e);
        }
    }

    public T getOriginalModel() {
        T clonedModel;

        BeanMapperModel beanMapperModel=new BeanMapperModel();
        Class<T> clazz = getConcreteClass();
        JpaRepository<T> jpaRepository = JpaRepository.of(clazz);
        Long id = this.getId();

        if (id!=null) {
            T model = jpaRepository.find(id);
            clonedModel=(T) beanMapperModel.getEntityCloned(clazz, model);
        } else {
            clonedModel=null;
        }

        return clonedModel;
    }

    public T findById(Long id) {
        Class<T> clazz = getConcreteClass();
        JpaRepository<T> jpaRepository = JpaRepository.of(clazz);

        return jpaRepository.find(id);
    }

    public T getModel(AllowProperties allowProperties) {
        T model;

        BeanMapperModel beanMapperModel=new BeanMapperModel();
        Class<T> clazz = getConcreteClass();
        JpaRepository<T> jpaRepository = JpaRepository.of(clazz);
        Map<String, Object> requestData = this.getRequestData();
        Long id = this.getId();

        if (id!=null) {
            model = jpaRepository.find(id);
        } else  {
            model = jpaRepository.create(null);
        }
        beanMapperModel.copyMapToEntity(clazz, requestData, model, allowProperties);

        return model;
    }

    private Class<T> getConcreteClass() {
        if (expectedModelClass == null) {
            throw new RuntimeException("No se puede obtener el modelo tipado sin especificar expectedModelClass en el constructor");
        }
        return expectedModelClass;
    }
}
