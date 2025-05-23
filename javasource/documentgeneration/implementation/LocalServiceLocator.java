package documentgeneration.implementation;

import java.io.File;
import java.util.ArrayList;

import org.apache.commons.lang3.SystemUtils;

import com.mendix.core.Core;
import com.mendix.logging.ILogNode;

import documentgeneration.proxies.constants.Constants;

public class LocalServiceLocator {
	private static String nodePath = null;
	private static String servicePath = null;
	private static String chromePath = null;

	public static void verify() {
		getNodePath();
		getServicePath();
		getChromePath();
	}

	public static String getNodePath() {
		if (nodePath != null)
			return nodePath;

		ArrayList<String> potentialNodePaths = new ArrayList<String>();

		if (Constants.getCustomNodePath() != null && Constants.getCustomNodePath().length() > 0)
			potentialNodePaths.add(Constants.getCustomNodePath());

		String basePath = String.join(File.separator,
				new String[] { Core.getConfiguration().getRuntimePath().getParent(), "modeler", "tools", "node" });

		if (SystemUtils.IS_OS_WINDOWS) {
			potentialNodePaths.add(String.join(File.separator, new String[] { basePath, "node.exe" }));
			potentialNodePaths.add(String.join(File.separator, new String[] { basePath, "win-x64", "node.exe" }));
		}

		if (SystemUtils.IS_OS_MAC_OSX) {
			if (ARM64_ARCHITECTURE.equals(SystemUtils.OS_ARCH)) {
				potentialNodePaths.add(String.join(File.separator, new String[] { basePath, "osx-arm64", "node" }));
			} else {
				potentialNodePaths.add(String.join(File.separator, new String[] { basePath, "osx-x64", "node" }));
			}
		}

		for (String potentialNodePath : potentialNodePaths) {
			File expectedNodePath = new File(potentialNodePath);
			logging.trace("Scanning for local Node executable: " + expectedNodePath);

			if (expectedNodePath.exists()) {
				nodePath = expectedNodePath.getAbsolutePath();
				logging.debug("Using local Node executable: " + nodePath);

				return nodePath;
			}
		}

		logging.error("Could not find local Node executable");
		return null;
	}

	public static String getServicePath() {
		if (servicePath != null)
			return servicePath;

		String resourcesPath = Core.getConfiguration().getResourcesPath().getPath();
		File expectedServicePath = new File(
				String.join(File.separator, new String[] { resourcesPath, moduleDirectory, "service.js" }));
		logging.trace("Expected path for local DocGen service: " + expectedServicePath);

		if (!expectedServicePath.exists()) {
			logging.error("Could not find local DocGen service");
			return null;
		}

		servicePath = expectedServicePath.getAbsolutePath();
		logging.debug("Using local service: " + servicePath);

		return servicePath;
	}

	public static String getChromePath() {
		if (chromePath != null)
			return chromePath;

		ArrayList<String> potentialChromePaths = new ArrayList<String>();

		if (Constants.getCustomChromePath() != null && Constants.getCustomChromePath().length() > 0)
			potentialChromePaths.add(Constants.getCustomChromePath());

		if (SystemUtils.IS_OS_WINDOWS) {
			String chromePath = String.join(File.separator,
					new String[] { "Google", "Chrome", "Application", "chrome.exe" });

			if (System.getenv("ProgramFiles") != null)
				potentialChromePaths
						.add(String.join(File.separator, new String[] { System.getenv("ProgramFiles"), chromePath }));

			if (System.getenv("ProgramFiles(x86)") != null)
				potentialChromePaths.add(
						String.join(File.separator, new String[] { System.getenv("ProgramFiles(x86)"), chromePath }));

			if (System.getenv("LocalAppData") != null)
				potentialChromePaths
						.add(String.join(File.separator, new String[] { System.getenv("LocalAppData"), chromePath }));
		}

		if (SystemUtils.IS_OS_MAC_OSX) {
			potentialChromePaths.add("/" + String.join(File.separator,
					new String[] { "Applications", "Google Chrome.app", "Contents", "MacOS", "Google Chrome" }));
		}

		for (String potentialChromePath : potentialChromePaths) {
			File expectedChromePath = new File(potentialChromePath);
			logging.trace("Scanning for local Chrome executable: " + expectedChromePath);

			if (expectedChromePath.exists()) {
				chromePath = expectedChromePath.getAbsolutePath();
				logging.debug("Using local Chrome executable: " + chromePath);

				return chromePath;
			}
		}

		logging.error("Could not find local Chrome executable");
		return null;
	}

	private static final String ARM64_ARCHITECTURE = "aarch64";

	private static final ILogNode logging = Logging.logNode;
	private static final String moduleDirectory = "documentgeneration";
}
