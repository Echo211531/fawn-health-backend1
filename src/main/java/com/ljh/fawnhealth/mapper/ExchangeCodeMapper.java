package com.ljh.fawnhealth.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ljh.fawnhealth.model.entity.ExchangeCode;
import org.springframework.data.repository.query.Param;


/**
* @author 27105
* @description 针对表【exchange_code(兑换码表)】的数据库操作Mapper
* @createDate 2025-05-02 23:03:33
* @Entity com.ljh.domain.ExchangeCode
*/
public interface ExchangeCodeMapper extends BaseMapper<ExchangeCode> {

}




