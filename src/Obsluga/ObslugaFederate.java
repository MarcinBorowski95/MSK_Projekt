package Obsluga;

import hla.rti.jlc.EncodingHelpers;
import hla.rti1516e.*;
import hla.rti1516e.encoding.EncoderFactory;
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
import java.util.Random;

public class ObslugaFederate {

    //Punkt Synchronizacji
    public static final String READY_TO_RUN = "ReadyToRun";

    //Główne zmienne
    private RTIambassador rtiamb;
    private ObslugaAmbassador fedamb;
    private HLAfloat64TimeFactory timeFactory; // set when we join
    protected EncoderFactory encoderFactory;     // set when we join

    //Zmienne Bankomatu
    private final double timeStep           = 1.0;

    //Zmienne Handle
    protected InteractionClassHandle addMoneyHandle;
    protected InteractionClassHandle StopWorkingHandle;
    protected InteractionClassHandle NoMoneyHandle;
    protected ParameterHandle quantityHandle;



    private void log( String message )
    {
        System.out.println( "ObslugaFederate   : " + message );
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
        fedamb = new ObslugaAmbassador( this );
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
            log( "Created Federation from Obsluga" );
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
                "ObslugaType",
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


        //////////////////////////////////////MAIN SIM LOOP//////////////////////////////////////////////////////
        while (fedamb.running) {

            if (fedamb.federateTime==0)
                advanceTime( timeStep );

            if(fedamb.externalEvents.size() > 0) {
                fedamb.externalEvents.sort(new ObslugaExternalEvent.ExternalEventComparator());
                for(ObslugaExternalEvent externalEvent : fedamb.externalEvents) {
                    fedamb.federateTime = externalEvent.getTime();
                    switch (externalEvent.getEventType()) {
                        case NoMoney:
                            sendObslStartInteraction();
                            advanceTime( 50);
                            sendObslStopInteraction();
                            break;
                    }
                }
                fedamb.externalEvents.clear();
            }
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
            log( "Destroyed Federation from Obsluga" );
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

    //////////////////////////////////////Metody Pomocnicze//////////////////////////////////////////////

    private void sendObslStartInteraction() throws RTIexception
    {
        ParameterHandleValueMap parameters = rtiamb.getParameterHandleValueMapFactory().create(0);
        HLAfloat64Time time = timeFactory.makeTime( fedamb.federateTime+fedamb.federateLookahead );

        log("Obsluga Start Working / Bankomat Stop Working");
        rtiamb.sendInteraction( StopWorkingHandle, parameters, generateTag(), time );
    }

    private void sendObslStopInteraction() throws RTIexception {
        ParameterHandleValueMap parameters = rtiamb.getParameterHandleValueMapFactory().create(0);
        HLAfloat64Time time = timeFactory.makeTime( fedamb.federateTime+fedamb.federateLookahead );

        byte[] quantity = EncodingHelpers.encodeInt(50000);
        quantityHandle = rtiamb.getParameterHandle( addMoneyHandle, "Quantity" );

        parameters.put(quantityHandle, quantity);

        log("Sending AddMoney: " + 50000);
        rtiamb.sendInteraction( addMoneyHandle, parameters, generateTag(), time );
    }

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
        StopWorkingHandle = rtiamb.getInteractionClassHandle( "InteractionRoot.StopWorking" );
        fedamb.StopWorkingHandle = StopWorkingHandle;
        rtiamb.publishInteractionClass( StopWorkingHandle );

        addMoneyHandle = rtiamb.getInteractionClassHandle( "InteractionRoot.AddMoney" );
        fedamb.addMoneyHandle = addMoneyHandle;
        rtiamb.publishInteractionClass( addMoneyHandle );

        NoMoneyHandle = rtiamb.getInteractionClassHandle( "InteractionRoot.InformNoMoney");
        fedamb.NoMoneyHandle = NoMoneyHandle;
        rtiamb.subscribeInteractionClass(NoMoneyHandle);
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
        String federateName = "ObslugaFederate";
        if( args.length != 0 )
        {
            federateName = args[0];
        }

        try
        {
            new ObslugaFederate().runFederate( federateName );
        }
        catch( Exception rtie )
        {
            rtie.printStackTrace();
        }
    }
}
