package com.myproj.code.service.impl;

import com.myproj.code.common.Result;
import com.myproj.code.service.GoodsDocumentService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.myproj.code.entity.GoodsDocument;
import com.myproj.code.mapper.GoodsDocumentMapper;
import com.myproj.code.utils.FileToDocuments;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GoodsDocumentServiceImpl extends ServiceImpl<GoodsDocumentMapper, GoodsDocument> implements GoodsDocumentService {

    private final MilvusVectorStore vectorStore;

    private final FileToDocuments fileToDocuments;

    @Override
    public List<GoodsDocument> getListByGoodsId(Integer id) {
        return lambdaQuery().eq(GoodsDocument::getGoodsId, id).list();
    }

    @Override
    public Result<?> upload(MultipartFile file, Long goodsId) throws IOException {
        // 跟据文件类型选择文件读取器
        List<Document> handle = fileToDocuments.handle(file);
        System.out.println(handle);
        return null;
    }
}
