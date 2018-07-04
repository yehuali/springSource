package circularDependency;

public class TestA {
    public TestB testB;
    public TestA(TestB testB){
        this.testB = testB;
    }
    public void a(){
        testB.b();
    }

    public TestB getTestB() {
        return testB;
    }

    public void setTestB(TestB testB) {
        this.testB = testB;
    }
}
