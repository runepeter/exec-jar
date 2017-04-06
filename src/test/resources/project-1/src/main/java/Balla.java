import java.security.Provider;
import java.security.Security;

public class Balla {

    static {
        if (Security.getProvider("BC") == null) {
            try {

                Class<Provider> providerClass = (Class<Provider>) Class.<Provider>forName("org.bouncycastle.jce.provider.BouncyCastleProvider");
                Provider provider = providerClass.newInstance();
                Security.addProvider(provider);
                System.out.println("BouncyCastle registered as security provider");

            } catch (Exception e) {
                throw new RuntimeException("Unable to initialize BouncyCastle as Security Provider.", e);
            }
        }
    }

    public static void main(String[] args) {
        System.out.println("Going to initialize HttpClient.");
        org.apache.http.impl.client.HttpClients.custom().build();
        System.out.println("END");

    }
}
