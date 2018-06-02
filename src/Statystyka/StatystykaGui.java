package Statystyka;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class StatystykaGui {

    public static StatystykaFederate statystykaFederate;
    private JFrame frame;

    private JLabel liczbaKlientowLabel;
    private JLabel liczbaZniecierpliwionychLabel;
    private JLabel przepustowoscLabel;

    private JLabel liczbaKlientowText;
    private JLabel liczbaZniecierpliwionychText;
    private JLabel przepustowoscText;

    public StatystykaGui() {
        init();
    }

    public StatystykaGui (StatystykaFederate federate){
        this.statystykaFederate = federate;
        init();
    }


    public void setStats(int liczbaKlientow, int liczbaObsluzonych){
        liczbaKlientowText.setText(String.valueOf(liczbaKlientow));
        liczbaZniecierpliwionychText.setText(String.valueOf(liczbaObsluzonych));
        przepustowoscText.setText(String.valueOf((float)((float)liczbaObsluzonych/liczbaKlientow)*100) + "%");

        //statystykaFederate.endSim();
    }

    private void init() {
        frame = new JFrame();
        frame.getContentPane().setLayout(null);
        frame.setResizable(false);
        frame.setSize(300, 300);
        frame.setTitle("Stats");

        liczbaKlientowLabel = new JLabel();
        liczbaKlientowLabel.setBounds(25,50,200,20);
        liczbaKlientowLabel.setText("Liczba klientów");

        liczbaZniecierpliwionychLabel = new JLabel();
        liczbaZniecierpliwionychLabel.setBounds(25,100,200,20);
        liczbaZniecierpliwionychLabel.setText("Liczba obsłużonych");

        przepustowoscLabel = new JLabel();
        przepustowoscLabel.setBounds(25,150,200,20);
        przepustowoscLabel.setText("Przepustowość");


        liczbaKlientowText = new JLabel();
        liczbaKlientowText.setBounds(200,50,200,20);
        liczbaZniecierpliwionychText = new JLabel();
        liczbaZniecierpliwionychText.setBounds(200,100,200,20);
        przepustowoscText = new JLabel();
        przepustowoscText.setBounds(200,150,200,20);


        frame.add(liczbaKlientowLabel);
        frame.add(liczbaZniecierpliwionychLabel);
        frame.add(przepustowoscLabel);

        frame.add(liczbaKlientowText);
        frame.add(liczbaZniecierpliwionychText);
        frame.add(przepustowoscText);


        frame.setVisible(true);

        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                try{
                    statystykaFederate.fedamb.running=false;

                } catch (NullPointerException ex){

                }

                frame.dispose();

            }
        });
    }

    public static void run(StatystykaFederate federat) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {

                    StatystykaGui statystykaGui = new StatystykaGui(federat);
                    statystykaGui.frame.setVisible(true);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}