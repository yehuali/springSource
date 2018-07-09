package spel;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Test {
    public static void main(String[] args) {
        ApplicationContext ctx=new ClassPathXmlApplicationContext("spel/beans.xml");
        Order order=ctx.getBean("order001",Order.class);
        System.out.println(order);
    }

    @org.junit.Test
    public void testUserManager(){
        ApplicationContext ctx = new ClassPathXmlApplicationContext("spel/beans.xml");
        UserManager userManager = (UserManager)ctx.getBean("userManager");
        System.out.println(userManager);
    }
}
