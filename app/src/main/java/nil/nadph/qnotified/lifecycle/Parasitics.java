/*
 * QNotified - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2022 dmca@ioctl.cc
 * https://github.com/ferredoxin/QNotified
 *
 * This software is non-free but opensource software: you can redistribute it
 * and/or modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version and our eula as published
 * by ferredoxin.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and eula along with this software.  If not, see
 * <https://www.gnu.org/licenses/>
 * <https://github.com/ferredoxin/QNotified/blob/master/LICENSE.md>.
 */
package nil.nadph.qnotified.lifecycle;

import static nil.nadph.qnotified.util.Utils.log;
import static nil.nadph.qnotified.util.Utils.logd;
import static nil.nadph.qnotified.util.Utils.loge;
import static nil.nadph.qnotified.util.Utils.logi;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.app.Instrumentation;
import android.app.UiAutomation;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.TestLooperManager;
import android.view.KeyEvent;
import android.view.MotionEvent;
import androidx.annotation.Nullable;
import cc.ioctl.H;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import me.singleneuron.qn_kernel.data.HostInfo;
import nil.nadph.qnotified.MainHook;
import nil.nadph.qnotified.R;
import nil.nadph.qnotified.startup.StartupInfo;
import nil.nadph.qnotified.ui.___WindowIsTranslucent;
import nil.nadph.qnotified.util.Initiator;
import nil.nadph.qnotified.util.MainProcess;

/**
 * Inject module Activities into host process and resources injection. Deprecated, private, internal
 * or other restricted APIs will be used.
 *
 * @author cinit
 */
public class Parasitics {

    private static boolean __stub_hooked = false;
    private static long sResInjectBeginTime = 0;
    private static long sResInjectEndTime = 0;
    private static long sActStubHookBeginTime = 0;
    private static long sActStubHookEndTime = 0;

    public static int getResourceInjectionCost() {
        if (sResInjectEndTime > 0) {
            return (int) (sResInjectEndTime - sResInjectBeginTime);
        }
        return -1;
    }

    public static int getActivityStubHookCost() {
        if (sActStubHookEndTime > 0) {
            return (int) (sActStubHookEndTime - sActStubHookBeginTime);
        }
        return -1;
    }

    @MainProcess
    @SuppressLint("PrivateApi")
    public static void injectModuleResources(Resources res) {
        if (res == null) {
            return;
        }
        try {
            res.getString(R.string.res_inject_success);
            return;
        } catch (Resources.NotFoundException ignored) {
        }
        try {
            String sModulePath = StartupInfo.modulePath;
            if (sModulePath == null) {
                throw new RuntimeException(
                    "get module path failed, loader=" + MainHook.class.getClassLoader());
            }
            AssetManager assets = res.getAssets();
            @SuppressLint("DiscouragedPrivateApi")
            Method addAssetPath = AssetManager.class
                .getDeclaredMethod("addAssetPath", String.class);
            addAssetPath.setAccessible(true);
            int cookie = (int) addAssetPath.invoke(assets, sModulePath);
            try {
                logd("injectModuleResources: " + res.getString(R.string.res_inject_success));
                if (sResInjectEndTime == 0) {
                    sResInjectEndTime = System.currentTimeMillis();
                }
            } catch (Resources.NotFoundException e) {
                loge("Fatal: injectModuleResources: test injection failure!");
                loge("injectModuleResources: cookie=" + cookie + ", path=" + sModulePath
                    + ", loader=" + MainHook.class.getClassLoader());
                long length = -1;
                boolean read = false;
                boolean exist = false;
                boolean isDir = false;
                try {
                    File f = new File(sModulePath);
                    exist = f.exists();
                    isDir = f.isDirectory();
                    length = f.length();
                    read = f.canRead();
                } catch (Throwable e2) {
                    log(e2);
                }
                loge("sModulePath: exists = " + exist + ", isDirectory = " + isDir + ", canRead = "
                    + read + ", fileLength = " + length);
            }
        } catch (Exception e) {
            log(e);
        }
    }

