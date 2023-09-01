package android.content.pm;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;

/**
 * Taken from <a href="https://github.com/RikkaApps/Shizuku-API/blob/master/demo-hidden-api-stub/src/main/java/android/content/pm/IPackageInstaller.java">Shizuku API Demo</a>
 *
 * @author RikkaW
 */
public interface IPackageInstaller extends IInterface {

    void abandonSession(int sessionId)
            throws RemoteException;

    IPackageInstallerSession openSession(int sessionId)
            throws RemoteException;

    ParceledListSlice<PackageInstaller.SessionInfo> getMySessions(String installerPackageName, int userId)
            throws RemoteException;

    abstract class Stub extends Binder implements IPackageInstaller {

        public static IPackageInstaller asInterface(IBinder binder) {
            throw new UnsupportedOperationException();
        }
    }
}

