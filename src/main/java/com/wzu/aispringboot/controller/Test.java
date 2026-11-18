package com.wzu.aispringboot.controller;



import com.wzu.aispringboot.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api")
public class Test {
    @GetMapping("/test")
    public Result<String> test(){
//        return "test";
        return Result.ok();
    }
}
