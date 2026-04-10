package com.scheduler;

import javafx.beans.property.*;

public class PCBTableRow {
    private final IntegerProperty id;
    private final StringProperty name;
    private final IntegerProperty arrivalTime;
    private final IntegerProperty priority;
    private final IntegerProperty startTime;
    private final IntegerProperty finishTime;
    private final IntegerProperty waitTime;
    private final IntegerProperty ioWaitTime;
    private final StringProperty state;

    public PCBTableRow(PCB p) {
        this.id          = new SimpleIntegerProperty(p.getId());
        this.name        = new SimpleStringProperty(p.getName());
        this.arrivalTime = new SimpleIntegerProperty(p.getArrivalTime());
        this.priority    = new SimpleIntegerProperty(p.getPriority());
        this.startTime   = new SimpleIntegerProperty(p.getStartTime());
        this.finishTime  = new SimpleIntegerProperty(p.getFinishTime());
        this.waitTime    = new SimpleIntegerProperty(p.getWaitTime());
        this.ioWaitTime  = new SimpleIntegerProperty(p.getIoWaitTime());
        this.state       = new SimpleStringProperty(p.getState().toString());
    }

    public int getId()              { return id.get(); }
    public String getName()         { return name.get(); }
    public int getArrivalTime()     { return arrivalTime.get(); }
    public int getPriority()        { return priority.get(); }
    public int getStartTime()       { return startTime.get(); }
    public int getFinishTime()      { return finishTime.get(); }
    public int getWaitTime()        { return waitTime.get(); }
    public int getIoWaitTime()      { return ioWaitTime.get(); }
    public String getState()        { return state.get(); }

    public IntegerProperty idProperty()          { return id; }
    public StringProperty nameProperty()         { return name; }
    public IntegerProperty arrivalTimeProperty() { return arrivalTime; }
    public IntegerProperty priorityProperty()    { return priority; }
    public IntegerProperty startTimeProperty()   { return startTime; }
    public IntegerProperty finishTimeProperty()  { return finishTime; }
    public IntegerProperty waitTimeProperty()    { return waitTime; }
    public IntegerProperty ioWaitTimeProperty()  { return ioWaitTime; }
    public StringProperty stateProperty()        { return state; }
}