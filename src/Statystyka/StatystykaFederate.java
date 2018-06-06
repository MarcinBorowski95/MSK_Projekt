package Statystyka;

import hla.rti.jlc.EncodingHelpers;
import hla.rti1516e.*;
import hla.rti1516e.encoding.ByteWrapper;
import hla.rti1516e.encoding.EncoderFactory;
import hla.rti1516e.encoding.HLAfloat32BE;
import hla.rti1516e.encoding.HLAinteger16BE;
import hla.rti1516e.exceptions.*;
import hla.rti1516e.time.HLAfloat64Interval;
import hla.rti1516e.time.HLAfloat64Time;
import hla.rti1516e.time.HLAfloat64TimeFactory;
import org.portico.impl.hla1516e.types.encoding.HLA1516eEncoderFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Random;

public class StatystykaFederate {

    //Punkt Synchronizacji
    public static final String READY_TO_RUN = "ReadyToRun";

    //Główne zmienne
    private RTIambassador rtiamb;
    protected StatystykaAmbassador fedamb;
    private HLAfloat64TimeFactory timeFactory; // set when we join
    protected EncoderFactory encoderFactory;     // set when we join

    //Zmienne Bankomatu
    private final double timeStep           = 10.0;
    private int nrKlient = 0;

    public StatystykaGui statystykaGui;

    //Zmienne Handle
    protected InteractionClassHandle koniecSymulacjiHandle;
    protected InteractionClassHandle wyslijWynikiHandle;
    protected InteractionClassHandle wyslijStatystykeHandle;
    protected ParameterHandle liczbaZniecierpliwionychHandle;
    protected ParameterHandle liczbaObsluzonychKlientowHandle;
    protected ParameterHandle liczbaKlientowHandle;
    //fixme ilosc czy liczba gotowki?
    protected ParameterHandle iloscWyplaconejGotowki;
    protected ParameterHandle iloscWplaconejGotowkiHandle;
    protected ParameterHandle ileRazyObslugaZawitala;
    protected ParameterHandle przepustowosc;



    private void log( String message )
    {
        System.out.println( "StatystykaFederate   : " + message );
    }

    private void waitForUser()
    {
        log( " >>>>>>>>>> Press Enter to Continue <<<<<<<<<<" );
        BufferedReader reader = new BufferedReader( new InputStreamReader(System.in) );
        try
        {
            reader.readLine();
        }
        catch( Exception e )
        {
            log( "Error while waiting for user input: " + e.getMessage() );
            e.printStackTrace();
        }
    }

    //////////////////////////////////Główna metoda symulacji//////////////////////////////////////

    public void runFederate (String federateName) throws Exception
    {
        log("Creating RTIambassador");
        rtiamb = RtiFactoryFactory.getRtiFactory().getRtiAmbassador();
        encoderFactory = RtiFactoryFactory.getRtiFactory().getEncoderFactory();

        log( "Connecting..." );
        fedamb = new StatystykaAmbassador( this );
        rtiamb.connect( fedamb, CallbackModel.HLA_EVOKED );

        log( "Creating Federation..." );
        // We attempt to create a new federation with the first three of the
        // restaurant FOM modules covering processes, food and drink
        try
        {
            URL[] modules = new URL[]{
                    (new File("foms/MSK_Fom.xml")).toURI().toURL(),
            };
            rtiamb.createFederationExecution( "MSKProjektFederation", modules );
            log( "Created Federation from Statystyka" );

//            File fom = new File( "MSK_Projekt.fed" );
//            rtiamb.createFederationExecution( "MSKProjektFederation",
//                    fom.toURI().toURL() );
//            log( "Created Federation from Klient" );
        }
        catch( FederationExecutionAlreadyExists exists )
        {
            log( "Didn't create federation, it already existed" );
        }
        catch( MalformedURLException urle )
        {
            log( "Exception loading one of the FOM modules from disk: " + urle.getMessage() );
            urle.printStackTrace();
            return;
        }

        rtiamb.joinFederationExecution( federateName,
                "StatystykaType",
                "MSKProjektFederation");

        log("Joined Federation as: " + federateName);

        this.timeFactory = (HLAfloat64TimeFactory)rtiamb.getTimeFactory();

        rtiamb.registerFederationSynchronizationPoint( READY_TO_RUN, null );
        // wait until the point is announced
        while( fedamb.isAnnounced == false )
        {
            rtiamb.evokeMultipleCallbacks( 0.1, 0.2 );
        }
        waitForUser();

        rtiamb.synchronizationPointAchieved( READY_TO_RUN );
        log( "Achieved sync point: " +READY_TO_RUN+ ", waiting for federation..." );
        while( fedamb.isReadyToRun == false )
        {
            rtiamb.evokeMultipleCallbacks( 0.1, 0.2 );
        }

        enableTimePolicy();
        log( "Time Policy Enabled" );

        publishAndSubscribe();
        log( "Published and Subscribed" );

        this.statystykaGui = new StatystykaGui(this);

        //////////////////////////////////////MAIN SIM LOOP//////////////////////////////////////////////////////
        while (fedamb.running) {

            if (fedamb.federateTime==0)
                advanceTime( timeStep );

            /*sendPrzepustowoscInteraction();
            sendGetMoneyInteraction();
*/
            sendPrzepustowoscInteraction();
            rtiamb.evokeMultipleCallbacks( 0.1, 0.2 );
            advanceTime( timeStep );
            log( "Time Advanced to " + fedamb.federateTime );
        }
        //////////////////////////////////////////////////////////////////////////////////////////////////////////



        rtiamb.resignFederationExecution( ResignAction.DELETE_OBJECTS );
        log( "Resigned from Federation" );

        try
        {
            rtiamb.destroyFederationExecution( "ExampleFederation" );
            log( "Destroyed Federation from Statystyka" );
        }
        catch( FederationExecutionDoesNotExist dne )
        {
            log( "No need to destroy federation, it doesn't exist" );
        }
        catch( FederatesCurrentlyJoined fcj )
        {
            log( "Didn't destroy federation, federates still joined" );
        }

    }

