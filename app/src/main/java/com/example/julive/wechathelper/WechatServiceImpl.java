package com.example.julive.wechathelper;

import android.accessibilityservice.AccessibilityService;
import android.os.Handler;
import android.view.accessibility.AccessibilityEvent;

public class WechatServiceImpl extends AccessibilityService {

    private Handler handler = new Handler();

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        String pkgName = event.getPackageName().toString();
        int eventType = event.getEventType();
        String className = event.getClassName().toString();
        String config = FileUtil.readLogByString(ConstantKt.getLogPath(), "0");
        if(eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED){
            switch (config){
                case "1":
                    System.out.println("add");
                    break;
                case "2":
                    System.out.println("autoSendImage");
                    break;
                default:
                    System.out.println("default");
            }
        }
    }

    @Override
    public void onInterrupt() {

    }
}
