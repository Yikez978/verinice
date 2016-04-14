/*******************************************************************************
 * Copyright (c) 2012 Sebastian Hagedorn <sh@sernet.de>.
 * This program is free software: you can redistribute it and/or 
 * modify it under the terms of the GNU General Public License 
 * as published by the Free Software Foundation, either version 3 
 * of the License, or (at your option) any later version.
 *     This program is distributed in the hope that it will be useful,    
 * but WITHOUT ANY WARRANTY; without even the implied warranty 
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
 * See the GNU General Public License for more details.
 *     You should have received a copy of the GNU General Public 
 * License along with this program. 
 * If not, see <http://www.gnu.org/licenses/>.
 * 
 * Contributors:
 *     Sebastian Hagedorn <sh@sernet.de> - initial API and implementation
 ******************************************************************************/
package sernet.gs.ui.rcp.main.bsi.dnd;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TransferData;

import sernet.gs.model.Baustein;
import sernet.gs.model.Massnahme;
import sernet.gs.ui.rcp.main.Activator;
import sernet.gs.ui.rcp.main.bsi.dialogs.SanityCheckDialog;
import sernet.gs.ui.rcp.main.bsi.dnd.transfer.BausteinElementTransfer;
import sernet.gs.ui.rcp.main.bsi.dnd.transfer.BausteinUmsetzungTransfer;
import sernet.gs.ui.rcp.main.bsi.dnd.transfer.IBSIStrukturElementTransfer;
import sernet.gs.ui.rcp.main.bsi.dnd.transfer.IGSModelElementTransfer;
import sernet.gs.ui.rcp.main.bsi.dnd.transfer.ISO27kElementTransfer;
import sernet.gs.ui.rcp.main.bsi.dnd.transfer.ISO27kGroupTransfer;
import sernet.gs.ui.rcp.main.bsi.views.BSIKatalogInvisibleRoot;
import sernet.gs.ui.rcp.main.common.model.BuildInput;
import sernet.gs.ui.rcp.main.common.model.CnAElementFactory;
import sernet.gs.ui.rcp.main.common.model.CnAElementHome;
import sernet.gs.ui.rcp.main.common.model.CnATreeElementBuildException;
import sernet.gs.ui.rcp.main.service.ServiceFactory;
import sernet.gs.ui.rcp.main.service.crudcommands.GetElementPathCommand;
import sernet.hui.common.VeriniceContext;
import sernet.springclient.RightsServiceClient;
import sernet.verinice.interfaces.ActionRightIDs;
import sernet.verinice.interfaces.CommandException;
import sernet.verinice.interfaces.IAuthService;
import sernet.verinice.interfaces.ICommandService;
import sernet.verinice.interfaces.RightEnabledUserInteraction;
import sernet.verinice.iso27k.rcp.RcpModelUpdater;
import sernet.verinice.iso27k.rcp.action.DropPerformer;
import sernet.verinice.iso27k.service.IModelUpdater;
import sernet.verinice.model.bsi.BausteinUmsetzung;
import sernet.verinice.model.bsi.IBSIStrukturElement;
import sernet.verinice.model.bsi.IBSIStrukturKategorie;
import sernet.verinice.model.bsi.IMassnahmeUmsetzung;
import sernet.verinice.model.bsi.MassnahmenUmsetzung;
import sernet.verinice.model.bsi.Raum;
import sernet.verinice.model.common.CnATreeElement;
import sernet.verinice.model.common.Permission;
import sernet.verinice.model.iso27k.IISO27kElement;
import sernet.verinice.model.iso27k.IISO27kGroup;
import sernet.verinice.service.commands.SaveElement;
import sernet.verinice.service.gstoolimport.MassnahmenFactory;

/**
 *
 */
public class BSIModelViewDropListener extends ViewerDropAdapter implements RightEnabledUserInteraction, DropPerformer {

    private TreeViewer viewer;

    private static final Logger LOG = Logger.getLogger(BSIModelViewDropListener.class);

    private boolean isActive = false;

    private Object target = null;

    private IAuthService authService;
    private ICommandService commandService;
    private IModelUpdater modelUpdater;

