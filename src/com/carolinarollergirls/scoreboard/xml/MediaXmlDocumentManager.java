package com.carolinarollergirls.scoreboard.xml;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.*;
import java.io.*;

import org.jdom.*;
import org.jdom.xpath.*;

import org.apache.commons.io.monitor.*;
import org.apache.commons.io.filefilter.*;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.jetty.*;
import com.carolinarollergirls.scoreboard.xml.*;

public class MediaXmlDocumentManager extends PartialOpenXmlDocumentManager implements XmlDocumentManager
{
	public MediaXmlDocumentManager(String n, String m) {
		super(n);
		mediaName = m;
	}

	public void reset() {
		synchronized (typeMonitors) {
			Iterator<String> types = typeMonitors.keySet().iterator();
			while (types.hasNext()) {
				String type = types.next();
				try {
					typeMonitors.get(type).stop();
				} catch (Exception e) {
					ScoreBoardManager.printMessage("MediaXmlDocumentManager for "+getMediaName()+" ERROR: Could not stop type "+type+" monitor : "+e.getMessage());
				}
			}
			typeMonitors.clear();
		}

		super.reset();
		monitorTypes();
	}

	protected boolean checkMediaNameElement(Element type, Element e) throws Exception {
		String id = editor.getId(e);
		Element mediaE = editor.getElement(type, getMediaName(), id, false);
		if (mediaE == null) /* no matching media element in our XML */
			return false;
		Element name = editor.getElement(e, "Name", null, false);
		if (name == null) /* no update to Name */
			return false;
		/* Only allow plain text content, drop any child nodes */
		String text = editor.getText(name);
		name.removeContent();
		editor.setText(name, text);
		return true;
	}

	/* Only allow setting Name for currently existing media */
	protected void processChildElement(Element e) throws Exception {
		synchronized (updateLock) { /* prevent element removal while/before updating */
			if (e.getName().equals("Type")) {
				Element type = editor.getElement(getXPathElement(), "Type", editor.getId(e), false);
				if (type == null)
					return;
				Iterator i = e.getChildren(getMediaName()).iterator();
				while (i.hasNext())
					if (!checkMediaNameElement(type, (Element)i.next()))
						i.remove();
			}
			super.processChildElement(e);
		}
	}

	protected String getPartialXPathString() {
		return getXPathString()+"/Type/"+getMediaName()+"/Name";
	}

	protected void monitorTypes() {
		File managedDir = new File(htmlDirName, getManagedDirName());
		Iterator<String> types = Arrays.asList(managedDir.list(DirectoryFileFilter.INSTANCE)).iterator();
		while (types.hasNext())
			monitorType(managedDir, types.next());
	}

	protected void monitorType(File managedDir, String type) {
		File typeDir = new File(managedDir, type);
		update(editor.getElement(createXPathElement(), "Type", type));
		FileAlterationObserver faO = new FileAlterationObserver(typeDir, mediaFileFilter);
		faO.addListener(new MediaTypeDirAlterationListener(type));
		FileAlterationMonitor monitor = new FileAlterationMonitor(INTERVAL, faO);
		try {
			monitor.start();
		} catch (Exception e) {
			ScoreBoardManager.printMessage("MediaXmlDocumentManager for "+getMediaName()+" ERROR: Could not start type "+type+" monitor : "+e.getMessage());
		}
		typeMonitors.put(type, monitor);
		Iterator<File> files = Arrays.asList(typeDir.listFiles((FileFilter)mediaFileFilter)).iterator();
		while (files.hasNext())
			addMediaElement(type, files.next());
	}

	protected String getManagedDirName() { return getManagedElementName().toLowerCase(); }
	protected String getMediaName() { return mediaName; }

	protected Element createTypeElement(String type) {
		return editor.getElement(createXPathElement(), "Type", type);
	}
	protected void addMediaElement(String type, File file) {
		synchronized (updateLock) {
			Element e = editor.getElement(createTypeElement(type), getMediaName(), file.getName());
			editor.addElement(e, "Name", null, file.getName().replaceAll("\\.[^.]*$", ""));
			editor.addElement(e, "Src", null, "/"+getManagedDirName()+"/"+type+"/"+file.getName());
			update(e);
		}
	}
	protected void removeMediaElement(String type, File file) {
		synchronized (updateLock) {
			update(editor.setRemovePI(editor.getElement(createTypeElement(type), getMediaName(), file.getName())));
		}
	}

	private Object updateLock = new Object();
	private Map<String, FileAlterationMonitor> typeMonitors = new Hashtable<String, FileAlterationMonitor>();
	private String mediaName;

	private String htmlDirName = ScoreBoardManager.getProperty(JettyServletScoreBoardController.PROPERTY_HTML_DIR_KEY);

	protected class MediaTypeDirAlterationListener extends FileAlterationListenerAdaptor implements FileAlterationListener
	{
		public MediaTypeDirAlterationListener(String t) { type = t; }
		public void onFileCreate(File f) { addMediaElement(type, f); }
		public void onFileDelete(File f) { removeMediaElement(type, f); }
		protected String type;
	}

	public static final String dbFileRegex = "^.*[.][dD][bB]$";
	public static final String dotFileRegex = "^[.].*$";
	public static final String invalidFileRegex = dbFileRegex+"|"+dotFileRegex;
	public static final IOFileFilter mediaFileNameFilter = new NotFileFilter(new RegexFileFilter(invalidFileRegex));
	public static final IOFileFilter mediaFileFilter = new AndFileFilter(FileFileFilter.FILE, mediaFileNameFilter);

	public static final long INTERVAL = 1000; // in ms
}
