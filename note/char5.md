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

### 2.缓存中获取单例bean
单例bean存在依赖注入的情况，Spring创建bean的原则是不等bean创建完成就会创建bean的ObjectFactory提早曝光加入到缓存中<br>
下一个bean创建时需要依赖上个bean，则直接使用ObjectFactory <br>
```
//参数allowEarlyReference 为true设置标识允许早期依赖
protected Object getSingleton(String beanName, boolean allowEarlyReference) {
        //检查缓存中是否存在实例
		Object singletonObject = this.singletonObjects.get(beanName);
		if (singletonObject == null && isSingletonCurrentlyInCreation(beanName)) {
		    //如果为空，则锁定全局变量并进行处理
			synchronized (this.singletonObjects) {
			    //如果此bean正在加载则不处理
				singletonObject = this.earlySingletonObjects.get(beanName);
				if (singletonObject == null && allowEarlyReference) {
				    //当某些方法需要提前初始化的时候则会调用addSingletonFactory方法将对应的ObjectFactory初始化策略存储在singletonFactories
					ObjectFactory<?> singletonFactory = this.singletonFactories.get(beanName);
					if (singletonFactory != null) {
					    //调用预先设定的getObject方法
						singletonObject = singletonFactory.getObject();
						//记录在缓存中，earlySingletonObjects和singletonFactories互斥
						this.earlySingletonObjects.put(beanName, singletonObject);
						this.singletonFactories.remove(beanName);
					}
				}
			}
		}
		return singletonObject;
	}
```
* singletonObjects:用于保存BeanName和创建bean实例之间的关系，bean name --> bean instance 
* singletonFactories:用于保存BeanName和创建bean的工厂之间的关系,bean name --> ObjectFactory
* earlySingletonObjects:保存BeanName和创建bean实例之间的关系，与singletonObjects的不同之处在于，当一个单例bean被放到这里面后，那么当bean还在创建过程中，就可以直接通过getBean方法获取，目的是用来检测循环引用<br>
* registeredSingletons:用来保存当前所有已注册的bean

### 3. 从bean的实例中获取对象
  getObjectForBeanInstance无论是从缓存中获得bean还是根据不同的scope策略加载bean，都用此方法检测当前bean是否是FactoryBean类型的bean，如果是则调用该bean对应的FactoryBean实例中的getObject（）作为返回值<br>
