package Klient;


import hla.rti1516e.LogicalTime;
import org.portico.impl.hla1516e.types.time.DoubleTime;

import java.util.Comparator;

public class KlientExternalEvent {

    public enum EventType {ZNIECIERPLIWIENIE}

    private int que;
    private EventType eventType;
    private Double time;

    public KlientExternalEvent(EventType eventType, LogicalTime time) {
        this.eventType = eventType;
        this.time = convertTime(time);
    }

    public KlientExternalEvent(int que, EventType eventType, LogicalTime time) {
        this.que=que;
        this.eventType = eventType;
        this.time = convertTime(time);
    }

    public int getQue(){
        return que;
    }


    public EventType getEventType() {
        return eventType;
    }

    public double getTime() {
        return time;
    }

    private double convertTime(LogicalTime logicalTime) {
        return ((DoubleTime) logicalTime).getTime();
    }

    public static class ExternalEventComparator implements Comparator<KlientExternalEvent> {
        @Override
        public int compare(KlientExternalEvent o1, KlientExternalEvent o2) {
            return o1.time.compareTo(o2.time);
        }
    }
}
