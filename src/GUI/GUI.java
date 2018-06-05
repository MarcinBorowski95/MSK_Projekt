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

    private JLabel liczbaKlientowText;
    private JLabel liczbaObsluzonychText;
    private JLabel przepustowoscText;

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
        przepustowoscText.setText(String.valueOf((float)((float)liczbaObsluzonych/liczbaKlientow)*100) + "%");

        //statystykaFederate.endSim();
    }

    private void init() {
        frame = new JFrame();
        frame.getContentPane().setLayout(null);
        frame.setResizable(false);
        frame.setSize(300, 300);
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


        liczbaKlientowText = new JLabel();
        liczbaKlientowText.setBounds(200,50,200,20);
        liczbaObsluzonychText = new JLabel();
        liczbaObsluzonychText.setBounds(200,100,200,20);
        przepustowoscText = new JLabel();
        przepustowoscText.setBounds(200,150,200,20);


        frame.add(liczbaKlientowLabel);
        frame.add(liczbaObsluzonychLabel);
        frame.add(przepustowoscLabel);

        frame.add(liczbaKlientowText);
        frame.add(liczbaObsluzonychText);
        frame.add(przepustowoscText);


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
