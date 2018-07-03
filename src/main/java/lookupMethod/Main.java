package lookupMethod;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * 获取器注入：把一个方法声明为返回某种类型的bean，但实际要返回的bean是在配置文件里配置的
 * -->该方法可用在设计有些可插拔的功能上，解除程序依赖
 */
public class Main {
    public static void main(String[] args) {
        ApplicationContext bf = new ClassPathXmlApplicationContext("lookupMethod/lookupTest.xml");
        GetBeanTest test = (GetBeanTest)bf.getBean("getBeanTest");
        test.showMe();
    }
}
