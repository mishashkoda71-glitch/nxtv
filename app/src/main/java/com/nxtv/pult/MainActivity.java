package com.nxtv.pult;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity {
    private final TcpClient client = new TcpClient();
    private final Handler ui = new Handler(Looper.getMainLooper());
    private EditText etHost, etPort, etCommand;
    private TextView tvOutput;
    private ScrollView scrollView;
    private ImageView ivScreenshot;

    private final Matrix matrix = new Matrix();
    private float scale = 1f;
    private ScaleGestureDetector scaleDetector;
    private GestureDetector gestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etHost = findViewById(R.id.etHost);
        etPort = findViewById(R.id.etPort);
        etCommand = findViewById(R.id.etCommand);
        tvOutput = findViewById(R.id.tvOutput);
        scrollView = findViewById(R.id.scrollView);
        ivScreenshot = findViewById(R.id.ivScreenshot);

        scaleDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float factor = detector.getScaleFactor();
                scale *= factor;
                scale = Math.max(0.1f, Math.min(scale, 10f));
                matrix.postScale(factor, factor, detector.getFocusX(), detector.getFocusY());
                ivScreenshot.setImageMatrix(matrix);
                return true;
            }
        });

        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float dx, float dy) {
                matrix.postTranslate(-dx, -dy);
                ivScreenshot.setImageMatrix(matrix);
                return true;
            }
        });

        ivScreenshot.setOnTouchListener((v, event) -> {
            scaleDetector.onTouchEvent(event);
            gestureDetector.onTouchEvent(event);
            return true;
        });

        findViewById(R.id.btnConnect).setOnClickListener(v -> {
            String host = etHost.getText().toString().trim();
            int port = Integer.parseInt(etPort.getText().toString().trim());
            new Thread(() -> {
                try {
                    client.connect(host, port);
                    ui.post(() -> append("[+] connected to " + host + ":" + port));
                } catch (Exception e) {
                    ui.post(() -> append("[!] " + e.getMessage()));
                }
            }).start();
        });

        findViewById(R.id.btnSend).setOnClickListener(v -> {
            String cmd = etCommand.getText().toString().trim();
            if (cmd.isEmpty()) return;
            new Thread(() -> {
                try {
                    if (!client.isConnected()) {
                        ui.post(() -> append("[!] not connected"));
                        return;
                    }
                    byte[] resp = client.send(cmd.getBytes(StandardCharsets.UTF_8));
                    if (resp.length > 4 && resp[0] == '[' && resp[1] == 'I' && resp[2] == 'M' && resp[3] == 'G') {
                        Bitmap bmp = BitmapFactory.decodeByteArray(resp, 4, resp.length - 4);
                        ui.post(() -> showScreenshot(bmp));
                    } else {
                        String text = new String(resp, StandardCharsets.UTF_8);
                        ui.post(() -> append(text));
                    }
                } catch (Exception e) {
                    ui.post(() -> append("[!] " + e.getMessage()));
                }
            }).start();
        });
    }

    private void showScreenshot(Bitmap bmp) {
        matrix.reset();
        scale = 1f;
        float viewW = ivScreenshot.getWidth();
        float bmpW = bmp.getWidth();
        float fitScale = viewW / bmpW;
        matrix.setScale(fitScale, fitScale);
        scale = fitScale;
        ivScreenshot.setImageMatrix(matrix);
        ivScreenshot.setImageBitmap(bmp);
        ivScreenshot.setVisibility(View.VISIBLE);
        scrollView.setVisibility(View.GONE);
        append("[IMG] " + bmp.getWidth() + "x" + bmp.getHeight() + " — пинч для зума, свайп для панорамы");
    }

    private void append(String s) {
        tvOutput.append(s + "\n\n");
        tvOutput.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
    }
}
