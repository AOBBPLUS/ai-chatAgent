package com.myproj.code.service.impl;

import com.myproj.code.code.ResultCode;
import com.myproj.code.common.Result;
import com.myproj.code.service.GoodsDocumentService;
import com.myproj.code.service.GoodsService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.myproj.code.entity.Goods;
import com.myproj.code.entity.GoodsDocument;
import com.myproj.code.mapper.GoodsDocumentMapper;
import com.myproj.code.mapper.GoodsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.myproj.code.code.DocumentCode.GOODS_ID;

@Service
@RequiredArgsConstructor
public class GoodsServiceImpl extends ServiceImpl<GoodsMapper, Goods> implements GoodsService {
    private final GoodsDocumentService goodsDocumentService;
    private final GoodsDocumentMapper goodsDocumentMapper;
    private final MilvusVectorStore vectorStore;
    private final GoodsMapper goodsMapper;

    @Override
    @Transactional
    public int add(Goods goods) {
        save(goods);
        return goods.getId();
    }


    /**
     * 这个修改的方法只对goods表中的树做修改
     *
     * @param goods 修改的树
     * @return 修改结果
     */
    @Override
    public Boolean update(Goods goods) {
        return updateById(goods);
    }

    /**
     * 根据id查询商品
     *
     * @param id 商品id
     * @return 商品信息和对应的文档数据
     */
    @Override
    public Goods detailById(Integer id) {
        Goods goods = getById(id);
        // 查询对应的文档
        List<GoodsDocument> documents = goodsDocumentMapper.selectList(new LambdaQueryWrapper<>(GoodsDocument.class)
                .eq(GoodsDocument::getGoodsId, id));
        goods.setDocuments(documents);
        return goods;
    }

    @Override
    public Result<?> delete(Integer id) {
        // 删除商品同时删除向量数据
        if (detailById(id) == null) {
            return Result.error(ResultCode.DELETE_GOODS_ERROR);
        }
        // 删除mysql/goodsDocument的数据
        LambdaQueryWrapper<GoodsDocument> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GoodsDocument::getGoodsId,id);
        goodsDocumentMapper.delete(wrapper);
        if (!removeById(id)) {
            return Result.error(ResultCode.DELETE_GOODS_ERROR);
        }
        // goodsMapper.deleteById(id);
        vectorStore.delete(new Filter.Expression(Filter.ExpressionType.EQ, new Filter.Key(GOODS_ID), new Filter.Value(id.toString())));
        return Result.success(ResultCode.DELETE_SUCCESS);
    }
}