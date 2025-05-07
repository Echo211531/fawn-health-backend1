package com.ljh.fawnhealth.utils;

import org.springframework.beans.BeanUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 工具类，提供对象拷贝和对象列表拷贝的功能，
 * 主要基于 Spring 的 BeanUtils 来实现属性复制操作。
 */
public class BeanCopyUtils {

    /**
     * 将一个源对象的属性值拷贝到一个新创建的目标对象实例中。
     *
     * @param source      源对象，要从中获取属性值的对象。
     * @param targetClass 目标对象的类类型，用于创建目标对象实例。
     * @param <S>         源对象的类型参数。
     * @param <T>         目标对象的类型参数。
     * @return 拷贝后的目标对象实例，如果源对象为 null，则返回 null。
     * @throws RuntimeException 如果在创建目标对象实例或复制属性时发生异常，
     *                          则会抛出包含异常信息的运行时异常。
     */
    public static <S, T> T copy(S source, Class<T> targetClass) {
        // 如果源对象为 null，直接返回 null
        if (source == null) return null;
        try {
            // 使用反射创建目标对象的实例
            T target = targetClass.getDeclaredConstructor().newInstance();
            // 使用 Spring 的 BeanUtils 将源对象的属性值复制到目标对象
            BeanUtils.copyProperties(source, target);
            // 返回拷贝后的目标对象
            return target;
        } catch (Exception e) {
            // 如果发生异常，抛出包含异常信息的运行时异常
            throw new RuntimeException("Bean 拷贝失败: " + e.getMessage(), e);
        }
    }

    /**
     * 将 source 对象的属性值拷贝到已存在的 target 对象中（不会创建新对象）。
     *
     * @param source 源对象
     * @param target 目标对象
     * @param <S>    源类型
     * @param <T>    目标类型
     */
    public static <S, T> void copy(S source, T target) {
        if (source == null || target == null) return;
        BeanUtils.copyProperties(source, target);
    }


    /**
     * 将一个源对象列表中的每个对象，拷贝成目标类型的对象，并组成一个新的列表返回。
     *
     * @param sourceList  源对象列表，包含要进行拷贝的对象。
     * @param targetClass 目标对象的类类型，用于创建每个目标对象实例。
     * @param <S>         源对象的类型参数。
     * @param <T>         目标对象的类型参数。
     * @return 包含拷贝后的目标对象的列表，如果源对象列表为 null，则返回一个空列表。
     * @throws RuntimeException 如果在拷贝过程中发生异常，比如某个对象拷贝失败，
     *                          则会抛出包含异常信息的运行时异常。
     */
    public static <S, T> List<T> copyList(List<S> sourceList, Class<T> targetClass) {
        // 如果源对象列表为 null，返回一个空列表
        if (sourceList == null) {
            return java.util.Collections.emptyList();
        }
        // 使用流操作对源对象列表中的每个对象进行拷贝，并收集成新的列表
        return sourceList.stream()
                .map(source -> copy(source, targetClass))
                .collect(Collectors.toList());
    }
}