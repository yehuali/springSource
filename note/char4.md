扩展Spring自定义标签配置步骤：
* 创建一个需要扩展的组件
* 定义一个XSD文件描述组件内容
* 创建一个文件，实现BeanDefinitionParser接口，用来解析XSD文件中的定义和组件定义
* 创建一个Handler文件，扩展子NamespaceHandlerSupport，目的是将组建注册到Spring容器
* 编写Spring.handlers和Spring.shcemas文件

```
@Nullable
	public BeanDefinition parseCustomElement(Element ele, @Nullable BeanDefinition containingBd) {
	    //获取对应的命名空间
		String namespaceUri = getNamespaceURI(ele);
		if (namespaceUri == null) {
			return null;
		}
		NamespaceHandler handler = this.readerContext.getNamespaceHandlerResolver().resolve(namespaceUri);
		if (handler == null) {
			error("Unable to locate Spring NamespaceHandler for XML schema namespace [" + namespaceUri + "]", ele);
			return null;
		}
		//调用自定义的NamespaceHandler进行解析
		return handler.parse(ele, new ParserContext(this.readerContext, this, containingBd));
	}
```
### 1.获取标签的命名空间
  标签的解析是从命名空间开始的，提取对应元素的命名空间在org.w3c.dom.Node中已经提供了方法<br>
    ```
    public String getNamespaceURI(Node node) {
    		return node.getNamespaceURI();
    	}
    ```
### 2.提取自定义标签处理器
  有了命名空间，就可以进行NamespaceHandler的提取了：NamespaceHandler handler = this.readerContext.getNamespaceHandlerResolver().resolve(namespaceUri);<br>
  在readerContext初始化的时候其属性namespaceHandlerResolver已经被初始化为了DefaultNamespaceHandlerResolver的实例<br>
```
public NamespaceHandler resolve(String namespaceUri) {
        //获取所有已经配置的handler映射
		Map<String, Object> handlerMappings = getHandlerMappings();
		//根据命名空间找到对应的信息
		Object handlerOrClassName = handlerMappings.get(namespaceUri);
		if (handlerOrClassName == null) {
			return null;
		}
		else if (handlerOrClassName instanceof NamespaceHandler) {
		    //已经做过解析的情况，直接从缓存读取
			return (NamespaceHandler) handlerOrClassName;
		}
		else { 
		    //没有做过解析，则返回的是类路径
			String className = (String) handlerOrClassName;
			try {
			    //使用反射将类路径转化为类
				Class<?> handlerClass = ClassUtils.forName(className, this.classLoader);
				if (!NamespaceHandler.class.isAssignableFrom(handlerClass)) {
					throw new FatalBeanException("Class [" + className + "] for namespace [" + namespaceUri +
							"] does not implement the [" + NamespaceHandler.class.getName() + "] interface");
				}
				//初始化类
				NamespaceHandler namespaceHandler = (NamespaceHandler) BeanUtils.instantiateClass(handlerClass);
				//调用自定义的NamespaceHandler的初始化方法
				namespaceHandler.init();
				//记录在缓存
				handlerMappings.put(namespaceUri, namespaceHandler);
				return namespaceHandler;
			}
			catch (ClassNotFoundException ex) {
				throw new FatalBeanException("Could not find NamespaceHandler class [" + className +
						"] for namespace [" + namespaceUri + "]", ex);
			}
			catch (LinkageError err) {
				throw new FatalBeanException("Unresolvable class definition for NamespaceHandler class [" +
						className + "] for namespace [" + namespaceUri + "]", err);
			}
		}
	}
```
getHandlerMappings的主要功能是读取Spring.handlers配置文件并将配置文件缓存在map中<br>
```
private Map<String, Object> getHandlerMappings() {
		Map<String, Object> handlerMappings = this.handlerMappings;
		//如果没有被缓存则开始进行缓存
		if (handlerMappings == null) {
			synchronized (this) {
				handlerMappings = this.handlerMappings;
				if (handlerMappings == null) {
					try {
					    //this.handlerMappingsLocation在构造函数中已经被初始化为:META-INF/Spring.handlers
						Properties mappings =
								PropertiesLoaderUtils.loadAllProperties(this.handlerMappingsLocation, this.classLoader);
						if (logger.isDebugEnabled()) {
							logger.debug("Loaded NamespaceHandler mappings: " + mappings);
						}
						Map<String, Object> mappingsToUse = new ConcurrentHashMap<>(mappings.size());
						//将Properties格式文件合并到Map格式的handlerMappings中
						CollectionUtils.mergePropertiesIntoMap(mappings, mappingsToUse);
						handlerMappings = mappingsToUse;
						this.handlerMappings = handlerMappings;
					}
					catch (IOException ex) {
						throw new IllegalStateException(
								"Unable to load NamespaceHandler mappings from location [" + this.handlerMappingsLocation + "]", ex);
					}
				}
			}
		}
		return handlerMappings;
	}
```

