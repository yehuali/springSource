package replacedMethod;

import org.springframework.beans.factory.support.MethodReplacer;

import java.lang.reflect.Method;

public class TestMethodReplacer implements MethodReplacer {
    @Override
    public Object reimplement(Object o, Method method, Object[] objects) throws Throwable {
        System.out.println("我替换了原有的方法");
        return null;
    }
}