```
protected Object getObjectForBeanInstance(
			Object beanInstance, String name, String beanName, @Nullable RootBeanDefinition mbd) {

		// Don't let calling code try to dereference the factory if the bean isn't a factory.
		if (BeanFactoryUtils.isFactoryDereference(name)) {
			if (beanInstance instanceof NullBean) {
				return beanInstance;
			}
			//如果指定的name是工厂相关(以&为前缀)且beanInstance又不是FactoryBean类型则验证不通过
			if (!(beanInstance instanceof FactoryBean)) {
				throw new BeanIsNotAFactoryException(transformedBeanName(name), beanInstance.getClass());
			}
		}

		// Now we have the bean instance, which may be a normal bean or a FactoryBean.
		// If it's a FactoryBean, we use it to create a bean instance, unless the
		// caller actually wants a reference to the factory.
		if (!(beanInstance instanceof FactoryBean) || BeanFactoryUtils.isFactoryDereference(name)) {
			return beanInstance;
		}
        
        //加载FactoryBean
		Object object = null;
		if (mbd == null) {
		    //尝试从缓存中加载bean
			object = getCachedObjectForFactoryBean(beanName);
		}
		if (object == null) {
			// Return bean instance from factory.
			//到这里已经明确知道beanInstance一定是FactoryBean类型
			FactoryBean<?> factory = (FactoryBean<?>) beanInstance;
			// Caches object obtained from FactoryBean if it is a singleton.
			//containsBeanDefinition 检测beanDefinitionMap中也就是在所有已经加载的类中检测是否定义beanName
			if (mbd == null && containsBeanDefinition(beanName)) {
			    //将存储XML配置文件的GernericBeanDefinition转换为RootBeanDefinition，如果指定BeanName是子Bean的话同时合并父类的相关属性
				mbd = getMergedLocalBeanDefinition(beanName);
			}
			//是否是用户定义的而不是应用程序本身定义的
			boolean synthetic = (mbd != null && mbd.isSynthetic());
			object = getObjectFromFactoryBean(factory, beanName, !synthetic);
		}
		return object;
	}
``` 
上面代码多数是辅助代码及一些功能性的判断，核心代码委托给getObjectFromFactoryBean<br>
(1)对FactoryBean正确性的验证<br>
(2)对非FactoryBean不做任何处理<br>
(3)对bean进行转换<br>
(4)将从Factory中解析bean的工作委托给getObjectFromFactoryBean<br>
```
protected Object getObjectFromFactoryBean(FactoryBean factory, String beanName, boolean shouldPostProcess) {
    //如果是单例模式
    if (factory.isSingleton() && containsSingleton(beanName)) {
        synchronized (getSingletonMutex()) {
            Object object = this.factoryBeanObjectCache.get(beanName);
             if (object == null) {
                object  =  doGetObjectFromFactoryBean(factory,  beanName,shouldPostProcess);
                 this.factoryBeanObjectCache.put(beanName, (object != null ? object :NULL_OBJECT));
              }
            return (object != NULL_OBJECT ? object : null);
        }
    }else {
        return doGetObjectFromFactoryBean(factory, beanName, shouldPostProcess);
    }
}
```
**此处在spring5.0.7版本有改动** <br>
```
private Object doGetObjectFromFactoryBean(final FactoryBean<?> factory, final String beanName) throws BeanCreationException {
		Object object;
		try {
		    //需要权限验证
			if (System.getSecurityManager() != null) {
				AccessControlContext acc = getAccessControlContext();
				try {
					object = AccessController.doPrivileged((PrivilegedExceptionAction<Object>) factory::getObject, acc);
				}
				catch (PrivilegedActionException pae) {
					throw pae.getException();
				}
			}
			else {
			    //直接调用getObject方法
				object = factory.getObject();
			}
		}
		catch (FactoryBeanNotInitializedException ex) {
			throw new BeanCurrentlyInCreationException(beanName, ex.toString());
		}
		catch (Throwable ex) {
			throw new BeanCreationException(beanName, "FactoryBean threw exception on object creation", ex);
		}

		// Do not accept a null value for a FactoryBean that's not fully
		// initialized yet: Many FactoryBeans just return null then.
		if (object == null) {
			if (isSingletonCurrentlyInCreation(beanName)) {
				throw new BeanCurrentlyInCreationException(
						beanName, "FactoryBean which is currently in creation returned null from getObject");
			}
			object = new NullBean();
		}
		return object;
	}
```  
spring5.0.7 将object = postProcessObjectFromFactoryBean(object, beanName) 调用ObjectFactory的后处理器提前至getObjectFromFactoryBean方法中
```
protected Object postProcessObjectFromFactoryBean(Object object, String beanName) {
		return applyBeanPostProcessorsAfterInitialization(object, beanName);
	}
```
```
public Object applyBeanPostProcessorsAfterInitialization(Object existingBean, String beanName)
        throws BeansException {

    Object result = existingBean;
    for (BeanPostProcessor beanProcessor : getBeanPostProcessors()) {
        Object current = beanProcessor.postProcessAfterInitialization(result, beanName);
        if (current == null) {
            return result;
        }
        result = current;
    }
    return result;
}
```  
尽可能保证所有bean初始化都会调用注册的BeanPostProcessor的postProcessAfterInitialization方法进行处理，在实际开发过程中针对此特性设计自己的业务逻辑<br>

