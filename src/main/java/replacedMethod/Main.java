package replacedMethod;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Main {
    public static void main(String[] args) {
        ApplicationContext bf = new ClassPathXmlApplicationContext("replacedMethod/replaceMethodTest.xml");
        TestChangeMethod test = (TestChangeMethod) bf.getBean("testChangeMethod");
        test.changeMe();
    }
}