    public BSIModelViewDropListener(TreeViewer viewer) {
        super(viewer);
        this.viewer = viewer;
        modelUpdater = new RcpModelUpdater();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.jface.viewers.ViewerDropAdapter#performDrop(java.lang.Object)
     */
    @Override
    public boolean performDrop(Object data) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("performDrop, data: " + data);
        }
        Object toDrop = data;
        Object selectedData = null;
        if (data == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("data is null - setting to selected Object");
            }

            selectedData = ((IStructuredSelection) this.getViewer().getSelection()).toArray();
        } else {
            selectedData = data;
        }
        List items = DNDHelper.arrayToList(selectedData);
        Object firstOne = items.get(0);
        if (toDrop != null && (toDrop instanceof Object[])) {
            Object[] o = (Object[]) toDrop;
            if (o.length > 0) {
                firstOne = o[0];
            }
        } else if (toDrop != null && (toDrop instanceof Object)) {
            firstOne = toDrop;
        }

        if (isActive()) {
            if (firstOne instanceof Baustein && target.getClass().getPackage().getName().contains("model.bsi")) {
                ArrayList<Baustein> list = new ArrayList<Baustein>(0);
                for (Object object : items) {
                    if (object instanceof Baustein) {
                        list.add((Baustein) object);
                    }
                }
                return dropBaustein((CnATreeElement) target, viewer, list.toArray(new Baustein[list.size()]));
            } else if (firstOne instanceof Massnahme && target instanceof BausteinUmsetzung) {
                ArrayList<Massnahme> list = new ArrayList<Massnahme>(0);
                for (Object object : items) {
                    if (object instanceof Massnahme) {
                        list.add((Massnahme) object);
                    }
                }
                return dropControls((BausteinUmsetzung) target, viewer, list.toArray(new Massnahme[list.size()]));
            } else if (firstOne != null && (firstOne instanceof IBSIStrukturElement || firstOne instanceof BausteinUmsetzung || firstOne instanceof IISO27kElement || firstOne instanceof IMassnahmeUmsetzung)) {
                CnATreeElement element = (CnATreeElement) target;
                LinkDropper dropper = new LinkDropper();
                ArrayList<CnATreeElement> list = new ArrayList<CnATreeElement>();
                for (Object object : items) {
                    if (object instanceof CnATreeElement) {
                        list.add((CnATreeElement) object);
                    }
                }
                return dropper.dropLink(list, element);
            }
        }
        return false;
    }

    @Override
    public void drop(DropTargetEvent event) {
        LOG.debug("entered drop(DropTargetEvent event)");
        target = determineTarget(event);
        super.drop(event);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.viewers.ViewerDropAdapter#validateDrop(java.lang.
     * Object, int, org.eclipse.swt.dnd.TransferData)
     */
    @Override
    public boolean validateDrop(Object target, int operation, TransferData transferType) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("validateDrop, target: " + target != null ? target.toString() : "no target set" + ", transfer type class: " + transferType.getClass().getName() + ", transfer-type-id: " + transferType.type);
        }
        if (!checkRights()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("ChechRights() failed  ... return false");
            }
            return false;
        }
        if (target == null){
            if (LOG.isDebugEnabled()) {
                LOG.debug("Target is null ... return false");
            }
            isActive=false;
            return isActive;
        } else {
            this.target = target;
        }
        if (!(target instanceof CnATreeElement)){
            if (LOG.isDebugEnabled()) {
                LOG.debug("Target is no CnATreeElement ... return false");
            }
            isActive=false;
            return isActive;
        }
        if (target instanceof IBSIStrukturKategorie){
            if (LOG.isDebugEnabled()) {
                LOG.debug("Target is BSI category ... return false");
            }
            isActive=false;
            return isActive;
        }
        if (target instanceof BausteinUmsetzung && !(IBSIStrukturElementTransfer.getInstance().isSupportedType(transferType))) {
            if (IGSModelElementTransfer.getInstance().isSupportedType(transferType) && ((BausteinUmsetzung) target).isOwnModule()) {
                isActive = true;
            } else {
                isActive = false;
                if (LOG.isDebugEnabled()) {
                    LOG.debug("target is BausteinUmsetzung,  IBSIStrukturElementTransfer.getInstance().isSupportedType(..) return false");
                }
            }
            return isActive;
        } else if (target instanceof IBSIStrukturElement && isSupportedData(transferType)) {
            isActive = true;
            if (LOG.isDebugEnabled()) {
                LOG.debug("target is IBSIStrukturElement,  isSupportedData return false");
            }
            return isActive;
        }
        if (target instanceof IISO27kGroup && BausteinElementTransfer.getInstance().isSupportedType(transferType)) {
            isActive = false;
            if (LOG.isDebugEnabled()) {
                LOG.debug("target is IISO27kGroup,  IISO27kGroup return true");
            }
            return isActive;
        }
        if (IGSModelElementTransfer.getInstance().isSupportedType(transferType)) {
            isActive = false;
            if (LOG.isDebugEnabled()) {
                LOG.debug("IGSModelElementTransfer.getInstance().isSupportedType(..) return true");
            }
            return isActive;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("return true");
        }
        isActive = true;
        return isActive;
    }

    /*
     * (non-Javadoc)
     * 
     * @see sernet.verinice.interfaces.RightEnabledUserInteraction#checkRights()
     */
    @Override
    public boolean checkRights() {
        RightsServiceClient service = (RightsServiceClient) VeriniceContext.get(VeriniceContext.RIGHTS_SERVICE);
        return service.isEnabled(getRightID());
    }

    /*
     * (non-Javadoc)
     * 
     * @see sernet.verinice.interfaces.RightEnabledUserInteraction#getRightID()
     */
    @Override
    public String getRightID() {
        return ActionRightIDs.TREEDND;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * sernet.verinice.interfaces.RightEnabledUserInteraction#setRightID(java.
     * lang.String)
     */
    @Override
    public void setRightID(String rightID) {
        // nothing
    }

    /*
     * (non-Javadoc)
     * 
     * @see sernet.verinice.iso27k.rcp.action.DropPerformer#isActive()
     */
    @Override
    public boolean isActive() {
        return isActive;
    }

    private boolean dropBaustein(final CnATreeElement target, Viewer viewer, final Baustein[] bausteine) {
        if (!CnAElementHome.getInstance().isNewChildAllowed(target)) {
            return false;
        }
        Check: for (Baustein baustein : bausteine) {
            int targetSchicht = 0;
            if (target instanceof IBSIStrukturElement) {
                targetSchicht = ((IBSIStrukturElement) target).getSchicht();
            }
            if (baustein.getSchicht() != targetSchicht) {
                if (!SanityCheckDialog.checkLayer(viewer.getControl().getShell(), baustein.getSchicht(), targetSchicht)) {
                    return false;
                } else {
                    break Check; // user say he knows what he's doing, stop
                }
                // checking.
            }

        }

        try {
            Job dropJob = new Job(Messages.getString("BSIModelViewDropListener.3")) { //$NON-NLS-1$
                @Override
                protected IStatus run(IProgressMonitor monitor) {
                    Activator.inheritVeriniceContextState();

                    try {
                        createBausteinUmsetzung(bausteine, target);
                    } catch (Exception e) {
                        Logger.getLogger(this.getClass()).error("Drop failed", e); //$NON-NLS-1$
                        return Status.CANCEL_STATUS;
                    }
                    return Status.OK_STATUS;
                }
            };
            dropJob.setUser(true);
            dropJob.setSystem(false);
            dropJob.schedule();
        } catch (Exception e) {
            LOG.error(Messages.getString("BSIModelViewDropListener.5"), e); //$NON-NLS-1$
            return false;
        }
        return true;
    }

    private void createBausteinUmsetzung(Baustein[] toDrop, CnATreeElement target) throws CnATreeElementBuildException, CommandException {
        CnATreeElement saveNew = null;
        for (Baustein baustein : toDrop) {
            saveNew = CnAElementFactory.getInstance().saveNew(target, BausteinUmsetzung.TYPE_ID, new BuildInput<Baustein>(baustein),
                    false /* do not notify single elements */,
                    false /* do not inherit icon */);
        }
        // notifying for the last element is sufficient to update all views:
        CnAElementFactory.getLoadedModel().childAdded(target, saveNew);
    }

    private boolean dropControls(final BausteinUmsetzung target, Viewer viewer, final Massnahme[] massnahmen) {
        if (!CnAElementHome.getInstance().isNewChildAllowed(target)) {
            return false;
        }

        try {
            Job dropJob = new Job(Messages.getString("BSIModelViewDropListener.3")) { //$NON-NLS-1$
                @Override
                protected IStatus run(IProgressMonitor monitor) {
                    Activator.inheritVeriniceContextState();

                    try {
                        addControlsToModule(massnahmen, target);
                    } catch (Exception e) {
                        Logger.getLogger(this.getClass()).error("Drop failed", e); //$NON-NLS-1$
                        return Status.CANCEL_STATUS;
                    }
                    return Status.OK_STATUS;
                }
            };
            dropJob.setUser(true);
            dropJob.setSystem(false);
            dropJob.schedule();
        } catch (Exception e) {
            LOG.error(Messages.getString("BSIModelViewDropListener.5"), e); //$NON-NLS-1$
            return false;
        }
        return true;
    }

    private void addControlsToModule(Massnahme[] controlsToDrop, BausteinUmsetzung target) throws CnATreeElementBuildException, CommandException {
        for (Massnahme controlToDrop : controlsToDrop) {
            if (target.containsControl(controlToDrop.getId())) {
                GetElementPathCommand pathLoader = new GetElementPathCommand(target.getUuid(), target.getTypeId());
                String elementPath = ServiceFactory.lookupCommandService().executeCommand(pathLoader).getResult();
                LOG.error("ElementContainer:\t" + elementPath + "(" + target.getDbId() + ")" + "\twith TypeId:\t" + target.getTypeId() + " contains already a massnahmen with id:\t" + controlToDrop.getId() + "\t" + controlToDrop.getTitel() + " is skipped because of this");
                continue;
            }
            addControlToModule(controlToDrop, target);
        }
    }

    private CnATreeElement addControlToModule(Massnahme controlToDrop, BausteinUmsetzung target) {
        CnATreeElement control = null;
        MassnahmenFactory mFactory = new MassnahmenFactory();
        control = mFactory.createMassnahmenUmsetzung(target, controlToDrop, BSIKatalogInvisibleRoot.getInstance().getLanguage());
        control.setParentAndScope(target);
        SaveElement<CnATreeElement> command = null;
        try {
            HashSet<Permission> newperms = new HashSet<Permission>();
            newperms.add(Permission.createPermission(control, getAuthService().getUsername(), true, true));
            control.setPermissions(newperms);
            command = new SaveElement<CnATreeElement>(control);
            command = getCommandService().executeCommand(command);
            control = command.getElement();
            control.setParentAndScope(target);
            target.addChild(control);
        } catch (CommandException e) {
            LOG.error("Error while inserting control", e); //$NON-NLS-1$
            throw new RuntimeException("Error while inserting control", e); //$NON-NLS-1$
        }
        // notifying for the last element is sufficient to update all views:
        modelUpdater.childAdded(target, control);
        return control;
    }

    @Override
    public void dropAccept(DropTargetEvent event) {
    }

    private boolean isSupportedData(TransferData transferType) {
        boolean retVal = IGSModelElementTransfer.getInstance().isSupportedType(transferType) || IBSIStrukturElementTransfer.getInstance().isSupportedType(transferType) || BausteinUmsetzungTransfer.getInstance().isSupportedType(transferType);
        retVal = retVal || ISO27kElementTransfer.getInstance().isSupportedType(transferType) || ISO27kGroupTransfer.getInstance().isSupportedType(transferType);
        return retVal;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * sernet.verinice.iso27k.rcp.action.DropPerformer#performDrop(java.lang.
     * Object, java.lang.Object, org.eclipse.jface.viewers.Viewer)
     */
    @Override
    public boolean performDrop(Object data, Object target, Viewer viewer) {
        return performDrop(data);
    }

    private IAuthService getAuthService() {
        if (authService == null) {
            authService = createAuthService();
        }
        return authService;
    }

    private IAuthService createAuthService() {
        return ServiceFactory.lookupAuthService();
    }

    private ICommandService getCommandService() {
        if (commandService == null) {
            commandService = createCommandServive();
        }
        return commandService;
    }

    private ICommandService createCommandServive() {
        return ServiceFactory.lookupCommandService();
    }
}
