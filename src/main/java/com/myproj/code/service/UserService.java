package com.myproj.code.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.myproj.code.common.Result;
import com.myproj.code.entity.User;

public interface UserService extends IService<User> {
    Result<?> login(User user);
}