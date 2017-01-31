package com.massivedisaster.adal.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;


public class BangBus {

    private final static String sArgumentData = "argument_bang_data";

    private Context mContext;
    private List<BroadcastReceiver> mLstBroadcastReceivers;
    private Object mClazz;

    public BangBus(Context context, final Object clazz) {
        mLstBroadcastReceivers = new LinkedList<>();
        mContext = context;
        mClazz = clazz;
    }

    public void bang(Serializable serializable) {
        bang(mContext, serializable);
    }

    public static void bang(Context context, Serializable serializable) {
        Intent intent = new Intent(serializable.getClass().getCanonicalName());
        intent.putExtra(sArgumentData, serializable);

        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public BangBus subscribe() {
        mLstBroadcastReceivers = new ArrayList<>();

        List<Method> lstMethods = getMethodsAnnotatedWith(mClazz.getClass(), SubscribeBang.class);

        for (final Method m : lstMethods) {
            BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent.hasExtra(sArgumentData)) {
                        Object object = intent.getSerializableExtra(sArgumentData);

                        try {
                            m.setAccessible(true);
                            m.invoke(mClazz, object);
                            m.setAccessible(false);
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        } catch (InvocationTargetException e) {
                            e.printStackTrace();
                        }
                    }
                }
            };

            mLstBroadcastReceivers.add(mBroadcastReceiver);
            LocalBroadcastManager.getInstance(mContext).registerReceiver(mBroadcastReceiver, new IntentFilter(m.getParameterTypes()[0].getCanonicalName()));
        }

        return this;
    }

    public void unsubscribe() {
        for (BroadcastReceiver broadcastReceiver : mLstBroadcastReceivers) {
            LocalBroadcastManager.getInstance(mContext).unregisterReceiver(broadcastReceiver);
        }
    }

    private static List<Method> getMethodsAnnotatedWith(final Class<?> type, final Class<? extends Annotation> annotation) {
        final List<Method> methods = new ArrayList<>();
        Class<?> klass = type;
        while (klass != Object.class) {
            final List<Method> allMethods = new ArrayList<>(Arrays.asList(klass.getDeclaredMethods()));
            for (final Method method : allMethods) {
                if (method.isAnnotationPresent(annotation)) {
                    if (method.getParameterTypes().length == 1)
                        methods.add(method);
                }
            }
            klass = klass.getSuperclass();
        }
        return methods;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface SubscribeBang {
    }
}
