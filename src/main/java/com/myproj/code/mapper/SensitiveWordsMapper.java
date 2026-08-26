package com.myproj.code.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.myproj.code.entity.SensitiveWords;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SensitiveWordsMapper extends BaseMapper<SensitiveWords> {
}