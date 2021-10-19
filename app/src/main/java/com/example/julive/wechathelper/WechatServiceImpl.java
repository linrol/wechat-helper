package com.example.julive.wechathelper;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Path;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

@RequiresApi(api = Build.VERSION_CODES.N)
public class WechatServiceImpl extends AccessibilityService {

    private final Handler handler = new Handler();

    /**
     * 控制该动作只操作一次
     */
    private boolean isOneTime = true;

    @Override
    public void onInterrupt() {
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        String pkgName = event.getPackageName().toString();
        int eventType = event.getEventType();
        String className = event.getClassName().toString();
        String config = FileUtil.readLogByString(ConstantKt.getLogPath(), "0");
        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            switch (config) {
                case "1":
                    autoAddFriend(className);
                    break;
                case "2":
                    autoSendImage(className);
                    break;
                case "3":
                    autoShareMiniPrograms(className);
                    break;
                default:
                    System.out.println("default");
            }
        }
    }

    private void autoShareMiniPrograms(String className) {
        if (className.equals("com.tencent.mm.ui.LauncherUI")) {
            click("发现");
            handler.postDelayed(() -> click("小程序"), 1000L);
        }
        if (className.equals("com.tencent.mm.plugin.appbrand.ui.AppBrandLauncherUI")) {
            if (isOneTime) {
                performMenuDoubleClick(() -> {
                    click("搜索");
                    handler.postDelayed(() -> input("爆文来了"), 1000L);
                });
            }
        }
    }

    private void autoSendImage(String className) {
        if (className.equals("com.tencent.mm.ui.LauncherUI")) {
            click("发现");
            handler.postDelayed(() -> click("朋友圈"), 1000L);
        }
        if (className.equals("com.tencent.mm.plugin.sns.ui.SnsTimeLineUI")) {
            click("拍照分享");
            click("从相册选择");
        }
        if (className.equals("com.tencent.mm.plugin.gallery.ui.AlbumPreviewUI")) {
            if (isOneTime) {
                performMenuDoubleClick(() -> {
                    choosePicture(0, 2);
                });
            }
        }
        if (className.equals("com.tencent.mm.plugin.sns.ui.SnsUploadUI")) {
            click("发表");
            isOneTime = true;
            resetConfig();
        }
    }

    private void autoAddFriend(String className) {
        if (className.equals("com.tencent.mm.ui.LauncherUI")) {
            click("搜索");
        }
        if (className.equals("com.tencent.mm.plugin.fts.ui.FTSMainUI")) {
            input("1779561752", () -> click("查找手机/QQ号"));
        }
        if (className.equals("com.tencent.mm.plugin.profile.ui.ContactInfoUI")) {
            click("添加到通讯录");
        }
        if (className.equals("com.tencent.mm.plugin.profile.ui.SayHiWithSnsPermissionUI")) {
            input((rootNode -> {
                // 寻找设置备注输入框的node
                AccessibilityNodeInfo remarkNode = rootNode.findAccessibilityNodeInfosByText("设置备注").stream().findFirst().get();
                return remarkNode.getParent().getChild(4);
            }),"1779561752", () -> click("发送", () ->{
                // 成功后返回微信主页
                performBackClick(2);
            }, AccessibilityNodeInfo::isClickable));
            resetConfig();
        }
    }

    /**
     * 关闭监控
     */
    private void resetConfig() {
        FileUtil.writeLog(ConstantKt.getLogPath(), "0", false, "utf-8");
        stopSelf(); //走完一次流程 关闭自己，防止一直监控
    }

    /**
     * 点击匹配的nodeInfo
     *
     * @param str text关键字
     */
    private void click(String str) {
        click(str, null, null, false);
    }
    private void click(String str, Runnable runnable) {
        click(str, runnable, null, false);
    }
    private void click(String str, Predicate<AccessibilityNodeInfo> onlyOneFilter) {
        click(str, null, onlyOneFilter, false);
    }
    private void click(String str, Runnable runnable, Predicate<AccessibilityNodeInfo> onlyOneFilter) {
        click(str, runnable, onlyOneFilter, false);
    }
    private void clickById(String str) {
        click(str, null, null, true);
    }
    private void click(String str, Runnable runnable, Predicate<AccessibilityNodeInfo> onlyOneFilter, boolean searchById) {
        int clickAction = AccessibilityNodeInfo.ACTION_CLICK;
        handler.postDelayed(() -> {
            AccessibilityNodeInfo rootNode = getRootInActiveWindow();
            if (rootNode == null) {
                Toast.makeText(this, "rootWindow为空", Toast.LENGTH_SHORT).show();
                return;
            }
            List<AccessibilityNodeInfo> list;
            if (searchById) {
                list = rootNode.findAccessibilityNodeInfosByViewId(str);
            }else {
                list = rootNode.findAccessibilityNodeInfosByText(str);
            }
            if (list == null || list.isEmpty()) {
                Toast.makeText(this, String.format("click 根据:[%s]未找到节点信息", str), Toast.LENGTH_SHORT).show();
                rootNode.recycle();
                return;
            }
            AccessibilityNodeInfo clickNode = list.get(0);
            if(list.size() > 1 && onlyOneFilter != null){
                clickNode = list.stream().filter(onlyOneFilter).findFirst().orElse(clickNode);
            }
            clickNode.performAction(clickAction);
            clickNode.getParent().performAction(clickAction);
            if(runnable != null) {
                runnable.run();
            }
            rootNode.recycle();
        }, 6000L);
    }

    //找到当前焦点的view进行数输入
    private void input(String str) {
        input((rootNode) -> rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT), str,null);
    }
    private void input(String str, Runnable runnable) {
        input((rootNode) -> rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT), str, runnable);
    }
    private void input(Function<AccessibilityNodeInfo, AccessibilityNodeInfo> targetNodeFunction, String str, Runnable runnable) {
        handler.postDelayed(() -> {
            AccessibilityNodeInfo rootNode = getRootInActiveWindow();
            if (rootNode == null) {
                Toast.makeText(this, "rootWindow为空", Toast.LENGTH_SHORT).show();
                return;
            }
            AccessibilityNodeInfo target = targetNodeFunction.apply(rootNode);
            if (target == null) {
                log("input: null");
                return;
            }
            Bundle arguments = new Bundle();
            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, str);
            target.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
            rootNode.recycle();
            if(runnable != null) {
                runnable.run();
            }
        }, 6000L);
    }

    private void log(String config) {
        Log.d("AccessibilityNodeInfo", config);
    }

    /**
     * 打开通知栏消息
     */
    private void openNotification(AccessibilityEvent event) {
        if (event.getParcelableData() == null) {
            return;
        }
        if (!(event.getParcelableData() instanceof Notification)) {
            return;
        }
        //将通知栏消息打开
        Notification notification = (Notification) event.getParcelableData();
        PendingIntent pendingIntent = notification.contentIntent;
        try {
            pendingIntent.send();
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }
    }

    /**
     * 点击回退按钮
     */
    private void performBackClick(int number) {
        if(number < 1) {
            return;
        }
        handler.postDelayed(() -> {
            performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
            performBackClick(number -1);
        }, 2000L);
    }

    /**
     * 回主页
     */
    private void performHomeClick() {
        handler.postDelayed(() -> performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME), 2000L);
    }

    /**
     * 点击菜单按钮
     */
    private void performMenuClick() {
        handler.postDelayed(() -> performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS), 2000L);
    }


    /**
     * 点击菜单按钮后一秒再点击按钮返回
     * 目的为了刷新当前页面，拿到当前页根节点
     */
    private void performMenuDoubleClick(Runnable runnable) {
        performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS);
        handler.postDelayed(() -> {
            performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
            runnable.run();
            isOneTime = false;
        }, 1000L);
    }

    /**
     * 点击选项框
     */
    private boolean performClickBtn(List<AccessibilityNodeInfo> accessibilityNodeInfoList) {
        if (accessibilityNodeInfoList == null || accessibilityNodeInfoList.isEmpty()) {
            return false;
        }
        return accessibilityNodeInfoList.stream().anyMatch(accessibilityNodeInfo -> {
            if (accessibilityNodeInfo.isClickable() && accessibilityNodeInfo.isEnabled()) {
                accessibilityNodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                return true;
            }
            return false;
        });
    }

    /**
     * 选择图片
     *
     * @param startPicIndex 从第startPicIndex张开始选
     * @param picCount      总共选picCount张
     */
    private void choosePicture(int startPicIndex, int picCount) {
        handler.postDelayed(() -> {
            AccessibilityNodeInfo accessibilityNodeInfo = getRootInActiveWindow();
            if (accessibilityNodeInfo == null) {
                Toast.makeText(this, "accessibilityNodeInfo is null", Toast.LENGTH_SHORT).show();
                return;
            }
            List<AccessibilityNodeInfo> accessibilityNodeInfoList = accessibilityNodeInfo.findAccessibilityNodeInfosByText("预览");
            if (accessibilityNodeInfoList == null ||
                    accessibilityNodeInfoList.size() == 0 ||
                    accessibilityNodeInfoList.get(0).getParent() == null ||
                    accessibilityNodeInfoList.get(0).getParent().getChildCount() == 0) {
                return;
            }
            AccessibilityNodeInfo tempInfo = accessibilityNodeInfoList.get(0).getParent().getChild(3);
            for (int i = startPicIndex; i < startPicIndex + picCount; i++) {
                AccessibilityNodeInfo childNodeInfo = tempInfo.getChild(i);
                if (childNodeInfo == null) {
                    continue;
                }
                for (int j = 0; j < childNodeInfo.getChildCount(); j++) {
                    if (childNodeInfo.getChild(j).isEnabled() && childNodeInfo.getChild(j).isClickable()) {
                        childNodeInfo.getChild(j).performAction(AccessibilityNodeInfo.ACTION_CLICK); //选中图片
                    }
                }
            }
            List<AccessibilityNodeInfo> finishList = accessibilityNodeInfo.findAccessibilityNodeInfosByText("完成($picCount/9)"); //点击确定
            performClickBtn(finishList);
        }, 2000L);
    }


    /**
     * 垂直滑动
     * 滑动比例 0~20
     */
    private void slideVertical(int startSlideRatio, int stopSlideRatio) {
        log("slideVertical");
        int screenHeight = getScreenHeight(this);
        int screenWidth = getScreenWidth(this);
        log("screenHeight $screenHeight");
        log("screenWidth $screenWidth");
        Path path = new Path();
        int start = screenHeight / 20 * startSlideRatio;
        int stop = screenHeight / 20 * stopSlideRatio;
        path.moveTo((float) (screenWidth / 2), (float) start); //如果只是设置moveTo就是点击
        path.lineTo((float) (screenWidth / 2), (float) stop); //如果设置这句就是滑动

        GestureDescription.Builder builder = new GestureDescription.Builder();
        GestureDescription gestureDescription = builder.addStroke(new GestureDescription.StrokeDescription(path, 0, 500)).build();

        dispatchGesture(gestureDescription, new GestureResultCallback() {
            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                super.onCancelled(gestureDescription);
                log("onCancelled");
            }

            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                super.onCompleted(gestureDescription);
                log("onCompleted");
            }
        }, null);
    }

    private int getScreenWidth(Context context) {
        Object systemService = context.getSystemService(Context.WINDOW_SERVICE);
        WindowManager wm = (WindowManager) systemService;
        DisplayMetrics outMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(outMetrics);
        return outMetrics.widthPixels;
    }

    private int getScreenHeight(Context context) {
        Object systemService = context.getSystemService(Context.WINDOW_SERVICE);
        WindowManager wm = (WindowManager) systemService;
        DisplayMetrics outMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(outMetrics);
        return outMetrics.heightPixels;
    }
}
