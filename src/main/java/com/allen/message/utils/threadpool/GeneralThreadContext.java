package com.allen.message.utils.threadpool;

import java.util.HashMap;
import java.util.Map;

public class GeneralThreadContext {
    private static final ThreadLocal<GeneralThreadContext> LOCAL = ThreadLocal.withInitial(GeneralThreadContext::new);
    private final Map<String, Object> values = new HashMap<String, Object>();

    public static GeneralThreadContext getContext() {
        return LOCAL.get();
    }

    public static void clear() {
        LOCAL.remove();
    }

    protected GeneralThreadContext() {}

    public Map<String, Object> get() {
        return values;
    }

    public GeneralThreadContext set(String key, Object value) {
        if (value == null) {
            values.remove(key);
        } else {
            values.put(key, value);
        }
        return this;
    }

    public GeneralThreadContext remove(String key) {
        values.remove(key);
        return this;
    }

    public Object get(String key) {
        return values.get(key);
    }

    /**
     * 复制并set当前context
     *
     * @param context
     */
    public static void setContext(GeneralThreadContext context) {
        GeneralThreadContext.clear();
        if (context == null || context.isEmpty()) {
            return;
        }
        GeneralThreadContext localContext = GeneralThreadContext.getContext();
        for (Map.Entry<String, Object> element : context.get().entrySet()) {
            localContext.set(element.getKey(), element.getValue());
        }
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public Map<String, Object> entriesWithKeyPrefix(String prefix) {
        Map<String, Object> matchingEntries = new HashMap<>();
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            if (entry.getKey().startsWith(prefix)) {
                matchingEntries.put(entry.getKey(), entry.getValue());
            }
        }
        return matchingEntries;
    }
}
