package com.ljh.fawnhealth.model.enums.user;

/**
 * 用户角色枚举类，定义了系统中不同类型的用户角色。
 * 每个角色都有一个对应的描述信息，用于更清晰地表示该角色的含义。
 * 这些角色可用于权限管理，不同角色在系统中拥有不同的操作权限。
 */
public enum UserRole {
    /**
     * 普通用户角色，是系统中最常见的用户类型。
     * 普通用户通常具有基本的使用系统功能的权限，
     * 例如查看食物信息、记录饮食等，但可能没有管理系统设置、用户信息等高级权限。
     */
    USER("user"),

    /**
     * 管理员角色，拥有比普通用户更多的系统管理权限。
     * 管理员可以进行一些系统配置、用户管理等操作，
     * 例如添加、删除食物分类，管理用户账号状态等，但可能无法进行一些涉及系统核心设置的操作。
     */
    ADMIN("admin"),

    /**
     * 超级管理员角色，是系统中权限最高的角色。
     * 超级管理员拥有系统的所有权限，可以进行全面的系统管理和设置，
     * 包括对管理员账号的管理、系统级别的参数配置等。
     */
    SUPER_ADMIN("super_admin");

    /**
     * 角色的描述信息，用于更直观地表示该角色的含义和用途。
     */
    private final String description;

    /**
     * 枚举类的构造函数，用于初始化每个角色的描述信息。
     *
     * @param description 角色的描述信息
     */
    UserRole(String description) {
        this.description = description;
    }

    /**
     * 获取角色的描述信息。
     *
     * @return 角色的描述信息
     */
    public String getDescription() {
        return description;
    }
}