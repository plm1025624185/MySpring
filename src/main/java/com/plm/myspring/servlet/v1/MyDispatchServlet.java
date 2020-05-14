package com.plm.myspring.servlet.v1;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class MyDispatchServlet extends HttpServlet {
	// 全局初始化参数key
	private static final String CONTEXT_CONFIG_LOCATION = "contextConfigLocation";
	
	private Properties contextConfig = new Properties(); // 全局配置文件
	private List<String> classNameList = new LinkedList<>(); // 符合条件的类文件 
	private Map<String, Object> ioc = new LinkedHashMap<>(); //

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doPost(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// 第六步：委派请求
		doDispatcher();
	}

	private void doDispatcher() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void init(ServletConfig config) throws ServletException {
		// 第一步：加载配置文件
		doLoadConfig(config.getInitParameter(CONTEXT_CONFIG_LOCATION));
		
		// 第二步：扫描类文件
		doScanner(contextConfig.getProperty("scanPackage"));
		
		// 第三步：初始化IoC容器，实例化相关类，并保存到IoC容器中   IoC部分
		doInstance();
		
		// 第四步：依赖注入     DI部分
		doAutowired();
		
		// 第五步：初始化HandlerMapping     MVC部分
		doHandlerMapping();
	}

	private void doHandlerMapping() {
		// TODO Auto-generated method stub
		
	}

	private void doAutowired() {
		// TODO Auto-generated method stub
		
	}

	/**
	 * 对有对应注解的类进行实例化
	 */
	private void doInstance() {
		
	}

	/**
	 * 对配置的scanPackage下的文件进行扫描，如果是文件夹就递归，
	 * 如果是文件，判断是否是class文件，是就进行存储，否则不做处理
	 * @param scanPackage
	 */
	private void doScanner(String scanPackage) {
		URL url = getClass().getClassLoader().getResource(scanPackage.replaceAll("\\.", "/"));
		File packageFile = new File(url.getFile());
		for (File file : packageFile.listFiles()) {
			if (file.isDirectory()) doScanner(scanPackage + "." + file.getName());
			else if (file.getName().endsWith(".class")) {
				String className = scanPackage + "." + file.getName().replace(".class", "");
				classNameList.add(className);
			}	
		}
	}

	private void doLoadConfig(String contextConfigLocation) {
		InputStream is = getClass().getClassLoader().getResourceAsStream(contextConfigLocation);
		try {
			contextConfig.load(is);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

}
