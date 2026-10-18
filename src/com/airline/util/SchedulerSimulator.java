package com.airline.util;

import java.util.*;

/**
 * Operating System CPU / Task Scheduling Engine (SPOS)
 * Implements two classical OS Scheduling Algorithms:
 * 1. FCFS (First Come, First Served) - Non-preemptive queue scheduling
 * 2. Round Robin (RR) - Preemptive scheduling with a configurable Time Quantum
 *
 * Models processes/tasks with Arrival Time, Burst Time, Completion Time,
 * Turnaround Time (TAT = CT - AT), and Waiting Time (WT = TAT - BT).
 */
public class SchedulerSimulator {

    public static class ProcessTask {
        public String pid;
        public String taskName;
        public int arrivalTime;
        public int burstTime;
        public int priority;
        public int remainingTime;
        public int completionTime;
        public int turnaroundTime;
        public int waitingTime;

        public ProcessTask(String pid, String taskName, int arrivalTime, int burstTime, int priority) {
            this.pid = pid;
            this.taskName = taskName;
            this.arrivalTime = arrivalTime;
            this.burstTime = burstTime;
            this.priority = priority;
            this.remainingTime = burstTime;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("pid", pid);
            map.put("task_name", taskName);
            map.put("arrival_time", arrivalTime);
            map.put("burst_time", burstTime);
            map.put("priority", priority);
            map.put("completion_time", completionTime);
            map.put("turnaround_time", turnaroundTime);
            map.put("waiting_time", waitingTime);
            return map;
        }
    }

    public static class GanttSlot {
        public String pid;
        public String taskName;
        public int startTime;
        public int endTime;

        public GanttSlot(String pid, String taskName, int startTime, int endTime) {
            this.pid = pid;
            this.taskName = taskName;
            this.startTime = startTime;
            this.endTime = endTime;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("pid", pid);
            map.put("task_name", taskName);
            map.put("start_time", startTime);
            map.put("end_time", endTime);
            map.put("duration", endTime - startTime);
            return map;
        }
    }

    public static List<ProcessTask> getDefaultAirlineTasks() {
        List<ProcessTask> tasks = new ArrayList<>();
        tasks.add(new ProcessTask("P1", "Seat Allocation & Lock", 0, 4, 1));
        tasks.add(new ProcessTask("P2", "Payment Gateway Simulation", 1, 5, 2));
        tasks.add(new ProcessTask("P3", "PNR Ticket Generation", 2, 2, 3));
        tasks.add(new ProcessTask("P4", "Baggage Dispatch Service", 3, 6, 2));
        tasks.add(new ProcessTask("P5", "Flight Schedule Check", 4, 3, 1));
        return tasks;
    }

    /**
     * Algorithm 1: First Come, First Served (FCFS) Scheduling
     */
    public static Map<String, Object> scheduleFCFS(List<ProcessTask> inputTasks) {
        List<ProcessTask> list = new ArrayList<>();
        for (ProcessTask p : inputTasks) {
            list.add(new ProcessTask(p.pid, p.taskName, p.arrivalTime, p.burstTime, p.priority));
        }

        // Sort primarily by Arrival Time
        list.sort(Comparator.comparingInt(a -> a.arrivalTime));

        List<GanttSlot> ganttChart = new ArrayList<>();
        int currentTime = 0;
        int totalWT = 0;
        int totalTAT = 0;

        for (ProcessTask p : list) {
            if (currentTime < p.arrivalTime) {
                // CPU idle
                ganttChart.add(new GanttSlot("IDLE", "CPU Idle", currentTime, p.arrivalTime));
                currentTime = p.arrivalTime;
            }

            int start = currentTime;
            currentTime += p.burstTime;
            p.completionTime = currentTime;
            p.turnaroundTime = p.completionTime - p.arrivalTime;
            p.waitingTime = p.turnaroundTime - p.burstTime;

            totalWT += p.waitingTime;
            totalTAT += p.turnaroundTime;

            ganttChart.add(new GanttSlot(p.pid, p.taskName, start, currentTime));
        }

        double avgWT = list.isEmpty() ? 0 : (double) totalWT / list.size();
        double avgTAT = list.isEmpty() ? 0 : (double) totalTAT / list.size();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("algorithm", "FCFS (First Come, First Served)");
        result.put("type", "Non-Preemptive");
        result.put("description", "Processes execute strictly in the order of arrival until CPU burst completion.");
        result.put("time_quantum", null);
        result.put("avg_waiting_time", Math.round(avgWT * 100.0) / 100.0);
        result.put("avg_turnaround_time", Math.round(avgTAT * 100.0) / 100.0);
        result.put("total_completion_time", currentTime);

        List<Map<String, Object>> taskMaps = new ArrayList<>();
        for (ProcessTask p : list) taskMaps.add(p.toMap());
        result.put("processes", taskMaps);

        List<Map<String, Object>> ganttMaps = new ArrayList<>();
        for (GanttSlot g : ganttChart) ganttMaps.add(g.toMap());
        result.put("gantt_chart", ganttMaps);

        return result;
    }

