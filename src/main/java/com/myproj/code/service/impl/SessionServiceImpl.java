package com.myproj.code.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.myproj.code.code.ResultCode;
import com.myproj.code.common.Result;
import com.myproj.code.entity.Session;
import com.myproj.code.mapper.SessionMapper;
import com.myproj.code.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SessionServiceImpl extends ServiceImpl<SessionMapper, Session> implements SessionService {
    @Value("${session.key}")
    private String key;
    @Value("${session.expiration-duration}")
    private Integer timeout;


    @Override
    public Result<List<Session>> userGetLastSessionList(Integer userId) {
        return Result.success(
                ResultCode.GET_SUCCESS,
                lambdaQuery()
                        .eq(Session::getUserId, userId)
                        .orderByDesc(Session::getTimestamp)
                        .list()
        );
    }

    @Override
    public Result<List<Session>> ctGetLastSessionList(Integer ctId) {
        return Result.success(
                ResultCode.GET_SUCCESS,
                lambdaQuery()
                        .eq(Session::getCtId, ctId)
                        .orderByDesc(Session::getTimestamp)
                        .list()
        );
    }
}