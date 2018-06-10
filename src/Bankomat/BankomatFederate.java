package Bankomat;

import hla.rti1516e.*;
import hla.rti1516e.encoding.EncoderFactory;
import hla.rti1516e.encoding.HLAinteger16BE;
import hla.rti1516e.encoding.HLAinteger32BE;
import hla.rti1516e.exceptions.FederatesCurrentlyJoined;
import hla.rti1516e.exceptions.FederationExecutionAlreadyExists;
import hla.rti1516e.exceptions.FederationExecutionDoesNotExist;
import hla.rti1516e.exceptions.RTIexception;
import hla.rti1516e.time.HLAfloat64Interval;
import hla.rti1516e.time.HLAfloat64Time;
import hla.rti1516e.time.HLAfloat64TimeFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;


public class BankomatFederate {

    //Punkt Synchronizacji
    public static final String READY_TO_RUN = "ReadyToRun";

    //Główne zmienne
    private RTIambassador rtiamb;
    private BankomatAmbassador fedamb;
    private HLAfloat64TimeFactory timeFactory; // set when we join
    protected EncoderFactory encoderFactory;     // set when we join

    //Zmienne Bankomatu
    private final double timeStep = 1.0;
    private int stock = 10000;
    private int queue = 0;
    private Boolean working = true;
    private int klienci = 0;
    private int obsluzeniKlienci=0;
    private int obslugaWizytacja = 0;

    //Zmienne Handle
    private ObjectClassHandle bankomatHandle;
    private AttributeHandle stockHandle;
    private AttributeHandle queueHandle;
    private AttributeHandle workingHandle;
    protected InteractionClassHandle addMoneyHandle;
    protected InteractionClassHandle getMoneyHandle;
    protected InteractionClassHandle addClientHandle;
    protected InteractionClassHandle clientLeaveHandle;
    protected InteractionClassHandle stopWorkingHandle;
    protected InteractionClassHandle informNoMoneyHandle;
    protected InteractionClassHandle wyslijWynikiHandle;
    protected ParameterHandle addQuantityHandle;
    protected ParameterHandle getQuantityHandle;
    protected ParameterHandle liczbaKlientow;
    protected ParameterHandle liczbaObsluzonychKlientow;
    protected ParameterHandle wizytaObslugi;


    private void log(String message) {
        System.out.println("BankomatFederate   : " + message);
    }

    private void waitForUser() {
        log(" >>>>>>>>>> Press Enter to Continue <<<<<<<<<<");
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        try {
            reader.readLine();
        } catch (Exception e) {
            log("Error while waiting for user input: " + e.getMessage());
            e.printStackTrace();
        }
    }

    //////////////////////////////////Główna metoda symulacji//////////////////////////////////////

