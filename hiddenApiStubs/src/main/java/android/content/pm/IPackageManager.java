package android.content.pm;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;

/** Hidden {@code IPackageManager}; only the members Canta calls. */
public interface IPackageManager extends IInterface {

    IPackageInstaller getPackageInstaller();

    void setApplicationEnabledSetting(
            String packageName,
            int newState,
            int flags,
            int userId,
            String callingPackage);

    abstract class Stub extends Binder implements IPackageManager {
        public static IPackageManager asInterface(IBinder binder) {
            throw new UnsupportedOperationException();
        }
    }
}
