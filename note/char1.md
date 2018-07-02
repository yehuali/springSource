**Spring**的整体架构<br>
![]( https://github.com/yehuali/springSource/raw/master/note/images/springFramework.jpg)
**(1)Core Container** <br>
基础概念是BeanFactory,提供对Factory模式的经典实现来消除对程序性单例模式的需要，从程序逻辑中分离出依赖关系和配置 <br>
  * Core模块主要包含Spring框架基本的核心工具类 <br>
  * 包含访问配置文件、创建和管理bean以及进行Ioc/DI操作相关的所有类 <br>
  * Context模块构建于Core和Beans模块基础之上，提供了框架式的对象访问方法。为Spring核心提供了大量的扩展，添加了对国际化、事件传播、资源加载和对Context的透明创建的支持。ApplicationContext接口是Context模块的关键 <br>
  * Expression Language提供了强大的表达式语言用于在运行时查询和操纵对象 <br>
  
**(2) Data Access/Integaration** <br>
  * JDBC模块提供了JDBC抽象层，消除冗长的JDBC编码和解析数据库厂特有的错误代码。包含了Spring对JDBC数据访问进行封装的所有类
  * ORM模块为流行的对象-关系映射API（如Hibernate等）提供了一个交互层
  
**(3)Web** <br>
**(4)AOP** <br>

    