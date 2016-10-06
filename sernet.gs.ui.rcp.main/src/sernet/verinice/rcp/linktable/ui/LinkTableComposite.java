/*******************************************************************************
 * Copyright (c) 2016 Ruth Motza.
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
 *     Ruth Motza <rm[at]sernet[dot]de> - initial API and implementation
 ******************************************************************************/
package sernet.verinice.rcp.linktable.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.elasticsearch.common.collect.Sets;

import sernet.verinice.rcp.linktable.ui.multiselectiondialog.LinkTableMultiSelectionControl;
import sernet.verinice.service.linktable.ColumnPathParser;
import sernet.verinice.service.linktable.vlt.VeriniceLinkTable;
import sernet.verinice.service.model.IObjectModelService;

/**
 * Composite to edit or create instances of {@link VeriniceLinkTable} (VLT).
 * For each column path in the VLT an instance of class {@link LinkTableColumn}
 * is created.
 * 
 * @author Ruth Motza <rm[at]sernet[dot]de>
 */
public class LinkTableComposite extends Composite {

    private static final Logger LOG = Logger.getLogger(LinkTableComposite.class);

    private static final Point DEFAULT_MARGIN = new Point(10, 10);
    private static final Point DEFAULT_MARGIN_CONTENT = new Point(10, 10);

    private VeriniceLinkTable veriniceLinkTable = null;
    
    private ArrayList<LinkTableColumn> columns = new ArrayList<>(); 
    private Composite columnsContainer;
    private Composite mainBody;
    private Composite subBody;
    private ScrolledComposite scrolledBody;
    private Composite buttons;
    private LinkTableMultiSelectionControl multiControl;
   
    private IObjectModelService objectModelService;
    
    private List<LinkTableFieldListener> listeners = new ArrayList<>();
    private boolean fireUpdate = false;
    private boolean fireValidation = false;

    private int numCols = 0;
    private boolean useAllScopes = true;

    public LinkTableComposite(VeriniceLinkTable vltContent,
            IObjectModelService objectModelService,
            Composite parent) {

        super(parent, SWT.NONE);
        this.objectModelService = objectModelService;
        this.veriniceLinkTable = vltContent;
        useAllScopes = vltContent.useAllScopes();
        createContent();
    }

    private void createContent() {
        Composite rootContainer = new Composite(this, SWT.BORDER);

        setHead(rootContainer);

        setBody(rootContainer);

        refresh(UpdateLinkTable.COLUMN_PATHS_CONTENT);

        rootContainer.setLayoutData(new GridData(GridData.FILL_BOTH));
        getDefaultLayoutFactory().generateLayout(rootContainer);
    }

    private GridLayoutFactory getDefaultLayoutFactory() {
        return GridLayoutFactory.fillDefaults();
    }

