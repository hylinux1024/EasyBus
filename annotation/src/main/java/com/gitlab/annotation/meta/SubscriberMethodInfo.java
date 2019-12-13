package com.gitlab.annotation.meta;

/**
 * 用于编译期间生成的订阅者信息
 * 其实可以通过代码直接生成 SubscribeMethod
 * 但为了尽量与 EventBus 的流程保持一致
 * 这里也使用一个 SubscriberMethodInfo
 *
 */
public class SubscriberMethodInfo {
    private final String methodName;
    private final Class<?> eventType;

    public SubscriberMethodInfo(String methodName, Class<?> eventType) {
        this.methodName = methodName;
        this.eventType = eventType;
    }

    public String getMethodName() {
        return methodName;
    }

    public Class<?> getEventType() {
        return eventType;
    }
}
