/*
 * Autopsy Forensic Browser
 *
 * Copyright 2011-2015 Basis Technology Corp.
 * Contact: carrier <at> sleuthkit <dot> org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sleuthkit.autopsy.casemodule;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.openide.util.NbBundle;
import static org.sleuthkit.autopsy.casemodule.XMLCaseManagement.TOP_ROOT_NAME;
import org.sleuthkit.autopsy.coreutils.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Provides access to case metadata.
 */
public final class CaseMetadata {

    /**
     * Exception thrown by the CaseMetadata class when there is a problem
     * accessing the metadata for a case.
     */
    public final static class CaseMetadataException extends Exception {

        private static final long serialVersionUID = 1L;

        private CaseMetadataException(String message) {
            super(message);
        }

        private CaseMetadataException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    final static String XSDFILE = "CaseSchema.xsd"; //NON-NLS
    final static String TOP_ROOT_NAME = "AutopsyCase"; //NON-NLS
    final static String CASE_ROOT_NAME = "Case"; //NON-NLS
    // general metadata about the case file
    final static String NAME = "Name"; //NON-NLS
    final static String NUMBER = "Number"; //NON-NLS
    final static String EXAMINER = "Examiner"; //NON-NLS
    final static String CREATED_DATE_NAME = "CreatedDate"; //NON-NLS
    final static String MODIFIED_DATE_NAME = "ModifiedDate"; //NON-NLS
    final static String SCHEMA_VERSION_NAME = "SchemaVersion"; //NON-NLS
    final static String AUTOPSY_CRVERSION_NAME = "AutopsyCreatedVersion"; //NON-NLS
    final static String AUTOPSY_MVERSION_NAME = "AutopsySavedVersion"; //NON-NLS
    final static String CASE_TEXT_INDEX_NAME = "TextIndexName"; //NON-NLS
    // folders inside case directory
    final static String LOG_FOLDER_NAME = "LogFolder"; //NON-NLS
    final static String LOG_FOLDER_RELPATH = "Log"; //NON-NLS
    final static String TEMP_FOLDER_NAME = "TempFolder"; //NON-NLS
    final static String TEMP_FOLDER_RELPATH = "Temp"; //NON-NLS
    final static String EXPORT_FOLDER_NAME = "ExportFolder"; //NON-NLS
    final static String EXPORT_FOLDER_RELPATH = "Export"; //NON-NLS
    final static String CACHE_FOLDER_NAME = "CacheFolder"; //NON-NLS
    final static String CACHE_FOLDER_RELPATH = "Cache"; //NON-NLS
    final static String CASE_TYPE = "CaseType"; //NON-NLS
    final static String DATABASE_NAME = "DatabaseName"; //NON-NLS
    // folders attribute
    final static String RELATIVE_NAME = "Relative";    // relevant path info NON-NLS
    // folder attr values
    final static String RELATIVE_TRUE = "true";     // if it's a relative path NON-NLS
    final static String RELATIVE_FALSE = "false";   // if it's not a relative path NON-NLS

    private Document doc;
    private final Case.CaseType caseType;
    private final String caseName;
    private final String caseNumber;
    private final String examiner;
    private final String caseDirectory;
    private final String caseDatabaseName;
    private final String caseTextIndexName;
    private static final Logger logger = Logger.getLogger(CaseMetadata.class.getName());

    public CaseMetadata(Case.CaseType caseType, String caseName, String caseNumber, String exainer, String caseDirectory, String caseDatabaseName, String caseTextIndexName) throws CaseMetadataException {
        this.caseType = caseType;
        this.caseName = caseName;
        this.caseNumber = caseNumber;
        this.examiner = exainer;
        this.caseDirectory = caseDirectory;
        this.caseDatabaseName = caseDatabaseName;
        this.caseTextIndexName = caseTextIndexName;
        this.create();
    }