    private void setHead(Composite parent) {
        Composite head = new Composite(parent, SWT.NONE);

        Composite scopeButtons = new Composite(head, getStyle());
        final Button[] scopeRadios = new Button[2];
        scopeRadios[0] = new Button(scopeButtons, SWT.RADIO);
        final Button useAllScopesButton = scopeRadios[0];
        scopeRadios[0].setText(Messages.VeriniceLinkTableComposite_0);
        scopeRadios[1] = new Button(scopeButtons, SWT.RADIO);

        final Button useSelectedScopes = scopeRadios[1];
        useSelectedScopes.setText(Messages.VeriniceLinkTableComposite_1);

        SelectionAdapter listener = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                Button selected = (Button) event.widget;
                if (LOG.isDebugEnabled()) {
                    LOG.debug(selected.getText() + " is selected");
                }

                useAllScopes = selected == useAllScopesButton;
                updateAndValidateVeriniceContent(UpdateLinkTable.USE_ALL_SCOPES);

            }
        };

        useAllScopesButton.addSelectionListener(listener);
        useSelectedScopes.addSelectionListener(listener);
        useAllScopesButton.setSelection(useAllScopes);
        useSelectedScopes.setSelection(!useAllScopes);

        getDefaultLayoutFactory().margins(DEFAULT_MARGIN).numColumns(1)
                .generateLayout(scopeButtons);
        Composite multiControlContainer = new Composite(head, getStyle());
        multiControl = new LinkTableMultiSelectionControl(multiControlContainer,
                this);

        getDefaultLayoutFactory().numColumns(2).generateLayout(multiControlContainer);
        GridData multiControlContainerData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
        multiControlContainer.setLayoutData(multiControlContainerData);
        getDefaultLayoutFactory().numColumns(2).margins(DEFAULT_MARGIN).generateLayout(head);

    }

    /**
     * To ensure functionality of the scrolledComposite the body of the
     * LinkTableComposite must be wrapped twice.
     */
    private void setBody(Composite parent) {
        scrolledBody = new ScrolledComposite(parent, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);

        mainBody = new Composite(scrolledBody, getStyle());
        scrolledBody.setContent(mainBody);
        scrolledBody.setExpandHorizontal(true);
        scrolledBody.setExpandVertical(true);
        scrolledBody.setLayoutData(new GridData(GridData.FILL_BOTH));

        subBody = new Composite(mainBody, getStyle());

        columnsContainer = new Composite(subBody, getStyle());
        getDefaultLayoutFactory().generateLayout(columnsContainer);
        addButtons(subBody);
        if (veriniceLinkTable.getColumnPaths() != null && !veriniceLinkTable.getColumnPaths().isEmpty()) {
            addColumnsWithContent();
        } else {
            addColumn(null);
        }
        
        getDefaultLayoutFactory().margins(DEFAULT_MARGIN_CONTENT).generateLayout(subBody);
        getDefaultLayoutFactory().margins(0, 0).generateLayout(mainBody);
        getDefaultLayoutFactory().margins(DEFAULT_MARGIN).generateLayout(scrolledBody);
    }

    private void addColumnsWithContent() {
        for (String column : veriniceLinkTable.getColumnPaths()) {
            List<String> path = ColumnPathParser.getColumnPathAsList(column, true);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Element " + path);
            }
            addColumn(path);
        }
    }

    private void addColumn(List<String> path) {
        LinkTableColumn column;
        boolean isNewColumn = true;
        if (path == null) {
            column = new LinkTableColumn(this, getStyle(), ++numCols);
        } else {
            if (columns.isEmpty()) {
                column = new LinkTableColumn(path, this, ++numCols);
            } else {
                ISelection selection = columns.get(0).getFirstCombo().getSelection();
                column = new LinkTableColumn(selection, path, this, ++numCols);
            }
            isNewColumn = false;

        }
        columns.add(column);
        addDeleteButtonListener(column);
        handleMoreThanOneColumn(isNewColumn);
    }

    private void handleMoreThanOneColumn(boolean isNewColumn) {
        boolean oneClumn = columns.size() <= 1;
        LinkTableColumn firstColumn = columns.get(0);
        firstColumn.getDeleteButton().setEnabled(!oneClumn);
        firstColumn.getFirstCombo().getCombo().setEnabled(oneClumn);
        if (!oneClumn) {
            LinkTableColumn lastColumn = columns.get(columns.size() - 1);
            lastColumn.getFirstCombo().getCombo().setEnabled(false);
            if (isNewColumn) {
                lastColumn.getFirstCombo().getCombo()
                        .select(firstColumn.getFirstCombo().getCombo().getSelectionIndex());
                lastColumn.getFirstCombo().selectionChanged(null);
            }
        } else {
            if (isNewColumn) {
                firstColumn.getFirstCombo().getCombo().select(0);
                firstColumn.getFirstCombo().selectionChanged(null);
            }
        }
    }

    private void addDeleteButtonListener(final LinkTableColumn column) {
        column.getDeleteButton().addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                boolean delete = columns.remove(column);
                handleMoreThanOneColumn(false);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Deleted " + delete);
                }
                column.getColumn().dispose();
                numCols = columns.size();
                renameColumns();
                refresh(UpdateLinkTable.COLUMN_PATHS_MOVE_CREATE_OR_DELETE);
            }
        });
    }

    protected void renameColumns() {
        int i = 1;
        for (LinkTableColumn column : columns) {
            column.setColumnNumber(i++);
        }
    }

    public void refresh(UpdateLinkTable... updateVeriniceLinkTable) {
        List<UpdateLinkTable> options = Arrays.asList(updateVeriniceLinkTable);
        columnsContainer.pack(true);
        subBody.pack(true);
        mainBody.pack(true);
        scrolledBody.setMinSize(subBody.getClientArea().width, subBody.getClientArea().height);

        mainBody.layout(true);
        subBody.layout(true);
        columnsContainer.layout(true);

        if (!options.contains(UpdateLinkTable.COLUMN_PATHS_MOVE_CREATE_OR_DELETE)) {
            scrolledBody.showControl(buttons);
        }

        updateAndValidateVeriniceContent(updateVeriniceLinkTable);
        enableShortcuts();
    }

    private void enableShortcuts() {
        columnsContainer.setFocus();
    }

    public void updateAndValidateVeriniceContent(UpdateLinkTable... updateVeriniceLinkTable) {
        Set<UpdateLinkTable> set = Sets.newHashSet(updateVeriniceLinkTable);
        if (veriniceLinkTable == null) {
            veriniceLinkTable = new VeriniceLinkTable.Builder().build();
        }
        fireUpdate = fireValidation = false;
        if (set.contains(UpdateLinkTable.USE_ALL_SCOPES)) {
            updateUseAllScopes();
        }
        if (set.contains(UpdateLinkTable.RELATION_IDS)) {
            updateRelationIds();
        }
        if (set.contains(UpdateLinkTable.COLUMN_PATHS_CONTENT)) {
            updateColumnPaths();
        }
        if (fireUpdate) {
            fireFieldChangedEvent();
        }
        if (fireValidation) {
            fireValidationEvent();
        }
    }

    private void updateColumnPaths() {
        ArrayList<String> columnPaths = new ArrayList<>(columns.size());
        String path;
        for (LinkTableColumn column : columns) {

            path = column.getColumnPath();
            columnPaths.add(path);
        }
        if (!veriniceLinkTable.getColumnPaths().equals(columnPaths)) {
            veriniceLinkTable.setColumnPaths(columnPaths);
            fireUpdate = true;
            fireValidation = true;
        }
    }

    private void updateUseAllScopes() {
        if (veriniceLinkTable.useAllScopes() != useAllScopes) {
            veriniceLinkTable.setAllScopes(useAllScopes);
            if (useAllScopes) {
                veriniceLinkTable.getScopeIds().clear();
            }
            fireUpdate = true;
        }
    }

    private void updateRelationIds() {
        if (multiControl != null) {
            ArrayList<String> relationIds = new ArrayList<>(multiControl.getSelectedRelationIDs());
            if (!veriniceLinkTable.getRelationIds().equals(relationIds)) {
                veriniceLinkTable.setRelationIds(relationIds);
                fireUpdate = true;
            }
        }
    }

    private void addButtons(Composite parent) {
        buttons = new Composite(parent, getStyle());
        GridDataFactory.fillDefaults().applyTo(buttons);

        Button addEmptyColumn = new Button(buttons, SWT.PUSH);
        addEmptyColumn.setText(Messages.VeriniceLinkTableComposite_2);

        addEmptyColumn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                addColumn(null);
                refresh(UpdateLinkTable.COLUMN_PATHS_CONTENT);
            }
        });

        Button cloneColumn = new Button(buttons, SWT.PUSH);
        cloneColumn.setText(Messages.VeriniceLinkTableComposite_3);

        cloneColumn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                LinkTableColumn lastColumn = columns.get(columns.size() - 1);
                LinkTableColumn duplicatedColumn = new LinkTableColumn(
                        lastColumn, ++numCols);
                columns.add(duplicatedColumn);
                addDeleteButtonListener(duplicatedColumn);
                handleMoreThanOneColumn(false);
                refresh(UpdateLinkTable.COLUMN_PATHS_CONTENT);
            }
        });
        getDefaultLayoutFactory().margins(DEFAULT_MARGIN).numColumns(2)
                .generateLayout(buttons);
    }

    public Composite getColumnsContainer() {
        return columnsContainer;
    }

    public void showComposite(Control composite) {
        scrolledBody.showControl(composite);
    }

    public VeriniceLinkTable getVeriniceLinkTable() {
        return veriniceLinkTable;
    }

    public Set<String> getAllUsedRelationIds() {
        HashSet<String> relationIDs = new HashSet<>();
        for (LinkTableColumn column : columns) {
            relationIDs.addAll(column.getFirstCombo().getAllUsedRelationIds());
        }
        return relationIDs;
    }

    public IObjectModelService getContentService() {
        return objectModelService;
    }

    public void addListener(LinkTableFieldListener l) {
        if (l != null)
            listeners.add(l);
    }

    private void fireFieldChangedEvent() {
        for (LinkTableFieldListener l : listeners) {
            l.fieldValueChanged();
        }
        fireUpdate = false;
    }

    public void fireValidationEvent() {
        for (LinkTableFieldListener l : listeners) {
            l.validate();
        }
        fireValidation = false;
    }

    public void moveColumnUp(LinkTableColumn column) {
        int index = columns.indexOf(column);

        if (index < 0) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Column " + column + " not found");
            }
            return;
        }
        if (index == 0) {

            if (LOG.isDebugEnabled()) {
                LOG.debug("Column " + column + " is first column, not possible to move up");
            }
            return;
        }
        LinkTableColumn prevElement = columns.get(index - 1);
        columns.set(index - 1, column);
        columns.set(index, prevElement);
        column.getColumn().moveAbove(prevElement.getColumn());

        renameColumns();
        refresh(UpdateLinkTable.COLUMN_PATHS_MOVE_CREATE_OR_DELETE);
    }

    public void moveColumnDown(LinkTableColumn column) {
        int index = columns.indexOf(column);

        if (index < 0) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Column " + column + "not found");
            }
            return;
        }
        if (index == columns.size() - 1) {

            if (LOG.isDebugEnabled()) {
                LOG.debug("Column " + column + " is last column, not possible to move down");
            }
            return;
        }
        LinkTableColumn nextElement = columns.get(index + 1);
        columns.set(index + 1, column);
        columns.set(index, nextElement);
        column.getColumn().moveBelow(nextElement.getColumn());

        renameColumns();
        refresh(UpdateLinkTable.COLUMN_PATHS_MOVE_CREATE_OR_DELETE);
    }
}