/**
 * Copyright (C) 2007-2017 52°North Initiative for Geospatial Open Source
 * Software GmbH
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation.
 *
 * If the program is linked with libraries which are licensed under one of
 * the following licenses, the combination of the program with the linked
 * library is not considered a "derivative work" of the program:
 *
 *       • Apache License, version 2.0
 *       • Apache Software License, version 1.0
 *       • GNU Lesser General Public License, version 3
 *       • Mozilla Public License, versions 1.0, 1.1 and 2.0
 *       • Common Development and Distribution License (CDDL), version 1.0
 *
 * Therefore the distribution of the program linked with libraries licensed
 * under the aforementioned licenses, is permitted by the copyright holders
 * if the distribution is compliant with both the GNU General Public
 * License version 2 and the aforementioned licenses.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 */
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.n52.wps.server.transactional.profiles.docker.managers;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerCertificates;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.LogStream;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.ContainerExit;
import com.spotify.docker.client.messages.ContainerInfo;
import com.spotify.docker.client.messages.HostConfig;
import com.spotify.docker.client.messages.HostConfig.Bind;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import net.opengis.ows.x20.MetadataType;
import net.opengis.wps.x20.ComplexDataType;
import net.opengis.wps.x20.DataInputType;
import net.opengis.wps.x20.ExecuteDocument;
import net.opengis.wps.x20.ExecuteRequestType;
import net.opengis.wps.x20.InputDescriptionType;
import net.opengis.wps.x20.LiteralDataType;
import net.opengis.wps.x20.OutputDefinitionType;
import net.opengis.wps.x20.OutputDescriptionType;
import net.opengis.wps.x20.ProcessOfferingDocument;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.xfer.FileSystemFile;
import net.schmizz.sshj.xfer.LocalDestFile;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.xmlbeans.XmlException;
import org.n52.wps.commons.WPSConfig;
import org.n52.wps.io.data.IData;
import org.n52.wps.io.data.binding.complex.GenericFileDataBinding;
import org.n52.wps.io.datahandler.parser.GenericFileParser;
import org.n52.wps.server.ExceptionReport;
import org.n52.wps.server.request.ExecuteRequest;
import org.n52.wps.server.request.ExecuteRequestV200;
import org.n52.wps.server.transactional.manager.AbstractTransactionalProcessManager;
import org.n52.wps.server.transactional.profiles.DeploymentProfile;
import org.n52.wps.server.transactional.util.MimeUtil;
import org.n52.wps.webapp.entities.RemoteDockerHostBackend;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

/**
 *
 * @author cnl
 */
public class RemoteDockerProcessManager extends AbstractTransactionalProcessManager {

    private static Logger log = LoggerFactory
            .getLogger(RemoteDockerProcessManager.class);
    private DockerClient docker;

    // list of environmnent variables to provide to the appplication
    private List<String> env = new ArrayList<String>();
    private List<String> dirToMount = new ArrayList<String>();
    private RemoteDockerHostBackend db = getBackendConfig();

    private ProcessOfferingDocument.ProcessOffering description;
    private String instanceId;
    private boolean mountEOStore;
    private SSHClient ssh;
    private Map<String, String> outputs = new HashMap<>();

    public RemoteDockerProcessManager(String processID) throws Exception {
        super(processID);

        /* note: see doc for connection pooling and authentication to private registries */
        /**
         * old docker-java DockerClientConfig config =
         * DefaultDockerClientConfig.createDefaultConfigBuilder()
         * .withDockerHost(db.getDockerHost()) .withDockerTlsVerify(true)
         * .withDockerCertPath(db.getDockerCertPath())
         * .withDockerConfig(db.getDockerConfig())
         * .withApiVersion(db.getApiVersion()) .withRegistryUrl("")
         * .withRegistryUsername(db.getRegistryUserName())
         * .withRegistryPassword(db.getRegistryPassword())
         * .withRegistryEmail(db.getRegistryEmail()) .build(); this.docker =
         * DockerClientBuilder.getInstance(config).build();
         */
    }

    @Override
    public boolean unDeployProcess(String processID) throws Exception {
        return true;
    }

