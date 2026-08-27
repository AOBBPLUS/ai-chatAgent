package com.myproj.code.service.impl;

import com.myproj.code.code.ResultCode;
import com.myproj.code.common.Result;
import com.myproj.code.service.GoodsDocumentService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.myproj.code.entity.GoodsDocument;
import com.myproj.code.mapper.GoodsDocumentMapper;
import com.myproj.code.utils.FileToDocuments;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

import static com.myproj.code.code.DocumentCode.FILE_ID;
import static com.myproj.code.code.DocumentCode.GOODS_ID;

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
        List<Document> documentList = fileToDocuments.handle(file);
        System.out.println(documentList);
        // 存储到数据库中，标识商品有哪些文本
        GoodsDocument fileData = GoodsDocument.builder().name(file.getOriginalFilename()).goodsId(goodsId).build();
        if (!save(fileData)) {
            return Result.error(ResultCode.ADD_ERROR);
        }
        documentList.forEach(d -> {
            // 设置元数据 文件对应id，文件对应商品的id
            d.getMetadata().put(FILE_ID, fileData.getId().toString());
            d.getMetadata().put(GOODS_ID, goodsId.toString());
        });
        // 注意 vectorStore 每次只能处理10条数据
        for (int i = 0; i < documentList.size(); i += 10) {
            int endIndex = Math.min(i + 10, documentList.size());
            List<Document> documents = documentList.subList(i, endIndex);
            vectorStore.add(documents);

        }
        return Result.success(ResultCode.ADD_SUCCESS, documentList);
    }

    @Override
    public Result<?> delete(Long id) {
        GoodsDocument goodsDocument = getById(id);
        if (goodsDocument == null) {
            return Result.error(ResultCode.DELETE_GOODS_ERROR);
        }
        if (!removeById(id)) {
            return Result.error(ResultCode.DELETE_FILE_ERROR);
        }
        vectorStore.delete(new Filter.Expression(Filter.ExpressionType.EQ,
                new Filter.Key(FILE_ID),
                new Filter.Value(id.toString())));
        return Result.success(ResultCode.DELETE_SUCCESS);
    }
}
