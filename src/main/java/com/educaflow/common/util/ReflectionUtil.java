package com.educaflow.common.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class ReflectionUtil {


    public static boolean hasMethod(Class<?> baseClass, String methodName, Class<?> returnClass, Class<? extends Annotation> annotation, Class<?>[] parameterTypes) {
            Method method=getMethod(baseClass, methodName, returnClass, annotation, parameterTypes);

            if (method!=null) {
                return true;
            } else {
                return false;
            }

    }

    public static Method getMethod(Class<?> baseClass, String methodName, Class<?> returnClass, Class<? extends Annotation> annotation, Class<?>[] parameterTypes) {
        List<Method> matchingMethods = new ArrayList<>();

        for (Method method : baseClass.getDeclaredMethods()) {

            if (methodName!=null) {
                if (!method.getName().equals(methodName)) {
                    continue;
                }
            }

            if (returnClass!=null) {
                if (!returnClass.isAssignableFrom(method.getReturnType())) {
                    continue;
                }
            }

            if (annotation!=null) {
                if (!method.isAnnotationPresent(annotation)) {
                    continue;
                }
            }


            if (parameterTypes!=null) {
                Class<?>[] methodParamTypes = method.getParameterTypes();
                if (methodParamTypes.length != parameterTypes.length) {
                    continue;
                }


                boolean paramsMatch = true;
                for (int i = 0; i < parameterTypes.length; i++) {
                    if (!methodParamTypes[i].equals(parameterTypes[i])) {
                        paramsMatch = false;
                        break;
                    }
                }

                if (paramsMatch==false) {
                    continue;
                }
            }

            matchingMethods.add(method);

        }

        if (matchingMethods.size() > 1) {
            throw new RuntimeException("Se encontró más de un método: " + methodName + " en la clase: " + baseClass.getName() + " con Nº parámetros: " + (parameterTypes != null ? parameterTypes.length : "N/A") + " y retorno: " + (returnClass != null ? returnClass.getName() : "N/A") + " y la anotación: " + (annotation != null ? annotation.getName() : "N/A"));
        }

        if (matchingMethods.size() == 1) {
            return matchingMethods.get(0);
        } else {
            return null;
        }


    }

    public static Enum getEnumConstant(Class<? extends Enum> enumClass, String constantName) {
        if (enumClass == null || constantName == null || constantName.isEmpty()) {
            throw new IllegalArgumentException("Enum class and constant name must not be null or empty");
        }

        try {
            return (Enum)Enum.valueOf(enumClass, constantName);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("No enum constant " + enumClass.getName() + "." + constantName, e);
        }
    }


    public static Object getFieldValue(Object obj, String fieldName) {
        try {
            Field profileField = obj.getClass().getDeclaredField(fieldName);
            profileField.setAccessible(true);
            return profileField.get(obj);
        } catch (Exception ex) {
            throw new RuntimeException("No se pudo acceder al campo '" + fieldName + "' del objeto " + obj, ex);
        }
    }



}
