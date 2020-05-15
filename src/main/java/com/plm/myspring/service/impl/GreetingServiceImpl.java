package com.plm.myspring.service.impl;

import com.plm.myspring.annotation.MyComponent;
import com.plm.myspring.service.IGreetingService;

/**
 * @author 潘磊明
 * @date 2020/5/15
 */
@MyComponent
public class GreetingServiceImpl implements IGreetingService {
    @Override
    public String greeting(String name) {
        return String.format("Hello, %s", name);
    }
}
