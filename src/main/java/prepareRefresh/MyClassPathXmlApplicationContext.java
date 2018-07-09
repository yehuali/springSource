package prepareRefresh;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class MyClassPathXmlApplicationContext extends ClassPathXmlApplicationContext {
    public MyClassPathXmlApplicationContext(String... configLocations){
        super(configLocations);
    }

    @Override
    protected void initPropertySources() {
        getEnvironment().setRequiredProperties("VAR");
    }
}
