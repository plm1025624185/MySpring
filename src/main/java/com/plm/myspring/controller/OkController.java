package com.plm.myspring.controller;

import com.plm.myspring.annotation.MyAutowired;
import com.plm.myspring.annotation.MyController;
import com.plm.myspring.annotation.MyRequestMapping;
import com.plm.myspring.service.IGreetingService;
import com.plm.myspring.service.IMessageService;

@MyController
@MyRequestMapping("ok")
public class OkController {

	@MyAutowired
	private IMessageService messageService;

	@MyAutowired
	private IGreetingService greetingService;

	@MyRequestMapping("ok")
	public String ok() {
		return "ok";
	}
	
	public String test() {
		return "test";
	}
}
