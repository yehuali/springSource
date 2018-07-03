```
private void parseDefaultElement(Element ele, BeanDefinitionParserDelegate delegate) {
    if (delegate.nodeNameEquals(ele, IMPORT_ELEMENT)) { //对 import 标签的处理
        importBeanDefinitionResource(ele);
    }else if (delegate.nodeNameEquals(ele, ALIAS_ELEMENT)) {//对 alias 标签的处理
        processAliasRegistration(ele);
    }else if (delegate.nodeNameEquals(ele, BEAN_ELEMENT)) { //对 bean 标签的处理
        processBeanDefinition(ele, delegate);
    }else if (delegate.nodeNameEquals(ele, NESTED_BEANS_ELEMENT)) {//对 beans 标签的处理
        doRegisterBeanDefinitions(ele);
    }
}
```
### 1.`bean`标签的解析及注册
```
protected void processBeanDefinition(Element ele, BeanDefinitionParserDelegate delegate) {
        BeanDefinitionHolder bdHolder = delegate.parseBeanDefinitionElement(ele);
        if (bdHolder != null) {
            bdHolder = delegate.decorateBeanDefinitionIfRequired(ele, bdHolder);
            try {
                BeanDefinitionReaderUtils.registerBeanDefinition(bdHolder, this.getReaderContext().getRegistry());
            } catch (BeanDefinitionStoreException var5) {
                this.getReaderContext().error("Failed to register bean definition with name '" + bdHolder.getBeanName() + "'", ele, var5);
            }
            this.getReaderContext().fireComponentRegistered(new BeanComponentDefinition(bdHolder));
        }
    }
```
（1）委托BeanDefinitionDelegate类的parseBeanDefinitionElement方法进行元素解析，返回BeanDefinitionHolder类型的实例bdHolder<br>
 &emsp;&emsp;&emsp;bdHolder实例已经包含配置文件中配置的各种属性了，例如class、name、id、alias之类<br>
