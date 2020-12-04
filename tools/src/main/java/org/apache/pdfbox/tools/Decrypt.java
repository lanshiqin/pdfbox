/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.pdfbox.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Callable;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * This will read a document from the filesystem, decrypt it and and then write
 * the result to the filesystem.
 *
 * @author  Ben Litchfield
 */
@Command(name = "Decrypt", description = "Decrypts a PDF file.")
public final class Decrypt implements Callable<Integer>
{
    @Option(names = "-alias", description = "the alias to the certificate in the keystore.")
    private String alias;

    @Option(names = {"-h", "--help"}, usageHelp = true, description = "display this help message")
    boolean usageHelpRequested;

    @Option(names = "-keyStore", description = "the path to the keystore that holds the certificate to decrypt the document. " + 
        "This is only required if the document is encrypted with a certificate, otherwise only the password is required.")
    private String keyStore;

    @Option(names = "-password", description = "the password for the PDF or certificate in keystore.")    
    private String password;

    @Parameters(paramLabel = "inputfile", index = "0", arity = "1", description = "the PDF file to decrypt.")
    private File infile;

    @Parameters(paramLabel = "outputfile", index = "1", arity = "0..1", description = "the decrypted PDF file.")
    private File outfile;

    private Decrypt()
    {
    }
    /**
     * This is the entry point for the application.
     *
     * @param args The command-line arguments.
     *
     * @throws IOException If there is an error decrypting the document.
     */
    public static void main(String[] args) throws IOException
    {
        // suppress the Dock icon on OS X
        System.setProperty("apple.awt.UIElement", "true");

        int exitCode = new CommandLine(new Decrypt()).execute(args);
        System.exit(exitCode);
    }


    public Integer call() throws IOException
    {
        try (InputStream keyStoreStream = keyStore == null ? null : new FileInputStream(keyStore); 
                PDDocument document = Loader.loadPDF(infile, password, keyStoreStream, alias))
        {
            // overwrite inputfile if no outputfile was specified
            if (outfile == null) {
                outfile = infile;
            }
            
            if (document.isEncrypted())
            {
                AccessPermission ap = document.getCurrentAccessPermission();
                if(ap.isOwnerPermission())
                {
                    document.setAllSecurityToBeRemoved(true);
                    document.save( outfile );
                }
                else
                {
                    throw new IOException(
                            "Error: You are only allowed to decrypt a document with the owner password." );
                }
            }
            else
            {
                System.err.println( "Error: Document is not encrypted." );
                return 1;
            }
        }
        return 0;
    }
}
