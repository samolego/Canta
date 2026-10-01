package android.content.pm;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;

/** Hidden {@code IPackageManager}; only the members Canta calls. */
public interface IPackageManager extends IInterface {

    IPackageInstaller getPackageInstaller();

    abstract class Stub extends Binder implements IPackageManager {
        public static IPackageManager asInterface(IBinder binder) {
            throw new UnsupportedOperationException();
        }
    }
}
