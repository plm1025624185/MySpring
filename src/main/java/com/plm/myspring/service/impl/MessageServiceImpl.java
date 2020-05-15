package com.plm.myspring.service.impl;

import com.plm.myspring.annotation.MyComponent;
import com.plm.myspring.service.IMessageService;

/**
 * @author 潘磊明
 * @date 2020/5/15
 */
@MyComponent
public class MessageServiceImpl implements IMessageService {
    @Override
    public String sendMessage(String recMsg) {
        return String.format("received the message:%s" + recMsg);
    }
}
