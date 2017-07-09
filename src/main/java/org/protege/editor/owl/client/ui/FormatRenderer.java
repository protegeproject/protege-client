package org.protege.editor.owl.client.ui;

import org.protege.editor.core.ui.error.ErrorLogPanel;

import javax.swing.table.DefaultTableCellRenderer;
import java.text.DateFormat;
import java.text.Format;

/*
 * Code from: http://tips4java.wordpress.com/2008/10/11/table-format-renderers/
 */
public class FormatRenderer extends DefaultTableCellRenderer {

	private static final long serialVersionUID = -5210230827997957826L;
	private Format formatter;

	/*
	 *   Use the specified formatter to format the Object
	 */
	public FormatRenderer(Format formatter) {
		this.formatter = formatter;
	}

	public void setValue(Object value) {
		//  Format the Object before setting its value in the renderer
		try {
			if (value != null) {
				value = formatter.format(value);
			}
		}
		catch(IllegalArgumentException e) {
			ErrorLogPanel.showErrorDialog(e);
		}

		super.setValue(value);
	}

	/*
	 *  Use the default date/time formatter for the default locale
	 */
	public static FormatRenderer getDateTimeRenderer() {
		return new FormatRenderer(DateFormat.getDateTimeInstance());
	}

	/*
	 *  Use the default time formatter for the default locale
	 */
	public static FormatRenderer getTimeRenderer() {
		return new FormatRenderer(DateFormat.getTimeInstance());
	}
}
