package circularDependency.prototype;

import org.junit.Test;
import org.springframework.beans.factory.BeanCurrentlyInCreationException;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Main {

    @Test(expected = BeanCurrentlyInCreationException.class)
    public void testCircleBySetterAndPrototype() throws Throwable{
        try{
            ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("circularDependency/prototype/beans.xml");
            System.out.println(ctx.getBean("testA"));
        }catch (Exception e){
            Throwable el = e.getCause().getCause().getCause();
            throw el;
        }
    }
}
