package android.content.pm;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;

/**
 * Taken from <a href="https://github.com/RikkaApps/Shizuku-API/blob/master/demo-hidden-api-stub/src/main/java/android/content/pm/IPackageInstallerSession.java">Shizuku API Demo</a>
 *
 * @author RikkaW
 */
public interface IPackageInstallerSession extends IInterface {

    abstract class Stub extends Binder implements IPackageInstallerSession {

        public static IPackageInstallerSession asInterface(IBinder binder) {
            throw new UnsupportedOperationException();
        }
    }
}
