import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class Util {


    // Méthode permettant d’obtenir un BufferedReader correspondant au flux entrant d’un // Socket
    static public BufferedReader fluxEntrant(Socket socket){
        InputStream inputStream;
        InputStreamReader inputStreamReader;
        BufferedReader fluxEntrant=null;
        try{
            inputStream=socket.getInputStream();
            inputStreamReader = new InputStreamReader (inputStream);
            fluxEntrant=new BufferedReader(inputStreamReader);
        }catch(IOException e ){
            e.printStackTrace();
            System.err.println( "!!! Problème lors de la création du flux entrant !!!");
            System.exit(1);
        }
        return fluxEntrant;
    }
    // Méthode permettant d’obtenir un PrintWriter correspondant au flux sortant d’un // Socket
    static public PrintWriter fluxSortant(Socket socket){
        OutputStream outputStream;
        PrintWriter fluxSortant=null;
        try{
            outputStream=socket.getOutputStream();
            fluxSortant=new PrintWriter(outputStream);
        }catch(IOException e ){
            e.printStackTrace();
            System.err.println( "!!! Problème lors de la création du flux sortant !!!");
            System.exit(1);
        }
        return fluxSortant;
    }
}