（2）当bdHolder不为空，若存在默认标签的子节点下再有自定义属性，还需再次对自定义标签进行解析<br>
（3）解析完成后，需要对解析后的bdHolder进行注册，同样，注册操作委托给了BeanDefinitionReaderUtils的registerBeanDefinition方法<br>
（4）发出响应事件，通知相关的监听器，这个bean已经加载<br>
 ![]( https://github.com/yehuali/springSource/raw/master/note/images/processBeanDefinition配合时序图.jpg) <br>
 
 #### 1.1 解析BeanDefinition
 ```
    public BeanDefinitionHolder parseBeanDefinitionElement(Element ele, @Nullable BeanDefinition containingBean) {
        String id = ele.getAttribute("id");
        String nameAttr = ele.getAttribute("name");
        ....
        //解析其他所有属性并统一封装至 GenericBeanDefinition 类型的实例中
        AbstractBeanDefinition beanDefinition = this.parseBeanDefinitionElement(ele, beanName, containingBean);
        ...
        //将获取到的信息封装到 BeanDefinitionHolder 的实例中
        return new BeanDefinitionHolder(beanDefinition, beanName, aliasesArray);
    }
 ```
  在开始对属性展开全面解析前，Spring在外层又做了一个当前层的功能架构<br>
  在当前层完成的主要工作:<br>
 &emsp;(1)提取元素中的id以及name属性<br>
 &emsp;(2)进一步解析其他所有属性并统一封装至GenericBeanDefinition类型的实例中<br>
 &emsp;(3)如果检测到bean没有指定beanName，那么使用默认规则为此Bean生成beanName<br>
 &emsp;(4)将获得到的信息封装到BeanDefinitionHolder的实例中<br>
 （2）中对标签其他属性的解析过程<br>
 ```
    public AbstractBeanDefinition parseBeanDefinitionElement(Element ele, String beanName, @Nullable BeanDefinition containingBean) {
            this.parseState.push(new BeanEntry(beanName));
            String className = null;
            //解析class属性
            if (ele.hasAttribute("class")) {
                className = ele.getAttribute("class").trim();
            }
    
            String parent = null;
            //解析parent属性
            if (ele.hasAttribute("parent")) {
                parent = ele.getAttribute("parent");
            }
    
            try {
                //创建用于承载属性的AbstractBeanDefinition类型的GenericBeanDefinition
                AbstractBeanDefinition bd = this.createBeanDefinition(className, parent);
                //硬编码解析默认bean的各种属性
                this.parseBeanDefinitionAttributes(ele, beanName, containingBean, bd);
                //提取description
                bd.setDescription(DomUtils.getChildElementValueByTagName(ele, "description"));
                //解析元数据
                this.parseMetaElements(ele, bd);
                //解析lookup-method属性
                this.parseLookupOverrideSubElements(ele, bd.getMethodOverrides());
                //解析replaced-method属性
                this.parseReplacedMethodSubElements(ele, bd.getMethodOverrides());
                //解析构造函数参数
                this.parseConstructorArgElements(ele, bd);
                //解析property子元素
                this.parsePropertyElements(ele, bd);
                //解析qualifier子元素
                this.parseQualifierElements(ele, bd);
                bd.setResource(this.readerContext.getResource());
                bd.setSource(this.extractSource(ele));
                AbstractBeanDefinition var7 = bd;
                return var7;
            } catch (ClassNotFoundException var13) {
                this.error("Bean class [" + className + "] not found", ele, var13);
            } catch (NoClassDefFoundError var14) {
                this.error("Class that bean class [" + className + "] depends on not found", ele, var14);
            } catch (Throwable var15) {
                this.error("Unexpected failure during bean definition parsing", ele, var15);
            } finally {
                this.parseState.pop();
            }
    
            return null;
        }
 ```
 **1.创建用于属性承载的BeanDefinition**<br>
![](https://github.com/yehuali/springSource/raw/master/note/images/beanDefinition.png) <br>
BeanDefinition是配置文件<bean>元素标签在容器中的内部表示形式，beanDefinition和<bean>中的属性是一一对应的。<br>
RootBeanDefinition是最常用的实现类，对应一般性的<bean>元素标签，GenericBeanDefinition是自2.5版本以后新加入的bean文件配置属性定义类，是一站式服务类<br>
父<bean>用RootBeanDefinition表示，而子<bean>用ChildBeanDefinition表示，没有父<bean>的使用RootBeanDefinition表示，AbstractBeanDefinition对两者共同的类信息进行抽象<br>

Spring通过BeanDefinition将配置文件中的<bean>配置信息转换为容器的内部表示，将BeanDefinition注册到BeanDefinitionRegistry中<br>
BeanDefinitionRegistry像是Spring配置信息的内存数据库，以map形式保存，后续操作直接从BeanDefinitionRegistry中读取配置信息<br>

创建了bean信息的承载实例后，便可以进行bean信息的各种属性解析<br>
parseBeanDeinitionAttributes方法对element所有元素属性进行解析<br>
```
 public AbstractBeanDefinition parseBeanDefinitionAttributes(Element ele, String beanName, @Nullable BeanDefinition containingBean, AbstractBeanDefinition bd) {
        //解析scope属性
        if (ele.hasAttribute("singleton")) {
            this.error("Old 1.x 'singleton' attribute in use - upgrade to 'scope' declaration", ele);
        } else if (ele.hasAttribute("scope")) {
            bd.setScope(ele.getAttribute("scope"));
        } else if (containingBean != null) {
            //在嵌入beanDifition情况下且没有单独制定scope属性则使用父类默认的属性
            bd.setScope(containingBean.getScope());
        }
        
        //解析abstract属性
        if (ele.hasAttribute("abstract")) {
            bd.setAbstract("true".equals(ele.getAttribute("abstract")));
        }
        //解析laxy-init属性
        String lazyInit = ele.getAttribute("lazy-init");
        if ("default".equals(lazyInit)) {
            lazyInit = this.defaults.getLazyInit();
        }
        //若没有设置或设置成其他字符都会被设置为false
        bd.setLazyInit("true".equals(lazyInit));
        
          //解析autowire属性
        String autowire = ele.getAttribute("autowire");
        bd.setAutowireMode(this.getAutowireMode(autowire));
        
        //解析 depends-on 属性
        String autowireCandidate;
        if (ele.hasAttribute("depends-on")) {
            autowireCandidate = ele.getAttribute("depends-on");
            bd.setDependsOn(StringUtils.tokenizeToStringArray(autowireCandidate, ",; "));
        }
        
        //解析 autowire-candidate 属性
        autowireCandidate = ele.getAttribute("autowire-candidate");
        String destroyMethodName;
        if (!"".equals(autowireCandidate) && !"default".equals(autowireCandidate)) {
            bd.setAutowireCandidate("true".equals(autowireCandidate));
        } else {
            destroyMethodName = this.defaults.getAutowireCandidates();
            if (destroyMethodName != null) {
                String[] patterns = StringUtils.commaDelimitedListToStringArray(destroyMethodName);
                bd.setAutowireCandidate(PatternMatchUtils.simpleMatch(patterns, beanName));
            }
        }
        
        //解析 primary 属性
        if (ele.hasAttribute("primary")) {
            bd.setPrimary("true".equals(ele.getAttribute("primary")));
        }
        
        //解析 init-method 属性
        if (ele.hasAttribute("init-method")) {
            destroyMethodName = ele.getAttribute("init-method");
            bd.setInitMethodName(destroyMethodName);
        } else if (this.defaults.getInitMethod() != null) {
            bd.setInitMethodName(this.defaults.getInitMethod());
            bd.setEnforceInitMethod(false);
        }
        
        //解析 destroy-method 属性
        if (ele.hasAttribute("destroy-method")) {
            destroyMethodName = ele.getAttribute("destroy-method");
            bd.setDestroyMethodName(destroyMethodName);
        } else if (this.defaults.getDestroyMethod() != null) {
            bd.setDestroyMethodName(this.defaults.getDestroyMethod());
            bd.setEnforceDestroyMethod(false);
        }
        
        //解析 factory-method 属性
        if (ele.hasAttribute("factory-method")) {
            bd.setFactoryMethodName(ele.getAttribute("factory-method"));
        }
        
        //解析 factory-bean 属性
        if (ele.hasAttribute("factory-bean")) {
            bd.setFactoryBeanName(ele.getAttribute("factory-bean"));
        }

        return bd;
    }
```
**2.解析子元素meta** <br>
```
<bean id="myTestBean" class="bean.MyTestBean">
    <meta key="testStr" value="aaaaaaaa"/>
</bean>
```
```
public void parseMetaElements(Element ele, BeanMetadataAttributeAccessor attributeAccessor) {
       //获取当前节点的所有子元素
        NodeList nl = ele.getChildNodes();

        for(int i = 0; i < nl.getLength(); ++i) {
            Node node = nl.item(i);
            //提取meta
            if (this.isCandidateElement(node) && this.nodeNameEquals(node, "meta")) {
                Element metaElement = (Element)node;
                String key = metaElement.getAttribute("key");
                String value = metaElement.getAttribute("value");
                //使用key、value构造BeanMetadataAttribute
                BeanMetadataAttribute attribute = new BeanMetadataAttribute(key, value);
                attribute.setSource(this.extractSource(metaElement));
                //记录信息
                attributeAccessor.addMetadataAttribute(attribute);
            }
        }

    }
```
**3.解析子元素lookup-method** <br>
```
public void parseLookupOverrideSubElements(Element beanEle, MethodOverrides overrides) {
		NodeList nl = beanEle.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
			Node node = nl.item(i);
			if (isCandidateElement(node) && nodeNameEquals(node, LOOKUP_METHOD_ELEMENT)) {
				Element ele = (Element) node;
				//获取要修饰的方法
				String methodName = ele.getAttribute(NAME_ATTRIBUTE);
				//获取配置返回的bean
				String beanRef = ele.getAttribute(BEAN_ELEMENT);
				LookupOverride override = new LookupOverride(methodName, beanRef);
				override.setSource(extractSource(ele));
				overrides.addOverride(override);
			}
		}
	}
```
**4.解析子元素replaced-method** <br>
```
public void parseReplacedMethodSubElements(Element beanEle, MethodOverrides overrides) {
		NodeList nl = beanEle.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
			Node node = nl.item(i);
			if (isCandidateElement(node) && nodeNameEquals(node, REPLACED_METHOD_ELEMENT)) {
				Element replacedMethodEle = (Element) node;
				//提取要替换的旧的方法
				String name = replacedMethodEle.getAttribute(NAME_ATTRIBUTE);
				//提取对应的新的替换方法
				String callback = replacedMethodEle.getAttribute(REPLACER_ATTRIBUTE);
				ReplaceOverride replaceOverride = new ReplaceOverride(name, callback);
				// Look for arg-type match elements.
				List<Element> argTypeEles = DomUtils.getChildElementsByTagName(replacedMethodEle, ARG_TYPE_ELEMENT);
				for (Element argTypeEle : argTypeEles) {
				    //记录参数
					String match = argTypeEle.getAttribute(ARG_TYPE_MATCH_ATTRIBUTE);
					match = (StringUtils.hasText(match) ? match : DomUtils.getTextValue(argTypeEle));
					if (StringUtils.hasText(match)) {
						replaceOverride.addTypeIdentifier(match);
					}
				}
				replaceOverride.setSource(extractSource(replacedMethodEle));
				overrides.addOverride(replaceOverride);
			}
		}
	}
```
无论look-up还是replaced-method都是构造一个MethodOverride，并最终记录在了AbstractBeanDefinition中的methodOverrides属性中<br>

**5.解析子元素constructor-arg** <br>
```
public void parseConstructorArgElements(Element beanEle, BeanDefinition bd) {
		NodeList nl = beanEle.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
			Node node = nl.item(i);
			if (isCandidateElement(node) && nodeNameEquals(node, CONSTRUCTOR_ARG_ELEMENT)) {
				parseConstructorArgElement((Element) node, bd);
			}
		}
	}
```
```
public void parseConstructorArgElement(Element ele, BeanDefinition bd) {
        //提取index属性
		String indexAttr = ele.getAttribute(INDEX_ATTRIBUTE);
		//提取type属性
		String typeAttr = ele.getAttribute(TYPE_ATTRIBUTE);
		//提取name属性
		String nameAttr = ele.getAttribute(NAME_ATTRIBUTE);
		if (StringUtils.hasLength(indexAttr)) {
			try {
				int index = Integer.parseInt(indexAttr);
				if (index < 0) {
					error("'index' cannot be lower than 0", ele);
				}
				else {
					try {
						this.parseState.push(new ConstructorArgumentEntry(index));
						//解析ele对应的属性元素
						Object value = parsePropertyValue(ele, bd, null);
						ConstructorArgumentValues.ValueHolder valueHolder = new ConstructorArgumentValues.ValueHolder(value);
						if (StringUtils.hasLength(typeAttr)) {
							valueHolder.setType(typeAttr);
						}
						if (StringUtils.hasLength(nameAttr)) {
							valueHolder.setName(nameAttr);
						}
						valueHolder.setSource(extractSource(ele));
						//不允许重复指定相同参数
						if (bd.getConstructorArgumentValues().hasIndexedArgumentValue(index)) {
							error("Ambiguous constructor-arg entries for index " + index, ele);
						}
						else {
							bd.getConstructorArgumentValues().addIndexedArgumentValue(index, valueHolder);
						}
					}
					finally {
						this.parseState.pop();
					}
				}
			}
			catch (NumberFormatException ex) {
				error("Attribute 'index' of tag 'constructor-arg' must be an integer", ele);
			}
		}
		else {//没有index属性则忽略去属性，自动寻找
			try {
				this.parseState.push(new ConstructorArgumentEntry());
				Object value = parsePropertyValue(ele, bd, null);
				ConstructorArgumentValues.ValueHolder valueHolder = new ConstructorArgumentValues.ValueHolder(value);
				if (StringUtils.hasLength(typeAttr)) {
					valueHolder.setType(typeAttr);
				}
				if (StringUtils.hasLength(nameAttr)) {
					valueHolder.setName(nameAttr);
				}
				valueHolder.setSource(extractSource(ele));
				bd.getConstructorArgumentValues().addGenericArgumentValue(valueHolder);
			}
			finally {
				this.parseState.pop();
			}
		}
	}

```
首先是提取constructor-arg上必要的属性(index、type、name)<br>
* 如果配置中指定了index属性<br>
 （1）解析constructor-arg的子元素<br>
 （2）使用ConstructorArgumentValues.ValueHolder类型来封装解析出来的元素 <br>
 （3）将type、name和index属性一并封装在ConstructorArgumentValues.ValueHolder类型中并添加至当前BeanDefinition的constructorArgymentValues的indexedArgumentValues属性中<br>
* 如果没有指定index属性<br>
  流程和指定了index属性类似，只是添加到当前BeanDefinition的constructorArgumentValues的genericArgumentValues属性中<br>
  
**解析构造函数配置中子元素的过程：parsePropertyValue** <br>
```
public Object parsePropertyValue(Element ele, BeanDefinition bd, @Nullable String propertyName) {
		String elementName = (propertyName != null ?
				"<property> element for property '" + propertyName + "'" :
				"<constructor-arg> element");

		// Should only have one child element: ref, value, list, etc.
		//一个属性只能对应一种类型：ref、value、list等
		NodeList nl = ele.getChildNodes();
		Element subElement = null;
		for (int i = 0; i < nl.getLength(); i++) {
			Node node = nl.item(i);
			//对应description或者meta不处理
			if (node instanceof Element && !nodeNameEquals(node, DESCRIPTION_ELEMENT) &&
					!nodeNameEquals(node, META_ELEMENT)) {
				// Child element is what we're looking for.
				if (subElement != null) {
					error(elementName + " must not contain more than one sub-element", ele);
				}
				else {
					subElement = (Element) node;
				}
			}
		}
        //解析constructor-arg上的ref属性
		boolean hasRefAttribute = ele.hasAttribute(REF_ATTRIBUTE);
		//解析constructor-arg上的value属性
		boolean hasValueAttribute = ele.hasAttribute(VALUE_ATTRIBUTE);
		if ((hasRefAttribute && hasValueAttribute) ||
				((hasRefAttribute || hasValueAttribute) && subElement != null)) {
			/**
			  *在constructor-arg上不存在：
			  *1.同时既有ref属性又有value属性
			  *2.存在ref属性或者value属性且又有子元素
			  */
			error(elementName +
					" is only allowed to contain either 'ref' attribute OR 'value' attribute OR sub-element", ele);
		}

		if (hasRefAttribute) {
		    //ref属性的处理，使用RuntimeBeanReference封装对应的ref名称
			String refName = ele.getAttribute(REF_ATTRIBUTE);
			if (!StringUtils.hasText(refName)) {
				error(elementName + " contains empty 'ref' attribute", ele);
			}
			RuntimeBeanReference ref = new RuntimeBeanReference(refName);
			ref.setSource(extractSource(ele));
			return ref;
		}
		else if (hasValueAttribute) {
		    //value属性的处理，使用TypedStringValue封装
			TypedStringValue valueHolder = new TypedStringValue(ele.getAttribute(VALUE_ATTRIBUTE));
			valueHolder.setSource(extractSource(ele));
			return valueHolder;
		}
		else if (subElement != null) {
		    //解析子元素
			return parsePropertySubElement(subElement, bd);
		}
		else {
			// Neither child element nor "ref" or "value" attribute found.
			error(elementName + " must specify a ref or value", ele);
			return null;
		}
	}
```
**子元素的处理** <br>
```
public Object parsePropertySubElement(Element ele, @Nullable BeanDefinition bd, @Nullable String defaultValueType) {
		if (!isDefaultNamespace(ele)) {
			return parseNestedCustomElement(ele, bd);
		}
		else if (nodeNameEquals(ele, BEAN_ELEMENT)) {
			BeanDefinitionHolder nestedBd = parseBeanDefinitionElement(ele, bd);
			if (nestedBd != null) {
				nestedBd = decorateBeanDefinitionIfRequired(ele, nestedBd, bd);
			}
			return nestedBd;
		}
		else if (nodeNameEquals(ele, REF_ELEMENT)) {
			// A generic reference to any name of any bean.
			String refName = ele.getAttribute(BEAN_REF_ATTRIBUTE);
			boolean toParent = false;
			if (!StringUtils.hasLength(refName)) {
				// A reference to the id of another bean in a parent context.
				refName = ele.getAttribute(PARENT_REF_ATTRIBUTE);
				toParent = true;
				if (!StringUtils.hasLength(refName)) {
					error("'bean' or 'parent' is required for <ref> element", ele);
					return null;
				}
			}
			if (!StringUtils.hasText(refName)) {
				error("<ref> element contains empty target attribute", ele);
				return null;
			}
			RuntimeBeanReference ref = new RuntimeBeanReference(refName, toParent);
			ref.setSource(extractSource(ele));
			return ref;
		}
		else if (nodeNameEquals(ele, IDREF_ELEMENT)) {
			return parseIdRefElement(ele);
		}
		else if (nodeNameEquals(ele, VALUE_ELEMENT)) {
			return parseValueElement(ele, defaultValueType);
		}
		else if (nodeNameEquals(ele, NULL_ELEMENT)) {
			// It's a distinguished null value. Let's wrap it in a TypedStringValue
			// object in order to preserve the source location.
			TypedStringValue nullHolder = new TypedStringValue(null);
			nullHolder.setSource(extractSource(ele));
			return nullHolder;
		}
		else if (nodeNameEquals(ele, ARRAY_ELEMENT)) {
			return parseArrayElement(ele, bd);
		}
		else if (nodeNameEquals(ele, LIST_ELEMENT)) {
			return parseListElement(ele, bd);
		}
		else if (nodeNameEquals(ele, SET_ELEMENT)) {
			return parseSetElement(ele, bd);
		}
		else if (nodeNameEquals(ele, MAP_ELEMENT)) {
			return parseMapElement(ele, bd);
		}
		else if (nodeNameEquals(ele, PROPS_ELEMENT)) {
			return parsePropsElement(ele);
		}
		else {
			error("Unknown property sub-element: [" + ele.getNodeName() + "]", ele);
			return null;
		}
	}
```
**6.解析子元素property**<br>
```
    public void parsePropertyElement(Element ele, BeanDefinition bd) {
        //获取配置元素中 name 的值
        String propertyName = ele.getAttribute(NAME_ATTRIBUTE);
        if (!StringUtils.hasLength(propertyName)) {
            error("Tag 'property' must have a 'name' attribute", ele);
            return;
        }
        this.parseState.push(new PropertyEntry(propertyName));
        try {
            //不允许多次对同一属性配置
            if (bd.getPropertyValues().contains(propertyName)) {
                error("Multiple 'property' definitions for property '" + propertyName + "'", ele);
                return;
            }
            Object val = parsePropertyValue(ele, bd, propertyName);
            PropertyValue pv = new PropertyValue(propertyName, val);
            parseMetaElements(ele, pv);
            pv.setSource(extractSource(ele));
            bd.getPropertyValues().addPropertyValue(pv);
        }finally {
            this.parseState.pop();
        }
    }

```

### 1.2解析默认标签中的自定义标签元素
bdHolder = delegate.decorateBeanDefinitionIfRequired(ele, bdHolder)代码的分析<br>
场景：<br>
```
<bean id="test" class="test.MyClass">
    <mybean:user username="aaa"/>
</bean>
```
```a
public BeanDefinitionHolder decorateBeanDefinitionIfRequired(Element ele, BeanDefinitionHolder definitionHolder) {
		return decorateBeanDefinitionIfRequired(ele, definitionHolder, null);
}
```
第三个参数是父类bean，当对某个嵌套配置进行分析时，需要传递父类beanDefinition<br>
这里传递参数其实为了使用父类的scope属性，以备子类若没有设置scope时默认使用父类的属性。这里分析的是顶层配置，所以传递null
```
public BeanDefinitionHolder decorateBeanDefinitionIfRequired(Element ele, BeanDefinitionHolder definitionHolder, BeanDefinition containingBd) {
    BeanDefinitionHolder finalDefinition = definitionHolder;
    NamedNodeMap attributes = ele.getAttributes();
    //遍历所有的属性，看看是否有适用于修饰的属性
    for (int i = 0; i < attributes.getLength(); i++) {
        Node node = attributes.item(i);
        finalDefinition = decorateIfRequired(node, finalDefinition, containingBd);
    }
    NodeList children = ele.getChildNodes();
    //遍历所有的子节点，看看是否有适用于修饰的子元素
    for (int i = 0; i < children.getLength(); i++) {
        Node node = children.item(i);
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            finalDefinition = decorateIfRequired(node, finalDefinition, containingBd);
        }
    }
    return finalDefinition;
}
```
```
private BeanDefinitionHolder decorateIfRequired(Node node, BeanDefinitionHolder originalDef, BeanDefinition containingBd) {
    //获取自定义标签的命名空间
    String namespaceUri = getNamespaceURI(node);
    //对于非默认标签进行修饰
    if (!isDefaultNamespace(namespaceUri)) {
        //根据命名空间找到对应的处理器
        NamespaceHandler handler = this.readerContext.getNamespaceHandlerResolver().resolve(namespaceUri);
        if (handler != null) {
        //进行修饰
        return handler.decorate(node, originalDef, new ParserContext(this.readerContext,this, containingBd));
        }else if (namespaceUri != null && namespaceUri.startsWith("http: //www.
            Springframework.org/")) {
            error("Unable to locate Spring NamespaceHandler for XML schema namespace [" + namespaceUri + "]", node);
        }else {
            // A custom namespace, not to be handled by Spring - maybe "xml:...".
            if (logger.isDebugEnabled()) {
                logger.debug("No Spring NamespaceHandler found for XML schemanamespace [" + namespaceUri + "]");
            }
        }
    }
    return originalDef;
}
```
在decorateBeanDefinitionIfRequired中，默认标签直接忽略，因为之前已经被处理了，只处理bean的自定义属性<br>

### 1.3 注册解析的BeanDefinition
BeanDefinitionReaderUtils.registerBeanDefinition(bdHolder, getReaderContext().getRegistry())代码解析<br>
```
public static void registerBeanDefinition(BeanDefinitionHolder definitionHolder, BeanDefinitionRegistry registry)throws BeanDefinitionStoreException {
    //使用 beanName 做唯一标识注册
    String beanName = definitionHolder.getBeanName();
    registry.registerBeanDefinition(beanName, definitionHolder.getBeanDefinition());
    
    //注册所有的别名
    String[] aliases = definitionHolder.getAliases();
    if (aliases != null) {
        for (String aliase : aliases) {
            registry.registerAlias(beanName, aliase);
        }
    }
}
```
**1.通过beanName注册BeanDefinition** <br>
```
public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition)
			throws BeanDefinitionStoreException {

		Assert.hasText(beanName, "Bean name must not be empty");
		Assert.notNull(beanDefinition, "BeanDefinition must not be null");

		if (beanDefinition instanceof AbstractBeanDefinition) {
			try {
			    /**
			     *注册前的最后一次校验，这里的校验不同于之前的XML文件校验
			     *主要是对于AbstractBeanDefinition属性中的methodOverrides校验
			     */校验methodOverrides是否与工厂方法并存或者methodOberrides对应的方法根本不存在
				((AbstractBeanDefinition) beanDefinition).validate();
			}
			catch (BeanDefinitionValidationException ex) {
				throw new BeanDefinitionStoreException(beanDefinition.getResourceDescription(), beanName,
						"Validation of bean definition failed", ex);
			}
		}

		BeanDefinition oldBeanDefinition;

		oldBeanDefinition = this.beanDefinitionMap.get(beanName);
		if (oldBeanDefinition != null) {
		    //如果对应的BeanName已经注册且在配置中配置了bean不允许被覆盖，则抛出异常
			if (!isAllowBeanDefinitionOverriding()) {
				throw new BeanDefinitionStoreException(beanDefinition.getResourceDescription(), beanName,
						"Cannot register bean definition [" + beanDefinition + "] for bean '" + beanName +
						"': There is already [" + oldBeanDefinition + "] bound.");
			}
			else if (oldBeanDefinition.getRole() < beanDefinition.getRole()) {
				// e.g. was ROLE_APPLICATION, now overriding with ROLE_SUPPORT or ROLE_INFRASTRUCTURE
				if (logger.isWarnEnabled()) {
					logger.warn("Overriding user-defined bean definition for bean '" + beanName +
							"' with a framework-generated bean definition: replacing [" +
							oldBeanDefinition + "] with [" + beanDefinition + "]");
				}
			}
			else if (!beanDefinition.equals(oldBeanDefinition)) {
				if (logger.isInfoEnabled()) {
					logger.info("Overriding bean definition for bean '" + beanName +
							"' with a different definition: replacing [" + oldBeanDefinition +
							"] with [" + beanDefinition + "]");
				}
			}
			else {
				if (logger.isDebugEnabled()) {
					logger.debug("Overriding bean definition for bean '" + beanName +
							"' with an equivalent definition: replacing [" + oldBeanDefinition +
							"] with [" + beanDefinition + "]");
				}
			}
			//注册beanDefinition
			this.beanDefinitionMap.put(beanName, beanDefinition);
		}
		else {
			if (hasBeanCreationStarted()) {
			    //因为beanDefinitionMap是全局变量，这里定会存在并发访问的情况
				// Cannot modify startup-time collection elements anymore (for stable iteration)
				synchronized (this.beanDefinitionMap) {
					this.beanDefinitionMap.put(beanName, beanDefinition);
					List<String> updatedDefinitions = new ArrayList<>(this.beanDefinitionNames.size() + 1);
					updatedDefinitions.addAll(this.beanDefinitionNames);
					updatedDefinitions.add(beanName);
					this.beanDefinitionNames = updatedDefinitions;
					if (this.manualSingletonNames.contains(beanName)) {
						Set<String> updatedSingletons = new LinkedHashSet<>(this.manualSingletonNames);
						updatedSingletons.remove(beanName);
						this.manualSingletonNames = updatedSingletons;
					}
				}
			}
			else {
				// Still in startup registration phase
				this.beanDefinitionMap.put(beanName, beanDefinition);
				this.beanDefinitionNames.add(beanName);
				this.manualSingletonNames.remove(beanName);
			}
			this.frozenBeanDefinitionNames = null;
		}

		if (oldBeanDefinition != null || containsSingleton(beanName)) {
			resetBeanDefinition(beanName);
		}
	}
```
### 1.4 通知监听器解析及注册完成
 getReaderContext().fireComponentRegistered(new BeanComponentDefinition(bdHolder)); <br>
 这里实现只为扩展，当需要对注册BeanDefinition事件进行监听时可以通过注册监听器的方式并将处理逻辑写入监听器中，目前在Spring中并没有对此事件做任何逻辑处理<br>
 
 
