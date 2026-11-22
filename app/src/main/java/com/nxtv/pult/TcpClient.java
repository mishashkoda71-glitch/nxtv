package com.nxtv.pult;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class TcpClient {
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    public void connect(String host, int port) throws IOException {
        socket = new Socket(host, port);
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
    }

    public void disconnect() {
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    public byte[] send(byte[] plain) throws Exception {
        out.write(Crypto.encrypt(plain));
        out.flush();
        byte[] buf = new byte[65536];
        int n = in.read(buf);
        if (n <= 0) return new byte[0];
        byte[] raw = new byte[n];
        System.arraycopy(buf, 0, raw, 0, n);
        return Crypto.decrypt(raw);
    }

    // === ВОТ ОНИ, НАШИ ПРОПАВШИЕ МЕТОДЫ ДЛЯ СТРИМА ===
    public int readInt() throws IOException {
        return in.readInt();
    }

    public byte[] readBytes(int length) throws IOException {
        byte[] buf = new byte[length];
        in.readFully(buf);
        return buf;
    }
}
