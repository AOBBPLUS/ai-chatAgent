package com.myproj.code.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.myproj.code.common.Result;
import com.myproj.code.entity.CommercialTenant;

public interface CommercialTenantService extends IService<CommercialTenant> {
    Result<?> login(CommercialTenant commercialTenant);
}