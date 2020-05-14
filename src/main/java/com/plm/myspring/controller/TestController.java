package com.plm.myspring.controller;

import com.plm.myspring.annotation.MyController;
import com.plm.myspring.annotation.MyRequestMapping;

@MyController
public class TestController {
	
	@MyRequestMapping("test")
	public String test() {
		return "test";
	}
	
	@MyRequestMapping("ok")
	public String ok() {
		return "ok";
	}
}
