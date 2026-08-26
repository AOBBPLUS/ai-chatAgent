package com.myproj.code.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.myproj.code.entity.Session;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SessionMapper extends BaseMapper<Session> {
}