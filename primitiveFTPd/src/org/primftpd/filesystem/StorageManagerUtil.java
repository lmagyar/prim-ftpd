package org.primftpd.filesystem;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.storage.StorageManager;
import android.provider.DocumentsContract;

import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.lang.reflect.Array;
import java.lang.reflect.Method;

public final class StorageManagerUtil {
    private static final String PRIMARY_VOLUME_NAME = "primary";

    private static Logger logger = LoggerFactory.getLogger(StorageManagerUtil.class);

    public static String getFullDocIdPathFromTreeUri(@Nullable final Uri treeUri, Context context) {
        if (treeUri == null) {
            return null;
        }
        String volumePath = getVolumePath(getVolumeIdFromTreeUri(treeUri), context);
        if (volumePath == null) {
            return File.separator;
        }
        if (volumePath.endsWith(File.separator)) {
            volumePath = volumePath.substring(0, volumePath.length() - 1);
        }

        String documentPath = getDocumentPathFromTreeUri(treeUri);
        if (documentPath.endsWith(File.separator)) {
            documentPath = documentPath.substring(0, documentPath.length() - 1);
        }

        if (documentPath.length() > 0) {
            if (documentPath.startsWith(File.separator)) {
                return volumePath + documentPath;
            } else {
                return volumePath + File.separator + documentPath;
            }
        } else {
            return volumePath;
        }
    }

    public static int getFilesystemTimeResolutionForTreeUri(Uri startUrl) {
        logger.trace("getFilesystemTimeResolutionForTreeUri({})", startUrl);
        String mountPoint = "/mnt/media_rw/" + getVolumeIdFromTreeUri(startUrl);
        try(BufferedReader br = new BufferedReader(new FileReader("/proc/mounts"))) {
            // sample contents for /proc/mounts
            // /dev/block/vold/public:xxx,xx /mnt/media_rw/XXXX-XXXX vfat ... 0 0                     -> 2000 ms
            // /dev/block/vold/public:xxx,xx /mnt/media_rw/XXXX-XXXX sdfat ...,fs=vfat:16,... 0 0     -> 2000 ms
            // /dev/block/vold/public:xxx,xx /mnt/media_rw/XXXX-XXXX sdfat ...,fs=vfat:32,... 0 0     -> 2000 ms
            // /dev/block/vold/public:xxx,xx /mnt/media_rw/XXXX-XXXX sdfat ...,fs=exfat,... 0 0       ->   10 ms
            for (String line; (line = br.readLine()) != null; ) {
                String[] mountInformations = line.split(" ");
                if (mountInformations.length >= 4 && mountInformations[1].equals(mountPoint)) {
                    if (mountInformations[2].equals("vfat")) {
                        logger.trace("  found mount point {} with type {} -> 2000ms", mountInformations[1], mountInformations[2]);
                        return 2000;
                    } else if (mountInformations[2].equals("sdfat")) {
                        for (String option : mountInformations[3].split(",")) {
                            if (option.startsWith("fs=")) {
                                if (option.startsWith("fs=vfat")) {
                                    logger.trace("  found mount point {} with type {} with option {} -> 2000ms", new Object[]{mountInformations[1], mountInformations[2], option});
                                    return 2000;
                                } else if (option.startsWith("fs=exfat")) {
                                    logger.trace("  found mount point {} with type {} with option {} -> 10ms", new Object[]{mountInformations[1], mountInformations[2], option});
                                    return 10;
                                }
                                break;
                            }
                        }
                    }
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("getFilesystemTimeResolutionForTreeUri() {}", e);
        }
        return 1; // we don't know, assume 1ms
    }

    @SuppressLint("ObsoleteSdkInt")
    private static String getVolumePath(final String volumeId, Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return null;
        }
        try {
            StorageManager mStorageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
            Class<?> storageVolumeClass = Class.forName("android.os.storage.StorageVolume");
            Method getVolumeList = mStorageManager.getClass().getMethod("getVolumeList");
            Method getUuid = storageVolumeClass.getMethod("getUuid");
            Method getPath = storageVolumeClass.getMethod("getPath");
            Method isPrimary = storageVolumeClass.getMethod("isPrimary");
            Object result = getVolumeList.invoke(mStorageManager);

            final int length = Array.getLength(result);
            for (int i = 0; i < length; i++) {
                Object storageVolumeElement = Array.get(result, i);
                String uuid = (String) getUuid.invoke(storageVolumeElement);
                Boolean primary = (Boolean) isPrimary.invoke(storageVolumeElement);

                // primary volume?
                if (primary && PRIMARY_VOLUME_NAME.equals(volumeId)) {
                    return (String) getPath.invoke(storageVolumeElement);
                }

                // other volumes?
                if (uuid != null && uuid.equals(volumeId)) {
                    return (String) getPath.invoke(storageVolumeElement);
                }
            }
            // not found.
            return null;
        } catch (Exception ex) {
            return null;
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static String getVolumeIdFromTreeUri(final Uri treeUri) {
        final String docId = DocumentsContract.getTreeDocumentId(treeUri);
        final String[] split = docId.split(":");
        if (split.length > 0) {
            return split[0];
        } else {
            return null;
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP) 
    private static String getDocumentPathFromTreeUri(final Uri treeUri) {
        final String docId = DocumentsContract.getDocumentId(treeUri);
        final String[] split = docId.split(":");
        if ((split.length >= 2) && (split[1] != null)) {
            return split[1];
        } else {
            return File.separator;
        }
    }
}
