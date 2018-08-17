package nl.NG.Jetfightergame.Tools;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author Geert van Ieperen created on 2-6-2018.
 */
public enum Logger {
    DEBUG, INFO, WARN, ERROR;

    /** prevents spamming the chat */
    static Set<String> callerBlacklist = new HashSet<>();
    private static List<Supplier<String>> onlinePrints = new CopyOnWriteArrayList<>();

    private Consumer<String> out = System.out::println;
    private boolean enabled = true;
    private String codeName = String.format("[%-5s]", this);

    private static String concatenate(Object[] x) {
        if (x.length == 0) return "";
        for (int i = 0; i < x.length; i++) {
            if (x[i] == null)
                x[i] = "null";
        }
        StringBuilder s = new StringBuilder(x[0].toString());
        for (int i = 1; i < x.length; i++) {
            s.append(" | ").append(x[i]);
        }
        return s.toString();
    }

    /**
     * sets the debug output of the given print method to the specified output
     * @param newOutput
     */
    public void setOutput(Consumer<String> newOutput) {
        if (newOutput == null) {
            Logger.ERROR.print("New output is null");
            return;
        }

        out = newOutput;
    }

    /**
     * adds a line to the online output roll
     * @param source
     */
    public static void printOnline(Supplier<String> source) {
        if (source == null) {
            Logger.ERROR.print("source is null");
        }
        onlinePrints.add(source);
    }

    /**
     * puts a message on the debug screen, which is updated every frame
     * @param accepter a method that prints the given string, on the same position as a previous call to this method
     */
    public static void setOnlineOutput(Consumer<String> accepter) {
        for (Supplier<String> source : onlinePrints) {
            String message = source.get();
            accepter.accept(message);
        }
    }

    /**
     * DEBUG method to get the calling method name
     * @param level the stack depth to receive. -1 = this method {@code getCallingMethod(int)} 0 = the calling method
     *              (yourself) 1 = the caller of the method this is called in
     * @return a string that completely describes the path to the file, the method and line number where this is called
     *         If DEBUG == false, return an empty string
     */
    public static String getCallingMethod(int level) {
        StackTraceElement caller;
        Exception exception = new Exception();
        StackTraceElement[] stackTrace = exception.getStackTrace();
        level++;
        do {
            caller = stackTrace[level++]; // level + 1
        } while (caller.isNativeMethod() && level < stackTrace.length);

        return String.format("%-100s ", caller);
    }

    /**
     * removes the specified updater off the debug screen
     * @param source an per-frame updated debug message that has previously added to the debug screen
     */
    public static void removeOnlineUpdate(Supplier<String> source) {
        onlinePrints.remove(source);
    }

    public static void setOutputLevel(Logger minimum) {
        Logger[] levels = values();
        for (int i = 0; i < levels.length; i++) {
            levels[i].enabled = (i >= minimum.ordinal());
        }
    }

    /**
     * prints the toString method of the given objects to System.out, preceded with calling method. Every unique
     * callside will only be allowed to print once. For recursive calls, every level will be regarded as a new level,
     * thus print once for every unique depth
     * @param identifier the string that identifies this call as unique
     * @param s          the strings to print
     */
    public synchronized void printSpamless(String identifier, Object... s) {
        if (!callerBlacklist.contains(identifier)) {
            printFrom(2, s);
            callerBlacklist.add(identifier);
        }
    }

    /**
     * prints the toString method of the given objects to the debug output, preceded with the method caller specified by
     * the given call depth
     * @param depth 0 = this method, 1 = the calling method (yourself)
     */
    public synchronized void printFrom(int depth, Object... s) {
        if (!enabled) return;

        String prefix = codeName;
        if (DEBUG.enabled) prefix = getCallingMethod(depth) + prefix;

        switch (this) {
            case DEBUG:
            case INFO:
                out.accept(prefix + ": " + concatenate(s));
                break;
            case WARN:
            case ERROR:
                System.err.println(prefix + ": " + concatenate(s));
                break;
        }
    }

    /**
     * prints the toString method of the given objects to System.out, preceded with calling method
     */
    public void print(Object... s) {
        printFrom(2, s);
    }

    public void printf(String format, Object... arguments) {
        printFrom(2, String.format(Locale.US, format, arguments));
    }
}