### 4.获取单例
Spring使用getSingleton的重载方法实现bean的加载过程<br>
```
public Object getSingleton(String beanName, ObjectFactory<?> singletonFactory) {
		Assert.notNull(beanName, "Bean name must not be null");
		//全局变量需要同步
		synchronized (this.singletonObjects) {
		     //首先检查对应的bean是否已经加载过，因为singleton模式其实就是复用以创建的bean
			Object singletonObject = this.singletonObjects.get(beanName);
			//如果为空才可以进行singleton的bean的初始化
			if (singletonObject == null) {
				if (this.singletonsCurrentlyInDestruction) {
					throw new BeanCreationNotAllowedException(beanName,
							"Singleton bean creation not allowed while singletons of this factory are in destruction " +
							"(Do not request a bean from a BeanFactory in a destroy method implementation!)");
				}
				if (logger.isDebugEnabled()) {
					logger.debug("Creating shared instance of singleton bean '" + beanName + "'");
				}
				beforeSingletonCreation(beanName);
				boolean newSingleton = false;
				boolean recordSuppressedExceptions = (this.suppressedExceptions == null);
				if (recordSuppressedExceptions) {
					this.suppressedExceptions = new LinkedHashSet<>();
				}
				try {
				    //初始化bean
					singletonObject = singletonFactory.getObject();
					newSingleton = true;
				}
				catch (IllegalStateException ex) {
					// Has the singleton object implicitly appeared in the meantime ->
					// if yes, proceed with it since the exception indicates that state.
					singletonObject = this.singletonObjects.get(beanName);
					if (singletonObject == null) {
						throw ex;
					}
				}
				catch (BeanCreationException ex) {
					if (recordSuppressedExceptions) {
						for (Exception suppressedException : this.suppressedExceptions) {
							ex.addRelatedCause(suppressedException);
						}
					}
					throw ex;
				}
				finally {
					if (recordSuppressedExceptions) {
						this.suppressedExceptions = null;
					}
					afterSingletonCreation(beanName);
				}
				if (newSingleton) {
				    //加入缓存
					addSingleton(beanName, singletonObject);
				}
			}
			return singletonObject;
		}
	}
```   
```
protected void beforeSingletonCreation(String beanName) {
        //this.singletonsCurrentlyInCreation.add(beanName)将当前正要创建的bean记录在缓存中，便可以对循环依赖进行检测
		if (!this.inCreationCheckExclusions.contains(beanName) && !this.singletonsCurrentlyInCreation.add(beanName)) {
			throw new BeanCurrentlyInCreationException(beanName);
		}
	}
```
```
protected void afterSingletonCreation(String beanName) {
        //当加载结束后需要移除缓存中对该bean的正在加载状态的记录
		if (!this.inCreationCheckExclusions.contains(beanName) && !this.singletonsCurrentlyInCreation.remove(beanName)) {
			throw new IllegalStateException("Singleton '" + beanName + "' isn't currently in creation");
		}
	}
```
```
将结果记录至缓存并删除加载bean过程中所记录的各种辅助状态
protected void addSingleton(String beanName, Object singletonObject) {
		synchronized (this.singletonObjects) {
			this.singletonObjects.put(beanName, singletonObject);
			this.singletonFactories.remove(beanName);
			this.earlySingletonObjects.remove(beanName);
			this.registeredSingletons.add(beanName);
		}
	}
```
上述代码使用了回调方法，使得程序可以在单例创建的前后做一些准备及处理操作，真正获取单例bean实现逻辑在ObjectFactory类型的实例singletonFactory中实现的<br>
```
 sharedInstance = this.getSingleton(beanName, () -> {
        try {
            return this.createBean(beanName, mbd, args);
        } catch (BeansException var5) {
            this.destroySingleton(beanName);
            throw var5;
        }
});
``` 
### 5.准备创建bean
```
protected Object createBean(String beanName, RootBeanDefinition mbd, @Nullable Object[] args)
			throws BeanCreationException {

		if (logger.isDebugEnabled()) {
			logger.debug("Creating instance of bean '" + beanName + "'");
		}
		RootBeanDefinition mbdToUse = mbd;

		// Make sure bean class is actually resolved at this point, and
		// clone the bean definition in case of a dynamically resolved Class
		// which cannot be stored in the shared merged bean definition.
		//锁定class，根据设置的class属性或者根据className来解析Class
		Class<?> resolvedClass = resolveBeanClass(mbd, beanName);
		if (resolvedClass != null && !mbd.hasBeanClass() && mbd.getBeanClassName() != null) {
			mbdToUse = new RootBeanDefinition(mbd);
			mbdToUse.setBeanClass(resolvedClass);
		}

		// Prepare method overrides.
		try {
		    //验证及准备覆盖的方法
			mbdToUse.prepareMethodOverrides();
		}
		catch (BeanDefinitionValidationException ex) {
			throw new BeanDefinitionStoreException(mbdToUse.getResourceDescription(),
					beanName, "Validation of method overrides failed", ex);
		}

		try {
			// Give BeanPostProcessors a chance to return a proxy instead of the target bean instance.
			//给BeanPostProcessors一个机会来返回代理来替代真正的实例
			Object bean = resolveBeforeInstantiation(beanName, mbdToUse);
			if (bean != null) {
				return bean;
			}
		}
		catch (Throwable ex) {
			throw new BeanCreationException(mbdToUse.getResourceDescription(), beanName,
					"BeanPostProcessor before instantiation of bean failed", ex);
		}

		try {
			Object beanInstance = doCreateBean(beanName, mbdToUse, args);
			if (logger.isDebugEnabled()) {
				logger.debug("Finished creating instance of bean '" + beanName + "'");
			}
			return beanInstance;
		}
		catch (BeanCreationException | ImplicitlyAppearedSingletonException ex) {
			// A previously detected exception with proper bean creation context already,
			// or illegal singleton state to be communicated up to DefaultSingletonBeanRegistry.
			throw ex;
		}
		catch (Throwable ex) {
			throw new BeanCreationException(
					mbdToUse.getResourceDescription(), beanName, "Unexpected exception during bean creation", ex);
		}
	}
```
总结出函数完成的具体步骤及功能:<br>
（1）根据设置的class属性或者根据className来解析class。<br>
（2）对override属性进行标记及验证<br>
 --->Spring配置中存在lookup-method和replace-method，这两个配置的加载将统一存放在BeanDefinition中的methodOverrides属性里<br>
