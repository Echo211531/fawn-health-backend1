package com.ljh.fawnhealth.controller;

import com.ljh.fawnhealth.commen.BaseResponse;
import com.ljh.fawnhealth.config.ResultUtils;
import com.ljh.fawnhealth.model.dto.address.AddressCreateDTO;
import com.ljh.fawnhealth.model.dto.address.AddressUpdateDTO;
import com.ljh.fawnhealth.model.dto.product.ProductCreateDTO;
import com.ljh.fawnhealth.model.dto.product.ProductUpdateDTO;
import com.ljh.fawnhealth.model.vo.address.AddressVO;
import com.ljh.fawnhealth.model.vo.product.ProductVO;
import com.ljh.fawnhealth.service.AddressService;
import com.ljh.fawnhealth.service.ProductService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 收货模块
 * 提供收货地址的创建、修改、删除等功能接口
 */
@Slf4j
@RestController
@RequestMapping("/address")
public class AddressController {

    @Resource
    private AddressService addressService;

    /**
     * 添加收货地址
     *
     * @param addressCreateDTO 收货地址信息
     * @return 新增的收货地址信息
     */
    @PostMapping("/addAddress")
    public BaseResponse<AddressVO> addAddress(@RequestBody AddressCreateDTO addressCreateDTO) {
        log.info("添加收货地址: {}", addressCreateDTO);
        AddressVO addressVO = addressService.addAddress(addressCreateDTO);
        return ResultUtils.success(addressVO);
    }


    /**
     * 根据ID删除收货地址
     *
     * @param id 地址ID
     * @return 删除结果
     */
    @GetMapping("/delete/{id}")
    public BaseResponse<String> deleteAddress(@PathVariable Long id) {
        log.info("删除收货地址, ID: {}", id);
        addressService.deleteAddressById(id);
        return ResultUtils.success("删除成功");
    }

    /**
     * 修改收货地址
     *
     * @param addressUpdateDTO 更新的地址信息
     * @return 更新后的地址信息
     */
    @PostMapping("/updateAddress")
    public BaseResponse<AddressVO> updateAddress(@Validated @RequestBody AddressUpdateDTO addressUpdateDTO) {
        log.info("修改收货地址: {}", addressUpdateDTO);
        AddressVO addressVO = addressService.updateAddress(addressUpdateDTO);
        return ResultUtils.success(addressVO);
    }

    /**
     * 根据用户 ID 查询未删除的收货地址列表，默认地址默认默认地址排在第一位
     *
     * @param userId 用户 ID
     * @return 地址列表
     */
    @GetMapping ("/list/{userId}")
    public BaseResponse<List<AddressVO>> getUserAddressList(@PathVariable Long userId) {
        log.info("查询用户收货地址列表，用户 ID: {}", userId);
        List<AddressVO> addressVOList = addressService.getAddressListByUserId(userId);
        return ResultUtils.success(addressVOList);
    }

    /**
     * 根据地址 ID 查询地址的详细信息
     *
     * @param id 地址ID
     * @return 地址详细信息
     */
    @GetMapping("/getAddressById/{id}")
    public BaseResponse<AddressVO> getAddressById(@PathVariable Long id) {
        log.info("查询地址详细信息，地址 ID: {}", id);
        AddressVO addressVO = addressService.getAddressById(id);
        return ResultUtils.success(addressVO);
    }
}