    @MainProcess
    @SuppressLint("PrivateApi")
    public static void initForStubActivity(Context ctx) {
        if (__stub_hooked) {
            return;
        }
        try {
            sActStubHookBeginTime = System.currentTimeMillis();
            Class<?> clazz_ActivityThread = Class.forName("android.app.ActivityThread");
            Method currentActivityThread = clazz_ActivityThread
                .getDeclaredMethod("currentActivityThread");
            currentActivityThread.setAccessible(true);
            Object sCurrentActivityThread = currentActivityThread.invoke(null);
            Field mInstrumentation = clazz_ActivityThread.getDeclaredField("mInstrumentation");
            mInstrumentation.setAccessible(true);
            Instrumentation instrumentation = (Instrumentation) mInstrumentation
                .get(sCurrentActivityThread);
            mInstrumentation.set(sCurrentActivityThread, new MyInstrumentation(instrumentation));
            //End of Instrumentation
            Field field_mH = clazz_ActivityThread.getDeclaredField("mH");
            field_mH.setAccessible(true);
            Handler oriHandler = (Handler) field_mH.get(sCurrentActivityThread);
            Field field_mCallback = Handler.class.getDeclaredField("mCallback");
            field_mCallback.setAccessible(true);
            Handler.Callback current = (Handler.Callback) field_mCallback.get(oriHandler);
            if (current == null || !current.getClass().getName().equals(MyH.class.getName())) {
                field_mCallback.set(oriHandler, new MyH(current));
            }
            //End of Handler
            Class<?> activityManagerClass;
            Field gDefaultField;
            try {
                activityManagerClass = Class.forName("android.app.ActivityManagerNative");
                gDefaultField = activityManagerClass.getDeclaredField("gDefault");
            } catch (Exception err1) {
                try {
                    activityManagerClass = Class.forName("android.app.ActivityManager");
                    gDefaultField = activityManagerClass
                        .getDeclaredField("IActivityManagerSingleton");
                } catch (Exception err2) {
                    logi("WTF: Unable to get IActivityManagerSingleton");
                    log(err1);
                    log(err2);
                    return;
                }
            }
            gDefaultField.setAccessible(true);
            Object gDefault = gDefaultField.get(null);
            Class<?> singletonClass = Class.forName("android.util.Singleton");
            Field mInstanceField = singletonClass.getDeclaredField("mInstance");
            mInstanceField.setAccessible(true);
            Object mInstance = mInstanceField.get(gDefault);
            Object amProxy = Proxy.newProxyInstance(
                Initiator.getPluginClassLoader(),
                new Class[]{Class.forName("android.app.IActivityManager")},
                new IActivityManagerHandler(mInstance));
            mInstanceField.set(gDefault, amProxy);
            //End of IActivityManager
            try {
                Class<?> activityTaskManagerClass = Class
                    .forName("android.app.ActivityTaskManager");
                Field fIActivityTaskManagerSingleton = activityTaskManagerClass
                    .getDeclaredField("IActivityTaskManagerSingleton");
                fIActivityTaskManagerSingleton.setAccessible(true);
                Object singleton = fIActivityTaskManagerSingleton.get(null);
                singletonClass.getMethod("get").invoke(singleton);
                Object mDefaultTaskMgr = mInstanceField.get(singleton);
                Object proxy2 = Proxy.newProxyInstance(
                    Initiator.getPluginClassLoader(),
                    new Class[]{Class.forName("android.app.IActivityTaskManager")},
                    new IActivityManagerHandler(mDefaultTaskMgr));
                mInstanceField.set(singleton, proxy2);
            } catch (Exception err3) {
            }
            //End of IActivityTaskManager
            //Begin of PackageManager
            Field sPackageManagerField = clazz_ActivityThread.getDeclaredField("sPackageManager");
            sPackageManagerField.setAccessible(true);
            Object packageManagerImpl = sPackageManagerField.get(sCurrentActivityThread);
            Class<?> iPackageManagerInterface = Class.forName("android.content.pm.IPackageManager");
            PackageManager pm = ctx.getPackageManager();
            Field mPmField = pm.getClass().getDeclaredField("mPM");
            mPmField.setAccessible(true);
            Object pmProxy = Proxy.newProxyInstance(iPackageManagerInterface.getClassLoader(),
                new Class[]{iPackageManagerInterface},
                new PackageManagerInvocationHandler(packageManagerImpl));
            sPackageManagerField.set(currentActivityThread, pmProxy);
            mPmField.set(pm, pmProxy);
            //End of PackageManager
            sActStubHookEndTime = System.currentTimeMillis();
            __stub_hooked = true;
        } catch (Exception e) {
            log(e);
        }
    }

