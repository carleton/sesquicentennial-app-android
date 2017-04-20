package carleton150.edu.carleton.reunion;

import android.util.Log;

/**
 * Created by haleyhinze on 5/6/16.
 *
 * Class to log exceptions that aren't caught in the app code. For debugging.
 * It might be good to expand this to save the report in a file and then if the
 * app crashes, give the user the option of emailing the file to the developer.
 */
public class TopExceptionHandler implements Thread.UncaughtExceptionHandler {

    private Thread.UncaughtExceptionHandler defaultUEH;

    public TopExceptionHandler() {
        this.defaultUEH = Thread.getDefaultUncaughtExceptionHandler();
    }

    public void uncaughtException(Thread t, Throwable e)
    {
        StackTraceElement[] arr = e.getStackTrace();
        String report = e.toString()+"\n\n";
        report += "--------- Stack trace ---------\n\n";
        for (int i=0; i<arr.length; i++)
        {
            report += "    "+arr[i].toString()+"\n";
        }
        report += "-------------------------------\n\n";

// If the exception was thrown in a background thread inside
// AsyncTask, then the actual exception can be found with getCause
        report += "--------- Cause ---------\n\n";
        Throwable cause = e.getCause();
        if(cause != null) {
            report += cause.toString() + "\n\n";
            arr = cause.getStackTrace();
            for (int i=0; i<arr.length; i++)
            {
                report += "    "+arr[i].toString()+"\n";
            }
        }
        report += "-------------------------------\n\n";
        Log.e(LogMessages.OVERALL_ERROR, report);
        defaultUEH.uncaughtException(t, e);
    }
}