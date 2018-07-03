```
MyTestBean bean=(MyTestBean) bf.getBean("myTestBean")
```
```
public Object getBean(String name) throws BeansException {
        return this.doGetBean(name, (Class)null, (Object[])null, false);
    }
```
```
protected <T> T doGetBean(String name, @Nullable Class<T> requiredType, @Nullable Object[] args, boolean typeCheckOnly) throws BeansException {
        //提取对应的beanName
        String beanName = this.transformedBeanName(name);
        /**
          *检查缓存中或者实例工厂中是否有对应的实例
          *在创建单例bean的时候会存在依赖注入的情况，而在创建依赖的时候为了避免循环依赖
          *Spring创建bean的原则是不等bean创建完成就会创建bean的ObjectFactory提早曝光
          *也就是将objectFactory加入到缓存中，一旦下个bean创建时候需要依赖上个bean则直接使用ObjectFactory
          */
        //直接尝试从缓存获取或者singletonFactories中的ObjectFactory中获取
        Object sharedInstance = this.getSingleton(beanName);
        Object bean;
        if (sharedInstance != null && args == null) {
            if (this.logger.isDebugEnabled()) {
                if (this.isSingletonCurrentlyInCreation(beanName)) {
                    this.logger.debug("Returning eagerly cached instance of singleton bean '" + beanName + "' that is not fully initialized yet - a consequence of a circular reference");
                } else {
                    this.logger.debug("Returning cached instance of singleton bean '" + beanName + "'");
                }
            }
            //返回对应的实例，有时候存在诸如BeanFactory的情况并不是直接返回实例本身而是返回指定方法返回的实例
            bean = this.getObjectForBeanInstance(sharedInstance, name, beanName, (RootBeanDefinition)null);
        } else {
            //只有单例情况下才会尝试解决循环依赖，原型模式情况下，如果存在
            //A中有B的属性，B中有A的属性，那么当依赖注入的时候，就会产生当A还未创建完的时候因为对于B的创建再次返回创建A，造成循环依赖
            if (this.isPrototypeCurrentlyInCreation(beanName)) {
                throw new BeanCurrentlyInCreationException(beanName);
            }

            BeanFactory parentBeanFactory = this.getParentBeanFactory();
            //如果beanDefinitionMap中也就是在所有已经加载的类中不包括beanName则尝试从parentBeanFactory中检测
            if (parentBeanFactory != null && !this.containsBeanDefinition(beanName)) {
                String nameToLookup = this.originalBeanName(name);
                if (parentBeanFactory instanceof AbstractBeanFactory) {
                    return ((AbstractBeanFactory)parentBeanFactory).doGetBean(nameToLookup, requiredType, args, typeCheckOnly);
                }
                //递归到BeanFactory中寻找
                if (args != null) {
                    return parentBeanFactory.getBean(nameToLookup, args);
                }

                return parentBeanFactory.getBean(nameToLookup, requiredType);
            }
            //如果不是仅仅做类型检查则是创建bean，这里要进行记录
            if (!typeCheckOnly) {
                this.markBeanAsCreated(beanName);
            }

            try {
                //将存储XML配置文件的GernericBeanDefinition转换为RootBeanDefinition，
                //如果指定beanName是子bean的话同时会合并父类的相关属性
                RootBeanDefinition mbd = this.getMergedLocalBeanDefinition(beanName);
                this.checkMergedBeanDefinition(mbd, beanName, args);
                String[] dependsOn = mbd.getDependsOn();
                String[] var11;
                //若存在依赖则需要递归实例化依赖的bean
                if (dependsOn != null) {
                    var11 = dependsOn;
                    int var12 = dependsOn.length;

                    for(int var13 = 0; var13 < var12; ++var13) {
                        String dep = var11[var13];
                        if (this.isDependent(beanName, dep)) {
                            throw new BeanCreationException(mbd.getResourceDescription(), beanName, "Circular depends-on relationship between '" + beanName + "' and '" + dep + "'");
                        }
                        //缓存依赖调用
                        this.registerDependentBean(dep, beanName);

                        try {
                            this.getBean(dep);
                        } catch (NoSuchBeanDefinitionException var24) {
                            throw new BeanCreationException(mbd.getResourceDescription(), beanName, "'" + beanName + "' depends on missing bean '" + dep + "'", var24);
                        }
                    }
                }
                //实例化依赖的bean后便可以实例化mbd本身了
                //singleton模式的创建
                if (mbd.isSingleton()) {
                    sharedInstance = this.getSingleton(beanName, () -> {
                        try {
                            return this.createBean(beanName, mbd, args);
                        } catch (BeansException var5) {
                            this.destroySingleton(beanName);
                            throw var5;
                        }
                    });
                    bean = this.getObjectForBeanInstance(sharedInstance, name, beanName, mbd);
                } else if (mbd.isPrototype()) {
                    //prototype模式的创建(new)
                    var11 = null;

                    Object prototypeInstance;
                    try {
                        this.beforePrototypeCreation(beanName);
                        prototypeInstance = this.createBean(beanName, mbd, args);
                    } finally {
                        this.afterPrototypeCreation(beanName);
                    }

                    bean = this.getObjectForBeanInstance(prototypeInstance, name, beanName, mbd);
                } else {
                    //指定的scope上实例化bean
                    String scopeName = mbd.getScope();
                    Scope scope = (Scope)this.scopes.get(scopeName);
                    if (scope == null) {
                        throw new IllegalStateException("No Scope registered for scope name '" + scopeName + "'");
                    }

                    try {
                        Object scopedInstance = scope.get(beanName, () -> {
                            this.beforePrototypeCreation(beanName);

                            Object var4;
                            try {
                                var4 = this.createBean(beanName, mbd, args);
                            } finally {
                                this.afterPrototypeCreation(beanName);
                            }

                            return var4;
                        });
                        bean = this.getObjectForBeanInstance(scopedInstance, name, beanName, mbd);
                    } catch (IllegalStateException var23) {
                        throw new BeanCreationException(beanName, "Scope '" + scopeName + "' is not active for the current thread; consider defining a scoped proxy for this bean if you intend to refer to it from a singleton", var23);
                    }
                }
            } catch (BeansException var26) {
                this.cleanupAfterBeanCreationFailure(beanName);
                throw var26;
            }
        }
        //检查需要的类型是否符合bean的实际类型
        if (requiredType != null && !requiredType.isInstance(bean)) {
            try {
                T convertedBean = this.getTypeConverter().convertIfNecessary(bean, requiredType);
                if (convertedBean == null) {
                    throw new BeanNotOfRequiredTypeException(name, requiredType, bean.getClass());
                } else {
                    return convertedBean;
                }
            } catch (TypeMismatchException var25) {
                if (this.logger.isDebugEnabled()) {
                    this.logger.debug("Failed to convert bean '" + name + "' to required type '" + ClassUtils.getQualifiedName(requiredType) + "'", var25);
                }

                throw new BeanNotOfRequiredTypeException(name, requiredType, bean.getClass());
            }
        } else {
            return bean;
        }
    }
```
（1）转换对应beanName <br>
  &emsp;&emsp;传入的参数可能是别名，也可能是FactoryBean，所以需要进行一系列解析<br>
  * 去除FactoryBean的修饰符，如果name="&aa" ，那么会首先去除&而使name="aa"
  * 取指定alias所表示的最终beanName，例如别名A指向名称B的bean则返回B <br>
