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

import java.text.MessageFormat;
import java.util.function.Function;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Dialog;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import org.scrivo.markdownfx.editor.MarkdownEditorPane;
import org.scrivo.markdownfx.editor.SmartEdit;
import org.scrivo.markdownfx.util.Action;
import org.scrivo.markdownfx.util.ActionUtils;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.*;

/**
 * Main window containing a tab pane in the center for file editors.
 *
 * @author Karl Tauber
 */
public class MarkdownEditorControl extends BorderPane
{
	private final MarkdownEditor theFileEditor;

	private final ReadOnlyObjectWrapper<MarkdownEditor> fileEditor = new ReadOnlyObjectWrapper<>();

	public MarkdownEditorControl() {
		theFileEditor = new MarkdownEditor(this, this, null);
		fileEditor.set(theFileEditor);

		this.setPrefSize(800, 800);
		this.setTop(createMenuBarAndToolBar());
		//borderPane.setCenter(theFileEditor.getNode());

	}


	private Node createMenuBarAndToolBar() {
		BooleanBinding activeFileEditorIsNull = fileEditor.isNull();

		// Edit actions
		Action editUndoAction = new Action(Messages.get("MainWindow.editUndoAction"), "Shortcut+Z", UNDO,
				e -> getActiveEditor().undo(),
				createActiveBooleanProperty(MarkdownEditor::canUndoProperty).not());
		Action editRedoAction = new Action(Messages.get("MainWindow.editRedoAction"), "Shortcut+Y", REPEAT,
				e -> getActiveEditor().redo(),
				createActiveBooleanProperty(MarkdownEditor::canRedoProperty).not());
		Action editFindAction = new Action(Messages.get("MainWindow.editFindAction"), "Shortcut+F", SEARCH,
				e -> getActiveEditor().find(false),
				activeFileEditorIsNull);
		Action editReplaceAction = new Action(Messages.get("MainWindow.editReplaceAction"), "Shortcut+H", RETWEET,
				e -> getActiveEditor().find(true),
				activeFileEditorIsNull);
		Action editFindNextAction = new Action(Messages.get("MainWindow.editFindNextAction"), "F3", null,
				e -> getActiveEditor().findNextPrevious(true),
				activeFileEditorIsNull);
		Action editFindPreviousAction = new Action(Messages.get("MainWindow.editFindPreviousAction"), "Shift+F3", null,
				e -> getActiveEditor().findNextPrevious(false),
				activeFileEditorIsNull);

		
		// Insert actions
		Action insertBoldAction = new Action(Messages.get("MainWindow.insertBoldAction"), "Shortcut+B", BOLD,
				e -> getActiveSmartEdit().surroundSelection("**", "**"),
				activeFileEditorIsNull);
		Action insertItalicAction = new Action(Messages.get("MainWindow.insertItalicAction"), "Shortcut+I", ITALIC,
				e -> getActiveSmartEdit().surroundSelection("*", "*"),
				activeFileEditorIsNull);
		Action insertStrikethroughAction = new Action(Messages.get("MainWindow.insertStrikethroughAction"), "Shortcut+T", STRIKETHROUGH,
				e -> getActiveSmartEdit().surroundSelection("~~", "~~"),
				activeFileEditorIsNull);
		Action insertBlockquoteAction = new Action(Messages.get("MainWindow.insertBlockquoteAction"), "Ctrl+Q", QUOTE_LEFT, // not Shortcut+Q because of conflict on Mac
				e -> getActiveSmartEdit().surroundSelection("\n\n> ", ""),
				activeFileEditorIsNull);
		Action insertCodeAction = new Action(Messages.get("MainWindow.insertCodeAction"), "Shortcut+K", CODE,
				e -> getActiveSmartEdit().surroundSelection("`", "`"),
				activeFileEditorIsNull);
		Action insertFencedCodeBlockAction = new Action(Messages.get("MainWindow.insertFencedCodeBlockAction"), "Shortcut+Shift+K", FILE_CODE_ALT,
				e -> getActiveSmartEdit().surroundSelection("\n\n```\n", "\n```\n\n", Messages.get("MainWindow.insertFencedCodeBlockText")),
				activeFileEditorIsNull);

		Action insertLinkAction = new Action(Messages.get("MainWindow.insertLinkAction"), "Shortcut+L", LINK,
				e -> getActiveSmartEdit().insertLink(),
				activeFileEditorIsNull);
		Action insertImageAction = new Action(Messages.get("MainWindow.insertImageAction"), "Shortcut+G", PICTURE_ALT,
				e -> getActiveSmartEdit().insertImage(),
				activeFileEditorIsNull);

		Action insertHeader1Action = new Action(Messages.get("MainWindow.insertHeader1Action"), "Shortcut+1", HEADER,
				e -> getActiveSmartEdit().surroundSelection("\n\n# ", "", Messages.get("MainWindow.insertHeader1Text")),
				activeFileEditorIsNull);
		Action insertHeader2Action = new Action(Messages.get("MainWindow.insertHeader2Action"), "Shortcut+2", HEADER,
				e -> getActiveSmartEdit().surroundSelection("\n\n## ", "", Messages.get("MainWindow.insertHeader2Text")),
				activeFileEditorIsNull);
		Action insertHeader3Action = new Action(Messages.get("MainWindow.insertHeader3Action"), "Shortcut+3", HEADER,
				e -> getActiveSmartEdit().surroundSelection("\n\n### ", "", Messages.get("MainWindow.insertHeader3Text")),
				activeFileEditorIsNull);
		Action insertHeader4Action = new Action(Messages.get("MainWindow.insertHeader4Action"), "Shortcut+4", HEADER,
				e -> getActiveSmartEdit().surroundSelection("\n\n#### ", "", Messages.get("MainWindow.insertHeader4Text")),
				activeFileEditorIsNull);
		Action insertHeader5Action = new Action(Messages.get("MainWindow.insertHeader5Action"), "Shortcut+5", HEADER,
				e -> getActiveSmartEdit().surroundSelection("\n\n##### ", "", Messages.get("MainWindow.insertHeader5Text")),
				activeFileEditorIsNull);
		Action insertHeader6Action = new Action(Messages.get("MainWindow.insertHeader6Action"), "Shortcut+6", HEADER,
				e -> getActiveSmartEdit().surroundSelection("\n\n###### ", "", Messages.get("MainWindow.insertHeader6Text")),
				activeFileEditorIsNull);

		Action insertUnorderedListAction = new Action(Messages.get("MainWindow.insertUnorderedListAction"), "Shortcut+U", LIST_UL,
				e -> getActiveSmartEdit().surroundSelection("\n\n* ", ""),
				activeFileEditorIsNull);
		Action insertOrderedListAction = new Action(Messages.get("MainWindow.insertOrderedListAction"), "Shortcut+Shift+O", LIST_OL,
				e -> getActiveSmartEdit().surroundSelection("\n\n1. ", ""),
				activeFileEditorIsNull);
		Action insertHorizontalRuleAction = new Action(Messages.get("MainWindow.insertHorizontalRuleAction"), "Shortcut+H", null,
				e -> getActiveSmartEdit().surroundSelection("\n\n---\n\n", ""),
				activeFileEditorIsNull);

		//---- ToolBar ----

		ToolBar toolBar = ActionUtils.createToolBar(
				editUndoAction,
				editRedoAction,
				null,
				insertBoldAction,
				insertItalicAction,
				insertBlockquoteAction,
				insertCodeAction,
				insertFencedCodeBlockAction,
				null,
				insertLinkAction,
				insertImageAction,
				null,
				insertHeader1Action,
				insertHeader2Action,
				insertHeader3Action,
				null,
				insertUnorderedListAction,
				insertOrderedListAction);

		return new VBox(toolBar);
	}

