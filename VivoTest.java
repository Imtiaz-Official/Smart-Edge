import java.lang.reflect.Method;
public class VivoTest {
    public static void main(String[] args) {
        try {
            Class<?> clazz = Class.forName("vivo.app.vivofreeform.AbsVivoFreeformManager");
            for (Method m : clazz.getDeclaredMethods()) {
                if (m.getName().contains("Start") || m.getName().contains("Freeform")) {
                    System.out.println(m);
                }
            }
        } catch (Exception e) {}
        try {
            Class<?> clazz = Class.forName("vivo.app.vivofreeform.IVivoFreeformManager");
            for (Method m : clazz.getDeclaredMethods()) {
                if (m.getName().contains("Start") || m.getName().contains("Freeform")) {
                    System.out.println(m);
                }
            }
        } catch (Exception e) {}
    }
}
