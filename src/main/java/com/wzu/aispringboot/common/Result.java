package com.wzu.aispringboot.common;


import lombok.Data;

@Data // 生成 getter 和 setter 方法
public class Result<T> { // T 为泛型，表示可以返回任意类型的数据
    private String code;
    private String msg;
    private T data;

    public static <T> Result<T> ok(){
        Result<T> result = new Result<>();
        result.setCode(ResultCode.SUCCESS.getCode());
        result.setMsg(ResultCode.SUCCESS.getMsg());
        return result;
    }
    //方法重载(成功)
    public static <T> Result<T> ok(T data){
        Result<T> result = ok(); //调用上面定义的ok()方法
        result.setData(data);
        return result;
    }


    public static <T> Result<T> error(){
        Result<T> result = new Result<>();
        result.setCode(ResultCode.ERROR.getCode());
        result.setMsg(ResultCode.ERROR.getMsg());
        return result;
    }
    public static <T> Result<T> error(String code,String msg, T data){
        Result<T> result = error();
        result.setCode(code);
        result.setMsg(msg);
        result.setData(data);
        return result;
    }
}

