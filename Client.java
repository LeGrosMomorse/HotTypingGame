import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Client {
    Socket socket;
    FenetreLettre window = new FenetreLettre();




    public void connexion(InetAddress adresseServeur, int port) {
        try {
            socket = new Socket(adresseServeur, port); // constructeur
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("\n *** IL FAUT LANCER LE SERVEUR EN PREMIER. *** \n");
            System.exit(1);
        }
        System.out.println("*** Connexion établie ***");
    }






    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("ERREUR : Client:main(String[] args) -- 1");
            System.exit(1);
        }

        Client c = new Client();
        try {
            c.connexion(InetAddress.getByName(args[0]), Integer.parseInt(args[1]));
        } catch (UnknownHostException e) {
            e.printStackTrace();
            System.err.println("ERREUR : Client:main(String[] args) -- 2");
            System.exit(1);
        }

        //on attend le message qui dit que ça commence
        BufferedReader fluxEntrant = Util.fluxEntrant(c.socket);
        PrintWriter fluxSortant = Util.fluxSortant(c.socket);


        String message = null;

        try {
            message = fluxEntrant.readLine();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("ERREUR : Client:main(String[] args) -- 3");
            System.exit(1);
        }
        System.out.println(message);


        while(true){
            try {

                System.out.println("On attend ...");

                //on recoit un message
                message = fluxEntrant.readLine();

                System.out.println("Message reçu !\n\t\t"+message);

                //dans tous les cas on split le message
                String[] parts = message.split(":");



                if(parts[0].equals("Partie terminée.")){


                    //pour clear le terminal : "\033[H\033[2J"
                    System.out.println("\033[H\033[2J\nPartie terminée.");

                    //on va recevoir le message de fin
                    message = fluxEntrant.readLine();

                    //on décrypte le message
                    message = message.replaceAll("_", "\n");
                    System.out.println(message);

                    //on arrete le programme
                    System.exit(1);
                }


                //on affiche la fenetre avec les informations qu'on a reçu
                c.window.afficherLettre(parts[0].charAt(0), Integer.parseInt(parts[1]));


                //on test si le joueur a cliqué
                while(!c.window.clique){}

                //le joueur a cliqué quand on sort de cette boucle
                //on envoit un message au serveur pour dire qu'on a cliqué
                fluxSortant.println(parts[1]);        //on envoit une réponse au serveur en lui retournant la couleur
                fluxSortant.flush();

            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("ERREUR : Client:main(String[] args) -- 4");
                System.exit(1);
            }
        }




    }
}