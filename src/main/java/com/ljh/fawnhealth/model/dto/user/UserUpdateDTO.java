package com.ljh.fawnhealth.model.dto.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

@Data
public class UserUpdateDTO {
    /**
     * 用户ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 头像URL
     */
    private String avatar;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 性别:0未知,1男,2女
     */
    private Integer gender;

    /**
     * 生日
     */
    private Date birthday;

    /**
     * 身高(cm)
     */
    private BigDecimal height;

    /**
     * 体重(kg)
     */
    private BigDecimal weight;

    /**
     * 目标体重(kg)
     */
    private BigDecimal targetWeight;

    /**
     * 目标天数
     */
    private Integer periodDays;
}