package com.alex.message.utils;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class ReflectUtil {

    /**
     * 获取对象泛型的实际类型
     */
    public static Class getActualType(Object obj) {
        Type type = obj.getClass().getGenericSuperclass();
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            return (Class) parameterizedType.getActualTypeArguments()[0];
        } else {
            return String.class;
        }
    }
}
