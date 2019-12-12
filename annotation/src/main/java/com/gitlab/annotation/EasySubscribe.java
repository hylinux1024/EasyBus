package com.gitlab.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义注解
 * 指定该注解修饰方法
 * 由于我们使用编译期间处理注解，所以指定其生命周期为只保留在源码文件中
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface EasySubscribe {
}

