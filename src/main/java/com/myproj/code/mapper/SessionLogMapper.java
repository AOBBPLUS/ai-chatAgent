package com.myproj.code.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.myproj.code.entity.SessionLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SessionLogMapper extends BaseMapper<SessionLog> {
}