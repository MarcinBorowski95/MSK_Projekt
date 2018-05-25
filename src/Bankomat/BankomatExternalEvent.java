package Bankomat;

import hla.rti1516e.LogicalTime;
import org.portico.impl.hla1516e.types.time.DoubleTime;

import java.util.Comparator;

public class BankomatExternalEvent {

    public enum EventType {AddClient, ClientLeave, ClientImpatience, StopWorking, AddMoney, GetMoney}

    private int qty;
    private EventType eventType;
    private Double time;

    public BankomatExternalEvent(EventType eventType, LogicalTime time) {
        this.eventType = eventType;
        this.time = convertTime(time);
    }

    public BankomatExternalEvent(int qty, EventType eventType, LogicalTime time) {
        this.qty = qty;
        this.eventType = eventType;
        this.time = convertTime(time);
    }


    public EventType getEventType() {
        return eventType;
    }

    public int getQty() {
        return qty;
    }

    public double getTime() {
        return time;
    }

    private double convertTime(LogicalTime logicalTime) {
        return ((DoubleTime) logicalTime).getTime();
    }

    public static class ExternalEventComparator implements Comparator<BankomatExternalEvent> {
        @Override
        public int compare(BankomatExternalEvent o1, BankomatExternalEvent o2) {
            return o1.time.compareTo(o2.time);
        }
    }


}