    @Override
    public boolean containsProcess(String processID) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Collection<String> getAllProcesses() throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private Map<String, IData> invoke(ExecuteDocument execute,
            String processId,
            ProcessOfferingDocument.ProcessOffering description, UUID instanceId) throws Exception {
        log.debug("Starting invoke in RemoteDocker manager");
        this.instanceId = instanceId.toString();
        this.description = description;
        this.ssh = DockerUtil.getSSHClient(db.getSshhost(), db.getUser(),
                db.getPassword());

        log.debug("Parse docker image");
        String dockerImageReference = parseDockerImage();
        docker = getDockerConnection();
        // get the ssh client connected to the host with NFS access
        log.debug("Getting SSH connection to the host connected to NFS stores");

        /**
         * TODO parsing of inputs , mouting of volumes, collecting of results*
         */
        /**
         * Inputs handling supports 3 types of inputs: simple parameters
         * (literal data), files to copy in the shared directory, fileReferences
         * (local NFS reference of files)
         */
        log.debug("Handling Inputs");
        // Handling inputs means prepare the environmnent variable and stores input file on configured NFS store
        handleInputs(execute, description);
        // Handling outputs consists of providing the output file locations in environmnent variables
        log.debug("Handling Outputs");
        handleOutputs(execute, description); // TODO CHANGE

        // Get the image URL
        String imageName = dockerImageReference;
        log.debug("pulling image:" + imageName);
        docker.pull(imageName);
        //   ImageInfo infoTest = docker.inspectImage("test");

        writeEnvPropertyFile(env);
        log.debug("Env variables passed to the containers");
        ContainerConfig.Builder build = ContainerConfig.builder().image(
                imageName).env(env);
        // Add all EO NFS directories to mount

        HostConfig.Builder hostConfigBuild = HostConfig.builder();

        for (String d : dirToMount) {
            log.debug("adding mounted directory:" + d);
            hostConfigBuild.appendBinds(Bind.from(d)
                    .to(d)
                    .readOnly(false).build());

        }
        // Mount WPS NFS directory (with inputDir/outputDir)
        hostConfigBuild.appendBinds(Bind.from(db.getNfsWPSPath())
                .to(db.getNfsWPSPath())
                .readOnly(false).build());

        // DEMO hack TODO remove
        hostConfigBuild.appendBinds(Bind.from("/data/landcover")
                .to("/home/worker/workDir")
                .readOnly(false).build());
        final HostConfig hostConfig = hostConfigBuild.build();
        // attach stdout and stderr
        build.attachStdout(true);
        build.attachStderr(true);
        build.hostConfig(hostConfig);
        // Not sure auto remove is already supported!
        hostConfig.autoRemove();

        // create container request
        ContainerConfig container = build.build();

        // create container
        ContainerCreation creation = docker.createContainer(container);

        // get container id
        String id = creation.id();
        // logs declarations
        String stdOutLog;
        String stdErrLog;
// start container
        docker.startContainer(id);

        ContainerInfo info = docker.inspectContainer(id);
        log.debug("Current status: " + info.state().status());

        //DockerUtil.logAndWait(docker, id);
        final ContainerExit exit = docker.waitContainer(id);
        LogStream stream = docker.logs(id, DockerClient.LogsParam.stdout());
        LogStream streamErr = docker.logs(id, DockerClient.LogsParam.stderr());

        log.debug(stream.readFully());
        log.debug(streamErr.readFully());

        //ContainerConfig.builder().image(imageName).cmd("sh", "-c", "while
        /**
         * ServiceCreateResponse serviceResponse = docker.createService(null);
         * Service service = docker.inspectService(serviceResponse.id());
         * service.spec().
         *
         */
        //   CreateContainerResponse container = docker.createContainerCmd("busybox")
        //      .withCmd("touch", "/test").exec();
        //        docker.startContainerCmd(container.getId()).exec();
        // Remove container
        docker.removeContainer(id);

        HashMap<String, IData> resultHash = new HashMap<String, IData>();
        for (String k
                : outputs.keySet()) {
            String outputPath = outputs.get(k);
            String extension = "";
            // define file Path
            ComplexDataType complexData = (ComplexDataType) DockerUtil.getOutputDesc(
                    k, description).getDataDescription();
            if (complexData.getFormatArray(0) != null && complexData.getFormatArray(
                    0).getMimeType() != null) {
                extension = "." + MimeUtil.getExtension(
                        complexData.getFormatArray(0).getMimeType());
            }
            String tempPath = Paths.get(System.getProperty("java.io.tmpdir"),
                    k + this.instanceId).toString() + extension;
            SFTPClient ftpClient = ssh.newSFTPClient();
            LocalDestFile tempLocalFile = new FileSystemFile(tempPath);
            ftpClient.get(outputPath, tempLocalFile);

            File temp = new File(tempPath);
            GenericFileParser parser = new GenericFileParser();
            GenericFileDataBinding parsedOutput = parser.parse(
                    FileUtils.openInputStream(temp), DockerUtil.getOutputDesc(k,
                    description).getDataDescription().getFormatArray()[0].getMimeType(),
                    null);
            log.debug("Putting output " + k);
            resultHash.put(k, parsedOutput);
        }

        //resultHash.put(key,OutputParser.handleLiteralValue(ioElement));
        //resultHash.put(key, OutputParser.handleComplexValue(			ioElement, getDescription()));
        log.debug(
                "Returning debug");
        return resultHash;

    }

