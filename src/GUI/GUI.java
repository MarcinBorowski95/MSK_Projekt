package GUI;

import Statystyka.StatystykaFederate;
import Statystyka.StatystykaGui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class GUI {
    public static GuiFederate guiFederate;
    private JFrame frame;

    private JLabel liczbaKlientowLabel;
    private JLabel liczbaObsluzonychLabel;
    private JLabel przepustowoscLabel;
    private JLabel bankomatStanLabel;
    private JLabel obslugaStanLabel;
    private JLabel obslugaWizytyLabel;
    private JLabel aktualnaKolejkaLabel;
    private JLabel liczbaZniecierpliwionychLabel;

    private JLabel liczbaKlientowText;
    private JLabel liczbaObsluzonychText;
    private JLabel przepustowoscText;
    private JLabel bankomatStanText;
    private JLabel obslugaStanText;
    private JLabel obslugaWizytyText;
    private JLabel aktualnaKolejkaText;
    private JLabel liczbaZniecierpliwionychText;

    public GUI() {
        init();
    }

    public GUI (GuiFederate federate){
        this.guiFederate = federate;
        init();
    }


    public void setStats(int liczbaKlientow, int liczbaObsluzonych){
        liczbaKlientowText.setText(String.valueOf(liczbaKlientow));
        liczbaObsluzonychText.setText(String.valueOf(liczbaObsluzonych));


        //statystykaFederate.endSim();
    }

    public void setStan(boolean bankomatWorking,boolean obsluga){
        bankomatStanText.setText(String.valueOf(bankomatWorking));
        obslugaStanText.setText(String.valueOf(obsluga));
    }

    public void setPrzepustowosc(float przepustowosc){
        przepustowoscText.setText(String.valueOf(przepustowosc) + "%");
    }

    public void setObsluga(int obsluga){
        obslugaWizytyText.setText(String.valueOf(obsluga));
    }

    public void setAktualnaKolejka(int que){
        aktualnaKolejkaText.setText(String.valueOf(que));
    }

    public void setLiczbaZniecier(int i){
        liczbaZniecierpliwionychText.setText(String.valueOf(i));
    }



    private void init() {
        frame = new JFrame();
        frame.getContentPane().setLayout(null);
        frame.setResizable(false);
        frame.setSize(500, 600);
        frame.setTitle("GUI");

        liczbaKlientowLabel = new JLabel();
        liczbaKlientowLabel.setBounds(25,50,200,20);
        liczbaKlientowLabel.setText("Liczba klientów");

        liczbaObsluzonychLabel = new JLabel();
        liczbaObsluzonychLabel.setBounds(25,100,200,20);
        liczbaObsluzonychLabel.setText("Liczba obsłużonych");

        przepustowoscLabel = new JLabel();
        przepustowoscLabel.setBounds(25,150,200,20);
        przepustowoscLabel.setText("Przepustowość");

        aktualnaKolejkaLabel = new JLabel();
        aktualnaKolejkaLabel.setBounds(25,200,200,20);
        aktualnaKolejkaLabel.setText("Aktualna kolejka: ");

        bankomatStanLabel = new JLabel();
        bankomatStanLabel.setBounds(25,250,200,20);
        bankomatStanLabel.setText("Stan Bankomatu");

        obslugaStanLabel = new JLabel();
        obslugaStanLabel.setBounds(25,300,200,20);
        obslugaStanLabel.setText("Stan Obslugi");

        obslugaWizytyLabel = new JLabel();
        obslugaWizytyLabel.setBounds(25,350,200,20);
        obslugaWizytyLabel.setText("Ile wizyt obslugi");

        liczbaZniecierpliwionychLabel = new JLabel();
        liczbaZniecierpliwionychLabel.setBounds(25,400,200,20);
        liczbaZniecierpliwionychLabel.setText("Liczba zniecierpliwionych: ");




        liczbaKlientowText = new JLabel();
        liczbaKlientowText.setBounds(200,50,200,20);
        liczbaObsluzonychText = new JLabel();
        liczbaObsluzonychText.setBounds(200,100,200,20);
        przepustowoscText = new JLabel();
        przepustowoscText.setBounds(200,150,200,20);
        aktualnaKolejkaText = new JLabel();
        aktualnaKolejkaText.setBounds(200,200,200,20);
        bankomatStanText = new JLabel();
        bankomatStanText.setBounds(200,250,200,20);
        obslugaStanText = new JLabel();
        obslugaStanText.setBounds(200,300,200,20);
        obslugaWizytyText = new JLabel();
        obslugaWizytyText.setBounds(200,350,200,20);
        liczbaZniecierpliwionychText = new JLabel();
        liczbaZniecierpliwionychText.setBounds(200,400,200,20);


        bankomatStanText.setText(String.valueOf(true));
        obslugaStanText.setText(String.valueOf(false));


        frame.add(liczbaKlientowLabel);
        frame.add(liczbaObsluzonychLabel);
        frame.add(przepustowoscLabel);
        frame.add(bankomatStanLabel);
        frame.add(obslugaStanLabel);
        frame.add(obslugaWizytyLabel);
        frame.add(aktualnaKolejkaLabel);
        frame.add(liczbaZniecierpliwionychLabel);

        frame.add(liczbaKlientowText);
        frame.add(liczbaObsluzonychText);
        frame.add(przepustowoscText);
        frame.add(bankomatStanText);
        frame.add(obslugaStanText);
        frame.add(obslugaWizytyText);
        frame.add(aktualnaKolejkaText);
        frame.add(liczbaZniecierpliwionychText);


        frame.setVisible(true);

        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                try{
                    guiFederate.fedamb.running=false;

                } catch (NullPointerException ex){

                }

                frame.dispose();

            }
        });
    }

    public static void run(GuiFederate federat) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {

                    GUI gui = new GUI(federat);
                    gui.frame.setVisible(true);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
