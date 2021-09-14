package me.duhblea.jrebuildertask;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * A custom gradle task that deploys a complete JRE.
 */
public class JreBuilderTask extends DefaultTask {
    /**
     * The distribution folder path.
     */
    @Input
    public String distFolderPath = null;

    /**
     * The system property find the java home path
     */
    private static final String JAVA_HOME_SYSTEM_PROPERTY = "java.home";

    /**
     * Contains the available modules from the JDK
     */
    private final List<String> availableJDKModules = new ArrayList<>();

    /**
     * Main task action to build the JRE.
     */
    @TaskAction
    public void buildJre() {
        // Check the Java version so that it is at least 15.
        checkJavaVersion();

        if (distFolderPath == null || !new File(distFolderPath).exists()) {
            throw new GradleException("The distFolderPath is invalid");
        }

        var javaBinaryPath = getJavaBinaryLocation();
        var jlinkBinaryPath = getJLinkBinaryLocation();

        // Populate available modules.
        populateAvailableModules(javaBinaryPath);

        // Create the JRE from the available modules
        if (!availableJDKModules.isEmpty()) {
            createJre(jlinkBinaryPath);
        }
    }

    /**
     * Checks the Java version. If it is not greater than 15, error our.
     */
    private void checkJavaVersion() {
        if (Runtime.version().feature() < 15) {
            throw new GradleException("To build a JRE, the version must be at least 15.");
        }
    }

    /**
     * Get the Java binary path.
     *
     * @return The java binary path.
     */
    private String getJavaBinaryLocation() {
        String javaExecutableBinaryPath
                = System.getProperty(JAVA_HOME_SYSTEM_PROPERTY) + File.separator + "bin" + File.separator + "java.exe";
        File file = new File(javaExecutableBinaryPath);
        if (file.exists() && file.canRead()) {
            return javaExecutableBinaryPath;
        } else {
            throw new GradleException("The Java executable could not be found");
        }
    }

    /**
     * Get the jlink binary path.
     *
     * @return The jlink binary path.
     */
    private String getJLinkBinaryLocation() {
        String jlinkExecutableBinaryPath
                = System.getProperty(JAVA_HOME_SYSTEM_PROPERTY) + File.separator + "bin" + File.separator + "jlink.exe";
        var file = new File(jlinkExecutableBinaryPath);
        if (file.exists() && file.canRead()) {
            return jlinkExecutableBinaryPath;
        } else {
            throw new GradleException("The jlink executable could not be found");
        }
    }

    /**
     * Gets the list of available JDK modules based on discovered path.
     *
     * @param javaExecutableBinaryPath The executable path to Java.
     */
    private void populateAvailableModules(String javaExecutableBinaryPath) {
        List<String> command = new ArrayList<>();
        command.add("\"" + javaExecutableBinaryPath + "\"");
        command.add("--list-modules");
        try {
            Process process = Runtime.getRuntime().exec(command.toArray(new String[0]));
            ProcessInputReader streamGobbler =
                    new ProcessInputReader(process.getInputStream(), this::addModule);
            Executors.newSingleThreadExecutor().submit(streamGobbler);
            process.waitFor();

        } catch (IOException | InterruptedException e) {
            throw new GradleException("Could not populate available modules: " + e.getMessage());
        }
    }

    /**
     * Creates the JRE from the needed modules.
     *
     * @param jLinkPath The path to the jlink tool.
     */
    private void createJre(String jLinkPath) {
        String jrePath = distFolderPath + File.separator + "jre";
        File file = new File(jrePath);
        if (file.exists()) {
            removeDirectory(file);
        }

        List<String> command = new ArrayList<>();
        command.add("\"" + jLinkPath + "\"");
        command.add("--output");
        command.add("\"" + jrePath + "\"");
        command.add("--strip-debug");
        command.add("--no-header-files");
        command.add("--compress");
        command.add("2");
        command.add("--no-man-pages");
        command.add("--add-modules");
        command.add(String.join(",", availableJDKModules));

        try {
            Process process = Runtime.getRuntime().exec(command.toArray(new String[0]));
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            throw new GradleException("Could not create the JRE: " + e.getMessage());
        }
    }

    /**
     * Adds an available module from the JDK.
     *
     * @param module A module.
     */
    private void addModule(String module) {
        if (module.contains("@")) {
            int indexAt = module.indexOf("@");
            String value = module.substring(0, indexAt);
            availableJDKModules.add(value);
        }
    }

    /**
     * Deletes all content in a directory.
     *
     * @param dir The directory to delete its content from.
     */
    private void removeDirectory(File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null && files.length > 0) {
                for (File aFile : files) {
                    removeDirectory(aFile);
                }
            }
            dir.delete();
        } else {
            dir.delete();
        }
    }

    /**
     * Reads standard input from a running process
     */
    private static class ProcessInputReader implements Runnable {
        private final InputStream inputStream;
        private final Consumer<String> consumer;

        /**
         * Constructor.
         *
         * @param inputStream The input stream.
         * @param consumer    The consumer.
         */
        public ProcessInputReader(InputStream inputStream, Consumer<String> consumer) {
            this.inputStream = inputStream;
            this.consumer = consumer;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void run() {
            new BufferedReader(new InputStreamReader(inputStream)).lines()
                    .forEach(consumer);
        }
    }
}