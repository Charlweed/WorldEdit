/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
 /*
 * LocalRegistrar
 * Copyright (C) 2011 Charles Hymes <http://www.hymerfania.com>
 */
package com.sk89q.worldedit.util.command.fluent;

import static com.google.common.base.Preconditions.checkNotNull;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.platform.PlatformManager;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import static java.util.Collections.EMPTY_LIST;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipException;
import static com.sk89q.worldedit.util.command.fluent.ClasspathJarAppender.findClass;
import java.util.HashMap;
import java.util.Map;

/**
 * Registers Local Commands by registering all of the commands, in all of the
 * jars, in the WorldEdit directory.
 * <br>
 * Usage: Given a jar that has a class or classes with methods annotated with
 * the <code>Command</code>  <code>Annotation</code>.<br>
 * Place the jar in the <code>"plugins/WorldEdit"</code> directory of your
 * server installation.<br>
 * Restart your server.<br> That's it!<br>
 * This allows new Java-based WorldEdit commands to be added, without
 * recompiling WorldEdit.
 * <br>
 * Developer Notes:
 * <br>
 * The available <code>Logger</code> does not seem to support the default
 * Loggers formatting functionality, so logs concatenate strings instead of
 * using <code>{}</code>.
 *
 * @author charles@hymes.name
 * @see com.sk89q.minecraft.util.commands.Command
 *
 */
public class LocalRegistrar {

    private static final Logger LOGGER = Logger.getLogger(System.class.getName());//Local Logger does not work yet.
    private static final IsDirFilter DIR_FILTER = new IsDirFilter();
    private static final IsFileFilter FILE_FILTER = new IsFileFilter();
    private static final IsJarFilter JAR_FILTER = new IsJarFilter();
    private static final ClassNameFilter CLASSNAME_FILTER = new ClassNameFilter();
    private static final int MAX_DEPTH = 5;
    private static final List<Class<?>> SEARCHED_CLASSES = new ArrayList<>();
    private static final List<File> SEARCHED_DIRS = new ArrayList<>();
    private static final List<File> SEARCHED_JARS = new ArrayList<>();
    private static final Map<String, Class<?>> KNOWN_CLASSES = new HashMap<>();
    public final static String WorldEdit_DATA_PATH = "plugins/WorldEdit";

