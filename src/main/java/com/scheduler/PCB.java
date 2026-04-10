package com.scheduler;

import java.util.ArrayList;
import java.util.List;

public class PCB {
    // Basic process info
    private String name;
    private int id;
    private int arrivalTime;
    private int priority;

    // CPU and IO burst lists
    private List<Integer> cpuBursts;
    private List<Integer> ioBursts;

    // Tracks current position in burst lists
    private int currentBurstIndex;  // index into cpuBursts
    private int currentIOIndex;     // index into ioBursts
    private int remainingBurstTime; // remaining time in current CPU burst
    private int remainingIOTime;    // remaining time in current IO burst
    private boolean onCPU;          // true = currently doing CPU burst, false = IO burst

    // Process state
    public enum State { NEW, READY, RUNNING, WAITING, TERMINATED }
    private State state;

    // Statistics
    private int startTime;
    private int finishTime;
    private int turnaroundTime;
    private int waitTime;
    private int ioWaitTime;

    public PCB(String name, int id, int arrivalTime, int priority,
               List<Integer> cpuBursts, List<Integer> ioBursts) {
        this.name = name;
        this.id = id;
        this.arrivalTime = arrivalTime;
        this.priority = priority;
        this.cpuBursts = new ArrayList<>(cpuBursts);
        this.ioBursts = new ArrayList<>(ioBursts);

        this.currentBurstIndex = 0;
        this.currentIOIndex = 0;
        this.remainingBurstTime = cpuBursts.get(0);
        this.remainingIOTime = 0;
        this.onCPU = true;

        this.state = State.NEW;
        this.startTime = -1;
        this.finishTime = -1;
        this.turnaroundTime = 0;
        this.waitTime = 0;
        this.ioWaitTime = 0;
    }

    // Move to next CPU burst after IO completes
    public void moveToNextCPUBurst() {
        currentBurstIndex++;
        remainingBurstTime = cpuBursts.get(currentBurstIndex);
        onCPU = true;
    }

    // Move to next IO burst after CPU burst completes
    public void moveToNextIOBurst() {
        remainingIOTime = ioBursts.get(currentIOIndex);
        onCPU = false;
    }

    // Returns true if this is the last CPU burst (no more IO after)
    public boolean isLastCPUBurst() {
        return currentBurstIndex == cpuBursts.size() - 1;
    }

    // Returns true if there is a next IO burst available
    public boolean hasNextIOBurst() {
        return currentIOIndex < ioBursts.size();
    }

    // Getters and setters
    public String getName() { return name; }
    public int getId() { return id; }
    public int getArrivalTime() { return arrivalTime; }
    public int getPriority() { return priority; }
    public State getState() { return state; }
    public void setState(State state) { this.state = state; }
    public int getRemainingBurstTime() { return remainingBurstTime; }
    public void setRemainingBurstTime(int t) { remainingBurstTime = t; }
    public int getRemainingIOTime() { return remainingIOTime; }
    public void setRemainingIOTime(int t) { remainingIOTime = t; }
    public boolean isOnCPU() { return onCPU; }
    public int getCurrentIOIndex() { return currentIOIndex; }
    public void incrementIOIndex() { currentIOIndex++; }
    public List<Integer> getCpuBursts() { return cpuBursts; }
    public List<Integer> getIoBursts() { return ioBursts; }
    public int getCurrentBurstIndex() { return currentBurstIndex; }

    public int getStartTime() { return startTime; }
    public void setStartTime(int t) {
        if (startTime == -1) startTime = t;
    }

    public int getFinishTime() { return finishTime; }
    public void setFinishTime(int t) {
        finishTime = t;
        turnaroundTime = finishTime - arrivalTime;
    }

    public int getTurnaroundTime() { return turnaroundTime; }
    public int getWaitTime() { return waitTime; }
    public void incrementWaitTime() { waitTime++; }
    public int getIoWaitTime() { return ioWaitTime; }
    public void incrementIOWaitTime() { ioWaitTime++; }

    @Override
    public String toString() {
        return "Process [name=" + name + ", id=" + id +
                ", arrivalTime=" + arrivalTime +
                ", priority=" + priority +
                ", cpuBursts=" + cpuBursts +
                ", ioBursts=" + ioBursts + "]";
    }
}