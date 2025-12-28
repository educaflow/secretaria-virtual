package com.educaflow.common.mapper;

import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import java.lang.reflect.Field;

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


}