    public static List<String> commandAliases(Method commandMethod) {
        List<String> result = new ArrayList<>();
        try {
            boolean isCommand = commandMethod.isAnnotationPresent(Command.class);
            if (isCommand) {
                com.sk89q.minecraft.util.commands.Command commandAnnotation = commandMethod.getAnnotation(Command.class);
                result.addAll(Arrays.asList(commandAnnotation.aliases()));
            } else {
                throw new IllegalArgumentException("Method" + commandMethod.getName() + " is not annotated as a command.");
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return Collections.unmodifiableList(result);
    }

    public static boolean hasWorldEditConstructor(Class<?> clazz) {
        boolean result = false;
        Class<?>[] parameterType = new Class<?>[]{WorldEdit.class};
        try {
            result = clazz.getConstructor(parameterType) != null;
        } catch (NoSuchMethodException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        return result;
    }

    private static Object commandInstance(Class<?> clazz, WorldEdit worldEdit) {
        Object result = null;
        if (hasWorldEditConstructor(clazz)) {
            try {
                Class<?>[] parameterType = new Class<?>[]{WorldEdit.class};
                Object[] arguments = {worldEdit};
                Constructor<?> constructor = clazz.getConstructor(parameterType);
                result = constructor.newInstance(arguments);
            } catch (InstantiationException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            } catch (IllegalArgumentException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            } catch (InvocationTargetException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            } catch (NoSuchMethodException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            } catch (SecurityException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        } else {
            try {
                result = clazz.newInstance();
            } catch (InstantiationException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            } catch (IllegalArgumentException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            } catch (SecurityException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public static List<Method> commandMethods(Class<?> someClass) {
        List<Method> result = new ArrayList<>();
        Method[] classMethods;
        if (!ClassNameFilter.test(someClass.getCanonicalName())) {
            LOGGER.log(Level.WARNING, "Skipping unacceptable class {0}", someClass.getName());
            return EMPTY_LIST;
        }
        try {
            classMethods = someClass.getMethods();
            for (Method aMethod : classMethods) {
                boolean isCommand;
                try {
                    isCommand = aMethod.isAnnotationPresent(Command.class);
                    if (isCommand) {
                        result.add(aMethod);
                        break;
                    }
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
                }
            }
        } catch (RuntimeException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        } /**
         * Not sure why this is thrown for a loaded class, but whatever
         */
        catch (java.lang.NoClassDefFoundError ncdf) {
            LOGGER.log(Level.SEVERE, "ncdf for {0}", someClass.getCanonicalName());
            LOGGER.log(Level.SEVERE, ncdf.getMessage(), ncdf);
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Calls RegisterandReturn on every Class that contains Commands, in all of
     * the jars, in the given directory. This allows for easy deployment of Java
     * based commands.
     *
     * @param platformManager The PlatformManager for this instance. May not be
     * null, but if uninitialized, workingDirectory is set to "./"
     * @param dispatcherNode the current dispatcher node.
     * @param worldEdit the WorldEdit instance
     * @return The same DispatcherNode passed as an argument.
     */
    public static DispatcherNode registerJaredCommands(PlatformManager platformManager, DispatcherNode dispatcherNode, WorldEdit worldEdit) {
        checkNotNull(platformManager);
        checkNotNull(dispatcherNode);
        checkNotNull(worldEdit);

        Path workingDir;
        Path commandJarsDir;
        /**
         * We should use
         * platformManager.getConfiguration().getWorkingDirectory() but at this
         * point in the code, PlatformManager is typically uninitialized. Lucky
         * for us, getWorkingDirectory() is currently just new File("."), so we
         * pass the static final String WorldEdit_DATA_PATH.
         *
         */
        try {
            workingDir = platformManager.getConfiguration().getWorkingDirectory().toPath();
        } catch (com.sk89q.worldedit.extension.platform.NoCapablePlatformException ncpe) {
            LOGGER.warning("PlatformManager instance is not (yet) uninitialized.");
            workingDir = Paths.get(".");
        }
        try {
            commandJarsDir = workingDir.resolve(WorldEdit_DATA_PATH).toAbsolutePath();
            if (Files.exists(commandJarsDir)) {
                LOGGER.log(Level.INFO, "Searching directory \"{0}\" for Commands", commandJarsDir.toString());
                LocalRegistrar jarRegistrar = new LocalRegistrar(dispatcherNode, commandJarsDir.toFile(), worldEdit);
                jarRegistrar.registerExtensionCommands();
            } else {
                LOGGER.log(Level.WARNING, "Plugin directory \"{0}\" does not (yet) exist.", commandJarsDir.toString());
            }
        } catch (RuntimeException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return dispatcherNode;
    }

    private final DispatcherNode _dispatcherNode;
    private final File _extenensionsDir;
    private final WorldEdit _worldEdit;

    /**
     * Creates an instance of LocalCommands. Not meant for public use, because
     * the static method
     *
     * @param commandManager the CommandsManager that will register the command.
     * @param extenensionsDir the location of the jars that contain the classes
     * with {@code Command}s.
     */
    private LocalRegistrar(DispatcherNode dispatcherNode, File extenensionsDir, WorldEdit worldEdit) {
        this._dispatcherNode = dispatcherNode;
        this._extenensionsDir = extenensionsDir;
        this._worldEdit = worldEdit;
    }

    /**
     * Registers all of the commands, in all of the classes, in all of the jars,
     * in all of all of the directories and subdirectories in the WorldEdit
     * directory.
     */
    private void registerExtensionCommands() {
        LOGGER.log(Level.WARNING, getExtenensionsDir().getAbsolutePath());
        List<URL> cmdJarUrls = new ArrayList<>(4);
        List<Object> dispatchables = new ArrayList(4);
        registerExtensionCommandsInDir(cmdJarUrls, dispatchables, getExtenensionsDir());
    }

    private void registerExtensionCommandsInDir(final List<URL> cmdJarUrls, final List<Object> dispatchables, final File extDir) {
        if (SEARCHED_DIRS.contains(extDir)) {
            return;
        }
        SEARCHED_DIRS.add(extDir);
        LOGGER.log(Level.WARNING, extDir.toString());

        if (!DIR_FILTER.accept(extDir)) {
            throw new IllegalArgumentException("File " + extDir.toString() + " is not a directory.");
        }
        int jarCount = extDir.listFiles(new IsJarFilter()).length;

        StringBuilder jarCountMessage = new StringBuilder("Found ");
        jarCountMessage.append(jarCount);
        jarCountMessage.append(" jar files in ");
        jarCountMessage.append(extDir.getAbsolutePath());
        LOGGER.log(Level.FINE, jarCountMessage.toString());
        /**
         * Add All Jars to the classpath before we attempt to load any class.*
         */
        for (File someJar : extDir.listFiles(new IsJarFilter())) {
            URL someJarURL;
            try {
                someJarURL = someJar.toURI().toURL();
                if (!cmdJarUrls.contains(someJarURL)) {
                    cmdJarUrls.add(someJarURL);
                    LOGGER.log(Level.WARNING, "Added {0} to commands dynamic classpath", someJarURL);
                }
            } catch (MalformedURLException ex) {
                LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }

        for (File someJar : extDir.listFiles(new IsJarFilter())) {
            registerExtensionCommandsInJar(cmdJarUrls, dispatchables, someJar);
        }

        for(Object dispatchable : dispatchables) {
            _dispatcherNode.registerMethods(dispatchable);
        }
    }

    private void registerExtensionCommandsInJar(final List<URL> cmdJarUrls, final List<Object> dispatchables, final File someJar) {
        String entryName;
        Class<?> entryClass;
        if (SEARCHED_JARS.contains(someJar)) {
            return;
        }
        SEARCHED_JARS.add(someJar);
        LOGGER.log(Level.INFO, "Searching {0} for commands.", someJar.toString());

        if (!JAR_FILTER.accept(someJar)) {
            throw new IllegalArgumentException("File " + someJar.toString() + " is not a jar file.");
        }
        try {
            JarFile jarfile;
            URL someJarURL;
            someJarURL = someJar.toURI().toURL();
            if (!cmdJarUrls.contains(someJarURL)) {
                cmdJarUrls.add(someJarURL);
                LOGGER.log(Level.INFO, "Added {0} to classpath", someJarURL);
            }
            URL[] jarURLs = new URL[cmdJarUrls.size()];
            java.net.URLClassLoader jarRegistrarLoader = new URLClassLoader(cmdJarUrls.toArray(jarURLs), getClass().getClassLoader());
            jarfile = new JarFile(someJar);
            for (JarEntry someZipEntry : Collections.list(jarfile.entries())) {
                entryName = someZipEntry.getName();
                if (entryName.endsWith(".class") && !entryName.contains("$")) {
                    LOGGER.log(Level.FINE, "Found class entry {0}", entryName);
                    entryClass = classFromName(jarRegistrarLoader, entryName);
                    if (entryClass != null) {
                        if (hasCommands(entryClass, 0)) {
                            /*We can't check for commands previously registerd
                            with the same name becuase the
                            'keys' for the command mappings are not exposed. We can
                            only catch the exception :(
                             */
                            try {
                                LOGGER.log(Level.INFO, "Registering commands of {0}", entryClass.getName());
                                Object cmdInstance = commandInstance(entryClass, _worldEdit);
                                if (cmdInstance != null) {
                                    dispatchables.add(cmdInstance);
                                } else {
                                    LOGGER.log(Level.WARNING, "Could not instanceiate  {0}", entryClass.getName());
                                }
                            } catch (IllegalArgumentException iae) {
                                LOGGER.log(Level.WARNING, "{0} not registered becuase it has at least one alias that has been previously registered", entryName);
                            }
                        } else {
                            LOGGER.log(Level.FINE, "{0} has no methods annotated as Commands", entryName);
                        }
                    } else {
                        LOGGER.log(Level.INFO, "Could not load {0}", entryName);
                    }
                }
            }
        } catch (ZipException ze) {
            LOGGER.log(Level.SEVERE, ze.getMessage(), ze);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    private Class<?> classFromName(final URLClassLoader jarRegistrarLoader, String entryName) {
        Class<?> someclass;
        String plainName = entryName.substring(0, entryName.length() - 6).replace("/", ".");
        if (KNOWN_CLASSES.containsKey(plainName)) {
            LOGGER.log(Level.FINE, "Returning Known Class {0}", plainName);
            return KNOWN_CLASSES.get(plainName);
        }
        LOGGER.log(Level.FINE, "Searching for class {0}", plainName);
        try {
            someclass = findClass(ClassNameFilter::test, jarRegistrarLoader, plainName);
            KNOWN_CLASSES.put(plainName, someclass);
        } catch (ClassNotFoundException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            someclass = null;
        } catch (RuntimeException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            someclass = null;
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            someclass = null;
        }
        return someclass;
    }

    private boolean hasCommands(final Class<?> someClass, final int depth) {
        boolean result = false;
        Method[] classMethods;
        Class<?>[] innerClasses;
        if (depth > MAX_DEPTH) {
            return false;
        }
        if (!CLASSNAME_FILTER.test(someClass.getCanonicalName())) {
            LOGGER.log(Level.WARNING, "Skipping unacceptable class {0}", someClass.getName());
            return false;
        }
        try {
            classMethods = someClass.getMethods();
            for (Method aMethod : classMethods) {
                boolean isCommand;
                try {
                    isCommand = aMethod.isAnnotationPresent(Command.class);
                    if (isCommand) {
                        result = true;
                        break;
                    }
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, null, ex);
                }

            }
        } catch (RuntimeException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        } /**
         * Not sure why this is thrown for a loaded class, but whatever
         */
        catch (java.lang.NoClassDefFoundError ncdf) {
            LOGGER.log(Level.SEVERE, "ncdf for {0}", someClass.getCanonicalName());
            LOGGER.log(Level.SEVERE, ncdf.getMessage(), ncdf);
        }
        if (!result && (depth < MAX_DEPTH)) {
            try {
                innerClasses = someClass.getClasses();
                for (Class<?> innerClass : innerClasses) {
                    if (hasCommands(innerClass, depth + 1)) {
                        result = true;
                        break;
                    }
                }
            } /**
             * Can't use Throwable,it enables the StackOverFlowError
             * catastrophe*
             */
            catch (RuntimeException ex) {
                LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
        return result;
    }

    /**
     * @return the _extenensionsDir
     */
    public File getExtenensionsDir() {
        return _extenensionsDir;
    }

    /**
     * *
     * Accepts any file that is a directory. Accepts Symbolic links to dirs as
     * well in Windows NT+. Symbolic links can only be truly detected in Java
     * 1.7 *
     */
    private static class IsDirFilter extends javax.swing.filechooser.FileFilter implements java.io.FileFilter {

        @Override
        public String getDescription() {
            return "Files that are jars";
        }

        @Override
        public boolean accept(File pathname) {
            return accept(pathname, null);
        }

        public boolean accept(File pathname, String dummy) {
            boolean result;
            result = pathname.isDirectory();
            /**
             * This gets more useful and more complicated in JVM 1.7*
             */
            return result;
        }
    }

    /**
     * *
     * Accepts any file that is a plain,regular file. Makes crude attempt to
     * reject symbolic links. Often fails to detect links in Windows NT+ Can
     * only be fixed in Java 1.7 *
     */
    private static class IsFileFilter extends javax.swing.filechooser.FileFilter implements java.io.FileFilter {

        @Override
        public String getDescription() {
            return "Files that are files, not directories or links.";
        }

        /**
         * ***
         * @param file
         * @return {@code true} if the file is a symbolic link. However this
         * often fails to correctly return true on JVMs before 1.7, especially
         * on Windows.
         * @throws IOException
         * @deprecated There is no good way to do this on Windows without a 1.7
         * JVM
         */
        @Deprecated
        public static boolean isSymlink(File file) throws IOException {
            boolean isSymbolicLink;

            if (file == null) {
                throw new IllegalArgumentException("File must not be null");
            } //        java.nio.file.attribute.BasicFileAttributes attrs = Attributes.readBasicFileAttributes(file.toPath());
            //        java.nio.file.attribute.BasicFileAttributes attrs;
            //        attrs = Files.readAttributes(file.toPath(),BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            //        isSymbolicLink = attrs.isSymbolicLink();
            else {
                File canon;
                if (file.getParent() == null) {
                    canon = file;
                } else {
                    File canonDir = file.getParentFile().getCanonicalFile();
                    canon = new File(canonDir, file.getName());
                }
                isSymbolicLink = !canon.getCanonicalFile().equals(canon.getAbsoluteFile());
            }

            if (isSymbolicLink) {
                LOGGER.log(Level.WARNING, "{0} is a symbolicLink", file.getPath());
                /**
                 * Logger does not support {} *
                 */
            }
            return isSymbolicLink;
        }

        @Override
        public boolean accept(File pathname) {
            return accept(pathname, null);
        }

        public boolean accept(File pathname, String dummy) {
            boolean result = false;

            try {
                result = pathname.isFile();
                if (!result) {
                    LOGGER.log(Level.FINEST, "{0} is not a file ", pathname.getPath());
                    /**
                     * Logger does not support {} *
                     */
                }
                result = result && !isSymlink(pathname);
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            }

            return result;
        }
    }

    /**
     * *
     * Accepts any file that is a file, and ends in ".jar" File does not have to
     * exist, nor be readable, etc. *
     */
    private static class IsJarFilter extends javax.swing.filechooser.FileFilter implements java.io.FileFilter {

        @Override
        public String getDescription() {
            return "Directories, not files.";
        }

        @Override
        public boolean accept(File pathname) {
            return accept(pathname, null);
        }

        public boolean accept(File pathname, String dummy) {
            boolean result;
            result = FILE_FILTER.accept(pathname);
            result = result && pathname.getName().endsWith(".jar");
            return result;
        }
    }

    /**
     * Rejects class names that belong to known WorldEdit Commands.
     *
     */
    private static class ClassNameFilter {

        private static final List<String> UNACCEPTABLE = Arrays.asList(
                "nmsblocks.CBXNmsBlock_1710",
                "com.sk89q.worldedit.command.BiomeCommands",
                "com.sk89q.worldedit.command.BrushCommands",
                "com.sk89q.worldedit.command.ChunkCommands",
                "com.sk89q.worldedit.command.ClipboardCommands",
                "com.sk89q.worldedit.command.GeneralCommands",
                "com.sk89q.worldedit.command.GenerationCommands",
                "com.sk89q.worldedit.command.HistoryCommands",
                "com.sk89q.worldedit.command.NavigationCommands",
                "com.sk89q.worldedit.command.RegionCommands",
                "com.sk89q.worldedit.command.SchematicCommands",
                "com.sk89q.worldedit.command.ScriptingCommands",
                "com.sk89q.worldedit.command.SelectionCommands",
                "com.sk89q.worldedit.command.SnapshotCommands",
                "com.sk89q.worldedit.command.SnapshotUtilCommands",
                "com.sk89q.worldedit.command.SuperPickaxeCommands",
                "com.sk89q.worldedit.command.ToolCommands",
                "com.sk89q.worldedit.command.ToolUtilCommands",
                "com.sk89q.worldedit.command.UtilityCommands",
                "com.sk89q.worldedit.command.WorldEditCommands"
        );

        public static boolean test(String className) {
            return !UNACCEPTABLE.contains(className);
        }
    }

}
