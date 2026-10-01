package android.content.pm;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;

/**
 * Hidden {@code IPackageInstaller}. Its methods (e.g. installExistingPackage)
 * change between Android versions, so they are invoked reflectively.
 */
public interface IPackageInstaller extends IInterface {

    abstract class Stub extends Binder implements IPackageInstaller {
        public static IPackageInstaller asInterface(IBinder binder) {
            throw new UnsupportedOperationException();
        }
    }
}
