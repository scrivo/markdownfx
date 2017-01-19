/*
 * Copyright (c) 2015 Karl Tauber <karl at jformdesigner dot com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  o Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 *  o Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.scrivo.markdownfx;

import java.nio.file.Path;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Dialog;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.layout.BorderPane;
import org.fxmisc.undo.UndoManager;
import org.scrivo.markdownfx.editor.MarkdownEditorPane;
import org.scrivo.markdownfx.options.Options;
import org.scrivo.markdownfx.options.Options.RendererType;
import org.scrivo.markdownfx.preview.MarkdownPreviewPane;
import org.scrivo.markdownfx.preview.MarkdownPreviewPane.Type;
import org.scrivo.markdownfx.util.PrefsBooleanProperty;

/**
 * Editor for a single file.
 *
 * @author Karl Tauber
 */
class MarkdownEditor
{
	private final MarkdownEditorControl mainWindow;
	private MarkdownEditorPane markdownEditorPane;
	private MarkdownPreviewPane markdownPreviewPane;

	BorderPane parent;

	private Node node;

	MarkdownEditor(MarkdownEditorControl mainWindow, BorderPane parent, Path path) {
		this.mainWindow = mainWindow;
		this.path.set(path);
		this.parent = parent;

		this.path.addListener((observable, oldPath, newPath) -> updateTab());
		this.modified.addListener((observable, oldPath, newPath) -> updateTab());
		updateTab();

		Platform.runLater(() -> activated());
	}

	Tab getTab() {
		return null;
	}

	MarkdownEditorPane getEditor() {
		return markdownEditorPane;
	}

	// 'path' property
	private final ObjectProperty<Path> path = new SimpleObjectProperty<>();
	Path getPath() { return path.get(); }
	void setPath(Path path) { this.path.set(path); }
	ObjectProperty<Path> pathProperty() { return path; }

	// 'modified' property
	private final ReadOnlyBooleanWrapper modified = new ReadOnlyBooleanWrapper();
	boolean isModified() { return modified.get(); }
	ReadOnlyBooleanProperty modifiedProperty() { return modified.getReadOnlyProperty(); }

	// 'canUndo' property
	private final BooleanProperty canUndo = new SimpleBooleanProperty();
	BooleanProperty canUndoProperty() { return canUndo; }

	// 'canRedo' property
	private final BooleanProperty canRedo = new SimpleBooleanProperty();
	private String tmpMarkDown;
	BooleanProperty canRedoProperty() { return canRedo; }

	private void updateTab() {
	}

	final PrefsBooleanProperty previewVisible = new PrefsBooleanProperty(true);
	final PrefsBooleanProperty htmlSourceVisible = new PrefsBooleanProperty();
	final PrefsBooleanProperty markdownAstVisible = new PrefsBooleanProperty();
	
	private boolean updatePreviewTypePending;
	
	private String imageDialogClassName;
	
	private void updatePreviewType() {
		if (markdownPreviewPane == null)
			return;

		// avoid too many (and useless) runLater() invocations
		if (updatePreviewTypePending)
			return;
		updatePreviewTypePending = true;

		Platform.runLater(() -> {
			updatePreviewTypePending = false;

			MarkdownPreviewPane.Type previewType = getPreviewType();

			//markdownPreviewPane.setRendererType(Options.getMarkdownRenderer());
			markdownPreviewPane.setRendererType(RendererType.FlexMark);
			markdownPreviewPane.setType(previewType);

			// add/remove previewPane from splitPane
			ObservableList<Node> splitItems = ((SplitPane)node).getItems();
			Node previewPane = markdownPreviewPane.getNode();
			if (previewType != Type.None) {
				if (!splitItems.contains(previewPane))
					splitItems.add(previewPane);
			} else
				splitItems.remove(previewPane);
		});
	}

	
	private MarkdownPreviewPane.Type getPreviewType() {
		MarkdownPreviewPane.Type previewType = Type.None;
		if (previewVisible.get())
			previewType = MarkdownPreviewPane.Type.Web;
		if (htmlSourceVisible.get())
			previewType = MarkdownPreviewPane.Type.Source;
		if (markdownAstVisible.get())
			previewType = MarkdownPreviewPane.Type.Ast;
		return previewType;
	}

	private void activated() {
		// load file and create UI when the tab becomes visible the first time

		markdownEditorPane = new MarkdownEditorPane();
		markdownPreviewPane = new MarkdownPreviewPane();

		if (null != this.tmpMarkDown) {
			markdownEditorPane.setMarkdown(this.tmpMarkDown);
			this.tmpMarkDown = null;
		}

		if (null != this.imageDialogClassName) {
			markdownEditorPane.getSmartEdit().setImageDialogClassName(imageDialogClassName);
			this.imageDialogClassName = null;
		}

			// clear undo history after first load
		markdownEditorPane.getUndoManager().forgetHistory();

		// bind preview to editor
		markdownPreviewPane.pathProperty().bind(pathProperty());
		markdownPreviewPane.markdownTextProperty().bind(markdownEditorPane.markdownTextProperty());
		markdownPreviewPane.markdownASTProperty().bind(markdownEditorPane.markdownASTProperty());
		markdownPreviewPane.scrollYProperty().bind(markdownEditorPane.scrollYProperty());

		// bind the editor undo manager to the properties
		UndoManager undoManager = markdownEditorPane.getUndoManager();
		modified.bind(Bindings.not(undoManager.atMarkedPositionProperty()));
		canUndo.bind(undoManager.undoAvailableProperty());
		canRedo.bind(undoManager.redoAvailableProperty());

		SplitPane splitPane = new SplitPane(markdownEditorPane.getNode());
		if (getPreviewType() != MarkdownPreviewPane.Type.None)
			splitPane.getItems().add(markdownPreviewPane.getNode());
		parent.setCenter(splitPane);
		//tab.setContent(splitPane);

		updatePreviewType();
		markdownEditorPane.requestFocus();

		node = splitPane;
	}

	public Node getNode() {
		return node;
	}

	public String getMarkdown() {
		if (null != markdownEditorPane) {
			return markdownEditorPane.getMarkdown();
		}
		return "";
	}

	public String getHTML() {
		if (null != markdownPreviewPane) {
			return markdownPreviewPane.getHTML();
		}
		return "";
	}

	public void setMarkdown(String md) {
		if (null != markdownEditorPane) {
			markdownEditorPane.setMarkdown(md);
			markdownEditorPane.getUndoManager().mark();
		} else {
			this.tmpMarkDown = md;
		}

	}

	public void setImageDialogClassName(String dlgClazz) {
		if (null != markdownEditorPane) {
			markdownEditorPane.getSmartEdit().setImageDialogClassName(dlgClazz);
		} else {
			this.imageDialogClassName = dlgClazz;
		}
	}

}
