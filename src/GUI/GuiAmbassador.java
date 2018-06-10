package GUI;

import Statystyka.StatystykaFederate;
import hla.rti.jlc.EncodingHelpers;
import hla.rti1516e.*;
import hla.rti1516e.exceptions.FederateInternalError;
import hla.rti1516e.time.HLAfloat64Time;

public class GuiAmbassador extends NullFederateAmbassador {


    //----------------------------------------------------------
    //                   INSTANCE VARIABLES
    //----------------------------------------------------------
    private GuiFederate federate;

    // these variables are accessible in the package
    protected double federateTime = 0.0;
    protected double federateLookahead = 1.0;

    protected boolean isRegulating = false;
    protected boolean isConstrained = false;
    protected boolean isAdvancing = false;

    protected boolean isAnnounced = false;
    protected boolean isReadyToRun = false;

    protected boolean running = true;

    public InteractionClassHandle wyslijWynikiHandle;
    public InteractionClassHandle StopWorkingHandle;
    public InteractionClassHandle addMoneyHandle;
    public InteractionClassHandle wyslijStatystyke;

    //----------------------------------------------------------

    public GuiAmbassador(GuiFederate federate) {
        this.federate = federate;
    }

    private void log(String message) {
        System.out.println("GuiAmbassador: " + message);
    }

    @Override
    public void synchronizationPointRegistrationFailed(String label,
                                                       SynchronizationPointFailureReason reason) {
        log("Failed to register sync point: " + label + ", reason=" + reason);
    }

    @Override
    public void synchronizationPointRegistrationSucceeded(String label) {
        log("Successfully registered sync point: " + label);
    }

    @Override
    public void announceSynchronizationPoint(String label, byte[] tag) {
        log("Synchronization point announced: " + label);
        if (label.equals(StatystykaFederate.READY_TO_RUN))
            this.isAnnounced = true;
    }

    @Override
    public void federationSynchronized(String label, FederateHandleSet failed) {
        log("Federation Synchronized: " + label);
        if (label.equals(StatystykaFederate.READY_TO_RUN))
            this.isReadyToRun = true;
    }

    /**
     * The RTI has informed us that time regulation is now enabled.
     */
    @Override
    public void timeRegulationEnabled(LogicalTime time) {
        this.federateTime = ((HLAfloat64Time) time).getValue();
        this.isRegulating = true;
    }

    @Override
    public void timeConstrainedEnabled(LogicalTime time) {
        this.federateTime = ((HLAfloat64Time) time).getValue();
        this.isConstrained = true;
    }

    @Override
    public void timeAdvanceGrant(LogicalTime time) {
        this.federateTime = ((HLAfloat64Time) time).getValue();
        this.isAdvancing = false;
    }

    @Override
    public void discoverObjectInstance(ObjectInstanceHandle theObject,
                                       ObjectClassHandle theObjectClass,
                                       String objectName)
            throws FederateInternalError {
        log("Discoverd Object: handle=" + theObject + ", classHandle=" +
                theObjectClass + ", name=" + objectName);
    }

    @Override
    public void removeObjectInstance(ObjectInstanceHandle theObject,
                                     byte[] tag,
                                     OrderType sentOrdering,
                                     SupplementalRemoveInfo removeInfo)
            throws FederateInternalError {
        log("Object Removed: handle=" + theObject);
    }


    @Override
    public void reflectAttributeValues(ObjectInstanceHandle theObject,
                                       AttributeHandleValueMap theAttributes,
                                       byte[] tag,
                                       OrderType sentOrder,
                                       TransportationTypeHandle transport,
                                       SupplementalReflectInfo reflectInfo)
            throws FederateInternalError {
        // just pass it on to the other method for printing purposes
        // passing null as the time will let the other method know it
        // it from us, not from the RTI
        reflectAttributeValues(theObject,
                theAttributes,
                tag,
                sentOrder,
                transport,
                null,
                sentOrder,
                reflectInfo);
    }

    @Override
    public void reflectAttributeValues(ObjectInstanceHandle theObject,
                                       AttributeHandleValueMap theAttributes,
                                       byte[] tag,
                                       OrderType sentOrdering,
                                       TransportationTypeHandle theTransport,
                                       LogicalTime time,
                                       OrderType receivedOrdering,
                                       SupplementalReflectInfo reflectInfo)
            throws FederateInternalError {
        //na nikogo nie subuje
    }

