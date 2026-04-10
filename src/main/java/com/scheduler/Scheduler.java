package com.scheduler;

import java.util.*;

public class Scheduler {

    public enum Algorithm { FCFS, SJF, PS, RR }

    private Algorithm algorithm;
    private int quantum;
    private int systemTime;
    private int quantumCounter;

    private CPU cpu;
    private IODevice ioDevice;

    private List<PCB> allProcesses;
    private List<PCB> readyQueue;
    private List<PCB> ioQueue;
    private List<PCB> finishedProcesses;

    // Event log callback so GUI can listen
    private List<String> eventLog;

    // Metrics
    private int totalIdleTime;

    public Scheduler(Algorithm algorithm, int quantum, List<PCB> processes) {
        this.algorithm = algorithm;
        this.quantum = quantum;
        this.systemTime = 0;
        this.quantumCounter = 0;

        this.cpu = new CPU();
        this.ioDevice = new IODevice();

        // Deep copy processes so we can reset without reloading file
        this.allProcesses = new ArrayList<>();
        for (PCB p : processes) {
            this.allProcesses.add(new PCB(
                    p.getName(), p.getId(), p.getArrivalTime(), p.getPriority(),
                    new ArrayList<>(p.getCpuBursts()), new ArrayList<>(p.getIoBursts())
            ));
        }

        this.readyQueue = new ArrayList<>();
        this.ioQueue = new ArrayList<>();
        this.finishedProcesses = new ArrayList<>();
        this.eventLog = new ArrayList<>();
        this.totalIdleTime = 0;
    }

    // Called once per simulation tick from the GUI timer
    public void tick() {
        if (isFinished()) return;

        // Step 1: Admit newly arrived processes to ready queue
        List<PCB> arrived = new ArrayList<>();
        for (PCB p : allProcesses) {
            if (p.getArrivalTime() == systemTime && p.getState() == PCB.State.NEW) {
                p.setState(PCB.State.READY);
                readyQueue.add(p);
                log("Time " + systemTime + ": " + p.getName() + " arrived → ready queue");
            }
        }

        // Step 2: Check if IO device finished
        if (!ioDevice.isIdle()) {
            PCB ioProc = ioDevice.getCurrentProcess();
            if (ioProc.getRemainingIOTime() == 0) {
                ioDevice.removeProcess();
                ioProc.incrementIOIndex();
                ioProc.moveToNextCPUBurst();
                ioProc.setState(PCB.State.READY);
                readyQueue.add(ioProc);
                log("Time " + systemTime + ": " + ioProc.getName() +
                        " finished IO → ready queue");
            }
        }

        // Step 3: Check if CPU process finished its burst
        if (!cpu.isIdle()) {
            PCB running = cpu.getCurrentProcess();
            if (running.getRemainingBurstTime() == 0) {
                cpu.removeProcess();
                if (running.isLastCPUBurst()) {
                    // Process terminates
                    running.setState(PCB.State.TERMINATED);
                    running.setFinishTime(systemTime);
                    finishedProcesses.add(running);
                    readyQueue.remove(running);
                    log("Time " + systemTime + ": " + running.getName() +
                            " TERMINATED | Turnaround: " + running.getTurnaroundTime() +
                            " | Wait: " + running.getWaitTime() +
                            " | IO Wait: " + running.getIoWaitTime());
                } else {
                    // Move to IO queue
                    running.moveToNextIOBurst();
                    running.setState(PCB.State.WAITING);
                    ioQueue.add(running);
                    readyQueue.remove(running);
                    log("Time " + systemTime + ": " + running.getName() +
                            " → IO queue");
                }
                quantumCounter = 0;
            }
        }

        // Step 4: Handle RR quantum expiry
        if (algorithm == Algorithm.RR && !cpu.isIdle()) {
            quantumCounter++;
            if (quantumCounter >= quantum) {
                PCB running = cpu.getCurrentProcess();
                cpu.removeProcess();
                running.setState(PCB.State.READY);
                readyQueue.add(running);
                quantumCounter = 0;
                log("Time " + systemTime + ": " + running.getName() +
                        " quantum expired → ready queue");
            }
        }

        // Step 5: Preemption check for PS
        if (algorithm == Algorithm.PS && !cpu.isIdle() && !readyQueue.isEmpty()) {
            PCB running = cpu.getCurrentProcess();
            PCB highest = getHighestPriority();
            if (highest != null && highest.getPriority() < running.getPriority()) {
                cpu.removeProcess();
                running.setState(PCB.State.READY);
                readyQueue.add(running);
                log("Time " + systemTime + ": " + running.getName() +
                        " preempted by " + highest.getName());
            }
        }

        // Step 6: Assign process to CPU if idle
        if (cpu.isIdle() && !readyQueue.isEmpty()) {
            PCB next = pickNextProcess();
            if (next != null) {
                readyQueue.remove(next);
                cpu.assignProcess(next);
                next.setStartTime(systemTime);
                quantumCounter = 0;
                log("Time " + systemTime + ": " + next.getName() +
                        " dispatched to CPU");
            }
        } else if (cpu.isIdle()) {
            totalIdleTime++;
        }

        // Step 7: Assign process to IO device if idle
        if (ioDevice.isIdle() && !ioQueue.isEmpty()) {
            PCB nextIO = ioQueue.get(0);
            ioQueue.remove(0);
            ioDevice.assignProcess(nextIO);
            log("Time " + systemTime + ": " + nextIO.getName() +
                    " dispatched to IO device");
        }

        // Step 8: Increment wait times for processes sitting in queues
        for (PCB p : readyQueue) {
            if (p.getState() == PCB.State.READY) {
                p.incrementWaitTime();
            }
        }
        for (PCB p : ioQueue) {
            p.incrementIOWaitTime();
        }

        // Step 9: Execute CPU and IO device
        cpu.execute();
        ioDevice.execute();

        // Step 10: Advance system time
        systemTime++;
    }