    /**
     * Constructs an object that provides access to case metadata.
     *
     * @param metadataFilePath Path to the metadata (.aut) file for a case.
     */
    public CaseMetadata(Path metadataFilePath) throws CaseMetadataException {
        try {
            /*
             * TODO (RC): This class should eventually replace the non-public
             * and unsafe XMLCaseManagement class altogether.
             */
            XMLCaseManagement metadata = new XMLCaseManagement();
            metadata.open(metadataFilePath.toString());
            try {
                caseType = metadata.getCaseType();
            } catch (NullPointerException unused) {
                throw new CaseMetadataException("Case type element missing");
            }
            try {
                caseName = metadata.getCaseName();
                if (caseName.isEmpty()) {
                    throw new CaseMetadataException("Case name missing");
                }
            } catch (NullPointerException unused) {
                throw new CaseMetadataException("Case name element missing");
            }
            try {
                caseNumber = metadata.getCaseNumber();
            } catch (NullPointerException unused) {
                throw new CaseMetadataException("Case number element missing");
            }
            try {
                examiner = metadata.getCaseExaminer();
            } catch (NullPointerException unused) {
                throw new CaseMetadataException("Examiner element missing");
            }
            try {
                caseDirectory = metadata.getCaseDirectory();
                if (caseDirectory.isEmpty()) {
                    throw new CaseMetadataException("Case directory missing");
                }
            } catch (NullPointerException unused) {
                throw new CaseMetadataException("Case directory element missing");
            }
            try {
                caseDatabaseName = metadata.getDatabaseName();
            } catch (NullPointerException unused) {
                throw new CaseMetadataException("Case database element missing");
            }
            try {
                caseTextIndexName = metadata.getTextIndexName();
                if (Case.CaseType.MULTI_USER_CASE == caseType && caseDatabaseName.isEmpty()) {
                    throw new CaseMetadataException("Case keyword search index name missing");
                }
            } catch (NullPointerException unused) {
                throw new CaseMetadataException("Case keyword search index name missing");
            }
        } catch (CaseActionException ex) {
            throw new CaseMetadataException(ex.getLocalizedMessage(), ex);
        }
        this.create();
    }

    /**
     * Gets the case type.
     *
     * @return The case type.
     */
    public Case.CaseType getCaseType() {
        return this.caseType;
    }

    /**
     * Gets the case name.
     *
     * @return The case name.
     */
    public String getCaseName() {
        return caseName;
    }

    /**
     * Gets the case number.
     *
     * @return The case number, may be empty.
     */
    public String getCaseNumber() {
        return caseNumber;
    }

    /**
     * Gets the examiner.
     *
     * @return The examiner, may be empty.
     */
    public String getExaminer() {
        return examiner;
    }

    /**
     * Gets the case directory.
     *
     * @return The case directory.
     */
    public String getCaseDirectory() {
        return caseDirectory;
    }

    /**
     * Gets the case database name.
     *
     * @return The case database name, will be empty for a single-user case.
     */
    public String getCaseDatabaseName() {
        return caseDatabaseName;
    }

    /**
     * Gets the text index name.
     *
     * @return The case text index name, will be empty for a single-user case.
     */
    public String getTextIndexName() {
        return caseTextIndexName;
    }