（3）应用初始化前的后处理器，解析指定bean是否存在初始化前的短路操作<br>
（4）创建bean<br>
#### 5.1 处理override属性
```
public void prepareMethodOverrides() throws BeanDefinitionValidationException {
		// Check that lookup methods exists.
		if (hasMethodOverrides()) {
			Set<MethodOverride> overrides = getMethodOverrides().getOverrides();
			synchronized (overrides) {
				for (MethodOverride mo : overrides) {
					prepareMethodOverride(mo);
				}
			}
		}
	}
``` 
```
	protected void prepareMethodOverride(MethodOverride mo) throws BeanDefinitionValidationException {
		//获取对应类中对应方法名的个数
		int count = ClassUtils.getMethodCountForName(getBeanClass(), mo.getMethodName());
		if (count == 0) {
			throw new BeanDefinitionValidationException(
					"Invalid method override: no method with name '" + mo.getMethodName() +
					"' on class [" + getBeanClassName() + "]");
		}
		else if (count == 1) {
			// Mark override as not overloaded, to avoid the overhead of arg type checking.
			mo.setOverloaded(false);
		}
	}
```
实现原理：在bean实例化的时候如果检测到存在methodOverrides属性，会动态地为当前bean生成代理并使用对应的拦截器为bean做增强处理<br>
对于方法匹配，如果一个类存在若干个重载方法，那么在函数调用及增强的时候还需根据参数类型进行匹配，来最终确定当前调用的到底是哪个函数<br>
如果当前类中的方法只有一个，那么将设置重载该方法没有被重载，不需要进行方法的参数匹配验证，还可以提前对方法存在性进行验证<br>
#### 5.2 实例化的前置处理
```
Object bean = resolveBeforeInstantiation(beanName, mbdToUse);
			if (bean != null) {
				return bean;
			}
```
当经过前置处理后返回的结果如果不为空，那么会直接略过后续的Bean的创建而直接返回结果，AOP功能就是基于这里的判断<br>
```
protected Object resolveBeforeInstantiation(String beanName, RootBeanDefinition mbd) {
		Object bean = null;
		//如果尚未被解析
		if (!Boolean.FALSE.equals(mbd.beforeInstantiationResolved)) {
			// Make sure bean class is actually resolved at this point.
			if (!mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {
				Class<?> targetType = determineTargetType(beanName, mbd);
				if (targetType != null) {
					bean = applyBeanPostProcessorsBeforeInstantiation(targetType, beanName);
					if (bean != null) {
						bean = applyBeanPostProcessorsAfterInitialization(bean, beanName);
					}
				}
			}
			mbd.beforeInstantiationResolved = (bean != null);
		}
		return bean;
	}
```  
 applyBeanPostProcessorsBeforeInstantiation 和 applyBeanPostProcessorsAfterInitialization<br>
 无非是对后处理器中的所有InstantiationAwareBeanPostProcessor类型的后处理器进行postProcessBeforeInstantiation方法<br>
 BeanPostProcessor进行postProcessAfterInitialization方法<br>