    private PCB pickNextProcess() {
        if (readyQueue.isEmpty()) return null;
        switch (algorithm) {
            case FCFS:
            case RR:
                return readyQueue.get(0);
            case SJF:
                return readyQueue.stream()
                        .min(Comparator.comparingInt(PCB::getRemainingBurstTime))
                        .orElse(null);
            case PS:
                return getHighestPriority();
            default:
                return readyQueue.get(0);
        }
    }

    private PCB getHighestPriority() {
        return readyQueue.stream()
                .min(Comparator.comparingInt(PCB::getPriority))
                .orElse(null);
    }

    public boolean isFinished() {
        return allProcesses.stream()
                .allMatch(p -> p.getState() == PCB.State.TERMINATED);
    }

    private void log(String message) {
        eventLog.add(message);
    }

    // Getters for GUI
    public int getSystemTime() { return systemTime; }
    public CPU getCpu() { return cpu; }
    public IODevice getIoDevice() { return ioDevice; }
    public List<PCB> getReadyQueue() { return readyQueue; }
    public List<PCB> getIoQueue() { return ioQueue; }
    public List<PCB> getAllProcesses() { return allProcesses; }
    public List<PCB> getFinishedProcesses() { return finishedProcesses; }
    public List<String> getEventLog() { return eventLog; }
    public Algorithm getAlgorithm() { return algorithm; }

    public double getCpuUtilization() {
        if (systemTime == 0) return 0;
        return (double) cpu.getTotalBusyTime() / systemTime * 100;
    }

    public double getAverageTurnaroundTime() {
        if (finishedProcesses.isEmpty()) return 0;
        return finishedProcesses.stream()
                .mapToInt(PCB::getTurnaroundTime)
                .average().orElse(0);
    }

    public double getAverageWaitTime() {
        if (finishedProcesses.isEmpty()) return 0;
        return finishedProcesses.stream()
                .mapToInt(PCB::getWaitTime)
                .average().orElse(0);
    }

    public double getThroughput() {
        if (systemTime == 0) return 0;
        return (double) finishedProcesses.size() / systemTime;
    }
}