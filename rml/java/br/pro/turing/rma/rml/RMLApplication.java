package br.pro.turing.rma.rml;

/**
 * The Resource Management Layer application.
 */
public class RMLApplication {

    /**
     * Main mathod.
     *
     * @param args GatewayIP_1 GatewayPort_1 GatewayIP_2 GatewayPort_2 ... GatewayIP_N GatewayPort_N
     */
    public static void main(String[] args) {
        //        Configurator.setRootLevel(Level.INFO);

        new RMLGatewayStarter(args).start();
        new RMLServer().start();
    }
}