    public void runFederate(String federateName) throws Exception {
        log("Creating RTIambassador");
        rtiamb = RtiFactoryFactory.getRtiFactory().getRtiAmbassador();
        encoderFactory = RtiFactoryFactory.getRtiFactory().getEncoderFactory();

        log("Connecting...");
        fedamb = new BankomatAmbassador(this);
        rtiamb.connect(fedamb, CallbackModel.HLA_EVOKED);

        log("Creating Federation...");
        // We attempt to create a new federation with the first three of the
        // restaurant FOM modules covering processes, food and drink
        try {
            URL[] modules = new URL[]{
                    (new File("foms/MSK_Fom.xml")).toURI().toURL(),
            };
            rtiamb.createFederationExecution("MSKProjektFederation", modules);
            log("Created Bankomat");

//            File fom = new File( "MSK_Projekt.fed" );
//            rtiamb.createFederationExecution( "MSKProjektFederation",
//                    fom.toURI().toURL() );
//            log( "Created Federation from MSK_Fom.xml" );
        } catch (FederationExecutionAlreadyExists exists) {
            log("Didn't create federation, it already existed");
        } catch (MalformedURLException urle) {
            log("Exception loading one of the FOM modules from disk: " + urle.getMessage());
            urle.printStackTrace();
            return;
        }

        rtiamb.joinFederationExecution(federateName,
                "BankomatType",
                "MSKProjektFederation");

        log("Joined Federation as: " + federateName);

        this.timeFactory = (HLAfloat64TimeFactory) rtiamb.getTimeFactory();

        rtiamb.registerFederationSynchronizationPoint(READY_TO_RUN, null);
        // wait until the point is announced
        while (fedamb.isAnnounced == false) {
            rtiamb.evokeMultipleCallbacks(0.1, 0.2);
        }
        waitForUser();

        rtiamb.synchronizationPointAchieved(READY_TO_RUN);
        log("Achieved sync point: " + READY_TO_RUN + ", waiting for federation...");
        while (fedamb.isReadyToRun == false) {
            rtiamb.evokeMultipleCallbacks(0.1, 0.2);
        }


        enableTimePolicy();
        log("Time Policy Enabled");


        publishAndSubscribe();
        log("Published and Subscribed");


        ObjectInstanceHandle objectHandle = registerObject();
        log("Registered Object, handle=" + objectHandle);

        //////////////////////////////////////MAIN SIM LOOP//////////////////////////////////////////////////////
        while (fedamb.running) {

            if (fedamb.externalEvents.size() > 0) {
                fedamb.externalEvents.sort(new BankomatExternalEvent.ExternalEventComparator());
                for (BankomatExternalEvent externalEvent : fedamb.externalEvents) {
                    fedamb.federateTime = externalEvent.getTime();
                    switch (externalEvent.getEventType()) {
                        case AddClient:
                            this.addToQueue();
                            break;

                        case ClientLeave:
                            this.removeFromQueue();
                            break;


                        case GetMoney:
                            if (working) {
                                this.getFromStock(externalEvent.getQty());
                            }
                            break;

                        case StopWorking:
                            this.stopBankomat();
                            log("working? " + this.working);
                            break;

                        case AddMoney:
                            this.startBankomat();
                            this.addToStock(externalEvent.getQty());
                            log("working? " + this.working);
                            break;

                        case ClientImpatience:
                            this.removeFromQueue();
                            break;
                    }
                }
                fedamb.externalEvents.clear();
            }

            rtiamb.evokeMultipleCallbacks(0.1, 0.2);
            advanceTime(timeStep);
            log("Time Advanced to " + fedamb.federateTime);
        }
        //////////////////////////////////////////////////////////////////////////////////////////////////////////

        deleteObject(objectHandle);
        log("Deleted Object, handle=" + objectHandle);

        rtiamb.resignFederationExecution(ResignAction.DELETE_OBJECTS);
        log("Resigned from Federation");

        try {
            rtiamb.destroyFederationExecution("ExampleFederation");
            log("Destroyed Federation from Bankomat");
        } catch (FederationExecutionDoesNotExist dne) {
            log("No need to destroy federation, it doesn't exist");
        } catch (FederatesCurrentlyJoined fcj) {
            log("Didn't destroy federation, federates still joined");
        }

    }

    //////////////////////////////////////Metody Pomocnicze//////////////////////////////////////////////

    public void addToStock(int qty) {
        this.stock += qty;
        log("Added " + qty + " at time: " + fedamb.federateTime + ", current stock: " + this.stock);
    }

    public void getFromStock(int qty) throws RTIexception {
        if (this.stock - qty < 0) {
            log("Not enough money at stock");
            // Wysłanie interakcji do obsługi że trzeba przyjść uzupełnić stock
            sendNoMoneyInteraction();
            stopBankomat();
        } else {
            ++this.obsluzeniKlienci;
            this.stock -= qty;
            log("Removed " + qty + " at time: " + fedamb.federateTime + ", current stock: " + this.stock);
            this.removeFromQueue();
        }
    }

    public void addToQueue() throws RTIexception {
        ++this.queue;
        ++this.klienci;
        sendStatystykiInteraction();
        log("Added to queue, at time: " + fedamb.federateTime + ", current queue: " + this.queue);
    }

