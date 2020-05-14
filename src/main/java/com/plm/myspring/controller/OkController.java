package com.plm.myspring.controller;

import com.plm.myspring.annotation.MyController;
import com.plm.myspring.annotation.MyRequestMapping;

@MyController
@MyRequestMapping("ok")
public class OkController {

	@MyRequestMapping("ok")
	public String ok() {
		return "ok";
	}
	
	public String test() {
		return "test";
	}
}
