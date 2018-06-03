package Bankomat;

import hla.rti1516e.ObjectInstanceHandle;
//TODO fixme trzeba sie zastanowic czy tak nie powinna wygladac bazowa klasa dla obiektu, aby moć ładnie pobierać dane stąd
public class Bankomat {

    private int gotowka;
    private int iloscOsob;

    private ObjectInstanceHandle bankomatHandle;

    public Bankomat(ObjectInstanceHandle bankomatHandle) {
        this.bankomatHandle = bankomatHandle;
    }

    public Bankomat(int gotowka, int iloscOsob) {
        this.gotowka = gotowka;
        this.iloscOsob = iloscOsob;
    }

    public Bankomat(){}

    public int getGotowka() {
        return gotowka;
    }

    public void setGotowka(int gotowka) {
        this.gotowka = gotowka;
    }

    public int getIloscOsob() {
        return iloscOsob;
    }

    public void setIloscOsob(int iloscOsob) {
        this.iloscOsob = iloscOsob;
    }

    @Override
    public String toString() {
        return "Bankomat{" +
                "gotowka=" + gotowka +
                ", iloscOsob=" + iloscOsob +
                '}';
    }
}
