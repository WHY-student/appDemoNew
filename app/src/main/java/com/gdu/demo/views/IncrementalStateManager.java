package com.gdu.demo.views;

// IncrementalStateManager.java
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class IncrementalStateManager {
    private static IncrementalStateManager instance;
    private boolean isIncremental;
    private final List<WeakReference<StateCallback>> callbacks = new ArrayList<>();

    public interface StateCallback {
        void onStateChanged(boolean isIncremental);
    }

    public static synchronized IncrementalStateManager getInstance() {
        if (instance == null) {
            instance = new IncrementalStateManager();
        }
        return instance;
    }

    public void setIncremental(boolean incremental) {
        if (this.isIncremental != incremental) {
            this.isIncremental = incremental;
            notifyStateChanged();
        }
    }

    // 注册回调（使用弱引用）
    public void registerCallback(StateCallback callback) {
        callbacks.add(new WeakReference<>(callback));
    }

    private void notifyStateChanged() {
        List<WeakReference<StateCallback>> toRemove = new ArrayList<>();

        for (WeakReference<StateCallback> ref : callbacks) {
            StateCallback callback = ref.get();
            if (callback != null) {
                callback.onStateChanged(isIncremental);
            } else {
                toRemove.add(ref); // 清理无效引用
            }
        }

        callbacks.removeAll(toRemove);
    }
    // IncrementalStateManager.java
    public boolean isIncremental() {
        return this.isIncremental;
    }
}