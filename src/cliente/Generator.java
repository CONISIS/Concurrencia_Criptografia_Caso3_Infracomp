package cliente;

import uniandes.gload.core.LoadGenerator;
import uniandes.gload.core.Task;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Generator
{
    private LoadGenerator generator;

    public Generator(String ip, int puerto, int nTareas, int espera)
    {
        Client.ip = ip;
        Client.puerto = puerto;
        Task tarea = new ClientTask();
        generator = new LoadGenerator("Prueba de carga", nTareas, tarea, espera);
        generator.generate();
    }

    public static void main(String[] args)
    {
        try
        {
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("Ingrese la ip a la que se conectará");
            String ip = in.readLine();
            System.out.println("Ingrese el puerto al que se conectará");
            int puerto = Integer.parseInt(in.readLine());
            System.out.println("Ingrese el número de conexiones que quiere lanzar");
            int tareas = Integer.parseInt(in.readLine());
            System.out.println("Ingrese la espera entre conexiones");
            int espera = Integer.parseInt(in.readLine());
            Generator gen = new Generator(ip, puerto, tareas, espera);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

    }

    private class ClientTask extends Task
    {
        @Override
        public void execute()
        {
            try
            {
                Client cliente = new Client();
                cliente.comunicacion1();
                String certificado = cliente.getInServidor().readLine();
                cliente.comunicacion2(certificado);
                cliente.comunicacion3();
                cliente.comunicacion4();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        @Override
        public void fail()
        {
            System.out.println(Task.MENSAJE_FAIL);
        }

        @Override
        public void success()
        {
            System.out.println(Task.OK_MESSAGE);
        }
    }
}
