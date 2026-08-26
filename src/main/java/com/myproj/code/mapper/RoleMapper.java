package com.myproj.code.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.myproj.code.entity.Role;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RoleMapper extends BaseMapper<Role> {
}