### 6 循环依赖
 **1.构造器循环依赖**<br>
  通过构造器注入构成的循环依赖，此依赖是无法解决的，只能抛出BeanCurrentlyInCreationException异常表示循环依赖<br>
  Spring容器将每一个正在创建的bean标识符放在一个"当前创建bean池"中，bean标识符在创建过程中将一直保持在这个池中，如果在创建bean过程中发现自己已经在"当前创建bean池"里时，将抛出BeanCurrentlyInCreationException异常表示循环依赖；而对于创建完毕的bean将从"当前创建bean池"中清除掉<br>
  **2.setter循环依赖**<br>
  对于setter注入造成的依赖是通过Spring容器提前暴露刚完成构造器注入但未完成其他步骤（如setter注入）的bean来完成的，而且只能解决单例作用域的bean循环依赖<br>
  通过提前暴露一个单例工厂方法，从而使其他bean能引用到该bean<br>
  具体步骤：<br>
  （1）Spring容器创建单例"testA"bean，首先根据无参构造器创建bean，并暴露一个"ObjectFactory"用于返回一个提前暴露一个创建中的bean，并将"testA"标识符放到"当前创建bean池",然后进行setter注入"testB"<br>
  （2）testC进行setter注入"testA"，进行注入"testA"时由于提前暴露了"ObjectFactory"工厂，从而使用它返回提前暴露一个创建中的bean<br>
  **3.prototype范围的依赖处理**<br>
   对于"prototyppe"作用域bean，Spring容器无法完成依赖注入，因为Spring容器不进行缓存"prototype"作用域的bean，因此无法提前暴露一个创建中的bean<br>
   
### 7.创建bean
  当经历过resolveBeforeInstantiation方法后，程序有两个选择，如果创建了代理或者说重写了InstantiationAwareBeanPostProcessor的postProcessBeforeInstantiation方法并在postProcessBeforeInstantiation中改变了bean <br>
  否认需要在doCreateBean中完成常规bean的创建<br>  
```
protected Object doCreateBean(final String beanName, final RootBeanDefinition mbd, final @Nullable Object[] args)
			throws BeanCreationException {

		// Instantiate the bean.
		BeanWrapper instanceWrapper = null;
		if (mbd.isSingleton()) {
			instanceWrapper = this.factoryBeanInstanceCache.remove(beanName);
		}
		if (instanceWrapper == null) {
		    //根据指定bean使用对应的策略创建新的实例，如：工厂方法、构造函数自动注入、简单初始化
			instanceWrapper = createBeanInstance(beanName, mbd, args);
		}
		final Object bean = instanceWrapper.getWrappedInstance();
		Class<?> beanType = instanceWrapper.getWrappedClass();
		if (beanType != NullBean.class) {
			mbd.resolvedTargetType = beanType;
		}

		// Allow post-processors to modify the merged bean definition.
		synchronized (mbd.postProcessingLock) {
			if (!mbd.postProcessed) {
				try {
				    //应用MergedBeanDefinitionPostProcessor
					applyMergedBeanDefinitionPostProcessors(mbd, beanType, beanName);
				}
				catch (Throwable ex) {
					throw new BeanCreationException(mbd.getResourceDescription(), beanName,
							"Post-processing of merged bean definition failed", ex);
				}
				mbd.postProcessed = true;
			}
		}

		// Eagerly cache singletons to be able to resolve circular references
		// even when triggered by lifecycle interfaces like BeanFactoryAware.
		//是否需要提早曝光:单例&允许循环依赖&当前bean正在创建中，检测循环依赖
		boolean earlySingletonExposure = (mbd.isSingleton() && this.allowCircularReferences &&
				isSingletonCurrentlyInCreation(beanName));
		if (earlySingletonExposure) {
			if (logger.isDebugEnabled()) {
				logger.debug("Eagerly caching bean '" + beanName +
						"' to allow for resolving potential circular references");
			}
			/**
			  *为避免后期循环依赖，可以在bean初始化完成前将创建实例的ObjectFactory加入工厂
			  *对bean再一次依赖引用，主要应用SmartInstantiationAware BeanPostProcessor
			  *其中熟知的AOP就是在这里将advice动态织入bean中，如没有则直接返回bean，不做任何处理
			  */
			addSingletonFactory(beanName, () -> getEarlyBeanReference(beanName, mbd, bean));
		}

		// Initialize the bean instance.
		Object exposedObject = bean;
		try {
		    //对bean进行填充，将各个属性值注入，其中，可能存在依赖于其他bean的属性，则会递归初始依赖bean
			populateBean(beanName, mbd, instanceWrapper);
			//调用初始化犯法，比如init-method
			exposedObject = initializeBean(beanName, exposedObject, mbd);
		}
		catch (Throwable ex) {
			if (ex instanceof BeanCreationException && beanName.equals(((BeanCreationException) ex).getBeanName())) {
				throw (BeanCreationException) ex;
			}
			else {
				throw new BeanCreationException(
						mbd.getResourceDescription(), beanName, "Initialization of bean failed", ex);
			}
		}

		if (earlySingletonExposure) {
			Object earlySingletonReference = getSingleton(beanName, false);
			//earlySingeletonReference只有在检测到有循环依赖的情况才会不为空
			if (earlySingletonReference != null) {
			    //如果exposedObject没有在初始化方法中被改变，也就是没有被增强
				if (exposedObject == bean) {
					exposedObject = earlySingletonReference;
				}
				else if (!this.allowRawInjectionDespiteWrapping && hasDependentBean(beanName)) {
					String[] dependentBeans = getDependentBeans(beanName);
					Set<String> actualDependentBeans = new LinkedHashSet<>(dependentBeans.length);
					for (String dependentBean : dependentBeans) {
					    //检测依赖
						if (!removeSingletonIfCreatedForTypeCheckOnly(dependentBean)) {
							actualDependentBeans.add(dependentBean);
						}
					}
					/**
					  *因为bean创建后其所依赖的Bean一定是已经创建的
					  *actualDependentBeans不为空则表示当前bean创建后其依赖的bean却没有全部创建完，也就是说存在循环依赖
					  */
					if (!actualDependentBeans.isEmpty()) {
						throw new BeanCurrentlyInCreationException(beanName,
								"Bean with name '" + beanName + "' has been injected into other beans [" +
								StringUtils.collectionToCommaDelimitedString(actualDependentBeans) +
								"] in its raw version as part of a circular reference, but has eventually been " +
								"wrapped. This means that said other beans do not use the final version of the " +
								"bean. This is often the result of over-eager type matching - consider using " +
								"'getBeanNamesOfType' with the 'allowEagerInit' flag turned off, for example.");
					}
				}
			}
		}

		// Register bean as disposable.
		try {
		    //根据scopse注册bean
			registerDisposableBeanIfNecessary(beanName, bean, mbd);
		}
		catch (BeanDefinitionValidationException ex) {
			throw new BeanCreationException(
					mbd.getResourceDescription(), beanName, "Invalid destruction signature", ex);
		}

		return exposedObject;
	}
```
整个函数的概要思路:<br>  
（1）如果是单例则需要首先清楚缓存<br>
（2）实例化bean，将BeanDefinition转换为BeanWrapper<br>
* 如果存在工厂方法则使用工厂方法进行初始化
* 一个类有多个构造函数，每个构造函数都有不同的参数，所以需要根据参数锁定构造函数并进行初始化
* 如果既不存在工厂方法也不存在带有参数的构造函数，则使用默认的构造函数进行bean的实例化<br>
（3）MergedBeanDefinitionPostProcessor的应用<br>
&emsp; &emsp;bean合并后的处理，Autowired注解正是通过此方法实现诸如类型的预解析<br>
（4）依赖处理<br>
  A中含有B属性，B中有A，A在创建B时不是直接创建B,而是通过放入缓存中的ObjectFactory来创建实例<br>
