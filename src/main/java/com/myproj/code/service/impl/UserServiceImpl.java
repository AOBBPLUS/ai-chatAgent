package com.myproj.code.service.impl;

import com.myproj.code.service.UserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.myproj.code.code.ResultCode;
import com.myproj.code.common.Result;
import com.myproj.code.entity.User;
import com.myproj.code.mapper.UserMapper;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    @Override
    public Result<?> login(User user) {
        User account = lambdaQuery().eq(User::getAccount, user.getAccount()).one();
        if (account == null) {
            return Result.error(ResultCode.USER_NOT_EXIST);
        }
        if (!account.getPassword().equals(user.getPassword())) {
            return Result.error(ResultCode.USER_NOT_EXIST);
        }
        account.clearPassword();
        return Result.success(ResultCode.LOGIN_SUCCESS, account);
    }

}