    public static class IActivityManagerHandler implements InvocationHandler {

        private final Object mOrigin;

        public IActivityManagerHandler(Object origin) {
            mOrigin = origin;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ("startActivity".equals(method.getName())) {
                int index = -1;
                for (int i = 0; i < args.length; i++) {
                    if (args[i] instanceof Intent) {
                        index = i;
                        break;
                    }
                }
                if (index != -1) {
                    Intent raw = (Intent) args[index];
                    ComponentName component = raw.getComponent();
                    Context hostApp = HostInfo.getHostInfo().getApplication();
                    if (hostApp != null && component != null
                        && hostApp.getPackageName().equals(component.getPackageName())
                        && ActProxyMgr.isModuleProxyActivity(component.getClassName())) {
                        boolean isTranslucent = false;
                        try {
                            Class<?> targetActivity = Class.forName(component.getClassName());
                            if (targetActivity != null && (___WindowIsTranslucent.class
                                .isAssignableFrom(targetActivity))) {
                                isTranslucent = true;
                            }
                        } catch (ClassNotFoundException ignored) {
                        }
                        Intent wrapper = new Intent();
                        wrapper.setClassName(component.getPackageName(),
                            isTranslucent ? ActProxyMgr.STUB_TRANSLUCENT_ACTIVITY
                                : ActProxyMgr.STUB_DEFAULT_ACTIVITY);
                        wrapper.putExtra(ActProxyMgr.ACTIVITY_PROXY_INTENT, raw);
                        args[index] = wrapper;
                    }
                }
            }
            try {
                return method.invoke(mOrigin, args);
            } catch (InvocationTargetException ite) {
                throw ite.getTargetException();
            }
        }
    }

    public static class MyH implements Handler.Callback {

        private final Handler.Callback mDefault;

        public MyH(Handler.Callback def) {
            mDefault = def;
        }