    private void create() throws CaseMetadataException {
        DocumentBuilder docBuilder;
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();

        // throw an error here
        try {
            docBuilder = docFactory.newDocumentBuilder();
        } catch (ParserConfigurationException ex) {
            clear();
            throw new CaseMetadataException(
                    NbBundle.getMessage(this.getClass(), "XMLCaseManagement.create.exception.msg"), ex);
        }
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss (z)");

        doc = docBuilder.newDocument();
        Element rootElement = doc.createElement(TOP_ROOT_NAME); // <AutopsyCase> ... </AutopsyCase>
        doc.appendChild(rootElement);

        Element crDateElement = doc.createElement(CREATED_DATE_NAME); // <CreatedDate> ... </CreatedDate>
        crDateElement.appendChild(doc.createTextNode(dateFormat.format(new Date())));
        rootElement.appendChild(crDateElement);

        Element mDateElement = doc.createElement(MODIFIED_DATE_NAME); // <ModifedDate> ... </ModifedDate>
        mDateElement.appendChild(doc.createTextNode(dateFormat.format(new Date())));
        rootElement.appendChild(mDateElement);

        Element autVerElement = doc.createElement(AUTOPSY_CRVERSION_NAME); // <AutopsyVersion> ... </AutopsyVersion>
        autVerElement.appendChild(doc.createTextNode(System.getProperty("netbeans.buildnumber")));
        rootElement.appendChild(autVerElement);

        Element autSavedVerElement = doc.createElement(AUTOPSY_MVERSION_NAME); // <AutopsySavedVersion> ... </AutopsySavedVersion>
        autSavedVerElement.appendChild(doc.createTextNode(System.getProperty("netbeans.buildnumber")));
        rootElement.appendChild(autSavedVerElement);

        Element schVerElement = doc.createElement(SCHEMA_VERSION_NAME); // <SchemaVersion> ... </SchemaVersion>
        schVerElement.appendChild(doc.createTextNode(schemaVersion));
        rootElement.appendChild(schVerElement);

        Element caseElement = doc.createElement(CASE_ROOT_NAME); // <Case> ... </Case>
        rootElement.appendChild(caseElement);

        Element nameElement = doc.createElement(NAME); // <Name> ... </Name>
        nameElement.appendChild(doc.createTextNode(caseName));
        caseElement.appendChild(nameElement);

        Element numberElement = doc.createElement(NUMBER); // <Number> ... </Number>
        numberElement.appendChild(doc.createTextNode(String.valueOf(caseNumber)));
        caseElement.appendChild(numberElement);

        Element examinerElement = doc.createElement(EXAMINER); // <Examiner> ... </Examiner>
        examinerElement.appendChild(doc.createTextNode(examiner));
        caseElement.appendChild(examinerElement);

        Element exportElement = doc.createElement(EXPORT_FOLDER_NAME); // <ExportFolder> ... </ExportFolder>
        exportElement.appendChild(doc.createTextNode(EXPORT_FOLDER_RELPATH));
        exportElement.setAttribute(RELATIVE_NAME, "true"); //NON-NLS
        caseElement.appendChild(exportElement);

        Element logElement = doc.createElement(LOG_FOLDER_NAME); // <LogFolder> ... </LogFolder>
        logElement.appendChild(doc.createTextNode(LOG_FOLDER_RELPATH));
        logElement.setAttribute(RELATIVE_NAME, "true"); //NON-NLS
        caseElement.appendChild(logElement);

        Element tempElement = doc.createElement(TEMP_FOLDER_NAME); // <TempFolder> ... </TempFolder>
        tempElement.appendChild(doc.createTextNode(TEMP_FOLDER_RELPATH));
        tempElement.setAttribute(RELATIVE_NAME, "true"); //NON-NLS
        caseElement.appendChild(tempElement);

        Element cacheElement = doc.createElement(CACHE_FOLDER_NAME); // <CacheFolder> ... </CacheFolder>
        cacheElement.appendChild(doc.createTextNode(CACHE_FOLDER_RELPATH));
        cacheElement.setAttribute(RELATIVE_NAME, "true"); //NON-NLS
        caseElement.appendChild(cacheElement);

        Element typeElement = doc.createElement(CASE_TYPE); // <CaseType> ... </CaseType>
        typeElement.appendChild(doc.createTextNode(caseType.toString()));
        caseElement.appendChild(typeElement);

        Element dbNameElement = doc.createElement(DATABASE_NAME); // <DatabaseName> ... </DatabaseName>
        dbNameElement.appendChild(doc.createTextNode(this.caseDatabaseName));
        caseElement.appendChild(dbNameElement);

        Element indexNameElement = doc.createElement(CASE_TEXT_INDEX_NAME); // <TextIndexName> ... </TextIndexName>
        indexNameElement.appendChild(doc.createTextNode(this.caseTextIndexName));
        caseElement.appendChild(indexNameElement);
        this.writeFile();
    }

    private void writeFile() throws CaseMetadataException {
        if (doc == null || caseName.equals("")) {
            throw new CaseMetadataException(
                    NbBundle.getMessage(this.getClass(), "XMLCaseManagement.writeFile.exception.noCase.msg"));
        }

        // Prepare the DOM document for writing
        Source source = new DOMSource(doc);

        // Prepare the data for the output file
        StringWriter sw = new StringWriter();
        Result result = new StreamResult(sw);

        // Write the DOM document to the file
        Transformer xformer;// = TransformerFactory.newInstance().newTransformer();
        TransformerFactory tfactory = TransformerFactory.newInstance();

        try {
            xformer = tfactory.newTransformer();
        } catch (TransformerConfigurationException ex) {
            logger.log(Level.SEVERE, "Could not setup tranformer and write case file"); //NON-NLS
            throw new CaseMetadataException(
                    NbBundle.getMessage(this.getClass(), "XMLCaseManagement.writeFile.exception.errWriteToFile.msg"), ex);
        }

        //Setup indenting to "pretty print"
        xformer.setOutputProperty(OutputKeys.INDENT, "yes"); //NON-NLS
        xformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2"); //NON-NLS

        try {
            xformer.transform(source, result);
        } catch (TransformerException ex) {
            logger.log(Level.SEVERE, "Could not run tranformer and write case file"); //NON-NLS
            throw new CaseMetadataException(
                    NbBundle.getMessage(this.getClass(), "XMLCaseManagement.writeFile.exception.errWriteToFile.msg"), ex);
        }

        // preparing the output file
        String xmlString = sw.toString();
        File file = new File(this.caseDirectory + File.separator + caseName + ".aut");

        // write the file
        try {
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
            bw.write(xmlString);
            bw.flush();
            bw.close();
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Error writing to case file"); //NON-NLS
            throw new CaseMetadataException(
                    NbBundle.getMessage(this.getClass(), "XMLCaseManagement.writeFile.exception.errWriteToFile.msg"), ex);
        }
    }

}
