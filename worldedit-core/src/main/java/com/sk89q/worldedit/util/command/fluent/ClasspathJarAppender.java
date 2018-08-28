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
     * I fear this method does NOT do what I expect or want.
     *
     * @param filter A predicate that rejects class names belonging to known
     * WorldEdit Commands.
     * @param jarRegistrarLoader the current ClassLoader
     * @param name the name of the class to find
     * @return The loaded class.
     * @throws ClassNotFoundException
     */
    public static Class<?> findClass(Predicate<String> filter, URLClassLoader jarRegistrarLoader, String name) throws ClassNotFoundException {
        LOG.log(Level.FINE, "finding and loading class {0}", name);
        Class<?> result = null;
        if (filter.test(name)) {
            try {
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
            }
        }
        if (result != null) {
            LOG.log(Level.INFO, "Successfully found and loaded class {0}", name);
        }
        return result;
    }

    private ClasspathJarAppender() {
    }

}
