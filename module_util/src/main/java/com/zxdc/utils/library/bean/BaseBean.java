package com.zxdc.utils.library.bean;

import java.io.Serializable;

/**
 * 网络请求状态实体类
 */

public class BaseBean implements Serializable{
    private int code;
    private String desc;

    public BaseBean(){}

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public boolean isSussess() {
        if (code == 200) {
            return true;
        }
        return false;
    }

}
