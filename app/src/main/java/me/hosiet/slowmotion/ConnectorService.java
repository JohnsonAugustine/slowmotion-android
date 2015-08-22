package me.hosiet.slowmotion;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import java.util.Random;

/**
 * Used to handle connection between the device and the robot.
 *
 * Always bound by the MainActivity.
 * Its lifecycle follows the lifecycle of MainActivity.
 *
 * @TODO Not used yet
 */
public class ConnectorService extends Service {
    // Binder given to clients
    private final IBinder mBinder = new ConnectorBinder();

    /**
     * Class used for the client Binder.
     *
     * Note that the service alsays run in the same process as its clients.
     * There is no IPC problem.
     */
    public class ConnectorBinder extends Binder {
        ConnectorService getService() {
            // Return the instance of ConnectorService.
            // Let the clients to call public methods.
            return ConnectorService.this;
        }
    }
    public ConnectorService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /* The following are the method for the clients */
}
