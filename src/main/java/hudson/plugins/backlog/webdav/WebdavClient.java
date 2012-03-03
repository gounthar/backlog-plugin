package hudson.plugins.backlog.webdav;

import hudson.FilePath;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.protocol.HTTP;

import com.googlecode.sardine.SardineFactory;
import com.googlecode.sardine.impl.SardineImpl;

public class WebdavClient {

	private final SardineImpl sardine;

	private final String url;

	private String removePrefix = "";

	public WebdavClient(String url, String username, String password) {
		this.url = url;

		sardine = (SardineImpl) SardineFactory.begin(username, password);

		// confirm parameters
		try {
			sardine.list(url);
		} catch (IOException e) {
			throw new IllegalArgumentException("Failed login to Backlog", e);
		}
	}

	public void put(FilePath filePath, String remotePath) throws IOException {
		HttpEntity entity = new FilePathEntity(filePath,
				HTTP.DEFAULT_CONTENT_TYPE);

		sardine.put(url + remotePath + filePath.getName(), entity,
				HTTP.DEFAULT_CONTENT_TYPE, true);
	}

	// if i can
	// TODO add option : multiple includes files
	// TODO add option : flatten
	// TODO implement : mkdir root directory
	// (if can't, remote directory is required)

	// TODO add option : delete dir before put
	// TODO add option : remote directory is time format

	public void putWithParent(FilePath filePath, String remotePath,
			FilePath basePath) throws IOException, InterruptedException {
		createDirectory(remotePath);
		createDirectoriesFromBase(filePath.getParent(), remotePath, basePath);

		put(filePath,
				remotePath + getPathFromBase(filePath.getParent(), basePath));
	}

	// -------------------------------------- helper method (package private)

	void createDirectory(String remotePath) throws IOException {
		String createUrl = url + remotePath;

		if (!sardine.exists(createUrl)) {
			sardine.createDirectory(createUrl);
		}
	}

	void delete(String remotePath) throws IOException {
		String deleteUrl = url + remotePath;

		if (sardine.exists(deleteUrl)) {
			sardine.delete(deleteUrl);
		}
	}

	void createDirectoriesFromBase(FilePath filePath, String remotePath,
			FilePath basePath) throws IOException, InterruptedException {
		String remote = remotePath;
		String pathFromBaseDir = getPathFromBase(filePath, basePath);

		for (String path : pathFromBaseDir.split("/")) {
			remote = remote + path + "/";
			createDirectory(remote);
		}
	}

	String getPathFromBase(FilePath filePath, FilePath basePath)
			throws IOException, InterruptedException {
		String pathString = filePath.toURI().normalize().getPath();
		String baseString = basePath.toURI().normalize().getPath();
		String pathFromBase = pathString.substring(baseString.length());

		String prefix;
		if (!removePrefix.isEmpty() && removePrefix.charAt(0) == '/') {
			prefix = removePrefix.substring(1);
		} else {
			prefix = removePrefix;
		}

		if (!pathFromBase.startsWith(prefix)) {
			// TODO i18n
			throw new IllegalArgumentException(
					"If you use remove prefix, then ALL source file paths MUST start with the prefix.");
		}
		return pathFromBase.substring(prefix.length());
	}

	// -------------------------------------- getter/setter

	public String getRemovePrefix() {
		return removePrefix;
	}

	public void setRemovePrefix(String removePrefix) {
		this.removePrefix = removePrefix;
	}

}