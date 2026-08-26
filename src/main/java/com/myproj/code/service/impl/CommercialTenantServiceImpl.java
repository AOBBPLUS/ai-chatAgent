package com.myproj.code.service.impl;

import com.myproj.code.service.CommercialTenantService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.myproj.code.code.ResultCode;
import com.myproj.code.common.Result;
import com.myproj.code.entity.CommercialTenant;
import com.myproj.code.mapper.CommercialTenantMapper;
import org.springframework.stereotype.Service;

@Service
public class CommercialTenantServiceImpl extends ServiceImpl<CommercialTenantMapper, CommercialTenant> implements CommercialTenantService {
    @Override
    public Result<?> login(CommercialTenant commercialTenant) {
        CommercialTenant account = lambdaQuery().eq(CommercialTenant::getAccount, commercialTenant.getAccount()).one();
        if (account == null) {
            return Result.error(ResultCode.USER_NOT_EXIST);
        }
        if (!account.getPassword().equals(commercialTenant.getPassword())) {
            return Result.error(ResultCode.PASSWORD_ERROR);
        }
        account.clearPassword();
        return Result.success(ResultCode.LOGIN_SUCCESS, account);
    }
}