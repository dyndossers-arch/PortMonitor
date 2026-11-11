package com.portmonitor.app;

import android.app.*;
import android.content.*;
import android.os.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.*;

public class MonitorService extends Service {
    public static final String ACTION_CHECK_NOW = "com.portmonitor.app.CHECK_NOW";
    private static final String CHANNEL_SERVICE = "monitor_service";
    private static final String CHANNEL_ALERTS = "port_alerts";
    private static final int FOREGROUND_ID = 10;
    private ScheduledExecutorService scheduler;
    private final ExecutorService workers = Executors.newFixedThreadPool(12);

    @Override public void onCreate() {
        super.onCreate();
        createChannels();
        startForeground(FOREGROUND_ID, serviceNotification("Мониторинг активен · проверка каждые 30 секунд"));
        Store.setRunning(this, true);
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::checkAll, 0, 30, TimeUnit.SECONDS);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_CHECK_NOW.equals(intent.getAction())) {
            workers.execute(this::checkAll);
        }
        return START_STICKY;
    }

    private void checkAll() {
        List<Endpoint> endpoints = Store.all(this);
        for (Endpoint e : endpoints) {
            workers.execute(() -> checkOne(e));
        }
    }

    private void checkOne(Endpoint endpoint) {
        boolean online = false;
        long started = System.currentTimeMillis();
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(endpoint.host, endpoint.port), endpoint.timeout);
            online = true;
        } catch (Exception ignored) {}
        String newState = online ? "ONLINE" : "OFFLINE";
        long now = System.currentTimeMillis();

        synchronized (MonitorService.class) {
            List<Endpoint> all = Store.all(this);
            for (Endpoint e : all) {
                if (e.id.equals(endpoint.id)) {
                    String old = e.state;
                    e.state = newState;
                    e.checkedAt = now;
                    if (!"UNKNOWN".equals(old) && !old.equals(newState)) {
                        sendAlert(e, newState, System.currentTimeMillis() - started);
                    }
                    Store.save(this, all);
                    break;
                }
            }
        }
        Intent update = new Intent("com.portmonitor.app.STATUS_UPDATED");
        update.setPackage(getPackageName());
        sendBroadcast(update);
    }

    private void sendAlert(Endpoint e, String state, long latency) {
        String title = ("OFFLINE".equals(state) ? "Порт недоступен: " : "Порт восстановлен: ") + e.name;
        String body = e.host + ":" + e.port + ("OFFLINE".equals(state) ? " · нет TCP-соединения" : " · снова доступен");
        Notification n = new Notification.Builder(this, CHANNEL_ALERTS)
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .setContentTitle(title).setContentText(body)
                .setStyle(new Notification.BigTextStyle().bigText(body))
                .setAutoCancel(true).build();
        ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify((e.id + state).hashCode(), n);
    }

    private Notification serviceNotification(String text) {
        return new Notification.Builder(this, CHANNEL_SERVICE)
                .setSmallIcon(android.R.drawable.stat_sys_upload)
                .setContentTitle("Port Monitor").setContentText(text)
                .setOngoing(true).build();
    }

    private void createChannels() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel(CHANNEL_SERVICE, "Фоновый мониторинг", NotificationManager.IMPORTANCE_LOW));
        nm.createNotificationChannel(new NotificationChannel(CHANNEL_ALERTS, "События портов", NotificationManager.IMPORTANCE_HIGH));
    }

    @Override public void onDestroy() {
        Store.setRunning(this, false);
        if (scheduler != null) scheduler.shutdownNow();
        workers.shutdownNow();
        super.onDestroy();
    }
    @Override public android.os.IBinder onBind(Intent intent) { return null; }
}
