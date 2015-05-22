package test2;

public class Inner {
    public void sample() throws Exception {
        java.util.Properties props = new java.util.Properties();
        java.rmi.activation.ActivationGroupDesc.CommandEnvironment ace = null;
        java.rmi.activation.ActivationGroupDesc agd = new
            java.rmi.activation.ActivationGroupDesc(props,ace);
    }
    public static void main(String args[]) {
        System.out.println("Inner");
    }
}
