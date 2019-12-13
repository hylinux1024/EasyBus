package com.gitlab.annotation.meta;

import com.gitlab.annotation.SubscriberMethod;

import java.lang.reflect.Method;

/**
 * 订阅者信息
 * 主要是从注解中解析出Class以及通知方法（即被@EasySubscribe标记的方法）
 */
public class SubscriberInfo {
    private Class subscriberClass;
    private SubscriberMethodInfo[] subscriberMethodInfos;

    public SubscriberInfo(Class subscriberClass, SubscriberMethodInfo[] subscriberMethods) {
        this.subscriberClass = subscriberClass;
        this.subscriberMethodInfos = subscriberMethods;
    }

    public Class getSubscriberClass() {
        return subscriberClass;
    }

    public synchronized SubscriberMethod[] getSubscriberMethods() {
        int length = subscriberMethodInfos.length;

        SubscriberMethod[] methods = new SubscriberMethod[length];
        for (int i = 0; i < length; i++) {
            SubscriberMethodInfo info = subscriberMethodInfos[i];
            SubscriberMethod method = createSubscribeMethod(info);
            if (method != null) {
                methods[i] = method;
            }
        }
        return methods;
    }

    private SubscriberMethod createSubscribeMethod(SubscriberMethodInfo info) {
        try {
            Method method = subscriberClass.getDeclaredMethod(info.getMethodName(), info.getEventType());
            return new SubscriberMethod(method, info.getEventType());
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

        return null;
    }
}
