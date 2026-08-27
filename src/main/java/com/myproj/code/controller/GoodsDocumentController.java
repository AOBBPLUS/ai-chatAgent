package com.myproj.code.controller;

import com.myproj.code.code.ResultCode;
import com.myproj.code.common.Result;
import com.myproj.code.service.GoodsDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/goodsDocument")
@RequiredArgsConstructor
public class GoodsDocumentController {

    private final GoodsDocumentService goodsDocumentService;

    /**
     * TODO:001文档上传
     *
     * @param file    文档
     * @param goodsId 商品id
     * @return 上传结果
     */
    @PostMapping("/upload")
    public Result<?> upload(@RequestBody MultipartFile file, Long goodsId) throws IOException {
        return Result.success(ResultCode.ADD_SUCCESS, goodsDocumentService.upload(file, goodsId));
    }

    /**
     * TODO:002 删除文档
     *
     * @param id 文档id
     * @return 删除结果
     */
    @DeleteMapping("/delete")
    public Result<?> delete(@RequestParam Long id) {
        return Result.success(ResultCode.DELETE_SUCCESS, goodsDocumentService.delete(id));
    }
}
