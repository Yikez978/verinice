/*******************************************************************************
 * Copyright (c) 2014 Benjamin Weißenfels.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 *
 * Contributors:
 *     Benjamin Weißenfels <bw[at]sernet[dot]de> - initial API and implementation
 ******************************************************************************/
package sernet.verinice.rcp;

import static org.apache.commons.io.FilenameUtils.concat;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import sernet.gs.service.ReportTemplateUtil;
import sernet.gs.ui.rcp.main.Activator;
import sernet.gs.ui.rcp.main.CnAWorkspace;
import sernet.gs.ui.rcp.main.common.model.CnAElementFactory;
import sernet.gs.ui.rcp.main.common.model.IModelLoadListener;
import sernet.gs.ui.rcp.main.preferences.PreferenceConstants;
import sernet.gs.ui.rcp.main.service.ServiceFactory;
import sernet.verinice.interfaces.IReportDepositService;
import sernet.verinice.iso27k.rcp.JobScheduler;
import sernet.verinice.model.bsi.BSIModel;
import sernet.verinice.model.iso27k.ISO27KModel;
import sernet.verinice.model.report.PropertyFileExistsException;
import sernet.verinice.model.report.ReportMetaDataException;
import sernet.verinice.model.report.ReportTemplate;
import sernet.verinice.model.report.ReportTemplateMetaData;

/**
 * @author Benjamin Weißenfels <bw[at]sernet[dot]de>
 *
 */
public class ReportTemplateSync extends WorkspaceJob implements IModelLoadListener {

    private ReportTemplateUtil clientServerReportTemplateUtil = new ReportTemplateUtil(CnAWorkspace.getInstance().getRemoteReportTemplateDir(), true);

    private static volatile IModelLoadListener modelLoadListener;

    private Logger LOG = Logger.getLogger(ReportTemplateSync.class);

    private ReportTemplateSync() {
        super("sync reports");
    }

    public static void sync() {
        if (CnAElementFactory.isModelLoaded()) {
            startSync();
        } else if (modelLoadListener == null) {
            CnAElementFactory.getInstance().addLoadListener(new ReportTemplateSync());
        }
    }

    private static void startSync() {

        Activator.inheritVeriniceContextState();

        WorkspaceJob syncReportsJob = new ReportTemplateSync();
        JobScheduler.scheduleInitJob(syncReportsJob);
    }

    private void syncReportTemplates(String locale) throws IOException, ReportMetaDataException, PropertyFileExistsException {

        String[] fileNames = clientServerReportTemplateUtil.getReportTemplateFileNames();
        Set<ReportTemplateMetaData> localServerTemplates = clientServerReportTemplateUtil.getReportTemplates(fileNames, locale);
        Set<ReportTemplateMetaData> remoteServerTemplates = getIReportDepositService().getServerReportTemplates(locale);
        if(LOG.isDebugEnabled()){
            LOG.debug("Found\t" + localServerTemplates.size() + "\tTemplates in local repo (" + CnAWorkspace.getInstance().getRemoteReportTemplateDir() + ") (server mirror) before the sync");
            LOG.debug("Found\t" + remoteServerTemplates.size() + "\tTemplates in server repo, which need to be synced");
            LOG.debug("Syncing will take place with following locale:\t" + locale);
        }
        addReports(locale, localServerTemplates, remoteServerTemplates);
        deleteReports(locale, remoteServerTemplates);
    }

    private void addReports(String locale, Set<ReportTemplateMetaData> localServerTemplates, Set<ReportTemplateMetaData> remoteSeverTemplates) throws IOException, ReportMetaDataException, PropertyFileExistsException {
        int i = 0;
        for (ReportTemplateMetaData remoteTemplateMetaData : remoteSeverTemplates) {
            if(LOG.isDebugEnabled()){
                LOG.debug("Syncing:\t" + remoteTemplateMetaData.getFilename() + "\t(" + String.valueOf(i) + ")");
            }
            if (!localServerTemplates.contains(remoteTemplateMetaData)) {
                syncTemplate(remoteTemplateMetaData, locale);
            } else if(LOG.isDebugEnabled()){
                LOG.debug("Template\t" + remoteTemplateMetaData.getOutputname() + "\twill not be synced, since it's already existant on client");
            }
            i++;
        }
    }

