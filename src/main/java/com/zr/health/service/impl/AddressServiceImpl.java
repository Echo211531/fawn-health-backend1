package com.zr.health.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.zr.health.exception.BusinessException;
import com.zr.health.exception.ErrorCode;
import com.zr.health.mapper.AddressMapper;
import com.zr.health.mapper.UserMapper;
import com.zr.health.model.dto.address.AddressCreateDTO;
import com.zr.health.model.dto.address.AddressUpdateDTO;
import com.zr.health.model.entity.Address;
import com.zr.health.model.entity.User;
import com.zr.health.model.vo.address.AddressVO;
import com.zr.health.service.AddressService;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
* @author 27105
* @description 针对表【shipping_address(收货地址表)】的数据库操作Service实现
* @createDate 2025-07-14 23:00:21
*/
@Service
public class AddressServiceImpl extends ServiceImpl<AddressMapper, Address>
    implements AddressService {

    @Resource
    private AddressMapper addressMapper;

    @Resource
    private UserMapper userMapper;

    /**
     * 添加收货地址
     *
     * @param addressCreateDTO 收货地址信息
     * @return 新增的收货地址信息
     */
    @Override
    public AddressVO addAddress(AddressCreateDTO addressCreateDTO) {
        // 1. 获取用户ID
        Long userId = addressCreateDTO.getUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID为空");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOTFOUND);
        }

        // 2. 如果设置为默认地址，需要将该用户其他地址设为非默认
        if (addressCreateDTO.getIsDefault() != null && addressCreateDTO.getIsDefault() == 1) {
            Address updateAddress = new Address();
            updateAddress.setIsDefault(0);
            baseMapper.update(updateAddress, new QueryWrapper<Address>()
                    .eq("user_id", userId)
                    .eq("is_default", 1)
                    .eq("is_delete", 0));
        }

        // 3. 转换DTO为实体对象
        Address address = new Address();
        BeanUtils.copyProperties(addressCreateDTO, address);
        address.setUserId(userId);
        address.setIsDelete(0);
        address.setCreateTime(new Date());
        address.setUpdateTime(new Date());

        // 4. 保存地址信息
        int saveResult = addressMapper.insert(address);
        if (saveResult < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "地址保存失败");
        }
        // 5. 转换为VO并返回
        AddressVO addressVO = new AddressVO();
        BeanUtils.copyProperties(address, addressVO);
        return addressVO;
    }

    /**
     * 根据ID删除收货地址
     *
     * @param id 地址ID
     * @return 删除结果
     */
    @Override
    public void deleteAddressById(Long id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "收货地址ID为空");
        }

        // 1. 校验地址是否存在
        Address address = addressMapper.selectById(id);
        if (address == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "收货地址不存在");
        }

        // 2. 校验是否已被逻辑删除
        if (address.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "收货地址已删除");
        }

        // 4. 执行逻辑删除（更新is_delete字段为1）
        Address updateAddress = new Address();
        updateAddress.setId(id);
        updateAddress.setIsDelete(1);
        updateAddress.setUpdateTime(new Date()); // 更新时间戳

        int i = addressMapper.updateById(updateAddress);
        if (i <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "收货地址删除失败");
        }
    }

    /**
     * 修改收货地址
     *
     * @param addressUpdateDTO 更新的地址信息
     * @return 更新后的地址信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AddressVO updateAddress(AddressUpdateDTO addressUpdateDTO) {
        // 1. 获取当前登录用户ID（权限校验）
        Long userId = addressUpdateDTO.getUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户未登录，无法修改地址");
        }

        // 2. 校验地址是否存在且属于当前用户
        Long addressId = addressUpdateDTO.getId();
        Address address = addressMapper.selectById(addressId);
        if (address == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "地址不存在");
        }
        if (address.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "地址已被删除");
        }
        if (!address.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "没有权限修改该地址");
        }

        // 3. 处理默认地址逻辑
        Integer isDefault = addressUpdateDTO.getIsDefault();
        if (isDefault != null) {
            // 若设置为默认地址，先将用户其他默认地址置为非默认
            if (isDefault == 1) {
                Address updateEntity = new Address();
                updateEntity.setIsDefault(0);
                addressMapper.update(
                        updateEntity,
                        new QueryWrapper<Address>()
                                .eq("user_id", userId)
                                .eq("is_default", 1)
                                .ne("id", addressId)
                                .eq("is_delete", 0)
                );
            }
            address.setIsDefault(isDefault);
        }

        // 4. 动态更新字段（只更新有传值的字段）
        if (StringUtils.hasText(addressUpdateDTO.getReceiverName())) {
            address.setReceiverName(addressUpdateDTO.getReceiverName());
        }
        if (StringUtils.hasText(addressUpdateDTO.getReceiverPhone())) {
            address.setReceiverPhone(addressUpdateDTO.getReceiverPhone());
        }
        if (StringUtils.hasText(addressUpdateDTO.getProvince())) {
            address.setProvince(addressUpdateDTO.getProvince());
        }
        if (StringUtils.hasText(addressUpdateDTO.getCity())) {
            address.setCity(addressUpdateDTO.getCity());
        }
        if (StringUtils.hasText(addressUpdateDTO.getDistrict())) {
            address.setDistrict(addressUpdateDTO.getDistrict());
        }
        if (StringUtils.hasText(addressUpdateDTO.getDetailAddress())) {
            address.setDetailAddress(addressUpdateDTO.getDetailAddress());
        }
        // 邮政编码允许为空字符串（清空操作）
        if (addressUpdateDTO.getPostalCode() != null) {
            address.setPostalCode(addressUpdateDTO.getPostalCode());
        }

        // 5. 更新时间戳
        address.setUpdateTime(new Date());

        // 6. 执行更新操作
        int updateRows = addressMapper.updateById(address);
        if (updateRows <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "地址更新失败，请检查数据是否有变化");
        }

        // 7. 转换为VO返回
        AddressVO addressVO = new AddressVO();
        BeanUtils.copyProperties(address, addressVO);
        return addressVO;
    }

    /**
     * 根据用户 ID 查询未删除的收货地址列表，默认地址默认默认地址排在第一位
     *
     * @param userId 用户 ID
     * @return 地址列表
     */
    @Override
    public List<AddressVO> getAddressListByUserId(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不能为空");
        }

        // 查询用户未删除的地址，按默认地址（1在前）和创建时间排序
        List<Address> addressList = baseMapper.selectList(new QueryWrapper<Address>()
                .eq("user_id", userId)
                .eq("is_delete", 0)
                .orderByDesc("is_default")  // 默认地址排在前面
                .orderByDesc("create_time"));  // 相同状态按创建时间倒序

        // 转换为VO列表返回
        return addressList.stream()
                .map(address -> {
                    AddressVO vo = new AddressVO();
                    BeanUtils.copyProperties(address, vo);
                    return vo;
                })
                .collect(Collectors.toList());
    }

    /**
     * 根据地址 ID 查询地址的详细信息
     *
     * @param id 地址ID
     * @return 地址详细信息
     */
    @Override
    public AddressVO getAddressById(Long id) {
        // 1. 校验地址ID不为空
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "地址ID不能为空");
        }

        // 2. 查询未删除的地址信息（修复语法错误）
        Address address = addressMapper.selectOne(new QueryWrapper<Address>()
                .eq("id", id)
                .eq("is_delete", 0));

        // 3. 校验地址是否存在
        if (address == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "地址不存在或已被删除");
        }

        // 4. 转换为VO并返回（补充完整地址信息）
        AddressVO addressVO = new AddressVO();
        BeanUtils.copyProperties(address, addressVO);
        // 新增：拼接完整地址（省+市+区+详细地址）
        String fullAddress = String.join("",
                address.getProvince(),
                address.getCity(),
                address.getDistrict(),
                address.getDetailAddress()
        );
        addressVO.setFullAddress(fullAddress);

        return addressVO;
    }

}