（5）属性填充。将所有属性填充至bean的实例中<br>
（6）循环依赖检查<br>
   检测已经加载的bean是否已经出现了依赖循环，并判断是否需要抛出异常<br>
（7）注册DisposableBean<br>
   如果配置了destroy-method，这里需要注册以便于在销毁时候调用<br>
（8）完成创建并返回<br>

**7.1 创建bean的实例**   
```
protected BeanWrapper createBeanInstance(String beanName, RootBeanDefinition mbd, @Nullable Object[] args) {
		// Make sure bean class is actually resolved at this point.
		//解析class
		Class<?> beanClass = resolveBeanClass(mbd, beanName);

		if (beanClass != null && !Modifier.isPublic(beanClass.getModifiers()) && !mbd.isNonPublicAccessAllowed()) {
			throw new BeanCreationException(mbd.getResourceDescription(), beanName,
					"Bean class isn't public, and non-public access not allowed: " + beanClass.getName());
		}

		Supplier<?> instanceSupplier = mbd.getInstanceSupplier();
		if (instanceSupplier != null) {
			return obtainFromSupplier(instanceSupplier, beanName);
		}
        //如果工厂方法不为空则使用工厂方法初始化策略
		if (mbd.getFactoryMethodName() != null)  {
			return instantiateUsingFactoryMethod(beanName, mbd, args);
		}

		// Shortcut when re-creating the same bean...
		boolean resolved = false;
		boolean autowireNecessary = false;
		if (args == null) {
			synchronized (mbd.constructorArgumentLock) {
			    //一个类有多个构造函数，每个构造函数都有不同的参数，所以调用前需要先根据参数锁定构造函数或对应的工厂方法
				if (mbd.resolvedConstructorOrFactoryMethod != null) {
					resolved = true;
					autowireNecessary = mbd.constructorArgumentsResolved;
				}
			}
		}
		//如果已经解析过则使用解析好的构造函数方法不需要再次锁定
		if (resolved) {
			if (autowireNecessary) {
			    //构造函数自动注入
				return autowireConstructor(beanName, mbd, null, null);
			}
			else {
			    //使用默认构造函数构造
				return instantiateBean(beanName, mbd);
			}
		}

		// Need to determine the constructor...
		//需要根据参数解析构造函数
		Constructor<?>[] ctors = determineConstructorsFromBeanPostProcessors(beanClass, beanName);
		if (ctors != null ||
				mbd.getResolvedAutowireMode() == RootBeanDefinition.AUTOWIRE_CONSTRUCTOR ||
				mbd.hasConstructorArgumentValues() || !ObjectUtils.isEmpty(args))  {
			//构造函数自动注入
			return autowireConstructor(beanName, mbd, ctors, args);
		}

		// No special handling: simply use no-arg constructor.
		//使用默认构造函数构造
		return instantiateBean(beanName, mbd);
	}
```