    public void removeFromQueue() {
        --this.queue;

        log("Removed from queue, at time: " + fedamb.federateTime + ", current queue: " + this.queue);
    }

    public void startBankomat() {
        this.working = true;
        ++this.obslugaWizytacja;
        log("obsluga ilosc osob: " + obslugaWizytacja);
        log("Bankomat is working again");
    }

    public void stopBankomat() {
        this.working = false;
        log("Bankomat is out of money");
    }

    private void enableTimePolicy() throws Exception {
        HLAfloat64Interval lookahead = timeFactory.makeInterval(fedamb.federateLookahead);

        this.rtiamb.enableTimeRegulation(lookahead);

        // tick until we get the callback
        while (fedamb.isRegulating == false) {
            rtiamb.evokeMultipleCallbacks(0.1, 0.2);
        }

        this.rtiamb.enableTimeConstrained();

        // tick until we get the callback
        while (fedamb.isConstrained == false) {
            rtiamb.evokeMultipleCallbacks(0.1, 0.2);
        }
    }

    private void publishAndSubscribe() throws RTIexception {

        this.bankomatHandle = rtiamb.getObjectClassHandle("ObjectRoot.Bankomat");
        this.stockHandle = rtiamb.getAttributeHandle(bankomatHandle, "stock");
        this.queueHandle = rtiamb.getAttributeHandle(bankomatHandle, "queue");
        this.workingHandle = rtiamb.getAttributeHandle(bankomatHandle, "working");

        AttributeHandleSet attributes = rtiamb.getAttributeHandleSetFactory().create();
        attributes.add(stockHandle);
        attributes.add(queueHandle);
        attributes.add(workingHandle);

        rtiamb.publishObjectClassAttributes(bankomatHandle, attributes);
        rtiamb.subscribeObjectClassAttributes(bankomatHandle, attributes);

        addMoneyHandle = rtiamb.getInteractionClassHandle("InteractionRoot.AddMoney");
        addQuantityHandle = rtiamb.getParameterHandle(this.addMoneyHandle, "Quantity");
        fedamb.addMoneyHandle = addMoneyHandle;
        rtiamb.subscribeInteractionClass(addMoneyHandle);

        getMoneyHandle = rtiamb.getInteractionClassHandle("InteractionRoot.GetMoney");
        getQuantityHandle = rtiamb.getParameterHandle(this.getMoneyHandle, "Quantity");
        fedamb.getMoneyHandle = getMoneyHandle;
        rtiamb.subscribeInteractionClass(getMoneyHandle);

        addClientHandle = rtiamb.getInteractionClassHandle("InteractionRoot.AddClient");
        fedamb.addClientHandle = addClientHandle;
        rtiamb.subscribeInteractionClass(addClientHandle);

        clientLeaveHandle = rtiamb.getInteractionClassHandle("InteractionRoot.clientLeave");
        fedamb.clientLeaveHandle = clientLeaveHandle;
        rtiamb.subscribeInteractionClass(clientLeaveHandle);

        stopWorkingHandle = rtiamb.getInteractionClassHandle("InteractionRoot.StopWorking");
        fedamb.stopWorkingHandle = stopWorkingHandle;
        rtiamb.publishInteractionClass(stopWorkingHandle);

        informNoMoneyHandle = rtiamb.getInteractionClassHandle("InteractionRoot.InformNoMoney");
        fedamb.informNoMoneyHandle = informNoMoneyHandle;
        rtiamb.publishInteractionClass(informNoMoneyHandle);

        wyslijWynikiHandle = rtiamb.getInteractionClassHandle("InteractionRoot.WyslijWyniki");
        liczbaKlientow = rtiamb.getParameterHandle(this.wyslijWynikiHandle, "liczbaKlientow");
        liczbaObsluzonychKlientow = rtiamb.getParameterHandle(this.wyslijWynikiHandle, "liczbaObsluzonychKlientow");
        wizytaObslugi = rtiamb.getParameterHandle(this.wyslijWynikiHandle, "ileRazyObslugaZawitala");
        rtiamb.publishInteractionClass(wyslijWynikiHandle);
    }