（2）尝试从缓存中加载单例<br>
   单例在Spring的同一个容器内只会被创建一次，后续再获取bean，就直接从单例缓存中获取了<br>
   如果加载不成功则再次尝试从singletonFactories中加载<br>
（3）bean的实例化<br>
  如果从缓存中得到了bean的原始状态，则需要对bean进行实例化（缓存中记录只是最原始的bean状态）,getObjectForBeanInstance就是完成工作的<br>
（4）原型模式的依赖检查<br>
   只有在单例情况下才会尝试解决循环依赖，也就是情况isPrototypeCurrentlyCreation(beanName)判断true<br>
（5）检测parentBeanFactory<br>
    parentBeanFactory != null && !this.containsBeanDefinition(beanName) <br>
    !this.containsBeanDefinition(beanName) 检测如果当前加载的XML配置文件中不包含beanName所对应的配置，就只能到parentBeanFactory去尝试下，再去递归的调用getBean方法<br>
（6）将存储XML配置文件的GernericBeanDefinition转换为RootBeanDefinition<br>
    因为从XML配置文件中读取到的Bean信息是存储在GernericBeanDefinition中的，但是所有的Bean后续处理都是针对于RootBeanDefinition的<br>
    如果父类bean不为空的话，则会一并合并父类的属性<br>
（7）寻找依赖<br>
**（8）针对不同的scope进行bean的创建<br>** 
     Spring会根据不同的配置进行不同的初始化策略<br>
（9）类型转换<br>
     通常对该方法的调用参数requiredType是为空的<br>
     但存在返回的bean其实是个String,但是requiredType却传入Integer类型，功能是将返回的bean转换为requiredType所指定的类型<br>
     在Spring中提供了各种各样的转换器，用户可以扩展转换器来满足需求<br>    
![]( https://github.com/yehuali/springSource/raw/master/note/images/bean的获取过程.jpg)

### 1.FactoryBean的使用
   一般情况下，Spring通过反射机制利用bean的class属性指定实现类来实例化bean <br>
   但某些情况下，实例化bean过程比较复杂，按照传统方式，需要在<bean>中提供大量的配置信息，这时采用编码的方式得到一个简单的方案<br>
   Spring为此提供了FactoryBean的工厂类接口，用户可以通过实现该接口定制实例化bean的逻辑<br>
   Spring自身提供了70多个FactoryBean的实现，从spring3.0开始，FactoryBean开始支持泛型
```
public interface FactoryBean<T> {
    @Nullable
    T getObject() throws Exception;
    @Nullable
    Class<?> getObjectType();
    default boolean isSingleton() {
        return true;
    }
}
```  
当配置文件中<bean>的class属性配置的实现类是FactoryBean时，通过getBean（）方法返回的不是FactoryBean本身，而是FactoryBean#getObject()方法返回的对象,相当于FactoryBean#getObject()代理了getObject()<br>



  
   
   
   
   
   
   
   
   
   
   
   
   
   
   
   
   
   
   
   
   
   
   
   
   