```
public BeanWrapper autowireConstructor(final String beanName, final RootBeanDefinition mbd,
											@Nullable Constructor<?>[] chosenCtors, @Nullable final Object[] explicitArgs) {

		BeanWrapperImpl bw = new BeanWrapperImpl();
		this.beanFactory.initBeanWrapper(bw);

		Constructor<?> constructorToUse = null;
		ArgumentsHolder argsHolderToUse = null;
		Object[] argsToUse = null;

		if (explicitArgs != null) {
			argsToUse = explicitArgs;
		}
		else {
			Object[] argsToResolve = null;
			synchronized (mbd.constructorArgumentLock) {
				constructorToUse = (Constructor<?>) mbd.resolvedConstructorOrFactoryMethod;
				if (constructorToUse != null && mbd.constructorArgumentsResolved) {
					// Found a cached constructor...
					argsToUse = mbd.resolvedConstructorArguments;
					if (argsToUse == null) {
						argsToResolve = mbd.preparedConstructorArguments;
					}
				}
			}
			if (argsToResolve != null) {
				argsToUse = resolvePreparedArguments(beanName, mbd, bw, constructorToUse, argsToResolve);
			}
		}

		if (constructorToUse == null) {
			// Need to resolve the constructor.
			boolean autowiring = (chosenCtors != null ||
					mbd.getResolvedAutowireMode() == RootBeanDefinition.AUTOWIRE_CONSTRUCTOR);
			ConstructorArgumentValues resolvedValues = null;

			int minNrOfArgs;
			if (explicitArgs != null) {
				minNrOfArgs = explicitArgs.length;
			}
			else {
				ConstructorArgumentValues cargs = mbd.getConstructorArgumentValues();
				resolvedValues = new ConstructorArgumentValues();
				minNrOfArgs = resolveConstructorArguments(beanName, mbd, bw, cargs, resolvedValues);
			}

			// Take specified constructors, if any.
			Constructor<?>[] candidates = chosenCtors;
			if (candidates == null) {
				Class<?> beanClass = mbd.getBeanClass();
				try {
					candidates = (mbd.isNonPublicAccessAllowed() ?
							beanClass.getDeclaredConstructors() : beanClass.getConstructors());
				}
				catch (Throwable ex) {
					throw new BeanCreationException(mbd.getResourceDescription(), beanName,
							"Resolution of declared constructors on bean Class [" + beanClass.getName() +
									"] from ClassLoader [" + beanClass.getClassLoader() + "] failed", ex);
				}
			}
			AutowireUtils.sortConstructors(candidates);
			int minTypeDiffWeight = Integer.MAX_VALUE;
			Set<Constructor<?>> ambiguousConstructors = null;
			LinkedList<UnsatisfiedDependencyException> causes = null;

			for (Constructor<?> candidate : candidates) {
				Class<?>[] paramTypes = candidate.getParameterTypes();

				if (constructorToUse != null && argsToUse.length > paramTypes.length) {
					// Already found greedy constructor that can be satisfied ->
					// do not look any further, there are only less greedy constructors left.
					break;
				}
				if (paramTypes.length < minNrOfArgs) {
					continue;
				}

				ArgumentsHolder argsHolder;
				if (resolvedValues != null) {
					try {
						String[] paramNames = ConstructorPropertiesChecker.evaluate(candidate, paramTypes.length);
						if (paramNames == null) {
							ParameterNameDiscoverer pnd = this.beanFactory.getParameterNameDiscoverer();
							if (pnd != null) {
								paramNames = pnd.getParameterNames(candidate);
							}
						}
						argsHolder = createArgumentArray(beanName, mbd, resolvedValues, bw, paramTypes, paramNames,
								getUserDeclaredConstructor(candidate), autowiring);
					}
					catch (UnsatisfiedDependencyException ex) {
						if (logger.isTraceEnabled()) {
							logger.trace("Ignoring constructor [" + candidate + "] of bean '" + beanName + "': " + ex);
						}
						// Swallow and try next constructor.
						if (causes == null) {
							causes = new LinkedList<>();
						}
						causes.add(ex);
						continue;
					}
				}
				else {
					// Explicit arguments given -> arguments length must match exactly.
					if (paramTypes.length != explicitArgs.length) {
						continue;
					}
					argsHolder = new ArgumentsHolder(explicitArgs);
				}

				int typeDiffWeight = (mbd.isLenientConstructorResolution() ?
						argsHolder.getTypeDifferenceWeight(paramTypes) : argsHolder.getAssignabilityWeight(paramTypes));
				// Choose this constructor if it represents the closest match.
				if (typeDiffWeight < minTypeDiffWeight) {
					constructorToUse = candidate;
					argsHolderToUse = argsHolder;
					argsToUse = argsHolder.arguments;
					minTypeDiffWeight = typeDiffWeight;
					ambiguousConstructors = null;
				}
				else if (constructorToUse != null && typeDiffWeight == minTypeDiffWeight) {
					if (ambiguousConstructors == null) {
						ambiguousConstructors = new LinkedHashSet<>();
						ambiguousConstructors.add(constructorToUse);
					}
					ambiguousConstructors.add(candidate);
				}
			}

			if (constructorToUse == null) {
				if (causes != null) {
					UnsatisfiedDependencyException ex = causes.removeLast();
					for (Exception cause : causes) {
						this.beanFactory.onSuppressedException(cause);
					}
					throw ex;
				}
				throw new BeanCreationException(mbd.getResourceDescription(), beanName,
						"Could not resolve matching constructor " +
								"(hint: specify index/type/name arguments for simple parameters to avoid type ambiguities)");
			}
			else if (ambiguousConstructors != null && !mbd.isLenientConstructorResolution()) {
				throw new BeanCreationException(mbd.getResourceDescription(), beanName,
						"Ambiguous constructor matches found in bean '" + beanName + "' " +
								"(hint: specify index/type/name arguments for simple parameters to avoid type ambiguities): " +
								ambiguousConstructors);
			}

			if (explicitArgs == null) {
				argsHolderToUse.storeCache(mbd, constructorToUse);
			}
		}

		try {
			final InstantiationStrategy strategy = this.beanFactory.getInstantiationStrategy();
			Object beanInstance;

			if (System.getSecurityManager() != null) {
				final Constructor<?> ctorToUse = constructorToUse;
				final Object[] argumentsToUse = argsToUse;
				beanInstance = AccessController.doPrivileged((PrivilegedAction<Object>) () ->
								strategy.instantiate(mbd, beanName, this.beanFactory, ctorToUse, argumentsToUse),
						this.beanFactory.getAccessControlContext());
			}
			else {
				beanInstance = strategy.instantiate(mbd, beanName, this.beanFactory, constructorToUse, argsToUse);
			}

			bw.setBeanInstance(beanInstance);
			return bw;
		}
		catch (Throwable ex) {
			throw new BeanCreationException(mbd.getResourceDescription(), beanName,
					"Bean instantiation via constructor failed", ex);
		}
	}
```   
实现的功能考虑方面:<br> 
（1）构造函数参数的确定
  * 根据explicitArgs参数判断<br>
     explicitArgs参数是在调用Bean的时候用户指定的，在BeanFactory类中存在方法:object getBean(String name,Object... args) throws BeansException；<br>
     在获取bean的时候，用户不但可以指定bean的名称还可以指定bean所对应类的构造函数或者工厂方法的方法参数,主要用于静态工厂方法的调用<br>
     
  
   
   
   
   
   
   
   
   
   