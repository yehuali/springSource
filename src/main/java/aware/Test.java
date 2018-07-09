package aware;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Spring提供一些Aware相关接口(比如BeanFactoryAware、ApplicationContextAware、ResourceLoaderAware、ServletContextAware等)
 * 实现这些Aware接口的bean初始化之后，可以取得一些相对应的资源
 */
public class Test implements BeanFactoryAware {
    private BeanFactory beanFactory;

    //生命bean的时候Spring会自动注入BeanFactory
    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    public void testAware(){
        Hello hello = (Hello) beanFactory.getBean("hello");
        hello.say();
    }

    public static void main(String[] args) {
        ApplicationContext ctx = new ClassPathXmlApplicationContext("aware/beans.xml");
        Test test = (Test) ctx.getBean("test");
        test.testAware();
    }
}
