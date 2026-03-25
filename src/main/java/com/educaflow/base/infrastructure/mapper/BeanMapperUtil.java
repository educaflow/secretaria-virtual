package com.educaflow.base.infrastructure.mapper;

import com.axelor.db.Model;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

public class BeanMapperUtil {


    public static boolean isManyToOne(Class<?> clazz, String nombrePropiedad) {
        Field field = getField(clazz, nombrePropiedad);
        return field.isAnnotationPresent(ManyToOne.class);
    }

    public static boolean isOneToOne(Class<?> clazz, String nombrePropiedad) {
        Field field = getField(clazz, nombrePropiedad);
        return field.isAnnotationPresent(OneToOne.class);
    }

    public static boolean isOneToMany(Class<?> clazz, String nombrePropiedad) {
        Field field = getField(clazz, nombrePropiedad);
        return field.isAnnotationPresent(OneToMany.class);
    }

    public static String getMappedByInOneToMany(Class<?> clazz, String nombrePropiedad) {
        Field field = getField(clazz, nombrePropiedad);
        return field.getAnnotation(OneToMany.class).mappedBy();
    }

    public static String getMappedByInManyToMany(Class<?> clazz, String nombrePropiedad) {
        Field field = getField(clazz, nombrePropiedad);
        return field.getAnnotation(ManyToMany.class).mappedBy();
    }


    public static Field getField(Class<?> clazz, String nombrePropiedad) {
        try {
            Field field = null;
            Class<?> currentClass = clazz;
            while (currentClass != null && field == null) {
                try {
                    field = currentClass.getDeclaredField(nombrePropiedad);
                } catch (NoSuchFieldException e) {
                    // Field not found in this class, try the superclass
                    currentClass = currentClass.getSuperclass();
                }
            }

            if (field == null) {
                throw new NoSuchFieldException("El field '" + nombrePropiedad + "' no se encontró en la clase " + clazz.getName() + " ni en sus superclases declaradas.");
            }


            return field;
        } catch (Exception e) {
            throw new RuntimeException("Error al obtener mappedBy para el field " + nombrePropiedad + " en la clase " + clazz.getName(), e);
        }
    }


    public static Object findInCollectionById(Collection<?> collection, Long id) {
        if (collection == null || collection.isEmpty() || id == null) {
            return null;
        }

        for (Object model : collection) {
            if (model == null || getId(model) == null) {
                continue;
            }

            if (id.equals(getId(model))) {
                return model;
            }
        }

        return null;
    }

    public static Object removeInCollectionById(Collection<?> collection,Long id) {
        if (collection == null || collection.isEmpty() || id == null) {
            return null;
        }

        Iterator<?> iterator = collection.iterator();
        while (iterator.hasNext()) {
            Object model = iterator.next();
            if (model == null || getId(model) == null) {
                continue;
            }

            if (id.equals(getId(model))) {
                iterator.remove();
                return model;
            }
        }

        return null;
    }

    public static Long getId(Object object) {
        Long id;

        if (object==null) {
            throw  new IllegalArgumentException("Object is null");
        }

        if (object instanceof Model) {
            id = ((Model) object).getId();
        } else if (object instanceof Map) {
            id=ScalarMapper.getScalarFromObject(((Map)object).get("id"),Long.class);
        } else {
            throw  new IllegalArgumentException("Object no es Model ni es Map");
        }

        return id;
    }

}
