package com.myproj.code.controller;

import com.myproj.code.common.Result;
import com.myproj.code.entity.User;
import com.myproj.code.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    @PostMapping("/login")
    public Result<?> login(@RequestBody User user){
        return userService.login(user);
    }
}