	private MarkdownEditorPane getActiveEditor() {
		return theFileEditor.getEditor();
	}

	private SmartEdit getActiveSmartEdit() {
		return getActiveEditor().getSmartEdit();
	}

	/**
	 * Creates a boolean property that is bound to another boolean value
	 * of the active editor.
	 */
	private BooleanProperty createActiveBooleanProperty(Function<MarkdownEditor, ObservableBooleanValue> func) {
		BooleanProperty b = new SimpleBooleanProperty();
		if (fileEditor != null)
			b.bind(func.apply(theFileEditor));
			fileEditor.addListener((observable, oldFileEditor, newFileEditor) -> {
			b.unbind();
			if (newFileEditor != null)
				b.bind(func.apply(newFileEditor));
			else
				b.set(false);
		});
		return b;
	}

	Alert createAlert(AlertType alertType, String title,
		String contentTextFormat, Object... contentTextArgs)
	{
		Alert alert = new Alert(alertType);
		alert.setTitle(title);
		alert.setHeaderText(null);
		alert.setContentText(MessageFormat.format(contentTextFormat, contentTextArgs));
		alert.initOwner(getScene().getWindow());
		return alert;
	}

	public String getMarkdown() {
		return theFileEditor.getMarkdown();
	}

	public String getHTML() {
		return theFileEditor.getHTML();
	}

	public void setMarkdown(String md) {
		theFileEditor.setMarkdown(md);
	}

	public void setImageDialogClassName(String className) {
		theFileEditor.setImageDialogClassName(className);
	}

}
