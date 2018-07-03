package customTag;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class main {
    public static void main(String[] args) {
        ApplicationContext bf = new ClassPathXmlApplicationContext("customTag/test.xml");
        User user = (User)bf.getBean("testbean");
        System.out.println(user.getUserName() + "," +user.getEmail());
    }
}
