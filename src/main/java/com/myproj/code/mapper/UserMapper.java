package com.myproj.code.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.myproj.code.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}