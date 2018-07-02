##1.核心类介绍
### 1.1 DefaultListableBeanFactory
  Xml继承自DefaultListableBeanFactory，而DefaultListableBeanFactory是整个bean加载的核心部分，是spring注册和加载bean的默认实现<br>
  而对于XmlBeanFactory与DefaultListableBeanFactory不同是前者使用了自定义的XML读取器XmlBeanDefinitionReader，而后者继承了AbstractAutowireCapbleBeanFactory并实现了ConfigurableListableBeanFactory以及BeanDefinitionRegistry接口
  
  ![]( https://github.com/yehuali/springSource/raw/master/note/images/DefaultListableBeanFactory.jpg)
  * AliasRegistry:定义对alias的简单增删改等操作
  * SimpleAliasRegistry:主要使用map作为alias的缓存，并对接口AliasRegistry进行实现
  * SingletonBeanRegistry：定义对单例的注册及获取
  * BeanFactory:定义获取bean及bean的各种属性
  * DefaultSingletonBeanRegistry:对接口SingletonBeanRegistry各函数的实现
  * HierarchicalBeanFactory：继承BeanFactory，在BeanFactory基础上增加了parentFactory的支持
  * BeanDefinitionRegistry:定义对BeanDefinition的各种增删改操作
  * FactoryBeanRegistrySupport：在DefaultSingletonBeanRegistry基础上增加了对FactoryBean的特殊处理功能
  * ConfigurableBeanFactory：提供配置 Factory 的各种方法
  * ListableBeanFactory：根据各种条件获取 bean 的配置清单
  * AbstractBeanFactory：综合 FactoryBeanRegistrySupport 和 ConfigurableBeanFactory 的功能。
  * AutowireCapableBeanFactory：提供创建 bean、自动注入、初始化以及应用 bean 的后处理器
  * AbstractAutowireCapableBeanFactory：综合 AbstractBeanFactory 并对接口 Autowire CapableBeanFactory进行实现
  * ConfigurableListableBeanFactory：BeanFactory 配置清单，指定忽略类型及接口等。
  
### 1.2 XmlBeanDefinitionReader

   **各个类的功能** <br>
  * ResourceLoader：定义资源加载器，主要应用于根据给定的资源文件地址返回对应的Resource。
  * BeanDefinitionReader：主要定义资源文件读取并转换为 BeanDefinition 的各个功能。
  * EnvironmentCapable：定义获取 Environment 方法
  * DocumentLoader：定义从资源文件加载到转换为 Document 的功能
  * AbstractBeanDefinitionReader：对 EnvironmentCapable、BeanDefinitionReader 类定义的功能进行实现。
  * BeanDefinitionDocumentReader：定义读取 Docuemnt 并注册 BeanDefinition 功能。
  * BeanDefinitionParserDelegate：定义解析 Element 的各种方法。
  
  ![]( https://github.com/yehuali/springSource/raw/master/note/images/xmlbeanDefinitionReader.jpg) <br>
  **如图所示，在XmlBeanDefinitionReader中主要包含以下几步的处理**<br>
 （1）通过继承自AbstractBeanDefinitionReader中的方法，来使用ResourceLoader将资源文件路径转换为对应的Resource文件<br>
 （2）通过DocumentLoader对Resource文件进行转换，将Resource文件转换为Document文件<br>
 （3）通过实现接口BeanDefinitionDocuemntReader的DefaultBeanDefinitionDocumentReader类对Document进行解析，并使用BeanDefinitionParserDelegate对Element进行解析<br>
 
## 2.容器的基础XmlBeanFactory
 XmlBeanFactory初始化时序图<br>
![]( https://github.com/yehuali/springSource/raw/master/note/images/XmlBeanFactory初始化时序图.jpg) <br>
 ###2.1配置文件封装
   在Java中，将不同来源的资源抽象成URL，通过注册不同的handler来处理不同来源的资源的读取逻辑 
   有了Resource接口便可以对所有资源文件进行统一处理
   
  ``` java 
  public  XmlBeanFactory(Resource  resource,  BeanFactory  parentBeanFactory) throws BeansException {
      super(parentBeanFactory);
      this.reader.loadBeanDefinitions(resource);
  }
  ```
  this.reader.loadBeanDefinitions(resource) 资源加载的真正实现，在实现之前调用super(parentBeanFactory)，跟踪代码到父类AbstractAutowireCapableBeanFactory的构造函数中：<br>
  ```
  public AbstractAutowireCapableBeanFactory() {
      super();
      ignoreDependencyInterface(BeanNameAware.class);
      ignoreDependencyInterface(BeanFactoryAware.class);
      ignoreDependencyInterface(BeanClassLoaderAware.class);
  }
  ```
  ignoreDependencyInterface的主要功能：忽略给定接口的自动装配功能
  
 ### 2.2 加载Bean
 ![]( https://github.com/yehuali/springSource/raw/master/note/images/loadBeanDefinition执行时序图.jpg) <br>
 （1）封装资源文件。对参数Resource使用EncodedResource类进行封装<br>
 （2）获取输入流。从Resource中获取对应的InputStream并构造InputSource<br>
 （3）通过构造InputSource实例和Resource实例继续调用函数doLoadBeanDefinitions<br>
 
 EncodedResource的作用。其中主要逻辑体现在getReader（）方法中，当设置了编码属性，Spring会使用相应的编码作为输入流的编码<br>
 ```
 public Reader getReader() throws IOException {
     if (this.encoding != null) {
        return new InputStreamReader(this.resource.getInputStream(), this.encoding);
     }else {
        return new InputStreamReader(this.resource.getInputStream());
     }
 }
 ```
 当构造好encodeResource对象后，再次转入了可复用方法loadBeanDefinitions（new EncodedResource(resource)）<br>
 ```
 public int loadBeanDefinitions(EncodedResource encodedResource) throws BeanDefinitionStoreException {
    .......
    //通过属性来记录已经加载的资源
    Set<EncodedResource> currentResources = this.resourcesCurrentlyBeingLoaded.get();
    ....
    //InputSource 这个类并不来自于 Spring，它的全路径是 org.xml.sax.InputSource
    InputSource inputSource = new InputSource(inputStream);
    if (encodedResource.getEncoding() != null) {
     inputSource.setEncoding(encodedResource.getEncoding());
    }
    //真正进入了逻辑核心部分
    return doLoadBeanDefinitions(inputSource, encodedResource.getResource());
   ....
 }
 ```
 (1)获取对XML文件的验证模式<br>
 (2)加载XML文件，并得到对应的Document<br>
 (3)根据返回的Document注册Bean信息<br>
 
 ## 3.获取XML的验证模式
  ### 3.1 验证模式的读取
  ```
    protected int getValidationModeForResource(Resource resource) {
        int validationModeToUse = getValidationMode();
        //如果手动指定了验证模式则使用指定的验证模式
        if (validationModeToUse != VALIDATION_AUTO) {
        return validationModeToUse;
        }
        //如果未指定则使用自动检测
        int detectedMode = detectValidationMode(resource);
        if (detectedMode != VALIDATION_AUTO) {
        return detectedMode;
        }
        return VALIDATION_XSD;
    }
  ```
  Spring检测验证模式的办法：判断是否包含DOCTYPE,如果包含就是DTD，否则就是XSD
  
  ### 3.2 获取Document
   ```
  public Document loadDocument(InputSource inputSource, EntityResolver entityResolver,ErrorHandler errorHandler, int validationMode, boolean namespaceAware) throws Exception {
      DocumentBuilderFactory factory = createDocumentBuilderFactory(validationMode,
      namespaceAware);
      if (logger.isDebugEnabled()) {
      logger.debug("Using JAXP provider [" + factory.getClass().getName() + "]");
      }
      DocumentBuilder builder = createDocumentBuilder(factory, entityResolver,
      errorHandler);
      return builder.parse(inputSource);
  }
   ```
  SAX解析XML文档的套路:<br>
   （1）创建DocumentBuilderFactory<br>
   （2）通过DocumentBuilderFactory创建DocumentBuilder<br>
   （3）解析inputSource来返回Document对象<br>
   
  #### 3.2.1 EntityResolver用法
  如果SAX应用程序需要实现自定义处理外部实体，则必须实现此接口并使用setEntityResolver方法向SAX驱动器注册一个实例<br>
  对于解析XML，SAX首先读取DTD定义。默认的寻找规则，即通过网络下载相应的DTD声明进行认证<br>
  * EntityResovler可以提供寻找DTD声明的方法
    1.entityResolver接口：InputSource resolveEntity(String publicId, String systemId)
    2.Spring中使用DelegatingEntityResolver为entityResolver的实现类，实现方法。<br>
    
   ```
        public  InputSource  resolveEntity(String  publicId,  String  systemId)  throws SAXException, IOException {
                     if (systemId != null) {
                         if (systemId.endsWith(DTD_SUFFIX)) {
                             //如果是 dtd 从这里解析
                             return this.dtdResolver.resolveEntity(publicId, systemId);
                         }else if (systemId.endsWith(XSD_SUFFIX)) {
                             //通过调用 META-INF/Spring.schemas 解析
                             return this.schemaResolver.resolveEntity(publicId, systemId);
                         }
                     }
                     return null;
         }
   ```
   
  #### 解析及注册BeanDefinitions
   ```
    public int registerBeanDefinitions(Document doc, Resource resource) throws BeanDefinitionStore Exception {
        //使用 DefaultBeanDefinitionDocumentReader 实例化 BeanDefinitionDocumentReader
        BeanDefinitionDocumentReader documentReader = createBeanDefinitionDocumentReader();
        //将环境变量设置其中
        documentReader.setEnvironment(this.getEnvironment());
        //在实例化 BeanDefinitionReader 时候会将 BeanDefinitionRegistry 传入，默认使用继承自
        DefaultListableBeanFactory 的子类
        //记录统计前 BeanDefinition 的加载个数
        int countBefore = getRegistry().getBeanDefinitionCount();
        //加载及注册 bean
        documentReader.registerBeanDefinitions(doc, createReaderContext(resource));
        //记录本次加载的 BeanDefinition 个数
        return getRegistry().getBeanDefinitionCount() - countBefore;
    }
   ```
   loadDocument应用了面向对象找那个单一职责的原则
   ```
       protected void doRegisterBeanDefinitions(Element root) {
           //处理 profile 属性
           String profileSpec = root.getAttribute(PROFILE_ _ATTRIBUTE);
           if (StringUtils.hasText(profileSpec)) {
               Assert.state(this.environment != null, "environment property must not be null");
               String[] specifiedProfiles = StringUtils.tokenizeToStringArray(profileSpec,
               BeanDefinitionParserDelegate.MULTI_VALUE_ATTRIBUTE_DELIMITERS);
               if (!this.environment.acceptsProfiles(specifiedProfiles)) {
                 return;
               }
           }
           //专门处理解析
           BeanDefinitionParserDelegate parent = this.delegate;
           this.delegate = createHelper(readerContext, root, parent);
           //解析前处理，留给子类实现
           preProcessXml(root);
           parseBeanDefinitions(root, this.delegate);
           //解析后处理，留给子类实现
           postProcessXml(root);
           this.delegate = parent;
       }
   ```
   一个类要么面向继承设计，要么就用final修饰。preProcessXml(root)或者postProcessXml(root)是空的，利用模板方法模式为子类设计<br>
   （1）profile的用法
   ```
   <beans>
       <beans profile="dev">
      ... ...
      </beans>
      <beans profile="production">
      ... ...
      </beans>
   </beans>
   
   在web.xml中
   <context-param>
       <param-name>Spring.profiles.active</param-name>
       <param-value>dev</param-value>
   </context-param>
   ```
   程序首先获取beans节点是否定义了profile属性，如果定义了则会到环境变量中寻找--->断言environment不可能为空<br>
   
   ```
   protected void parseBeanDefinitions(Element root, BeanDefinitionParserDelegate delegate) {
       //对 beans 的处理 
       if (delegate.isDefaultNamespace(root)) {
           NodeList nl = root.getChildNodes();
           for (int i = 0; i < nl.getLength(); i++) {
               Node node = nl.item(i);
               if (node instanceof Element) {
                   Element ele = (Element) node;
                   if (delegate.isDefaultNamespace(ele)) {
                       //对 bean 的处理
                       parseDefaultElement(ele, delegate);
                   }else {
                         //对 bean 的处理
                        delegate.parseCustomElement(ele);
                    }
                }
            }
       }else {
        delegate.parseCustomElement(root);
       }
   }
   ```
   Spring的XML配置有两大类Bean声明,一个是默认的，一个是自定义的<br>
   判断是否默认命名空间使用node.getNamespaceURI()获取命名空间，并与Spring中固定的命名空间 http://www.Springframework.org/schema/beans 进行比对<br>
    
   
    
    
 
 
  
    
  

  
  
