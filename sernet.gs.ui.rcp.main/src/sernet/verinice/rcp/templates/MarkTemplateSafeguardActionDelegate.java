/******************************************************************************* 
 * Copyright (c) 2016 Viktor Schmidt. 
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
 *     Viktor Schmidt <vschmidt[at]ckc[dot]de> - initial API and implementation 
 ******************************************************************************/
package sernet.verinice.rcp.templates;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPart;

import sernet.gs.ui.rcp.main.ExceptionUtil;
import sernet.gs.ui.rcp.main.bsi.actions.Messages;
import sernet.gs.ui.rcp.main.common.model.CnAElementFactory;
import sernet.gs.ui.rcp.main.common.model.CnAElementHome;
import sernet.verinice.interfaces.CommandException;
import sernet.verinice.model.bsi.MassnahmenUmsetzung;
import sernet.verinice.model.common.CnATreeElement;
import sernet.verinice.model.common.CnATreeElement.TemplateType;

/**
 * This action mark the selected safequard {@link MassnahmenUmsetzung} as
 * central ({@link TemplateType#TEMPLATE}).
 * 
 * @author Viktor Schmidt <vschmidt[at]ckc[dot]de>
 * @see MarkTemplateActionDelegate
 */
public class MarkTemplateSafeguardActionDelegate extends MarkTemplateActionDelegate {
    private IWorkbenchPart targetPart;

    @Override
    public void setActivePart(IAction action, IWorkbenchPart targetPart) {
        this.targetPart = targetPart;
    }

    @Override
    public void run(IAction action) {
        try {
            markSelectionsAsTemplateSafeguardsAndUpdate();
            markParentAsTemplateModuleAndUpdate();
        } catch (Exception e) {
            ExceptionUtil.log(e, Messages.MarkTemplateSafeguardActionDelegate_0);
        }
    }

    private void markSelectionsAsTemplateSafeguardsAndUpdate() throws CommandException {
        Object[] selections = ((IStructuredSelection) targetPart.getSite().getSelectionProvider().getSelection()).toArray();
        for (Object selection : selections) {
            if (selection instanceof MassnahmenUmsetzung) {
                MassnahmenUmsetzung safeguard = (MassnahmenUmsetzung) selection;
                safeguard.setTemplateTypeValue(CnATreeElement.TemplateType.TEMPLATE.name());
                CnAElementHome.getInstance().update(safeguard);

                // notify all listeners:
                CnAElementFactory.getModel(safeguard.getParent()).childChanged(safeguard);
                CnAElementFactory.getModel(safeguard.getParent()).databaseChildChanged(safeguard);
            }
        }
    }

    private void markParentAsTemplateModuleAndUpdate() throws CommandException {
        Object firstElement = ((IStructuredSelection) targetPart.getSite().getSelectionProvider().getSelection()).getFirstElement();
        if (firstElement instanceof MassnahmenUmsetzung) {
            CnATreeElement parent = ((MassnahmenUmsetzung) firstElement).getParent();
            if (!parent.isImplementation()) {
                parent.setTemplateTypeValue(CnATreeElement.TemplateType.TEMPLATE.name());
                CnAElementHome.getInstance().update(parent);
                // notify all listeners:
                CnAElementFactory.getModel(parent.getParent()).childChanged(parent);
                CnAElementFactory.getModel(parent.getParent()).databaseChildChanged(parent);
            }
        }
    }
}