package com.alex.message.utils;

import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * bean 工具类，提供基于bean的静态方法，比如bean转换为其他格式
 * 
 * @author Ternence
 *
 */
public class BeanUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(BeanUtils.class);

    /**
     * 对象转换 ,将source对象中的所有属性复制到clazz类的实例中并返回
     * 
     * 这里采用fastjson来实现转换操作，原因是先将对象序列化为串，然后在将串反序列化为另一对象，这一过程中不需要考虑类型不匹配的情况 ，
     * 比如：包装类型的Integer值为null，需要转换到int；又或者强类型转换，两个List泛型类型不一致的情况
     * 
     * @param source
     * @param clazz
     * @return
     * @date 2015年7月8日
     * @author Ternence
     */
    public static <T> T toBean(Object source, Class<T> clazz) {
        if (source == null) {
            return null;
        }
        try {
            String jsonstr = JSON.toJSONString(source);
            return JSON.parseObject(jsonstr, clazz);
        } catch (RuntimeException e) {
            LOGGER.error("", e);
        }
        return null;
    }

    /**
     * 克隆对象
     *
     * @param source
     * @return
     * @date 2017年6月7日
     * @author tanlin
     */
    @SuppressWarnings("unchecked")
    public static <T> T clone(T source) {
        try {
            T clone = (T) source.getClass().newInstance();
            org.springframework.beans.BeanUtils.copyProperties(source, clone);
            return clone;
        } catch (Exception e) {
            LOGGER.error("", e);
            return null;
        }
    }
}
