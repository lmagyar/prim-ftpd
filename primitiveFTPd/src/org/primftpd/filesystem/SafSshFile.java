package org.primftpd.filesystem;

import android.content.ContentResolver;
import androidx.documentfile.provider.DocumentFile;

import org.apache.sshd.common.Session;
import org.apache.sshd.common.file.SshFile;
import org.primftpd.services.PftpdService;

import java.io.IOException;
import java.util.List;

public class SafSshFile extends SafFile<SshFile> implements SshFile {

    private final Session session;

    public SafSshFile(
            ContentResolver contentResolver,
            DocumentFile parentDocumentFile,
            DocumentFile documentFile,
            String absPath,
            PftpdService pftpdService,
            SafFileSystemView fileSystemView,
            Session session) {
        super(contentResolver, parentDocumentFile, documentFile, absPath, pftpdService, fileSystemView);
        this.session = session;
    }

    public SafSshFile(
            ContentResolver contentResolver,
            DocumentFile parentDocumentFile,
            String name,
            String absPath,
            PftpdService pftpdService,
            SafFileSystemView fileSystemView,
            Session session) {
        super(contentResolver, parentDocumentFile, name, absPath, pftpdService, fileSystemView);
        this.session = session;
    }

    @Override
    protected SshFile createFile(
            ContentResolver contentResolver,
            DocumentFile parentDocumentFile,
            DocumentFile documentFile,
            String absPath,
            PftpdService pftpdService,
            SafFileSystemView fileSystemView) {
        return new SafSshFile(contentResolver, parentDocumentFile, documentFile, absPath, pftpdService, fileSystemView, session);
    }

    @Override
    public String getClientIp() {
        return SshUtils.getClientIp(session);
    }

    @Override
    public boolean move(SshFile target) {
        logger.trace("move()");
        return super.move((SafFile)target);
    }

    @Override
    public String getOwner() {
        logger.trace("[{}] getOwner()", name);
        return session.getUsername();
    }

    @Override
    public boolean create() throws IOException {
        logger.trace("[{}] create()", name);
        return createNewFile();
    }

    @Override
    public SshFile getParentFile() {
        logger.trace("[{}] getParentFile()", name);
        String parentPath = Utils.parent(absPath);
        if (parentPath.length() == 0) {
            // in SAF we don't keep track of home dir
            parentPath = "/";
        }
        logger.trace("[{}]   getParentFile() -> {}", name, parentPath);
        return (SshFile)fileSystemView.getFile(parentPath);
    }

    @Override
    public List<SshFile> listSshFiles() {
        return listFiles();
    }
}
