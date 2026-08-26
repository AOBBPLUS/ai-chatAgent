package com.myproj.code.service.impl;

import com.myproj.code.service.SensitiveWordsService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.myproj.code.entity.SensitiveWords;
import com.myproj.code.mapper.SensitiveWordsMapper;
import org.springframework.stereotype.Service;

@Service
public class SensitiveWordsServiceImpl extends ServiceImpl<SensitiveWordsMapper, SensitiveWords> implements SensitiveWordsService {
}