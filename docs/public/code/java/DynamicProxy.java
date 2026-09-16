import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * 委托类都实现了Sell接口
 */
interface Sell {
    void sell();

    void ad();
}

/**
 * 生产厂家(委托类)
 */
class Vendor implements Sell {
    public void sell() {
        System.out.println("In sell method");
    }

    public void ad() {
        System.out.println("ad method");
    }
}

/**
 * 静态代理类
 */
class BusinessAgent implements Sell {
    // 委托类实例 
    private Vendor mVendor;

    public BusinessAgent(Vendor vendor) {
        this.mVendor = vendor;
    }

    public void sell() {
        System.out.println("before");
        mVendor.sell();
        System.out.println("after");
    }

    public void ad() {
        System.out.println("before");
        mVendor.ad();
        System.out.println("after");
    }
}

/**
 * 中间类
 */
public class DynamicProxy implements InvocationHandler {
    // 委托类实例 
    private Object obj;

    public DynamicProxy(Object obj) {
        this.obj = obj;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println("before");
        System.err.println(method.getDeclaringClass()); 
        Object result = method.invoke(obj, args);
        System.out.println("after");
        return result;
    }
}

/**
 * 动态代理实际为：两个静态代理的组合。代理类代理中介类，中介类代理委托类
 */
class Main {
    public static void main(String[] args) {
        // 创建中介类实例 
        DynamicProxy inter = new DynamicProxy(new Vendor());
        // 加上这句将会产生一个$Proxy0.class文件，这个文件即为动态生成的代理类文件
        System.getProperties().put("sun.misc.ProxyGenerator.saveGeneratedFiles", "true");

        // 获取代理类实例 
        Sell sell = (Sell) (Proxy.newProxyInstance(Sell.class.getClassLoader(), new Class[] { Sell.class }, inter));

        // 通过代理类对象调用代理类方法，实际上会转到invoke方法调用 
        sell.sell();
        sell.ad();
    }
}