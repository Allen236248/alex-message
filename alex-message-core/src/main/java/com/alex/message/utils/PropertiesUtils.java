package com.alex.message.utils;

import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.util.StringValueResolver;

@Component
public class PropertiesUtils implements EmbeddedValueResolverAware {

    private StringValueResolver stringValueResolver;

    @Override
    public void setEmbeddedValueResolver(StringValueResolver resolver) {
        stringValueResolver = resolver;
    }

    public String getPropertiesValue(String name) {
        name = "${" + name + "}";
        return stringValueResolver.resolveStringValue(name);
    }
}