        @Override
        public boolean handleMessage(Message msg) {
            if (msg.what == 100) { // LAUNCH_ACTIVITY
                try {
                    Object record = msg.obj;
                    Field field_intent = record.getClass().getDeclaredField("intent");
                    field_intent.setAccessible(true);
                    Intent intent = (Intent) field_intent.get(record);
                    Bundle bundle = null;
                    try {
                        Field fExtras = Intent.class.getDeclaredField("mExtras");
                        fExtras.setAccessible(true);
                        bundle = (Bundle) fExtras.get(intent);
                    } catch (Exception e) {
                        log(e);
                    }
                    if (bundle != null) {
                        bundle.setClassLoader(Initiator.getHostClassLoader());
                        //we do NOT have a custom Bundle, but the host may have
                        if (intent.hasExtra(ActProxyMgr.ACTIVITY_PROXY_INTENT)) {
                            Intent realIntent = intent
                                .getParcelableExtra(ActProxyMgr.ACTIVITY_PROXY_INTENT);
                            field_intent.set(record, realIntent);
                        }
                    }
                } catch (Exception e) {
                    log(e);
                }
            } else if (msg.what == 159) {
                // EXECUTE_TRANSACTION
                Object clientTransaction = msg.obj;
                try {
                    if (clientTransaction != null) {
                        Method getCallbacks = Class
                            .forName("android.app.servertransaction.ClientTransaction")
                            .getDeclaredMethod("getCallbacks");
                        getCallbacks.setAccessible(true);
                        List clientTransactionItems = (List) getCallbacks.invoke(clientTransaction);
                        if (clientTransactionItems != null && clientTransactionItems.size() > 0) {
                            for (Object item : clientTransactionItems) {
                                Class c = item.getClass();
                                if (c.getName().contains("LaunchActivityItem")) {
                                    Field fmIntent = c.getDeclaredField("mIntent");
                                    fmIntent.setAccessible(true);
                                    Intent wrapper = (Intent) fmIntent.get(item);
                                    Bundle bundle = null;
                                    try {
                                        Field fExtras = Intent.class.getDeclaredField("mExtras");
                                        fExtras.setAccessible(true);
                                        bundle = (Bundle) fExtras.get(wrapper);
                                    } catch (Exception e) {
                                        log(e);
                                    }
                                    if (bundle != null) {
                                        bundle.setClassLoader(Initiator.getHostClassLoader());
                                        if (wrapper.hasExtra(ActProxyMgr.ACTIVITY_PROXY_INTENT)) {
                                            Intent realIntent = wrapper.getParcelableExtra(
                                                ActProxyMgr.ACTIVITY_PROXY_INTENT);
                                            fmIntent.set(item, realIntent);
                                            if (Build.VERSION.SDK_INT >= 31) {
                                                IBinder token = (IBinder) clientTransaction.getClass().getMethod("getActivityToken").invoke(clientTransaction);
                                                Class<?> clazz_ActivityThread = Class.forName("android.app.ActivityThread");
                                                Method currentActivityThread = clazz_ActivityThread.getDeclaredMethod("currentActivityThread");
                                                currentActivityThread.setAccessible(true);
                                                Object activityThread = currentActivityThread.invoke(null);
                                                // Accessing hidden method Landroid/app/ClientTransactionHandler;->getLaunchingActivity(Landroid/os/IBinder;)Landroid/app/ActivityThread$ActivityClientRecord; (blocked, reflection, denied)
                                                // Accessing hidden method Landroid/app/ActivityThread;->getLaunchingActivity(Landroid/os/IBinder;)Landroid/app/ActivityThread$ActivityClientRecord; (blocked, reflection, denied)
                                                Object acr = activityThread.getClass().getMethod("getLaunchingActivity", IBinder.class).invoke(activityThread, token);
                                                if (acr != null) {
                                                    Field fAcrIntent = acr.getClass().getDeclaredField("intent");
                                                    fAcrIntent.setAccessible(true);
                                                    fAcrIntent.set(acr, realIntent);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    log(e);
                }
            }
            if (mDefault != null) {
                return mDefault.handleMessage(msg);
            }
            return false;
        }
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    public static class MyInstrumentation extends Instrumentation {

        private final Instrumentation mBase;

        public MyInstrumentation(Instrumentation base) {
            this.mBase = base;
        }

        @Override
        public Activity newActivity(ClassLoader cl, String className, Intent intent)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
            try {
                return mBase.newActivity(cl, className, intent);
            } catch (Exception e) {
                if (ActProxyMgr.isModuleProxyActivity(className)) {
                    return (Activity) Initiator.class.getClassLoader().loadClass(className)
                        .newInstance();
                }
                throw e;
            }
        }

        @Override
        public void onCreate(Bundle arguments) {
            mBase.onCreate(arguments);
        }

        @Override
        public void start() {
            mBase.start();
        }

        @Override
        public void onStart() {
            mBase.onStart();
        }

        @Override
        public boolean onException(Object obj, Throwable e) {
            return mBase.onException(obj, e);
        }

        @Override
        public void sendStatus(int resultCode, Bundle results) {
            mBase.sendStatus(resultCode, results);
        }

        @Override
        public void addResults(Bundle results) {
            mBase.addResults(results);
        }

        @Override
        public void finish(int resultCode, Bundle results) {
            mBase.finish(resultCode, results);
        }

        @Override
        public void setAutomaticPerformanceSnapshots() {
            mBase.setAutomaticPerformanceSnapshots();
        }

        @Override
        public void startPerformanceSnapshot() {
            mBase.startPerformanceSnapshot();
        }

        @Override
        public void endPerformanceSnapshot() {
            mBase.endPerformanceSnapshot();
        }

        @Override
        public void onDestroy() {
            mBase.onDestroy();
        }

        @Override
        public Context getContext() {
            return mBase.getContext();
        }

        @Override
        public ComponentName getComponentName() {
            return mBase.getComponentName();
        }

        @Override
        public Context getTargetContext() {
            return mBase.getTargetContext();
        }


        @Override
        public String getProcessName() {
            return mBase.getProcessName();
        }

        @Override
        public boolean isProfiling() {
            return mBase.isProfiling();
        }

        @Override
        public void startProfiling() {
            mBase.startProfiling();
        }

        @Override
        public void stopProfiling() {
            mBase.stopProfiling();
        }

        @Override
        public void setInTouchMode(boolean inTouch) {
            mBase.setInTouchMode(inTouch);
        }

        @Override
        public void waitForIdle(Runnable recipient) {
            mBase.waitForIdle(recipient);
        }

        @Override
        public void waitForIdleSync() {
            mBase.waitForIdleSync();
        }

        @Override
        public void runOnMainSync(Runnable runner) {
            mBase.runOnMainSync(runner);
        }

        @Override
        public Activity startActivitySync(Intent intent) {
            return mBase.startActivitySync(intent);
        }

        @Override
        public Activity startActivitySync(Intent intent, Bundle options) {
            return super.startActivitySync(intent, options);
        }

        @Override
        public void addMonitor(ActivityMonitor monitor) {
            mBase.addMonitor(monitor);
        }

        @Override
        public ActivityMonitor addMonitor(IntentFilter filter, ActivityResult result,
            boolean block) {
            return mBase.addMonitor(filter, result, block);
        }

        @Override
        public ActivityMonitor addMonitor(String cls, ActivityResult result, boolean block) {
            return mBase.addMonitor(cls, result, block);
        }

        @Override
        public boolean checkMonitorHit(ActivityMonitor monitor, int minHits) {
            return mBase.checkMonitorHit(monitor, minHits);
        }

        @Override
        public Activity waitForMonitor(ActivityMonitor monitor) {
            return mBase.waitForMonitor(monitor);
        }

        @Override
        public Activity waitForMonitorWithTimeout(ActivityMonitor monitor, long timeOut) {
            return mBase.waitForMonitorWithTimeout(monitor, timeOut);
        }

        @Override
        public void removeMonitor(ActivityMonitor monitor) {
            mBase.removeMonitor(monitor);
        }

        @Override
        public boolean invokeMenuActionSync(Activity targetActivity, int id, int flag) {
            return mBase.invokeMenuActionSync(targetActivity, id, flag);
        }

        @Override
        public boolean invokeContextMenuAction(Activity targetActivity, int id, int flag) {
            return mBase.invokeContextMenuAction(targetActivity, id, flag);
        }

        @Override
        public void sendStringSync(String text) {
            mBase.sendStringSync(text);
        }

        @Override
        public void sendKeySync(KeyEvent event) {
            mBase.sendKeySync(event);
        }

        @Override
        public void sendKeyDownUpSync(int key) {
            mBase.sendKeyDownUpSync(key);
        }

        @Override
        public void sendCharacterSync(int keyCode) {
            mBase.sendCharacterSync(keyCode);
        }

        @Override
        public void sendPointerSync(MotionEvent event) {
            mBase.sendPointerSync(event);
        }

        @Override
        public void sendTrackballEventSync(MotionEvent event) {
            mBase.sendTrackballEventSync(event);
        }

        @Override
        public Application newApplication(ClassLoader cl, String className, Context context)
            throws ClassNotFoundException, IllegalAccessException, InstantiationException {
            return mBase.newApplication(cl, className, context);
        }

        @Override
        public void callApplicationOnCreate(Application app) {
            mBase.callApplicationOnCreate(app);
        }

        @Override
        public Activity newActivity(Class<?> clazz, Context context, IBinder token,
            Application application, Intent intent, ActivityInfo info, CharSequence title,
            Activity parent, String id, Object lastNonConfigurationInstance)
            throws IllegalAccessException, InstantiationException {
            return mBase
                .newActivity(clazz, context, token, application, intent, info, title, parent, id,
                    lastNonConfigurationInstance);
        }

        @Override
        public void callActivityOnCreate(Activity activity, Bundle icicle) {
            if (icicle != null) {
                String className = activity.getClass().getName();
                if (ActProxyMgr.isModuleBundleClassLoaderRequired(className)) {
                    icicle.setClassLoader(MainHook.class.getClassLoader());
                }
            }
            injectModuleResources(activity.getResources());
            mBase.callActivityOnCreate(activity, icicle);
        }

        @Override
        public void callActivityOnCreate(Activity activity, Bundle icicle,
            PersistableBundle persistentState) {
            if (icicle != null) {
                String className = activity.getClass().getName();
                if (ActProxyMgr.isModuleBundleClassLoaderRequired(className)) {
                    icicle.setClassLoader(MainHook.class.getClassLoader());
                }
            }
            injectModuleResources(activity.getResources());
            mBase.callActivityOnCreate(activity, icicle, persistentState);
        }

        @Override
        public void callActivityOnDestroy(Activity activity) {
            mBase.callActivityOnDestroy(activity);
        }

        @Override
        public void callActivityOnRestoreInstanceState(Activity activity,
            Bundle savedInstanceState) {
            mBase.callActivityOnRestoreInstanceState(activity, savedInstanceState);
        }


        @Override
        public void callActivityOnRestoreInstanceState(Activity activity, Bundle savedInstanceState,
            PersistableBundle persistentState) {
            mBase.callActivityOnRestoreInstanceState(activity, savedInstanceState, persistentState);
        }

        @Override
        public void callActivityOnPostCreate(Activity activity, Bundle savedInstanceState) {
            mBase.callActivityOnPostCreate(activity, savedInstanceState);
        }

        @Override
        public void callActivityOnPostCreate(Activity activity, @Nullable Bundle savedInstanceState,
            @Nullable PersistableBundle persistentState) {
            mBase.callActivityOnPostCreate(activity, savedInstanceState, persistentState);
        }

        @Override
        public void callActivityOnNewIntent(Activity activity, Intent intent) {
            mBase.callActivityOnNewIntent(activity, intent);
        }

        @Override
        public void callActivityOnStart(Activity activity) {
            mBase.callActivityOnStart(activity);
        }

        @Override
        public void callActivityOnRestart(Activity activity) {
            mBase.callActivityOnRestart(activity);
        }

        @Override
        public void callActivityOnResume(Activity activity) {
            mBase.callActivityOnResume(activity);
        }

        @Override
        public void callActivityOnStop(Activity activity) {
            mBase.callActivityOnStop(activity);
        }

        @Override
        public void callActivityOnSaveInstanceState(Activity activity, Bundle outState) {
            mBase.callActivityOnSaveInstanceState(activity, outState);
        }

        @Override
        public void callActivityOnSaveInstanceState(Activity activity, Bundle outState,
            PersistableBundle outPersistentState) {
            mBase.callActivityOnSaveInstanceState(activity, outState, outPersistentState);
        }

        @Override
        public void callActivityOnPause(Activity activity) {
            mBase.callActivityOnPause(activity);
        }

        @Override
        public void callActivityOnUserLeaving(Activity activity) {
            mBase.callActivityOnUserLeaving(activity);
        }

        @Override
        public void startAllocCounting() {
            mBase.startAllocCounting();
        }

        @Override
        public void stopAllocCounting() {
            mBase.stopAllocCounting();
        }

        @Override
        public Bundle getAllocCounts() {
            return mBase.getAllocCounts();
        }

        @Override
        public Bundle getBinderCounts() {
            return mBase.getBinderCounts();
        }

        @Override
        public UiAutomation getUiAutomation() {
            return mBase.getUiAutomation();
        }

        @Override
        public UiAutomation getUiAutomation(int flags) {
            return mBase.getUiAutomation(flags);
        }

        @Override
        public TestLooperManager acquireLooperManager(Looper looper) {
            return mBase.acquireLooperManager(looper);
        }

    }

    public static class PackageManagerInvocationHandler implements InvocationHandler {

        private final Object target;

        public PackageManagerInvocationHandler(Object target) {
            if (target == null) {
                throw new NullPointerException("IPackageManager == null");
            }
            this.target = target;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // prototype: ActivityInfo getActivityInfo(in ComponentName className, int flags, int userId)
            try {
                if ("getActivityInfo".equals(method.getName())) {
                    ActivityInfo ai = (ActivityInfo) method.invoke(target, args);
                    if (ai != null) {
                        return ai;
                    }
                    ComponentName component = (ComponentName) args[0];
                    int flags = (Integer) args[1];
                    if (H.getPackageName().equals(component.getPackageName())
                        && ActProxyMgr.isModuleProxyActivity(component.getClassName())) {
                        return CounterfeitActivityInfoFactory
                            .makeProxyActivityInfo(component.getClassName(), flags);
                    } else {
                        return null;
                    }
                } else {
                    return method.invoke(target, args);
                }
            } catch (InvocationTargetException ite) {
                throw ite.getTargetException();
            }
        }
    }
}
