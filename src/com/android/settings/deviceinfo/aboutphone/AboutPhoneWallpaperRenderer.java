/*
 * Copyright (C) 2026 The uwuAOSP Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.deviceinfo.aboutphone;

import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.app.wallpaper.WallpaperDescription;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.IBinder;
import android.os.RemoteException;
import android.service.wallpaper.IWallpaperConnection;
import android.service.wallpaper.IWallpaperEngine;
import android.service.wallpaper.IWallpaperService;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.SurfaceControl;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.NonNull;

/** Renders one frame from a live wallpaper into a SurfaceView. */
final class AboutPhoneWallpaperRenderer extends IWallpaperConnection.Stub
        implements ServiceConnection {

    interface Listener {
        void onEngineReady();

        void onConnectionFailed();
    }

    private static final String TAG = "AboutPhoneWallpaper";
    private final Context mContext;
    private final SurfaceView mSurfaceView;
    private final Listener mListener;
    private final ComponentName mComponent;
    private final Point mDisplaySize = new Point();

    private IWallpaperService mService;
    private IWallpaperEngine mEngine;
    private IBinder mWindowToken;
    private SurfaceControl mMirrorSurface;
    private boolean mBound;
    private boolean mAttached;
    private boolean mDestroyed;
    private boolean mReadyNotified;
    private int mMirrorAttempts;
    private final View.OnAttachStateChangeListener mAttachListener;

    AboutPhoneWallpaperRenderer(
            @NonNull Context context,
            @NonNull WallpaperInfo wallpaperInfo,
            @NonNull SurfaceView surfaceView,
            @NonNull Listener listener) {
        mContext = context.getApplicationContext();
        mComponent = wallpaperInfo.getComponent();
        mSurfaceView = surfaceView;
        mListener = listener;
        mAttachListener = new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View view) {
                mSurfaceView.removeOnAttachStateChangeListener(this);
                attachWhenReady();
            }

            @Override
            public void onViewDetachedFromWindow(View view) {
            }
        };
    }

    boolean connect() {
        if (mDestroyed || mBound) {
            return false;
        }
        final Intent intent = new Intent(WallpaperService.SERVICE_INTERFACE)
                .setComponent(mComponent);
        try {
            mBound = mContext.bindService(intent, this,
                    Context.BIND_AUTO_CREATE | Context.BIND_IMPORTANT
                            | Context.BIND_ALLOW_ACTIVITY_STARTS);
        } catch (RuntimeException e) {
            Log.w(TAG, "Unable to bind live wallpaper", e);
            mBound = false;
        }
        if (!mBound) {
            mListener.onConnectionFailed();
        }
        return mBound;
    }

    void destroy() {
        if (mDestroyed) {
            return;
        }
        mDestroyed = true;
        mSurfaceView.removeOnAttachStateChangeListener(mAttachListener);
        if (mEngine != null) {
            try {
                mEngine.destroy();
            } catch (RemoteException ignored) {
            }
            mEngine = null;
        }
        if (mService != null && mWindowToken != null) {
            try {
                mService.detach(mWindowToken);
            } catch (RemoteException ignored) {
            }
        }
        mWindowToken = null;
        if (mMirrorSurface != null) {
            mMirrorSurface.release();
            mMirrorSurface = null;
        }
        if (mBound) {
            try {
                mContext.unbindService(this);
            } catch (IllegalArgumentException ignored) {
            }
            mBound = false;
        }
        mService = null;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        if (mDestroyed) {
            return;
        }
        mService = IWallpaperService.Stub.asInterface(service);
        if (mService == null) {
            mListener.onConnectionFailed();
            return;
        }
        attachWhenReady();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        mService = null;
        mEngine = null;
        if (!mDestroyed) {
            mListener.onConnectionFailed();
        }
    }

    private void attachWhenReady() {
        if (mDestroyed || mAttached || mService == null) {
            return;
        }
        if (!mSurfaceView.isAttachedToWindow()) {
            mSurfaceView.removeOnAttachStateChangeListener(mAttachListener);
            mSurfaceView.addOnAttachStateChangeListener(mAttachListener);
            return;
        }
        if (mSurfaceView.getWidth() <= 0 || mSurfaceView.getHeight() <= 0
                || mSurfaceView.getWindowToken() == null) {
            mSurfaceView.postDelayed(this::attachWhenReady, 16L);
            return;
        }
        final android.view.Display display = mSurfaceView.getDisplay();
        if (display == null) {
            mListener.onConnectionFailed();
            return;
        }
        display.getRealSize(mDisplaySize);
        if (mDisplaySize.x <= 0 || mDisplaySize.y <= 0) {
            mListener.onConnectionFailed();
            return;
        }
        mWindowToken = mSurfaceView.getWindowToken();
        try {
            mService.attach(this, mWindowToken, WindowManager.LayoutParams.TYPE_APPLICATION_MEDIA,
                    true, mDisplaySize.x, mDisplaySize.y, new Rect(0, 0, 0, 0),
                    display.getDisplayId(), WallpaperManager.FLAG_SYSTEM,
                    null, new WallpaperDescription.Builder().build());
            mAttached = true;
        } catch (RemoteException | RuntimeException e) {
            Log.w(TAG, "Unable to attach live wallpaper", e);
            mListener.onConnectionFailed();
        }
    }

    @Override
    public void attachEngine(IWallpaperEngine engine, int displayId) {
        if (mDestroyed) {
            if (engine != null) {
                try {
                    engine.destroy();
                } catch (RemoteException ignored) {
                }
            }
            return;
        }
        if (engine == null) {
            mListener.onConnectionFailed();
            return;
        }
        mEngine = engine;
        try {
            engine.setVisibility(true);
            engine.resizePreview(new Rect(0, 0, mDisplaySize.x, mDisplaySize.y));
            engine.requestWallpaperColors();
        } catch (RemoteException e) {
            Log.w(TAG, "Unable to initialize live wallpaper", e);
            mListener.onConnectionFailed();
        }
    }

    @Override
    public void engineShown(IWallpaperEngine engine) {
        mSurfaceView.post(() -> {
            if (mDestroyed || mEngine == null) {
                return;
            }
            if (!mirrorAndReparent()) {
                if (mMirrorAttempts++ < 5) {
                    mSurfaceView.postDelayed(() -> engineShown(engine), 40L);
                } else {
                    mListener.onConnectionFailed();
                }
                return;
            }
            mMirrorAttempts = 0;
            if (!mReadyNotified) {
                mReadyNotified = true;
                mListener.onEngineReady();
            }
        });
    }

    private boolean mirrorAndReparent() {
        if (mMirrorSurface != null || mEngine == null || mSurfaceView.getSurfaceControl() == null) {
            return mMirrorSurface != null;
        }
        try {
            mMirrorSurface = mEngine.mirrorSurfaceControl();
            if (mMirrorSurface == null) {
                return false;
            }
            final float scaleX = (float) mSurfaceView.getWidth() / mDisplaySize.x;
            final float scaleY = (float) mSurfaceView.getHeight() / mDisplaySize.y;
            try (SurfaceControl.Transaction transaction = new SurfaceControl.Transaction()) {
                transaction.setMatrix(mMirrorSurface, scaleX, 0f, 0f, scaleY);
                transaction.reparent(mMirrorSurface, mSurfaceView.getSurfaceControl());
                transaction.show(mMirrorSurface);
                transaction.apply();
            }
            return true;
        } catch (RemoteException | RuntimeException e) {
            Log.w(TAG, "Unable to mirror live wallpaper", e);
            if (mMirrorSurface != null) {
                mMirrorSurface.release();
                mMirrorSurface = null;
            }
            return false;
        }
    }

    @Override
    public android.os.ParcelFileDescriptor setWallpaper(String name) {
        return null;
    }

    @Override
    public void onWallpaperColorsChanged(
            android.app.WallpaperColors colors,
            int displayId,
            android.app.WallpaperColors persistedColors) {
    }

    @Override
    public void onLocalWallpaperColorsChanged(
            android.graphics.RectF area,
            android.app.WallpaperColors colors,
            int displayId) {
    }

}
