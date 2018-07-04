package circularDependency;

import org.junit.Test;
import org.springframework.beans.factory.BeanCurrentlyInCreationException;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Main {
    @Test(expected = BeanCurrentlyInCreationException.class)
    public void testCircleByConstructor() throws Throwable{
        try {
            new ClassPathXmlApplicationContext("circularDependency/beans.xml");
        }catch (Exception e){
            Throwable el = e.getCause().getCause().getCause();
            throw el;
        }
    }
}
