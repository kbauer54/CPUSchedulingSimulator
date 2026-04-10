package com.scheduler;

public class CPU {
    private PCB currentProcess;
    private int totalBusyTime;

    public CPU() {
        this.currentProcess = null;
        this.totalBusyTime = 0;
    }

    public void assignProcess(PCB process) {
        this.currentProcess = process;
        if (process != null) {
            process.setState(PCB.State.RUNNING);
        }
    }

    public void execute() {
        if (currentProcess != null) {
            currentProcess.setRemainingBurstTime(
                    currentProcess.getRemainingBurstTime() - 1
            );
            totalBusyTime++;
        }
    }

    public boolean isIdle() {
        return currentProcess == null;
    }

    public PCB getCurrentProcess() {
        return currentProcess;
    }

    public void removeProcess() {
        currentProcess = null;
    }

    public int getTotalBusyTime() {
        return totalBusyTime;
    }
}