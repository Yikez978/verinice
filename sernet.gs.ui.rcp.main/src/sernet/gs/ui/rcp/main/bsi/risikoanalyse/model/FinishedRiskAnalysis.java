package sernet.gs.ui.rcp.main.bsi.risikoanalyse.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import sernet.gs.ui.rcp.main.bsi.model.CnaStructureHelper;
import sernet.gs.ui.rcp.main.bsi.model.MassnahmenUmsetzung;
import sernet.gs.ui.rcp.main.common.model.CnATreeElement;

public class FinishedRiskAnalysis extends CnATreeElement {

	public static final String TYPE_ID = "riskanalysis";
	private Set<CnATreeElement> emptyList = new HashSet<CnATreeElement>();
	private CnATreeElement[] emptyArray = new CnATreeElement[] {};
	
	public FinishedRiskAnalysis(CnATreeElement cnaElement) {
		setParent(cnaElement);
	}
	
	@Override
	public Set<CnATreeElement> getChildren() {
		return emptyList;
	}
	
	@Override
	public CnATreeElement[] getChildrenAsArray() {
		return emptyArray;
	}

	@Override
	public String getTitel() {
		return "Risikoanalyse";
	}

	@Override
	public String getTypeId() {
		return this.TYPE_ID;
	}
	
	@Override
	public boolean canContain(Object obj) {
		if (obj instanceof MassnahmenUmsetzung
				|| obj instanceof GefaehrdungsUmsetzung)
			return true;
		return CnaStructureHelper.canContain(obj);
	}

}
