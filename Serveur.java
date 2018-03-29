import java.awt.*;
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
    public ArrayList<String> listeCouleur;                    //liste qui donne les couleurs

    public static int NB_MACHINES = 9;          //nombre maxi de machine connectée
    public static int NB_EQUIPES = 9;           //nombre maxi d'équipes : 8
    public static int NB_SCORE_GAGNANT = 1;     //score pour gagner
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
            this.machineUtilisee[i] = 9999;      //pour la fonction choisirMachineAleatoire     -- il ne peut pas y avoir plus de 9999 machines connectées
        }
        this.listeCouleur = new ArrayList<String>();
        this.listeCouleur.add("ROUGE");
        this.listeCouleur.add("JAUNE");
        this.listeCouleur.add("BLEU");
        this.listeCouleur.add("ROSE");
        this.listeCouleur.add("VERT");
        this.listeCouleur.add("NOIR");
        this.listeCouleur.add("ORANGE");
        this.listeCouleur.add("MAGENTA");
        this.listeCouleur.add("BLANC");


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



    public Socket nouvelleConnexion(int i) {
        Socket socket = null;
        try {
            socket = serverSocket.accept(); // fonction bloquante, crée et retourne le socket
            System.out.println("*** Nouvelle connexion : "+(i+1)+" ***");
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

        System.out.println("CONFIGURATION : \n\tNombres de machines : "+NB_MACHINES+"\n\tNombre d'équipes : "+NB_EQUIPES);

        int port = Integer.parseInt(args[0]);
        Serveur s = new Serveur();

        s.enregistrementService(port);

        for (int i = 0 ; i < NB_MACHINES ; i++){
            Socket socket = s.nouvelleConnexion(i);
            s.listeFluxEntrants[i] = Util.fluxEntrant(socket);
            s.listeFluxSortants[i] = Util.fluxSortant(socket);
        }

        //il faut appuyer pour lancer la partie
        s.lancementPartie();

        System.out.println("La partie commence !");

        //on dit sur toutes les machines que la partie a commencée
        s.envoieCollectif("La partie a commencée !");


        //on envoit un premier message pour initialiser
        for (int i = 0 ; i < NB_EQUIPES ; i++){

            s.envoieCombinaisonAUneEquipe(i);
        }

        String message;
        boolean gagne = false;

        while(!gagne) {

            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            for (int i = 0 ; i < NB_EQUIPES ; i++){
                //on parcourt la liste des flux entrant possibles (en fonction des machines utilisées)

                try {
                    if(s.listeFluxEntrants[s.machineUtilisee[i]].ready()){

                        message = s.listeFluxEntrants[s.machineUtilisee[i]].readLine();
                        int numeroEquipe = Integer.parseInt(message);
                        s.scoreEquipes[numeroEquipe] += 1;                     //on augmente de 1 le score de l'équipe

                        if(s.scoreEquipes[numeroEquipe] == NB_SCORE_GAGNANT){

                            gagne = true;

                            message = "Partie terminée.";

                            //pour clear le terminal : "\033[H\033[2J"
                            System.out.println("\033[H\033[2J\n"+message);
                            s.envoieCollectif(message);


                            //PB : lorsqu'on envoit un message avec "\n" il envoit le message jusqu'au "\n" et ignore la suite !
                            //donc on envoit avec des '_' à la place des "\n" puis on utilise la fonction replaceAll qui permet de remplacer

                            message = "L'équipe "+s.listeCouleur.get(numeroEquipe)+" a gagnée !"+s.lesScoresDesEquipes();
                            //on l'envoit avant de le décrypter!!
                            s.envoieCollectif(message);

                            //on décrypte le message
                            message = message.replaceAll("_", "\n");
                            System.out.println(message);

                            //on envoit les scores à tous les clients



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



    public String nouvelleCombinaisonLettreCouleur(int i){

        return choisirLettreAleatoire() + ":" + String.valueOf(i);
    }



    public String lesScoresDesEquipes(){
        String lesScores = "__SCORES :_";
        //j'affiche les scores de l'équipes
        for(int i = 0 ; i < NB_EQUIPES ; i++){
            lesScores += "\tEquipe "+this.listeCouleur.get(i)+"\t-> "+this.scoreEquipes[i]+"_";
        }

        return lesScores;
    }

    public void lancementPartie(){

        System.out.println("Appuyer pour lancer la partie !");
        Scanner scan = new Scanner(System.in);
        String saisie = scan.nextLine();
    }




}