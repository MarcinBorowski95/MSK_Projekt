package Obsluga;

import hla.rti1516e.LogicalTime;
import org.portico.impl.hla1516e.types.time.DoubleTime;

import java.util.Comparator;

public class ObslugaExternalEvent {

    public enum EventType {NoMoney}

    private EventType eventType;
    private Double time;

    public ObslugaExternalEvent(EventType eventType, LogicalTime time) {
        this.eventType = eventType;
        this.time = convertTime(time);
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

    public static class ExternalEventComparator implements Comparator<ObslugaExternalEvent> {
        @Override
        public int compare(ObslugaExternalEvent o1, ObslugaExternalEvent o2) {
            return o1.time.compareTo(o2.time);
        }
    }
}
