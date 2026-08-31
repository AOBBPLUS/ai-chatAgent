package com.myproj.code.service.impl;

import cn.hutool.json.JSON;
import cn.hutool.json.JSONUtil;
import com.myproj.code.service.RoleService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.myproj.code.code.ResultCode;
import com.myproj.code.common.Result;
import com.myproj.code.entity.Role;
import com.myproj.code.mapper.RoleMapper;
import com.myproj.code.utils.KeyUtils;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {
    @Resource
    private final StringRedisTemplate stringRedisTemplate;
    @Value("${role.key}")
    private String key;
    @Value("${role.expiration-duration}")
    private Integer timeout;


    public RoleServiceImpl(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public Result<?> add(Role role) {
        // 验证该商家是否已经有对应的AI客服了
        Role have = lambdaQuery().eq(Role::getCtId, role.getCtId()).one();
        if (have != null) {
            return Result.error(ResultCode.FAIL).setMessage("该商户已有AI客服");
        }
        // 保存设置的AI信息
        if (!save(role)) {
            return Result.error(ResultCode.ADD_ERROR);
        }
        return Result.success(ResultCode.ADD_SUCCESS);
    }

    @Override
    public Result<?> delete(Integer id) {
        if (!removeById(id)) {
            return Result.error(ResultCode.DELETE_ERROR);
        }
        stringRedisTemplate.delete(KeyUtils.redisKeyUtils(key, id));
        return Result.success(ResultCode.DELETE_SUCCESS);
    }

    @Override
    public Result<?> update(Role role) {
        if (!updateById(role)) {
            return Result.error(ResultCode.UPDATE_ERROR);
        }
        stringRedisTemplate.delete(KeyUtils.redisKeyUtils(key, role.getCtId()));
        return Result.success(ResultCode.UPDATE_SUCCESS);
    }

    @Override
    public Result<?> detailsByCtId(Integer ctId) {
        Role role = lambdaQuery().eq(Role::getCtId, ctId).one();
        if (role == null) {
            return Result.error(ResultCode.NOT_FOUND).setMessage("该商户没有AI客服");
        }
        return Result.success(ResultCode.SUCCESS, role);
    }

    @Override
    public Role getRoleById(Integer ctId) {
        // 查询Redis缓存
        String json = stringRedisTemplate.opsForValue().getAndExpire(KeyUtils.redisKeyUtils(key, ctId), timeout, TimeUnit.MINUTES);
        if (json != null) {
            Role role = JSONUtil.toBean(json, Role.class);
        }
        // 为空->查询数据库
        Role role = lambdaQuery().eq(Role::getCtId, ctId).one();
        if (role == null) {
            return null; //没有角色
        }
        // redis存储
        stringRedisTemplate.opsForValue().set(KeyUtils.redisKeyUtils(key, ctId), JSONUtil.toJsonStr(role), timeout, TimeUnit.MINUTES);
        return role;
    }
}