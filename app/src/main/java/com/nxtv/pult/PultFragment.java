package com.nxtv.pult;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import java.nio.charset.StandardCharsets;

public class PultFragment extends Fragment {
    private final TcpClient client = new TcpClient();
    private final Handler ui = new Handler(Looper.getMainLooper());
    private EditText etHost, etPort, etCommand;
    private TextView tvOutput;
    private ScrollView scrollView;
    private ImageView ivScreenshot;
    private Button btnConnect, btnStream;

    private final Matrix matrix = new Matrix();
    private float scale = 1f;
    private ScaleGestureDetector scaleDetector;
    private GestureDetector gestureDetector;
    private volatile boolean isStreaming = false;
    private Thread streamThread;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pult, container, false);

        etHost = view.findViewById(R.id.etHost);
        etPort = view.findViewById(R.id.etPort);
        etCommand = view.findViewById(R.id.etCommand);
        tvOutput = view.findViewById(R.id.tvOutput);
        scrollView = view.findViewById(R.id.scrollView);
        ivScreenshot = view.findViewById(R.id.ivScreenshot);
        btnConnect = view.findViewById(R.id.btnConnect);
        btnStream = view.findViewById(R.id.btnStream);

        scaleDetector = new ScaleGestureDetector(getContext(), new ScaleGestureDetector.SimpleOnScaleGestureListener() {
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

        gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
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

        btnConnect.setOnClickListener(v -> {
            if (client.isConnected()) {
                stopVideoStream();
                client.disconnect();
                btnConnect.setText("ПОДКЛЮЧИТЬСЯ");
                append("[!] Отключено");
                return;
            }
            String host = etHost.getText().toString().trim();
            int port = Integer.parseInt(etPort.getText().toString().trim());
            new Thread(() -> {
                try {
                    client.connect(host, port);
                    ui.post(() -> {
                        append("[+] Подключено к " + host + ":" + port);
                        btnConnect.setText("ОТКЛЮЧИТЬСЯ");
                    });
                } catch (Exception e) {
                    ui.post(() -> append("[!] Ошибка: " + e.getMessage()));
                }
            }).start();
        });

        view.findViewById(R.id.btnScreen).setOnClickListener(v -> { showTextMode(); sendCommand("/screen"); });
        view.findViewById(R.id.btnProc).setOnClickListener(v -> { showTextMode(); sendCommand("/proc"); });
        view.findViewById(R.id.btnLock).setOnClickListener(v -> { showTextMode(); sendCommand("/lock"); });
        view.findViewById(R.id.btnUnlock).setOnClickListener(v -> { showTextMode(); sendCommand("/unlock"); });

        btnStream.setOnClickListener(v -> {
            if (isStreaming) {
                stopVideoStream();
                append("[!] Стрим остановлен");
            } else {
                sendCommand("/stream_start");
                startVideoStream();
                append("[+] Стрим запущен");
            }
        });

        view.findViewById(R.id.btnSend).setOnClickListener(v -> {
            String cmd = etCommand.getText().toString().trim();
            if (!cmd.isEmpty()) sendCommand(cmd);
        });

        return view;
    }

    private void sendCommand(String cmd) {
        new Thread(() -> {
            try {
                if (!client.isConnected()) {
                    ui.post(() -> append("[!] Нет подключения"));
                    return;
                }
                if (cmd.equals("/stream_start")) {
                    client.send(cmd.getBytes(StandardCharsets.UTF_8));
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
    }

    private void startVideoStream() {
        isStreaming = true;
        streamThread = new Thread(() -> {
            try {
                while (isStreaming && client.isConnected()) {
                    int len = client.readInt();
                    if (len <= 0) break;
                    byte[] frame = client.readBytes(len);
                    Bitmap bmp = BitmapFactory.decodeByteArray(frame, 0, len);
                    ui.post(() -> {
                        if (bmp != null) {
                            ivScreenshot.setImageBitmap(bmp);
                            ivScreenshot.setVisibility(View.VISIBLE);
                            scrollView.setVisibility(View.GONE);
                        }
                    });
                }
            } catch (Exception e) {
                ui.post(() -> append("[!] Стрим завершен: " + e.getMessage()));
            }
        });
        streamThread.start();
    }

    private void stopVideoStream() {
        isStreaming = false;
        if (streamThread != null) streamThread.interrupt();
        ivScreenshot.setVisibility(View.GONE);
        scrollView.setVisibility(View.VISIBLE);
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
        append("[IMG] " + bmp.getWidth() + "x" + bmp.getHeight() + " — пинч для зума");
    }

    private void showTextMode() {
        ivScreenshot.setVisibility(View.GONE);
        scrollView.setVisibility(View.VISIBLE);
    }

    private void append(String s) {
        tvOutput.append(s + "\n\n");
        tvOutput.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
    }
}
