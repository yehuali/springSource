package factoryBean;

import customTag.User;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class main {
    public static void main(String[] args) {
        ApplicationContext bf = new ClassPathXmlApplicationContext("factoryBean/bean.xml");
        Car car = (Car)bf.getBean("car");
        System.out.println(car.getBrand());
    }
}
