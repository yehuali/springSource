package prepareRefresh;

import aware.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Main {
    public static void main(String[] args) {
        ApplicationContext ctx = new MyClassPathXmlApplicationContext("aware/beans.xml");
        Test test = (Test) ctx.getBean("test");
        test.testAware();
    }


}
