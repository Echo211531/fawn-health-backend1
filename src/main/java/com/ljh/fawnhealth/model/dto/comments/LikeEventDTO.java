package com.ljh.fawnhealth.model.dto.comments;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LikeEventDTO {
    private Long commentId;
    private Long userId;
    private Boolean liked; // true 点赞，false 取消点赞
}
