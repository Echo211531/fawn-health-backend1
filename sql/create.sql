CREATE TABLE `user` (
                        `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
                        `email` varchar(100) NOT NULL COMMENT '邮箱',
                        `email_verified` tinyint(1) NOT NULL DEFAULT '0' COMMENT '邮箱验证状态',
                        `password` varchar(255) DEFAULT NULL COMMENT '密码(加密存储，可选)',
                        `avatar` varchar(255) DEFAULT NULL COMMENT '头像URL',
                        `nickname` varchar(50) DEFAULT NULL COMMENT '用户昵称',
                        `role` varchar(20) NOT NULL DEFAULT 'user' COMMENT '角色:user-普通用户,admin-管理员,super_admin-超级管理员',
                        `gender` tinyint NOT NULL DEFAULT '0' COMMENT '性别:0未知,1男,2女',
                        `birthday` date DEFAULT NULL COMMENT '生日',
                        `height` decimal(5,2) DEFAULT NULL COMMENT '身高(cm)',
                        `weight` decimal(5,2) DEFAULT NULL COMMENT '体重(kg)',
                        `target_weight` decimal(5,2) DEFAULT NULL COMMENT '目标体重(kg)',
                        `bmi` decimal(5,2) DEFAULT NULL COMMENT 'BMI指数',
                        `daily_calories` decimal(10,2) DEFAULT NULL COMMENT '每日建议摄入热量(大卡)',
                         -- VIP相关字段
                        `is_vip` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否VIP:0否,1是',
                        `vip_expire_time` datetime DEFAULT NULL COMMENT 'VIP过期时间',
                        `vip_level` tinyint DEFAULT '0' COMMENT 'VIP等级',

                        `status` tinyint NOT NULL DEFAULT '1' COMMENT '账号状态:0禁用,1正常',
                        `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
                        `last_login_ip` varchar(50) DEFAULT NULL COMMENT '最后登录IP',
                        `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                        `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                        PRIMARY KEY (`id`),
                        UNIQUE KEY `idx_email` (`email`),
                        KEY `idx_nickname` (`nickname`),
                        KEY `idx_vip` (`is_vip`, `vip_expire_time`) COMMENT 'VIP状态索引',
                        KEY `idx_role` (`role`) COMMENT '角色索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE `food_library` (
                                `id` bigint NOT NULL AUTO_INCREMENT COMMENT '食物ID',
                                `name` varchar(100) NOT NULL COMMENT '食物名称',
                                `category_id` bigint DEFAULT NULL COMMENT '分类ID',
                                `category_name` varchar(50) DEFAULT NULL COMMENT '分类名称',
                                `calories` decimal(8,2) NOT NULL COMMENT '热量(kcal/100g)',
                                `protein` decimal(8,2) DEFAULT NULL COMMENT '蛋白质(g/100g)',
                                `fat` decimal(8,2) DEFAULT NULL COMMENT '脂肪(g/100g)',
                                `carbohydrate` decimal(8,2) DEFAULT NULL COMMENT '碳水化合物(g/100g)',
                                `fiber` decimal(8,2) DEFAULT NULL COMMENT '膳食纤维(g/100g)',
                                `image` varchar(255) DEFAULT NULL COMMENT '图片URL',
                                `is_common` tinyint DEFAULT '0' COMMENT '是否常见食物:0否,1是',
                                `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                `is_delete` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除:0否,1是',
                                PRIMARY KEY (`id`),
                                KEY `idx_name` (`name`),
                                KEY `idx_category` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='食物库表';

CREATE TABLE `food_categories` (
                                   `id` bigint NOT NULL AUTO_INCREMENT COMMENT '分类ID',
                                   `name` varchar(50) NOT NULL COMMENT '分类名称',
                                   `icon` varchar(255) DEFAULT NULL COMMENT '分类图标',
                                   `description` varchar(255) DEFAULT NULL COMMENT '分类描述',
                                   `sort_order` int DEFAULT '0' COMMENT '排序权重',
                                   `status` tinyint DEFAULT '1' COMMENT '状态:0禁用,1启用',
                                   `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                   `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                   `is_delete` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除:0否,1是',
                                   PRIMARY KEY (`id`),
                                   UNIQUE KEY `idx_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='食物分类表';

CREATE TABLE `coupons` (
                           `id` bigint NOT NULL AUTO_INCREMENT COMMENT '优惠券ID',
                           `name` varchar(100) NOT NULL COMMENT '优惠券名称',
                           `type` tinyint NOT NULL DEFAULT '1' COMMENT '优惠券类型，1：普通券。目前就一种，保留字段',
                           `discount_type` tinyint NOT NULL COMMENT '折扣类型，1：满减，2：每满减，3：折扣，4：无门槛',
                           `specific` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否限定作用范围，false：不限定，true：限定。默认false',
                           `discount_value` int NOT NULL DEFAULT '1' COMMENT '折扣值，如果是满减则存满减金额，如果是折扣，则存折扣率，8折就是存80',
                           `threshold_amount` int NOT NULL DEFAULT '0' COMMENT '使用门槛，0：表示无门槛，其他值：最低消费金额',
                           `max_discount_amount` DECIMAL(10, 2) DEFAULT 0 COMMENT '最高优惠金额，满减最大，0：表示没有限制，不为0，则表示该券有金额上限',
                           `obtain_way` tinyint NOT NULL DEFAULT '0' COMMENT '获取方式：1：手动领取，2：兑换码',
                           `issue_begin_time` DATETIME COMMENT '开始发放时间',
                           `issue_end_time` DATETIME COMMENT '结束发放时间',
                           `term_days` INT DEFAULT 0 COMMENT '优惠券有效期天数，0：表示有效期是指定有效期的',
                           `term_begin_time` DATETIME COMMENT '优惠券有效期开始时间',
                           `term_end_time` DATETIME COMMENT '优惠券有效期结束时间',
                           `status` TINYINT DEFAULT 1 COMMENT '优惠券配置状态，1：待发放，2：未开始，3：进行中，4：已结束，5：未知（可根据实际情况调整）',
                           `total_num` int NOT NULL DEFAULT '0' COMMENT '总数量，不超过5000',
                           `issue_num` int NOT NULL DEFAULT '0' COMMENT '已发行数量，用于判断是否超发',
                           `used_num` int NOT NULL DEFAULT '0' COMMENT '已使用数量',
                           `user_limit` int NOT NULL DEFAULT '1' COMMENT '每个人限领的数量，默认1',
                           `ext_param` TEXT COMMENT '拓展参数字段，保留字段',
                           `code` varchar(50) NOT NULL COMMENT '优惠券代码',
                           `description` text COMMENT '优惠券描述',
                           `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                           `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                           `is_delete` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除',
                           PRIMARY KEY (`id`),
                           UNIQUE KEY `idx_code` (`code`),
                           KEY `idx_time` (`issue_begin_time`, `issue_end_time`),
                           KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券表';

CREATE TABLE `coupons_scope` (
                                 `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
                                 `type` tinyint NOT NULL COMMENT '范围限定类型：1-分类，等等',
                                 `coupon_id` bigint NOT NULL COMMENT '优惠券ID',
                                 `biz_id` bigint NOT NULL COMMENT '优惠券作用范围的业务id,例如商品的分类id',
                                 INDEX idx_coupon_id (coupon_id),
                                 INDEX idx_biz_id (biz_id),
                                 INDEX idx_coupon_biz (coupon_id, biz_id),
                                 PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券使用范围表';

CREATE TABLE `exchange_code` (
                                 `id` bigint NOT NULL COMMENT '兑换码id',
                                 `code` varchar(10) NOT NULL COMMENT '兑换码',
                                 `status` tinyint NOT NULL DEFAULT 1 COMMENT '兑换码状态，1：待兑换，2：已兑换，3：兑换活动已结束',
                                 `user_id` bigint NOT NULL DEFAULT '0' COMMENT '兑换人',
                                 `type` tinyint NOT NULL DEFAULT 1 COMMENT '兑换类型，1：优惠券，以后再添加其它类型',
                                 `exchange_target_id` bigint COMMENT '兑换码目标id，例如兑换优惠券，该id则是优惠券的配置id',
                                 `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                 `expired_time` datetime NOT NULL COMMENT '兑换码过期时间',
                                 `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                 INDEX idx_code (code),
                                 INDEX idx_status (status),
                                 INDEX idx_type (type),
                                 PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='兑换码表';

CREATE TABLE `user_coupons` (
                                `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
                                `user_id` bigint NOT NULL COMMENT '用户ID',
                                `coupon_id` bigint NOT NULL COMMENT '优惠券ID',
                                `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态:0未使用,1已使用,2已过期',
                                `get_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '领取时间',
                                `use_time` datetime DEFAULT NULL COMMENT '使用时间',
                                `order_no` varchar(50) DEFAULT NULL COMMENT '使用的订单号',
                                `expire_time` datetime NOT NULL COMMENT '过期时间',
                                `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                `is_delete` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除',
                                PRIMARY KEY (`id`),
                                UNIQUE KEY `idx_user_coupon` (`user_id`,`coupon_id`),
                                KEY `idx_status` (`status`),
                                KEY `idx_expire` (`expire_time`),
                                KEY `idx_user_id` (`user_id`),
                                KEY `idx_coupon_id` (`coupon_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户优惠券表';

CREATE TABLE `user_coupon`  (
                                `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '用户券id',
                                `user_id` bigint(0) NOT NULL COMMENT '优惠券的拥有者',
                                `coupon_id` bigint(0) NOT NULL COMMENT '优惠券模板id',
                                `term_begin_time` datetime(0) NULL DEFAULT NULL COMMENT '优惠券有效期开始时间',
                                `term_end_time` datetime(0) NOT NULL COMMENT '优惠券有效期结束时间',
                                `used_time` datetime(0) NULL DEFAULT NULL COMMENT '优惠券使用时间（核销时间）',
                                `status` tinyint(0) NOT NULL DEFAULT 1 COMMENT '优惠券状态，1：未使用，2：已使用，3：已失效',
                                `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
                                `update_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
                                PRIMARY KEY (`id`) USING BTREE,
                                INDEX `idx_coupon`(`coupon_id`) USING BTREE,
                                INDEX `idx_user_coupon`(`user_id`, `coupon_id`) USING BTREE
) ENGINE=InnoDB  CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户领取优惠券的记录，是真正使用的优惠券信息' ROW_FORMAT = Dynamic;

CREATE TABLE `coupon_usage_logs` (
                                     `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
                                     `user_id` bigint NOT NULL COMMENT '用户ID',
                                     `coupon_id` bigint NOT NULL COMMENT '优惠券ID',
                                     `order_no` varchar(50) NOT NULL COMMENT '订单编号',
                                     `discount_amount` decimal(10,2) NOT NULL COMMENT '优惠金额',
                                     `order_amount` decimal(10,2) NOT NULL COMMENT '订单金额',
                                     `use_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '使用时间',
                                     `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                     PRIMARY KEY (`id`),
                                     KEY `idx_user_id` (`user_id`),
                                     KEY `idx_coupon_id` (`coupon_id`),
                                     KEY `idx_order` (`order_no`),
                                     KEY `idx_use_time` (`use_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券使用记录表';

CREATE TABLE `community_posts` (
                                   `id` bigint NOT NULL AUTO_INCREMENT COMMENT '帖子ID',
                                   `user_id` bigint NOT NULL COMMENT '用户ID',
                                   `title` varchar(100) DEFAULT NULL COMMENT '标题',
                                   `cover_img` varchar(1024) DEFAULT NULL COMMENT '封面',
                                   `content` text NOT NULL COMMENT '内容',
                                   `is_public` tinyint NOT NULL DEFAULT '1' COMMENT '是否公开:0私密,1公开',
                                   `post_type` tinyint NOT NULL COMMENT '类型:1打卡,2分享,3求助,4成绩单',
                                   `images` varchar(1000) DEFAULT NULL COMMENT '图片URL,多个用逗号分隔',
                                   `like_count` int DEFAULT '0' COMMENT '点赞数',
                                   `comment_count` int DEFAULT '0' COMMENT '评论数',
                                   `share_count` int DEFAULT '0' COMMENT '分享数',
                                   `is_top` tinyint DEFAULT '0' COMMENT '是否置顶:0否,1是',
                                   `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                   `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                   `is_delete` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除:0否,1是',
                                   PRIMARY KEY (`id`),
                                   KEY `idx_user_id` (`user_id`),
                                   KEY `idx_post_type` (`post_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='社区帖子表';

CREATE TABLE `post_likes` (
                              `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
                              `post_id` bigint NOT NULL COMMENT '帖子ID',
                              `user_id` bigint NOT NULL COMMENT '点赞用户ID',
                              `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                              `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                              `is_delete` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除:0否,1是',
                              PRIMARY KEY (`id`),
                              UNIQUE KEY `idx_post_user` (`post_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='帖子点赞表';

CREATE TABLE `diet_records` (
                                `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主记录ID',
                                `user_id` BIGINT NOT NULL COMMENT '用户ID',
                                `meal_type` TINYINT NOT NULL COMMENT '餐次类型（1早餐，2午餐，3晚餐，4加餐）',
                                `record_date` DATE NOT NULL COMMENT '记录日期（仅日期部分）',
                                `record_time` TIME DEFAULT NULL COMMENT '记录时间（具体时刻）',
                                `total_calories` DECIMAL(10,2) DEFAULT 0 COMMENT '总热量(kcal)', -- 新增字段
                                `note` VARCHAR(255) DEFAULT NULL COMMENT '备注信息',
                                `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
                                PRIMARY KEY (`id`),
                                KEY `idx_user_date` (`user_id`, `record_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='饮食记录主表';


CREATE TABLE `diet_food_items` (
                                   `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                   `record_id` BIGINT NOT NULL COMMENT '关联diet_records主键ID',
                                   `food_id` BIGINT DEFAULT NULL COMMENT '食物ID（可为空，若手动添加）',
                                   `food_name` VARCHAR(100) NOT NULL COMMENT '食物名称',
                                   `amount` DECIMAL(8,2) NOT NULL COMMENT '食用数量',
                                   `unit` VARCHAR(20) NOT NULL COMMENT '单位（g/ml/份）',
                                   `calories` DECIMAL(8,2) DEFAULT NULL COMMENT '热量(kcal)',
                                   `protein` DECIMAL(8,2) DEFAULT NULL COMMENT '蛋白质(g)',
                                   `fat` DECIMAL(8,2) DEFAULT NULL COMMENT '脂肪(g)',
                                   `carbohydrate` DECIMAL(8,2) DEFAULT NULL COMMENT '碳水化合物(g)',
                                   `images` VARCHAR(1000) DEFAULT NULL COMMENT '食物图片（多个用逗号分隔）',
                                   `note` VARCHAR(255) DEFAULT NULL COMMENT '备注信息',
                                   `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                   `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                   `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
                                   PRIMARY KEY (`id`),
                                   KEY `idx_record_id` (`record_id`),
                                   CONSTRAINT `fk_record_id` FOREIGN KEY (`record_id`) REFERENCES `diet_records`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='饮食记录-食物项表';

CREATE TABLE `vip_orders` (
                              `id` bigint NOT NULL AUTO_INCREMENT COMMENT '订单ID',
                              `order_no` varchar(50) NOT NULL COMMENT '订单编号',
                              `user_id` bigint NOT NULL COMMENT '用户ID',
                              `vip_type` tinyint NOT NULL COMMENT '会员类型:1月卡,2季卡,3年卡',
                              `vip_plan_id` bigint DEFAULT NULL COMMENT '会员方案ID（预留字段）',
                              `amount` decimal(10,2) NOT NULL COMMENT '订单金额',
                              `discount_amount` decimal(10,2) DEFAULT '0.00' COMMENT '优惠金额',
                              `final_amount` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '最终支付金额，等于订单金额减去优惠金额',
                              `payment_method` tinyint DEFAULT NULL COMMENT '支付方式:1微信,2支付宝,3苹果支付',
                              `trade_no` varchar(100) DEFAULT NULL COMMENT '第三方支付订单号',
                              `payment_time` datetime DEFAULT NULL COMMENT '支付时间',
                              `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态:0未支付,1已支付,2已取消,3已退款',
                              `start_time` datetime DEFAULT NULL COMMENT '会员开始时间',
                              `end_time` datetime DEFAULT NULL COMMENT '会员结束时间',
                              `coupon_id` bigint DEFAULT NULL COMMENT '使用的优惠券ID',
                              `refund_amount` decimal(10,2) DEFAULT NULL COMMENT '退款金额',
                              `refund_time` datetime DEFAULT NULL COMMENT '退款时间',
                              `source` tinyint DEFAULT NULL COMMENT '订单来源:1App,2小程序,3H5,4后台',
                              `is_renewal` tinyint DEFAULT 0 COMMENT '是否为自动续费订单: 0否, 1是',
                              `remark` varchar(255) DEFAULT NULL COMMENT '订单备注',
                              `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                              `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                              `is_delete` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除',
                              PRIMARY KEY (`id`),
                              UNIQUE KEY `idx_order_no` (`order_no`),
                              KEY `idx_user_id` (`user_id`),
                              KEY `idx_create_time` (`create_time`),
                              KEY `idx_status` (`status`),
                              KEY `idx_final_amount` (`final_amount`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员订单表';

CREATE TABLE `vip_benefits` (
                                `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                `vip_type` tinyint NOT NULL COMMENT '会员类型: 1月卡, 2季卡, 3年卡（或绑定vip_orders的vip_type）',
                                `benefit_code` varchar(50) NOT NULL COMMENT '权益编码，例如：DAILY_REPORT、NO_ADS、UNLOCK_RECIPES',
                                `benefit_name` varchar(100) NOT NULL COMMENT '权益名称',
                                `price` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '权益价格（针对单独购买场景）', -- 新增字段
                                `description` varchar(255) DEFAULT NULL COMMENT '权益描述',
                                `value` varchar(100) DEFAULT NULL COMMENT '权益值（如次数、额度、期限等）',
                                `sort_order` int DEFAULT 0 COMMENT '展示顺序',
                                `status` tinyint NOT NULL DEFAULT 1 COMMENT '是否启用: 0停用, 1启用',
                                `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除',
                                PRIMARY KEY (`id`),
                                KEY `idx_vip_type` (`vip_type`),
                                KEY `idx_benefit_code` (`benefit_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='VIP权益表';

ALTER TABLE `vip_benefits`
    ADD COLUMN `price` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '权益价格（针对单独购买场景）' AFTER `benefit_name`;

CREATE TABLE `comments` (
                            `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '评论ID',
                            `post_id` BIGINT NOT NULL COMMENT '帖子ID',
                            `user_id` BIGINT NOT NULL COMMENT '评论用户ID',
                            `parent_id` BIGINT NOT NULL DEFAULT 0 COMMENT '父评论ID，0 表示一级评论',
                            `root_id` BIGINT NOT NULL DEFAULT 0 COMMENT '根评论ID，指向一级评论，便于树结构检索',
                            `reply_to_user_id` BIGINT DEFAULT NULL COMMENT '被回复的用户ID，用于展示 @xxx',
                            `content` TEXT NOT NULL COMMENT '评论内容',
                            `like_count` INT NOT NULL DEFAULT 0 COMMENT '点赞数',
                            `is_author` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否作者回复:0否,1是',
                            `status` TINYINT NOT NULL DEFAULT 0 COMMENT '评论状态：0正常，1待审，2屏蔽/违规',
                            `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除:0否,1是',
                            `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                            `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                            PRIMARY KEY (`id`),
                            KEY `idx_post_id` (`post_id`),
                            KEY `idx_user_id` (`user_id`),
                            KEY `idx_parent_id` (`parent_id`),
                            KEY `idx_root_id` (`root_id`),
                            KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论表（支持多级嵌套与审核）';


CREATE TABLE `comment_likes` (
                                 `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
                                 `comment_id` BIGINT NOT NULL COMMENT '评论ID',
                                 `user_id` BIGINT NOT NULL COMMENT '点赞用户ID',
                                 `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
                                 `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                 `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除:0否,1是（取消点赞）',
                                 PRIMARY KEY (`id`),
                                 UNIQUE KEY `uk_comment_user` (`comment_id`, `user_id`),
                                 KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论点赞表';

ALTER TABLE `user`
    ADD COLUMN `daily_calories` decimal(10,2) DEFAULT NULL COMMENT '每日建议摄入热量(大卡)';
-- 追加每日热量摄入字段
-- 为每日热量字段添加索引（可选，根据查询需求决定）
ALTER TABLE `user`
    ADD INDEX `idx_daily_calories` (`daily_calories`) COMMENT '每日热量索引';


CREATE TABLE `product` (
                           `id` bigint NOT NULL AUTO_INCREMENT COMMENT '商品ID',
                           `name` varchar(100) NOT NULL COMMENT '商品名称',
                           `category_id` bigint NOT NULL COMMENT '分类ID',
                           `description` text COMMENT '商品描述',
                           `price` decimal(10,2) NOT NULL COMMENT '商品价格',
                           `original_price` decimal(10,2) COMMENT '原价',
                           `stock` int NOT NULL DEFAULT 0 COMMENT '库存数量',
                           `sales` int NOT NULL DEFAULT 0 COMMENT '销量',
                           `main_image` varchar(255) COMMENT '主图URL',
                           `sub_images` varchar(1000) COMMENT '子图URL，多个用逗号分隔',
                           `detail` text COMMENT '商品详情',
                           `specs` text COMMENT '商品规格，JSON格式',
                           `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0-下架，1-上架，2-缺货',
                           `weight` decimal(8,2) COMMENT '商品重量(g)',
                           `is_hot` tinyint DEFAULT 0 COMMENT '是否热销：0-否，1-是',
                           `is_recommend` tinyint DEFAULT 0 COMMENT '是否推荐：0-否，1-是',
                           `sort_order` int DEFAULT 0 COMMENT '排序权重',
                           `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                           `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                           `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
                           PRIMARY KEY (`id`),
                           KEY `idx_category` (`category_id`),
                           KEY `idx_status` (`status`),
                           KEY `idx_sort` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

CREATE TABLE `product_category` (
                                    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '分类ID',
                                    `parent_id` bigint DEFAULT 0 COMMENT '父分类ID，0表示一级分类',
                                    `name` varchar(50) NOT NULL COMMENT '分类名称',
                                    `level` tinyint  NULL COMMENT '分类层级：1-一级，2-二级，3-三级',
                                    `icon` varchar(255) COMMENT '分类图标',
                                    `description` varchar(255) COMMENT '分类描述',
                                    `sort_order` int DEFAULT 0 COMMENT '排序权重',
                                    `status` tinyint DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
                                    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                    `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
                                    PRIMARY KEY (`id`),
                                    KEY `idx_parent` (`parent_id`),
                                    KEY `idx_sort` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表';

CREATE TABLE `cart` (
                        `id` bigint NOT NULL AUTO_INCREMENT COMMENT '购物车ID',
                        `user_id` bigint NOT NULL COMMENT '用户ID',
                        `product_id` bigint NOT NULL COMMENT '商品ID',
                        `quantity` int NOT NULL DEFAULT 1 COMMENT '数量',
                        `selected` tinyint NOT NULL DEFAULT 1 COMMENT '是否选中：0-否，1-是',
                        `specs` json COMMENT '商品规格，JSON格式',
                        `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                        `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                        `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
                        PRIMARY KEY (`id`),
                        UNIQUE KEY `idx_user_product` (`user_id`, `product_id`),
                        KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车表';

CREATE TABLE `order` (
                         `id` bigint NOT NULL AUTO_INCREMENT COMMENT '订单ID',
                         `order_no` varchar(50) NOT NULL COMMENT '订单编号',
                         `user_id` bigint NOT NULL COMMENT '用户ID',
                         `total_amount` decimal(10,2) NOT NULL COMMENT '订单总金额',
                         `payment_amount` decimal(10,2) NOT NULL COMMENT '实付金额',
                         `freight_amount` decimal(10,2) DEFAULT 0 COMMENT '运费',
                         `discount_amount` decimal(10,2) DEFAULT 0 COMMENT '优惠金额',
                         `coupon_amount` decimal(10,2) DEFAULT 0 COMMENT '优惠券抵扣金额',
                         `payment_type` tinyint COMMENT '支付方式：1-支付宝，2-微信，3-银联',
                         `payment_time` datetime COMMENT '支付时间',
                         `payment_serial_number` varchar(100) COMMENT '支付流水号',
                         `status` tinyint NOT NULL DEFAULT 0 COMMENT '订单状态：0-待支付，1-已支付待发货，2-已发货，3-已完成，4-已取消，5-已退款，6-已关闭',
                         `delivery_company` varchar(50) COMMENT '物流公司',
                         `delivery_no` varchar(50) COMMENT '物流单号',
                         `delivery_time` datetime COMMENT '发货时间',
                         `receive_time` datetime COMMENT '收货时间',
                         `note` varchar(500) COMMENT '订单备注',
                         `source` tinyint COMMENT '订单来源：1-PC，2-APP，3-小程序，4-H5',
                         `confirm_status` tinyint DEFAULT 0 COMMENT '确认收货状态：0-未确认，1-已确认',
                         `delete_status` tinyint DEFAULT 0 COMMENT '删除状态：0-未删除，1-已删除',
                         `coupon_id` bigint COMMENT '使用的优惠券ID',
                         `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                         `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                         `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
                         PRIMARY KEY (`id`),
                         UNIQUE KEY `idx_order_no` (`order_no`),
                         KEY `idx_user` (`user_id`),
                         KEY `idx_status` (`status`),
                         KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

CREATE TABLE `order_item` (
                              `id` bigint NOT NULL AUTO_INCREMENT COMMENT '订单商品ID',
                              `order_id` bigint NOT NULL COMMENT '订单ID',
                              `order_no` varchar(50) NOT NULL COMMENT '订单编号',
                              `product_id` bigint NOT NULL COMMENT '商品ID',
                              `product_name` varchar(100) NOT NULL COMMENT '商品名称',
                              `product_image` varchar(255) COMMENT '商品图片',
                              `current_price` decimal(10,2) NOT NULL COMMENT '下单时的商品单价',
                              `quantity` int NOT NULL COMMENT '购买数量',
                              `total_price` decimal(10,2) NOT NULL COMMENT '商品总价',
                              `specs` json COMMENT '商品规格，JSON格式',
                              `refund_status` tinyint DEFAULT 0 COMMENT '退款状态：0-未退款，1-退款中，2-已退款',
                              `refund_amount` decimal(10,2) DEFAULT 0 COMMENT '退款金额',
                              `refund_time` datetime COMMENT '退款时间',
                              `refund_reason` varchar(255) COMMENT '退款原因',
                              `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                              `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                              `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
                              PRIMARY KEY (`id`),
                              KEY `idx_order` (`order_id`),
                              KEY `idx_product` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单商品明细表';

CREATE TABLE `shipping_address` (
                                    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '地址ID',
                                    `user_id` bigint NOT NULL COMMENT '用户ID',
                                    `receiver_name` varchar(50) NOT NULL COMMENT '收货人姓名',
                                    `receiver_phone` varchar(20) NOT NULL COMMENT '收货人电话',
                                    `province` varchar(50) NOT NULL COMMENT '省',
                                    `city` varchar(50) NOT NULL COMMENT '市',
                                    `district` varchar(50) NOT NULL COMMENT '区',
                                    `detail_address` varchar(255) NOT NULL COMMENT '详细地址',
                                    `postal_code` varchar(20) COMMENT '邮政编码',
                                    `is_default` tinyint DEFAULT 0 COMMENT '是否默认地址：0-否，1-是',
                                    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                    `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
                                    PRIMARY KEY (`id`),
                                    KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收货地址表';

CREATE TABLE `order_operation_log` (
                                       `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
                                       `order_id` bigint NOT NULL COMMENT '订单ID',
                                       `order_no` varchar(50) NOT NULL COMMENT '订单编号',
                                       `operator` varchar(50) COMMENT '操作人，用户ID或管理员ID',
                                       `operation_type` tinyint NOT NULL COMMENT '操作类型：1-创建订单，2-支付订单，3-发货，4-确认收货，5-取消订单，6-申请退款，7-退款成功，8-订单完成',
                                       `operation_note` varchar(255) COMMENT '操作备注',
                                       `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                       `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
                                       PRIMARY KEY (`id`),
                                       KEY `idx_order` (`order_id`),
                                       KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单操作日志表';

CREATE TABLE `refund_application` (
                                      `id` bigint NOT NULL AUTO_INCREMENT COMMENT '退款ID',
                                      `order_id` bigint NOT NULL COMMENT '订单ID',
                                      `order_no` varchar(50) NOT NULL COMMENT '订单编号',
                                      `user_id` bigint NOT NULL COMMENT '用户ID',
                                      `refund_amount` decimal(10,2) NOT NULL COMMENT '退款金额',
                                      `refund_type` tinyint NOT NULL COMMENT '退款类型：1-仅退款，2-退货退款',
                                      `refund_reason` varchar(255) NOT NULL COMMENT '退款原因',
                                      `refund_remark` varchar(500) COMMENT '退款说明',
                                      `status` tinyint NOT NULL DEFAULT 0 COMMENT '退款状态：0-待处理，1-处理中，2-退款成功，3-退款失败，4-已取消',
                                      `handle_time` datetime COMMENT '处理时间',
                                      `handle_note` varchar(500) COMMENT '处理备注',
                                      `handler_id` bigint COMMENT '处理人ID',
                                      `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                      `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                      `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
                                      PRIMARY KEY (`id`),
                                      KEY `idx_order` (`order_id`),
                                      KEY `idx_user` (`user_id`),
                                      KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退款申请表';

ALTER TABLE `order`
    ADD COLUMN `address_id` bigint COMMENT '收货地址ID（关联shipping_address表的id）'
        AFTER `coupon_id`;