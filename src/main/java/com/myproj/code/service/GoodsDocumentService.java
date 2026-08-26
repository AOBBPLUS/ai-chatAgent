package com.myproj.code.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.myproj.code.common.Result;
import com.myproj.code.entity.GoodsDocument;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface GoodsDocumentService extends IService<GoodsDocument> {
    List<GoodsDocument> getListByGoodsId(Integer id);

    Result<?> upload(MultipartFile file, Long goodsId) throws IOException;
}