    private void deleteReports(String locale, Set<ReportTemplateMetaData> remoteSeverTemplates) throws IOException, ReportMetaDataException, PropertyFileExistsException {
        if (isNotStandalone()) {
            for (ReportTemplateMetaData localTemplateMetaData : clientServerReportTemplateUtil.getReportTemplates(locale)) {
                if (!remoteSeverTemplates.contains(localTemplateMetaData)) {
                    deleteRptdesignAndPropertiesFiles(localTemplateMetaData.getFilename());
                }
            }
        }
    }

    private boolean isNotStandalone() {
        return Activator.getDefault().getPreferenceStore().getString(PreferenceConstants.OPERATION_MODE).equals(PreferenceConstants.OPERATION_MODE_REMOTE_SERVER);
    }

    private IReportDepositService getIReportDepositService() {
        return ServiceFactory.lookupReportDepositService();
    }

    private void syncTemplate(ReportTemplateMetaData metadata, String locale) throws IOException, ReportMetaDataException, PropertyFileExistsException {

        if(LOG.isDebugEnabled()){
            LOG.debug("Syncing:\t" + metadata.getOutputname());
        }
        
        deleteRptdesignAndPropertiesFiles(metadata.getFilename());

        ReportTemplate template = getIReportDepositService().getReportTemplate(metadata, locale);
        String directory = CnAWorkspace.getInstance().getRemoteReportTemplateDir();
        File rptdesignTemplate = new File(concat(directory, template.getMetaData().getFilename()));
        FileUtils.writeByteArrayToFile(rptdesignTemplate, template.getRptdesignFile());
        if(LOG.isDebugEnabled()){
            LOG.debug("Template:\t" + metadata.getFilename() + " written to:\t" + rptdesignTemplate.getAbsolutePath());
        }
        

        for (Entry<String, byte[]> e : template.getPropertiesFiles().entrySet()) {
            FileUtils.writeByteArrayToFile(new File(concat(directory, e.getKey())), e.getValue());
            if(LOG.isDebugEnabled()){
                LOG.debug("Propertyfile:\t" + concat(directory, e.getKey()) + "\twritten");
            }
        }
    }

    private void deleteRptdesignAndPropertiesFiles(String fileName) {

        String filePath = CnAWorkspace.getInstance().getRemoteReportTemplateDir();
        if(!filePath.endsWith(String.valueOf(File.separatorChar))){
            filePath = filePath + File.separatorChar;
        }
        filePath = filePath + fileName;
        File rptdesign = new File(filePath);

        if (rptdesign.exists()) {
            rptdesign.delete();
            if(LOG.isDebugEnabled()){
                LOG.debug("TemplateFile:\t" + rptdesign.getAbsolutePath() + "\tdeleted");
            }
        }

        // delete properties files
        Iterator<File> iter = clientServerReportTemplateUtil.listPropertiesFiles(fileName);
        while (iter.hasNext()) {
            File f = iter.next();
            String path = f.getAbsolutePath();
            f.delete();
            if(LOG.isDebugEnabled()){
                LOG.debug("PropertyFile:\t" + path + "\tdeleted");
            }
        }
    }

    @Override
    public IStatus runInWorkspace(IProgressMonitor arg0) throws CoreException {

        IStatus status = Status.OK_STATUS;

        try {
            Activator.inheritVeriniceContextState();
            String locale = Locale.getDefault().toString(); 
            if(locale.length() > 2 && locale.contains(String.valueOf('_'))){
                // as we do not deal with dialects like en_UK here, we just take the leftside locale (e.g. "en")
                locale = locale.substring(0, locale.indexOf(String.valueOf('_')));
            }
            if ("en".equals(locale.toLowerCase())) {
                locale = "";
            } else {
                locale = locale.toLowerCase();
            }
            syncReportTemplates(locale);
        } catch (IOException e) {
            status = errorHandler(e);
        } catch (ReportMetaDataException e) {
            status = errorHandler(e);
            e.printStackTrace();
        } catch (PropertyFileExistsException e) {
            status = errorHandler(e);
        }

        return status;
    }

    private IStatus errorHandler(Exception e) {
        IStatus status;
        String msg = "error while syncing report templates:\t" + e.getMessage();
        LOG.error(msg, e);
        status = new Status(Status.ERROR, "sernet.gs.ui.rcp.main", msg);
        return status;
    }

    @Override
    public void loaded(BSIModel model) {
    }

    @Override
    public void loaded(ISO27KModel model) {
        sync();
    }

    @Override
    public void closed(BSIModel model) {
    }

}