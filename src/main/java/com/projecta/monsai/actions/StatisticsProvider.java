package com.projecta.monsai.actions;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.FutureTask;

import org.springframework.stereotype.Component;

import com.projecta.monsai.mondrian.MondrianConnector;

import mondrian.olap.Result;
import mondrian.rolap.RolapResultShepherd;
import mondrian.server.Execution;
import mondrian.util.Pair;

/**
 * Retrieves some statistics, like memory usage and currently running queries
 */
@Component
public class StatisticsProvider {

    private static final double MB = 1024.0 * 1024.0;


    /**
     * Returns the statistics as a text
     */
    public String getStatistics() throws Exception {

        StringBuilder result = new StringBuilder();
        Runtime runtime = Runtime.getRuntime();

        result.append("Used Memory: "  + Math.round((runtime.totalMemory() - runtime.freeMemory()) / MB) + " MB, ");
        result.append("Free Memory: "  + Math.round(runtime.freeMemory() / MB) + " MB, ");
        result.append("Total Memory: " + Math.round(runtime.totalMemory() / MB) + " MB, ");
        result.append("Max Memory: "   + Math.round(runtime.maxMemory() / MB) + " MB\n\n");


        RolapResultShepherd shepherd = MondrianConnector.getMondrianServer().getResultShepherd();

        Field tasksField = RolapResultShepherd.class.getDeclaredField("tasks");
        tasksField.setAccessible(true);

        List<Pair<FutureTask<Result>, Execution>> tasks = (List<Pair<FutureTask<Result>, Execution>>) tasksField.get(shepherd);

        for (Pair<FutureTask<Result>, Execution> task : tasks) {
            try {
                Execution execution = task.getValue();
                result.append("Task " + execution.getId() + ":\n" + execution.getMondrianStatement().getQuery());
                result.append("\nExecution time: " + formatDuration(execution.getElapsedMillis()) + "\n\n");
            }
            catch (Exception ex) {
                // nothing to do here
            }
        }

        if (tasks.isEmpty()) {
            result.append("No tasks are executing at the moment");
        }
        return result.toString();
    }


    /** Formats durations in a readable format */
    private String formatDuration(long duration) {
        long minutes = duration / 60000;
        long seconds = duration / 1000 - minutes * 60;
        long millis = duration % 1000;

        return (minutes > 0 ? minutes + " m " : "") + (seconds > 0 || minutes > 0 ? seconds + " s " : "") + millis
                + " ms";
    }

}
