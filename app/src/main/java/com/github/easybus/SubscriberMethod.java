package com.github.easybus;

import java.lang.reflect.Method;

/**
 * 封装订阅者的通知方法（onEvent方法）以及方法中参数的（事件）类型（Class类型参数）
 */
public class SubscriberMethod {
    final Method method;
    final Class<?> eventType;

    public SubscriberMethod(Method method, Class<?> eventType) {
        this.method = method;
        this.eventType = eventType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SubscriberMethod that = (SubscriberMethod) o;

        if (method != null ? !method.equals(that.method) : that.method != null) return false;
        return eventType != null ? eventType.equals(that.eventType) : that.eventType == null;
    }

    @Override
    public int hashCode() {
        int result = method != null ? method.hashCode() : 0;
        result = 31 * result + (eventType != null ? eventType.hashCode() : 0);
        return result;
    }
}