    @Override
    public Map<String, IData> invoke(ExecuteRequest request, String algorithmID,
            ProcessOfferingDocument.ProcessOffering description) throws Exception {
        ExecuteRequestType doc = ((ExecuteRequestV200) request).getExecute();
        ExecuteDocument execute = ExecuteDocument.Factory.newInstance();
        execute.setExecute(doc);
        return invoke(execute, algorithmID,
                description, request.getUniqueId());
    }

    @Override
    public boolean deployProcess(DeploymentProfile request) throws Exception {
        // get a context with docker that offers the portable ComputeService api

// release resources
        return true;
    }

    @Override
    public RemoteDockerHostBackend
            getBackendConfig() throws Exception {
        return ((RemoteDockerHostBackend) WPSConfig.getInstance().getConfigurationManager().getConfigurationServices().getConfigurationModule(
                RemoteDockerHostBackend.class
                        .getName()));

    }

    public DockerClient getDocker() {
        return docker;
    }

    /**
     * Read the execute request and process description in order to classify
     * inputs and parse the values
     *
     * @param execute
     * @param description
     * @param db
     * @throws ExceptionReport
     */
    private void handleInputs(ExecuteDocument execute,
            ProcessOfferingDocument.ProcessOffering description) throws ExceptionReport, IOException {
        HashMap<String, byte[]> files = new HashMap<String, byte[]>();
        for (DataInputType input : execute.getExecute().getInputArray()) {
            String id = input.getId();
            log.debug("handling input:" + id);
            InputDescriptionType inputDesc = DockerUtil.getInputDesc(id,
                    description);
            if (inputDesc.getDataDescription() instanceof ComplexDataType) {
                log.debug("complex data");
                ComplexDataType complexData = (ComplexDataType) inputDesc.getDataDescription();
                if (input.isSetReference()) {
                    log.debug("input by reference");
                    // reference File
                    // Check if starting with prefix
                    String ref = input.getReference().getHref();
                    if (ref.startsWith(db.getEODataConversionPrefix())) {
                        log.debug(
                                "Reference starts with the configured prefix " + db.getEODataConversionPrefix());
                        String convertedRef = ref.replaceFirst(
                                db.getEODataConversionPrefix(),
                                db.getNfsEODataPath());
                        log.debug("Converted reference to " + convertedRef);
                        // indicate the EO store must be mounted to container for execution
                        dirToMount.add(convertedRef);
                        // add location to the environmnent variables list
                        env.add("WPS_INPUT_" + id.toUpperCase() + "=" + convertedRef);
                    }
                    db.getNfsEODataPath();
                } else if (input.isSetData()) {
                    log.debug("input by data");
                    // TODO parse this
                    byte[] binary = Base64.getDecoder().decode(
                            input.getData().getDomNode().getFirstChild().getNodeValue());
                    String testString = new String(binary);
                    log.debug("decoded:" + testString);
                    log.debug("putting file " + id);
                    files.put(id, binary);
                } else {
                    if (inputDesc.getMinOccurs().intValue() >= 1) {
                        throw new ExceptionReport(
                                id + " input is missing (mandatory)",
                                ExceptionReport.INVALID_PARAMETER_VALUE);
                    }
                }
            } else if (inputDesc.getDataDescription() instanceof LiteralDataType) {
                LiteralDataType literalData = (LiteralDataType) inputDesc.getDataDescription();
                // add to the environmnent variable
                env.add("WPS_INPUT_" + id.toUpperCase() + "=" + input.getData().getDomNode().getFirstChild().getNodeValue());
            } else {
                throw new ExceptionReport(
                        id + " input is neither ComplexData nor LiteralData!",
                        ExceptionReport.INVALID_PARAMETER_VALUE);
            }
        }
        // write the input files to NFS
        log.debug("write input files to NFS store");
        writeFilesToNFS(ssh, files,
                getInputDirectory(this.getProcessID(), instanceId.toString())
        );
    }

