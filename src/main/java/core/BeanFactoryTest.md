**功能分析** <br>
![]( https://github.com/yehuali/springSource/raw/master/note/images/beanFactoryTestUML.jpg)
* ConfigReader:用于读取及验证配置文件，然后放置在内存中
* ReflectionUtil：根据配置文件的配置进行反射实例化
* App:用于完成整个逻辑的串联
