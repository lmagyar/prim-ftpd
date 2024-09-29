package org.primftpd.filesystem;

import android.content.Context;
import android.net.Uri;

import java.io.File;

import org.apache.sshd.common.file.SshFile;
import org.apache.sshd.common.Session;
import org.apache.sshd.common.file.FileSystemView;
import org.primftpd.services.PftpdService;

public class FsSshFileSystemView extends FsFileSystemView<FsSshFile, SshFile> implements FileSystemView {

	private final File homeDir;
	private final Session session;

	public FsSshFileSystemView(PftpdService pftpdService, Uri safStartUrl, File homeDir, Session session) {
		super(pftpdService, safStartUrl);
		this.homeDir = homeDir;
		this.session = session;
	}

	@Override
	protected FsSshFile createFile(File file) {
		return new FsSshFile(this, file, session);
	}

	@Override
	protected String absolute(String file) {
		return Utils.absoluteOrHome(file, homeDir.getAbsolutePath());
	}

	@Override
	public SshFile getFile(SshFile baseDir, String file) {
		logger.trace("getFile(baseDir: {}, file: {})", baseDir.getAbsolutePath(), file);
		// e.g. for scp
		return getFile(baseDir.getAbsolutePath() + "/" + file);
	}

	@Override
	public FileSystemView getNormalizedView() {
		logger.trace("getNormalizedView()");
		return this;
	}
}
