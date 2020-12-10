/*
 * This file is part of the MDj Command-line Interface program
 * (aka: mdj-cli).
 *
 * Copyright (C) 2020 Bradley Willcott
 *
 * mdj-cli is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * mdj-cli is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.bewsoftware.mdj.cli;

/**
 * Create a 'jar' file.
 *
 * @author <a href="mailto:bw.opensource@yahoo.com">Bradley Willcott</a>
 *
 * @since 0.1
 * @version 1.0.14
 */
import com.bewsoftware.fileio.ini.IniFile;
import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.Deflater;

import static com.bewsoftware.fileio.BEWFiles.getResource;
import static java.nio.file.Path.of;

public class Jar {

    /**
     * Create a 'jar' file containing the files whose paths are supplied.
     *
     * @param jarFile        The new jar file.
     * @param jarFilePaths   Jar file paths to include.
     * @param jarFileDirPath Jar file directory path.
     * @param filePaths      Paths to the files to include.
     * @param fileDirPath    Directory to process.
     * @param manifest       The manifest to include.
     * @param vlevel         Reporting verbosity level.
     *
     * @throws IOException        if any.
     * @throws URISyntaxException if any.
     */
    public static void createJAR(final File jarFile,
                                 final List<Path> jarFilePaths,
                                 final Path jarFileDirPath,
                                 final List<Path> filePaths,
                                 final Path fileDirPath,
                                 final Manifest manifest, final int vlevel)
            throws IOException, URISyntaxException {

        // Hold the exceptions.
        List<IOException> exceptions = new ArrayList<>();

        try ( JarOutputStream jos = new JarOutputStream(new BufferedOutputStream(
                new FileOutputStream(jarFile)), manifest))
        {

            jos.setLevel(Deflater.BEST_COMPRESSION);

            //
            // Copy files from MDj-CLI jar file...
            //
            jarFilePaths.stream().<File>map(Path::toFile)
                    .filter(name -> name.exists() && !name.isDirectory())
                    .map(File::toPath)
                    .forEachOrdered(jarFilePath ->
                    {
                        try
                        {
                            jos.putNextEntry(new JarEntry(
                                    jarFileDirPath.relativize(jarFilePath).toString()));

                            addEntryContent(jos, jarFilePath);
                            jos.closeEntry();
                        } catch (IOException ex)
                        {
                            exceptions.add(ex);
                        }
                    });

            //
            // Copy files from the user's docs directory...
            //
            filePaths.stream().<File>map(Path::toFile)
                    .filter(name -> name.exists() && !name.isDirectory())
                    .map(File::toPath)
                    .forEachOrdered(filePath ->
                    {
                        try
                        {
                            jos.putNextEntry(new JarEntry(
                                    fileDirPath.relativize(filePath).toString()));

                            addEntryContent(jos, filePath);
                            jos.closeEntry();
                        } catch (IOException ex)
                        {
                            exceptions.add(ex);
                        }
                    });

            if (!exceptions.isEmpty())
            {
                throw exceptions.remove(0);
            }
        }
    }

    /**
     * Create a new Manifest.
     *
     * @param pom  The program's POM properties.
     * @param conf The document's configuration data.
     *
     * @return the new Manifest.
     */
    public static Manifest getManifest(final MCPOMProperties pom, final IniFile conf) {
        Manifest manifest = getManifest(pom.title + " (" + pom.version + ")");

        if (conf != null)
        {
            System.out.println("adding [MANIFEST.mf] entries");
            if (conf.iniDoc.containsSection("MANIFEST.mf"))
            {
                Attributes mainAttribs = manifest.getMainAttributes();

                conf.iniDoc.getSection("MANIFEST.mf").forEach(prop
                        -> mainAttribs.put(new Attributes.Name(prop.key()), prop.value()));
            }
        } else
        {
            System.out.println("=== No conf! ===");
        }

        return manifest;
    }

    /**
     * Create a new Manifest.
     *
     * @param progname The program name - should include version data:
     *                 {@code <program name> (<version>)}
     *
     * @return the new Manifest.
     */
    public static Manifest getManifest(final String progname) {

        Manifest manifest = new Manifest();
        Attributes mainAttribs = manifest.getMainAttributes();
        mainAttribs.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        mainAttribs.put(new Attributes.Name("Created-By"), progname);
        mainAttribs.put(Attributes.Name.CONTENT_TYPE, "text/html");
        mainAttribs.put(new Attributes.Name("Build-Jdk-Spec"), "12");
        mainAttribs.put(new Attributes.Name("HTTPServer-version"), "2.5.2");
        mainAttribs.put(Attributes.Name.MAIN_CLASS, "com.bewsoftware.httpserver.HTTPServer");

        return manifest;
    }

    /**
     * Add the contents of the file for the new entry into the jar file.
     *
     * @param jos           The jar file.
     * @param entryFilePath The file to copy in.
     *
     * @throws IOException if any.
     */
    private static void addEntryContent(final JarOutputStream jos, final Path entryFilePath)
            throws IOException {

        try ( BufferedInputStream bis = new BufferedInputStream(
                new FileInputStream(entryFilePath.toFile())))
        {

            byte[] buffer = new byte[1024];
            int count = -1;

            while ((count = bis.read(buffer)) != -1)
            {
                jos.write(buffer, 0, count);
            }
        }
    }

    /**
     * Not meant to be instantiated.
     */
    private Jar() {
    }
}
