package com.alex.message.utils;

import com.alex.message.consumer.handler.MessageHandler;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class ReflectionUtils {

    /**
     * 获取对象泛型的实际类型
     */
    public static Class getActualType(Object obj, Class<?> clazz) {
        Type type = obj.getClass().getGenericSuperclass();
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            return (Class) parameterizedType.getActualTypeArguments()[0];
        }
        Type[] types = obj.getClass().getGenericInterfaces();
        for (Type _type : types) {
            if (!(_type instanceof ParameterizedType))
                continue;
            ParameterizedType t = (ParameterizedType) _type;
            String typeName = t.getRawType().getTypeName();
            if (clazz.getName().equals(typeName)) {
                return (Class) t.getActualTypeArguments()[0];
            }
        }
        return String.class;
    }
}
