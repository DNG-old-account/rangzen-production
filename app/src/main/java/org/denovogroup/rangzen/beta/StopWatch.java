package org.denovogroup.rangzen.beta;

/**
 * Created by Liran on 9/10/2015.
 *
 * This is a simple duration wrapper for measuring trafic and connectivity speeds
 */
public class StopWatch {

    private boolean isRunning = false;
    private long start = 0;
    private long stop = 0;

    public void start(){
        this.start = System.nanoTime();
        isRunning = true;
    }

    public void stop(){
        if(isRunning){
            stop = System.nanoTime();
            isRunning = false;
        }
    }

    public long getElapsedTime() {
        long elapsed;
        if (isRunning) {
            elapsed = (System.nanoTime() - start);
        }
        else {
            elapsed = (stop - start);
        }
        return elapsed;
    }
}
