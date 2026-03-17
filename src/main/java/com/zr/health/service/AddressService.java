package com.zr.health.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zr.health.model.dto.address.AddressCreateDTO;
import com.zr.health.model.dto.address.AddressUpdateDTO;
import com.zr.health.model.entity.Address;
import com.zr.health.model.vo.address.AddressVO;

import java.util.List;

/**
* @author 27105
* @description 针对表【shipping_address(收货地址表)】的数据库操作Service
* @createDate 2025-07-14 23:00:21
*/
public interface AddressService extends IService<Address> {

    /**
     * 添加收货地址
     *
     * @param addressCreateDTO 收货地址信息
     * @return 新增的收货地址信息
     */
    AddressVO addAddress(AddressCreateDTO addressCreateDTO);

    /**
     * 根据ID删除收货地址
     *
     * @param id 地址ID
     * @return 删除结果
     */
    void deleteAddressById(Long id);

    /**
     * 修改收货地址
     *
     * @param addressUpdateDTO 更新的地址信息
     * @return 更新后的地址信息
     */
    AddressVO updateAddress(AddressUpdateDTO addressUpdateDTO);

    /**
     * 根据用户 ID 查询未删除的收货地址列表，默认地址默认默认地址排在第一位
     *
     * @param userId 用户 ID
     * @return 地址列表
     */
    List<AddressVO> getAddressListByUserId(Long userId);

    /**
     * 根据地址 ID 查询地址的详细信息
     *
     * @param id 地址ID
     * @return 地址详细信息
     */
    AddressVO getAddressById(Long id);
}