### 3.标签解析  
  MyNamespaceHandler已经完成了初始化工作，但是实现的自定义命名空间处理器并没有实现parse方法。所以推断方法是父类的实现<br>
 ```
    public BeanDefinition parse(Element element, ParserContext parserContext) {
        //寻找解析器并进行解析操作
        BeanDefinitionParser parser = this.findParserForElement(element, parserContext);
        return parser != null ? parser.parse(element, parserContext) : null;
    }
```
```
private BeanDefinitionParser findParserForElement(Element element, ParserContext parserContext) {
        //获取元素名称，也就是<myname:user>中的user
        String localName = parserContext.getDelegate().getLocalName(element);
        //根据user找到对应的解析器,也就是在registerBeanDefinitionParser("user", new UserBeanDefinitionParser());
        BeanDefinitionParser parser = (BeanDefinitionParser)this.parsers.get(localName);
        if (parser == null) {
            parserContext.getReaderContext().fatal("Cannot locate BeanDefinitionParser for element [" + localName + "]", element);
        }

        return parser;
    }
```
```
public final BeanDefinition parse(Element element, ParserContext parserContext) {
		AbstractBeanDefinition definition = parseInternal(element, parserContext);
		if (definition != null && !parserContext.isNested()) {
			try {
				String id = resolveId(element, definition, parserContext);
				if (!StringUtils.hasText(id)) {
					parserContext.getReaderContext().error(
							"Id is required for element '" + parserContext.getDelegate().getLocalName(element)
									+ "' when used as a top-level tag", element);
				}
				String[] aliases = null;
				if (shouldParseNameAsAliases()) {
					String name = element.getAttribute(NAME_ATTRIBUTE);
					if (StringUtils.hasLength(name)) {
						aliases = StringUtils.trimArrayElements(StringUtils.commaDelimitedListToStringArray(name));
					}
				}
				BeanDefinitionHolder holder = new BeanDefinitionHolder(definition, id, aliases);
				registerBeanDefinition(holder, parserContext.getRegistry());
				if (shouldFireEvents()) {
					BeanComponentDefinition componentDefinition = new BeanComponentDefinition(holder);
					postProcessComponentDefinition(componentDefinition);
					parserContext.registerComponent(componentDefinition);
				}
			}
			catch (BeanDefinitionStoreException ex) {
				String msg = ex.getMessage();
				parserContext.getReaderContext().error((msg != null ? msg : ex.toString()), element);
				return null;
			}
		}
		return definition;
	}
```
函数中大部分的代码用来处理解析后的AbstractBeanDefinition转化为BeanDefinitionHolder并注册的功能，而解析的事情委托给函数parseInternal<br>
```
protected final AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition();
		String parentName = getParentName(element);
		if (parentName != null) {
			builder.getRawBeanDefinition().setParentName(parentName);
		}
		//获取自定义标签中的class，此时会调用自定义解析器如UserBeanDefinitionParser中的getBeanClass方法
		Class<?> beanClass = getBeanClass(element);
		if (beanClass != null) {
			builder.getRawBeanDefinition().setBeanClass(beanClass);
		}
		else {
		    //若子类没有重写getBeanClass方法则尝试检查子类是否重写getBeanClassName方法
			String beanClassName = getBeanClassName(element);
			if (beanClassName != null) {
				builder.getRawBeanDefinition().setBeanClassName(beanClassName);
			}
		}
		builder.getRawBeanDefinition().setSource(parserContext.extractSource(element));
		BeanDefinition containingBd = parserContext.getContainingBeanDefinition();
		if (containingBd != null) {
			// Inner bean definition must receive same scope as containing bean.
			builder.setScope(containingBd.getScope());
		}
		if (parserContext.isDefaultLazyInit()) {
			// Default-lazy-init applies to custom bean definitions as well.
			builder.setLazyInit(true);
		}
		//调用子类重写的doParse方法进行解析
		doParse(element, parserContext, builder);
		return builder.getBeanDefinition();
	}
```

     