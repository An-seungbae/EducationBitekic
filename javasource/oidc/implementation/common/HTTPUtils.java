package oidc.implementation.common;

import com.mendix.core.Core;
import org.apache.velocity.app.VelocityEngine;

import java.io.File;
import java.util.Properties;

public class HTTPUtils {

    public static VelocityEngine getEngine() {
        VelocityEngine engine = new VelocityEngine();
        Properties p = new Properties();
        final String templateDir = Core.getConfiguration().getResourcesPath().getAbsolutePath() + File.separator + "OIDC" + File.separator;
        p.setProperty("file.resource.loader.path", templateDir);
        p.setProperty("runtime.log", Core.getConfiguration().getTempPath().getAbsolutePath() + File.separator + "velocity.log");
        engine.init(p);

        return engine;
    }
}