# 100行代码拆解EventBus
>关于我
>一个有思想的程序猿，终身学习实践者，目前在一个创业团队任team lead，技术栈涉及Android、Python、Java和Go，这个也是我们团队的主要技术栈。
Github：https://github.com/hylinux1024
微信公众号：终身开发者(angrycode)

`EventBus` 作为一个基础的消息传递组件，了解其核心实现原理是日常开发工作之外需要做的必修课。本系列希望通过自己实现一个类似的消息传递组件 `EasyBus` 来理解 `EventBus` 的核心实现原理。

![EventBus](https://wx3.sinaimg.cn/mw690/5f90ffefgy1g8xkhxrblpj20zk0db74s.jpg)
从官方的原理图可以直观的看出 `EventBus` 是一个基于订阅发布的消息传递组件。订阅者通过 `EventBus` 进行注册，登记自己感兴趣的事件 `Event`，发布者将 `Event` 发送到 `EventBus` 后就将会查询监听事件 `Event` 的订阅者，然后将事件 `Event` 转发到订阅者中。

可以发现其实就是观察者模式的实现。

![Observer](https://wx3.sinaimg.cn/mw690/5f90ffefgy1g925zthwhpj210o0iw40t.jpg)

观察者模式中，被观察者（`Subject`）内部会通过 `List` 来存储观察者的实例，然后就通过 `notify()` 方法可以通知观察者。
在 `EventBus` 库中，定义了 `onEventXXXX(MessageEvent)` 方法的类就是**观察者**，而 `EventBus` 自己就是**被观察者**。

接下来实现一个简易版本的消息传递 `EasyBus` 。

首先预览下整体的类结构

```Java
com/github/easybus
├── EasyBus.java
├── Logger.java
├── SubscriberMethod.java
├── Subscription.java
└── demo
    ├── MainActivity.kt
    └── MessageEvent.java
```

参考 `EventBus` 定义了一个 `EasyBus` 类，这个将是我们使用 `EasyBus` 的主要入口。


#### 单例方法

```
public class EasyBus {
    ...
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
    ...
}
```
在 `EasyBus` 中使用静态内部类的方式实现单例，这样可以保证在一个进程内只有一个 `EasyBus` 的实例。
这里有两个变量需要重点解释一下：
```
    /**
     * 通过 EventType 查询订阅者信息（即注册EasyBus的Class类以及回调的通知方法）
     * 当调用post方法的时候，可以通过Event类查询到对应的订阅者信息
     * 从而快速的执行通知接口
     */
    private final Map<Class<?>, CopyOnWriteArrayList<Subscription>> subscriptionsByEventType;
    /**
     * 通过订阅者（即注册 EasyBus 的类实例）查询事件 EventType Class
     * 事件类型即为 onEvent 方法中的参数类型
     */
    private final Map<Object, List<Class<?>>> eventTypeBySubscriber;
```

这两个变量都是 `Map` 类型。`subscriptionsByEventType` 的 `key` 是 `Event` 类的类型，`value` 则订阅者的列表信息，通过 `Subscription` 类来封装；而 `eventTypeBySubscriber` 的 `key` 是订阅者实例，`value` 是 `Event` 类的类型。

回忆上面的观察者模式类图，**被观察者**需要有一个 `List` 用来存储**观察者列表**，这里的观察者列表就是通过上面两个 `Map` 变量来实现的。

#### register

```Java
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
```

注册方法的作用是**解析观察者的方法和事件类型**，然后将**观察者**信息添加到`被观察者`中的 `List` 里。实现逻辑也很简单，通过 `Class` 类进行反射调用类中的方法，过滤参数个数为1，且方法名称是以 `onEvent*` 开头的方法，就把其添加到 `subscriberMethods` 列表中。其中 `SubscriberMethod` 是订阅者的接收通知的方法（可以对比观察者模式中的 `update` 方法），它**封装了方法名称、和方法中的参数类型（即事件类型）。**

接下来就调用 `subscribe` 方法实现订阅逻辑。

```Java
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
              = new CopyOnWriteArrayList<>();
        }
        eventTypes.add(method.eventType);

        eventTypeBySubscriber.put(subscriber, eventTypes);
    }
```

该方法的主要作用就是将上面解析到的方法和事件类型添加到 `subscriptionsByEventType` 和 `eventTypeBySubscriber` 这两个 `Map` 中。
首先查询 `subscriptionsByEventType` 是否有对应的订阅信息，如果没有则初始化列表 `subscriptions` 。然后将订阅者实例(`subscriber`)与订阅者中的方法(`method`)封装成 `Subscription` 。最后添加到 `subscriptions` 中。
同样地查询 `eventTypeBySubscriber` 中是否有对应的事件类型，没有则初始化列表 `eventTypes`。之后将 `eventTypes` 存储在 `eventTypeBySubscriber` 这个 `Map` 里。

#### post

```Java
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
```

`post` 逻辑也很简单，主要通过事件类型查询对应的订阅者信息 `List` ，然后遍历该列表进行消息事件通知。

订阅者信息类主要封装订阅者实例信息和对应的方法信息
```Java
final class Subscription {
    final Object subscriber;
    final SubscriberMethod subscriberMethod;

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
    ...
}
```
`Subscription` 中还有一个重要的方法 `notifySubscriber` , 它是通过反射对订阅者的方法进行调用。

#### unregister

梳理了上面的 `register` 方法 `unregister` 方法就比较容易理解了。

```Java
public void unregister(Object subscriber) {
        List<Class<?>> eventTypes = eventTypeBySubscriber.get(subscriber);
        if (eventTypes != null) {
            for (Class<?> eventType : eventTypes) {
                unsubscribe(subscriber, eventType);
            }
        }
    }
```
通过订阅者实例信息从 `eventTypeBySubscriber` 查询到订阅的事件类型列表，然后遍历 `eventTypes` 执行 `unsubscribe` 方法。

```Java
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
```
在 `unsubscribe` 方法中将订阅者信息从订阅者列表中移除，就完成了退订功能。

#### 引用

- https://github.com/greenrobot/EventBus
- https://github.com/hylinux1024/EasyBus
本文源码

