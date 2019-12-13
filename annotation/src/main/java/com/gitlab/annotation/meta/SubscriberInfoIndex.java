package com.gitlab.annotation.meta;

/**
 * 订阅者的索引接口
 * 通过Class获取到该Class下定义的被标记的 @EasySubscribe 方法
 */
public interface SubscriberInfoIndex {
    SubscriberInfo getSubscriberInfo(Class<?> subscriberClass);
}