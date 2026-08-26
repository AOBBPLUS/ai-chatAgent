package com.myproj.code.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.myproj.code.common.Result;
import com.myproj.code.entity.Session;

import java.util.List;

public interface SessionService extends IService<Session> {

    Result<List<Session>> userGetLastSessionList(Integer userId);

    Result<List<Session>> ctGetLastSessionList(Integer ctId);
}