    private ObjectInstanceHandle registerObject() throws RTIexception {
        return rtiamb.registerObjectInstance(bankomatHandle);
    }

    private void updateAttributeValues(ObjectInstanceHandle objectHandle) throws RTIexception {
        AttributeHandleValueMap attributes = rtiamb.getAttributeHandleValueMapFactory().create(2);

        HLAinteger16BE stockValue = encoderFactory.createHLAinteger16BE(getTimeAsShort());
        attributes.put(stockHandle, stockValue.toByteArray());
        HLAinteger16BE queueValue = encoderFactory.createHLAinteger16BE(getTimeAsShort());
        attributes.put(queueHandle, queueValue.toByteArray());
        HLAinteger16BE workingValue = encoderFactory.createHLAinteger16BE(getTimeAsShort());
        attributes.put(workingHandle, workingValue.toByteArray());

        HLAfloat64Time time = timeFactory.makeTime(fedamb.federateTime + fedamb.federateLookahead);
        rtiamb.updateAttributeValues(objectHandle, attributes, generateTag(), time);
    }

    private void sendNoMoneyInteraction() throws RTIexception {
        //////////////////////////////Wysłanie z czasem////////////////////////////////////////////
        ParameterHandleValueMap parameters = rtiamb.getParameterHandleValueMapFactory().create(0);
        HLAfloat64Time time = timeFactory.makeTime(fedamb.federateTime + fedamb.federateLookahead);
        rtiamb.sendInteraction(informNoMoneyHandle, parameters, generateTag(), time);
    }

    private void sendStatystykiInteraction() throws RTIexception {
        ParameterHandleValueMap parameters = rtiamb.getParameterHandleValueMapFactory().create(0);
        HLAfloat64Time time = timeFactory.makeTime(fedamb.federateTime + fedamb.federateLookahead);
        HLAinteger32BE klienci = encoderFactory.createHLAinteger32BE( this.klienci );
        HLAinteger32BE obsluzeniKlienci = encoderFactory.createHLAinteger32BE( this.obsluzeniKlienci );
        HLAinteger32BE obslugaWizyta = encoderFactory.createHLAinteger32BE(this.obslugaWizytacja);
        parameters.put(liczbaObsluzonychKlientow, obsluzeniKlienci.toByteArray());
        parameters.put(liczbaKlientow, klienci.toByteArray());
        parameters.put(wizytaObslugi,obslugaWizyta.toByteArray());
        rtiamb.sendInteraction(wyslijWynikiHandle, parameters, generateTag(), time);
    }

    private void advanceTime(double timestep) throws RTIexception {
        // request the advance
        fedamb.isAdvancing = true;
        HLAfloat64Time time = timeFactory.makeTime(fedamb.federateTime + timestep);
        rtiamb.timeAdvanceRequest(time);

        // wait for the time advance to be granted. ticking will tell the
        // LRC to start delivering callbacks to the federate
        while (fedamb.isAdvancing) {
            rtiamb.evokeMultipleCallbacks(0.1, 0.2);
        }
    }

    private void deleteObject(ObjectInstanceHandle handle) throws RTIexception {
        rtiamb.deleteObjectInstance(handle, generateTag());
    }

    private short getTimeAsShort() {
        return (short) fedamb.federateTime;
    }

    private byte[] generateTag() {
        return ("(timestamp) " + System.currentTimeMillis()).getBytes();
    }

    public static void main(String[] args) {
        String federateName = "BankomatFederate";
        if (args.length != 0) {
            federateName = args[0];
        }

        try {
            new BankomatFederate().runFederate(federateName);
        } catch (Exception rtie) {
            rtie.printStackTrace();
        }
    }
}
