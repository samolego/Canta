package android.content.pm;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;


/**
 * Taken from <a href="https://github.com/RikkaApps/Shizuku-API/blob/master/demo-hidden-api-stub/src/main/java/android/content/pm/IPackageManager.java">Shizuku API Demo</a>
 *
 * @author RikkaW
 */
public interface IPackageManager extends IInterface {

    IPackageInstaller getPackageInstaller()
            throws RemoteException;

    abstract class Stub extends Binder implements IPackageManager {

        public static IPackageManager asInterface(IBinder obj) {
            throw new UnsupportedOperationException();
        }
    }
}
