package Klient;

import hla.rti.jlc.EncodingHelpers;
import hla.rti1516e.*;
import hla.rti1516e.encoding.ByteWrapper;
import hla.rti1516e.encoding.EncoderFactory;
import hla.rti1516e.encoding.HLAinteger16BE;
import hla.rti1516e.exceptions.FederatesCurrentlyJoined;
import hla.rti1516e.exceptions.FederationExecutionAlreadyExists;
import hla.rti1516e.exceptions.FederationExecutionDoesNotExist;
import hla.rti1516e.exceptions.RTIexception;
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

public class KlientFederate {

    //Punkt Synchronizacji
    public static final String READY_TO_RUN = "ReadyToRun";

    //Główne zmienne
    private RTIambassador rtiamb;
    private KlientAmbassador fedamb;
    private HLAfloat64TimeFactory timeFactory; // set when we join
    protected EncoderFactory encoderFactory;     // set when we join

    //Zmienne Bankomatu
    private final double timeStep           = 10.0;
    private int nrKlient = 0;

    //Zmienne Handle
    private AttributeHandle nrKlientHandle;
    protected InteractionClassHandle getMoneyHandle;
    protected InteractionClassHandle addClientHandle;
    protected ParameterHandle quantityHandle;



    private void log( String message )
    {
        System.out.println( "KlientFederate   : " + message );
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
        fedamb = new KlientAmbassador( this );
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
            log( "Created Federation from Klient" );

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
                "KlientType",
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

            sendAddClientInteraction();
            sendGetMoneyInteraction();

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
            log( "Destroyed Federation from Klient" );
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

    private void sendGetMoneyInteraction() throws RTIexception {
        ParameterHandleValueMap parameters = rtiamb.getParameterHandleValueMapFactory().create(0);
        HLAfloat64Time time = timeFactory.makeTime( fedamb.federateTime+fedamb.federateLookahead );

        Random random = new Random();
        int quantityInt = random.nextInt(10000) + 1;
        byte[] quantity = EncodingHelpers.encodeInt(quantityInt);
        quantityHandle = rtiamb.getParameterHandle( getMoneyHandle, "Quantity" );


        parameters.put(quantityHandle, quantity);

        log("Sending GetMoney: " + quantityInt);
        rtiamb.sendInteraction( getMoneyHandle, parameters, generateTag(), time );
    }

    private void sendAddClientInteraction() throws RTIexception {
        ParameterHandleValueMap parameters = rtiamb.getParameterHandleValueMapFactory().create(0);
        HLAfloat64Time time = timeFactory.makeTime( fedamb.federateTime+fedamb.federateLookahead );
        rtiamb.sendInteraction( addClientHandle, parameters, generateTag(), time );
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
        getMoneyHandle = rtiamb.getInteractionClassHandle( "InteractionRoot.GetMoney" );
        fedamb.getMoneyHandle = getMoneyHandle;
        rtiamb.publishInteractionClass( getMoneyHandle );

        addClientHandle = rtiamb.getInteractionClassHandle( "InteractionRoot.AddClient" );
        fedamb.addClientHandle = addClientHandle;
        rtiamb.publishInteractionClass( addClientHandle );
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
        String federateName = "KlientFederate";
        if( args.length != 0 )
        {
            federateName = args[0];
        }

        try
        {
            new KlientFederate().runFederate( federateName );
        }
        catch( Exception rtie )
        {
            rtie.printStackTrace();
        }
    }
}
