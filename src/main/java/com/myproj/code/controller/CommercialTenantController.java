package com.myproj.code.controller;

import com.myproj.code.common.Result;
import com.myproj.code.entity.CommercialTenant;
import com.myproj.code.service.CommercialTenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/commercialTenant")
@RequiredArgsConstructor
public class CommercialTenantController {
    private final CommercialTenantService commercialTenantService;
    @PostMapping("/login")
    public Result<?> login(@RequestBody CommercialTenant commercialTenant){
        return commercialTenantService.login(commercialTenant);
    }
}
