package pt.ulisboa.tecnico.sec.utils;

import java.io.PrintStream;

public abstract class Logger {
    public static final PrintStream DEFAULT_PRINT_STREAM = System.err;
    
    public static void Log(Object message) {
        DEFAULT_PRINT_STREAM.print(message);
    }

    public static void Log(Object message, PrintStream... printStreams) {
        if (printStreams == null || printStreams.length == 0) {
            Log(message);
            return;
        }

        for (PrintStream printStream : printStreams) {
            if (printStream == null) continue;

            printStream.print(message);
        }
    }

    public static void Logln(Object message, PrintStream... printStreams) {
        Log(String.format("%s%n", message.toString()), printStreams);
    }

    public static void ThreadLog(int id, Object message, PrintStream... printStreams) {
        Log("[" + id + "] " + message, printStreams);
    }

    public static void ThreadLogln(int id, Object message, PrintStream... printStreams) {
        ThreadLog(id, String.format("%s%n", message.toString()), printStreams);
    }
}