    /**
     * Algorithm 2: Round Robin (RR) Scheduling with Time Quantum
     */
    public static Map<String, Object> scheduleRoundRobin(List<ProcessTask> inputTasks, int timeQuantum) {
        if (timeQuantum <= 0) timeQuantum = 2;

        List<ProcessTask> list = new ArrayList<>();
        for (ProcessTask p : inputTasks) {
            list.add(new ProcessTask(p.pid, p.taskName, p.arrivalTime, p.burstTime, p.priority));
        }

        list.sort(Comparator.comparingInt(a -> a.arrivalTime));

        List<GanttSlot> ganttChart = new ArrayList<>();
        Queue<ProcessTask> readyQueue = new LinkedList<>();

        int currentTime = 0;
        int completed = 0;
        int n = list.size();
        boolean[] inQueue = new boolean[n];

        // Push initially available processes
        for (int i = 0; i < n; i++) {
            if (list.get(i).arrivalTime <= currentTime) {
                readyQueue.offer(list.get(i));
                inQueue[i] = true;
            }
        }

        while (completed < n) {
            if (readyQueue.isEmpty()) {
                // Find next arrival
                int nextArrival = Integer.MAX_VALUE;
                for (int i = 0; i < n; i++) {
                    if (!inQueue[i] && list.get(i).remainingTime > 0) {
                        nextArrival = Math.min(nextArrival, list.get(i).arrivalTime);
                    }
                }
                if (nextArrival != Integer.MAX_VALUE) {
                    ganttChart.add(new GanttSlot("IDLE", "CPU Idle", currentTime, nextArrival));
                    currentTime = nextArrival;
                    for (int i = 0; i < n; i++) {
                        if (!inQueue[i] && list.get(i).arrivalTime <= currentTime && list.get(i).remainingTime > 0) {
                            readyQueue.offer(list.get(i));
                            inQueue[i] = true;
                        }
                    }
                }
                continue;
            }

            ProcessTask current = readyQueue.poll();
            int executionSlice = Math.min(current.remainingTime, timeQuantum);
            int start = currentTime;
            currentTime += executionSlice;
            current.remainingTime -= executionSlice;

            ganttChart.add(new GanttSlot(current.pid, current.taskName, start, currentTime));

            // Check new arrivals during this execution slice
            for (int i = 0; i < n; i++) {
                if (!inQueue[i] && list.get(i).arrivalTime <= currentTime && list.get(i).remainingTime > 0) {
                    readyQueue.offer(list.get(i));
                    inQueue[i] = true;
                }
            }

            if (current.remainingTime > 0) {
                // Re-queue preempted process
                readyQueue.offer(current);
            } else {
                current.completionTime = currentTime;
                current.turnaroundTime = current.completionTime - current.arrivalTime;
                current.waitingTime = current.turnaroundTime - current.burstTime;
                completed++;
            }
        }

        int totalWT = 0;
        int totalTAT = 0;
        for (ProcessTask p : list) {
            totalWT += p.waitingTime;
            totalTAT += p.turnaroundTime;
        }

        double avgWT = list.isEmpty() ? 0 : (double) totalWT / list.size();
        double avgTAT = list.isEmpty() ? 0 : (double) totalTAT / list.size();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("algorithm", "Round Robin (RR)");
        result.put("type", "Preemptive (Time Slice = " + timeQuantum + " units)");
        result.put("description", "Each process is assigned a fixed time slice (quantum). Unfinished processes are placed at the back of the ready queue.");
        result.put("time_quantum", timeQuantum);
        result.put("avg_waiting_time", Math.round(avgWT * 100.0) / 100.0);
        result.put("avg_turnaround_time", Math.round(avgTAT * 100.0) / 100.0);
        result.put("total_completion_time", currentTime);

        List<Map<String, Object>> taskMaps = new ArrayList<>();
        for (ProcessTask p : list) taskMaps.add(p.toMap());
        result.put("processes", taskMaps);

        List<Map<String, Object>> ganttMaps = new ArrayList<>();
        for (GanttSlot g : ganttChart) ganttMaps.add(g.toMap());
        result.put("gantt_chart", ganttMaps);

        return result;
    }
}
