/*******************************************************************************
 * Copyright (c) 2009 Robert Schuster <r.schuster@tarent.de>.
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
 *     Robert Schuster <r.schuster@tarent.de> - initial API and implementation
 ******************************************************************************/
package sernet.gs.server.commands;

import java.util.ArrayList;
import java.util.List;

import sernet.gs.ui.rcp.main.bsi.model.MassnahmenUmsetzung;
import sernet.gs.ui.rcp.main.common.model.configuration.Configuration;

/**
 * Container class that holds the information generated by the
 * {@link PrepareExpirationNotification} command.
 * 
 * <p>The class is meant to make it easy to access the data in an iterative
 * way.</p>
 * 
 * @author Robert Schuster <r.schuster@tarent.de>
 *
 */
public class ExpirationInfo {

	final Configuration configuration;
	
	private boolean completionExpired;
	
	private boolean revisionExpired;

	private List<MassnahmenUmsetzung> globalExpiredCompletions = new ArrayList<MassnahmenUmsetzung>(); 
	
	private List<MassnahmenUmsetzung> globalExpiredRevisions = new ArrayList<MassnahmenUmsetzung>(); 
	
	ExpirationInfo(Configuration c)
	{
		configuration = c;
	}
	
	public int hashCode()
	{
		return configuration.hashCode();
	}
	
	public boolean equals(Object o)
	{
		if (o instanceof ExpirationInfo)
			return configuration.equals(((ExpirationInfo)o).configuration);
		
		return false;
	}

	public boolean isRevisionExpired() {
		return revisionExpired;
	}

	public void setRevisionExpired(boolean revisionExpired) {
		this.revisionExpired = revisionExpired;
	}

	public boolean isCompletionExpired() {
		return completionExpired;
	}

	public void setCompletionExpired(boolean completionExpired) {
		this.completionExpired = completionExpired;
	}
	
	public void addGlobalExpiredCompletion(MassnahmenUmsetzung mu)
	{
		globalExpiredCompletions.add(mu);
	}
	
	public List<MassnahmenUmsetzung> getGlobalExpiredCompletions()
	{
		return globalExpiredCompletions;
	}
	
	public void addGlobalExpiredRevision(MassnahmenUmsetzung mu)
	{
		globalExpiredRevisions.add(mu);
	}

	public List<MassnahmenUmsetzung> getGlobalExpiredRevisions()
	{
		return globalExpiredRevisions;
	}

	public Configuration getConfiguration() {
		return configuration;
	}

}
