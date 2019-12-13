package com.gitlab.annotation;

/**
 * 封装订阅者的信息，包括订阅者的实例（通过EasyBus注册的接口）和通知方法
 */
public final class Subscription {
    public final Object subscriber;
    public final SubscriberMethod subscriberMethod;

    public Subscription(Object subscriber, SubscriberMethod subscriberMethod) {
        this.subscriber = subscriber;
        this.subscriberMethod = subscriberMethod;
    }

    public void notifySubscriber(Object event) {
        try {
            subscriberMethod.method.invoke(subscriber, event);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Subscription that = (Subscription) o;

        if (!subscriber.equals(that.subscriber)) return false;
        return subscriberMethod.equals(that.subscriberMethod);
    }

    @Override
    public int hashCode() {
        int result = subscriber.hashCode();
        result = 31 * result + subscriberMethod.hashCode();
        return result;
    }
}
