package haven.purus.pbot.external;

import haven.purus.automation.exception.BotInterruptedException;
import haven.purus.pbot.api.PBotSession;

import static java.lang.Thread.currentThread;

public abstract class HnhBot {

    protected final static String dataFolderPath = System.getProperty("user.home") + "/.haven/scriptData/";

    protected boolean isInterrupted = false;
    private boolean isRunning = false;
    private Thread runThread = null;

    public abstract void execute(PBotSession botSession);

    public void run(PBotSession botSession, Thread thread) {
        isRunning = true;
        isInterrupted = false;
        runThread = thread;

        try {
            execute(botSession);
        } finally {
            isRunning = false;
            isInterrupted = false;
            runThread = null;
        }
    }

    public void stop() {
        isInterrupted = true;
        if (runThread != null) {
            runThread.interrupt();
        }
    }

    public boolean isRunning() {
        return isRunning;
    }

    public boolean isNotInterruptedCheck() {
        if (currentThread().isInterrupted()) {
            throw new BotInterruptedException();
        }

        return true;
    }

}
