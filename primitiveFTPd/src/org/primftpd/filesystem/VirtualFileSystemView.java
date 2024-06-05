package org.primftpd.filesystem;

import java.io.File;
import java.nio.file.Paths;

import org.primftpd.services.PftpdService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class VirtualFileSystemView<
        MinaType,
        FsType extends FsFile<MinaType>,
        RootType extends RootFile<MinaType>,
        SafType extends SafFile<MinaType>,
        RoSafType extends RoSafFile<MinaType>
        > {

    public static final String PREFIX_FS = "fs";
    public static final String PREFIX_ROOT = "superuser";
    public static final String PREFIX_SAF = "saf";
    public static final String PREFIX_ROSAF = "rosaf";

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final FsFileSystemView<FsType, MinaType> fsFileSystemView;
    protected final RootFileSystemView<RootType, MinaType> rootFileSystemView;
    protected final SafFileSystemView<SafType, MinaType> safFileSystemView;
    protected final RoSafFileSystemView<RoSafType, MinaType> roSafFileSystemView;
    protected final PftpdService pftpdService;

    public VirtualFileSystemView(
            FsFileSystemView<FsType, MinaType> fsFileSystemView,
            RootFileSystemView<RootType, MinaType> rootFileSystemView,
            SafFileSystemView<SafType, MinaType> safFileSystemView,
            RoSafFileSystemView<RoSafType, MinaType> roSafFileSystemView,
            PftpdService pftpdService) {
        this.fsFileSystemView = fsFileSystemView;
        this.rootFileSystemView = rootFileSystemView;
        this.safFileSystemView = safFileSystemView;
        this.roSafFileSystemView = roSafFileSystemView;
        this.pftpdService = pftpdService;
    }

    public abstract MinaType createFile(String absPath, AbstractFile delegate, PftpdService pftpdService);
    public abstract MinaType createFile(String absPath, AbstractFile delegate, boolean exists, PftpdService pftpdService);

    protected abstract String absolute(String file);

    public MinaType getFile(String file) {
        String absoluteVirtualPath = absolute(file);
        logger.debug("getFile '{}', absolute: '{}'", file, absoluteVirtualPath);
        if ("/".equals(absoluteVirtualPath) || "~".equals(absoluteVirtualPath)) {
            return createFile(absoluteVirtualPath, null, true, pftpdService);
        } else if (isStorageType(absoluteVirtualPath, PREFIX_FS)) {
            String realPath = toRealPath(absoluteVirtualPath, PREFIX_FS);
            AbstractFile delegate = fsFileSystemView.getFile(realPath);
            absoluteVirtualPath = "/" + PREFIX_FS + escapeRoot(delegate.getAbsolutePath());
            logger.debug("Using FS '{}' for '{}'", realPath, absoluteVirtualPath);
            return createFile(absoluteVirtualPath, delegate, pftpdService);
        } else if (isStorageType(absoluteVirtualPath, PREFIX_ROOT)) {
            String realPath = toRealPath(absoluteVirtualPath, PREFIX_ROOT);
            AbstractFile delegate = rootFileSystemView.getFile(realPath);
            absoluteVirtualPath = "/" + PREFIX_ROOT + escapeRoot(delegate.getAbsolutePath());
            logger.debug("Using ROOT '{}' for '{}'", realPath, absoluteVirtualPath);
            return createFile(absoluteVirtualPath, delegate, pftpdService);
        } else if (isStorageType(absoluteVirtualPath, PREFIX_SAF)) {
            String realPath = toRealPath(absoluteVirtualPath, PREFIX_SAF);
            AbstractFile delegate = safFileSystemView.getFile(realPath);
            absoluteVirtualPath = "/" + PREFIX_SAF + escapeRoot(delegate.getAbsolutePath());
            logger.debug("Using SAF '{}' for '{}'", realPath, absoluteVirtualPath);
            return createFile(absoluteVirtualPath, delegate, pftpdService);
        } else if (isStorageType(absoluteVirtualPath, PREFIX_ROSAF)) {
            String realPath = toRealPath(absoluteVirtualPath, PREFIX_ROSAF);
            AbstractFile delegate = roSafFileSystemView.getFile(realPath);
            absoluteVirtualPath = "/" + PREFIX_ROSAF + escapeRoot(delegate.getAbsolutePath());
            logger.debug("Using ROSAF '{}' for '{}'", realPath, absoluteVirtualPath);
            return createFile(absoluteVirtualPath, delegate, pftpdService);
        } else {
            logger.debug("Using VirtualFile for unknown path '{}'", absoluteVirtualPath);
            return createFile(absoluteVirtualPath, null, false, pftpdService);
        }
    }

    private boolean isStorageType(String path, String prefix) {
        return path.equals(prefix) || path.equals("/" + prefix) || path.startsWith(prefix + "/") || path.startsWith("/" + prefix + "/");
    }

    private String toRealPath(String path, String prefix) {
        if (path.charAt(0) == '/') {
            if (path.length() > prefix.length() + 2) {
                return path.substring(prefix.length() + 1); // '/fs/xxx' -> '/xxx'
            } else {
                return "/";                                 // '/fs'     -> '/'
            }
        } else {
            if (path.length() > prefix.length() + 1) {
                return path.substring(prefix.length() + 1); // 'fs/xxx'  -> 'xxx'
            } else {
                return ".";                                 // 'fs'      -> '.'
            }
        }
    }

    private String escapeRoot(String path) {
        if ("/".equals(path)) {
            return "";
        }
        return path;
    }
}
