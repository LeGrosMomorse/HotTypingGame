import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;
import java.util.Scanner;

public class Serveur {

    ServerSocket serverSocket;
    public BufferedReader[] listeFluxEntrants;  //regroupe les flux entrants dans une liste avec comme index l'ordre de connexion des machines
    public PrintWriter[] listeFluxSortants;     //regroupe les flux sortants dans une liste avec comme index l'ordre de connexion des machines
    public int[] machineUtilisee;               //donne pour chaque equipe (index) l'index de la machine qui est actuellement utilisée  -- s'il n'y en a pas alors -> 9999
    public int[] scoreEquipes;                  //donne pour chaque equipe (index) le score de l'équipe en question


    public static int NB_MACHINES = 7;          //nombre maxi de machine connectée
    public static int NB_EQUIPES = 4;           //nombre maxi d'équipes : 8
                                                // 0:red - 1:yellow - 2:blue - 3:pink - 4:green - 5:black - 6:orange - 7:magenta - default:white
    public Random generateurNombreAleatoire;


    public Serveur() {

        if(NB_EQUIPES>NB_MACHINES) {
            System.out.println("ERREUR - Serveur:public Serveur()");
            System.exit(1);
        }

        this.generateurNombreAleatoire = new Random((new Date()).getTime());

        this.listeFluxEntrants = new BufferedReader[NB_MACHINES];
        this.listeFluxSortants = new PrintWriter[NB_MACHINES];
        this.scoreEquipes = new int[NB_EQUIPES];
        this.machineUtilisee = new int[NB_EQUIPES];

        for(int i = 0 ; i < NB_EQUIPES ; i++) {
            this.scoreEquipes[i] = 0;
            this.machineUtilisee[i] = 9999;      //pour la fonction choisirMachineAleatoire
        }
    }

    public void enregistrementService(int port) {
        try {
            System.out.println("*** Serveur en attente ***");
            serverSocket = new ServerSocket(port, NB_MACHINES); // file d'attente de taille 4 créée sur le port
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("ERREUR - Serveur:enregistrementService(int port)");
            System.exit(1);
        }
    }

    public Socket nouvelleConnexion() {
        Socket socket = null;
        try {
            socket = serverSocket.accept(); // fonction bloquante, crée et retourne le socket
            System.out.println("*** Nouvelle connexion ***");
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("ERREUR - Serveur:nouvelleConnexion()");
            System.exit(1);
        }
        return socket;
    }

    public static void main(String[] args) {

        if (args.length != 1) {
            System.err.println("ERREUR - Serveur:main(String[] args) -- 1");
            System.exit(1);
        }

        int port = Integer.parseInt(args[0]);
        Serveur s = new Serveur();

        s.enregistrementService(port);

        for (int i = 0 ; i < NB_MACHINES ; i++){
            Socket socket = s.nouvelleConnexion();
            s.listeFluxEntrants[i] = Util.fluxEntrant(socket);
            s.listeFluxSortants[i] = Util.fluxSortant(socket);
        }

        System.out.println("La partie commence !");

        //on dit sur toutes les machines que la partie a commencée
        s.envoieCollectif("La partie a commencée !");

        String message;

        //on envoit un premier message pour initialiser
        for (int i = 0 ; i < NB_EQUIPES ; i++){

            s.envoieCombinaisonAUneEquipe(i);
        }

        boolean gagne = false;

        while(!gagne) {

            for (int i = 0 ; i < NB_EQUIPES ; i++){
                //on parcourt la liste des flux entrant possibles (en fonction des machines utilisées)

                try {
                    if(s.listeFluxEntrants[s.machineUtilisee[i]].ready()){

                        System.out.println("ON EST LA - 1");
                        message = s.listeFluxEntrants[s.machineUtilisee[i]].readLine();
                        System.out.println("ON EST LA - 2");
                        int numeroEquipe = Integer.parseInt(message);
                        s.scoreEquipes[numeroEquipe] += 1;                     //on augmente de 1 le score de l'équipe


                        //j'affiche les scores de l'équipes
                        for(int j = 0 ; j < NB_EQUIPES ; j++){
                            System.out.print("e"+j+" : "+s.scoreEquipes[j]+"\n");
                        }


                        if(s.scoreEquipes[numeroEquipe] == 10){
                            gagne = true;
                            s.envoieCollectif("Le gagnant est l'équipe n°"+numeroEquipe);
                        }
                        else{

                            //on envoit un nouveau message sur une machine disponible
                            s.envoieCombinaisonAUneEquipe(numeroEquipe);

                        }

                    }
                } catch (IOException e) {
                    System.err.println("ERREUR - Serveur:main(String[] args) -- 2");
                    e.printStackTrace();
                    System.exit(1);
                }

            }

        }



    }


    //FONCTION RESEAU

    public void envoieCollectif(String message){
        for (int i = 0 ; i < NB_MACHINES ; i++){
            this.listeFluxSortants[i].println(message);
            this.listeFluxSortants[i].flush();
        }
        System.out.println(" *** "+message+" *** ");
    }

    public void envoieCombinaisonAUneEquipe(int numEquipe){

        //on choisit une machine disponible
        int numeroDeMachineDispo = this.choisirMachineAleatoireDispo(numEquipe);

        String message = this.nouvelleCombinaisonLettreCouleur(numEquipe);      //lettre:couleur    ex: P:2

        this.listeFluxSortants[numeroDeMachineDispo].println(message);
        this.listeFluxSortants[numeroDeMachineDispo].flush();

        System.out.println(" ** OUT : "+message+" TO "+numEquipe+" ** ");
    }



    //FONCTION JEU

    public int choisirMachineAleatoireDispo(int numEquipe){

        //on recherche une machine libre aléatoirement

        int nouveauNumeroMachine = machineUtilisee[numEquipe];

        while(appartientALaListe(nouveauNumeroMachine, machineUtilisee)){
            nouveauNumeroMachine = generateurNombreAleatoire.nextInt(NB_MACHINES);
        }
        machineUtilisee[numEquipe] = nouveauNumeroMachine;
        return nouveauNumeroMachine;
    }


    public boolean appartientALaListe(int n, int[] l){
        boolean bob = false;
        for( int i = 0 ; i < l.length ; i++ ){
            if(n == l[i])
                bob = true;
        }
        return bob;

    }



    public char choisirLettreAleatoire(){
        int n = generateurNombreAleatoire.nextInt(26);
        return (char)((int)'A'+n);
    }

/*
    public String choisirCouleurAleatoire(){
        return String.valueOf(generateurNombreAleatoire.nextInt(NB_EQUIPES));
    }
*/
    public String nouvelleCombinaisonLettreCouleur(int i){
        return choisirLettreAleatoire() + ":" + String.valueOf(i);
    }




}