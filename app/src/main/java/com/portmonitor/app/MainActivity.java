package com.portmonitor.app;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.*;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
    private LinearLayout list;
    private TextView serviceStatus;
    private BroadcastReceiver receiver;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 24, 24, 24);
        root.setBackgroundColor(Color.rgb(247,248,250));

        TextView title = new TextView(this);
        title.setText("Port Monitor");
        title.setTextSize(26); title.setTextColor(Color.BLACK); title.setTypeface(null, 1);
        root.addView(title);

        serviceStatus = new TextView(this);
        serviceStatus.setTextColor(Color.DKGRAY);
        serviceStatus.setPadding(0, 8, 0, 16);
        root.addView(serviceStatus);

        LinearLayout actions = new LinearLayout(this);
        Button add = new Button(this); add.setText("+ Добавить");
        add.setOnClickListener(v -> showAddDialog());
        Button check = new Button(this); check.setText("Проверить сейчас");
        check.setOnClickListener(v -> {
            Intent i = new Intent(this, MonitorService.class); i.setAction(MonitorService.ACTION_CHECK_NOW);
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i);
        });
        actions.addView(add, new LinearLayout.LayoutParams(0, -2, 1));
        actions.addView(check, new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(actions);

        LinearLayout controls = new LinearLayout(this);
        Button start = new Button(this); start.setText("Запустить");
        start.setOnClickListener(v -> startMonitor());
        Button stop = new Button(this); stop.setText("Остановить");
        stop.setOnClickListener(v -> stopService(new Intent(this, MonitorService.class)));
        controls.addView(start, new LinearLayout.LayoutParams(0, -2, 1));
        controls.addView(stop, new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(controls);

        ScrollView scroll = new ScrollView(this);
        list = new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
        render();

        receiver = new BroadcastReceiver() { @Override public void onReceive(Context c, Intent i) { render(); } };
        registerReceiver(receiver, new IntentFilter("com.portmonitor.app.STATUS_UPDATED"));
        if (Store.isRunning(this)) startMonitor();
    }

    private void startMonitor() {
        Intent i = new Intent(this, MonitorService.class);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i);
        Store.setRunning(this, true);
        render();
    }

    private void showAddDialog() {
        LinearLayout form = new LinearLayout(this); form.setOrientation(LinearLayout.VERTICAL); form.setPadding(20,10,20,0);
        EditText name = field("Название, например L2 Login", form, InputType.TYPE_CLASS_TEXT);
        EditText host = field("IP или домен", form, InputType.TYPE_CLASS_TEXT);
        EditText port = field("TCP-порт (например 2106)", form, InputType.TYPE_CLASS_NUMBER);
        EditText timeout = field("Таймаут, мс (1000–15000)", form, InputType.TYPE_CLASS_NUMBER);
        timeout.setText("3000");
        new AlertDialog.Builder(this).setTitle("Добавить порт").setView(form)
          .setNegativeButton("Отмена", null).setPositiveButton("Сохранить", (d,w) -> {
              try {
                  int p = Integer.parseInt(port.getText().toString());
                  int t = Integer.parseInt(timeout.getText().toString());
                  if (p < 1 || p > 65535 || t < 1000 || t > 15000 || host.getText().toString().trim().isEmpty()) throw new Exception();
                  List<Endpoint> all = Store.all(this);
                  all.add(new Endpoint(UUID.randomUUID().toString(), name.getText().toString().trim().isEmpty() ? host.getText().toString() : name.getText().toString().trim(),
                          host.getText().toString().trim(), p, t, "UNKNOWN", 0));
                  Store.save(this, all); render();
                  if (Store.isRunning(this)) {
                      Intent i = new Intent(this, MonitorService.class); i.setAction(MonitorService.ACTION_CHECK_NOW);
                      if (Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i);
                  }
              } catch (Exception ex) { Toast.makeText(this, "Проверь IP, порт и таймаут", Toast.LENGTH_LONG).show(); }
          }).show();
    }

    private EditText field(String hint, LinearLayout parent, int type) {
        EditText e = new EditText(this); e.setHint(hint); e.setInputType(type); parent.addView(e); return e;
    }

    private void render() {
        if (list == null) return;
        serviceStatus.setText(Store.isRunning(this) ? "● Мониторинг включён · интервал 30 секунд" : "○ Мониторинг остановлен");
        list.removeAllViews();
        List<Endpoint> all = Store.all(this);
        if (all.isEmpty()) {
            TextView empty = new TextView(this); empty.setText("Пока нет портов. Нажми «Добавить»."); empty.setPadding(8,30,8,8); list.addView(empty);
        }
        for (Endpoint e : all) {
            LinearLayout card = new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(18,14,18,14); card.setBackgroundColor(Color.WHITE);
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2); cp.setMargins(0,12,0,0); list.addView(card, cp);

            TextView label = new TextView(this);
            label.setText(e.name + "   " + e.host + ":" + e.port);
            label.setTextSize(16); label.setTextColor(Color.BLACK); label.setTypeface(null,1);
            card.addView(label);

            TextView status = new TextView(this);
            status.setText("● " + e.state + "   ·   timeout " + e.timeout + " мс");
            status.setTextColor("ONLINE".equals(e.state) ? Color.rgb(20,145,70) : "OFFLINE".equals(e.state) ? Color.RED : Color.GRAY);
            card.addView(status);

            TextView checked = new TextView(this);
            checked.setText(e.checkedAt == 0 ? "Ещё не проверялся" : "Проверка: " + new Date(e.checkedAt).toString());
            checked.setTextColor(Color.DKGRAY); card.addView(checked);

            Button del = new Button(this); del.setText("Удалить");
            del.setOnClickListener(v -> new AlertDialog.Builder(this).setMessage("Удалить " + e.name + "?")
                    .setNegativeButton("Нет",null).setPositiveButton("Да",(a,b)->{
                        List<Endpoint> items=Store.all(this); items.removeIf(x->x.id.equals(e.id)); Store.save(this,items); render();
                    }).show());
            card.addView(del);
        }
    }

    @Override protected void onDestroy() {
        if (receiver != null) unregisterReceiver(receiver);
        super.onDestroy();
    }
}