    @Override
    public void receiveInteraction(InteractionClassHandle interactionClass,
                                   ParameterHandleValueMap theParameters,
                                   byte[] tag,
                                   OrderType sentOrdering,
                                   TransportationTypeHandle theTransport,
                                   SupplementalReceiveInfo receiveInfo)
            throws FederateInternalError {
        // just pass it on to the other method for printing purposes
        // passing null as the time will let the other method know it
        // it from us, not from the RTI
        this.receiveInteraction(interactionClass,
                theParameters,
                tag,
                sentOrdering,
                theTransport,
                null,
                sentOrdering,
                receiveInfo);
    }

    @Override
    public void receiveInteraction(InteractionClassHandle interactionClass,
                                   ParameterHandleValueMap theParameters,
                                   byte[] tag,
                                   OrderType sentOrdering,
                                   TransportationTypeHandle theTransport,
                                   LogicalTime time,
                                   OrderType receivedOrdering,
                                   SupplementalReceiveInfo receiveInfo)
            throws FederateInternalError {
        //odebranie interakcji od gui o info
        StringBuilder builder = new StringBuilder("Interaction Received:");

        builder.append(" handle=" + interactionClass);

      /*  if (interactionClass.equals(federate.wyslijWynikiHandle)) {
            int liczbaKlientow = EncodingHelpers.decodeInt(theParameters.get(federate.liczbaKlientowHandle));
            int liczbaObsluzonychKlientow = EncodingHelpers.decodeInt(theParameters.get(federate.liczbaObsluzonychKlientowHandle));
            //  int liczbaZniecierpliwionych = EncodingHelpers.decodeInt(theParameters.get(federate.liczbaZniecierpliwionychHandle));
          *//*  int iloscWyplaconejGotowki = EncodingHelpers.decodeInt(theParameters.get(federate.iloscWyplaconejGotowki));
            int iloscWplaconejGotowki = EncodingHelpers.decodeInt(theParameters.get(federate.iloscWplaconejGotowkiHandle));
            int ileRazyObsluga = EncodingHelpers.decodeInt(theParameters.get(federate.ileRazyObslugaZawitala));*//*

            federate.gui.setStats(liczbaKlientow,liczbaObsluzonychKlientow);
        }*/
        if(interactionClass.equals(federate.addMoneyHandle)){
            federate.gui.setStan(true,false);
        }
        if(interactionClass.equals(federate.StopWorkingHandle)){
            federate.gui.setStan(false,true);
        }
        if(interactionClass.equals(federate.wyslijStatystyke)){
            float przepustowosc = EncodingHelpers.decodeFloat(theParameters.get(federate.przepustowosc));
            int liczbaKlientow = EncodingHelpers.decodeInt(theParameters.get(federate.liczbaKlientowToSentHandle));
            int liczbaObsluzonych = EncodingHelpers.decodeInt(theParameters.get(federate.liczbaObsluzonychKlientowToSentHandle));
            int obsluga = EncodingHelpers.decodeInt(theParameters.get(federate.obslugaToSent));
            federate.gui.setPrzepustowosc(przepustowosc);
            federate.gui.setStats(liczbaKlientow,liczbaObsluzonych);
            federate.gui.setObsluga(obsluga);
        }


 /*       if( interactionClass.equals(federate.koniecSymulacjiHandle) )
        {
            System.out.println("Po co mi koniec symulacji? XD");
        }
       *//*

            //TODO stats?
            federate.statystykaGui.setStats(liczbaKlientow, liczbaObsluzonychKlientow);

            System.out.println("Klienci: " + liczbaKlientow);
            System.out.println(" Obsluzeni klienci: " + liczbaObsluzonychKlientow);
           *//* System.out.println("Zniecierpliwieni Klienci: " + liczbaZniecierpliwionych);
            System.out.println("Wplacona gotówka: " + iloscWplaconejGotowki);
            System.out.println("Wypłacona gotówka: " + iloscWyplaconejGotowki);
            System.out.println("Obsluga: " + ileRazyObsluga);*//*

        }*/

            builder.append(", tag=" + new String(tag) + ", time=" + ((HLAfloat64Time) time).getValue());

            log(builder.toString());

    }
}