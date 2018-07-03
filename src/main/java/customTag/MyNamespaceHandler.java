package customTag;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;
//创建一个Handler文件，扩展自NamespaceHandlerSupport，目的是将组建注册到Spring容器
public class MyNamespaceHandler extends NamespaceHandlerSupport {
    @Override
    public void init() {
        registerBeanDefinitionParser("user",new UserBeanDefinitionParser());
    }
}
