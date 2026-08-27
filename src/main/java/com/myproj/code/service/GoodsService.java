package com.myproj.code.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.myproj.code.common.Result;
import com.myproj.code.entity.Goods;

public interface GoodsService extends IService<Goods> {
    int add(Goods goods);


    Boolean update(Goods goods);

    Goods detailById(Integer id);

    Result<?> delete(Integer id);
}