    /**
     * Handle Outputs
     *
     * @param execute
     * @param description
     * @param db
     * @throws ExceptionReport
     * @throws IOException
     */
    private void handleOutputs(ExecuteDocument execute,
            ProcessOfferingDocument.ProcessOffering description
    ) throws ExceptionReport, IOException {
        for (OutputDefinitionType output : execute.getExecute().getOutputArray()) {
            String id = output.getId();
            log.debug("id:" + id);
            OutputDescriptionType outputDesc = DockerUtil.getOutputDesc(id,
                    description);
            log.debug("output desc " + outputDesc.getIdentifier());
            if (outputDesc.getDataDescription() instanceof ComplexDataType) {
                log.debug("getting data description");
                ComplexDataType complexData = (ComplexDataType) outputDesc.getDataDescription();
                String pathDir = FilenameUtils.separatorsToUnix(Paths.get(
                        getOutputDirectory(processID, instanceId).toString()).toString());
                String extension = "";
                if (complexData.getFormatArray(0) != null && complexData.getFormatArray(
                        0).getMimeType() != null) {
                    extension = "." + MimeUtil.getExtension(
                            complexData.getFormatArray(0).getMimeType());
                }
                String path = FilenameUtils.separatorsToUnix(Paths.get(
                        getOutputDirectory(processID, instanceId).toString(), id).toString()) + extension;
                SFTPClient ftpClient = ssh.newSFTPClient();
                ftpClient.mkdirs(pathDir);
                env.add("WPS_OUTPUT_" + id.toUpperCase() + "=" + path);
                outputs.put(id, path);

            }
        }
    }

    /**
     * Write input files into the NFS directory
     *
     * @param ssh
     * @param files
     * @param path
     * @throws IOException
     */
    private void writeFilesToNFS(SSHClient ssh, HashMap<String, byte[]> files,
            Path path) throws IOException {
        for (String i : files.keySet()) {
            log.debug("handling file store of " + i);
            byte[] fileBytes = files.get(i);
            InputDescriptionType inputDesc = DockerUtil.getInputDesc(i,
                    description);
            // TODO implement if image location is overriden in description
            // create TMP file
            File temp = File.createTempFile("wps_inp", ".tmp");
            // write bytes in temp file
            FileUtils.writeByteArrayToFile(temp, fileBytes);
            String targetDirLocation = FilenameUtils.separatorsToUnix(Paths.get(
                    getInputDirectory(processID,
                            instanceId).toString()).toString());
            String targetFileLocation = FilenameUtils.separatorsToUnix(
                    Paths.get(getInputDirectory(processID,
                            instanceId).toString(), i).toString());
            SFTPClient ftpClient = ssh.newSFTPClient();
            ftpClient.mkdirs(targetDirLocation);
            ftpClient.put(temp.getAbsolutePath(),
                    targetFileLocation);
            temp.delete();
            // add input location to env variables
            log.debug("adding " + i + " =" + targetFileLocation);
            env.add("WPS_INPUT_" + i.toUpperCase() + "=" + targetFileLocation);
        }

        // ssh.disconnect();
    }

