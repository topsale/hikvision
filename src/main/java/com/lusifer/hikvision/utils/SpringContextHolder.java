package com.lusifer.hikvision.utils;

import org.springframework.aop.framework.AopContext;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Spring Bean 管理工具
 */
public final class SpringContextHolder implements BeanFactoryPostProcessor, ApplicationContextAware {

    /**
     * Bean 工厂
     */
    private static ConfigurableListableBeanFactory configurableListableBeanFactory;

    /**
     * IoC 容器
     */
    private static ApplicationContext applicationContext;

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory configurableListableBeanFactory) throws BeansException {
        SpringContextHolder.configurableListableBeanFactory = configurableListableBeanFactory;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        SpringContextHolder.applicationContext = applicationContext;
    }

    /**
     * 获取对象
     */
    @SuppressWarnings("unchecked")
    public static <T> T getBean(String name) throws BeansException {
        return (T) configurableListableBeanFactory.getBean(name);
    }

    /**
     * 获取对象
     */
    public static <T> T getBean(Class<T> clz) throws BeansException {
        return (T) configurableListableBeanFactory.getBean(clz);
    }

    /**
     * 获取 AOP 代理
     */
    @SuppressWarnings("unchecked")
    public static <T> T getAopProxy(T invoker) {
        return (T) AopContext.currentProxy();
    }

    /**
     * 获取当前的环境配置
     *
     * @return 没有则返回 null
     */
    public static String[] getActiveProfiles() {
        return applicationContext.getEnvironment().getActiveProfiles();
    }

}
