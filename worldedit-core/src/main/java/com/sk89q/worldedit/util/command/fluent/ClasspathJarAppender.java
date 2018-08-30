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
 * ClasspathJarAppender
 * Copyright (C) 2011 Charles Hymes <http://www.hymerfania.com>
 */
package com.sk89q.worldedit.util.command.fluent;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author chymes
 */
public class ClasspathJarAppender {

    private static final Logger LOG = Logger.getLogger(System.class.getName());//We can only depend upon a logger from the JRE.

    /**
     * Parameters of the method to add an URL to the System classes.
     */
    private static final String FAIL_PREFIX = "Error, could not load class from URL ";
    private static final String FAIL_SUFFIX = " via local ClassLoader";

    /**
     * Adds a file to the classpath.
     *
     * @param jarRegistrarLoader
     * @param fileName a String pointing to the file
     * @throws IOException
     */
    public static void addFile(URLClassLoader jarRegistrarLoader, String fileName) throws IOException {
        Path path = Paths.get(fileName);
        addFile(jarRegistrarLoader, path);
    }

    /**
     * Adds a file to the classpath
     *
     * @param path the file to be added
     * @param jarRegistrarLoader the value of localClassLoader
     * @throws IOException
     */
    public static void addFile(URLClassLoader jarRegistrarLoader, Path path) throws IOException {
        LOG.log(Level.FINE, path.toString());
        addURL(jarRegistrarLoader, new URL[]{path.toUri().toURL()});
    }

    /**
     * Adds the content pointed by the URL to the classpath.
     *
     * @param jarRegistrarLoader the current classLoader
     * @param classURLs the URLs pointing to the content to be added
     * @return A new class loader subordinate to existing localClassLoader
     * @throws IOException
     */
    public static URLClassLoader addURL(final URLClassLoader jarRegistrarLoader, final URL[] classURLs) throws IOException {
        return new URLClassLoader(classURLs, jarRegistrarLoader);
    }

    /**
     * <p>
     * A Wrapper method for Class.forName, that ignores class names rejected by
     * the filter predicate.</p>
     * <p>
     * <b>This method exposes bugs in the JVM.</b> Loading some classes, in
     * particular classes already loaded from the <code>com.sk89q</code>
     * packages, may crash the JVM instead of throwing
     * <code>Exceptions</code>.</p>
     *
     * @param filter A <code>Predicate</code> that rejects class names belonging
     * to known WorldEdit Commands, previously loaded classes, and other classes
     * where their should be no load attempt. This test can prevent the JVM from
     * crashing, so be as general as possible. For example, reject any class
     * containing the substring <code>com.sk89q</code>
     * @param jarRegistrarLoader the subordinate Jar ClassLoader
     * @param name The name of the class to find
     * @return The loaded class.
     * @throws ClassNotFoundException
     */
    public static Class<?> findClass(Predicate<String> filter, URLClassLoader jarRegistrarLoader, String name) throws ClassNotFoundException {
        StringBuilder message = new StringBuilder("Finding and loading class ");
        message.append(name);
        LOG.log(Level.FINE, "{0}", new String[]{message.toString()});
        Class<?> result = null;
        //This test must prevent the JVM from crashing!
        if (filter.test(name)) {
            try {
//                System.err.println(message);//When the JVM crashes, all loggers fail.
                System.err.flush();//If crash, we need logger output to be unmixed.
                System.out.flush();//If crash, we need logger output to be unmixed.                
                result = Class.forName(name, true, jarRegistrarLoader);
            } //May have different handling of these exceptions someday
            catch (SecurityException ex) {
                LOG.log(Level.SEVERE, ex.getMessage(), ex);
                throw new ClassNotFoundException(FAIL_PREFIX + name + FAIL_SUFFIX, ex);
            } catch (RuntimeException ex) {
                LOG.log(Level.SEVERE, ex.getMessage(), ex);
                throw new ClassNotFoundException(FAIL_PREFIX + name + FAIL_SUFFIX, ex);
            } catch (Exception ex) {
                LOG.log(Level.SEVERE, ex.getMessage(), ex);
                throw new ClassNotFoundException(FAIL_PREFIX + name + FAIL_SUFFIX, ex);
            } finally {
                System.out.flush();
                System.err.flush();
            }
        } else {
            LOG.log(Level.INFO, "Ignored classname {0}", new String[]{name});
        }
        if (result != null) {
            LOG.log(Level.FINE, "Successfully found and loaded class {0}", name);
        }
        return result;
    }

    private ClasspathJarAppender() {
    }

}
