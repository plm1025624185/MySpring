package com.plm.myspring.servlet.v1;

import com.plm.myspring.annotation.MyAutowired;
import com.plm.myspring.annotation.MyComponent;
import com.plm.myspring.annotation.MyController;
import com.plm.myspring.annotation.MyRequestMapping;
import com.plm.myspring.annotation.MyRequestParam;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
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
	private Map<String, Object> ioc = new LinkedHashMap<>(); // ioc容器
	private Map<String, Method> handlerMapping = new LinkedHashMap<>(); // 路径映射容器

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doPost(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// 第六步：委派请求
		try {
			doDispatcher(req, resp);
		} catch (Exception e) {
			PrintWriter write = resp.getWriter();
			write.print("500 exception!");
			e.printStackTrace(write);
		}
	}

	/**
	 * 对路径进行委派
	 * @param resp 
	 * @param req 
	 * @throws Exception 
	 */
	private void doDispatcher(HttpServletRequest req, HttpServletResponse resp) throws Exception {
		// 获取当前的请求路径
		String uri = req.getRequestURI();
		String contextPath = req.getContextPath();
		uri = uri.replace(contextPath, "");
	
		// 检查当前路径是否注册
		if (!handlerMapping.containsKey(uri)) throw new Exception("当前请求路径无法处理");
		Method method = handlerMapping.get(uri);
		Parameter[] params = method.getParameters(); // 获取运行时的参数
		Object[] paramObjs = new Object[params.length];
		for (int i = 0; i < params.length; i++) {
			Class<?> parameterType = params[i].getType();
			if (parameterType.equals(HttpServletRequest.class)) paramObjs[i] = req;
			else if (parameterType.equals(HttpServletResponse.class)) paramObjs[i] = resp;
			else {
				MyRequestParam annotation = params[i].getAnnotation(MyRequestParam.class); // 获取运行时参数上是否含有注解
				if (annotation == null) continue;
				String paramName = annotation.value();
				paramObjs[i] = req.getParameter(paramName);
			}
		}
		Object target = ioc.get(method.getDeclaringClass().getName());
		Object obj = method.invoke(target, paramObjs);
		resp.getWriter().write(obj.toString());
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
		
		System.out.println("初始化成功");
	}

	/**
	 * 对配置的url进行映射
	 */
	private void doHandlerMapping() {
		try {
			// 对所有的控制器进行映射处理
			for (Object controller : ioc.values()) {
				Class<?> clazz = controller.getClass();
				if (!clazz.isAnnotationPresent(MyController.class)) continue;
				String baseUrl = "/";
				// 首先判断控制器上是否有MyRequestMapping注解，有的话将值赋值给基地址
				if (clazz.isAnnotationPresent(MyRequestMapping.class)) {
					baseUrl += clazz.getAnnotation(MyRequestMapping.class).value();
				}
				// 获取所有方法，对有MyRequestMapping注解的方法进行存储
				for (Method method : clazz.getMethods()) {
					if (!method.isAnnotationPresent(MyRequestMapping.class)) continue;
					String path = baseUrl + "/" + method.getAnnotation(MyRequestMapping.class).value();
					path = path.replaceAll("/+", "/");
					if (handlerMapping.containsKey(path)) throw new Exception("地址路径已经存在！");
					handlerMapping.put(path, method);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 对所有标志了@MyAutowire的字段进行依赖注入
	 */
	private void doAutowired() {
		try {
			for( Object obj : ioc.values()) {
				// 对所有的私有属性进行判断
				for(Field field : obj.getClass().getDeclaredFields()) {
					if (!field.isAnnotationPresent(MyAutowired.class)) continue;
					// 字段有可能没有getset方法，所以强制赋值
					field.setAccessible(true);
					field.set(obj, ioc.get(field.getType().getName()));
				}
			}
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 对有对应注解的类进行实例化
	 */
	private void doInstance() {
		try {
			for (String str : classNameList) {
				Class<?> clazz = Class.forName(str);
				if (clazz.isAnnotationPresent(MyController.class)) {
					doInstanceForMyController(clazz);
				} else if (clazz.isAnnotationPresent(MyComponent.class)) {
					doInstanceForMyComponent(clazz);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 对MyComponent注解进行处理
	 * 由于实现类除了自身还有其接口类，
	 * 也需要将这些存放到ioc容器中去存储
	 * @param clazz
	 */
	private void doInstanceForMyComponent(Class<?> clazz) throws Exception {
		// 首先先将当前类存放到ioc容器中
		saveCommonClass(clazz);
		// 接口类进行处理
		for (Class<?> in : clazz.getInterfaces()) {
			if (ioc.containsKey(in.getName())) throw new Exception("已存在该接口实例！");
			ioc.put(in.getName(), ioc.get(clazz.getName()));
		}
	}

	/**
	 * 对MyContoller注解进行处理
	 * 控制器只需要直接进行初始化就好了
	 * @param clazz
	 */
	private void doInstanceForMyController(Class<?> clazz) throws Exception {
			saveCommonClass(clazz);
	}

	/**
	 * 使用类的全名作为ioc容器的key，实例作为ioc容器的value
	 * @param clazz
	 */
	private void saveCommonClass(Class<?> clazz) throws Exception {
		String key = clazz.getName();
		Object value = clazz.newInstance();
		// 判断当前key是否已经被占用，如果被占用了，就抛出异常
		if (ioc.containsKey(key)) throw  new Exception("ioc容器实例化失败，已经存在对应的键值：" + key);
		ioc.put(key, value);
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
