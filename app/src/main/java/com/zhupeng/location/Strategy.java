package com.zhupeng.location;

import android.content.Context;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;

public abstract class Strategy implements IServiceInterface {

    protected final RemoteCallbackList<IResultListener> mResultListeners = new RemoteCallbackList<>();

    public abstract void execute(Context context);

    public abstract void finish(Context context);

    @Override
    public void addResultListener(IResultListener listener) throws RemoteException {
        if (mResultListeners != null) {
            mResultListeners.register(listener);
        }
    }

    @Override
    public void removeResultListener(IResultListener listener) throws RemoteException {
        if (mResultListeners != null) {
            mResultListeners.unregister(listener);
        }
    }

    @Override
    public void removeAllResultListeners() throws RemoteException {
        if (mResultListeners != null) {
            mResultListeners.kill();
        }
    }

    @Override
    public IBinder asBinder() {
        return null;
    }
}
