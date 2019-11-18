package com.github.easybus;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class EasyBus {

    private static final String TAG = EasyBus.class.getSimpleName();

    /**
     * 通过EventType 查询订阅者信息（即注册EasyBus的Class类以及回调的通知方法）
     * 当调用post方法的时候，可以通过Event类查询到对应的订阅者信息
     * 从而快速的执行通知接口
     */
    private final Map<Class<?>, CopyOnWriteArrayList<Subscription>> subscriptionsByEventType;
    /**
     * 通过订阅者（即注册 EasyBus 的类实例）查询事件 EventType Class
     * 事件类型即为 onEvent 方法中的参数类型
     */
    private final Map<Object, List<Class<?>>> eventTypeBySubscriber;


    private EasyBus() {
        eventTypeBySubscriber = new HashMap<>();
        subscriptionsByEventType = new HashMap<>();
    }

    /**
     * 使用静态内部类的方式实现单例
     */
    private static class Holder {
        static EasyBus instance = new EasyBus();
    }

    public static EasyBus getInstance() {
        return Holder.instance;
    }

    public void register(Object subscriber) {
        Class<?> subscriberClass = subscriber.getClass();
        Method[] methods = subscriberClass.getDeclaredMethods();
        List<SubscriberMethod> subscriberMethods = new ArrayList<>();
        for (Method method : methods) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length != 1) {
                continue;
            }
            if (method.getName().startsWith("onEvent")) {
                subscriberMethods.add(new SubscriberMethod(method, parameterTypes[0]));
            }
        }
        synchronized (this) {
            for (SubscriberMethod method : subscriberMethods) {
                subscribe(subscriber, method);
            }
        }
    }

    private void subscribe(Object subscriber, SubscriberMethod method) {
        Logger.i(subscriber.getClass().getSimpleName() + ":" + method.method.getName());
        // 查询这个事件类型中对应的订阅者信息（即接收相同事件的注册者的方法有哪些）
        CopyOnWriteArrayList<Subscription> subscriptions = subscriptionsByEventType.get(method.eventType);
        if (subscriptions == null) {
            subscriptions = new CopyOnWriteArrayList<>();
        }
        subscriptions.add(new Subscription(subscriber, method));
        subscriptionsByEventType.put(method.eventType, subscriptions);
        // 查询注册者中的接收的事件 Event 类型（即 onEvent 方法中的参数类型）
        List<Class<?>> eventTypes = eventTypeBySubscriber.get(subscriber);
        if (eventTypes == null) {
            eventTypes = new CopyOnWriteArrayList<>();
        }
        eventTypes.add(method.eventType);

        eventTypeBySubscriber.put(subscriber, eventTypes);
    }

    public void unregister(Object subscriber) {
        List<Class<?>> eventTypes = eventTypeBySubscriber.get(subscriber);
        if (eventTypes != null) {
            for (Class<?> eventType : eventTypes) {
                unsubscribe(subscriber, eventType);
            }
        }
    }

    private void unsubscribe(Object subscriber, Class<?> eventType) {
        List<Subscription> subscriptionList = subscriptionsByEventType.get(eventType);
        if (subscriptionList == null) {
            return;
        }
        int size = subscriptionList.size();
        for (int i = 0; i < size; i++) {
            if (subscriptionList.get(i).subscriber == subscriber) {
                subscriptionList.remove(i);
                i--;
                size--;
            }
        }
    }

    public void post(Object event) {

        List<Subscription> subscriptionList = subscriptionsByEventType.get(event.getClass());
        if (subscriptionList == null) {
            Logger.i("not found subscriber");
            return;
        }
        for (Subscription subscription : subscriptionList) {
            subscription.notifySubscriber(event);
        }
    }
}
