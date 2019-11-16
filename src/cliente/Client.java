package cliente;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.net.Socket;
import java.security.Key;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;

/**
 * @author Juan
 * @author Andres
 */
public class Client
{
    // CADENAS DE CONTROL

    public static final String HOLA = "HOLA";

    public static final String OK = "OK";

    public static final String ERROR = "ERROR";

    public static final String ALGORITMOS = "ALGORITMOS";

    // SEPARADOR

    public static final String SEPARADOR = ":";

    // ALGORITMOS SIMÉTRICOS

    public static final String AES = "AES";

    // ALGORITMOS ASIMÉTRICOS

    public static final String RSA = "RSA";

    // ALGORITMOS HMAC

    public static final String SHA256 = "HMACSHA256";
    

    // ATRIBUTOS

    private Socket canal;

    private BufferedReader inServidor;

    private PrintWriter out;

    private String hmac = "";

    private String simetrico = "";
    
    private Key KW;
    
    private SecretKey KS;
    
    public String reto = "reto";

    public static String ip = "localhost";
    public static int puerto = 8000;
    public String cc = "2087";
    public String clave = "hola";

    // CONSTRUCTOR

    public Client()
    {
        try
        {
            canal = new Socket(ip, puerto);
            inServidor = new BufferedReader(new InputStreamReader(canal.getInputStream()));
            out = new PrintWriter(canal.getOutputStream(), true);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

    }

    // MÉTODOS

    public Socket getCanal()
    {
        return canal;
    }

    public void setCanal(Socket canal)
    {
        this.canal = canal;
    }

    public BufferedReader getInServidor()
    {
        return inServidor;
    }

    public void setInServidor(BufferedReader inServidor)
    {
        this.inServidor = inServidor;
    }


    public PrintWriter getOut()
    {
        return out;
    }

    public void setOut(PrintWriter out)
    {
        this.out = out;
    }

    /**
     * Etapa 1 de la comunicación con el servidor en que se seleccionan los algoritmos a usar.
     *
     * @throws Exception En caso de que el servidor mande un mensaje de ERROR.
     */
    public void comunicacion1() throws Exception
    {
        try
        {
            getOut().println(HOLA);
            if (ERROR.equals(getInServidor().readLine()))
                throw new Exception("Hubo un error al iniciar la comunicación con el servidor");
            hmac = SHA256;
            simetrico = AES;
            getOut().println(ALGORITMOS + SEPARADOR + simetrico + SEPARADOR + RSA + SEPARADOR + hmac);
            if (ERROR.equals(getInServidor().readLine()))
                throw new Exception("Hubo un error al enviar algoritmos de cifrado al servidor");
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Etapa 2 del protocolo de comunicación, se autenticará al servidor.
     *
     * @param certificado certificado del servidor web
     */
    public void comunicacion2(String certificado)
    {
    	//Recibir y verificar certificado
    	byte[] certEntryBytes = DatatypeConverter.parseBase64Binary(certificado);
    	InputStream in = new ByteArrayInputStream(certEntryBytes);
    	X509Certificate cert = null;
    	try {
    		CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
    		cert = (X509Certificate) certFactory.generateCertificate(in);
			cert.checkValidity();
			System.out.println("Certificado valido");  
			in.close();
		} catch (Exception e) {
			System.out.println("El certificado no es valido.. terminado conexion");
        	System.exit(0);
		}
    	
    	// Generar llave simetrica y enviarla
    	KW =  cert.getPublicKey();
    	byte[] retoCifrado = null;
    	try {

    		KeyGenerator keyGen = KeyGenerator.getInstance(simetrico);
    	    KS = keyGen.generateKey();

            Cipher cifrador = Cipher.getInstance("RSA");
			cifrador.init(Cipher.ENCRYPT_MODE, KW);
			retoCifrado = cifrador.doFinal(KS.getEncoded());
			
			String mensaje = DatatypeConverter.printBase64Binary(retoCifrado);
			getOut().println(mensaje);
			
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Hubo un error al cifrar el mensaje");
		}
    	
    	//Envia reto
    	getOut().println(reto);
	
    	//Validar reto
    	Cipher cifrador;
		try {
			cifrador = Cipher.getInstance(simetrico);
			cifrador.init(Cipher.DECRYPT_MODE, KS);
			byte[] retoDescifrado = cifrador.doFinal(DatatypeConverter.parseBase64Binary(getInServidor().readLine()));
			
			if(reto.equals(DatatypeConverter.printBase64Binary(retoDescifrado))){
				System.out.println("reto validado");
				getOut().println(OK);
			}
			else{
				System.out.println("El reto no es valido... terminando conexion");
				getOut().println(ERROR);
	        	System.exit(0);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
    }

    /**
     * Etapa 3 del protocolo de comunicación, se autenticará al cliente.
     */
    public void comunicacion3()
    {
    	Cipher cifrador;
		try {
			
			//enviar cc
			cifrador = Cipher.getInstance(simetrico);
			cifrador.init(Cipher.ENCRYPT_MODE, KS);
			byte[] ccCifrado = cifrador.doFinal(DatatypeConverter.parseBase64Binary(cc));
			String mensaje = DatatypeConverter.printBase64Binary(ccCifrado);
			getOut().println(mensaje);
	    	
			//enviar clave
			cifrador = Cipher.getInstance(simetrico);
			cifrador.init(Cipher.ENCRYPT_MODE, KS);
			byte[] claveCifrado = cifrador.doFinal(DatatypeConverter.parseBase64Binary(clave));
			mensaje = DatatypeConverter.printBase64Binary(claveCifrado);
			getOut().println(mensaje);
			
		} catch (Exception e) {
			e.printStackTrace();
		}

    }
    /**
     * Etapa 4 del protooolo de comunicación, se solicita información y se valifa la respuesta.
     */
    public void comunicacion4()
    {
    	Cipher cifrador;
		try {
			cifrador = Cipher.getInstance(simetrico);
		
			//descifra el valor
			cifrador.init(Cipher.DECRYPT_MODE, KS);
			byte[] valorDescifrado = cifrador.doFinal(DatatypeConverter.parseBase64Binary(getInServidor().readLine()));
			System.out.println("Monto ahorrado: "+DatatypeConverter.printBase64Binary(valorDescifrado));
			
			//descifra el hmac
			cifrador = Cipher.getInstance(RSA);
			cifrador.init(Cipher.DECRYPT_MODE, KW);
			byte[] macu = cifrador.doFinal(DatatypeConverter.parseBase64Binary(getInServidor().readLine()));
			
			//calcula hmac
	    	Mac mc = Mac.getInstance(hmac);
			mc.init( KS);
			byte[] mac = mc.doFinal(valorDescifrado);
			
			//compara hmacs
			if(Arrays.equals(macu,mac)){
				System.out.println("Valor confirmado por hmac");
				getOut().println(OK);
			}
			else{
				System.out.println("Hmac invalido... terminado conexion");
				getOut().println(ERROR);
				System.exit(0);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
    
    public static void imprimirByte(byte[] a){
    	for (int i = 0; i < a.length-1; i++) {
			System.out.print(a[i]+" ");
		}
    	System.out.println(a[a.length-1]+" ");
    }
}