    private void sendPrzepustowoscInteraction() throws RTIexception {
        ParameterHandleValueMap parameters = rtiamb.getParameterHandleValueMapFactory().create(0);
        HLAfloat64Time time = timeFactory.makeTime( fedamb.federateTime+fedamb.federateLookahead );


        HLAfloat32BE przepustowoscFloat = encoderFactory.createHLAfloat32BE(statystykaGui.getPrzepustowosc());


        parameters.put(przepustowosc, przepustowoscFloat.toByteArray());

        log("Sending Przepustowosc: " +statystykaGui.getPrzepustowosc());
        rtiamb.sendInteraction( wyslijStatystykeHandle, parameters, generateTag(), time );
    }

    //////////////////////////////////////Metody Pomocnicze//////////////////////////////////////////////



    private void enableTimePolicy() throws Exception
    {
        HLAfloat64Interval lookahead = timeFactory.makeInterval( fedamb.federateLookahead );

        this.rtiamb.enableTimeRegulation( lookahead );

        // tick until we get the callback
        while( fedamb.isRegulating == false )
        {
            rtiamb.evokeMultipleCallbacks( 0.1, 0.2 );
        }

        this.rtiamb.enableTimeConstrained();

        // tick until we get the callback
        while( fedamb.isConstrained == false )
        {
            rtiamb.evokeMultipleCallbacks( 0.1, 0.2 );
        }
    }

    private void publishAndSubscribe() throws RTIexception {
        koniecSymulacjiHandle = rtiamb.getInteractionClassHandle("InteractionRoot.koniecSymulacji");
        rtiamb.subscribeInteractionClass(koniecSymulacjiHandle);

        wyslijWynikiHandle = rtiamb.getInteractionClassHandle("InteractionRoot.WyslijWyniki");
        liczbaObsluzonychKlientowHandle = rtiamb.getParameterHandle(wyslijWynikiHandle,"liczbaObsluzonychKlientow");
        liczbaKlientowHandle = rtiamb.getParameterHandle(wyslijWynikiHandle,"liczbaKlientow");
        /*liczbaZniecierpliwionychHandle = rtiamb.getParameterHandle(wyslijWynikiHandle,"liczbaZniecierpliwionychKlientow");
        iloscWplaconejGotowkiHandle = rtiamb.getParameterHandle(wyslijWynikiHandle,"iloscWplaconejGotowki");
        iloscWyplaconejGotowki = rtiamb.getParameterHandle(wyslijWynikiHandle,"iloscWyplaconejGotowki");
        ileRazyObslugaZawitala = rtiamb.getParameterHandle(wyslijWynikiHandle,"ileRazyObslugaZawitala");*/
        rtiamb.subscribeInteractionClass(wyslijWynikiHandle);

        wyslijStatystykeHandle = rtiamb.getInteractionClassHandle("InteractionRoot.WyslijStatystyke");
        przepustowosc = rtiamb.getParameterHandle(wyslijStatystykeHandle,"Przepustowosc");
        rtiamb.publishInteractionClass(wyslijStatystykeHandle);
    }


    private void advanceTime( double timestep ) throws RTIexception
    {
        // request the advance
        fedamb.isAdvancing = true;
        HLAfloat64Time time = timeFactory.makeTime( fedamb.federateTime + timestep );
        rtiamb.timeAdvanceRequest( time );

        // wait for the time advance to be granted. ticking will tell the
        // LRC to start delivering callbacks to the federate
        while( fedamb.isAdvancing )
        {
            rtiamb.evokeMultipleCallbacks( 0.1, 0.2 );
        }
    }


    private short getTimeAsShort()
    {
        return (short)fedamb.federateTime;
    }

    private byte[] generateTag()
    {
        return ("(timestamp) "+System.currentTimeMillis()).getBytes();
    }

    public static void main( String[] args )
    {
        String federateName = "StatystykaFederate";
        if( args.length != 0 )
        {
            federateName = args[0];
        }

        try
        {
            new StatystykaFederate().runFederate( federateName );
        }
        catch( Exception rtie )
        {
            rtie.printStackTrace();
        }
    }
}