    /**
     * Return the Working Directory Path on NFS
     *
     * @param processId
     * @param instanceId
     * @return
     */
    private Path getWorkingDirectory(String processId, String instanceId) {
        log.debug("getWorkingdirctory for " + processId + " " + instanceId);
        return Paths.get(db.getNfsWPSPath(), processId, instanceId);
    }

    private Path getInputDirectory(String processId, String instanceId) {
        return Paths.get(getWorkingDirectory(processId, instanceId).toString(),
                db.getInputDir());
    }

    private Path getOutputDirectory(String processId, String instanceId) {
        return Paths.get(getWorkingDirectory(processId, instanceId).toString(),
                db.getOutputDir());
    }

    private DockerClient getDockerConnection() throws DockerCertificateException {
        DockerClient docker = DefaultDockerClient.builder().uri(URI.create(
                db.getDockerURL())).dockerCertificates(new DockerCertificates(
                Paths.get("D:\\vm1"))).build();
        return docker;
    }

    public static void main(String[] args) {
        try {
            MetadataType appContextMetadata = MetadataType.Factory.parse(
                    new File("D:/app.txt"));
            log.debug("Parsing appContext " + appContextMetadata.toString());
            XPath xPath = XPathFactory.newInstance().newXPath();
            NamespaceContext context = DockerUtil.getTB13NamespaceContext();
            log.debug("Context:" + context.getNamespaceURI("eoc"));
            xPath.setNamespaceContext(context);
            Node node = (Node) xPath.evaluate(
                    "//eoc:ApplicationContext/descendant::owc:offering[@code='http://www.opengis.net/tb13/eoc/DockerImage']/ows:AdditionalParameters/ows:AdditionalParameter/ows:Name[text()='eoc.reference']/../ows:Value/text()",
                    appContextMetadata.copy().getDomNode(), XPathConstants.NODE);
            if (node == null) {
                log.warn("null node");
                return;
            }
            log.debug("Node content is :" + node.toString());
            log.debug("Node content is :" + node.getNodeValue());
            log.debug("Node content is :" + node.getTextContent());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String parseDockerImage() throws XPathExpressionException, XmlException {
        MetadataType appContextMetadata = DockerUtil.getMetadataContentByKey(
                "http://www.opengis.net//tb13/eoc/applicationContext",
                description);
        log.debug("Parsing appContext " + appContextMetadata.toString());
        XPath xPath = XPathFactory.newInstance().newXPath();
        NamespaceContext context = DockerUtil.getTB13NamespaceContext();
        log.debug("Context:" + context.getNamespaceURI("eoc"));
        xPath.setNamespaceContext(context);
       Node node = (Node) xPath.evaluate(
                    "//eoc:ApplicationContext/descendant::owc:offering[@code='http://www.opengis.net/tb13/eoc/DockerImage']/ows:AdditionalParameters/ows:AdditionalParameter/ows:Name[text()='eoc.reference']/../ows:Value/text()",
                    appContextMetadata.copy().getDomNode(), XPathConstants.NODE);
        log.debug("Node content is :" + node.getNodeValue());
        return node.getNodeValue();
    }

    /**
     * Write the environmnent variables property file
     *
     * @param env
     */
    private void writeEnvPropertyFile(List<String> env) throws FileNotFoundException, IOException {
        Properties prop = new Properties();
        OutputStream output = null;
        File temp = File.createTempFile("wps_env", ".tmp");
        log.debug("envDir :" + db.getEnvDir());
        String envDir = FilenameUtils.separatorsToUnix(Paths.get(
                getWorkingDirectory(this.processID, this.instanceId).toString(),
                db.getEnvDir()).toString());
        String envFile = FilenameUtils.separatorsToUnix(Paths.get(
                getWorkingDirectory(this.getProcessID(), this.instanceId).toString(),
                db.getEnvDir(), "env.properties").toString());
        output = new FileOutputStream(temp.getAbsolutePath());
        for (String e : env) {
            log.debug(e);
            String[] split = e.split("=");
            prop.setProperty(split[0], split[1]);
        }
        // Store property file
        prop.store(output, null);
        SFTPClient ftpClient = ssh.newSFTPClient();
        ftpClient.mkdirs(envDir);
        ftpClient.put(temp.getAbsolutePath(),
                envFile);
    }

}