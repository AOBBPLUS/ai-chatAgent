package com.myproj.code.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.myproj.code.common.Result;
import com.myproj.code.entity.Role;

public interface RoleService extends IService<Role> {
    Result<?> add(Role role);

    Result<?> delete(Integer id);

    Result<?> update(Role role);

    Result<?> detailsByCtId(Integer ctId);

    Role getRoleById( Integer ctId);
}