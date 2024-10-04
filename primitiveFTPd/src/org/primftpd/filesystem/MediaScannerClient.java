package org.primftpd.filesystem;

import android.content.Context;
import android.net.Uri;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MediaScannerClient implements MediaScannerConnectionClient {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final MediaScannerConnection connection;

    public MediaScannerClient(Context context) {
        this.connection = new MediaScannerConnection(context, this);
    }

    public void ensureConnected() {
        synchronized (connection) {
            while (!connection.isConnected()) {
                try {
                    connection.connect();
                    connection.wait();
                } catch (Exception e) {
                    logger.error("  media scanning connection error '{}'", e.toString());
                }
            }
        }
    }

    @Override
    public void onMediaScannerConnected() {
        synchronized (connection) {
            connection.notify();
        }
    }

    public void scanFile(String path) {
        logger.info("media scanning started for file '{}'", path);
        try {
            ensureConnected();
            connection.scanFile(path, null);
        } catch (Exception e) {
            logger.error("  media scanning error '{}' for file '{}'", e.toString(), path);
        }
    }

    @Override
    public void onScanCompleted(String path, Uri uri) {
        logger.info("media scanning completed for file '{}'", path);
    }
};
