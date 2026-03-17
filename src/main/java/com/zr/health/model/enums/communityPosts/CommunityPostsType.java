package com.zr.health.model.enums.communityPosts;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 社区帖子类型枚举类
 * 对应表字段：post_type（TINYINT类型）
 * 约定：1-4为业务类型，100+为系统扩展类型
 */
@Getter
@AllArgsConstructor
public enum CommunityPostsType {


    CHECK_IN(1, "打卡"),
    SHARE(2, "分享"),
    HELP(3, "求助"),
    TRANSCRIPT(4, "成绩单"),
    SYSTEM_ANNOUNCEMENT(101, "系统公告"),
    VOTE(102, "投票"),
    Q_A(103, "问答");

    @JsonValue
    @EnumValue
    private final int value;

    private final String desc;

    /**
     * 通过数值获取枚举实例（JSON反序列化专用）
     * @param value 数据库存储的数值
     * @return 枚举实例，未找到返回null
     */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static CommunityPostsType of(Integer value) {
        if (value == null) return null;
        for (CommunityPostsType type : values()) {
            if (type.value == value) {
                return type;
            }
        }
        return null;
    }

    /**
     * 获取所有枚举值列表（用于下拉菜单等场景）
     * @return 包含value和desc的键值对列表
     */
    public static List<Map<String, Object>> toList() {
        CommunityPostsType[] types = values();
        List<Map<String, Object>> result = new ArrayList<>(types.length);
        for (CommunityPostsType type : types) {
            result.add(Map.of("value", type.value, "desc", type.desc));
        }
        return result;
    }
}